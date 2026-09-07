# Executive Decision Brief: Hell Mode Modernization

This document provides a high-level summary of the modernization plan for quick review by the repository owner.

---

## 1. What Are We Changing?
- **Updating the Legacy Addon:** Transitioning Doctor's Hell Mode (~April 2026 snapshot) to be 100% compatible with current **Cobbleverse 1.7.42**, **Cobblemon 1.7.3**, and **RCT 0.18.1-beta**.
- **Fixing Content Bugs:** Correcting 28 broken held-item IDs (hyphenated Z-Crystals, item typos), repairing form aspect tags, and removing invalid gimmick keys.
- **Filling Upstream Gaps:** Adding competitive Doubles teams for 54 newer Cobbleverse trainers (including Team Galactic commanders and Hisui NPCs) that were completely missing from the legacy snapshot.
- **Server Guardrail:** Adding a tiny, server-only companion mod to enforce a maximum of 1 Legendary or Mythical Pokémon in the player's active party.

---

## 2. Why?
The original addon is mechanically solid and loved for its difficulty, but has fallen out of sync with upstream mod changes:
- Players face broken Z-Moves, missing items, and typos on major bosses (e.g. Erika's Ogerpon mask).
- 54 newer storyline NPCs remain in vanilla Singles, creating an inconsistent gameplay experience.
- The addon currently overwrites 1,663 files unconditionally, masking useful upstream updates.

---

## 3. What Are We Deliberately NOT Building Yet?
To avoid scope creep, we are **not** building:
- Dynamic 12-mon team selection drafts.
- Adaptive AI counter-picking based on the player's party.
- Complex VGC Item Clause / Species Clause tournament enforcement engines.
- Invasive rewrites of RCT's internal battle creation or level-cap code.

---

## 4. Key Established Technical Facts
- **`GEN_9_DOUBLES` is Current:** Verified in RCT API 0.15.2 bytecode; no Gen 10 format exists.
- **Level Caps are Config-Driven:** RCT already calculates level caps based on the next required trainer + `relativeLevelCap` from `rctmod-server.toml`. Setting `relativeLevelCap = 0` achieves exact level parity without custom code.
- **Legendary Classification is Built-in:** Cobblemon 1.7.3 provides native `pokemon.isLegendary()` and `pokemon.isMythical()` methods that automatically account for all Mega, Primal, and Fusion forms.
- **Server-Only Feasibility:** Pre-battle party validation can be intercepted entirely on the server using a small Mixin into `TrainerMob.canBattleAgainst` and `replyTo`. Zero client mod installations or custom packets are required.

---

## 5. Owner Decisions Required

Before starting bulk trainer generation, the repository owner should confirm:

| Decision Area | Options | Recommended Default |
| :--- | :--- | :--- |
| **Level Cap Tuning** | `relativeLevelCap = 0` (Exact parity) vs. Negative (e.g. `-2` for severe under-leveling) | **Start with `0`**, evaluate difficulty, and tune negative later if requested. |
| **Trivial Overrides (~273 files)** | Keep all 273 files vs. Prune to let upstream route data handle minor trainers | **Prune verified trivial duplicates** to minimize maintenance debt. |
| **54 Missing Upstream Trainers** | Port full custom VGC Doubles teams vs. Quick basic Doubles conversions | **Design full VGC Doubles teams** for Cyrus/Commanders/Hisui to match Hell Mode standards. |
| **Multi-Held Items (201 cases)** | Favor primary Mega Stone/Z-Crystal vs. Favor passive item (Leftovers/Life Orb) | **Favor the primary Mega Stone or Z-Crystal** since gimmicks define boss identity. |
| **Unusual Gimmicks (Eternamax/GMax)** | Retain cosmetic aspect tags vs. Normalize to standard competitive forms | **Retain where functional in Showdown**, normalize if battle actor errors occur. |
| **Run & Bun AI Integration** | Include as optional companion vs. Require as mandatory dependency | **Keep optional/loosely coupled** until bounded testing confirms zero side-effects. |

---

## 6. Runtime Assumptions Needing Verification
1. **Showdown Execution of Multi-Item Fallback:** Confirm in-game whether Cobblemon equips slot 0 when an array is passed.
2. **RCT Sight-Trigger vs. Mixin:** Verify that `ForceIntoBattleGoal` respects the `canBattleAgainst` mixin cancellation without spamming player chat.
3. **Mega Showdown Aspect Recognition:** Verify that Mega forms triggered via `aspects: ["mega"]` function identically to held Mega Stones in Doubles.

---

## 7. Proposed Implementation Order
1. **Phase A (Now):** Repository setup, Git hygiene, README, baseline documentation.
2. **Phase B:** Automated registry audit of all held items, moves, and aspects against mod JARs.
3. **Phase C & D:** Content normalization (batch-fixing item IDs and namespaces) and pruning obsolete files.
4. **Phase E:** Porting and designing Doubles rosters for the 54 missing Cobbleverse trainers.
5. **Phase F:** Setting and validating the `relativeLevelCap = 0` progression curve.
6. **Phase G:** Building and testing the minimal server-only Legendary/Mythical restriction mod.
7. **Phase H & I:** Run & Bun evaluation and representative in-game smoke testing.
8. **Phase J:** Packaging, artifact generation, and deployment instructions.
