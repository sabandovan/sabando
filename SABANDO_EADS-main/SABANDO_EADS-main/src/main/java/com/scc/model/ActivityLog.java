package com.scc.model;

public record ActivityLog(
        int id,
        Integer userId,
        String actionType,
        String description,
        String timestamp
) {
}