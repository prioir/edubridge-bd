package com.edubridge.entity;

public enum ApplicationStatus {
    SAVED("Saved"),
    PLANNING("Planning"),
    APPLIED("Applied"),
    COMPLETED("Completed");

    private final String displayName;

    ApplicationStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
