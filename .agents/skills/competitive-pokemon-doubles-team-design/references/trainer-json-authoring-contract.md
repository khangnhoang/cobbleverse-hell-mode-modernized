# Trainer JSON Authoring Contract & Dynamic Lead Selection Specification

This document provides the authoritative engineering specification for authoring, reviewing, and validating NPC trainer JSON definitions in `datapacks/hell-mode/data/rctmod/trainers/`.

---

## Section A: Authority & Scope

### 1. Governed Scope
This contract governs all JSON files residing in:
`datapacks/hell-mode/data/rctmod/trainers/`

Reading this specification is **mandatory** before planning, modifying, reviewing, or validating any trainer definition file, including routine Mode 2 configuration edits.

### 2. Architectural Boundary: Roster Design vs. JSON Authoring
Trainer development comprises two decoupled engineering disciplines:
- **Competitive Roster Design:** Governed by [`.agents/skills/competitive-pokemon-doubles-team-design/SKILL.md`](../SKILL.md) and related strategy references. Defines team archetype, speed control, EV/IV distribution, held items, moveset synergy, and the Turn-1 Gimmick Axiom.
- **JSON Configuration Authoring:** Governed strictly by this contract. Defines syntactic legality, field typing, slot bounds, `leadPresets` scoring heuristics, tie-breaking precedence, dynamic speed threshold derivation, and drift guard verification.

Both disciplines must be satisfied. A competitively sound roster embedded in an invalid JSON structure is broken; a syntactically valid JSON containing anti-synergies violates Hell Mode invariants.

---

## Section B: Trainer JSON Mental Model

Trainer JSON definitions are loaded by Cobblemon and the companion mod runtime (`DynamicLeadResourceListener.java`). A complete trainer definition consists of root metadata, team definitions, and optional dynamic lead presets.

### 1. Root Level Schema

| Field | Type | Mandatory | Description & Constraints |
| :--- | :--- | :--- | :--- |
| `name` | Object | Yes | Display name wrapper: `{"literal": "<Name>"}`. |
| `identity` | String | No | Internal identifier string (e.g., `"Leader Brock"`). |
| `battleFormat` | String | Yes | Must be declared as `"GEN_9_DOUBLES"`. |
| `battleRules` | Object | Yes | Match rules. E.g., `{"maxItemUses": 0}` (or `2` for major bosses). |
| `ai` | Object | Yes | AI engine selector: `{"type": "rct", "data": {...}}` or `{"type": "rb"}`. |
| `bag` | Array | Yes | Trainer inventory. Only battle items permitted (`cobblemon:full_restore`). **Zero revives**. |
| `team` | Array | Yes | List of exactly 6 Pokémon objects for standard doubles. |
| `leadPresets` | Array | No | List of dynamic lead selection preset objects evaluated by `LeadSelectionEngine`. |

### 2. Team Member Schema (`team[]`)

Each Pokémon object in `team` must declare:
- `species`: Lowercase Cobblemon 1.7.3 species identifier (e.g., `"raichu"`, `"rotom"`, `"electrode"`).
- `gender`: String primitive: `"MALE"`, `"FEMALE"`, or `"GENDERLESS"`.
- `level`: Integer representing progression level.
- `nature`: Lowercase nature string (e.g., `"adamant"`, `"timid"`, `"modest"`).
- `ability`: Lowercase ability identifier (e.g., `"surgesurfer"`, `"levitate"`).
- `moveset`: Array of exactly 4 lowercase Showdown move strings without hyphens or spaces (e.g., `["thunderbolt", "voltswitch", "fakeout", "surf"]`).
- `ivs`: Object declaring exact integer values (0–31) for `hp`, `atk`, `def`, `spa`, `spd`, `spe`.
- `evs`: Object declaring exact integer values (0–252) for `hp`, `atk`, `def`, `spa`, `spd`, `spe` (sum $\le$ 510).
- `heldItem`: Array of strings containing legal item namespaces (e.g., `["life_orb"]`, `["smooth_rock"]`, `["mega_showdown:aerodactylite"]`).
- `aspects`: Optional array of aspect strings for regional or appliance forms (e.g., `["alolan"]`, `["hisuian"]`, `["wash-appliance"]`).
- `gimmicks`: Object declaring runtime battle mechanics (e.g., `{"dynamax": true}`). **Obsolete `"mega": true` is strictly prohibited**.

---

## Section C: Current Dynamic Lead-Selection Vocabulary

Dynamic lead presets allow an NPC boss to inspect the player's predicted lead pair and select an optimal counter-lead from their 6-mon roster. The configuration is parsed by `LeadSelectionConfig.java` and evaluated by `LeadSelectionEngine.java`.

### 1. Preset Field Vocabulary (`leadPresets[]`)

| Field | Type | Bounds / Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | String | Non-blank, unique per trainer | Preset identifier (e.g., `"terrain_surfer"`, `"anti_ground"`). |
| `leadSlots` | Array[2] | Exactly 2 distinct integers in `[0, len(team)-1]` | Team slot indices to deploy as leads if preset wins. |
| `baseWeight` | Integer | Exact integer in `[-2, 2]`, defaults to `0` | Baseline heuristic bias added to `totalScore` and used in tie-breaking. |
| `expectedLeadMembers` | Array[2] | Exactly 2 objects matching `leadSlots` | Semantic drift guard. Validates slot identity against roster. |
| `description` | String | Optional string | Documentation explaining tactical purpose. |
| `favoredAgainst` | Array | Lowercase valid Pokémon types | Type list granting +2 bonus per matching player lead. |
| `favoredAgainstSpecies` | Array | Lowercase valid Pokémon species | Species list granting +2 bonus per matching player lead. |
| `minFastOpponents` | Integer | Exact integer $\ge 0$, defaults to `0` | Minimum opposing leads meeting speed threshold to trigger fast bonus. |
| `fastSpeedThreshold` | Integer | Exact integer $\ge 0$, defaults to `0` | Static speed cutoff. If $\le 0$, enables dynamic speed derivation. |
| `opponentMatch` | Object | Optional | **Per-opponent conjunction.** Sub-fields below. Rejected if not a JSON object, if every condition sub-field is absent, if `bonus` is absent, if a value is malformed, or if an unknown sub-key is present. |
| `opponentMatch.type` | String | Optional, canonical Gen 9 type | Matches an opposing lead whose own typing includes this type. |
| `opponentMatch.damagingMoveType` | String | Optional, canonical Gen 9 type | Matches an opposing lead carrying an equipped **damaging** (non-`status`) move of this type. |
| `opponentMatch.fasterThanRosterSlot` | Integer | Optional, exact integer $\ge 0$ and $< \text{len(team)}$ | Matches an opposing lead strictly faster than the **resolved** speed of this NPC roster slot. |
| `opponentMatch.bonus` | Integer | **Mandatory when `opponentMatch` is present**; exact integer in `[1, 16]` | Additive score awarded when the conjunction holds. **No default.** |
| `default` | Boolean | Boolean primitive, defaults to `false` | Marks fallback preset. **At most one preset may declare `default: true`**. |

**`bonus` is mandatory and the bound is a policy guard, not a derivation.** `opponentMatch` is a weight, not a condition, so it is exempt from the "every property is optional" rule that governs `type`, `damagingMoveType` and `fasterThanRosterSlot`: all three condition properties remain independently omittable and any subset (including one) is expressible. An omitted `bonus` cannot be defaulted — the parser cannot distinguish "the author meant a small number" from "the author forgot" — so making it mandatory forces the calibration decision into the diff. The `[1, 16]` bound exists to keep a typo (`bonus: 1400`) within the order of magnitude of the other score components; it is **not** derived from the aggregate maximum of 12.

### 2. Scoring & Selection Pipeline

When a battle initiates, `LeadSelectionEngine.java` computes `totalScore` for every declared preset:

$$\text{totalScore} = \text{offScore} + \text{defScore} + \text{baseWeight} + \text{typeFavoredBonus} + \text{speciesFavoredBonus} + \text{fastBonus} + \text{opponentMatchBonus}$$

#### Discrete Matchup Scoring (`offScore`, `defScore`)
- Evaluated across both NPC leads ($A$ and $B$) against both opposing player leads:
  - $\text{offScore} = \sum \text{mapOffensive}(\text{best STAB vs defender types})$
  - $\text{defScore} = \sum \text{mapDefensive}(\text{worst incoming STAB vs NPC defender types})$
- Multiplier mapping:
  - Offensive: $4.0\times \to +4$, $2.0\times \to +2$, $1.0\times \to 0$, $0.5\times \to -1$, $0.25\times \to -2$, $0.0\times \to -4$.
  - Defensive: $4.0\times \to -4$, $2.0\times \to -2$, $1.0\times \to 0$, $0.5\times \to +1$, $0.25\times \to +2$, $0.0\times \to +4$.

#### Conditional Bonuses
- **`typeFavoredBonus` (+2 per matching player lead, max +4):** For each player lead, if at least one of its types is present in `favoredAgainst`, +2 is added. A dual-type matching multiple favored types receives +2 only once.
- **`speciesFavoredBonus` (+2 per matching player lead, max +4):** For each player lead, if its species matches an entry in `favoredAgainstSpecies`, +2 is added.
- **`fastBonus` (+4 flat):** If `minFastOpponents > 0` and the count of opposing leads meeting the speed threshold is $\ge \text{minFastOpponents}$, a flat +4 is awarded.
- **`opponentMatchBonus` (`opponentMatch.bonus`, authored per preset):** Unlike every other term in this pipeline, the magnitude here is authored in the trainer JSON rather than frozen in the engine. The matcher is evaluated per opposing lead (see the conjunction rules below) and its `bonus` is added at most once.

#### Per-Opponent Conjunction (`opponentMatch`)
- **Same-opponent AND.** Every present sub-field must be satisfied by **one single** opposing lead. Satisfaction is never assembled across two different opposing leads.
- **Omitted sub-field = unconstrained.** A sub-field that is not declared is skipped, never treated as false.
- **Exists-quantifier.** One qualifying opposing lead is sufficient; a second qualifying lead does not duplicate the bonus.
- **Strict inequality.** `fasterThanRosterSlot` compares $\text{playerSpeed} > \text{slotSpeed}$, matching the dynamic-threshold convention below.
- **Speed authority.** Both sides of the comparison are values produced by `CobblemonLeadAdapter.resolveSpeed()`; the engine never re-applies the Choice Scarf multiplier.
- **Degeneracy guard.** If the referenced roster slot is absent, or its resolved speed is $\le 0$, the property is **unsatisfiable** — it does not become vacuously true.

#### Speed Threshold Derivation
- **Runtime Speed Authority (`CobblemonLeadAdapter.resolveSpeed()`):**
  - Evaluates runtime speed from `pokemon.getSpeed()` (accounting for level, IVs, EVs, nature).
  - Explicitly applies a Choice Scarf multiplier: $\lfloor\text{speed} \times 1.5\rfloor$.
  - Species base Speed is strictly an emergency fallback when runtime speed is $\le 0$, never the primary matcher authority.
- **Dynamic Mode (`fastSpeedThreshold <= 0` and `dynamicThreatSpeed > 0`):**
  - $\text{dynamicThreatSpeed} = \max(\text{speed}_A, \text{speed}_B)$ of the resolved default lead preset.
  - Evaluates via strict inequality: $\text{playerSpeed} > \text{dynamicThreatSpeed}$.
- **Static Mode (`fastSpeedThreshold > 0`):**
  - Evaluates via inclusive inequality: $\text{playerSpeed} \ge \text{fastSpeedThreshold}$ (falls back to 100 if dynamic speed is 0).

### 3. Tie-Breaking Precedence
When multiple presets are evaluated, the winning preset is selected strictly by:
1. `totalScore` descending (highest score wins);
2. `baseWeight` descending (higher base weight wins);
3. `declarationIndex` ascending (order declared in JSON file wins).

*This section is deliberately unchanged by the `opponentMatch` extension.* No first-class precedence or
priority tier was introduced; a matcher influences the outcome through its additive `bonus` only, so the
ordering contract above governs all presets exactly as before.

### 4. Default Preset Resolution
The default preset (used for dynamic threat speed derivation and fallback) is resolved sequentially:
1. First preset with `"default": true` (at most one permitted);
2. First preset declared with no conditions (`minFastOpponents == 0`, empty `favoredAgainst`, empty `favoredAgainstSpecies`, **and no `opponentMatch`**);
3. First declared preset in the `leadPresets` array.

Rule 2 excludes matcher-bearing presets so a matcher-only preset is never mis-resolved as
"unconditional". Rule 3 is retained unchanged as the documented last-resort fallback. Rule 3 is live
today: `kanto_koga.json` declares three presets, all with a non-empty `favoredAgainstSpecies` and none
with `default: true`, so `default_sun` (index 0) supplies `dynamicThreatSpeed` through it. The residual
boundary is narrow but real: a **future** trainer whose only rule-3 candidate carried an `opponentMatch`
would derive `dynamicThreatSpeed` from that preset — pre-existing rule-3 behaviour, not behaviour
introduced by the matcher.

> [!NOTE]
> **A `bonus` above the aggregate maximum (12) is a precedence declaration.** The board-aggregate
> conditions can award at most 12 (`typeFavored` 4 + `speciesFavored` 4 + `fastBonus` 4), and
> `baseWeight` is capped at $\pm 2$. A matcher whose `bonus` exceeds 12 therefore outranks the aggregate
> conditions by construction, which means it is doing **precedence** work while being named a bonus. Such
> a value must be calibrated against the structural scores of the presets it has to beat on that trainer's
> roster — not read as an ordinary weight, and not copied between trainers. The honest upgrade path, if
> categorical precedence is ever needed rather than calibrated precedence, is a first-class priority tier.

### 5. Semantic Drift Guard (`expectedLeadMembers`)
`expectedLeadMembers` is a regression-prevention mechanism. Each entry validates:
- `species`: Must match the Pokémon species at `leadSlots[i]`.
- `form`: Must match the form at `leadSlots[i]` if specified.
- `requiredAspects`: Must be a subset of aspects and gender on the Pokémon at `leadSlots[i]`.

*Note:* `expectedLeadMembers` does **not** contribute to `totalScore`. It is validated at mod startup and during CI validation (`validate_repo.py`) to prevent slot index drift when teams are reordered.

---

## Section D: Critical Authoring Rule: Conjunction vs. Independent Conditions

> [!WARNING]
> **Independent conditions and bonuses do NOT imply that the same opposing Pokémon satisfies all of them.**
> This is exactly the behaviour that `opponentMatch` (§C.1) exists to fix. The aggregate predicates below
> remain the right tool for *board-wide* pressure; use `opponentMatch` when the requirement is a
> conjunction on a **single** opposing Pokémon.

In `LeadSelectionEngine.java`, aggregate condition evaluations are computed **independently across the opposing board**:
- `favoredAgainst` iterates over player leads and checks types.
- `favoredAgainstSpecies` iterates over player leads and checks species.
- `fastBonus` iterates over player leads and counts how many are fast.

### Concrete Failure Walkthrough (the behaviour `opponentMatch` fixes)
Suppose an author wants an anti-fast-electric preset (attempting to counter fast Electric threats like Regieleki, Electrode, or Kilowattrel) and writes:

```json
{
  "id": "anti_fast_electric_attempt",
  "leadSlots": [2, 3],
  "favoredAgainst": ["electric"],
  "minFastOpponents": 1
}
```

Now assume the resolved speed threshold is $T = 210$ (derived dynamically from the default lead pair or configured statically), and the player leads with:

1. **Pokémon A (e.g., Ampharos):**
   - Typing: Electric
   - Actual resolved Speed: $180$ (derived via `CobblemonLeadAdapter.resolveSpeed()`, querying runtime `pokemon.getSpeed()` plus any explicit adapter adjustments like Choice Scarf $\times 1.5$)
   - Speed evaluation: $180 \le 210 \to$ **not fast**

2. **Pokémon B (e.g., Aerodactyl):**
   - Typing: Rock/Flying (non-Electric)
   - Actual resolved Speed: $230$
   - Speed evaluation: $230 > 210 \to$ **fast**

*(Note: Species names are illustrative. The selector uses the Pokémon's runtime Speed from `pokemon.getSpeed()`. `CobblemonLeadAdapter` then applies its explicit Choice Scarf $\times 1.5$ adjustment. Species base Speed is not used as the matcher authority).*

**Engine Evaluation:**
1. `favoredAgainst` (`"electric"`) is satisfied exclusively by **Pokémon A** $\to$ matches `"electric"` $\to$ awards **+2**.
2. `minFastOpponents` (`1`) is satisfied exclusively by **Pokémon B** $\to$ actual resolved Speed ($230 > 210$) $\to$ fast count = 1 $\ge 1 \to$ awards **+4**.
3. **Total conditional bonus awarded: +6**.

**The False-Positive Fallacy:**
The preset triggers with maximum conditional bonus (+6) despite the player having brought **zero fast Electric Pokémon**:
- `favoredAgainst` is satisfied exclusively by Pokémon A (Electric, but actual Speed $180 \le 210$).
- `minFastOpponents` is satisfied exclusively by Pokémon B (actual Speed $230 > 210$, but non-Electric).
- **No single opposing Pokémon satisfies both conditions.**

Because `LeadSelectionEngine.java` computes these criteria independently across the entire opposing lead board, independent additive fields cannot express a single-Pokémon conjunction requirement.

**Supported remedy — the `opponentMatch` primitive.** The same intent expressed with the conjunction matcher:

```json
{
  "id": "anti_fast_electric",
  "leadSlots": [3, 4],
  "baseWeight": 0,
  "opponentMatch": {
    "type": "electric",
    "damagingMoveType": "electric",
    "fasterThanRosterSlot": 1,
    "bonus": 14
  }
}
```

This fires only when **one** opposing lead is simultaneously Electric, carries a damaging Electric move,
and is strictly faster than NPC roster slot 1 — so the Ampharos + Aerodactyl board above awards **nothing**.

**Authoring Principle:**
Never rely on combinations of independent aggregate fields to target a conjoined threat profile. Reach
for `opponentMatch` when the traits must co-occur on one Pokémon; keep `favoredAgainst`,
`favoredAgainstSpecies` and `minFastOpponents` for board-wide pressure; and use `favoredAgainstSpecies`
when a specific species (not a trait conjunction) is the target.

---

## Section E: Extension Guidance

When existing primitives cannot express a desired tactical behavior, adhere to these principles:

1. **Smallest-Data-Path Principle:** Exhaust existing primitives (`baseWeight`, `favoredAgainst`, `favoredAgainstSpecies`, `minFastOpponents`, `opponentMatch`) before proposing code modifications.
2. **Generic Composable Primitives:** Any future companion mod engine extension must be generic, parameter-driven, and composable. Never introduce trainer-specific Java logic (e.g., no `antiFastElectricLeadSelector`).
3. **Per-Opponent Conjunction Abstractions:** Conjoined trait matching must be an explicit, distinct abstraction evaluated per opposing Pokémon, keeping aggregate board conditions separate. This now exists as `opponentMatch` (§C.1); extend it with further *properties* rather than by overloading aggregate predicates.
4. **Zero Domain-Engine I/O:** Domain services like `LeadSelectionEngine.java` must remain pure algorithms with zero file I/O, zero Minecraft dependencies, and zero static mutable state.
5. **Authored Weights, Not Trainer Branching:** A weight whose correct value depends on one trainer's roster and on the presets competing with it is **calibration data** and belongs in that trainer's JSON file. E.1/E.2 forbid freezing such a value in the engine, because a constant chosen so that one trainer's preset wins is trainer-specific logic inside generic engine code. `opponentMatch.bonus` is authored for this reason; a weight that reads the same for every trainer may still be frozen in the engine.

---

## Section F: Current vs. Future Syntax Discipline

To prevent invalid configurations from entering the codebase:
- **Active Syntax Only:** Author configurations strictly using fields currently recognized by `LeadSelectionConfig.java` and verified by `validate_repo.py`. The active vocabulary is: `id`, `leadSlots`, `expectedLeadMembers`, `description`, `baseWeight`, `favoredAgainst`, `favoredAgainstSpecies`, `minFastOpponents`, `fastSpeedThreshold`, `default`, and `opponentMatch` (with its four sub-fields).
- **`opponentMatch` rejection rules (active):** a non-object value, an absent `bonus`, a `bonus` outside `[1, 16]`, any non-exact-integer or negative `fasterThanRosterSlot`, a `type`/`damagingMoveType` that is not a canonical Gen 9 type, a matcher declaring no condition sub-field, and any **unknown sub-key inside the `opponentMatch` object** all cause rejection. The parser rejects per attempt in isolation — a malformed preset is skipped and its valid siblings survive.
- **Known boundary — top-level keys remain permissive:** strict unknown-key rejection is enforced **only** for sub-keys inside `opponentMatch`. Neither `LeadSelectionConfig` nor `validate_repo.py` rejects an unknown **top-level** attempt key, so a typo such as `opponentMAtch` is silently ignored today and the resulting preset (no `minFastOpponents`, empty favored lists, no matcher) also becomes eligible for default-resolution rule 2. Closing that gap is a separate change with legacy-data blast radius across every preset-carrying trainer.
- **Proposals Must Be Labeled:** If proposing hypothetical extensions in design discussions, explicitly mark them as `[PROPOSED / UNIMPLEMENTED]`. Never document hypothetical schemas as active syntax.
- **Strict Parsing Rejection:** Any unrecognized sub-field inside `opponentMatch`, or improper primitive type in `leadPresets`, causes validation failure in CI or the attempt to be skipped at runtime.

---

## Section G: Cookbook

### Pattern 1: Type-Favored Preset (Countering Ground/Rock Leads)
Deploys Levitate Rotom-Wash and Surge Surfer Raichu-Alola when facing opposing Ground types:

```json
{
  "id": "anti_ground",
  "leadSlots": [2, 1],
  "expectedLeadMembers": [
    {
      "species": "rotom",
      "form": "wash",
      "requiredAspects": ["wash-appliance"]
    },
    {
      "species": "raichu",
      "form": "alola",
      "requiredAspects": ["alolan"]
    }
  ],
  "baseWeight": 0,
  "favoredAgainst": ["ground", "rock"],
  "description": "Levitate Rotom-Wash Hydro Pump counters Ground leads alongside Raichu-Alola"
}
```

### Pattern 2: Species-Favored Preset (Targeting Specific Threats)
Deploys Grass STAB and Water resistance when countering known hard counters (e.g., Swampert or Gastrodon):

```json
{
  "id": "anti_water_ground",
  "leadSlots": [2, 5],
  "expectedLeadMembers": [
    {
      "species": "rotom",
      "form": "wash",
      "requiredAspects": ["wash-appliance"]
    },
    {
      "species": "electrode",
      "requiredAspects": ["hisuian"]
    }
  ],
  "baseWeight": 0,
  "favoredAgainstSpecies": ["swampert", "gastrodon"],
  "description": "Rotom-Wash + Hisuian Electrode Grass STAB vs Swampert and Gastrodon"
}
```

### Pattern 3: Speed Control / Anti-Fast Preset
Triggers Trick Room or Tailwind counter-leads when opposing leads exceed default speed:

```json
{
  "id": "anti_fast_offense",
  "leadSlots": [0, 3],
  "expectedLeadMembers": [
    {
      "species": "pincurchin"
    },
    {
      "species": "porygon2"
    }
  ],
  "baseWeight": 0,
  "minFastOpponents": 2,
  "fastSpeedThreshold": 0,
  "description": "Deploys Porygon2 Trick Room lead when opponent deploys 2 fast sweepers"
}
```

### Pattern 4: Explicit Default Preset
Establishes the primary team synergy, acts as fallback, and sets dynamic threat speed:

```json
{
  "id": "terrain_surfer",
  "leadSlots": [0, 1],
  "expectedLeadMembers": [
    {
      "species": "pincurchin"
    },
    {
      "species": "raichu",
      "form": "alola",
      "requiredAspects": ["alolan"]
    }
  ],
  "baseWeight": 1,
  "default": true,
  "description": "Electric Surge + Surge Surfer doubles Raichu-Alola speed with fast Fake Out support"
}
```

### Pattern 5: Per-Opponent Conjunction (`opponentMatch`)
Deploys a counter-lead only when **one** opposing Pokémon combines every listed trait (e.g. Blaine's
`anti_fast_electric`, which must not fire when the Electric lead is slow and the fast lead is not Electric):

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
  "description": "Rillaboom + Mega Golisopod vs a fast Electric STAB lead that outruns Mega Charizard Y"
}
```

`fasterThanRosterSlot` refers to the **resolved** speed of NPC team slot `1` (Mega Charizard Y here), the
lead this preset exists to protect. `bonus: 14` exceeds the aggregate maximum of 12 and is therefore a
**precedence declaration** calibrated against Blaine's competing presets — a value a different trainer must
re-derive from its own roster, never copy.

### Anti-Pattern: Conjunction Fallacy via Independent Fields
**INCORRECT:**
```json
{
  "id": "flawed_conjunction",
  "leadSlots": [2, 4],
  "favoredAgainst": ["fire"],
  "minFastOpponents": 1,
  "description": "Flawed attempt to counter fast Fire types; misfires on slow Fire + fast Grass"
}
```
*Why it fails:* Matches slow Fire Pokémon (Torkoal) + fast non-Fire Pokémon (Whimsicott) independently, triggering the full bonus.

*Status:* This intent is now **expressible** with `opponentMatch` (Pattern 5) — the pattern above is correct
for the *board-aggregate* case and remains an anti-pattern only when a single-Pokémon conjunction was meant.
Note that a conjunction cannot be assembled from aggregate fields even in combination with `opponentMatch`:
the matcher is a single additional term, not a per-lead override of the aggregate predicates.

### Fallback Workflow When Schema Cannot Express Requirement
If a desired matchup condition cannot be cleanly expressed using independent type, species, or speed filters:
1. Evaluate whether `baseWeight` tuning provides appropriate preference ordering.
2. Rely on discrete offensive/defensive type chart scoring (`offScore`/`defScore`), which naturally evaluates dual-type matchups accurately.
3. If an explicit conjunction is mandatory, document the limitation in the workstream plan and initiate a managed schema extension request.

---

## Section H: Verification Expectations

Verification authority is strictly partitioned across repository layers:

### 1. Pure Trainer JSON Modifications
When modifying or creating trainer JSON files without Java changes:
- **Layer 1 Verification (Mandatory):**
  ```powershell
  python scripts/ci/validate_repo.py
  ```
  Must exit with code 0 (`RESULT: ALL REPOSITORY & DATA VALIDATIONS PASSED`). Validates JSON syntax, species/item registries, slot indices, drift guards, and single default declarations.

### 2. Lead Selection Engine Modifications
When modifying engine logic or scoring algorithms:
- **Layer 2 Verification (Mandatory):**
  ```powershell
  cd companion-mod
  ./gradlew test --tests "com.cobbleverse.legendaryrule.lead.*"
  ```
  Must exit with code 0 across all unit test suites. Run the wrapper **from inside `companion-mod/`**:
  `companion-mod/settings.gradle` declares `rootProject.name = 'rct-legendary-rule-companion'` and includes
  no subprojects, so the `:companion-mod:` project selector has nothing to resolve.

### 3. Schema Extension Modifications
When adding new fields to `leadPresets`:
- Synchronous updates are required across:
  1. Parser: `companion-mod/src/main/java/com/cobbleverse/legendaryrule/lead/LeadSelectionConfig.java`
  2. Domain engine: `LeadSelectionEngine.java`
  3. CI validator: `scripts/ci/validate_repo.py`
  4. Authoring contract: This document.

The `opponentMatch` extension is the worked example of this rule: the parser sub-key surface
(`type`, `damagingMoveType`, `fasterThanRosterSlot`, `bonus`), the engine's conjunction term and
rule-2 default predicate, the `validate_repo.py` block (which additionally enforces
`fasterThanRosterSlot < len(team)`), and §C.1/§C.2/§C.4/§D/§E/§F/§G of this contract all moved in the
same change. Because the parser and the validator are **separate implementations** of one schema, the same
fixtures are asserted against both.

### 4. Offline vs. Production Reality
Automated local passes (Layers 0, 1, and 2) confirm syntactic legality, schema conformance, and unit mathematical correctness. They do not substitute for live production gameplay verification (Layer 5) on the dedicated server host.
