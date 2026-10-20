# Stage 1.1 — UI, themes and icons

**Version 1.1.0 (2)** · `com.chargeguardian.app` · Android 8+ · target API 36.

## Changes

- Compact dashboard: live percentage, circular progress, charger state, threshold slider, monitoring toggle/status, test notification and offline footer. The introductory slogan and tutorial are removed.
- The palette button opens Appearance; `?` opens scrollable Help with Zepp, HyperOS, permissions and troubleshooting instructions. Both support visible and system back navigation and activity recreation.
- Appearance applies immediately and persists in a separate DataStore key. Existing threshold, monitoring and session preferences are preserved. Missing/unknown theme keys default to Electric Neon.

| Theme | Accent | Background | Panels |
|---|---|---|---|
| Electric Neon (default) | `#83FF00` | `#090C09` | `#141A12` |
| Neon Mint | `#83F7BB` | `#0B1014` | `#141F22` |

Both palettes use contrasting text and light system-bar icons. Measured main text, secondary text, accent and button-label contrast ratios exceed 4.5:1. There are no continuous animations or glow effects.

The slider retains the existing **1–100%** range, preserving older sub-80 preferences. A clean track replaces the dense tick marks. Whole-percent snapping and accessible minus/plus buttons support precise adjustment; every value from **80% through 100%** is tested.

## Icons and notifications

`assets/Cozy Neon Battery Hug.png` is unchanged. Launcher images are proportional resizes of the complete artwork. Its adaptive foreground occupies a 46dp square inside the 108dp layer, fitting within Android's 66dp circular safe area. Padding preserves the face, battery and original frame under different masks.

Resources include five densities, rounded legacy fallbacks (48/72/96/144/192px), adaptive foregrounds and explicit `mipmap-anydpi-v26` launcher/round XML. Both adaptive resources declare the artwork-derived monochrome bolt for Android 13+ themed icons; older Android versions ignore that element. `scripts/generate-icons.sh` regenerates PNGs using ImageMagick. The generic Stage 1 icon is removed.

A white, transparent silhouette traced from the artwork's lightning bolt serves as the notification small icon. Threshold/test alerts read the saved theme for their accent, retaining the channel IDs, app name and configured-threshold message. Theme changes do not repost alerts or reset sessions. The ongoing notification also uses the bolt; its quiet channel behavior is unchanged.

Android and **Zepp control notification presentation**. The full-color artwork and accent cannot be guaranteed on Amazfit Active 2. No watch app, Bluetooth connection, internet permission or watch API was added.

## Monitoring and validation

`MonitorService.kt`, `Session.kt`, `Battery.kt` and the 12 original session tests are unchanged. Store adds the appearance preference and a constructor for testing real persisted storage. Notification theme reads occur in the existing coroutine context; no polling or background theme worker is added.

`./gradlew testDebugUnitTest lintDebug assembleDebug` completed successfully:

- **19 tests passed**, including all 12 original session tests; zero failures/errors/skips.
- Lint: **0 errors, 7 warnings** — six newer-dependency notices and one advisory about the explicit API 26 resource qualifier. The explicit qualifier is retained after verifying adaptive resource selection; both launcher variants expose their monochrome resource in tests.
- All 15 launcher PNGs have the expected density dimensions; regeneration produced identical bytes.
- APK Signature Scheme v2 and 16 KB ZIP alignment verified. Package/version/target SDK and absence of INTERNET permission verified in the packaged/merged metadata.
- APK SHA-256: `ffd3358a3eba7504deea0e86d443c222cfca536ee6c39678f91b09f73b3d8012`.

Tests cover real DataStore close/reopen without losing the alert claim, theme selection/recreation, visible/system back, Help scrolling, all 80–100% slider values, adaptive/monochrome rendering, white/transparent icons at 18/24/32px, and notification text/icon/color in both themes. UI/resource tests use Robolectric with native graphics and an API 35 framework. Rendered PNGs are under `app/build/outputs/ui-validation/` and `app/build/outputs/icon-validation/`; these are simulated renders, not device screenshots.

APK: `app/build/outputs/apk/debug/app-debug.apk`. CI uploads `Charge-Guardian-1.1.0-debug`. Install with `adb install -r` using the same debug signing key as the previous installation. Uninstalling to resolve a signing-key mismatch erases preferences.

## Device testing still pending

On Xiaomi HyperOS, verify launcher masks/themed icons, both palettes, large fonts, gesture/button back and restart persistence. Drag/step through 80–100%; check phone notifications and Zepp forwarding with each theme. Watch appearance and OEM service survival require physical testing. The [existing device checklist](DEVICE_TEST_CHECKLIST.md) and Stage 1 recovery limitations remain applicable.
