package dev.shreyasayyengar.bot.utils;

import dev.shreyasayyengar.bot.DiscordBot;
import dev.shreyasayyengar.bot.customer.Customer;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.apache.commons.collections4.CollectionUtils;

import java.awt.*;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Stream;

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

    public static long convertDateToEpoch(String timestamp) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
        Instant instant = Instant.from(formatter.parse(timestamp));

        return instant.getEpochSecond();
    }

    public static Collection<Permission> getAllowedPermissions() {
        return Stream.of(
                Permission.VIEW_CHANNEL,
                Permission.MESSAGE_SEND,
                Permission.MESSAGE_ADD_REACTION,
                Permission.MESSAGE_EMBED_LINKS,
                Permission.MESSAGE_ATTACH_FILES,
                Permission.VOICE_CONNECT,
                Permission.VOICE_SPEAK,
                Permission.VOICE_USE_VAD
        ).toList();
    }

    public static Collection<Permission> getDeniedPermissions() {
        return CollectionUtils.subtract(Util.getAllowedPermissions(), Arrays.stream(Permission.values()).toList());
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