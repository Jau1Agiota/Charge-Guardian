# Stage 1.3 — Xiaomi 17T / Amazfit Active 2 test procedure

Record the HyperOS/Android build, Zepp version, Active 2 firmware, Charge Guardian version **1.3.0 (5)**, and the comparison app/version. The known baseline is: phone icon correct, Zepp settings show the full-color launcher artwork, watch receives text but its icon is blank, restarting already tried without success.

1. **Install without losing state.** Install the delivered debug APK over the existing app with the same signing key (`adb install -r Charge-Guardian-1.3.0-debug.apk`), or build/run the project with the existing Android Studio signing setup. Do not uninstall, clear app data, clear launcher storage, re-pair or factory-reset as part of this investigation. Open Help to confirm version 1.3.0 (5). Preserve the existing theme/threshold and monitoring preference.
2. **Create a fresh test.** Clear only previous Charge Guardian alert/test notifications if needed. Tap **Send Test Notification**. Record the test time and configured threshold. The test does not consume a real charging-session alert.
3. **Inspect Android.** Confirm the recognizable battery-and-bolt small icon, Charge Guardian name, Test title and readable threshold message. Inspect the expanded notification as well. A green icon is expected Android theme tint, not a failure of the white vector. The full-color launcher logo and small status icon are different resources.
4. **Inspect the watch.** Confirm the matching message arrives and record whether the icon is correct, generic or blank. Account for Zepp's existing screen-off-only setting: lock the phone after checking its notification if that setting requires it. Record delivery and icon rendering separately; delivered text with a blank logo is not a charging-monitoring failure. Photograph/crop the Charge Guardian notification on both devices without unrelated private notifications.
5. **Use a comparison control.** If available, trigger a normal notification from AccuBattery (or another installed app), with Zepp forwarding enabled for that app. Record the same phone/watch fields. If the control icon works but Charge Guardian is blank, package-specific mapping/conversion/cache handling is more likely. If both watch icons are blank, a broader companion/firmware issue is more likely. This comparison does not prove either cause.
6. **Optional real threshold confirmation.** During a new charging session, set a reachable threshold and verify the normal battery alert separately. It uses the same small icon, channel, category and visibility as the test, but a different title/notification ID. Unplug to reset a session that already alerted. Do not change production code or repeatedly generate threshold alerts to test the watch logo.
7. **Capture the outcome.** If Android remains correct and the watch is blank, classify it as an unresolved likely **Zepp forwarding/icon-mapping or firmware limitation**. Zepp already recognizing the launcher icon rules out total phone-side app recognition failure, but does not prove a logo was transferred. Do not report this build as a watch-icon fix without a physical result.

| App/path | Phone icon/name/text | Zepp lists app/logo | Watch text arrives | Watch icon | Versions/time |
|---|---|---|---|---|---|
| Charge Guardian test | | | | | |
| Charge Guardian threshold (optional) | | | | | |
| AccuBattery / control app | | | | | |

If reporting the issue to Zepp/Amazfit, use the existing user support/feedback flow and include these versions, the steps and cropped evidence; avoid claiming an undocumented vendor restriction as fact. Useful vendor questions: does this firmware accept custom logos for arbitrary Android packages, where does Zepp obtain the logo (small icon, application icon or mapping), and is there a supported logo-cache refresh or a known Active 2 issue?

No further reboot loop is prescribed: the user already tested restarts. A successful result after this unchanged-icon build could be cache/version-state related; it would not prove the Android vector was previously defective.
