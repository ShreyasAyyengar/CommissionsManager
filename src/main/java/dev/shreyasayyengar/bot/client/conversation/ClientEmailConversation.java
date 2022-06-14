package dev.shreyasayyengar.bot.client.conversation;

import dev.shreyasayyengar.bot.DiscordBot;
import dev.shreyasayyengar.bot.client.ClientInfo;
import dev.shreyasayyengar.bot.misc.utils.Util;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class ClientEmailConversation extends ListenerAdapter {

    private final ClientInfo clientInfo;

    public ClientEmailConversation(ClientInfo clientInfo) {
        this.clientInfo = clientInfo;

        clientInfo.getTextChannel().sendMessage(clientInfo.getHolder().getAsMention()).queue(msg -> msg.delete().queue());
        MessageEmbed embed = new EmbedBuilder()
                .setTitle("Register your PayPal Email")
                .setDescription("Please enter your email address")
                .setColor(Util.getColor())
                .setFooter("Your email will be used to send invoices regarding your commissions")
                .setThumbnail("https://pngimg.com/uploads/paypal/paypal_PNG22.png")
                .build();

        clientInfo.getTextChannel().sendMessageEmbeds(embed).queue();

        DiscordBot.get().bot().addEventListener(this);
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {

        if (!event.getTextChannel().getId().equalsIgnoreCase(clientInfo.getTextChannel().getId())) return;
        if (!event.getAuthor().getId().equals(clientInfo.getHolder().getId())) return;

        String email = event.getMessage().getContentStripped().trim();
        clientInfo.setPaypalEmail(email);

        finish();
    }

    private void finish() {

        MessageEmbed embed = new EmbedBuilder()
                .setTitle("Register your PayPal Email")
                .setDescription("**Success!**: `Your email has been registered`")
                .setColor(Color.GREEN)
                .setThumbnail("https://pngimg.com/uploads/paypal/paypal_PNG22.png")
                .setFooter("The email " + clientInfo.getPaypalEmail() + " has been registered and will be used for the future.")
                .build();

        clientInfo.getTextChannel().sendMessage("<@690755476555563019>").queue(msg -> msg.delete().queue());
        clientInfo.getTextChannel().sendMessageEmbeds(embed).queue();

        DiscordBot.get().bot().removeEventListener(this);
    }
}
