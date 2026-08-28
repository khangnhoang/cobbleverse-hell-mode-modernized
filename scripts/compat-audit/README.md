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
   - Requires the external installed Cobbleverse instance (Cobblemon 1.7.3 JAR, Mega Showdown 1.8.4 JAR, RCT Mod 0.18.1-beta JAR, and Cobbleverse DP v20).
   - **Authoritative Item Registries:**
     - Mega Showdown: Bytecode extraction of registered `RegistrySupplier` item fields directly from `MegaShowdownItems.class` constant pool.
     - Cobblemon: Authoritative item definitions from `assets/cobblemon/lang/en_us.json` cross-referenced with item models.
     - Vanilla Minecraft: Validated standard Minecraft 1.21.1 items used as held items (`minecraft:gold_nugget`, `minecraft:charcoal`). Note that vanilla runtime registries reside in Minecraft's version JAR outside the modpack instance directory.
   - **Ambiguity-Safe Aspect Matching:** Evaluates all candidate FormData aspects in Cobblemon; fuzzy matches with multiple candidates strictly produce `INVALID_AMBIGUOUS` with `None` rather than arbitrarily selecting the first candidate.
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
Provide the path to the installed Cobbleverse instance via CLI argument or environment variable:

```bash
# Via command-line argument:
python scripts/compat-audit/audit.py --instance "C:/path/to/Cobbleverse/Instance"

# Or via environment variable:
set COBBLEVERSE_INSTANCE_PATH="C:/path/to/Cobbleverse/Instance"
python scripts/compat-audit/audit.py
```

If neither is supplied, `audit.py` fails with clear instructions.

### 2. Verify Audit Determinism & Integrity
```bash
# Set instance path in environment
set COBBLEVERSE_INSTANCE_PATH="C:/path/to/Cobbleverse/Instance"

# Run integration tests (checks binary presence and verifies byte-for-byte report determinism)
python -m unittest scripts/compat-audit/test_local_integration.py -v
```
*(If `COBBLEVERSE_INSTANCE_PATH` is not set, this integration test skips cleanly with an informative message).*

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
