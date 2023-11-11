package dev.shreyasayyengar.bot.listeners.interactions;

import dev.shreyasayyengar.bot.DiscordBot;
import dev.shreyasayyengar.bot.customer.CustomerCommission;
import dev.shreyasayyengar.bot.customer.Customer;
import dev.shreyasayyengar.bot.misc.utils.EmbedUtil;
import dev.shreyasayyengar.bot.paypal.Invoice;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.util.List;

public class MenuSelect extends ListenerAdapter {

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {

        if (event.getComponentId().equalsIgnoreCase("menu:commissions")) {

            String pluginName = event.getValues().get(0).replace("commission.", "");
            Customer customer = DiscordBot.get().getCustomerManger().getByTextChannel(event.getChannel().asTextChannel());

            CustomerCommission commission = customer.getCommission(pluginName);

            List<Button> buttons = List.of(
                    Button.primary("commission-info." + pluginName, "Commission Information").withEmoji(Emoji.fromUnicode("\uD83D\uDCDD")),
                    Button.secondary("invoice-info." + pluginName, "Invoice Management").withEmoji(Emoji.fromUnicode("\uD83D\uDCB2"))
            );

            event.replyEmbeds(EmbedUtil.commissionInformation(commission.getPluginName())).addActionRow(buttons).setEphemeral(true).queue();
        }

        if (event.getComponentId().equalsIgnoreCase("menu:invoices")) {

            String invoiceID = event.getValues().get(0).replace("invoice.", "");
            Customer customer = DiscordBot.get().getCustomerManger().getByTextChannel(event.getChannel().asTextChannel());


            Invoice invoice = customer.getInvoice(invoiceID);

            List<Button> invoiceButtons = List.of(
                    Button.secondary("invoice-management." + invoice.getID() + ".nudge", "Nudge Payment").withEmoji(Emoji.fromUnicode("U+1F64B")),
                    Button.secondary("invoice-management." + invoice.getID() + ".view-info", "View Invoice Information").withEmoji(Emoji.fromUnicode("U+2139")),
                    Button.secondary("invoice-management." + invoice.getID() + ".file-holding", "Add files to Holding").withEmoji(Emoji.fromUnicode("U+1F4C2")),
                    Button.danger("invoice-management." + invoice.getID() + ".cancel", "Cancel Invoice").withEmoji(Emoji.fromUnicode("U+26D4"))
            );

            event.getInteraction().editMessageEmbeds(EmbedUtil.invoiceInformation(invoiceID)).setActionRow(invoiceButtons).queue();
        }
    }
}