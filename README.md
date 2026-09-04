# CrowdTransportFeedback — Milestone 4

CrowdTransportFeedback is an offline-first Android application backed by one modular Spring Boot monolith. Android persists feedback in Room and synchronizes it through WorkManager; the backend owns authentication, authorization, feedback ownership, and PostgreSQL persistence.

## Architecture

- `app/`: Kotlin/Jetpack Compose Android client. Room schema **version 4** retains distributed `feedbackId` UUIDs, local-only `localId`, tombstones, and `PENDING_DELETE -> PENDING_CREATE -> reconciliation` synchronization. Authenticated local feedback also stores nullable `createdByUserId` provenance.
- `backend/`: Java 21, Maven, Spring Boot, Web, Security, Data JPA, Validation, Flyway, Actuator, JJWT, and PostgreSQL. It remains one modular monolith with `auth`, `security`, `user`, `feedback`, and `common` packages, not microservices.
- `docker-compose.yml`: PostgreSQL runs on host port 5434 and the backend on port 8080, with a persistent named database volume.

## Authentication and authorization

Registration always creates a `USER`. Login and registration return a short-lived access JWT plus an opaque rotating refresh token. Refresh sessions use a configurable sliding inactivity window, and only token hashes are stored by the backend. Successful login or registration schedules the existing unique one-time WorkManager synchronization.

Roles are enforced by Spring Security: `USER` and `ADMIN` may read and create feedback; only `ADMIN` may delete. Feedback ownership always comes from the authenticated backend security context, never from client-provided ownership metadata. The backend validates the complete structured feedback contract, including transport type, line, all four ratings, coordinates, UUID, and timestamp.

An optional development ADMIN can be bootstrapped through the environment configuration documented in `.env.example`. There is no client-side promotion flow.

## Android sessions and offline behavior

Authentication credentials are stored in Android Keystore-backed encrypted preferences and excluded from backup/device transfer. An interceptor adds bearer access tokens, and the OkHttp authenticator performs automatic refresh. `SessionManager` serializes refreshes through a process-wide coroutine `Mutex` so concurrent requests do not rotate the same refresh token independently.

A stored user session restores without login-screen flashing. Temporary refresh network/server failures retain the local session and permit offline use; definitive refresh rejection clears it. Explicit Logout performs best-effort server revocation but always clears the local session and returns to Login, including while offline.

New local feedback is stamped with the authenticated creator before persistence. A `PENDING_CREATE` row uploads only when that stored creator matches the currently authenticated user. Switching accounts therefore cannot upload another user's offline-created feedback. Legacy ownerless rows remain unclaimed.

WorkManager keeps the existing unique one-time and periodic jobs, network constraints, exponential backoff, tombstone-first processing, pending-create upload, remote reconciliation, immediate local Save/Delete, uncertain-POST confirmation, and the process-wide synchronization mutex.

## Local development

1. Copy `.env.example` to an untracked `.env` and provide your local development values.
2. Run `docker compose config`, then `docker compose up --build`.
3. Verify the backend health endpoint at `http://localhost:8080/actuator/health`.

The Windows host backend URL is `http://localhost:8080`. The Android emulator uses `http://10.0.2.2:8080/`. Cleartext HTTP is narrowly enabled only for that emulator-to-host development destination; production endpoints should use HTTPS.

### One-time json-server development cutover

json-server is no longer in the normal path. Prototype records have no authenticated ownership and are not imported or assigned silently. For the first full Milestone 4 test, use a fresh PostgreSQL development database and clear Android application data/reinstall if the device contains old json-server-era rows. This is a manual development cutover; application code does not wipe data automatically.

## Tests and checks

Run backend tests/package from `backend/` with `./mvnw test` and `./mvnw package`. Run Android checks from the root with `./gradlew testDebugUnitTest`, `./gradlew lintDebug`, and `./gradlew assembleDebug`; an emulator is required for `./gradlew connectedDebugAndroidTest`. Docker validation uses `docker compose config`; `docker compose up --build` plus the health endpoint validates the complete runtime.
