# 🤝 Contributing to Local Events

Fork this repository, create a focused branch, and open a pull request with a clear description and relevant validation. Use Conventional Commits where practical.

For Android prerequisites, configuration, and commands, see [the Android guide](src/android/README.md). Open `src/android` in Android Studio. This repository does not require .NET or MAUI.

Run the checks appropriate to your change from `src/android`:

```powershell
.\gradlew.bat :app:assembleDevDebug
.\gradlew.bat :app:testDevDebugUnitTest
.\gradlew.bat :app:lintDevDebug
```

Run `:app:connectedDevDebugAndroidTest` only with an emulator or device. Keep localized resources consistent across all supported locales and respect AndroidKit's ownership of shared UI.

For a backend implementation, start from [the public contract](docs/backend-contract.md). Discuss incompatible contract changes before implementation; preserve compatibility for `/v1`. Submit public contract and native-client changes here. Backend implementation, administration, and private operational configuration belong in the private service repository.