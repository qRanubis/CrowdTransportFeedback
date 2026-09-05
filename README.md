# CrowdTransportFeedback — Milestone 5

CrowdTransportFeedback is an offline-first Android application backed by one modular Spring Boot monolith. Android persists feedback in Room and synchronizes it through WorkManager; the backend owns authentication, authorization, feedback ownership, validation, and PostgreSQL persistence.

## Architecture

- `app/`: Kotlin/Jetpack Compose Android client. Room schema **version 6** retains distributed `feedbackId` UUIDs, local-only `localId`, tombstones, and chronological pending synchronization. Authenticated local feedback stores nullable author identity/avatar provenance and creator-scoped permanent rejection details.
- `backend/`: Java 21, Maven, Spring Boot, Web, Security, Data JPA, Validation, Flyway, Actuator, JJWT, and PostgreSQL. It remains one modular monolith with `auth`, `security`, `user`, `feedback`, and `common` packages, not microservices.
- `docker-compose.yml`: PostgreSQL runs on host port 5434 and the backend on port 8080, with a persistent named database volume.

## Authentication, users, and authorization

Registration always creates a `USER`. Email addresses are normalized with trim + lowercase. New registrations use the project-specific rule that the address must end with a dot followed by exactly 2 or 3 letters, for example `.ro`, `.it`, or `.com`; one-letter and 4+-letter endings are rejected on both Android and backend. Login keeps a broader syntactic email check so existing development accounts such as `.local` remain usable. Registration passwords require at least 8 characters and must contain at least one lowercase letter, one uppercase letter, one digit, and one symbol.

Each account also has a permanent public username chosen at registration. Usernames are unique, 3–20 characters long, and contain only lowercase ASCII letters and digits (`^[a-z0-9]{3,20}$`). Existing development users are backfilled by Flyway migration `V3__add_usernames.sql`. Public feedback displays the username rather than exposing the author email address.

Login and registration return a short-lived access JWT plus an opaque rotating refresh token. Refresh sessions use a configurable sliding inactivity window, and only token hashes are stored by the backend. Successful login or registration schedules the existing unique one-time WorkManager synchronization.

Roles are enforced on the backend. `USER` and `ADMIN` may read and create feedback. A feedback item may be deleted by its author or by an `ADMIN`; another `USER` cannot delete it. Feedback ownership always comes from the authenticated backend security context, never from client-provided ownership metadata.

An optional development ADMIN can be bootstrapped through the environment configuration documented in `.env.example`. There is no client-side promotion flow.

## Structured feedback and overall rating

A new feedback item requires:

- transport type and a valid line from the Bucharest transit catalog;
- punctuality rating from 1 to 5;
- cleanliness rating from 1 to 5;
- crowding-comfort rating from 1 to 5, where 1 means very crowded/poor and 5 means plenty of space/good;
- GPS coordinates;
- an optional trimmed comment.

The user no longer enters a separate overall rating. The application derives it automatically as:

`overallRating = (punctuality + cleanliness + crowdingComfort) / 3`

The backend independently derives the same value instead of trusting a client-provided overall score. It rounds the result to one decimal place and stores that decimal value in PostgreSQL `feedback.score` as `DOUBLE PRECISION`, so ratings such as `3.7` are persisted instead of being rounded to integer `4`. Android displays the calculated value with one decimal place. The three component ratings remain the source of truth; the legacy local Room integer `score` is retained only as a compatibility fallback for older local rows.

## Android sessions and offline behavior

Authentication credentials are stored in Android Keystore-backed encrypted preferences and excluded from backup/device transfer. An interceptor adds bearer access tokens, and the OkHttp authenticator performs automatic refresh. `SessionManager` serializes refreshes through a process-wide coroutine `Mutex` so concurrent requests do not rotate the same refresh token independently.

A stored user session restores without login-screen flashing. Temporary refresh network/server failures retain the local session and permit offline use; definitive refresh rejection clears it. Explicit Logout performs best-effort server revocation but always clears the local session and returns to Login, including while offline.

New local feedback is stamped with the authenticated creator before persistence. A `PENDING_CREATE` row uploads only when that stored creator matches the currently authenticated user. Pending or otherwise unsynchronized feedback is visible only to its creator on the shared device; synchronized feedback remains globally visible to authenticated users. Switching accounts therefore neither exposes another user's unpublished pending feedback nor uploads it under the wrong account. Legacy ownerless pending rows remain unclaimed and hidden from other accounts.

If the original author returns, their pending feedback becomes visible again and can synchronize normally. The author can also delete their own pending feedback locally before upload. Synchronized deletes use the existing tombstone workflow and are authorized again by the backend.

WorkManager keeps the existing unique one-time and periodic jobs, network constraints, exponential backoff, tombstone-first processing, pending-create upload, remote reconciliation, immediate local Save/Delete, uncertain-POST confirmation, and the process-wide synchronization mutex.

## Database migrations

- `V1__initial_schema.sql`: creates users, refresh sessions, and feedback tables.
- `V2__align_feedback_rating_column_types.sql`: aligns feedback rating column types with the JPA model.
- `V3__add_usernames.sql`: adds permanent unique usernames and safely backfills existing development users.
- `V4__store_decimal_overall_score.sql`: converts PostgreSQL `feedback.score` to `DOUBLE PRECISION` and recalculates existing scores from the three component ratings to one decimal place.

On Android, Room migration `4 -> 5` adds `createdByUsername` without discarding existing local feedback.

## Local development

1. Copy `.env.example` to an untracked `.env` and provide your local development values.
2. Run `docker compose config`, then `docker compose up --build`.
3. Verify the backend health endpoint at `http://localhost:8080/actuator/health`.

The Windows host backend URL is `http://localhost:8080`. The Android emulator uses `http://10.0.2.2:8080/`. Cleartext HTTP is narrowly enabled only for that emulator-to-host development destination; production endpoints should use HTTPS.

### One-time json-server development cutover

json-server is no longer in the normal path. Prototype records have no authenticated ownership and are not imported or assigned silently. For the first full Milestone 4 test, use a fresh PostgreSQL development database and clear Android application data/reinstall if the device contains old json-server-era rows. This is a manual development cutover; application code does not wipe data automatically.

## Tests and checks

Backend verification uses `./mvnw test` and `./mvnw package` from `backend/`. Android verification uses `./gradlew testDebugUnitTest`, `./gradlew lintDebug`, `./gradlew assembleDebug`, and `./gradlew connectedDebugAndroidTest` from the project root, with an emulator required for connected tests.

Milestone 4 manual integration testing also covers registration and username persistence, automatic login after registration, PostgreSQL ownership, calculated overall rating, offline logout, account-isolated pending feedback, synchronization when the creator returns, author delete, non-owner delete denial, and ADMIN delete visibility/authorization.
# Milestone 5: profiles and gamification

Milestone 5 adds authenticated **My Profile** and username-addressed public profiles, three neutral built-in avatars (`COMMUTER`, `NAVIGATOR`, and `EXPLORER`), achievements, and one leaderboard screen with XP/Achievements/Contributions metrics and All Time/This Month periods. Public responses intentionally omit email and security/session data. Leaderboards return at most 100 rows plus a separate current-user rank (monthly zero values are unranked).

Gamification is server authoritative. Accepted feedback awards 10 XP; the lifetime first contribution and first transport type bonuses award 40 XP each, and a lifetime first `transportType + normalized line` bonus awards 30 XP. The immutable event ledger and database uniqueness constraint make retries idempotent. Deletion creates at most one -10 XP reversal; first-time bonuses and unlocked achievements remain permanent. The ledger is deliberately source-based so a future `REPORT_ACCEPTED` event can be introduced without changing its model.

Levels are derived, never client supplied: Passenger (0), Contributor (100), Observer (225), Explorer (375), Route Explorer (550), Network Explorer (775), Navigator (1050), City Navigator (1375), Transit Mapper (1750), Network Mapper (2175), Mobility Analyst (2650), Senior Mapper (3175), Transit Specialist (3750), Network Specialist (4375), and Urban Mobility Expert (5000). Level 15 is the maximum, but XP remains uncapped.

The exact 28-achievement catalog is grouped into Contribution (7), Network Exploration (5), Transport (10), and UTC-day Consistency (6). The API supplies progress and unlock timestamps; achievements grant no XP and remain unlocked after deletion. Users may pin zero to three distinct unlocked badges in a persisted order.

The backend and local store enforce a canonicalized 30-minute per-user, per-transport/line cooldown. Pending records participate locally. A server cooldown conflict becomes a creator-visible `REJECTED` row with `feedback_cooldown`, rather than an endless WorkManager retry. Pending rows never alter authoritative profile XP. Profile/leaderboard requests show server data and a clear unavailable state when offline.

M5 endpoints (all authenticated) are `GET /api/profile/me`, `GET /api/profile/{username}`, `GET /api/profile/me/achievements`, `PATCH /api/profile/me/avatar`, `PUT /api/profile/me/pinned-achievements`, and `GET /api/leaderboard?metric=XP&period=ALL_TIME&limit=100`. Feedback responses additionally carry the author's avatar plus server-awarded XP/new-achievement metadata.

Flyway `V5__profiles_and_gamification.sql` non-destructively adds avatars, canonical lines, the event ledger, permanent unlocks, ordered pins, indexes, constraints, and an idempotent historical XP backfill using original timestamps. Startup replays each user's feedback chronologically and records every historical achievement at the earliest feedback timestamp at which its threshold was reached, so monthly achievement rankings are not inflated during deployment. Room is version 6; `MIGRATION_5_6` preserves rows while adding nullable author-avatar and permanent-rejection-reason columns, and migration tests exercise both version 5 and older upgrade paths.

Level progress uses an explicit server contract: `levelStartXp`, `xpIntoLevel`, `xpNeededForNextLevel`, and the absolute `nextLevelThreshold`; all next-level fields are absent at maximum level. Same-line cooldown checks use the open UTC timestamp interval 30 minutes before through 30 minutes after an incoming timestamp, making offline synchronization order irrelevant while allowing submissions exactly 30 minutes apart. Monthly leaderboard queries use the explicit UTC half-open interval `[start of month, start of next month)`.

Verification commands: `cd backend && ./mvnw test && ./mvnw package`; from the repository root run `./gradlew testDebugUnitTest lintDebug assembleDebug connectedDebugAndroidTest`, then `docker compose up --build -d`, `docker compose ps`, and check `http://localhost:8080/actuator/health`.

## Milestone 6: Google Maps and feedback visualization

The Android feedback list now opens an interactive Google Map backed by the same observable Room data. The public map shows one marker per synchronized feedback row with valid coordinates, offers All/Bus/Metro/Tram/Trolleybus/Night bus filters, and visually distinguishes transport types. A marker info window shows its line, overall rating, and safe public username; tapping that info window opens the existing Feedback Detail screen, with Back returning to the map.

The map falls back to a city-level view of Bucharest. Location permission is optional: the screen remains available without it, while **Enable location** requests foreground permission and **My location** performs a one-shot recenter when a position is available. There is no continuous or background tracking. Map tiles remain dependent on Google Maps network availability; feedback stays safely stored in Room when tiles are unavailable, and no custom offline tile cache is provided.

For local setup, add a `MAPS_API_KEY` property with your key to the gitignored root `local.properties`, then rebuild the app. If it is absent or blank, the app compiles and the Map screen displays a setup message instead of initializing Maps. Restrict the Android key in Google Cloud to this application's package and signing certificate; never commit or log it.

M6 deliberately renders individual synchronized feedback only. Trust-score/geospatial aggregation and heatmap visualization are deferred to Milestone 7.

## Milestone 7: geospatial trust analytics

The Map now has **Feedback** and **Heatmap** modes. Feedback mode keeps authoritative,
synchronized reports and groups overlapping reports into the same deterministic cell;
its area sheet shows the newest five and pages the complete local list 20 at a time.
Heatmap mode requests one authoritative aggregate per populated cell from the backend,
supports Trust, Crowding, Punctuality, and Cleanliness, and provides clickable area details.
Filters cover 24 hours, 7 days, 30 days (default), or all time, plus transport type and a
catalog-backed line. Changing transport clears the previous line.

### Aggregation model

Coordinates are projected to spherical Web Mercator metres (`R = 6,378,137 m`) and
assigned with `floor(projectedCoordinate / 250)`. The inverse-projected cell midpoint is
used for display. This is a stable, explainable approximation, but Mercator scale distortion
means cells are only approximately 250 m on the ground (particularly away from Bucharest).
Invalid coordinates are discarded.

For each metric, included reports are first grouped by authenticated contributor. Each
contributor receives a recency-weighted average using `0.5^(ageDays / 30)` and no more
than 1.0 total effective weight. The public score is then Bayesian-smoothed:

```
(2 × 3.0 + Σ contributorWeight × contributorAverage) / (2 + Σ contributorWeight)
```

Scores are clamped to 1..5. Thus one fresh 5/5 report yields about 3.67, not 5.0.
Confidence is separate evidence metadata: 1–2 unique contributors is LOW, 3–5 MEDIUM,
and 6+ HIGH. Empty cells are omitted. Crowding is explicitly **in-vehicle comfort**
(1 crowded/uncomfortable, 5 spacious/comfortable); punctuality does not imply a cause.

### Development demo data

The deterministic seed is off by default and has two guards: the `demo` Spring profile
and `app.demo-seed-enabled=true`. It creates eight local-only users and 50 idempotent,
varied reports around five documented Bucharest demo centres. Start it from `backend/`:

```bash
SPRING_PROFILES_ACTIVE=demo DEMO_SEED_ENABLED=true JWT_SECRET='local-development-secret-at-least-32-bytes' ./mvnw spring-boot:run
```

The demo password is `demo-only-password` and is **LOCAL DEVELOPMENT ONLY**. Existing
seed timestamps are not rewritten. To recreate timestamps, stop the stack and reset the
local database volume (`docker compose down -v`), then start the demo configuration again.
Never enable the demo profile/property in production.

The Google Maps key remains local in `local.properties` as described above and must not be
committed.

### M7 limitations

Heatmaps represent user-reported experiences, not objective real-time conditions. M7 has
no road-traffic API, GTFS route geometry, segment-level route analytics, real-time
prediction, reverse geocoding, or PostGIS. It does not infer neighborhood names. The 250 m
grid is approximate, and the heat visualization uses aggregated cell centres rather than
route geometry. Offline synchronized Feedback mode remains available when analytics is
unavailable; heatmap responses are not cached in Room.

## Milestone 8 — administration and community moderation

Feedback by another author can be reported online once per account using one of six reasons; `OTHER` requires details and all details are limited to 250 characters. Reports move from `PENDING` to `DISMISSED` when an administrator keeps feedback, `CONFIRMED` when an administrator deletes it, or `CLOSED` when its author deletes it first. Reports are deliberately excluded from heatmap and trust calculations, and their history survives hard deletion.

Confirmed reports advance the **Watchful Commuter** (1), **Community Guardian** (5), and **Trusted Reporter** (15) achievements without awarding XP or changing leaderboard position. Pending, dismissed, and closed reports do not advance them, and unlocked moderation achievements use the existing pinning system.

The online-only Android Admin Dashboard is visible only for authenticated `ADMIN` accounts and provides Overview, grouped Reports moderation, Feedback, read-only Users, and Reporting/CSV sections. The backend independently protects every `/api/admin/**` endpoint with the `ADMIN` role. Administrators remain externally provisioned exclusively through `AdminBootstrap` environment configuration; the app provides no role or account-management operations.

Migration `V6__moderation_and_admin.sql` adds durable `feedback_report` and `admin_audit_log` tables plus moderation indexes. Audit entries are persisted for report dismissal, reported-feedback deletion, and direct administrative deletion. CSV exports use the dashboard reporting filters and the Android system document picker; administrative responses are never stored in Room.

### Local M8 walkthrough

1. Start PostgreSQL/backend with the documented environment-based administrator and start the Android app. Demo data remains opt-in (`DEMO_SEED_ENABLED=true`) and does not seed reports.
2. As one normal user, open another user's synchronized feedback, submit a report, and verify **Reported · Pending review** replaces the action.
3. As the administrator, open **Admin**, review the grouped queue, then Keep or confirm Delete. Reopen the reporter profile to inspect achievement progress.
4. Verify author deletion closes pending reports without progress, and direct admin deletion applies the same confirmation semantics as queue deletion.
5. Use Reporting → **Export CSV** and select a destination through Android's document picker.
6. Stop the backend and verify the dashboard shows **Admin dashboard requires a connection** while existing offline feedback remains available.
