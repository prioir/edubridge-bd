package com.edubridge.controller;

import com.edubridge.entity.ApplicationRecord;
import com.edubridge.entity.ApplicationStatus;
import com.edubridge.entity.Opportunity;
import com.edubridge.entity.User;
import com.edubridge.form.ProfileForm;
import com.edubridge.service.ApplicationService;
import com.edubridge.service.OpportunityService;
import com.edubridge.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.Comparator;
import java.util.List;

/**
 * Everything under /student is for logged-in STUDENT accounts only (see SecurityConfig).
 * Every action uses principal.getName() (the logged-in email) and never an id sent by the browser,
 * so a student can only ever touch their own bookmarks and records.
 */
@Controller
@RequestMapping("/student")
public class StudentController {

    private final UserService userService;
    private final OpportunityService opportunityService;
    private final ApplicationService applicationService;

    public StudentController(UserService userService,
                             OpportunityService opportunityService,
                             ApplicationService applicationService) {
        this.userService = userService;
        this.opportunityService = opportunityService;
        this.applicationService = applicationService;
    }

    // ---------- Dashboard ----------
    @GetMapping
    public String dashboard(Principal principal, Model model) {
        User user = userService.getByEmail(principal.getName());
        List<Opportunity> bookmarks = userService.getBookmarkedOpportunities(user);
        List<ApplicationRecord> records = applicationService.getForUser(user.getId());

        List<Opportunity> upcoming = bookmarks.stream()
                .filter(o -> !o.isExpired())
                .sorted(Comparator.comparing(Opportunity::getDeadline))
                .limit(5)
                .toList();

        model.addAttribute("fullName", user.getFullName());
        int completed = 0;
        if (user.getFullName() != null && !user.getFullName().isBlank()) completed++;
        if (user.getProfilePictureUrl() != null && !user.getProfilePictureUrl().isBlank()) completed++;
        if (user.getUniversity() != null && !user.getUniversity().isBlank()) completed++;
        if (user.getDepartment() != null && !user.getDepartment().isBlank()) completed++;
        if (user.getStudyLevel() != null && !user.getStudyLevel().isBlank()) completed++;
        if (user.getInterests() != null && !user.getInterests().isBlank()) completed++;
        model.addAttribute("profileCompletion", Math.round(completed * 100.0 / 6));
        model.addAttribute("bookmarkCount", bookmarks.size());
        model.addAttribute("statusCounts", applicationService.countByStatus(records));
        model.addAttribute("upcoming", upcoming);
        return "student-dashboard";
    }

    // ---------- Profile ----------
    @GetMapping("/profile")
    public String profilePage(Principal principal, Model model) {
        User user = userService.getByEmail(principal.getName());
        model.addAttribute("profileForm", userService.toProfileForm(user));
        model.addAttribute("email", user.getEmail());
        return "profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@Valid @ModelAttribute("profileForm") ProfileForm form,
                                BindingResult result,
                                Principal principal,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("email", principal.getName());
            return "profile";
        }
        userService.updateProfile(principal.getName(), form);
        redirectAttributes.addFlashAttribute("successMessage", "Profile updated.");
        return "redirect:/student/profile";
    }

    // ---------- Profile picture (always scoped to the authenticated account) ----------
    @PostMapping("/profile/photo")
    public String uploadProfilePicture(@RequestParam("profilePicture") MultipartFile file,
                                       Principal principal,
                                       RedirectAttributes redirectAttributes) {
        if (file == null || file.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Choose an image to upload.");
            return "redirect:/student/profile";
        }
        if (file.getSize() > 2 * 1024 * 1024) {
            redirectAttributes.addFlashAttribute("errorMessage", "Image must be 2 MB or smaller.");
            return "redirect:/student/profile";
        }

        String contentType = file.getContentType();
        String extension;
        if ("image/jpeg".equalsIgnoreCase(contentType)) extension = ".jpg";
        else if ("image/png".equalsIgnoreCase(contentType)) extension = ".png";
        else if ("image/webp".equalsIgnoreCase(contentType)) extension = ".webp";
        else {
            redirectAttributes.addFlashAttribute("errorMessage", "Use a JPG, PNG, or WebP image.");
            return "redirect:/student/profile";
        }

        try {
            byte[] header = file.getInputStream().readNBytes(12);
            boolean validImage = (header.length >= 3 && (header[0] & 0xff) == 0xff
                    && (header[1] & 0xff) == 0xd8 && (header[2] & 0xff) == 0xff)
                    || (header.length >= 8 && header[0] == (byte) 0x89 && header[1] == 0x50
                    && header[2] == 0x4e && header[3] == 0x47)
                    || (header.length >= 12 && header[0] == 'R' && header[1] == 'I'
                    && header[2] == 'F' && header[3] == 'F' && header[8] == 'W'
                    && header[9] == 'E' && header[10] == 'B' && header[11] == 'P');
            if (!validImage) {
                redirectAttributes.addFlashAttribute("errorMessage", "The selected file is not a valid JPG, PNG, or WebP image.");
                return "redirect:/student/profile";
            }
            Path uploadDir = Paths.get("uploads", "profile").toAbsolutePath().normalize();
            Files.createDirectories(uploadDir);
            String filename = UUID.randomUUID() + extension;
            Path destination = uploadDir.resolve(filename).normalize();
            if (!destination.getParent().equals(uploadDir)) {
                redirectAttributes.addFlashAttribute("errorMessage", "Invalid image path.");
                return "redirect:/student/profile";
            }
            Files.copy(file.getInputStream(), destination);
            User currentUser = userService.getByEmail(principal.getName());
            String previousUrl = currentUser.getProfilePictureUrl();
            userService.updateProfilePictureUrl(principal.getName(), "/uploads/profile/" + filename);
            if (previousUrl != null && previousUrl.startsWith("/uploads/profile/")) {
                Path previous = uploadDir.resolve(previousUrl.substring("/uploads/profile/".length())).normalize();
                if (previous.getParent().equals(uploadDir)) Files.deleteIfExists(previous);
            }
            redirectAttributes.addFlashAttribute("successMessage", "Profile picture updated.");
        } catch (IOException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Could not save the image. Please try again.");
        }
        return "redirect:/student/profile";
    }

    @PostMapping("/profile/photo/remove")
    public String removeProfilePicture(Principal principal, RedirectAttributes redirectAttributes) {
        User user = userService.getByEmail(principal.getName());
        String pictureUrl = user.getProfilePictureUrl();
        userService.updateProfilePictureUrl(principal.getName(), null);
        if (pictureUrl != null && pictureUrl.startsWith("/uploads/profile/")) {
            try {
                Path uploadDir = Paths.get("uploads", "profile").toAbsolutePath().normalize();
                Path previous = uploadDir.resolve(pictureUrl.substring("/uploads/profile/".length())).normalize();
                if (previous.getParent().equals(uploadDir)) Files.deleteIfExists(previous);
            } catch (IOException ignored) {
                // The database is already updated; an orphaned file does not affect the account.
            }
        }
        redirectAttributes.addFlashAttribute("successMessage", "Profile picture removed.");
        return "redirect:/student/profile";
    }

    // ---------- Bookmarks ----------
    @GetMapping("/bookmarks")
    public String bookmarks(Principal principal, Model model) {
        User user = userService.getByEmail(principal.getName());
        model.addAttribute("opportunities", userService.getBookmarkedOpportunities(user));
        return "bookmarks";
    }

    @PostMapping("/bookmarks/{opportunityId}/add")
    public String addBookmark(@PathVariable String opportunityId,
                              Principal principal,
                              RedirectAttributes redirectAttributes) {
        userService.addBookmark(principal.getName(), opportunityId);
        redirectAttributes.addFlashAttribute("successMessage", "Saved to your bookmarks.");
        return "redirect:/opportunities/" + opportunityId;
    }

    @PostMapping("/bookmarks/{opportunityId}/remove")
    public String removeBookmark(@PathVariable String opportunityId,
                                 @RequestParam(name = "from", required = false) String from,
                                 Principal principal,
                                 RedirectAttributes redirectAttributes) {
        userService.removeBookmark(principal.getName(), opportunityId);
        redirectAttributes.addFlashAttribute("successMessage", "Bookmark removed.");
        // Only two fixed destinations: no user-supplied URL, so no open redirect.
        if ("details".equals(from)) {
            return "redirect:/opportunities/" + opportunityId;
        }
        return "redirect:/student/bookmarks";
    }

    // ---------- Application tracker ----------
    @GetMapping("/applications")
    public String applications(Principal principal, Model model) {
        User user = userService.getByEmail(principal.getName());
        List<ApplicationRecord> records = applicationService.getForUser(user.getId());

        model.addAttribute("records", records);
        model.addAttribute("opportunityMap", opportunityService.findAsMap(
                records.stream().map(ApplicationRecord::getOpportunityId).toList()));
        model.addAttribute("statuses", ApplicationStatus.values());
        return "applications";
    }

    @PostMapping("/applications")
    public String trackApplication(@RequestParam String opportunityId,
                                   @RequestParam ApplicationStatus status,
                                   Principal principal,
                                   RedirectAttributes redirectAttributes) {
        User user = userService.getByEmail(principal.getName());
        applicationService.track(user.getId(), opportunityId, status);
        redirectAttributes.addFlashAttribute("successMessage", "Tracker updated.");
        return "redirect:/opportunities/" + opportunityId;
    }

    @PostMapping("/applications/{recordId}/status")
    public String updateStatus(@PathVariable String recordId,
                               @RequestParam ApplicationStatus status,
                               Principal principal,
                               RedirectAttributes redirectAttributes) {
        User user = userService.getByEmail(principal.getName());
        applicationService.updateStatus(user.getId(), recordId, status);
        redirectAttributes.addFlashAttribute("successMessage", "Status updated.");
        return "redirect:/student/applications";
    }

    @PostMapping("/applications/{recordId}/delete")
    public String deleteRecord(@PathVariable String recordId,
                               Principal principal,
                               RedirectAttributes redirectAttributes) {
        User user = userService.getByEmail(principal.getName());
        applicationService.delete(user.getId(), recordId);
        redirectAttributes.addFlashAttribute("successMessage", "Tracking record removed.");
        return "redirect:/student/applications";
    }
}
