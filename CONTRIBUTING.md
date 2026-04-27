# 🤝 Contributing to Local Events

Thank you for contributing.

## Getting started

- Fork the repository
- Clone your fork
- Install the current supported .NET SDK
- Install the MAUI workload if needed: `dotnet workload install maui`
- Run `dotnet restore`

## Workflow

- Create a branch for your change
- Make your changes
- Run tests: `dotnet test`
- Commit with a clear message
- Open a pull request

## Branches

- `main`: stable code
- Use feature, fix, or hotfix branches as needed

## Commit messages

Use Conventional Commits when possible:

```text
feat: add event filtering
fix: resolve startup crash
docs: update contributing guide
chore: upgrade dependencies