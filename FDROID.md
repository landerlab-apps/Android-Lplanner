# Publishing to F-Droid

Notes for submitting Lplanner to the F-Droid main repository, and the things
about this project that a build recipe has to account for.

## What F-Droid needs that a normal build does not

F-Droid compiles the app **on their servers, from source, and signs it with
their own key**. Nothing you build locally is uploaded. That imposes three
requirements this project has to satisfy:

1. **Every line of source must be public and freely licensed.** That includes
   the decompression engine, which is a separate repository — see below.
2. **No proprietary dependencies and no pre-built binaries** in the tree. All
   dependencies here are AndroidX and Compose from Maven Central and Google's
   repository, which are acceptable.
3. **The build must be reproducible by a machine that has only the repository**
   — no absolute paths pointing into your own disk.

## The engine is a submodule

The Android app does not contain the decompression engine. `app/src/main/cpp/
CMakeLists.txt` compiles `czplan.c` out of the ZPlanKit repository, which is
the same source the macOS and iOS builds use — that is what guarantees a dive
plans identically on every platform.

For local development the path comes from `gradle.properties`:

```
zplankit.dir=../../../DeveloperApple/ZPlanKit
```

That path exists only on the developer's machine, so **it must not be what
F-Droid uses**. Add ZPlanKit as a git submodule and point the build at it:

```bash
git submodule add https://github.com/landerlab-apps/ZPlanKit.git engine
git commit -m "Add the decompression engine as a submodule"
```

Then the recipe overrides the property at build time with `-Pzplankit.dir=engine`,
and F-Droid's `submodules: true` fetches it. Pinning to a submodule commit is
also what stops an engine change from silently altering a published schedule:
the app version and the engine commit move together.

## Build recipe

F-Droid metadata lives in their `fdroiddata` repository as
`metadata/com.landerlab.lplanner.fdroid.yml`. Submit it as a merge request.

```yaml
Categories:
  - Sports & Health
License: GPL-3.0-only
AuthorName: Carlos Lander
AuthorEmail: scubalander@gmail.com
SourceCode: https://github.com/landerlab-apps/Android-Lplanner
IssueTracker: https://github.com/landerlab-apps/Android-Lplanner/issues
Changelog: https://github.com/landerlab-apps/lplanner/blob/main/CHANGELOG.md

AutoName: Lplanner

RepoType: git
Repo: https://github.com/landerlab-apps/Android-Lplanner.git

Builds:
  - versionName: 1.6.0
    versionCode: 11
    commit: v1.6.0
    subdir: app
    submodules: true
    gradle:
      - fdroid
    ndk: r27c
    gradleprops:
      - zplankit.dir=engine

AutoUpdateMode: Version
UpdateCheckMode: Tags
CurrentVersion: 1.6.0
CurrentVersionCode: 11
```

Points worth checking before you submit:

- `gradle: [fdroid]` selects the flavour. The `play` flavour is never built by
  F-Droid.
- `ndk: r27c` must match `ndkVersion` in `app/build.gradle.kts`
  (27.2.12479018). If you bump one, bump the other.
- `commit:` must be a **tag**, not a branch. Tag each release: `git tag v1.6.0`.
- The application id F-Droid publishes is `com.landerlab.lplanner.fdroid`,
  because of the flavour suffix. The metadata filename must match it.

## Signing and the two application ids

The Play build and the F-Droid build have different application ids on
purpose. Google Play signs release artefacts with a key Google holds; F-Droid
signs with F-Droid's key. Two different certificates under one application id
means Android will not install either build over the other — a diver moving
between them would have to uninstall first, losing his plan log and his
carried tissue loading.

The suffix costs an untidy package name and buys side-by-side installation.

If seamless switching ever matters more than that, the route is F-Droid's
**reproducible builds**: F-Droid compiles from source, verifies their output
matches an APK you signed yourself, and then distributes *your* APK under
*your* signature. It requires a byte-for-byte deterministic build including
the NDK output, which is real work, and it is incompatible with Play App
Signing generating the Play artefacts. Not worth attempting for a first
release.

## Anti-features

None apply. The app has no network permission, no advertising, no tracking,
and no dependency on a non-free network service. Expect a clean listing.

## Checklist

- [ ] ZPlanKit published at `github.com/landerlab-apps/ZPlanKit`, GPL-3.0
- [ ] `engine` submodule added and committed
- [ ] `Android-Lplanner` public on GitHub
- [ ] Release tagged `v1.6.0`
- [ ] `./gradlew assembleFdroidRelease` succeeds from a clean checkout
- [ ] Screenshots in `fastlane/metadata/android/en-US/images/phoneScreenshots/`
- [ ] Merge request opened against `fdroiddata`
