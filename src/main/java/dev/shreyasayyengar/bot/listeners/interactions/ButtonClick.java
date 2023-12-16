package dev.shreyasayyengar.bot.listeners.interactions;

import dev.shreyasayyengar.bot.DiscordBot;
import dev.shreyasayyengar.bot.customer.CustomerCommission;
import dev.shreyasayyengar.bot.customer.Customer;
import dev.shreyasayyengar.bot.customer.conversation.impl.CustomerEmailConversation;
import dev.shreyasayyengar.bot.customer.conversation.impl.InvoiceAddFileConversation;
import dev.shreyasayyengar.bot.customer.conversation.impl.QuoteChangeConversation;
import dev.shreyasayyengar.bot.misc.utils.EmbedUtil;
import dev.shreyasayyengar.bot.misc.utils.Util;
import dev.shreyasayyengar.bot.paypal.Invoice;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;

public class ButtonClick extends ListenerAdapter {
    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {

        String buttonID = event.getButton().getId().toLowerCase();

        if (buttonID.equalsIgnoreCase("purge-channel")) {
            Customer customerToDelete = Util.getCustomerByChannelId(event.getChannel().asTextChannel());

            customerToDelete.getTextChannel().delete().queue();
            customerToDelete.getVoiceChannel().delete().queue();

            DiscordBot.get().getCustomerManger().getMap().remove(customerToDelete.getHolder().getId());
        }

        // ------------------------- Initial `/commission` buttons -------------------- //
        if (buttonID.startsWith("commission-info.")) {
            String commissionID = buttonID.replace("commission-info.", "");
            CustomerCommission commission = DiscordBot.get().getCustomerManger().getByTextChannel(event.getChannel().asTextChannel()).getCommission(commissionID);
            String pluginName = commission.getPluginName();

            List<Button> commissionInfoButtons = List.of(
                    Button.primary("commission." + pluginName + ".confirm", "Gain Confirmation")
                            .withEmoji(Emoji.fromUnicode("U+2611")),
                    Button.secondary("commission." + pluginName + ".source-code", "Toggle Source Code")
                            .withEmoji(Emoji.fromUnicode("U+1F4DD")),
                    Button.secondary("commission." + pluginName + ".change-quote", "Set Price")
                            .withEmoji(Emoji.fromUnicode("U+1FA99")),
                    Button.secondary("commission." + pluginName + ".info", "Information")
                            .withEmoji(Emoji.fromUnicode("U+1F4C4"))

                    // TODO add progress feature!
            );

            List<Button> finalCommissionButtons = List.of(
                    Button.success("commission." + pluginName + ".complete", "Complete")
                            .withEmoji(Emoji.fromUnicode("U+2705")),
                    Button.danger("commission." + pluginName + ".cancel", "Cancel")
                            .withEmoji(Emoji.fromUnicode("U+26D4"))
            );

            event.getInteraction().editMessageEmbeds(EmbedUtil.commissionInformation(pluginName)).setComponents(ActionRow.of(finalCommissionButtons), ActionRow.of(commissionInfoButtons)).queue();
        }

        if (buttonID.startsWith("invoice-info.")) {
            String commissionID = buttonID.replace("invoice-info.", "");
            CustomerCommission commission = DiscordBot.get().getCustomerManger().getByTextChannel(event.getChannel().asTextChannel()).getCommission(commissionID);
            String pluginName = commission.getPluginName();

            List<Button> invoiceInfoButtons = List.of(
                    Button.primary("invoice." + pluginName + ".generate", "Generate Invoice").withEmoji(Emoji.fromUnicode("U+1F4B3")),
                    Button.secondary("invoice." + pluginName + ".view-invoices", "View Outstanding Invoices").withEmoji(Emoji.fromUnicode("U+1F9FE"))
            );

            event.getInteraction().editMessageEmbeds(EmbedUtil.invoiceInformation(pluginName)).setComponents(ActionRow.of(invoiceInfoButtons)).queue();
        }

        // ------------------------- Commission Info Buttons ------------------------ //
        if (buttonID.startsWith("commission.")) {
            String pluginName = buttonID.split("\\.")[1];
            String action = buttonID.split("\\.")[2];

            Customer customer = DiscordBot.get().getCustomerManger().getByTextChannel(event.getChannel().asTextChannel());
            CustomerCommission commission = customer.getCommission(pluginName);

            switch (action) {

                case "confirm" -> {
                    if (commission.checkPrice()) {
                        event.replyEmbeds(EmbedUtil.noPriceSet()).setEphemeral(true).queue();
                        return;
                    }

                    if (commission.isConfirmed()) {
                        event.replyEmbeds(EmbedUtil.alreadyConfirmed()).setEphemeral(true).queue();
                        return;
                    }

                    customer.getTextChannel().sendMessage(customer.getHolder().getAsMention()).queue(message -> message.delete().queue());

                    event.replyEmbeds(EmbedUtil.confirmCommission(commission))
                            .addActionRow(
                                    Button.success("commission." + pluginName + ".accept", "Accept Quote").withEmoji(Emoji.fromUnicode("U+2705")),
                                    Button.danger("commission." + pluginName + ".reject", "Reject Quote").withEmoji(Emoji.fromUnicode("U+26D4"))
                            )
                            .queue();
                }

                case "source-code" -> {
                    commission.setRequestedSourceCode(!commission.hasRequestedSourceCode());
                    event.replyEmbeds(EmbedUtil.sourceCodeUpdate(commission)).queue();
                }

                case "change-quote" -> new QuoteChangeConversation(commission, event);

                case "info" -> {
                    Message infoMessage = customer.getTextChannel().retrieveMessageById(commission.getInfoEmbed()).complete();
                    event.replyEmbeds(infoMessage.getEmbeds().get(0)).setEphemeral(true).queue();
                }

                case "complete" -> {
                    event.replyEmbeds(EmbedUtil.commissionComplete(commission))
                            .addActionRow(
                                    Button.success("vouch", "Write a Vouch!").withEmoji(Emoji.fromUnicode("U+270D")),
                                    Button.link("https://tinyurl.com/mpmk7fy2", "Vouch on SpigotMC").withEmoji(Emoji.fromFormatted("<:spigot:933250194877849640>"))
                            ).queue();
                    event.getChannel().sendMessage(customer.getHolder().getAsMention()).queue(message -> message.delete().queue());
                    customer.closeCommission(commission);
                }

                case "cancel" -> {
                    customer.closeCommission(commission);

                    if (commission.getInfoEmbed() != null) {
                        customer.getTextChannel().retrieveMessageById(commission.getInfoEmbed()).complete().delete().queue();
                    }

                    event.replyEmbeds(EmbedUtil.cancelCommission(commission)).queue();
                }

                // -------------------- Quote Confirmation Buttons (Accept/Reject) -------------------- //
                case "accept" -> {
                    event.editComponents(event.getMessage().getActionRows().stream().map(ActionRow::asDisabled).toList()).queue();
                    event.getHook().sendMessageEmbeds(EmbedUtil.acceptedQuote()).queue();
                    commission.setConfirmed(true);
                }

                case "reject" -> {
                    event.editComponents(event.getMessage().getActionRows().stream().map(ActionRow::asDisabled).toList()).queue();
                    event.getHook().sendMessageEmbeds(EmbedUtil.rejectedQuote()).queue();
                }
            }
        }

        // ------------------------- Invoice Action Buttons ------------------------- //
        if (buttonID.startsWith("invoice.")) {
            String pluginName = buttonID.split("\\.")[1];
            String action = buttonID.split("\\.")[2];

            Customer customer = DiscordBot.get().getCustomerManger().getByTextChannel(event.getChannel().asTextChannel());
            CustomerCommission commission = customer.getCommission(pluginName);

            switch (action) {
                case "generate" -> {

                    if (commission.getCustomer().getPaypalEmail() == null) {
                        event.replyEmbeds(EmbedUtil.paypalEmailNotSet()).setEphemeral(true).queue();
                        new CustomerEmailConversation(customer);
                        return;
                    }

                    List<Button> checkButtons = List.of(
                            Button.success("invoice-management." + pluginName + ".yes", "Yes").withEmoji(Emoji.fromUnicode("U+2705")),
                            Button.danger("invoice-management." + pluginName + ".no", "No").withEmoji(Emoji.fromUnicode("U+26D4"))
                    );

                    event.getInteraction().editMessageEmbeds(EmbedUtil.askPurpose(commission)).setComponents(ActionRow.of(checkButtons)).queue();
                    return;
                }

                case "view-invoices" -> {
                    if (commission.getInvoices().isEmpty()) {
                        event.replyEmbeds(EmbedUtil.noInvoices()).setEphemeral(true).queue();
                        return;
                    }

                    StringSelectMenu.Builder menuBuilder = StringSelectMenu.create("menu:invoices");

                    menuBuilder.setPlaceholder("Select an outstanding Invoice");
                    menuBuilder.setRequiredRange(1, 1);

                    for (Invoice invoice : commission.getInvoices()) {
                        menuBuilder.addOption(invoice.getID(), "invoice." + invoice.getID());
                    }

                    event.getInteraction().editMessageEmbeds(EmbedUtil.invoiceInformation(pluginName)).setActionRow(menuBuilder.build()).queue();
                }
            }
        }

        // ---------------------- Specific Invoice Management Buttons ------------------------ //
        if (buttonID.startsWith("invoice-management.")) {
            String value = buttonID.split("\\.")[1]; // This could be the commission name or the invoice ID
            String action = buttonID.split("\\.")[2];

            Customer customer = DiscordBot.get().getCustomerManger().getByTextChannel(event.getChannel().asTextChannel());

            switch (action) {
                case "yes" -> {
                    CustomerCommission commission = customer.getCommission(value);

                    if (commission.checkPrice()) {
                        event.replyEmbeds(EmbedUtil.noPriceSet()).setEphemeral(true).queue();
                        return;
                    }

                    if (!commission.isConfirmed()) {
                        event.replyEmbeds(EmbedUtil.notConfirmed()).setEphemeral(true).queue();
                        return;
                    }

                    try {
                        commission.generateInvoice(event);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }

                case "no" -> {
                    CustomerCommission commission = customer.getCommission(value);

                    Modal.Builder builder = Modal.create("sub-invoice." + commission.getPluginName(), "Create a Sub-Invoice");
                    builder.addActionRow(TextInput.create("description", "Sub-Invoice Description", TextInputStyle.SHORT).setRequired(true).build());
                    builder.addActionRow(TextInput.create("amount", "Sub-Invoice Price", TextInputStyle.SHORT).setRequired(true).build());

                    event.replyModal(builder.build()).queue();
                }

                case "file-holding" -> {
                    Invoice invoice = customer.getInvoice(value);

                    new InvoiceAddFileConversation(invoice);
                    event.getInteraction().editMessageEmbeds(EmbedUtil.checkDMForMore()).setComponents().queue();
                }

                case "view-info" -> {
                    Invoice invoice = customer.getInvoice(value);
                    event.getChannel().retrieveMessageById(invoice.getMessageID()).queue(message -> event.replyEmbeds(message.getEmbeds().get(0)).setEphemeral(true).queue());
                }

                case "nudge" -> {
                    Invoice invoice = customer.getInvoice(value);
                    invoice.nudgePayment(event);
                }

                case "cancel" -> {
                    Invoice invoice = customer.getInvoice(value);

                    event.getInteraction().replyEmbeds(EmbedUtil.nudge(invoice)).setEphemeral(true).queue();
                    invoice.cancel();
                }
            }
        }

        // -------------------- Vouch Button-------------------- //
        if (buttonID.equalsIgnoreCase("vouch")) {

            TextInput vouchBox = TextInput.create("text-box", "Please write your vouch here!", TextInputStyle.PARAGRAPH).setRequired(true).build();
            TextInput spigotBox = TextInput.create("spigotmc", "Have a SpigotMC account? Tag it here", TextInputStyle.SHORT).setRequired(false).build();

            Modal vouchModal = Modal.create("vouch", "Write your vouch!")
                    .addComponents(ActionRow.of(vouchBox), ActionRow.of(spigotBox))
                    .build();
            event.replyModal(vouchModal).queue();
        }
    }
}