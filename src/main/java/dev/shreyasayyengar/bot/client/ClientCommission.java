package dev.shreyasayyengar.bot.client;

import dev.shreyasayyengar.bot.DiscordBot;
import dev.shreyasayyengar.bot.misc.utils.EmbedUtil;
import dev.shreyasayyengar.bot.paypal.InvoiceDraft;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClientCommission {

    public static final Collection<ClientCommission> COMMISSIONS = new HashSet<>();

    private final ClientInfo client;
    private final String pluginName;

    private boolean requestedSourceCode;
    private boolean confirmed;
    private double price;

    public static void registerCommissions() {
        ExecutorService service = Executors.newSingleThreadExecutor();

        service.execute(() -> {
            try {
                ResultSet resultSet = DiscordBot.get().database.preparedStatement("SELECT * FROM CM_commission_info").executeQuery();

                while (resultSet.next()) {
                    String holderId = resultSet.getString("holder_id");
                    String pluginName = resultSet.getString("plugin_name");
                    boolean requestedSourceCode = resultSet.getBoolean("source_code");
                    boolean confirmed = resultSet.getBoolean("confirmed");
                    double price = resultSet.getDouble("price");

                    new ClientCommission(holderId, pluginName, requestedSourceCode, confirmed, price);
                }

            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });

        service.shutdown();

    }

    public ClientCommission(ClientInfo client, String pluginName, boolean requestedSourceCode) {
        this.client = client;
        this.pluginName = pluginName;
        this.requestedSourceCode = requestedSourceCode;
        this.confirmed = false;

        COMMISSIONS.add(this);
    }

    public ClientCommission(String holderId, String pluginName, boolean requestedSourceCode, boolean confirmed, double price) {
        this.pluginName = pluginName;
        this.requestedSourceCode = requestedSourceCode;
        this.confirmed = confirmed;
        this.price = price;

        this.client = DiscordBot.get().getClientManger().get(holderId);
        this.client.getCommissions().add(this);
        COMMISSIONS.add(this);
    }

    public void generateInvoice(ButtonInteractionEvent event) throws IOException, URISyntaxException {
        event.replyEmbeds(EmbedUtil.invoiceInProgress()).queue();

        if (requestedSourceCode) {
            price += 5;
        }

        InvoiceDraft invoiceDraft = new InvoiceDraft(client, pluginName, price, event.getHook());
        invoiceDraft.generateInvoice();

        client.getInvoices().add(invoiceDraft.getFinalInvoice());
    }

    public void serialise() {
        try {
            // Does the commission exist?
            ResultSet resultSet = DiscordBot.get().database.preparedStatementBuilder("SELECT * FROM CM_commission_info WHERE holder_id = ?")
                    .setString(client.getHolder().getId())
                    .build().executeQuery();

            if (resultSet.next()) {
                DiscordBot.get().database.preparedStatementBuilder("UPDATE CM_commission_info SET plugin_name = ?, source_code = ?, confirmed = ?, price = ? WHERE holder_id = ?")
                        .setString(pluginName)
                        .setBoolean(requestedSourceCode)
                        .setBoolean(confirmed)
                        .setDouble(price)
                        .setString(client.getHolder().getId())
                        .build().executeUpdate();
            } else {
                DiscordBot.get().database.preparedStatementBuilder("insert into CM_commission_info (holder_id, plugin_name, source_code, confirmed, price) values (?, ?, ?, ?, ?);")
                        .setString(client.getHolder().getId())
                        .setString(pluginName)
                        .setBoolean(requestedSourceCode)
                        .setBoolean(confirmed)
                        .setDouble(price)
                        .build().executeUpdate();
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean checkPrice() {
        return !(price > 0);
    }

    // --------------------------------------- Getters & Setters --------------------------------------- //

    public ClientInfo getClient() {
        return client;
    }

    public String getPluginName() {
        return pluginName;
    }

    public double getPrice() {
        return price;
    }

    public boolean hasRequestedSourceCode() {
        return requestedSourceCode;
    }

    public boolean isConfirmed() {
        return confirmed;
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
