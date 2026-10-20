# Stage 1.3 — Zepp / Active 2 notification-icon investigation

**Version 1.3.0 (5)** · `com.chargeguardian.app`.

## Audit findings, before resource changes

The supplied device observations establish that Android renders the battery-and-bolt correctly, Zepp's settings recognize the package and show its launcher artwork, and readable notifications reach the watch. Restarting did not help. Those observations isolate the remaining symptom to the watch-logo path; they do not establish its exact cause.

| Notification | Builder / call path | Small icon | Channel / importance | Category / priority / visibility |
|---|---|---|---|---|
| Battery threshold | `Alerts.send(threshold, false)`, ID 2 | `R.drawable.ic_notification` | `battery_alerts`, HIGH | REMINDER, HIGH, PRIVATE (default) |
| Test | `GuardianViewModel.test()` → `Alerts.send(threshold, true)`, ID 3 | same | same | same |
| Monitoring | `MonitorService.onCreate()` → `Alerts.ongoing()` → `startForeground(1, …)` | same | `monitoring`, LOW | SERVICE, DEFAULT priority, PRIVATE; ongoing and silent |

All builders explicitly call `setSmallIcon()`. No notification uses a launcher drawable, empty fallback, full-color small icon or large icon. Android 8+ channel importance controls alert importance; the monitoring notification's DEFAULT legacy priority does not override its LOW channel. Channel IDs and user settings are retained. PRIVATE is Android's existing lock-screen visibility default, not evidence of an icon error; it is not changed to PUBLIC as a speculative workaround.

`app/src/main/res/drawable/ic_notification.xml` is already a dedicated **24dp white battery outline/cap and lightning-bolt vector**, with transparent background and interior cutouts. Both paths use opaque `#FFFFFFFF`; anti-aliased edges are normal. There are no gradients, shadows or photographic details. Its geometry and alpha are tested at 18/24/32px. The green phone-side icon is Android's tint using the selected theme accent, not colored artwork in the small-icon resource.

**No unsuitable resource or notification configuration was found.** Replacing this correct icon again has no evidence-based benefit. Accordingly, this release leaves the drawable and every production notification builder unchanged. It adds a serialization/resolution regression test, release metadata and this investigation rather than claiming a speculative watch fix.

## Four distinct icon paths

1. **Android small icon:** an APK resource referenced by `Notification.smallIcon`; Android resolves its alpha mask and applies its own presentation/tint.
2. **Android launcher icon:** the full-color adaptive artwork returned by PackageManager. Zepp displaying it in settings proves phone-side recognition, not successful watch-side conversion or transfer.
3. **Companion/watch logo mapping:** notification text and an application logo can travel through separate handling. A known-app mapping, package-keyed uploaded image, cache or firmware renderer may be involved. Charge Guardian cannot register a watch logo simply by adding an Android drawable.
4. **Native Zepp OS app icon:** an asset bundled with a separately installed watch app. Zepp Health's official Hello World sample declares `appId`, `appType` and `"icon": "icon.png"` in its own `app.json`. That is not an Android notification resource or a registration API for forwarded notifications.

### Evidence and its limits

- [Gadgetbridge ZeppOsNotificationService, pinned source](https://github.com/Freeyourgadget/Gadgetbridge/blob/a0948ee1cbc2a870f91d313f8e37df5f524465f7/app/src/main/java/nodomain/freeyourgadget/gadgetbridge/service/devices/huami/zeppos/services/ZeppOsNotificationService.java): receives `NOTIFICATION_CMD_ICON_REQUEST` with package name, dimensions and image format; `sendIconForPackage()` renders an application icon and sends a package-keyed `notification://logo` image with acknowledgement.
- [Its NotificationUtils.getAppIcon implementation](https://github.com/Freeyourgadget/Gadgetbridge/blob/a0948ee1cbc2a870f91d313f8e37df5f524465f7/app/src/main/java/nodomain/freeyourgadget/gadgetbridge/util/NotificationUtils.java): uses `PackageManager.getApplicationIcon()` with a launcher-activity fallback, rather than the notification small-icon vector.
- [Zepp Health's native-app manifest example](https://github.com/zepp-health/zeppos-samples/blob/7cee748bb774512b0682cec8bb1209a89e02a351/application/4.0/hello-world/app.json): shows the separate watch-package identity/icon asset.

The first two sources demonstrate a custom application-logo transfer mechanism in the related Zepp OS ecosystem. They **do not prove proprietary Zepp uses this exact code, or that the installed Active 2 firmware accepts the same mechanism for every third-party/sideloaded package**. The official sample proves native app packaging, not notification-logo forwarding. Official Zepp documentation and Amazfit support endpoints returned HTTP 403 in this environment, so no vendor guarantee for this exact phone/Zepp/watch-version combination was verified.

There is no basis to claim all third-party icons are unsupported, nor that the new APK installs an icon on the watch. With a working Android icon, recognized launcher artwork and delivered watch text, a **Zepp forwarding, conversion, cache/mapping or watch-firmware logo limitation is the likely area**, with the exact cause unconfirmed. A native watch app, direct Bluetooth integration, FCM, extra permissions, arbitrary `largeIcon`, package spoofing and channel resets are not justified fixes.

## Changes and validation

- Inspected: `ic_notification.xml`; both builders in `Alerts.kt`; `GuardianViewModel.test()`; `MonitorService` foreground/threshold paths; manifest application label/launcher references; adaptive, legacy and monochrome launcher resources; channel, priority, visibility, title and message metadata.
- Changed: version metadata, CI APK artifact name, `IconNotificationTest.kt`, README and investigation/device-test documentation.
- Preserved: **all production Kotlin, every icon resource, launcher artwork and signing configuration**. The new test round-trips monitoring, threshold and test notifications through Android `Parcel`, then verifies resource type, package, ID, drawable resolution, visibility, priority, channel, category and readable fields. This models Android serialization, not Zepp's proprietary transformation.

`./gradlew testDebugUnitTest lintDebug assembleDebug`: **BUILD SUCCESSFUL**. **22 tests passed**, zero failures/errors/skips, including all 12 unchanged session tests. Lint: **0 errors, 7 existing warnings** (dependency updates and the retained API 26 qualifier). Tests run on a simulated API 35 framework with native graphics, not physical Android 16 or Zepp.

APK v2 signature, 16 KB ZIP alignment, version/package metadata and absence of network/Bluetooth permissions verified. Signing certificate matches the delivered 1.2.1 APK. APK SHA-256: `1b927e20c35c644276e12d2b49b0a2c3089257b587a4565ecf586da11565f46d`.

APK: `app/build/outputs/apk/debug/app-debug.apk`; delivery copy: `/workspace/artifacts/Charge-Guardian-1.3.0-debug.apk`. CI artifact: `Charge-Guardian-1.3.0-debug`.

**The watch icon is not confirmed fixed.** No physical-device retest was performed here. Follow the [Xiaomi / Amazfit test procedure](STAGE_1_3_DEVICE_TESTS.md); keep the result classified as unresolved if the watch remains blank despite a correct phone icon.
