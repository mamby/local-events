# Architecture Overview

This repository uses a vertical slice architecture with a multi-platform .NET solution.

## Structure

- **ASP.NET Core backend**
  - Feature-based vertical slices
  - OIDC authentication
  - Domain and application logic close to each feature

- **.NET MAUI client**
  - Cross-platform UI for Android, iOS
  - Small, feature-focused view models
  - Aligned with backend features when needed