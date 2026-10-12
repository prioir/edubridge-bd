package com.edubridge.controller;

import com.edubridge.service.UserService;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Adds login information to every page so the navbar can show the right links. */
@ControllerAdvice
public class GlobalModelAdvice {

    private final UserService userService;

    public GlobalModelAdvice(UserService userService) {
        this.userService = userService;
    }

    @ModelAttribute
    public void addAuthInfo(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean loggedIn = auth != null && auth.isAuthenticated()
                && !(auth instanceof AnonymousAuthenticationToken);

        boolean admin = loggedIn && hasRole(auth, "ROLE_ADMIN");
        boolean student = loggedIn && hasRole(auth, "ROLE_STUDENT");

        model.addAttribute("isLoggedIn", loggedIn);
        model.addAttribute("isAdmin", admin);
        model.addAttribute("isStudent", student);
        model.addAttribute("currentUserName", loggedIn ? auth.getName() : null);
        String profilePictureUrl = null;
        if (loggedIn) {
            try {
                profilePictureUrl = userService.getByEmail(auth.getName()).getProfilePictureUrl();
            } catch (RuntimeException ignored) {
                // Keep public/error pages renderable if the account record is unavailable.
            }
        }
        model.addAttribute("currentUserProfilePictureUrl", profilePictureUrl);
    }

    private boolean hasRole(Authentication auth, String role) {
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(role));
    }
}
