# 🤝 Contributing to Local Events

Thank you for your interest in contributing! This guide will help you get started.

## Table of Contents

- [Getting Started](#getting-started)
- [Development Workflow](#development-workflow)
- [Branching Strategy](#branching-strategy)
- [Commit Conventions](#commit-conventions)
- [Architecture Guidelines](#architecture-guidelines)
- [Pull Requests](#pull-requests)
- [Code Style](#code-style)
- [Testing](#testing)
- [Reporting Issues](#reporting-issues)

## Getting Started

1. Fork the repository
2. Clone your fork locally
3. Ensure .NET 9.0 SDK is installed
4. Install the MAUI workload: `dotnet workload install maui`
5. Run `dotnet restore` to install dependencies
6. Create a branch for your work (see [Branching Strategy](#branching-strategy))

## Development Workflow

```bash
# Create a feature branch
git checkout -b feature/your-feature-name

# Make your changes, then run tests
dotnet test

# Commit with a conventional message
git commit -m "feat: add event filtering by category"

# Push and open a PR
git push origin feature/your-feature-name
```

## Branching Strategy

| Branch | Purpose |
|--------|---------|
| `main` | Stable, production-ready code |
| `develop` | Integration branch for upcoming release |
| `feature/*` | New features (branch from `develop`) |
| `bugfix/*` | Bug fixes (branch from `develop`) |
| `hotfix/*` | Urgent production fixes (branch from `main`) |
| `release/*` | Release preparation (branch from `develop`) |

- Always create feature and bugfix branches from `develop`
- PRs should target `develop` unless it's a hotfix
- `main` is updated only via release merges or hotfixes

## Commit Conventions

We follow [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<optional scope>): <description>

[optional body]

[optional footer]
```

### Types

| Type | Description |
|------|-------------|
| `feat` | A new feature |
| `fix` | A bug fix |
| `docs` | Documentation changes |
| `style` | Formatting, semicolons, etc. (no logic change) |
| `refactor` | Code restructuring (no feature or fix) |
| `test` | Adding or updating tests |
| `chore` | Build process, tooling, dependencies |
| `perf` | Performance improvements |

### Examples

```
feat(feed): add pull-to-refresh on event feed
fix(android): resolve status bar transparency issue
docs: update architecture decision records
chore: upgrade to .NET 9.0
```

## Architecture Guidelines

This project follows **vertical slice architecture**. Please review [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) before making structural changes.

Key principles:

- **Features are self-contained** — each feature owns its models, handlers, and views
- **Keep it modular** — avoid cross-feature dependencies
- **No city-specific logic in core layers** — city customization belongs in configuration, not code
- **Shared code goes in `Core`** — domain models, interfaces, and common utilities
- **Platform-specific code is isolated** — use `Platforms/` folders in the MAUI project

## Pull Requests

### Before Submitting

- [ ] Your branch is up to date with `develop`
- [ ] All existing tests pass (`dotnet test`)
- [ ] New features include tests
- [ ] Code follows the project style guidelines
- [ ] You've tested on at least one target platform (Android or iOS)

### PR Guidelines

- Keep PRs **small and focused** — one feature or fix per PR
- Write a **clear description** of what changed and why
- Reference related issues (e.g., `Closes #42`)
- Include screenshots for UI changes
- Use the [PR template](/.github/PULL_REQUEST_TEMPLATE.md)

## Code Style

- Use the **latest C# language features** (file-scoped namespaces, primary constructors, etc.)
- Keep services and handlers **small and focused**
- Use **meaningful names** — avoid abbreviations
- Follow .NET naming conventions (PascalCase for public members, camelCase for locals)
- Use `sealed` on classes that aren't designed for inheritance
- Prefer **record types** for DTOs and value objects
- Use **nullable reference types** — the project has `<Nullable>enable</Nullable>`

## Testing

- **Unit tests**: Use xUnit for business logic and services
- **Integration tests**: Use `WebApplicationFactory` for API endpoint testing
- **UI tests**: Manual testing on target platforms (automated UI tests welcome as contributions)
- Aim for meaningful coverage — focus on business logic, not boilerplate

```bash
# Run all tests
dotnet test

# Run tests with coverage
dotnet test --collect:"XPlat Code Coverage"
```

## Reporting Issues

- Use the [Bug Report](.github/ISSUE_TEMPLATE/bug_report.md) template for bugs
- Use the [Feature Request](.github/ISSUE_TEMPLATE/feature_request.md) template for new ideas
- Check existing issues before creating a new one
- Include reproduction steps, expected behavior, and screenshots when applicable

---

Thank you for helping make local events more accessible! 🎉