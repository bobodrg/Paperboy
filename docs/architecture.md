# Architecture

## Overview

```
Android / iOS app (Kotlin Multiplatform, Compose Multiplatform)
   UI (Compose) → ViewModel (StateFlow<UiState>) → Repository → local DB (source of truth)
                                                              ↘ API client (Ktor)
                                   │ HTTPS / JSON
Backend (Ktor)
   REST API /v1 ── PostgreSQL
   Ingestion jobs: RSS feeds → normalise → deduplicate → tag topics → cluster → store
```

## App

- **Unidirectional data flow.** Each screen has an immutable `UiState`; its `ViewModel` exposes a `StateFlow<UiState>` and receives user events.
- **Offline first.** The UI observes the local database. A background sync refreshes it before the user's briefing time (WorkManager on Android, BGTaskScheduler on iOS).
- **Dependency injection** with Koin. Platform entry points call `initKoin()` once.
- **Platform-specific code** lives behind `expect`/`actual` declarations or interfaces.

## Modules

| Module | Targets | Contents |
|---|---|---|
| `core/model` | Android, iOS, JVM | `@Serializable` domain models shared with the backend |
| `shared` | Android, iOS | Design system, features, data layer |
| `androidApp` | Android | `Application`, `MainActivity` |
| `iosApp` | iOS | SwiftUI host for the shared UI |
| `backend` | JVM | Ktor API and ingestion |

`shared` will be split into `core/*` and `feature/*` modules when it grows enough for build times or boundaries to matter.

## Backend

- Ktor, kotlinx.serialization, PostgreSQL with Flyway migrations.
- Versioned REST API under `/v1`.
- Stateless container, configured through environment variables, deployed to an EU region.

## Quality gates

Every pull request runs ktlint, the shared and backend tests, the Android build, the backend Docker build and a secret scan. The iOS simulator tests and iOS build run when shared or iOS code changes.

## Security

- No third-party API keys in the apps.
- Auth tokens are stored in the Android Keystore and iOS Keychain.
- TLS only.
- The backend validates every request and applies per-user authorisation and rate limits.
- Personal data is kept to a minimum, accounts can be deleted from inside the app, and data is hosted in the EU.

Decisions and their trade-offs are in [`adr/`](adr).
