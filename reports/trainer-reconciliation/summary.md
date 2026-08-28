# Phase C — Trainer Reconciliation Report

**Target Modpack:** COBBLEVERSE 1.7.42-CF (Minecraft 1.21.1 Fabric)  
**Cobblemon:** 1.7.3 | **RCT Mod:** 0.18.1-beta | **RCT API:** 0.15.2-beta  
**Legacy Addon:** `!Doctors HELL MODE DOUBLE BATTLE EVERYTHING`  
**Modernized Datapack Location:** `pack/`  

---

## 1. Inventory Summary

| Metric | Count | Details |
| :--- | :--- | :--- |
| **Legacy Hell Mode Trainers** | 1,663 | Preserved read-only in baseline directory |
| **Obsolete Orphan Trainers Removed** | 3 | `galaxy_bobbo.json`, `galaxy_ominorosso.json`, `swimmer_gengar.json` |
| **Upstream Missing Trainers Ingested** | 54 | Team Galactic bosses/grunts and Hisui NPCs from Cobbleverse DP v20 |
| **Shared Legacy Overrides Evaluated** | 1,660 | Bounded comparison against current Cobbleverse baseline |
| **Overrides Safely Pruned** | 0 | 0 files were identical; all 1,660 retain deliberate AI / Doubles gameplay |
| **Overrides Conservatively Retained** | 1,660 | Retained in full to preserve Doubles format, AI tuning, and teams |
| **Resulting `pack/` Trainer Count** | **1,714** | Matches upstream effective baseline (1,559 JAR + 155 DP) exactly |
| **Trainers Pending Phase E Redesign** | 54 | Ingested in upstream state; queued for competitive Doubles design |

---

## 2. Inventory Changes

### Obsolete Trainers Removed (3)
These trainer files existed in legacy Hell Mode but do not exist anywhere in current Cobbleverse (neither in RCT mod JAR nor Cobbleverse DP v20):
1. `galaxy_bobbo.json`
2. `galaxy_ominorosso.json`
3. `swimmer_gengar.json`

### Upstream Missing Trainers Ingested (54)
These trainer files exist in `COBBLEVERSE-RCT-DP-v20.zip` but were absent from legacy Hell Mode. In Phase C, they are ingested with their authentic upstream fields and semantics intact:
- **Major Bosses:** `team_galactic_cyrus.json`, `team_galactic_commander_mars.json`, `team_galactic_commander_jupiter.json`, `team_galactic_commander_saturn.json`, `team_galactic_charon.json`
- **Story / NPC Trainers:** `hisui_damon.json`, `hisui_perula.json`, `hoenn_pat.json`, `sinnoh_buck.json`, `sinnoh_elfio.json`
- **Team Galactic Personnel:** 44 grunts, scientists, engineers, recruits, secretaries, researchers, supervisor, apiarist, garbageman, and director.

These 54 trainers currently retain their upstream Singles definitions and are explicitly cataloged as **Phase E pending work** for competitive Doubles modernization.

---

## 3. Override Pruning Policy Evaluation
- **Candidate Evaluation:** All 1,660 shared trainer overrides were compared against the effective upstream baseline.
- **Criteria:** An override was only safely removable if it was 100% semantically equivalent to upstream, losing no intended Hell Mode behavior.
- **Result:** **0 overrides were pruned; 1,660 were conservatively retained.** Every legacy override includes custom AI bias data (`moveBias: 1`, `switchBias: 0.7`, `statMoveBias: 1`, `itemBias: 0.8`), `GEN_9_DOUBLES` format, custom movesets, EVs, IVs, or held items. Removing any override would silently revert the trainer to vanilla RCT behavior.

---

## 4. Cobbleverse Boss / Story Overrides Reconciliation
- **Evaluated Overlap:** 101 legacy Hell trainers overlap current Cobbleverse-custom boss and gym leader definitions in DP v20.
- **Schema & Metadata Analysis:** Compared non-team fields across all 101 files. Exactly **0 upstream schema keys** were missing in legacy Hell Mode.
- **Metadata Integrity:** Legacy files preserve Italian `identity` tags (e.g. `"identity": "Rocco"`, `"identity": "Adriano"`) required for Cobbleverse NPC spawning and story progression, while providing English display names (`"name": {"literal": "Steven"}`), `GEN_9_DOUBLES` battle format, and item use caps (`maxItemUses: 2`). No metadata reconciliation was required.

---

## 5. Next Steps
- **Phase D (Content Normalization):** Perform deterministic automated repairs on the 1,714 files in `pack/` (held items, move/ability typos, aspects/forms, gimmick cleanup).
- **Phase E (Doubles Modernization):** Redesign the 54 newly added trainers for competitive Doubles and balance high-difficulty encounters.
