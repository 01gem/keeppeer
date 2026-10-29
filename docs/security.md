# Keeppeer security — Phase 2

This is an offline development build. Use synthetic entries until the outstanding device matrix is complete. Release packaging remains disabled. Security here reduces exposure at rest and across normal app lifecycle transitions; it does not protect against a determined attacker controlling an unlocked process, rooted device, malicious keyboard, compromised OS, or external camera.

## Dependencies and documented primitives

Production uses Room 2.8.5 with net.zetetic:sqlcipher-android:4.20.0 (not the retired android-database-sqlcipher artifact), AndroidX SQLite 2.7.1, Biometric 1.1.0, and Bouncy Castle bcprov-jdk18on 1.86 lightweight crypto. Versions are pinned. Fragment 1.9.1 supports AndroidX BiometricPrompt's FragmentActivity host.

Sources checked on 2026-10-29:

- [Zetetic Room 2 integration and explicit native initialization](https://github.com/sqlcipher/sqlcipher-android): SupportOpenHelperFactory and System.loadLibrary("sqlcipher").
- [Zetetic migration/API differences](https://www.zetetic.net/sqlcipher/sqlcipher-for-android-migration/).
- [Android SecretKeyFactory availability](https://developer.android.com/reference/javax/crypto/SecretKeyFactory): platform PBKDF2-HMAC-SHA256 requires API 26.
- [Bouncy Castle release information](https://www.bouncycastle.org/download/bouncy-castle-java/) and [PKCS5S2ParametersGenerator API](https://downloads.bouncycastle.org/java/docs/bcprov-jdk18on-javadoc/org/bouncycastle/crypto/generators/PKCS5S2ParametersGenerator.html).
- [OWASP PBKDF2 work factors](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html): 600,000 HMAC-SHA256 iterations.
- [Android cryptographic biometric authentication](https://developer.android.com/identity/sign-in/biometric-auth).
- [Official Fragment releases](https://developer.android.com/jetpack/androidx/releases/fragment).

The current IDE artifact lookup confirmed the pinned SQLCipher, BC and Fragment stable releases. No dependency version was floated. Successful debug/native instrumentation results and remaining device gaps are recorded separately in phase-status.md.

## Key lifecycle and recovery limits

First setup requires matching master passwords of at least 12 characters and explains password loss and device-key loss. CryptoManager generates a random 32-byte database passphrase with SecureRandom. The password derives a separate 256-bit key-encryption key using PBKDF2-HMAC-SHA256, UTF-8 password encoding, a random 16-byte salt and 600,000 iterations. API 26+ uses the vetted platform PBKDF2WithHmacSHA256 implementation; API 24–25 uses Bouncy Castle's lightweight implementation directly. Both use the same algorithm, UTF-8 semantics and cost. An instrumentation test is provided to compare their output for a Unicode password; its final device execution was interrupted and remains pending. No SHA1 downgrade or global provider registration exists. The portable path can be noticeably slower on older devices. Cost is bounded when parsing metadata (210,000–2,000,000) to prevent an invalid envelope causing unbounded work. Passwords are not trimmed or normalized.

The database key is wrapped with AES-256-GCM using a fresh random 12-byte nonce and 128-bit authentication tag. The complete metadata header is authenticated as AAD. KDF work, native database opening and envelope I/O run on Dispatchers.IO.

The password-wrapped envelope is itself encrypted with a distinct Android Keystore AES-256-GCM storage key. This key does not require biometric authentication and is never exported. No EncryptedSharedPreferences dependency is used. Only this outer authenticated ciphertext resides in private noBackupFilesDir. Ordinary preferences contain only the public lock-timeout choice.

The master password, derived KEK and raw DB key are never stored in preferences or files. Owned mutable password/key buffers are cleared where feasible, including abandoned unlocks and session closure. Kotlin/Java strings, Compose text, cryptographic providers and native code may retain copies until collection; complete JVM/native memory erasure is not claimed.

The outer Keystore key makes the envelope device-bound. Copying the database and envelope to another device is insufficient. Uninstall, clear-data, factory reset, missing Keystore keys, or device loss can permanently prevent recovery even with the master password. No account/server/reset/recovery service exists. No automatic reset is offered for an encrypted vault.

## Version 1 storage format

All integers are big-endian; lengths are checked before cryptographic work.

- Password envelope: version int=1; KDF int=1 (PBKDF2-HMAC-SHA256, UTF-8); iterations int; salt[16]; nonce[12]; AES-GCM ciphertext/tag[48]. Total 88 bytes. The first 40 bytes are AAD, including version, KDF, cost, salt and nonce.
- Device envelope: version int=1; creation-pending byte (0/1); storage nonce[12]; AES-GCM ciphertext/tag[104]. Total 121 bytes. AAD includes a domain label and the version/pending header, so the initialization state is authenticated.
- Biometric envelope: version byte=1; biometric nonce[12]; ciphertext/tag[48]. Total 61 bytes. A distinct Keystore alias and domain-separated AAD protect this alternate wrapper.

Reads are bounded to 8 KiB and reject malformed/unknown versions. Writes use Android AtomicFile with explicit stream sync, finishWrite and failWrite. Password change verifies the current password, generates fresh salt/nonces and atomically replaces only the wrapping envelope around the same DB key. The prior working envelope survives a failed pre-commit write. Database pages are not re-encrypted. Rewrapping does not revoke previously copied database/envelope pairs or plaintext exports.

Initialization commits a creation-pending authenticated envelope before creating the encrypted DB. Correct-password unlock may resume an interrupted pending creation using that same key. Only after a real Room query succeeds is the envelope marked active and an unlocked session published. Active envelopes require a nontruncated existing DB (the pinned SQLCipher 4 defaults use 4096-byte pages). The Room helper also rejects onCreate for an active vault, so a valid encrypted file with a missing schema/version cannot be silently initialized again. Only first setup or an authenticated creation-pending journal authorizes creation. Missing, truncated or unopenable active DBs are preserved as errors. Missing keys, wrong passwords, malformed envelopes and DB errors preserve existing files.

## Plaintext prototype policy

Production has no plaintext Room provider or destructive migration fallback. The old keeppeer_prototype.db is detected without opening it. If no encrypted vault exists, a clearly labeled development screen requires explicit confirmation to delete disposable synthetic Phase 1 data. Cancel leaves it untouched. The encrypted vault uses keeppeer_vault.db.

There is no preservation migration in this phase. Do not confirm a reset containing real credentials. Files removed from flash storage may remain in wear-leveling, snapshots or other forensic remnants; deletion is not secure erasure. Existing encrypted files are never a reset target. Test-only plaintext databases remain solely in synthetic DAO compatibility tests.

## Authentication and session revocation

VaultSession is application-scoped and exposes first-run, legacy-reset, locked, unlocking, unlocked and recoverable-error states. Room is constructed/opened only after authenticated key unwrap. A real SELECT from entries proves that the supplied key opened the schema before success is published.

Each unlock creates a fresh VaultAccess with its own repository, cancellable coroutine tree and ViewModelStore. Lock revokes the repository immediately, cancels entry work, clears the navigation/destination ViewModels and their decrypted states, waits for vault work, closes Room off main, and wipes its owned key buffer. Stale repository handles cannot perform further operations. No unlocked NavHost exists on the locked branch. Back cannot reveal a previous entry. A new process always starts locked when a vault exists.

The default background timeout is immediate, with 1-minute and 5-minute options. SystemClock.elapsedRealtime measures absence, a timer locks while backgrounded, and foreground entry independently checks elapsed expiry and Keyguard state. MainActivity.onStop excludes configuration changes; rotation retains memory-only entry drafts and resets Compose reveal state. An application-lifetime SCREEN_OFF receiver locks regardless of the chosen timeout.

## Biometric unlock

Biometrics are optional and always retain master-password fallback. Enabling requires re-verifying the current master password, then authenticating the actual encryption Cipher through BiometricPrompt.CryptoObject. A separate Android Keystore AES-GCM key is authentication-bound per operation: API 30+ uses setUserAuthenticationParameters(0, AUTH_BIOMETRIC_STRONG); API 24–29 uses the documented -1 per-operation validity configuration. Enrollment invalidates the key.

The authenticated Cipher wraps the same database key into a separate file. Unlock initializes that key's decryption Cipher and binds it to another CryptoObject; only doFinal success supplies a DB key. A successful UI callback alone cannot unlock. The callback must return the expected Cipher. Cancellation, no enrollment, unsupported hardware, lockout, invalidated/missing keys and DB failures leave the password envelope intact. Disable deletes the wrapper and removes the biometric key, reporting a failure if cleanup cannot complete. Re-enable creates a fresh biometric key. Prompt cancellation during Activity loss leaves password unlock available.

## Screen, clipboard and draft boundaries

MainActivity sets FLAG_SECURE before composing content. Compose dialogs inherit the secure parent window. Passwords are masked by default; list rows contain neither passwords nor notes. Password entry uses password keyboard options. No secrets enter logs, navigation arguments, SavedStateHandle, rememberSaveable, ordinary preferences or persisted drafts.

SensitiveClipboard is the single copy boundary, including Compose selection/cut and hardware-keyboard copy through both current and legacy Compose clipboard providers. It marks clips sensitive, assigns a unique ownership label and expires app-owned content after about 30 seconds. Background/lock clears while permitted; denied access keeps the ownership token for a foreground retry. Clearing checks ownership so newer unrelated clipboard content is preserved. API 24–27 clears using an empty replacement; API 28+ uses clearPrimaryClip. Android/OEM clipboard access restrictions, keyboard clipboard histories and other apps' prior reads cannot be undone.

Drafts and passwords live only in memory and disappear on lock/process death. Navigation arguments contain numeric IDs only. Reopen a saved entry only after authentication. Phase 3 picker launches must count as background; result handlers must verify the still-current session generation before obtaining a repository or delivering plaintext. No picker exemption is implemented.

## Offline, backup and verification policy

minSdk remains 24. Backup is disabled with explicit cloud/device-transfer exclusions. Shipped manifests exclude Internet and broad storage permissions. Phase 3 SAF will scope access to user-selected files. No analytics or remote services exist.

See phase-status.md, reports/phase-2-report.json and phase-2-device-checks.md for actual commands, results, and outstanding API/hardware/manual coverage. Unexecuted checks are not evidence of protection. Release remains disabled until the required device protections are verified.
