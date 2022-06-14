package dev.shreyasayyengar.bot;

import dev.shreyasayyengar.bot.client.ClientCommission;
import dev.shreyasayyengar.bot.commands.MiscellaneousCommandManager;
import dev.shreyasayyengar.bot.commands.MiscellaneousSlashCommandManager;
import dev.shreyasayyengar.bot.commands.PrivateChannelCommandManager;
import dev.shreyasayyengar.bot.database.MySQL;
import dev.shreyasayyengar.bot.listeners.MemberUpdatePending;
import dev.shreyasayyengar.bot.listeners.interactions.ButtonClick;
import dev.shreyasayyengar.bot.listeners.interactions.MenuSelect;
import dev.shreyasayyengar.bot.listeners.interactions.ModalSubmit;
import dev.shreyasayyengar.bot.misc.managers.ClientInfoManager;
import dev.shreyasayyengar.bot.misc.managers.ShutdownManager;
import dev.shreyasayyengar.bot.paypal.AccessTokenRequest;
import dev.shreyasayyengar.bot.paypal.Invoice;
import dev.shreyasayyengar.bot.properties.MySQLProperty;
import dev.shreyasayyengar.bot.properties.PrimaryDiscordProperty;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
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

public class DiscordBot {

    private static DiscordBot instance;

    private String paypalAccessToken;

    public Guild workingGuild;
    public Member owner; // TODO use this
    public MySQL database;

    private JDA discordBot;
    private ClientInfoManager clientInfoManager;

    public static void log(String message) {
        System.out.println("[CommissionsManager] " + message);
    }

    public DiscordBot() throws LoginException, InterruptedException, IOException, SQLException {
        instance = this;
        maintainAccessToken();

        initMySQLConnection();
        createBot();
        fixData();
        deserialiseMySQLData();

        initShutdownHook();

        log("Bot is *truly* ready!");
        System.gc();
    }

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

    private void initMySQLConnection() throws IOException, SQLException {

        database = new MySQL(
                MySQLProperty.USERNAME.get(),
                MySQLProperty.PASSWORD.get(),
                MySQLProperty.DATABASE.get(),
                MySQLProperty.HOST.get(),
                Integer.parseInt(MySQLProperty.PORT.get())
        );

        log("[NySQL] Loading Tables...");
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
                "    client_id tinytext null" +

                ");").executeUpdate();

        database.preparedStatement("create table if not exists CM_commission_info(" +
                "    holder_id   tinytext null," +
                "    plugin_name tinytext null," +
                "    source_code boolean  null," +
                "    confirmed   boolean  null," +
                "    price       double   null" +
                ");").executeUpdate();
    }

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
        this.owner = workingGuild.getOwner();
        this.workingGuild.loadMembers().get();
        this.clientInfoManager = new ClientInfoManager();
    }

    private void deserialiseMySQLData() {
        ScheduledExecutorService scheduledService = Executors.newSingleThreadScheduledExecutor();
        scheduledService.schedule(ClientCommission::registerCommissions, 1, TimeUnit.SECONDS);

        Invoice.registerInvoices();
    }

    private void initShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new ShutdownManager());
    }

    // ----------------------------- GETTERS --------------------------------- //

    private Stream<EventListener> getListeners() {
        return Stream.of(
                new MemberUpdatePending(),
//                new MemberVoiceUpdate(),
                new PrivateChannelCommandManager(),
                new MiscellaneousSlashCommandManager(),
                new MiscellaneousCommandManager(),
                new ButtonClick(),
                new MenuSelect(),
                new ModalSubmit()
//                new MiscellaneousCommandManager()
        );
    }

    public JDA bot() {
        return discordBot;
    }

    public static DiscordBot get() {
        return instance;
    }

    public ClientInfoManager getClientManger() {
        return clientInfoManager;
    }

    public String getPaypalAccessToken() {
        return paypalAccessToken;
    }
}

// TODO: figure out what on EARTH happened when registering ClientInfo through the ClientInfoManager and then subsequently registering ClientCommissions
// Ideas: 1. Remove all executor servers and see what happens, (completely remove them from the startup proess, check running order)
//        2. Hold a reference to the Thread ExecutorService in DiscordBot and then call it when making SQL requests
//        3. Make ClientInfoManager more of a ClientManager, that registeres ClientInfo AND THEN registers ClientCommissions
//        4. Make a new class that extends Thread and then call all startup MySQL requests and register ClientInfo & ClientCommissions in it

// TODO: System.gc(); literally reduced RAM by like 10MB so worth looking into