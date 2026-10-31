# Phase 4 behavior contract

Implementation and verification outcome are recorded in [the Phase 4 report](reports/phase-4-report.json). Earlier phase evidence keeps its original source, app and test scope. Release remains disabled; Phase 5 is unstarted.

## Generator

The single executable policy is `domain/PasswordGenerator.kt`: default length 20, inclusive bounds 8–128, all four classes enabled, ambiguous exclusion enabled. Pools are distinct ASCII characters:

| Class | Exact pool before exclusion |
| --- | --- |
| Lowercase | `abcdefghijklmnopqrstuvwxyz` |
| Uppercase | `ABCDEFGHIJKLMNOPQRSTUVWXYZ` |
| Digits | `0123456789` |
| Symbols | `!@#$%^&*()-_=+[]{};:,.?/` |

The exact ambiguous set is `Il1O0o|`. Exclusion removes matching characters from every selected pool (some ambiguous characters are deliberately absent from the base pools). It is a visual convenience, not a security/strength rating. No character normalization or duplication occurs. Configuration validates bounds, a nonempty selection, nonempty effective pools and sufficient length for one character from each class. The minimum length is greater than the maximum number of classes, so the last constraint cannot fail independently under the current fixed policy.

Production uses `java.security.SecureRandom.nextInt(bound)`, one bounded unbiased draw from each selected effective class, bounded draws from the combined pool for the remaining positions, then Fisher–Yates with bounded secure draws. Every selected effective class occurs at least once, without a fixed required-character position. This construction does not claim uniformity over all passwords satisfying the coverage condition or any entropy/strength score. Initialization/draw failure fails generation without a seed or fallback. The deterministic bounded-draw seam is internal and used only by property/algorithm tests. Explicit mutable character buffers are cleared; JVM strings cannot be guaranteed erased.

Generate opens a separate registered secure dialog and prepares a candidate. Options change only generator configuration and invalidate its previous candidate; Regenerate prepares a new candidate. Use password explicitly replaces the editor password and closes the dialog; it never saves or copies. Cancel/back/outside dismissal drops the candidate and leaves the original draft intact. Copy calls the existing ownership/expiry-aware sensitive clipboard service and reports its real boolean result. Copy is also guarded against a stale access handle. Generated values never enter action labels, accessibility descriptions, navigation, settings, saved state or logs.

Options and candidate are editor-ViewModel memory only. Candidate/draft may survive rotation in the same access-owned store; reveal resets on rotation, theme change and generator open/close. Saving closes/drops the generator. Revocation clears the store and rejects stale edits/generate/use/save. Process death has no draft/candidate recovery path. No generator options are persisted.

## Metadata, sections and filters

EntrySummary adds only `isFavorite`; no schema/migration change. List/filter/category state contains no password or notes. One reactive metadata subscription supplies the current complete vault, independent of the search result. Within each section DAO order is `updated_at DESC, id DESC`. Partitioning retains that order and stable numeric row IDs; a favorite appears only in Favorites and other rows only in Entries/Other entries.

The list star performs one dedicated SQL field update through the revocable repository. It updates only `is_favorite` and `updated_at`, preserves credentials/createdAt, and advances time to max(now, old + 1). Long.MAX_VALUE is rejected without overflow; the overflow check reads only the timestamp. It never fetches a full credential to toggle favorite. A row is disabled during its update so rapid duplicate taps do not queue stale toggles. Missing/deleted entries and storage failures produce a safe text error; the reactive query remains authoritative.

Filters combine Favorites only, category and the existing trimmed-query, Unicode-aware case-insensitive literal substring search across title/username/URL/category. Percent, underscore and backslash remain literal; passwords and notes are excluded. All and Uncategorized are typed identities. Real named categories are labelled “Category: value”, so names such as All and Uncategorized remain distinct. Null, empty and whitespace-only values share the Uncategorized choice. Other category values are exact and case-sensitive, including surrounding whitespace; values are never rewritten. Available nonblank categories are distinct, sorted by Kotlin String order from all current metadata. Search does not shrink category choices.

If a selected named category loses its last entry, its typed identity and an explanatory selected choice remain until the user changes or clears filters. Clear filters resets query/category/Favorites only. Empty vault and no matches have different messages. Metadata and filters are access-owned memory; no search text/category name is persisted.

## Theme and settings

A single application-owned Preferences DataStore instance stores only the enum-name theme preference in `appearance.preferences_pb`. System is the default and evaluates Compose’s actual system-theme configuration; Light/Dark override it. The existing dynamic colors on API31+ and safe older color schemes remain. Loading uses System safely, read failures offer retry, and failed writes retain the previous applied selection and show failure. Pending writes do not present a transient preference as saved.

Theme recomposition does not replace the session/access or navigation/editor ViewModels. Settings is a registered secure dialog over the current destination, preserving an unsaved draft while changing appearance. Locked/onboarding content and every owned dialog inherit the app theme. No secrets enter DataStore. The existing SharedPreferences timeout store and immediate/1-minute/5-minute timing remain unchanged.

Settings retains Lock now, master-password change with optional CSV backup, availability-based per-use CryptoObject enrollment/disable and password fallback, and real SAF import/export. About reads the installed PackageInfo version and explains offline/no-sync, provider-controlled plaintext exports, forgotten-password/device-key loss and realistic protection limits. No fake buttons or automatic enrollment changes are introduced.

## Interface and privacy

Polished flows use Material3 resources for visible strings and accessibility labels, heading/selected/checkbox semantics, full-row options with at least 48dp targets, scrollable content, existing Scaffold/system/IME insets and stacked long action groups. Errors include descriptive text, not color alone. The development-build notice uses a quiet distinct surface; release packaging stays disabled.

The public Compose text-input interceptor requests IME_FLAG_NO_PERSONALIZED_LEARNING on API26+, removes autocorrection and adds TYPE_TEXT_FLAG_NO_SUGGESTIONS for text inputs across sensitive flows. Text/URI/password keyboard variations remain appropriate to each field. API24–25 still receive no-suggestions/password variations. Keeppeer cannot control every third-party IME. Passwords and candidates stay masked by default. Theme mode changes reset reveal, without persisting reveal state.

Generator, filter, settings and About dialogs call ProtectDialogWindow, as do all retained CSV/delete/enrollment dialogs. Activity/window FLAG_SECURE, focus redaction, closing opacity/original-opacity restoration and authentication/background policy remain intact. CSV parser/writer, exact values, transaction/duplicate policy, pending picker ownership and renewed unlocked approval remain unchanged; parser failure categories are mapped to localized UI resources rather than raw English exception text.

## Verification limits

**Current required gap:** the full generator Copy test fails ownership after successful service submission. Its final safe diagnostic classifies the replacement label as host_clipboard, with sensitive marker false. It does not inspect the replacement payload. Sharing is unchanged. The failure is retained; an independent generator state journey excluding Copy passes, and is not presented as a Copy pass. Phase4 therefore remains incomplete pending controlled native ownership/exact-payload verification on a suitable explicitly authorized fixture/provider. No change to the existing clipboard path or forbidden host controls is used to conceal this result.

See the structured report for exact commands, native completion, durations and artifact identities, including unsuccessful attempts. Host closed-store/new-reader persistence is not a full device restart. Native persistence uses separately launched instrumentation processes. Automated bounds/semantics checks and injected 2.0 font scale are not a manual TalkBack or physical-device visual session. No current direct clipboard expiry, hardware/real biometric, other-provider or spreadsheet claim is implied by this phase.
