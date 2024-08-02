package dev.shreyasayyengar.bot.customer;

import dev.shreyasayyengar.bot.DiscordBot;
import dev.shreyasayyengar.bot.paypal.Invoice;
import dev.shreyasayyengar.bot.utils.Util;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.attribute.IPermissionContainer;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.requests.restaction.ChannelAction;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * The Customer class is used to store all data that oversees a customer in the Discord Server. This class stores
 * the customer's private TextChannels and VoiceChannels to manage sending messages and conversations.
 * The Customer class also stores active {@link CustomerCommission}s.
 * <p></p>
 *
 * @author Shreyas Ayyengar
 */
public class Customer {

    private static final Category CHAT_CATEGORY = DiscordBot.get().workingGuild.getCategoryById("1091671476752613396");
    private static final Category VOICE_CATEGORY = DiscordBot.get().workingGuild.getCategoryById("1091671511569551371");

    private final Collection<CustomerCommission> commissions = new HashSet<>();

    private final Member holder;
    private final TextChannel textChannel;
    private VoiceChannel temporaryVoiceChannel;

    private String paypalEmail;

    /**
     * This creates a new Customer object, given the JDA Member object of the user. <b>This constructor should
     * only be used when a new Member has joined a server.</b> If ran outside this context, the Customer object will generate
     * duplicate channels.
     * <p></p>
     * To <b>load</b> an existing Customer object via a ResultSet use the {@link #Customer(String, String)} constructor.
     *
     * <p></p>
     *
     * @param holder The JDA Member object of the user to wrap.
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public Customer(Member holder) {
        this.holder = holder;

        Guild workingGuild = DiscordBot.get().workingGuild;

        // region Text Channel
        ChannelAction<TextChannel> textChannelAction = workingGuild.createTextChannel(holder.getEffectiveName() + "-text").setParent(CHAT_CATEGORY)
                .setTopic("Discussions relating to " + holder.getEffectiveName() + "'s commissions & work");

        textChannelAction.addMemberPermissionOverride(holder.getIdLong(), Util.getAllowedPermissions(), Util.getDeniedPermissions());
        textChannelAction.addPermissionOverride(workingGuild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL));

        this.textChannel = textChannelAction.complete();
        // endregion

        DiscordBot.get().getCustomerManger().add(holder.getId(), this);
    }

    /**
     * This constructor is used to load an existing Customer object from a {@link ResultSet}. This constructor will
     * initialise the object and keep it in the CustomerManager, to be used for later. This <b>will not generate any new channels</b>
     * or run any other setup actions, this merely loads the Customer object to be recognised and visible.
     *
     * @param holderId      The Discord ID of the holder of the Customer object.
     * @param textChannelId The Discord ID of the TextChannel of the Customer object.
     */
    public Customer(String holderId, String textChannelId) {
        this.holder = DiscordBot.get().workingGuild.getMemberById(holderId);
        this.textChannel = DiscordBot.get().workingGuild.getTextChannelById(textChannelId);

        DiscordBot.get().getCustomerManger().add(holder.getId(), this);
    }

    /**
     * Add a Member to the customer's {@link #textChannel}. This enables the member to see the channel and its messages.
     */
    public void addCollaborator(Member member) {
        Stream<? extends IPermissionContainer> channelGroups = Stream.of(textChannel);
        if (temporaryVoiceChannel != null) {
            channelGroups = Stream.concat(channelGroups, Stream.of(temporaryVoiceChannel));
        }

        channelGroups.forEach(channel -> channel.upsertPermissionOverride(member).setPermissions(Util.getAllowedPermissions(), Util.getDeniedPermissions()).queue());

        textChannel.sendMessage(member.getAsMention()).queue(message -> message.delete().queue());
    }

    /**
     * Remove a Member from the customer's {@link #textChannel}. This disables the member from seeing the channel and its messages.
     */
    public void removeCollaborator(Member member) {
        Stream<? extends IPermissionContainer> channelGroups = Stream.of(textChannel);
        if (temporaryVoiceChannel != null) {
            channelGroups = Stream.concat(channelGroups, Stream.of(temporaryVoiceChannel));
        }

        channelGroups.forEach(channel -> channel.upsertPermissionOverride(member).setPermissions(new HashSet<>(), Arrays.stream(Permission.values()).toList()).queue());
    }

    public MessageEmbed tryGenerateTemporaryVoiceChannel() {
        if (this.temporaryVoiceChannel != null) {
            return new EmbedBuilder()
                    .setTitle("Voice Channel Already Exists")
                    .setDescription("You already have a voice channel: " + this.temporaryVoiceChannel.getAsMention())
                    .setColor(Util.THEME_COLOUR)
                    .build();
        }

        ChannelAction<VoiceChannel> temporaryVoiceChannel = DiscordBot.get().workingGuild.createVoiceChannel(holder.getEffectiveName() + "-vc").setParent(VOICE_CATEGORY)
                .setBitrate(64000);

        for (Member member : this.getTextChannel().getMembers()) {
            temporaryVoiceChannel.addMemberPermissionOverride(member.getIdLong(), Util.getAllowedPermissions(), Util.getDeniedPermissions()); // allow customer + collaborators
        }
        temporaryVoiceChannel.addPermissionOverride(DiscordBot.get().workingGuild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL)); // deny public

        this.temporaryVoiceChannel = temporaryVoiceChannel.complete();

        return new EmbedBuilder()
                .setTitle("Temporary Voice Channel Created")
                .setDescription("A temporary voice channel has been created for you: " + this.temporaryVoiceChannel.getAsMention())
                .setColor(Util.THEME_COLOUR)
                .build();
    }

    /**
     * Serialise the Customer object to the MySQL server.
     */
    public void serialise() {
        // Does the customer exist?
        DiscordBot.get().database.preparedStatementBuilder("SELECT * FROM customer_info WHERE member_id = '" + holder.getId() + "'").executeQuery(resultSet -> {
            try {

                if (resultSet.next()) {
                    // Update the customer info
                    DiscordBot.get().database.preparedStatementBuilder("UPDATE customer_info SET text_id = ?, paypal_email = ? WHERE member_id = ?")
                            .setString(textChannel.getId())
                            .setString(paypalEmail)
                            .setString(holder.getId())
                            .build().executeUpdate();
                } else {
                    // Create customer info
                    DiscordBot.get().database.preparedStatementBuilder("INSERT INTO customer_info (member_id, text_id, paypal_email) VALUES (?, ?, ?)")
                            .setString(holder.getId())
                            .setString(textChannel.getId())
                            .setString(paypalEmail == null ? null : paypalEmail)
                            .build().executeUpdate();
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Iterates through all CustomerCommissions inside the {@link #commissions} list to the MySQL
     * database.
     */
    public void serialiseCommissions() {
        for (CustomerCommission commission : commissions) {
            commission.serialise();
        }
    }

    // ------------------------------------------------- Getters ----------------------------------------------------//
    public Member getHolder() {
        return holder;
    }

    public TextChannel getTextChannel() {
        return textChannel;
    }

    public VoiceChannel getTemporaryVoiceChannel() {
        return temporaryVoiceChannel;
    }

    public CustomerCommission getCommission(String pluginName) {
        return commissions
                .stream()
                .filter(commission -> commission.getPluginName().equalsIgnoreCase(pluginName))
                .findFirst()
                .orElse(null);
    }

    public Invoice getInvoiceByID(String id) {
        return commissions
                .stream()
                .flatMap(commission -> commission.getInvoices().stream())
                .filter(invoice -> invoice.getID().equalsIgnoreCase(id))
                .findFirst()
                .orElse(null);
    }

    public Collection<CustomerCommission> getCommissions() {
        return commissions;
    }

    public String getPaypalEmail() {
        return paypalEmail;
    }

    public void setPaypalEmail(String paypalEmail) {
        this.paypalEmail = paypalEmail;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Customer that = (Customer) o;
        return Objects.equals(holder.getId().toLowerCase(), that.holder.getId().toLowerCase());
    }
}