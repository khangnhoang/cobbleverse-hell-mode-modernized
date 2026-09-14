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
| `default` | Boolean | Boolean primitive, defaults to `false` | Marks fallback preset. **At most one preset may declare `default: true`**. |

### 2. Scoring & Selection Pipeline

When a battle initiates, `LeadSelectionEngine.java` computes `totalScore` for every declared preset:

$$\text{totalScore} = \text{offScore} + \text{defScore} + \text{baseWeight} + \text{typeFavoredBonus} + \text{speciesFavoredBonus} + \text{fastBonus}$$

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

#### Speed Threshold Derivation
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

### 4. Default Preset Resolution
The default preset (used for dynamic threat speed derivation and fallback) is resolved sequentially:
1. First preset with `"default": true` (at most one permitted);
2. First preset declared with no conditions (`minFastOpponents == 0`, empty `favoredAgainst`, and empty `favoredAgainstSpecies`);
3. First declared preset in the `leadPresets` array.

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

In `LeadSelectionEngine.java`, all condition evaluations are computed **independently across the opposing board**:
- `favoredAgainst` iterates over player leads and checks types.
- `favoredAgainstSpecies` iterates over player leads and checks species.
- `fastBonus` iterates over player leads and counts how many are fast.

### Concrete Failure Walkthrough
Suppose an author wants an anti-fast-electric preset (attempting to counter fast Electric threats like Regieleki, Electrode, or Kilowattrel) and writes:

```json
{
  "id": "anti_fast_electric_attempt",
  "leadSlots": [2, 3],
  "favoredAgainst": ["electric"],
  "minFastOpponents": 1
}
```

Now assume the player leads with:
1. **Pokémon A (Slow Electric):** **Ampharos** — Electric typing, Base Speed 55 (Slow).
2. **Pokémon B (Fast non-Electric):** **Aerodactyl** — Rock/Flying typing, Base Speed 130 (Fast, non-Electric).

**Engine Evaluation:**
1. `favoredAgainst` (`"electric"`) is satisfied by **Pokémon A (Ampharos)** $\to$ matches `"electric"` $\to$ awards **+2**.
2. `minFastOpponents` (`1`) is satisfied by **Pokémon B (Aerodactyl)** $\to$ speed exceeds threshold $\to$ fast count = 1 $\ge 1 \to$ awards **+4**.
3. **Total conditional bonus awarded: +6**.

**The False-Positive Fallacy:**
The preset triggers with maximum conditional bonus (+6) despite the player having brought **zero fast Electric Pokémon**:
- `favoredAgainst` is satisfied exclusively by Pokémon A (Ampharos), which is Electric but slow.
- `minFastOpponents` is satisfied exclusively by Pokémon B (Aerodactyl), which is fast but non-Electric.
- **No single opposing Pokémon satisfies both conditions.**

Because `LeadSelectionEngine.java` computes these criteria independently across the entire opposing lead board, independent additive fields cannot express a single-Pokémon conjunction requirement.

**Authoring Principle:**
Never rely on combinations of independent fields to target a conjoined threat profile. If targeting a specific threat, use `favoredAgainstSpecies` or evaluate whether the preset remains sound if the traits are distributed across two different opposing leads.

---

## Section E: Extension Guidance

When existing primitives cannot express a desired tactical behavior, adhere to these principles:

1. **Smallest-Data-Path Principle:** Exhaust existing primitives (`baseWeight`, `favoredAgainst`, `favoredAgainstSpecies`, `minFastOpponents`) before proposing code modifications.
2. **Generic Composable Primitives:** Any future companion mod engine extension must be generic, parameter-driven, and composable. Never introduce trainer-specific Java logic (e.g., no `antiFastElectricLeadSelector`).
3. **Per-Opponent Conjunction Abstractions:** If conjoined trait matching is implemented in the future, it must be introduced as an explicit, distinct abstraction (such as an array of conjoined opponent criteria) evaluated per opposing Pokémon, keeping aggregate board conditions separate.
4. **Zero Domain-Engine I/O:** Domain services like `LeadSelectionEngine.java` must remain pure algorithms with zero file I/O, zero Minecraft dependencies, and zero static mutable state.

---

## Section F: Current vs. Future Syntax Discipline

To prevent invalid configurations from entering the codebase:
- **Active Syntax Only:** Author configurations strictly using fields currently recognized by `LeadSelectionConfig.java` and verified by `validate_repo.py`.
- **Proposals Must Be Labeled:** If proposing hypothetical extensions in design discussions, explicitly mark them as `[PROPOSED / UNIMPLEMENTED]`. Never document hypothetical schemas as active syntax.
- **Strict Parsing Rejection:** Any unrecognized field or improper primitive type in `leadPresets` will cause validation failure in CI or runtime fallback.

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
  ./gradlew :companion-mod:test --tests "com.cobbleverse.legendaryrule.lead.*"
  ```
  Must exit with code 0 across all unit test suites.

### 3. Schema Extension Modifications
When adding new fields to `leadPresets`:
- Synchronous updates are required across:
  1. Parser: `companion-mod/src/main/java/com/cobbleverse/legendaryrule/lead/LeadSelectionConfig.java`
  2. Domain engine: `LeadSelectionEngine.java`
  3. CI validator: `scripts/ci/validate_repo.py`
  4. Authoring contract: This document.

### 4. Offline vs. Production Reality
Automated local passes (Layers 0, 1, and 2) confirm syntactic legality, schema conformance, and unit mathematical correctness. They do not substitute for live production gameplay verification (Layer 5) on the dedicated server host.
