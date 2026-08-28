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

### Phase B — Canonical Compatibility Audit
Before executing bulk modifications, run automated validation scripts against the current mod JARs:
1. **Held Items:** Cross-reference all items against `mega_showdown-1.8.4` and `Cobblemon-1.7.3` item registries.
2. **Species, Moves, Abilities:** Validate against Showdown data inside Cobblemon 1.7.3 (`data/cobblemon/showdown.zip`).
3. **Aspects & Forms:** Validate all form strings against Cobblemon `FormData` (e.g. distinguishing `cornerstone-mask` vs `cornerstone_mask`, `ice-rider` vs `ice_rider`).
4. **Gimmicks:** Validate that only `tera`, `dynamax`, and `gmax` are placed in the `gimmicks` record, removing defunct `"mega": true` attributes.
5. **Multi-Held Items:** Inventory all 201 instances of dual-item assignments for single-item resolution.

### Phase C — Trainer Reconciliation & Override Pruning
1. **Prune Obsolete Trainers:** Delete the 3 orphan IDs (`galaxy_bobbo`, `galaxy_ominorosso`, `swimmer_gengar`).
2. **Port Missing Upstream Content:** Ingest the 54 missing Cobbleverse DP v20 trainers (Team Galactic bosses Cyrus, Mars, Jupiter, Saturn, Charon, and Hisuian NPCs). Design authentic, competitive Doubles teams for them rather than leaving them in default Singles.
3. **Review Category B Overrides:** Inspect the ~273 trivial format-only overrides. If a trainer's roster is unchanged from upstream, consider retiring the override to reduce maintenance footprint.
4. **Reconcile Category D Bosses:** Deliberately review the ~101 story and gym encounters against Cobbleverse DP v20 lore.

### Phase D — Content Normalization
Automate deterministic data repairs:
- Replace hyphenated Z-Crystal IDs with underscore variants (`mega_showdown:darkinium-z` -> `mega_showdown:darkinium_z`).
- Fix missing underscores in Primal/Drive/Memory items (`blueorb` -> `blue_orb`, `redorb` -> `red_orb`, `steelmemory` -> `steel_memory`, `dousedrive` -> `douse_drive`).
- Fix namespace errors (`megas_showdown:wellspring_mask` -> `mega_showdown:wellspring_mask`, `charcoal` -> `minecraft:charcoal`, `booster_energy` -> `mega_showdown:booster_energy`).
- Flatten multi-held items into a single primary held item per Pokémon.
- Remove invalid top-level or gimmick `"mega": true` flags, relying strictly on Mega Stones or `aspects: ["mega"]`.

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
