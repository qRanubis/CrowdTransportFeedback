# CrowdTransportFeedback — Milestone 4

CrowdTransportFeedback is an offline-first Android application backed by one modular Spring Boot monolith. Android persists feedback in Room and synchronizes it through WorkManager; the backend owns authentication, authorization, feedback ownership, validation, and PostgreSQL persistence.

## Architecture

- `app/`: Kotlin/Jetpack Compose Android client. Room schema **version 5** retains distributed `feedbackId` UUIDs, local-only `localId`, tombstones, and `PENDING_DELETE -> PENDING_CREATE -> reconciliation` synchronization. Authenticated local feedback stores nullable `createdByUserId` and `createdByUsername` provenance.
- `backend/`: Java 21, Maven, Spring Boot, Web, Security, Data JPA, Validation, Flyway, Actuator, JJWT, and PostgreSQL. It remains one modular monolith with `auth`, `security`, `user`, `feedback`, and `common` packages, not microservices.
- `docker-compose.yml`: PostgreSQL runs on host port 5434 and the backend on port 8080, with a persistent named database volume.

## Authentication, users, and authorization

Registration always creates a `USER`. Email addresses are normalized with trim + lowercase and validated syntactically on both Android and backend. Registration passwords require at least 8 characters and must contain at least one lowercase letter, one uppercase letter, one digit, and one symbol. Login keeps its own simpler credential validation so registration-strength rules do not invalidate existing accounts.

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

The backend independently derives the same value instead of trusting a client-provided overall score. Android displays the calculated value with one decimal place. The legacy integer `score` field is currently retained for compatibility, while the three component ratings remain the source of truth for the displayed overall rating.

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
