# Stage 1.2 — Icon corrections

**Version 1.2.0 (3)** · `com.chargeguardian.app` · min API 26, target API 36.

## Confirmed launcher cause and correction

Stage 1.1 fitted the entire square illustration into a 46dp square within a 108dp adaptive layer. Since the launcher displays approximately the central 72dp, the illustration occupied only 64% of its visible width. Legacy resources added their own 70% reduction and the round fallback was derived from an already masked square.

The original `assets/Cozy Neon Battery Hug.png` is unchanged. The foreground now uses a proportional **58dp** illustration on a **transparent 108dp layer**, over the existing separate dark background. It occupies about **81%** of the visible viewport: **26% larger linearly** than before. No foreground mask is baked in; Android applies its launcher mask. Legacy rounded-square and round icons are generated independently from the same composition and masked once. All five densities are included, and manifest `android:icon` / `android:roundIcon` still point to their respective tested adaptive resources. Existing themed-icon support is retained.

Layer tests check transparent foreground margins and the solid background separately. Measured foreground bounds and marked source landmarks for the crown/flyaway hair, face and battery are checked against the 66dp circular safe area. Common-mask previews were visually inspected for those details. Peripheral frame/background corners may be cropped by the launcher mask, rather than shrinking the entire illustration to preserve empty corners.

- [48px previews, without image upscaling](previews/stage-1-2-launcher-48px.png)
- [144px previews (48dp at 3× density)](previews/stage-1-2-launcher-144px.png)

These are modeled masks, not HyperOS screenshots. The referenced launcher screenshot was not attached or present in the repository; exact comparison against the user's installed-app grid and physical Xiaomi verification remain pending.

## Notification icon and metadata

The old bolt was a valid, nonblank Android small icon, explicitly assigned in both alert/test and monitoring builders. Therefore a missing Android resource is **not an established cause** of the watch's blank image.

A dedicated **white battery outline/cap and lightning bolt** now replaces the thin bolt. It is a 24dp vector with transparent background/cutouts, no color artwork, gradients or shadows. Native rendering tests verify white pixels, opaque stroke interiors, transparent holes/corners and alpha coverage at **18/24/32px**. [Actual-size notification previews](previews/stage-1-2-notifications.png) show the silhouette on a dark plate; that plate is not part of the icon resource.

Every notification builder explicitly uses `setSmallIcon(R.drawable.ic_notification)`. No path uses a launcher or empty icon. Threshold/test notifications retain `battery_alerts`, HIGH importance, REMINDER category, selected-theme accent, Charge Guardian title and configured-threshold message. Ongoing monitoring uses its existing quiet LOW-importance `monitoring` channel, the same valid small icon, and an explicit SERVICE category. Channel IDs and user settings are preserved; Android notification channels do not provide a separate watch-logo field. Application label, launcher metadata and rendered notification fields are tested.

## Zepp / Amazfit findings and limits

Android small-icon validity does **not** prove Zepp will send that image to the watch. A forwarding companion must render/encode an image or select a known application-logo mapping; the phone APK's resource ID is not itself a watch image.

The related, open-source [Gadgetbridge ZeppOsNotificationService](https://github.com/Freeyourgadget/Gadgetbridge/blob/master/app/src/main/java/nodomain/freeyourgadget/gadgetbridge/service/devices/huami/zeppos/services/ZeppOsNotificationService.java) handles icon requests keyed by package name, calls `NotificationUtils.getAppIcon`, and sends a `notification://logo?app_id=...` bitmap. This is evidence of a package/application-icon path in the ecosystem, **not proof of proprietary Zepp's implementation or this Active 2 firmware's behavior**. Requests to Amazfit support endpoints returned HTTP 403 here; no version-specific guarantee of custom/sideloaded-app icon forwarding was verified.

The watch-side root cause remains unconfirmed: package mapping, cached logos, companion conversion or firmware behavior need device evidence. After updating, clear old notifications, toggle Charge Guardian in Zepp App alerts, reconnect the watch, and send a fresh test. Compare with a known app and record phone/Zepp/watch firmware versions. If the phone renders the new icon but the watch remains blank, that Zepp/firmware combination may not support a logo for this sideloaded package. Text forwarding can still work; no Bluetooth integration, watch application, speculative large-icon workaround or new permission is added.

## Validation and delivery

`./gradlew testDebugUnitTest lintDebug assembleDebug`: **BUILD SUCCESSFUL**. **21 tests passed**, zero failures/errors/skips, including the 12 unchanged session tests. Lint: **0 errors, 7 existing warnings** (six dependency-update notices and the retained API 26 resource-qualifier advisory).

All 15 PNG dimensions and repeatable generation verified. APK v2 signature, 16 KB ZIP alignment, version 1.2.0 (3), target API 36 and absence of network/Bluetooth permissions verified. APK SHA-256: `cec9d72d69e1e202b1c4d28f7a1f1de098f718d34ea3af09851c7c25bc53e993`.

Monitoring/session logic, thresholds, DataStore, themes, navigation and dashboard sources are unchanged. The only notification-code change is the ongoing SERVICE category; alert icon assignment already referenced the dedicated resource that was replaced. New tests cover foreground scale/safe landmarks/common masks and ongoing metadata alongside all existing suites.

Build APK: `app/build/outputs/apk/debug/app-debug.apk`. Versioned delivery copy: `/workspace/artifacts/Charge-Guardian-1.2.0-debug.apk`. CI artifact: `Charge-Guardian-1.2.0-debug`. Updating an existing installation requires the same signing key; uninstalling erases saved app/session state.

Physical HyperOS launcher rendering and Amazfit/Zepp icon forwarding still require retesting on the actual devices. No hardware success is claimed.
