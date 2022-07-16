package dev.shreyasayyengar.bot.listeners.interactions;

import dev.shreyasayyengar.bot.DiscordBot;
import dev.shreyasayyengar.bot.client.ClientCommission;
import dev.shreyasayyengar.bot.client.ClientInfo;
import dev.shreyasayyengar.bot.misc.utils.EmbedUtil;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class MenuSelect extends ListenerAdapter {

    @Override
    public void onSelectMenuInteraction(@NotNull SelectMenuInteractionEvent event) {

        if (event.getComponentId().equalsIgnoreCase("menu:commissions")) {

            String pluginName = event.getValues().get(0).replace("commission.", "");
            ClientInfo clientInfo = DiscordBot.get().getClientManger().getByTextChannel(event.getTextChannel());

            clientInfo.getCommissions().stream().filter(commission -> commission.getPluginName().equals(pluginName)).findFirst().ifPresent(commission -> {

                List<Button> buttons = List.of(
                        Button.primary("commission-info." + pluginName, "Commission Information").withEmoji(Emoji.fromUnicode("\uD83D\uDCDD")),
                        Button.secondary("invoice-info." + pluginName, "Invoice Management").withEmoji(Emoji.fromUnicode("\uD83D\uDCB2"))
                );

                event.replyEmbeds(EmbedUtil.commissionInformation(commission.getPluginName())).addActionRow(buttons).setEphemeral(true).queue();
            });
        }

        if (event.getComponentId().equalsIgnoreCase("menu:invoices")) {

            String invoiceID = event.getValues().get(0).replace("invoice.", "");
            ClientInfo clientInfo = DiscordBot.get().getClientManger().getByTextChannel(event.getTextChannel());

            for (ClientCommission commission : clientInfo.getCommissions()) {

                commission.getInvoices().stream().filter(invoice -> invoice.getID().equalsIgnoreCase(invoiceID)).findFirst().ifPresent(invoice -> {

                    List<Button> invoiceButtons = List.of(
                            Button.secondary("invoice-management." + invoice.getID() + ".nudge", "Nudge Payment").withEmoji(Emoji.fromUnicode("\uD83D\uDDE3")),
                            Button.secondary("invoice-management." + invoice.getID() + ".view-info", "View Invoice Information").withEmoji(Emoji.fromUnicode("\uD83E\uDDFE")),
                            Button.secondary("invoice-management." + invoice.getID() + ".file-holding", "Add files to Holding").withEmoji(Emoji.fromUnicode("\uD83D\uDCC1")),
                            Button.danger("invoice-management." + invoice.getID() + ".cancel", "Cancel Invoice").withEmoji(Emoji.fromUnicode("⛔"))
                    );

                    event.getInteraction().editMessageEmbeds(EmbedUtil.invoiceInformation(invoiceID)).setActionRow(invoiceButtons).queue();
                });
            }
        }
    }
}