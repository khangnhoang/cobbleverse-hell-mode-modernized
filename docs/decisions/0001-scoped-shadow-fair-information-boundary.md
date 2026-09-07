# 1. Scoped Shadow BattlePokemon for Fair Information Boundary

- **Date:** 2026-09-07
- **Status:** Accepted (Implemented in PR #18)
- **Deciders:** Repository Owner & Antigravity

---

## Context & Problem Statement

Native Cobblemon and Run & Bun AI (`rbrctai`) query real battle objects directly during decision-making. Consequently, NPC AI possesses omniscient access to the player's Pokémon:
- Real active and benched movesets before any move is used.
- Exact held items before consumption or reveal.
- True abilities before trigger.
- Configured Terastallization target and type.

This omniscience prevents competitive bluffing, distorts damage scoring, and creates unfair difficulty.

We needed an anti-cheat mechanism that hides private opponent state during AI decision evaluation, while preserving real server battle execution and native battle mechanics.

---

## Considered Options

### Option 1: Wrap or Detach `ActiveBattlePokemon` (Rejected)
- **Concept:** Replace the opponent's `ActiveBattlePokemon` (ABP) references with dummy or wrapped ABP objects when passing them into `RunBunAI.choose()`.
- **Why Rejected:** In executable feasibility spikes, replacing ABP instances completely broke Cobblemon's internal targeting, side mapping, and native PNX index calculation. ABP identity is deeply coupled with native battle actor mechanics.

### Option 2: Mutate Real Objects In-Place and Restore (Rejected)
- **Concept:** Temporarily wipe moves and items on the player's real `BattlePokemon` before calling `RunBunAI.choose()`, then restore them immediately after.
- **Why Rejected:** Exceptionally dangerous. If an unhandled exception or crash occurs during `choose()`, the player's real Pokémon permanently lose their moves or held items. Furthermore, any concurrent thread or packet sync observing the battle during that window would observe corrupted data.

### Option 3: Scoped ThreadLocal Getter Firewall (Accepted)
- **Concept:**
  1. Maintain the real `ActiveBattlePokemon` object identity untouched.
  2. Wrap `RunBunAI.choose()` with a scoped `FairBattleContext` (`ThreadLocal`) using strict `try-finally`.
  3. Intercept `ActiveBattlePokemon.getBattlePokemon()` via Mixin:
     - If `FairBattleContext` is active AND the queried ABP belongs to the opponent side, return a sanitized shadow `BattlePokemon`.
     - If the context is inactive or the ABP belongs to the NPC's own side, return the real `BattlePokemon` unmodified.

---

## Decision Outcome

**Chosen Option:** Option 3 (Scoped ThreadLocal Getter Firewall).

### Positive Consequences
- **Zero Real Battle Mutation:** The player's and server's real objects are never modified.
- **Native PNX Intact:** `ActiveBattlePokemon` identity, actor references, and position indices remain 100% native.
- **Strict Leak Prevention:** Outside of `RunBunAI.choose()`, `FairBattleContext.isActive()` returns `false`, ensuring normal battle ticks, damage applications, and client syncs see real data.
- **Symmetric Safety:** The NPC AI retains full knowledge of its own moves, item, and ability.

### Negative Consequences / Trade-offs
- **Recharge Check Vulnerability:** Native `RunBunAI.choose()` unconditionally executed `oppMoves.getFirst().getName().equals("recharge")`. With opponent movesets sanitized (empty), this threw `NoSuchElementException`.
- **Mitigation:** Solved via a targeted `@WrapOperation` in `RunBunAIChooseMixin` tightly sliced around the recharge check, returning a safe sentinel move when the moveset is empty.
