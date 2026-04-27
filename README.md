# 🎉 Local Events Platform

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![.NET](https://img.shields.io/badge/.NET-9.0-512BD4)](https://dotnet.microsoft.com/)
[![MAUI](https://img.shields.io/badge/MAUI-Cross--Platform-blueviolet)](https://learn.microsoft.com/dotnet/maui/)

Apps to discover and share local events across cities.

## Purpose

Local events are often fragmented across multiple sources and poorly accessible for newcomers and tourists. This project unifies them into a simple, structured experience — giving every city a reusable foundation for making local culture and events more accessible to everyone.

## Features

- 🏙️ City-based event discovery
- 📡 Open data ingestion from public APIs
- 📱 Mobile-first UX with native feel
- 🔐 Secure authentication via OIDC
- 🌍 Multi-city, multi-language ready

## Tech Stack

| Layer | Technology |
|-------|------------|
| Mobile App | .NET MAUI (Android & iOS) |
| Backend API | ASP.NET Core |
| Data Access | Entity Framework Core |
| Auth | OpenID Connect (OIDC) |
| Language | C# (latest) |

## Prerequisites

- [.NET 9.0 SDK](https://dotnet.microsoft.com/download/dotnet/9.0) or later
- .NET MAUI workload (`dotnet workload install maui`)
- For Android: Android SDK via Visual Studio or standalone
- For iOS: macOS with Xcode (required for iOS builds)
- A supported IDE: [Visual Studio 2022](https://visualstudio.microsoft.com/) (recommended) or [VS Code](https://code.visualstudio.com/) with C# Dev Kit

## Getting Started

```bash
# Clone the repository
git clone https://github.com/mamby/local-events.git
cd local-events

# Restore dependencies
dotnet restore

# Run the API
dotnet run --project src/LocalEvents.Api

# Run the MAUI app (Android)
dotnet build -t:Run -f net9.0-android --project src/LocalEvents.App
```

> **Note:** Update connection strings and OIDC settings in `appsettings.json` / environment variables before running.

## Project Structure

```
local-events/
├── src/
│   ├── LocalEvents.Api/          # ASP.NET Core backend API
│   ├── LocalEvents.App/          # .NET MAUI mobile application
│   ├── LocalEvents.Core/         # Shared domain models & interfaces
│   └── LocalEvents.Infrastructure/ # Data access, external services
├── tests/
│   ├── LocalEvents.Api.Tests/
│   └── LocalEvents.Core.Tests/
├── docs/
│   └── ARCHITECTURE.md           # Architecture & design decisions
├── .github/
│   ├── ISSUE_TEMPLATE/
│   └── PULL_REQUEST_TEMPLATE.md
├── CONTRIBUTING.md
├── CODE_OF_CONDUCT.md
├── CHANGELOG.md
├── SECURITY.md
└── LICENSE
```

## Documentation

- [Architecture & Design](docs/ARCHITECTURE.md)
- [Contributing Guidelines](CONTRIBUTING.md)
- [Code of Conduct](CODE_OF_CONDUCT.md)
- [Changelog](CHANGELOG.md)
- [Security Policy](SECURITY.md)

## License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.

## Author

**Mamby Camara** — [@mamby](https://github.com/mamby)