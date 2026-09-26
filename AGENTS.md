# Repository guidance

This public repository contains native clients and the public backend contract only.

- Android: `src/android` (Kotlin, Jetpack Compose, AndroidX, Hilt, Ktor, DataStore, Media3, AndroidKit).
- Contract: `docs/backend-contract.md` and `docs/api/openapi.v1.json`.
- Shared native assets: `assets`.
- Future clients belong in `src/ios`, `src/macos`, or `src/windows` when implemented.
- Backend implementation, infrastructure, and future Admin applications belong in the separate private `local-events-service` repository.

Use official platform APIs, declarative UI, lifecycle-aware state, and existing localization patterns. Preserve Kit-owned UI boundaries. Do not add tests unless requested. Existing Android JVM and instrumentation tests stay in their Gradle source sets.

From `src/android`, set `LOCAL_EVENTS_API_DEV` and run the narrowest useful check: `./gradlew :app:assembleDevDebug`. Run `:app:lintDevDebug` for resources/localization changes. JVM tests use `:app:testDevDebugUnitTest`; instrumentation requires a device and `:app:connectedDevDebugAndroidTest`.

Do not change existing README content; append necessary material underneath. Keep the public contract independent of the private implementation. Do not import backend source or private configuration here.

The Android namespace and application ID are `net.mamby.events`. Preserve existing stored preference names, telemetry headers, and external service URLs unless explicitly requested otherwise. Do not invent replacement domains.
