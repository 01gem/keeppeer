# Phase 2 — privacy closeout and original acceptance

## Window lifecycle and controlled clipboard continuation — current 2026-10-30

**Phase2 remains incomplete for the final affected window verification.** The original-opacity correction has a full API31 real-window pass (1/1,23.728s,shell-653) and actual revealed-draft Recents transition/settled own-card redaction on corrected production app1c3adda7…965dc. Final test APK is6369b611…3e433; the full earlier window pass used test1c2fa358 and is not relabelled. API25 final WindowRegistrationTest failed0pass/1fail/0skip,19.751s at task return with the host CREATED. The final-package API31 rerun never started: automatic review rejected combined wake/menu input as potentially bypassing keyguard without explicit authorization. That action stopped without another route.

**Controlled clipboard verification is resolved.** New explicit “Approve temporary isolation and restore” authorization was executed separately from historical rejections. The sole dedicated API31 client's sharing preference was originally enabled, temporarily disabled, then restored/independently verified enabled. Only the2unchanged approved methods ran:2/2pass,0fail/skip,58.101s,expiry30056ms,app1c3adda7/testfde7e6ce,shell-631. No host clipboard value was read/exported; production clipboard logic and Android permissions/AppOps/security settings were unchanged. Current biometric3/3 and rotation individual passes (shell-626), actual Recents/lifecycle passes (shell-643) retain exact test hashes and their failed containing groups. Final assembly/test assembly/lint passed,0errors14warnings.

See [window correction, native failures, approvals, sharing restoration and current acceptance checklist](phase-2-window-lifecycle-clipboard.md) and authoritative windowLifecycleClipboardContinuation in [the JSON report](reports/phase-2-report.json). Remaining work is bounded API25 task-return diagnosis and one final API31 Window rerun after explicitly approved ordinary disposable unlock. No additional API31 live-card manual observation, full22-test matrix or universal physical/OEM gate is introduced.

Recovery remains closed by the disposable-entry decision; cause unknown, preserved material untouched. The clean Defender full-scan statement is user-reported, exact time unknown/uninspected and not a historical false-positive verdict; protection remained enabled/unchanged. No personal Samsung/wireless-alias operation, preservation access, wipe/reset/rekey or enrollment change. Both dedicated emulators were stopped retaining data. Release disabled; Phase3 unstarted.

The dated sections below retain historical binary/count/approval scopes; their former clipboard/recovery/approval blockers are superseded only as stated above.

## Focused live Recents correction — historical reviewed baseline

**The failing API31 live-card exposure is corrected and verified; Phase2 remains incomplete for controlled clipboard-regression verification.** Public window-focus redaction covers the Activity/app-owned dialogs and hides the IME without changing session expiry or biometric cancellation. Actual Recents regression1/1passed; affected API31 group6passed/2clipboard failures/0skipped (approved biometric3/3passed); API25 compatibility2passed/0failed/1strong-enrollment skip. Assembly/test assembly/lint passed,0errors14warnings. Current hashes, sampled transition/settled UI, real per-use authentication, Home/grace/expiry/Back/screen-lock and protected-dialog PNG evidence are in [focused correction and acceptance checklist](phase-2-live-recents-correction.md) and liveRecentsCorrection in the [JSON report](reports/phase-2-report.json).

Clipboard sharing remains enabled: automatic review rejected disabling that emulator toggle without explicit authorization. An isolated run showed an owned clip label replaced by “host clipboard”; the2checks remain failed/unresolved, with no weakened assertions or indirect setting change. Prior live-card failure and reverted exclusion trial, all earlier counts/artifact identities, disposable-entry recovery closure and the uninspected user clean-scan report remain history. Cause/old Defender classification remain unknown; personal/preserved material untouched; release disabled; Phase3 unstarted.

Earlier verification/status statements below retain their historical artifact scopes where superseded by this section. Standing preservation and security boundaries still apply.

**Outcome: incomplete.** The only remaining original privacy gap is the exposed direct live current-task Recents preview on the disposable API31 provider. Ordinary Android system screenshots now have valid protected-image evidence, including an actual app-owned dialog. This closeout does not call the live-card exposure fixed.

The original request was inspected at `C:/Users/gemdu/Documents/Codex/2026-10-29/gen/outputs/password-keeper-prompts/phase-2-security.txt`. Its implementation clauses1–10 and acceptance bulletsA1–A4 are mapped below. The approved3-test run and recovery-priority decision are resolved; historical Defender attribution and unobserved every-OEM/physical-hardware behavior are not new Android acceptance gates. Release stays disabled for the later release process; Phase3 remains unstarted.

## Scope and source identity

The retained registered AVD `Keeppeer_Guard_API31_20261030_123138` was rediscovered and launched visibly as `emulator-5582`, qemu1/API31/x86_64, boot complete. The existing synthetic-only fixture and ordinary PIN/password UI were used. No new enrollment or vault setup, app-data clearing, uninstall or key regeneration occurred. All device actions used the exact emulator serial. The personal Samsung RR8R402RR7T and its wireless aliases received no deployment, instrumentation, process, enrollment, setup, clearing, vault/candidate/preservation operation.

All78 final app/toolchain source/config files match the reviewed01:44:39 PM PHT/783095-byte source snapshot. The attempted single manifest attribute was reverted; no app/test/build/exporter change is retained. Baseline app SHA-256: `2692954259667faa77710eea0b82ef5d6a69362eb31bcb7d979c4b4cdfb54e3d`. Test SHA-256 remains `cfbc62596ee1e56f5ee3c962a30701443ae3da37e4ee7b7f0abad84976156e1a`.

Rebuilding the restored source produced app SHA-256 `a6c3dfe4f52a13dd3240fe56a33f5839c38c1486132742d13193edf96d9aa061`, independently matched to the final disposable installation. This is a different binary identity; its cause was not investigated and old passes are not relabeled as runs on it. APK Analyzer confirms original MainActivity declaration without exclusion, minSdk24/target37, backup disabled, no INTERNET/broad storage permission. No dependency, KDF, envelope, SQLCipher, session, clipboard or biometric code changed.

## Ordinary screenshot and live UI observations

These actions used Android's system screenshot key event, `adb -s emulator-5582 shell input keyevent KEYCODE_SYSRQ`. The resulting files were saved by System UI, queried as public MediaStore screenshots, pulled from their exact public paths and decoded with the IDE image reader. The desktop observer captured only the selected emulator window to inspect ordinary visible UI. None of the failed UiAutomation/CLI/native screencap paths was replayed; no flag/security setting was disabled.

| Observation | Actual result and identity |
| --- | --- |
| Harmless Display Settings control | PASS for mechanism only. System-produced Screenshot_20261030-084513.png,1080×1920,91793bytes; decoded and visually reviewed, readable generic settings. Settings was launched excluded from recents. |
| Revealed unsaved synthetic draft | PASS for system screenshot protection on baseline app2692954. Live foreground showed revealed synthetic sample before and after the action. System-produced Screenshot_20261030-085313.png,1080×1920,8818bytes decoded successfully: app and keyboard content black, only system navigation remained. This is a valid blank protected image, not a null/empty file or inferred OS refusal. |
| Own direct live current-task Recents | FAIL on baseline app2692954. Two settled live observations showed the synthetic revealed draft and keyboard in Keeppeer's card. This was not just an Activity-flag assertion or screenshot failure. Window metadata retained SECURE; Activity remained RESUMED/visible/focused and current-PID trace had no ACTIVITY_STOP. This identifies the observed live-preview condition, not a definitive universal platform root cause or proof of a persisted cached thumbnail. |
| Actual app-owned Compose dialog | PASS for system screenshot protection on manifest-only trial appd9c8494. Delete-entry confirmation was visibly open; no final Delete action was selected. Screenshot_20261030-091616.png,1080×1920,13894bytes decoded: protected app/dialog black with system status/navigation only. Cancel dismissed it without deleting the synthetic entry. MainActivity/Compose dialog/window code was identical to the baseline and final restored source. |
| Home then Recents during exclusion trial | Own app card absent; a blank own test-harness task area remained and was not opened/browsed. This is distinct from the still-visible direct live-current-task card. It is not relabeled as an observation on the restored original manifest. |

The control and protected files are real nonempty PNGs, each visually inspected. No explicit OS-policy refusal was observed or claimed. Expected secure-window behavior is described in [Android's sensitive-activity guidance](https://developer.android.com/security/fraud-prevention/activities); the result here rests on the actual saved images and live observations.

Only generic/synthetic runtime images exist in Android screenshot storage and host TEMP. No capture, DB, envelope, key, private preservation manifest/payload or credential is included in source-code.txt. The synthetic sample value is omitted from this report. No unrelated app screens or cards were opened for inspection.

## Reverted correction attempt and affected checks

Because of the demonstrated live-card exposure, MainActivity temporarily received `android:excludeFromRecents="true"`, using the [documented public manifest policy](https://developer.android.com/guide/topics/manifest/activity-element#exclude). Trial app SHA-256: `d9c8494a3fe1a7c727d3f19b8ec40f410e5c5ed9c63bb1d1682894d421ff99d5`. The task intent actually contained EXCLUDE_FROM_RECENTS (0x10800000), but the own live locked-app card still appeared. The trial did not establish a fix for the original revealed-draft exposure, although the backgrounded task was absent after Home.

The attribute was removed again. No speculative privacy curtain, lifecycle/authentication change, hidden API or OS setting workaround was retained. The source is back to the exact reviewed guard tree; no guard redesign occurred.

| Check | Result / artifact scope |
| --- | --- |
| Trial debug assembly and lint | PASS via IDE Gradle `:app:assembleDebug :app:lintDebug --console=plain`; appd9c8494. |
| Trial affected rotation/lifecycle group |2 expected/started/passed,0fail/skip,54.630s,OK(2tests),shell-321.log; appd9c8494/testcfbc6259. This is not a recents-privacy pass. |
| Restored-source debug assembly and lint | PASS with same IDE tasks; appa6c3dfe4. Earlier15 JVM tests/14 lint warnings remain historical; no fresh JVM run or fresh warning-count measurement is claimed. |
| Final affected rotation/lifecycle group |2 expected/started/executed/passed,0fail/skip,52.089s,OK(2tests),native code-1/shell exit0,shell-351.log; appa6c3dfe4/testcfbc6259. Full hashes are in the structured report. |

The native command selected only `EntryDraftRotationTest,VaultLifecycleUiTest` with SyntheticTestRunner. It verifies actual recreation/draft/reveal/cancel and background/manual-lock/repository revocation after the package changes. Files/preferences/aliases remain UUID isolated. Closed3/3 biometric and22/22 API24/API25/API31 groups were not repeated. No full-suite or new older-API pass is claimed for the final rebuilt binary. All source/core behavior beyond the reverted manifest attribute remained unchanged.

Earlier recording evidence is retained at its original app/test identity. MainActivity text from the earlier preservation-review source exactly matches final MainActivity, including Activity/legacy DialogFragment secure-window handling. The final retained recording has a readable control,26 protected-draft black frames and one protected-dialog black frame. Those static/short changed-frame timelines do not prove5seconds of independently encoded frames, current live-recents protection, physical buttons or every OEM.

## Original acceptance map

“Covered” refers to the exact retained tests/manual evidence for unchanged source and the new observations above; it is not a blanket full run on appa6c3dfe4.

| Original clause | Requirement | Current assessment | Evidence / limits |
| --- | --- | --- | --- |
| C1–C4 / A1–A2 | Centralized authenticated vault; vetted API24/25 KDF; AEAD metadata/device binding; actual SQLCipher; failure preservation | COVERED | 15 recorded JVM tests including published PBKDF2/Unicode vectors, roundtrip/wrong-password/tamper, fresh salts/nonces/same DB key; current-core22/22 on each API24/25/31 include actual Keystore, SQLCipher wrong-key/plain-SQLite/schema/reopen checks and guard/writes/failures. Exact hashes and times retained in original report objects. |
| C5–C6 | Explicit synthetic legacy reset; confirmed setup/warnings; atomic same-key master-password change | COVERED | Current-core selected native groups include legacy cancel/confirmed isolated reset, validation, password change/restart/wrong password, failed replacement/device key and repository revocation. No real-vault migration/reset. Phase3 export nudge only. |
| C7 / A3 | Secure windows; screenshot/recording/dialog/recents privacy; cold launch, rotation, timeouts, screen/manual lock and secret/repository revocation | PARTIAL — live current-task Recents exposure | Ordinary system screenshot valid readable control and valid black protected image; window source/FLAG_SECURE unchanged; prior native recording frames retained with original artifacts/short timelines. Actual API25 real1/5minute/Home/suspended process/death/Back/screen-lock evidence plus current-core native timeout/guard/UI checks; rotation/lifecycle tests additionally passed on attempted manifest-only artifact. Own settled live Recents currently exposed synthetic revealed draft; no blanket privacy pass. |
| C8 / A3 | Central sensitive clipboard,30s expiry, background/lock clearing/retry, newer unrelated replacement preservation | COVERED with platform-access limits | Retained actual touch Cut, injected selection/Ctrl+C/expiry, ownership/sensitive-flag tests; API25 controlled AppOps denial and foreground retry preserved newer unrelated Settings clip. No physical hardware-keyboard result or naturally observed every-OEM denial policy claimed. |
| C9 / A3 | Per-use real CryptoObject biometric wrapping/unlock and password fallback under errors/invalidation/disable/re-enable | COVERED with provider/artifact limits | Explicit approved3/3 at143.002s remains closed; actual API31 per-use encrypt/decrypt/marker completion, genuine enrollment1→2 KeyPermanentlyInvalidatedException and retained entry/password fallback. Older API25 native failed-scan/retry/lockout/cancel/rotation/Home/screen-lock/disable-reenable evidence retained; API25 non-invalidation remains a provider limit, not relabeled pass. |
| C10 / A3 | In-memory-only sensitive drafts; no saved-state restoration; authenticated saved-entry IDs and Phase3 picker plan | COVERED | Current-core rotation/reveal/cancel and lifecycle revocation plus actual historical API25 process-death/unsaved-draft/screen-lock checks; source saved-state boundary unchanged. Picker behavior is documented future Phase3 plan, not a Phase2 feature or new test gate. |
| A4 / constraints | Relevant tests, debug assembly/lint; minSdk24/offline/backup/permission constraints; honest limits and disabled release | COVERED, phase remains incomplete for named privacy gap | Original current-core host assembly/15JVM/lint0errors14warnings retained; manifest-attempt debug assembly/lint and affected2/2 passed separately. No dependency/KDF/auth/source algorithm changes; no full rerun. Release disabled per current user instruction; Phase3 unstarted. |

## Closed decisions and security history

The three explicitly approved BiometricEnrollmentTest methods remain3/3 passed,0failures/skips,143.002s,shell-254.log on their original app2692954/testcfbc6259. Older automatic-review rejections are retained history, not a current approval blocker. Actual API31 per-use enrollment, marker-less biometric database opening, genuine key invalidation and retained password/entry fallback remain separately scoped evidence.

The user confirmed that the unavailable original entries were disposable test entries. Recovery is no longer requested. Cause remains unknown; no restoration, deletion, reset or rekey authority follows. All preserved files and historical incident evidence remain untouched.

The current user instruction reports that, after the01:44:39 PM export, the user said in the originating review chat on2026-10-30: **“Full scan finished; no threats or new alerts.”** Exact time is unknown. This was not independently inspected and is not a false-positive verdict on the historical Defender detection. No Defender allow/restore/exclusion/protection setting changed and no encoded/security-blocked command was replayed. Historical classification uncertainty is separate from Android functional acceptance.

## Exact remaining work and practical observation

**Required open item: resolve and verify redaction of the own direct live current-task Recents preview from a revealed synthetic draft.** The API31 observation is a real exposure, not unavailable inspection; screenshots and dialog protection do not close it.

A small next manual comparison can scope the provider behavior: on a separate explicitly disposable device/provider, reveal a synthetic draft, press Recents directly and record whether its own card shows content; compare Home then Recents and report the device/API and which path was used. A different-provider pass does not erase the recorded API31 failure. Verification after a reliable correction must repeat that specific case; a new broad matrix or generic capture retry is not requested.

Physical hardware-keyboard, physical screenshot-button and every-OEM behavior remain unobserved compatibility limits, not newly invented universal Phase2 gates. Do not use the personal Samsung or real passwords for this observation. No extra scan report or historical attribution verdict is required to pass an Android function.

One metadata-only MediaStore query initially rejected a spaced sort argument despite exit0; the corrected query succeeded without another screenshot action. Initial black live frames were an asleep/locked emulator; normal wake/unlock restored observation without changing settings. These collection limits are separate from the actual recents failure. The AVD was stopped cleanly after checks, with its files/keys/registration retained.

The fresh atomic source export must therefore say **Phase2 incomplete**, with this named gap only, release disabled and Phase3 unstarted.
