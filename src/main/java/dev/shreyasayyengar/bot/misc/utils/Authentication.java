package dev.shreyasayyengar.bot.misc.utils;

public enum Authentication {

    BOT_TOKEN,
    GUILD_ID,
    OWNER_ID,
    MYSQL_USERNAME,
    MYSQL_PASSWORD,
    MYSQL_DATABASE,
    MYSQL_HOST,
    PAYPAL_CLIENT_ID,
    PAYPAL_CLIENT_SECRET;

    public String get() {
        return System.getenv(this.name());
    }
}