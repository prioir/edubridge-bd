package com.edubridge.controller;

import com.edubridge.entity.Category;
import com.edubridge.service.OpportunityService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final OpportunityService opportunityService;

    public HomeController(OpportunityService opportunityService) {
        this.opportunityService = opportunityService;
    }

    @GetMapping("/help")
    public String help() { return "help"; }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("recentOpportunities", opportunityService.getRecentPublished());
        model.addAttribute("closingSoon", opportunityService.getClosingSoon());
        model.addAttribute("categories", Category.values());
        return "home";
    }
}
