package com.scc.dao;

import com.scc.model.ActivityLog;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ActivityLogDao {

    /**
     * Records an action performed by a specific user.
     */
    public boolean logAction(int userId, String actionType, String description) {
        return logAction(Integer.valueOf(userId), actionType, description);
    }

    /**
     * A null userId represents a system-level action.
     */
    public boolean logAction(Integer userId, String actionType, String description) {

        if (actionType == null || actionType.isBlank()
                || description == null || description.isBlank()) {
            return false;
        }

        String sql = """
                INSERT INTO activity_logs
                (user_id, action, action_type, description, timestamp)
                VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            if (userId == null) {
                ps.setNull(1, Types.INTEGER);
            } else {
                ps.setInt(1, userId);
            }

            ps.setString(2, actionType + ": " + description);
            ps.setString(3, actionType);
            ps.setString(4, description);

            return ps.executeUpdate() == 1;

        } catch (SQLException e) {
            System.err.println("Activity log insert failed: " + e.getMessage());
            return false;
        }
    }

    public List<ActivityLog> getAllLogs() {
        return fetchLogs(null);
    }

    public List<ActivityLog> getLogsByUserId(int userId) {
        return fetchLogs(userId);
    }

    private List<ActivityLog> fetchLogs(Integer userId) {

        List<ActivityLog> logs = new ArrayList<>();

        String sql = """
                SELECT id, user_id, action_type, description, timestamp
                FROM activity_logs
                """ + (userId == null ? "" : " WHERE user_id = ?")
                + " ORDER BY timestamp DESC, id DESC";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            if (userId != null) {
                ps.setInt(1, userId);
            }

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    int value = rs.getInt("user_id");
                    Integer nullableUserId = rs.wasNull() ? null : value;

                    logs.add(new ActivityLog(
                            rs.getInt("id"),
                            nullableUserId,
                            rs.getString("action_type"),
                            rs.getString("description"),
                            rs.getString("timestamp")
                    ));
                }
            }

        } catch (SQLException e) {
            System.err.println("Activity log retrieval failed: " + e.getMessage());
        }

        return logs;
    }

    /**
     * Deletes all historical logs.
     * Call only after explicit administrator confirmation.
     */
    public boolean clearAllLogs() {

        String sql = "DELETE FROM activity_logs";

        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement()) {

            statement.executeUpdate(sql);
            return true;

        } catch (SQLException e) {
            System.err.println("Activity log clearing failed: " + e.getMessage());
            return false;
        }
    }
}