# Phase 2 — read-only vault-context reconciliation

Investigation completed to the metadata boundary on 2026-10-30; closing observation marker **12:47:45 AM PHT (UTC+08:00)** (no main PID; launcher still foreground). Phase 2 remains incomplete and release disabled. This task changed documentation/reports only. No build, APK installation, instrumentation, application launch/restart/force-stop, cache cleanup, enrollment/key change, migration or recovery was performed. No screenshots, entry contents, passwords, database/envelope payloads or keys were collected.

## Evidence and correction

The reviewed snapshot was generated **2026-10-30 12:11:46 AM PHT**. Its package launch showed first-run labels and conventional-path checks reported missing files. There was no pre-update file baseline.

The user subsequently confirmed in the originating review chat on **2026-10-30** that the existing vault currently unlocks and entries are visible on the same phone, without uninstall, clear-data, device or Android-profile switch. This is **user-reported evidence**; exact time was not supplied. The agent did not observe credentials or entries. It supersedes the earlier unanswered app-data-history question. It does not prove the identity/context or preservation of any particular file set.

**Correction:** the earlier claim that process metadata verified the normal runtime `KeeppeerApplication` was too strong. The `class=`/application-name information in the earlier process/package dump is declared `ApplicationInfo` metadata. It does not establish the class of the actual live `Application` object or the `VaultSession` context. An ordinary launch intent alone also does not supply that evidence. No data loss, recovery, active test-context use or passing normal-file-preservation check is established.

## Identity observed without app interaction

| Property | Actual observation |
|---|---|
| Device | `RR8R402RR7T`, Samsung SM-A125F, Android 12/API 31 |
| Android user | `am get-current-user` returned `0`; foreground Activity belonged to user 0 |
| Foreground Activity | Samsung launcher `com.sec.android.app.launcher/.activities.LauncherActivity`; no Keeppeer screen was read |
| Main package | `com.gem.keeppeer`, versionCode 1/versionName 1.0, UID 10358 (`u0_a358`) |
| Installed APK | `/data/app/~~dyU3rt05QVCSXWse3AWDLQ==/com.gem.keeppeer-r5oIsgxq4fXC-3_ldhxOKw==/base.apk` |
| APK SHA-256 | `c103aac417587198697f90f37b2195e0732adb1f5a7380d7c3bb514ef841c169` |
| Package data directory | `/data/user/0/com.gem.keeppeer` |
| Installation metadata | firstInstallTime 2026-10-29 08:14:00; lastUpdateTime 2026-10-29 23:36:20, device local time |
| Main PID | `pidof com.gem.keeppeer` returned empty output/status 1 on repeated passive observations; filtered process inventory contained no main package or colon-suffixed main process |
| Companion test process | `com.gem.keeppeer.test`, PID 3375, UID 10359 (`u0_a359`); presence alone is not active instrumentation |
| Instrumentation | No `ActiveInstrumentation` entry in filtered ActivityManager process metadata |
| Inspection identity | `run-as com.gem.keeppeer id` returned UID/GID 10358 and `runas_app` SELinux domain; `pwd` returned `/data/user/0/com.gem.keeppeer` |

The device serial, user 0, package UID, APK path/hash and install metadata match the earlier absence observation. The earlier main PID was **3921**; no current main PID is available. There is no live accessible main session to bind to storage in this inspection. No conclusion about the user's reported visible session follows from the launcher observation alone.

## Conventional-path metadata

Each path was checked separately using the exact absolute path:

```text
adb -s RR8R402RR7T shell run-as com.gem.keeppeer stat -c %n:%F:%s:%Y <absolute-path>
```

Each native exit status was captured immediately and separately, rather than inferred from a later command's status.

| Absolute path under /data/user/0/com.gem.keeppeer | Result |
|---|---|
| `no_backup` | Directory, 3488 bytes, mtime epoch 1793289396 = 2026-10-29 11:56:36 PM PHT; status 0 |
| `databases` | Directory, 3488 bytes, same mtime; status 0 |
| `no_backup/vault-envelope.bin` | Status 1, `No such file or directory` |
| `no_backup/vault-envelope.bin.bak` | Status 1, same safe error classification |
| `no_backup/vault-envelope.bin.new` | Status 1, same safe error classification |
| `no_backup/biometric-envelope.bin` | Status 1, same safe error classification |
| `databases/keeppeer_vault.db` | Status 1, same safe error classification |
| `databases/keeppeer_vault.db-wal` | Status 1, same safe error classification |
| `databases/keeppeer_vault.db-shm` | Status 1, same safe error classification |

The parent-directory controls succeeded under the expected app UID. No permission-denied error was observed. These checks establish missing **specific conventional paths for package/user 0**, not absence of every possible vault or the storage of the user's accessible session.

## Candidate test-cache metadata only

A bounded `stat` glob inspected only `cache/keeppeer.test.*` directories and their known database/envelope filenames under the main package's app UID. Five candidate directories were found; three had the following regular-file pairs. No candidate was opened, decrypted, copied, selected, promoted or associated with user entries.

| UUID namespace suffix | DB bytes / modification time (PHT, 2026-10-29) | Device-envelope bytes / modification time |
|---|---|---|
| `23cc2b6d-d810-47ed-9fe3-72f12544c3c8` | 16384 / 11:55:49 PM | 121 / 11:55:24 PM |
| `93f341f8-0f32-43eb-89d4-ddf3f91322cf` | 16384 / 11:46:39 PM | 121 / 11:46:02 PM |
| `ebd54a02-9b75-442f-b017-1e14479942e5` | 16384 / 11:39:14 PM | 121 / 11:38:37 PM |

Paths are `/data/user/0/com.gem.keeppeer/cache/keeppeer.test.<UUID>/keeppeer_vault.db` and `.../no-backup/vault-envelope.bin`. The two additional directories were `keeppeer.test.0a760dad-de01-4134-b92c-51bb151c6e4a` and `keeppeer.test.cd89f022-1653-43a2-b8a8-eb48c24b4905`. There was no match for the bounded `.../no-backup/biometric-envelope.bin` pattern; that unmatched glob produced status 1 alongside successful metadata for existing candidates. It is not a global permission or directory-failure result.

The companion package was checked under its own verified UID 10359 and cwd `/data/user/0/com.gem.keeppeer.test`. Its specific conventional vault paths and bounded `cache/keeppeer.test.*` patterns returned no-match/no-such-file errors. No package/profile directories outside these two project packages were examined.

## Source context mappings, not runtime claims

| Source path | Expected locations and namespace |
|---|---|
| `KeeppeerApplication.createContainer → DefaultAppContainer` | Normal application context; `no_backup/vault-envelope.bin`, `no_backup/biometric-envelope.bin`, `databases/keeppeer_vault.db`; aliases `keeppeer.storage.v1` and `keeppeer.biometric.v1` |
| `SyntheticTestRunner.newApplication → SyntheticTestApplication.createContainer` | UUID `SyntheticVaultContext` under `cache/keeppeer.test.<UUID>`; database directly in that root, envelopes in `no-backup`; source-derived aliases `keeppeer.test.<UUID>.storage.v1` and `...biometric.v1` |

The test context overrides `getApplicationContext`, `getNoBackupFilesDir`, `getDatabasePath` and namespaced preferences. The alias strings above are source mappings; Keystore aliases/keys were not enumerated or read. There is no observed active-session namespace or resolved active-session path. The existing Studio debug tool would build/deploy the app; it was not used. No heap dump, debugger deployment or replacement diagnostic was attempted.

## Reassessment of the earlier absence commands

- The initial relative-path command used `stat -c '%n %s %y'` through ADB. Its output shows that remote argument handling treated `%s` and `%y` as extra filenames. That command did not provide valid size/time fields. It also emitted missing-path errors for the named conventional files.
- Its outer shell result was 0 after later package/`dumpsys` commands; the individual `stat` result was not captured. An aggregate status cannot prove each command succeeded.
- The later no-space `stat -c %n:%s:%y:%z` directory check worked, and a separate bounded cache query had mixed successes plus an unmatched biometric glob. Those results must retain their exact scopes.
- Earlier relative cwd/effective UID were not independently recorded at that moment. The current absolute-path per-status check confirms app-UID access and missing conventional paths now; it does not establish historical runtime context.
- The earlier `dumpsys activity instrumentation` attempt returned an unsupported-command message and is not evidence that instrumentation was absent. Current filtered `dumpsys activity processes` is the valid metadata source used here.
- The prior first-run UI result is a historical filtered-label observation. No new UI content or screenshots were collected, and no app was started to repeat it.

## Conclusion and smallest next observation

**Established:** static device/user/package/build identity matches; conventional owner-user paths are missing with readable parents; bounded test-cache candidates exist; main process is absent during this inspection; no active instrumentation was reported.

**User-reported:** the existing vault unlocks and entries are visible without uninstall/clear-data/device/profile switch, dated 2026-10-30, exact time unknown.

**Corrected:** the earlier declared application-name metadata did not prove the runtime Application class or session context. A specific file-set preservation, loss or recovery conclusion is unsupported.

**Unknown:** package/PID/context at the moment of the accessible session, actual runtime Application/container, resolved active-session database/envelope paths and alias namespace, relationship of any candidate to the reported entries, and the time/cause behind the differing observations.

The smallest next observation is a **time-correlated passive package/user/PID snapshot while the user already has the accessible vault visible**. Do not launch/restart it to manufacture that state. If a live PID exists, an app-UID read-only, vault-filename-filtered file-descriptor target check may identify open database paths without reading payloads. A denied/unavailable descriptor or unavailable runtime introspection must be reported as a limitation, not absence. Do not use build/deploy, instrumentation or heap inspection to obtain it in this task.

Only if an active cache-backed session is confirmed should a preservation/migration proposal be prepared: keep the exact cache directory, namespace and Keystore aliases untouched; design an explicit authenticated migration with an independently validated destination and retained original before any write. No copying, rekeying, namespace substitution or migration is authorized/performed by this read-only investigation.

All prior artifact-scoped API 24/API 25/API 31 and targeted Samsung results remain unchanged. The other manual gaps remain open. No Phase 3 work or release/distribution followed.
