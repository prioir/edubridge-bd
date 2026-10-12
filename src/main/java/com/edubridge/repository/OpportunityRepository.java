package com.edubridge.repository;

import com.edubridge.entity.Category;
import com.edubridge.entity.Opportunity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OpportunityRepository extends MongoRepository<Opportunity, String> {

    // ----- Public (published only) -----
    List<Opportunity> findTop6ByPublishedTrueOrderByCreatedAtDesc();

    List<Opportunity> findByPublishedTrueOrderByCreatedAtDesc();

    List<Opportunity> findByPublishedTrueAndVerifiedTrueOrderByCreatedAtDesc();

    List<Opportunity> findByPublishedTrueAndCategoryOrderByCreatedAtDesc(Category category);

    List<Opportunity> findByPublishedTrueAndDeadlineBetweenOrderByDeadlineAsc(LocalDate from, LocalDate to);

    Optional<Opportunity> findByIdAndPublishedTrue(String id);

    List<Opportunity> findByIdInAndPublishedTrue(Collection<String> ids);

    // Case-insensitive "contains" on title OR organization.
    @Query(value = "{ 'published': true, $or: [ "
            + "{ 'title': { $regex: ?0, $options: 'i' } }, "
            + "{ 'organizationName': { $regex: ?0, $options: 'i' } } ] }",
            sort = "{ 'createdAt': -1 }")
    List<Opportunity> searchPublished(String keywordRegex);

    @Query(value = "{ 'published': true, 'category': ?1, $or: [ "
            + "{ 'title': { $regex: ?0, $options: 'i' } }, "
            + "{ 'organizationName': { $regex: ?0, $options: 'i' } } ] }",
            sort = "{ 'createdAt': -1 }")
    List<Opportunity> searchPublishedByCategory(String keywordRegex, String categoryName);

    // ----- Admin (all opportunities) -----
    List<Opportunity> findAllByOrderByCreatedAtDesc();

    List<Opportunity> findTop5ByOrderByCreatedAtDesc();

    long countByPublishedTrue();

    long countByPublishedFalse();

    List<Opportunity> findByIdIn(Collection<String> ids);
}
