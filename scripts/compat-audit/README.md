# Compatibility Audit Tooling

This directory contains automated, repeatable validation tooling to audit RCT trainer JSON files against current Cobbleverse, Cobblemon, and Mega Showdown environments.

---

## Tooling Architecture

Validation is split cleanly into two layers:

1. **CI-Safe Regression Suite (`test_audit.py`)**:
   - Runs in GitHub Actions on clean Ubuntu runners.
   - Evaluates repository-owned artifacts: generated reports, JSON syntax, classification invariants, schema shapes, and pure audit functions.
   - Requires zero external binaries or local Minecraft installations.

2. **Local Full Integration Audit (`audit.py` & `test_local_integration.py`)**:
   - Runs locally on the developer machine.
   - Requires the live Cobbleverse instance (Cobblemon 1.7.3 JAR, Mega Showdown 1.8.4 JAR, RCT Mod 0.18.1-beta JAR, and Cobbleverse DP v20).
   - Extracts bytecode registries, checks item models, parses Showdown moves/abilities, and verifies report regeneration determinism.
   - **Note on Mod Binaries:** Game and mod JARs are copyrighted third-party assets and are intentionally not committed to Git.

---

## Running CI-Safe Checks (Any Environment)

To run the CI-safe unit and regression tests:
```bash
python -m unittest scripts/compat-audit/test_audit.py -v
```

To run the repository-wide data validator:
```bash
python scripts/ci/validate_repo.py
```

To verify legacy baseline immutability:
```bash
python scripts/ci/check_legacy_baseline.py
```

---

## Running Full Local Integration Audit

### 1. Execute Master Audit
```bash
# Using default local CurseForge instance
python scripts/compat-audit/audit.py

# Or pointing to a custom instance directory
python scripts/compat-audit/audit.py --instance "C:/path/to/Instances/COBBLEVERSE - Pokemon Adventure [Cobblemon]"
```

### 2. Verify Audit Determinism & Integrity
```bash
# Runs full audit against the instance and verifies byte-for-byte report stability
python -m unittest scripts/compat-audit/test_local_integration.py -v
```

---

## Generated Reports

Outputs are saved in `reports/compat-audit/`:
- `trainer-inventory.json`: Trainer ID delta (shared, missing, obsolete).
- `held-items.json`: Full canonical classification of all 214 held items.
- `species.json`: Validation of all 784 species against Cobblemon.
- `moves.json`: Validation of all 703 moves against Cobblemon/Showdown.
- `abilities.json`: Validation of all 277 abilities against Cobblemon/Showdown.
- `aspects.json`: Validation of all 114 species-aspect combinations.
- `gimmicks.json`: Verification of RCT gimmicks record usages.
- `multi-held-items.json`: Inventory of all 201 Pokémon configured with multi-item arrays.
- `current-baseline.json`: Effective baseline trainer IDs by source.
- `summary.md`: Human-readable executive audit summary answering canonical questions.
