package com.scc.dao;

import com.scc.model.User;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

public class UserDao {

    private static final int ITERATIONS = 210000;
    private static final int KEY_LENGTH = 256;

    private static final List<String> VALID_ROLES =
            List.of("ADMIN", "MANAGER", "STAFF");

    private static final List<String> VALID_STATUSES =
            List.of("PENDING", "APPROVED", "REJECTED");

    private final ActivityLogDao activityLogDao = new ActivityLogDao();

    // ==========================================
    // PASSWORD HASHING
    // ==========================================

    private String hashPassword(String password) throws Exception {

        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);

        PBEKeySpec spec = new PBEKeySpec(
                password.toCharArray(),
                salt,
                ITERATIONS,
                KEY_LENGTH
        );

        try {

            byte[] hash = SecretKeyFactory
                    .getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec)
                    .getEncoded();

            return "PBKDF2$" + ITERATIONS + "$"
                    + Base64.getEncoder().encodeToString(salt) + "$"
                    + Base64.getEncoder().encodeToString(hash);

        } finally {
            spec.clearPassword();
        }
    }

    private boolean verifyPassword(String password, String stored)
            throws Exception {

        if (stored == null || !stored.startsWith("PBKDF2$")) {
            return false;
        }

        String[] parts = stored.split("\\$");

        if (parts.length != 4) {
            return false;
        }

        int iterations = Integer.parseInt(parts[1]);

        if (iterations <= 0 || iterations > 1000000) {
            return false;
        }

        byte[] salt = Base64.getDecoder().decode(parts[2]);
        byte[] expected = Base64.getDecoder().decode(parts[3]);

        PBEKeySpec spec = new PBEKeySpec(
                password.toCharArray(),
                salt,
                iterations,
                expected.length * 8
        );

        try {

            byte[] actual = SecretKeyFactory
                    .getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec)
                    .getEncoded();

            return MessageDigest.isEqual(expected, actual);

        } finally {
            spec.clearPassword();
        }
    }

    // ==========================================
    // REGISTER USER
    // ==========================================

    public boolean register(String username, String password, String role) {

        if (username == null || username.isBlank()
                || password == null || password.isBlank()) {

            System.out.println("Username and password are required.");
            return false;
        }

        if (role == null) {
            return false;
        }

        role = role.trim().toUpperCase(Locale.ROOT);

        if (!VALID_ROLES.contains(role)) {
            System.out.println("Invalid user role.");
            return false;
        }

        String sql = """
                INSERT INTO users (username, password, roles, status)
                VALUES (?, ?, ?, 'PENDING')
                """;

        int generatedId = -1;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, username.trim());
            ps.setString(2, hashPassword(password));
            ps.setString(3, role);

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    generatedId = keys.getInt(1);
                }
            }

        } catch (SQLException e) {

            if (e.getMessage() != null
                    && e.getMessage().toUpperCase(Locale.ROOT).contains("UNIQUE")) {

                System.out.println("Username already exists.");

            } else {
                System.out.println("Registration error: " + e.getMessage());
            }

            return false;

        } catch (Exception e) {
            System.out.println("Password processing error: " + e.getMessage());
            return false;
        }

        if (generatedId > 0) {
            activityLogDao.logAction(
                    generatedId,
                    "USER_REGISTERED",
                    "User " + username.trim()
                            + " registered with role " + role
                            + "; awaiting approval."
            );
        }

        System.out.println(
                "Registration successful! Account status: PENDING."
        );
        System.out.println("Wait for administrator approval.");

        return true;
    }

    // ==========================================
    // CHECK APPROVED ADMIN
    // ==========================================

    public boolean hasAdminAccount() {

        String sql = """
                SELECT COUNT(*)
                FROM users
                WHERE roles = 'ADMIN'
                AND status = 'APPROVED'
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            return rs.next() && rs.getInt(1) > 0;

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Unable to check administrator account.", e
            );
        }
    }

    // ==========================================
    // CREATE INITIAL ADMIN
    // ==========================================

    public void createInitialAdmin(String username, String password) {

        if (username == null || username.isBlank()
                || password == null || password.isBlank()) {

            System.out.println("Admin username and password are required.");
            return;
        }

        if (hasAdminAccount()) {
            System.out.println("An approved ADMIN account already exists.");
            return;
        }

        String sql = """
                INSERT INTO users (username, password, roles, status)
                VALUES (?, ?, 'ADMIN', 'APPROVED')
                """;

        int generatedId = -1;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, username.trim());
            ps.setString(2, hashPassword(password));

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    generatedId = keys.getInt(1);
                }
            }

        } catch (Exception e) {
            System.out.println("Initial ADMIN setup failed: " + e.getMessage());
            return;
        }

        if (generatedId > 0) {
            activityLogDao.logAction(
                    generatedId,
                    "ADMIN_CREATED",
                    "Initial administrator account created."
            );
        }

        System.out.println("Initial ADMIN account created and approved.");
    }

    // ==========================================
    // LOGIN
    // ==========================================

    public User login(String username, String password) {

        if (username == null || username.isBlank()
                || password == null || password.isBlank()) {

            System.out.println("Username and password are required.");
            return null;
        }

        String sql = """
                SELECT id, username, password, roles, status
                FROM users
                WHERE username = ?
                """;

        User user = null;
        String logAction = null;
        String logDescription = null;
        Integer logUserId = null;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setString(1, username.trim());

            try (ResultSet rs = ps.executeQuery()) {

                if (!rs.next()) {
                    System.out.println("Account not found.");
                    return null;
                }

                int id = rs.getInt("id");
                String storedPassword = rs.getString("password");
                String status = rs.getString("status");

                logUserId = id;

                if (!"APPROVED".equalsIgnoreCase(status)) {

                    logAction = "LOGIN_BLOCKED";
                    logDescription = "Login blocked because account status is "
                            + status + ".";

                    System.out.println(
                            "Account is " + status
                                    + ". Please wait for approval."
                    );

                } else if (!verifyPassword(password, storedPassword)) {

                    logAction = "LOGIN_FAILED";
                    logDescription = "Incorrect password entered.";

                    System.out.println("Incorrect password.");

                } else {

                    user = new User(
                            id,
                            rs.getString("username"),
                            storedPassword,
                            rs.getString("roles"),
                            status
                    );

                    logAction = "USER_LOGIN";
                    logDescription = "User " + user.username()
                            + " logged in successfully.";

                    System.out.println("Login successful!");
                }
            }

        } catch (Exception e) {
            System.out.println("Login error: " + e.getMessage());
            return null;
        }

        // Log after the login connection is closed.
        if (logAction != null && logUserId != null) {
            activityLogDao.logAction(
                    logUserId,
                    logAction,
                    logDescription
            );
        }

        return user;
    }

    // ==========================================
    // GET ALL USERS
    // ==========================================

    public List<User> getAllUsers() {

        String sql = """
                SELECT id, username, password, roles, status
                FROM users
                ORDER BY id
                """;

        return getUsers(sql);
    }

    // ==========================================
    // GET PENDING USERS
    // ==========================================

    public List<User> getPendingUsers() {

        String sql = """
                SELECT id, username, password, roles, status
                FROM users
                WHERE status = 'PENDING'
                ORDER BY id
                """;

        return getUsers(sql);
    }

    private List<User> getUsers(String sql) {

        List<User> users = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {

                users.add(new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("roles"),
                        rs.getString("status")
                ));
            }

        } catch (SQLException e) {
            System.out.println("Unable to retrieve users: " + e.getMessage());
        }

        return users;
    }

    // ==========================================
    // APPROVE USER
    // ==========================================

    public boolean approveUser(int userId) {
        return approveUser(userId, (Integer) null);
    }

    public boolean approveUser(int userId, int adminId) {
        return approveUser(userId, Integer.valueOf(adminId));
    }

    private boolean approveUser(int userId, Integer adminId) {

        String sql = """
                UPDATE users
                SET status = 'APPROVED'
                WHERE id = ?
                AND status = 'PENDING'
                """;

        boolean success;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setInt(1, userId);
            success = ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("Approval error: " + e.getMessage());
            return false;
        }

        if (success) {
            activityLogDao.logAction(
                    adminId,
                    "USER_APPROVED",
                    "Admin " + (adminId == null ? "SYSTEM" : "ID " + adminId)
                            + " approved account for User ID " + userId + "."
            );
        }

        return success;
    }

    // ==========================================
    // UPDATE USER STATUS
    // ==========================================

    public boolean updateUserStatus(int userId, String newStatus) {
        return updateUserStatus(userId, newStatus, (Integer) null);
    }

    public boolean updateUserStatus(
            int userId, String newStatus, int adminId) {

        return updateUserStatus(
                userId, newStatus, Integer.valueOf(adminId)
        );
    }

    private boolean updateUserStatus(
            int userId, String newStatus, Integer adminId) {

        if (newStatus == null) {
            return false;
        }

        newStatus = newStatus.trim().toUpperCase(Locale.ROOT);

        if (!VALID_STATUSES.contains(newStatus)) {
            System.out.println("Invalid user status.");
            return false;
        }

        String sql = "UPDATE users SET status = ? WHERE id = ?";
        boolean success;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setString(1, newStatus);
            ps.setInt(2, userId);

            success = ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("Status update error: " + e.getMessage());
            return false;
        }

        if (success) {
            activityLogDao.logAction(
                    adminId,
                    "USER_STATUS_UPDATED",
                    "Admin " + (adminId == null ? "SYSTEM" : "ID " + adminId)
                            + " changed status of User " + userId
                            + " to " + newStatus + "."
            );
        }

        return success;
    }

    // ==========================================
    // UPDATE USER ROLE AND STATUS
    // ==========================================

    public boolean updateUserRoleAndStatus(
            int userId, String newRole, String newStatus) {

        return updateUserRoleAndStatus(
                userId, newRole, newStatus, (Integer) null
        );
    }

    public boolean updateUserRoleAndStatus(
            int userId, String newRole, String newStatus, int adminId) {

        return updateUserRoleAndStatus(
                userId, newRole, newStatus, Integer.valueOf(adminId)
        );
    }

    private boolean updateUserRoleAndStatus(
            int userId,
            String newRole,
            String newStatus,
            Integer adminId) {

        if (newRole == null || newStatus == null) {
            return false;
        }

        newRole = newRole.trim().toUpperCase(Locale.ROOT);
        newStatus = newStatus.trim().toUpperCase(Locale.ROOT);

        if (!VALID_ROLES.contains(newRole)
                || !VALID_STATUSES.contains(newStatus)) {

            System.out.println("Invalid role or status.");
            return false;
        }

        String sql = """
                UPDATE users
                SET roles = ?, status = ?
                WHERE id = ?
                """;

        boolean success;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setString(1, newRole);
            ps.setString(2, newStatus);
            ps.setInt(3, userId);

            success = ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("User update error: " + e.getMessage());
            return false;
        }

        if (success) {
            activityLogDao.logAction(
                    adminId,
                    "USER_UPDATED",
                    "Admin " + (adminId == null ? "SYSTEM" : "ID " + adminId)
                            + " changed role/status of User " + userId
                            + " to " + newRole + " / " + newStatus + "."
            );
        }

        return success;
    }
}