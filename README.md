# Keeppeer — Offline Password Keeper

An offline-only Android password keeper built with Kotlin, Jetpack Compose, and Material 3. All data stays on-device with no network access.

> ⚠️ **Development prototype** — This app is under active development and is **not ready for real credentials**. The upcoming Phase 1 plaintext database stores synthetic data only.

## Features (V1 Roadmap)

- Password entry CRUD with search
- Master password lock with optional biometric unlock
- Auto-lock on background/inactivity
- Secure password generator
- Favorites and categories
- RFC 4180 CSV import/export via SAF

## Tech Stack & Verified Toolchain

| Component | Declared Version | Source |
|---|---|---|
| Android Studio | Meerkat / Ladybug (2024.2+) | IDE environment |
| Android Gradle Plugin (AGP) | 9.5.0 | `gradle/libs.versions.toml` |
| Kotlin | 2.2.10 | `gradle/libs.versions.toml` |
| KSP (Symbol Processing) | 2.3.12 | `gradle/libs.versions.toml` |
| Gradle | 9.8.2 | `gradle/wrapper/gradle-wrapper.properties` |
| Daemon JDK | 25 | `gradle/gradle-daemon-jvm.properties` |
| Compose BOM | 2026.09.00 | `gradle/libs.versions.toml` |
| Material 3 | via Compose BOM | `gradle/libs.versions.toml` |
| Navigation Compose | 2.10.2 | `gradle/libs.versions.toml` |
| Lifecycle/ViewModel Compose | 2.11.0 | `gradle/libs.versions.toml` |
| Room Runtime & Compiler | 2.8.5 | `gradle/libs.versions.toml` |
| SQLCipher Android | 4.20.0 | `net.zetetic:sqlcipher-android` |
| AndroidX SQLite KTX | 2.7.1 | `androidx.sqlite:sqlite-ktx` |
| AndroidX Biometric | 1.1.0 | `androidx.biometric:biometric` |
| DataStore Preferences | 1.3.0 | `androidx.datastore:datastore-preferences` |
| compileSdk / targetSdk | 37 | `app/build.gradle.kts` |
| minSdk | 24 | `app/build.gradle.kts` |
| applicationId | `com.gem.keeppeer` | `app/build.gradle.kts` |

## Project Setup

1. Open the project in Android Studio.
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

1. **Update Structured Report**: Before exporting, update or create the phase report file at `docs/reports/phase-<Phase>-report.json` with actual check results (PASS/FAIL/NOT RUN), added/modified/deleted paths, and implementation notes.
2. **Execute Exporter**:
   ```powershell
   .\tools\export-source-code.ps1 -Phase 0 -Outcome complete
   ```
   The exporter discovers all reviewable source, verifies project metadata, redacts potential secrets in the review snapshot, validates boundaries, and atomically replaces `source-code.txt`.
3. **Verify Exporter Functionality**:
   An automated fixture test is available:
   ```powershell
   .\tools\test-export-source-code.ps1
   ```
   This creates an isolated temporary project, testing discovery, deletions, exclusions, secret redaction, and failure handling.

## Phase Status

See [docs/phase-status.md](docs/phase-status.md) for current progress and verification logs.
