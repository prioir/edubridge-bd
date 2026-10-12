package com.edubridge.controller;

import com.edubridge.entity.Category;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import com.edubridge.entity.Opportunity;
import com.edubridge.service.OpportunityService;
import com.edubridge.service.UserService;
import com.edubridge.service.AuditService;
import java.security.Principal;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Everything under /admin needs the ADMIN role (see SecurityConfig). */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private final OpportunityService opportunityService;
    private final UserService userService;
    private final AuditService auditService;

    public AdminController(OpportunityService opportunityService, UserService userService, AuditService auditService) {
        this.opportunityService = opportunityService;
        this.userService = userService;
        this.auditService = auditService;
    }

    // ---------- Dashboard ----------
    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("totalOpportunities", opportunityService.countAll());
        model.addAttribute("publishedCount", opportunityService.countPublished());
        model.addAttribute("unpublishedCount", opportunityService.countUnpublished());
        model.addAttribute("studentCount", userService.countStudents());
        model.addAttribute("recentOpportunities", opportunityService.getRecentForAdmin());
        return "admin/dashboard";
    }

    // ---------- List ----------
    @GetMapping("/opportunities")
    public String list(Model model) {
        model.addAttribute("opportunities", opportunityService.getAll());
        return "admin/opportunities";
    }

    // ---------- Create ----------
    @GetMapping("/opportunities/new")
    public String newForm(Model model) {
        model.addAttribute("opportunity", new Opportunity());
        prepareForm(model, "/admin/opportunities", false);
        return "admin/opportunity-form";
    }

    @PostMapping("/opportunities")
    public String create(@Valid @ModelAttribute("opportunity") Opportunity opportunity,
                         BindingResult result,
                         Model model,
                         RedirectAttributes redirectAttributes, Principal principal) {
        if (result.hasErrors()) {
            prepareForm(model, "/admin/opportunities", false);
            return "admin/opportunity-form";
        }
        Opportunity created = opportunityService.create(opportunity);
        if (created != null) auditService.record(principal.getName(), "OPPORTUNITY_CREATED", created.getId(), created.getTitle());
        redirectAttributes.addFlashAttribute("successMessage", "Opportunity created.");
        return "redirect:/admin/opportunities";
    }

    // ---------- Edit ----------
    @GetMapping("/opportunities/{id}/edit")
    public String editForm(@PathVariable String id, Model model) {
        model.addAttribute("opportunity", opportunityService.getById(id));
        prepareForm(model, "/admin/opportunities/" + id, true);
        return "admin/opportunity-form";
    }

    @PostMapping("/opportunities/{id}")
    public String update(@PathVariable String id,
                         @Valid @ModelAttribute("opportunity") Opportunity opportunity,
                         BindingResult result,
                         Model model,
                         RedirectAttributes redirectAttributes, Principal principal) {
        if (result.hasErrors()) {
            prepareForm(model, "/admin/opportunities/" + id, true);
            return "admin/opportunity-form";
        }
        opportunityService.update(id, opportunity);
        auditService.record(principal.getName(), "OPPORTUNITY_UPDATED", id, opportunity.getTitle());
        redirectAttributes.addFlashAttribute("successMessage", "Opportunity updated.");
        return "redirect:/admin/opportunities";
    }

    // ---------- Publish / unpublish ----------
    @PostMapping("/opportunities/{id}/publish")
    public String togglePublish(@PathVariable String id, RedirectAttributes redirectAttributes, Principal principal) {
        Opportunity current = opportunityService.getById(id);
        boolean nowPublished = !current.isPublished();
        opportunityService.setPublished(id, nowPublished);
        auditService.record(principal.getName(), nowPublished ? "OPPORTUNITY_PUBLISHED" : "OPPORTUNITY_UNPUBLISHED", id, current.getTitle());
        redirectAttributes.addFlashAttribute("successMessage",
                nowPublished ? "Opportunity published." : "Opportunity unpublished.");
        return "redirect:/admin/opportunities";
    }

    // ---------- Delete (the browser asks for confirmation first) ----------
    @PostMapping("/opportunities/{id}/delete")
    public String delete(@PathVariable String id, RedirectAttributes redirectAttributes, Principal principal) {
        String title = opportunityService.getById(id).getTitle();
        opportunityService.delete(id);
        auditService.record(principal.getName(), "OPPORTUNITY_DELETED", id, title);
        redirectAttributes.addFlashAttribute("successMessage", "Opportunity deleted.");
        return "redirect:/admin/opportunities";
    }

    @GetMapping("/students")
    public String students(Model model) {
        List<Map<String, Object>> students = userService.getStudents().stream().map(user -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("fullName", user.getFullName()); row.put("email", user.getEmail());
            row.put("university", user.getUniversity()); row.put("studyLevel", user.getStudyLevel());
            row.put("createdAt", user.getCreatedAt()); return row;
        }).toList();
        model.addAttribute("students", students);
        return "admin/students";
    }

    @GetMapping("/audit")
    public String audit(Model model) { model.addAttribute("events", auditService.latest()); return "admin/audit"; }

    private void prepareForm(Model model, String formAction, boolean editing) {
        model.addAttribute("categories", Category.values());
        model.addAttribute("formAction", formAction);
        model.addAttribute("editing", editing);
    }
}
