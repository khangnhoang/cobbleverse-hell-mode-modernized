# CP4 Live Runtime Failure — Battle Evidence & Narrow Discovery Brief

## Context

Branch:

`feat/surge-toxtricity-strategy`

Relevant implementation commit:

`81c6b00 feat(companion): implement strategy layer CP1-CP3 with runtime contract correction`

Pre-live verification before CP4:

- Runtime contract: `33/33 PASS`
- `./gradlew test --rerun-tasks`: `BUILD SUCCESSFUL`
- `./gradlew build`: `BUILD SUCCESSFUL`
- Repo validator: `PASS`
- Legacy baseline: `PASS`

Frozen strategy flow:

```text
RunBunAI.choose(...)
→ native delegate response
→ StrategicBattleAIDecorator
→ rewrite only while ThroatSpraySoundPolicy is eligible
→ otherwise return native response unchanged
```

Frozen post-consumption behavior:

```text
Throat Spray still present
→ strategy may force Overdrive

Throat Spray consumed
→ strategy must drop out
→ native R&B decision must pass through unchanged
```

Current adapter eligibility source:

```java
battlePokemon.getHeldItemManager().showdownId(battlePokemon)
```

CP4 live testing shows that this source is not yet proven authoritative after item consumption.

---

# Live Battle Timeline

Battle ID:

`0d147df6-1e2c-4b98-8cef-b5e63a51a745`

Trainer:

`kanto_ltsurge`

Initial player leads:

- Incineroar
- Rillaboom

Dynamic lead selector:

```text
[HellMode-Lead] Trainer=kanto_ltsurge,
PlayerLeads=[incineroar, rillaboom],
Selected=terrain_punk,
Scores=[terrain_surfer=-2, terrain_punk=2, anti_ground=-3]
```

Lt. Surge opened with:

- Pincurchin
- Toxtricity Low-Key

---

## Turn 1 — Tera preserved, move prevented by Fake Out

### Native R&B decision

At `02:15:53`:

```text
terablast   vs Incineroar -> Score -5  -> Damage 40
sludgebomb  vs Incineroar -> Score 5   -> Damage 68
protect     vs Incineroar -> Score 6   -> Damage 0
overdrive   vs Incineroar -> Score -5  -> Damage 44

terablast   vs Rillaboom  -> Score 5   -> Damage 62
sludgebomb  vs Rillaboom  -> Score 12  -> Damage 207
protect     vs Rillaboom  -> Score 6   -> Damage 0
overdrive   vs Rillaboom  -> Score -5  -> Damage 34

CHOOSEN BEST MOVE sludgebomb -> Rillaboom -> 207 dmg
```

### Actual battle log

```text
Lt. Surge's Toxtricity has Terastallized into the Grass-type!

Khangbodoipromax's Incineroar used Fake Out on Lt. Surge's Toxtricity!

Lt. Surge's Toxtricity flinched and couldn't move!
```

### Interpretation

Verified:

- native Tera lifecycle was preserved;
- Toxtricity Terastallized into Grass;
- the selected move did not execute because of Fake Out;
- Throat Spray should remain unconsumed;
- strategy eligibility on the next decision is expected.

This validates retry semantics:

```text
prevented move execution
≠
strategy permanently consumed
```

---

## Turn 2 — Rewrite succeeds and Throat Spray activates

Rillaboom was replaced by Swampert before this decision.

### Native R&B decision

At `02:16:39`:

```text
overdrive   vs Incineroar -> Score -5  -> Damage 44
protect     vs Incineroar -> Score 6   -> Damage 0
sludgebomb  vs Incineroar -> Score 5   -> Damage 68
terablast   vs Incineroar -> Score -5  -> Damage 20

overdrive   vs Swampert -> Score -50 -> Damage 0
protect     vs Swampert -> Score 6   -> Damage 0
sludgebomb  vs Swampert -> Score 5   -> Damage 47
terablast   vs Swampert -> Score 14  -> Damage 224

CHOOSEN BEST MOVE terablast -> Swampert -> 224 dmg
```

### Actual battle log

```text
Lt. Surge's Toxtricity used Overdrive!

But Khangbodoipromax's Swampert is immune!

%1$s used its %2$s!

Lt. Surge's Toxtricity's Special Attack rose.
```

The `%1$s used its %2$s!` line appears to be a broken held-item localization/template message.

### Interpretation

This proves end-to-end rewrite:

```text
native R&B response: Tera Blast
→ actual executed move: Overdrive
```

The immediate Special Attack increase after Overdrive is consistent with Throat Spray activation.

Verified live behavior:

- Overdrive executed;
- spread targeting worked;
- Swampert immunity was respected;
- Throat Spray effect activated;
- Toxtricity gained Special Attack;
- the held item should now be consumed in authoritative battle state.

---

## Turn 3 — Post-consumption drop-out fails

### Native R&B decision

At `02:17:15`:

```text
overdrive   vs Incineroar -> Score -5  -> Damage 44
protect     vs Incineroar -> Score 6   -> Damage 0
terablast   vs Incineroar -> Score -5  -> Damage 20
sludgebomb  vs Incineroar -> Score 14  -> Damage 68

overdrive   vs Swampert -> Score -50 -> Damage 0
protect     vs Swampert -> Score 6   -> Damage 0
terablast   vs Swampert -> Score 12  -> Damage 224
sludgebomb  vs Swampert -> Score 5   -> Damage 47

CHOOSEN BEST MOVE sludgebomb -> Incineroar -> 68 dmg
```

### Actual battle log

```text
Lt. Surge's Pincurchin used Sucker Punch on Swampert!

Lt. Surge's Toxtricity used Overdrive!

But Khangbodoipromax's Swampert is immune!
```

### Interpretation

This is the CP4 failure.

Native R&B chose:

```text
Sludge Bomb
```

but the battle executed:

```text
Overdrive
```

Therefore the strategy rewrote the native response again after the previous turn's Throat Spray activation.

Expected:

```text
Throat Spray consumed
→ policy ineligible
→ native Sludge Bomb passes through
```

Actual:

```text
Throat Spray activated on previous turn
→ policy still appears eligible
→ native Sludge Bomb rewritten to Overdrive
```

---

## Later evidence

At `02:23:20`, much later in the same battle, R&B still generated a native Toxtricity decision:

```text
sludgebomb  vs Rillaboom -> Score 12 -> Damage 207
terablast   vs Swampert  -> Score 12 -> Damage 224

CHOOSEN BEST MOVE terablast -> Swampert -> 224 dmg
```

This makes a simple immediate one-tick synchronization delay less likely.

Treat the issue as a persistent state-source or lifecycle problem until proven otherwise.

---

# CP4 Adjudication

## Passed

- Per-battle strategy wiring
- Delegate-first behavior
- Tera lifecycle preservation
- Grass Tera execution
- Conservative move-response rewrite path
- Overdrive rewrite
- Spread target semantics
- Retry after Fake Out / prevented move
- Throat Spray activation
- Special Attack increase
- Native R&B continues to compute independently

## Failed

- Post-Throat-Spray eligibility drop-out

## Status

```text
CP4 FAILED — narrow runtime lifecycle/state defect
```

Do not reopen the whole architecture.

The failure is localized to how the strategy determines whether Throat Spray is still present after battle consumption.

---

# Primary Discovery Question

Current adapter reads:

```java
battlePokemon.getHeldItemManager().showdownId(battlePokemon)
```

Frozen design assumed this represented authoritative current battle held-item state.

Live evidence now shows that after the battle engine visibly activates Throat Spray and raises Special Attack, the strategy still behaves as eligible.

Primary question:

> What state visible to `BattleAI.choose()` authoritatively reflects a consumed held item on the next decision?

Do not assume `HeldItemManager.showdownId(...)` is stale until the runtime path is traced.

---

# Required Discovery

## D1 — Trace Throat Spray consumption from Showdown into Java state

Find the exact path for a held-item consumption event, especially the Showdown protocol event corresponding to item removal, e.g.:

```text
|-enditem|...
```

Trace:

```text
Showdown battle message
→ Cobblemon/RCT parser or handler
→ BattlePokemon / Pokemon / HeldItemManager mutation
→ state visible to BattleAI.choose()
```

Answer:

1. Which Java object is mutated when an item is consumed?
2. Which method performs that mutation?
3. Does it mutate `BattlePokemon`, underlying `Pokemon`, `HeldItemManager`, a Showdown-only representation, or another battle-state object?
4. Is the NPC trainer's battle Pokémon updated through the same path as player Pokémon?

Provide source or bytecode evidence.

---

## D2 — Determine what `HeldItemManager.showdownId(battlePokemon)` actually represents

Inspect:

```java
battlePokemon.getHeldItemManager()
```

and:

```java
HeldItemManager.showdownId(BattlePokemon)
```

Determine whether it reads:

- original configured item;
- current underlying `Pokemon` item;
- current battle item;
- computed Showdown export state;
- cached state;
- another source.

Specifically answer:

> After Throat Spray is consumed, why can this method still allow the policy to match `"throatspray"` on a later AI decision?

If it should not, identify the broken propagation path instead.

---

## D3 — Verify object identity and copy lifecycle

Trace:

```text
RCTMod.makeBattle
→ TrainerNPC
→ BattleActor
→ BattlePokemon
→ RunBunAI / StrategicBattleAIDecorator.choose()
```

Determine whether the `BattlePokemon` passed to the decorator is:

- the same runtime battle object mutated by battle events;
- reconstructed between decisions;
- backed by the original trainer `Pokemon`;
- copied from another source;
- stale relative to Showdown state.

Check identity/copy boundaries, not just equal field values.

---

## D4 — Find authoritative alternatives already exposed by runtime

Before adding custom strategy lifecycle state, inspect whether the current battle runtime already exposes an authoritative current-item source.

Possible categories, only if they actually exist:

- current underlying `Pokemon` held-item state;
- battle actor / battle Pokémon state;
- Showdown request or battle-state snapshot;
- battle message/event history;
- volatile battle properties;
- another Cobblemon/RCT battle API.

For every candidate answer:

```text
available inside BattleAI.choose()?
updated after item consumption?
works for NPC battle Pokémon?
stable across switch/re-entry?
```

Prefer an existing authoritative source if one exists.

---

## D5 — Establish timing semantics

Determine ordering around the successful Throat Spray turn:

```text
AI choose
→ move request sent
→ Overdrive executes
→ Throat Spray activates
→ item consumption event processed
→ next request / next AI choose
```

Confirm whether item-removal state is guaranteed to be applied before the next `BattleAI.choose()`.

The later evidence makes a persistent issue more likely than a one-tick race, but verify rather than assume.

---

## D6 — Check switch/re-entry semantics

If Toxtricity switches out after Throat Spray consumption and later returns:

- what held-item state does AI observe?
- could an active-Pokémon wrapper lose consumed state?
- is there stable battle Pokémon identity across switch/re-entry?

This matters if the eventual fix needs state beyond the current active object.

---

# Do Not Implement a Fix Yet

Discovery only.

Do not immediately add:

```java
boolean sprayUsed;
```

or equivalent custom state.

Do not change the frozen strategy architecture merely because the current state source failed live verification.

First establish whether an authoritative runtime item state already exists.

Only if discovery proves that Java-side AI cannot reliably observe consumed held-item state should a custom per-battle strategy lifecycle mechanism be considered.

---

# Candidate Fix Classes — Only After Discovery

## A. Better authoritative runtime state exists

Preferred.

```text
adapter reads correct current-item state
→ domain contract remains conceptually unchanged
→ minimal adapter correction
```

## B. Existing state should update but propagation is broken

Determine whether the defect is in:

- Cobblemon;
- RCT;
- companion wiring/copy semantics.

Avoid patching upstream globally unless required.

Prefer a local reliable read path.

## C. No authoritative consumed-item state is exposed to BattleAI

Only then consider explicit per-battle strategy lifecycle state.

Any such design must correctly handle:

- successful Overdrive + Spray activation;
- Fake Out / flinch / failed move must NOT mark consumed;
- switching out and back in;
- new battle reset;
- no cross-battle leakage;
- trainer singleton mutation isolation;
- relevant immune/failure semantics according to actual Throat Spray behavior.

Do not implement this category without a separate correction design.

---

# Required Discovery Report

Return:

## 1. Current state source

What exactly does:

```java
battlePokemon.getHeldItemManager().showdownId(battlePokemon)
```

read from?

## 2. Consumption path

Exact source/bytecode path for held-item removal.

## 3. Object lifecycle

Which object is mutated, copied, or stale?

## 4. Next-turn visibility

What item state is visible to `BattleAI.choose()` after consumption?

## 5. Root cause

Classify as:

```text
CONFIRMED
LIKELY
INCONCLUSIVE
```

Do not overstate evidence.

## 6. Narrow correction options

Rank smallest safe correction first.

For each option include:

- likely files affected;
- new state, if any;
- lifecycle risks;
- tests required;
- whether frozen Contract 5 needs wording correction.

## 7. Recommendation

Recommend one correction direction.

Do not implement it.

---

# Scope Restrictions

During discovery:

- do not modify source files;
- do not modify tests;
- do not modify the frozen plan;
- do not modify `kanto_ltsurge.json`;
- do not patch `RunBunAI.choose()`;
- do not patch the upstream `0.5625` spread-damage bug;
- do not commit;
- do not push;
- do not create PRs.

Temporary scratch scripts for bytecode inspection are allowed, but remove them before final report unless explicitly requested otherwise.

Final action:

```text
DISCOVERY REPORT
→ STOP
```
