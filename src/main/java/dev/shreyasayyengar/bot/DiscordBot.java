package dev.shreyasayyengar.bot;

import dev.shreyasayyengar.bot.client.ClientCommission;
import dev.shreyasayyengar.bot.commands.MiscellaneousCommandManager;
import dev.shreyasayyengar.bot.commands.MiscellaneousSlashCommandManager;
import dev.shreyasayyengar.bot.commands.PrivateChannelCommandManager;
import dev.shreyasayyengar.bot.database.MySQL;
import dev.shreyasayyengar.bot.listeners.MemberRemove;
import dev.shreyasayyengar.bot.listeners.MemberUpdatePending;
import dev.shreyasayyengar.bot.listeners.interactions.ButtonClick;
import dev.shreyasayyengar.bot.listeners.interactions.MenuSelect;
import dev.shreyasayyengar.bot.listeners.interactions.ModalSubmit;
import dev.shreyasayyengar.bot.misc.managers.ClientInfoManager;
import dev.shreyasayyengar.bot.misc.managers.ShutdownManager;
import dev.shreyasayyengar.bot.misc.utils.Department;
import dev.shreyasayyengar.bot.paypal.AccessTokenRequest;
import dev.shreyasayyengar.bot.paypal.Invoice;
import dev.shreyasayyengar.bot.properties.MySQLProperty;
import dev.shreyasayyengar.bot.properties.PrimaryDiscordProperty;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * The main class of the Discord Bot. This class is responsible for initializing the bot and
 * constructing/initialising/assigning relevant data, that will later be used during runtime.
 *
 * @author Shreyas Ayyengar
 */
public class DiscordBot {

    private static DiscordBot instance;

    private JDA discordBot;
    private ClientInfoManager clientInfoManager;
    private String paypalAccessToken;

    public Guild workingGuild;
    public MySQL database;

    public static DiscordBot get() {
        return instance;
    }

    public static void log(Department department, String message) {
        System.out.println("[CommissionsManager - " + department.name() + "] " + message);
    }

    public DiscordBot() throws LoginException, InterruptedException, IOException, SQLException {
        instance = this;
        maintainAccessToken();

        initMySQL();
        createBot();
        fixData();
        deserialiseMySQLData();
        initShutdownHook();

        log(Department.Main, "*** CommissionsManager Ready! ***");
        System.gc();
    }

    /**
     * This method generates a new OAuth2 access token for the PayPal API
     * every 5 hours. This is done via HTTP requests to the PayPal API.
     *
     * @author Shreyas Ayyengar
     * @see AccessTokenRequest
     */
    private void maintainAccessToken() {
        ScheduledExecutorService scheduledService = Executors.newSingleThreadScheduledExecutor();
        scheduledService.scheduleAtFixedRate(() -> {
            try {
                this.paypalAccessToken = new AccessTokenRequest().getToken();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, 0, 5, TimeUnit.HOURS);
    }

    /**
     * This method initializes the MySQL database. It also sends
     * keep-alive queries to the database every 30 seconds to prevent
     * the database connection from closing. (Will fix in future)
     *
     * @author Shreyas Ayyengar
     * @see MySQL
     */
    private void initMySQL() {
        try {
            database = new MySQL(
                    MySQLProperty.USERNAME.get(),
                    MySQLProperty.PASSWORD.get(),
                    MySQLProperty.DATABASE.get(),
                    MySQLProperty.HOST.get(),
                    Integer.parseInt(MySQLProperty.PORT.get())
            );

            log(Department.MySQL, "Loading Tables...");
            database.preparedStatement("create table if not exists CM_client_info(" +
                    "    member_id    tinytext     null," +
                    "    text_id      tinytext     null," +
                    "    voice_id     tinytext     null," +
                    "    category_id  tinytext     null," +
                    "    paypal_email tinytext null" +
                    ");").executeUpdate();

            database.preparedStatement("create table if not exists CM_invoice_info(" +
                    "    invoice_id tinytext null," +
                    "    message_id tinytext null," +
                    "    client_id  tinytext null" +

                    ");").executeUpdate();

            database.preparedStatement("create table if not exists CM_commission_info(" +
                    "    holder_id   tinytext null," +
                    "    plugin_name tinytext null," +
                    "    source_code boolean  null," +
                    "    confirmed   boolean  null," +
                    "    price       double   null," +
                    "    info_embed  tinytext null" +
                    ");").executeUpdate();

            Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
                try {
                    database.preparedStatementBuilder("select 1;").executeQuery();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }, 5, 30, TimeUnit.SECONDS);

        } catch (Exception e) {
            log(Department.MySQL, "FATAL ERROR WHEN initMySQL: " + e.getMessage());
            e.printStackTrace();

            System.exit(1);
        }
    }

    /**
     * This method initializes the JDA object, and sets the required data.
     *
     * @author Shreyas Ayyengar
     */
    private void createBot() throws LoginException, InterruptedException {
        this.discordBot = JDABuilder.createDefault(PrimaryDiscordProperty.BOT_TOKEN.get())
                .setActivity(Activity.watching("shreyasayyengar.dev"))
                .addEventListeners(getListeners().toArray())
                .setEnabledIntents(Arrays.stream(GatewayIntent.values()).filter(intent -> intent != GatewayIntent.GUILD_PRESENCES).toList())
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .build().awaitReady();
    }

    private void fixData() {
        this.workingGuild = discordBot.getGuildById(PrimaryDiscordProperty.WORKING_GUILD.get());
        this.workingGuild.loadMembers().get();
        this.clientInfoManager = new ClientInfoManager();
        this.clientInfoManager.registerExistingClients();
    }

    private void deserialiseMySQLData() {
        ClientCommission.registerCommissions();
        Invoice.registerInvoices();
    }

    private void initShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new ShutdownManager());
    }

    // ----------------------------- GETTERS --------------------------------- //

    private Stream<EventListener> getListeners() {
        return Stream.of(
                new MemberUpdatePending(),
                new MemberRemove(),
                new PrivateChannelCommandManager(),
                new MiscellaneousSlashCommandManager(),
                new ButtonClick(),
                new MenuSelect(),
                new ModalSubmit(),
                new MiscellaneousCommandManager()
        );
    }

    public JDA bot() {
        return discordBot;
    }

    public ClientInfoManager getClientManger() {
        return clientInfoManager;
    }

    public String getPaypalAccessToken() {
        return paypalAccessToken;
    }
}