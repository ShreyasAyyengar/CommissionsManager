package dev.shreyasayyengar.bot.misc.managers;

import dev.shreyasayyengar.bot.DiscordBot;
import dev.shreyasayyengar.bot.client.ClientCommission;
import dev.shreyasayyengar.bot.client.ClientInfo;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.util.HashMap;

/**
 * The ClientInfoManager, instantiated in the {@link DiscordBot} class, is used to manage ClientInfo objects.
 *
 * <p></p>
 *
 * @author Shreyas Ayyengar
 */
public class ClientInfoManager {

    private final HashMap<String, ClientInfo> clientInfoMap = new HashMap<>();

    /**
     * The registerExistingClients is used to pull data from the provided MySQL database
     * and reconstruct the ClientInfo objects using {@link ClientInfo#ClientInfo(String, String, String)}
     */
    public void registerExistingClients() {
        DiscordBot.get().database.preparedStatementBuilder("select * from CM_client_info;").executeQuery(resultSet -> {
            try {

                while (resultSet.next()) {

                    String member_id = resultSet.getString("member_id");
                    String text_id = resultSet.getString("text_id");
                    String voice_id = resultSet.getString("voice_id");
                    String paypal_email = resultSet.getString("paypal_email");

                    if (DiscordBot.get().workingGuild.getMemberById(member_id) != null) {
                        new ClientInfo(member_id, voice_id, text_id).setPaypalEmail(paypal_email);
                    }
                }

            } catch (Exception err) {
                err.printStackTrace();
            }
        });
    }

    /**
     * This method purges all information tied to a {@link ClientInfo} (if any) from the {@link User}
     * passed in. This also removed any information from the MySQL databse.
     */
    public void purgeMemberSQL(User user) {
        try {
            ClientInfo remove = DiscordBot.get().getClientManger().getMap().remove(user.getId());
            remove.getCommissions().forEach(ClientCommission::close);

            DiscordBot.get().database.preparedStatementBuilder("delete from CM_client_info where member_id = ?;").setString(1, user.getId()).executeUpdate();
        } catch (Exception err) {
            err.printStackTrace();
        }
    }

    /**
     * Gets the {@link ClientInfo} object associated with the provided {@link TextChannel}
     */
    public ClientInfo getByTextChannel(TextChannel textChannel) {
        for (ClientInfo clientInfo : clientInfoMap.values()) {
            if (clientInfo.getTextChannel().getId().equalsIgnoreCase(textChannel.getId())) {
                return clientInfo;
            }
        }

        return null;
    }

    /**
     * Gets the ClientInfo object associated with the provided user ID.
     */
    public ClientInfo get(String id) {
        return clientInfoMap.get(id);
    }

    public HashMap<String, ClientInfo> getMap() {
        return clientInfoMap;
    }

    public void add(String id, ClientInfo clientInfo) {
        clientInfoMap.put(id, clientInfo);
    }

    public boolean containsInfoOf(String id) {
        return clientInfoMap.containsKey(id);
    }
}