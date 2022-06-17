package dev.shreyasayyengar.bot.listeners.interactions;

import dev.shreyasayyengar.bot.DiscordBot;
import dev.shreyasayyengar.bot.client.ClientCommission;
import dev.shreyasayyengar.bot.client.ClientInfo;
import dev.shreyasayyengar.bot.client.conversation.ClientEmailConversation;
import dev.shreyasayyengar.bot.client.conversation.QuoteChangeConversation;
import dev.shreyasayyengar.bot.misc.utils.EmbedUtil;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Modal;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
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

        // ------------------------- Commission Info Buttons

        if (buttonID.contains(".")) {

            String pluginName = buttonID.split("\\.")[0];
            String action = buttonID.split("\\.")[1];

            ClientInfo clientInfo = DiscordBot.get().getClientManger().getByTextChannel(event.getTextChannel());
            ClientCommission commission = clientInfo.getCommission(pluginName);

            switch (action) {

                // ----------------------------- Commission Info Buttons --------------------------------- //
                case "invoice" -> {
                    if (commission.checkPrice()) {
                        event.replyEmbeds(EmbedUtil.noPriceSet()).setEphemeral(true).queue();
                        return;
                    }

                    if (!commission.isConfirmed()) {
                        event.replyEmbeds(EmbedUtil.notConfirmed()).setEphemeral(true).queue();
                        return;
                    }

                    if (commission.getClient().getPaypalEmail() == null) {
                        event.replyEmbeds(EmbedUtil.paypalEmailNotSet()).setEphemeral(true).queue();
                        new ClientEmailConversation(clientInfo);
                        return;
                    }

                    try {
                        commission.generateInvoice(event);
                    } catch (IOException | URISyntaxException e) {
                        throw new RuntimeException(e);
                    }
                }

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
                                    Button.success(pluginName + ".accept", "Accept Quote").withEmoji(Emoji.fromMarkdown("✅")),
                                    Button.danger(pluginName + ".reject", "Reject Quote").withEmoji(Emoji.fromMarkdown("⛔")))
                            .queue();
                }

                case "source-code" -> {
                    commission.setRequestedSourceCode(!commission.hasRequestedSourceCode());
                    event.replyEmbeds(EmbedUtil.sourceCodeUpdate(commission)).queue();
                }

                case "changequote" -> new QuoteChangeConversation(commission, event);

                case "info" -> {
                    Message infoMessage = clientInfo.getTextChannel().retrieveMessageById(commission.getInfoEmbed()).complete();
                    event.replyEmbeds(infoMessage.getEmbeds().get(0)).setEphemeral(true).queue();
                }

                case "complete" -> {
                    event.replyEmbeds(EmbedUtil.commissionComplete(commission))
                            .addActionRow(
                                    Button.success("vouch-modal", "Write a Vouch!").withEmoji(Emoji.fromMarkdown("✍️")),
                                    Button.link("https://tinyurl.com/mpmk7fy2", "Vouch on SpigotMC").withEmoji(Emoji.fromMarkdown("<:spigot:933250194877849640>"))
                            ).queue();
                    event.getChannel().sendMessage(clientInfo.getHolder().getAsMention()).queue(message -> message.delete().queue());
                    clientInfo.getCommissions().remove(commission);
                }

                case "cancel" -> {
                    clientInfo.getCommissions().remove(commission);
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

                // -------------------- Vouch Button-------------------- //
            }
        }

        if (buttonID.equalsIgnoreCase("vouch-modal")) {

            TextInput vouchBox = TextInput.create("text-box", "Please write your vouch here!", TextInputStyle.PARAGRAPH).setRequired(true).build();
            TextInput spigotBox = TextInput.create("spigotmc", "Have a SpigotMC account? Tag it here", TextInputStyle.SHORT).setRequired(false).build();

            Modal vouchModal = Modal.create("vouch-modal", "Write your vouch!")
                    .addActionRows(ActionRow.of(vouchBox), ActionRow.of(spigotBox))
                    .build();
            event.replyModal(vouchModal).queue();
        }
    }
}
