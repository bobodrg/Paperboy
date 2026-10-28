# 0001. Kotlin Multiplatform with Compose Multiplatform

- **Status:** accepted
- **Date:** 2026-10-25

## Context

Paperboy ships on Android and iOS and is built by one developer. The backend is also written in Kotlin.

## Decision

One Kotlin codebase. Business logic and UI are shared with Compose Multiplatform. Platform-specific pieces (secure storage, background refresh, push, in-app browser) sit behind `expect`/`actual` declarations or interfaces. Domain models live in `core/model` and are shared with the backend.

Libraries follow from this: Koin (Hilt is Android-only), Ktor client (Retrofit is JVM-only), Room KMP, Coil 3.

## Consequences

- One team's worth of work instead of two, and one language end to end, so API models can't drift between app and server.
- The iOS UI is not 100 % native: some controls and scroll physics differ slightly from UIKit.
- Smaller library ecosystem than Flutter.
- iOS builds need macOS CI runners, which cost more minutes.
- Revisit if iOS users notice the UI feels foreign, or if a key platform feature can't be reached through interop.

Alternatives considered: Flutter (Dart, no knowledge carried over from Kotlin), React Native (JS/TS), two native apps (double the work).
