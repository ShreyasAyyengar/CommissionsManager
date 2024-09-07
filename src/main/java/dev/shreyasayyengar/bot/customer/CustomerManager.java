package dev.shreyasayyengar.bot.customer;

import dev.shreyasayyengar.bot.DiscordBot;
import dev.shreyasayyengar.bot.paypal.Invoice;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

/**
 * The ClientInfoManager, instantiated in the {@link DiscordBot} class, is used to manage ClientInfo objects.
 *
 * <p></p>
 *
 * @author Shreyas Ayyengar
 */
public class CustomerManager {

    private final HashMap<String, Customer> customerMap = new HashMap<>();

    /**
     * The registerExistingClients is used to pull data from the provided MySQL database
     * and reconstruct the ClientInfo objects using {@link Customer#Customer(String, String, String, Collection)}
     */
    public void registerExistingCustomers() {
        DiscordBot.get().database.preparedStatementBuilder("SELECT * FROM customer_data;").executeQuery(resultSet -> {
            try {
                while (resultSet.next()) {
                    JSONObject data = new JSONObject(resultSet.getString("data"));
                    String userId = data.getString("user_id");
                    String textId = data.getString("text_id");
                    String paypalEmail = data.optString("paypal_email", null);

                    Customer customer = new Customer(userId, textId, paypalEmail, new HashSet<>());

                    data.optJSONArray("commissions", new JSONArray()).forEach(entryJSONCommission -> {
                        JSONObject commission = (JSONObject) entryJSONCommission;
                        CustomerCommission customerCommission = new CustomerCommission(
                                userId,
                                commission.getString("plugin_name"),
                                commission.getBoolean("source_code"),
                                commission.getBoolean("confirmed"),
                                commission.getDouble("price"),
                                commission.getString("info_embed_id")
                        );

                        customer.getCommissions().add(customerCommission);

                        commission.optJSONArray("invoices", new JSONArray()).forEach(entryJSONInvoice -> {
                            JSONObject invoice = (JSONObject) entryJSONInvoice;
                            String invoiceId = invoice.getString("invoice_id");
                            String messageId = invoice.getString("message_id");

                            customerCommission.getInvoices().add(new Invoice(userId, customerCommission.getPluginName(), messageId, invoiceId));
                        });
                    });
                }
            } catch (Exception err) {
                err.printStackTrace();
            }
        });
    }

    /**
     * This method purges all information tied to a {@link Customer} (if any) from the {@link User}
     * passed in. This also removed any information from the MySQL database.
     */
    public void handleRemoval(User user) {
        try {
            Customer toPurge = customerMap.remove(user.getId());
            toPurge.getCommissions().forEach(customerCommission -> customerCommission.close(false));

            DiscordBot.get().database.preparedStatementBuilder("DELETE FROM customer_data WHERE user_id = ?;").setString(1, user.getId()).executeUpdate();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Gets the {@link Customer} object associated with the provided {@link TextChannel}
     */
    public Customer getByTextChannel(TextChannel textChannel) {
        for (Customer customer : customerMap.values()) {
            if (customer.getTextChannel().getId().equalsIgnoreCase(textChannel.getId())) {
                return customer;
            }
        }
        return null;
    }

    /**
     * Gets the ClientInfo object associated with the provided user ID.
     */
    public Customer get(String id) {
        return customerMap.get(id);
    }

    public HashMap<String, Customer> getMap() {
        return customerMap;
    }

    public void add(String id, Customer customer) {
        customerMap.put(id, customer);
    }

    public boolean containsInfoOf(String id) {
        return customerMap.containsKey(id);
    }
}