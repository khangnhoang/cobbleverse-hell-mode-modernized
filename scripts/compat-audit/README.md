# Compatibility Audit Tooling

This directory contains automated, repeatable validation tooling to audit RCT trainer JSON files against current Cobbleverse, Cobblemon, and Mega Showdown environments.

---

## Tooling Overview

- **`audit.py`**: Master audit script. Inspects the legacy dataset (`!Doctors HELL MODE DOUBLE BATTLE EVERYTHING/`) against authoritative mod JARs and datapacks in the reference instance, generating machine-readable JSON reports and an executive summary in `reports/compat-audit/`.
- **`test_audit.py`**: Automated unit and regression test suite verifying canonical replacements, namespace handling, and deterministic report outputs.

---

## How to Run the Audit

### 1. Standard Run (Default Instance Location)
```bash
python scripts/compat-audit/audit.py
```

### 2. Custom Instance Location
You can pass the instance directory via argument or environment variable:

```bash
# As command-line argument
python scripts/compat-audit/audit.py "C:/path/to/CurseForge/Instances/COBBLEVERSE - Pokemon Adventure [Cobblemon]"

# Or via environment variable
set COBBLEVERSE_INSTANCE_PATH="C:/path/to/CurseForge/Instances/COBBLEVERSE - Pokemon Adventure [Cobblemon]"
python scripts/compat-audit/audit.py
```

---

## Running the Verification Tests

To verify that audit mappings and outputs are valid and deterministic:

```bash
python scripts/compat-audit/test_audit.py
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
