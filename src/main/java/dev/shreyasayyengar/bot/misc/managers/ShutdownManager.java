package dev.shreyasayyengar.bot.misc.managers;

import dev.shreyasayyengar.bot.DiscordBot;
import dev.shreyasayyengar.bot.client.ClientInfo;
import dev.shreyasayyengar.bot.misc.utils.Department;
import dev.shreyasayyengar.bot.paypal.Invoice;

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
