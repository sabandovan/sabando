
package com.scc.model;

public record User(
        int id,
        String username,
        String password,
        String roles,
        String status
) {

    // Check if user is an administrator
    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(roles);
    }


    // Check if user is a staff member
    public boolean isStaff() {
        return "STAFF".equalsIgnoreCase(roles);
    }

    // Check if user is a manager
    public boolean isManager() {
        return "MANAGER".equalsIgnoreCase(roles);
    }

    // Check if account is approved
    public boolean isApproved() {
        return "APPROVED".equalsIgnoreCase(status);
    }

    // Check if account is pending
    public boolean isPending() {
        return "PENDING".equalsIgnoreCase(status);
    }
}
