# Architecture

Local Events is a collection of native applications that consume a documented HTTP API. This public repository contains client source, native assets, and the backend contract.

| Path | Responsibility |
| --- | --- |
| `src/android` | Kotlin, Jetpack Compose, ViewModels, coroutines/Flow, Hilt, Ktor, DataStore, Media3, and AndroidKit |
| `assets` | Shared native branding assets |
| `docs/backend-contract.md` | Language-independent backend implementation guide |
| `docs/api/openapi.v1.json` | Public API schemas and endpoint definitions |

The Android app keeps presentation in feature folders, data contracts and repositories in `core`, and dependency registration in `core/di`. Preferences, favorites, cached feed data, and pending telemetry are stored locally. AndroidKit owns shared UI rendering and localization; the app provides its data and actions.

The API base URL is configured at build time per environment. A compatible backend can use any language, framework, storage, event sources, or hosting provider. No reference to the private service project is required to build a client.

The private `local-events-service` repository contains the .NET service, SQL Server persistence, migrations, service tests, and future administrative applications. Its implementation is not part of this repository.

iOS, macOS, and Windows clients are planned; there are no runnable applications for those platforms yet. Add each under `src/<platform>` when implementation begins, using the same public contract.