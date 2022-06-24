package dev.shreyasayyengar.bot.listeners.interactions;

import dev.shreyasayyengar.bot.DiscordBot;
import dev.shreyasayyengar.bot.client.ClientCommission;
import dev.shreyasayyengar.bot.client.ClientInfo;
import dev.shreyasayyengar.bot.client.conversation.ClientEmailConversation;
import dev.shreyasayyengar.bot.client.conversation.InvoiceAddFileConversation;
import dev.shreyasayyengar.bot.client.conversation.QuoteChangeConversation;
import dev.shreyasayyengar.bot.misc.utils.EmbedUtil;
import dev.shreyasayyengar.bot.paypal.Invoice;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Modal;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

public class ButtonClick extends ListenerAdapter {

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {

        String buttonID = event.getButton().getId().toLowerCase();
        List<ActionRow> disabledButtons = event.getMessage().getActionRows().stream().map(ActionRow::asDisabled).toList();

        if (buttonID.equalsIgnoreCase("purge-channel")) {
            Category parentCategory = event.getTextChannel().getParentCategory();
            parentCategory.getChannels().forEach(channel -> channel.delete().queue());
            parentCategory.delete().queue();
        }

        // ------------------------- Initial /commission buttons -------------------- //
        if (buttonID.startsWith("commission-info.")) {
            String commissionID = buttonID.replace("commission-info.", "");
            ClientCommission commission = DiscordBot.get().getClientManger().getByTextChannel(event.getTextChannel()).getCommission(commissionID);
            String pluginName = commission.getPluginName();

            List<Button> commissionInfoButtons = List.of(
                    Button.primary("commission." + pluginName + ".confirm", "Gain Confirmation")
                            .withEmoji(Emoji.fromMarkdown("☑️")),
                    Button.secondary("commission." + pluginName + ".source-code", "Toggle Source Code")
                            .withEmoji(Emoji.fromUnicode("\uD83D\uDCDD")),
                    Button.secondary("commission." + pluginName + ".change-quote", "Set Price")
                            .withEmoji(Emoji.fromUnicode("\uD83E\uDE99")),
                    Button.secondary("commission." + pluginName + ".info", "Information")
                            .withEmoji(Emoji.fromUnicode("\uD83D\uDCC4"))

                    // TODO add progress feature!
            );

            List<Button> finalCommissionButtons = List.of(
                    Button.success("commission." + pluginName + ".complete", "Complete")
                            .withEmoji(Emoji.fromMarkdown("✅")),
                    Button.danger("commission." + pluginName + ".cancel", "Cancel")
                            .withEmoji(Emoji.fromMarkdown("⛔"))
            );

            event.getInteraction().editMessageEmbeds(EmbedUtil.commissionInformation(pluginName)).setActionRows(ActionRow.of(finalCommissionButtons), ActionRow.of(commissionInfoButtons)).queue();
        }

        if (buttonID.startsWith("invoice-info.")) {
            String commissionID = buttonID.replace("invoice-info.", "");
            ClientCommission commission = DiscordBot.get().getClientManger().getByTextChannel(event.getTextChannel()).getCommission(commissionID);
            String pluginName = commission.getPluginName();

            List<Button> invoiceInfoButtons = List.of(
                    Button.primary("invoice." + pluginName + ".generate", "Generate Invoice").withEmoji(Emoji.fromUnicode("\uD83D\uDCB3")),
                    Button.secondary("invoice." + pluginName + ".view-invoices", "View Outstanding Invoices").withEmoji(Emoji.fromUnicode("\uD83E\uDDFE"))
            );

            event.getInteraction().editMessageEmbeds(EmbedUtil.invoiceInformation(pluginName)).setActionRows(ActionRow.of(invoiceInfoButtons)).queue();
        }

        // ------------------------- Commission Info Buttons ------------------------ //
        if (buttonID.startsWith("commission.")) {
            String pluginName = buttonID.split("\\.")[1];
            String action = buttonID.split("\\.")[2];

            ClientInfo clientInfo = DiscordBot.get().getClientManger().getByTextChannel(event.getTextChannel());
            ClientCommission commission = clientInfo.getCommission(pluginName);

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

                    clientInfo.getTextChannel().sendMessage(clientInfo.getHolder().getAsMention()).queue(message -> message.delete().queue());

                    event.replyEmbeds(EmbedUtil.confirmCommission(commission))
                            .addActionRow(
                                    Button.success("commission." + pluginName + ".accept", "Accept Quote").withEmoji(Emoji.fromMarkdown("✅")),
                                    Button.danger("commission." + pluginName + ".reject", "Reject Quote").withEmoji(Emoji.fromMarkdown("⛔")))
                            .queue();
                }

                case "source-code" -> {
                    commission.setRequestedSourceCode(!commission.hasRequestedSourceCode());
                    event.replyEmbeds(EmbedUtil.sourceCodeUpdate(commission)).queue();
                }

                case "change-quote" -> new QuoteChangeConversation(commission, event);

                case "info" -> {
                    Message infoMessage = clientInfo.getTextChannel().retrieveMessageById(commission.getInfoEmbed()).complete();
                    event.replyEmbeds(infoMessage.getEmbeds().get(0)).setEphemeral(true).queue();
                }

                case "complete" -> {
                    event.replyEmbeds(EmbedUtil.commissionComplete(commission))
                            .addActionRow(
                                    Button.success("vouch", "Write a Vouch!").withEmoji(Emoji.fromMarkdown("✍️")),
                                    Button.link("https://tinyurl.com/mpmk7fy2", "Vouch on SpigotMC").withEmoji(Emoji.fromMarkdown("<:spigot:933250194877849640>"))
                            ).queue();
                    event.getChannel().sendMessage(clientInfo.getHolder().getAsMention()).queue(message -> message.delete().queue());
                    clientInfo.closeCommission(commission);
                }

                case "cancel" -> {
                    clientInfo.closeCommission(commission);
                    clientInfo.getTextChannel().retrieveMessageById(commission.getInfoEmbed()).complete().delete().queue();
                    event.replyEmbeds(EmbedUtil.cancelCommission(commission)).queue();
                }

                // -------------------- Quote Confirmation Buttons (Accept/Reject) -------------------- //
                case "accept" -> {
                    event.editComponents(disabledButtons).queue();
                    event.getHook().sendMessageEmbeds(EmbedUtil.acceptedQuote()).queue();
                    commission.setConfirmed(true);
                }

                case "reject" -> {
                    event.editComponents(disabledButtons).queue();
                    event.getHook().sendMessageEmbeds(EmbedUtil.rejectedQuote()).queue();
                }
            }
        }

        // ------------------------- Invoice Action Buttons ------------------------- //
        if (buttonID.startsWith("invoice.")) {
            String pluginName = buttonID.split("\\.")[1];
            String action = buttonID.split("\\.")[2];

            ClientInfo clientInfo = DiscordBot.get().getClientManger().getByTextChannel(event.getTextChannel());
            ClientCommission commission = clientInfo.getCommission(pluginName);

            switch (action) {
                case "generate" -> {

                    if (commission.getClient().getPaypalEmail() == null) {
                        event.replyEmbeds(EmbedUtil.paypalEmailNotSet()).setEphemeral(true).queue();
                        new ClientEmailConversation(clientInfo);
                        return;
                    }

                    List<Button> checkButtons = List.of(
                            Button.success("invoice-management." + pluginName + ".yes", "Yes").withEmoji(Emoji.fromMarkdown("✅")),
                            Button.danger("invoice-management." + pluginName + ".no", "No").withEmoji(Emoji.fromMarkdown("⛔"))
                    );

                    event.getInteraction().editMessageEmbeds(EmbedUtil.askPurpose(commission)).setActionRows(ActionRow.of(checkButtons)).queue();
                    return;
                }

                case "view-invoices" -> {
                    if (commission.getInvoices().size() == 0) {
                        event.replyEmbeds(EmbedUtil.noInvoices()).setEphemeral(true).queue();
                        return;
                    }

                    SelectMenu.Builder menuBuilder = SelectMenu.create("menu:invoices");

                    menuBuilder.setPlaceholder("Select an outstanding Invoice");
                    menuBuilder.setRequiredRange(1, 1);

                    for (Invoice invoice : commission.getInvoices()) {
                        menuBuilder.addOption(invoice.getID(), "invoice." + invoice.getID());
                    }

                    event.getInteraction().editMessageEmbeds(EmbedUtil.invoiceInformation(pluginName)).setActionRow(menuBuilder.build()).queue();
                }
            }
        }

        // ---------------------- Invoice Management Buttons ------------------------ //
        if (buttonID.startsWith("invoice-management.")) {
            String value = buttonID.split("\\.")[1]; // This could be the plugin name or the invoice ID
            String action = buttonID.split("\\.")[2];

            ClientInfo clientInfo = DiscordBot.get().getClientManger().getByTextChannel(event.getTextChannel());

            switch (action) {
                case "yes" -> {
                    ClientCommission commission = clientInfo.getCommission(value);

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
                    } catch (IOException | URISyntaxException e) {
                        throw new RuntimeException(e);
                    }
                }

                case "no" -> {
                    ClientCommission commission = clientInfo.getCommission(value);

                    Modal.Builder builder = Modal.create("sub-invoice." + commission.getPluginName(), "Create a Sub-Invoice");
                    builder.addActionRow(TextInput.create("description", "Sub-Invoice Description", TextInputStyle.SHORT).setRequired(true).build());
                    builder.addActionRow(TextInput.create("amount", "Sub-Invoice Price", TextInputStyle.SHORT).setRequired(true).build());


                    event.replyModal(builder.build()).queue();
                }

                case "file-holding" -> {
                    clientInfo.getCommissions().stream().flatMap(commission -> commission.getInvoices().stream()).filter(inv -> inv.getID().equalsIgnoreCase(value)).findFirst().ifPresent(InvoiceAddFileConversation::new);
                    event.getInteraction().replyEmbeds(EmbedUtil.checkDMForMore()).setEphemeral(true).queue();
                }

                case "view-info" ->
                        clientInfo.getCommissions().stream().flatMap(commission -> commission.getInvoices().stream()).filter(inv -> inv.getID().equalsIgnoreCase(value)).findFirst().ifPresent(invoice -> {
                            event.getTextChannel().retrieveMessageById(invoice.getMessageID()).queue(message -> {
                                event.replyEmbeds(message.getEmbeds().get(0)).setEphemeral(true).queue();
                            });
                        });

                case "nudge" ->
                        clientInfo.getCommissions().stream().flatMap(commission -> commission.getInvoices().stream()).filter(inv -> inv.getID().equalsIgnoreCase(value)).findFirst().ifPresent(Invoice::nudgePayment);

                case "cancel" ->
                        clientInfo.getCommissions().stream().flatMap(commission -> commission.getInvoices().stream()).filter(inv -> inv.getID().equalsIgnoreCase(value)).findFirst().ifPresent(Invoice::cancel);

            }
        }

        // -------------------- Vouch Button-------------------- //
        if (buttonID.equalsIgnoreCase("vouch")) {

            TextInput vouchBox = TextInput.create("text-box", "Please write your vouch here!", TextInputStyle.PARAGRAPH).setRequired(true).build();
            TextInput spigotBox = TextInput.create("spigotmc", "Have a SpigotMC account? Tag it here", TextInputStyle.SHORT).setRequired(false).build();

            Modal vouchModal = Modal.create("vouch", "Write your vouch!")
                    .addActionRows(ActionRow.of(vouchBox), ActionRow.of(spigotBox))
                    .build();
            event.replyModal(vouchModal).queue();
        }
    }
}
