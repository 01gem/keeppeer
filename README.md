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

## Tech Stack

| Component | Version |
|---|---|
| Kotlin | 2.2.10 |
| AGP | 9.5.0 |
| Gradle | 9.8.2 |
| JDK (daemon) | 25 |
| Compose BOM | 2026.09.00 |
| Material 3 | via BOM |
| Navigation Compose | 2.10.2 |
| Lifecycle/ViewModel | 2.11.0 |
| Room | 2.8.5 |
| SQLCipher Android | 4.20.0 |
| AndroidX SQLite | 2.7.1 |
| AndroidX Biometric | 1.1.0 |
| DataStore Preferences | 1.3.0 |
| compileSdk / targetSdk | 37 |
| minSdk | 24 |
| applicationId | `com.gem.keeppeer` |

## Project Setup

1. Open the project in Android Studio (Meerkat or newer recommended).
2. Let Gradle sync complete.
3. Build: `./gradlew :app:assembleDebug`
4. Run on a device/emulator (API 24+).

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

- `android:allowBackup="false"` with explicit cloud-backup and device-transfer exclusions.
- `INTERNET` permission removed via manifest merger (`tools:node="remove"`).
- No broad storage permissions; CSV uses SAF.
- Signing secrets, keystores, and vault databases excluded from Git.
- See [docs/security.md](docs/security.md) for detailed security documentation.

## Source Export

To generate the `source-code.txt` review snapshot:

```powershell
.\tools\export-source-code.ps1 -Phase 0 -Outcome complete
```

The script creates `source-code.txt` in the project root with a Philippine-time timestamp. This file is excluded from Git.

## Phase Status

See [docs/phase-status.md](docs/phase-status.md) for current progress.
