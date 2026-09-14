## Verification Evidence Manifest

- **Workstream ID:** `opponent-match-conjunction-lead-preset`
- **Baseline Commit:** `466c595c1738ffa2ab7d69a53d824659c25348c4` (`main`)
- **Plan-Freeze Commit:** `d63f833`
- **Branch:** `feat/opponent-match-conjunction-lead-preset`
- **Frozen Plan Hash:** `1bb898c568eff07092cecb934c9d4a060f09c8ae` (verified with `git hash-object docs/workstreams/opponent-match-conjunction-lead-preset/plan.md` before any file was modified; matches the hash of blob `d63f833`)
- **Implementation Candidate:** working tree relative to `d63f833` (uncommitted; Main Controller owns commits)

### Working-Tree Status

```text
 M .agents/skills/competitive-pokemon-doubles-team-design/references/trainer-json-authoring-contract.md
 M companion-mod/src/main/java/com/cobbleverse/legendaryrule/lead/AttemptScore.java
 M companion-mod/src/main/java/com/cobbleverse/legendaryrule/lead/LeadAttempt.java
 M companion-mod/src/main/java/com/cobbleverse/legendaryrule/lead/LeadSelectionConfig.java
 M companion-mod/src/main/java/com/cobbleverse/legendaryrule/lead/LeadSelectionEngine.java
 M companion-mod/src/main/java/com/cobbleverse/legendaryrule/lead/LeadSelectionService.java
 M companion-mod/src/main/java/com/cobbleverse/legendaryrule/lead/PlayerLeadTyping.java
 M companion-mod/src/main/java/com/cobbleverse/legendaryrule/lead/adapter/CobblemonLeadAdapter.java
 M companion-mod/src/test/java/com/cobbleverse/legendaryrule/lead/BlaineLeadSelectionTest.java
 M companion-mod/src/test/java/com/cobbleverse/legendaryrule/lead/LeadSelectionConfigTest.java
 M datapacks/hell-mode/data/rctmod/trainers/kanto_blaine.json
 M scripts/ci/validate_repo.py
?? companion-mod/src/main/java/com/cobbleverse/legendaryrule/lead/OpponentMatch.java
?? companion-mod/src/test/java/com/cobbleverse/legendaryrule/lead/BlaineFastElectricLeadPresetTest.java
?? companion-mod/src/test/java/com/cobbleverse/legendaryrule/lead/OpponentMatchTest.java
?? companion-mod/src/test/java/com/cobbleverse/legendaryrule/lead/adapter/CobblemonLeadAdapterMovesetTest.java
```

`docs/workstreams/opponent-match-conjunction-lead-preset/plan.md` is **not** in this list: the frozen plan was not edited. The working set is exactly the 16 files of frozen plan §6 — 12 modified, 4 created — with no extras.

### Tracked Diff Summary

```text
 .../references/trainer-json-authoring-contract.md  | 130 +++++++++++++++--
 .../legendaryrule/lead/AttemptScore.java           |   5 +
 .../legendaryrule/lead/LeadAttempt.java            |  12 +-
 .../legendaryrule/lead/LeadSelectionConfig.java    |  52 ++++++-
 .../legendaryrule/lead/LeadSelectionEngine.java    |  28 +++-
 .../legendaryrule/lead/LeadSelectionService.java   |  12 +-
 .../legendaryrule/lead/PlayerLeadTyping.java       |  21 ++-
 .../lead/adapter/CobblemonLeadAdapter.java         |  55 ++++++-
 .../lead/BlaineLeadSelectionTest.java              |   2 +-
 .../lead/LeadSelectionConfigTest.java              | 162 +++++++++++++++++++++
 .../data/rctmod/trainers/kanto_blaine.json         |  26 ++++
 scripts/ci/validate_repo.py                        |  36 +++++
 12 files changed, 518 insertions(+), 23 deletions(-)
```

Plus the 4 created files, which do not appear in `git diff` (untracked).

### Verification Suite Results (Minimal Orthogonal Set)

#### Executed Applicable Layers

##### Layer 2: Java Unit & Boundary Tests

**Command 1 — focused**
```text
cd companion-mod && ./gradlew test --tests "com.cobbleverse.legendaryrule.lead.*"
```
- **Exit Code:** 0
- **Log Excerpt:** `BUILD SUCCESSFUL in 6s`
- **Result:** 181 tests, 0 failures, 0 errors, 0 skipped across the 24 `com.cobbleverse.legendaryrule.lead.*` suites.
- New/changed suites in this run:
  - `OpponentMatchTest` — 12 tests (G1–G12), 0 failures
  - `BlaineFastElectricLeadPresetTest` — 14 tests (B1, B2, B3, B3b, B4, B5, B6, B7, B8, B9, B10, B11, B12 + the Checkpoint-3 calibration measurement), 0 failures
  - `CobblemonLeadAdapterMovesetTest` — 7 tests, 0 failures
  - `LeadSelectionConfigTest` — 14 tests (12 pre-existing + 2 new), 0 failures
  - `BlaineLeadSelectionTest` — 16 tests (preset-count assertion updated 4 → 5), 0 failures

**Command 2 — full companion suite**
```text
cd companion-mod && ./gradlew test --rerun-tasks
```
- **Exit Code:** 0
- **Log Excerpt:** `BUILD SUCCESSFUL in 12s` / `4 actionable tasks: 4 executed`
- **Result:** 493 tests, 0 failures, 0 errors, 0 skipped across 60 suite files. All tasks re-executed (no stale results).

**Command 3 — build**
```text
cd companion-mod && ./gradlew build
```
- **Exit Code:** 0
- **Log Excerpt:** `BUILD SUCCESSFUL in 8s` / `9 actionable tasks: 4 executed, 5 up-to-date`

##### Layer 1: Datapack & Trainer Schema Validation

**Command 4**
```text
python scripts/ci/validate_repo.py
```
- **Exit Code:** 0
- **Log Excerpt:** `RESULT: ALL REPOSITORY & DATA VALIDATIONS PASSED`
- Notable lines: `[PASS] All 1663 legacy trainer JSON files parsed successfully without errors.` / `[PASS] All 1714 modernized trainer JSON files parsed and verified successfully.` / `[PASS] Modernized pack trainer count (1714) matches upstream baseline (1714).`
- This is the run that validates the new `anti_fast_electric` preset, including the new `opponentMatch` block.

**Command 5**
```text
python scripts/ci/check_legacy_baseline.py
```
- **Exit Code:** 0
- **Log Excerpt:** `PASS: Legacy baseline verified (1664 files, digest 130833447db6fae0...)`
- `!Doctors HELL MODE DOUBLE BATTLE EVERYTHING/` is unmodified.

##### CI Parity (companion to Layers 1–2)

**Command 6**
```text
python -m unittest scripts/compat-audit/test_audit.py -v
```
- **Exit Code:** 0
- **Log Excerpt:** `Ran 19 tests in 0.698s` / `OK`

##### Optional Corroboration

**Command 7**
```text
python scripts/runtime-contract/test_typechart_parity.py
```
- **Exit Code:** 0
- **Log Excerpt:** `SUCCESS: 100% parity verified across all 324 directed type interactions (Cobblemon-fabric-1.7.3+1.21.1.jar <-> typechart_gen9.json)`
- Corroborates that `typechart_gen9.json`, on which the precedence argument rests, is unmodified. Not a gate.

##### Layer 0: Markdown & Governance (Sole Applicable Automated Check for the contract edit)

- **Checks:** relative-link integrity, YAML frontmatter, file line-count bound (< 500 lines), proscribed-absolute-phrase scan.
- **Result:**
  - `trainer-json-authoring-contract.md` — **457 lines** (< 500 bound). Its only relative link, `](../SKILL.md)`, resolves to `.agents/skills/competitive-pokemon-doubles-team-design/SKILL.md` (79 lines, present on disk).
  - Proscribed-absolute scan over the edited document: **0 matches** for `hoàn toàn` / `triệt để` / `guarantees` / `flawless` / `zero risk` / `tuyệt đối` / `completely closes` / `100% tuân thủ`.
  - `AGENTS.md` and `SKILL.md` were **not** edited; the contract was not duplicated into either.
- **Note:** this is a manual structural inspection only. No automated markdown/link/frontmatter checker exists under `scripts/`. Structural automation verifies syntax, not semantic correctness; semantic authority belongs to independent contract review.

##### Parser ↔ Validator Schema Agreement (contract §H.3)

Because `LeadSelectionConfig` and `validate_repo.py` are **separate implementations of one schema**, the
same rejection set was exercised against both.

Java side: `LeadSelectionConfigTest.testOpponentMatchParsingAndStrictPerAttemptRejection` submits 19 attempts
(4 valid matchers + 14 malformed matchers + 1 valid matcherless sibling) and asserts the surviving set is
exactly `[valid_full, valid_type_only, valid_damaging_only, valid_slot_only, valid_sibling_no_matcher]` —
i.e. every malformed attempt is skipped in isolation and valid siblings survive.

Python side: a scratch harness outside the repository (never added to the working tree) mutated the
`anti_fast_electric` preset into each malformed variant in turn, ran `python scripts/ci/validate_repo.py`,
and restored the file. Result: **13/13 fixtures rejected** (exit 1, with an `opponentMatch`-specific error
line each), then the file was restored and its blob hash re-verified unchanged
(`c8d02e8ff7c0b65bfb5070d31b3554e297e47d2a` before and after), and a clean re-run returned
`exit 0 / RESULT: ALL REPOSITORY & DATA VALIDATIONS PASSED`.

| Fixture | Validator outcome | Error surface |
| :--- | :--- | :--- |
| `opponentMatch` not an object | rejected | `opponentMatch must be a dict` |
| `bonus` absent | rejected | `requires a mandatory 'bonus' integer in [1, 16]` |
| unknown sub-key (`typ`) | rejected | `contains unknown sub-key(s): ['typ']` |
| unknown `type` (`light`) | rejected | `opponentMatch.type must be a canonical Gen 9 type` |
| unknown `damagingMoveType` (`shadow`) | rejected | `opponentMatch.damagingMoveType must be a canonical Gen 9 type` |
| `fasterThanRosterSlot` fractional (`1.5`) | rejected | `must be a non-negative integer` |
| `fasterThanRosterSlot` negative (`-1`) | rejected | `must be a non-negative integer` |
| `fasterThanRosterSlot` string (`"1"`) | rejected | `must be a non-negative integer` |
| `fasterThanRosterSlot` out of bounds (`99`) | rejected | `out of bounds for team of size 6` |
| `bonus` = `0` | rejected | `bonus must be an integer in [1, 16]` |
| `bonus` = `17` | rejected | `bonus must be an integer in [1, 16]` |
| `bonus` = `"14"` | rejected | `bonus must be an integer in [1, 16]` |
| all condition sub-fields absent | rejected | `must declare at least one of type, damagingMoveType, fasterThanRosterSlot` |

The parser's own rejection surface for the same fixtures is asserted in
`LeadSelectionConfigTest.testOpponentMatchParsingAndStrictPerAttemptRejection` and
`testOpponentMatchRecordRejectsOutOfPolicyValuesDirectly`.

#### Inapplicable / Skipped Layers

- **Layer 3 (Bytecode & Shadow Runtime Contracts):** Skipped. Zero Mixin, shadow field, or bytecode target changes; no runtime-injection edit.
- **Layer 4 (Headless Server Bootstrap Smoke):** Skipped. No Mixin/runtime-injection change, so a bootstrap smoke run would not exercise any modified contract.
- **Layer 5 (Production Canary):** Not claimed. See the boundary statement below.

### Checkpoint-3 Calibration Re-measurement (frozen plan §5.5 / §8)

The frozen plan mandates re-measuring its precedence calibration table against the real engine. This was
done in `BlaineFastElectricLeadPresetTest.checkpoint3_precedenceCalibrationMeasuredAgainstTheRealEngine`,
which runs the four required boards through the real `LeadSelectionEngine` with the datapack-loaded Blaine
roster and presets, and asserts the winner, the winner's exact `totalScore`, and the runner-up's exact
`totalScore`.

| Board (player leads) | `anti_fast_electric` structural | `+ bonus 14` | Winner | Runner-up | Margin |
| :--- | ---: | ---: | :--- | ---: | ---: |
| `pelipper(water/flying,65)` + `regieleki(electric,250,[electric])` | −6 | **8** | `anti_fast_electric` | `anti_rain_core` 5 | **3** |
| `barraskewda(water,250)` + `regieleki(electric,250,[electric])` | 3 | **17** | `anti_fast_electric` | `anti_rain_core` 8 | **9** |
| `regieleki(electric,250,[electric])` + `dragapult(dragon/ghost,304)` | −3 | **11** | `anti_fast_electric` | `anti_fast_threats` 0 | **11** |
| `swampert(water/ground,60)` + `regieleki(electric,250,[electric])` | 5 | **19** | `anti_fast_electric` | `anti_water_ground` 4 | **15** |

**Correction recorded here, not in the plan.** The frozen plan's table row 4 prints the winner cell as
`5 → 17` with margin `13`. With the table's own `bonus=14` header the value is `5 + 14 = 19` and the margin
over `anti_water_ground` (4) is `15`; the printed `17` equals `5 + 12`. The plan's two prose restatements of
the margin list inherit the same slip. This was flagged by Plan Reviewer `R` as a non-blocking finding, and
the plan was deliberately **not** edited (it is a frozen immutable contract). The engine and the tests are
the authority: row 4 measures **19 / margin 15**, and the test asserts those values.

Rows 1–3 were measured exactly as the plan printed them (8/5 margin 3; 17/8 margin 9; 11/0 margin 11), and
the row-1 binding margin of 3 — the calibration's operative claim — is confirmed. The structural scores the
plan attributes to the competing presets on those boards (`anti_rain_core` 5 and 8, `anti_fast_threats` 0,
`anti_water_ground` 4) were also confirmed by direct measurement.

**Claim strength.** The precedence claim in this workstream is bounded to the boards enumerated above and to
the regression boards in `BlaineFastElectricLeadPresetTest.b9`: the matcher outranks the competing presets
on those boards by measured margins of 3, 9, 11 and 15. This is calibrated precedence, **not** a guarantee
against every possible board; a future board with unusually high structural scores for another preset could
still outscore it. That limitation is stated in the authoring contract's C.4 note.

### Freshness Status

- **Evaluation:** All evidence above was freshly executed on the current working tree, in this session,
  after the final edit to every source file. The full suite was run with `--rerun-tasks` (all tasks
  executed, no cached results), and no source file was modified after that run.
- **Provenance:** Working tree relative to `d63f833`; per-file blob hashes are recorded in
  `candidate_manifest.json` and in the file table below.

### File Identity (blob hashes of the 16 implementation files)

```text
df1469898a6109f4acd316f1feda98fad1c4b902  .agents/skills/competitive-pokemon-doubles-team-design/references/trainer-json-authoring-contract.md
1d9dd43d61c046bf5ac75dc4ae1619d0fe35b355  companion-mod/src/main/java/com/cobbleverse/legendaryrule/lead/AttemptScore.java
264c9a26be1402d17d1009c13b97a74cc573b1c7  companion-mod/src/main/java/com/cobbleverse/legendaryrule/lead/LeadAttempt.java
e951c109f6e6b5c1d4842befe6861a0c8dfaf8f5  companion-mod/src/main/java/com/cobbleverse/legendaryrule/lead/LeadSelectionConfig.java
a5b5291e07727d3b46cac86abc087dd627f1afe1  companion-mod/src/main/java/com/cobbleverse/legendaryrule/lead/LeadSelectionEngine.java
270d61d1d94140449a5c013bb016bc41b2e8a991  companion-mod/src/main/java/com/cobbleverse/legendaryrule/lead/LeadSelectionService.java
87d0a9b64e65f608658bd08e13c20a36fe57392c  companion-mod/src/main/java/com/cobbleverse/legendaryrule/lead/PlayerLeadTyping.java
4709a11fef213dc43bfbdb991203cee6cc3a0587  companion-mod/src/main/java/com/cobbleverse/legendaryrule/lead/adapter/CobblemonLeadAdapter.java
1a4ebb7ae50dee40877dd6e33706b8cd4c857ae3  companion-mod/src/test/java/com/cobbleverse/legendaryrule/lead/BlaineLeadSelectionTest.java
d58fab860325fc595eca5a67b46be731009fadf8  companion-mod/src/test/java/com/cobbleverse/legendaryrule/lead/LeadSelectionConfigTest.java
c8d02e8ff7c0b65bfb5070d31b3554e297e47d2a  datapacks/hell-mode/data/rctmod/trainers/kanto_blaine.json
a10975db5bf1ba3ccbe2f24f356b57a18361d295  scripts/ci/validate_repo.py
e046869dff760a44a6b4b8a75c4e1ed0be367b8a  companion-mod/src/main/java/com/cobbleverse/legendaryrule/lead/OpponentMatch.java
644be18b255803ccfe5d41530823e54a55221039  companion-mod/src/test/java/com/cobbleverse/legendaryrule/lead/OpponentMatchTest.java
86dccd5c486f24119b36015e15ba2c789bfd6c11  companion-mod/src/test/java/com/cobbleverse/legendaryrule/lead/BlaineFastElectricLeadPresetTest.java
1b9143d3fb3c934d42af21d116408ffe43460251  companion-mod/src/test/java/com/cobbleverse/legendaryrule/lead/adapter/CobblemonLeadAdapterMovesetTest.java
```

This document's own blob hash is recorded in `candidate_manifest.json`.

### Unverified / Skipped Items Requiring Live Verification

- **Layer 5 Live Canary: NOT CLAIMED.** Layers 0–2 establish syntax, schema conformance, and unit
  arithmetic only. Nothing in this evidence establishes that a live battle on a dedicated production host
  selects `anti_fast_electric`, that the RCT AI opens with Rillaboom + Mega Golisopod when it does, or that
  the Mega Golisopod aspect behaves as intended in-game. No local development client or CurseForge instance
  was used or may be described as production.
- **Dynamic move types (known limitation, unverified in-game).** `Weather Ball`, `Tera Blast`, `Hidden
  Power` and `Revelation Dance` report their *template* type, so a weather-resolved Electric move would not
  be seen as Electric by `damagingMoveType`. This does not affect the Electric-STAB threat class the rule
  targets; it is documented in the contract's residual-risk surface and is not measured here.
- **Roster-speed divergence at runtime (unverified offline).** The tests derive roster speeds from the
  datapack's IV/EV/nature values via `Turn1StatCalculator`; the live path uses
  `CobblemonLeadAdapter.resolveSpeed()` on a real `Pokemon`. If those diverge, only the fast-threshold
  boards shift, and `fasterThanRosterSlot` still compares against whatever `RosterMemberTyping.speed()`
  holds. Speeds 210 (Charizard) and 194 (Arcanine) are asserted in the existing
  `BlaineLeadSelectionTest.test10` and were re-confirmed by the full suite passing.
- **`Pokemon.getMoveSet()` returns the equipped moveset.** Asserted at the adapter boundary with a
  reflected `MoveSet` fixture; not observed on a live party Pokémon in a running client.
- **Plan Reviewer `R`'s Status-row observation (not fixed, per instruction):** the plan's Status table
  states "not a governance-contract task (no `AGENTS.md` / `.agents/skills/` routing changes)", yet this
  workstream does edit a manifest-listed authority file
  (`.agents/skills/competitive-pokemon-doubles-team-design/references/trainer-json-authoring-contract.md`),
  as frozen plan §6 item 10 and §8 Checkpoint 4 require. The edit was made per the plan; the Status row's
  characterization is left as written because the plan is frozen.
