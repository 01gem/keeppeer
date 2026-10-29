# Phase 2 device verification

## Read-only vault-context reconciliation — 2026-10-30

**Phase 2 remains incomplete; release disabled. The discrepancy concerns instance/storage context; no data-loss, recovery or file-preservation conclusion is established.** The user reports that the existing vault unlocks and entries are visible on the same phone, with no uninstall, clear-data, device or Android-profile switch (2026-10-30; exact time unknown; not independently observed).

Read-only metadata matches the earlier Samsung serial, Android user 0, package UID 10358 and installed app SHA-256. During this inspection the launcher was foreground and no main Keeppeer PID existed; a cached companion test-package process is not evidence of active instrumentation. Conventional owner-user vault paths returned per-path `No such file` statuses under the verified app UID, with readable parent directories. Three UUID test-cache database/envelope pairs remain as unselected candidates. Their existence does not identify the user's accessible session.

**Correction:** the earlier `class=KeeppeerApplication` process/package metadata was a declared application name, not proof of the actual runtime Application class or VaultSession context. The active session's resolved database/envelope paths and alias namespace are still unknown. The supplied user confirmation supersedes the earlier app-data-history question.

No app start/stop/restart, instrumentation, build/install, cache/data/key/enrollment change, migration or payload/capture collection occurred in this investigation. The smallest next observation is passive package/user/PID metadata while the user already has the accessible vault visible, then vault-file descriptor targets if readable without changing that process. No diagnostic deployment or restart is permitted here. Prior artifact-scoped results and other manual gaps remain unchanged. See [the metadata-only investigation](phase-2-vault-context-check.md).

## Historical Samsung continuation — 2026-10-29/30

**Phase 2 remains incomplete; release disabled. Normal persistent-vault state is unresolved.** Subsequent device work used Samsung SM-A125F / Android 12 / API 31 / `RR8R402RR7T` as requested; the emulator was stopped. App SHA-256 `c103aac417587198697f90f37b2195e0732adb1f5a7380d7c3bb514ef841c169` was installed in place. No uninstall, clear-data, normal-vault setup/reset, enrollment change or automatic recovery was performed.

Six affected isolated checks passed **6/6, 0 failed/skipped**, 214.107 s, completed 2026-10-29 11:49:17 PM PHT (test `831d329e8efd363109f157ff60d53766aa195bf8bdb7f3df4ea0c7018e461054`). They cover native prompt cancellation/wrapper preservation, password/enrollment rejection, AAD compatibility, touch Cut, clipboard ownership/sensitive marking and 30,063 ms expiry, plus synthetic capture collection. OEM Back/Cut drivers use grounded OS input and verify actual effects. New capture-only enrollment precondition avoids a false failure without enrolled hardware; final test `fc7f89c1d75d9a5b45e6231bcae9d221a2965e0821b205d3061a0c878af21345` passed its affected capture target **1/1**, 52.468 s at 11:55:50 PM PHT. No complete current 23-test run is claimed. Latest host assembly, 15 JVM tests and lint (0 errors/14 warnings) passed.

Native recording samples were decoded and visually checked against a readable generic Display Settings control. Final capture: one control frame, 26 revealed synthetic-draft frames and one confirmation-dialog frame; all app content is black in protected samples, with OS controls/bars visible. These static/short timelines are scoped samples, despite the recorder's requested five seconds. Protected PNGs were empty, so screenshot protection remains **INCONCLUSIVE**. Only synthetic app screens and generic settings were captured; no normal entry screens or other apps' recents were collected.

**Prior package-path observation (interpretation corrected on 2026-10-30):** an ordinary package launch displayed first-run labels and conventional-path checks reported missing files. Declared ApplicationInfo/data-directory metadata was recorded; the actual runtime Application class and session context were not verified. No pre-update normal-file baseline exists. Subsequent user-reported vault accessibility without uninstall/clear-data/device/profile switch supersedes the earlier pending history question. Current findings are an unresolved identity/storage-context discrepancy, not established data loss, recovery or preservation of a particular file set. See the read-only reconciliation above.

Enrollment-change invalidation still needs a suitable disposable device/provider; the API 25 old key remained usable after enrollment changed. Physical keyboard, actual screenshot-button policy, Samsung recents and wider unsupported/no-enrollment/OEM checks remain open. Earlier API 24/25 and accepted original API 31 results below retain their exact artifacts; user-reported fallback/intact entries dated 2026-10-29 remain historical evidence, with no invented exact time. See [structured results](reports/phase-2-report.json) for commands, failed histories, source scope and the storage discrepancy.

## Earlier emulator continuation evidence — 2026-10-29

**Historical emulator artifacts.** App SHA-256 `0752f431044628cc0ee78ee30c90fd67c658723f760b60a76bfa511fa4fda240`; test `80b973bbc4addd0662f322922b3aa7301a12b4193f64c1b409f9e69d7b6dbd30`; baseline `382fdcaa7f818869820389ed78a195db05320d2e` plus the changes described here. The physical API 31 accepted 21/21 result below retains its original artifacts and is historical acceptance, not a pass for this changed app.

Demonstrated fixes: pin Biometric 1.4.0-alpha07 for the obsolete Core fingerprint bridge; use an AppCompat host theme for the real legacy prompt; secure app-owned DialogFragment windows; retain Compose 1.12's AndroidClipboard native metadata bridge while routing selection/cut writes through SensitiveClipboard. The actual touch-menu crash is fixed. Crypto/session gates, post-authentication AAD, wrapper format, 600,000-iteration KDF, minSdk 24 and offline/backup/release restrictions are preserved.

Current enrolled API 25 x86_64 full native suite: **22 started/passed, 0 failed/skipped**, 116.245 s, completed 10:49:04 PM PHT. Includes actual legacy prompt/secure flags/cancel, touch Cut/sensitive ownership, native Keystore/SQLCipher, portable KDF, preservation/session/UI and clipboard expiry (30,102 ms). Current API 24 x86_64 full suite completed **21 passed, 0 failed, 1 no-enrollment skip**, 118.243 s at 11:00:25 PM PHT; touch Cut passed, clipboard expiry was 30,052 ms. Earlier 20/0/1 artifact results remain history. Full commands, identities and failed/targeted histories are in [the report](reports/phase-2-report.json).

Actual disposable API 25 manual checks passed: real 1-minute below/expired returns; 5-minute below return and 300-second stopped-process expiry (timer/foreground ordering limit recorded); process death/Back/cold lock; screen lock clears unsaved edit/reveal; emulated native CryptoObject enrollment/decrypt; rejected scan/retry, lockout/password fallback; prompt rotation/Home/screen-lock cancellation; disable/re-enable preserving the saved synthetic entry. Controlled Android AppOps read denial retained the owned clip on background; restored access/foreground cleared it, and a newer clip copied in Settings survived the same retry. This is imposed OS denial, not a natural OEM-policy observation. No physical keyboard result is claimed.

Enrollment-change invalidation remains **NOT VERIFIED**: API 25 fingerprint count changed 1→2, but the old authenticated key still decrypted. Missing-key tests are separate. API 25 recents showed blank protected content; screenshot tools produced empty files (inconclusive), and native screenrecord was unavailable. Modern emulator capture/invalidation checks and physical/OEM coverage remain pending. The normal phone's vault, keys, enrollment, wrapper and APK were preserved. User-reported password fallback/intact entries dated 2026-10-29 remain recorded with no invented exact time or independent-observation claim.

The sections below preserve earlier artifact-specific evidence; their then-pending rows are superseded only by explicit current results above and in the structured report.

## Remaining checks continuation — 2026-10-29

**User-reported PASS, 2026-10-29:** after enabling biometrics, master-password unlock works and existing entries are intact. Exact observation time was not supplied. The agent did not independently observe the password or entry contents. This closes the earlier pending user-confirmation item; it does not establish the remaining hardware/lifecycle matrix.

The previously completed 21/21 API 31 suite and its source/app/test identity are preserved without a redundant rerun. New API 24/25 environment and manual-check results are recorded in `remainingChecksContinuation` in [the report](reports/phase-2-report.json); the older inventory and pending rows below are historical until superseded by that record. The earlier native/manual results in the table used app `58c3e0827ad6339f4fc2e16f2c1c167cf466727d6d2b4b2cae94eb55083bda06`. Enrolled API 25 testing subsequently demonstrated Biometric 1.1.0/Core 1.19.1 legacy-bridge incompatibility; the pinned Biometric 1.4.0-alpha07 correction uses new app `bf4fd8beb7b26113c61acb1a2c17028804a7a528355da773fb0c160da4f3743d` and test `e20879ecc5fa054fc60a0946c4af5d508be14b814ecc619bdf9a5a0addf45b65`. Its native-capability/dialog target passed 1/1, 0 skips on the same enrolled AVD at 06:35:12 PM PHT. Post-fix full/native/manual results are separate; no old result is relabeled for the new artifact.

| Continuation check | Actual result / limit |
|---|---|
| API 24 environment/native suite | EXECUTED on Android 7.0/x86_64 Google APIs, Nexus 5X disposable AVD `Keeppeer_Phase2_API24_20261029`. Full suite 20 pass / 0 fail / 1 no-enrollment assumption skip, 104.840 s, completed 05:11:22 PM PHT; test `8eadbfc4844e50169b84465e0bb145fcede9bece3c150128c0851498c42b5da4`. |
| API 25 environment/native suite | EXECUTED on Android 7.1.1/x86_64 Google APIs, separate AVD `Keeppeer_Phase2_API25_20261029`. Final full suite 20 pass / 0 fail / 1 no-enrollment assumption skip, 104.452 s, completed 05:49:37 PM PHT; test `b5267badc86ea5f39d19de355bfc3fbca4f73366cd00b7ce9d3b27ccfc784bf4`. |
| Native coverage on both APIs | Actual Android Keystore and SQLCipher setup, correct/wrong password/key, plaintext rejection, close/reopen, failed-write/malformed/missing-key preservation, active-file/schema guards, original-key initialization retry, same-key password change, stale reads/writes, portable PBKDF2 known vector/Unicode/600,000 iterations and main-loop progress passed. No platform PBKDF2-SHA256 assumed below API 26. |
| Harness corrections/history | Android 7 ClipboardManager requests must run on a Looper thread; only synthetic fixtures changed. API 25 setup coordinate/IME driver left rejected fields uncleared; validation now uses semantic input/button actions and raw InputText assertions. Manual normal-app touch setup succeeded. All failed/diagnostic/targeted/full runs retain separate hashes/counts in the report; no timeout or auth check was weakened. |
| 1-minute real absence, API 25 | PASS: after Home, unlocked settings retained at 45,670 ms; locked UI with empty master-password field at 60,430 ms. Actual wall-clock waits; exact threshold is separately exercised by deterministic tests. |
| 5-minute below-boundary real absence, API 25 | PASS: 5-minute radio checked; unlocked settings retained after 270,733 ms Home absence. At/after-expiry suspended-process check is recorded separately when complete. |

Installed APK identity on Android 7 uses native device `sha1sum` equality with host SHA-1 plus the host SHA-256 above/in the report; native SHA-256 utility was unavailable. This SHA-1 artifact identity check does not change the app's SHA-256 password KDF. A test-artifact pass is claimed only for its recorded source state. The new test artifact has not been substituted for the accepted API 31 artifact.

## Prior verification continuation — 2026-10-29

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
