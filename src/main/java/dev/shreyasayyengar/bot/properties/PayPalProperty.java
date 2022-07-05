package dev.shreyasayyengar.bot.properties;

import java.io.IOException;
import java.util.Properties;

public enum PayPalProperty {

    CLIENT_ID("clientId"),
    CLIENT_SECRET("clientSecret"),
    WEBSITE("website"),
    EMAIL("email");

    private final String property;

    PayPalProperty(String property) {
        this.property = property;
    }

    public String get() {
        Properties prop = new Properties();
        try {
            prop.load(this.getClass().getResourceAsStream("/properties/paypal.properties"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return prop.getProperty(this.property);
    }
}