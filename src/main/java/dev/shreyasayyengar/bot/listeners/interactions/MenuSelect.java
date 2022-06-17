package dev.shreyasayyengar.bot.listeners.interactions;

import dev.shreyasayyengar.bot.DiscordBot;
import dev.shreyasayyengar.bot.client.ClientInfo;
import dev.shreyasayyengar.bot.misc.utils.EmbedUtil;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Stream;

public class MenuSelect extends ListenerAdapter {

    @Override
    public void onSelectMenuInteraction(@NotNull SelectMenuInteractionEvent event) {
        String pluginName = event.getValues().get(0);

        ClientInfo clientInfo = DiscordBot.get().getClientManger().getByTextChannel(event.getTextChannel());

        clientInfo.getCommissions().stream().filter(commission -> commission.getPluginName().equals(pluginName)).findFirst().ifPresent(commission -> {

            List<Button> buttons = Stream.of(
                    Button.success(pluginName + ".invoice", "Generate Invoice")
                            .withEmoji(Emoji.fromMarkdown("\uD83D\uDCB3")),
                    Button.primary(pluginName + ".confirm", "Gain Confirmation")
                            .withEmoji(Emoji.fromMarkdown("☑️")),
                    Button.secondary(pluginName + ".source-code", "Toggle Source Code")
                            .withEmoji(Emoji.fromUnicode("\uD83D\uDCDD")),
                    Button.secondary(pluginName + ".changequote", "Set Price")
                            .withEmoji(Emoji.fromUnicode("\uD83E\uDE99")),
                    Button.secondary(pluginName + ".info", "Info")
                            .withEmoji(Emoji.fromUnicode("\uD83D\uDCC4"))
                    ).toList();

            List<Button> completeButton = Stream.of(
                    Button.success(pluginName + ".complete", "Complete")
                            .withEmoji(Emoji.fromMarkdown("✅")),
                    Button.danger(pluginName + ".cancel", "Cancel")
                            .withEmoji(Emoji.fromMarkdown("⛔"))
            ).toList();

            event.replyEmbeds(EmbedUtil.commissionInformation(commission.getPluginName())).addActionRow(buttons).addActionRow(completeButton).setEphemeral(true).queue();
        });
    }
}
