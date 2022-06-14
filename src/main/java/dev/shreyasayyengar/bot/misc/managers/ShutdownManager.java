package dev.shreyasayyengar.bot.misc.managers;

import dev.shreyasayyengar.bot.DiscordBot;
import dev.shreyasayyengar.bot.client.ClientInfo;
import dev.shreyasayyengar.bot.paypal.Invoice;

public class ShutdownManager extends Thread {

    @Override
    public void run() {
        DiscordBot.log("[ShutdownManager] Shutting down...");
        DiscordBot.log("[ShutdownManager] [MySQL] Serialising ClientInfo...");
        DiscordBot.get().getClientManger().getMap().values().forEach(ClientInfo::serialise);
        DiscordBot.log("[ShutdownManager] [MySQL] Serialising Commissions...");
        DiscordBot.get().getClientManger().getMap().values().forEach(ClientInfo::serialiseCommissions);
        DiscordBot.log("[ShutdownManager] [MySQL] Serialising Active Invoices...");
        Invoice.INVOICES.forEach(Invoice::serialise);
    }
}
