# 🎉 Local Events Platform

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
![Android](https://img.shields.io/badge/Android-green.svg)
![Kotlin](https://img.shields.io/badge/Kotlin-purple.svg)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-UI-blue.svg)
![iOS](https://img.shields.io/badge/iOS-black.svg)
![macOS](https://img.shields.io/badge/macOS-black.svg)
![Swift](https://img.shields.io/badge/Swift-orange.svg)
![SwiftUI](https://img.shields.io/badge/SwiftUI-UI-blue.svg)
![Windows](https://img.shields.io/badge/Windows-blue.svg)
![WinUI](https://img.shields.io/badge/WinUI-UI-blue.svg)

Apps to discover local events across cities.

## Purpose

A simple foundation for discovering local events.

## Native clients

This public repository contains native client applications and the backend contract. Backend implementation, infrastructure, and future administrative applications are maintained separately in the private `local-events-service` repository.

- [Android application and build instructions](src/android/README.md): Kotlin and Jetpack Compose, under `src/android`.
- iOS, macOS, and Windows clients are planned; their applications are not implemented yet.
- [Architecture](docs/ARCHITECTURE.md).

## Bring your own backend

Implement the [public backend contract](docs/backend-contract.md) using any stack or event sources, then configure the client with your service's base URL. The [OpenAPI document](docs/api/openapi.v1.json) defines the current endpoints and schemas. Access to the private reference implementation is not required.

The current reference feed is a development/demo feed. The contract guide distinguishes its behavior from requirements for interoperable implementations.