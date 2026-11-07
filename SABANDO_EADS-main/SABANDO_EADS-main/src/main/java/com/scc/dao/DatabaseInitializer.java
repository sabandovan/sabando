package com.scc.dao;

import java.sql.*;
import java.util.HashSet;
import java.util.Set;

public class DatabaseInitializer {

    public static void initialize() {

        String usersTable = """
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT NOT NULL UNIQUE,
                    password TEXT NOT NULL,
                    roles TEXT NOT NULL
                        CHECK (roles IN ('ADMIN', 'MANAGER', 'STAFF')),
                    status TEXT NOT NULL DEFAULT 'PENDING'
                        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
                )
                """;

        String assetsTable = """
                CREATE TABLE IF NOT EXISTS assets (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    asset_name TEXT NOT NULL,
                    description TEXT,
                    quantity INTEGER NOT NULL DEFAULT 0,
                    status TEXT NOT NULL DEFAULT 'AVAILABLE',
                    assigned_to INTEGER,
                    FOREIGN KEY (assigned_to)
                        REFERENCES users(id)
                        ON UPDATE CASCADE
                        ON DELETE SET NULL
                )
                """;

        String logsTable = """
                CREATE TABLE IF NOT EXISTS activity_logs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER,
                    action TEXT NOT NULL,
                    action_type TEXT,
                    description TEXT,
                    timestamp TEXT DEFAULT CURRENT_TIMESTAMP,
                    activity_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (user_id)
                        REFERENCES users(id)
                        ON UPDATE CASCADE
                        ON DELETE CASCADE
                )
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement()) {

            statement.execute("PRAGMA foreign_keys = ON");

            statement.execute(usersTable);

            migrateLegacyUserRoles(connection);

            statement.execute(assetsTable);
            statement.execute(logsTable);

            migrateActivityLogColumns(connection);

            statement.execute("""
                    CREATE INDEX IF NOT EXISTS idx_activity_logs_user
                    ON activity_logs(user_id)
                    """);

            statement.execute("""
                    CREATE INDEX IF NOT EXISTS idx_activity_logs_timestamp
                    ON activity_logs(timestamp)
                    """);

            System.out.println("EADS database initialized successfully.");

        } catch (SQLException e) {
            throw new RuntimeException(
                    "EADS database initialization failed: " + e.getMessage(), e
            );
        }
    }

    /**
     * Adds missing Deliverable 8 columns to an older activity_logs table.
     */
    private static void migrateActivityLogColumns(Connection connection)
            throws SQLException {

        Set<String> columns = new HashSet<>();

        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(
                     "PRAGMA table_info(activity_logs)")) {

            while (rs.next()) {
                columns.add(rs.getString("name").toLowerCase());
            }
        }

        try (Statement statement = connection.createStatement()) {

            if (!columns.contains("action")) {
                statement.execute(
                        "ALTER TABLE activity_logs ADD COLUMN action TEXT"
                );
            }

            if (!columns.contains("action_type")) {
                statement.execute(
                        "ALTER TABLE activity_logs ADD COLUMN action_type TEXT"
                );
            }

            if (!columns.contains("description")) {
                statement.execute(
                        "ALTER TABLE activity_logs ADD COLUMN description TEXT"
                );
            }

            if (!columns.contains("timestamp")) {
                statement.execute(
                        "ALTER TABLE activity_logs ADD COLUMN timestamp TEXT"
                );
            }

            if (!columns.contains("activity_time")) {
                statement.execute(
                        "ALTER TABLE activity_logs ADD COLUMN activity_time TIMESTAMP"
                );
            }

            statement.executeUpdate("""
                    UPDATE activity_logs
                    SET action_type = COALESCE(action_type, 'LEGACY_ACTION'),
                        description = COALESCE(description, action, 'Previous activity'),
                        timestamp = COALESCE(
                            timestamp,
                            CAST(activity_time AS TEXT),
                            CURRENT_TIMESTAMP
                        ),
                        action = COALESCE(action, action_type, 'LEGACY_ACTION')
                    """);
        }
    }

    /**
     * Converts older ADMINISTRATOR role constraints to ADMIN.
     */
    private static void migrateLegacyUserRoles(Connection connection)
            throws SQLException {

        String tableSql;

        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT sql FROM sqlite_master WHERE type='table' AND name='users'");
             ResultSet rs = ps.executeQuery()) {

            if (!rs.next()) {
                return;
            }

            tableSql = rs.getString(1);
        }

        if (tableSql == null
                || !tableSql.contains("ADMINISTRATOR")
                || tableSql.contains("'ADMIN', 'MANAGER', 'STAFF'")) {
            return;
        }

        try (Statement statement = connection.createStatement()) {

            statement.execute("PRAGMA foreign_keys = OFF");
            statement.execute("BEGIN");

            try {

                statement.execute("""
                        CREATE TABLE users_new (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            username TEXT NOT NULL UNIQUE,
                            password TEXT NOT NULL,
                            roles TEXT NOT NULL
                                CHECK (roles IN ('ADMIN', 'MANAGER', 'STAFF')),
                            status TEXT NOT NULL DEFAULT 'PENDING'
                                CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
                        )
                        """);

                statement.execute("""
                        INSERT INTO users_new
                        (id, username, password, roles, status)
                        SELECT
                            id,
                            username,
                            password,
                            CASE
                                WHEN roles = 'ADMINISTRATOR' THEN 'ADMIN'
                                ELSE roles
                            END,
                            status
                        FROM users
                        """);

                statement.execute("DROP TABLE users");
                statement.execute("ALTER TABLE users_new RENAME TO users");
                statement.execute("COMMIT");

            } catch (SQLException e) {

                statement.execute("ROLLBACK");
                throw e;

            } finally {
                statement.execute("PRAGMA foreign_keys = ON");
            }
        }
    }
}