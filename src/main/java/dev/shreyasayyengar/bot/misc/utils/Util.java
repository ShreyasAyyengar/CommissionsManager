package dev.shreyasayyengar.bot.misc.utils;

import dev.shreyasayyengar.bot.DiscordBot;
import dev.shreyasayyengar.bot.client.ClientInfo;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.awt.*;

/**
 * Utility class for the program. (Possible for removal)
 *
 * @author Shreyas Ayyengar
 */
public class Util {

    private Util() {
    }

    public static boolean isThisGuild(Guild guild) {
        return guild.getId().equalsIgnoreCase(DiscordBot.get().workingGuild.getId());
    }

    public static boolean privateChannel(TextChannel textChannel) {
        boolean isPrivate = false;

        for (ClientInfo clientInfo : DiscordBot.get().getClientManger().getMap().values()) {
            if (clientInfo.getTextChannel().getId().equalsIgnoreCase(textChannel.getId())) {
                isPrivate = true;
            }
        }

        return !isPrivate;
    }

    public static ClientInfo getClientInfoByChannelId(Channel channel) {
        for (ClientInfo clientInfo : DiscordBot.get().getClientManger().getMap().values()) {
            if (clientInfo.getTextChannel().getId().equalsIgnoreCase(channel.getId())) {
                return clientInfo;
            }

            if (clientInfo.getVoiceChannel().getId().equalsIgnoreCase(channel.getId())) {
                return clientInfo;
            }
        }
        return null;
    }

    public static Color getColor() {
        return new Color(123, 3, 252);
    }
}