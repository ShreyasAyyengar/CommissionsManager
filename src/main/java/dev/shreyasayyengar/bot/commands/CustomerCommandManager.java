package dev.shreyasayyengar.bot.commands;

import dev.shreyasayyengar.bot.DiscordBot;
import dev.shreyasayyengar.bot.customer.Customer;
import dev.shreyasayyengar.bot.customer.CustomerCommission;
import dev.shreyasayyengar.bot.functional.type.DiscordModal;
import dev.shreyasayyengar.bot.utils.EmbedUtil;
import dev.shreyasayyengar.bot.utils.Util;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.Collection;
import java.util.stream.Stream;

public class CustomerCommandManager extends ListenerAdapter {
    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        Stream<String> commandName = Stream.of("request", "collaborator", "quote", "commissions");

        if (commandName.anyMatch(event.getName().toLowerCase()::contains)) {
            if (Util.privateChannel(event.getChannel().asTextChannel())) {
                event.replyEmbeds(EmbedUtil.onlyInPrivateChannels()).setEphemeral(true).queue();
                return;
            }
        }

        Customer customer = DiscordBot.get().getCustomerManger().getByTextChannel(event.getChannel().asTextChannel());

        switch (event.getName().toLowerCase()) {
            case "request" -> {
                if (!DiscordBot.get().getCustomerManger().containsInfoOf(event.getMember().getId())) {
                    event.replyEmbeds(EmbedUtil.notAClient()).setEphemeral(true).queue();
                    return;
                }

                DiscordModal modal = new DiscordModal("Request Form")
                        .addTextInput(
                                TextInput.create("name", "Plugin Name", TextInputStyle.SHORT)
                                        .setRequired(true)
                                        .setRequiredRange(1, 30)
                                        .setPlaceholder("Ex: LootBoxes, PotionMasters, WorldManager")
                                        .build()
                        )
                        .addTextInput(
                                TextInput.create("description", "What do you need developed! Be specific!", TextInputStyle.PARAGRAPH)
                                        .setRequired(true)
                                        .setPlaceholder("Ex: I want a plugin that disables crafting tables. Permissions, Commands, Config...")
                                        .build()
                        )
                        .addTextInput(
                                TextInput.create("server-type", "What type of server do you run? (Plugin Type)", TextInputStyle.SHORT)
                                        .setRequired(true)
                                        .setPlaceholder("Ex: Bukkit, Spigot, Paper, BungeeCord etc...")
                                        .build()
                        )
                        .addTextInput(
                                TextInput.create("version", "What version of Minecraft do you run?", TextInputStyle.SHORT)
                                        .setRequired(true)
                                        .setRequiredRange(2, 100)
                                        .setPlaceholder("1.8.8, 1.12.2, 1.16, 1.20")
                                        .build()
                        )
                        .addTextInput(
                                TextInput.create("source-code", "Send source code after completion? (Adds 5%)", TextInputStyle.SHORT)
                                        .setRequired(true)
                                        .setRequiredRange(2, 3)
                                        .setPlaceholder("Yes/No")
                                        .build()
                        )
                        .onSubmit((modalUser, modalEvent) -> {
                            String name = modalEvent.getValue("name").getAsString();
                            String description = modalEvent.getValue("description").getAsString();
                            String serverType = modalEvent.getValue("server-type").getAsString();
                            String version = modalEvent.getValue("version").getAsString();
                            String sourceCode = modalEvent.getValue("source-code").getAsString();
                            boolean addSourceCode = sourceCode.equalsIgnoreCase("yes");

                            String arrow = "<:purple_arrow:980020213863055390>";
                            boolean longerDescription = description.length() > 1000;

                            EmbedBuilder compiledResponsesEmbedBuilder = new EmbedBuilder();
                            compiledResponsesEmbedBuilder.setTitle(customer.getHolder().getEffectiveName() + "'s Plugin Request");
                            compiledResponsesEmbedBuilder.setDescription("For the plugin: `" + name + "`");

                            if (longerDescription) {
                                compiledResponsesEmbedBuilder.addField("Description:", "See Below", false);
                            } else compiledResponsesEmbedBuilder.addField("Description:", arrow + " " + description, false);

                            compiledResponsesEmbedBuilder.addField("Plugin/Server Type:", arrow + " " + serverType, false);
                            compiledResponsesEmbedBuilder.addField("Version:", arrow + " " + version, false);
                            compiledResponsesEmbedBuilder.addField("Source Code (adds 5% to total quote):", arrow + " " + (addSourceCode ? "Yes" : "No") + "\nIf you would like this changed, please let me know!", false);
                            compiledResponsesEmbedBuilder.setColor(Util.THEME_COLOUR);
                            modalEvent.deferReply().queue();

                            modalEvent.getHook().sendMessageEmbeds(compiledResponsesEmbedBuilder.build()).queue(commissionRequestDoneMessage -> {
                                if (longerDescription) {
                                    MessageEmbed longerDescriptionEmbed = new EmbedBuilder()
                                            .setTitle("Description:")
                                            .setDescription(description)
                                            .setColor(Util.THEME_COLOUR)
                                            .build();
                                    customer.getTextChannel().sendMessageEmbeds(longerDescriptionEmbed).queue();
                                }

                                commissionRequestDoneMessage.pin().queue();

                                customer.getTextChannel().sendMessage("<@690755476555563019>").queue(message -> message.delete().queue());
                                customer.getCommissions().add(new CustomerCommission(customer, name, addSourceCode, commissionRequestDoneMessage.getId()));
                            });
                        });

                event.replyModal(modal.asModal()).queue();
            }

            case "vc" -> {

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

            case "email" -> { // TODO check double implementation
                String email = event.getOption("email").getAsString();

                if (!email.matches("([A-Za-z\\d-_.]+@[A-Za-z\\d-_]+(?:\\.[A-Za-z\\d]+)+)")) {
                    event.replyEmbeds(EmbedUtil.invalidEmail()).setEphemeral(true).queue();
                    return;
                }
                customer.setPaypalEmail(email);

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
        }
    }
}