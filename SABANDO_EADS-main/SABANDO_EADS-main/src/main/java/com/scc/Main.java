
package com.scc;

import com.scc.dao.DatabaseInitializer;
import com.scc.dao.UserDao;
import com.scc.ui.MenuScreen;

import java.util.Scanner;

public class Main {

    public static void main(String[] args) {

        try {
            DatabaseInitializer.initialize();

            try (Scanner scanner = new Scanner(System.in)) {

                UserDao userDao = new UserDao();

                // Create initial admin only if none exists
                if (!userDao.hasAdminAccount()) {

                    System.out.println("\n===== INITIAL ADMIN SETUP =====");

                    System.out.print("Admin username: ");
                    String username = scanner.nextLine().trim();

                    System.out.print("Admin password: ");
                    String password = scanner.nextLine();

                    if (username.isBlank() || password.isBlank()) {
                        System.out.println("Username and password are required.");
                        return;
                    }

                    userDao.createInitialAdmin(username, password);
                }

                // Display main menu
                MenuScreen menu = new MenuScreen(scanner);
                menu.start();
            }

        } catch (Exception e) {
            System.err.println("EADS startup error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
