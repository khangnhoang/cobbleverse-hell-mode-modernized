# Repository Layout & Architecture

This document describes the canonical structural layout of the `cobbleverse-hell-mode-modernized` repository and details the operational role of each component.

---

## Top-Level Repository Tree

```text
.
├── !Doctors HELL MODE DOUBLE BATTLE EVERYTHING/ # Preserved legacy baseline (READ-ONLY EXCEPTION)
├── .agents/                                     # Agent skills and methodology configurations
│   └── skills/
│       └── competitive-pokemon-doubles-team-design/
├── .github/                                     # GitHub Actions workflows and automation
│   └── workflows/
│       └── ci.yml
├── companion-mod/                               # Native Java Fabric companion mod (rct_legendary_rule)
│   ├── src/
│   │   ├── main/java/com/cobbleverse/legendaryrule/
│   │   │   ├── fair/                            # Scoped fair-information boundary components
│   │   │   ├── lead/                            # Dynamic trainer lead selection logic
│   │   │   ├── mixin/                           # Fabric bytecode mixin injectors
│   │   │   └── strategy/                        # Tactical battle AI and item-state tracking
│   │   └── test/java/                           # Automated JUnit test suite
│   ├── build.gradle
│   └── gradle.properties
├── config/                                      # Configuration templates (e.g., Cobblemon mod configs)
├── datapacks/                                   # Deployable Minecraft datapacks
│   ├── hell-mode/                               # Modernized Hell Mode trainer datapack (1,714 trainers)
│   ├── legendary-encounters/                    # Legendary Pokémon spawn and encounter datapack
│   └── disable-master-ball/                     # Master Ball crafting/spawning restriction datapack
├── docs/                                        # Centralized project documentation architecture
│   ├── README.md                                # Documentation index and SSOT routing
│   ├── roadmap.md                               # Strategic sequence and milestone boundaries
│   ├── progress.md                              # Authoritative delivery status
│   ├── architecture/                            # Durable system architecture documents
│   ├── workstreams/                             # Implementation plans, review briefs, investigations
│   ├── operations/                              # Operational SOPs, environments, and canary rules
│   └── decisions/                               # Architecture Decision Records (ADRs)
├── reports/                                     # Machine-generated verification and audit reports
│   ├── compat-audit/                            # Compatibility audit datasets (items, moves, species)
│   ├── content-normalization/                   # Phase D normalization execution metrics
│   └── trainer-reconciliation/                  # Phase C reconciliation metrics
├── resource-pack/                               # Client/server resource pack assets
│   ├── assets/
│   └── pack.mcmeta
├── scripts/                                     # Tooling, CI validation, and runtime contract suites
│   ├── ci/                                      # CI validation gates and legacy baseline checks
│   ├── compat-audit/                            # Schema auditing and compatibility test harnesses
│   ├── normalize-pack/                          # Deterministic content normalization script
│   └── runtime-contract/                        # Bytecode contract tests for Cobblemon, RCT, and R&B AI
├── .gitattributes
├── .gitignore
├── AGENTS.md                                    # Canonical agent guidelines and engineering SSOT
└── README.md                                    # Project overview, quickstart, and layout summary
```

---

## Component Roles & Boundaries

### 1. `datapacks/`
Contains all standalone deployable Minecraft datapacks.
- **`datapacks/hell-mode/`**: The core deliverable. Contains modernized trainer JSON files (1,714 active trainers) under `data/rctmod/trainers/` with valid `pack.mcmeta` (format 48).
- **`datapacks/legendary-encounters/`**: Custom spawn pools and world functions managing legendary encounter progression.
- **`datapacks/disable-master-ball/`**: Balance restriction datapack disabling master ball acquisition.

### 2. `companion-mod/`
A native Fabric mod (`rct_legendary_rule`) for Minecraft 1.21.1, built with Fabric Loom 1.7.4. It handles competitive Doubles logic that cannot be expressed purely through datapack JSON:
- Dynamic trainer lead selection via `RCTMod.makeBattle` injection.
- Strategic move valuation and throat spray item tracking.
- Doubles spread move valuation and partner Tera target reservation.
- Fair-information boundary intercepting opponent state queries during Run & Bun AI decision calculations.

### 3. `scripts/`
A comprehensive Python tooling layer supporting repository validation and safety:
- **`scripts/ci/`**: Gatekeepers for pull requests, including `validate_repo.py` and `check_legacy_baseline.py`.
- **`scripts/runtime-contract/`**: Bytecode-level inspection verifying that all classes, method descriptors, and LVT slots assumed by companion mod mixins exist identically in installed third-party mod JARs (`rctmod`, `rctapi`, `Cobblemon`, `rbrctai`).

### 4. `reports/`
Persistent, machine-readable evidence generated during major modernization phases. These reports serve as the historical baseline for validation invariant checks and must not be deleted or rewritten during routine edits.

---

## Intentional Legacy Exception: `!Doctors HELL MODE DOUBLE BATTLE EVERYTHING/`

The directory `!Doctors HELL MODE DOUBLE BATTLE EVERYTHING/` resides at the root of the repository as an **intentional, strictly read-only legacy exception**.

### Rationale
1. **Absolute Immutability**: This dataset contains 1,664 legacy trainer JSON files representing the raw upstream starting point of Doctors Hell Mode. Its entire file tree is cryptographically pinned via SHA-256 in [`scripts/ci/legacy_baseline.sha256`](../../scripts/ci/legacy_baseline.sha256).
2. **CI Checksum Integrity**: The first step of CI (`scripts/ci/check_legacy_baseline.py`) computes a deterministic SHA-256 digest of this directory on every commit to prove zero bytes have been altered or corrupted.
3. **Avoid Pathological Git Churn**: Renaming or moving 1,664 files creates severe diff noise across git history, complicates merge/rebase ancestry, and introduces risk of silent byte or line-ending mutations.
4. **Historical Report Coupling**: All accepted compatibility and audit reports in `reports/` reference `"legacy_dataset": "!Doctors HELL MODE DOUBLE BATTLE EVERYTHING"`.

Therefore, this directory is preserved at repository root and treated as immutable reference data.
