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


        if (event.getComponentId().equalsIgnoreCase("menu:commissions")) {
            String pluginName = event.getValues().get(0).replace("commissions.", "");

            ClientInfo clientInfo = DiscordBot.get().getClientManger().getByTextChannel(event.getTextChannel());

            clientInfo.getCommissions().stream().filter(commission -> commission.getPluginName().equals(pluginName)).findFirst().ifPresent(commission -> {

                List<Button> buttons = Stream.of(
                        Button.success("commission." + pluginName + ".invoice", "Generate Invoice")
                                .withEmoji(Emoji.fromMarkdown("\uD83D\uDCB3")),
                        Button.primary("commission." + pluginName + ".confirm", "Gain Confirmation")
                                .withEmoji(Emoji.fromMarkdown("☑️")),
                        Button.secondary("commission." + pluginName + ".source-code", "Toggle Source Code")
                                .withEmoji(Emoji.fromUnicode("\uD83D\uDCDD")),
                        Button.secondary("commission." + pluginName + ".changequote", "Set Price")
                                .withEmoji(Emoji.fromUnicode("\uD83E\uDE99")),
                        Button.secondary("commission." + pluginName + ".info", "Info")
                                .withEmoji(Emoji.fromUnicode("\uD83D\uDCC4"))
                ).toList();

                List<Button> completeButton = Stream.of(
                        Button.success("commission." + pluginName + ".complete", "Complete")
                                .withEmoji(Emoji.fromMarkdown("✅")),
                        Button.danger("commission." + pluginName + ".cancel", "Cancel")
                                .withEmoji(Emoji.fromMarkdown("⛔"))
                ).toList();

                event.replyEmbeds(EmbedUtil.commissionInformation(commission.getPluginName())).addActionRow(buttons).addActionRow(completeButton).setEphemeral(true).queue();
            });
        }

        if (event.getComponentId().equalsIgnoreCase("menu:invoices")) {
            String invoiceId = event.getValues().get(0).replace("invoices.", "");

            ClientInfo clientInfo = DiscordBot.get().getClientManger().getByTextChannel(event.getTextChannel());

            clientInfo.getInvoices().stream().filter(invoice -> invoice.getInvoiceID().equalsIgnoreCase(invoiceId)).findFirst().ifPresent(invoice -> {

                List<Button> buttons = Stream.of(
                        Button.primary("invoice." + invoiceId + ".nudge", "Nudge Payment")
                                .withEmoji(Emoji.fromMarkdown("\uD83D\uDDE3️")),
                        Button.danger("invoice." + invoiceId + ".cancel", "Cancel Invoice")
                                .withEmoji(Emoji.fromMarkdown("⛔"))
                ).toList();

                event.replyEmbeds(EmbedUtil.invoiceInformation(invoice.getInvoiceID())).addActionRow(buttons).setEphemeral(true).queue();
            });
        }
    }
}
