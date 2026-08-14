# Lplanner for Android — v1.0.0

Android port of the Lplanner iOS dive planner. Jetpack Compose front end over the
**same ZPlanKit C engine** (v1.7.0) the iOS app uses — compiled here with the NDK,
not reimplemented.

```
Lplanner-Android/
├── app/src/main/cpp/
│   ├── CMakeLists.txt          compiles ../../ZPlanKit/Sources/CZPlan/czplan.c
│   └── zplan_jni.c             JNI bridge (profile text in, report text out)
├── app/src/main/java/com/landerlab/lplanner/
│   ├── ZPlan.kt                Kotlin API — mirrors ZPlanKit.swift
│   ├── PlannerModel.kt         port of PlannerModel (ZPlannerView.swift)
│   ├── MainActivity.kt         equivalent of LplannerApp.swift
│   └── ui/                     Theme · Common · PlannerScreen · ConfigSheet · LogSheet
└── play-store-icon-512.png     512×512 listing icon
```

---

## Layout

```
/Volumes/KingstonData/
├── DeveloperApple/
│   ├── Lplanner/           iOS + macOS app (Xcode)
│   └── ZPlanKit/           the shared C engine  <-- compiled by BOTH apps
├── DeveloperAndroid/
│   └── Lplanner-Android/   this project
└── KeyStocklite/           backup copy of the signing key

/Volumes/StockThesis/
└── stocklite-flutter/      Stock Lite (Flutter), same Play account
```

**Keep these paths free of spaces.** The Android NDK's CMake/ninja toolchain
fails on paths containing spaces, and the failure is misleading: Gradle sync
succeeds and the Kotlin half compiles, then the C step dies. The folders were
renamed from `Developer Apple` / `Developer Android` for exactly this reason.

## Why there is only one engine

`app/build.gradle.kts` passes `zplankit.dir` into CMake, which compiles `czplan.c`
**in place** from your existing ZPlanKit checkout. Nothing is copied or vendored.
A fix to the decompression model lands on both platforms at once, and the two apps
cannot silently drift apart.

If you move either directory, update `zplankit.dir` in `gradle.properties`
(currently `../../../DeveloperApple/ZPlanKit`, relative to the app module).
An absolute path is the safer choice.

The engine is C99 with no dependency beyond libm, so the NDK builds it unmodified
for `arm64-v8a`, `armeabi-v7a` and `x86_64`.

## The bridge is deliberately narrow

`ZPlanKit.swift` drives the engine entirely through `profile.dat` **text** —
`zp_parse_profile()` builds the whole `zp_config`. The JNI layer does the same, so
only three strings ever cross the boundary:

```
profile text (+ optional tissue text)  →  report text, warnings, tissue text
```

None of `zp_config`'s ~40 fields are marshalled field by field. That is what keeps
the two front ends honest: `PlannerModel.profileText` in Kotlin is a line-for-line
port of the Swift original, and it is the only thing that has to match.

## Verification performed

| Check | Result |
|---|---|
| Engine compiled standalone, run on `Samples/profile.dat` | byte-identical to committed `plan.out` |
| AddressSanitizer + UndefinedBehaviorSanitizer on reference dive | clean, output unchanged |
| Tissue round-trip (`write_tissues` → `load_tissues`) | max error 4.9×10⁻⁷ bar over 34 compartments |
| Repetitive dive (60 min SI, loaded tissues) | deco 14 → 28 min, correctly longer |
| All 26 profile keys emitted by `PlannerModel` | every one recognised by the parser |
| Conditional branches: GF pair, CCR `DecoSetpoint`, VVAL-18, imperial | all parse and plan correctly |

Not yet verified on device — see Remaining below.

## Building

Requires Android Studio (Ladybug or newer), NDK 27, CMake 3.22.1.

```bash
cd Lplanner-Android
./gradlew assembleDebug          # or open the folder in Android Studio
```

The Gradle wrapper JAR is not committed. First time, either open the project in
Android Studio (it creates the wrapper) or run `gradle wrapper` once.

## Publishing

Same Google Play developer account as Stock Lite.

- **Stock Lite's applicationId is `com.landerlab.stock_lite`** (a Flutter app).
  Play requires a *unique* applicationId per app, so Lplanner cannot reuse it.
  This project uses **`com.landerlab.lplanner`** — same `com.landerlab` namespace,
  same developer account, same signing key. Once published the ID can never be
  changed, so confirm it before the first upload.
- **targetSdk 36 is mandatory** for new submissions and updates from
  **31 August 2026**. This project already targets 36.
- **Signing**: copy `key.properties.template` to `key.properties` (git-ignored)
  and add the two passwords. Everything else is already filled in from the working
  Stock Lite config at `/Volumes/StockThesis/stocklite-flutter/android/key.properties`:

  | | |
  |---|---|
  | storeFile | `/Users/carloslander/landerlab-keystore.jks` |
  | keyAlias | `landerlab` |

  One keystore signs any number of apps, so Lplanner uses the same key as Stock Lite.
- Build the upload artifact with `./gradlew bundleRelease` (Play requires an AAB).
- Store listing icon: `play-store-icon-512.png`.

### 16 KB page size

Play requires every 64-bit `.so` to have LOAD segments aligned to 16 KB. Two
separate fixes are in place:

| Library | Cause | Fix |
|---|---|---|
| `libzplan.so` | NDK r27 aligns to 4 KB by default | `ANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON` plus explicit `-Wl,-z,max-page-size=16384` in `CMakeLists.txt` |
| `libandroidx.graphics.path.so` | Compose pulls `graphics-path` 1.0.1 (2024), built before the requirement | pinned to `1.1.0` in the version catalog |

Verify a built AAB/APK before uploading:

```bash
unzip -o app/build/outputs/bundle/release/app-release.aab -d /tmp/aab
for f in /tmp/aab/base/lib/arm64-v8a/*.so; do
  echo "$f: $($ANDROID_HOME/ndk/*/toolchains/llvm/prebuilt/*/bin/llvm-readelf -lW "$f" \
    | awk '/LOAD/{print $NF; exit}')"
done
# 0x4000 (16 KB) or larger passes; 0x1000 (4 KB) fails.
```

Upgrading to NDK r28+ makes the first row unnecessary — it aligns to 16 KB by
default — but the explicit flags are harmless there and keep the build correct on
either toolchain.

> **Keystore.** Stock Lite signs with `/Users/carloslander/landerlab-keystore.jks`;
> `/Volumes/KingstonData/KeyStocklite/` holds the backup copy. If both are lost you
> cannot ship updates to Stock Lite *or* Lplanner again — Play's only remedy is a
> manual upload-key reset request, so a third copy off these two drives is worth
> having. Note the backup folder also stores the keystore password in plaintext in
> `stocklite.txt`, right beside the key; separating those two would mean a single
> leaked folder no longer hands over signing authority for both apps.

Because the app calculates decompression schedules, expect the listing to need a
clear safety disclaimer. Play's Health apps policy and the medical-disclaimer
requirement are worth reading before submitting — the iOS listing's wording is a
reasonable starting point.

## Brand

Monochrome throughout: black on white, greys for secondary text and strokes, with
dark-theme equivalents. The single exception is `WarningRed` — functional dive
warnings must render in red, per the ZPlanKit README. The launcher icon keeps the
blue gauge mark for store recognition, and ships an Android 13+ themed
(monochrome) layer derived from it.

## Remaining before release

1. Run on a physical device — the NDK build has been verified by desk-checking and
   host compilation, but has not been compiled by the NDK toolchain itself.
2. Differential test: run the same profiles through iOS and Android, diff the
   reports. They share an engine, so any difference points at `profileText`.
3. Persist config across launches (the iOS app does not either — parity today).
4. Decide whether `Log` should survive process death; it is session-only, matching iOS.

---

> **WARNING — this software plans decompression dives and can kill you.**
> It is experimental and unvalidated. Never dive a schedule from this program
> without independently verifying it against trusted tables or software, and never
> without formal mixed-gas decompression training.
