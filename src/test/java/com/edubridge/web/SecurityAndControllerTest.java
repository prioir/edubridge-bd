package com.edubridge.web;

import com.edubridge.security.SecurityConfig;
import com.edubridge.service.ApplicationService;
import com.edubridge.service.OpportunityService;
import com.edubridge.service.UserService;
import com.edubridge.service.AuditService;
import com.edubridge.service.AiRecommendationService;
import com.edubridge.repository.StudentDocumentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Tests the real security rules and the real controllers. The services are mocks,
 * so no MongoDB is needed.
 */
@WebMvcTest
@Import(SecurityConfig.class)
class SecurityAndControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean UserService userService;
    @MockitoBean OpportunityService opportunityService;
    @MockitoBean ApplicationService applicationService;
    @MockitoBean AuditService auditService;
    @MockitoBean AiRecommendationService aiRecommendationService;
    @MockitoBean StudentDocumentRepository studentDocumentRepository;

    // ---------- Public pages and listing ----------

    @Test
    void anyoneCanOpenHomeAndListing() throws Exception {
        mockMvc.perform(get("/")).andExpect(status().isOk()).andExpect(view().name("home"));
        mockMvc.perform(get("/opportunities")).andExpect(status().isOk()).andExpect(view().name("opportunities"));
    }

    @Test
    void loginAndRegisterPagesArePublic() throws Exception {
        mockMvc.perform(get("/login")).andExpect(status().isOk());
        mockMvc.perform(get("/register")).andExpect(status().isOk());
    }

    // ---------- Role-based authorization ----------

    @Test
    void anonymousUserIsSentToLoginFromAdminAndStudentAreas() throws Exception {
        mockMvc.perform(get("/admin")).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
        mockMvc.perform(get("/student/bookmarks")).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void studentCannotOpenAdminPages() throws Exception {
        mockMvc.perform(get("/admin")).andExpect(status().isForbidden()).andExpect(forwardedUrl("/access-denied"));
        mockMvc.perform(get("/admin/opportunities/new")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCannotUseStudentPages() throws Exception {
        mockMvc.perform(get("/student")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanOpenDashboardAndForm() throws Exception {
        mockMvc.perform(get("/admin")).andExpect(status().isOk()).andExpect(view().name("admin/dashboard"));
        mockMvc.perform(get("/admin/opportunities/new")).andExpect(status().isOk())
                .andExpect(view().name("admin/opportunity-form"));
    }

    // ---------- CSRF ----------

    @Test
    @WithMockUser(roles = "ADMIN")
    void postWithoutCsrfTokenIsRejected() throws Exception {
        mockMvc.perform(post("/admin/opportunities/o1/delete")).andExpect(status().isForbidden());
        verify(opportunityService, never()).delete(any());
    }

    // ---------- Validation errors ----------

    @Test
    @WithMockUser(roles = "ADMIN")
    void invalidOpportunityShowsErrorsAndIsNotSaved() throws Exception {
        mockMvc.perform(post("/admin/opportunities").with(csrf()).param("title", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/opportunity-form"))
                .andExpect(model().attributeHasFieldErrors("opportunity", "title", "category", "deadline"));
        verify(opportunityService, never()).create(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void validOpportunityIsSavedThenRedirects() throws Exception {
        mockMvc.perform(post("/admin/opportunities").with(csrf())
                        .param("title", "Test scholarship")
                        .param("category", "SCHOLARSHIP")
                        .param("organizationName", "Test Org")
                        .param("shortDescription", "Short")
                        .param("detailedDescription", "Long")
                        .param("eligibility", "Students")
                        .param("deadline", "2030-01-31")
                        .param("applicationUrl", "https://example.com/apply"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/opportunities"));
        verify(opportunityService).create(any());
    }

    @Test
    void registrationWithMismatchedPasswordsShowsError() throws Exception {
        mockMvc.perform(post("/register").with(csrf())
                        .param("fullName", "Rahim")
                        .param("email", "rahim@example.com")
                        .param("password", "Password123")
                        .param("confirmPassword", "Different123"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeHasFieldErrors("registrationForm", "confirmPassword"));
        verify(userService, never()).register(any());
    }

    @Test
    void validRegistrationRedirectsToLogin() throws Exception {
        mockMvc.perform(post("/register").with(csrf())
                        .param("fullName", "Rahim")
                        .param("email", "rahim@example.com")
                        .param("password", "Password123")
                        .param("confirmPassword", "Password123")
                        .param("role", "ADMIN"))          // an attacker tries to pick a role: it is ignored
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
        verify(userService).register(any());
    }

    // ---------- Ownership ----------

    @Test
    @WithMockUser(username = "me@example.com", roles = "STUDENT")
    void bookmarkAlwaysUsesTheLoggedInUsersEmail() throws Exception {
        mockMvc.perform(post("/student/bookmarks/o1/add").with(csrf())
                        .param("email", "victim@example.com"))   // extra parameter must be ignored
                .andExpect(status().is3xxRedirection());

        verify(userService).addBookmark("me@example.com", "o1");
        verify(userService, never()).addBookmark(eq("victim@example.com"), any());
    }
}
