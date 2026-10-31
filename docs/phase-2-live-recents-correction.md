# Phase 2 — focused live Recents correction

## Final window verification — current 2026-10-31

**Phase2 is complete for the original required criteria, with the recorded provider/artifact limits.** The retained API25 WindowRegistrationTest passed 1/1 in 7.379s. Its actual command/result is retained in session 5ede27b9 steps.jsonl:107 (tool call c428bac8-5cc1-45d0-82a6-e5c6b15cac42); no contemporaneous per-run APK hash attestation was located, so current hashes are not assigned retrospectively.

The final affected API31 method passed on the new separately registered Keeppeer_Guard_API31_FinalWindow_20261031_d72faa02, explicit serial emulator-5554: 1 expected/started/executed/passed, 0 failed/skipped, 15.781s, OK (1 test), native code -1, shell exit 0 (shell-92.log). App SHA-256 1c3adda7dd80d515b8ec64e387a77d64d26b6da3bc261961dd007dc98a4965dc; final test 40305e7296b7f9966ca3d98903fa588997bc9f568b9b44cda9ffc646c9d373d0; WindowRegistrationTest source c4ab7c965e58373f22c8cbecc4bf111e9622252b21328aefd1a802a3ad3fb673. Host and installed APK hashes match. Final diff review restored the explicit post-return same-window identity assertion omitted by the pre-existing dirty edit. The first API31 attempt failed in the host-only focus wait (39.233s); the smallest fixture correction recognizes focus on its actual owned dialog or original Activity. The rerun observed dialog focus, then parent focus, and passed every unchanged same-window, alpha0.8, hidden-closing, secure-flag, parent-return and recreation assertion. Production code is unchanged.

The new fixture reached its default launcher without credential/wake/swipe input; qemu1/API31/x86_64/boot complete and unlocked state were verified. Only this new AVD was stopped; registration/data retained. The retained API31 fixture remains FBE/keyguard locked with an unknown synthetic credential. That newer blocker is an unknown-credential provisioning constraint, not a new unauthorized-input attempt or new review rejection. The earlier combined wake/menu rejection remains historical and was not replayed. The fresh run does not unlock or recover the retained AVD.

Controlled clipboard 2/2 (58.101s, shell-631, test fde7e6ce) and restored enabled sharing, approved biometric/guard/native results, original matrices and capture/lifecycle evidence retain their exact original scopes. No broad reruns, opacity/Recents redesign or universal hardware gate. Recovery stays closed for disposable entries; personal/candidate/preservation material and Defender/platform security untouched. Clean scan remains user-reported; old detection classification unknown. **No required Phase2 gap remains. Release disabled; Phase3 unstarted.**

See [final evidence and identity limits](phase-2-final-window-evidence.md), [window/clipboard acceptance checklist](phase-2-window-lifecycle-clipboard.md) and currentApi25WindowVerification/finalFreshApi31Verification in [the report](reports/phase-2-report.json). Earlier dated sections below are historical where superseded; their original failures and artifact identities remain unchanged.

## Historical live Recents correction at the reviewed baseline

**Live Recents correction verified on the failing disposable API31 provider. Phase2 remains incomplete for the current clipboard-regression verification limitation.** This continuation supersedes the previous privacy closeout only where new evidence is stated below. Historical failures, binaries and test counts retain their original identities. Release remains disabled; Phase3 is unstarted.

## Actual transition and correction

Retained registered disposable AVD: Keeppeer_Guard_API31_20261030_123138, rediscovered as emulator-5584, qemu=1, API31, x86_64. Identity checks preceded device actions. Only synthetic fixtures were used. The personal Samsung and every wireless alias, candidates and preservation material were excluded.

A temporary debug-only, 200-event maximum trace recorded elapsed-realtime order, lifecycle, focus, top-resumed, shown and secure metadata. It never logged field contents, credentials, keys or exception/object dumps. The uncorrected trace APK was d49cf164f5c4b1729b7c504337c55a68bea7446f84830bfe3853350c6bc10eb2.

From the revealed unsaved synthetic draft with IME open, the visible Android Recents button produced FOCUS_LOSS at elapsed t_ms=906786, sequence5. Lifecycle remained RESUMED, top=true, shown=true, secure=true; no PAUSE, STOP, USER_LEAVE_HINT or TOP_RESUMED=false arrived. Four ordinary live-window observations at 10:15:27.568–10:15:28.268 UTC showed the settled own live card exposing draft/sample/keyboard. Returning to the own card produced FOCUS_GAIN, sequence6; KEYCODE_APP_SWITCH then produced the same focus-only loss, sequence7. The prior metadata's focused=true observation therefore did not identify the useful transition callback; the bounded event trace did.

A minimal window-alpha/IME trial on focus dispatch hid the settled live own card and keyboard, with four observations at 10:30:21.749–10:30:22.239 UTC. This trial was preliminary, not final transition-frame proof. Its retained log used a constant privacy=UNCHANGED trace label, so that label is not a measurement of the trial's resulting alpha.

The retained implementation registers the Activity and app-owned Compose/DialogFragment windows. When none has input focus it sets each registered window's public alpha to0 and requests IME hiding during focus dispatch. Any owned window gaining focus restores the original window alpha. No timer or waiting for Compose recomposition drives redaction. Dialog registration preserves FLAG_SECURE. The composition and in-memory draft ViewModels remain alive. Session timeout/expiry, screen-lock handling and native biometric ownership/cancellation remain on their previous paths; loss of focus alone never locks or cancels authentication.

Public APIs: [window-focus listener](https://developer.android.com/reference/android/view/ViewTreeObserver.OnWindowFocusChangeListener), [whole-window alpha](https://developer.android.com/reference/android/view/WindowManager.LayoutParams#alpha), [Activity/window-focus behavior](https://developer.android.com/reference/android/app/Activity#onWindowFocusChanged(boolean)). Documentation establishes the public mechanism, not provider rendering proof. The trace and actual live observations determine this provider's behavior.

Temporary tracing was removed before the retained build. No task-exclusion attribute, hidden API, permission/dependency change, screen-security bypass, persisted secret or authentication callback fabrication was introduced.

## Retained identities and checks

- App SHA-256: f74c15d03e76d04da4a3e6d30b58fe77a54114fd95caf8404010cc18b674dd37.
- Test SHA-256: 691f1b4c0144a70e5badfa1d3a016dccb3e7ce2d62aea1193271c66897302992.
- IDE Gradle :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug --console=plain: PASS; lint XML0errors,14warnings.
- New actual-Recents native regression:1expected/started/executed/passed,0failures/skips,45.339s,OK(1test),native code-1,shell exit0,shell-439.log, API31/emulator-5584.
- The new regression invokes real KEYCODE_APP_SWITCH from a revealed draft, verifies redaction while the API31 Activity remains RESUMED and the same session remains open, returns with real Back, checks retained draft, then repeats actual Recents/Back with the real Delete entry confirmation. It cancels without deleting that entry. This is event/visibility regression evidence; it is not pixel proof by itself.
- SyntheticTestRunner registration targets com.gem.keeppeer; SyntheticVaultContext uses UUID files/preferences/Keystore alias namespaces. Native runs select exact classes/methods with the explicit emulator serial, never an unqualified connected-device task.

## Affected run and clipboard isolation limit

The selected API31 group ran8tests:6passed,2failed,0skipped,226.910s,FAILURES!!!,native code-1,shell exit0,shell-440.log. All3approved BiometricEnrollmentTest methods, EntryDraftRotationTest, VaultLifecycleUiTest and the monotonic all-timeout/device-lock check passed. Both selection-copy/expiry and real touch-cut failed the central ownership-label assertions; no pass is claimed.

Isolating only those2checks with unchanged app/test bytes again yielded0passed,2failed,0skipped,35.997s,shell-442.log. In that run an initially correct Keeppeer ownership label was replaced by the fixed label “host clipboard” before the1second assertion. This is evidence consistent with emulator host clipboard synchronization interfering, not a definitive exclusion of every app defect. Emulator Extended Controls visibly showed Enable clipboard sharing enabled. No host clipboard value was inspected or exported.

Automatic approval review rejected disabling only that emulator sharing toggle: “Disabling emulator host clipboard sharing changes a security/privacy-related environment setting that the user did not explicitly authorize and may fall under the task’s prohibition on OS security changes; the failed tests should instead be reported or isolated without mutating that setting.” The setting remained enabled, confirmed visually. Approval was requested explicitly; no indirect command, restart flag, headless substitution or test-assertion weakening was used to evade the rejection. A further already-issued2test run remained under unchanged conditions:0passed,2failed,0skipped,29.975s,shell-445.log; the clipboard extras check also encountered null metadata. All failed attempts remain recorded.

## Final live UI observations (API31)

On final appf74c15d0, the same normal synthetic vault was unlocked by its ordinary password UI. The installed app and test SHA-256 independently matched the host hashes via the public installed APK paths. A revealed unsaved synthetic draft with IME open was directly observed.

The actual visible Recents navigation button was clicked at10:53:22.854UTC. Six immediate bounded live-window frames covered10:53:22.990–10:53:24.812UTC: the app initially remained normally full-screen; the final frame showed its entire app surface redacted to wallpaper while the keyboard was beginning to hide, before the reduced card appeared. Six later settled observations at10:53:50.257–10:53:51.049UTC showed the own live current-task card containing wallpaper only, with no draft, revealed value or keyboard. No unrelated card was selected or browsed. A sliver of the own blank instrumentation-harness card was outside the evaluated own-app card. These are sampled ordinary live observations, not a claim of every compositor frame or every OEM. Back restored the same permitted in-memory draft and revealed sample without a permanent blank surface.

With the real Security settings1minute timeout selected, Home then Recents showed the own backgrounded card blank, distinct from the wallpaper live card. Returning to that own card within44.378seconds preserved the open settings session. The actual background trace recorded STOP/SESSION_BACKGROUND at10:57:14.322UTC and SESSION_GENERATION at10:58:14.390UTC (60.068seconds). Return showed Keeppeer locked. Immediate was restored in the normal synthetic app's settings afterward.

The real Delete entry confirmation remained normally readable with its underlying app before Recents. Direct APP_SWITCH showed only darkened wallpaper in the own settled card, without app/dialog content. Back restored the same dialog; two Back actions cancelled it and returned to the list with the synthetic entry still present. No final deletion was selected.

Ordinary KEYCODE_SYSRQ with that real dialog open produced MediaStore image48, Screenshot_20261030-110141.png,1080×1920,13169bytes. Its exact public path was pulled to host TEMP and decoded by the IDE image reader: app and dialog area were black, system status/navigation remained. This is a valid protected PNG, not an OS refusal or generic/null error. The same provider/mechanism's earlier91793-byte readable generic Display Settings control remains evidence for the control, with its original app/device identity; it is not relabelled as a new control capture. No failed capture API was retried.

The old synthetic biometric wrapper reported KeyPermanentlyInvalidatedException at DECRYPT_INIT and offered normal master-password fallback. Password unlock retained the existing synthetic entry. In the normal app, only that disposable wrapper was disabled/re-enabled using the ordinary confirmation form and the2existing synthetic fingerprint templates; no OS enrollment change occurred. A real per-use prompt authorized encryption: PROMPT_SUCCEEDED/CIPHER_BINDING, APPROVAL_CHECK/SESSION_CHECK/FOREGROUND_CHECK, ENCRYPT_AAD/ENCRYPT_FINAL/WRAPPER_WRITE/COMPLETE at11:05:59UTC. Manual lock, prompt cancellation (code10), retry with an unmatched template (PROMPT_FAILED), then the existing enrolled template produced CIPHER_BINDING, DECRYPT_AAD/DECRYPT_FINAL/DATABASE_OPEN/COMPLETE by11:08:33.824UTC. The retained synthetic entry was visible; no black-screen or cancellation loop appeared. These are real emulator sensor events and actual Keystore operations, not fabricated authentication callbacks or physical-hardware claims.

A subsequent screen-off/wake/normal-PIN sequence was attempted, but the immediate accessibility inspection did not establish the final Keeppeer screen before the AVD was stopped. That attempt is not a fresh screen-lock pass or a demonstrated secret exposure. Historical actual API25 screen-lock evidence and the current all-timeout/device-lock native pass remain separately scoped; source screen-off/session expiry logic is unchanged. This observation limit is retained honestly.

The API31 AVD was stopped with its exact verified emulator serial; registration, userdata, templates and preserved material were not wiped/reset. A later host inventory briefly listed the personal wireless alias; no command targeted it. A usage-limit failure in automatic approval review prevented the first API25 identity action from executing. After the limit's stated retry time and the user's continuation, the actual API25 serial/AVD/qemu/API/ABI were verified again before proceeding. This was an approval-service availability failure, not a security verdict, and did not authorize changing the rejected clipboard setting.

## Focused older-API compatibility

The registered dedicated synthetic Keeppeer_Guard_API25_20261030_123138 AVD was independently verified as emulator-5586,qemu1/API25/x86_64,boot complete. Its older image exposes the AVD property through ro.kernel/qemu naming rather than ro.boot; the console name and guarded API/ABI checks were used. The same app/test APKs were installed directly, and SyntheticTestRunner's target registration was checked.

Only rotation, lifecycle and the approved enrollment-dialog method were selected:3expected/started,2passed,0failed,1skipped,22.008s,shell-564.log,native code-1,shell exit0. The runner prints OK(3tests), but status-4 explicitly skipped the enrollment-dependent method: “Strong biometric enrollment required for the settings button.” It is not a third pass or legacy-dialog observation. No OS enrollment/setup was added to manufacture availability. The2passes establish retained draft/reveal reset and immediate/manual lock/repository revocation with the new focus registration on API25. They do not claim older-API live-card pixel protection or current legacy-prompt coverage. Historical real older-provider authentication evidence remains separately scoped. The exact API25 AVD was stopped without wipe/reset.

## Final screen-lock observation and acceptance map

The same registered API31 AVD was rediscovered again as emulator-5588, qemu1/API31/x86_64/boot complete. Initial device-offline during boot and asleep keyguard frames were collection states; normal wake/menu/PIN input restored the fixture without changing any protection setting. The existing biometric wrapper opened the same synthetic entry again through actual DECRYPT_FINAL/DATABASE_OPEN/COMPLETE at13:38:45.782UTC.

With the app's1minute grace visibly selected and checked, actual screen off/wake and normal device unlock returned directly to Keeppeer locked. The first live return image established the locked screen before the grace interval could expire; a subsequent accessibility confirmation was recorded at13:40:37.204UTC,53.838seconds after the pre-action timestamp13:39:43.366UTC. Safe stages show session generation during the screen-lock sequence at13:39:48–51UTC. Back then reopening the own app still showed Keeppeer locked with no old entry. This completes the fresh screen-lock observation; it does not relabel the earlier inconclusive immediate inspection as a pass. The exact emulator-5588 was stopped, without wiping registration, userdata, templates or vault material.

| Original acceptance | Current coverage |
| --- | --- |
| C1–C4 / A1–A2: encrypted vault, KDF/device binding, SQLCipher and failure preservation | Retained15JVM and selected22/22 per API24/25/31 at original hashes/scopes; these algorithms/files were unchanged and not broadly rerun. |
| C5–C6: explicit synthetic legacy reset and same-key password change | Retained exact earlier tests; initialization/envelopes/password-change source unchanged. No personal reset/migration. |
| C7 / A3: secure windows, screenshots/recording/dialogs/Recents, lifecycle/timeouts/lock | Live own-card gap resolved on failing API31 using focus-driven window redaction, with transition/settled live samples and actual-Recents regression. New protected dialog PNG, Home/control/return/Back/minute-grace/expiry/screen-lock observations; current rotation/lifecycle pass on API31 and API25. Prior screenshot control/recording and real5minute checks retain original scope/limits. |
| C8 / A3: clipboard ownership/sensitive flag,30s expiry, newer-clip preservation | Historical coverage retained, but current2clipboard regressions fail with enabled emulator host sharing; one trace showed replacement by “host clipboard”. Controlled rerun remains outstanding pending explicit approval for that emulator toggle. No app/clipboard fix, weakened assertion or pass manufactured. |
| C9 / A3: real per-use CryptoObject authentication/cancel/retry/fallback | Current API31 approved3/3 and real normal-app encrypt/decrypt/cancel/unmatched-scan/retry/entry-retention pass. API25 enrollment-dependent check skipped for unavailable strong enrollment; prior real legacy/provider evidence remains historical. No new enrollment setup or universal hardware claim. |
| C10: in-memory drafts/reveal reset/process death and future picker plan | Current rotation/recreation/cancel/lifecycle passes plus direct-Recents permitted draft retention. No secret saved-state/navigation/preference change. Earlier process-death evidence and Phase3-only picker plan carried forward. |
| A4 / constraints | Current app/test assembly and lint PASS,0errors14warnings. Relevant native results retain failures/skips; full matrix/KDF/SQLCipher history not rerun. minSdk24, offline permissions/backup policy and release restriction unchanged. |

### Closeout checklist

- [x] Reproduce the actual direct live-current-task failure and identify an observed public signal.
- [x] Retain a small visual policy covering Activity/app-owned dialogs and IME; preserve session/authentication decisions.
- [x] Verify the failing-provider transition/settled own card, return, real dialog, Home control, grace/expiry and screen lock.
- [x] Add and pass an actual OS Recents regression; pass relevant rotation/lifecycle and approved biometric checks on API31.
- [x] Run bounded older-API compatibility checks; record2passes and1unavailable-enrollment skip honestly.
- [ ] Establish the two current clipboard regressions under a controlled emulator sharing condition; explicit toggle approval is pending.
- [x] Keep incident/recovery/Defender history separate; retain failed/reverted trials and exact binary identities.
- [x] Keep release disabled, Phase3 unstarted and personal/preserved material untouched.

The only current required verification blocker is the clipboard regression isolation. The practical next action is explicit approval to disable only the emulator's Enable clipboard sharing option and rerun the same2unchanged checks; if sharing must stay enabled, keep their failure/limitation recorded rather than claiming a pass. No new physical/every-OEM or historical Defender verdict gate is introduced. No live-Recents manual observation remains outstanding on the tested API31 provider.

## Final source/report integrity

All79 IDE-readable existing app/toolchain files were compared to the latest reviewed snapshot using its sequential inventory/file boundaries. Only MainActivity, VaultRoot and EntryDetailScreen differ, as intended; the privacy helper and actual-Recents test are the2new Kotlin files. MainActivity exactly matches the source used for the retained app build, and both APK hashes were rechecked unchanged. The remaining Windows wrapper read was unavailable through the IDE's MIME serializer; it was not edited, and the unchanged atomic exporter includes its full eligible text. JSON parsing,15unchanged historical top-level objects, all68original check entries and git diff --check were verified. Current failure/skip histories remain intact. The final export follows all source/documentation changes.

## Preserved history and boundaries

Earlier reviewed app2692954259667faa77710eea0b82ef5d6a69362eb31bcb7d979c4b4cdfb54e3d, failed/reverted manifest-only triald9c8494a3fe1a7c727d3f19b8ec40f410e5c5ed9c63bb1d1682894d421ff99d5 and restored-source rebuilt appa6c3dfe4f52a13dd3240fe56a33f5839c38c1486132742d13193edf96d9aa061 remain distinct. Their full reports and failures remain in privacyCloseout. Binary reproducibility was not investigated.

The earlier selected22/22 groups on each API24/25/31, the approved3/3 run at143.002s on original app269/testcfbc62596ee1e56f5ee3c962a30701443ae3da37e4ee7b7f0abad84976156e1a and historical actual per-use/invalidation/password fallback remain scoped to their original artifacts; no broad matrix or closed KDF/SQLCipher rerun was requested.

The original entries were disposable test entries: recovery is no longer requested, cause unknown, no restoration claim or deletion/reset/rekey authorization. Preserved material was not accessed or changed. The user reported on2026-10-30, exact time unknown, “Full scan finished; no threats or new alerts.” This was not independently inspected and does not classify the old Defender detection. Defender remained enabled; no detection restore/allow, exclusion, protection change or blocked/encoded-command replay occurred.

Physical screenshot buttons, physical keyboards and every-OEM behavior remain bounded compatibility limits, not universal acceptance gates. Injected input is not physical evidence. Prior unavailable capture paths were not retried. All runtime captures, databases, envelopes, keys, credentials and preservation material remain excluded from the source handoff.
