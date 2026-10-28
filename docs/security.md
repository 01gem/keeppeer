# Keeppeer — Security Documentation

## Overview

Keeppeer is an offline-only password keeper. All credentials remain on-device with no network access.

## Current Security Posture (Phase 0)

### Network Isolation

- The `INTERNET` permission is explicitly removed from the merged manifest via `tools:node="remove"`.
- No networking libraries are included as dependencies.
- Any transitive dependency that adds `INTERNET` will be blocked by the merger.

### Backup Exclusions

- `android:allowBackup="false"` disables auto-backup globally.
- `backup_rules.xml` (API ≤ 30): Excludes all database, shared preferences, and file domains.
- `data_extraction_rules.xml` (API 31+): Excludes all domains from both cloud backup and device-to-device transfer.

### Git Hygiene

- `.gitignore` excludes: signing keystores (`*.jks`, `*.keystore`), `keystore.properties`, `local.properties`, `.env` files, vault databases (`*.db`, `*.db-shm`, `*.db-wal`), and generated review snapshots.

### Manifest Permissions

- No permissions are declared by the app.
- `INTERNET` is proactively removed from any dependency that may add it.
- No broad storage permissions (`READ_EXTERNAL_STORAGE`, `WRITE_EXTERNAL_STORAGE`, `MANAGE_EXTERNAL_STORAGE`). CSV import/export will use SAF (`ACTION_OPEN_DOCUMENT` / `ACTION_CREATE_DOCUMENT`).

## Planned Security Features

### Phase 2 — Encrypted Vault

- Room database encrypted with SQLCipher Android.
- Random database key wrapped with master-password-derived key (KDF + AEAD).
- KDF compatible with API 24–25 and newer devices.
- Keystore-bound storage protection.
- Separate authentication-bound biometric wrapping.
- Cold-start launches locked; vault opens only after cryptographic unlock.

### Phase 2 — Auto-Lock & Screen Protection

- Configurable background timeouts: immediate, 1 minute, 5 minutes.
- `FLAG_SECURE` on all app windows.
- Passwords masked by default; not shown in list rows.
- Clipboard cleared after ~30 seconds and on background/lock.

### Phase 3 — CSV Security

- Plaintext CSV export warning before proceeding.
- SAF for file selection (no broad storage permissions).
- No vault data in source-code review snapshots.

## Limitations

- **Phase 0–1 use a plaintext database** for development. Do not store real credentials.
- Vault protection is device-bound; factory reset or device loss means data loss.
- No cloud sync or recovery mechanism by design.
