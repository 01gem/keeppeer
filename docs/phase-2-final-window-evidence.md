# Final Phase2 window evidence — 2026-10-31

Phase2 complete for the original requirements with the recorded provider/artifact limits. Release disabled; Phase3 unstarted. This final task corrected the WindowRegistrationTest return driver, restored an explicit same-window assertion omitted by the pre-existing dirty edit, and updated documentation. Production unchanged.

## Retained API25 pass — no rerun

The actual retained command/result is in:
C:/Users/gemdu/.android/studio/agent/conversations/Keeppeer-3e303456340a7b8dc0dbedcd81c5b1/5ede27b9-7705-4984-9f8a-26fabf065e86/steps.jsonl
line107, ToolCallStep c428bac8-5cc1-45d0-82a6-e5c6b15cac42.

Anchored IDE grep checks against that exact record confirm explicit serial emulator-5554, am instrument -w -e class, com.gem.keeppeer.WindowRegistrationTest#hiddenDialogStopStartRestoresOriginalAlphaAndClosingStaysRedacted, and com.gem.keeppeer.test/com.gem.keeppeer.SyntheticTestRunner. The retained native result is Time:7.379, OK (1 test), shell exit0. Original-alpha-restored, closing-stays-redacted, recreated-visible and dismissed-parent-visible milestone strings are retained. Count:1 expected/started/executed/passed,0 failed/skipped, derived from the complete native single-method result and final milestones. Raw streamed start statuses/final instrumentation code were not retained in this -w record; final native code is unknown.

The same retained session line212, ToolCallStep call_797317, records :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug and “Build finished successfully”. This is later session build evidence, not a hash attestation for the API25 run. The registered fixture is Keeppeer_Guard_API25_20261030_123138; the serial5554 belongs to that earlier session, not the fresh API31 fixture that later reused the port.

No contemporaneous per-run app/test SHA-256 or installed-byte hash attestation was located. Keep those structured values null. The current host app 1c3adda7dd80d515b8ec64e387a77d64d26b6da3bc261961dd007dc98a4965dc, pre-API31-correction test e56bba1e4c4b6a9b555162cabaff909b477a5c683a88a99df64d48c104d62033, and pre-correction WindowRegistrationTest source7b0c7d0706a6dde67c1cf6081f9941933ad5e79c639aa1dd0dc334d15c566e81 were measured now; they are not retrospectively assigned to the retained API25 run. In particular test6369b611 remains the historical failed shell-701 artifact.

Studio read_file cannot serialize application/json-lines. Fields above were verified through anchored IDE grep predicates on the exact retained records; full raw JSONL was not exported. Earlier failed API25 records are unchanged. No API25 native run occurred in this task.

## New disposable API31 provisioning and identity

Created without force/overwrite after checking both registration and directory absence:
Keeppeer_Guard_API31_FinalWindow_20261031_d72faa02.
Registration/data: C:/Users/gemdu/.android/avd/Keeppeer_Guard_API31_FinalWindow_20261031_d72faa02.avd.
Image: system-images;android-31;google_apis;x86_64 revision12, pixel_2.
Emulator37.2.12.0 build16428233; WHPX, SwiftShader,2cores. Requested1536MB; emulator increased RAM to2048MB. Started with -no-snapshot -no-audio. Existing registrations were not reused or wiped.

Discovered serial emulator-5554. emu avd name and ro.boot.qemu.avd_name match the new registration; ro.kernel.qemu=1, SDK31, x86_64, sys.boot_completed=1. Observed Pixel is starting, then the normal default launcher. Trust/window metadata: deviceLocked=0, showing=false, inputRestricted=false. No wake/swipe, PIN, fingerprint, normal master-vault setup or lock-setting change was needed.

Creation/start/identity/install/hash evidence: C:/Users/gemdu/.android/studio/agent/conversations/Keeppeer-3e303456340a7b8dc0dbedcd81c5b1/7320c63d-6ff1-45f7-9b53-1c02acab3c4c/session/agent/shell-logs/shell-64.log, shell-66.log, shell-73.log, shell-75.log, shell-76.log, shell-79.log. All installations and instrumentation used explicit -s emulator-5554 after the target guard. Installed runner registration is com.gem.keeppeer.test/com.gem.keeppeer.SyntheticTestRunner (target=com.gem.keeppeer); its SyntheticTestApplication/UUID vault context remains unchanged.

## Initial failed check and smallest correction

shell-77.log:1 expected/started/executed,0 passed/1 failed/0 skipped,39.233s; FAILURES!!! Tests run:1, Failures:1; native code-1, shell exit0. App 1c3adda7dd80d515b8ec64e387a77d64d26b6da3bc261961dd007dc98a4965dc, test e56bba1e4c4b6a9b555162cabaff909b477a5c683a88a99df64d48c104d62033. It reached SAME_WINDOW_STOPPED, then returnToOwnActivity:96 timed out with Activity RESUMED.

The driver waited only for host.hasWindowFocus even while the actual DialogFragment dialog owned focus. The correction keeps NEW_TASK|REORDER_TO_FRONT and waits for focus on the original host or its actual tagged dialog. The subsequent exact same-window,0.8 restoration,hidden-closing,parent visibility,recreation/new-window and secure-flag assertions remain unchanged. It neither forces opacity nor starts a substitute Activity to simulate the expected window.

Required test-only rebuild: :app:assembleDebugAndroidTest --console=plain, shell-78.log, BUILD SUCCESSFUL in1m3s,56 actionable/5 executed/51 up-to-date. Production Kotlin tasks were up-to-date; production APK bytes unchanged. Prior assembly/lint0errors14warnings stays at its recorded earlier scope. IDE inspection found no errors (style/unused-import warnings only).

## Intermediate pass and final assertion audit

The owned-focus correction first passed in shell-80:1/1,20.875s,OK (1 test),app1c3adda7dd80d515b8ec64e387a77d64d26b6da3bc261961dd007dc98a4965dc,testffc24ad18488906e977a6271c7f11d00b8ee3813e389e1e9dc56acfd22943f36,Window source731f35b0d60e31a7091baed506ec46a348125e1e16eda0e05c6bcdc06b214ffc. That intermediate result is retained at its own identity.

Final Git diff review found the pre-existing dirty edit had removed explicit assertSame(original,dialogWindow()) immediately after task return. It was restored, preserving all original explicit same-window assertions in addition to original Window focus/alpha checks. Required test-only rebuild shell-83 passed in11s,56 actionable/5 executed/51 up-to-date. Final test 40305e7296b7f9966ca3d98903fa588997bc9f568b9b44cda9ffc646c9d373d0; Window source c4ab7c965e58373f22c8cbecc4bf111e9622252b21328aefd1a802a3ad3fb673.

The same newly created AVD was restarted after its first guarded stop; no second AVD, overwrite,wipe or lock change. It again reached the unlocked default launcher without wake/swipe/credential input. shell-89 executed1test and failed0pass/1fail/0skip in26.858s at initial awaitWindow:115 before ORIGINAL_VISIBLE (native code-1,shell exit0). Immediate screen inspection showed the foreground System UI isn't responding ANR dialog holding focus. Underlying System UI ANR cause is unestablished; this is not a demonstrated Keeppeer product defect. A single ordinary tap selected the observed Wait action with explicit new-AVD guard (shell-91); subsequent UI inspection confirmed the focused launcher. No code,assertion,opacity or security change followed. The exact unchanged affected check then passed.

## Final affected API31 pass

Exact invocation after AVD/qemu/API/ABI/boot guards:
```text
adb -s emulator-5554 shell am instrument -w -r -e class com.gem.keeppeer.WindowRegistrationTest#hiddenDialogStopStartRestoresOriginalAlphaAndClosingStaysRedacted com.gem.keeppeer.test/com.gem.keeppeer.SyntheticTestRunner
```

Revision: c0d0c44588bd7d48e9fda4f0df86368ea7e2587b plus dirty work.
Production app SHA-256: 1c3adda7dd80d515b8ec64e387a77d64d26b6da3bc261961dd007dc98a4965dc (32091251bytes).
Final test SHA-256: 40305e7296b7f9966ca3d98903fa588997bc9f568b9b44cda9ffc646c9d373d0.
WindowRegistrationTest.kt SHA-256: c4ab7c965e58373f22c8cbecc4bf111e9622252b21328aefd1a802a3ad3fb673.
PrivacyWindows.kt unchanged: e84f930002f9249590bc6af648e45d4bdbd5ab0aab19aa68a8dd4fb63121789d.
LiveRecentsPrivacyTest.kt unchanged: da8ce82b4ec684939591686ba58b9c0e271779df34d923da018e02ec5f4abbed.
Host/installed app and test SHA-256 match (shell-88.log).

Native log: C:/Users/gemdu/.android/studio/agent/conversations/Keeppeer-3e303456340a7b8dc0dbedcd81c5b1/7320c63d-6ff1-45f7-9b53-1c02acab3c4c/session/agent/shell-logs/shell-92.log.
Invocation start2026-10-30T20:29:37.4418245Z; completion2026-10-30T20:30:00.6053277Z (2026-10-31 04:30:00 AM PHT).
1 expected/started/executed/passed,0 failed/skipped; native duration15.781s.
Start status1, method completion status0, final OK (1 test), INSTRUMENTATION_CODE:-1, ADB_EXIT=0. This is a complete native pass, not shell-exit-only evidence.

Completed native steps:
- ORIGINAL_VISIBLE
- RECENTS_REDACTED
- SAME_WINDOW_STOPPED
- RETURN_FOCUS_HOST_false_DIALOG_true
- ORIGINAL_ALPHA_RESTORED
- CLOSING_STAYS_REDACTED
- RETURN_FOCUS_HOST_true_DIALOG_false
- PARENT_RETURNED
- BEFORE_RECREATE
- AFTER_RECREATE
- RECREATED_VISIBLE
- DISMISSED_PARENT_VISIBLE

The observed HOST_false_DIALOG_true at the first return confirms the exact fixture diagnosis; HOST_true_DIALOG_false after hidden dismissal confirms the parent returned. All remaining lifecycle/privacy assertions then completed.

Only the newly verified AVD was stopped with an AVD-name guard and explicit serial (shell-93.log: OK killing emulator; launcher process exited0). Its registration/data remains. No AVD was deleted.

## Carried acceptance and boundaries

C1–C6 and C9–C10 guard/authentication/crypto/native evidence, original selected22-test matrices, approved biometric results, and historical screenshot/recording/manual lifecycle scope remain closed at their original identities. Controlled clipboard2/2 in58.101s (shell-631,testfde7e6ce) and enabled→disabled→enabled restoration remain unchanged. C7/A3 and A4 final affected window gap is now closed by the retained API25 pass plus this complete fresh API31 run. API25 hash-attestation and hardware/provider limits are documented, not invented passes or new universal hardware gates.

The retained API31 AVD's newer blocker was unknown credential/FBE after cold boot, not a new unauthorized-input attempt in this task or a new automatic-review rejection. It remained stopped and untouched. The earlier combined wake/menu rejection and original artifact identity remain historical; that rejected command was not replayed or disguised. The fresh run does not unlock/recover the old AVD.

No clipboard/BiometricEnrollmentTest/full matrix/KDF/SQLCipher rerun, capture journey, personal Samsung/wireless operation, candidate/preservation access or prior-data recovery occurred. Recovery remains closed for disposable entries; cause unknown. Defender/platform security unchanged; no exclusions, detection restore/allow or bypass. The clean Defender scan remains uninspected user testimony, not a historical false-positive verdict.
