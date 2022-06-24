package dev.shreyasayyengar.bot.commands;

import dev.shreyasayyengar.bot.misc.utils.EmbedUtil;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Modal;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MiscellaneousSlashCommandManager extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {

        if (event.getName().equalsIgnoreCase("feedback")) {

            TextInput feedbackTextInput = TextInput.create("feedback", "Please write any feedback here!", TextInputStyle.PARAGRAPH).build();
            Modal feedbackModal = Modal.create("feedback", "Feedback Form!")
                    .addActionRows(ActionRow.of(feedbackTextInput))
                    .build();

            event.replyModal(feedbackModal).queue();
        }

        if (event.getName().equalsIgnoreCase("clear")) {

            List<Message> amount = event.getTextChannel().getHistory().retrievePast(event.getOption("amount").getAsInt()).complete();
            event.getTextChannel().deleteMessages(amount).queue();

            event.reply("Cleared " + amount.size() + " messages!").setEphemeral(true).queue();
        }

        if (event.getName().equalsIgnoreCase("invite")) {

            event.replyEmbeds(EmbedUtil.inviteEmbed()).queue();
        }

        if (event.getName().equalsIgnoreCase("record")) {
            event.reply("This command is currently under development!").setEphemeral(true).queue();
        }

        if (event.getName().equalsIgnoreCase("statistics")) {
            event.replyEmbeds(EmbedUtil.statistics()).setEphemeral(true).queue();
        }

        if (event.getName().equalsIgnoreCase("gc")) {
            System.gc();
            event.replyEmbeds(EmbedUtil.garbageCollected()).setEphemeral(true).queue();
        }

        if (event.getName().equalsIgnoreCase("exit")) {
            event.replyEmbeds(EmbedUtil.exiting()).setEphemeral(true).queue();

            ScheduledExecutorService server = Executors.newSingleThreadScheduledExecutor();
            server.schedule(() -> System.exit(0), 1, TimeUnit.SECONDS);
        }
    }
}
