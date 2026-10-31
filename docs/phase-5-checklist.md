# Phase 5 checklist — in progress

This task authorizes local release preparation and bounded synthetic verification. Phases0–4 are accepted within their recorded scopes; the old Phase4 prerequisite/unstarted wording is superseded. No production signing, publishing, upload or distribution is authorized or performed.

- [x] Reconcile starting commit92e4f06 and seven pre-existing dirty Phase5 paths; preserve accepted reports/failures/artifact identities and unrelated/preserved data.
- [x] Audit v1 source/config against [the requirement map](phase-5-verification.md), schema1, fail-closed storage, logging/saved-state/permission/backup boundaries and the pinned biometric preview exception.
- [x] Prepare opt-in whole-app R8/resource shrinking, unsigned production candidate, non-debuggable visibly development-signed validation, and safe separately opted-in production signing configuration. No production credential sources read or key created/used.
- [x] Remove ordinary minified legacy-reset eligibility, repair invalid partial Compose import, and replace default launcher metadata with the project lock vector.
- [x] Run69 JVM tests, debug/test packaging and lint; build and inspect actual initial minified artifacts, mapping/usage/DEX, merged manifest, signing and12 native libraries across4 ABIs. Earlier failed commands/harness starts remain history.
- [x] Register new distinct API24/25 fixtures from installed images without overwrite. Only one emulator runs at a time; exact identity guard precedes actions.
- [x] Initial normal minified API24 synthetic setup/save and cold unlock expose an actual Settings IME layout defect. Apply the documented full-screen Dialog edge-to-edge correction without changing secure-window registration or cryptography.
- [x] Complete refreshed final-source builds/JVM/lint/packaging and affected normal minified Settings regression (Phase5Harness ordinaryCompatibilityReopen PASS on API24 post-DialogProperties fix; EncryptedVaultTest 2/2 PASS, VaultSecurityTest 7/7 PASS; WindowRegistrationTest NOT APPLICABLE on API24 as test source restricts API to 25/31).
- [/] Complete actual normal minified whole-app SAF/generator/favorite/theme/password-change/cold journeys and targeted API24/25 native security/window evidence (API25 ordinaryCompatibilitySetupAndReopen failure recorded at await Edit — visible ScrollView present, harness await("Edit") does not scroll; consistent with harness scroll deficiency rather than proven product failure).
- [ ] Reopen modern API31 verification (API31 fixture Keeppeer_Guard_Phase5_31_20261031_e31b2 was successfully created with JDK 25 JAVA_HOME and booted, but Phase5StandaloneRunner ordinaryCompatibilitySetupAndReopen FAILED at await("Add") on that fixture; required pass not yet achieved).
- [x] Establish actual distribution history (confirmed no prior distribution; versionCode 1 / versionName 1.0 confirmed as initial v1 release version).
- [x] Record actual ABI/API/page-size and optional hardware/OEM/manual/TalkBack/spreadsheet limits. No compatible16KiB image is installed; packaging alignment alone is not runtime evidence. Client-wide settings restored.
- [ ] Finish release notes/checklist/security/status/report and atomically regenerate source-code.txt after the final included change. Engineering completion must remain distinct from pending production signing/distribution.

Exact counts, selections, durations, identities, failures and remaining gates: [phase-5-report.json](reports/phase-5-report.json). Reproducible commands and signing handoff: [release-checklist.md](release-checklist.md).
