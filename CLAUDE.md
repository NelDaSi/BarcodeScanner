# DAF Scanner

## What this is

An Android app ("DAF Scanner", package `com.neldasi.dafscanner`) for scanning and
verifying industrial Data Matrix codes against part serial numbers. Built and
maintained solo by Nelson. Core workflows:

- **Scan mode**: point the camera at a Data Matrix code, parse it into
  Type Code / Supplier Code / Serial (hex + decimal) / Batch Number, save it
  to a local history with an optional photo attached.
- **Verify mode**: import an expected-parts list from a CSV (column named
  "Product ID", comma- or semicolon-delimited), then scan items to check them
  off against that list and export a verification report.
- **Convert**: quick hex↔decimal serial conversion, with its own history.

Barcode payloads follow three known industrial formats (see
`extras/PartParser.kt`): **MX11** (contains `K`), **P14** (27 numeric
digits), **MX13** (older 29-char / `0074`-marker format). Parsing logic for
these lives entirely in `parseScannedCode()` — this is the single most
business-critical function in the app.

## System map — what handles what

| Concern | Where | Notes |
|---|---|---|
| UI | `screens/*.kt` | 100% Jetpack Compose, Material 3, one file per screen (Camera, Search/Verify list, Detail, Converter, Settings, Main) |
| Navigation | `navigation/` | Compose Navigation, single-Activity |
| State management | `viewmodels/*.kt` | `AndroidViewModel` + `StateFlow`. No DI framework — each ViewModel constructs its own DAOs/repos in `init {}` |
| Camera capture | CameraX (`androidx.camera.*`) | Bound in `CameraScanScreen.kt` |
| Barcode decoding | ML Kit Barcode Scanning, primary | `extras/ImageAnalyzer.kt` (`DafImageAnalyzer`) |
| Barcode decoding fallback | ZXing `DataMatrixReader`, center-crop | Same file, used when ML Kit finds nothing; both listener chains run on the camera's background executor (not main thread) to keep the UI thread free |
| Barcode → structured data | `extras/PartParser.kt` | `parseScannedCode()` / `EngineFormat` enum |
| CSV import parsing | `extras/SearchCsvParser.kt` | Pure function `parseSearchItemsCsv(String)`, hand-rolled tokenizer (handles quoted fields, `;`/`,` delimiter sniffing, header aliasing) — deliberately kept free of Android/Context deps so it's unit-testable |
| Persistence (structured data) | Room, `data/` | `AppDatabase` (version 4) with `ScanDao`/`ScannedPart`, `SearchItemDao`/`SearchItem`, `ConversionDao`/`ConversionRecord`. Migrations must be added explicitly on any version bump — no destructive fallback |
| Persistence (settings/filters) | SharedPreferences via `extras/ScanStorage.kt` | Also owns the pending-scan queue used to bridge continuous-scan mode back to the previous screen |
| Scan photos | Plain files in `context.filesDir` | Named `img_<fullCode>.jpg` (`extras/ImageUtils.kt`); excluded from Android auto-backup (see below) — deleting a scan also deletes its photo |
| Self-update check | `extras/UpdateManager.kt` | Polls the GitHub Releases API for this repo, compares `versionName`, offers an in-app APK download+install (no Play Store) |
| Image loading (thumbnails) | Coil | |
| JSON | Gson (pending-scan queue), kotlinx.serialization (ScannedPart, GitHub API responses) | Both are in use; not consolidated |

Build config: `compileSdk 37`, `minSdk 27`, `targetSdk 35`, Kotlin + KSP (for
Room), R8 minify+shrink on release builds, no flavors.

## Deploy workflow

There is **no Play Store distribution** — releases are GitHub Releases, and
the app updates itself by polling that feed (`UpdateManager.kt`).

1. **Cut a release locally**: run `scripts/release.sh`. It's an interactive
   bash tool that requires a clean working tree, prompts for a new
   version name/code (suggests `currentCode + 1`), bumps
   `app/build.gradle.kts`, commits (`chore: bump version to X (Y)`), tags
   `vX.Y`, and offers to push. Two modes:
   - *Standard*: releases from the current branch as-is.
   - *Production*: merges the current branch into `main`, releases from
     `main`, then merges `main` back into the original branch.
2. **CI build+sign+publish**: pushing a `v*` tag triggers
   `.github/workflows/release.yml`, which runs `./gradlew assembleRelease`,
   signs the APK (`r0adkll/sign-android-release`, secrets `SIGNING_KEY` /
   `ALIAS` / `KEYSTORE_PASSWORD` / `KEY_PASSWORD`), renames it to
   `dafscanner-<tag>.apk`, and publishes it as a non-draft GitHub Release
   (`softprops/action-gh-release`) with the triggering commit message as
   release notes.
3. **In-app self-update**: on launch (and on demand from Settings),
   `UpdateManager.checkForUpdates()` hits
   `api.github.com/repos/.../releases/latest`, compares tag vs installed
   `versionName`, and if newer prompts the user. Accepting downloads the APK
   via Android `DownloadManager` into `Downloads/` and fires an install
   intent through `FileProvider` (`REQUEST_INSTALL_PACKAGES` permission).

So the full loop is: `scripts/release.sh` → tag push → GitHub Actions
build/sign/publish → installed apps notice on their own and self-update.

## Working conventions in this repo (established this session)

- Java for Gradle CLI runs isn't on PATH by default in this environment —
  set `JAVA_HOME="/var/home/neldasi/Android Studio/jbr"` before `./gradlew`.
- An emulator (`emulator-5554`) may already be running; instrumented tests
  (`./gradlew :app:connectedDebugAndroidTest`) can run against it directly.
- Room: never reintroduce `.fallbackToDestructiveMigration()` for upgrades —
  only `.fallbackToDestructiveMigrationOnDowngrade()` is acceptable. Any
  future `version` bump on `AppDatabase` needs a real `Migration`, or the DB
  will refuse to open (intentional — fail loud, not silently wipe data).
