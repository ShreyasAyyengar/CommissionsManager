package dev.shreyasayyengar.bot.commands;

import dev.shreyasayyengar.bot.functional.type.DiscordModal;
import dev.shreyasayyengar.bot.utils.EmbedUtil;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MiscellaneousSlashCommandManager extends ListenerAdapter {
    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (event.getName().equalsIgnoreCase("feedback")) {

            DiscordModal feedbackModal = new DiscordModal("Feedback Form")
                    .addTextInput(TextInput.create("feedback", "Please write any feedback here!", TextInputStyle.PARAGRAPH).build())
                    .onSubmit((modalUser, modalEvent) -> {
                        String feedback = modalEvent.getValue("feedback").getAsString();
                        modalEvent.replyEmbeds(EmbedUtil.feedbackSubmitted()).setEphemeral(true).queue();
                        modalEvent.getGuild().getTextChannelById("982112203794685963").sendMessageEmbeds(EmbedUtil.feedback(feedback, modalUser)).queue();
                    });

            event.replyModal(feedbackModal.asModal()).queue();
        }

        if (event.getName().equalsIgnoreCase("clear")) {
            event.getChannel().getHistory().retrievePast(event.getOption("amount").getAsInt()).queue(messages -> {
                event.getChannel().asTextChannel().deleteMessages(messages).queue();
                event.reply("Cleared " + messages.size() + " messages!").setEphemeral(true).queue();
            });
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