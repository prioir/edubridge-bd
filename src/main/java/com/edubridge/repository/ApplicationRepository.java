package com.edubridge.repository;

import com.edubridge.entity.ApplicationRecord;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ApplicationRepository extends MongoRepository<ApplicationRecord, String> {

    List<ApplicationRecord> findByUserIdOrderByUpdatedAtDesc(String userId);

    // Looking up by id AND userId is how we stop one student touching another student's record.
    Optional<ApplicationRecord> findByIdAndUserId(String id, String userId);

    Optional<ApplicationRecord> findByUserIdAndOpportunityId(String userId, String opportunityId);

    void deleteByOpportunityId(String opportunityId);
}
