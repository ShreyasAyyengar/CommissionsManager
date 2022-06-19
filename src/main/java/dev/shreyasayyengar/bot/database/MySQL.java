package dev.shreyasayyengar.bot.database;

import dev.shreyasayyengar.bot.DiscordBot;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class MySQL {

    private final String username;
    private final String password;
    private final String database;
    private final String host;
    private final int port;

    private Connection connection;

    public MySQL(String username, String password, String database, String host, int port) {
        this.username = username;
        this.password = password;
        this.database = database;
        this.host = host;
        this.port = port;

        try {
            openConnection();
            DiscordBot.log("[MySQL] Connection to MySQL database established!");
        } catch (SQLException x) {
            DiscordBot.log("[MySQL] There was a problem connecting to the MySQL database!");
            Runtime.getRuntime().exit(-1);
        }
    }

    private void openConnection() throws SQLException {
        if (this.connection != null && !this.connection.isClosed()) {
            return;
        }
        this.connection = DriverManager.getConnection("jdbc:mysql://" + this.host + ":" + this.port + "/" + this.database + "?autoReconnect=true", this.username, this.password);

    }

    public PreparedStatement preparedStatement(String query) {
        PreparedStatement ps = null;
        try {
            ps = this.connection.prepareStatement(query);
        } catch (SQLException x) {
            DiscordBot.log("[MySQL] There was a problem communicating with the MySQL Database!");
        }
        return ps;
    }

    public PreparedStatementBuilder preparedStatementBuilder(String query) throws SQLException {
        return new PreparedStatementBuilder(this.connection, query);
    }
}
