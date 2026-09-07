#!/usr/bin/env python3
"""
CI Repository & Data Validator for cobbleverse-hell-mode-modernized.
Validates:
- Datapack path structure and pack.mcmeta parsing.
- Complete JSON parseability across all legacy trainer files.
- Report JSON parseability, schema invariants, and classification logic.
- Ensures no ambiguous or unresolvable items claim approved auto-fixes.
- Fail-closed validation of modernized datapacks/hell-mode/ if present (preventing false-green CI).
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

VALID_TYPES = {
    "normal", "fire", "water", "grass", "electric", "ice",
    "fighting", "poison", "ground", "flying", "psychic", "bug",
    "rock", "ghost", "dragon", "steel", "dark", "fairy"
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
    pack_dir = os.path.join(repo_root, "datapacks", "hell-mode")
    if not os.path.exists(pack_dir):
        log_pass("Modernized pack directory ('datapacks/hell-mode/') not present yet; skipping Phase C/D checks cleanly.")
        return True

    log_pass("Modernized pack directory ('datapacks/hell-mode/') detected; executing fail-closed structure & syntax validation...")

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
            log_fail("datapacks/hell-mode/pack.mcmeta must contain 'pack' object with 'pack_format' and 'description'")
            return False
        log_pass(f"datapacks/hell-mode/pack.mcmeta valid (pack_format: {pack_sec.get('pack_format')})")
    except Exception as e:
        log_fail(f"Failed to parse datapacks/hell-mode/pack.mcmeta as JSON: {e}")
        return False

    # 2. trainers directory validation
    trainers_dir = os.path.join(pack_dir, "data", "rctmod", "trainers")
    if not os.path.isdir(trainers_dir):
        log_fail(f"Modernized pack missing trainers directory at: {trainers_dir}")
        return False

    # 3. Phase D semantic rules from accepted compatibility reports
    compat_reports_dir = os.path.join(repo_root, "reports", "compat-audit")
    forbidden_items = set()
    forbidden_moves = set()
    forbidden_abilities = set()
    forbidden_aspects = set()

    if os.path.isdir(compat_reports_dir):
        hi_path = os.path.join(compat_reports_dir, "held-items.json")
        if os.path.exists(hi_path):
            with open(hi_path, "r", encoding="utf-8") as f:
                forbidden_items = {k for k, v in json.load(f).get("items", {}).items() if v.get("status") == "INVALID_UNIQUE_CANONICAL_MATCH"}

        mv_path = os.path.join(compat_reports_dir, "moves.json")
        if os.path.exists(mv_path):
            with open(mv_path, "r", encoding="utf-8") as f:
                forbidden_moves = {k for k, v in json.load(f).get("moves", {}).items() if v.get("status") == "INVALID_UNIQUE_CANONICAL_MATCH"}
        forbidden_moves.add("x_scissor")
        forbidden_moves.add("shadowblitz")

        ab_path = os.path.join(compat_reports_dir, "abilities.json")
        if os.path.exists(ab_path):
            with open(ab_path, "r", encoding="utf-8") as f:
                forbidden_abilities = {k for k, v in json.load(f).get("abilities", {}).items() if v.get("status") == "INVALID_UNIQUE_CANONICAL_MATCH"}

        asp_path = os.path.join(compat_reports_dir, "aspects.json")
        if os.path.exists(asp_path):
            with open(asp_path, "r", encoding="utf-8") as f:
                forbidden_aspects = {k.lower() for k, v in json.load(f).get("aspect_combinations", {}).items() if v.get("status") == "INVALID_UNIQUE_CANONICAL_MATCH"}

        sp_path = os.path.join(compat_reports_dir, "species.json")
        valid_species = set()
        if os.path.exists(sp_path):
            with open(sp_path, "r", encoding="utf-8") as f:
                valid_species = {k.lower() for k in json.load(f).get("species", {}).keys()}

    # 4. trainer JSON syntax, structure & semantic validation
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
                    else:
                        for b in d.get("bag", []):
                            if "revive" in b.get("item", "").lower():
                                errors.append(f"{f}: contains unsupported revive item '{b.get('item')}' in bag")

                        for p in d.get("team", []):
                            sp = p.get("species", "").lower()
                            hi = p.get("heldItem")
                            if isinstance(hi, list):
                                for it in hi:
                                    if it in forbidden_items:
                                        errors.append(f"{f}: contains unnormalized heldItem '{it}'")
                            elif isinstance(hi, str) and hi in forbidden_items:
                                errors.append(f"{f}: contains unnormalized heldItem '{hi}'")

                            for m in p.get("moveset", []):
                                if m in forbidden_moves:
                                    errors.append(f"{f}: contains unnormalized move '{m}'")

                            if p.get("ability") in forbidden_abilities:
                                errors.append(f"{f}: contains unnormalized ability '{p.get('ability')}'")

                            for a in p.get("aspects", []):
                                if f"{sp}::{a.lower()}" in forbidden_aspects:
                                    errors.append(f"{f}: contains unnormalized aspect '{a}' on species '{sp}'")

                            if isinstance(p.get("gimmicks"), dict) and "mega" in p["gimmicks"]:
                                errors.append(f"{f}: contains invalid 'mega' key in 'gimmicks' record")

                        if "leadPresets" in d:
                            presets = d["leadPresets"]
                            if not isinstance(presets, list):
                                errors.append(f"{f}: 'leadPresets' must be a list")
                            else:
                                team_len = len(d["team"])
                                for p_idx, preset in enumerate(presets):
                                    if not isinstance(preset, dict):
                                        errors.append(f"{f}: leadPresets[{p_idx}] must be a dict")
                                        continue
                                    pid = preset.get("id")
                                    if not pid or not isinstance(pid, str) or not pid.strip():
                                        errors.append(f"{f}: leadPresets[{p_idx}] missing valid 'id'")

                                    slots = preset.get("leadSlots")
                                    if not isinstance(slots, list) or len(slots) != 2 or not all(isinstance(x, int) and not isinstance(x, bool) for x in slots):
                                        errors.append(f"{f}: preset '{pid}' leadSlots must be a list of 2 integers")
                                        continue

                                    if slots[0] < 0 or slots[0] >= team_len or slots[1] < 0 or slots[1] >= team_len:
                                        errors.append(f"{f}: preset '{pid}' leadSlots [{slots[0]}, {slots[1]}] out of bounds for team of size {team_len}")
                                    if slots[0] == slots[1]:
                                        errors.append(f"{f}: preset '{pid}' leadSlots must be distinct, got {slots}")

                                    if "baseWeight" in preset:
                                        bw = preset["baseWeight"]
                                        if not isinstance(bw, int) or isinstance(bw, bool) or bw < -2 or bw > 2:
                                            errors.append(f"{f}: preset '{pid}' baseWeight must be an integer in [-2, 2], got {bw}")

                                    if "favoredAgainst" in preset:
                                        fa = preset["favoredAgainst"]
                                        if not isinstance(fa, list):
                                            errors.append(f"{f}: preset '{pid}' favoredAgainst must be a list")
                                        else:
                                            for t in fa:
                                                if not isinstance(t, str) or t.strip().lower() not in VALID_TYPES:
                                                    errors.append(f"{f}: preset '{pid}' favoredAgainst contains invalid type '{t}'")

                                    if "favoredAgainstSpecies" in preset:
                                        fas = preset["favoredAgainstSpecies"]
                                        if not isinstance(fas, list):
                                            errors.append(f"{f}: preset '{pid}' favoredAgainstSpecies must be a list")
                                        else:
                                            for sp_item in fas:
                                                if not isinstance(sp_item, str) or not sp_item.strip():
                                                    errors.append(f"{f}: preset '{pid}' favoredAgainstSpecies contains empty or non-string entry")
                                                elif valid_species and sp_item.strip().lower() not in valid_species:
                                                    errors.append(f"{f}: preset '{pid}' favoredAgainstSpecies contains unknown species '{sp_item}'")

                                    if "expectedLeadMembers" in preset:
                                        exp = preset["expectedLeadMembers"]
                                        if not isinstance(exp, list) or len(exp) != 2:
                                            errors.append(f"{f}: preset '{pid}' expectedLeadMembers must be a list of 2 members")
                                        else:
                                            for idx, exp_m in enumerate(exp):
                                                if not isinstance(exp_m, dict):
                                                    errors.append(f"{f}: preset '{pid}' expectedLeadMembers[{idx}] must be a dict")
                                                    continue
                                                exp_sp = exp_m.get("species")
                                                if not exp_sp or not isinstance(exp_sp, str):
                                                    errors.append(f"{f}: preset '{pid}' expectedLeadMembers[{idx}] missing 'species'")
                                                    continue
                                                actual_slot = slots[idx]
                                                if 0 <= actual_slot < team_len:
                                                    actual_mon = d["team"][actual_slot]
                                                    actual_sp = actual_mon.get("species", "").lower()
                                                    if exp_sp.strip().lower() != actual_sp:
                                                        errors.append(f"{f}: preset '{pid}' expected member {idx} species '{exp_sp}' does not match slot {actual_slot} '{actual_sp}'")

                                                    if "form" in exp_m and exp_m["form"] is not None:
                                                        exp_form = str(exp_m["form"]).strip().lower()
                                                        if "form" in actual_mon and actual_mon["form"] is not None:
                                                            actual_form = str(actual_mon.get("form", "")).strip().lower()
                                                            if exp_form != actual_form:
                                                                errors.append(f"{f}: preset '{pid}' expected member {idx} form '{exp_form}' does not match slot {actual_slot} '{actual_form}'")

                                                    if "requiredAspects" in exp_m and exp_m["requiredAspects"] is not None:
                                                        req_asp = exp_m["requiredAspects"]
                                                        if not isinstance(req_asp, list):
                                                            errors.append(f"{f}: preset '{pid}' expected member {idx} requiredAspects must be a list")
                                                        else:
                                                            actual_aspects = {str(a).strip().lower() for a in actual_mon.get("aspects", [])}
                                                            if "gender" in actual_mon and actual_mon["gender"]:
                                                                actual_aspects.add(str(actual_mon["gender"]).strip().lower())
                                                            for a in req_asp:
                                                                if str(a).strip().lower() not in actual_aspects:
                                                                    errors.append(f"{f}: preset '{pid}' expected member {idx} aspect '{a}' missing on slot {actual_slot}")
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
    if forbidden_items or forbidden_moves or forbidden_abilities or forbidden_aspects:
        log_pass("Phase D semantic validation: zero forbidden deterministic compatibility invalids found in modernized pack.")

    # 4. Inventory checks (when reports/compat-audit/trainer-inventory.json is present)
    inv_file = os.path.join(repo_root, "reports", "compat-audit", "trainer-inventory.json")
    if os.path.exists(inv_file):
        try:
            with open(inv_file, "r", encoding="utf-8") as jf:
                inv = json.load(jf)
            obsolete_ids = set(inv.get("obsolete_trainers_in_hell", []))
            pack_filenames = set(os.listdir(trainers_dir))
            present_obsolete = obsolete_ids & pack_filenames
            if present_obsolete:
                log_fail(f"Obsolete trainer IDs must not exist in modernized pack: {sorted(present_obsolete)}")
                return False
            log_pass(f"Obsolete trainer IDs ({len(obsolete_ids)}) confirmed absent from modernized pack.")

            expected_count = inv.get("summary", {}).get("total_effective_baseline_trainers")
            if expected_count and count != expected_count:
                log_fail(f"Modernized pack trainer count ({count}) does not match expected baseline ({expected_count})")
                return False
            if expected_count:
                log_pass(f"Modernized pack trainer count ({count}) matches upstream baseline ({expected_count}).")
        except Exception as e:
            log_fail(f"Failed to check trainer-inventory.json against modernized pack: {e}")
            return False

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
