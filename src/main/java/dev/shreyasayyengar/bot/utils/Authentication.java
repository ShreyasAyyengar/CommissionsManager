package dev.shreyasayyengar.bot.utils;

public enum Authentication {
    BOT_TOKEN,
    GUILD_ID,
    OWNER_ID,
    MYSQL_USERNAME,
    MYSQL_PASSWORD,
    MYSQL_DATABASE,
    MYSQL_HOST,
    MYSQL_PORT,
    PAYPAL_CLIENT_ID,
    PAYPAL_CLIENT_SECRET;

    public String get() {
        return System.getenv(this.name());
    }
}