
package com.scc.ui;

import com.scc.model.User;

import java.util.Scanner;

public class StaffMenu {

    private final Scanner scanner;
    private final User currentStaff;

    public StaffMenu(Scanner scanner, User currentStaff) {
        this.scanner = scanner;
        this.currentStaff = currentStaff;
    }

    public void start() {

        boolean running = true;

        while (running) {

            System.out.println("\n======================================");
            System.out.println("       EADS STAFF DASHBOARD");
            System.out.println("======================================");
            System.out.println("Welcome, " + currentStaff.username());
            System.out.println("Role: " + currentStaff.roles());
            System.out.println("--------------------------------------");
            System.out.println("1. View Available Assets");
            System.out.println("2. View Account Information");
            System.out.println("3. Logout");
            System.out.println("--------------------------------------");

            System.out.print("Enter choice: ");
            String choice = scanner.nextLine().trim();

            switch (choice) {

                case "1" -> viewAvailableAssets();

                case "2" -> viewAccountInformation();

                case "3" -> {
                    System.out.println("Staff logged out successfully.");
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
                "Asset viewing feature will be connected "
                        + "to your AssetDao."
        );
    }

    // VIEW ACCOUNT INFORMATION
    private void viewAccountInformation() {

        System.out.println("\n========== STAFF ACCOUNT ==========");
        System.out.println("User ID: " + currentStaff.id());
        System.out.println("Username: " + currentStaff.username());
        System.out.println("Role: " + currentStaff.roles());
        System.out.println("Status: " + currentStaff.status());
    }
}
