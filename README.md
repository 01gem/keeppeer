# Keeppeer — Offline Password Keeper

An Android password keeper built with Kotlin, Jetpack Compose, and Material 3. The encrypted vault stays on-device and the app has no network permission. A user-selected SAF provider controls where plaintext CSV backups are stored, including possible remote destinations.

> **Phase 3 complete for the requested CSV scope, with recorded provider/JVM/spreadsheet limits.** 46 JVM tests pass; debug assembly, AndroidTest packaging and lint pass (0 errors/15 warnings). All 11 selected device methods have passing evidence across scoped runs: 9 in the retained mixed group and the final focused 2/2 in 58.650s. Failed attempts retain their original identities. See [CSV contract](docs/csv-contract.md) and [Phase 3 report](docs/reports/phase-3-report.json). Release stays disabled; Phase 4 is unstarted; do not distribute.

Settings → Export CSV requires the plaintext warning, a chosen document destination, and a fresh unlocked-session confirmation. Settings → Import CSV validates the whole file before a count-only confirmation with Skip (default), Update existing or Keep both. File pickers follow the ordinary immediate/timed background lock policy. Password-change backup remains optional.

Phase 3 review snapshot: `powershell -NoProfile -File ./tools/export-source-code.ps1 -Phase 3 -Outcome complete`. The exporter validates the structured outcome and generates the full eligible inventory atomically. Upload source-code.txt manually to the originating review chat. Never upload an actual vault CSV with review sources.

### Phase 2 final verification (historical baseline)

> **Historical Phase2 baseline: complete for the original required criteria, with documented provider/artifact limits.** Retained API25 window regression: 1/1 pass, 7.379s. Final new disposable API31 window regression: 1/1 pass, 15.781s, OK (1 test), all original lifecycle/privacy assertions intact. **Release disabled; Phase3 unstarted; do not distribute.**

Production app SHA-256 1c3adda7dd80d515b8ec64e387a77d64d26b6da3bc261961dd007dc98a4965dc; final test 40305e7296b7f9966ca3d98903fa588997bc9f568b9b44cda9ffc646c9d373d0. The test return driver recognizes focus on its actual dialog or original Activity; final diff review also restored the explicit post-return same-window identity assertion omitted by the pre-existing dirty edit. Host and installed hashes match. The retained API25 native pass has no located contemporaneous APK hash attestation; current hashes are not assigned to that old run. See [final evidence](docs/phase-2-final-window-evidence.md), [current acceptance checklist](docs/phase-2-window-lifecycle-clipboard.md), and [structured report](docs/reports/phase-2-report.json).

The fresh API31 fixture was stopped with registration/data retained. The old unknown-credential/FBE-locked AVD was untouched; the fresh run neither unlocks nor recovers it. Earlier wake/menu rejection remains historical. Clipboard2/2/restored sharing, approved biometric and guard/native results retain original scope. Recovery stays closed for disposable entries; personal/preserved files untouched. Defender clean scan remains user-reported and protection unchanged; no historical false-positive verdict. Earlier sections below are historical where superseded.

### Previous privacy closeout (historical)

> **Phase 2 security development build** — The prior-initialization guard prevents defined silent-setup cases. Core acceptance is covered, but a revealed synthetic draft remained visible in this API31 provider's direct live Recents card; Phase2 stays incomplete for that privacy gap. Ordinary system screenshots and an actual app-owned dialog produced valid protected images. Original missing entries were disposable tests; recovery is no longer requested, cause remains unknown, and preserved material remains untouched. Release stays disabled; Phase3 unstarted; do not distribute. See [privacy closeout](docs/phase-2-privacy-closeout.md).

Current evidence: approved biometric3/3 and selected22/22 on each API24/API25/API31 remain closed with original identities. The ineffective recents-exclusion trial was reverted; no app/test/build/exporter change is retained. The final rebuilt app has a distinct hash and passed only the affected rotation/lifecycle pair2/2,0fail/skip,52.089s; assembly/lint passed. The user's2026-10-30 full-scan/no-new-alerts report is recorded as user-reported, exact time unknown, not a false-positive verdict or an Android test gate. Only the named live-recents requirement remains open; hardware/OEM limits remain explicit. See [acceptance coverage and the precise next observation](docs/phase-2-privacy-closeout.md).

## Features (V1 Roadmap)

- Password entry CRUD with search
- Master password lock with optional biometric unlock
- Auto-lock on background/inactivity
- Secure password generator
- Favorites and categories
- RFC 4180 CSV import/export via SAF

## Tech Stack & Verified Toolchain

| Component | Verified Environment / Declared Version | Source / Notes |
|---|---|---|
| Android Studio | Android Studio 2026.2.2 (Rabbit 2, build `AI-262.10968.63.2622.16730706`) | Installed IDE; minimum required for AGP 9.5 per [official release matrix](https://developer.android.com/studio/releases) |
| Android Gradle Plugin (AGP) | 9.5.0 | Declared in `gradle/libs.versions.toml` |
| Kotlin | 2.2.10 | Declared in `gradle/libs.versions.toml` |
| KSP (Symbol Processing) | 2.3.12 | Declared in `gradle/libs.versions.toml` |
| Gradle | 9.8.2 | Declared in `gradle/wrapper/gradle-wrapper.properties` |
| Daemon JDK | 25 | Declared in `gradle/gradle-daemon-jvm.properties` |
| Compose BOM | 2026.09.00 | Declared in `gradle/libs.versions.toml` |
| Material 3 | via Compose BOM | Declared in `gradle/libs.versions.toml` |
| Navigation Compose | 2.10.2 | Declared in `gradle/libs.versions.toml` |
| Lifecycle/ViewModel Compose | 2.11.0 | Declared in `gradle/libs.versions.toml` |
| Room Runtime & Compiler | 2.8.5 | Declared in `gradle/libs.versions.toml` |
| SQLCipher Android | 4.20.0 | `net.zetetic:sqlcipher-android` |
| AndroidX SQLite KTX | 2.7.1 | `androidx.sqlite:sqlite-ktx` |
| AndroidX Biometric | 1.4.0-alpha07 | Pinned preview exception: fixes legacy fingerprint compatibility with Core 1.19.1; see security report |
| Bouncy Castle | 1.86 | Lightweight PBKDF2-HMAC-SHA256 for API 24–25 |
| Fragment | 1.9.1 | FragmentActivity biometric host |
| DataStore Preferences | 1.3.0 | `androidx.datastore:datastore-preferences` |
| compileSdk / targetSdk | 37 | Declared in `app/build.gradle.kts` |
| minSdk | 24 | Declared in `app/build.gradle.kts` |
| applicationId | `com.gem.keeppeer` | Declared in `app/build.gradle.kts` |

## Project Setup

1. Open the project in Android Studio Rabbit 2 (2026.2.2+).
2. Let Gradle sync complete.
3. Build debug APK: `./gradlew :app:assembleDebug`
4. Run unit tests: `./gradlew :app:testDebugUnitTest`
5. Run lint checks: `./gradlew :app:lintDebug`

## Architecture

Single `app` module with manual DI. Package structure:

| Package | Responsibility |
|---|---|
| `data/local` | Room database, DAOs, entities |
| `data/repo` | Repository abstractions |
| `domain` | Use cases and domain models |
| `ui/lock` | Lock/unlock screen |
| `ui/list` | Password list screen |
| `ui/edit` | Add/edit password screen |
| `ui/settings` | Settings screen |
| `ui/export` | CSV import/export UI |
| `ui/theme` | Material 3 theming |
| `ui/components` | Shared reusable composables |
| `ui/navigation` | Navigation graph and routes |

## Security Posture

- `android:allowBackup="false"` with explicit cloud-backup and device-transfer exclusions (`backup_rules.xml`, `data_extraction_rules.xml`).
- `INTERNET` permission proactively removed via manifest merger (`tools:node="remove"`).
- No broad storage permissions; CSV uses Storage Access Framework (SAF).
- Signing secrets, keystores, environment files, and vault databases excluded from Git.
- See [docs/security.md](docs/security.md) for detailed security documentation.

## Agent Skill Location

The Password Keeper project skill is installed at:
```text
.agents/skills/password-keeper-android/SKILL.md
```
This project-local location is discoverable by Android Studio agents and included in the source snapshot.

## Source Export & Review Workflow

The project uses a structured, verifiable source-code review workflow:

1. **Update Structured Report**: Before exporting, update or create the phase report file at `docs/reports/phase-<Phase>-report.json` with actual check results (PASS/FAIL/NOT RUN), execution timestamps, added/modified/deleted paths, and implementation notes.
2. **Execute Exporter**:
   ```powershell
   .\tools\export-source-code.ps1 -Phase 2 -Outcome incomplete
   ```
   The exporter discovers all reviewable source, verifies project metadata, applies heuristic secret redaction to the review copy without modifying disk files, validates boundaries, and atomically replaces `source-code.txt` using `System.IO.File.Replace`.
3. **Verify Exporter Functionality**:
   An automated fixture test is available:
   ```powershell
   .\tools\test-export-source-code.ps1
   ```
   This creates an isolated synthetic temporary project with seven checks: first write, repeated atomic replacement/deletion reflection, typed/untyped secret redaction, unreadable-source I/O failure, report outcome mismatch, locked-destination replacement failure, and file-type-aware redaction. The last check preserves ordinary Kotlin mappings and synthetic source embedded in scripts while verifying labeled redaction of an included sensitive build-property value.

   Run `.\tools\test-source-export-privacy.ps1` for the separate host-only incident privacy fixture. The exporter prunes reserved preservation/capture/runtime directories, excludes sensitive filename families even with reviewable extensions, and skips links/junctions. Payloads and detailed preservation manifests must remain app-private, outside this repository and export.

## Phase 2: encrypted vault and lock

- First run confirms a strong master password and explains irreversible password/device-key loss. Existing Phase 1 data requires an explicitly confirmed synthetic reset; no silent migration/reset occurs.
- A random DB key is wrapped with PBKDF2-HMAC-SHA256 (600,000 iterations) and AES-GCM, then additionally protected by a distinct Android Keystore key in no-backup storage. Bouncy Castle's vetted KDF supports API 24–25. Native Keystore/SQLCipher/KDF suites have now executed on separate API 24/25 x86_64 emulators: pre-correction suites each completed with 20 passes and one enrollment-dependent skip. Enrollment then exposed Biometric 1.1.0/Core 1.19.1 incompatibility; the pinned Biometric preview correction and its separate reruns are recorded in the report. Artifact-specific results and remaining hardware checks are in the phase report.
- Only authenticated sessions can open SQLCipher or obtain a usable repository. Lock revokes old handles, cancels vault work, closes Room, clears decrypted ViewModels/navigation and resets reveal state.
- Security settings provide Lock now, immediate/1-minute/5-minute background timeouts, password change, and optional CryptoObject-bound biometric unlock with password fallback. CSV export-before-change is a nudge until Phase 3.
- Screens use FLAG_SECURE. Copies (including Compose selection/cut) are sensitive, clear after about 30 seconds and on lock/background where supported, and preserve newer unrelated clips.
- A durable, namespace-bound STARTED/COMPLETE marker guards first creation and is backfilled only after authenticated open of older valid vaults. Inspection runs off main with creation disabled while checking. Existing aliases, markers, envelope siblings or SQLite sidecars block silent fresh setup when required material is missing; errors fail closed. The existing creationPending envelope still resumes with its original key. Full data/key removal can erase these signals; this is prevention, not recovery.
- Instrumentation source routes fixture files and aliases to isolated synthetic contexts. Historical test passes do not establish normal-vault preservation.

See [security.md](docs/security.md), [phase status](docs/phase-status.md), and the [device checklist](docs/phase-2-device-checks.md) for limitations and actual verification. The [biometric investigation](docs/phase-2-biometric-diagnosis.md) records the reproduced post-scan failure, focused AAD ordering fix, test evidence and installed APK identity. Enrollment now uses its own current-password confirmation dialog; it does not use the password-change fields. Phase 2 remains incomplete.

## Phase 1: preserved CRUD behavior

- Add a synthetic entry from the list, open its details, edit it, or delete it after confirmation. Cancel/back discards the draft without writing.
- Title is required. Passwords and notes retain exact input, including spaces, Unicode, and line breaks. Username, URL, category, and notes are optional. Favorite/category values are editable and stored; their list filters arrive in Phase 4.
- Search is a case-insensitive literal substring match across title, username, URL, and category. Kotlin matches Unicode case variants on a Room metadata-only projection because SQLite LIKE/NOCASE only folds ASCII. Percent signs, underscores, and backslashes are literal. Passwords and notes are neither selected for list rows nor searched. This scans metadata in memory, appropriate for the small offline prototype.
- Creation sets both timestamps to the same epoch millisecond. Updates preserve stored creation time and strictly advance modification time, even within one millisecond or after a clock rollback.
- Each navigation destination owns a ViewModel and exposes immutable StateFlow state collected with the lifecycle. Navigation contains numeric IDs only.
- Drafts survive rotation in ViewModel memory. Password reveal resets on rotation/navigation. After process death, saved entries reload by ID and unsaved sensitive drafts are lost. No draft, password, notes, or search text is written to saved state/preferences.
- Manual DI uses KeeppeerApplication and AppContainer. DatabaseProvider is the replaceable database-opening boundary; no destructive migration fallback is configured.
- Phase 2 replaces the production provider with SQLCipher. Old plaintext data has a separate confirmed synthetic-reset workflow. Release remains disabled until the required vault/session checks are complete.

Verification results are in [docs/phase-status.md](docs/phase-status.md) and [docs/reports/phase-1-report.json](docs/reports/phase-1-report.json). Device steps are in [docs/phase-1-device-checks.md](docs/phase-1-device-checks.md).

Historical device-check procedure (paused during the current vault incident; do not execute this procedure as part of preservation/history review):

```powershell
.\gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell pm list instrumentation
adb shell am instrument -w -r com.gem.keeppeer.test/com.gem.keeppeer.SyntheticTestRunner
```

Confirm host/installed APK identities and runner target before execution. Do not uninstall, clear app data or use a harness that removes the normal app. See the Phase 2 checklist/report for source-specific counts and remaining hardware/API checks.

Room/repository tests run against real in-memory SQLite under Robolectric 4.17 / API 28 (verified against the [official compatibility table](https://robolectric.org/compatibility_table/) and [JDK setup guidance](https://robolectric.org/getting-started/)); device instrumentation covers in-memory CRUD, persistence after database reopen, rotation, and cancel. Device tests require a connected emulator/device. The lifecycle-runtime-compose dependency stays pinned to Lifecycle 2.11.0, verified using the IDE version lookup and [official lifecycle guidance](https://developer.android.com/topic/libraries/architecture/lifecycle).

## Phase Status

See [docs/phase-status.md](docs/phase-status.md) for current progress and verification logs.
