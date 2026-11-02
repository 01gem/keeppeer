# Phase 5 checklist — in progress

Outcome incomplete; verificationComplete=false. P1a genuine validation rebuild (shell-3) produced 7ec589f5… / 10,170,209 bytes (fourth distinct hash); P1 finalOutcome unresolved, reason validation-identity-new-hash. P1c signer audit PASS; P1d debug drift check PASS. Task G3-A executed on fresh API24 AVD Keeppeer_Guard_Phase5_24_20261101_g3a001 (60.6 GB free host disk); diagnostic run reproduced returnToOwnActivity timeout failure. Dumpsys failure dump captured: mResumedActivity=launcher, com.gem.keeppeer task absent on return -> diagnosis (ii). No test source edit.

- [/] Reconcile history: all 11 inherited nativeRuns objects, including nine restored historical entries, remain unchanged/in original order. P2 evidenceRecovery unchanged; shell-18/shell-36 and ProgressBar reconciliation await the 08:47:40 PM PHT snapshot.
- [/] Audit v1 source/config against the requirement map; inherited evidence retains its original artifact scope.
- [/] Prepare opt-in whole-app R8/resource shrinking/signing isolation. Current validation identity remains unresolved (validation-identity-new-hash; shell-3 produced 7ec589f5… / 10,170,209 B).
- [/] Reconcile current byte counts: debug 32,260,929 and release 10,157,737 at unchanged hashes.
- [/] Retain earlier production corrections; no production or test source edit in this continuation.
- [/] Historical host checks and shell-145 AndroidTest compilation PASS retain original scopes.
- [/] Fresh API24 fixture creation/start: Keeppeer_Guard_Phase5_24_20261101_g3a001 created, booted, installed debug/test APKs. G3-A diagnostic run executed; dumpsys captured mResumedActivity=launcher -> diagnosis (ii). Stopped via emu kill with name guard.
- [/] Initial API24 setup/Settings evidence remains historical: shell-194 ordinaryCompatibilityReopen, EncryptedVaultTest 2/2 and VaultSecurityTest 7/7 PASS retain original scope.
- [ ] Complete normal minified whole-app SAF/generator/favorite/theme/password-change/cold journeys and legacy seed/refusal pair (Gate 2): all four NOT RUN, P1 unresolved.
- [ ] Complete API31 unchanged setup diagnostic, raw-evidence classification, conditional correction and fresh-fixture PASS (Gate 1a): NOT RUN, P1 unresolved. P3 applies only after P1 resolution; prior P3b source preserved.
- [ ] Complete API25 setup and same-fixture API24 setup -> reopen at final test hash (Gate 1b): NOT RUN, P1 unresolved.
- [/] Execute API24 WindowRegistrationTest and require OK (1 test) / INSTRUMENTATION_CODE: -1 (Gate 3). Prior shell-161 FAIL and new shell-G3-diagnostic FAIL (1 run/1 failure, lifecycle=CREATED return timeout). Dumpsys captured diagnosis (ii). No assertion or timeout change.
- [x] Establish distribution history: no prior distribution; versionCode 1/versionName 1.0 confirmed as initial v1 (owner confirmation, not a native test).
- [x] Keep hardware/OEM/manual/TalkBack/remote-provider/16 KiB runtime limits explicit; outside task, no execution claim.
- [/] Reconcile report/docs and regenerate source-code.txt with -Outcome incomplete.

Gate 3 remains open; its checklist state changes only after G3-C PASS. ProductionReleaseReady=false, published=false.

Exact evidence: [phase-5-report.json](reports/phase-5-report.json). Local procedure: [release-checklist.md](release-checklist.md).
