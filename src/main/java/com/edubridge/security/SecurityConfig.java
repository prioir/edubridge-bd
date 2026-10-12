package com.edubridge.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import java.io.IOException;

/**
 * Access rules. CSRF protection stays ON: every POST form uses th:action,
 * which adds the hidden CSRF token automatically.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/css/**", "/js/**", "/images/**", "/error", "/access-denied", "/actuator/health/**").permitAll()
                .requestMatchers("/login", "/register", "/help").permitAll()
                .requestMatchers(HttpMethod.GET, "/", "/opportunities", "/opportunities/*").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/student/**").hasRole("STUDENT")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .successHandler(this::redirectAfterLogin)
                .failureUrl("/login?error")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")                 // POST only (CSRF-protected)
                .logoutSuccessUrl("/login?logout")
            )
            .headers(headers -> headers
                .contentTypeOptions(options -> {})
                .frameOptions(frame -> frame.deny())
                .referrerPolicy(referrer -> referrer.policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)))
            .exceptionHandling(ex -> ex.accessDeniedPage("/access-denied"));

        return http.build();
    }

    /** Admins land on the admin dashboard, students on their own dashboard. */
    private void redirectAfterLogin(HttpServletRequest request, HttpServletResponse response,
                                    Authentication authentication) throws IOException {
        boolean admin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        response.sendRedirect(request.getContextPath() + (admin ? "/admin" : "/student"));
    }
}
