package com.scc.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnection {

    private static final String URL = "jdbc:sqlite:exchange_assets.db";

    public static Connection getConnection() throws SQLException {

        Connection connection = DriverManager.getConnection(URL);

        try (Statement statement = connection.createStatement()) {

            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("PRAGMA busy_timeout = 5000");

        } catch (SQLException e) {

            connection.close();
            throw e;
        }

        return connection;
    }
}