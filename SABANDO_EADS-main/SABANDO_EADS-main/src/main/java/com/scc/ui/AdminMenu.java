package com.scc.ui;

import com.scc.dao.ActivityLogDao;
import com.scc.dao.UserDao;
import com.scc.model.ActivityLog;
import com.scc.model.User;

import java.util.List;
import java.util.Locale;
import java.util.Scanner;

public class AdminMenu {

    private final Scanner scanner;
    private final UserDao userDao;
    private final ActivityLogDao activityLogDao;
    private final User currentAdmin;

    public AdminMenu(Scanner scanner, User currentAdmin) {

        this.scanner = scanner;
        this.currentAdmin = currentAdmin;

        this.userDao = new UserDao();
        this.activityLogDao = new ActivityLogDao();
    }

    // ==========================================
    // ADMIN MAIN MENU
    // ==========================================

    public void start() {

        boolean running = true;

        while (running) {

            System.out.println("\n========================================");
            System.out.println("          EADS ADMIN DASHBOARD");
            System.out.println("========================================");
            System.out.println("Welcome, " + currentAdmin.username());
            System.out.println("----------------------------------------");
            System.out.println("1. View All System Users");
            System.out.println("2. View Pending Registrations");
            System.out.println("3. Approve User Account");
            System.out.println("4. Update User Role and Status");
            System.out.println("5. View Account Information");
            System.out.println("6. View System Audit Logs");
            System.out.println("0. Logout");
            System.out.println("----------------------------------------");

            int choice = readIntInput("Enter choice: ");

            switch (choice) {

                case 1 -> viewAllUsers();

                case 2 -> viewPendingUsers();

                case 3 -> approveUser();

                case 4 -> updateUser();

                case 5 -> viewAccountInformation();

                case 6 -> auditLogMenu();

                case 0 -> {
                    activityLogDao.logAction(
                            currentAdmin.id(),
                            "USER_LOGOUT",
                            "Admin " + currentAdmin.username()
                                    + " logged out."
                    );

                    System.out.println("Admin logged out successfully.");
                    running = false;
                }

                default ->
                        System.out.println("Invalid choice! Please try again.");
            }
        }
    }

    // ==========================================
    // VIEW ALL USERS
    // ==========================================

    private void viewAllUsers() {

        List<User> users = userDao.getAllUsers();

        System.out.println("\n========== ALL SYSTEM USERS ==========");

        if (users.isEmpty()) {
            System.out.println("No users found.");
            return;
        }

        displayUsers(users);
    }

    // ==========================================
    // VIEW PENDING USERS
    // ==========================================

    private void viewPendingUsers() {

        List<User> users = userDao.getPendingUsers();

        System.out.println("\n========== PENDING REGISTRATIONS ==========");

        if (users.isEmpty()) {
            System.out.println("No pending registrations.");
            return;
        }

        displayUsers(users);
    }

    // ==========================================
    // DISPLAY USERS TABLE
    // ==========================================

    private void displayUsers(List<User> users) {

        System.out.printf(
                "%-8s %-20s %-15s %-15s%n",
                "ID", "USERNAME", "ROLE", "STATUS"
        );

        System.out.println(
                "------------------------------------------------------------"
        );

        for (User user : users) {

            System.out.printf(
                    "%-8d %-20s %-15s %-15s%n",
                    user.id(),
                    user.username(),
                    user.roles(),
                    user.status()
            );
        }
    }

    // ==========================================
    // APPROVE USER ACCOUNT
    // ==========================================

    private void approveUser() {

        viewPendingUsers();

        int userId = readIntInput(
                "\nEnter User ID to approve (0 to cancel): "
        );

        if (userId == 0) {
            System.out.println("Approval cancelled.");
            return;
        }

        if (userId < 0) {
            System.out.println("Invalid User ID.");
            return;
        }

        User targetUser = findUserById(userId);

        if (targetUser == null) {
            System.out.println("User ID not found.");
            return;
        }

        if (!"PENDING".equalsIgnoreCase(targetUser.status())) {
            System.out.println("This account is not pending approval.");
            return;
        }

        boolean success = userDao.approveUser(
                userId,
                currentAdmin.id()
        );

        if (success) {
            System.out.println("User account approved successfully!");
        } else {
            System.out.println("Unable to approve user account.");
        }
    }

    // ==========================================
    // UPDATE USER ROLE AND STATUS
    // ==========================================

    private void updateUser() {

        System.out.println("\n========== UPDATE USER ROLE AND STATUS ==========");

        viewAllUsers();

        int targetUserId = readIntInput(
                "\nEnter User ID to update (0 to cancel): "
        );

        if (targetUserId == 0) {
            System.out.println("Update cancelled.");
            return;
        }

        if (targetUserId < 0) {
            System.out.println("Invalid User ID.");
            return;
        }

        // Restrict editing the currently active admin account.
        if (targetUserId == currentAdmin.id()) {

            System.out.println(
                    "WARNING: Modifying the currently active admin account is restricted."
            );

            return;
        }

        User targetUser = findUserById(targetUserId);

        if (targetUser == null) {
            System.out.println("User ID not found.");
            return;
        }

        System.out.println("\nSelected User:");
        System.out.println("User ID: " + targetUser.id());
        System.out.println("Username: " + targetUser.username());
        System.out.println("Current Role: " + targetUser.roles());
        System.out.println("Current Status: " + targetUser.status());

        String newRole = readValidRole();

        if (newRole == null) {
            System.out.println("Update cancelled.");
            return;
        }

        String newStatus = readValidStatus();

        if (newStatus == null) {
            System.out.println("Update cancelled.");
            return;
        }

        System.out.println("\n========== CONFIRM USER UPDATE ==========");
        System.out.println("User ID: " + targetUser.id());
        System.out.println("Username: " + targetUser.username());
        System.out.println("Old Role: " + targetUser.roles());
        System.out.println("New Role: " + newRole);
        System.out.println("Old Status: " + targetUser.status());
        System.out.println("New Status: " + newStatus);

        String confirmation = readInput(
                "Confirm update? (Y/N): "
        );

        if (confirmation.equalsIgnoreCase("0")
                || confirmation.equalsIgnoreCase("N")) {

            System.out.println("Update cancelled.");
            return;
        }

        if (!confirmation.equalsIgnoreCase("Y")) {
            System.out.println("Invalid confirmation. Update cancelled.");
            return;
        }

        boolean success = userDao.updateUserRoleAndStatus(
                targetUserId,
                newRole,
                newStatus,
                currentAdmin.id()
        );

        if (success) {

            System.out.println("\nUser successfully updated!");
            System.out.println("Updated Username: " + targetUser.username());
            System.out.println("Updated Role: " + newRole);
            System.out.println("Updated Status: " + newStatus);

        } else {

            System.out.println("Failed to update user.");
        }
    }

    // ==========================================
    // FIND USER BY ID
    // ==========================================

    private User findUserById(int userId) {

        return userDao.getAllUsers()
                .stream()
                .filter(user -> user.id() == userId)
                .findFirst()
                .orElse(null);
    }

    // ==========================================
    // SELECT ROLE
    // ==========================================

    private String readValidRole() {

        while (true) {

            System.out.println("\nAvailable Roles:");
            System.out.println("1. STAFF");
            System.out.println("2. MANAGER");
            System.out.println("3. ADMIN");
            System.out.println("0. Cancel");

            String choice = readInput("Enter new role: ")
                    .toUpperCase(Locale.ROOT);

            switch (choice) {

                case "1", "STAFF":
                    return "STAFF";

                case "2", "MANAGER":
                    return "MANAGER";

                case "3", "ADMIN":
                    return "ADMIN";

                case "0":
                    return null;

                default:
                    System.out.println("Invalid role. Please try again.");
            }
        }
    }

    // ==========================================
    // SELECT STATUS
    // ==========================================

    private String readValidStatus() {

        while (true) {

            System.out.println("\nAvailable Status:");
            System.out.println("1. APPROVED");
            System.out.println("2. PENDING");
            System.out.println("3. REJECTED");
            System.out.println("0. Cancel");

            String choice = readInput("Enter new status: ")
                    .toUpperCase(Locale.ROOT);

            switch (choice) {

                case "1", "APPROVED":
                    return "APPROVED";

                case "2", "PENDING":
                    return "PENDING";

                case "3", "REJECTED":
                    return "REJECTED";

                case "0":
                    return null;

                default:
                    System.out.println("Invalid status. Please try again.");
            }
        }
    }

    // ==========================================
    // SYSTEM AUDIT LOG MENU
    // ==========================================

    private void auditLogMenu() {

        boolean viewing = true;

        while (viewing) {

            System.out.println("\n========== SYSTEM AUDIT LOGS ==========");
            System.out.println("1. View All System Logs");
            System.out.println("2. Filter Logs by User ID");
            System.out.println("3. Clear Historical Logs");
            System.out.println("0. Back to Admin Menu");

            int choice = readIntInput("Enter choice: ");

            switch (choice) {

                case 1:
                    displayAuditLogs(activityLogDao.getAllLogs());
                    break;

                case 2: {

                    int userId = readIntInput(
                            "Enter User ID (0 to cancel): "
                    );

                    if (userId > 0) {
                        displayAuditLogs(
                                activityLogDao.getLogsByUserId(userId)
                        );
                    } else if (userId < 0) {
                        System.out.println("Invalid User ID.");
                    }

                    break;
                }

                case 3: {

                    String confirmation = readInput(
                            "Delete ALL historical logs? Type YES to confirm: "
                    );

                    if (confirmation.equals("YES")) {

                        if (activityLogDao.clearAllLogs()) {
                            System.out.println("Historical logs cleared.");
                        } else {
                            System.out.println(
                                    "Unable to clear historical logs."
                            );
                        }

                    } else {
                        System.out.println("Clear operation cancelled.");
                    }

                    break;
                }

                case 0:
                    viewing = false;
                    break;

                default:
                    System.out.println("Invalid choice.");
            }
        }
    }

    // ==========================================
    // DISPLAY AUDIT LOGS
    // ==========================================

    private void displayAuditLogs(List<ActivityLog> logs) {

        System.out.println(
                "\n============================== AUDIT TRAIL =============================="
        );

        System.out.printf(
                "%-5s | %-8s | %-20s | %-35s | %-20s%n",
                "ID", "USER ID", "ACTION", "DESCRIPTION", "TIMESTAMP"
        );

        System.out.println(
                "---------------------------------------------------------------------------------------------------------------"
        );

        if (logs == null || logs.isEmpty()) {
            System.out.println("No activity logs found.");
            return;
        }

        for (ActivityLog log : logs) {

            String userId = log.userId() == null
                    ? "SYSTEM"
                    : String.valueOf(log.userId());

            System.out.printf(
                    "%-5d | %-8s | %-20s | %-35s | %-20s%n",
                    log.id(),
                    shorten(userId, 8),
                    shorten(log.actionType(), 20),
                    shorten(log.description(), 35),
                    shorten(log.timestamp(), 20)
            );
        }
    }

    // ==========================================
    // SHORTEN LONG TABLE VALUES
    // ==========================================

    private String shorten(String value, int maxLength) {

        if (value == null) {
            return "";
        }

        if (value.length() <= maxLength) {
            return value;
        }

        return value.substring(0, maxLength - 3) + "...";
    }

    // ==========================================
    // INTEGER INPUT VALIDATION
    // ==========================================

    private int readIntInput(String prompt) {

        while (true) {

            System.out.print(prompt);

            String input = scanner.nextLine().trim();

            try {
                return Integer.parseInt(input);

            } catch (NumberFormatException e) {

                System.out.println(
                        "Invalid input! Please enter numbers only."
                );
            }
        }
    }

    // ==========================================
    // GENERAL INPUT
    // ==========================================

    private String readInput(String prompt) {

        System.out.print(prompt);

        return scanner.nextLine().trim();
    }

    // ==========================================
    // VIEW ADMIN ACCOUNT
    // ==========================================

    private void viewAccountInformation() {

        System.out.println("\n========== ADMIN ACCOUNT ==========");
        System.out.println("User ID: " + currentAdmin.id());
        System.out.println("Username: " + currentAdmin.username());
        System.out.println("Role: " + currentAdmin.roles());
        System.out.println("Status: " + currentAdmin.status());
    }
}