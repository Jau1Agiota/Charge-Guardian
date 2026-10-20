# Charge Guardian

A native, offline Android battery alert app. Kotlin, Jetpack Compose / Material 3, minimal MVVM, Preferences DataStore. Package `com.chargeguardian.app`, version **1.2.0 (3)**. Android 8+; compiles and targets Android 16 (API 36).

## Build and install

Open this folder in Android Studio with JDK 17+ (cloud build uses JDK 21). Install Android SDK platform 36 and build tools 36.0.0, set `ANDROID_HOME` or an untracked `local.properties` with `sdk.dir`, then:

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Gradle 8.13 is supplied through the checksum-pinned wrapper. Dependencies resolve from Google Maven and Maven Central. No app runtime credentials, server, internet permission, or watch API are needed. CI runs the same checks and uploads the debug APK and reports. A debug build is signed with the local Android debug key; it is not a production release signature.

## Use

Grant notifications, enable Monitoring while the app is open, and leave the ongoing notification active. Default alert threshold is 95%; the slider supports 1–100%. Test Notification uses the configured threshold without changing session state. Enable Charge Guardian in Zepp's app alerts to the Amazfit Active 2.

Electric Neon is the default dark theme; choose Neon Mint in Appearance using the palette icon. Help (`?`) contains Zepp, HyperOS and troubleshooting instructions. Both themes and the alert threshold persist locally.

See [Stage 1.2 icon corrections](docs/STAGE_1_2.md), [Stage 1.1 changes and validation](docs/STAGE_1_1.md), [implementation and validation](docs/IMPLEMENTATION.md) and the [physical-device checklist](docs/DEVICE_TEST_CHECKLIST.md). Android and HyperOS can stop background apps; the app cannot promise delivery after force-stop, OEM termination or notification suppression.
