package com.edubridge.entity;

import com.edubridge.form.RegistrationForm;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    private Opportunity validOpportunity() {
        Opportunity o = new Opportunity();
        o.setTitle("Test scholarship");
        o.setCategory(Category.SCHOLARSHIP);
        o.setOrganizationName("Test Org");
        o.setShortDescription("Short");
        o.setDetailedDescription("Long description");
        o.setEligibility("Students");
        o.setDeadline(LocalDate.now().plusDays(10));
        o.setApplicationUrl("https://example.com/apply");
        return o;
    }

    private Set<String> invalidFields(Object bean) {
        return validator.validate(bean).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());
    }

    @Test
    void validOpportunityHasNoViolations() {
        assertTrue(validator.validate(validOpportunity()).isEmpty());
    }

    @Test
    void emptyOpportunityReportsAllRequiredFields() {
        Set<String> fields = invalidFields(new Opportunity());

        assertTrue(fields.containsAll(Set.of("title", "category", "organizationName", "shortDescription",
                "detailedDescription", "eligibility", "deadline", "applicationUrl")));
    }

    @Test
    void nonHttpUrlIsRejected() {
        Opportunity o = validOpportunity();
        o.setApplicationUrl("javascript:alert(1)");

        assertTrue(invalidFields(o).contains("applicationUrl"));
    }

    @Test
    void registrationRequiresValidEmailAndLongPassword() {
        RegistrationForm form = new RegistrationForm();
        form.setFullName("Test");
        form.setEmail("not-an-email");
        form.setPassword("short");
        form.setConfirmPassword("short");

        Set<String> fields = invalidFields(form);

        assertTrue(fields.contains("email"));
        assertTrue(fields.contains("password"));
    }
}
