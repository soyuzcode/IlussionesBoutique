# Backend for Ilussiones Boutique

## Database changes

Run `database/boutique_features.sql` in the Supabase SQL editor before deploying
(Hibernate runs with `ddl-auto=validate`).

## Optional configuration

| Feature | Environment variables |
| --- | --- |
| Frontend URL (reset links, OAuth return) | `APP_FRONTEND_URL` |
| Session cookie lifetime in days (default 14) | `APP_SESSION_TTL_DAYS` |
| Extra frontend origins allowed to use the session cookie (comma-separated patterns) | `APP_CORS_ALLOWED_ORIGINS` |
| Session cookie `Secure` / `SameSite` (defaults `true` / `None`) | `APP_SESSION_COOKIE_SECURE`, `APP_SESSION_COOKIE_SAME_SITE` |
| SMTP for password-reset and reminder e-mails | `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`, `APP_MAIL_FROM` |
| Password reset | `APP_PASSWORD_RESET_TTL_MINUTES` (default 30), `APP_PASSWORD_RESET_LOG_LINK` (dev only) |
| Appointment reminder e-mails | `APP_REMINDERS_ENABLED`, `APP_REMINDERS_NOTIFY_EMAIL`, `APP_REMINDERS_LEAD_HOURS` (default 24), `APP_REMINDERS_CRON` |
| Calendar sync | `APP_CALENDAR_REDIRECT_BASE_URL` (public backend URL), `APP_TIME_ZONE`, `APP_CALENDAR_TOKEN_KEY` |
| Google Calendar | `GOOGLE_CALENDAR_CLIENT_ID`, `GOOGLE_CALENDAR_CLIENT_SECRET` |
| Microsoft Outlook | `MICROSOFT_CALENDAR_CLIENT_ID`, `MICROSOFT_CALENDAR_CLIENT_SECRET`, `MICROSOFT_CALENDAR_TENANT` |

OAuth redirect URIs to register with the providers:

- Google: `<APP_CALENDAR_REDIRECT_BASE_URL>/api/calendar/google/callback` (scope `calendar.events`)
- Microsoft: `<APP_CALENDAR_REDIRECT_BASE_URL>/api/calendar/microsoft/callback` (scopes `Calendars.ReadWrite`, `offline_access`)

Without these variables the features degrade gracefully: reset links and
reminders are not e-mailed, and calendar providers show as "not configured".
