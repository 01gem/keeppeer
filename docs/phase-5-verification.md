# V1 requirement and evidence map

This map separates accepted Phases 0–4 evidence from final-source Phase 5 checks. Exact results/artifact identities belong to reports/phase-5-report.json; historical passes are never reassigned to a new APK. No universal OEM, keyboard or hardware claim.

| Requirement | Implementation | Verification boundary |
|---|---|---|
| Offline single-module Kotlin/Compose/Material3/manual DI; API24+ | app build, KeeppeerApplication, AppContainer | Pinned versions unchanged; packaged manifest audit and final builds |
| Exact-value CRUD/search/favorites/category | EntryDao, EntryRepositoryImpl, EntryFilters, list/edit/detail ViewModels | JVM repository/filter/favorite/CSV tests; accepted UI matrices; Phase5 minified normal UI smoke |
| Masked reveal and secure generator cancel/use/copy | EntryEditViewModel, PasswordGenerator, GeneratorDialog | JVM algorithm/failure/lifetime tests; accepted original Copy ownership/candidate/sensitive pass retains baseline hash; final minified generator UI separately recorded |
| Password setup/unlock/change and encrypted disk | CryptoManager, EnvelopeStore, EncryptedDatabaseProvider, VaultSession | JVM crypto plus selected native correct/wrong/plain-SQLite/KDF/storage tests; actual minified normal setup/reopen and password change where completed |
| Revocable repositories/work; fail-closed missing/corrupt storage | VaultAccess, FileVaultInitialization, VaultSession | Existing initialization/security/lifecycle suites selected explicitly; no destructive migration or plaintext production fallback |
| Same-schema encrypted upgrade | Room schema1 and unchanged envelope/KDF formats | Synthetic reopen/password change and markerless authenticated upgrade tests; actual replacement install preservation separately recorded; incompatible legacy plaintext safely refused in minified builds |
| Background immediate/1/5min, screen lock, process death | MainActivity/KeeppeerApplication lifecycle, elapsed-time VaultSession | Controlled-clock native timeout test is distinct from actual timer observations; historical elapsed runs retain scope; final cold process and UI runs identified individually |
| Rotation/draft/preview lifetime | Access-owned ViewModels; no sensitive saved state | Existing rotation/window/CSV tests; process loss intentionally drops unsaved work |
| Plaintext SAF export/import, renewed approval, bounds/duplicate policy/rollback | CsvPickerModel, CsvViewModel, CsvTransfer, VaultCsv, Room transaction | Meaningful JVM parser/provider/failure/transaction suites plus actual DocumentsUI export/restore; injected providers do not establish every external provider |
| Clipboard ownership/sensitive/exact/expiry/newer unrelated clip | SensitiveClipboard and Compose adapters | Original accepted controlled tests retain artifacts; any Phase5 timing/bridging isolation/restoration listed explicitly; host payload never read |
| Secure owned windows/original opacity/Recents | PrivacyWindows, ProtectDialogWindow, MainActivity | Original WindowRegistrationTest assertions retained; bounded actual Recents test separate from screenshots; unavailable/empty captures never counted as pass |
| Optional real CryptoObject biometrics/password fallback | BiometricVault, real BiometricPrompt, post-auth AAD | Preview 1.4.0-alpha07 compatibility exception retained; real emulated enrollment/invalidation and injected cases distinguished; physical/OEM observations not invented |
| Theme/settings/About/accessibility/narrow layouts | ThemeSettings DataStore; resource-based controls, semantics/scroll targets | JVM appearance tests; accepted layout/semantics matrices; final minified settings/theme smoke; manual visual/TalkBack/spreadsheet results separately scoped |
| Release metadata, optimization, signing isolation | Gated release/validation build types, icon/label, default+consumer R8 rules | Whole-app R8 mapping/usage/DEX and final manifest/native inspections; unsigned release and visibly test-signed non-debuggable validation |

## Upgrade and recovery contract

Only a same-device encrypted schema1 vault with the same applicationId and compatible signing identity is a supported in-place path. Room has no destructive fallback. No schema migration was added because no schema changed. Minified builds refuse detected Phase1 plaintext data without offering synthetic reset; they never pretend helper-factory replacement encrypts it. Debug retains the separately confirmed development reset for synthetic data only. Active storage loss/corruption must remain unavailable, never fresh setup.

A forgotten master password or lost device keys may permanently prevent access. Uninstall/clear-data/transfer can lose device-bound keys. Preserve valid data rather than attempt undocumented recovery. User-selected CSV is plaintext and provider-controlled (possibly remote); formulas, multiline Unicode and credential spaces are retained, with documented optional-empty/null behavior. Quoting is not formula neutralization. Password-change backup is optional. No server/account/cloud recovery, network permission or automatic transfer exists.

## Unobserved scope

A missing installed 16KiB image is a runtime gap, not an alignment pass. Only installed API24/25/31 x86_64 images were discovered. Other ABIs may be packaged but are not runtime-tested. Physical biometric/OEM/keyboard and external-provider/manual spreadsheet behavior retain historical limits. Semantics or automated configuration tests do not establish a manual TalkBack or visual session. Any required uncompleted verification remains an explicit Phase5 gate in the report.
