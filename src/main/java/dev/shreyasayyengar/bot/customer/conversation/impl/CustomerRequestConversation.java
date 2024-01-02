package dev.shreyasayyengar.bot.customer.conversation.impl;

import dev.shreyasayyengar.bot.DiscordBot;
import dev.shreyasayyengar.bot.customer.Customer;
import dev.shreyasayyengar.bot.customer.CustomerCommission;
import dev.shreyasayyengar.bot.misc.utils.Util;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionHook;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * A CustomerRequestConversation is a user <---> bot conversation that is initiated by a customer
 * who wishes to make a commission request. Once started, the discord bot will take
 * the customer through the various stages of {@link CustomerRequestStage}. Once finished
 * the confirmation provided by the conversation will construct a new {@link CustomerCommission}.
 * <p></p>
 *
 * @author Shreyas Ayyengar
 */
public class CustomerRequestConversation extends ListenerAdapter {

    public static final Set<Customer> ALREADY_REQUESTING = new HashSet<>();

    private final Customer customer;
    private final InteractionHook initialMessageHook;
    private final List<String> responses = new ArrayList<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private ScheduledFuture<?> timeoutFuture = scheduler.schedule(() -> this.cancel(true), 5, TimeUnit.MINUTES);
    private Message currentMessage;
    private CustomerRequestStage stage = CustomerRequestStage.NAME;

    public CustomerRequestConversation(Customer customer, InteractionHook initialMessageHook) {
        this.customer = customer;
        this.initialMessageHook = initialMessageHook;

        ALREADY_REQUESTING.add(customer);

        checkStage();
        DiscordBot.get().bot().addEventListener(this);
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {

        if (!event.getChannel().getId().equalsIgnoreCase(customer.getTextChannel().getId())) return;
        if (!event.getAuthor().getId().equals(customer.getHolder().getId())) return;

        String contentRaw = event.getMessage().getContentRaw();

        if (contentRaw.contains("!stoprequest")) {
            cancel(false);
            return;
        }

        if (stage == CustomerRequestStage.NAME && contentRaw.length() > 30) {
            event.getMessage().delete().queue();
            customer.getTextChannel().sendMessage("The plugin name requested is far too long! Please keep it within 30 characters.").queue(message -> message.delete().queueAfter(5, TimeUnit.SECONDS));
            return;
        }

        responses.add(contentRaw);

        event.getChannel().asTextChannel().deleteMessages(List.of(event.getMessage(), currentMessage)).queue();

        if (responses.size() < CustomerRequestStage.values().length) {
            setStage(stage.next());
            checkStage();
        } else {
            initialMessageHook.deleteOriginal().queue();
            finish();
        }
    }

    private void checkStage() {
        customer.getTextChannel().sendMessageEmbeds(stage.getEmbedInstruction()).queue(message -> currentMessage = message);
        resetTimeout();
    } // Check the current CustomerRequestStage and instruct.

    private void finish() {

        Collection<String> no = Stream.of("no", "none", "n").toList();

        String arrow = "<:purple_arrow:980020213863055390> ";

        boolean longerDescription = false;
        boolean longerExtraInfo = false;

        EmbedBuilder compiledResponses = new EmbedBuilder();
        compiledResponses.setTitle(customer.getHolder().getEffectiveName() + "'s Plugin Request");
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

        Message commissionRequestDone = customer.getTextChannel().sendMessageEmbeds(compiledResponses.build()).complete();
        String id = commissionRequestDone.getId();

        if (longerDescription) {
            MessageEmbed description = new EmbedBuilder()
                    .setTitle("Description:")
                    .setDescription(responses.get(1))
                    .setColor(Util.getColor())
                    .build();
            customer.getTextChannel().sendMessageEmbeds(description).complete();
        }

        if (longerExtraInfo) {
            MessageEmbed extraInfo = new EmbedBuilder()
                    .setTitle("Extra Info:")
                    .setDescription(responses.get(6))
                    .setColor(Util.getColor())
                    .build();
            customer.getTextChannel().sendMessageEmbeds(extraInfo).complete();
        }

        commissionRequestDone.pin().complete();
        customer.getTextChannel().sendMessage("<@690755476555563019>").complete();
        customer.getTextChannel().getHistory().retrievePast(2).complete().forEach(message -> message.delete().queue());

        customer.getCommissions().add(new CustomerCommission(customer, responses.get(0), no.stream().noneMatch(responses.get(5)::equalsIgnoreCase), id));

        timeoutFuture.cancel(true);
        ALREADY_REQUESTING.remove(customer);
        DiscordBot.get().bot().removeEventListener(this);
    }

    private void cancel(boolean inactive) {
        initialMessageHook.deleteOriginal().queue();

        EmbedBuilder embedBuilder = new EmbedBuilder()
                .setTitle("Cancelled Request")
                .setDescription("Your request has been cancelled.")
                .setColor(Color.RED);

        if (inactive) {
            embedBuilder.setFooter("You were inactive for too long. Please try again.");
        }

        customer.getTextChannel().sendMessageEmbeds(embedBuilder.build()).queue();
        timeoutFuture.cancel(true);
        ALREADY_REQUESTING.remove(customer);
        DiscordBot.get().bot().removeEventListener(this);
    }

    // Example usage
    public void resetTimeout() {
        // Cancel any existing timeout task
        if (timeoutFuture != null && !timeoutFuture.isDone()) {
            timeoutFuture.cancel(true);
        }

        // Start a new timeout task
        timeoutFuture = scheduler.schedule(() -> this.cancel(true), 5, TimeUnit.MINUTES);
    }

    // ------------------ CustomerRequestStage ------------------ //

    enum CustomerRequestStage {
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

        public CustomerRequestStage next() {
            return CustomerRequestStage.values()[ordinal() + 1];
        }
    }

    public void setStage(CustomerRequestStage stage) {
        this.stage = stage;
    }
}