
package com.scc.ui;

import com.scc.model.User;

import java.util.Scanner;

public class ManagerMenu {

    private final Scanner scanner;
    private final User currentManager;

    public ManagerMenu(Scanner scanner, User currentManager) {
        this.scanner = scanner;
        this.currentManager = currentManager;
    }

    public void start() {

        boolean running = true;

        while (running) {

            System.out.println("\n======================================");
            System.out.println("       EADS MANAGER DASHBOARD");
            System.out.println("======================================");
            System.out.println("Welcome, " + currentManager.username());
            System.out.println("Role: " + currentManager.roles());
            System.out.println("--------------------------------------");
            System.out.println("1. View Available Assets");
            System.out.println("2. View Account Information");
            System.out.println("3. View Asset Distribution");
            System.out.println("4. Logout");
            System.out.println("--------------------------------------");

            System.out.print("Enter choice: ");
            String choice = scanner.nextLine().trim();

            switch (choice) {

                case "1" -> viewAvailableAssets();

                case "2" -> viewAccountInformation();

                case "3" -> viewAssetDistribution();

                case "4" -> {
                    System.out.println("Manager logged out successfully.");
                    running = false;
                }

                default ->
                        System.out.println("Invalid choice! Please try again.");
            }
        }
    }

    // VIEW AVAILABLE ASSETS
    private void viewAvailableAssets() {

        System.out.println("\n========== AVAILABLE ASSETS ==========");
        System.out.println(
                "Asset viewing will be connected to your AssetDao."
        );
    }

    // VIEW ACCOUNT INFORMATION
    private void viewAccountInformation() {

        System.out.println("\n========== MANAGER ACCOUNT ==========");
        System.out.println("User ID: " + currentManager.id());
        System.out.println("Username: " + currentManager.username());
        System.out.println("Role: " + currentManager.roles());
        System.out.println("Status: " + currentManager.status());
    }

    // VIEW ASSET DISTRIBUTION
    private void viewAssetDistribution() {

        System.out.println("\n========== ASSET DISTRIBUTION ==========");
        System.out.println(
                "Asset distribution report will be connected "
                        + "to your AssetDao."
        );
    }
}
