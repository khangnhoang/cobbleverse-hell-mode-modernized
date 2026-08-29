---
name: competitive-pokemon-doubles-team-design
description: Design, modernize, or review competitive NPC Pokémon Doubles teams for Cobbleverse Hell Mode. Use when creating or refining 6-mon NPC rosters, establishing Doubles battle plans (weather, Trick Room, Tailwind, redirection, terrain), assigning items/abilities/moves, or evaluating turn-1 gimmick safety for RCT AI.
---

# Competitive Pokémon Doubles Team Design for Cobbleverse Hell Mode

This skill teaches how to design, modernize, and review six-Pokémon NPC Doubles teams as coherent battle systems for Cobbleverse Hell Mode. It is grounded in current RCT/Mega Showdown runtime capabilities and concrete lessons learned from repository pilot implementations.

---

## 1. Core Workflow: Team Design Order of Operations

A competitive Doubles team must be designed from the top down as an interactive six-Pokémon engine, not by assembling six disconnected strong Pokémon.

### Step 1: Preserve Trainer Identity First
Before touching any JSON or selecting Pokémon, establish:
- **Trainer lore & role:** Gym Leader, Clan Leader, Villain Executive, Stat Companion, Thematic Specialist, or Facility Staff.
- **Progression level:** Level placement determines viable evolutions, base stat totals, and damage scaling.
- **Thematic/type identity:** Honor established regional themes (e.g., Hisui ancient forms, Floaroma honey/bee pollinators, Galactic heavy machinery).
- **Signature & intentional Pokémon:** Retain key partners (e.g., Claydol for Buck, Glaceon for Irida, Rotom appliances for Charon) unless there is an irreconcilable runtime defect.
- **Difficulty calibration:** Hell Mode demands high synergy, full 6-mon depth, and competitive item/EV spreads. However:
  - Do NOT solve difficulty by arbitrarily inserting top-tier Legendaries/Mythicals.
  - Do NOT use arbitrary stat inflation as a substitute for team synergy.

### Step 2: Choose the Battle Plan Before Individual Movesets
Define the core system the team executes before picking four moves for slot 1. Supported archetypes include:
- **Tailwind Offense:** Fast tempo, spread pressure, and priority speed advantage.
- **Trick Room (Hard or Semi-Room):** Minimum-speed sweepers abusing inverted turn orders.
- **Weather Synergies:**
  - *Sun:* Chlorophyll speed doubling, Flower Gift buffs, Solar Blade, and uncharged Fire blasts.
  - *Rain:* Swift Swim doubling, 100% accurate Hurricane, boosted Water STAB, and Storm Drain redirection.
  - *Snow/Hail:* 50% Ice Defense boost, 100% accurate Blizzard spreads, and Slush Rush.
- **Electric / Terrain Offense:** Electric Surge powering Quark Drive paradox threats and preventing Sleep.
- **Shadow Tag / Trapping:** Eliminating player switching to force fatal elemental matchups.
- **Redirection + Setup / Wallbreaking:** Follow Me, Rage Powder, or Ally Switch shielding a dedicated sweeper.
- **Spread Defense / Control:** Wide Guard countering Rock Slide/Earthquake/Heat Wave/Surf/Blizzard while pivoting with Intimidate and Snarl.

*Anti-Pattern Warning:*
- One weather setter does not automatically create a weather team.
- One Trick Room setter does not create a Trick Room team.
- Six individually viable OU Pokémon do not make a coherent Doubles team.

### Step 3: Identify the Win Condition
Explicitly establish:
1. What sequence of pressure actually wins the battle for this team?
2. Which Pokémon deliver that decisive offensive or defensive pressure?
3. What board state must exist before that win condition becomes lethal (e.g., active Tailwind, Sun, Trick Room, trapped targets, screens)?
4. Which teammates create those setup opportunities?
5. What is the team's Plan B when the primary game plan is interrupted?

### Step 4: Establish Speed Control
Every team must deliberately control turn order or explicitly plan for being slower:
- **Active speed multipliers:** Tailwind, Chlorophyll, Swift Swim, Slush Rush, Quark Drive (Speed).
- **Turn inversion:** Trick Room (must have genuine minimum-Speed beneficiaries, e.g., Brave/Quiet 0-Spe IVs).
- **Spread speed drops:** Icy Wind, Electroweb, Snarl.
- **Priority pressure:** Fake Out (tempo), Extreme Speed (+2), Sucker Punch, Aqua Jet, Shadow Sneak, Bullet Punch, Ice Shard.
- **Bulky durability:** High defensive bulk, redirection, and Wide Guard designed to absorb hits without traditional speed advantage.

### Step 5: Design Real Doubles Interactions
Evaluate interactive combinations across the complete team:
- **Fake Out:** Buys free setup turns on Turn 1 for Tailwind, weather, screens, or Trick Room.
- **Redirection (Follow Me / Rage Powder):** Diverts single-target super-effective hits away from setup sweepers or Motor Drive partners.
- **Wide Guard:** Shields the entire team against spread damage (Earthquake, Surf, Rock Slide, Heat Wave, Blizzard, Hyper Voice).
- **Friendly-Fire Avoidance:** If a Pokémon uses Earthquake or Surf:
  - Partners must have Levitate, Flying type, Telepathy, Water Absorb, Storm Drain, Protect, or Air Balloon.
  - Otherwise, prefer single-target moves like `highhorsepower` or `liquidation` over `earthquake` or `surf`.
- **Intimidate & Snarl:** Lowers opponent physical and special output on entry/spread, compounding defensive bulk.
- **Helping Hand & Pollen Puff:** Elevates partner damage thresholds or restores 50% partner HP.

### Step 6: Build Coherent Individual Pokémon
Select attributes strictly after the team plan is solidified:
- **Species & Aspects:** Strictly verified against Cobblemon 1.7.3 registry. Use exact aspect syntax (e.g., `aspects: ["hisuian"]`, `aspects: ["wash-appliance"]`, `aspects: ["alolan"]`).
- **Abilities:** Synergize with the field (e.g., Drought + Chlorophyll, Drizzle + Swift Swim, Electric Surge + Quark Drive, Defiant/Competitive to punish Intimidate).
- **Held Items:** Legal, namespaced where required, and non-conflicting.
  - *Mega Stones:* `mega_showdown:<stone>` (e.g., `mega_showdown:gengarite`).
  - *Namespace rules:* Standard Cobblemon items default to bare names or `cobblemon:`. Avoid unnamespaced Minecraft items (e.g., do not use bare `charcoal`; use `passho_berry` or `life_orb` instead).
  - *No revives in bag:* Only in-battle healing items (e.g., `cobblemon:full_restore`).
- **Nature, EVs, IVs:**
  - Fast sweepers: 252 Spe / 252 Atk or SpA / 4 HP with Jolly/Timid.
  - Trick Room sweepers: 252 HP / 252 Atk or SpA / 4 SpD with Brave/Quiet and **0 Speed IVs**.
  - Defensive anchors: 252 HP / optimized Def and SpD splits with Impish/Bold/Careful/Calm.
- **Movesets:** Exactly 4 valid Showdown move IDs (lowercase alphanumeric, no hyphens or underscores).

*Anti-Synergy Checklist:*
- NEVER give a Choice item (`choice_band`, `choice_specs`, `choice_scarf`) to a Pokémon with Protect, setup moves, or status moves.
- NEVER give `assault_vest` to a Pokémon with status, healing, or setup moves.
- NEVER run redundant STAB attacks of the same damage category without distinct utility reasons (e.g., running both Flamethrower and Fire Blast without a specific utility distinction).

---

## 2. Critical NPC AI Constraint: Gimmick Architecture

In Cobbleverse RCT, the NPC AI does NOT possess human-level timing foresight.
**Core AI Axiom:** If a battle gimmick is legal and available, the NPC AI may activate it on Turn 1 at the earliest opportunity.

Design all gimmicks under this strict constraint:

### Terastallization
- **Rule:** Only assign Tera to a Pokémon if immediate Turn 1 activation is strategically sound.
- **Evaluation Requirements:**
  - Do NOT design Tera assuming the AI will wait until low HP or save it for a late-game sweep.
  - Evaluate both sides of the type change:
    - *Offensive:* Does the Pokémon's moveset actually benefit from the new STAB? (e.g., running Tera Fire without a Fire-type attack or Tera Blast yields zero offensive value).
    - *Defensive:* What weaknesses are removed, but what NEW weaknesses and lost resistances are introduced?
  - Never claim a defensive Tera "cannot backfire" without evaluating the complete defensive matchup.

### Dynamax / Gigantamax
- **Constrained Eligibility:** Expose Dynamax eligibility (`"gimmicks": {"dynamax": true}`) strictly to **1 intended user** or at most a tightly curated allowlist of 2–3 candidates.
- **Turn 1 Soundness:** The eligible Dynamax user must gain immediate, massive value if activated on Turn 1 (e.g., Max Rockfall setting Sandstorm and boosting SpD, Max Quake boosting SpD, Max Flare setting Sun, or G-Max Malodor poisoning both opponents).
- **Rule:** NEVER expose Dynamax across the entire 6-Pokémon roster merely because the schema allows it.

### Mega Evolution
- **Syntax:** Must be represented strictly via the Mega Showdown held item: `heldItem: ["mega_showdown:<species>ite"]`.
- **No Obsolete Fields:** NEVER reintroduce the obsolete `"mega": true` key inside the `gimmicks` record.
- **Immediate Viability:** Immediate Turn 1 Mega Evolution is standard competitive practice because the stat boosts, ability changes (e.g., Shadow Tag, Aerilate, Tough Claws, Mold Breaker), and speed tier adjustments take effect immediately.
- **Coexistence Note:** RCT runtime evidence proves that an NPC can Mega Evolve one Pokémon and Terastallize another in the same battle. This proves mechanical coexistence, NOT intelligent sequential timing.

---

## 3. AI Realism: Compositional Emergence vs. AI-Dependent Sequencing

When designing an NPC team, distinguish between behaviors that occur naturally from mechanics versus behaviors that require intelligent decision-making.

| Emerges Naturally from Composition (Reliable) | Requires Intelligent Sequencing (AI-Dependent) |
| :--- | :--- |
| Chlorophyll doubling Speed under active Sun | Recognizing opponent Trick Room and deliberately reversing it |
| Mega Gengar trapping opponents via Shadow Tag | Saving a gimmick for the optimal late-game cleaner |
| Slow Rhyperior moving first under active Trick Room | Dynamically selecting a defensive Tera to counter a specific attack |
| Spread moves (Heat Wave, Rock Slide) hitting both slots | Executing multi-turn defensive switch-cycling into resistances |
| Redirection (Follow Me) automatically drawing single-target hits | Predicting an opponent's Protect and doubling into the other slot |

**Guideline:** If a tactic depends on AI-dependent sequencing, mark it as `AI-dependent` and provide passive durability or alternative lines so the team does not crumble when the AI makes a basic choice.

---

## 4. Failure-Mode Reasoning & Plan B

Every trainer design must withstand common competitive disruptions:
1. **Disrupted Speed Control:** What happens if the Tailwind setter is Taunted/KO'd, or Trick Room is delayed?
2. **Weather / Terrain Overwrite:** What happens if the player brings opposing weather (e.g., Pelipper Rain against Adaman's Sun)?
3. **Redirection & Spread Counterplay:** What happens if the player uses Wide Guard against the team's primary spread attack?
4. **Early Loss of Gimmick User:** If the primary Dynamax or Mega user is neutralized early, does the remaining team have independent offensive presence?

**Guideline:** Equip teams with secondary speed control (e.g., priority attacks, Icy Wind, secondary setters) or complementary offensive typings so the battle remains challenging if Plan A fails.

---

## 5. Anti-Overclaim Language Rules

Avoid unjustified absolutes in team reviews and design documentation:
- Do NOT use: *"guaranteed setup"*, *"cannot backfire"*, *"always optimal"*, *"completely shuts down"*, or *"unbeatable"*.
- DO use: *"resists common priority"*, *"shields against single-target attacks"*, *"heavily pressures Ground switch-ins"*, *"removes Bug/Grass weaknesses while introducing Water/Rock weaknesses"*.

State precisely what a tool covers and what counterplay remains viable.

---

## 6. Concrete Case Studies from the Phase E Pilot

Apply these three concrete lessons from the merged Phase E pilot to all future designs:

### Case 1: Charon / Mega Gengar (Positive Example — Compositional Emergence)
- **Design:** Lead Mega Gengar (`mega_showdown:gengarite`) alongside Rotom-Wash.
- **Why It Works:** Mega Gengar immediately activates Shadow Tag on Turn 1. This traps both opposing Pokémon on the field without requiring the AI to time anything. Trapped opponents cannot switch away from Rotom's Hydro Pump/Will-O-Wisp or Crobat's Super Fang.
- **Lesson:** The best NPC Doubles strategies rely on persistent field states and immediate ability effects that function automatically upon entry.

### Case 2: Buck / Dusknoir (Good Architecture, Unjustified Certainty)
- **Design:** Minimum-Speed Trick Room team with Mental Herb Dusknoir, Drought Torkoal, and sole Dynamax Rhyperior.
- **The Reasoning Flaw:** Claiming Mental Herb Dusknoir offers *"guaranteed Trick Room"*.
- **Why It Was Flawed:** While Mental Herb stops single-target Taunt and Encore, Trick Room can still be disrupted by flinches (Fake Out), double-target focus-fire KOs, Imprison, or opposing priority.
- **Lesson:** Never claim setup is guaranteed. Always analyze failure modes and acknowledge disruption windows.

### Case 3: Adaman / Leafeon (Immediate Gimmick Robustness)
- **Design:** Chlorophyll Sun team assigning Tera Fire to Leafeon.
- **The Reasoning Flaw:** Claiming Tera Fire *"cannot backfire"* and creates an uncounterable turn-1 threat.
- **Why It Was Flawed:**
  1. Leafeon's moveset had Solar Blade, Leaf Blade, Bite, Protect. Without a Fire-type attack (like Tera Blast Fire or Sunny Day boosted Fire attacks), Tera Fire provided zero offensive STAB boost.
  2. While Tera Fire removes Ice, Bug, and Fire weaknesses, it introduces weaknesses to Water, Rock, and Ground—critical vulnerabilities in Doubles against common moves like Surf, Muddy Water, Rock Slide, and High Horsepower.
- **Lesson:** Always audit the complete defensive chart and ensure the moveset actually leverages the Tera type before approving it for an NPC.

---

## 7. Reusable Per-Trainer Reasoning Checklist

Before writing or approving any modernized trainer JSON, evaluate this concise checklist:

1. **Identity & Constraints:**
   - [ ] Lore, progression level, and trainer theme preserved?
   - [ ] Signature Pokémon retained?
   - [ ] Full 6-Pokémon roster provided?
2. **Doubles Architecture:**
   - [ ] Clear archetype established (Tailwind, Trick Room, Weather, Redirection, Terrain)?
   - [ ] Win condition clearly identified?
   - [ ] Primary turn-1 lead pair established?
   - [ ] Speed control method identified and supported by teammates?
3. **Synergy & Safety:**
   - [ ] Spread moves checked for friendly-fire avoidance (immunities, Protect, or single-target substitutes)?
   - [ ] Zero Choice items paired with Protect, setup, or status moves?
   - [ ] Zero Assault Vests paired with status moves?
   - [ ] Complementary defensive typings and pivots present?
4. **Gimmick Architecture:**
   - [ ] Mega Evolution uses `mega_showdown:<stone>` held item with zero obsolete `gimmicks.mega` fields?
   - [ ] Dynamax eligibility restricted to 1 intended user (or small allowlist)?
   - [ ] Tera type evaluated for both offensive gains and new defensive vulnerabilities?
   - [ ] Gimmick is strategically viable if activated immediately on Turn 1?
5. **AI Realism & Failure Modes:**
   - [ ] Does the plan emerge naturally from composition rather than requiring AI sequencing?
   - [ ] Failure modes identified (interrupted setup, weather overwrite, early KO)?
   - [ ] Plan B exists without cluttering the team with unrelated mechanics?
6. **Schema & Runtime Legality:**
   - [ ] `battleFormat: "GEN_9_DOUBLES"`?
   - [ ] All 4 moves per Pokémon exist in Showdown move registry (lowercase alphanumeric)?
   - [ ] Species and aspects strictly valid in Cobblemon 1.7.3?
   - [ ] Held items valid and correctly namespaced?
   - [ ] No revive items in bag (`cobblemon:full_restore` only)?
