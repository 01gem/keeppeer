# Local release candidate procedure

Phase 5 local work is authorized. Publishing, uploading, distribution and production key creation/use are outside this task. See the Phase 5 report for actual completed checks and remaining gates.

## Reproducible builds

Use the pinned wrapper/toolchain. Ordinary builds keep release and validation variants disabled. Explicit local opt-in:

```powershell
.\gradlew.bat :app:assembleRelease :app:assembleValidation '-Pkeeppeer.enableReleaseBuilds=true' --console=plain
```

Release is unsigned unless the separate production-signing opt-in is supplied. Validation inherits release optimization, resources, applicationId, manifest and debuggable=false. It uses only the standard local Android debug/development certificate, has version suffix `-validation-test-signed`, a visible validation banner and About notice. It is never a production identity. Test fixtures/runners remain in androidTest, outside both candidate APKs. Never replace a minified smoke test with assembleDebug. Install only on a newly verified authorized Phase5 fixture:

```powershell
adb -s VERIFIED_PHASE5_SERIAL install -r app/build/outputs/apk/validation/app-validation.apk
```

Confirm serial, AVD name, qemu=1, API, ABI, boot completion and page size first. Do not run connected-device tasks without a verified target. Keep one emulator running at a time.

AGP 9.5 uses `optimization { enable = true }` without packageScope, enabling whole-app code/resource optimization and default Android rules. Dependency consumer rules remain active. Additional broad keeps are absent. Inspect mapping/usage/configuration and packaged DEX; retain mappings only locally under ignored build outputs and record hashes. Official references: [R8 configuration](https://developer.android.com/topic/performance/app-optimization/enable-app-optimization), [16 KiB verification](https://developer.android.com/guide/practices/page-sizes), [SQLCipher native support](https://www.zetetic.net/blog/2025/06/26/sqlcipher-for-android-16kb-page-size-support/). Alignment inspection and actual runtime page-size scope are separate evidence.

## Guarded device verification

Build the separate framework-only ordinary-Application harness:

```powershell
.\gradlew.bat :app:assembleDebugAndroidTest '-Pkeeppeer.ordinaryLifecycleTests=true' --console=plain
.\tools\check-phase5.ps1 -Serial VERIFIED_SERIAL -Avd EXACT_NEW_PHASE5_AVD -Api VERIFIED_API -Mode InstallValidation
.\tools\check-phase5.ps1 -Serial VERIFIED_SERIAL -Avd EXACT_NEW_PHASE5_AVD -Api VERIFIED_API -Mode Test -OrdinaryApplication -Selection ordinaryCompatibilitySetupAndReopen -ExpectedTests 1
```

Replace placeholders only with independently verified registered fixtures in the report. The guard refuses other names/serials/API/ABI or incomplete boot. Phase5StandaloneRunner uses Android framework/Java only, retains the real KeeppeerApplication, checks debuggable=false, and compares self-hashed installed app/test APKs with host hashes before UI work. This avoids debug-test runtime linkage to R8-renamed Kotlin classes. Each invocation reports one harness journey with explicit counts and duration; it is not JUnit. No keeps are added for tests. Whole-journey, cold-reopen and legacy-refusal selections are separate, ordered fixtures; never run every selection indiscriminately against one vault.

For the existing isolated JUnit suites, rebuild test packaging without the ordinaryLifecycleTests property, install Debug only on the exact new fixture, and use the default SyntheticTestRunner with an explicit class/method selection and expected count. Those results are additional debug/synthetic evidence, not a minified normal-lifecycle smoke. [Official test runner configuration](https://developer.android.com/training/testing/instrumented-tests).

## Production signing — later explicit instruction only

This task does not create, choose, use or rotate a production key. A later authorized owner must provide an existing approved identity and confirm update continuity. Store all four fields in ignored root `keystore.properties` or the specifically named environment variables: `KEEPPEER_STORE_FILE`, `KEEPPEER_STORE_PASSWORD`, `KEEPPEER_KEY_ALIAS`, `KEEPPEER_KEY_PASSWORD`. Never pass passwords on a command line, commit them, print them or export them for review. Properties use Java properties escaping (forward slashes are suitable for Windows paths). No default production password exists.

Only a later explicitly authorized signing invocation adds both `-Pkeeppeer.enableReleaseBuilds=true` and `-Pkeeppeer.productionSigning=true` to `:app:assembleRelease`. The signing opt-in requires all four nonblank values and an existing keystore; incomplete material fails configuration. Without the signing opt-in no production credential source is read and release stays unsigned, even if material exists locally. Validation always uses its development signing configuration.

Before distribution, verify the approved certificate fingerprint, version, manifest, complete required tests and backup/recovery guidance; securely retain mapping identities and signing continuity. Production signing/distribution are pending separate explicit instruction.

## Version and update policy

applicationId remains `com.gem.keeppeer`, minSdk 24, confirmed versionCode 1 / versionName 1.0. Distribution history is confirmed: `com.gem.keeppeer` has never been distributed (no Play Store, no sideload, no beta). GitHub repository source upload is planned; that is not app distribution and does not affect versionCode. Any future actual distributed build must increase versionCode and maintain signing identity continuity. No invented legal or store metadata.

Room schema remains version 1. Encrypted v1 updates retain the same schema/envelopes/KDF and device keys; no destructive migration fallback. Real same-device/same-certificate update preservation needs synthetic verification. Production/validation refuse legacy Phase1 plaintext material without opening, deleting or converting it. Only debug retains the confirmed synthetic-reset development flow. A factory replacement is not encryption. Unknown/missing/corrupt active storage fails closed.

CSV is plaintext and its selected provider may be remote. Formula-like strings remain exact and can be evaluated by spreadsheets. Lost password/device-bound keys can permanently lose access; no server recovery or automatic backup exists. See security.md and csv-contract.md.
