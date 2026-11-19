# Samsung A12 acceptance

Tested build: release, personal key cert ec37b324, 2026-11-19

Device: Samsung A12 (`RR8R402RR7T`, SM-A125F, Android 12 / API 31). Owner re-scope 2026-11-19; this list supersedes Phase 5 Gate 2.

Phase 5 is done when every item below is checked on the A12, the JVM unit suite passes 69/69, and the release build is signed with the owner's personal key.

Use synthetic credentials only. Never write real passwords, the master password, CSV contents or vault data here. One result line per item.

- [x] Fresh setup with a 12+ char synthetic password; wrong-password unlock shows an error
  - Result / date: PASS (owner-verified on the A12), 2026-11-19
- [x] Correct unlock completes (no stuck "Opening encrypted vault…")
  - Result / date: PASS (owner-verified on the A12), 2026-11-19
- [x] Lock now; auto-lock Immediate / 1 min / 5 min; screen-off locks; rotation does NOT lock
  - Result / date: PASS (owner-verified on the A12), 2026-11-19
- [x] Force-stop and phone reboot both open locked
  - Result / date: PASS (owner-verified on the A12), 2026-11-19
- [x] Add / edit / delete / search; favorites + category filters
  - Result / date: PASS (owner-verified on the A12), 2026-11-19
- [x] Generator: use, copy, cancel; copied password clears after ~30 s and on lock
  - Result / date: PASS (owner-verified on the A12), 2026-11-19
- [x] Screenshot blocked; Recents card is blank
  - Result / date: PASS (owner-verified on the A12), 2026-11-19
- [x] CSV export to a local folder; import it back with Skip / Update / Keep both
  - Result / date: PASS (owner-verified on the A12), 2026-11-19
- [x] Change master password; unlock with the new one; old one fails
  - Result / date: PASS (owner-verified on the A12), 2026-11-19
- [x] Fingerprint: enroll, unlock, cancel → password fallback still works
  - Result / date: PASS (owner-verified on the A12), 2026-11-19
- [x] Release build signed with my personal key installs and unlocks
  - Result / date: PASS (owner-verified on the A12), 2026-11-19
