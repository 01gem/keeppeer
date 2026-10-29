# Phase 2 device verification

## Verification continuation — 2026-10-29

**Phase 2 remains incomplete; release disabled.** Device: Samsung SM-A125F, Android 12/API 31, installed app arm64-v8a; device also supports armeabi-v7a/armeabi. Source: `76487e762bea52aa03aefffff39e121991065c17` plus local diagnostic/AAD-fix work and this follow-up's instrumentation/document changes. Current app hash: `58c3e0827ad6339f4fc2e16f2c1c167cf466727d6d2b4b2cae94eb55083bda06`. Full test-APK identities, commands and completion counts are in [the report](reports/phase-2-report.json).

| Check | Actual evidence / limits |
|---|---|
| FINAL complete current suite | PASS: 21 expected/executed/passed, 0 failed/skipped; completed 647.604 s at 01:50:59 PM PHT. Test APK `347d3b00c03f61cb9c5a1b09cdaa50bc14f45e5b792eba67690477d9c95ac369`, installed/host hash equality verified. Final portable/platform KDF 11,693/15,528 ms; main-loop ticks 1,675/max gap 17 ms; keyboard copy expiry 30,015 ms. |
| Original complete current-at-resumption suite | PASS: 17 expected/executed/passed, 0 failed/skipped, 442.505 s, completed 12:52:44 PM PHT. This artifact precedes the follow-up's added assertions/tests. |
| First expanded suite | Completed: 21 executed, 20 passed, 1 test-harness failure, 0 skipped, 643.717 s. Password semantics remain after reveal; corrected test checks actual rendered text. Corrected rotation target passed 1/1 in 34.062 s. Full corrected-suite result is separate in the report. |
| Second expanded suite / setup harness correction | Completed: 21 executed, 20 passed, 1 viewport assertion failed, 0 skipped, 516.971 s. Validation text existed off-screen after keyboard input; `performScrollTo()` now precedes the unchanged visibility assertion. Corrected setup target passed 1/1 in 42.187 s. Viewport-corrected test APK: `e3397300e3602c52906c03488b82bc529d61bd1fcc1b873f36abb3c5433c00d1`; final full-run result is separate in the report. |
| Interrupted fixture-timeout attempt | 21 expected, 11 started, 10 complete results (9 pass/1 fail), 11 without complete results. No final runner summary. A 120-second unlock wait failed before rotation; device remained connected/Awake/screen ON/no keyguard input restriction. Cause unconfirmed. Setup now synchronizes with idle/resumed UI and explicitly checks terminal authentication. Current test APK `347d3b00c03f61cb9c5a1b09cdaa50bc14f45e5b792eba67690477d9c95ac369`; final full result is separate. |
| Native SQLCipher and preservation, API 31 | Correct/wrong key, plain SQLite rejection, close/reopen, persisted synthetic entries, missing/truncated active files, missing schema, pending creation with the same original generated key, failed envelope write and missing storage-key preservation passed. |
| Password/session, API 31 | Password change retained the DB key and entries, rejected old password, cold session locked, stale repository reads and writes rejected. Below-boundary and at-boundary monotonic timeout checks passed for immediate/1-/5-minute choices. This is not a real 1-/5-minute timer/sleep run. |
| Malformed envelopes / missing biometric key | Synthetic length/version/tag errors preserved exact prior file bytes and denied access; restoring the original synthetic envelope retained password access and its entry. A missing biometric key was not regenerated. |
| Legacy reset / setup UI | Cancel preserved disposable Phase 1 data; confirmed reset changed only the isolated fixture; short/mismatched passwords rejected without creating a vault; valid setup succeeded. |
| KDF, API 31 | Known portable SHA-256 PBKDF2 vector and Unicode portable/platform agreement at 600,000 iterations passed. Initial expanded-run metrics: portable 11,718 ms, platform 15,871 ms, 1,692 main-looper ticks, largest gap 19 ms. Final measurements are separate in the report. Heartbeats are not frame-time or physical-input benchmarking. |
| Rotation / drafts | Corrected rendering assertion passed individually: reveal first, masked after recreation, intended memory draft retained, cancel wrote nothing. Lifecycle lock test removed draft UI and revoked its repository. Actual process-death draft behavior remains pending. |
| Clipboard | On-device selection plus Ctrl+C used the central service and sensitive flag; initial observed expiry 30,076 ms; background/foreground clearing endpoint passed. Newer unrelated synthetic clip preservation passed. Touch-menu cut, physical keyboard and observed denied-access/retry remain pending. |
| Normal-phone cold launch and real biometrics | Cold launch locked. Two actual phone-authenticated operations reached Cipher binding, DECRYPT_AAD, DECRYPT_FINAL, DATABASE_OPEN, COMPLETE and existing entry-list access, including retry after Lock now. No entry contents were opened/exported and no wrapper re-enrollment was performed. |
| Actual prompt cancel and fallback UI | Samsung fingerprint prompt -> Use master password -> PROMPT_ERROR code 13 -> cancellation message/locked UI, password field and wrapper option still present. Successful password fallback on the normal vault and original-entry-content confirmation remain pending. |
| Background on normal phone | Unlocked -> Home -> return observed locked, with password and biometric choices. Retry authenticated before Home, so prompt-background cancellation was NOT verified. Real 1-/5-minute absence, suspended timer, screen lock and prompt rotation remain pending. |
| Secure windows / captures | FLAG_SECURE assertion and inherited-dialog source inspected. IDE UI inspector failed to decode its screenshot; text hierarchy used instead. No screenshot/recording/recents/dialog/OEM protection success is inferred from that failure. |
| API 24 / API 25 | NOT RUN: no matching attached device, no AVDs, SDK system-images directory absent. Explicit SDK emulator.exe -list-avds returned none after the bare emulator command was not found on PATH. Keep minSdk 24; no inference from host/API 31. |
| Other real biometric cases | Failed scan, lockout, unsupported/no enrollment, enrollment change/invalidation, disable/re-enable and prompt rotation/background/screen lock remain NOT RUN. Use a disposable device/enrollment where changing enrollment would otherwise invalidate the user's working wrapper. |

Use in-place updates and raw direct runner output for subsequent full runs:

```powershell
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell pm list instrumentation
adb shell am instrument -w -r com.gem.keeppeer.test/com.gem.keeppeer.SyntheticTestRunner
```

Verify host/installed app and test APK SHA-256 values before each source-state run. Count per-test start/pass/failure/skip statuses and require the final runner summary/completion; INSTRUMENTATION_CODE -1 alone does not establish test success. Metric status 2 is not a test result. No connectedDebugAndroidTest/uninstall/pm clear was used in this follow-up. Normal-phone checks used only locked-screen/settings actions and restricted current-PID stage/type/code logs. The old unidentified preparation error is historical, not a current completion requirement.

## Isolation and original checklist

Use only synthetic credentials. Instrumentation uses SyntheticTestRunner, an isolated database/envelope directory and unique test-only Keystore aliases. It must never confirm a reset against the developer's normal vault.

Automated instrumentation covers the actual SQLCipher Room helper, wrong-key/plain-SQLite rejection, reopen, actual Android Keystore device envelope, failed-write rollback, missing key preservation, initialization recovery, master-password change, repository revocation, monotonic timeout boundaries, explicit legacy reset and clipboard replacement. UI tests cover rotation and cancel; further results are recorded in phase-status.md.

## Historical biometric diagnostic follow-up

The diagnostic binary reproduced the post-scan failure: a real success callback and expected Cipher, passing approval/session/foreground checks, then IllegalBlockSizeException at ENCRYPT_FINAL before wrapper write. The ordering fix now applies identical AAD after authentication immediately before doFinal in both directions. The earlier preparation error remains undiagnosed; final-source fingerprint confirmation is recorded in the structured report. The SM-A125F/API 31 installed pre-update APK differs from the local host APK; details and hashes are in [the diagnostic record](phase-2-biometric-diagnosis.md).

Use the dedicated current-password confirmation dialog under Enable biometrics; leave all password-change fields blank. Never send a password to the agent. Capture only KeeppeerBiometric stage/type/allowlisted-code records. Check PROMPT_SUCCEEDED versus PROMPT_FAILED/PROMPT_ERROR, Cipher identity, Activity stop/start/destroy, session generation/cancellation, approval/foreground checks, doFinal and wrapper write independently. Then lock, authenticate a real biometric decrypt, verify original entries on the phone, and verify master-password fallback. Cancelling/retrying must preserve access.

For a device with an existing vault, use explicit in-place replacement and direct instrumentation so no harness uninstalls the target app:

```powershell
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w com.gem.keeppeer.test/com.gem.keeppeer.SyntheticTestRunner
```

Pre-AAD-fix diagnostic run: PASS, all 16 isolated tests on SM-A125F/API 31, 397.114 seconds; completion observed 2026-10-29 09:37:05 AM PHT. Includes the two new enrollment rejection/dialog regressions and existing native encryption, Keystore, lifecycle and Unicode KDF tests. Installed diagnostic APK SHA-256 matches the host build (see investigation). Final-source host assembly/15 JVM tests/lint and all 3 targeted biometric device tests pass (84.001 seconds). Fixed installed/host hash: 3554c74680f7aeff0ca0edd2b67793a09f63246a99448b598346676303b3d1e1. Real user-operated fingerprint enrollment reaches ENCRYPT_AAD, ENCRYPT_FINAL, WRAPPER_WRITE and COMPLETE after actual CryptoObject success/Cipher/session checks. Real decrypt/database open, original entries, password fallback, cancellation/lockout/invalidation and API 24/25/manual checks remain separately pending.

No uninstall, clear-data, vault reset or Android fingerprint enrollment change is authorized for this diagnosis. Tests use SyntheticTestRunner isolation. The direct runner does not claim real biometric success. See reports/phase-2-report.json for executed commands and current results.

Repeat on API 24 and API 25 devices/emulators, then on a current physical device. Host JVM/Robolectric execution does not substitute for Android Keystore/native SQLCipher execution on those API levels.

Manual checklist (record result and device/API; do not assume these passed):

- Fresh install: confirmation mismatch, short password, first setup, password-loss/device-bound warnings, correct/wrong unlock, restart.
- Existing Phase 1 prototype: cancel reset preserves data; deliberate synthetic reset removes only the legacy database. Never test this with real entries.
- Seed an entry; launch locked after force-stop/process death; unlock and reopen by ID. Killing the process loses unsaved drafts. Check Back cannot return to a previous unlocked screen.
- Rotate with a draft: draft survives; reveal resets. Home/return with immediate, 1 minute and 5 minutes; include a long sleep where the background timer cannot execute. Return must enforce elapsed-time expiry.
- Screen off/lock while on details, editing and in biometric prompt; entries and drafts disappear on return. Check manual Lock now from settings and database closure.
- System screenshot, recording, recents thumbnail and Compose dialogs should obscure app content. FLAG_SECURE cannot defend against an external camera, a compromised OS, or all OEM defects.
- Copy a synthetic password: sensitive flag, approximate 30-second expiry, clear on background/lock if Android permits it, retry on foreground. Replace with a different app's clip before expiry: the replacement must remain. Check selection/cut/hardware keyboard copy also uses the central clipboard service.
- Enable biometrics only after current-password verification and a real cryptographic prompt. Test successful unlock, cancel, failed scan, lockout, no enrollment, unavailable hardware, enrollment changes and invalidated/missing biometric keys. Password fallback and entries must survive. Disable removes the wrapper; re-enable uses a fresh key.
- Change password: wrong current password and mismatch preserve the old password. Successful change preserves every entry, rejects old password on the next password unlock, and leaves enabled biometrics working around the same DB key.
- Phase 3 file picker plan: picker launch counts as background. Recheck session/access generation before reading/writing on result; after expiry discard pending decrypted data and require unlock. Do not introduce a global picker exemption.
