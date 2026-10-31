# Phase 2 — prior-initialization guard

## Final window verification — current 2026-10-31

**Phase2 is complete for the original required criteria, with the recorded provider/artifact limits.** The retained API25 WindowRegistrationTest passed 1/1 in 7.379s. Its actual command/result is retained in session 5ede27b9 steps.jsonl:107 (tool call c428bac8-5cc1-45d0-82a6-e5c6b15cac42); no contemporaneous per-run APK hash attestation was located, so current hashes are not assigned retrospectively.

The final affected API31 method passed on the new separately registered Keeppeer_Guard_API31_FinalWindow_20261031_d72faa02, explicit serial emulator-5554: 1 expected/started/executed/passed, 0 failed/skipped, 15.781s, OK (1 test), native code -1, shell exit 0 (shell-92.log). App SHA-256 1c3adda7dd80d515b8ec64e387a77d64d26b6da3bc261961dd007dc98a4965dc; final test 40305e7296b7f9966ca3d98903fa588997bc9f568b9b44cda9ffc646c9d373d0; WindowRegistrationTest source c4ab7c965e58373f22c8cbecc4bf111e9622252b21328aefd1a802a3ad3fb673. Host and installed APK hashes match. Final diff review restored the explicit post-return same-window identity assertion omitted by the pre-existing dirty edit. The first API31 attempt failed in the host-only focus wait (39.233s); the smallest fixture correction recognizes focus on its actual owned dialog or original Activity. The rerun observed dialog focus, then parent focus, and passed every unchanged same-window, alpha0.8, hidden-closing, secure-flag, parent-return and recreation assertion. Production code is unchanged.

The new fixture reached its default launcher without credential/wake/swipe input; qemu1/API31/x86_64/boot complete and unlocked state were verified. Only this new AVD was stopped; registration/data retained. The retained API31 fixture remains FBE/keyguard locked with an unknown synthetic credential. That newer blocker is an unknown-credential provisioning constraint, not a new unauthorized-input attempt or new review rejection. The earlier combined wake/menu rejection remains historical and was not replayed. The fresh run does not unlock or recover the retained AVD.

Controlled clipboard 2/2 (58.101s, shell-631, test fde7e6ce) and restored enabled sharing, approved biometric/guard/native results, original matrices and capture/lifecycle evidence retain their exact original scopes. No broad reruns, opacity/Recents redesign or universal hardware gate. Recovery stays closed for disposable entries; personal/candidate/preservation material and Defender/platform security untouched. Clean scan remains user-reported; old detection classification unknown. **No required Phase2 gap remains. Release disabled; Phase3 unstarted.**

See [final evidence and identity limits](phase-2-final-window-evidence.md), [window/clipboard acceptance checklist](phase-2-window-lifecycle-clipboard.md) and currentApi25WindowVerification/finalFreshApi31Verification in [the report](reports/phase-2-report.json). Earlier dated sections below are historical where superseded; their original failures and artifact identities remain unchanged.

## Focused live Recents correction — historical reviewed baseline

**The failing API31 live-card exposure is corrected and verified; Phase2 remains incomplete for controlled clipboard-regression verification.** Public window-focus redaction covers the Activity/app-owned dialogs and hides the IME without changing session expiry or biometric cancellation. Actual Recents regression1/1passed; affected API31 group6passed/2clipboard failures/0skipped (approved biometric3/3passed); API25 compatibility2passed/0failed/1strong-enrollment skip. Assembly/test assembly/lint passed,0errors14warnings. Current hashes, sampled transition/settled UI, real per-use authentication, Home/grace/expiry/Back/screen-lock and protected-dialog PNG evidence are in [focused correction and acceptance checklist](phase-2-live-recents-correction.md) and liveRecentsCorrection in the [JSON report](reports/phase-2-report.json).

Clipboard sharing remains enabled: automatic review rejected disabling that emulator toggle without explicit authorization. An isolated run showed an owned clip label replaced by “host clipboard”; the2checks remain failed/unresolved, with no weakened assertions or indirect setting change. Prior live-card failure and reverted exclusion trial, all earlier counts/artifact identities, disposable-entry recovery closure and the uninspected user clean-scan report remain history. Cause/old Defender classification remain unknown; personal/preserved material untouched; release disabled; Phase3 unstarted.

Earlier verification/status statements below retain their historical artifact scopes where superseded by this section. Standing preservation and security boundaries still apply.

This change prevents defined future silent-setup cases. It does not recover entries, identify the original vault, explain the availability incident, or make preservation copies portable. Phase 2 remains incomplete; release disabled; Phase 3 unstarted.

## Privacy closeout — 2026-10-30

**Phase2 remains incomplete for one concrete original privacy gap:** the dedicated API31 provider's settled direct live current-task Recents card exposed a revealed synthetic draft. The app remained RESUMED/visible/focused with SECURE flags. This is an observed failure, not unavailable inspection or a proven persisted thumbnail; no universal platform root cause is asserted.

Ordinary Android system screenshot action now has actual decoded evidence: readable generic Display Settings control (1080×1920/91793bytes), black protected revealed-draft PNG (8818bytes, baseline app2692954), and black protected actual app-owned Compose dialog PNG (13894bytes, manifest-only trial appd9c8494). App/keyboard/dialog content was hidden in these valid nonempty images; system bars/navigation may remain. No UiAutomation/CLI/native screencap capture path was retried and no security flag/setting was weakened.

A single `excludeFromRecents` manifest attribute was tried after the live-card failure. Its task flag was present and the backgrounded app disappeared after Home, but the direct live own card still appeared. The trial was not a verified fix and was reverted. All78 final app/toolchain source/config files match the reviewed guard; no app/test/build/exporter change is retained. Rebuilt final app SHA-256 is `a6c3dfe4f52a13dd3240fe56a33f5839c38c1486132742d13193edf96d9aa061`, distinct from reviewed app2692954; the cause of binary difference was not investigated. Test hash remains `cfbc62596ee1e56f5ee3c962a30701443ae3da37e4ee7b7f0abad84976156e1a`.

Debug assembly/lint passed. Only affected rotation/lifecycle checks ran:2/2,0fail/skip,54.630s on triald9c8494;2/2,0fail/skip,52.089s on finala6c3dfe4. Earlier3/3 biometric143.002s and22/22 API24/API25/API31 results remain closed at their exact original artifacts, not relabeled or blanket-rerun. Earlier short recording samples retain their limits; MainActivity secure-window source was verified identical.

The user reports that, after the01:44:39 PM export, the originating review chat received **“Full scan finished; no threats or new alerts.”** on2026-10-30; exact time unknown. This is user-reported, not independently inspected or a false-positive verdict. Defender stayed enabled/unchanged; historical classification is not a new Android functional acceptance gate.

Recovery is no longer requested because the missing original entries were disposable tests. Cause remains unknown; no deletion/reset/rekey or restoration claim. Personal Samsung/wireless aliases and all candidate/preservation material remained untouched. Physical/OEM/hardware-keyboard behavior is an honest unobserved compatibility limit, not an every-device gate.

**Next required work:** resolve and verify redaction of direct live own-app Recents from a revealed synthetic draft. A small manual comparison on another explicitly disposable provider can distinguish direct Recents from Home→Recents; report device/API and whether content appears. Another-provider pass does not erase the recorded API31 failure. No extra approval, recovery attempt, generic capture retry or historical antivirus verdict is required. Release remains disabled; Phase3 unstarted.

See [original acceptance map and exact evidence](phase-2-privacy-closeout.md) and authoritative `privacyCloseout` in [the report](reports/phase-2-report.json). Older pending-approval, recovery, broad matrix and security-attribution statements below retain their dated historical scope.

## Historical approved disposable biometric tests — earlier 2026-10-30

The user explicitly approved only the three existing tests in `com.gem.keeppeer.BiometricEnrollmentTest` on disposable emulators with synthetic data, and reported selecting “Approve disposable-emulator tests” in the originating review chat on2026-10-30. This current instruction supplies the previously missing approval; no access to the external chat is claimed. Historical automatic-review rejections remain unchanged below/in the report. The approved direct command was accepted; no renamed/indirect invocation or security bypass was used.

**Result:3 expected,3 started/executed,3 passed,0 failed,0 skipped;143.002s; native `OK (3 tests)`, instrumentation code-1, shell exit0.** Completion observed 2026-10-30 01:41:27 PM PHT (UTC+08:00). All three existing tests passed unchanged:

- emptyWrongAndUnapprovedEnrollmentPreservePasswordAccess.
- postAuthenticationAadPreservesSingleApplicationAndV1Compatibility.
- enrollmentDialogRejectsBlankAndDoesNotUsePasswordChangeFields.

The retained registered `Keeppeer_Guard_API31_20261030_123138` AVD was rediscovered as `emulator-5580`, with qemu1/API31/x86_64/boot-complete verified. Its two existing synthetic fingerprints met the dialog availability assumption; that test was not skipped. Installed SyntheticTestRunner targets com.gem.keeppeer and substitutes UUID-isolated SyntheticVaultContext file/preferences/alias namespaces. Existing synthetic PIN unlock was normal lock-screen input; no enrollment, credential configuration, reset or wipe occurred. The AVD was stopped after completion, retaining its data.

All78 app/toolchain source/config files matched the reviewed01:23:09 PM PHT/749200-byte snapshot. Host and installed app SHA-256 `2692954259667faa77710eea0b82ef5d6a69362eb31bcb7d979c4b4cdfb54e3d` and test SHA-256 `cfbc62596ee1e56f5ee3c962a30701443ae3da37e4ee7b7f0abad84976156e1a` matched. No production/test edit, rebuild, reinstall or broad rerun was needed. Earlier22/22 API24/API25/API31 runs and API31 actual per-use marker/invalidation/password-entry evidence retain their exact prior identities; they were not rerun or relabeled. Detailed current command/results are in `approvedDisposableBiometricVerification` in [the report](reports/phase-2-report.json).

**User decision: the unavailable original entries were disposable test entries; recovery is no longer requested.** This resolves the recovery-priority question only. Cause remains unknown; no recovery is claimed and no deletion/reset/rekey is authorized. Historical incident evidence and preserved files remain unchanged. No personal Samsung RR8R402RR7T/wireless-alias installation, instrumentation, process action, enrollment, setup, reset/clearing, vault/candidate/preservation access occurred.

**Phase2 remains incomplete; release disabled; Phase3 unstarted.** Existing bounded limitations remain: protected screenshot/dialog/own-recents pixels and physical screenshot-policy evidence, physical hardware-keyboard observations, and unresolved Defender attribution/verdict. No unavailable capture path was retried, no new gate added, and no Defender detection/allow/restore/exclusion/protection setting changed. Historical pending-approval/recovery statements below describe the earlier state and are superseded by this explicit approval, actual run and user decision.

## Availability and namespace boundaries

VaultSession begins in CHECKING. File and exact-alias inspection runs under the session mutex on Dispatchers.IO. No create, password or biometric unlock controls appear while checking. Inspection failures become UNAVAILABLE, never confirmed absence. UNAVAILABLE has an explanation and no setup, impossible unlock, reset or fake recovery action.

Only the resolved context's known password/biometric envelope paths, their AtomicFile .bak/.new siblings, encrypted/prototype DBs and SQLite -wal/-shm/-journal sidecars, and the initialization record are inspected. Ancestor/file types, accessibility and native lstat errors are checked; unexpected links, unreadable evidence and errors fail closed. The two exact namespace-specific Keystore aliases are queried with containsAlias; no enumeration, key generation or key export occurs during inspection. Preferences and generic directories are not proof of a completed vault. No other cache namespace, preservation directory or candidate payload is scanned/imported.

A usable password-envelope filename (.bin or .bak) retains the password authentication path, including creationPending recovery. Existing encrypted evidence without that envelope blocks setup. Legacy-only evidence retains the explicit synthetic prototype reset flow, with eligibility rechecked before deletion. A successful inspection with no prior-use/legacy evidence permits FIRST_RUN. Setup checks eligibility again under the serialized gate immediately before provisioning; the store serializes its start inspection/commit across session instances. Stale/repeated calls reject and clear supplied password/confirmation buffers.

## Durable state and crash handling

FileVaultInitialization stores a bounded, versioned, namespace-bound record named vault-initialization-v1.bin in the resolved private no-backup directory. The record contains only a format label, namespace and STARTED/COMPLETE phase; no password, entry or key material.

STARTED is committed through AtomicFile with file synchronization and parent-directory fsync before provisioning storage keys or vault material. A required write/sync failure stops provisioning. COMPLETE is committed only after authenticated SQLCipher open/schema verification, while retaining the existing creationPending envelope protocol. A completion-write failure closes/revokes the attempted access and reports failure; it cannot publish an unlocked session.

- Crash with STARTED but no usable envelope: unavailable, preserve material, no automatic regeneration or reset.
- STARTED plus a usable creationPending envelope: password authentication resumes with its existing DB key; no new key is generated.
- Existing active vault without a marker: inspection does not backfill. Verified authenticated password or genuine CryptoObject-backed biometric database open establishes completion without rekeying or replacing the vault/envelope.
- Final marker failure with valid DB/envelope: retain material and password retry. Successful authenticated retry commits completion.
- Marker corruption, namespace mismatch, inspection uncertainty or lone residual siblings: fail closed. No automatic repair is offered.

Generation/cancellation checks, background timeouts, database closure, repository revocation and decrypted UI clearing remain in place. A lock immediately revokes access and starts a new guarded inspection; tests wait for that inspection rather than assuming synchronous file/Keystore work.

Markers and keys can themselves disappear with full app-data removal/uninstall. An older installation without evidence is not proof that it never held a vault. These limits remain explicit; there is no recovery claim.

## Disposable guard verification — 2026-10-30

**Current scope: disposable emulators only. The personal Samsung RR8R402RR7T, including its wireless alias, is excluded from deployment, instrumentation, process actions, enrollment, clearing and candidate/preservation access.** This supersedes the earlier Samsung execution scope retained below.

The reviewed 12:21:51 PM PHT snapshot (696006 bytes, revision401d359 plus dirty work) matched all 78 current app/toolchain source/config files. No production or test source changed and no rebuild was needed. Current app SHA-256 is `2692954259667faa77710eea0b82ef5d6a69362eb31bcb7d979c4b4cdfb54e3d`; test SHA-256 is `cfbc62596ee1e56f5ee3c962a30701443ae3da37e4ee7b7f0abad84976156e1a`. Existing host assembly/15 JVM tests/lint0errors14warnings remain carried-forward evidence.

| Current selected native group | Passed | Failed | Skipped | Native seconds |
| --- | ---: | ---: | ---: | ---: |
| New disposable API24 / x86_64 | 22 | 0 | 0 | 57.395 |
| New disposable API25 / x86_64 | 22 | 0 | 0 | 57.945 |
| New disposable API31 / x86_64 | 22 | 0 | 0 | 166.763 |

Each group includes the 11 guard/UI tests plus 11 relevant existing security/session/SQLCipher/UI tests. These are scoped current-artifact runs, not a full-suite claim. Fresh registered AVDs were created without wiping/reusing stale directories; exact target metadata was checked. API31 installed app/test hashes matched host. API24 installed-byte checksum/pull was unavailable; API25 installed-byte hashing was not attempted. Direct fresh installs succeeded and host hashes matched on all targets.

On API31's Fingerprint21 emulated provider, actual per-use enrollment and cold biometric unlock passed through native CryptoObject/Cipher binding, post-auth AAD, finalization and database open. Removing only the disposable vault's marker before cold launch left the app locked; authenticated biometric open restored the synthetic entry and a 45-byte expected COMPLETE record. The password/biometric envelopes retained their prior sizes/timestamps. Adding a second synthetic fingerprint changed native enrollment count1→2, and the existing key genuinely raised KeyPermanentlyInvalidatedException. Password fallback reopened the same synthetic entry. This is emulator-provider evidence, not a physical-phone result.

Automatic approval review again rejected the requested BiometricEnrollmentTest class even for the new API24 AVD, citing its retained explicit-approval requirement. The class remains **unrun**; it was excluded from the 22-test groups, without renaming or indirect invocation. The separately authorized normal-app synthetic biometric flow was accepted and is reported separately.

Capture remains unavailable: API31 native screenshot test recorded **0pass/0fail/1skip,17.853s** after a valid readable control but a null protected-draft bitmap. A separate CLI generic control produced a visually reviewed readable PNG; protected-dialog and own-recents attempts each reported exit0 but produced empty files. No protection pass and no repeated unavailable capture attempt. Only own synthetic app/test task cards were inspected. Earlier recording evidence retains its original artifact/timeline limits.

**Phase2 remains incomplete; release disabled; Phase3 unstarted.** Remaining: explicit approval/execution of the rejected class if required; valid protected screenshot/dialog/own-recents pixels and physical screenshot-policy evidence; physical hardware keyboard; original-vault incident/user decision and Defender attribution/verdict. No suitable disposable physical hardware was available. Injected input is not physical evidence. Original-vault cause, identity and recovery remain unknown; no personal files/keys/candidates/preservation were accessed. Defender stayed unchanged; no rejected/encoded command, protection bypass or exclusion was used.

See [detailed current evidence](phase-2-guard-verification.md) and the authoritative `guardVerificationContinuation` object in [structured report](reports/phase-2-report.json). Earlier dated verification sections and report objects retain their original scope, artifacts, failures and skips as history.

## Compatibility and earlier Samsung verification history

minSdk 24, dependencies, PBKDF2 600,000 iterations, SQLCipher schema, envelope formats and default alias names are unchanged. Biometric 1.4.0-alpha07 remains the documented compatibility exception. Per-use strong-biometric CryptoObject/Cipher binding and post-authentication AAD ordering are unchanged. Envelope wrappers defer resolving private file paths until used.

New native regressions use fresh SyntheticVaultContext UUID paths/aliases and actual Android files, AtomicFile, Keystore and SQLCipher. Deterministic write/inspection failures are injected at explicit store boundaries; authentication success is never mocked. Coverage includes fresh/restarted and marker-less valid vaults, key/entry retention, loss of both normal files, alias-only and residual evidence, unreadable/failed inspection, initial/final write failures, interrupted provisioning, pending recovery and stale setup. The UI test checks blocked checking/unavailable controls and secret clearing while an IO inspection is held.

The user explicitly redirected execution to Samsung SM-A125F, serial adb-RR8R402RR7T-jsNzTx._adb-tls-connect._tcp, after the continuation prompt originally restricted execution to disposable targets. Only in-place installs and explicitly targeted SyntheticTestRunner execution are used. No connected-device cleanup harness, uninstall, clear-data/cache, normal-vault setup/password attempt/recovery, preservation-file write, or enrollment change is authorized/performed. Fixture deletion/permission changes apply only to newly created synthetic namespaces.

Earlier Samsung verification: host assembly and 15 JVM tests passed; lint has 0 errors/14 warnings. New guard/UI regressions passed 11/11 (132.738s). The affected group retained 10 passes/1 timing-fixture failure; its corrected timing target passed on the next test artifact. That next two-test run also retained a screenshot readiness failure before capture. After the readiness correction, the screenshot-only check recorded 0 passes/0 failures/1 skip (4.722s): the generic control screenshot API returned null, so no valid images existed for visual review and no protection pass is claimed. The current app hash stayed unchanged across the test-only corrections. The report records all three test APK identities separately.

Earlier remaining actions (superseded by the disposable results above): authorized API 24/25 guard verification; the blocked biometric class approval and actual per-use biometric success as applicable; real enrollment-change invalidation on a suitable disposable provider; supported screenshot/physical-button/recents and physical-keyboard evidence; and the unresolved vault/security/user decisions. No further retries of the unavailable capture path or original-vault authentication are requested.

Current source/app/test identities, actual counts and remaining checks are recorded in reports/phase-2-report.json. Earlier API 24/25 and Samsung results remain attached to their original artifacts. No old full-suite result is relabeled for this app. API 24/25 execution and enrollment-invalidation/physical-input checks must be labeled unavailable when no authorized suitable target exists.

## Security-control incident, separate from vault behavior

The supplied continuation prompt reports Windows Defender Trojan:Win32/Commando.A!ml on an encoded PowerShell launch around 2026-10-30 01:12 AM. Two earlier process-launch attempts returned CreateProcess error 5 / Access is denied while attempting bounded metadata/free-space operations. Exact correlation with the Defender event and any false-positive determination remain unverified.

Those failed launches are not passing checks and are separate from the later completed private preservation evidence. Subsequent command success or a different shell does not prove safety. No blocked command is replayed; no detection is restored/allowed and no security exclusion, protection change or obfuscation is used. Normal IDE file/build APIs and explicit established command-line tools are used for independent authorized work; a newly security-blocked execution path must stop without a wrapper/shell workaround.
