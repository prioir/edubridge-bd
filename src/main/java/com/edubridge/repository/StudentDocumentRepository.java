package com.edubridge.repository;

import com.edubridge.entity.StudentDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface StudentDocumentRepository extends MongoRepository<StudentDocument, String> {
    List<StudentDocument> findByUserIdOrderByUploadedAtDesc(String userId);
    Optional<StudentDocument> findByIdAndUserId(String id, String userId);
}
