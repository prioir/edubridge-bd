package com.edubridge.entity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.validator.constraints.URL;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Document(collection = "opportunities")
@Getter
@Setter
@NoArgsConstructor
public class Opportunity {

    @Id
    private String id;

    @NotBlank(message = "Title is required")
    @Size(max = 150, message = "Title must be at most 150 characters")
    private String title;

    @NotNull(message = "Please choose a category")
    @Indexed
    private Category category;

    @NotBlank(message = "Organization name is required")
    @Size(max = 120, message = "Organization name must be at most 120 characters")
    private String organizationName;

    @NotBlank(message = "Short description is required")
    @Size(max = 250, message = "Short description must be at most 250 characters")
    private String shortDescription;

    @NotBlank(message = "Detailed description is required")
    @Size(max = 5000, message = "Detailed description is too long")
    private String detailedDescription;

    @NotBlank(message = "Eligibility requirements are required")
    @Size(max = 2000, message = "Eligibility text is too long")
    private String eligibility;

    @NotNull(message = "Application deadline is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate deadline;

    @NotBlank(message = "Official application URL is required")
    @Size(max = 500, message = "URL is too long")
    @URL(regexp = "^(http|https)://.*$", message = "Enter a valid http(s) URL")
    private String applicationUrl;

    private boolean published;

    /** True once an admin has checked that the opportunity is genuine. */
    private boolean verified;

    private LocalDateTime createdAt = LocalDateTime.now();

    // ---- Helpers for Thymeleaf (not stored in MongoDB) ----

    public boolean isExpired() {
        return deadline != null && deadline.isBefore(LocalDate.now());
    }

    /** Days left until the deadline. Negative if already expired. */
    public long getDaysLeft() {
        return deadline == null ? 0 : ChronoUnit.DAYS.between(LocalDate.now(), deadline);
    }
}
