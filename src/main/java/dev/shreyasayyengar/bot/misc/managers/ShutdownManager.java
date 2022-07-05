package dev.shreyasayyengar.bot.misc.managers;

import dev.shreyasayyengar.bot.DiscordBot;
import dev.shreyasayyengar.bot.client.ClientInfo;
import dev.shreyasayyengar.bot.misc.utils.Department;
import dev.shreyasayyengar.bot.paypal.Invoice;

/**
 * The ShutdownManager is crucial to the programs' functionality. It is used to
 * shut down the bot and save all data to the database. Without the ShutdownManager,
 * the bot will not be able to save data to the database, and will be reset to its
 * default state upon restart. Simple serialisation is called via {@link #serialise()} methods
 * from {@link ClientInfo} & {@link Invoice} classes.
 *
 * <p></p>
 *
 * @author Shreyas Ayyengar
 */
public class ShutdownManager extends Thread {

    @Override
    public void run() {
        DiscordBot.log(Department.ShutdownManager, "Shutting down...");
        DiscordBot.log(Department.ShutdownManager, "[MySQL] Serialising ClientInfo...");
        DiscordBot.get().getClientManger().getMap().values().forEach(ClientInfo::serialise);
        DiscordBot.log(Department.ShutdownManager, "[MySQL] Serialising Commissions...");
        DiscordBot.get().getClientManger().getMap().values().forEach(ClientInfo::serialiseCommissions);
        DiscordBot.log(Department.ShutdownManager, "[MySQL] Serialising Active Invoices...");
        Invoice.INVOICES.forEach(Invoice::serialise);
    }
}