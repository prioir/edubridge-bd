package com.edubridge.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A user account. Never pass this object's passwordHash to a template.
 * Bookmarks are kept as a Set of opportunity IDs, so a duplicate bookmark is impossible.
 */
@Document(collection = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    private String id;

    private String fullName;

    @Indexed(unique = true)
    private String email;

    private String passwordHash;

    private Role role;

    private String university;
    private String department;
    private String studyLevel;
    private String interests;

    /** Relative public URL for this account's uploaded profile photo. */
    private String profilePictureUrl;

    private Set<String> bookmarkIds = new LinkedHashSet<>();

    private boolean emailRemindersEnabled;

    /** Idempotency keys prevent sending the same opportunity/date reminder twice. */
    private Set<String> sentReminderKeys = new LinkedHashSet<>();

    private LocalDateTime createdAt = LocalDateTime.now();
}
