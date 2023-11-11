package dev.shreyasayyengar.bot.customer;

import dev.shreyasayyengar.bot.DiscordBot;
import dev.shreyasayyengar.bot.misc.utils.EmbedUtil;
import dev.shreyasayyengar.bot.paypal.Invoice;
import dev.shreyasayyengar.bot.paypal.InvoiceDraft;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

import java.io.IOException;
import java.sql.SQLException;
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

    public static final Collection<CustomerCommission> COMMISSIONS = new HashSet<>();

    private final Collection<Invoice> invoices = new HashSet<>();

    private final Customer customer;
    private final String pluginName;
    private final String infoEmbed;

    private boolean requestedSourceCode;
    private boolean confirmed;
    private double price;

    /**
     * Registers all serialise commissions from the MySQL database and loads them up
     * and assigns them to the corresponding {@link Customer} object.
     */
    public static void registerCommissions() {
        DiscordBot.get().database.preparedStatementBuilder("SELECT * FROM CM_commission_info").executeQuery(resultSet -> {
            try {
                while (resultSet.next()) {
                    String holderId = resultSet.getString("holder_id");
                    String pluginName = resultSet.getString("plugin_name");
                    boolean requestedSourceCode = resultSet.getBoolean("source_code");
                    boolean confirmed = resultSet.getBoolean("confirmed");
                    double price = resultSet.getDouble("price");
                    String infoEmbed = resultSet.getString("info_embed");

                    new CustomerCommission(holderId, pluginName, requestedSourceCode, confirmed, price, infoEmbed);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Constructs a new CustomerCommission object during Runtime. <b>This method should not be called
     * if the Commission is not being generated live.</b> i.e. only when created via the <code>/request</code>
     * command used by users in the Discord server.
     * <p></p>
     *
     * @param customer              The {@link Customer} object that the commission is linked to.
     * @param pluginName          The name of the plugin that the commission is for.
     * @param requestedSourceCode Whether the customer has requested source code.
     * @param infoEmbed           The {@link net.dv8tion.jda.api.entities.MessageEmbed} that contains the info for the commission.
     */
    public CustomerCommission(Customer customer, String pluginName, boolean requestedSourceCode, String infoEmbed) {
        this.customer = customer;
        this.pluginName = pluginName;
        this.requestedSourceCode = requestedSourceCode;
        this.confirmed = false;
        this.infoEmbed = infoEmbed;

        COMMISSIONS.add(this);
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
     * @param infoEmbed           The {@link net.dv8tion.jda.api.entities.MessageEmbed} message ID that contains the info for the commission.
     */
    public CustomerCommission(String holderId, String pluginName, boolean requestedSourceCode, boolean confirmed, double price, String infoEmbed) {
        this.pluginName = pluginName;
        this.requestedSourceCode = requestedSourceCode;
        this.confirmed = confirmed;
        this.price = price;
        this.infoEmbed = infoEmbed;

        this.customer = DiscordBot.get().getCustomerManger().get(holderId);
        this.customer.getCommissions().add(this);
        COMMISSIONS.add(this);
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
     * Serialises the Commission and vital data to the MySQL database.
     */
    public void serialise() {
        try {
            // Does the commission exist?
            DiscordBot.get().database.preparedStatementBuilder("SELECT * FROM CM_commission_info WHERE holder_id = ? AND plugin_name = ?")
                    .setString(customer.getHolder().getId())
                    .setString(pluginName).executeQuery(resultSet -> {
                        try {
                            if (resultSet.next()) {
                                DiscordBot.get().database.preparedStatementBuilder("UPDATE CM_commission_info SET plugin_name = ?, source_code = ?, confirmed = ?, price = ?, info_embed = ? WHERE holder_id = ? AND plugin_name = ?")
                                        .setString(pluginName)
                                        .setBoolean(requestedSourceCode)
                                        .setBoolean(confirmed)
                                        .setDouble(price)
                                        .setString(infoEmbed)
                                        .setString(customer.getHolder().getId())
                                        .setString(pluginName)
                                        .build().executeUpdate();
                            } else {
                                DiscordBot.get().database.preparedStatementBuilder("insert into CM_commission_info (holder_id, plugin_name, source_code, confirmed, price, info_embed) values (?, ?, ?, ?, ?, ?);")
                                        .setString(customer.getHolder().getId())
                                        .setString(pluginName)
                                        .setBoolean(requestedSourceCode)
                                        .setBoolean(confirmed)
                                        .setDouble(price)
                                        .setString(infoEmbed)
                                        .build().executeUpdate();
                            }
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    });


        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Closes the commission for any reason and removes it from the {@link #COMMISSIONS} & {@link Customer#getCommissions()} list.
     */
    public void close() {
        COMMISSIONS.remove(this);
        this.customer.getCommissions().remove(this);
        this.invoices.forEach(Invoice::cancel);

        try {
            DiscordBot.get().database.preparedStatementBuilder("DELETE FROM CM_commission_info WHERE holder_id = ?")
                    .setString(customer.getHolder().getId()).executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
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

    public String getInfoEmbed() {
        return infoEmbed;
    }

    public double getPrice() {
        return price;
    }

    public double getFinalPrice() {
        double rebasedPrice = price + 0.30 + (requestedSourceCode ? 5 : 0);
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
}