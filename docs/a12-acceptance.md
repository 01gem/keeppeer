# Samsung A12 acceptance

Tested build: release, personal key cert ec37b324, 2026-11-19

Device: Samsung A12 (`RR8R402RR7T`, SM-A125F, Android 12 / API 31). Owner re-scope 2026-11-19; this list supersedes Phase 5 Gate 2.

Phase 5 is done when every item below is checked on the A12, the JVM unit suite passes 69/69, and the release build is signed with the owner's personal key.

Use synthetic credentials only. Never write real passwords, the master password, CSV contents or vault data here. One result line per item.

- [ ] Fresh setup with a 12+ char synthetic password; wrong-password unlock shows an error
  - Result / date:
- [ ] Correct unlock completes (no stuck "Opening encrypted vault…")
  - Result / date:
- [ ] Lock now; auto-lock Immediate / 1 min / 5 min; screen-off locks; rotation does NOT lock
  - Result / date:
- [ ] Force-stop and phone reboot both open locked
  - Result / date:
- [ ] Add / edit / delete / search; favorites + category filters
  - Result / date:
- [ ] Generator: use, copy, cancel; copied password clears after ~30 s and on lock
  - Result / date:
- [ ] Screenshot blocked; Recents card is blank
  - Result / date:
- [ ] CSV export to a local folder; import it back with Skip / Update / Keep both
  - Result / date:
- [ ] Change master password; unlock with the new one; old one fails
  - Result / date:
- [ ] Fingerprint: enroll, unlock, cancel → password fallback still works
  - Result / date:
- [ ] Release build signed with my personal key installs and unlocks
  - Result / date:
