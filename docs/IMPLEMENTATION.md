# Stage 1 implementation report

Historical validation for version 1.0.0. For the current APK and results, see [Stage 1.2](STAGE_1_2.md).

## Monitoring approach

The user grants notification permission and explicitly enables monitoring from the visible activity. An ongoing, low-importance foreground-service notification makes continuous operation visible. The service uses `specialUse`, declares `FOREGROUND_SERVICE_SPECIAL_USE` and describes the subtype in the manifest. Battery-threshold observation is not a data-sync, connected-device, location or short-service workload; using those types would misrepresent it. Google Play distribution would require review of this special-use justification; this stage delivers a sideloadable debug app and does not claim Play approval.

While enabled, the foreground service remains active even when unplugged, so it can detect the next connection without attempting a prohibited arbitrary background launch. It dynamically receives system `ACTION_BATTERY_CHANGED`, `ACTION_POWER_CONNECTED` and `ACTION_POWER_DISCONNECTED`. Level/scale and plugged state come from BatteryManager broadcast extras; the sticky battery snapshot provides initial/recovery state. Plugged state includes full batteries, rather than relying only on CHARGING status. There is no polling, network work, wake lock or direct smartwatch integration.

The service returns `START_STICKY` for system-managed process recovery. A manifest receiver attempts recovery after `BOOT_COMPLETED` (after unlock) and `MY_PACKAGE_REPLACED`, only if monitoring was previously enabled. Boot is an explicit background-start exemption, and Android 16's default boot foreground-service type allowlist includes `specialUse`. A charging broadcast is not treated as permission to launch a foreground service from an arbitrary background state. No WorkManager periodic job is used: its timing cannot ensure a timely threshold alert.

### Platform research

The developer documentation endpoints returned HTTP 403 in this cloud environment. The implementation was cross-checked against Android 16 platform source through the AOSP GitHub mirror:

- [ActiveServices.java, android16-release](https://github.com/aosp-mirror/platform_frameworks_base/blob/android16-release/services/core/java/com/android/server/am/ActiveServices.java): `FGS_BOOT_COMPLETED_RESTRICTIONS`, `shouldAllowBootCompletedStart`, and the foreground-service start/type checks.
- [ActivityManagerConstants.java, android16-release](https://github.com/aosp-mirror/platform_frameworks_base/blob/android16-release/services/core/java/com/android/server/am/ActivityManagerConstants.java): `DEFAULT_FGS_BOOT_COMPLETED_ALLOWLIST` includes `FOREGROUND_SERVICE_TYPE_SPECIAL_USE`.
- Relevant Android guidance for reviewers: [background-start restrictions](https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start), [service types / special use](https://developer.android.com/develop/background-work/services/fgs/service-types#special-use), [broadcast restrictions](https://developer.android.com/develop/background-work/background-tasks/broadcasts/broadcast-exceptions).

These platform checks do not establish Xiaomi firmware behavior. Use the physical-device checklist before relying on unattended alerts.

## State and alert semantics

A pure session reducer and one Preferences DataStore transaction persist the connected flag, monotonically advancing session ID, alerted flag, monitoring preference and threshold. A single service coroutine serializes observed battery events; DataStore edits also serialize state changes. Disconnect clears the alert flag. Connecting at or above threshold alerts immediately. A threshold change causes a fresh sample: lowering can alert immediately; raising waits; an already claimed session never alerts again until an observed disconnect. Tests and real alerts use different notification IDs; test alerts never modify session state.

Notification permission and the battery-alert channel are checked before claiming a threshold alert. The claim is committed before posting, prioritizing duplicate prevention after process death. This is at-most-once notification **attempt** semantics: Android notification posting and DataStore cannot form one atomic transaction. A crash or permission revocation between claim and post can lose that alert. Delivery, sound and watch vibration are also subject to Android/Zepp/DND/user settings. Notifications use a dedicated high-importance battery channel and readable standard text suitable for Zepp forwarding; the ongoing monitoring channel is separate and quiet.

## Android and OEM limitations

- Fresh installs require opening the app, granting notifications and enabling monitoring once. The monitoring preference is retained; backup is disabled so restoring to a different device does not carry an active session.
- A foreground service improves process priority but cannot defeat force-stop, Android's Active apps stop, OEM termination, or notification suppression. HyperOS autostart and No restrictions settings are documented for both this app and Zepp. Reopen and enable monitoring if it stops.
- A sticky service reconciles the current battery snapshot after restart. If an entire unplug/replug cycle happened while the process could not run, Android provides no historical charging-session ID. The app conservatively retains the prior alert claim rather than inventing a new session and risking a duplicate. The same limitation applies across downtime/reboot or an intentionally paused monitoring interval.
- Boot recovery is after normal boot/unlock, not direct boot. OEM policy may delay/deny it. Startup denial is handled without repeatedly trying background launches.
- The app detects framework battery percentages; firmware batching may delay changes. It does not promise exact delivery at the instant chemical charge crosses the threshold.

## Validation and artifact

Verified in the cloud environment with a full JDK 21, Android SDK 36 / build tools 36.0.0, and checksum-pinned Gradle 8.13:

- `./gradlew testDebugUnitTest lintDebug assembleDebug`: **BUILD SUCCESSFUL**.
- Session unit tests: **12 passed, 0 failed, 0 errors, 0 skipped**. Coverage includes the six requested behaviors plus unknown/disconnected battery values and recovery variants.
- Android lint: **0 errors, 6 warnings**, all reporting newer dependency versions. The project deliberately pins the versions validated in this build; no lint baseline, disabled tests, or suppressed assertions were used.
- `apksigner verify --verbose`: valid APK Signature Scheme v2 debug signature.
- `zipalign -c -P 16 4`: passed.
- `aapt dump badging`: confirmed package, 1.0.0 (1), minSdk 26 and targetSdk 36. Merged manifest contains no INTERNET permission.
- The reusable cloud installation script was executed successfully, including a repeated dependency setup and build. JDK and SDK downloads were checksum-verified; TLS verification remains enabled.

Current APK SHA-256: `b895de9eb64d5c834235fa7391ffc47f974b096c2ae2744c87591f5fe8793b3d`. A regenerated debug build can have a different checksum/signing key.

Physical Xiaomi / Zepp checks remain **pending**. No device installation, UI instrumentation, OEM process-survival test or smartwatch forwarding test is claimed. Follow the separate manual checklist before relying on unattended alerts.

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`. Package `com.chargeguardian.app`, versionName `1.0.0`, versionCode `1`, minSdk 26, targetSdk 36. Install with `adb install -r app/build/outputs/apk/debug/app-debug.apk`. CI uploads the APK under `Charge-Guardian-1.0.0-debug`.
