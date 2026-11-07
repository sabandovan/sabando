
package com.scc.ui;

import com.scc.dao.UserDao;
import com.scc.model.User;

import java.util.Scanner;

public class MenuScreen {

    private final Scanner scanner;
    private final UserDao userDao;

    public MenuScreen(Scanner scanner) {
        this.scanner = scanner;
        this.userDao = new UserDao();
    }

    public void start() {
        boolean running = true;

        while (running) {
            System.out.println("\n======================================");
            System.out.println("   EXCHANGE ASSET DISTRIBUTION SYSTEM");
            System.out.println("======================================");
            System.out.println("1. Register Account");
            System.out.println("2. Login Account");
            System.out.println("0. Exit");
            System.out.println("======================================");

            System.out.print("Enter choice: ");
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> registerAccount();
                case "2" -> loginAccount();
                case "0" -> {
                    System.out.println("Thank you for using EADS!");
                    running = false;
                }
                default -> System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    // REGISTER ACCOUNT
    private void registerAccount() {
        System.out.println("\n========== REGISTER ACCOUNT ==========");

        System.out.print("Enter username: ");
        String username = scanner.nextLine().trim();

        System.out.print("Enter password: ");
        String password = scanner.nextLine().trim();

        if (username.isEmpty() || password.isEmpty()) {
            System.out.println("Username and password are required.");
            return;
        }

        String role = selectRole();

        if (role == null) {
            System.out.println("Registration cancelled.");
            return;
        }

        userDao.register(username, password, role);

        System.out.println("Registration request submitted.");
        System.out.println("Your account must be approved by an administrator.");
    }

    // SELECT USER ROLE
    private String selectRole() {
        while (true) {
            System.out.println("\n========== SELECT ROLE ==========");
            System.out.println("1. STAFF");
            System.out.println("2. MANAGER");
            System.out.println("3. ADMIN");
            System.out.println("0. Cancel");
            System.out.print("Choose role: ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    return "STAFF";
                case "2":
                    return "MANAGER";
                case "3":
                    return "ADMIN";
                case "0":
                    return null;
                default:
                    System.out.println("Invalid role selection. Try again.");
            }
        }
    }

    // LOGIN ACCOUNT
    private void loginAccount() {
        System.out.println("\n========== LOGIN ACCOUNT ==========");

        System.out.print("Enter username: ");
        String username = scanner.nextLine().trim();

        System.out.print("Enter password: ");
        String password = scanner.nextLine().trim();

        if (username.isEmpty() || password.isEmpty()) {
            System.out.println("Username and password are required.");
            return;
        }

        User user = userDao.login(username, password);

        if (user == null) {
            System.out.println("Invalid username or password.");
            return;
        }

        if (!"APPROVED".equalsIgnoreCase(user.status())) {
            System.out.println("Your account is not yet approved.");
            return;
        }

        System.out.println("\nLogin successful!");
        System.out.println("Welcome, " + user.username() + "!");
        System.out.println("Role: " + user.roles());

        openRoleMenu(user);
    }

    // ROLE-BASED MENU ROUTING
    private void openRoleMenu(User user) {
        switch (user.roles().toUpperCase()) {

            case "ADMIN" ->
                    new AdminMenu(scanner, user).start();

            case "MANAGER" ->
                    new ManagerMenu(scanner, user).start();

            case "STAFF" ->
                    new StaffMenu(scanner, user).start();

            default ->
                    System.out.println("Invalid user role. Please contact the administrator.");
        }
    }
}
