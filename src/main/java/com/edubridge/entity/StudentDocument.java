package com.edubridge.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/** Metadata for a private student-owned uploaded document. */
@Document(collection = "student_documents")
@Getter @Setter @NoArgsConstructor
public class StudentDocument {
    @Id private String id;
    @Indexed private String userId;
    private String originalFilename;
    private String storedFilename;
    private String contentType;
    private long size;
    private LocalDateTime uploadedAt = LocalDateTime.now();
}
