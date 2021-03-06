package dev.shreyasayyengar.bot.commands;

import dev.shreyasayyengar.bot.DiscordBot;
import dev.shreyasayyengar.bot.client.ClientCommission;
import dev.shreyasayyengar.bot.client.ClientInfo;
import dev.shreyasayyengar.bot.client.conversation.impl.ClientRequestConversation;
import dev.shreyasayyengar.bot.misc.utils.EmbedUtil;
import dev.shreyasayyengar.bot.misc.utils.Util;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.Collection;
import java.util.stream.Stream;

public class PrivateChannelCommandManager extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {

        Stream<String> request = Stream.of("request", "collaborator", "quote", "commissions");

        if (request.anyMatch(event.getName().toLowerCase()::contains)) {
            if (Util.privateChannel(event.getChannel().asTextChannel())) {
                event.replyEmbeds(EmbedUtil.onlyInPrivateChannels()).setEphemeral(true).queue();
                return;
            }
        }

        ClientInfo clientInfo = DiscordBot.get().getClientManger().getByTextChannel(event.getChannel().asTextChannel());

        switch (event.getName().toLowerCase()) {
            case "collaborator" -> {
                String action = event.getSubcommandName().toLowerCase();
                Member member = event.getOption("member").getAsMember();

                if (member.getId().equalsIgnoreCase(clientInfo.getHolder().getId())) {
                    event.replyEmbeds(EmbedUtil.alreadyCollaborator()).setEphemeral(true).queue();
                    return;
                }

                if (action.equalsIgnoreCase("add")) {
                    clientInfo.addCollaborator(member);
                }

                if (action.equalsIgnoreCase("remove")) {
                    if (clientInfo.getTextChannel().getMembers().stream().map(Member::getId).anyMatch(id -> id.equalsIgnoreCase(member.getId()))) {
                        clientInfo.removeCollaborator(member);
                    } else {
                        event.replyEmbeds(EmbedUtil.notCollaborator()).setEphemeral(true).queue();
                    }
                }
            }

            case "request" -> {
                if (!DiscordBot.get().getClientManger().containsInfoOf(event.getMember().getId())) {
                    event.replyEmbeds(EmbedUtil.notAClient()).setEphemeral(true).queue();
                    return;
                }

                MessageEmbed embed = new EmbedBuilder()
                        .setTitle("Please fill out the following information by answering in the chat!")
                        .setDescription("If you would like to cancel this request, please type `!stoprequest`")
                        .setColor(Util.getColor())
                        .build();

                event.replyEmbeds(embed).queue(interactionHook -> new ClientRequestConversation(DiscordBot.get().getClientManger().get(event.getMember().getId()), interactionHook));
            }

            case "commissions" -> {
                Collection<ClientCommission> commissions = clientInfo.getCommissions();

                if (commissions.size() == 0) {
                    event.replyEmbeds(EmbedUtil.noCommissions()).setEphemeral(true).queue();
                    return;
                }

                SelectMenu.Builder builder = SelectMenu.create("menu:commissions")
                        .setPlaceholder("Select a commission...")
                        .setRequiredRange(1, 1);

                for (ClientCommission commission : commissions) {
                    builder.addOption(commission.getPluginName(), "commission." + commission.getPluginName());
                }

                SelectMenu commissionsMenu = builder.build();
                event.replyEmbeds(EmbedUtil.selectCommission()).addActionRow(commissionsMenu).setEphemeral(true).queue();
            }

            case "email" -> {
                String email = event.getOption("email").getAsString();
                clientInfo.setPaypalEmail(email);

                // check if email looks valid
                if (!email.matches("([A-Za-z\\d-_.]+@[A-Za-z\\d-_]+(?:\\.[A-Za-z\\d]+)+)")) {
                    event.replyEmbeds(EmbedUtil.invalidEmail()).setEphemeral(true).queue();
                    return;
                }

                MessageEmbed embed = new EmbedBuilder()
                        .setTitle("Register your PayPal Email")
                        .setDescription("**Success!**: `Your email has been registered`")
                        .setColor(Color.GREEN)
                        .setThumbnail("https://pngimg.com/uploads/paypal/paypal_PNG22.png")
                        .setFooter("The email " + clientInfo.getPaypalEmail() + " has been registered and will be used for the future.")
                        .build();

                clientInfo.getTextChannel().sendMessage("<@690755476555563019>").queue(msg -> msg.delete().queue());
                event.replyEmbeds(embed).queue();
            }
        }

    }
}