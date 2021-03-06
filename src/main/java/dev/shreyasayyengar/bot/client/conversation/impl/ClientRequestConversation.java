package dev.shreyasayyengar.bot.client.conversation.impl;

import dev.shreyasayyengar.bot.DiscordBot;
import dev.shreyasayyengar.bot.client.ClientCommission;
import dev.shreyasayyengar.bot.client.ClientInfo;
import dev.shreyasayyengar.bot.misc.utils.Util;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionHook;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

/**
 * A ClientRequestConversation is a user <---> bot conversation that is initiated by a client
 * who wishes to make a commission request. Once started, the discord bot will take
 * the client through the various stages of {@link ClientRequestStage}. Once finished
 * the confirmation provided by the conversation will construct a new {@link ClientCommission}.
 * <p></p>
 *
 * @author Shreyas Ayyengar
 */
public class ClientRequestConversation extends ListenerAdapter {

    private final ClientInfo client;
    private final InteractionHook initialMessageHook;
    private final List<String> responses = new ArrayList<>();

    private Message currentMessage;
    private ClientRequestStage stage = ClientRequestStage.NAME;

    public ClientRequestConversation(ClientInfo clientInfo, InteractionHook initialMessageHook) {
        this.client = clientInfo;
        this.initialMessageHook = initialMessageHook;

        checkStage();
        DiscordBot.get().bot().addEventListener(this);
    }

    private void checkStage() {
        client.getTextChannel().sendMessageEmbeds(stage.getEmbedInstruction()).queue(message -> currentMessage = message);
    } // Check the current ClientRequestStage and instruct.

    private void finish() {

        Collection<String> no = Stream.of("no", "none", "n").toList();

        String arrow = "<:purple_arrow:980020213863055390> ";

        boolean longerDescription = false;
        boolean longerExtraInfo = false;

        EmbedBuilder compiledResponses = new EmbedBuilder();
        compiledResponses.setTitle(client.getHolder().getEffectiveName() + "'s Plugin Request");
        compiledResponses.setDescription("For the plugin: `" + responses.get(0) + "`");

        if (responses.get(1).length() > 1000) {
            compiledResponses.addField("Description:", arrow + "`See below:`", false);
            longerDescription = true;
        } else {
            compiledResponses.addField("Description:", arrow + "`" + responses.get(1) + "`", false);
        }

        compiledResponses.addField("Plugin Type:", arrow + "`" + responses.get(2) + "`", false);
        compiledResponses.addField("Minecraft Version:", arrow + "`" + responses.get(3) + "`", false);
        compiledResponses.addField("Java Version:", arrow + "`" + responses.get(4) + "`", false);

        if (no.stream().noneMatch(responses.get(5)::equalsIgnoreCase)) {
            compiledResponses.addField("Requested SRC:", arrow + "`True|Yes`", false);
        }

        if (no.stream().noneMatch(string -> responses.get(6).toLowerCase().startsWith(string))) {
            if (responses.get(6).length() > 1000) {
                compiledResponses.addField("Extra Info:", arrow + "`See below:`", false);
                longerExtraInfo = true;
            } else {
                compiledResponses.addField("Extra Info:", arrow + "`" + responses.get(6) + "`", false);
            }
        }

        compiledResponses.setColor(Util.getColor());

        Message commissionRequestDone = client.getTextChannel().sendMessageEmbeds(compiledResponses.build()).complete();
        String id = commissionRequestDone.getId();

        if (longerDescription) {
            MessageEmbed description = new EmbedBuilder()
                    .setTitle("Description:")
                    .setDescription(responses.get(1))
                    .setColor(Util.getColor())
                    .build();
            client.getTextChannel().sendMessageEmbeds(description).complete();
        }

        if (longerExtraInfo) {
            MessageEmbed extraInfo = new EmbedBuilder()
                    .setTitle("Extra Info:")
                    .setDescription(responses.get(6))
                    .setColor(Util.getColor())
                    .build();
            client.getTextChannel().sendMessageEmbeds(extraInfo).complete();
        }

        commissionRequestDone.pin().complete();
        client.getTextChannel().sendMessage("<@690755476555563019>").complete();
        client.getTextChannel().getHistory().retrievePast(2).complete().forEach(message -> message.delete().queue());

        client.getCommissions().add(new ClientCommission(client, responses.get(0), no.stream().noneMatch(responses.get(5)::equalsIgnoreCase), id));

        DiscordBot.get().bot().removeEventListener(this);
    }

    private void cancel() {
        client.getTextChannel().getHistory().retrievePast(3).complete().forEach(message -> message.delete().queue());

        MessageEmbed embed = new EmbedBuilder()
                .setTitle("Cancelled Request")
                .setDescription("Your request has been cancelled.")
                .setColor(Color.RED)
                .build();

        client.getTextChannel().sendMessageEmbeds(embed).queue();

        DiscordBot.get().bot().removeEventListener(this);
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {

        if (!event.getChannel().getId().equalsIgnoreCase(client.getTextChannel().getId())) return;
        if (!event.getAuthor().getId().equals(client.getHolder().getId())) return;

        if (event.getMessage().getContentRaw().contains("!stoprequest")) {
            cancel();
            return;
        }

        responses.add(event.getMessage().getContentRaw());

        event.getChannel().asTextChannel().deleteMessages(List.of(event.getMessage(), currentMessage)).queue();

        if (responses.size() < ClientRequestStage.values().length) {
            setStage(stage.next());
            checkStage();
        } else {
            initialMessageHook.deleteOriginal().queue();
            finish();
        }
    }

    // ------------------ ClientRequestStage ------------------ //

    enum ClientRequestStage {
        NAME,
        DESCRIPTION,
        SERVER_TYPE,
        MINECRAFT_VERSION,
        JAVA_VERSION,
        SRC,
        EXTRA_INFORMATION;

        public MessageEmbed getEmbedInstruction() {

            EmbedBuilder embed = null;

            switch (this) {
                case DESCRIPTION -> embed = new EmbedBuilder()
                        .setTitle("What exactly do you need developed? (in one message)")
                        .addField("Be specific! Specificity is good!!", "Please feel free to include any images/videos/documents/etc to aid in the development process.", true);

                case SERVER_TYPE -> embed = new EmbedBuilder()
                        .setTitle("What type of plugin do you need developed?")
                        .addField("Options include:", "`Bukkit`\n `Spigot`\n `Paper`\n `Bungee`", true)
                        .setFooter("Or perhaps you would like a different type of plugin...");

                case MINECRAFT_VERSION -> embed = new EmbedBuilder()
                        .setTitle("What Minecraft version does your server run?")
                        .addField("Plugins are pretty flexible when it comes to cross-version-compatibility", "but I need to know the base Minecraft version!", false)
                        .setFooter("For example: `1.8.8`, `1.12.2`, `1.13.2`, `1.14.4`, `1.15.2`, `1.16.1`, `1.17` etc.");

                case JAVA_VERSION -> embed = new EmbedBuilder()
                        .setTitle("What version of Java does your server run?")
                        .addField("This can be pretty hard to find specifically: ", "a. Type `java -version` in your terminal and type the result\n" +
                                        "b. Reply with `Unknown` if you are not sure."
                                , true);

                case NAME -> embed = new EmbedBuilder()
                        .setTitle("What would you like this plugin to be called?")
                        .addField("Please be specific!", "This will be used to name your plugin and will be used in the final product.", true)
                        .setFooter("For example: `LootBoxes`, `PotionMasters`, `WorldManager`, `CommandLogger` etc.");

                case SRC -> embed = new EmbedBuilder()
                        .setTitle("Would you like access to the source code once your plugin is finished?")
                        .addField("Options include:", "`Yes` | `No`", true)
                        .addField("Reminder:", "If the source code is requested, this adds a $5 fee to the total.", true)
                        .setFooter("You can always opt out or opt of this at any point. Just let me know!");

                case EXTRA_INFORMATION -> embed = new EmbedBuilder()
                        .setTitle("Anything else you would like to add?")
                        .setDescription("Just reply with any additional information you would like to add.")
                        .setFooter("This is optional, but I would love to know more about your project!\nIf not, please just type 'no'");

            }

            embed.setColor(Util.getColor());
            return embed.build();
        }

        public ClientRequestStage next() {
            return ClientRequestStage.values()[ordinal() + 1];
        }
    }

    public void setStage(ClientRequestStage stage) {
        this.stage = stage;
    }
}