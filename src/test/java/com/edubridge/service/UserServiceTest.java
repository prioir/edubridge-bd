package com.edubridge.service;

import com.edubridge.entity.Opportunity;
import com.edubridge.entity.Role;
import com.edubridge.entity.User;
import com.edubridge.exception.ResourceNotFoundException;
import com.edubridge.form.RegistrationForm;
import com.edubridge.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock OpportunityService opportunityService;
    @Mock PasswordEncoder passwordEncoder;
    @InjectMocks UserService userService;

    private RegistrationForm form(String email) {
        RegistrationForm form = new RegistrationForm();
        form.setFullName("  Rahim Uddin ");
        form.setEmail(email);
        form.setPassword("Password123");
        form.setConfirmPassword("Password123");
        return form;
    }

    @Test
    void publicRegistrationAlwaysCreatesStudentWithHashedPassword() {
        when(userRepository.existsByEmail("rahim@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123")).thenReturn("HASHED");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User saved = userService.register(form("  Rahim@Example.com "));

        assertEquals(Role.STUDENT, saved.getRole());
        assertEquals("HASHED", saved.getPasswordHash());
        assertNotEquals("Password123", saved.getPasswordHash());
        assertEquals("rahim@example.com", saved.getEmail());
        assertEquals("Rahim Uddin", saved.getFullName());
    }

    @Test
    void duplicateEmailIsRejected() {
        when(userRepository.existsByEmail("rahim@example.com")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> userService.register(form("rahim@example.com")));
        verify(userRepository, never()).save(any());
    }

    @Test
    void bookmarkingTwiceStoresOnlyOneEntry() {
        User student = new User();
        student.setEmail("a@example.com");
        when(userRepository.findByEmail("a@example.com")).thenReturn(Optional.of(student));
        when(opportunityService.getPublishedById("o1")).thenReturn(new Opportunity());

        userService.addBookmark("a@example.com", "o1");
        userService.addBookmark("a@example.com", "o1");

        assertEquals(1, student.getBookmarkIds().size());
        verify(userRepository, times(1)).save(student);   // the second call changed nothing
    }

    @Test
    void bookmarkChangesOnlyTheLoggedInUsersDocument() {
        User me = new User();
        me.setEmail("me@example.com");
        when(userRepository.findByEmail("me@example.com")).thenReturn(Optional.of(me));
        when(opportunityService.getPublishedById("o1")).thenReturn(new Opportunity());

        userService.addBookmark("me@example.com", "o1");

        verify(userRepository).findByEmail("me@example.com");
        verify(userRepository, never()).findByEmail("victim@example.com");
    }

    @Test
    void unpublishedOrMissingOpportunityCannotBeBookmarked() {
        when(opportunityService.getPublishedById("draft"))
                .thenThrow(new ResourceNotFoundException("Opportunity not found"));

        assertThrows(ResourceNotFoundException.class, () -> userService.addBookmark("a@example.com", "draft"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void removingBookmarkWorks() {
        User student = new User();
        student.getBookmarkIds().add("o1");
        when(userRepository.findByEmail("a@example.com")).thenReturn(Optional.of(student));

        userService.removeBookmark("a@example.com", "o1");

        assertEquals(0, student.getBookmarkIds().size());
        verify(userRepository).save(student);
    }
}
