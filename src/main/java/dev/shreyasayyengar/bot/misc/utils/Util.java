package dev.shreyasayyengar.bot.misc.utils;

import dev.shreyasayyengar.bot.DiscordBot;
import dev.shreyasayyengar.bot.customer.Customer;
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

        for (Customer customer : DiscordBot.get().getCustomerManger().getMap().values()) {
            if (customer.getTextChannel().getId().equalsIgnoreCase(textChannel.getId())) {
                isPrivate = true;
            }
        }

        return !isPrivate;
    }

    public static Customer getCustomerByChannelId(Channel channel) {
        for (Customer customer : DiscordBot.get().getCustomerManger().getMap().values()) {
            if (customer.getTextChannel().getId().equalsIgnoreCase(channel.getId())) {
                return customer;
            }

            if (customer.getVoiceChannel().getId().equalsIgnoreCase(channel.getId())) {
                return customer;
            }
        }
        return null;
    }

    public static Color getColor() {
        return new Color(123, 3, 252);
    }
}