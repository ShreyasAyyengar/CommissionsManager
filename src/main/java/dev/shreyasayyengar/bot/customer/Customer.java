package dev.shreyasayyengar.bot.customer;

import dev.shreyasayyengar.bot.DiscordBot;
import dev.shreyasayyengar.bot.misc.utils.EmbedUtil;
import dev.shreyasayyengar.bot.paypal.Invoice;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.attribute.ICopyableChannel;
import net.dv8tion.jda.api.entities.channel.attribute.IPermissionContainer;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.requests.restaction.ChannelAction;
import org.apache.commons.collections4.CollectionUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;
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
    private final VoiceChannel voiceChannel;
    private final TextChannel textChannel;

    private String paypalEmail;

    /**
     * This creates a new Customer object, given the JDA Member object of the user. <b>This constructor should
     * only be used when a new Member has joined a server.</b> If ran outside this context, the Customer object will generate
     * duplicate channels.
     * <p></p>
     * To <b>load</b> an existing Customer object via a ResultSet use the {@link #Customer(String, String, String)} constructor.
     *
     * <p></p>
     *
     * @param holder The JDA Member object of the user to wrap.
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public Customer(Member holder) {
        this.holder = holder;

        Guild workingGuild = DiscordBot.get().workingGuild;

        // region Voice and Text Channels
        ChannelAction<VoiceChannel> voiceChannelAction = workingGuild.createVoiceChannel(holder.getEffectiveName() + "-vc").setParent(VOICE_CATEGORY)
                .setBitrate(64000);

        ChannelAction<TextChannel> textChannelAction = workingGuild.createTextChannel(holder.getEffectiveName() + "-text").setParent(CHAT_CATEGORY)
                .setTopic("Discussions relating to " + holder.getEffectiveName() + "'s commissions & work");
        // endregion

        // region Permissions
        Stream<? extends ChannelAction<? extends ICopyableChannel>> channels = Stream.of(voiceChannelAction, textChannelAction);
        channels.forEach(channel -> {
            channel.addMemberPermissionOverride(holder.getIdLong(), getAllowedPermissions(), getDeniedPermissions());
            channel.addPermissionOverride(workingGuild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL));
        });
        // endregion

        // Finalising & Creating
        this.voiceChannel = voiceChannelAction.complete();
        this.textChannel = textChannelAction.complete();

        DiscordBot.get().getCustomerManger().add(holder.getId(), this);
    }

    /**
     * This constructor is used to load an existing Customer object from a {@link ResultSet}. This constructor will
     * initialise the object and keep it in the CustomerManager, to be used for later. This <b>will not generate any new channels</b>
     * or run any other setup actions, this merely loads the Customer object to be recognised and visible.
     *
     * @param holderId       The Discord ID of the holder of the Customer object.
     * @param voiceChannelId The Discord ID of the VoiceChannel of the Customer object.
     * @param textChannelId  The Discord ID of the TextChannel of the Customer object.
     */
    public Customer(String holderId, String voiceChannelId, String textChannelId) {
        this.holder = DiscordBot.get().workingGuild.getMemberById(holderId);
        this.voiceChannel = DiscordBot.get().workingGuild.getVoiceChannelById(voiceChannelId);
        this.textChannel = DiscordBot.get().workingGuild.getTextChannelById(textChannelId);

        DiscordBot.get().getCustomerManger().add(holder.getId(), this);
    }

    /**
     * Add a Member to the customer's {@link #textChannel}. This enables the member to see the channel and its messages.
     */
    public void addCollaborator(Member member) {
        Stream<? extends IPermissionContainer> channelGroups = Stream.of(textChannel, voiceChannel);
        channelGroups.forEach(channel -> channel.upsertPermissionOverride(member).setPermissions(getAllowedPermissions(), getDeniedPermissions()).queue());

        textChannel.sendMessage(member.getAsMention()).queue(message -> message.delete().queue());
        textChannel.sendMessageEmbeds(EmbedUtil.joinedAsCollaborator(member)).queue();
    }

    /**
     * Remove a Member from the customer's {@link #textChannel}. This disables the member from seeing the channel and its messages.
     */
    public void removeCollaborator(Member member) {
        Stream<? extends IPermissionContainer> channelGroups = Stream.of(textChannel, voiceChannel);
        channelGroups.forEach(channel -> channel.upsertPermissionOverride(member).setPermissions(new HashSet<>(), Arrays.stream(Permission.values()).toList()).queue());

        textChannel.sendMessageEmbeds(EmbedUtil.leftAsCollaborator(member)).queue();
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
                    DiscordBot.get().database.preparedStatementBuilder("UPDATE customer_info SET voice_id = ?, text_id = ?, paypal_email = ? WHERE member_id = ?")
                            .setString(voiceChannel.getId())
                            .setString(textChannel.getId())
                            .setString(paypalEmail)
                            .setString(holder.getId())
                            .build().executeUpdate();
                } else {
                    // Create customer info
                    DiscordBot.get().database.preparedStatementBuilder("insert into customer_info (member_id, text_id, voice_id, paypal_email) values (?, ?, ?, ?)")
                            .setString(holder.getId())
                            .setString(textChannel.getId())
                            .setString(voiceChannel.getId())
                            .setString(paypalEmail == null ? null : paypalEmail)
                            .build().executeUpdate();
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * This removes the CustomerCommission passed in from the {@link #commissions} list and from
     * the global CustomerCommission list.
     */
    public void closeCommission(CustomerCommission commission) {
        commission.close();
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

    public VoiceChannel getVoiceChannel() {
        return voiceChannel;
    }

    public CustomerCommission getCommission(String pluginName) {
        return commissions
                .stream()
                .filter(commission -> commission.getPluginName().equalsIgnoreCase(pluginName))
                .findFirst()
                .orElse(null);
    }

    public Invoice getInvoice(String id) {
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

    public Collection<Permission> getAllowedPermissions() {
        return Stream.of(
                Permission.VIEW_CHANNEL,
                Permission.MESSAGE_SEND,
                Permission.MESSAGE_ADD_REACTION,
                Permission.MESSAGE_EMBED_LINKS,
                Permission.MESSAGE_ATTACH_FILES,
                Permission.VOICE_CONNECT,
                Permission.VOICE_SPEAK,
                Permission.VOICE_USE_VAD
        ).collect(Collectors.toList());
    }

    public Collection<Permission> getDeniedPermissions() {
        return CollectionUtils.subtract(getAllowedPermissions(), Arrays.stream(Permission.values()).toList());
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