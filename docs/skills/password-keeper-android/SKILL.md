---
name: password-keeper-android
description: Implement and review phases of the Password Keeper Android project using its build plan, preserving its offline security requirements and producing source-code.txt review snapshots with Philippine-time timestamps after changes. Use for this project's implementation and review fixes, not unrelated Android projects.
---

# Password Keeper Android workflow

Use this skill with the user's Password Keeper phase prompt. It carries the project rules and the review-file handoff across agent conversations; the phase prompt supplies the detailed implementation checklist.

## Choose the current work

Read applicable repository instructions, the supplied phase prompt, README, `docs/phase-status.md`, and `docs/security.md` when present. Inspect the actual project before treating a phase as complete. Preserve existing package identifiers, architecture decisions, and unrelated changes.

Implement the requested phase and its necessary prerequisite fixes. Record deviations and blockers. Stop after its handoff; do not start the next phase automatically. Subsequent review fixes remain part of the current phase and require a new review snapshot.

If the user asks for analysis only, report findings against the supplied code without changing the project. If working from an uploaded `source-code.txt`, state its phase/timestamp and distinguish source inspection from checks actually executed. Do not invent access to the original project or a running device.

## Project contract

- Use Kotlin, Jetpack Compose, Material 3, Coroutines/Flow, Navigation Compose, and one app module. Keep minSdk 24. Preserve the existing DI approach; use manual DI for a new project unless the user chooses otherwise.
- Verify compatible stable Android/Gradle/Kotlin/library versions against official documentation when changing dependencies. Pin versions. Preserve the plan's Room/SQLCipher integration approach unless a justified change is documented.
- V1 includes entry CRUD/search, master-password lock, optional biometric unlock, auto-lock, generator, favorites/categories, and CSV import/export. Exclude cloud sync, sharing, folder trees, attachments, browser autofill, and analytics.
- Keep `INTERNET` and broad storage permissions out of every shipped merged manifest. Use SAF for user-selected CSV files. Disable backup with `android:allowBackup="false"` and applicable cloud-backup/device-transfer exclusions.
- Use synthetic credentials during development and tests. Do not log secrets or place entry passwords, master passwords, keys, or decrypted notes/drafts in saved state, navigation arguments, ordinary preferences, or persisted debug output.

## Phase boundaries

| Phase | Outcome |
| --- | --- |
| 0 | Runnable project foundation, compatible dependencies, manifest/Git hygiene, phase documentation, reusable source-export helper, and the first `source-code.txt`. |
| 1 | Working Room CRUD/search and Compose state/navigation using a clearly marked plaintext development database, with synthetic data only and an insecure-prototype release restriction. |
| 2 | Real encrypted vault, password setup/unlock/change, centralized authenticated session, auto-lock, screenshot/clipboard protection, and optional biometric unlock with master-password fallback. |
| 3 | Tested RFC 4180 CSV import/export via SAF, plaintext warning, validation, transactional import, duplicate policy, and picker/lock coordination. |
| 4 | Secure password generator, favorites/category filters, accessible light/dark UI, and fully working settings. |
| 5 | Whole-app verification, documented device checks, minified release preparation, safe signing configuration, and an honest release-readiness report. |

Use the phase prompt for its full schema, screen requirements, tests, and acceptance criteria. Do not treat this table as permission to skip those details. Do not publish, upload builds, or distribute the app as part of these phases.

## Preserve the important security boundaries

- Phase 1's plaintext database is a prototype. When adding SQLCipher, use an explicit verified migration or a clearly confirmed synthetic-data reset. Changing the Room helper factory does not encrypt an existing database. Never silently wipe a vault or use destructive migration as an error recovery path.
- Keep the random database key separate from the master-password-derived wrapping key. Use vetted KDF/AEAD implementations and versioned authenticated wrapping metadata. The chosen KDF must work on API 24–25 as well as newer devices. Verify Android algorithm availability instead of assuming newer APIs exist on minSdk.
- Preserve the plan's Keystore-bound storage protection and separate authentication-bound biometric wrapping. Do not introduce deprecated EncryptedSharedPreferences into a new implementation without documenting the decision. Never store a raw database key in preferences or source.
- The vault opens only after successful cryptographic unlock. BiometricPrompt must authorize the actual Keystore cryptographic operation; a successful callback alone is insufficient. Enrollment/key invalidation, cancellation, and unsupported hardware must preserve password fallback and existing data.
- Start cold/process-death launches locked. Support immediate, 1-minute, and 5-minute background timeouts using elapsed time; rotation is not backgrounding. Locking closes database access, cancels vault work, clears decrypted UI state, and resets password reveal state. File pickers must not bypass the lock.
- Apply `FLAG_SECURE` to app screens/windows. Mask passwords by default and omit them from list rows. Clear app-owned sensitive clipboard contents after about 30 seconds and on background/lock where supported, without deleting newer unrelated clips.
- Keep plaintext CSV warnings, exact field values, quoted multiline notes, and a deliberate duplicate policy. Never export vault data as part of the source review file. Warn honestly about password-loss and device-bound recovery limits; do not overstate protection.

## Required source-code.txt handoff

After every implemented phase and every subsequent project fix, deliver a fresh `<project-root>/source-code.txt` for the user to upload to the originating review chat. This requirement also applies when a build or device check is blocked: export the actual current work and label its status honestly.

### Create once, refresh after changes

In Phase 0, create a reusable project-local exporter, such as `tools/export-source-code.ps1` on Windows or an equivalent suitable for the project. Document its invocation in README. Include the exporter itself in the review file. Later phases must reuse and maintain it; create/restore it if missing.

Finish source changes, documentation, tests, and fixes before exporting. Generate a full current snapshot, replacing the previous one rather than appending changes. Discover new/untracked source and reflect deleted files. Regenerate again if any included file changes afterward. Add the generated review file and its temporary copies to `.gitignore`.

### Header and Philippine timestamp

Include project name, requested phase, phase outcome (`complete`, `incomplete`, or `blocked`), applicationId, SDK/toolchain/dependency versions, and Git revision/status when available.

Label the generation timestamp `Generated at (Philippine time)`. Convert the actual generation instant to Asia/Manila, UTC+08:00, and display a date, 12-hour time with seconds, explicit AM/PM, and the timezone:

```text
Generated at (Philippine time): 2026-10-29 07:15:30 PM PHT (UTC+08:00)
```

The value above illustrates the format; never reuse it as the real timestamp. Refresh the timestamp on every export. Do not relabel the computer's local time as Philippine time. One Windows implementation is:

```powershell
$reviewTimestamp = [DateTimeOffset]::UtcNow.ToOffset([TimeSpan]::FromHours(8))
$reviewTimestamp.ToString('yyyy-MM-dd hh:mm:ss tt', [Globalization.CultureInfo]::InvariantCulture) + ' PHT (UTC+08:00)'
```

### Include complete reviewable text

Include a sorted relative-path inventory and the complete current contents of project-owned source and reviewable configuration:

- Kotlin/Java source across all source sets, manifests, XML/Compose resources, and tests with synthetic fixtures.
- Gradle settings/build scripts/properties, version catalog, wrapper text scripts/properties, ProGuard/R8 rules, and Room schemas.
- README, project/phase/security/release documentation, `.gitignore`, applicable project instructions, this skill, and the export helper.

Use explicit boundaries and preserve source line breaks:

```text
===== BEGIN FILE: app/src/main/AndroidManifest.xml =====
[complete current file contents belong here]
===== END FILE: app/src/main/AndroidManifest.xml =====
```

The bracketed line illustrates where real contents go; it must not appear as a substitute in the generated snapshot. Include full files, not snippets or diffs. Do not silently truncate large files or omit unreadable source files.

Include a short phase report: implementation summary; added/changed/deleted paths; exact checks attempted with pass/fail/not-run results and concise sanitized failure details; known issues; and outstanding device/manual checks. Do not paste full noisy build logs or claim tests passed without execution.

### Exclude private and generated data

Exclude the snapshot itself and its temporary/previous copies; `.git`, `.gradle`, `.idea`, caches, build/generated outputs; binaries, APKs, AABs, JARs and archives; `local.properties`, signing files/keystores, `.env` and credential files; runtime vault databases/key envelopes; and plaintext CSV exports. List exclusion categories with reasons. Do not exclude all hidden or ignored paths blindly: project instructions and new source may live there.

Preserve actual security/crypto implementation and synthetic tests. If included text contains a real secret, redact only that value in the review copy and label the redaction with file/line context. Do not disclose the value or alter the underlying source merely to redact the snapshot. Flag any resulting review limits.

### Validate and deliver

Write through a temporary file and replace the target only after validating readable nonempty UTF-8 output, inventory/content coverage, matching file boundaries, current additions/deletions, exclusions/redactions, and the fresh Philippine timestamp format. Avoid including the temporary output while discovering files.

If export fails, preserve the previous usable file, explicitly mark its status as stale in the handoff, and explain the blocker. Never present a stale or partial snapshot as current.

In the final reply, give the exact absolute path, generation timestamp, and file size of `source-code.txt`, plus a concise phase/check summary. Tell the user to upload the file to the originating review chat for analysis. Do not upload automatically or paste the entire file into the reply. Stop after the requested phase. The review handoff requires either a current snapshot or an explicit export-failure report.
