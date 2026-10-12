package com.edubridge.service;

import com.edubridge.entity.Category;
import com.edubridge.entity.Opportunity;
import com.edubridge.exception.ResourceNotFoundException;
import com.edubridge.repository.ApplicationRepository;
import com.edubridge.repository.OpportunityRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class OpportunityService {

    private final OpportunityRepository opportunityRepository;
    private final ApplicationRepository applicationRepository;

    public OpportunityService(OpportunityRepository opportunityRepository,
                              ApplicationRepository applicationRepository) {
        this.opportunityRepository = opportunityRepository;
        this.applicationRepository = applicationRepository;
    }

    // ================= Public side (published only) =================

    public List<Opportunity> getAllPublished() { return opportunityRepository.findByPublishedTrueOrderByCreatedAtDesc(); }

    public List<Opportunity> getVerifiedPublished() { return opportunityRepository.findByPublishedTrueAndVerifiedTrueOrderByCreatedAtDesc(); }

    public List<Opportunity> getRecentPublished() {
        return opportunityRepository.findTop6ByPublishedTrueOrderByCreatedAtDesc();
    }

    /** Published opportunities whose deadline is within the next 14 days. */
    public List<Opportunity> getClosingSoon() {
        LocalDate today = LocalDate.now();
        return opportunityRepository
                .findByPublishedTrueAndDeadlineBetweenOrderByDeadlineAsc(today, today.plusDays(14));
    }

    public List<Opportunity> searchPublished(String keyword, Category category) {
        boolean hasKeyword = keyword != null && !keyword.isBlank();

        if (!hasKeyword && category == null) {
            return opportunityRepository.findByPublishedTrueOrderByCreatedAtDesc();
        }
        if (!hasKeyword) {
            return opportunityRepository.findByPublishedTrueAndCategoryOrderByCreatedAtDesc(category);
        }
        // Pattern.quote makes sure user input is treated as plain text, not as a regex.
        String regex = Pattern.quote(keyword.trim());
        if (category == null) {
            return opportunityRepository.searchPublished(regex);
        }
        return opportunityRepository.searchPublishedByCategory(regex, category.name());
    }

    /** Students can only open published opportunities; drafts look like "not found". */
    public Opportunity getPublishedById(String id) {
        return opportunityRepository.findByIdAndPublishedTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Opportunity not found"));
    }

    public List<Opportunity> findPublishedByIds(Collection<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return opportunityRepository.findByIdInAndPublishedTrue(ids);
    }

    /** Looks up opportunities (any status) and returns them keyed by id. */
    public Map<String, Opportunity> findAsMap(Collection<String> ids) {
        Map<String, Opportunity> map = new HashMap<>();
        if (ids == null || ids.isEmpty()) {
            return map;
        }
        for (Opportunity o : opportunityRepository.findByIdIn(ids)) {
            map.put(o.getId(), o);
        }
        return map;
    }

    // ================= Admin side =================

    public List<Opportunity> getAll() {
        return opportunityRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<Opportunity> getRecentForAdmin() {
        return opportunityRepository.findTop5ByOrderByCreatedAtDesc();
    }

    public long countAll() {
        return opportunityRepository.count();
    }

    public long countPublished() {
        return opportunityRepository.countByPublishedTrue();
    }

    public long countUnpublished() {
        return opportunityRepository.countByPublishedFalse();
    }

    public Opportunity getById(String id) {
        return opportunityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Opportunity not found"));
    }

    public Opportunity create(Opportunity form) {
        form.setId(null);                          // never trust an id from the form
        form.setCreatedAt(LocalDateTime.now());
        return opportunityRepository.save(form);
    }

    /** Copies only the editable fields, so id and createdAt can never be overwritten. */
    public Opportunity update(String id, Opportunity form) {
        Opportunity existing = getById(id);
        existing.setTitle(form.getTitle());
        existing.setCategory(form.getCategory());
        existing.setOrganizationName(form.getOrganizationName());
        existing.setShortDescription(form.getShortDescription());
        existing.setDetailedDescription(form.getDetailedDescription());
        existing.setEligibility(form.getEligibility());
        existing.setDeadline(form.getDeadline());
        existing.setApplicationUrl(form.getApplicationUrl());
        existing.setPublished(form.isPublished());
        existing.setVerified(form.isVerified());
        return opportunityRepository.save(existing);
    }

    public Opportunity setPublished(String id, boolean published) {
        Opportunity existing = getById(id);
        existing.setPublished(published);
        return opportunityRepository.save(existing);
    }

    /** Deletes the opportunity and every student's tracking record for it. */
    public void delete(String id) {
        getById(id);
        applicationRepository.deleteByOpportunityId(id);
        opportunityRepository.deleteById(id);
    }
}
