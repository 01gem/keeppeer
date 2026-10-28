# Phase 1 device verification

Status: NOT RUN in this handoff unless a later result is explicitly recorded in phase-status.md. No Android device/emulator was connected during this session (`adb devices` returned an empty list).

Use synthetic data only. This prototype has no encryption or authentication.

1. Run `.\gradlew.bat :app:connectedDebugAndroidTest` with an emulator/device attached. The suite includes real SQLite CRUD, persistence after reopening the database, and draft rotation/cancel.
2. Launch debug. Verify the unencrypted/synthetic-only notice is visible on list, detail, add, and edit screens.
3. Add title `Synthetic École 100%_\\`, username `Alice`, password with leading/trailing spaces, URL `https://example.test`, category `WÖRK`, favorite checked, and Unicode multiline notes `雲\n🙂`. Save once and confirm details.
4. Verify list rows contain no password or notes. Search each metadata field using different letter case, including `école` and `wörk`; verify `%`, `_`, and backslash match literally. Searching password/notes must not find the entry.
5. Edit all fields; rotate while editing. Draft text must survive. Reveal must reset to masked. Save and confirm creation time is unchanged and update time advances.
6. Change a draft and cancel using both Cancel and system Back. Reopen to verify stored values are unchanged. Repeat from Add and verify no new row exists.
7. Test optional blanks, blank/whitespace-only title validation, and an empty password. Exact password and note whitespace/line breaks must survive save/reopen.
8. Dismiss delete confirmation and verify the entry remains; confirm deletion and verify return to list. Reopening a removed ID must show the missing-entry state without crashing.
9. Save a synthetic entry, force-stop/relaunch, and verify persistence. End the process with an unsaved edit; reopening may restore the saved entry by ID but must discard the sensitive draft.
10. Check portrait/landscape, keyboard open/closed, large text, light/dark mode, and navigation/status bar visibility on API 24/25 and a current Android version.

Release restriction: `:app:assembleRelease` and `:app:bundleRelease` must fail because the release variant is disabled. Do not bypass it or distribute this prototype.
