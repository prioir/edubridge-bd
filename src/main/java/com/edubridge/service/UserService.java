package com.edubridge.service;

import com.edubridge.entity.Opportunity;
import com.edubridge.entity.Role;
import com.edubridge.entity.User;
import com.edubridge.exception.ResourceNotFoundException;
import com.edubridge.form.ProfileForm;
import com.edubridge.form.RegistrationForm;
import com.edubridge.repository.UserRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final OpportunityService opportunityService;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       OpportunityService opportunityService,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.opportunityService = opportunityService;
        this.passwordEncoder = passwordEncoder;
    }

    // ================= Accounts =================

    /** Public registration. The role is ALWAYS STUDENT. */
    public User register(RegistrationForm form) {
        return createUser(form.getFullName(), form.getEmail(), form.getPassword(), Role.STUDENT);
    }

    /** Used only by AdminSeeder at startup. There is no web page that can create an admin. */
    public User createAdmin(String fullName, String email, String rawPassword) {
        return createUser(fullName, email, rawPassword, Role.ADMIN);
    }

    private User createUser(String fullName, String email, String rawPassword, Role role) {
        String normalizedEmail = normalize(email);
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("An account with this email already exists");
        }
        User user = new User();
        user.setFullName(fullName.trim());
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        try {
            return userRepository.save(user);
        } catch (DuplicateKeyException e) {
            // Two people registering the same email at the same instant.
            throw new IllegalArgumentException("An account with this email already exists");
        }
    }

    public boolean emailExists(String email) {
        return userRepository.existsByEmail(normalize(email));
    }

    public User getByEmail(String email) {
        return userRepository.findByEmail(normalize(email))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public User save(User user) { return userRepository.save(user); }

    public List<User> getStudents() { return userRepository.findByRoleOrderByCreatedAtDesc(Role.STUDENT); }

    public long countStudents() {
        return userRepository.countByRole(Role.STUDENT);
    }

    // ================= Profile =================

    public ProfileForm toProfileForm(User user) {
        ProfileForm form = new ProfileForm();
        form.setFullName(user.getFullName());
        form.setUniversity(user.getUniversity());
        form.setDepartment(user.getDepartment());
        form.setStudyLevel(user.getStudyLevel());
        form.setInterests(user.getInterests());
        return form;
    }

    public void updateProfilePictureUrl(String email, String pictureUrl) {
        User user = getByEmail(email);
        user.setProfilePictureUrl(pictureUrl);
        userRepository.save(user);
    }

    public void updateProfile(String email, ProfileForm form) {
        User user = getByEmail(email);
        user.setFullName(form.getFullName().trim());
        user.setUniversity(blankToNull(form.getUniversity()));
        user.setDepartment(blankToNull(form.getDepartment()));
        user.setStudyLevel(blankToNull(form.getStudyLevel()));
        user.setInterests(blankToNull(form.getInterests()));
        userRepository.save(user);
    }

    // ================= Bookmarks (always for the logged-in user's own account) =================

    public List<Opportunity> getBookmarkedOpportunities(User user) {
        return opportunityService.findPublishedByIds(user.getBookmarkIds());
    }

    public void addBookmark(String email, String opportunityId) {
        opportunityService.getPublishedById(opportunityId);   // 404 if missing or unpublished
        User user = getByEmail(email);
        if (user.getBookmarkIds().add(opportunityId)) {        // a Set: duplicates are ignored
            userRepository.save(user);
        }
    }

    public void removeBookmark(String email, String opportunityId) {
        User user = getByEmail(email);
        if (user.getBookmarkIds().remove(opportunityId)) {
            userRepository.save(user);
        }
    }

    // ================= Helpers =================

    private String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
