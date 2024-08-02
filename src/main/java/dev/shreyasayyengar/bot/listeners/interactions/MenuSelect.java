package dev.shreyasayyengar.bot.listeners.interactions;

import dev.shreyasayyengar.bot.DiscordBot;
import dev.shreyasayyengar.bot.customer.Customer;
import dev.shreyasayyengar.bot.customer.CustomerCommission;
import dev.shreyasayyengar.bot.customer.conversation.InvoiceAddFileConversation;
import dev.shreyasayyengar.bot.functional.type.DiscordButton;
import dev.shreyasayyengar.bot.functional.type.DiscordMenu;
import dev.shreyasayyengar.bot.functional.type.DiscordModal;
import dev.shreyasayyengar.bot.paypal.Invoice;
import dev.shreyasayyengar.bot.paypal.InvoiceDraft;
import dev.shreyasayyengar.bot.utils.EmbedUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MenuSelect extends ListenerAdapter {
    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        if (event.getComponentId().equalsIgnoreCase("menu:commissions")) {
            String pluginName = event.getValues().get(0).replace("commission.", "");
            Customer customer = DiscordBot.get().getCustomerManger().getByTextChannel(event.getChannel().asTextChannel());
            CustomerCommission commission = customer.getCommission(pluginName);

            List<DiscordButton> projectActionSettings = new ArrayList<>();

            projectActionSettings.add(
                    new DiscordButton(ButtonStyle.PRIMARY, "Commission Management", "ℹ", (buttonUser, commissionButtonEvent) -> {

                        List<DiscordButton> commissionActionButtons = new ArrayList<>() {{
                            add(new DiscordButton(ButtonStyle.PRIMARY, "Description", "\uD83D\uDCDD", (user1, descriptionButtonEvent) -> {
                                Message infoMessage = customer.getTextChannel().retrieveMessageById(commission.getInfoEmbedId()).complete();
                                descriptionButtonEvent.replyEmbeds(infoMessage.getEmbeds().get(0)).setEphemeral(true).queue();
                            }));
                            add(new DiscordButton(ButtonStyle.PRIMARY, "Set Quote", "\uD83D\uDCB2", (user1, quoteButtonEvent) -> {
                                DiscordModal quoteModal = new DiscordModal("Set Commission Quote: Current Price: $" + commission.getPrice())
                                        .addTextInput(TextInput.create("amount", "Amount", TextInputStyle.SHORT).setRequired(true).setPlaceholder("$...").build())
                                        .onSubmit((modalUser, amountSubmitEvent) -> {
                                            commission.setPrice(Double.parseDouble(amountSubmitEvent.getValue("amount").getAsString()));
                                            commission.setConfirmed(false);

                                            List<DiscordButton> confirmDenyButtons = new ArrayList<>() {{
                                                add(new DiscordButton(ButtonStyle.SUCCESS, "Accept", "✅", (user1, confirmButtonEvent) -> {
                                                    commission.setConfirmed(true);

                                                    MessageEmbed acceptedEmbed = new EmbedBuilder()
                                                            .setTitle("Commission Price Confirmed: " + commission.getPluginName())
                                                            .setDescription("This price for this commission, set for `$" + String.format("%.2f", commission.getFinalPrice()) + "` (**Inclusive** of Tax and SRC if requested) has been confirmed.")
                                                            .addField(":warning: This may not be the final price! :warning:", "As more work is completed and process continues, the price **may or may not** increase or decrease. " +
                                                                    "If this happens to be the case **you will see a message just like this one** alerting you of a price change. ", false)
                                                            .setFooter("Should there ever be a change in the price, your confirmation again will be required before generating any related invoices.", DiscordBot.get().workingGuild.getOwner().getEffectiveAvatarUrl())
                                                            .setColor(Color.GREEN)
                                                            .build();
                                                    confirmButtonEvent.deferEdit().queue();

                                                    confirmButtonEvent.getHook().editOriginalEmbeds(acceptedEmbed).setReplace(true).queue();
                                                    confirmButtonEvent.getHook().sendMessageEmbeds(EmbedUtil.acceptedQuote()).queue();
                                                }));
                                                add(new DiscordButton(ButtonStyle.DANGER, "Deny", "⛔", (user1, denyButtonEvent) -> {
                                                    commission.setConfirmed(false);

                                                    MessageEmbed deniedEmbed = new EmbedBuilder()
                                                            .setTitle("Commission Price Denied: " + commission.getPluginName())
                                                            .setDescription("This price for this commission, set for `$" + String.format("%.2f", commission.getFinalPrice()) + "` (**Inclusive** of Tax and SRC if requested) has been denied.")
                                                            .setFooter("Since you didn't agree to this quote, describe what you would like changed to meet you budget!")
                                                            .setColor(Color.RED)
                                                            .build();
                                                    denyButtonEvent.editMessageEmbeds(deniedEmbed).setReplace(true).queue();
                                                }));
                                            }};

                                            // TODO reply to modal
                                            amountSubmitEvent.reply("@here")
                                                    .setEmbeds(EmbedUtil.confirmCommission(commission))
                                                    .setActionRow(confirmDenyButtons.stream().map(DiscordButton::asButton).toList())
                                                    .queue();
                                        });

                                quoteButtonEvent.replyModal(quoteModal.asModal()).queue();
                            }));
                            add(new DiscordButton(ButtonStyle.SECONDARY, "Source Code", "<:discorddeveloper:697686848545488986>", (user1, srcButtonEvent) -> {
                                commission.setRequestedSourceCode(!commission.hasRequestedSourceCode());
                                srcButtonEvent.replyEmbeds(EmbedUtil.sourceCodeUpdate(commission)).queue();
                            }));
                            add(new DiscordButton(ButtonStyle.SECONDARY, "Back", "⬅", (user1, buttonInteractionEvent) -> {
                                buttonInteractionEvent.editComponents(ActionRow.of(projectActionSettings.stream().map(DiscordButton::asButton).toList())).queue();
                            }));
                        }};

                        List<DiscordButton> finaliseCommissionButtons = new ArrayList<>() {{
                            add(new DiscordButton(ButtonStyle.SUCCESS, "Complete", "✅", (user1, completeButtonEvent) -> {
                                commission.close(true);
                                List<DiscordButton> closingButtons = new ArrayList<>() {{
                                    add(new DiscordButton(ButtonStyle.SUCCESS, "Write a Vouch!", "U+270D", (user1, vouchButtonEvent) -> {
                                        DiscordModal inputVouchModal = new DiscordModal("Write your vouch!")
                                                .addTextInput(TextInput.create("text-box", "Please write your vouch here!", TextInputStyle.PARAGRAPH).setRequired(true).build())
                                                .addTextInput(TextInput.create("spigotmc", "Have a SpigotMC account? Tag it here", TextInputStyle.SHORT).setRequired(false).build())
                                                .onSubmit((modalUser, modalEvent) -> {
                                                    String vouch = modalEvent.getValue("text-box").getAsString();
                                                    MessageEmbed vouchEmbed;

                                                    if (!modalEvent.getValue("spigotmc").getAsString().isEmpty()) {
                                                        vouchEmbed = EmbedUtil.vouch(vouch, modalUser, modalEvent.getValue("spigotmc").getAsString());
                                                    } else vouchEmbed = EmbedUtil.vouch(vouch, modalUser);

                                                    modalEvent.deferEdit().queue();
                                                    vouchButtonEvent.getHook().editOriginalComponents().setEmbeds(EmbedUtil.vouchSuccess()).queue();
                                                    DiscordBot.get().workingGuild.getTextChannelById("980373571807367208").sendMessageEmbeds(vouchEmbed).queue();
                                                });
                                        vouchButtonEvent.replyModal(inputVouchModal.asModal()).queue();
                                    }));
                                    add(new DiscordButton("Vouch on SpigotMC", "https://tinyurl.com/mpmk7fy2"));
                                }};

                                completeButtonEvent.replyEmbeds(EmbedUtil.commissionCompleted(commission)).addContent(customer.getHolder().getAsMention()).addActionRow(closingButtons.stream().map(DiscordButton::asButton).toList()).queue();
                            }));
                            add(new DiscordButton(ButtonStyle.DANGER, "Cancel", "⛔", (user1, cancelButtonEvent) -> {
                                commission.close(false);

                                cancelButtonEvent.replyEmbeds(EmbedUtil.commissionCancelled(commission)).queue();
                            }));
                        }};

                        commissionButtonEvent.getInteraction()
                                .editComponents(
                                        ActionRow.of(commissionActionButtons.stream().map(DiscordButton::asButton).toList()),
                                        ActionRow.of(finaliseCommissionButtons.stream().map(DiscordButton::asButton).toList())
                                )
                                .setEmbeds(EmbedUtil.commissionInformation(commission.getPluginName()))
                                .queue();
                    })
            );

            projectActionSettings.add(
                    new DiscordButton(ButtonStyle.SECONDARY, "Invoice Management", "\uD83D\uDCB2", (user, invoiceButtonEvent) -> {
                        List<DiscordButton> invoiceActionButtons = new ArrayList<>() {{
                            add(new DiscordButton(ButtonStyle.PRIMARY, "Generate Invoice", "U+1F4B3", (buttonUser, generateButtonEvent) -> {
                                if (commission.getCustomer().getPaypalEmail() == null) {
                                    DiscordButton setEmailButton = new DiscordButton(ButtonStyle.PRIMARY, "Set PayPal Email", "\uD83D\uDCE7", (user, setEmailButtonEvent) -> {

                                        DiscordModal emailModal = new DiscordModal("Submit your email")
                                                .addTextInput(TextInput.create("email", "Email", TextInputStyle.SHORT)
                                                        .setRequired(true)
                                                        .setPlaceholder("Ex: john.appleseed@example.com")
                                                        .build()
                                                )
                                                .onSubmit((modalUser, emailSubmitEvent) -> {
                                                    String email = emailSubmitEvent.getValue("email").getAsString();

                                                    if (!email.matches("([A-Za-z\\d-_.]+@[A-Za-z\\d-_]+(?:\\.[A-Za-z\\d]+)+)")) {
                                                        emailSubmitEvent.replyEmbeds(EmbedUtil.invalidEmail()).setEphemeral(true).queue();
                                                        return;
                                                    }

                                                    commission.getCustomer().setPaypalEmail(email);

                                                    emailSubmitEvent.deferEdit().queue();
                                                    setEmailButtonEvent.getHook().editOriginalComponents().setEmbeds(EmbedUtil.emailSet(customer.getPaypalEmail())).queue();
                                                });

                                        setEmailButtonEvent.replyModal(emailModal.asModal()).queue();
                                    });

                                    generateButtonEvent.deferEdit().queue();
                                    generateButtonEvent.getHook().editOriginalEmbeds(EmbedUtil.paypalEmailNotSet()).setComponents().queue();
                                    generateButtonEvent.getHook().sendMessageEmbeds(EmbedUtil.promptEmail()).addContent(customer.getHolder().getAsMention()).setActionRow(setEmailButton.asButton()).queue();
                                    return;
                                }

                                List<DiscordButton> isForPrimaryInvoiceButtons = new ArrayList<>() {{
                                    add(new DiscordButton(ButtonStyle.PRIMARY, "Primary Invoice", "U+2705", (buttonUser1, yesButtonEvent) -> {
                                        if (commission.checkPrice()) {
                                            yesButtonEvent.replyEmbeds(EmbedUtil.noPriceSet()).setEphemeral(true).queue();
                                            return;
                                        }

                                        if (!commission.isConfirmed()) {
                                            yesButtonEvent.replyEmbeds(EmbedUtil.notConfirmed()).setEphemeral(true).queue();
                                            return;
                                        }

                                        try {
                                            commission.generateInvoice(yesButtonEvent);
                                        } catch (IOException e) {
                                            throw new RuntimeException(e);
                                        }
                                    }));
                                    add(new DiscordButton(ButtonStyle.SECONDARY, "Secondary Invoice", "U+26D4", (buttonUser1, noButtonEvent) -> {

                                        DiscordModal subInvoiceModal = new DiscordModal("Create a Sub-Invoice")
                                                .addTextInput(TextInput.create("description", "Sub-Invoice Description", TextInputStyle.SHORT).setRequired(true).build())
                                                .addTextInput(TextInput.create("amount", "Sub-Invoice Amount", TextInputStyle.SHORT).setRequired(true).build())
                                                .onSubmit((modalUser, subInvoiceEvent) -> {
                                                    subInvoiceEvent.deferEdit().queue();

                                                    String description = subInvoiceEvent.getValue("description").getAsString();
                                                    int amount = Integer.parseInt(subInvoiceEvent.getValue("amount").getAsString());

                                                    try {
                                                        noButtonEvent.getHook().editOriginalEmbeds(EmbedUtil.invoiceInProgress()).setActionRow().queue();
                                                        new InvoiceDraft(commission, commission.getPluginName() + "-" + description, amount, noButtonEvent.getHook()).generateInvoice();
                                                    } catch (Exception e) {
                                                        throw new RuntimeException(e);
                                                    }
                                                });

                                        noButtonEvent.replyModal(subInvoiceModal.asModal()).queue();
                                    }));
                                }};

                                generateButtonEvent.editMessageEmbeds(EmbedUtil.isInvoicePrimary(commission)).setActionRow(isForPrimaryInvoiceButtons.stream().map(DiscordButton::asButton).toList()).queue();
                            }));
                            add(new DiscordButton(ButtonStyle.SECONDARY, "View Outstanding Invoices", "U+1F9FE", (buttonUser, viewButtonEvent) -> {
                                if (commission.getInvoices().isEmpty()) {
                                    viewButtonEvent.editMessageEmbeds(EmbedUtil.noOutstandingInvoices()).setReplace(true).queue();
                                    return;
                                }

                                DiscordMenu outstandingInvoicesMenu = new DiscordMenu()
                                        .setPlaceholder("Select an Outstanding Invoice")
                                        .setRequiredRange(1, 1);

                                for (Invoice invoice : commission.getInvoices()) {
                                    outstandingInvoicesMenu.addOptions(SelectOption.of(invoice.getID() + " (" + invoice.getTotal() + ")", invoice.getID()));
                                }

                                outstandingInvoicesMenu.onSelect((menuUser, menuEvent) -> {
                                    String invoiceID = menuEvent.getValues().get(0).replace("invoice.", "");
                                    Invoice invoice = customer.getInvoiceByID(invoiceID);

                                    List<DiscordButton> invoiceActionButtons = new ArrayList<>() {{
                                        add(new DiscordButton(ButtonStyle.SECONDARY, "Nudge Payment", "U+1F64B", (buttonUser1, nudgeButtonEvent) -> {
                                            invoice.nudgePayment(nudgeButtonEvent);
                                        }));
                                        add(new DiscordButton(ButtonStyle.SECONDARY, "View Invoice Information", "U+2139", (buttonUser1, viewButtonEvent) -> {
                                            viewButtonEvent.getChannel().retrieveMessageById(invoice.getMessageID()).queue(message -> viewButtonEvent.replyEmbeds(message.getEmbeds().get(0)).setEphemeral(true).queue());
                                        }));
                                        add(new DiscordButton(ButtonStyle.SECONDARY, "Add files to Holding", "U+1F4C2", (buttonUser1, fileButtonEvent) -> {
                                            new InvoiceAddFileConversation(invoice);
                                            fileButtonEvent.getInteraction().editMessageEmbeds(EmbedUtil.checkDMForMore()).setComponents().queue();
                                        }));
                                        add(new DiscordButton(ButtonStyle.DANGER, "Cancel Invoice", "U+26D4", (buttonUser1, cancelInvoiceEvent) -> {
                                            invoice.cancel();
                                        }));
                                    }};
                                    menuEvent.getInteraction().editMessageEmbeds(EmbedUtil.genericInvoiceInformation(invoiceID)).setActionRow(invoiceActionButtons.stream().map(DiscordButton::asButton).toList()).queue();
                                });

                                viewButtonEvent.editMessageEmbeds(EmbedUtil.selectInvoice()).setActionRow(outstandingInvoicesMenu.asMenu()).queue();
                            }));
                        }};

                        invoiceButtonEvent.getInteraction()
                                .editComponents(ActionRow.of(invoiceActionButtons.stream().map(DiscordButton::asButton).toList()))
                                .setEmbeds(EmbedUtil.invoiceInformation(commission.getPluginName()))
                                .queue();
                    })
            );

            event.getInteraction()
                    .editComponents(ActionRow.of(projectActionSettings.stream().map(DiscordButton::asButton).toList()))
                    .setEmbeds(EmbedUtil.projectInformation(pluginName))
                    .queue();
        }
    }
}