package dev.shreyasayyengar.bot.properties;

import java.io.IOException;
import java.util.Properties;

public enum MySQLProperty {

    USERNAME("username"),
    PASSWORD("password"),
    DATABASE("database"),
    HOST("host"),
    PORT("port");

    private final String property;

    MySQLProperty(String property) {
        this.property = property;
    }

    public String get() throws IOException {
        Properties prop = new Properties();
        prop.load(this.getClass().getResourceAsStream("/properties/mysql.properties"));

        return prop.getProperty(this.property);
    }
}

