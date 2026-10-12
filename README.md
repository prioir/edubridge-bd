# EduBridge BD

EduBridge BD is a Spring Boot, Thymeleaf and MongoDB student-opportunity portal.

## Requirements
- JDK 21
- Maven 3.9+
- MongoDB database

## Run locally
1. Set `MONGODB_URI` to your MongoDB connection string. A local default (`mongodb://localhost:27017/edubridge`) is used when it is not set.
2. Set `SPRING_PROFILES_ACTIVE=dev` for sample opportunities, or `prod` to disable sample data.
3. Run `mvn spring-boot:run`.
4. Register a student account at `/register`. To seed an admin, set `ADMIN_EMAIL` and `ADMIN_PASSWORD` (minimum 8 characters) before startup.

Never commit database credentials or admin passwords. If credentials were previously committed or shared in an archive, rotate them.

## Upload storage
- Profile photos: `uploads/profile/` (served as public avatar images).
- Student documents: `uploads/documents/{userId}/` (not exposed as public static files; download/delete routes verify ownership).
- Set `UPLOAD_DIR` to a persistent volume path in production. Back up this directory separately from MongoDB.
- Profile photos are limited to 2 MB. Private documents are limited to 5 MB and accept PDF/JPG/PNG only.

## Email reminders
Email reminders are opt-in per student and send a reminder at 09:00 server time on the day before a bookmarked opportunity's deadline. Configure `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, and `MAIL_FROM`. If SMTP is not configured, in-app reminders still work but email delivery will fail and be logged. Ensure the server timezone matches the deployment's intended timezone.

## Included functionality
- Student accounts, profile and profile photo; bookmarks and application tracking
- Opportunity search, category, verified-only and closing-soon filters
- Optional AI-ranked opportunity recommendations using an OpenAI-compatible API; falls back to profile-based rules when no API key is configured
- Deadline list/calendar with Google Calendar event links
- In-app deadline alerts and optional SMTP email reminders
- Private student document vault with ownership checks
- Admin opportunity management and audit trail for create/update/publish/unpublish/delete actions
- Light/dark theme preference, responsive Bootstrap layout, CSRF protection, role-based routes, validation and security headers

## Production checklist
- Use `SPRING_PROFILES_ACTIVE=prod` and a unique, least-privilege MongoDB user.
- Set `COOKIE_SECURE=true` when HTTPS is terminated correctly.
- Configure persistent upload storage, SMTP, log collection and backups.
- Run `mvn test` and `mvn package` with JDK 21 before deploying. A Dockerfile is included for reproducible Java 21 builds; `/actuator/health` is enabled for basic health checks.
- Confirm all opportunity content and official URLs are verified by an administrator.
- Restore-test backups and review authorization/upload security before public launch.

When AI ranking is enabled, profile fields (study level, department, university, interests) and opportunity metadata are sent to the configured AI provider; name, email, password and uploaded documents are not sent by this feature. AI ranking is optional: set `AI_API_KEY` (and optionally `AI_BASE_URL` and `AI_MODEL`) to enable it. Without a key or if the provider is unavailable, local profile-based ranking is used. Matching is advisory and does not guarantee eligibility. Google Calendar links open Google's event form; they do not use OAuth or sync automatically.
