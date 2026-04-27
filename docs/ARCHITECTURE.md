# Architecture Overview

This repository follows a **vertical slice architecture** combined with a **multi-platform .NET solution** targeting both mobile and web.

## High-Level Structure

The solution is split into two main application surfaces:

- ASP.NET Core backend
  - Minimal APIs
  - Feature-based vertical slices
  - Authentication (OIDC)
  - Domain + application logic close to features

- .NET MAUI client
  - Cross-platform UI (Android, iOS, Windows)
  - MVVM or minimal ViewModel logic per feature
  - Direct alignment with backend feature slices where applicable

## Vertical Slice Architecture

Each feature is self-contained:

Features/
  Events/
    CreateEvent/
      Endpoint.cs
      Request.cs
      Response.cs
      Handler.cs

## Design Goals

- Simplicity over abstraction
- Feature isolation
- Minimal shared code
