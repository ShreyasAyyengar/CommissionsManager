package dev.shreyasayyengar.bot.customer;

import dev.shreyasayyengar.bot.DiscordBot;
import dev.shreyasayyengar.bot.paypal.Invoice;
import dev.shreyasayyengar.bot.paypal.InvoiceDraft;
import dev.shreyasayyengar.bot.utils.EmbedUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.*;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;

/**
 * A CustomerCommission represents an ongoing commission for a customer. This object
 * may be constructed in two different ways: Either by creating a new commission, or by
 * loading an existing commission from the database.
 * <p></p>
 * The commission stores vital info, such as the {@link Customer} it is linked to,
 * the price of the commission, whether the customer has requested source code,
 * and the {@link Invoice}s that is associated with the commission.
 * <p></p>
 *
 * @author Shreyas Ayyengar
 */
public class CustomerCommission {
    private final Customer customer;
    private final String pluginName;
    private final String infoEmbedId;
    private boolean requestedSourceCode;
    private boolean confirmed;
    private double price;
    private final Collection<Invoice> invoices = new HashSet<>();

    /**
     * Constructs a new CustomerCommission object during Runtime. <b>This method should not be called
     * if the Commission is not being generated live.</b> i.e. only when created via the <code>/request</code>
     * command used by users in the Discord server.
     * <p></p>
     *
     * @param customer            The {@link Customer} object that the commission is linked to.
     * @param pluginName          The name of the plugin that the commission is for.
     * @param requestedSourceCode Whether the customer has requested source code.
     * @param infoEmbedId         The {@link net.dv8tion.jda.api.entities.MessageEmbed} that contains the info for the commission.
     */
    public CustomerCommission(Customer customer, String pluginName, boolean requestedSourceCode, String infoEmbedId) {
        this.customer = customer;
        this.pluginName = pluginName;
        this.requestedSourceCode = requestedSourceCode;
        this.confirmed = false;
        this.infoEmbedId = infoEmbedId;
    }

    /**
     * Constructs a new CustomerCommission object from the MySQL database. <b>This method should not be called
     * if the Commission is not being loaded from the database.</b>
     * <p></p>
     *
     * @param holderId            The {@link Customer}'s holder ID that the commission is linked to.
     * @param pluginName          The name of the plugin that the commission is for.
     * @param requestedSourceCode Whether the customer has requested source code.
     * @param confirmed           Whether the commission has been confirmed by the customer.
     * @param price               The price of the commission.
     * @param infoEmbedId         The {@link net.dv8tion.jda.api.entities.MessageEmbed} message ID that contains the info for the commission.
     */
    public CustomerCommission(String holderId, String pluginName, boolean requestedSourceCode, boolean confirmed, double price, String infoEmbedId) {
        this.pluginName = pluginName;
        this.requestedSourceCode = requestedSourceCode;
        this.confirmed = confirmed;
        this.price = price;
        this.infoEmbedId = infoEmbedId;

        this.customer = DiscordBot.get().getCustomerManger().get(holderId);
        this.customer.getCommissions().add(this);
    }

    /**
     * Generates a PayPal {@link InvoiceDraft} which is later pushed and converted to a final {@link Invoice},
     * which is also stored in {@link #invoices} once pushed to its final state.
     * <p></p>
     *
     * @param event The {@link ButtonInteractionEvent} that triggered this invoice generation request. (The Interaction
     *              WebHook will update the Embeds with the status of the invoice.)
     * @see InvoiceDraft
     * @see Invoice
     */
    public void generateInvoice(ButtonInteractionEvent event) throws IOException {
        event.replyEmbeds(EmbedUtil.invoiceInProgress()).queue();

        InvoiceDraft invoiceDraft = new InvoiceDraft(this, pluginName, requestedSourceCode ? price + 5 : price, event.getHook());
        invoiceDraft.generateInvoice();
    }

    /**
     * Closes the commission for any reason and removes it from the {@link} & {@link Customer#getCommissions()} list.
     */
    public void close(boolean completed) {
        this.customer.getCommissions().remove(this);
        this.invoices.forEach(Invoice::cancel);

        customer.getTextChannel().retrieveMessageById(infoEmbedId).queue(message -> {
            MessageEmbed messageEmbed = message.getEmbeds().get(0);

            EmbedBuilder completedEmbed = new EmbedBuilder(messageEmbed);
            if (completed) {
                completedEmbed.setTitle(messageEmbed.getTitle() + " (COMPLETED)")
                        .setColor(Color.GREEN)
                        .build();
            } else {
                completedEmbed.setTitle(messageEmbed.getTitle() + " (CANCELLED)")
                        .setColor(Color.RED)
                        .build();
            }
            message.editMessageEmbeds(completedEmbed.build()).queue();
        });
    }

    /**
     * @return A simple predicate determining if the price is within a valid range.
     */
    public boolean checkPrice() {
        return !(price > 0);
    }

    // --------------------------------------- Getters & Setters --------------------------------------- //

    public Customer getCustomer() {
        return customer;
    }

    public String getPluginName() {
        return pluginName;
    }

    public String getInfoEmbedId() {
        return infoEmbedId;
    }

    public double getPrice() {
        return price;
    }

    public double getFinalPrice() {
        double rebasedPrice = price + (requestedSourceCode ? price * 0.05 : 0);
        double tax = rebasedPrice * 0.0725;

        return rebasedPrice + tax;
    }

    public boolean hasRequestedSourceCode() {
        return requestedSourceCode;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public Collection<Invoice> getInvoices() {
        return invoices;
    }

    public void setRequestedSourceCode(boolean requestedSourceCode) {
        this.requestedSourceCode = requestedSourceCode;
    }

    public void setConfirmed(boolean confirmed) {
        this.confirmed = confirmed;
    }

    public void setPrice(double newQuote) {
        this.price = newQuote;
    }

    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("user_id", customer.getUser().getId());
        jsonObject.put("plugin_name", pluginName);
        jsonObject.put("source_code", requestedSourceCode);
        jsonObject.put("confirmed", confirmed);
        jsonObject.put("price", price);
        jsonObject.put("info_embed_id", infoEmbedId);

        JSONArray invoicesArray = new JSONArray();
        invoices.forEach(invoice -> invoicesArray.put(invoice.toJSON()));
        jsonObject.put("invoices", invoicesArray);

        return jsonObject;
    }
}