# Reusable Trainer Audit & Review Checklist

This reference provides the canonical 6-part checklist required for reviewing, modernizing, or creating NPC Doubles trainer definitions in Cobbleverse Hell Mode.

---

## 1. Lore, Identity & Constraints
- [ ] Trainer lore, character role (Gym Leader, Clan Leader, Galactic Executive, Facility Staff), and progression level preserved?
- [ ] Signature Pokémon retained unless there is an irreconcilable runtime defect?
- [ ] Complete 6-Pokémon roster provided?
- [ ] Difficulty calibrated via synergy, competitive itemization, and full EV spreads rather than arbitrary Legendary insertion?

## 2. Doubles Team Architecture
- [ ] Clear team archetype established (Tailwind, Trick Room, Weather, Redirection, Terrain, Spread Defense)?
- [ ] Primary win condition explicitly identified?
- [ ] Primary Turn 1 lead pair established?
- [ ] Speed control method identified and supported by teammates?

## 3. Synergy, Friendly-Fire & Item Safety
- [ ] Spread attacks (Earthquake, Surf) checked for friendly-fire avoidance (Flying, Levitate, Telepathy, Water Absorb, Storm Drain, Protect)?
- [ ] **ZERO Choice items** (`choice_band`, `choice_specs`, `choice_scarf`) paired with Protect, setup moves, or status moves?
- [ ] **ZERO Assault Vests** paired with status, healing, or setup moves?
- [ ] **ZERO redundant STAB attacks** of the same damage category without distinct utility reasons?
- [ ] Complementary defensive typings and pivots present?

## 4. Gimmick Architecture & Turn 1 Soundness
- [ ] Mega Evolution represented strictly via `heldItem: ["mega_showdown:<species>ite"]`?
- [ ] **ZERO obsolete fields** (`"mega": true` inside `"gimmicks"`)?
- [ ] Dynamax eligibility (`"gimmicks": {"dynamax": true}`) restricted to exactly 1 intended user (or tightly curated allowlist of 2–3)?
- [ ] Tera assignment audited for both offensive STAB gains and new defensive vulnerabilities?
- [ ] All gimmicks strategically sound if activated immediately on Turn 1?

## 5. AI Realism & Failure Modes
- [ ] Plan emerges naturally from compositional mechanics rather than requiring human-level AI prediction?
- [ ] Key failure modes analyzed (interrupted setup, weather overwrite, early KO)?
- [ ] Plan B exists without introducing conflicting field conditions?

## 6. Schema & Cobblemon Runtime Legality
- [ ] `battleFormat: "GEN_9_DOUBLES"` declared?
- [ ] All moves (exactly 4 per Pokémon) exist in Showdown registry (lowercase alphanumeric, no hyphens/underscores)?
- [ ] Species and aspects strictly valid in Cobblemon 1.7.3 registry?
- [ ] Held items valid and correctly namespaced?
- [ ] No revive items in trainer bag (`cobblemon:full_restore` or battle-only items only)?
