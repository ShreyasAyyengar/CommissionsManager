package dev.shreyasayyengar.bot.utils;

import dev.shreyasayyengar.bot.DiscordBot;
import dev.shreyasayyengar.bot.customer.Customer;
import dev.shreyasayyengar.bot.paypal.Invoice;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.awt.*;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * The ThreadHandler is crucial to the programs' shutdown functionality. It is used to
 * shut down the bot and save all data to the database. Without the ThreadHandler,
 * the bot will not be able to save data to the database, and will be reset to its
 * default state upon restart. Simple serialisation is called via serialise() methods
 * from {@link Customer} & {@link Invoice} classes. The ThreadHandler is also responsible for
 * printing stacktraces and error messages to a designated {@link net.dv8tion.jda.api.entities.channel.concrete.TextChannel}.
 * <p></p>
 *
 * @author Shreyas Ayyengar
 */
public class ThreadHandler extends Thread {

    @Override
    public void run() {
        DiscordBot.get().shutdown();
    }

    @Override
    public UncaughtExceptionHandler getUncaughtExceptionHandler() {
        return (thread, exception) -> {

            StringWriter sw = new StringWriter();
            exception.printStackTrace(new PrintWriter(sw));
            String stacktrace = sw.toString();

            MessageEmbed build = new EmbedBuilder()
                    .setTitle("Error: `" + exception.getClass().getSimpleName() + "` ->")
                    .setDescription("`" + exception.getMessage() + "`")
                    .addField("**Full Stacktrace**:", "\n```v\n" + stacktrace + "```\nhttps://github.com/ShreyasAyyengar/CommissionsManager/tree/master/src/main/java/dev/shreyasayyengar/bot", true)
                    .setColor(Color.RED)
                    .setFooter("--CommissionsManager--")
                    .build();

            if (DiscordBot.get().bot().getTextChannelById("997328980086632448") != null) {
                DiscordBot.get().bot().getTextChannelById("997328980086632448").sendMessageEmbeds(build).queue();
            }

            exception.printStackTrace();
        };
    }
}