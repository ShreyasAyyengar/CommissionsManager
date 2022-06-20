package dev.shreyasayyengar.bot.client.conversation;

import dev.shreyasayyengar.bot.DiscordBot;
import dev.shreyasayyengar.bot.client.ClientCommission;
import dev.shreyasayyengar.bot.client.ClientInfo;
import dev.shreyasayyengar.bot.misc.utils.Util;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

public class ClientRequestConversation extends ListenerAdapter {

    private final ClientInfo client;
    private final List<String> responses = new ArrayList<>();
    private ClientRequestStage stage = ClientRequestStage.NAME;

    public ClientRequestConversation(ClientInfo clientInfo) {
        this.client = clientInfo;

        clientInfo.getTextChannel().sendMessage(clientInfo.getHolder().getAsMention()).queue(message -> message.delete().queue());

        checkStage();

        DiscordBot.get().bot().addEventListener(this);
    }

    private void checkStage() {
        client.getTextChannel().sendMessageEmbeds(getStage().getEmbedInstruction()).queue();
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {

        if (!event.getTextChannel().getId().equalsIgnoreCase(client.getTextChannel().getId())) return;
        if (!event.getAuthor().getId().equals(client.getHolder().getId())) return;

        if (event.getMessage().getContentRaw().contains("!stoprequest")) {
            cancel();
            return;
        }

        responses.add(event.getMessage().getContentRaw());

        if (responses.size() < 7) {
            event.getTextChannel().deleteMessages(event.getChannel().getHistory().retrievePast(2).complete()).queue();

            setStage(stage.next());
            checkStage();

        } else {
            event.getTextChannel().deleteMessages(event.getChannel().getHistory().retrievePast(3).complete()).queue();
            finish();
        }
    }

    private void finish() {

        Collection<String> no = Stream.of("no", "none", "n").toList();

        String arrow = "<:purple_arrow:980020213863055390> ";
        EmbedBuilder compiledResponses = new EmbedBuilder()
                .setTitle(client.getHolder().getEffectiveName() + "'s Plugin Request")
                .setDescription("For the plugin: `" + responses.get(0) + "`")
                .addField("Description:", arrow + "`" + responses.get(1) + "`", false)
                .addField("Plugin Type:", arrow + "`" + responses.get(2) + "`", false)
                .addField("Minecraft Version:", arrow + "`" + responses.get(3) + "`", false)
                .addField("Java Version:", arrow + "`" + responses.get(4) + "`", false);

        if (no.stream().noneMatch(responses.get(5)::equalsIgnoreCase)) {
            compiledResponses.addField("Requested SRC:", arrow + "`True|Yes`", false);
        }

        if (no.stream().noneMatch(string -> responses.get(6).toLowerCase().startsWith(string))) {
            compiledResponses.addField("Extra Information", arrow + "`" + responses.get(6) + "`", false);
        }

        compiledResponses.setColor(Util.getColor());

        Message commissionRequestDone = client.getTextChannel().sendMessageEmbeds(compiledResponses.build()).complete();
        commissionRequestDone.pin().complete();

        client.getTextChannel().sendMessage("<@690755476555563019>").complete();
        client.getTextChannel().getHistory().retrievePast(2).complete().forEach(message -> message.delete().queue());

        client.getCommissions().add(new ClientCommission(client, responses.get(0), no.stream().noneMatch(responses.get(5)::equalsIgnoreCase), commissionRequestDone.getId()));
        System.out.println("added commission");

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

    public ClientRequestStage getStage() {
        return stage;
    }
}
