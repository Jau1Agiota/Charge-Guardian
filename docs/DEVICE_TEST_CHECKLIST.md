# Xiaomi HyperOS and Amazfit acceptance checks

Physical-device checks are pending; cloud unit tests do not verify OEM lifecycle behavior or watch forwarding. Record phone/HyperOS build, Android security patch, Zepp version and watch firmware with results.

## Initial setup

- Install the debug APK on the Xiaomi 17T / Android 16. Open Charge Guardian and grant notifications. Verify default threshold 95%, live percentage, charger status and progress.
- Enable monitoring while the app is visible. Confirm the ongoing low-importance notification appears. Disable monitoring and confirm it stops. Enable again.
- In HyperOS app settings enable Background autostart and choose Battery saver → No restrictions for both Charge Guardian and Zepp. Menu names vary. If available, lock both in recent apps. This is an OEM mitigation, not a guarantee.
- Give Zepp Notification access in Android settings. In Zepp → Amazfit Active 2 → Notifications and reminders → App alerts, enable Charge Guardian. If the app is not listed, send a test notification first. Check Zepp's “only when screen off” option.
- Send Test Notification. Verify exact configured percentage on the phone and watch, readable full text, vibration, and opening the phone app by tapping. Test alerts do not consume a charging-session alert.

## Charging behavior

- Set a threshold just above current battery. Connect power, close/swipe away UI, lock screen, and cross threshold. Verify one phone alert and one forwarded watch alert.
- Leave connected; allow more percentage updates, reconnect watch and reopen app. Verify no second threshold alert.
- Unplug, reconnect while above threshold, and verify one new alert. Repeat with USB, AC and wireless charging if supported. A plugged, full battery must still count as connected.
- Lower threshold below current percentage before an alert: expect an immediate alert. Raise it before crossing: expect the app to wait. Change it after an alert: expect no rearming until unplugged.
- Change threshold, exit and relaunch. Verify it persists.

## Recovery and restrictions

- With monitoring enabled, kill only the background process using a debug process-kill method (not Settings Force stop). Confirm system sticky-service recovery and no duplicate after an earlier alert. Repeat below threshold, then cross it.
- Reboot with monitoring enabled, unlock once, and check that monitoring returns. Repeat both connected and disconnected, and before/after an alert. Reboot with monitoring disabled: it must remain off.
- Leave the phone screen off for an extended charging session with HyperOS power saving enabled/disabled. Record whether the service survives and alerts arrive on time. Observe an overnight session without assuming Android emulator behavior matches HyperOS.
- Force-stop in app settings or stop via Android's Active apps control. Expect monitoring to cease; Android does not guarantee automatic recovery after user stop. Reopen the app and enable monitoring again. A fresh install also requires opening/enabling once.
- Deny POST_NOTIFICATIONS; monitoring must not enable. Revoke notification permission or disable Battery alerts while already monitoring; no alert may be claimed while blocked. Restore permission, reopen and re-enable if needed; verify the current threshold is evaluated. Disabling the Monitoring status channel alone does not disable Battery alerts.
- Disconnect/reconnect while the process is prevented from running. Document the limitation: an entirely unobserved unplug/replug cannot be reconstructed from Android's current battery snapshot; persisted state conservatively avoids a duplicate.
- Enable DND/silent mode and Zepp screen-off-only forwarding separately. Verify user notification settings are respected; do not interpret suppressed watch vibration as duplicate/session failure.

Optional ADB battery simulation on a test device: `adb shell dumpsys battery set ac 1`, `adb shell dumpsys battery set level 94`, then `95`. Simulate unplug with `adb shell dumpsys battery set ac 0` (also clear USB/wireless if set). Always finish with `adb shell dumpsys battery reset`. These commands simulate framework values and do not prove physical charging or HyperOS survival.
