package dev.shreyasayyengar.bot.listeners.interactions;

import dev.shreyasayyengar.bot.DiscordBot;
import dev.shreyasayyengar.bot.customer.Customer;
import dev.shreyasayyengar.bot.customer.CustomerCommission;
import dev.shreyasayyengar.bot.customer.conversation.QuoteChangeConversation;
import dev.shreyasayyengar.bot.listeners.interactions.button.JDAButton;
import dev.shreyasayyengar.bot.misc.utils.EmbedUtil;
import dev.shreyasayyengar.bot.paypal.Invoice;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MenuSelect extends ListenerAdapter {
    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {

        if (event.getComponentId().equalsIgnoreCase("menu:commissions")) {

            String pluginName = event.getValues().get(0).replace("commission.", "");
            Customer customer = DiscordBot.get().getCustomerManger().getByTextChannel(event.getChannel().asTextChannel());
            CustomerCommission commission = customer.getCommission(pluginName);

            List<JDAButton> initialButtons = new ArrayList<>();

            initialButtons.add(
                    JDAButton.of(ButtonStyle.PRIMARY, "Commission Information", "ℹ", (user, infoButtonEvent) -> {

                        List<Button> finaliseButtons = Stream.of(
                                JDAButton.of(ButtonStyle.SUCCESS, "Complete", "✅", (user1, completeButtonEvent) -> {
                                    customer.closeCommission(commission);
                                    completeButtonEvent.getChannel().sendMessage(customer.getHolder().getAsMention()).queue(message -> message.delete().queue());

                                    List<Button> closingButtons = List.of(
                                            JDAButton.of(ButtonStyle.SUCCESS, "Write a Vouch!", "✍", (user2, vouchButtonEvent) -> {
                                                TextInput vouchBox = TextInput.create("text-box", "Please write your vouch here!", TextInputStyle.PARAGRAPH).setRequired(true).build();
                                                TextInput spigotBox = TextInput.create("spigotmc", "Have a SpigotMC account? Tag it here", TextInputStyle.SHORT).setRequired(false).build();

                                                Modal vouchModal = Modal.create("vouch", "Write your vouch!")
                                                        .addComponents(ActionRow.of(vouchBox), ActionRow.of(spigotBox))
                                                        .build();
                                                vouchButtonEvent.replyModal(vouchModal).queue();
                                            }).asButton(),
                                            Button.link("https://tinyurl.com/mpmk7fy2", "Vouch on SpigotMC").withEmoji(Emoji.fromFormatted("<:spigot:933250194877849640>"))
                                    );

                                    completeButtonEvent.replyEmbeds(EmbedUtil.commissionComplete(commission)).addActionRow(closingButtons).queue();
                                }),
                                JDAButton.of(ButtonStyle.DANGER, "Cancel", "⛔", (user1, cancelButtonEvent) -> {
                                    customer.closeCommission(commission);

                                    if (commission.getInfoEmbed() != null) {
                                        customer.getTextChannel().retrieveMessageById(commission.getInfoEmbed()).complete().delete().queue();
                                    }

                                    cancelButtonEvent.replyEmbeds(EmbedUtil.cancelCommission(commission)).queue();
                                })
                        ).map(JDAButton::asButton).collect(Collectors.toList());

                        List<Button> commissionInfoButtons = Stream.of(
                                JDAButton.of(ButtonStyle.PRIMARY, "Description", "\uD83D\uDCDD", (user1, descriptionButtonEvent) -> {
                                    Message infoMessage = customer.getTextChannel().retrieveMessageById(commission.getInfoEmbed()).complete();
                                    descriptionButtonEvent.replyEmbeds(infoMessage.getEmbeds().get(0)).setEphemeral(true).queue();
                                }),
                                JDAButton.of(ButtonStyle.PRIMARY, "Set Quote", "\uD83D\uDCB2", (user1, quoteButtonEvent) -> {
                                    new QuoteChangeConversation(commission, quoteButtonEvent);
                                }),
                                JDAButton.of(ButtonStyle.SECONDARY, "Source Code", "<:discorddeveloper:697686848545488986>", (user1, srcButtonEvent) -> {
                                    commission.setRequestedSourceCode(!commission.hasRequestedSourceCode());
                                    srcButtonEvent.replyEmbeds(EmbedUtil.sourceCodeUpdate(commission)).queue();
                                }),
                                JDAButton.of(ButtonStyle.SECONDARY, "Confirm Work", "\uD83E\uDD1D", (user1, confirmButtonEvent) -> {
                                    if (commission.checkPrice()) {
                                        confirmButtonEvent.replyEmbeds(EmbedUtil.noPriceSet()).setEphemeral(true).queue();
                                        return;
                                    }

                                    if (commission.isConfirmed()) {
                                        confirmButtonEvent.replyEmbeds(EmbedUtil.alreadyConfirmed()).setEphemeral(true).queue();
                                        return;
                                    }

                                    customer.getTextChannel().sendMessage(customer.getHolder().getAsMention()).queue(message -> message.delete().queue());

                                    confirmButtonEvent.replyEmbeds(EmbedUtil.confirmCommission(commission))
                                            .addActionRow(
                                                    Button.success("commission." + pluginName + ".accept", "Accept Quote").withEmoji(Emoji.fromUnicode("U+2705")),
                                                    Button.danger("commission." + pluginName + ".reject", "Reject Quote").withEmoji(Emoji.fromUnicode("U+26D4"))
                                            )
                                            .queue();
                                }),
                                JDAButton.of(ButtonStyle.SECONDARY, "Back", "⬅", (user1, buttonInteractionEvent) -> {
                                    buttonInteractionEvent.editComponents(ActionRow.of(initialButtons.stream().map(JDAButton::asButton).collect(Collectors.toList()))).queue();
                                })
                        ).map(JDAButton::asButton).collect(Collectors.toList());

                        infoButtonEvent.getInteraction().editComponents(ActionRow.of(finaliseButtons), ActionRow.of(commissionInfoButtons)).queue();
                    })
            );

            initialButtons.add(
                    JDAButton.of(ButtonStyle.SECONDARY, "Invoice Management", "\uD83D\uDCB2", (user, buttonInteractionEvent) -> {

                        List<Button> invoiceInfoButtons = List.of(
                                Button.primary("invoice." + pluginName + ".generate", "Generate Invoice").withEmoji(Emoji.fromUnicode("U+1F4B3")),
                                Button.secondary("invoice." + pluginName + ".view-invoices", "View Outstanding Invoices").withEmoji(Emoji.fromUnicode("U+1F9FE"))
                        );

                        buttonInteractionEvent.getInteraction().editComponents(ActionRow.of(invoiceInfoButtons)).queue();
                    })
            );

            event.replyEmbeds(EmbedUtil.commissionInformation(commission.getPluginName())).addActionRow(initialButtons.stream().map(JDAButton::asButton).toList()).setEphemeral(true).queue();
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

// TODO: vouch buttons and Accept/Deny quote buttons should have longer expirations.