# Verification Report: Turn-1 Lead Simulation Harness

## Scope

The implementation is limited to the test-scoped Turn-1 simulation harness, Koga's calibrated trainer datapack entry, workstream documentation, and calibration reports. No production Java battle-AI code was changed.

## Local evidence

| Layer | Command | Result |
| --- | --- | --- |
| Layer 1 | `python scripts/ci/check_legacy_baseline.py` | PASS: legacy baseline verified; 1664 files |
| Layer 1 | `python scripts/ci/validate_repo.py` | PASS: all repository and data validations passed; 1714 modernized trainers |
| Layer 1 | `python -m unittest scripts/compat-audit/test_audit.py -v` | PASS: 19 tests, 0 failures |
| Layer 2 | `companion-mod/gradlew.bat test --tests com.cobbleverse.legendaryrule.lead.simulation.*` | PASS: 31 simulation tests, 0 failures |
| Layer 2 | `companion-mod/gradlew.bat test --tests com.cobbleverse.legendaryrule.lead.KogaLeadCalibrationHarnessTest` | PASS: 3 calibration tests, 0 failures |

The Gradle tests were executed against the current working-tree sources with Fabric Loom 1.7.4 and Gradle 8.10.2. The repository CI workflow does not run the Gradle suite; CI remains the authority for the required GitHub validation job after push.

## Calibration outputs

- `reports/koga_51pair_turn1_matrix_report.txt`: 51 cases; strict agreement 31/51 (60.8%); combined viable outcomes 40/51 (78.4%); catastrophic selections 0/51.
- `reports/koga_calibration_round1.txt`, `reports/koga_calibration_round2.txt`, and `reports/koga_calibration_round3.txt`: reproducible type-scoring calibration outputs for the Koga presets.
- The reports are offline calibration evidence. They do not establish production multiplayer battle behavior or player-choice parity.

## Residual limitations

- The simulator models bounded Turn-1 lines and standard competitive profiles; it is not a multi-turn battle engine.
- Live gameplay and progression remain unverified and require the dedicated production canary layer.
- The GitHub PR head, required check conclusion, mergeability, and final merge commit must be recorded after remote delivery.
