package dev.shreyasayyengar.bot.customer.conversation;

import dev.shreyasayyengar.bot.DiscordBot;
import dev.shreyasayyengar.bot.customer.CustomerCommission;
import dev.shreyasayyengar.bot.misc.utils.Authentication;
import dev.shreyasayyengar.bot.misc.utils.EmbedUtil;
import dev.shreyasayyengar.bot.misc.utils.Util;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

/**
 * A QuoteChangeConversation is a restricted conversation that is initiated only by the owner
 * of the discord bot / developer / me. This conversation is used to change the quote of an existing {@link CustomerCommission}
 * by calling {@link CustomerCommission#setPrice(double)}.
 * <p></p>
 *
 * @author Shreyas Ayyengar
 */
public class QuoteChangeConversation extends ListenerAdapter {

    private final CustomerCommission commission;
    private final TextChannel textChannel;

    public QuoteChangeConversation(CustomerCommission commission, ButtonInteractionEvent event) {
        this.commission = commission;
        this.textChannel = commission.getCustomer().getTextChannel();

        MessageEmbed embed = new EmbedBuilder()
                .setTitle("Quote Change: " + commission.getPluginName())
                .setDescription("Please enter the new price for this commission.")
                .addField("Current Price:", "`$" + commission.getPrice() + "`", false)
                .setFooter("Type '!cancel' to cancel this process.")
                .setColor(Util.THEME_COLOUR)
                .build();

        event.replyEmbeds(embed).setEphemeral(true).queue();

        DiscordBot.get().bot().addEventListener(this);
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (!event.getChannel().asTextChannel().getId().equalsIgnoreCase(textChannel.getId())) return;
        if (!event.getAuthor().getId().equals(Authentication.OWNER_ID.get())) return;

        String message = event.getMessage().getContentRaw();

        if (message.equalsIgnoreCase("!cancel")) {
            event.getMessage().delete().queue();
            DiscordBot.get().bot().removeEventListener(this);
            return;
        }

        double newQuote = Double.parseDouble(message);
        commission.setPrice(newQuote);

        event.getMessage().delete().queue();
        commission.getCustomer().getTextChannel().sendMessage("@here").setEmbeds(EmbedUtil.priceUpdated(commission)).queue();
        commission.setConfirmed(false);

        DiscordBot.get().bot().removeEventListener(this);
    }
}