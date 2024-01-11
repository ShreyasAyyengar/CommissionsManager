package dev.shreyasayyengar.bot.misc.utils;

import dev.shreyasayyengar.bot.DiscordBot;
import dev.shreyasayyengar.bot.customer.Customer;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;

import java.awt.*;

/**
 * Utility class for the program. (Possible for removal)
 *
 * @author Shreyas Ayyengar
 */
public class Util {
    public static final Color THEME_COLOUR = new Color(123, 3, 252);

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

    public static Customer getCustomerByGenericChannel(Channel channel) {
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

    public static Modal submitEmailModal() {
        Modal.Builder emailModal = Modal.create("submit-email", "Submit your email");
        emailModal.addActionRow(TextInput.create("email", "Email", TextInputStyle.SHORT)
                .setRequired(true)
                .setPlaceholder("Ex: j.appleseed@example.com")
                .build()
        );

        return emailModal.build();
    }
}