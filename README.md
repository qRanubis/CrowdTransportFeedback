#  CrowdTransportFeedback

**Crowdsourcing App for Mapping Trust Levels in Public Transport**

Android application for collecting user feedback about public transport services, featuring offline-first storage, mandatory GPS location, REST API synchronization and admin moderation.

---

## Application Overview

The application allows users to submit feedback regarding public transport (bus, metro, etc.) by providing:

- a score 
- a textual comment
- the transport line
- GPS location (mandatory)

Feedback can be stored locally (offline) and synchronized with a REST server when available.

---

## Implemented Features

- Feedback list screen
- Feedback detail screen (score, comment, line, GPS, date)
- Add feedback form
- **Mandatory GPS integration**
- Local persistence using Room
- REST API communication using **Retrofit**
- Offline-first, eventually consistent synchronization strategy
- Durable automatic retry with AndroidX WorkManager
- Admin-only delete functionality
- MVVM architecture
- UI built with Jetpack Compose

---

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose
- **Architecture**: MVVM
- **Local DB**: Room
- **Networking**: Retrofit + Json
- **Location**: Google Play Services Location
- **State Management**: ViewModel + StateFlow
- **Background work**: AndroidX WorkManager

---

# How to Run the Project

### 1️. Open the Android App

1. Open the project in Android Studio
2. Let Gradle sync
3. Run the app on an Android Emulator

---

### 2️. GPS Configuration (IMPORTANT)

GPS is **mandatory** when adding feedback.

In Android Emulator:

1. Open Extended Controls
2. Go to Location
3. Set a location 
4. Click Send

If GPS is not set:
- the app will not allow saving feedback

---

### 3️. Offline Mode (No Server Required)

You can fully test the app **without any server**:

- Add feedback
- Data is saved locally in Room
- `syncState = PENDING_CREATE`
- App continues to work normally

This demonstrates **offline-first behavior**.

---

##  REST API Server 

The app uses a simple REST API for synchronization.

### Server Technology
- `json-server`
- `Node.js`

---

### Start the Server

#### Requirements
- Node.js installed

#### Commands

```bash
mkdir dir
cd dir
npm install
npx json-server@0.17.4 --watch db.json --host 0.0.0.0 --port 3000
```

---


### Emulator Networking Note

`0.0.0.0` is only the server bind address; do not open it in a browser.

From the Windows host, open:

`http://localhost:3000/feedback`

Android Emulator cannot access the Windows host through `localhost`. From the
Android Emulator, the app or browser uses:

`http://10.0.2.2:3000/feedback`

This maps to your local machine.

---

### Synchronization Logic

#### Upload (Local to Server)

- Feedback is saved locally first
- App attempts to `POST` data to the server
- If successful: `syncState = SYNCED`
- If server is unavailable: remains local
- Failed uploads remain `PENDING_CREATE` and a connectivity-constrained worker retries them
- Retries first look up the stable UUID on the server, so a timed-out POST cannot create a duplicate

#### Download and reconciliation

- Press **Sync now** to run the same complete sync pass used by background work
- Server entries are inserted/updated locally
- Deleted server entries are removed locally
- Unsynchronized local creates and delete tombstones are preserved during reconciliation

#### Deletion

- A delete is first stored as a `PENDING_DELETE` tombstone and immediately hidden from normal lists
- The server is deleted by the stable feedback UUID; both a successful response and HTTP 404 confirm deletion
- Network/server failures retain the tombstone for automatic retry instead of losing it locally

#### Automatic synchronization

- One-time unique work is requested after a failed mutation and at application startup
- A unique 15-minute periodic job provides a safety net without creating duplicate jobs
- Both jobs require a connected network and use WorkManager exponential retry/backoff
- Each pass processes pending deletes, pending creates, and finally the server download in that order
  
---

### Offline-First Strategy

You can fully test the app without any server:

- Add feedback
- Data is saved locally in Room
- `syncState = PENDING_CREATE`
- App continues to work normally

---

### Admin Mode

The app supports **two roles**:

#### User
- Can add and view feedback
- Cannot delete feedback

#### Admin
- Can delete feedback
- Deletion propagates to server (if synced)

#### Enable Admin Mode

Set in code:

```kotlin
val isAdmin = true
```
---
### GPS Integration
- Location permission requested at runtime
- Saving feedback requires valid latitude & longitude
- Coordinates are stored:
  - locally (Room)
  - remotely (REST API)

---

### Project Structure

```text
app/
 ├─ data/
 │   ├─ local/        # Room entities, DAO, database
 │   ├─ remote/       # Retrofit API, DTOs
 │   └─ repository/   # Sync & business logic
 │
 ├─ ui/
 │   ├─ screens/      # Compose screens
 │   ├─ viewmodel/    # ViewModels
 │   └─ navigation/   # Navigation graph
 │
 └─ MainActivity.kt
```

---
### Author

Vitregu Valentin-Rareș - SCPD

## Milestone 3: structured transport feedback

New reports use four mandatory clickable 1–5 ratings: **Overall trust** (very low to very high), crowding comfort (extremely crowded to plenty of space), cleanliness (very dirty to very clean), and punctuality (very poor to very good). Every scale consistently uses 1 as negative and 5 as positive. A report also requires an explicitly selected Bus, Night bus, Tram, Trolleybus, or Metro line from the static September 2026 Bucharest catalog; normal and night buses remain separate.

Room database version 3 uses a non-destructive 2 → 3 migration. Legacy rows retain their identity, rating, comment, coordinates, line, timestamp, and sync state while new structured columns remain null. Legacy json-server records with missing structured properties also deserialize as null.

Location captures one current point when a report is submitted (with last-known location only as fallback). The app does not continuously track trips and does not request background location. Validation requires all structured selections and an available location; comments are optional and trimmed. Saving is accepted after the row is persisted locally, while the unchanged Milestone 2 offline synchronization uploads and retries it separately.
