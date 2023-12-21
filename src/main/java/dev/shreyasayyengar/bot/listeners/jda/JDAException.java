package dev.shreyasayyengar.bot.listeners.jda;

import dev.shreyasayyengar.bot.DiscordBot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.ExceptionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.PrintWriter;
import java.io.StringWriter;

public class JDAException extends ListenerAdapter {

    @Override
    public void onException(@NotNull ExceptionEvent event) {
        StringWriter sw = new StringWriter();
        event.getCause().printStackTrace(new PrintWriter(sw));
        String stacktrace = sw.toString();

        MessageEmbed build = new EmbedBuilder()
                .setTitle("Error: `" + event.getCause().getClass().getSimpleName() + "` ->")
                .setDescription("`" + event.getCause().getMessage() + "`")
                .addField("**Full Stacktrace**:", "\n```v\n" + stacktrace + "```\nhttps://github.com/ShreyasAyyengar/CommissionsManager/tree/master/src/main/java/dev/shreyasayyengar/bot", true)
                .setColor(Color.RED)
                .setFooter("--CommissionsManager--")
                .build();

        if (DiscordBot.get().bot().getTextChannelById("997328980086632448") != null) {
            DiscordBot.get().bot().getTextChannelById("997328980086632448").sendMessageEmbeds(build).queue();
        }
    }
}
