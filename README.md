# Cobbleverse Hell Mode Modernized

A community modernization project bringing the intense, world-wide Doubles battle experience of **Doctor's Hell Mode** up to date with modern **Cobbleverse** versions.

---

## Overview

The original Hell Mode created by Doctor provided a comprehensive overhaul that turned Cobbleverse trainer battles into challenging competitive Doubles encounters featuring Protect, speed control, VGC weather/terrain synergies, and competitive team compositions.

This project preserves the spirit and high-difficulty competitive Doubles design of Doctor's original work while modernizing it against current modpack standards, eliminating stale items, and fixing AI bugs via a native companion mod.

---

## Target Compatibility Baseline

| Component | Target Version |
| :--- | :--- |
| **Modpack** | COBBLEVERSE 1.7.42-CF |
| **Minecraft** | 1.21.1 (Fabric) |
| **Cobblemon** | 1.7.3 |
| **Radical Cobblemon Trainers (RCT)** | 0.18.1-beta |
| **RCT API** | 0.15.2-beta |
| **Run & Bun AI (`rbrctai`)** | 0.15.4-beta |
| **Mega Showdown** | 1.8.4 |

---

## High-Level Repository Layout

```text
├── !Doctors HELL MODE DOUBLE BATTLE EVERYTHING/ # Preserved legacy baseline (READ-ONLY)
├── .agents/                                     # Agent skills & methodology
├── companion-mod/                               # Native Fabric mod for competitive Doubles AI & rules
├── config/                                      # Configuration templates
├── datapacks/                                   # Modernized deployable datapacks
│   ├── hell-mode/                               # Main Hell Mode trainer datapack (1,714 trainers)
│   ├── legendary-encounters/                    # Legendary spawn/encounter progression
│   └── disable-master-ball/                     # Master Ball restrictions
├── docs/                                        # Centralized project documentation & SSOT
├── reports/                                     # Machine-readable audit & normalization reports
├── resource-pack/                               # Client/server resource pack assets
├── scripts/                                     # Tooling, CI validation, and runtime contracts
├── AGENTS.md                                    # Agent guidelines & engineering system contract
└── README.md                                    # Project overview & quickstart
```

For detailed architectural specifications and directory ownership, see [`docs/architecture/repository-layout.md`](docs/architecture/repository-layout.md).

---

## Quick Verification Commands

```bash
# 1. Verify legacy baseline immutability (cryptographic SHA-256 check)
python scripts/ci/check_legacy_baseline.py

# 2. Run repository data validation (datapack syntax & schemas)
python scripts/ci/validate_repo.py

# 3. Run audit regression tests
python -m unittest scripts/compat-audit/test_audit.py -v

# 4. Run offline bytecode contract checks across installed mod JARs
python scripts/runtime-contract/test_rct_runtime_contract.py

# 5. Run companion mod unit tests & build
cd companion-mod && ./gradlew test --rerun-tasks && ./gradlew build
```

---

## Documentation Navigation

All detailed technical documentation is organized under [`docs/`](docs/README.md):

- **[Project Progress](docs/progress.md)**: Current delivery status of all completed and active workstreams.
- **[Roadmap](docs/roadmap.md)**: High-level sequence of upcoming engineering initiatives.
- **[System Architecture](docs/architecture/)**: Verified architecture of the companion mod, anti-cheat AI boundary, and repository layout.
- **[Workstreams](docs/workstreams/)**: In-depth implementation plans, review briefs, and technical investigations.
- **[Operations & Testing](docs/operations/)**: Verification environments, production canary guidelines, and rollback SOPs.
- **[Agent Guidelines](AGENTS.md)**: Core engineering principles and system contracts for agentic development.

---

## Credits & Disclaimer

- **Original Concept & Overhaul:** Full credit goes to **Doctor** for creating the original *HELL MODE DOUBLE BATTLE EVERYTHING* addon and designing its vast roster of competitive Doubles teams.
- **Disclaimer:** This project is an independent community modernization effort and is **not affiliated with or endorsed by** the original Doctor addon author.
- **Licensing Notice:** The files in this repository represent derivative modifications of third-party community content. No commercial use or relicensing is claimed.
