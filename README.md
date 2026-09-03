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
- Offline-first synchronization strategy
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
npx json-server --watch db.json --port 3000
```

---


### Emulator Networking Note

Android Emulator cannot access localhost.

The app is configured to use:

`http://10.0.2.2:3000`

This maps to your local machine.

---

### Synchronization Logic

#### Upload (Local to Server)

- Feedback is saved locally first
- App attempts to `POST` data to the server
- If successful: `syncState = SYNCED`
- If server is unavailable: remains local

#### Downlaod (Server to Local)

- Press Sync from server
- Server entries are inserted/updated locally
- Deleted server entries are removed locally
  
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
