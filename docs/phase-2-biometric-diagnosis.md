# Phase 2 biometric enrollment investigation

Phase 2 remains incomplete. Release remains disabled; Phase 3 has not started.

## Verification continuation — 2026-10-29

The enrollment ordering fix is preserved and was not reinvestigated. On the current installed/host app `58c3e0827ad6339f4fc2e16f2c1c167cf466727d6d2b4b2cae94eb55083bda06`, SM-A125F/Android 12/API 31/arm64-v8a, normal-phone checks at approximately 01:10–01:15 PM PHT confirmed cold launch locked and two actual fingerprint-authorized decrypt operations:

```text
PROMPT_SUCCEEDED
CIPHER_BINDING
SESSION_GENERATION
WRAPPER_READ
DECRYPT_AAD
DECRYPT_FINAL
DATABASE_OPEN
COMPLETE
```

The existing entry list became accessible, including a retry after Security -> Lock now. No entry contents were opened/exported and the working wrapper was not disabled or re-enrolled. An actual Samsung prompt's Use master password action produced PROMPT_ERROR code=13, the cancellation message and locked password-fallback UI; successful normal-vault password unlock/original-entry-content confirmation remains pending. Home/return from the unlocked app returned locked. A retry authenticated before Home, so no prompt-background cancellation is claimed.

The restored original 17-test suite passed in 442.505 seconds. The expanded-suite artifacts and separate full/targeted/interrupted results are in [the current report](reports/phase-2-report.json). Synthetic software AES-GCM regressions remain distinct from the real Keystore decrypt evidence above. API 24/25, failed scans/lockout/invalidation/unsupported hardware and remaining lifecycle/OEM checks are still pending. The unidentified old APK's preparation error is historical uncertainty, not a present acceptance requirement.

## Historical symptom, binary identity and reproduced failure

The user reported an initial "Password verification or biometric setup failed" message, followed by a separate attempt after explicitly entering the current master password: a fingerprint prompt appeared, a scan occurred, and the app reported cancellation/unavailability. The initial preparation error remains undiagnosed. It must not be used to dismiss the later scan attempt as empty input.

Connected device: Samsung SM-A125F, Android 12/API 31. The pre-update package com.gem.keeppeer reported versionCode 1/versionName 1.0 and firstInstallTime/lastUpdateTime 2026-10-29 08:14:00 (device-reported time). Its APK SHA-256 was 4d947dc25ee96516a505865cbfebd7641ed9ed095ce4b5bd97d43d8c8b8106d1. The pre-edit local APK was 3908ae85819dea55f16f26a406f9a25d1e39c721a574085efb8d6d68eca860c0. Those artifacts differ, and the old package has no embedded Git identity. Its exact source revision is unknown. Work began at clean Git revision 76487e762bea52aa03aefffff39e121991065c17.

The first diagnostic APK was installed using adb install -r, with installed/host SHA-256 both 98344916efc4c22202f8d9fe593f4246303bfe98228962277d8d318923b5c141. Its lastUpdateTime became 09:27:09 while firstInstallTime remained 08:14:00. A user-operated enrollment attempt on this known binary reproduced the failure with this restricted trace:

```text
PASSWORD_INPUT
PASSWORD_VERIFY
AVAILABILITY code=0
WRAPPER_REMOVE
KEY_GENERATE
ENCRYPT_INIT
ENCRYPT_AAD
PROMPT_LAUNCH
PROMPT_SUCCEEDED
CIPHER_BINDING
APPROVAL_CHECK
SESSION_CHECK
FOREGROUND_CHECK
ENCRYPT_FINAL
ENCRYPT_FINAL exception=IllegalBlockSizeException
```

This establishes that onAuthenticationSucceeded was delivered, the returned Cipher matched, and approval/current-session/foreground checks passed. Failure occurred at authenticated doFinal before WRAPPER_WRITE. This attempt was not a prompt cancellation or a wrong-password rejection. No Activity stop/destroy or session-generation change intervened in this trace. This does not retrospectively identify the initial preparation failure or prove the exact cause in the older installed APK.

## Bounded changes

The original Boolean result combined prompt cancellation/errors, Cipher mismatch, session rejection, authenticated encryption and wrapper-write failure. Structured results and messages now distinguish those stages, cancellation, wrong password, unavailable/invalidated keys, Keystore and I/O failures. All three biometric callbacks have dedicated records; rejected scans remain retryable in the native prompt. Per-prompt completion isolates stale callbacks. Approval binds the exact Cipher and current session generation. Unlock success is delivered only after authenticated decrypt, actual database open and current-session publication.

Enrollment has its own masked current-password confirmation dialog, independent of all password-change fields. Blank input is rejected locally and at the session boundary before expensive work. Owned password CharArrays are wiped; login input is not retained. Dialog state is memory-only and cleared on submission/cancellation.

The reproduced finalization failure implicates the pre-authentication AAD operation. The focused cryptographic change moves the same domain-separated AAD from both Cipher preparation methods into save/decrypt, exactly once immediately before doFinal, after CryptoObject authentication. The actual key remains per-operation BIOMETRIC_STRONG with exact Cipher binding. The alias, key configuration, 61-byte v1 wrapper, 12-byte nonce and 128-bit tag remain unchanged. AAD is not removed. Finalization continues to fail closed without actual cryptographic authorization.

A new real AES-GCM synthetic format regression checks single AAD application, compatibility with the original v1 AAD/wrapper, and tamper rejection. It does not simulate biometric authorization. Controlled real-phone enrollment after the ordering change succeeded on the hash-verified fixed binary: PROMPT_SUCCEEDED and Cipher/session checks passed, then ENCRYPT_AAD, ENCRYPT_FINAL, WRAPPER_WRITE and COMPLETE. This before/after result supports an authentication-per-operation AAD-ordering compatibility failure on this phone. The exact internal vendor error code is not exposed through the public API 31 interface; no native message parsing or hidden API access was introduced. Real biometric unlock and user confirmation of fallback/original entries are recorded separately in the structured report.

Debug-only KeeppeerBiometric output contains only fixed stage identifiers, exception class names and allowlisted numeric platform codes. No exception message, stack/Throwable dump, native error text, password, entry data, key, envelope or credential-bearing object is logged. Activity start/stop/destroy, backgrounding, generation changes and cancellation are separate events. Public numeric Keystore codes are collected only on API 33+, where available; API 31 uses exception types and biometric callback codes without reflection/message parsing.

No broad lifecycle exemption, validity-duration change, weak-biometric fallback, callback-only unlock, uninstall, clear-data, vault reset, fingerprint-enrollment change or unrelated key regeneration occurs. Failed enrollment may leave only its unused separate biometric key; retry/disable clears that alias/wrapper. The master envelope, device storage key and entries are not cleanup targets.

## Historical checks before this verification continuation

Before the AAD ordering fix, host debug app/test assembly, 15 JVM tests and lint passed (0 errors, 14 warnings). A complete isolated SyntheticTestRunner run passed all 16 tests on SM-A125F/API 31 in 397.114 seconds, completion observed 2026-10-29 09:37:05 AM PHT. Both new blank/wrong-password/unapproved-Cipher and password-dialog tests passed, preserving DB key and password access. Existing native encryption/schema guards, CRUD, rotation/lifecycle, Keystore rollback, password change/revocation, timeout, synthetic-reset, clipboard and Unicode platform/portable KDF checks passed.

After the AAD ordering change, host assembly, all 15 JVM tests and lint passed again. Explicit app/test replacement succeeded, and installed/local APK SHA-256 both equal 3554c74680f7aeff0ca0edd2b67793a09f63246a99448b598346676303b3d1e1 (reconfirmed after reconnect on 2026-10-29). The final-source targeted BiometricEnrollmentTest run passed all 3 tests in 84.001 seconds, including the real AES-GCM AAD/v1 compatibility/tamper regression. The earlier full 16-test pass belongs to the pre-AAD-fix diagnostic source; a final full 17-test run is not claimed.

The controlled enrollment result is recorded in reports/phase-2-report.json. The old preparation failure remains unidentified historical context. API 24/25 native execution and the remaining manual matrix are current acceptance gaps. Successful compilation and synthetic software crypto are not real biometric success; later real decrypt evidence is recorded in the continuation above.

## Safe real-phone procedure

For an already-working wrapper, preserve it and proceed directly to Lock now/unlock/fallback checks. Enrollment instructions below apply only when enrollment itself needs testing on a disposable fixture; do not unnecessarily re-enroll the user's wrapper.

Use adb install -r updates, never uninstall/clear data. Record installed/local APK hash equality. Install the test APK explicitly and use am instrument rather than a harness that might uninstall the target app. SyntheticTestRunner substitutes isolated synthetic files and unique Keystore aliases.

After tests finish, launch the normal app. The user enters their password only on the phone, selects Security / Enable biometrics, and uses the dedicated dialog with the password-change form blank. Capture only KeeppeerBiometric records. Distinguish native cancellation, rejected scans, success callback/Cipher binding, session/foreground rejection, ENCRYPT_AAD/ENCRYPT_FINAL and WRAPPER_WRITE/COMPLETE.

After a real successful enrollment, Lock now and perform real biometric unlock through DECRYPT_AAD, DECRYPT_FINAL, DATABASE_OPEN and COMPLETE. Then verify master-password fallback and the original entries on the phone. Cover cancellation/retry, lockout, invalidation and lifecycle loss without initially changing the user's fingerprint enrollment. Do not read/export production vault files, envelopes, keys, entry contents or passwords for diagnosis.

Primary references: [Android cryptographic biometric flow](https://developer.android.com/identity/sign-in/biometric-auth), [AOSP GCM AAD stream](https://android.googlesource.com/platform/frameworks/base/+/915a0307f2595f136ffc3267cc474139c8bcd56e/keystore/java/android/security/keystore2/AndroidKeyStoreAuthenticatedAESCipherSpi.java), [AOSP operation-update authorization](https://android.googlesource.com/platform/system/security/+/refs/heads/master/keystore2/src/operation.rs), [public Keystore error API](https://developer.android.com/reference/android/security/KeyStoreException).
