# Paperboy

Your morning paper, delivered. Pick the topics you care about, read the handful of stories that matter today, and get on with your day.

Paperboy is a calm, finite news brief for Android and iOS: open it in the morning, read 5–10 stories ranked for your interests, see *"You're all caught up"*, close it.

## Repository layout

| Path | What |
|---|---|
| `core/model` | Domain models shared by the apps and the backend (Kotlin Multiplatform) |
| `shared` | App code shared by Android and iOS: UI (Compose Multiplatform), view models, data |
| `androidApp` | Android entry point |
| `iosApp` | iOS entry point (Xcode project generated with XcodeGen) |
| `backend` | API and news ingestion (Ktor) |
| `docs` | Architecture decision records and project docs |

## Stack

Kotlin Multiplatform · Compose Multiplatform + Material 3 · MVVM / unidirectional data flow · Koin · Ktor (client and server) · Room · Coroutines + Flow · PostgreSQL + Flyway · Rome (RSS)

## Getting started

Requirements: JDK 21, Android SDK (API 37), Xcode 26+ for iOS.

```bash
./gradlew :androidApp:assembleDebug     # Android debug build
./gradlew allTests                       # shared + backend tests
./gradlew :backend:run                   # API on http://localhost:8080
./gradlew ktlintCheck                    # code style
```

Backend (needs Docker):

```bash
docker compose up -d db                   # PostgreSQL on localhost:5432
./gradlew :backend:run                    # migrates, then ingests the RSS feeds every 15 minutes
curl 'http://localhost:8080/v1/brief?topics=tech,ai&limit=10'
```

| Endpoint | What |
|---|---|
| `GET /health` | Liveness check |
| `GET /v1/topics` | Topics a reader can follow |
| `GET /v1/brief?topics=tech,ai&limit=10` | Today's ranked brief for those topics (1–20 topics, limit 1–20) |

Pointing the apps at the local API (debug builds use `http://localhost:8080`):

```bash
adb reverse tcp:8080 tcp:8080             # Android emulator or device: forward its localhost:8080 to this computer
```

The iOS simulator shares the Mac's network, so it needs nothing extra.

Configuration comes from environment variables: `PORT`, `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD`, `INGESTION_ENABLED`, `INGESTION_INTERVAL_MINUTES`.

iOS:

```bash
brew install xcodegen
cd iosApp && xcodegen generate && open iosApp.xcodeproj
```

## Contributing

`main` is protected: work on a branch, open a pull request, and merge once CI is green. Commit messages follow [Conventional Commits](https://www.conventionalcommits.org). Decisions are recorded in [`docs/adr`](docs/adr).

## License

Copyright © 2026 Bogdan Dragomir. All rights reserved.
