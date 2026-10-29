# Keeppeer — Offline Password Keeper

An offline-only Android password keeper built with Kotlin, Jetpack Compose, and Material 3. All data stays on-device with no network access.

> **Phase 2 security development build** — The user reports the existing vault is accessible without uninstall/clear-data/profile change. Read-only checks leave an unresolved identity/storage-context discrepancy; the earlier declared Application-name metadata did not prove runtime context. No data loss, recovery or normal-file-preservation pass is established. See [the metadata-only investigation](docs/phase-2-vault-context-check.md). Earlier artifact-scoped test results remain unchanged. Phase 2 is incomplete and release packaging disabled; do not distribute.

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

## Phase 2: encrypted vault and lock

- First run confirms a strong master password and explains irreversible password/device-key loss. Existing Phase 1 data requires an explicitly confirmed synthetic reset; no silent migration/reset occurs.
- A random DB key is wrapped with PBKDF2-HMAC-SHA256 (600,000 iterations) and AES-GCM, then additionally protected by a distinct Android Keystore key in no-backup storage. Bouncy Castle's vetted KDF supports API 24–25. Native Keystore/SQLCipher/KDF suites have now executed on separate API 24/25 x86_64 emulators: pre-correction suites each completed with 20 passes and one enrollment-dependent skip. Enrollment then exposed Biometric 1.1.0/Core 1.19.1 incompatibility; the pinned Biometric preview correction and its separate reruns are recorded in the report. Artifact-specific results and remaining hardware checks are in the phase report.
- Only authenticated sessions can open SQLCipher or obtain a usable repository. Lock revokes old handles, cancels vault work, closes Room, clears decrypted ViewModels/navigation and resets reveal state.
- Security settings provide Lock now, immediate/1-minute/5-minute background timeouts, password change, and optional CryptoObject-bound biometric unlock with password fallback. CSV export-before-change is a nudge until Phase 3.
- Screens use FLAG_SECURE. Copies (including Compose selection/cut) are sensitive, clear after about 30 seconds and on lock/background where supported, and preserve newer unrelated clips.
- A first-creation journal permits password-authenticated resumption after interruption. Missing/corrupt active vaults or unavailable Keystore keys fail closed without recreating data.
- Instrumentation uses isolated synthetic file directories and unique test Keystore aliases. It does not reset a developer's normal vault.

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

Additional device checks (preserve an installed vault with in-place replacements and the isolated runner):

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
