package com.edubridge.entity;

/** Opportunity categories. An enum is enough, so there is no Category collection. */
public enum Category {
    SCHOLARSHIP("Scholarship"),
    INTERNSHIP("Internship"),
    COMPETITION("Competition"),
    TRAINING("Training");

    private final String displayName;

    Category(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
