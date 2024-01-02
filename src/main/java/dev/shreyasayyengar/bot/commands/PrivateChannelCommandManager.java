package dev.shreyasayyengar.bot.commands;

import dev.shreyasayyengar.bot.DiscordBot;
import dev.shreyasayyengar.bot.customer.CustomerCommission;
import dev.shreyasayyengar.bot.customer.Customer;
import dev.shreyasayyengar.bot.customer.conversation.impl.CustomerRequestConversation;
import dev.shreyasayyengar.bot.misc.utils.EmbedUtil;
import dev.shreyasayyengar.bot.misc.utils.Util;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
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

        Customer customer = DiscordBot.get().getCustomerManger().getByTextChannel(event.getChannel().asTextChannel());

        switch (event.getName().toLowerCase()) {
            case "collaborator" -> {
                String action = event.getSubcommandName().toLowerCase();
                Member member = event.getOption("member").getAsMember();

                if (member.getId().equalsIgnoreCase(customer.getHolder().getId())) {
                    event.replyEmbeds(EmbedUtil.alreadyCollaborator()).setEphemeral(true).queue();
                    return;
                }

                if (action.equalsIgnoreCase("add")) {
                    customer.addCollaborator(member);
                    event.replyEmbeds(EmbedUtil.joinedAsCollaborator(member)).setEphemeral(false).queue();
                }

                if (action.equalsIgnoreCase("remove")) {
                    if (customer.getTextChannel().getMembers().stream().map(Member::getId).anyMatch(id -> id.equalsIgnoreCase(member.getId()))) {
                        customer.removeCollaborator(member);
                        event.replyEmbeds(EmbedUtil.removedAsCollaborator(member)).setEphemeral(false).queue();
                    } else {
                        event.replyEmbeds(EmbedUtil.notCollaborator()).setEphemeral(true).queue();
                    }
                }
            }

            case "request" -> {
                if (!DiscordBot.get().getCustomerManger().containsInfoOf(event.getMember().getId())) {
                    event.replyEmbeds(EmbedUtil.notAClient()).setEphemeral(true).queue();
                    return;
                }

                if (CustomerRequestConversation.ALREADY_REQUESTING.contains(customer)) {
                    event.replyEmbeds(EmbedUtil.alreadyRequesting()).setEphemeral(true).queue();
                    return;
                }

                MessageEmbed embed = new EmbedBuilder()
                        .setTitle("Please fill out the following information by answering in the chat!")
                        .setDescription("If you would like to cancel this request, please type `!stoprequest`")
                        .setColor(Util.getColor())
                        .build();

                event.replyEmbeds(embed).queue(interactionHook -> new CustomerRequestConversation(DiscordBot.get().getCustomerManger().get(event.getMember().getId()), interactionHook));
            }

            case "commissions" -> {
                Collection<CustomerCommission> commissions = customer.getCommissions();

                if (commissions.isEmpty()) {
                    event.replyEmbeds(EmbedUtil.noCommissions()).setEphemeral(true).queue();
                    return;
                }

                StringSelectMenu.Builder builder = StringSelectMenu.create("menu:commissions")
                        .setPlaceholder("Select a commission...")
                        .setRequiredRange(1, 1);

                for (CustomerCommission commission : commissions) {
                    builder.addOption(commission.getPluginName(), "commission." + commission.getPluginName());
                }

                SelectMenu commissionsMenu = builder.build();
                event.replyEmbeds(EmbedUtil.selectCommission()).addActionRow(commissionsMenu).setEphemeral(true).queue();
            }

            case "email" -> {
                String email = event.getOption("email").getAsString();
                customer.setPaypalEmail(email);

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
                        .setFooter("The email " + customer.getPaypalEmail() + " has been registered and will be used for the future.")
                        .build();

                customer.getTextChannel().sendMessage("<@690755476555563019>").queue(msg -> msg.delete().queue());
                event.replyEmbeds(embed).queue();
            }
        }
    }
}