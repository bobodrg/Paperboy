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

Kotlin Multiplatform · Compose Multiplatform + Material 3 · MVVM / unidirectional data flow · Koin · Ktor (client and server) · Room · Coroutines + Flow · PostgreSQL

## Getting started

Requirements: JDK 21, Android SDK (API 37), Xcode 26+ for iOS.

```bash
./gradlew :androidApp:assembleDebug     # Android debug build
./gradlew allTests                       # shared + backend tests
./gradlew :backend:run                   # API on http://localhost:8080
./gradlew ktlintCheck                    # code style
```

iOS:

```bash
brew install xcodegen
cd iosApp && xcodegen generate && open iosApp.xcodeproj
```

## Contributing

`main` is protected: work on a branch, open a pull request, and merge once CI is green. Commit messages follow [Conventional Commits](https://www.conventionalcommits.org). Decisions are recorded in [`docs/adr`](docs/adr).

## License

Copyright © 2026 Bogdan Dragomir. All rights reserved.
