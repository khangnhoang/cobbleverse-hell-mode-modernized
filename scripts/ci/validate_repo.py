#!/usr/bin/env python3
"""
CI Repository & Data Validator for cobbleverse-hell-mode-modernized.
Validates:
- Datapack path structure and pack.mcmeta parsing.
- Complete JSON parseability across all legacy trainer files.
- Report JSON parseability, schema invariants, and classification logic.
- Ensures no ambiguous or unresolvable items claim approved auto-fixes.
- Fail-closed validation of modernized pack/ if present (preventing false-green CI).
"""

import os
import sys
import json

ALLOWED_STATUSES = {
    "VALID_EXACT",
    "INVALID_UNIQUE_CANONICAL_MATCH",
    "INVALID_AMBIGUOUS",
    "INVALID_NO_MATCH",
    "NEEDS_RUNTIME_TEST"
}

EXPECTED_REPORTS = [
    "current-baseline.json",
    "trainer-inventory.json",
    "held-items.json",
    "species.json",
    "moves.json",
    "abilities.json",
    "aspects.json",
    "gimmicks.json",
    "multi-held-items.json",
    "summary.md"
]

def log_pass(msg):
    print(f"  [PASS] {msg}")

def log_fail(msg):
    print(f"  [FAIL] {msg}")

def validate_datapack_structure(repo_root):
    print("\n--- 1. Validating Datapack Structure & Metadata ---")
    legacy_dir = os.path.join(repo_root, "!Doctors HELL MODE DOUBLE BATTLE EVERYTHING")
    mcmeta_path = os.path.join(legacy_dir, "pack.mcmeta")
    trainers_dir = os.path.join(legacy_dir, "data", "rctmod", "trainers")

    if not os.path.exists(mcmeta_path):
        log_fail(f"pack.mcmeta missing: {mcmeta_path}")
        return False

    try:
        with open(mcmeta_path, "r", encoding="utf-8") as f:
            meta = json.load(f)
        pack_sec = meta.get("pack")
        if not isinstance(pack_sec, dict) or "pack_format" not in pack_sec:
            log_fail("pack.mcmeta missing required 'pack.pack_format' object")
            return False
        log_pass(f"pack.mcmeta is valid JSON (pack_format: {pack_sec.get('pack_format')})")
    except Exception as e:
        log_fail(f"Failed to parse pack.mcmeta: {e}")
        return False

    if not os.path.isdir(trainers_dir):
        log_fail(f"Trainers directory missing: {trainers_dir}")
        return False
    log_pass(f"Legacy trainers directory found: {os.path.relpath(trainers_dir, repo_root)}")
    return True

def validate_legacy_trainers(repo_root):
    print("\n--- 2. Validating Legacy Trainer JSON Syntax & Schema ---")
    trainers_dir = os.path.join(repo_root, "!Doctors HELL MODE DOUBLE BATTLE EVERYTHING", "data", "rctmod", "trainers")
    count = 0
    errors = []

    for root, dirs, files in os.walk(trainers_dir):
        for f in files:
            if f.endswith(".json"):
                count += 1
                fp = os.path.join(root, f)
                try:
                    with open(fp, "r", encoding="utf-8") as jf:
                        d = json.load(jf)
                    if not isinstance(d, dict):
                        errors.append(f"{f}: root is not a JSON object")
                    elif "team" not in d or not isinstance(d["team"], list):
                        errors.append(f"{f}: missing or invalid 'team' array")
                except Exception as e:
                    errors.append(f"{f}: JSON parse error: {e}")

    if errors:
        for err in errors[:10]:
            log_fail(err)
        if len(errors) > 10:
            log_fail(f"... and {len(errors) - 10} more errors")
        return False

    log_pass(f"All {count} legacy trainer JSON files parsed successfully without errors.")
    return True

def validate_reports(repo_root):
    print("\n--- 3. Validating Audit Reports & Classification Invariants ---")
    reports_dir = os.path.join(repo_root, "reports", "compat-audit")
    if not os.path.isdir(reports_dir):
        log_fail(f"Reports directory missing: {reports_dir}")
        return False

    # Check existence
    missing = [r for r in EXPECTED_REPORTS if not os.path.exists(os.path.join(reports_dir, r))]
    if missing:
        log_fail(f"Missing expected reports: {missing}")
        return False
    log_pass(f"All {len(EXPECTED_REPORTS)} expected audit report files exist.")

    # Validate held-items.json
    try:
        with open(os.path.join(reports_dir, "held-items.json"), "r", encoding="utf-8") as f:
            hi_data = json.load(f)
        summary = hi_data.get("summary", {})
        items = hi_data.get("items", {})
        if not items or not summary:
            log_fail("held-items.json missing 'items' or 'summary'")
            return False

        for raw, info in items.items():
            st = info.get("status")
            if st not in ALLOWED_STATUSES:
                log_fail(f"held-items.json: item '{raw}' has unrecognized status '{st}'")
                return False
            canon = info.get("canonical_replacement")
            if st in ("VALID_EXACT", "INVALID_UNIQUE_CANONICAL_MATCH"):
                if not canon or not isinstance(canon, str) or ":" not in canon:
                    log_fail(f"held-items.json: item '{raw}' marked {st} but has invalid canonical_replacement '{canon}'")
                    return False
            elif st in ("INVALID_AMBIGUOUS", "INVALID_NO_MATCH"):
                if canon is not None:
                    log_fail(f"held-items.json: item '{raw}' marked {st} must have canonical_replacement=None, got '{canon}'")
                    return False

        log_pass(f"held-items.json invariant check passed ({len(items)} items verified)")
    except Exception as e:
        log_fail(f"Error parsing held-items.json: {e}")
        return False

    # Validate aspects.json
    try:
        with open(os.path.join(reports_dir, "aspects.json"), "r", encoding="utf-8") as f:
            asp_data = json.load(f)
        combos = asp_data.get("aspect_combinations", {})
        for key, info in combos.items():
            st = info.get("status")
            if st not in ALLOWED_STATUSES:
                log_fail(f"aspects.json: combo '{key}' has unrecognized status '{st}'")
                return False
            canon = info.get("canonical_replacement")
            if st in ("INVALID_NO_MATCH", "INVALID_AMBIGUOUS") and canon is not None:
                log_fail(f"aspects.json: combo '{key}' marked {st} must have canonical_replacement=None, got '{canon}'")
                return False
        log_pass(f"aspects.json invariant check passed ({len(combos)} combinations verified)")
    except Exception as e:
        log_fail(f"Error parsing aspects.json: {e}")
        return False

    # Validate gimmicks.json
    try:
        with open(os.path.join(reports_dir, "gimmicks.json"), "r", encoding="utf-8") as f:
            gim_data = json.load(f)
        inv_usages = gim_data.get("invalid_gimmick_usages", [])
        for u in inv_usages:
            if u.get("invalid_key") not in ("mega",):
                log_fail(f"gimmicks.json: unexpected invalid gimmick key '{u.get('invalid_key')}'")
                return False
        log_pass(f"gimmicks.json invariant check passed ({len(inv_usages)} invalid usages verified)")
    except Exception as e:
        log_fail(f"Error parsing gimmicks.json: {e}")
        return False

    # Validate trainer-inventory.json
    try:
        with open(os.path.join(reports_dir, "trainer-inventory.json"), "r", encoding="utf-8") as f:
            ti_data = json.load(f)
        ti_sum = ti_data.get("summary", {})
        shared = ti_sum.get("shared_trainer_ids_count", 0)
        missing_cnt = ti_sum.get("missing_from_hell_count", 0)
        total_baseline = ti_sum.get("total_effective_baseline_trainers", 0)
        if shared + missing_cnt != total_baseline:
            log_fail(f"trainer-inventory.json sum mismatch: shared({shared}) + missing({missing_cnt}) != baseline({total_baseline})")
            return False
        log_pass(f"trainer-inventory.json invariant check passed ({total_baseline} baseline trainers verified)")
    except Exception as e:
        log_fail(f"Error parsing trainer-inventory.json: {e}")
        return False

    return True

def validate_future_pack(repo_root):
    print("\n--- 4. Modernized Pack Validation (Phase C/D Preparation) ---")
    pack_dir = os.path.join(repo_root, "pack")
    if not os.path.exists(pack_dir):
        log_pass("Modernized pack directory ('pack/') not present yet; skipping Phase C/D checks cleanly.")
        return True

    log_pass("Modernized pack directory ('pack/') detected; executing fail-closed structure & syntax validation...")

    # 1. pack.mcmeta validation
    mcmeta_path = os.path.join(pack_dir, "pack.mcmeta")
    if not os.path.exists(mcmeta_path):
        log_fail(f"Modernized pack missing required pack.mcmeta at: {mcmeta_path}")
        return False

    try:
        with open(mcmeta_path, "r", encoding="utf-8") as f:
            meta = json.load(f)
        pack_sec = meta.get("pack")
        if not isinstance(pack_sec, dict) or "pack_format" not in pack_sec or "description" not in pack_sec:
            log_fail("pack/pack.mcmeta must contain 'pack' object with 'pack_format' and 'description'")
            return False
        log_pass(f"pack/pack.mcmeta valid (pack_format: {pack_sec.get('pack_format')})")
    except Exception as e:
        log_fail(f"Failed to parse pack/pack.mcmeta as JSON: {e}")
        return False

    # 2. trainers directory validation
    trainers_dir = os.path.join(pack_dir, "data", "rctmod", "trainers")
    if not os.path.isdir(trainers_dir):
        log_fail(f"Modernized pack missing trainers directory at: {trainers_dir}")
        return False

    # 3. trainer JSON syntax & structure validation
    count = 0
    errors = []
    for root, dirs, files in os.walk(trainers_dir):
        for f in files:
            if f.endswith(".json"):
                count += 1
                fp = os.path.join(root, f)
                try:
                    with open(fp, "r", encoding="utf-8") as jf:
                        d = json.load(jf)
                    if not isinstance(d, dict):
                        errors.append(f"{f}: root is not a JSON object")
                    elif "team" not in d or not isinstance(d["team"], list):
                        errors.append(f"{f}: missing or invalid 'team' array")
                    elif len(d["team"]) == 0:
                        errors.append(f"{f}: 'team' array must contain at least 1 Pokemon")
                except Exception as e:
                    errors.append(f"{f}: JSON parse error: {e}")

    if count == 0:
        log_fail("Modernized pack directory contains zero trainer JSON files")
        return False

    if errors:
        for err in errors[:10]:
            log_fail(f"pack trainer error: {err}")
        if len(errors) > 10:
            log_fail(f"... and {len(errors) - 10} more trainer errors")
        return False

    log_pass(f"All {count} modernized trainer JSON files parsed and verified successfully.")
    return True

def main():
    script_dir = os.path.dirname(os.path.abspath(__file__))
    repo_root = os.path.abspath(os.path.join(script_dir, "..", ".."))

    print("=" * 70)
    print("COBBLEVERSE HELL MODE REPOSITORY & DATA VALIDATION")
    print("=" * 70)
    print(f"Repository Root: {repo_root}")

    success = True
    if not validate_datapack_structure(repo_root):
        success = False
    if not validate_legacy_trainers(repo_root):
        success = False
    if not validate_reports(repo_root):
        success = False
    if not validate_future_pack(repo_root):
        success = False

    print("\n" + "=" * 70)
    if success:
        print("RESULT: ALL REPOSITORY & DATA VALIDATIONS PASSED")
        print("=" * 70)
        sys.exit(0)
    else:
        print("RESULT: REPOSITORY VALIDATION ENCOUNTERED FAILURES")
        print("=" * 70)
        sys.exit(1)

if __name__ == "__main__":
    main()
