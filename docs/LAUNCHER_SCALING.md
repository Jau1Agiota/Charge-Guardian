# Launcher footprint correction — investigation

Measured before changing resources:

- Original `assets/Cozy Neon Battery Hug.png`: 1254 × 1254, fully opaque. The saturated bright-green frame/battery bounds are **1178 × 1168 at (38, 23)**. There is no transparent source padding to trim; the rounded neon frame and black corners are already rendered into the image.
- xxxhdpi foreground: 432 × 432, nontransparent bounds **232 × 232 at (100, 100)**, corresponding to 58dp within the 108dp layer.
- Thus the scene occupied 58/72 = 80.6% of the visible adaptive window, and the bright frame about **75.7%**. Android's outer mask surrounds an already inset framed tile. The small appearance is real nested visual padding, not a bad-density or manifest-reference problem.
- Both adaptive XMLs use the same foreground/background and retain a monochrome layer. The application and round-icon manifest references are correct. Legacy generation also shrank the complete framed image.

The previous requirement to fit every source corner/flyaway inside a 66dp circle protected irrelevant corner/background pixels at the expense of launcher scale. Safe-area guidance protects essential content; it does not require the entire square photograph to become a small badge.

Implementation and final validation follow below.

## Corrected composition

The layer canvas remains **108 × 108dp**. The scene now fills the central **72 × 72dp viewport**, with a **2dp downward optical offset** to keep the crown clear of the circular mask. At xxxhdpi its alpha bounds are **288 × 288 at (72, 80)** in the 432px layer. The original bright frame now occupies roughly **94%** of visible width, versus 76% before: about **24% larger linearly**.

The central **66 × 66dp safe area** protects the face, battery and adjacent hair; tests additionally check main-crown/hair landmarks against the circular viewport. It is not used to shrink the whole photograph, including black corners, into a small circular badge. The outer neon frame/background corners and peripheral stray pixels can be trimmed by the OS mask. The previews preserve the important portrait and battery details, but do not claim that every original peripheral pixel survives every OEM mask or parallax implementation.

The foreground has transparent outer layer margins for adaptive rendering; the separate existing background remains unchanged. No new inner rounded-square mask is added. Legacy round and rounded-square fallbacks use the same full-viewport composition and receive only their final outer mask. Density-specific pixel positioning is quantized to whole pixels.

## Visual verification

- [Original versus corrected, actual 48px images](previews/launcher-footprint-48px.png)
- [Original versus corrected, 144px images (48dp at 3×)](previews/launcher-footprint-144px.png)
- [Layer geometry: cyan 72dp viewport, yellow 66dp safe area](previews/launcher-layer-safe-area.png)
- [Unmodified original artwork](../assets/Cozy%20Neon%20Battery%20Hug.png)

“Before” is version 1.2.0 rendered with the same mask model. The image supplied in this conversation illustrates the nested-padding problem; it does not contain the actual Xiaomi app grid or Aurora Video / Midnight ASMR icons. No substitute competitor icons or fabricated comparison are used. **Exact visual acceptance beside those installed apps remains pending a real HyperOS screenshot/device check.** The renders verify the changed footprint, not physical Xiaomi launcher behavior.

## Exact resources changed

- `scripts/generate-icons.sh`: full-viewport composition and optical positioning.
- `app/src/main/res/drawable-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/ic_launcher_foreground.png`: five corrected 108dp layers.
- `app/src/main/res/mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/ic_launcher.png` and `ic_launcher_round.png`: ten consistent legacy resources.
- Launcher geometry assertions in `IconNotificationTest.kt`, patch version metadata and the CI artifact name; this report and previews.

Both adaptive XMLs and manifest icon references already point to these resources and are unchanged. The monochrome themed-icon resource is unchanged. Notification small icon, `Alerts.kt`, monitoring/session code, DataStore, themes, dashboard/navigation and the source artwork are byte-identical to the base revision. No Zepp investigation or workaround is part of this change.

## Build and installation

Version **1.2.1 (4)**; application ID remains `com.chargeguardian.app`, with the existing debug signing configuration/key. The delivered APK’s signing certificate matches the previously delivered 1.2.0 APK.

`./gradlew testDebugUnitTest lintDebug assembleDebug`: **BUILD SUCCESSFUL**. **21 tests passed**, zero failures/errors/skips. Lint: **0 errors, 7 existing warnings** (dependency updates and the explicit API 26 resource qualifier). All 15 generated 8-bit PNG dimensions and deterministic regeneration checked. APK v2 signature, 16 KB ZIP alignment, package identity and version metadata verified.

APK SHA-256: `ee2d10cf045683ac7dbd2f905e7314ccc43db3139f25e56f03e1940b0136c0be`.

Open the project in Android Studio and run the app on the connected Xiaomi, or install the delivered APK with `adb install -r`. The signed copy is `/workspace/artifacts/Charge-Guardian-1.2.1-debug.apk`; the Gradle output is `app/build/outputs/apk/debug/app-debug.apk`. If HyperOS retains an old shortcut image, remove/re-add that shortcut or restart the launcher/phone before comparing. Do not clear launcher storage or uninstall merely to refresh an icon: those actions can lose launcher layout or app data.
