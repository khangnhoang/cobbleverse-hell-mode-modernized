# Lt. Surge Toxtricity Strategy Layer (Master Plan)

Introduce the minimal reusable BattleAI strategy boundary required for the **Lt. Surge — Kanto Gym 3 (`kanto_ltsurge`)** encounter. Specifically, this strategy ensures that his designated ace Pokémon, **Toxtricity Low-Key**, executes its intended battle synergy:
$$\text{Electric Terrain} + \text{Punk Rock} + \text{Overdrive (Spread)} + \text{Throat Spray} \longrightarrow +1\text{ SpA Snowball}$$
The system executes via a **Delegate-First + Conservative Rewrite** architecture, preserving Run & Bun AI's native Terastallization lifecycle and tactical switching while preventing upstream spread-damage scoring bugs from suppressing Overdrive.

---

## User Review Required

> [!IMPORTANT]
> **Delegate-First & Zero Parallel Tera Lifecycle Guarantee:**
> Decompiled bytecode audits of `RunBunAI.java` confirm that when `teraTarget = "toxtricitylowkey"` enters the field, Run & Bun AI commits `hasUsedTera = true` and generates `gimmick = "terastallization"` inside `RunBunAI.choose()` (L401–402) **before** evaluating individual moves. By invoking `delegate.choose(...)` first and preserving `baseResponse.getGimmick()`, the decorator guarantees that Terastallization is safely triggered on Overdrive with **zero state drift and zero parallel Tera lifecycle**.

> [!NOTE]
> **Decision Timing & Canonical Throat Spray Lifecycle:**
> The strategy does **not** assume Toxtricity is on the field on turn 1 of the battle (Pincurchin and Alolan Raichu lead), nor does it maintain a hidden one-shot boolean flag.
> Instead, **on each eligible decision while Throat Spray remains unconsumed, the strategy prefers Overdrive**. Under normal successful resolution, this occurs once: on the first eligible decision after Toxtricity becomes active. If an executed Overdrive fails to trigger Throat Spray (e.g. both foes use Protect or are immune), Throat Spray remains in inventory, and the policy remains eligible on the subsequent decision until successfully consumed.
> Furthermore, if Run & Bun AI evaluates a tactical switch (`SwitchActionResponse`) or `forceSwitch == true`, the Conservative Rewrite contract dictates that the decorator passes through the switch untouched. There is zero forced Overdrive when the AI decides to switch.

> [!NOTE]
> **Encounter Synergy Context vs. Policy Eligibility:**
> Electric Terrain and Punk Rock are native damage modifiers and encounter synergy context, **not** policy eligibility requirements. The strategy layer owns only the missing sequencing intent: successfully triggering Throat Spray through Overdrive. The strategy does not require Electric Terrain to be active (e.g. if terrain expires or Pincurchin faints, Overdrive remains the preferred setup move).

> [!NOTE]
> **Showdown Ownership of Throat Spray Lifecycle:**
> The strategy layer does **not** consume items or mutate stat stages directly. It merely rewrites the selected move to Overdrive. Pokémon Showdown and Cobblemon own the resolution:
> $$\text{Overdrive executes successfully} \longrightarrow \text{Throat Spray triggers} \longrightarrow \text{Item consumed} \longrightarrow +1\text{ SpA applied}$$
> If Overdrive fails, Showdown does not consume Throat Spray. Once consumed, `battlePokemon.getHeldItemManager().showdownId(battlePokemon)` returns `null`, and the strategy naturally deactivates for all subsequent turns.

> [!WARNING]
> **Out-of-Scope: Upstream 0.5625 Spread Damage Multiplier Bug:**
> Bytecode analysis of `PokeMathMax.damage` proved that Run & Bun AI applies the spread move $0.75\times$ multiplier twice ($0.75 \times 0.75 = 0.5625$). **We do NOT modify `PokeMathMax` or fix this global upstream bug in this branch**, as doing so would alter damage calculations across all Run & Bun Doubles trainers. We record this as a standalone follow-up backlog item.

---

## 1. Goal / Non-Goals

### Goal
- **On each eligible decision while Throat Spray remains unconsumed, prefer Overdrive.** Under normal successful resolution this occurs once, on the first eligible decision after Toxtricity becomes active.
- Rewrite the chosen action to **Overdrive** (with spread target `null`) and preserve the Tera gimmick (`baseResponse.getGimmick()`), provided that:
  1. `forceSwitch == false`.
  2. Delegate returned a `MoveActionResponse`.
  3. Throat Spray is currently unconsumed (`heldItem == "throatspray"`).
  4. Overdrive is usable according to canonical in-battle move state.
  5. Board viability passes (not all opposing active targets are confirmed immune).
- Allow Showdown's native Throat Spray lifecycle to trigger from a successfully executed Overdrive; do not mutate item or stat-stage state inside the strategy layer.
- Ensure 100% unconstrained fallback to Run & Bun AI once Throat Spray has been consumed (or whenever R&B executes a tactical switch).
- Ensure all policy branches are deterministically unit-testable in plain JUnit 5.

### Non-Goals (Explicit Out-of-Scope)
- **Do NOT modify `kanto_ltsurge.json` or rebalance Lt. Surge's roster, movesets, or items in this branch.**
- **Do NOT fix the global Run & Bun `0.5625` spread damage bug** in `PokeMathMax.damage`.
- **Do NOT rewrite or modify `RunBunAI.choose()`** or patch `rbrctai` bytecode directly.
- **Do NOT alter tactical switching**; if `RunBunAI` chooses `SwitchActionResponse` or `forceSwitch == true`, pass through untouched.
- **Do NOT build a universal Pokémon AI DSL or public mod API**; keep the strategy registry minimal, targeted, and scoped.
- **Do NOT modify other trainers**; unconfigured trainers must run 100% unmodified native RCT / Run & Bun AI.

---

## 2. Current Architecture & Context

### Encounter Context (`kanto_ltsurge.json`)
- **Format:** Gen 9 Doubles, Level 42.
- **AI:** Run & Bun (`rb`), `canTera = true`, `teraTarget = "toxtricitylowkey"`.
- **Toxtricity Low-Key:**
  - Ability: `Punk Rock` ($+30\%$ sound damage).
  - Item: `Throat Spray` (activates on sound move, $+1$ SpA).
  - Tera Type: `Grass` (covers Ground weakness).
  - Moves: `Overdrive`, `Sludge Bomb`, `Tera Blast`, `Protect`.
- **Synergy Lead:** Pincurchin with `Electric Surge` (Electric Terrain boosts Electric moves by $30\%$).

### Root Cause of Overdrive Suppression in Run & Bun AI
1. **Spread Damage Bug:** `targets` ($0.75$) multiplied twice in `PokeMathMax.damage` $\rightarrow 0.5625$ multiplier.
2. **Lack of Setup Awareness:** Run & Bun AI evaluates immediate turn damage per opponent slot without accounting for Throat Spray's permanent $+1$ SpA setup value.
3. **Targeted Bias:** Sludge Bomb or Protect often rolls higher single-target score than the bugged $0.5625\times$ Overdrive.

---

## 3. Frozen Contracts

```text
RCTModMakeBattleMixin
        │
        ├─ Dynamic Lead Selection (Existing: reorders team in perBattleNPC)
        │
        └─ Strategic BattleAI Wrapping (New: wraps battleAI in perBattleNPC)
                    │
                    ▼
        StrategicBattleAIDecorator
                    │
            delegate.choose(...)
                    │
             inspect response
                    │
        ┌───────────┴────────────┐
        │                        │
    pass/switch/etc          MoveActionResponse
        │                        │
    return unchanged        evaluate policy
                                 │
                        ┌────────┴────────┐
                        │                 │
                     no match          match
                        │                 │
                   base response       rewrite:
                                       - move = "overdrive"
                                       - target = null
                                       - gimmick = baseResponse.gimmick
```

### Contract 1: Delegate-First
The decorator MUST invoke `delegate.choose(activeBattlePokemon, forceSwitch)` first.
- *Rationale:* Run & Bun AI sets its internal `hasUsedTera = true` and generates `gimmick = "terastallization"` inside `choose()`. Calling delegate first captures the official Tera gimmick and preserves AI lifecycle state.

### Contract 2: Conservative Rewrite
The decorator is ONLY permitted to rewrite if ALL of the following hold:
1. `forceSwitch == false`.
2. `baseResponse instanceof MoveActionResponse`.
3. `activeBattlePokemon` matches target Pokémon configured in policy (`toxtricitylowkey`).
4. Configured strategy policy returns a non-empty `StrategicDecision`.
- If `baseResponse` is `SwitchActionResponse`, `PassActionResponse`, `ForcePassActionResponse`, or any other type $\rightarrow$ return `baseResponse` unchanged immediately.

### Contract 3: Tera Preservation
When rewriting to Overdrive:
```kotlin
MoveActionResponse(
    name = "overdrive",
    targetPnx = null,
    gimmick = baseResponse.gimmick
)
```
The decorator MUST preserve `baseResponse.getGimmick()`. It must NEVER instantiate an independent Tera lifecycle.

### Contract 4: Spread Target Semantics
Overdrive targets `MoveTarget.allAdjacentFoes`. In Cobblemon / Run & Bun convention:
- `targetPnx = null`.
Cobblemon serializes this as `"move $moveIndex"`, and Showdown applies damage to all adjacent foes without requiring a specific target slot.

### Contract 5: Authoritative Runtime Item State
In-battle Throat Spray consumption is authoritative only when confirmed by Showdown's `|-enditem|` message (captured via `BattleItemStateTracker` on `BattlePokemon`). Entity-level `Pokemon.heldItem` is persistent inventory state and must not be used alone for post-consumption eligibility:
- `HeldItemManager.showdownId()` exposes configured item (`"throatspray"`).
- `BattleItemStateTracker.cobbleverse$isThroatSprayEnded() == false` $\rightarrow$ eligible for strategy.
- When `cobbleverse$isThroatSprayEnded() == true` (or if tracker is absent) $\rightarrow$ adapter exposes `heldItemShowdownId = null`, strategy drops out, 100% pass-through to Run & Bun AI.

### Contract 6: Move Legality via Canonical In-Battle State
Use the same canonical in-battle move-usability primitive used by the current Cobblemon/R&B decision path.
- During CP3 adapter implementation, verify the exact semantics of `InBattleMove.canBeUsed()` against the installed Cobblemon version. Do not recreate move legality in domain code.

### Contract 7: Board Viability Primitive & Clean Immunity Boundary
Do not force Overdrive when all opposing active targets are confirmed immune to the preferred move:
- **Domain Policy Responsibility:**
  `ThroatSpraySoundPolicy` evaluates immunity exclusively via `StrategicOpponentContext.confirmedImmune()`.
  - `opponents.isEmpty()` $\rightarrow$ empty decision.
  - All active opponents `confirmedImmune == true` $\rightarrow$ empty decision.
  - At least one active opponent `confirmedImmune == false` $\rightarrow$ Overdrive decision.
  The policy has **zero knowledge of Pokémon types, Ground mechanics, Soundproof, or abilities**.
- **Adapter Responsibility (CP3):**
  `CobblemonTurnContextAdapter` resolves `confirmedImmune` per opponent:
  - If effective runtime abilities are readily readable from existing adapters:
    `Ground typing || Soundproof || Volt Absorb || Motor Drive || Lightning Rod` $\rightarrow$ `confirmedImmune = true`.
  - If effective runtime abilities require heavy new infrastructure:
    `Ground typing only for v1` $\rightarrow$ `confirmedImmune = true`, with ability-based immunity recorded as a follow-up.
  - Must evaluate **effective runtime state on the active board**, not static ability declared in species/trainer JSON (to avoid misinterpreting suppressed, changed, or swapped abilities).

---

## 4. Architecture & Responsibility Separation

```text
TrainerStrategyRegistry
        │ (maps trainerId "kanto_ltsurge" -> BattleStrategyPolicy)
        ▼
ThroatSpraySoundPolicy
        │ (configured with: pokemonId="toxtricitylowkey", itemId="throatspray", moveName="overdrive")
        │ (trainer-agnostic & type-chart-agnostic; evaluates ONLY pokemon, item, move usability, and confirmedImmune)
        ▼
StrategicDecision ("overdrive", targetPnx=null)
```

### Clean Boundary Rule
- **`TrainerStrategyRegistry`** owns trainer mapping: maps `"kanto_ltsurge"` $\rightarrow$ `ThroatSpraySoundPolicy`. Trainer mismatch is tested at the registry level.
- **`ThroatSpraySoundPolicy`** is constructor-configured, trainer-agnostic, and type-chart-agnostic:
  ```java
  new ThroatSpraySoundPolicy("toxtricitylowkey", "throatspray", "overdrive");
  ```
  It evaluates ONLY Pokémon identity, held item, move usability, and `confirmedImmune` flags. It has zero knowledge of Lt. Surge, Ground types, or specific abilities, enabling clean reuse for future trainers without fake generics or inverted dependencies.

---

## 5. Proposed File Layout

```text
companion-mod/src/main/java/com/cobbleverse/legendaryrule/
├── strategy/
│   ├── domain/
│   │   ├── BattleTurnContext.java               [Pure domain context snapshot]
│   │   ├── StrategicDecision.java               [Pure domain move rewrite decision]
│   │   ├── StrategicOpponentContext.java        [Pure domain opponent active & confirmedImmune snapshot]
│   │   ├── BattleStrategyPolicy.java            [Policy interface]
│   │   └── ThroatSpraySoundPolicy.java          [Generic Throat Spray + Sound move policy]
│   ├── decorator/
│   │   └── StrategicBattleAIDecorator.java      [BattleAI wrapper implementing delegate-first contract]
│   ├── adapter/
│   │   └── CobblemonTurnContextAdapter.java     [Extracts BattleTurnContext and computes confirmedImmune from ActiveBattlePokemon]
│   └── registry/
│       └── TrainerStrategyRegistry.java         [Maps trainer ID to BattleStrategyPolicy]
└── mixin/
    └── RCTModMakeBattleMixin.java               [Install decorator onto perBattleNPC with copy semantics preservation]
```

---

## 6. Implementation Checkpoints

### Checkpoint 1: Strategy Domain & Policy Semantics
- **Goal:** Implement pure domain abstractions and `ThroatSpraySoundPolicy` with zero dependencies on Minecraft/Cobblemon/RCT and zero hardcoded type/ability knowledge.
- **Files Created:**
  - `BattleTurnContext.java`, `StrategicDecision.java`, `StrategicOpponentContext.java`
    ```java
    public record StrategicOpponentContext(String showdownId, boolean active, boolean confirmedImmune) {}
    ```
  - `BattleStrategyPolicy.java`
  - `ThroatSpraySoundPolicy.java`
- **Unit Tests (`src/test/.../strategy/domain/ThroatSpraySoundPolicyTest.java`):**
  - T01: Target Pokémon (`toxtricitylowkey`) + unused Throat Spray + usable Overdrive + vulnerable opponent (`confirmedImmune = false`) $\rightarrow$ returns Overdrive decision.
  - T02: Throat Spray consumed (`heldItem = null` or other) $\rightarrow$ returns empty.
  - T03: Non-target Pokémon (e.g. Pincurchin) $\rightarrow$ returns empty.
  - T04: Overdrive unusable (`isUsable = false`) $\rightarrow$ returns empty.
  - T05: All active opponents `confirmedImmune = true` $\rightarrow$ returns empty (does not force Overdrive).
  - T06: One opponent `confirmedImmune = true`, one `confirmedImmune = false` $\rightarrow$ returns Overdrive decision.
  - T07: Empty opponents list or null context $\rightarrow$ returns empty.
- **Exit Gate:** All policy branches deterministically unit-testable in plain JUnit 5. Pure Java domain, no external dependencies, zero type-chart logic in policy.

### Checkpoint 2: BattleAI Decorator & Response Rewrite Boundary
- **Goal:** Implement `StrategicBattleAIDecorator` enforcing delegate-first, conservative response checks, and gimmick preservation.
- **Files Created:**
  - `StrategicBattleAIDecorator.java`
- **Unit Tests (`src/test/.../strategy/decorator/StrategicBattleAIDecoratorTest.java`):**
  - T08: `forceSwitch == true` $\rightarrow$ returns exact delegate response.
  - T09: Delegate returns `SwitchActionResponse` $\rightarrow$ returns exact delegate response.
  - T10: Delegate returns `PassActionResponse` $\rightarrow$ returns exact delegate response.
  - T11: Delegate returns `MoveActionResponse`, policy returns empty $\rightarrow$ returns exact delegate response.
  - T12: Policy matches $\rightarrow$ returns rewritten `MoveActionResponse("overdrive", null, gimmick)`.
  - T13: Rewritten response verifies `targetPnx == null` and `gimmick == baseResponse.getGimmick()`.
- **Exit Gate:** Decorator test suite passes with exact pass-through verification.

### Checkpoint 3: RCT Wiring, Adapter & Trainer Strategy Resolution
- **Goal:** Connect strategy registry to `RCTModMakeBattleMixin`, wrapping the AI on `perBattleNPC` for `kanto_ltsurge` while preserving dynamic lead selection and all `TrainerNPC` clone semantics.
- **Pre-Code Verification 1 (`canBeUsed` semantics):**
  - Verify exact decompiled implementation of `InBattleMove.canBeUsed()` in Cobblemon 1.7.3. Confirm whether it covers full legality (PP, disabled, lock states) or basic availability. Report any contract gap before relying on it.
- **Pre-Code Verification 2 (`TrainerNPC` copy semantics):**
  - Use the existing copy constructor `TrainerNPC(TrainerNPC other)` as the authoritative source of truth.
  - Verify that constructing `perBattleNPC` with `wrappedAI` preserves exact clone semantics:
    `copyTeam()` for team array, `new GimmicksMap()` for gimmicks, and reference handling matching existing per-battle construction.
- **Files Created/Modified:**
  - `TrainerStrategyRegistry.java`
  - `CobblemonTurnContextAdapter.java`
  - `RCTModMakeBattleMixin.java` (wrap `perBattleNPC.getBattleAI()`)
- **Unit & Integration Tests (`src/test/.../strategy/`):**
  - T14: `TrainerStrategyRegistryTest`:
    - Trainer mismatch (`"kanto_brock"`, `"kanto_sabrina"`) $\rightarrow$ returns `Optional.empty()`.
    - Trainer `"kanto_ltsurge"` $\rightarrow$ resolves configured `ThroatSpraySoundPolicy`.
  - T15: Dynamic lead selection tests (Sabrina, etc.) continue to pass unmodified.
  - T16: **Mutation Isolation & State Invariant Test:**
    - Construct `perBattleNPC` from `original` singleton.
    - Mutate `perBattleNPC` (e.g. modify team array elements, reorder team, mutate gimmicks, consume bag items if API permits).
    - Assert `original` singleton state: `original.getTeam()` completely unmutated, `original.getGimmicks()` unmutated, `original.getBattleAI()` unchanged, and reference isolation (`perBattle.getTeam() != original.getTeam()`, `perBattle.getGimmicks() != original.getGimmicks()`).
  - T17: Adapter tests in `CobblemonTurnContextAdapterTest`:
    - Verify Ground typing correctly sets `confirmedImmune = true`.
    - Verify non-immune target sets `confirmedImmune = false`.
    - Verify effective runtime `Soundproof` / `Volt Absorb` (if included in v1) sets `confirmedImmune = true`.
  - T18: Full Gradle build and test suite (`./gradlew test --rerun-tasks`) execute cleanly.
- **Exit Gate:** Existing functionality unchanged; strategy installed only in intended encounter; zero registry singleton pollution verified by mutation-isolation tests.

### Checkpoint 4: Integration & Live Runtime Verification
- **Goal:** Verify end-to-end execution in live Minecraft/Cobblemon server environment against Lt. Surge.
- **Verification Procedure:**
  1. Deploy companion-mod jar to Minecraft test instance.
  2. Initiate battle with Lt. Surge (`/rct battle kanto_ltsurge`).
  3. Play until Toxtricity Low-Key becomes active on the field.
  4. Verify the following chain of events:
     - Toxtricity enters the field.
     - Terastallization triggers (Tera Grass visual and prompt).
     - Overdrive executes hitting all adjacent foes.
     - Showdown triggers Throat Spray: item is removed/consumed and Toxtricity's Special Attack stage increases by $+1$.
     - Next eligible decision: With Throat Spray consumed, strategy drops out and Toxtricity evaluates naturally via Run & Bun AI with $+1$ SpA.
- **Exit Gate:** Live battle log confirms full combo execution with zero exceptions.

---

## 7. Risks, Edge Cases & Mitigation

1. **Board Immunity (Ground / Soundproof / Electric-Immunity Abilities):**
   - *Risk:* If all opponents are Ground-type or have `Soundproof` / `Volt Absorb` / `Motor Drive` / `Lightning Rod`, forcing Overdrive wastes the turn.
   - *Mitigation:* `CobblemonTurnContextAdapter` resolves `confirmedImmune` per opponent. If all opponents are confirmed immune, `ThroatSpraySoundPolicy` aborts, allowing Run & Bun AI to choose a coverage move (e.g. Tera Blast Grass or Sludge Bomb).
2. **Move Locking (Throat Chop, Encore, Choice Lock, Disable):**
   - *Risk:* If Toxtricity is affected by Throat Chop (preventing sound moves) or locked into another move, forcing Overdrive causes an illegal action response.
   - *Mitigation:* The adapter uses the canonical in-battle usability primitive used by the installed Cobblemon/R&B path. CP3 verifies the exact semantics of `InBattleMove.canBeUsed()` before relying on it; no parallel legality engine will be introduced.
3. **Fail-Safe Native-AI Fallback:**
   - *Risk:* An unexpected runtime exception in the decorator could stall or crash the battle.
   - *Mitigation:* The decorator wraps strategy evaluation in a fail-safe `try/catch` block. If any error occurs, it logs a warning and returns `baseResponse` immediately to native Run & Bun AI.
4. **Registry Singleton State Pollution:**
   - *Risk:* Constructing `perBattleNPC` with a custom constructor could inadvertently pass mutable references (`team`, `gimmicks`), allowing battle state or lead reordering to mutate the registry singleton.
   - *Mitigation:* CP3 pre-code check verifies clone semantics against `TrainerNPC(TrainerNPC other)`. Mutation-isolation test T16 verifies that the singleton remains completely unmutated across battles.

---

## 8. Rollback & Fallback Behavior

- If the strategy is disabled or removed from `TrainerStrategyRegistry`, `RCTModMakeBattleMixin` skips wrapping, and the trainer immediately runs 100% native Run & Bun AI.
- If dynamic lead selection fails, `DynamicLeadFallbackBoundary` returns `original` without disrupting battle instantiation.
- If `StrategicBattleAIDecorator` catches any runtime exception during `choose()`, it logs the exception and returns the untouched `baseResponse` from `delegate.choose(...)`.
