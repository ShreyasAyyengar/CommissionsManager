package dev.shreyasayyengar.bot.misc.managers;

import dev.shreyasayyengar.bot.DiscordBot;
import dev.shreyasayyengar.bot.client.ClientInfo;
import net.dv8tion.jda.api.entities.TextChannel;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClientInfoManager {

    private final HashMap<String, ClientInfo> clientInfoMap = new HashMap<>();

    public ClientInfoManager() {
        ExecutorService service = Executors.newSingleThreadExecutor();
        service.execute(() -> {
            try {
                ResultSet resultSet = DiscordBot.get().database.preparedStatement("select * from CM_client_info;").executeQuery();
                while (resultSet.next()) {

                    String member_id = resultSet.getString("member_id");
                    String text_id = resultSet.getString("text_id");
                    String voice_id = resultSet.getString("voice_id");
                    String category_id = resultSet.getString("category_id");
                    String paypal_email = resultSet.getString("paypal_email");

                    if (DiscordBot.get().workingGuild.getMemberById(member_id) != null) {
                        new ClientInfo(member_id, voice_id, text_id, category_id).setPaypalEmail(paypal_email);
                    }
                }

            } catch (Exception err) {
                err.printStackTrace();
            }
        });

        service.shutdown();
    }

    public HashMap<String, ClientInfo> getMap() {
        return clientInfoMap;
    }

    public void add(String id, ClientInfo clientInfo) {
        clientInfoMap.put(id, clientInfo);
    }

    public ClientInfo get(String id) {
        return clientInfoMap.get(id);
    }

    public ClientInfo getByTextChannel(TextChannel textChannel) {
        for (ClientInfo clientInfo : clientInfoMap.values()) {
            if (clientInfo.getTextChannel().getId().equalsIgnoreCase(textChannel.getId())) {
                return clientInfo;
            }
        }

        return null;
    }


    public boolean containsInfoOf(String id) {
        return clientInfoMap.containsKey(id);
    }
}
