---
name: competitive-pokemon-doubles-team-design
description: Design, modernize, or review competitive NPC Pokémon Doubles teams for Cobbleverse Hell Mode. Use when creating or refining 6-mon NPC rosters, establishing Doubles battle plans (weather, Trick Room, Tailwind, redirection, terrain), assigning items/abilities/moves, or evaluating turn-1 gimmick safety for RCT AI.
---

# Competitive Pokémon Doubles Team Design for Cobbleverse Hell Mode

This skill defines the core engineering principles, mechanical invariants, and evaluation methodology for designing, modernizing, and reviewing six-Pokémon NPC Doubles rosters in Cobbleverse Hell Mode.

---

## 1. Activation Scope & Ownership

Activate this skill whenever a task touches:
- Creating or refining 6-mon NPC trainer rosters in datapack JSON (`datapacks/hell-mode/data/rctmod/trainers/`);
- Establishing competitive team archetypes (Weather, Trick Room, Tailwind, Redirection, Terrain);
- Auditing team synergy, item/ability legality, speed control, or friendly-fire avoidance;
- Evaluating turn-1 gimmick safety (Mega Evolution, Terastallization, Dynamax) under RCT AI constraints;
- Reviewing trainer PRs or diffs for competitive coherence and schema compliance.

This skill owns competitive team methodology, synergy invariants, and gimmick turn-1 constraints. It does not own global agent lifecycle loops (governed by `AGENTS.md`), companion mod Mixin architecture (`docs/architecture/companion-mod.md`), or repository CI validation (`scripts/ci/validate_repo.py`).

---

## 2. Resource Routing

Read bundled references strictly when their conditions match:

| Resource | Read Condition | Skip When |
| :--- | :--- | :--- |
| [`references/doubles-archetypes-and-synergies.md`](references/doubles-archetypes-and-synergies.md) | Read before selecting or analyzing team archetypes (Weather, Trick Room, Tailwind, Redirection, Terrain), speed control mechanisms, win conditions, or interactive doubles combinations. | Archetype and speed tier are already determined, or task is an isolated data/syntax fix. |
| [`references/gimmick-architecture-and-runtime-rules.md`](references/gimmick-architecture-and-runtime-rules.md) | Read before assigning, evaluating, or auditing Mega Evolution, Terastallization, Dynamax, aspect syntax, or held item namespaces. | Roster contains zero gimmicks, or task does not evaluate gimmick mechanics. |
| [`references/failure-modes-and-plan-b.md`](references/failure-modes-and-plan-b.md) | Read before designing or reviewing team resilience against disruption (opposing weather, Trick Room denial, Wide Guard, early gimmick neutralization). | Task is a routine typo/moveset correction without strategic scope. |
| [`references/historical-case-studies.md`](references/historical-case-studies.md) | Read when analyzing comparative design examples, reviewing past pilot failures (Charon, Buck, Adaman), or resolving team balance debates. | Routine operational task with clear precedent. |
| [`references/trainer-audit-checklist.md`](references/trainer-audit-checklist.md) | Read before submitting a final trainer review checkpoint, completing an audit report, or validating modernized JSON against the full 6-part rubric. | Task is in exploratory discovery or brainstorming phase. |

---

## 3. Core Design Methodology & Order of Operations

A competitive Doubles team must be engineered from the top down as an interactive six-Pokémon system, not by assembling six disconnected strong Pokémon:

1. **Preserve Trainer Identity First:** Establish trainer lore, character role, progression level, and regional thematic identity. Retain signature Pokémon unless there is an irreconcilable runtime defect. Hell Mode difficulty comes from team synergy and competitive spreads, NOT arbitrary Legendary insertion or artificial stat inflation.
2. **Choose Battle Plan Before Individual Movesets:** Determine the primary engine (Tailwind, Trick Room, Weather, Terrain, Redirection, Spread Defense). One weather setter does not make a weather team; six isolated OU Pokémon do not make a Doubles team.
3. **Establish Concrete Win Conditions:** Identify the exact offensive or defensive pressure sequence that wins the battle, the required field state, and the primary damage dealers.
4. **Enforce Turn-Order Control:** Every team must deliberately control speed (multipliers, Trick Room turn inversion, spread drops, priority pressure, or redirection bulk).
5. **Eliminate Anti-Synergies & Friendly Fire:**
   - **FORBIDDEN:** Choice items (`choice_band`, `choice_specs`, `choice_scarf`) paired with Protect, setup, or status moves.
   - **FORBIDDEN:** Assault Vest paired with status, healing, or setup moves.
   - **FORBIDDEN:** Unshielded friendly-fire spread attacks (e.g., Earthquake without Flying/Levitate/Telepathy/Protect; Surf without Water Absorb/Storm Drain/Dry Skin).
   - **FORBIDDEN:** Redundant STAB moves of the same damage category without distinct utility reasons.

---

## 4. Critical NPC AI Invariant: Turn-1 Gimmick Axiom

> **The Turn-1 Gimmick Axiom:** If a battle gimmick is legal and available to an NPC in RCT, the AI may activate it on Turn 1 at the earliest opportunity. The NPC AI possesses zero human-like timing foresight and will NOT hold gimmicks for late-game sweeps or save them for low HP.

- **Mega Evolution:** Must be represented strictly via `heldItem: ["mega_showdown:<species>ite"]`. Never use obsolete `"mega": true` in `gimmicks`. Turn 1 activation is standard and highly viable.
- **Terastallization:** Assign Tera ONLY if immediate Turn 1 activation is strategically sound. Evaluate both offensive STAB gains and new defensive vulnerabilities. Never claim a defensive Tera "cannot backfire" without auditing the full defensive chart.
- **Dynamax / Gigantamax:** Restrict eligibility strictly to 1 intended user (or small allowlist of 2–3). Never expose Dynamax to all 6 Pokémon. The user must gain massive, immediate value if activated on Turn 1 (e.g., weather/terrain setting or persistent stat boosts).

---

## 5. Compositional Emergence vs. AI-Dependent Sequencing

Distinguish between interactions that occur naturally from game mechanics (reliable) versus those requiring human-level AI prediction (unreliable):
- **Reliable (Compositional Emergence):** Chlorophyll speed doubling under Sun, Mega Gengar trapping via Shadow Tag, slow attackers moving first under Trick Room, Wide Guard blocking spread moves, Redirection drawing single-target attacks.
- **Unreliable (AI-Dependent):** Reversing opposing Trick Room on prediction, holding gimmicks for late-game cleanup, switching into defensive resistances on prediction.
- *Rule:* If a tactic requires intelligent sequencing, classify it as `AI-dependent` and provide passive bulk or alternative lines so the team does not collapse if the AI makes a basic choice.

---

## 6. Language & Anti-Overclaim Invariants

Avoid unjustified certainty in design documentation and review reports:
- **FORBIDDEN:** *"guaranteed setup"*, *"cannot backfire"*, *"always optimal"*, *"completely shuts down"*, *"unbeatable"*.
- **REQUIRED:** State exact coverage, residual disruption windows (e.g., Fake Out flinches, double focus-fire, opposing priority, Prankster Taunt), and specific type interactions.
