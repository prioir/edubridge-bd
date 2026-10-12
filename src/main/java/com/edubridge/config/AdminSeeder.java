package com.edubridge.config;

import com.edubridge.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Creates the first admin account at startup from ADMIN_EMAIL and ADMIN_PASSWORD.
 * This is the ONLY way to get an admin: public registration always creates a STUDENT.
 */
@Component
public class AdminSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final UserService userService;
    private final String adminEmail;
    private final String adminPassword;

    public AdminSeeder(UserService userService,
                       @Value("${app.admin.email:}") String adminEmail,
                       @Value("${app.admin.password:}") String adminPassword) {
        this.userService = userService;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        if (adminEmail.isBlank() || adminPassword.isBlank()) {
            log.info("ADMIN_EMAIL / ADMIN_PASSWORD not set: no admin account was created.");
            return;
        }
        if (adminPassword.length() < 8) {
            log.warn("ADMIN_PASSWORD is shorter than 8 characters: admin account was NOT created.");
            return;
        }
        if (userService.emailExists(adminEmail)) {
            return;
        }
        userService.createAdmin("Administrator", adminEmail, adminPassword);
        log.info("Admin account created for {}", adminEmail);
    }
}
