package dev.shreyasayyengar.bot.customer.conversation.impl;

import dev.shreyasayyengar.bot.DiscordBot;
import dev.shreyasayyengar.bot.customer.Customer;
import dev.shreyasayyengar.bot.misc.utils.Util;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

/**
 * A CustomerEmailConversation is a user <---> bot conversation that is initiated by the user.
 * This conversation is used to store the user's email address that will later be used
 * to send an {@link dev.shreyasayyengar.bot.paypal.Invoice} to.
 * <p></p>
 * @author Shreyas Ayyengar
 */
public class CustomerEmailConversation extends ListenerAdapter {

    private final Customer customer;

    public CustomerEmailConversation(Customer customer) {
        this.customer = customer;

        customer.getTextChannel().sendMessage(customer.getHolder().getAsMention()).queue(msg -> msg.delete().queue());
        MessageEmbed embed = new EmbedBuilder()
                .setTitle("Register your PayPal Email")
                .setDescription("Please enter your email address")
                .setColor(Util.getColor())
                .setFooter("Your email will be used to send invoices regarding your commissions")
                .setThumbnail("https://pngimg.com/uploads/paypal/paypal_PNG22.png")
                .build();

        customer.getTextChannel().sendMessageEmbeds(embed).queue();

        DiscordBot.get().bot().addEventListener(this);
    }

    private void finish() {

        MessageEmbed embed = new EmbedBuilder()
                .setTitle("Register your PayPal Email")
                .setDescription("**Success!**: `Your email has been registered`")
                .setColor(Color.GREEN)
                .setThumbnail("https://pngimg.com/uploads/paypal/paypal_PNG22.png")
                .setFooter("The email " + customer.getPaypalEmail() + " has been registered and will be used for the future.")
                .build();

        customer.getTextChannel().sendMessage("<@690755476555563019>").queue(msg -> msg.delete().queue());
        customer.getTextChannel().sendMessageEmbeds(embed).queue();

        DiscordBot.get().bot().removeEventListener(this);
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {

        if (!event.getChannel().getId().equalsIgnoreCase(customer.getTextChannel().getId())) return;
        if (!event.getAuthor().getId().equals(customer.getHolder().getId())) return;

        String email = event.getMessage().getContentStripped().trim();
        customer.setPaypalEmail(email);

        finish();
    }
}