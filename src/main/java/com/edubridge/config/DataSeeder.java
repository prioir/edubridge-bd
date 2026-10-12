package com.edubridge.config;

import com.edubridge.entity.Category;
import com.edubridge.entity.Opportunity;
import com.edubridge.repository.OpportunityRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/** Inserts sample opportunities when the database is empty. Runs only with the "dev" profile. */
@Component
@Profile("dev")
public class DataSeeder implements CommandLineRunner {

    private final OpportunityRepository repository;

    public DataSeeder(OpportunityRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) {
        if (repository.count() > 0) {
            return;
        }
        repository.saveAll(List.of(
            sample("Sample Merit Scholarship 2026", Category.SCHOLARSHIP, "Example Foundation",
                    "Full tuition support for high-achieving undergraduate students.", 45, true),
            sample("Sample Software Engineering Internship", Category.INTERNSHIP, "Example Tech Ltd.",
                    "A 3-month paid internship working on real web projects.", 20, true),
            sample("Sample National Programming Contest", Category.COMPETITION, "Example Univ. IT Club",
                    "Inter-university team programming contest with cash prizes.", 10, true),
            sample("Sample Data Analytics Bootcamp", Category.TRAINING, "Example Skills Academy",
                    "Hands-on training in Excel, SQL, and Power BI for students.", 5, true),
            sample("Sample Expired Research Grant", Category.SCHOLARSHIP, "Example Research Council",
                    "A small research grant. Deadline already passed (tests expired display).", -5, true),
            sample("Sample Unpublished Draft", Category.TRAINING, "Example Org",
                    "This draft must NOT appear on public pages.", 40, false)
        ));
    }

    private Opportunity sample(String title, Category category, String org,
                               String shortDescription, int daysFromNow, boolean published) {
        Opportunity o = new Opportunity();
        o.setTitle(title);
        o.setCategory(category);
        o.setOrganizationName(org);
        o.setShortDescription(shortDescription);
        o.setDetailedDescription(shortDescription + "\n\nThis is sample data. Replace it with real details.");
        o.setEligibility("Currently enrolled university student in Bangladesh.");
        o.setDeadline(LocalDate.now().plusDays(daysFromNow));
        o.setApplicationUrl("https://example.com/apply");
        o.setPublished(published);
        o.setVerified(true);
        return o;
    }
}
