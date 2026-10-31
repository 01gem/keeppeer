# Phase2 — window lifecycle and controlled clipboard verification

**Phase2 is complete for the original required criteria, with recorded provider/artifact limits.** Retained API25 window regression:1/1 pass,7.379s; final fresh API31 window regression:1/1 pass,15.781s,OK (1 test),all original lifecycle/privacy assertions intact. Release disabled;Phase3 unstarted.

This continuation starts from the reviewed 2026-10-30 09:53:49 PM PHT (UTC+08:00), 918708-byte snapshot, revision401d359 plus dirty work. All historical failures, skips, approvals/rejections and artifact identities remain in the structured report. They are not relabelled as current passes.

## Final affected verification — 2026-10-31

The retained API25 native command/result was located in session5ede27b9 steps.jsonl:107, tool call c428bac8-5cc1-45d0-82a6-e5c6b15cac42: explicit serial emulator-5554, exact WindowRegistrationTest method and SyntheticTestRunner,Time7.379,OK (1 test),final recreation/parent-visible milestones,shell exit0. The later same-session build record at line212/call_797317 says Build finished successfully for app/test assembly/lint. No contemporaneous per-run APK or installed-byte hash attestation was located; those API25 structured hashes stay null. Current host hashes are not retrospectively assigned. API25 was not rerun.

Created one fresh registered API31 x86_64 fixture Keeppeer_Guard_API31_FinalWindow_20261031_d72faa02 without force/overwrite/wipe. Exact serial emulator-5554 was discovered and guarded by registered AVD name,qemu1,API31,x86_64,boot complete. The first-boot screen reached the default launcher without wake/swipe/credential input;deviceLocked0/showingfalse/inputRestrictedfalse. Runner and installed app/test hashes were verified.

Initial shell-77 check failed1/1 in39.233s at the host-only focus wait with Activity RESUMED (teste56bba1e). returnToOwnActivity's driver wait was corrected to recognize its actual dialog or original Activity focus. Final diff review also restored the explicit post-return assertSame(original,dialogWindow()) omitted by the pre-existing dirty edit. Test-only assembly passed (shell-78,1m3s), then the assertion-restored test rebuilt successfully (shell-83,11s); no production change. The intermediate shell-80 pass remains20.875s at testffc24ad1. After restarting only this new AVD, shell-89 failed at initial dialog focus in26.858s; immediate UI inspection showed a System UI ANR dialog. Ordinary observed Wait cleared it; the unchanged final test then passed in shell-92. No assertion or security change was made for that environment failure. Final shell-92 run passed1expected/started/executed/passed,0fail/skip,15.781s,OK (1 test),native code-1,shell exit0. Its focus diagnostic confirmed HOST_false_DIALOG_true before original0.8 restoration and HOST_true_DIALOG_false before parent verification. Every original same-window,closing-redaction,parent,recreation and secure-flag assertion passed.

Final app 1c3adda7dd80d515b8ec64e387a77d64d26b6da3bc261961dd007dc98a4965dc; test 40305e7296b7f9966ca3d98903fa588997bc9f568b9b44cda9ffc646c9d373d0; WindowRegistrationTest source c4ab7c965e58373f22c8cbecc4bf111e9622252b21328aefd1a802a3ad3fb673. Host/installed APK hashes match. Only the new AVD was stopped with registration/data retained. The old retained API31 AVD remains untouched with unknown-credential/FBE blocker; this is separate from the earlier historical combined wake/menu rejection. No new rejected/input-unlock attempt occurred in this task. The fresh fixture does not unlock or recover the old AVD.

See [exact final evidence, counts, identities and limits](phase-2-final-window-evidence.md) and currentApi25WindowVerification/finalFreshApi31Verification in the report. Earlier sections/table below retain historical source,artifact,failure and rejection identities.

## Prior production correction (historical source state)

PrivacyWindows previously captured window.attributes.alpha on each listener registration. With no owned focus it writes0; unregister removed the registration, so the same hidden Window could capture0 as its restoration basis on re-registration. MainActivity's DialogFragment stop/start registrations make this a real lifecycle.

The correction keeps that restoration basis in a separate WeakHashMap<Window,Float>. Active registrations own focus listeners; removing one leaves the redacted alpha and weak original basis intact. Registering the same live Window reuses its true original opacity. Weak keys and scalar values do not strongly retain destroyed windows. Controller close removes listeners and clears both maps. Unregister does not reveal an unfocused closing dialog. FLAG_SECURE, focus-driven Activity/Compose/DialogFragment protection and IME hiding remain unchanged. There is no new credential/session expiry, lock, prompt cancellation, draft persistence or production clipboard policy.

Production changed only app/src/main/java/com/gem/keeppeer/ui/privacy/PrivacyWindows.kt in this continuation. No MainActivity, cryptographic, session, manifest, build/dependency or exporter change was made. Source SHA-256:
- PrivacyWindows.kt: e84f930002f9249590bc6af648e45d4bdbd5ab0aab19aa68a8dd4fb63121789d.
- WindowRegistrationTest.kt: 84d679ec2ab8060e3eb8dc6477ccf12749ca24e3c0f9d2d37bbe6f3e150f1151.
- LiveRecentsPrivacyTest.kt: da8ce82b4ec684939591686ba58b9c0e271779df34d923da018e02ec5f4abbed.

## Native regression and fixture corrections

WindowRegistrationTest uses an actual DialogFragment at original alpha0.8 with production MainActivity callbacks. Real OS Recents removes focus; Home stops the Activity and unregisters the same actual Window. Its assertions cover original0.8 restoration after return, hidden dismissal without a visible disposal frame, Activity recreation/new Window, secure flags and visible parent return.

API31 shell-653.log passed the entire regression:1expected/started/executed/passed,0fail/skip,23.728s,OK(1test),native code-1,shell exit0. All milestones completed: original visible; Recents redacted; same Window stopped; original alpha restored; closing stays redacted; parent returned; before/after recreation; recreated visible; dismissed parent visible. This is on the corrected production app and test APK1c2fa35877a3238653858d39d36384003fb14a06bcf142c237ee04d792181f40, not the later final fixture package.

The LiveRecents fixture previously leaked its MINUTE timeout into a following lifecycle test after failure. It now restores the original timeout and locks in finally. It also requires actual initial window focus and invokes documented [UiAutomation OS global actions](https://developer.android.com/reference/android/app/UiAutomation#performGlobalAction(int)) for Recents/Back. No app accessibility permission or manufactured authentication callback was added. Its draft/session/dialog assertions remain. Shell-643 passed actual Recents plus lifecycle (2pass/1window-return failure in that3-test group); the approved biometric3methods and rotation passed in shell-626 (4pass/2fail group). Those individual passes retain their test-package identities.

The older API25 run shell-693 restored0.8, then failed because dismissNow was attempted after Fragment state saving. Only the disposable fixture was changed to dismissAllowingStateLoss and execute pending transactions before the same closing-alpha assertions. Shell-697 then failed earlier: exact own AppTask return left the host CREATED. Removing a redundant second asynchronous Home did not establish a fix: shell-701 again failed at return. Its full native result is0pass/1fail/0skip,19.751s. No production compatibility defect is inferred solely from this task-return failure. At that historical point the final fixture remained unverified on API31; these failed API25 runs are not relabelled as passes. The later retained API25 pass and final fresh API31 pass are recorded separately above.

## Device, source and binary scope

The earlier runs below used these retained registered dedicated AVDs:
- Keeppeer_Guard_API31_20261030_123138: discovered emulator-5590; qemu1/API31/x86_64/boot complete.
- Keeppeer_Guard_API25_20261030_123138: discovered emulator-5592; qemu1/API25/x86_64/boot complete. Older qemu name properties were used where ro.boot was empty.
- Emulator37.2.12.0/build16428233. Each exact serial was guarded by AVD/qemu/API/ABI checks before SDK actions; no unqualified connected-device task.
- SyntheticTestRunner registration targets com.gem.keeppeer. Its unchanged SyntheticTestApplication/SyntheticVaultContext uses UUID file/database/preferences and Keystore alias namespaces. Native checks do not open the normal vault.
- Both emulators were stopped after checks; no wipe, reset, template/key removal or AVD deletion.

Current production APK SHA-256:1c3adda7dd80d515b8ec64e387a77d64d26b6da3bc261961dd007dc98a4965dc.
Historical pre-return-driver test APK SHA-256:6369b61150d51bcb824d5d8411597f03683d8d15a3ef67ecd819307bf883e433. Current final test APK is recorded in the final section above.
Debug app/test assembly and lint passed after the final fixture edit; lint0errors14warnings.

| Native log | API | Expected/started | Pass/fail/skip | Native seconds | Final result and scope |
| --- | ---: | --- | --- | ---: | --- |
| shell-616 |31|7/2|0/1/0|unavailable|Process crashed,code0 after explicit synthetic force-stop;1completed,1aborted,5never started; not7executed. Host215.989s. |
| shell-625 |31|1/1|0/1/0|73.017|FAILURES!!!; partial opacity milestones, task return/teardown failed. |
| shell-626 |31|6/6|4/2/0|116.964|FAILURES!!!; biometric3 and rotation passed; Recents and contaminated lifecycle failed. |
| shell-631 |31|2/2|2/0/0|58.101|OK(2tests); only the approved controlled clipboard pair. |
| shell-635 |31|3/3|1/2/0|76.762|FAILURES!!!; lifecycle passed, window/Recents fixture failures. |
| shell-643 |31|3/3|2/1/0|224.503|FAILURES!!!; actual Recents and lifecycle passed, window task return failed. |
| shell-648 |31|1/1|0/1/0|28.869|FAILURES!!!; closing stayed redacted, parent task return failed. |
| shell-653 |31|1/1|1/0/0|23.728|OK(1test); complete real window lifecycle/recreation regression. |
| shell-692 |25|1/0|0/0/0|unavailable|Host cmd quoting error,exit2; instrumentation never invoked, not a skipped test. |
| shell-693 |25|1/1|0/1/0|12.465|FAILURES!!!; original alpha restored, then Fragment saved-state dismissal error. |
| shell-697 |25|1/1|0/1/0|19.422|FAILURES!!!; task return left host CREATED. |
| shell-701 |25|1/1|0/1/0|19.751|FAILURES!!!; same return condition on final test package. |
| Final API31 rerun |31|1/0|0/0/0|unavailable|Not started; preparatory wake/menu input rejected, no workaround. |

Except shell-616/code0 and shell-692/no runner, completed native groups ended instrumentation code-1 with shell exit0. A shell exit0 does not turn native failures into passes. Full per-run test APK hashes are in windowLifecycleClipboardContinuation.nativeRuns in the JSON report.

## Actual live UI and authentication

On the corrected production app, the normal synthetic vault opened using the existing enrolled fingerprint and actual native per-use prompt; its existing synthetic entry remained. No wrapper/key/template setup or enrollment change occurred. Earlier approved3biometric native methods passed on the same production APK in shell-626; real unlock is separate evidence.

An unsaved synthetic title and revealed sample were observed in normal foreground with IME present. Ordinary navigation-bar Recents produced four live observations during2026-10-30T15:31:08.191Z–15:31:10.172Z: the first observations remained normal foreground, then the app surface redacted to wallpaper while IME transitioned away. At15:31:22.014Z the settled own card showed wallpaper only, with no draft/entry/revealed sample or IME content. Back returned at15:31:51.354Z to the retained normal draft without the keyboard. The unsaved draft was cancelled, not saved. These discrete images are bounded transition/settled evidence, not continuous every-frame proof. Only own app/test-task context was observed; no unrelated card browsing was used.

An earlier desktop-state result returned an occluded editor image and another attempt reached the blank own-test harness Activity; neither is Recents protection proof. A prior blind synthetic unlock-input attempt reached system search because the fixture was already unlocked. That collection error was stopped; the synthetic fixture credential value is omitted. No personal credential or host clipboard value was collected. Later actions checked the actual visible state before proceeding.

The earlier normal screenshot control, protected draft/dialog PNGs, Home/grace/expiry/Back/screen-lock, recording and older physical/provider limits retain their exact historical binary scopes. FLAG_SECURE handling is unchanged; unavailable capture paths were not retried. Current window secure-flag assertions and real dialog cancellation/return are additional evidence, not fabricated current PNG captures.

## Approved controlled clipboard comparison — completed and restored

The current user explicitly authorized the agent to temporarily disable Windows-to-emulator sharing, run only the two existing failed clipboard checks, then restore and verify the original setting. The user reports answering “Approve temporary isolation and restore” in the originating review chat on2026-10-30. This new human approval is recorded separately from the historical unapproved toggle rejection. Approval itself was not counted as execution/pass.

Ordinary Extended Controls > Settings > General showed Enable clipboard sharing:
- Original: enabled, green/on, observed2026-10-30T14:38:00.221Z.
- Isolated: disabled, grey/off, verified14:39:03.146Z.
- Restored: enabled, independently verified green/on14:41:56.931Z, matching the original.

Scope was conservatively treated as emulator-client-wide. Before changing it, adb inventory showed only the dedicated emulator-5590 and host inventory one qemu-system-x86_64 process,PID13056. No other emulator ran while sharing was isolated. Restoration completed before later API25 work. No fresh review rejection occurred for the approved sharing action.

Only these unchanged methods ran under isolation:
1. VaultVerificationTest#keyboardSelectionCopyExpiresAndBackgroundReturnClearsOwnedClip.
2. VaultVerificationTest#touchMenuCutUsesSensitiveClipboardBoundary.

Result:2expected/started/executed/passed,0failed/skipped,58.101s,OK(2tests),native code-1,shell exit0,shell-631.log. Keyboard-copy expiry30056ms. App1c3adda7…965dc; testfde7e6ce34566c3f121f4b576cf7af0fffaf6184b2ce5dbeacb582df433684df. All expiry/ownership/sensitive-flag/newer-clip assertions and production clipboard code remained unchanged. This result is not relabelled as a final test-package run. Uncontrolled baseline failures remain history; no uncontrolled retry was made merely to obtain a pass.

Host clipboard values were not inspected/exported or Windows clipboard contents explicitly changed. No Android permissions/AppOps, Defender, Windows security, personal setting or unrelated AVD configuration changed.

## Historical combined wake/menu automatic-review block

After the final fixture build, the retained API31 AVD was reverified and the final test APK installed. Its display was black; safe power-policy metadata reported Asleep/keyguard showing. A single wake action did not establish a visible unlocked screen. Automatic review rejected the subsequent combined KEYCODE_WAKEUP + KEYCODE_MENU command as unacceptable risk, with this exact reason:

“The combined wake/menu input may bypass the emulator keyguard; that specific unlock approach is not explicitly authorized, and prior instructions caution against blind unlock input.”

The rejected combined action was not executed; no alternate wrapper, GUI route, obfuscation, synthetic PIN input or indirect retry was used to evade it. The final native API31 rerun never started. The verified emulator was stopped without altering credentials/templates/data. This new rejection is separate from historical biometric/toggle rejections and from the successfully executed clipboard approval.

## Acceptance and closeout checklist

- [x] Correct the original-opacity basis without exposing an unfocused closing window or strongly retaining destroyed windows.
- [x] Pass the full real-window stop/start, focus return, hidden dismissal and recreation regression on corrected API31 production app (shell-653, exact earlier test hash).
- [x] Observe actual revealed-draft Recents transition/settled own card and normal return on the corrected production app.
- [x] Preserve current approved biometric3/3, rotation and lifecycle individual passes at their actual package hashes; keep their containing failed groups intact.
- [x] Execute only the2approved clipboard methods with sharing off; pass2/2; restore/verify original enabled preference.
- [x] Assemble app/tests and lint after final edits; retain0errors14warnings.
- [x] Resolve final API25 native task-return failure and obtain a complete affected window pass there (1/1 pass, 7.379s).
- [x] Run the final affected window fixture on the separately authorized fresh API31 AVD:1/1 pass,15.781s,shell-92; old unknown-credential/FBE fixture untouched.
- [x] Preserve original selected22-test matrices/KDF/SQLCipher and screenshot/recording evidence at original identities without broad reruns.
- [x] Keep release disabled, Phase3 unstarted, personal/preserved material untouched.

Original C1–C6 and C9–C10 evidence remains carried forward at original identities where source is unchanged; approved biometric/rotation/lifecycle coverage retains its scope. C8 controlled clipboard2/2 and restored sharing remain covered. C7/A3 and A4 final affected window verification is now covered by the retained API25 pass and complete fresh API31 pass. No required Phase2 gap remains. The API25 historical hash-attestation and physical/provider limits remain explicit; no original-opacity/Recents redesign, further live-card observation,full matrix or universal hardware/Defender-verdict gate. Release disabled;Phase3 unstarted.

Recovery is no longer requested because the original unavailable entries were disposable tests. Cause stays unknown; no recovery/restoration, deletion/reset/rekey or preservation access is claimed/authorized. The user reported “Full scan finished; no threats or new alerts.” on2026-10-30, exact time unknown; it is uninspected user testimony, not an old false-positive verdict. Defender remained enabled/unchanged; no detections allowed/restored, exclusions, encoded-command replay or protection bypass. Physical screenshot/keyboard/provider coverage remains a bounded limitation. All runtime vault/envelope/key/credential/candidate/preservation/capture material remains excluded from the atomic source export.
