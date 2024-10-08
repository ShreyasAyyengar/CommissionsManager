package dev.shreyasayyengar.bot;

import dev.shreyasayyengar.bot.commands.CustomerCommandManager;
import dev.shreyasayyengar.bot.commands.MiscellaneousCommandManager;
import dev.shreyasayyengar.bot.commands.MiscellaneousSlashCommandManager;
import dev.shreyasayyengar.bot.customer.Customer;
import dev.shreyasayyengar.bot.customer.CustomerManager;
import dev.shreyasayyengar.bot.database.MySQL;
import dev.shreyasayyengar.bot.functional.InteractionManager;
import dev.shreyasayyengar.bot.listeners.interactions.MenuSelect;
import dev.shreyasayyengar.bot.listeners.jda.GuildVoiceUpdate;
import dev.shreyasayyengar.bot.listeners.jda.JDAException;
import dev.shreyasayyengar.bot.listeners.jda.MemberRemove;
import dev.shreyasayyengar.bot.listeners.jda.MemberScreeningPass;
import dev.shreyasayyengar.bot.listeners.jda.MemberUpdateName;
import dev.shreyasayyengar.bot.paypal.Invoice;
import dev.shreyasayyengar.bot.utils.Authentication;
import dev.shreyasayyengar.bot.utils.Department;
import dev.shreyasayyengar.bot.utils.EmbedUtil;
import dev.shreyasayyengar.bot.utils.ThreadHandler;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import okhttp3.Credentials;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONObject;

import java.io.IOException;
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

    private CustomerManager customerManager;
    private JDA discordBot;
    public MySQL database;
    private InteractionManager interactionManager;
    public Guild workingGuild;
    private String paypalAccessToken;

    public static DiscordBot get() {
        return instance;
    }

    public static void log(Department department, String message) {
        System.out.println("[CommissionsManager - " + department.name() + "] " + message);
    }

    public DiscordBot() throws InterruptedException {
        instance = this;
        maintainAccessToken();
        initMySQL();
        createBot();
        loadData();
        initThreadHandler();

        log(Department.MAIN, "*** CommissionsManager Ready! ***");
        System.gc();
    }

    /**
     * This method generates a new OAuth2 access token for the PayPal API
     * every 5 hours. This is done via HTTP requests to the PayPal API.
     */
    private void maintainAccessToken() {
        ScheduledExecutorService scheduledService = Executors.newSingleThreadScheduledExecutor();
        scheduledService.scheduleAtFixedRate(() -> this.paypalAccessToken = this.getAccessToken(), 0, 5, TimeUnit.HOURS);

    }

    private String getAccessToken() {
        try {
            OkHttpClient client = new OkHttpClient();
            MediaType JSON = MediaType.get("application/json; charset=utf-8");
            RequestBody body = RequestBody.create("grant_type=client_credentials", JSON);

            Request request = new Request.Builder()
                    .url("https://api-m.paypal.com/v1/oauth2/token")
                    .addHeader("Accept", "application/json")
                    .addHeader("Accept-Language", "en_US")
                    .addHeader("Authorization", Credentials.basic(Authentication.PAYPAL_CLIENT_ID.get(), Authentication.PAYPAL_CLIENT_SECRET.get()))
                    .post(body)
                    .build();

            Response response = client.newCall(request).execute();

            String responseJSON = response.body().string();
            JSONObject decode = new JSONObject(responseJSON);
            response.close();

            return decode.getString("access_token");
        } catch (IOException e) {
            log(Department.PAYPAL, "FATAL ERROR WHEN ATTEMPTING TO GET PAYPAL ACCESS TOKEN: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    /**
     * This method initializes the MySQL database. It also sends
     * keep-alive queries to the database every 30 seconds to prevent
     * the database connection from closing.
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
                    Integer.parseInt(Authentication.MYSQL_PORT.get())
            );
            log(Department.DATABASE, "Loading Tables...");

            Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> this.database.preparedStatementBuilder("select * from customer_data").executeQuery(resultSet -> {}, false), 100, 60, TimeUnit.SECONDS);
        } catch (Exception e) {
            log(Department.DATABASE, "FATAL ERROR WHEN initMySQL: " + e.getMessage());
            e.printStackTrace();

            System.exit(1);
        }
    }

    /**
     * This method initializes the JDA object, and sets the required data.
     */
    private void createBot() throws InterruptedException {
        this.discordBot = JDABuilder.createDefault(Authentication.BOT_TOKEN.get())
                .setActivity(Activity.watching("shreyasayyengar.dev"))
                .addEventListeners(getListeners().toArray())
                .setEnabledIntents(Arrays.stream(GatewayIntent.values()).filter(intent -> intent != GatewayIntent.GUILD_PRESENCES).toList())
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .build().awaitReady();

        this.interactionManager = new InteractionManager();
    }

    /**
     * The fixData method assigns the all private variables of this class
     * such as the Working Discord JDA Guild, and the ClientInfoManager.
     *
     * @see CustomerManager
     */
    private void loadData() {
        this.workingGuild = discordBot.getGuildById(Authentication.GUILD_ID.get());
        this.workingGuild.loadMembers().get();
        this.customerManager = new CustomerManager();
        this.customerManager.registerExistingCustomers();

        Invoice.pollInvoices();
    }

    private void initThreadHandler() {
        ThreadHandler handler = new ThreadHandler();
        Runtime.getRuntime().addShutdownHook(handler);
        Thread.currentThread().setUncaughtExceptionHandler(handler.getUncaughtExceptionHandler());
    }

    public void shutdown() {
        DiscordBot.log(Department.SHUTDOWN_MANAGER, "Shutting down...");

        DiscordBot.log(Department.SHUTDOWN_MANAGER, "[MySQL] Serialising Customers, Commissions, and Invoices...");
        DiscordBot.get().getCustomerManger().getMap().values().forEach(Customer::serialise);

        DiscordBot.get().discordBot.getTextChannelById("997328980086632448").sendMessageEmbeds(EmbedUtil.dataSaved()).queue();

        DiscordBot.log(Department.SHUTDOWN_MANAGER, "Shutting down JDA...");
        DiscordBot.get().discordBot.getTextChannelById("997328980086632448").sendMessageEmbeds(EmbedUtil.botShutdown()).queue();
        DiscordBot.get().discordBot.shutdown();

        DiscordBot.log(Department.SHUTDOWN_MANAGER, "CommissionsManager has been shut down.");
    }

    // ----------------------------- GETTERS --------------------------------- //

    private Stream<EventListener> getListeners() {
        return Stream.of(
                new MemberScreeningPass(),
                new MemberRemove(),
                new MemberUpdateName(),
                new GuildVoiceUpdate(),
                new MenuSelect(),
                new CustomerCommandManager(),
                new MiscellaneousSlashCommandManager(),
                new MiscellaneousCommandManager(),
                new JDAException()
        );
    }

    public JDA bot() {
        return discordBot;
    }

    public CustomerManager getCustomerManger() {
        return customerManager;
    }

    public String getPaypalAccessToken() {
        return paypalAccessToken;
    }

    public InteractionManager getInteractionManager() {
        return interactionManager;
    }
}