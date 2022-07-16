package dev.shreyasayyengar.bot;

import dev.shreyasayyengar.bot.client.ClientCommission;
import dev.shreyasayyengar.bot.commands.MiscellaneousCommandManager;
import dev.shreyasayyengar.bot.commands.MiscellaneousSlashCommandManager;
import dev.shreyasayyengar.bot.commands.PrivateChannelCommandManager;
import dev.shreyasayyengar.bot.database.MySQL;
import dev.shreyasayyengar.bot.listeners.JDAException;
import dev.shreyasayyengar.bot.listeners.MemberRemove;
import dev.shreyasayyengar.bot.listeners.MemberUpdatePending;
import dev.shreyasayyengar.bot.listeners.interactions.ButtonClick;
import dev.shreyasayyengar.bot.listeners.interactions.MenuSelect;
import dev.shreyasayyengar.bot.listeners.interactions.ModalSubmit;
import dev.shreyasayyengar.bot.misc.managers.ClientInfoManager;
import dev.shreyasayyengar.bot.misc.managers.ThreadHandler;
import dev.shreyasayyengar.bot.misc.utils.Authentication;
import dev.shreyasayyengar.bot.misc.utils.Department;
import dev.shreyasayyengar.bot.paypal.AccessTokenRequest;
import dev.shreyasayyengar.bot.paypal.Invoice;
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

    private static DiscordBot instance; // Ths instance of this class. (Singleton)

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

    public DiscordBot() throws LoginException, InterruptedException {
        instance = this;
        maintainAccessToken();

        initMySQL();
        createBot();
        fixData();
        deserialiseMySQLData();
        initThreadHandler();

        log(Department.Main, "*** CommissionsManager Ready! ***");
        System.gc();
    }

    /**
     * This method generates a new OAuth2 access token for the PayPal API
     * every 5 hours. This is done via HTTP requests to the PayPal API.
     *
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
     * the database connection from closing. TODO: (Will fix in future)
     *
     * @see MySQL
     */
    private void initMySQL() {
        try {
            database = new MySQL(
                    Authentication.MYSQL_USERNAME.get(),
                    Authentication.MYSQL_PASSWORD.get(),
                    Authentication.MYSQL_DATABASE.get(),
                    Authentication.MYSQL_HOST.get(),
                    3306
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
     */
    private void createBot() throws LoginException, InterruptedException {
        this.discordBot = JDABuilder.createDefault(Authentication.BOT_TOKEN.get())
                .setActivity(Activity.watching("shreyasayyengar.dev"))
                .addEventListeners(getListeners().toArray())
                .setEnabledIntents(Arrays.stream(GatewayIntent.values()).filter(intent -> intent != GatewayIntent.GUILD_PRESENCES).toList())
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .build().awaitReady();
    }

    /**
     * The fixData method assigns the all private variables of this class
     * such as the Working Discord JDA Guild, and the ClientInfoManager.
     *
     * @see ClientInfoManager
     */
    private void fixData() {
        this.workingGuild = discordBot.getGuildById(Authentication.GUILD_ID.get());
        this.workingGuild.loadMembers().get();
        this.clientInfoManager = new ClientInfoManager();
        this.clientInfoManager.registerExistingClients();
    }

    /**
     * This method deserialises the data from the MySQL database and stores
     * them in respective objects and lists.
     *
     * @see ClientInfoManager
     * @see Invoice#registerInvoices()
     */
    private void deserialiseMySQLData() {
        ClientCommission.registerCommissions();
        Invoice.registerInvoices();
    }

    /**
     * This method initializes the ThreadHandler, which is used to
     * properly close the MySQL database connection, serialise
     * any important data for the next time the bot is started,
     * and manage stacktraces or errors that may occur during
     * runtime.
     *
     * @see ThreadHandler
     */
    private void initThreadHandler() {
        ThreadHandler handler = new ThreadHandler();
        Runtime.getRuntime().addShutdownHook(handler);
        Thread.currentThread().setUncaughtExceptionHandler(handler.getUncaughtExceptionHandler());
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
                new MiscellaneousCommandManager(),
                new JDAException()
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

// TODO: Consider migrating Authentication#OWNER_ID to Guild#getOwner()#getId()