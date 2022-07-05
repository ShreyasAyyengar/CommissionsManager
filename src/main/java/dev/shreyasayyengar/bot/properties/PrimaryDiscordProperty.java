package dev.shreyasayyengar.bot.properties;

import java.io.IOException;
import java.util.Properties;

public enum PrimaryDiscordProperty {

    BOT_TOKEN("botToken"),
    WORKING_GUILD("guildId"),
    OWNER_ID("ownerId");

    private final String property;

    PrimaryDiscordProperty(String property) {
        this.property = property;
    }

    public String get() {
        Properties prop = new Properties();
        try {
            prop.load(this.getClass().getResourceAsStream("/properties/discord.properties"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return prop.getProperty(this.property);
    }
}