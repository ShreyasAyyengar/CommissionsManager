package dev.shreyasayyengar.bot.customer.conversation;

import dev.shreyasayyengar.bot.customer.Customer;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

// TODO
@SuppressWarnings("all")
public abstract class AbstractCustomerConversation extends ListenerAdapter {

    private final Customer customer;

    protected AbstractCustomerConversation(Customer customer) {
        this.customer = customer;
    }

    public abstract void finish();

    public abstract void cancel();

    public abstract void onMessageReceived(@NotNull MessageReceivedEvent event);
}