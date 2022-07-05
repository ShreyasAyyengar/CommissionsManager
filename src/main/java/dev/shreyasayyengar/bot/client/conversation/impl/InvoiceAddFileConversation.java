package dev.shreyasayyengar.bot.client.conversation.impl;

import dev.shreyasayyengar.bot.DiscordBot;
import dev.shreyasayyengar.bot.misc.utils.EmbedUtil;
import dev.shreyasayyengar.bot.paypal.Invoice;
import dev.shreyasayyengar.bot.properties.PrimaryDiscordProperty;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * A InvoiceAddFileConversation is a restricted conversation that is initiated only by the owner
 * of the discord bot / developer / me. This conversation is used to add a file to an existing {@link Invoice}
 * by calling {@link Invoice#addFileToHolding(File)}. This conversation opens a Private DM/Channel with the user
 * and requires the user to upload a file, most commonly a <code>.jar</code> file.
 * <p></p>
 *
 * @author Shreyas Ayyengar
 */
public class InvoiceAddFileConversation extends ListenerAdapter {

    private final Invoice invoice;

    public InvoiceAddFileConversation(Invoice invoice) {
        this.invoice = invoice;

        DiscordBot.get().bot().getUserById(PrimaryDiscordProperty.OWNER_ID.get()).openPrivateChannel().queue(privateChannel -> privateChannel.sendMessageEmbeds(EmbedUtil.addAttachments(invoice)).queue());
        DiscordBot.get().bot().addEventListener(this);
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (event.getChannelType() != ChannelType.PRIVATE) return;
        if (!event.getPrivateChannel().getUser().getId().equalsIgnoreCase(PrimaryDiscordProperty.OWNER_ID.get()))
            return;

        PrivateChannel privateChannel = event.getPrivateChannel();
        Message message = event.getMessage();

        if (message.getAuthor().isBot()) return;

        if (message.getContentRaw().equalsIgnoreCase("!cancel")) {
            DiscordBot.get().bot().removeEventListener(this);
            return;
        }

        if (message.getAttachments().size() == 0) {
            privateChannel.sendMessageEmbeds(EmbedUtil.noAttachments()).queue();
            return;
        }

        for (Message.Attachment attachment : message.getAttachments()) {

            try {
                File folder = new File("files-holding");
                if (!folder.exists()) folder.mkdir();

                File file = new File("files-holding/" + attachment.getFileName());
                attachment.downloadToFile(file).thenAcceptAsync(downloadedFile -> invoice.addFileToHolding(file));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        privateChannel.sendMessageEmbeds(EmbedUtil.attachmentsAdded()).queue();
        DiscordBot.get().bot().removeEventListener(this);
    }
}