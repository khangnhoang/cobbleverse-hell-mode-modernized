# Fair Opponent Information Boundary Architecture

This document specifies the current production architecture of the fair-information anti-cheat boundary around Run & Bun AI (`rbrctai`), delivered in PR #18.

---

## 1. Problem Statement & Design Objectives

### The "Omniscient AI" Defect
Native Cobblemon and Run & Bun AI query real battle objects directly during decision-making. As a consequence, the AI has omniscient access to:
- Opponent's unrevealed movesets.
- Opponent's exact held item before activation or reveal.
- Opponent's true ability before trigger.
- Opponent's configured Terastallization target and type.

This omniscience prevents competitive bluffing, distorts damage scoring, and creates unfair difficulty.

### Core Design Constraints
1. **Preserve Real Battle State:** Real `ActiveBattlePokemon`, native Cobblemon PNX indices, side references, and server-side health/items must never be modified.
2. **Preserve Real `ActiveBattlePokemon` Identity:** Replacing or detaching `ActiveBattlePokemon` breaks internal targeting and battle actor mappings.
3. **Strictly Opponent-Scoped:** The NPC trainer's own Pokémon must never be shadowed; the AI must know its own moves, item, and ability completely.
4. **Leak-Proof Lifecycle:** The boundary must activate only while `RunBunAI.choose()` is executing and clear unconditionally in a `finally` block.

---

## 2. Architecture Overview

```text
┌─────────────────────────────────────────────────────────────┐
│                    RunBunAI.choose()                        │
│                                                             │
│  try {                                                      │
│    FairBattleContext.push(currentActor, opponentSide)       │
│                                                             │
│    ActiveBattlePokemon.getBattlePokemon()                   │
│           │                                                 │
│           ▼                                                 │
│    ActiveBattlePokemonMixin (Getter Firewall)               │
│      ├── Is Own Side?    ──► Return Real BattlePokemon      │
│      └── Is Opponent Side?                                  │
│                 │                                           │
│                 ▼                                           │
│          FairShadowPokemonBuilder                           │
│          (Sanitized Shadow BattlePokemon)                   │
│                                                             │
│    Native AI evaluates moves using shadow state             │
│    Line 1779 recharge guard handles empty moveset           │
│                                                             │
│  } finally {                                                │
│    FairBattleContext.pop()                                  │
│  }                                                          │
└─────────────────────────────────────────────────────────────┘
                               │
                               ▼
        Real battle execution continues with real objects
```

---

## 3. Key Architectural Components

### A. Scoped Lifecycle (`FairBattleContext`)
Implemented as a `ThreadLocal` context stack:
- **`push(BattleActor actor, BattleSide opponentSide)`**: Pushes the current evaluation scope and increments re-entrancy depth.
- **`pop()`**: Decrements depth and clears the ThreadLocal once depth reaches 0.
- Managed by `RunBunAIFairLifecycleMixin` around `RunBunAI.choose()` with strict `try { ... } finally { FairBattleContext.pop(); }`.
- Zero leakage: outside of `choose()`, `FairBattleContext.isActive()` returns `false`, ensuring ordinary battle ticks, damage applications, and packet syncs access the real `BattlePokemon`.

### B. Getter Firewall (`ActiveBattlePokemonMixin`)
Rather than attempting to wrap or replace `ActiveBattlePokemon` (which breaks PNX and side indexing), the boundary intercepts `ActiveBattlePokemon.getBattlePokemon()`:
```java
@Inject(method = "getBattlePokemon", at = @At("HEAD"), cancellable = true)
private void cobbleverse$getFairBattlePokemon(CallbackInfoReturnable<BattlePokemon> cir) {
    if (FairBattleContext.isActive() && FairBattleContext.isOpponentSide(this.getSide())) {
        cir.setReturnValue(FairBattleContext.getOrCreateShadow((ActiveBattlePokemon) (Object) this));
    }
}
```
- **Real Identity Preserved:** The calling code continues to hold the real `ActiveBattlePokemon`.
- **Selective Substitution:** If `this.getSide()` matches the opponent side, the firewall returns the cached sanitized shadow `BattlePokemon` for this scope. If it is the NPC's own side, the real `BattlePokemon` is returned.

### C. Sanitization Engine (`FairShadowPokemonBuilder`)
Generates a cloned `BattlePokemon` instance sanitizing private information while maintaining visible battlefield metrics:

| Field | Treatment | Rationale |
| :--- | :--- | :--- |
| **Species / Form** | Preserved | Visibly apparent on the battlefield. |
| **Level / Gender / Shiny** | Preserved | Visibly apparent. |
| **Current / Max HP** | Preserved | HP gauge is visible to the player and opponent. |
| **Visible Status** | Preserved | Burns, paralysis, sleep, poison, freeze are visible. |
| **Stat Stage Changes** | Preserved | Stat stage boosts/drops are tracked publicly in battle. |
| **Aspects** | Preserved | Visual cosmetic aspects. |
| **Held Item** | **Sanitized (Empty)** | The AI must not know the opponent's item before activation. |
| **Moveset** | **Sanitized (Empty)** | Hidden moves are cleared; zero revealed moves is UNKNOWN. |
| **Ability** | **Sanitized (Dummy)** | Replaced with neutral dummy ability (`runaway`) without battle effects. |
| **Tera Type / Target** | **Sanitized (Fallback)** | Replaced with non-leaking default placeholder type. |

### D. Native Line 1779 Recharge Guard (`RunBunAIChooseMixin`)
Native `RunBunAI.choose()` line 1779 contains an unconditional check:
```java
if (oppMoves.getFirst().getName().equals("recharge"))
```
Because native R&B AI assumes every Pokémon always has at least one move in battle, clearing unrevealed moves causes `oppMoves` to be empty, throwing `java.util.NoSuchElementException: ArrayList.getFirst()`.

**The Solution:**
A `@WrapOperation` in `RunBunAIChooseMixin` tightly sliced between:
- `from = @At(value = "CONSTANT", args = "stringValue=truant", ordinal = 1)` (pc 12436)
- `to = @At(value = "FIELD", target = "Lcom/gitlab/surilexa/rbrctai/api/ai/RunBunAI;generalSetupMoves:Ljava/util/List;")` (pc 12485)

When `oppMoves` is empty, the wrap returns a safe non-playable sentinel dummy move (`name = "__SENTINEL_EMPTY__"`), safely evaluating `isRecharging` as `false` without fabricating real moves or crashing.

---

## 4. Current Limitations & Scope Boundary

1. **Sanitized Empty Moveset (Zero Known Moves = UNKNOWN)**:
   - Under the current shadow, the opponent's moveset is completely cleared and empty (`moveset: empty`).
   - Zero known moves means UNKNOWN to the AI.
2. **No Reveal Persistence / Turn Memory**:
   - There is currently NO `BattleMemory` or turn-by-turn reveal persistence in the codebase.
   - Moves used by the player on prior turns are NOT yet remembered and are NOT re-materialized on subsequent turns.
   - Each `RunBunAI.choose()` evaluation constructs shadow state statelessly from scratch.
3. **No Species or Level Default Priors**:
   - No species/level default move priors or heuristic guesses are currently injected into the shadow.
4. **Planned Future Workstream (`BattleMemory / reveal ingestion`)**:
   - The planned `BattleMemory / reveal ingestion` workstream will track CONFIRMED revealed player moves (along with observed items and abilities) as they occur during battle execution, safely re-injecting only confirmed moves into the shadow moveset on subsequent turns. This is strictly future/planned behavior.
