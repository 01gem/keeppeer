# Phase 3 CSV contract

The vault has no INTERNET or broad storage permission. CSV is a deliberately plaintext backup selected through Android SAF. A separate document provider may store it locally or remotely. The warning, before CREATE_DOCUMENT and again before writing the returned document, is: **This file contains your passwords in plain text. Store it somewhere safe.**

## Format and validation

UTF-8 without BOM is exported; import accepts one leading UTF-8 BOM. Malformed UTF-8 is rejected, never replaced. The header has exactly these nine case-sensitive cells in this order, with no extra, missing, reordered or whitespace-adjusted names:

```text
title,username,password,url,notes,category,favorite,created_at,updated_at
```

Export writes this literal header, quotes every data cell, doubles internal quotes, and uses CRLF between records, including the final record. Import also accepts LF separators and a final record without a separator. A bare CR outside a quoted field, quotes within an unquoted field, text after a closing quote, unclosed quotes, empty records, and wrong column counts are errors. Actual CR, LF and CRLF inside quoted fields are retained exactly. Empty and trailing cells are parsed, then validated according to their column. No line/comma splitting, trimming, Unicode normalization or literal backslash-n substitution occurs.

Title must satisfy the actual model's nonblank rule; its stored whitespace is preserved. Password is required as a cell but may be empty, matching the existing model. Empty username, URL, notes and category cells become null. Nonempty optional values are exact. Consequently a backup cannot distinguish null from an empty optional string; an existing empty optional string exports empty and reimports as null. Empty password stays empty.

Favorite accepts only lowercase true or false. Timestamps accept nonempty ASCII decimal digits, including leading zeros, in 0..Long.MAX_VALUE epoch milliseconds; updated_at must be at least created_at. No inferred date, timezone conversion, negative, signed, floating-point, blank or padded timestamps. Existing CRUD supplies current epoch milliseconds; the model itself previously enforced no timestamp range. CSV adds this explicit nonnegative range and ordering without a schema change. Long.MAX_VALUE can be inserted and exported, but an update to such a stored timestamp fails the complete transaction rather than overflowing or letting SQLite coerce the result to REAL. Ordinary CRUD now also raises a safe save failure for that exhausted timestamp instead of overflowing or misreporting the entry as deleted.

No IDs, envelopes, master password, key material or initialization metadata are in the CSV. Export is one consistent bounded full-entry Room read through the existing VaultAccess, ordered by ascending local ID, preserving duplicates. It does not reconstruct secrets from EntrySummary or perform N+1 UI lookups.

## Bounds and safe errors

Input is bounded while reading regardless of absent, false or changing provider metadata: 8 MiB bytes, 10,000 data records, 262,144 UTF-16 code units in any cell, and 4,194,304 aggregate code units across all cells including header. Quoting/separators consume the byte budget; decoded cell values consume the character budget. Complete bounded parsing and model validation precede any mutation. Header-only input is valid.

These character and record budgets bound aggregate parsed text/object allocation; they are a memory budget proxy, not a guarantee about total JVM heap or collector copies. The current vault is separately limited to 10,000 records and 8 MiB summed UTF-8 field payload before full-entry allocation, checked in the same Room transaction. Imported combined vault payload must satisfy that bound before commit. Export additionally enforces the same field, aggregate-character and serialized-byte bounds as import. Existing larger vaults remain intact and ordinary CRUD is unaffected; CSV refuses them with a safe limit error. No unbounded preview or full serialized CSV string is created. A maximum-size field is serialized one record at a time and emitted in at most 8 KiB chunks. Checks between characters/chunks/rows cooperate with cancellation.

Parser errors include only category and detection record/physical line, with header as record 1, and CRLF counted as one line. Decoder read-ahead can detect invalid UTF-8 before the parser reaches the offending field; that location is the last parsing boundary, not a claim to identify the invalid byte. Generic provider/vault errors omit provider exception text, URI and row content. JVM immutable strings and internal library buffers cannot be guaranteed erased. References and explicit byte buffers are dropped/cleared promptly; no secure-erasure claim.

## Duplicate plan and transaction

A match is exact case-sensitive title plus exact username, treating null and empty username as equivalent. No trimming or case-folding. Preview counts all input records, new keys, and conflicts, comparing against current entries and earlier file rows. It stores counts and an ephemeral digest of the complete ordered existing state; no existing passwords or notes enter preview UI or saved state.

The choices are:

- **Skip (default):** retain the existing target; insert only the first previously absent key. Repeated import is idempotent.
- **Update existing:** select the lowest local ID for each matching key. Preserve that ID and stored createdAt; leave other pre-existing matches untouched. Set updatedAt to max(stored updatedAt + 1, imported updatedAt, repository clock). Process in file order: later same-key rows update the same target, including one inserted earlier in this import. Every update advances again.
- **Keep both:** insert every row with a newly generated local ID, including conflicts and identical rows.

New rows preserve valid imported createdAt and updatedAt. Counts are inserted/updated/skipped operations, so two updates to one target count as two updated rows.

Inside one Room transaction, reload the bounded existing state and compare the complete preview identity, counts and targets before mutation. Any intervening CRUD/state change returns refreshed counts and resets policy to Skip for a new confirmation, even if counts happen to be unchanged. All accepted writes and the post-write vault bound commit together. SQL failure, exhausted timestamp or cancellation before commit rolls back the whole transaction. Cancellation after an already completed commit cannot undo it. No migration, destructive fallback or second database connection was added.

## SAF, lifecycle and resources

Activity Result registration lives outside the unlocked-only subtree. Activity ViewModel retains at most pending action/URI and notices through rotation. It has no saved-state persistence and never stores entries, rows, secrets, grants or keys. Process loss discards this state; an uncorrelated callback requests selection again. No persistent URI permission is taken. Ordinary temporary SAF grants are used only for the selected operation. Only content-scheme results are accepted; file-scheme or uncorrelated results never reach I/O or cleanup.

The picker never alters immediate/default or timed background lock policy. Its callback only records nonsecret selection. After return, a current unlocked session and a fresh explicit confirmation are required before any input reading, preview, vault reading or output writing. No previous session approval starts work. Settings export is also the optional export-before-password-change path.

Parsed rows live only in the current access-owned ViewModelStore. Lock revokes that access, cancels its work tree, clears the store/approvals, and drops decrypted rows and UI. Awaited work is cancelled and joined before its caller leaves; database close waits for the access work tree. All stream/database work uses IO. Streams close on all terminal paths; a cancellation watcher also asks the provider to close an in-flight stream. A provider may ignore cancellation or block open/close; Android cannot guarantee prompt completion of arbitrary external-provider calls. A stale callback cannot revive secret UI or automatically resume work. Rotation preserves access-owned work/preview; process death never serializes it. Existing drafts, background timeouts, encryption, initialization, biometrics, clipboard, secure flags and focus redaction remain in place.

Every new app-owned warning, renewed approval, duplicate, progress and result/error dialog calls ProtectDialogWindow. Success is shown only after write, flush and close succeed and the operation is still current. Picker cancellation is a no-op. Null streams, unreadable/revoked grants, provider exceptions, low-storage IO failures and close/flush failures fail safely without logging payloads.

A failed/cancelled export may leave plaintext, including a partial document. Cleanup is attempted only for the exact newly created CREATE_DOCUMENT URI and only if provider flags support deletion. No import source or unrelated document is deleted. If selection is discarded before writing, cleanup targets only that new document. Deletion failure is contained; the UI warns the user to inspect the provider. There is no atomic-write or secure-erasure promise for external storage. An uncorrelated process-recreation callback cannot safely identify a created document for cleanup.

## Spreadsheet interpretation

Formula-like strings (=, +, -, @ and others) are preserved exactly. Quoting does **not** stop spreadsheet formula interpretation. Spreadsheet programs may also reinterpret timestamps, Unicode, newlines, null/empty cells, dates and leading zeros or save another encoding/delimiter. Reimporting an app-authored CSV is the supported backup round trip. No cloud spreadsheet/upload is part of verification. Actual manual spreadsheet checks and all host/device outcomes are recorded in [the Phase 3 report](reports/phase-3-report.json).

Implementation references: Android's [SAF document guidance](https://developer.android.com/training/data-storage/shared/documents-files), consulted through Android Studio's official documentation tools; existing Room transaction and Activity Result dependencies were reused. No dependency change.
