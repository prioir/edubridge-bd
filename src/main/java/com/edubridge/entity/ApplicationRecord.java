package com.edubridge.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/** A student's own note about how far they have got with one opportunity. */
@Document(collection = "application_records")
@CompoundIndex(name = "user_opportunity_unique", def = "{'userId': 1, 'opportunityId': 1}", unique = true)
@Getter
@Setter
@NoArgsConstructor
public class ApplicationRecord {

    @Id
    private String id;

    private String userId;
    private String opportunityId;
    private ApplicationStatus status;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();
}
