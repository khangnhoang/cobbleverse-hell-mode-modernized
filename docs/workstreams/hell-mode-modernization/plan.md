# Hell Mode Modernization Plan

## Goal

The objective of this project is to modernize **Doctor's Hell Mode** for the current Cobbleverse modpack ecosystem:
- **Preserve:**
  - The distinctive Doctor-style intense world-wide Doubles battle experience.
  - The competitive team compositions, doubles synergies, and challenging boss designs.
- **Eliminate:**
  - Legacy snapshot drift and desynchronization from active Cobbleverse content.
  - Broken held-item identifiers, malformed namespaces, and hyphenation bugs.
  - Obsolete/orphan trainer definitions removed upstream.
  - Unnecessary overriding of upstream trainers that masks current modpack updates.
  - Fragile, manual one-by-one hotfixing in favor of automated validation and structured migration.

---

## Current Baseline

The project is pinned against the following verified environment:

| Component | Version / Identifier | Source Location |
| :--- | :--- | :--- |
| **Modpack** | COBBLEVERSE 1.7.42-CF | Minecraft CurseForge Instance |
| **Minecraft** | 1.21.1 (Fabric) | Instance root |
| **Cobblemon** | 1.7.3 | `mods/Cobblemon-fabric-1.7.3+1.21.1.jar` |
| **Radical Cobblemon Trainers (RCT)** | 0.18.1-beta | `mods/rctmod-fabric-1.21.1-0.18.1-beta.jar` |
| **RCT API** | 0.15.2-beta | `mods/rctapi-fabric-1.21.1-0.15.2-beta.jar` |
| **Mega Showdown** | 1.8.4 | `mods/mega_showdown-fabric-1.8.4+1.7.3+1.21.1.jar` |

### Upstream Effective Trainer Inventory:
1. **Primary Datapack:** `datapacks/COBBLEVERSE-RCT-DP-v20.zip` (155 trainer JSONs defining all 52 Gyms, Elite Four, Champions, and major story bosses).
2. **Base Mod Data:** `mods/rctmod-fabric-1.21.1-0.18.1-beta.jar` (1,559 trainer JSONs defining open-world route trainers).
- **Total Upstream Effective Baseline:** ~1,714 unique trainer IDs.

### Legacy Hell Mode Addon Snapshot:
- **Total Files:** 1,663 trainer JSONs.
- **Shared with Baseline:** 1,660 IDs.
- **Missing from Hell Mode (Present in 1.7.42):** 54 IDs (e.g. Team Galactic Cyrus, Mars, Jupiter, Saturn, Charon, Hisuian trainers).
- **Obsolete in Hell Mode (Removed Upstream):** 3 IDs (`galaxy_bobbo`, `galaxy_ominorosso`, `swimmer_gengar`).

---

## Decisions Already Established

Based on empirical decompilation and instance auditing:
1. **Sole Base:** Doctor's Hell Mode is the sole foundation. `Doctors Tougher Cobbleverse Gyms` has been decommissioned and is strictly excluded from this project.
2. **Curated Scope:** The project will not blindly preserve the entire frozen 1,663-file snapshot. Overrides will be curated, pruning zero-diff or trivial overrides to allow upstream route balancing to function naturally.
3. **Battle Format:** `GEN_9_DOUBLES` is verified as 100% valid and supported in RCT API 0.15.2 bytecode (`BattleFormat.class`). No `GEN_10_*` identifier exists.
4. **Level-Cap Engine:** Native RCT configuration (`config/rctmod-server.toml`) fully owns level caps. Initial modernization target is `relativeLevelCap = 0` (player level cap equals the next required trainer's maximum level).
5. **Party Restriction Choke Point:** Enforcing a maximum of 1 Legendary/Mythical Pokémon will be implemented via a lightweight server-only Fabric companion mod hooking `TrainerMob.canBattleAgainst` and `replyTo`.
6. **No Generalized Engine in This Pass:** Complex custom tournament rule engines (species clause, item clause, dynamic roster drafts) are deferred.

---

## Repository & Project Layout

```
cobbleverse-hell-mode-modernized/
├── .gitattributes                          # Enforced LF line endings
├── .gitignore                              # Build and runtime ignores
├── README.md                               # Public-facing documentation
├── implementation-plans/                   # Planning and technical architecture
│   └── hell-mode-modernization/
│       ├── plan.md                         # This master plan
│       └── owner-review-brief.md           # Executive decision brief
├── !Doctors HELL MODE DOUBLE BATTLE EVERYTHING/ # Preserved legacy baseline reference
├── pack/                                   # Modernized deployable datapack (Phase C/D)
│   ├── pack.mcmeta
│   └── data/
│       └── rctmod/
│           └── trainers/                   # Curated, validated trainer JSONs
└── companion-mod/                          # Future server companion Fabric mod (Phase G)
    ├── build.gradle
    ├── src/main/java/...
    └── src/main/resources/fabric.mod.json
```

*Note: The installed CurseForge Cobbleverse instance is strictly external and read-only; no game files or instance binaries are tracked in Git.*

---

## Modernization Phases

### Phase A — Repository Baseline & Hygiene
- Establish `.gitattributes` to prevent CRLF/LF churn across Windows and Linux environments.
- Establish `.gitignore` covering Gradle, build artifacts, IDE metadata, and Minecraft runtime folders.
- Write public-facing `README.md` clearly crediting Doctor and setting work-in-progress expectations.
- Maintain immutable reference to the imported legacy baseline commit.

### Phase B — Canonical Compatibility Audit (Completed Tooling & Evidence)
Automated validation tooling has been established in `scripts/compat-audit/` and verified against reference mod JARs:
- **Master Audit Runner:** `python scripts/compat-audit/audit.py`
- **Regression Test Suite:** `python scripts/compat-audit/test_audit.py`
- **Generated Reports:** Stored in `reports/compat-audit/` covering held items, species, moves, abilities, aspects, gimmicks, multi-held items, and trainer inventories.
- **Audit Findings Summary:**
  1. **Held Items:** 181 valid; 33 invalid with deterministic replacements (22 hyphenated Z-Crystals, 6 missing underscores, 4 bare namespace omissions, 1 typo); 0 ambiguous.
  2. **Species:** All 784 species verified 100% valid against Cobblemon 1.7.3.
  3. **Moves & Abilities:** 681 valid moves, 21 truncated typos (safe matches), 1 unsupported shadow move (`shadowblitz`); 275 valid abilities, 2 truncated typos (`magic` -> `magicbounce`, `shield` -> `shielddust`).
  4. **Aspects:** 79 exact matches; 20 syntax discrepancies with safe matches; 11 unsupported Radical Red forms (`sevii`); 4 cosmetics needing runtime check.
  5. **Gimmicks:** Only 2 invalid usages of `"mega": true` inside the `gimmicks` record (`team_rocket_admin_apollo` and `team_rocket_giovanni`).
  6. **Multi-Held Items:** 201 cases inventoried (200 clearly intending Mega/Z-Crystal priority; 1 requiring design review).

### Phase C — Trainer Reconciliation & Modernized Pack Baseline
1. **Create Modernized Pack Baseline:** Establish `pack/` with valid `pack.mcmeta` and `data/rctmod/trainers/` structure without altering the preserved legacy baseline.
2. **Prune Obsolete Trainers:** Exclude the 3 orphan IDs (`galaxy_bobbo`, `galaxy_ominorosso`, `swimmer_gengar`) from `pack/`.
3. **Port Missing Upstream Content:** Ingest the 54 missing Cobbleverse DP v20 trainers (Team Galactic bosses Cyrus, Mars, Jupiter, Saturn, Charon, and Hisuian NPCs) with authentic upstream definitions preserved, queueing competitive Doubles redesign for Phase E.
4. **Evaluate Override Pruning:** Inspect the shared legacy overrides. Conservative pruning policy: retain overrides containing deliberate AI bias weights (`moveBias`, `switchBias`, `statMoveBias`, `itemBias`) or Doubles formatting to prevent gameplay degradation to vanilla RCT.
5. **Reconcile Boss & Story Overrides:** Verified all 101 overlapping Cobbleverse DP v20 boss/story definitions retain necessary schema/identity fields.
6. **Provenance & Reporting:** Generate machine-readable inventory reconciliation report and summary under `reports/trainer-reconciliation/`.

### Phase D — Content Normalization
Automate deterministic data repairs:
- Replace hyphenated Z-Crystal IDs with underscore variants (`mega_showdown:darkinium-z` -> `mega_showdown:darkinium_z`).
- Fix missing underscores in Primal/Drive/Memory items (`blueorb` -> `blue_orb`, `redorb` -> `red_orb`, `steelmemory` -> `steel_memory`, `dousedrive` -> `douse_drive`).
- Fix namespace errors (`megas_showdown:wellspring_mask` -> `mega_showdown:wellspring_mask`, `charcoal` -> `minecraft:charcoal`, `booster_energy` -> `mega_showdown:booster_energy`).
- Canonicalize identifiers inside multi-held item arrays while strictly preserving array structure and element order (destructive flattening is explicitly deferred to runtime/design verification).
- Remove confirmed invalid `"mega": true` keys from the `gimmicks` record (relying on Mega Stones or `aspects: ["mega"]` without altering valid `dynamax`/`gmax`/`tera` gimmicks).

### Phase E — Doubles Team Modernization
Ensure all retained and newly created teams adhere to competitive Doubles design standards:
- **Speed Control:** Balanced distribution of Tailwind, Trick Room, Icy Wind, and Electroweb.
- **Protection & Positioning:** Deliberate use of Protect, Detect, Wide Guard, and redirection (Follow Me, Rage Powder).
- **Spread Coverage:** Effective spread moves (Rock Slide, Heat Wave, Earthquake with Flying/Telepathy partners).
- **Synergies:** Functional weather (Rain, Sun, Sand, Snow) and terrain compositions.
- **Fair Stat Spread:** Legitimate IVs/EVs and competitive held items rather than uncounterable artificial stat boosts.

### Phase F — Level-Cap Alignment
- Pinned to native RCT mechanics (`LevelUtils.levelCap`).
- Set `relativeLevelCap = 0` in server configuration so player caps match the maximum level of the opponent's team.
- Validate progression across Kanto, Johto, Hoenn, and Sinnoh series graphs to prevent unintended level gating.

### Phase G — Server Companion Rule (Max 1 Legendary/Mythical)
Develop a minimal Fabric companion mod:
- **Injection Points:**
  - `TrainerMob.canBattleAgainst`: Check if `countLegendaries(player.party) > 1`; if so, return `false`.
  - `TrainerMob.replyTo`: Deliver formatted chat rejection (`§cTrainer rules permit at most 1 Legendary or Mythical Pokémon!`).
- **Detection API:** Use Cobblemon's built-in `Pokemon.isLegendary()` and `Pokemon.isMythical()` methods (which automatically evaluate form labels for Mega, Primal, and Fusion forms).
- **Architecture:** 100% server-side. Zero client packets, zero custom GUI, zero client mod requirements.

### Phase H — Run & Bun AI Compatibility Verification
- Conduct bounded testing of RCT's Showdown battle AI against the custom heuristics in Run & Bun.
- Verify whether Run & Bun affects only move-selection logic or imposes schema/admission constraints.
- Maintain loose coupling so AI packages can be swapped without rewriting trainer JSONs.

### Phase I — Runtime Mechanics Verification Matrix
Execute smoke tests against:
1. Standard route trainer (Doubles format, AI turn execution).
2. Major Gym Leader (Flat level curve, Tera activation, Protect usage).
3. Ported Team Galactic boss (Cyrus in Doubles).
4. Corrected item resolution (Z-Moves, Primal Reversion, Ogerpon masks).
5. Special aspect rendering (Mega Evolutions, Ultra Burst, Eternamax).
6. Level-cap rejection dialog at `relativeLevelCap = 0`.
7. Server companion rejection on parties with $\ge 2$ Legendaries/Mythicals.

### Phase J — Deployment & Release Management
- Package the modernized datapack into a versioned `.zip` artifact.
- Compile the server-only companion mod `.jar`.
- Provide automated installation documentation for dedicated servers.
- Establish clean rollback tags in Git.

---

## Open Questions & Verification Gates

1. **Category B Override Policy:** Confirm whether the ~273 trivial overrides should be deleted (falling back cleanly to upstream RCT) or kept to guarantee uniform AI bias.
2. **Dual-Item Resolution:** Confirm priority when resolving legacy dual-held items (e.g. Mega Stone + Leftovers -> favor Mega Stone).
3. **Run & Bun Integration:** Determine if Run & Bun AI is intended as a mandatory dependency or an optional enhancement.

---

## Out of Scope (Deferred)

The following advanced competitive systems are explicitly deferred to future milestones:
- 12-Pokémon pool dynamic roster selection before battle.
- Adaptive counter-picking AI based on player team composition.
- Full VGC Item Clause and Species Clause enforcement.
- Generic dynamic tournament rulesets.

---

## Completion Criteria

- [ ] Repository has clean Git hygiene with enforced LF line endings and proper ignores.
- [ ] 100% of held items, forms, aspects, and gimmick tags pass automated registry validation.
- [ ] 54 missing Cobbleverse DP v20 trainers are ported and equipped with competitive Doubles teams.
- [ ] 3 obsolete upstream trainers are removed.
- [ ] All 52 Gym and League encounters are verified in `GEN_9_DOUBLES`.
- [ ] Server companion mod enforces `maxLegendaryMythical = 1` without client-side requirements.
- [ ] Deployment artifacts can be installed on a dedicated server with zero client crashes.
