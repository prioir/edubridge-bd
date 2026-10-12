package com.edubridge.service;

import com.edubridge.entity.ApplicationRecord;
import com.edubridge.entity.ApplicationStatus;
import com.edubridge.entity.Opportunity;
import com.edubridge.exception.ResourceNotFoundException;
import com.edubridge.repository.ApplicationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock ApplicationRepository applicationRepository;
    @Mock OpportunityService opportunityService;
    @InjectMocks ApplicationService service;

    @Test
    void ownerCanUpdateOwnRecord() {
        ApplicationRecord record = new ApplicationRecord();
        record.setStatus(ApplicationStatus.SAVED);
        when(applicationRepository.findByIdAndUserId("r1", "userA")).thenReturn(Optional.of(record));
        when(applicationRepository.save(any(ApplicationRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        ApplicationRecord updated = service.updateStatus("userA", "r1", ApplicationStatus.APPLIED);

        assertEquals(ApplicationStatus.APPLIED, updated.getStatus());
    }

    @Test
    void anotherStudentCannotUpdateTheRecord() {
        // The lookup is by id AND userId, so userB gets nothing back.
        when(applicationRepository.findByIdAndUserId("r1", "userB")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.updateStatus("userB", "r1", ApplicationStatus.COMPLETED));
        verify(applicationRepository, never()).save(any());
    }

    @Test
    void anotherStudentCannotDeleteTheRecord() {
        when(applicationRepository.findByIdAndUserId("r1", "userB")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.delete("userB", "r1"));
        verify(applicationRepository, never()).delete(any(ApplicationRecord.class));
    }

    @Test
    void trackingTwiceReusesTheSameRecord() {
        ApplicationRecord existing = new ApplicationRecord();
        existing.setUserId("userA");
        existing.setOpportunityId("o1");
        existing.setStatus(ApplicationStatus.SAVED);
        when(opportunityService.getPublishedById("o1")).thenReturn(new Opportunity());
        when(applicationRepository.findByUserIdAndOpportunityId("userA", "o1")).thenReturn(Optional.of(existing));
        when(applicationRepository.save(any(ApplicationRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        ApplicationRecord result = service.track("userA", "o1", ApplicationStatus.PLANNING);

        assertEquals(ApplicationStatus.PLANNING, result.getStatus());
        assertEquals("o1", result.getOpportunityId());
    }

    @Test
    void cannotTrackAnUnpublishedOpportunity() {
        when(opportunityService.getPublishedById("draft"))
                .thenThrow(new ResourceNotFoundException("Opportunity not found"));

        assertThrows(ResourceNotFoundException.class,
                () -> service.track("userA", "draft", ApplicationStatus.SAVED));
        verify(applicationRepository, never()).save(any());
    }

    @Test
    void countsEveryStatusEvenWhenZero() {
        ApplicationRecord a = new ApplicationRecord();
        a.setStatus(ApplicationStatus.APPLIED);
        ApplicationRecord b = new ApplicationRecord();
        b.setStatus(ApplicationStatus.APPLIED);

        Map<ApplicationStatus, Long> counts = service.countByStatus(List.of(a, b));

        assertEquals(2L, counts.get(ApplicationStatus.APPLIED));
        assertEquals(0L, counts.get(ApplicationStatus.SAVED));
    }
}
