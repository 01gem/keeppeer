# Phase 2 device verification

Use only synthetic credentials. Instrumentation uses SyntheticTestRunner, an isolated database/envelope directory and unique test-only Keystore aliases. It must never confirm a reset against the developer's normal vault.

Automated instrumentation covers the actual SQLCipher Room helper, wrong-key/plain-SQLite rejection, reopen, actual Android Keystore device envelope, failed-write rollback, missing key preservation, initialization recovery, master-password change, repository revocation, monotonic timeout boundaries, explicit legacy reset and clipboard replacement. UI tests cover rotation and cancel; further results are recorded in phase-status.md.

Run:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest --console=plain
```

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
