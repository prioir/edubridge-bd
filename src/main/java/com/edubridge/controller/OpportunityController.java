package com.edubridge.controller;

import com.edubridge.entity.ApplicationStatus;
import com.edubridge.entity.Category;
import com.edubridge.entity.Opportunity;
import com.edubridge.entity.User;
import com.edubridge.service.ApplicationService;
import com.edubridge.service.OpportunityService;
import com.edubridge.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

@Controller
public class OpportunityController {

    private final OpportunityService opportunityService;
    private final UserService userService;
    private final ApplicationService applicationService;

    public OpportunityController(OpportunityService opportunityService,
                                 UserService userService,
                                 ApplicationService applicationService) {
        this.opportunityService = opportunityService;
        this.userService = userService;
        this.applicationService = applicationService;
    }

    // Examples:  /opportunities   /opportunities?q=brac   /opportunities?category=SCHOLARSHIP&q=japan
    @GetMapping("/opportunities")
    public String list(@RequestParam(name = "q", required = false) String keyword,
                       @RequestParam(name = "category", required = false) Category category,
                       @RequestParam(name = "verified", defaultValue = "false") boolean verifiedOnly,
                       @RequestParam(name = "closingSoon", defaultValue = "false") boolean closingSoon,
                       Model model) {
        List<Opportunity> results = opportunityService.searchPublished(keyword, category);
        if (verifiedOnly) results = results.stream().filter(Opportunity::isVerified).toList();
        if (closingSoon) results = results.stream().filter(o -> !o.isExpired() && o.getDaysLeft() <= 14).toList();
        model.addAttribute("opportunities", results);
        model.addAttribute("verifiedOnly", verifiedOnly);
        model.addAttribute("closingSoon", closingSoon);
        model.addAttribute("categories", Category.values());
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedCategory", category);
        return "opportunities";
    }

    @GetMapping("/opportunities/{id}")
    public String details(@PathVariable String id, Principal principal, Model model) {
        Opportunity opportunity = opportunityService.getPublishedById(id);   // drafts -> 404
        model.addAttribute("opportunity", opportunity);

        // Bookmark / tracker buttons are only for logged-in students.
        boolean bookmarked = false;
        if (principal != null) {
            User user = userService.getByEmail(principal.getName());
            bookmarked = user.getBookmarkIds().contains(id);
            model.addAttribute("record",
                    applicationService.findForUserAndOpportunity(user.getId(), id).orElse(null));
        }
        model.addAttribute("bookmarked", bookmarked);
        model.addAttribute("statuses", ApplicationStatus.values());
        return "opportunity-details";
    }
}
