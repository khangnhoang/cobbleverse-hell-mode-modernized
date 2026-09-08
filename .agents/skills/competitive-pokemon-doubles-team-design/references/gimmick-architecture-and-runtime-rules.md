# Gimmick Architecture & Cobblemon Runtime Rules

This reference defines the strict mechanical constraints, schema requirements, and runtime invariants for battle gimmicks (Mega Evolution, Terastallization, Dynamax) and legal Pokémon attributes in Cobbleverse Hell Mode.

---

## 1. The Core NPC AI Axiom

> **Axiom:** If a battle gimmick is legal and available to an NPC in RCT, the AI may activate it on Turn 1 at the earliest opportunity. The NPC AI possesses zero human-like timing foresight and will NOT hold gimmicks for late-game sweeps or save them for low HP.

All gimmicks must be designed to be strategically sound if activated immediately on Turn 1.

---

## 2. Mega Evolution

### Representation & Syntax
- Mega Evolution is represented **strictly** via the Mega Showdown held item:
  ```json
  "heldItem": ["mega_showdown:<species>ite"]
  ```
  Example: `"mega_showdown:gengarite"`, `"mega_showdown:lucarionite"`, `"mega_showdown:charizardite_y"`.
- **FORBIDDEN:** Never reintroduce the obsolete `"mega": true` field inside the `"gimmicks"` record.

### Turn 1 Viability
- Immediate Turn 1 Mega Evolution is standard competitive practice because base stat increases (+100 BST), speed tier shifts, and new abilities (e.g., Shadow Tag, Tough Claws, Aerilate, Drought, Swift Swim) take effect immediately.

### Coexistence Rules
- RCT runtime evidence confirms that an NPC can Mega Evolve one Pokémon and Terastallize another in the same battle. This reflects mechanical coexistence in the mod, not deliberate sequential intelligence.

---

## 3. Terastallization (Tera)

### Assignment Rule
Assign Tera to a Pokémon **only** if immediate Turn 1 activation provides massive strategic value or alters key defensive matchups safely.

### Dual-Sided Evaluation Requirements
1. **Offensive Benefit:**
   - Does the moveset actually benefit from the new STAB?
   - *Rule:* Assigning Tera Fire without a Fire attack or `terablast` provides zero offensive benefit.
2. **Defensive Shift:**
   - What weaknesses are removed?
   - What NEW weaknesses and lost resistances are introduced?
   - *Rule:* Never claim a defensive Tera "cannot backfire" without auditing the entire defensive type chart against common Doubles attacks (Rock Slide, Surf, Earthquake, Dazzling Gleam, Close Combat).

---

## 4. Dynamax / Gigantamax

### Constrained Eligibility
- Expose Dynamax eligibility (`"gimmicks": {"dynamax": true}`) strictly to **1 intended user** or at most a tightly curated allowlist of 2–3 candidates.
- **FORBIDDEN:** Never expose Dynamax across an entire 6-Pokémon roster merely because the schema permits it.

### Turn 1 Soundness
- The eligible Dynamax user must gain immediate, massive value if activated on Turn 1:
  - Max Flare (sets Sun).
  - Max Geyser (sets Rain).
  - Max Rockfall (sets Sandstorm and boosts Rock SpD).
  - Max Quake / Max Steelspike (boosts team SpD or Def).
  - G-Max Malodor / G-Max Steelsurge (field hazards / persistent status).

---

## 5. Cobblemon 1.7.3 Data & Schema Invariants

### Species & Aspects
- Must be valid in the Cobblemon 1.7.3 registry.
- Exact aspect syntax:
  - Regional forms: `aspects: ["hisuian"]`, `aspects: ["alolan"]`, `aspects: ["galarian"]`, `aspects: ["paldean"]`.
  - Rotom appliances: `aspects: ["wash-appliance"]`, `aspects: ["heat-appliance"]`, `aspects: ["frost-appliance"]`, `aspects: ["mow-appliance"]`, `aspects: ["fan-appliance"]`.
  - Gender/Paldean variants: `aspects: ["female"]`, `aspects: ["combat_breed"]`.

### Movesets
- Exactly 4 valid Showdown move IDs (lowercase alphanumeric, no hyphens, no underscores, e.g., `tailwind`, `trickroom`, `solarbeam`, `leafblade`, `wideguard`, `fakeout`).

### Held Items
- Standard Cobblemon items default to bare names or `cobblemon:` prefix (e.g., `life_orb`, `focus_sash`, `choice_specs`, `leftovers`, `assault_vest`, `mental_herb`, `sitrus_berry`).
- Avoid unnamespaced Minecraft items (e.g., do not use bare `charcoal`; use `passho_berry` or `life_orb` instead).
- Mega Stones strictly use `mega_showdown:<species>ite`.

### Bag Invariants
- No revive items in the NPC trainer bag (`cobblemon:full_restore` or battle-only items only).
