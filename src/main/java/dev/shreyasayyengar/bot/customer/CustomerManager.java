package dev.shreyasayyengar.bot.customer;

import dev.shreyasayyengar.bot.DiscordBot;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.util.HashMap;

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
     * and reconstruct the ClientInfo objects using {@link Customer#Customer(String, String)}
     */
    public void registerExistingCustomers() {
        DiscordBot.get().database.preparedStatementBuilder("SELECT * FROM customer_info;").executeQuery(resultSet -> {
            try {

                while (resultSet.next()) {

                    String member_id = resultSet.getString("member_id");
                    String text_id = resultSet.getString("text_id");
                    String paypal_email = resultSet.getString("paypal_email");

                    if (DiscordBot.get().workingGuild.getMemberById(member_id) != null) {
                        new Customer(member_id, text_id).setPaypalEmail(paypal_email);
                    }
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
    public void purgeMemberSQL(User user) {
        try {
            Customer toPurge = DiscordBot.get().getCustomerManger().getMap().get(user.getId());
            toPurge.getCommissions().forEach(customerCommission -> customerCommission.close(false));

            DiscordBot.get().database.preparedStatementBuilder("DELETE FROM customer_info WHERE member_id = ?;").setString(1, user.getId()).executeUpdate();
        } catch (Exception err) {
            err.printStackTrace();
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