package com.edubridge.service;

import com.edubridge.entity.ApplicationRecord;
import com.edubridge.entity.ApplicationStatus;
import com.edubridge.exception.ResourceNotFoundException;
import com.edubridge.repository.ApplicationRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Tracks a student's progress. EduBridge never submits applications:
 * students apply on the official website and update the status here by hand.
 */
@Service
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final OpportunityService opportunityService;

    public ApplicationService(ApplicationRepository applicationRepository,
                              OpportunityService opportunityService) {
        this.applicationRepository = applicationRepository;
        this.opportunityService = opportunityService;
    }

    /** Creates a tracking record, or updates the status if one already exists. */
    public ApplicationRecord track(String userId, String opportunityId, ApplicationStatus status) {
        opportunityService.getPublishedById(opportunityId);   // 404 if missing or unpublished

        ApplicationRecord record = applicationRepository
                .findByUserIdAndOpportunityId(userId, opportunityId)
                .orElseGet(() -> {
                    ApplicationRecord created = new ApplicationRecord();
                    created.setUserId(userId);
                    created.setOpportunityId(opportunityId);
                    return created;
                });
        record.setStatus(status);
        record.setUpdatedAt(LocalDateTime.now());
        return applicationRepository.save(record);
    }

    public List<ApplicationRecord> getForUser(String userId) {
        return applicationRepository.findByUserIdOrderByUpdatedAtDesc(userId);
    }

    public Optional<ApplicationRecord> findForUserAndOpportunity(String userId, String opportunityId) {
        return applicationRepository.findByUserIdAndOpportunityId(userId, opportunityId);
    }

    /** Only the owner can change a record. Someone else's record looks like "not found". */
    public ApplicationRecord updateStatus(String userId, String recordId, ApplicationStatus status) {
        ApplicationRecord record = applicationRepository.findByIdAndUserId(recordId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Record not found"));
        record.setStatus(status);
        record.setUpdatedAt(LocalDateTime.now());
        return applicationRepository.save(record);
    }

    public void delete(String userId, String recordId) {
        ApplicationRecord record = applicationRepository.findByIdAndUserId(recordId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Record not found"));
        applicationRepository.delete(record);
    }

    public Map<ApplicationStatus, Long> countByStatus(List<ApplicationRecord> records) {
        Map<ApplicationStatus, Long> counts = new EnumMap<>(ApplicationStatus.class);
        for (ApplicationStatus s : ApplicationStatus.values()) {
            counts.put(s, 0L);
        }
        for (ApplicationRecord r : records) {
            counts.merge(r.getStatus(), 1L, Long::sum);
        }
        return counts;
    }
}
