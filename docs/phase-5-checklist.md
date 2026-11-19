# Phase 5 checklist — complete (Samsung A12 scope)

Superseded by docs/a12-acceptance.md (11/11, 2026-11-19). Items below are historical.

**2026-11-19 owner re-scope.** Target is the Samsung A12 (`RR8R402RR7T`) only. Gates 1a, 1b and 3 and the historical snapshot reconciliation are RETIRED (owner re-scope 2026-11-19), not passed. Gate 2 is superseded by [docs/a12-acceptance.md](a12-acceptance.md). Entries below are kept as history.

**2026-11-19 continuation.** Gate 1a identity rule re-baselined by owner decision on 2026-11-19; prior target 218e1375… retired because AGP embeds VCS/AGP metadata. `vcsInfo { include = false }` added to release/validation. Candidate: validation `eb213062da801a0ac3a8ddad697e5529f98512a8598f8a00b93cc718365ae012` / 10,170,124 B, test `d93681c343142f1461f1dadb1d3d29156a3c24d40f30fe2afdd5a94923e829b8` / 1,245,770 B. Forced second rebuild **not stable** (`aa547fe2…`, `c87bb30d…`): entries identical; AGP encrypted dependency-info signing sub-block `0x504b4453` differs. After owner-approved `dependenciesInfo { includeInApk = false }`, two forced rebuilds match: **reproducible candidate** validation `33c824c85dfe5a39525d1a8cdd91eaada72a44554df5c59d5fb4c92926a85110` / 10,161,932 B, test `1ed336de81264d9b5d91cfd2056eaf812b4a1735f849f0676f09283fbc86eacb` / 1,244,360 B.

- [x] STEP 0 read-only A12 inspection: RR8R402RR7T / SM-A125F / API31 (Android 12) / arm64-v8a; app not installed; test package signer `de88078f` matches host debug cert.
- [x] Physical lane `tools/check-phase5-physical.ps1` added (supplementary; `check-phase5.ps1` unchanged). Inspect PASS; serial/legacy refusals verified.
- [x] Runner `phase5PhysicalSerial` change applied (androidTest only); A12 `install -r` PASS for both APKs, signers `de88078f`.
- [ ] Physical whole journey (supplementary, API 31, arm): **FAIL** at `await Add` after Create vault — UI held `Keeppeer locked` + spinner (UNLOCKING) ≥120 s; no crash/ANR; vault was created (cold-launch probe shows Unlock). Same symptom as API31 emulator shell-13. Cause unproven.
- [ ] Physical unlock-only diagnostic (`ordinaryCompatibilityReopen`, owner-requested): **FAIL** at `await Add` after Unlock (122.9 s). Logcat: keystore op, SQLCipher keying returned 0, envelope re-read keystore op all ran within ~4 s; the UI then stayed on the locked-branch spinner. Stall is not setup-specific; blocking point unproven (non-debuggable, no thread dump).
- [ ] Caption rerun (androidTest-only `captions=` in failure summary; test APK `ad8ec9c9…`, incremental, diagnostic only): **FAIL**, caption **`Opening encrypted vault…`** = UNLOCKING. `authenticate()` never reaches UNLOCKED, `lock()` or the error path; it stalls after SQLCipher keying and the second keystore op (VaultSession.kt:227-238). Exact line needs a thread dump.
- [x] A12 manual setup/unlock (owner by hand, fresh synthetic vault, 2026-11-19): debug, traced validation (R8) and clean validation all open the vault; wrong password shows the error. Stall not reproduced by hand; earlier stalls were instrumented-only. Temporary trace removed; no app code change.
- [ ] Physical cold reopen: NOT RUN — precondition unmet; A12 fresh-vault chain consumed (uninstall/pm clear forbidden). Legacy pair refused on A12. Physical evidence never closes Gates 1a/1b/2.
- [ ] **RETIRED (owner re-scope 2026-11-19)** — Gate 3 test-only hypothesis (`am start -W -f 0x00020000` in `returnToOwnActivity()`): applied; hypothesis untested until a real API24 run passes.
- [x] Make validation build reproducible (dependency-info block) and confirm two forced rebuilds match.

Earlier (2026-11-09): Outcome incomplete; verificationComplete=false. Fresh validation packaging passed under the current working tree but produced SHA-256 d8a5a2d72a5fd443ef5b49332261b3eb901c0df373737dcbc8af4109142ac186 / 10170209 bytes instead of recorded target 218e1375…. The pre-existing AGP 9.5.1 edit is preserved; the baseline records 9.5.0. Debug and AndroidTest packaging passed. API24/API25/API31 AVDs were registered; emulator acceleration check exited 6, and software boots stayed offline. No APK install or app test occurred.

- [x] Host JVM unit test suite rerun (`.\gradlew.bat :app:testDebugUnitTest --rerun-tasks --console=plain`): **69/69 passed**, 0 failed, 0 skipped at ~06:10 AM PHT on 2026-11-09 (session `shell-28`, revision `67a63926568c755909109ae7e0e36f674b6073aa`).
- [x] Register fresh API24/API25/API31 x86_64 AVDs under workspace work/android/avd; startup attempts recorded, no fixture booted.
- [x] Run validation rebuild and package debug/AndroidTest; a reversible AGP 9.5.0 build also succeeded but missed the target. Both the 9.5.1 catalog and D8A5 candidate were restored. Identity remains unresolved.
- [x] Check emulator acceleration and retry boot across API24/API25/API31. Host reports Hyper-V/Windows Hypervisor Platform configuration required; no app tests started.
- [ ] **RETIRED (owner re-scope 2026-11-19)** — Resume gates after enabling host virtualization and rebooting; reconnect Samsung A12 by USB for physical-device tests.
- [ ] **RETIRED (owner re-scope 2026-11-19)** — Reconcile history: all 11 inherited nativeRuns objects, including nine restored historical entries, remain unchanged/in original order. P2 evidenceRecovery unchanged; shell-18/shell-36 and ProgressBar reconciliation await the 08:47:40 PM PHT snapshot.
- [/] Audit v1 source/config against the requirement map; inherited evidence retains its original artifact scope.
- [/] Prepare opt-in whole-app R8/resource shrinking/signing isolation. Current validation identity differs from the recorded target; latest build is d8a5a2d72a5fd443ef5b49332261b3eb901c0df373737dcbc8af4109142ac186 / 10,170,209 B under AGP 9.5.1, with no device test yet.
- [/] Historical debug/release counts remain unchanged for those builds; current debug is feb56f1be7d2b1be1ea02b17ac7b3b6e72999c9a0cd3cb0f83adf5bd54047e3d / 32253280 B, and release was not rebuilt here.
- [/] Retain earlier production corrections; no production or test source edit in this continuation.
- [/] Historical host checks and shell-145 AndroidTest compilation PASS retain original scopes.
- [/] Fresh API24 fixture creation/start: Keeppeer_Guard_Phase5_24_20261101_g3a001 created, booted, installed debug/test APKs. G3-A diagnostic run executed; dumpsys captured mResumedActivity=launcher -> diagnosis (ii). Stopped via emu kill with name guard.
- [/] Initial API24 setup/Settings evidence remains historical: shell-194 ordinaryCompatibilityReopen, EncryptedVaultTest 2/2 and VaultSecurityTest 7/7 PASS retain original scope.
- [ ] **Superseded by [docs/a12-acceptance.md](a12-acceptance.md)** — Complete normal minified whole-app SAF/generator/favorite/theme/password-change/cold journeys and legacy seed/refusal pair (Gate 2): NOT RUN; no AVD reached boot-complete.
- [ ] **RETIRED (owner re-scope 2026-11-19)** — Complete API31 setup/reopen diagnostic and fresh-fixture PASS (Gate 1a): NOT RUN; API31 AVD remained ADB-offline. Candidate hash differs from the recorded baseline.
- [ ] **RETIRED (owner re-scope 2026-11-19)** — Complete API25 setup and same-fixture API24 setup -> reopen (Gate 1b): NOT RUN; API24/API25 AVDs did not reach boot-complete.
- [ ] **RETIRED (owner re-scope 2026-11-19)** — Execute API24 WindowRegistrationTest and require OK (1 test) / INSTRUMENTATION_CODE: -1 (Gate 3). Historical API24 run failed (lifecycle=CREATED return timeout; launcher foreground/task absent). This continuation API24 retry did not start because the emulator remained offline before APK installation.
- [x] Establish distribution history: no prior distribution; versionCode 1/versionName 1.0 confirmed as initial v1 (owner confirmation, not a native test).
- [x] Keep hardware/OEM/manual/TalkBack/remote-provider/16 KiB runtime limits explicit; outside task, no execution claim.
- [x] Reconcile report/docs and regenerate source-code.txt with -Outcome incomplete.

Gates 1a, 1b and 3 are RETIRED (owner re-scope 2026-11-19), not passed; Gate 2 is superseded by [docs/a12-acceptance.md](a12-acceptance.md). Unlock stall not reproduced by hand on the A12 (2026-11-19); next: [A12 acceptance](a12-acceptance.md). ProductionReleaseReady=false, published=false.

Exact evidence: [phase-5-report.json](reports/phase-5-report.json). Local procedure: [release-checklist.md](release-checklist.md).
