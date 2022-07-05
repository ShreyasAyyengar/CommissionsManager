package dev.shreyasayyengar.bot.client.conversation;

import dev.shreyasayyengar.bot.client.ClientInfo;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

// TODO
public abstract class AbstractClientConversation extends ListenerAdapter {

    private final ClientInfo clientInfo;

    protected AbstractClientConversation(ClientInfo clientInfo) {
        this.clientInfo = clientInfo;
    }

    public abstract void finish();

    public abstract void cancel();

    public abstract void onMessageReceived(@NotNull MessageReceivedEvent event);
}