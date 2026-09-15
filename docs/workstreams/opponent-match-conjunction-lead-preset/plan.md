# Workstream Plan: Per-Opponent Conjunction Matcher for Dynamic Lead Presets (Phase 1 — `opponentMatch` Primitive + Blaine `anti_fast_electric`)

## Status & Ownership

| Field | Value |
| :--- | :--- |
| **Workstream ID** | `opponent-match-conjunction-lead-preset` |
| **Document Role** | Candidate Workstream Plan (Ready for Independent Plan Review) |
| **Baseline Commit** | `466c595c1738ffa2ab7d69a53d824659c25348c4` (`main`) |
| **Target Branch** | `feat/opponent-match-conjunction-lead-preset` |
| **Canary Lineage** | N/A (no prior verified lineage for this primitive) |
| **Authority Closure** | N/A — not a governance-contract task (no `AGENTS.md` / `.agents/skills/` routing changes) |
| **Author Submission State** | `Candidate Plan (Ready for Review)` |

---

## 1. Context & Problem Statement

`LeadSelectionEngine` currently evaluates every authored lead preset using **board-aggregate** conditions only: `favoredAgainst` (type per opposing lead), `favoredAgainstSpecies` (species per opposing lead), and `minFastOpponents` (count of opposing leads above a derived speed threshold). Each condition is computed over the whole opposing board and summed (`LeadSelectionEngine.java:70-109`). As documented in `.agents/skills/competitive-pokemon-doubles-team-design/references/trainer-json-authoring-contract.md` Section D (lines 129-180), this makes a single-Pokémon conjunction inexpressible: a slow Electric lead and a fast non-Electric lead award the full conditional bonus even though **no single opposing Pokémon is a fast Electric threat**.

Consequently Blaine (`datapacks/hell-mode/data/rctmod/trainers/kanto_blaine.json`) cannot currently avoid leading Mega Charizard Y (`slot 1`, `fire/flying`) into a player lead that outruns it with a real Electric STAB move. His authored presets (`kanto_blaine.json:224-338`) are:
`anti_rain_core` `[5,3]`, `anti_water_ground` `[1,3]`, `anti_fast_threats` `[1,2]`, `default_sun_intimidate` `[1,0]` (declared default, `baseWeight` 2).

This workstream adds **one generic, composable, per-opponent conjunction primitive** (`opponentMatch`) to the existing additive scoring model, and uses it for exactly one new Blaine preset, `anti_fast_electric` → Rillaboom (`slot 3`) + Mega Golisopod (`slot 4`). No damage estimation, OHKO prediction, or matchup simulator is involved.

---

## 2. Concrete Facts vs. Assumptions vs. Conflicts

| Category | Item Description | Evidence / Citation | Impact / Resolution |
| :--- | :--- | :--- | :--- |
| **Confirmed Fact** | `PlayerLeadTyping` is a record of `(String species, List<String> types, int speed)` with a 3-arg and a 2-arg convenience constructor; `baseSpeed()` returns `speed`. | `companion-mod/src/main/java/com/cobbleverse/legendaryrule/lead/PlayerLeadTyping.java:9,18,22` | New `damagingMoveTypes` component must be appended while preserving both existing constructors. |
| **Confirmed Fact** | `RosterMemberTyping` is a record of `(int slot, String species, List<String> types, int speed)`. | `…/lead/RosterMemberTyping.java:8` | Unchanged; already carries resolved speed per roster slot. |
| **Confirmed Fact** | `LeadAttempt` is a 10-component record; `baseWeight` is strictly validated in `[-2,+2]` (throws), and **5** delegating constructors exist for backward compatibility: 9-arg `:62`, 7-arg `:66`, 6-arg `:70`, 5-arg `:74`, 4-arg `:78`. | `…/lead/LeadAttempt.java:11-22,35-37,62,66,70,74,78` | `baseWeight` cannot express a large precedence weight; a new primitive component is required. |
| **Confirmed Fact** | Parser is strict and per-attempt isolated: a malformed attempt is caught and skipped, valid siblings survive. | `…/lead/LeadSelectionConfig.java:113-138,133-135` | Malformed `opponentMatch` must throw inside `parseAttempt` so the attempt is skipped, not clamped. |
| **Confirmed Fact** | `parseExactInt` rejects fractional values, non-numeric primitives, and overflow; `parseNonBlankString` rejects blank/non-string. | `…/lead/LeadSelectionConfig.java:140-160` | Reuse for `fasterThanRosterSlot` and `bonus` validation; do not invent a second numeric parser. |
| **Confirmed Fact** | Engine builds `Map<Integer,RosterMemberTyping> rosterBySlot` from the **full** trainer team and derives `dynamicThreatSpeed` from the default preset's two slots. | `…/lead/LeadSelectionEngine.java:34-47` | `fasterThanRosterSlot` can resolve against the full roster with zero new plumbing. |
| **Confirmed Fact** | Per-attempt scoring loop iterates `playerLeads`, accumulating `offScore`, `defScore`, `typeFavoredBonus`, `speciesFavoredBonus`, then a single `fastBonus`; `total` is the plain sum. | `…/lead/LeadSelectionEngine.java:70-115` | New conjunctive bonus is one additional additive term evaluated **inside** the same per-opponent loop. |
| **Confirmed Fact** | Tie-breaking is `totalScore` desc → `baseWeight` desc → `declarationIndex` asc. | `…/lead/LeadSelectionEngine.java:117-122` | A satisfied conjunction must win **strictly** on total where required; ties are unreliable and are avoided by calibration. |
| **Confirmed Fact** | Default-preset resolution has three rules, evaluated in order: (1) first preset with `isDefault`; (2) first preset with `minFastOpponents == 0` and empty `favoredAgainst` and empty `favoredAgainstSpecies`; (3) `return attempts.get(0)`. | `…/lead/LeadSelectionEngine.java:128-142` | Rule 2's predicate must also exclude a non-null `opponentMatch`, otherwise a matcher-only preset could be mis-resolved as "unconditional". Rule 3 is retained as a documented last-resort fallback (see §5.3). |
| **Confirmed Fact** | Rule 3 is reachable in the current datapack: `kanto_koga.json` declares three presets, every one with a non-empty `favoredAgainstSpecies` and none with `default: true`, so neither rule 1 nor rule 2 can match and `default_sun` (index 0) resolves via rule 3. Blaine is the only preset-carrying trainer with `default: true`; erika, koga, ltsurge and sabrina have none. | `datapacks/hell-mode/data/rctmod/trainers/kanto_koga.json` (`leadPresets`); all five preset-carrying trainers enumerated (`kanto_blaine`, `kanto_erika`, `kanto_koga`, `kanto_ltsurge`, `kanto_sabrina`) | Rule 3 cannot be dismissed as dead code; its interaction with a matcher-bearing preset must be stated explicitly rather than assumed away. Koga's `fastBonus` path is inert (no Koga preset sets `minFastOpponents`), so no current outcome shifts. |
| **Confirmed Fact** | `AttemptScore` is `(attemptId, offensiveScore, defensiveScore, baseWeight, typeFavoredBonus, speciesFavoredBonus, fastBonus, totalScore)` plus 2 convenience constructors; constructed only at `LeadSelectionEngine.java:112`. | `…/lead/AttemptScore.java:6-22`; `grep -rn "new AttemptScore"` → single hit | Adding an evidence component touches exactly one call site; keep the 8-arg and 5-arg constructors. |
| **Confirmed Fact** | `LeadSelectionService` performs trainer-bound validation (slot `< team.length`, `expectedLeadMembers` drift guard), skips failing attempts with a warn log, and builds `npcRoster` over the whole team before calling the engine. | `…/lead/LeadSelectionService.java:134-175` | `fasterThanRosterSlot >= team.length` must be rejected here (mirrors the existing slot check at lines 139-143). |
| **Confirmed Fact** | Player leads resolve from the live party at battle creation: first two non-fainted `Pokemon`. | `…/lead/adapter/PlayerLeadResolver.java:21-41` | The engine's input is the actual battle-time party, satisfying "actual equipped moveset at battle-creation time". |
| **Confirmed Fact** | `CobblemonLeadAdapter.resolveSpeed(Pokemon)` = `pokemon.getSpeed()`, base-stat fallback if `<= 0`, then explicit Choice Scarf `floor(speed * 1.5)`; `isHoldingChoiceScarf` matches item path `choice_scarf`. | `…/lead/adapter/CobblemonLeadAdapter.java:52-100` | Speed authority is single-sourced; `fasterThanRosterSlot` must reuse it and must not re-apply the scarf. |
| **Confirmed Fact** | `toRosterMemberTyping` already stores `resolveSpeed(pokemon)` into `RosterMemberTyping.speed`. | `…/lead/adapter/CobblemonLeadAdapter.java:111-118` | Referenced roster speed arrives pre-resolved; engine compares against `RosterMemberTyping.speed()` verbatim. |
| **Confirmed Fact** | `Pokemon.getMoveSet()` → `MoveSet`; `MoveSet.getMoves()` → `List<Move>`; `Move.getType()` → `MoveTemplate.getElementalType()`; `Move.getDamageCategory()` → `MoveTemplate.getDamageCategory()`. | `javap` on `Cobblemon-fabric-1.7.3+1.21.1` jar (`Move.getType` / `Move.getDamageCategory` delegate via `getfield template`) | Authoritative runtime API for the damaging-move classification; no move-name allowlist needed. |
| **Confirmed Fact** | `DamageCategory` names registered by `DamageCategories` are exactly `"physical"`, `"special"`, `"status"`. | `javap -c` static init of `com.cobblemon.mod.common.api.moves.categories.DamageCategories` | Damaging = category name `!= "status"`. This is the authoritative status/damaging distinction. |
| **Confirmed Fact** | A damaging-move predicate **already exists** in the repository: `SpreadFriendlyFireValuationStrategy.isDamaging(Move)` (`public static`), which returns `false` when the category name is `"status"`, otherwise falls back to `move.getPower() > 0` — including a second `move.getPower() > 0` fallback inside its `catch (Throwable)` branch. | `…/strategy/spread/SpreadFriendlyFireValuationStrategy.java:278-294` | **Cross-package reuse was considered and rejected, with a deliberate behavioural divergence.** (a) *Divergence:* `extractDamagingMoveTypes` skips a move whose category is `null` (treating an unreadable category as "not established as damaging"), whereas `isDamaging` falls back to `getPower() > 0`; and the matcher never calls `getPower()` at all, because the matcher's subject is the *type* set, not a damage estimate. (b) *Reuse:* `SpreadFriendlyFireValuationStrategy` lives in `strategy.spread`; `CobblemonLeadAdapter` lives in `lead.adapter`. Neither package currently imports the other (`grep` for `import com.cobbleverse.legendaryrule.strategy` under `lead/` and for `import com.cobbleverse.legendaryrule.lead` under `strategy/` both return nothing in `src/main`), so reuse would introduce a **new** `lead.adapter → strategy.spread` dependency, coupling the lead subsystem to an AI valuation strategy. That is a larger architectural change than the primitive requires, so the adapter keeps its own four-line classification. The divergence is intentional and is pinned by `CobblemonLeadAdapterMovesetTest`. |
| **Confirmed Fact** | `MoveSet` holds `private final Move[] moves`; `Pokemon` holds a private `moveSet` field; existing tests set both via `Unsafe.allocateInstance` + reflection. | `javap -p` on `MoveSet`; `…/strategy/switchai/SwitchCandidateScorerTest.java:303-309`; `…/strategy/switchai/DeadMatchupDetectorTest.java:130-158` | An adapter-level test for damaging-move extraction is feasible using the established pattern. |
| **Confirmed Fact** | `kanto_blaine.json` roster slots: 0 `arcanine`, 1 `charizard` (`aspects:["mega_y"]`), 2 `moltres`, 3 `rillaboom`, 4 `golisopod` (`aspects:["mega"]`), 5 `zeraora`. | `datapacks/hell-mode/data/rctmod/trainers/kanto_blaine.json:24-223` (charizard aspect at `:88-90`, golisopod aspect at `:187-189`) | Desired leads resolve to `leadSlots [3,4]` with `expectedLeadMembers` `rillaboom` + `golisopod{requiredAspects:["mega"]}`. |
| **Confirmed Fact** | Resolved roster speeds (Gen 9 formula, level 70, from the datapack IV/EV/nature values): arcanine 194, charizard 210, moltres 215, rillaboom 145, golisopod 82, zeraora 298. Default preset `[1,0]` ⇒ dynamic threshold = max(210,194) = 210. | Recomputed with `Turn1StatCalculator` semantics — the formula is documented at `companion-mod/src/test/java/com/cobbleverse/legendaryrule/lead/simulation/calculator/Turn1StatCalculator.java:17-22` and implemented in `calculateStat` at `:89-92`, applied to the SPE stat from `:74-80` (note: this helper lives in **test** sources). Independently corroborated by `BlaineLeadSelectionTest.java:341-348`, which asserts exactly 210 and 194 | `moltres` (215) is faster than `charizard` (210), so the threshold depends on which preset is the declared default; `default_sun_intimidate` is `"default": true` (`kanto_blaine.json:319`), so rule 1 pins the default to `[1,0]` and the threshold stays 210. The new preset does **not** depend on this: `fasterThanRosterSlot: 1` pins Mega Charizard Y explicitly. |
| **Confirmed Fact** | The mod reads presets from the datapack trainer JSON (`trainers/<id>.json` → trainerId `kanto_blaine`) via `DynamicLeadResourceListener`. | `…/lead/DynamicLeadResourceListener.java:39-58` | Only `datapacks/hell-mode/.../kanto_blaine.json` needs editing. |
| **Confirmed Fact** | `!Doctors HELL MODE DOUBLE BATTLE EVERYTHING/` is an immutable read-only baseline enforced by CI. | `scripts/ci/check_legacy_baseline.py:1-13,13`; `.github/workflows/ci.yml:28-29` | The legacy duplicate `kanto_blaine.json` must NOT be touched. |
| **Confirmed Fact** | `validate_repo.py` validates `leadPresets` (slot bounds/type/species/default/drift guard) only for the modernized pack, and does **not** currently validate `minFastOpponents`, `fastSpeedThreshold`, or unknown keys. | `scripts/ci/validate_repo.py:203` (`validate_future_pack`), `:318-410` (`leadPresets` block) — no `minFastOpponents` handler exists | New matcher validation is a genuine addition. **Unknown-key behaviour is a stated boundary, not an oversight** (Finding-07): neither `LeadSelectionConfig.parseAttempt` (`:162-279`, which reads only keys it knows via `obj.has(...)`) nor `validate_repo.py` rejects an unknown **top-level** attempt key, so a typo such as `opponentMAtch` is silently ignored today — and because the resulting preset then has `minFastOpponents == 0` with empty favored lists, it also becomes eligible for `resolveDefaultAttempt` rule 2. This workstream closes the hole **for the new object only**: unknown sub-keys inside `opponentMatch` are rejected in both the parser and the validator (§5.2). Extending strict rejection to top-level attempt keys is a separate change with legacy-data blast radius across all preset-carrying trainers and is explicitly out of scope (§3.2). |
| **Confirmed Fact** | CI runs only `check_legacy_baseline.py`, `validate_repo.py`, and `scripts/compat-audit/test_audit.py`; Gradle tests are local-only. | `.github/workflows/ci.yml:28-35` | Layer 2 evidence must be produced locally and recorded in the verification manifest. |
| **Confirmed Fact** | No automated markdown/link/frontmatter checker exists in `scripts/`; the only governance gate script is `audit_review_gate.py`. | `find scripts -name "*.py"`; `.agents/skills/managed-agent-workflow/scripts/audit_review_gate.py` | Layer 0 claim must be limited to manual structural inspection + independent review. |
| **Assumption** | `Pokemon.getMoveSet()` returns the equipped 4-move `MoveSet` (not a learnset) for a party Pokémon at battle creation. | Needs adapter test verification | Verified indirectly by `PlayerLeadResolver` reading live party `Pokemon` (`PlayerLeadResolver.java:29-38`); a dedicated adapter test will assert it. |
| **Assumption** | `Move.getType()` returns the move template's base type, so moves with runtime-variable types (Weather Ball, Tera Blast, Hidden Power, Revelation Dance) report their template type rather than a weather/tera-resolved type. | Inferred from `Move.getType()` delegating to `MoveTemplate.getElementalType()`; consistent with `…/strategy/dynamic/DynamicMoveResolver.java:186-195` which synthesizes weather-ball surrogates because template type is not weather-aware | Accepted limitation; documented in the contract and in Residual Risks. Does not affect the Electric-STAB threat class. |
| **Assumption** | Resolved Blaine roster speeds at runtime via `CobblemonLeadAdapter.resolveSpeed` equal the JSON-derived values used by the tests. | Test harness derives speeds from JSON (`BlaineLeadSelectionTest.java:38-54`); runtime path uses `pokemon.getSpeed()` (`CobblemonLeadAdapter.java:52-81`) | Speeds are consumed only through `RosterMemberTyping.speed()`; if they diverge, only the fast-threshold boards shift, and `fasterThanRosterSlot` still compares against whatever the roster map holds. |
| **Conflict** | **C1 — Adding a fifth Blaine preset breaks an explicit count assertion.** `test15` asserts `presets.size() == 4`. | `BlaineLeadSelectionTest.java:463-465` | Update to 5; the loop body at `:477-489` then automatically drift-guards the new preset's `expectedLeadMembers`. |
| **Conflict** | **C2 — Aggregate scoring already gives another Blaine preset a higher structural score than Rillaboom+Golisopod on the Water board.** On the `pelipper + fast-Electric` board `anti_rain_core` scores 5 while `anti_fast_electric` scores −6 structurally. | Measured by a faithful port of `LeadSelectionEngine.java:70-115` over `typechart_gen9.json` (see §5.5 table) | A fixed small bonus (e.g. the existing `fastBonus = 4`) cannot satisfy the required precedence; a larger, authored weight is required. |
| **Open Question** | Q1 — Should `opponentMatch` also accept a species-based property (e.g. `speciesEquals`)? | Brief limits scope to three properties | Default: implement only `type`, `damagingMoveType`, `fasterThanRosterSlot`. Owner may extend later. |
| **Open Question** | Q2 — Should the new preset be declared before or after `default_sun_intimidate`? | Tie-break rule (`LeadSelectionEngine.java:117-122`) makes order observable only on exact ties | Default: declare **last** (index 4) so that on any exact tie the pre-existing presets keep winning, preserving current behaviour on non-matching boards. |

---

## 3. Scope Boundaries & Blast Radius

### 3.1 Strict In-Scope

| # | Path | Change |
| :--- | :--- | :--- |
| 1 | `companion-mod/src/main/java/com/cobbleverse/legendaryrule/lead/OpponentMatch.java` | **NEW** pure domain record for the conjunction matcher. |
| 2 | `…/lead/PlayerLeadTyping.java` | Append `List<String> damagingMoveTypes` component; preserve 3-arg/2-arg constructors. |
| 3 | `…/lead/LeadAttempt.java` | Append `OpponentMatch opponentMatch` component; preserve all existing constructors, `equals`, `hashCode`. |
| 4 | `…/lead/AttemptScore.java` | Append `int opponentMatchBonus` evidence component; preserve the 8-arg canonical and the 7-arg/5-arg convenience constructors. |
| 5 | `…/lead/LeadSelectionEngine.java` | Evaluate the matcher per attempt, add its bonus to `total`, extend default-attempt predicate. |
| 6 | `…/lead/LeadSelectionConfig.java` | Parse + strictly validate `opponentMatch`; reject malformed values. |
| 7 | `…/lead/LeadSelectionService.java` | Trainer-bound rejection of `fasterThanRosterSlot >= trainerTeam.length`. |
| 8 | `…/lead/adapter/CobblemonLeadAdapter.java` | Add `extractDamagingMoveTypes(Pokemon)`; feed it into `toPlayerLeadTyping`. |
| 9 | `datapacks/hell-mode/data/rctmod/trainers/kanto_blaine.json` | Append the `anti_fast_electric` preset (index 4). |
| 10 | `scripts/ci/validate_repo.py` | Validate `opponentMatch` shape/bounds/types against the roster. |
| 11 | `.agents/skills/competitive-pokemon-doubles-team-design/references/trainer-json-authoring-contract.md` | Move `opponentMatch` from future guidance to current vocabulary (Sections C/D/E/F/G/H). |
| 12 | `companion-mod/src/test/java/com/cobbleverse/legendaryrule/lead/OpponentMatchTest.java` | **NEW** generic matcher regressions. |
| 13 | `…/lead/BlaineFastElectricLeadPresetTest.java` | **NEW** Blaine-level regressions. |
| 14 | `…/lead/adapter/CobblemonLeadAdapterMovesetTest.java` | **NEW** damaging-move classification tests. |
| 15 | `…/lead/LeadSelectionConfigTest.java` | Add `opponentMatch` parsing/rejection tests. |
| 16 | `…/lead/BlaineLeadSelectionTest.java` | Update the preset-count assertion (`:465`) to 5. |

### 3.2 Explicit Out-of-Scope

- No Blaine roster changes (species, moves, items, abilities, EVs/IVs, aspects, `ai` block, `battleRules`).
- No Grassy Glide / Solar Beam / Weather Ball / any other valuation-strategy change.
- No RB or RCT AI move-selection change; no new Mixin; no new battle-runtime context; no `RCTMod` interception change.
- No headless server bootstrap required (no Mixin/runtime-injection change).
- No matchup simulator, no damage/OHKO estimator, no generic "threat engine", no Miraidon/Zeraora/Raichu/Tapu Koko species allowlist.
- No trainer-specific Java branch or constant (no `antiFastElectric()`-shaped helper).
- No hardcoding of move names or a damaging-move allowlist.
- No refactor of lead-selection architecture beyond the minimal contiguous path.
- No edit to `!Doctors HELL MODE DOUBLE BATTLE EVERYTHING/` (CI-enforced immutable baseline).
- No duplication of the authoring contract into `AGENTS.md` or the domain `SKILL.md`.
- No change to existing `favoredAgainst` / `favoredAgainstSpecies` / `minFastOpponents` / `fastSpeedThreshold` semantics.
- No strict unknown-key rejection for **top-level** attempt keys (`LeadSelectionConfig` / `validate_repo.py`); strict rejection is scoped to sub-keys inside the new `opponentMatch` object. Extending it to top-level keys would touch every existing preset across five trainers and is a separate change.
- No change to the frozen magnitudes of the existing bonuses (`LeadSelectionEngine.java:81,90,107`) and no attempt to re-express them as authored weights.
- No first-class precedence/priority tier, and no change to the tie-break comparator (`LeadSelectionEngine.java:117-122`) or to contract §C.3.
- No reuse of, and no edit to, `SpreadFriendlyFireValuationStrategy.isDamaging` (`strategy/spread/SpreadFriendlyFireValuationStrategy.java:281-294`); no new `lead.* → strategy.*` package dependency.

---

## 4. Architectural & Invariant Analysis

**Repository-global invariants**
1. *Read Before Write:* every signature cited above was verified against repository source or `javap` on the Cobblemon 1.7.3 jar; no signature is assumed from memory.
2. *Surgical Scope / Simplicity First:* one new record, four extended records, one adjacency in the engine, one parser branch, one validator branch, one adapter method, one JSON preset. No speculative abstraction and no second matcher property beyond the three required.
3. *Public Contract & Backward Compatibility:* every existing constructor form is retained. `PlayerLeadTyping`: canonical 3-arg plus the 2-arg convenience form (`PlayerLeadTyping.java:9,18`). `LeadAttempt`: canonical 10-arg (record header, `:11-22`) plus the 5 delegating forms at `:62,66,70,74,78`. `AttemptScore`: canonical 8-arg plus the 7-arg and 5-arg convenience forms (`AttemptScore.java:17,21`). Measured call sites for `new LeadAttempt(` under `companion-mod/src` use the 10-arg (5), 7-arg (52), 6-arg (3), 5-arg (20) and 4-arg (6) forms — 86 in total — so retaining every form is what keeps the existing corpus compiling. `LeadSelectionEngine.select`'s signature is unchanged.
4. *Claim Strength Discipline:* the precedence claim in §5.5 is stated as measured margins on enumerated boards, not as a universal guarantee.
5. *Zero in-repo transient artifacts:* calibration scratch work lives outside the repository.

**Domain invariants (routed to `competitive-pokemon-doubles-team-design` + its authoring contract)**
- Generic, parameter-driven, composable primitives only; no trainer-specific engine logic (contract §E.2).
- Per-opponent conjunction must be a **distinct** abstraction from board-aggregate conditions (contract §E.3).
- `LeadSelectionEngine` remains a pure algorithm: zero file I/O, zero Minecraft/Cobblemon/Fabric/Gson dependencies, zero static mutable state (contract §E.4). `OpponentMatch` must therefore live in the pure `lead` package and the adapter must own all Cobblemon API access.
- Turn-1 Gimmick Axiom unaffected: Mega Golisopod's `aspects:["mega"]` is preserved and drift-guarded, not altered.
- Trainer authoring precedence is calibrated with the existing scoring model, in config, not in Java. The operative invariant is the **config-vs-code boundary**: a weight whose correct value depends on one trainer's roster and on the presets competing with it is calibration data and belongs in that trainer's JSON file; a weight whose value reads the same for every trainer may be frozen in the engine (see §5.5).

---

## 5. Target Architecture & Slicing Strategy

Slices are ordered by dependency: contract/model → schema/parsing → pure logic → boundary/adapter → data → documentation → verification, per `slicing-and-dependency-strategies.md` §2. The parsing slice precedes the data slice deliberately: the config parser must understand `opponentMatch` before any trainer JSON declares one, otherwise the new preset is silently ignored (or, worse, silently mis-resolved as a default candidate).

### 5.1 Slice 1 — Contract & Model

**NEW `OpponentMatch.java` (pure record):**

```java
public record OpponentMatch(String type, String damagingMoveType, Integer fasterThanRosterSlot, int bonus)
```

Compact constructor rules:
1. `type` and `damagingMoveType` are trimmed + lowercased when non-null/non-blank; otherwise set to `null`.
2. `type` and `damagingMoveType`, when non-null, must be members of `TypeChartData.CANONICAL_TYPES` (`TypeChartData.java:32-36`); otherwise `IllegalArgumentException`.
3. `fasterThanRosterSlot`, when non-null, must be `>= 0`; otherwise `IllegalArgumentException`.
4. `bonus` must be in `[1, 16]`; otherwise `IllegalArgumentException`. Because the component is a primitive `int`, absence cannot be represented in the record — **presence is enforced by the parser** (§5.2), which rejects an `opponentMatch` object with no `bonus`. There is **no default**. The bound is a policy guard, not a derivation (§5.5).
5. **At least one** of `type` / `damagingMoveType` / `fasterThanRosterSlot` must be non-null. An empty matcher is rejected — a matcher with no condition would otherwise match unconditionally.
6. `bonus` is a **weight, not a condition**, and is therefore exempt from the "every property is optional" rule that governs `type`, `damagingMoveType` and `fasterThanRosterSlot`. Requiring it does not constrain the composition surface: all three condition properties remain independently omittable, and any subset (including one) is expressible.

`LeadAttempt` gains the component `OpponentMatch opponentMatch` (11th), normalized to `null` when absent, and an explicit 10-arg constructor delegating with `null` so every existing call site compiles unchanged. `equals`/`hashCode` gain the new component.

`PlayerLeadTyping` gains `List<String> damagingMoveTypes` (4th), normalized to an immutable lowercased distinct list, defaulting to `List.of()`; the existing 3-arg and 2-arg constructors delegate with `List.of()`.

`AttemptScore` gains `int opponentMatchBonus` (9th) plus a retained explicit 8-arg constructor defaulting it to `0` (the existing 7-arg and 5-arg convenience constructors then chain through it unchanged).

### 5.2 Slice 2 — Schema & Parsing (Config, Service, CI Validator)

This slice owns the three surfaces that must agree on the schema, per contract §H.3 (`trainer-json-authoring-contract.md:340-347`). It must land **before the data slice (Slice 5)**: the parser has to understand `opponentMatch` before any trainer JSON declares one.

**1. `LeadSelectionConfig.parseAttempt` (`LeadSelectionConfig.java:162-279`) gains an `opponentMatch` branch:**

| Condition | Behaviour |
| :--- | :--- |
| `opponentMatch` present but not a JSON object | throw → attempt skipped |
| `bonus` absent, non-exact-integer, or outside `[1,16]` | throw → attempt skipped |
| an unknown sub-key inside `opponentMatch` | throw → attempt skipped |
| `type` / `damagingMoveType` present but not a string, or not in `TypeChartData.CANONICAL_TYPES` | throw → attempt skipped |
| `fasterThanRosterSlot` present but not an exact integer, or `< 0` | throw → attempt skipped |
| all three condition fields absent | throw → attempt skipped |

Every error is thrown from `parseAttempt`, so the existing per-attempt `try/catch` at `LeadSelectionConfig.java:113-138` skips exactly that attempt and its valid siblings survive. `parseExactInt` (`:140-153`) and `parseNonBlankString` (`:155-160`) are reused; no second numeric parser is introduced. **Unknown sub-key rejection is scoped to the `opponentMatch` object** — see the Finding-07 boundary stated in §2 and §3.2.

**2. `LeadSelectionService.selectLeadWithConfig` (`:134-175`)** gains, alongside the existing slot check at `:139-143`:

```java
Integer refSlot = attempt.opponentMatch() != null ? attempt.opponentMatch().fasterThanRosterSlot() : null;
if (refSlot != null && refSlot >= trainerTeam.length) { /* warn + continue (skip attempt) */ }
```

The evidence log at `:178-184` currently prints `baseWeight`/`type`/`spec` but **not** `fastBonus`; `opponentMatchBonus` is appended so the new component is visible in the runtime log. `fastBonus`'s omission is pre-existing and is left unchanged to keep the delta surgical.

**3. `scripts/ci/validate_repo.py`** gains an `opponentMatch` block inside the existing preset loop (`:318-410`), rejecting the same set as the parser, plus `fasterThanRosterSlot >= team length`. Because the validator and the parser are separate implementations, the same fixtures are asserted against both (contract §H.3).

### 5.3 Slice 3 — Pure Logic (Engine)

Inside the existing per-attempt loop (`LeadSelectionEngine.java:52-115`), after `fastBonus`:

```
opponentMatchBonus = 0
if attempt.opponentMatch() != null:
    refSlot = match.fasterThanRosterSlot()
    ref = (refSlot == null) ? null : rosterBySlot.get(refSlot)
    resolvable  = (refSlot == null) || (ref != null && ref.speed() > 0)
    if resolvable:
        for player in playerLeads:                    # exists-quantifier over opposing leads
            if (match.type() == null || player.types().contains(match.type()))
            && (match.damagingMoveType() == null || player.damagingMoveTypes().contains(match.damagingMoveType()))
            && (ref == null || player.speed() > ref.speed()):   # STRICT >
                opponentMatchBonus = match.bonus()
                break
total = offScore + defScore + baseWeight + typeFavoredBonus + speciesFavoredBonus + fastBonus + opponentMatchBonus
```

Semantics that must hold:
- **Same-opponent AND.** All present properties are tested on one `PlayerLeadTyping` instance inside one loop iteration; satisfaction is never assembled across different opposing leads.
- **Omitted property = unconstrained.** A `null` property is skipped, never treated as false.
- **Exists, not all.** One qualifying opposing lead suffices; the second is irrelevant.
- **Strict inequality** for `fasterThanRosterSlot` (`player.speed() > ref.speed()`), matching the dynamic-threshold convention at `LeadSelectionEngine.java:101`.
- **Reuse of speed authority.** The comparison uses `PlayerLeadTyping.speed()` and `RosterMemberTyping.speed()`, both produced by `CobblemonLeadAdapter.resolveSpeed()`; the engine never re-applies Choice Scarf.
- **Degeneracy guard.** A referenced roster slot that is absent from `rosterBySlot`, or whose resolved speed is `<= 0`, makes the property unsatisfiable (no bonus) rather than vacuously true.

**Default-attempt predicate (Finding-02, resolved by option (b)).** `resolveDefaultAttempt` (`:128-142`) rule 2 additionally requires `attempt.opponentMatch() == null`, so a matcher-bearing preset is never treated as "unconditional". Rule 3 (`return attempts.get(0)`, `:141`) is deliberately **retained unchanged as the documented last-resort fallback**; option (a) — extending rule 3 to also skip matcher-bearing presets — was rejected because it would require defining behaviour for trainers that have no conditionless preset at all, widening the change into contract §C.4 default-resolution semantics for every preset-carrying trainer.

**Rule-3 boundary, stated explicitly rather than assumed away.** Rule 3 is live today: `kanto_koga.json` declares three presets, all with non-empty `favoredAgainstSpecies` and none with `default: true`, so rule 1 and rule 2 both fail and `default_sun` (index 0) supplies `dynamicThreatSpeed` (`LeadSelectionEngine.java:141`). This change does not alter that path — Koga has no matcher-bearing preset. The residual, documented boundary is narrower: if a **future** trainer had a matcher-bearing preset as its only rule-3 candidate, that preset's `leadSlots` would supply `dynamicThreatSpeed`. That is pre-existing rule-3 behaviour, not behaviour introduced here; it is recorded in §9 and is what the G12 fixture is specified to exercise (rule 2, not rule 3).

### 5.4 Slice 4 — Boundary / Adapter

`CobblemonLeadAdapter` gains:

```java
public static List<String> extractDamagingMoveTypes(Pokemon pokemon)
```
- Returns `List.of()` when `pokemon == null` or `getMoveSet() == null`.
- Iterates `pokemon.getMoveSet().getMoves()`, skips `null` moves, and for each move: skips when `getDamageCategory()` is `null` or its `getName()` equals `"status"` (case-insensitive); otherwise adds `getType().getName().toLowerCase(Locale.ROOT)` when non-blank. De-duplicates via `LinkedHashSet`, returns `List.copyOf`.
- Whole loop wrapped in `try/catch (Throwable ignored)` returning the accumulated result, consistent with the adapter's existing defensive posture (`CobblemonLeadAdapter.java:56-60,74-75,97-99`).
- Deliberate divergence from the pre-existing `SpreadFriendlyFireValuationStrategy.isDamaging` (`strategy/spread/SpreadFriendlyFireValuationStrategy.java:281-294`) and the reason cross-package reuse was rejected are recorded in §2.
- `toPlayerLeadTyping` passes the result into the new `PlayerLeadTyping` component.

The trainer-bound `fasterThanRosterSlot` check and the evidence-log change live in Slice 2 (§5.2), not here — they are config-boundary validation, not Cobblemon API access.

### 5.5 Slice 5 — Scoring Integration & Calibration

**Schema (final, authoritative):**

| Field | Type | Bounds / Rules |
| :--- | :--- | :--- |
| `opponentMatch` | Object, optional | Sub-fields below. Rejected if not a JSON object, if every condition field is absent, if `bonus` is absent, if any value is malformed, or if an unknown sub-key is present. |
| `opponentMatch.type` | String, optional | Must be a canonical Gen 9 type (lowercase after normalization), validated against `TypeChartData.CANONICAL_TYPES` and `VALID_TYPES` (`validate_repo.py:24-28`). |
| `opponentMatch.damagingMoveType` | String, optional | Same type validation. Matches a **damaging** (non-`status`) equipped move of that type. |
| `opponentMatch.fasterThanRosterSlot` | Integer, optional | Exact non-negative integer; must be `< team.length` (CI + service). References the **resolved** speed of that NPC roster slot. |
| `opponentMatch.bonus` | Integer, **mandatory when `opponentMatch` is present** | Exact integer in `[1,16]`. **No default.** Additive score awarded when the conjunction is satisfied. |

**Required precedence, measured.** A faithful port of the engine scoring (Slice 3 + type chart + measured roster speeds) yields:

| Board (player leads) | `anti_rain_core` | `anti_water_ground` | `anti_fast_threats` | `default_sun_intimidate` | `anti_fast_electric` structural | Winner with `bonus=14` |
| :--- | ---: | ---: | ---: | ---: | ---: | :--- |
| `pelipper(water/flying,65)` + `regieleki(electric,250, dmg electric)` | 5 | −7 | −8 | −5 | −6 → **8** | `anti_fast_electric` (margin 3) |
| `barraskewda(water,250)` + `regieleki(electric,250, dmg electric)` | 8 | −2 | −4 | −5 | 3 → **17** | `anti_fast_electric` (margin 9) |
| `regieleki(electric,250)` + `dragapult(dragon/ghost,304)` | −3 | −4 | 0 | −1 | −3 → **11** | `anti_fast_electric` (margin 11) |
| `swampert(water/ground,60)` + `regieleki(electric,250, dmg electric)` | 2 | 4 | −8 | −5 | 5 → **17** | `anti_fast_electric` (margin 13) |

Without the matcher bonus, `anti_fast_electric` scores −6 / 3 / −3 / 5 on rows 1–4 and **loses** rows 1 and 3 (row 1: −6 vs `anti_rain_core` 5; row 3: −3 vs `anti_fast_threats` 0). Because `baseWeight` is capped at `±2` (`LeadAttempt.java:35-37`), precedence cannot come from `baseWeight`. The weight is therefore **authored in the trainer JSON** (`bonus: 14`).

**The config-vs-code boundary is the axis here, not the cap arithmetic.** The rule the model actually follows is: a weight whose correct value reads the same for every trainer may be frozen in the engine; a weight whose correct value depends on one trainer's roster and on the presets competing with it is calibration data and belongs in that trainer's file. `2` is such a universal unit — `typeFavoredBonus += 2` (`LeadSelectionEngine.java:81`) reads as "one lead the type chart favours is worth 2" for any trainer and any roster. `12` and `14` are not: they carry meaning only against Blaine's roster and against `anti_rain_core` / `anti_fast_threats` / `default_sun_intimidate`. A constant chosen so that Blaine's preset wins would be trainer-specific logic inside generic engine code, which contract §E.1/§E.2 (`trainer-json-authoring-contract.md:187-188`) forbids — merely relocated into a `private static final int`.

**The "every other bonus is already authored" premise is false, and is recorded here so it is not repeated.** `favoredAgainst` (+2 per matching lead, cap +4) is frozen at `LeadSelectionEngine.java:81`; `favoredAgainstSpecies` (+2 per match, cap +4) at `:90`; the `minFastOpponents` bonus (+4 flat) at `:107`. Only `baseWeight` (`LeadAttempt.java:35-37`, `[-2,+2]`) is authored. The model's convention is **condition-authored, weight-frozen**. `opponentMatch` is a deliberate, narrow exception to the *weight* half of that convention, justified by the boundary above — it is not an application of a uniformity rule, because no such rule exists.

**A frozen engine constant would be Blaine-conditioned too, so `bonus` does not buy expressiveness — it buys the location of the number.** Measured: a frozen constant of `12` already satisfies all four required boards (row 1 `−6 + 12 = 6 > 5`, margin 1; row 2 `3 + 12 = 15 > 8`; row 3 `−3 + 12 = 9 > 0`; row 4 `5 + 12 = 17 > 4`). And `12` is not an independent quantity either — it is exactly "one more than the 11-point deficit on the `pelipper + regieleki` board". Neither option escapes the fact that the number is fitted to Blaine's roster; freezing it in Java relocates the fit from a reviewable config line into engine code that applies to every trainer. `bonus` is the option that keeps the number where it can be read, diffed, and re-calibrated per trainer.

**`[1,16]` is a policy bound, not a derivation.** No principle states that a conjunction may not outweigh the sum of the board-aggregate conditions; the aggregate maximum happens to be 12 (`typeFavored` 4 + `speciesFavored` 4 + `fastBonus` 4), but deriving `16 = 12 + 2` from it would be post-hoc arithmetic dressed as a principle. The bound's actual function is a **sanity guard**: it holds `bonus` within the order of magnitude of the other score components so that a typo (`bonus: 1400`) cannot silently dominate every board. The contract must state it in those terms. The lower bound of `1` is likewise a policy choice, not an arithmetic one: `bonus: 0` describes a matcher that cannot change any outcome, which is a config smell rather than a meaningful authoring intent.

**`bonus: 14` is Blaine calibration, not a reusable semantic weight.** The binding board is row 1: `anti_fast_electric` scores −6 structurally against `anti_rain_core`'s 5, so any value `>= 12` wins it. `12` and `14` produce identical winners on **every** board enumerated in this plan — the four required boards and all eleven regression boards (the regression boards are matcher-false, so the bonus is 0 there) — and they differ only in margin on the required boards: 1/7/9/13 at `12` versus 3/9/11/13 at `14`. `14` was therefore chosen over `12` solely to hold margin 3 instead of margin 1 on the binding board, as headroom against a future competing preset. That is a judgment about headroom, not a derived quantity. A future author must re-derive their own value from their own competing presets; copying `14` is not meaningful.

**No default (Owner directive).** An omitted `bonus` would produce a **silent weak behaviour**: config that is valid, parses, runs, and does not deliver the precedence it looks like it should — a false green. The parser cannot distinguish "the author meant a small number" from "the author forgot", so any fixed default is either inert or an unearned weight attached to a condition. Requiring `bonus` costs the author one integer and forces the calibration decision into the diff, which is the same fail-loud posture the parser already takes for `baseWeight` range violations (`LeadSelectionConfig.java:186-190`). Making it mandatory **does not** restrict composition: `bonus` is a weight, not a condition, so `type`, `damagingMoveType` and `fasterThanRosterSlot` each remain independently optional and any subset remains expressible (§5.1 rule 6).

**Precedence tier: considered and rejected.** A first-class `priority` component is feasible in this model — one new `LeadAttempt` component, an extra leading term in the comparator at `LeadSelectionEngine.java:117-122`, plus parser, validator and contract surface; all small. It was rejected for three reasons: (i) the brief directs calibrating precedence *within the existing scoring model* rather than adding a precedence branch; (ii) a tier changes the tie-break contract itself (`trainer-json-authoring-contract.md:107-111`, §C.3), which governs all five preset-carrying trainers, whereas `bonus` adds one additive term and leaves ordering semantics untouched; (iii) the review delta is smaller. The accepted cost is a legibility one — `bonus` performs precedence work while being named a bonus — and the contract must therefore state explicitly that a matcher whose `bonus` exceeds the aggregate maximum is acting as a **precedence declaration** whose value has to be calibrated against the structural scores of the competing presets, not read as an ordinary weight. The tier remains the honest upgrade path if categorical (rather than calibrated) precedence is ever needed.

`anti_fast_electric` keeps `baseWeight: 0`. This is required, not cosmetic: with `baseWeight: 2` the preset's matcher-free structural score on the existing Miraidon + Iron Hands board (−5 + 2 = −3) would beat `default_sun_intimidate` on that board and break `BlaineLeadSelectionTest.test1`. On the Miraidon board `default_sun_intimidate` totals **−4** (offScore −2, defScore −4, baseWeight +2), consistent with the −4 in the regression list below and in §7.3's B9, so `−3 > −4` and the regression does break. (An earlier draft of this paragraph wrote `default_sun_intimidate (−5)` here; that was a transcription error against the same plan's own −4, and it does not change the conclusion.)

**Blaine preset JSON to append (declared last, index 4):**

```json
{
  "id": "anti_fast_electric",
  "leadSlots": [3, 4],
  "expectedLeadMembers": [
    { "species": "rillaboom" },
    { "species": "golisopod", "requiredAspects": ["mega"] }
  ],
  "baseWeight": 0,
  "opponentMatch": {
    "type": "electric",
    "damagingMoveType": "electric",
    "fasterThanRosterSlot": 1,
    "bonus": 14
  },
  "description": "Rillaboom + Mega Golisopod (Grass STAB + Mega Tough Claws) vs a fast Electric STAB lead that outruns Mega Charizard Y"
}
```
Slot 3 = `rillaboom`, slot 4 = `golisopod` (`kanto_blaine.json:124-190`); `requiredAspects:["mega"]` matches `kanto_blaine.json:187-189` and preserves the Mega drift guard. `fasterThanRosterSlot: 1` = Mega Charizard Y, the referenced survival rule.

**Regression safety of appending a fifth preset.** On every matcher-false board enumerated in §7, `anti_fast_electric` scores strictly below the previous winner (measured: rain core 7 vs 1; water/ground 7 vs 4; single-water 7 vs −9; single-ground 7 vs 2; double-fast 4 vs −2; single-fast 3 vs −2; mid-speed 8 vs −6; Miraidon −4 vs −5; swift-swim 7 vs 4; scarf 4 vs −5; neutral 8 vs −6). Appending last additionally makes existing presets win any exact tie via `declarationIndex`.

### 5.6 Slice 6 — Documentation & Contract Update

`validate_repo.py` is owned by Slice 2 (§5.2) — it is the same schema concern as the parser, and contract §H.3 requires the two to move together. This slice is documentation only.

The authoring contract (`.agents/skills/competitive-pokemon-doubles-team-design/references/trainer-json-authoring-contract.md`) is updated in the same branch:

| Section | Edit |
| :--- | :--- |
| C.1 field table (`:64-76`) | Add the `opponentMatch` row plus its four sub-field constraints, with `bonus` marked **mandatory** and `[1,16]` marked a policy bound. |
| C.2 scoring pipeline (`:79-95`) | Add `+ opponentMatchBonus` to the formula and state that `bonus` is the one author-calibrated weight in the pipeline. |
| C.3 tie-break (`:107-111`) | Unchanged — and say so explicitly, so a reader sees the tier was not adopted. |
| C.4 default resolution (`:113-117`) | Rule 2 additionally excludes matcher-bearing presets; rule 3 remains the last-resort fallback, with the Koga case (no `default: true`, no conditionless preset) named as the live example. |
| New note (insert after C.4) | State that a matcher whose `bonus` exceeds the aggregate maximum (12) is acting as a **precedence declaration**, whose value must be calibrated against the competing presets' structural scores. This is the legibility guard for the `bonus`-named-precedence cost accepted in §5.5. Placed as a note rather than a new numbered subsection so C.5 (`Semantic Drift Guard`, `:119-127`) keeps its number and existing references stay valid. |
| D (`:129-180`) | The conjunction warning now points at the supported primitive and contrasts it with aggregate predicates such as `minFastOpponents`; the false-positive walkthrough is retained and re-labelled as the behaviour `opponentMatch` fixes. |
| E (`:183-191`) | The per-opponent conjunction abstraction now exists (E.3)); the no-trainer-specific-Java rule (E.1/E.2) stands and is cited as the reason `bonus` is authored rather than frozen. |
| F (`:194-199`) | `opponentMatch` moves to Active Syntax with its rejection rules, **and** an explicit annotation that strict rejection is currently enforced only for sub-keys inside the new object, while unknown **top-level** attempt keys remain permissive (the Finding-07 boundary). |
| G (`:203-316`) | Add a cookbook pattern for the conjunction matcher, and note that the former "Anti-Pattern: Conjunction Fallacy via Independent Fields" (`:299-311`) is now expressible. |
| H.3 (`:340-347`) | Synchronous parser/engine/validator/contract updates, extended to name the `opponentMatch` sub-key surface. |
| **H.2 (`:332-337`)** | **Correct the Layer 2 command.** The section currently reads `./gradlew :companion-mod:test --tests "com.cobbleverse.legendaryrule.lead.*"` (`:336`), which targets a subproject that does not exist — `companion-mod/settings.gradle:12` sets `rootProject.name = 'rct-legendary-rule-companion'` and includes no subprojects, so the `:companion-mod:` project selector cannot resolve. Replace with `cd companion-mod && ./gradlew test --tests "com.cobbleverse.legendaryrule.lead.*"`, matching `README.md:68` and `docs/workstreams/turn1-lead-simulation-harness/verification.md:14-15` (`companion-mod/gradlew.bat test --tests ...`). |

The contract currently stands at 349 lines; the Layer 0 bound is `< 500` lines per file (`.agents/skills/test-and-verification-strategy/references/verification-layers-and-tooling.md:13,111`), so the additions must be written to fit inside that budget rather than appended without limit.

---

## 6. Exact Files to Create / Modify / Delete

### Files to Create
1. `companion-mod/src/main/java/com/cobbleverse/legendaryrule/lead/OpponentMatch.java` — Slice 1.
2. `companion-mod/src/test/java/com/cobbleverse/legendaryrule/lead/OpponentMatchTest.java` — Slice 3 (G1–G12).
3. `companion-mod/src/test/java/com/cobbleverse/legendaryrule/lead/BlaineFastElectricLeadPresetTest.java` — Slice 5 (B1–B12).
4. `companion-mod/src/test/java/com/cobbleverse/legendaryrule/lead/adapter/CobblemonLeadAdapterMovesetTest.java` — Slice 4.

### Files to Modify
1. `…/lead/PlayerLeadTyping.java` (Slice 1): append `damagingMoveTypes`; retain 3-arg/2-arg constructors.
2. `…/lead/LeadAttempt.java` (Slice 1): append `opponentMatch`; retain all 5 delegating constructors; extend `equals`/`hashCode`.
3. `…/lead/AttemptScore.java` (Slice 1): append `opponentMatchBonus`; retain the 8-arg canonical plus the 7-arg/5-arg convenience constructors.
4. `…/lead/LeadSelectionEngine.java` (Slice 3): matcher evaluation + additive term + evidence + default-predicate tightening.
5. `…/lead/LeadSelectionConfig.java` (Slice 2): parse and strictly validate `opponentMatch`, including rejection of unknown sub-keys.
6. `…/lead/LeadSelectionService.java` (Slice 2): reject `fasterThanRosterSlot >= team.length`; add `opponentMatchBonus` to the evidence log.
7. `…/lead/adapter/CobblemonLeadAdapter.java` (Slice 4): `extractDamagingMoveTypes` + wire into `toPlayerLeadTyping`.
8. `datapacks/hell-mode/data/rctmod/trainers/kanto_blaine.json` (Slice 5): append `anti_fast_electric` (index 4).
9. `scripts/ci/validate_repo.py` (Slice 2): `opponentMatch` validation.
10. `.agents/skills/competitive-pokemon-doubles-team-design/references/trainer-json-authoring-contract.md` (Slice 6): current-vocabulary documentation, including the §H.2 command correction.
11. `…/lead/LeadSelectionConfigTest.java` (Slice 2): `opponentMatch` parse/reject tests.
12. `…/lead/BlaineLeadSelectionTest.java` (Slice 5): preset count 4 → 5.

### Files to Delete / Prune
None.

---

## 7. Orthogonal Verification Strategy (Finding E)

### 7.1 Minimal covering set
Java source, datapack JSON, and CI tooling are all modified, so **Layers 1 and 2 both apply** (orthogonal, no inheritance). Layer 0 applies only to the documentation edits and has no automated checker. Layers 3–5 are inapplicable and are explicitly skipped: no Mixin, no bytecode/shadow contract, no runtime injection, no gameplay-logic change reachable only in a live battle.

### 7.2 Commands

| Layer | Command | Pass criterion |
| :--- | :--- | :--- |
| 1 (Datapack & trainer schema) | `python scripts/ci/validate_repo.py` | exit 0, `RESULT: ALL REPOSITORY & DATA VALIDATIONS PASSED` |
| 1 (baseline immutability) | `python scripts/ci/check_legacy_baseline.py` | exit 0, legacy digest unchanged |
| 2 (focused) | `cd companion-mod && ./gradlew test --tests "com.cobbleverse.legendaryrule.lead.*"` | exit 0 |
| 2 (full companion suite) | `cd companion-mod && ./gradlew test --rerun-tasks` | exit 0 |
| 2 (build) | `cd companion-mod && ./gradlew build` | exit 0 |
| 0 (documentation) | Manual relative-link/path inspection of the two edited documents + independent contract review | no broken relative links; documented syntax matches parser/validator behaviour; **each edited markdown file remains `< 500` lines** (the Layer 0 file-line bound defined at `.agents/skills/test-and-verification-strategy/references/verification-layers-and-tooling.md:13,111` and `…/SKILL.md:45`) |
| CI parity | `python -m unittest scripts/compat-audit/test_audit.py -v` | exit 0 |
| Corroboration (optional) | `python scripts/runtime-contract/test_typechart_parity.py` | exit 0 — corroborates that `typechart_gen9.json`, on which the whole §5.5 precedence argument rests, is unmodified. Not a gate; recorded in the evidence manifest. |

**Command-form note (Finding-01).** The focused Layer 2 command is `cd companion-mod && ./gradlew test --tests …`, **not** `./gradlew :companion-mod:test …`. `companion-mod/settings.gradle:12` sets `rootProject.name = 'rct-legendary-rule-companion'` and includes no subprojects, so the `:companion-mod:` project selector has nothing to resolve; the only wrapper is `companion-mod/gradlew` (`companion-mod/gradlew.bat` on Windows). The same incorrect form exists in the authority file at `trainer-json-authoring-contract.md:336` and is corrected by Slice 6.

### 7.3 Test plan

**`OpponentMatchTest.java`** — generic matcher regressions (engine-level, pure). Roster per test is explicit so results do not depend on datapack drift; scorer built from `TypeChartResourceLoader.load()` as in `LeadSelectionEngineTest.java:17-23`.

| ID | Scenario | Assertion |
| :--- | :--- | :--- |
| G1 | `{"type":"water"}` only, opponent `water/flying` | bonus awarded; `AttemptScore.opponentMatchBonus()` equals the authored value |
| G2 | `{"damagingMoveType":"ground"}`, opponent with `damagingMoveTypes=[ground]` | bonus awarded |
| G3 | `{"fasterThanRosterSlot":1}` only, opponent speed > roster slot 1 speed | bonus awarded |
| G4 | All three properties present, one opponent satisfies all three | bonus awarded exactly once (`break` after first match) |
| G5 | **Split-condition false positive:** Opponent A `electric` + damaging electric + slow; Opponent B non-electric + fast | bonus **not** awarded (0) |
| G6 | `damagingMoveType:"electric"` with a board whose only Electric source is a status move | bonus **not** awarded (adapter classification asserted separately in `CobblemonLeadAdapterMovesetTest`) |
| G7 | `fasterThanRosterSlot` referencing an absent roster slot | bonus **not** awarded |
| G8 | `fasterThanRosterSlot` referencing a roster slot whose speed is `0` | bonus **not** awarded |
| G9 | Boundary: opponent speed exactly equal to referenced roster speed | bonus **not** awarded (strict `>`) |
| G10 | `opponentMatch` absent | `opponentMatchBonus() == 0`, all pre-existing fields unchanged |
| G11 | Two presets identical except one carries a satisfied matcher | matcher-bearing preset wins strictly |
| G12 | `resolveDefaultAttempt` **rule-2 guard**. Fixture must be: an attempt carrying a satisfied `opponentMatch` and no other condition, declared **first**; a **conditionless** attempt declared behind it; and **no** `isDefault` anywhere. | rule 2 skips the matcher-bearing attempt and resolves the conditionless attempt; assert the resolved threat-speed source is the conditionless attempt's slot maximum, **not** the matcher-bearing attempt's. (Without the trailing conditionless attempt the fixture would resolve via rule 3; with `isDefault` set it would resolve via rule 1. Either variant passes without touching the new guard, which is why this fixture is specified exactly.) |

**`BlaineFastElectricLeadPresetTest.java`** — datapack-loaded regressions reusing `BlaineLeadSelectionTest.loadBlainePresetsFromDatapack()` and `loadBlaineRosterFromDatapack()` (both `public static`, `BlaineLeadSelectionTest.java:56,81`).

| ID | Player leads | Assertion |
| :--- | :--- | :--- |
| B1 | `pelipper(water/flying,65)` + `regieleki(electric,250,[electric])` | selected `anti_fast_electric`, `leadSlots == [3,4]` |
| B2 | Same but `damagingMoveTypes = []` | not `anti_fast_electric` (`anti_rain_core` selected on the measured board) |
| B3 | `pelipper` + `ampharos(electric,180,[electric])` | not `anti_fast_electric` (speed ≤ 210) |
| B3b | `pelipper` + electric lead at exactly 210 with a damaging Electric move | not `anti_fast_electric` (strict `>`) |
| B4 | `pelipper` + `dragapult(dragon/ghost,304,[electric])` | not `anti_fast_electric` (type is not Electric) |
| B5 | `ampharos(electric,180,[electric])` + `dragapult(dragon/ghost,304,[dragon])` | not `anti_fast_electric`; `default_sun_intimidate` selected |
| B6 | `swampert(water/ground,60)` + `regieleki(electric,250,[electric])` | selected `anti_fast_electric` |
| B7 | `regieleki(electric,250,[electric])` + `dragapult(dragon/ghost,304,[dragon])` | selected `anti_fast_electric`; `anti_fast_threats` score is strictly lower although its legacy `minFastOpponents: 2` holds |
| B8 | `corviknight(steel/flying,67)` + `clefable(fairy,60)` | selected `default_sun_intimidate` |
| B9 | Existing boards: true rain core, single water + Scizor, water/ground pair, single ground + Scizor, double fast non-Electric, single fast + Clefable, mid-speed pair, Miraidon + Iron Hands, swift-swim pair, Choice-Scarf Gengar + Dragapult | unchanged winners and unchanged `totalScore` values (7, 7, 7, 7, 4, 3, 8, −4, 7, 4) |
| B10 | Choice Scarf: player Electric lead at raw 150 → adapter-resolved 225 > 210 | matcher satisfied, **no** double-counted ×1.5 |
| B11 | Preset inventory | 5 presets; `anti_fast_electric` declared last; exactly one `default: true` |
| B12 | `expectedLeadMembers` drift guard for the new preset against the real roster identity list | `rillaboom` matches slot 3; `golisopod` with `requiredAspects:["mega"]` matches slot 4 |

**`CobblemonLeadAdapterMovesetTest.java`** — damaging-move classification via the established `Unsafe.allocateInstance` + reflection pattern (`DeadMatchupDetectorTest.java:130-158`, `SwitchCandidateScorerTest.java:303-309`), setting `Move.template` → `MoveTemplate.{name,elementalType,damageCategory,power}` and `Pokemon.moveSet`:
`thunderbolt`(electric/special) → `["electric"]`; `thunderwave`(electric/status) → `[]`; `protect`(normal/status) → `[]`; `voltswitch`(electric/special) + `wildcharge`(electric/physical) + `protect` → `["electric"]` (deduplicated); `earthquake`(ground/physical) + `thunderwave`(electric/status) → `["ground"]`; null `Pokemon` / null `MoveSet` → `[]`.

**`LeadSelectionConfigTest.java`** — additions asserting per-attempt isolation for: valid 3-property matcher; valid 1-property matchers (each property alone); matcher that is not an object; **`bonus` absent** (mandatory-field rejection); **unknown sub-key inside `opponentMatch`** (e.g. `opponentMatch.typ`); unknown `type`; unknown `damagingMoveType`; `fasterThanRosterSlot` fractional (`1.5`), negative (`-1`), string (`"1"`), overflow; `bonus` outside `[1,16]` in both directions (`0` and `17`), fractional, string; all-conditions-absent matcher; and a valid sibling attempt surviving alongside each rejected one. The same rejection set is asserted against `validate_repo.py` fixtures per contract §H.3.

**`BlaineLeadSelectionTest.java`** — single edit: `assertEquals(4, presets.size(), …)` → `5` at `:465`; the existing `:477-489` loop then drift-guards the new preset.

### 7.4 Offline != Production Invariant (Canary Lessons 6 & 7)
Layers 0–2 confirm syntax, schema conformance, and unit arithmetic only. They do **not** establish that the in-game lead selector picks `anti_fast_electric` in a live battle, nor that the Mega Golisopod aspect behaves as intended on a live host. No headless bootstrap is required for this change (no Mixin/runtime-injection edit), so Layer 4 adds nothing here. Live verification remains Layer 5 on the dedicated production host and is explicitly **not** claimed by this workstream.

### 7.5 Artifact Freshness (Finding F)
Layer 2 evidence is fresh when the recorded test-execution commit/working-tree matches the sources of files 1–8 and 12 of §6. Gradle `--rerun-tasks` is run once per implementation checkpoint; redundant rebuilds are not performed. Layer 1 evidence is fresh when `scripts/ci/validate_repo.py` and the trainer JSON have unchanged content hashes relative to the recorded run.

---

## 8. Implementation Checkpoints (Task-Derived)

- **Checkpoint 1: Plan Freeze Checkpoint** — Main Controller identity/hash gate; two-phase Reviewer boot handshake; acceptance predicate `audit_review_gate.py` exit 0, visible provenance block, verdict `PASS`.
- **Checkpoint 2: Surgical Implementation of Slices 1–4** — model (Slice 1), **schema/parsing (Slice 2: `LeadSelectionConfig` + `LeadSelectionService` + `validate_repo.py`, all three surfaces moved together per contract §H.3)**, pure engine logic (Slice 3), adapter/boundary (Slice 4). The focus suite `cd companion-mod && ./gradlew test --tests "com.cobbleverse.legendaryrule.lead.*"` must be green, and `LeadSelectionConfigTest` must be green **before any trainer JSON declares an `opponentMatch`** — the parser must understand the object before the data slice uses it.
- **Checkpoint 3: Calibration & Data Slice (Slice 5)** — append `anti_fast_electric`, re-measure the §5.5 table against the real engine, then run Layer 1 (`validate_repo.py`, `check_legacy_baseline.py`), full Layer 2 (`./gradlew test --rerun-tasks`, `./gradlew build`), and assemble `docs/workstreams/opponent-match-conjunction-lead-preset/verification.md`.
- **Checkpoint 4: Documentation & Contract Update (Slice 6)** — the contract edits of §5.6, including the §H.2 command correction, then Layer 0 manual structural inspection (relative links, file-line bound) and independent contract review.
- **Checkpoint 5: Implementation Review Gate** — two-phase Reviewer boot handshake, independent verdict `PASS` under `audit_review_gate.py` exit 0 and visible provenance block, then the verified-implementation local checkpoint commit.

---

## 9. Residual Risks & Fallback Boundaries

| Risk | Likelihood | Impact | Mitigation / Fallback |
| :--- | :--- | :--- | :--- |
| **Precedence is calibrated, not guaranteed.** The matcher bonus outranks competitors on the enumerated boards by measured margins (3, 9, 11, 13), but a future board with unusually high structural scores for another preset could still outscore it. `bonus` is doing precedence work under a weight's name. | Medium | Medium | Tests assert the enumerated boards and exact totals. The limitation is documented in the contract, and the contract states explicitly that a `bonus` above the aggregate maximum (12) is a **precedence declaration** whose value must be calibrated against competing presets (§5.6, C.5 note). Raising `bonus` (bounded by 16) is the supported remedy; a first-class precedence tier is the upgrade path if categorical precedence is ever needed (§5.5). No universal precedence is claimed. |
| **Rule-3 boundary: a future matcher-bearing preset could become the default-threat-speed source.** `resolveDefaultAttempt` rule 3 (`LeadSelectionEngine.java:141`) is retained unchanged as a last-resort fallback and does not exclude matcher-bearing presets. Rule 3 is live today (Koga: no `default: true`, no conditionless preset), but no current trainer has a matcher-bearing preset, so no outcome shifts. | Low | Low | Documented in §5.3 and in contract §C.4. Blaine resolves via rule 1 (`"default": true`), so the new preset never reaches rule 3. If a future trainer were affected, the remedy is the rejected option (a) — extend rule 3 — which is recorded in §5.3 so the decision is revisitable rather than lost. |
| **Unknown-key behaviour is only partly tightened.** Strict rejection is enforced for sub-keys inside `opponentMatch`; an unknown **top-level** attempt key (e.g. `opponentMAtch`) is still silently ignored, and because such a preset then has `minFastOpponents == 0` with empty favored lists it also becomes rule-2-eligible for default resolution. | Low | Low | Stated as an explicit boundary in §2, §3.2, §5.2, §5.6 (Section F annotation) and here, rather than implied. The new object's surface — the only surface this workstream introduces — is closed. Tightening top-level keys is a separate change with legacy-data blast radius across all preset-carrying trainers and is out of scope. |
| **Roster-speed divergence at runtime** (adapter resolves a different speed than the JSON-derived value used by tests, e.g. because a held item or ability changes speed). | Low | Low | Only the fast-threshold boards shift. `anti_fast_electric` compares against `RosterMemberTyping.speed()` for slot 1 whatever its source, so the referenced survival rule stays self-consistent. `BlaineLeadSelectionTest.test10` already pins 210/194 and will re-measure at implementation time. |
| **Dynamic move types** (Weather Ball, Tera Blast, Hidden Power, Revelation Dance) report their template type, so a weather-resolved Electric move would not be seen as Electric. | Low | Low | Documented as a known limitation in the contract; does not affect the Electric-STAB threat class this rule targets. |
| **Parser/validator drift**: `opponentMatch` validated in two places with different error surfaces. | Medium | Medium | Both validated by the same test fixtures in this workstream, and Section H.3 of the contract mandates synchronous updates. |
| **Tie behaviour** if a future board produces an exact `totalScore` + `baseWeight` tie. | Low | Low | The preset is declared **last**, so pre-existing presets win ties; `declarationIndex` tie-break is already deterministic and is covered by an explicit engine test. |
| **Damaging-move classification** relies on `DamageCategory.getName() != "status"` rather than `getPower() > 0`; fixed-damage moves (Seismic Toss) are correctly damaging, while a template-level `status`-category move with non-zero power would be excluded. | Low | Low | Category is the canonical mechanical distinction; a dedicated adapter test pins the behaviour. |
| **`extractDamagingMoveTypes` returns unbounded distinct types** (max 4 move types). | Very Low | Very Low | Bounded by the 4-move moveset; `List.copyOf` keeps it immutable. |

### Fallback boundary
If Layer 2 focused tests fail after Slice 2+3 but before the data slice, revert the JSON append and re-run; the data slice is independently revertible because the engine treats an absent `opponentMatch` as "no matcher" (proven by G10 and by every existing preset having no such field).

---

## Author Pre-Submission Self-Check

1. **Grounded signatures:** all cited Java, JSON, Python, and Cobblemon API signatures verified against repository source or `javap` on `Cobblemon-fabric-1.7.3+1.21.1`; no signature taken from memory.
2. **Fact vs. assumption:** every substantive claim carries `path:line`; unverified runtime hypotheses are labelled Assumption; the two conflicts (count assertion, structural-score deficit) are stated with measured evidence and explicit resolutions.
3. **Surgical scope:** 16 in-scope files listed; the mutation of the immutable legacy baseline and all valuation/AI/Mixin/simulator work are explicitly out of scope.
4. **Invariants:** repository-global and domain invariants evaluated in §4, with engine purity, backward-compatible constructors, and config-not-Java calibration called out.
5. **Verification:** minimal orthogonal set = Layers 1 + 2 (plus manual Layer 0 for docs); exact commands and pass criteria given; skipped layers justified; offline ≠ production stated without overclaiming.
6. **Proportionality:** document is under 500 lines and structured to the canonical template.
7. **Reconciliation cycle 1 (post-review corrections):** both blocking findings are resolved — the focused Layer 2 command now uses the single-project form in §7.2 and §8 (Finding-01, and the same correction is specified for the authority file's §H.2), and the default-preset predicate is resolved by the documented option (b) with an explicitly specified G12 fixture (§5.3, §7.3; Finding-02). Owner design directives applied: `bonus` is **mandatory** in `[1,16]` with no default, the bound is stated as policy rather than derivation, `bonus: 14` is documented as Blaine calibration, and the rejected precedence tier is recorded (§5.1, §5.5, §5.6). Non-blocking findings addressed: Finding-03 (`−5` → `−4` on the Miraidon board, with the correction noted in place), Finding-04 (pre-existing `isDamaging` predicate cited, divergence and reuse decision recorded), Finding-05 (bound, above), Finding-06 (new Slice 2 — Schema & Parsing — owns config/service/validator; §6 files now carry slice tags; §8 Checkpoint 2 references it), Finding-07 (unknown sub-key rejection specified for the new object; the top-level gap is annotated as an explicit boundary rather than left implicit), Finding-08 (constructor counts, call-site arities and the `Turn1StatCalculator` citation corrected), Finding-09 (Layer 0 file-line bound added to §7.2; typechart parity recorded as optional corroboration). No slices were added to the blast radius: the file set in §6 is unchanged.
