package com.edubridge.form;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Registration form. It has NO role field, so a visitor can never choose ADMIN. */
@Getter
@Setter
@NoArgsConstructor
public class RegistrationForm {

    @NotBlank(message = "Full name is required")
    @Size(max = 100, message = "Name must be at most 100 characters")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email address")
    @Size(max = 150, message = "Email is too long")
    private String email;

    // BCrypt only uses the first 72 bytes, so we cap the length.
    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 72, message = "Password must be 8 to 72 characters")
    private String password;

    @NotBlank(message = "Please confirm your password")
    private String confirmPassword;
}
