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
import org.json.JSONArray;
import org.json.JSONObject;

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

//    private final Member user;
//    private final TextChannel textChannel;
    private  Member user;
    private  TextChannel textChannel;
    private VoiceChannel temporaryVoiceChannel;
    private String paypalEmail;

    /**
     * This creates a new Customer object, given the JDA Member object of the user. <b>This constructor should
     * only be used when a new Member has joined a server.</b> If ran outside this context, the Customer object will generate
     * duplicate channels.
     *
     * @param user The JDA Member object of the user to wrap.
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public Customer(Member user) {
        this.user = user;

        Guild workingGuild = DiscordBot.get().workingGuild;

        // region Text Channel
        ChannelAction<TextChannel> textChannelAction = workingGuild.createTextChannel(user.getEffectiveName() + "-text").setParent(CHAT_CATEGORY)
                .setTopic("Discussions relating to " + user.getEffectiveName() + "'s commissions & work");

        textChannelAction.addMemberPermissionOverride(user.getIdLong(), Util.getAllowedPermissions(), Util.getDeniedPermissions());
        textChannelAction.addPermissionOverride(workingGuild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL));

        this.textChannel = textChannelAction.complete();
        // endregion

        DiscordBot.get().getCustomerManger().add(user.getId(), this);
    }

    /**
     * This constructor is used to load an existing Customer object from a {@link ResultSet}. This constructor will
     * initialise the object and keep it in the CustomerManager, to be used for later. This <b>will not generate any new channels</b>
     * or run any other setup actions, this merely loads the Customer object to be recognised and visible.
     *
     * @param userId        The Discord ID of the holder of the Customer object.
     * @param textChannelId The Discord ID of the TextChannel of the Customer object.
     */
    public Customer(String userId, String textId, String paypalEmail, Collection<CustomerCommission> commissions) {
        try {
            this.user = DiscordBot.get().workingGuild.getMemberById(userId);
            this.textChannel = DiscordBot.get().workingGuild.getTextChannelById(textId);
            this.paypalEmail = paypalEmail;

            DiscordBot.get().getCustomerManger().add(user.getId(), this);
        } catch (Exception e) {
            System.out.println("Issue with customer: " + userId);
        }
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

        ChannelAction<VoiceChannel> temporaryVoiceChannel = DiscordBot.get().workingGuild.createVoiceChannel(user.getEffectiveName() + "-vc").setParent(VOICE_CATEGORY)
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
        DiscordBot.get().database.preparedStatementBuilder("SELECT * FROM customer_data WHERE user_id = '" + user.getId() + "'").executeQuery(resultSet -> {
            try {
                if (resultSet.next()) {
                    // Update the customer info
                    DiscordBot.get().database.preparedStatementBuilder("UPDATE customer_data SET data = ? WHERE user_id = ?")
                            .setString(toJSON().toString())
                            .setString(user.getId())
                            .build().executeUpdate();
                } else {
                    // Create customer info
                    DiscordBot.get().database.preparedStatementBuilder("INSERT INTO customer_data (user_id, data) VALUES (?, ?)")
                            .setString(user.getId())
                            .setString(toJSON().toString())
                            .build().executeUpdate();
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public JSONObject toJSON() {
        JSONObject customer = new JSONObject();
        customer.put("user_id", user.getId());
        customer.put("text_id", textChannel.getId());
        customer.put("paypal_email", paypalEmail);

        JSONArray commissions = new JSONArray();
        for (CustomerCommission commission : this.commissions) {
            JSONObject json = commission.toJSON();
            commissions.put(json);
        }
        customer.put("commissions", commissions);

        return customer;
    }

    // ------------------------------------------------- Getters ----------------------------------------------------//
    public Member getUser() {
        return user;
    }

    public Collection<CustomerCommission> getCommissions() {
        return commissions;
    }

    public CustomerCommission getCommission(String pluginName) {
        return commissions
                .stream()
                .filter(commission -> commission.getPluginName().equalsIgnoreCase(pluginName))
                .findFirst()
                .orElse(null);
    }

    public TextChannel getTextChannel() {
        return textChannel;
    }

    public VoiceChannel getTemporaryVoiceChannel() {
        return temporaryVoiceChannel;
    }

    public Invoice getInvoiceByID(String id) {
        return commissions
                .stream()
                .flatMap(commission -> commission.getInvoices().stream())
                .filter(invoice -> invoice.getId().equalsIgnoreCase(id))
                .findFirst()
                .orElse(null);
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
        return Objects.equals(user.getId().toLowerCase(), that.user.getId().toLowerCase());
    }
}