# Local Events for Android

Native Kotlin/Jetpack Compose client. Open this directory in Android Studio.

## Build prerequisites

- JDK 21 or a compatible newer JDK, configured through Android Studio's Gradle JDK or `JAVA_HOME`.
- Android SDK platform 37 and NDK `30.0.16248370`, matching `app/build.gradle.kts`.
- Use the committed Gradle wrapper. Plugin and dependency versions are pinned in the Gradle files.
- AndroidKit `0.1.31-SNAPSHOT` must be published to Maven Local. The existing build resolves `net.mamby.androidkit` exclusively from Maven Local. Build/publish that snapshot from the [AndroidKit repository](https://github.com/mamby/android-kit) before building this app; it is not bundled here.
- Configure your SDK with Android Studio, `ANDROID_HOME`, or an ignored `local.properties` containing `sdk.dir`.

The repository does not pin a machine-specific JDK installation path.

## Select a backend

Set the URL for the variant being built. Use an absolute URL with a trailing slash; the client appends relative `v1/...` paths. No private backend checkout is required.

| Flavor | Environment variable |
| --- | --- |
| `dev` | `LOCAL_EVENTS_API_DEV` |
| `beta` | `LOCAL_EVENTS_API_BETA` |
| `stage` | `LOCAL_EVENTS_API_STAGE` |
| `prod` | `LOCAL_EVENTS_API_PROD` |

```powershell
$env:LOCAL_EVENTS_API_DEV = 'https://your-api.example/'
.\gradlew.bat :app:assembleDevDebug
```

Replace the example with your reachable backend using HTTPS and a certificate trusted by the device. The current manifests do not enable cleartext HTTP or bypass certificate validation. The standard Android emulator reaches its host through `10.0.2.2`, but a localhost development certificate does not automatically cover that address.

Only `dev` has a debug variant; beta, stage, and prod use release variants. Configure your own release signing outside version control before distribution.

## Validation

```powershell
.\gradlew.bat :app:testDevDebugUnitTest
.\gradlew.bat :app:lintDevDebug
.\gradlew.bat :app:compileDevDebugAndroidTestKotlin
```

Device tests use `:app:connectedDevDebugAndroidTest`. See [the backend contract](../../docs/backend-contract.md) for response schemas.

## Updated Android Kit prerequisite

The Maven Local prerequisite above is superseded. This project now downloads
Android Kit `0.1.53` from the public [Kit Maven repository](https://mamby.github.io/android-kit-docs/maven/).
No Kit implementation checkout is needed. Maven Local is an explicit development
opt-in using `-PandroidKitUseMavenLocal=true -PandroidKitVersion=<local-version>`.
The committed resource validator and packaged MIT/third-party notices match the
public release. GitHub Actions builds use the public repository exclusively.

The current pinned Android Kit release is `0.1.65`, downloaded from the public
Maven repository with Maven Local disabled by default.
