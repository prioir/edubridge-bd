package com.edubridge.service;

import com.edubridge.entity.Category;
import com.edubridge.entity.Opportunity;
import com.edubridge.exception.ResourceNotFoundException;
import com.edubridge.repository.ApplicationRepository;
import com.edubridge.repository.OpportunityRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpportunityServiceTest {

    @Mock OpportunityRepository opportunityRepository;
    @Mock ApplicationRepository applicationRepository;
    @InjectMocks OpportunityService service;

    @Test
    void listingWithoutFiltersReturnsPublishedOnly() {
        Opportunity o = new Opportunity();
        when(opportunityRepository.findByPublishedTrueOrderByCreatedAtDesc()).thenReturn(List.of(o));

        assertEquals(List.of(o), service.searchPublished("  ", null));
    }

    @Test
    void keywordAndCategoryUseQuotedRegex() {
        service.searchPublished("brac", Category.SCHOLARSHIP);

        verify(opportunityRepository)
                .searchPublishedByCategory(Pattern.quote("brac"), "SCHOLARSHIP");
    }

    @Test
    void draftsLookLikeNotFound() {
        when(opportunityRepository.findByIdAndPublishedTrue("draft1")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getPublishedById("draft1"));
    }

    @Test
    void createIgnoresAnIdSentByTheForm() {
        when(opportunityRepository.save(any(Opportunity.class))).thenAnswer(inv -> inv.getArgument(0));
        Opportunity form = new Opportunity();
        form.setId("hacker-chosen-id");

        Opportunity saved = service.create(form);

        assertNull(saved.getId());
    }

    @Test
    void updateKeepsIdAndCreatedAt() {
        Opportunity existing = new Opportunity();
        existing.setId("o1");
        LocalDateTime originalCreatedAt = LocalDateTime.of(2026, 1, 1, 10, 0);
        existing.setCreatedAt(originalCreatedAt);
        when(opportunityRepository.findById("o1")).thenReturn(Optional.of(existing));
        when(opportunityRepository.save(any(Opportunity.class))).thenAnswer(inv -> inv.getArgument(0));

        Opportunity form = new Opportunity();
        form.setTitle("New title");
        form.setCategory(Category.TRAINING);
        form.setDeadline(LocalDate.now().plusDays(3));
        form.setCreatedAt(LocalDateTime.now());

        Opportunity saved = service.update("o1", form);

        assertEquals("o1", saved.getId());
        assertEquals("New title", saved.getTitle());
        assertEquals(originalCreatedAt, saved.getCreatedAt());
    }

    @Test
    void deleteAlsoRemovesTrackingRecords() {
        when(opportunityRepository.findById("o1")).thenReturn(Optional.of(new Opportunity()));

        service.delete("o1");

        verify(applicationRepository).deleteByOpportunityId("o1");
        verify(opportunityRepository).deleteById("o1");
    }

    @Test
    void deletingMissingOpportunityFailsBeforeDeletingAnything() {
        when(opportunityRepository.findById("nope")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.delete("nope"));
        verify(opportunityRepository, never()).deleteById(any());
    }

    @Test
    void expiredHelperWorks() {
        Opportunity past = new Opportunity();
        past.setDeadline(LocalDate.now().minusDays(1));
        Opportunity future = new Opportunity();
        future.setDeadline(LocalDate.now().plusDays(1));

        assertEquals(true, past.isExpired());
        assertEquals(false, future.isExpired());
        assertEquals(1, future.getDaysLeft());
    }
}
