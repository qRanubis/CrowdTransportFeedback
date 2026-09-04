# CrowdTransportFeedback — Milestone 4

CrowdTransportFeedback is an offline-first Android application backed by one modular Spring Boot monolith. Android persists feedback in Room and synchronizes it through WorkManager; the backend owns authentication, authorization, feedback ownership, and PostgreSQL persistence.

## Architecture

- `app/`: Kotlin/Jetpack Compose Android client. Room remains schema **version 3** and retains distributed `feedbackId` UUIDs, local-only `localId`, tombstones, and `PENDING_DELETE -> PENDING_CREATE -> reconciliation` synchronization.
- `backend/`: Java 21, Maven, Spring Boot, Web, Security, Data JPA, Validation, Flyway, Actuator, JJWT, and the PostgreSQL driver. It is one modular monolith with `auth`, `security`, `user`, `feedback`, and `common` packages—not microservices.
- `docker-compose.yml`: PostgreSQL 17 on host port 5434 and the backend on port 8080, with a named `postgres_data` volume.

## Authentication and authorization

`POST /api/auth/register` validates email/password and immediately returns an authenticated access/refresh pair; registration always creates `USER`. `POST /api/auth/login` does the same for valid credentials. Passwords are BCrypt hashes and emails are trimmed/lowercased. Feedback APIs require a bearer access JWT. JWTs contain the user UUID and role, are signed with `JWT_SECRET`, and default to 20 minutes.

Refresh tokens are opaque 256-bit random values. PostgreSQL stores only their SHA-256 hashes. `POST /api/auth/refresh` pessimistically locks the session, revokes the old token, links it to its replacement, and returns rotated access/refresh tokens. Each successful rotation starts a new configurable inactivity period (90 days by default). Rotated, revoked, or inactive tokens are rejected. `POST /api/auth/logout` accepts the refresh token without requiring a valid access JWT and revokes it.

Roles are enforced by Spring Security: `USER` and `ADMIN` may GET/POST feedback; only `ADMIN` may DELETE. Ownership always comes from the authenticated JWT—not request JSON. Duplicate identical creates by the same owner are idempotent; conflicting content or ownership returns 409.

### Optional local admin

Set both `APP_ADMIN_EMAIL` and `APP_ADMIN_PASSWORD` before startup. The application creates a BCrypt-protected `ADMIN` only when the normalized email does not exist. There is no client-side promotion flow. Register through Android to test `USER`; log in with the environment-bootstrapped account to test `ADMIN`.

## Android sessions and offline behavior

`SecureTokenStore` stores access token, refresh token, user UUID/email/role in Android Keystore-backed encrypted preferences and excludes that file from backup. A single application-scoped dependency container is shared by UI and WorkManager. An interceptor attaches access JWTs; an OkHttp authenticator retries once after rotation. `SessionManager` uses a process-wide coroutine `Mutex`, so concurrent UI/worker 401s perform one refresh and wait for its result.

A stored user session restores without login-screen flashing. Temporary refresh I/O and 5xx failures retain the encrypted local session and permit offline use; definitive refresh 401/403 clears it. Explicit Logout attempts server revocation and always clears local credentials. Delete UI derives exclusively from the stored server role.

Authenticated WorkManager synchronization retains unique one-time/periodic work, connected-network constraints, exponential backoff, the process-wide synchronization mutex, tombstone-first ordering, immediate local Save/Delete, and uncertain-POST confirmation.

## Local development

1. Copy `.env.example` to untracked `.env` and replace `DATABASE_PASSWORD` and `JWT_SECRET` (at least 32 random bytes).
2. Run `docker compose config`, then `docker compose up --build`.
3. Verify `curl http://localhost:8080/actuator/health`.

The Windows/host URL is `http://localhost:8080`. The Android emulator uses `http://10.0.2.2:8080/`; never use `0.0.0.0` as a client destination. Cleartext traffic is narrowly allowed only for emulator host `10.0.2.2`; production endpoints must use HTTPS. Only `/actuator/health` is exposed by Actuator.

### One-time json-server development cutover

json-server is no longer in the normal path. Prototype records have no authenticated ownership and are not imported or assigned silently. For the first full Milestone 4 test, use a **fresh PostgreSQL development database** and clear Android application data/reinstall if the device contains old json-server-era rows. This is a manual development cutover, not a destructive Room migration; application code never wipes data automatically.

## Configuration

See `.env.example`: `DATABASE_URL` (used directly outside Compose), `DATABASE_USERNAME`, `DATABASE_PASSWORD`, `JWT_SECRET`, `ACCESS_TOKEN_LIFETIME`, `REFRESH_SESSION_INACTIVITY`, and optional admin credentials. Never commit `.env` or real credentials.

## Tests and checks

Run backend tests/package from `backend/` with `./mvnw test` and `./mvnw package`. Run Android checks from the root with `./gradlew testDebugUnitTest`, `./gradlew lintDebug`, and `./gradlew assembleDebug`; an emulator is required for `./gradlew connectedDebugAndroidTest`. Docker validation uses `docker compose config`; `docker compose up --build` plus the health curl validates the complete runtime.
