# Phase 5 checklist — unstarted

This is a planning handoff, not release authorization or a readiness claim. Phase4 must finish its recorded implementation/verification gaps first. Release remains disabled. No signing, publishing, uploads or distribution have been performed.

- Reconcile the final Phase4 source review and resolve actionable findings; regenerate source-code.txt after every fix.
- Plan whole-app synthetic journeys across onboarding/unlock/password change/optional biometrics, CRUD, generator, filters, appearance, background/rotation/process loss and SAF backup. Preserve original artifact scopes and failures; do not substitute shell success or skips for native completion.
- Choose explicitly authorized disposable API24/25 and modern-device checks for the final whole-app artifact. Avoid a blanket rerun of historical matrices without a changed risk. Personal/retained unknown-credential devices and preserved material remain outside scope.
- Complete remaining manual narrow/landscape/large-font visual and TalkBack checks on an authorized suitable device; distinguish them from automated semantics checks. Review keyboard, focus, contrast and error/retry usability.
- Reassess provider/OEM/hardware limits: real biometric enrollment/invalidation/fallback if specifically authorized; actual clipboard timing/ownership/background behavior without changing host sharing; protected owned windows/Recents; other-provider/physical-device CSV and local spreadsheet interpretation as available.
- Reconfirm no network/broad-storage permissions, disabled backup/device transfer, no secret logging/saved state/plaintext caches, current dependency compatibility and the biometric preview exception. No general upgrade exercise.
- Separately design and review minification/R8 rules and release build configuration before enabling a variant. Test a minified artifact only in Phase5 after its release restriction is deliberately reviewed.
- Design safe local signing configuration with credentials outside Git/review snapshots; no production signing or publishing without the later explicit task authorization.
- Run the final affected JVM/native/lint/build/manifest checks and record exact source/app/test identities. Document pass/fail/not-run and realistic protection/recovery limits.
- Produce an honest Phase5 readiness report and full atomic source snapshot. Publishing/distribution remains a separate decision.
