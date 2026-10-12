package com.edubridge.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Only the fields a student may edit. Role, email and bookmarks cannot be changed from here. */
@Getter
@Setter
@NoArgsConstructor
public class ProfileForm {

    @NotBlank(message = "Full name is required")
    @Size(max = 100, message = "Name must be at most 100 characters")
    private String fullName;

    @Size(max = 150, message = "University name is too long")
    private String university;

    @Size(max = 100, message = "Department name is too long")
    private String department;

    @Size(max = 50, message = "Study level is too long")
    private String studyLevel;

    @Size(max = 300, message = "Interests must be at most 300 characters")
    private String interests;
}
