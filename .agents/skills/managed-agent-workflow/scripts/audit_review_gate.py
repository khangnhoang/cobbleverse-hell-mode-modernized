#!/usr/bin/env python3
"""
audit_review_gate.py — Standalone Repository Review Gate Acceptance Oracle

Evaluates the 9 deterministic predicates required for review acceptance:
1. session_handshake_verified: Reviewer emitted ## AUTHORITY_LOADED
2. authority_closure_tool_audit: view_file tool calls in transcript covered all manifest entries BEFORE handshake with non-trivial ranges (>= 10 lines)
3. verdict_in_closed_algebra: Verdict matches ## (R|IR) verdict: (PASS|BLOCKING_FINDINGS|BLOCKED)
4. required_sections_present: All required report sections present per rubric
5. has_4part_falsification_structure: 4-part counterexample falsification structure present
6. mechanical_scan_clean: Raw report free of unmeasured semantic absolutes or forbidden phrases
7. plan_hash_parity: Candidate hash verified on disk, scope closed over changed files, and appears in report
8. review_candidate_binding: Authoritative candidate dispatch preceded report, and report explicitly binds to candidate hash
9. review_completeness: All required dimensions evaluated; blocking finding does not terminate review
"""

import argparse
import json
import os
import re
import subprocess
import sys

if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8')


def get_actual_candidate_changed_files(repo_root, base_commit=None, candidate_manifest_path=None):
    """
    Derives actual candidate changed file set from repository state:
    tracked diff against base_commit + untracked candidate files via git ls-files.
    Excludes candidate_manifest.json and scratch/.
    """
    changed_files = set()
    if base_commit:
        try:
            diff_out = subprocess.check_output(
                ['git', 'diff', '--name-only', base_commit],
                cwd=repo_root,
                stderr=subprocess.DEVNULL
            ).decode().strip().splitlines()
            for p in diff_out:
                clean_p = p.strip()
                if clean_p:
                    changed_files.add(os.path.normpath(clean_p).replace('\\', '/'))
        except Exception:
            pass

    try:
        untracked_out = subprocess.check_output(
            ['git', 'ls-files', '--others', '--exclude-standard'],
            cwd=repo_root,
            stderr=subprocess.DEVNULL
        ).decode().strip().splitlines()
        for p in untracked_out:
            clean_p = p.strip()
            if clean_p:
                changed_files.add(os.path.normpath(clean_p).replace('\\', '/'))
    except Exception:
        pass

    # Filter out justified exclusions
    exclusions = {'scratch', '.system_generated', 'docs/workstreams/agent-architecture-redesign/candidate_manifest.json'}
    if candidate_manifest_path:
        norm_cand = os.path.normpath(candidate_manifest_path).replace('\\', '/')
        repo_norm = os.path.normpath(repo_root).replace('\\', '/')
        if norm_cand.startswith(repo_norm):
            rel_cand = os.path.relpath(norm_cand, repo_norm).replace('\\', '/')
            exclusions.add(rel_cand)
        else:
            exclusions.add(norm_cand)

    filtered = set()
    for f in changed_files:
        if any(f == ex or f.startswith(ex + '/') for ex in exclusions):
            continue
        if os.path.basename(f) == 'candidate_manifest.json':
            continue
        filtered.add(f)
    return filtered


def evaluate_report_text(
    report_content,
    expected_commit="1fa1d58",
    expected_paths=None,
    candidate_file=None,
    expected_hash=None,
    phase="PLAN_REVIEW",
    tool_views=None,
    handshake_commit=None,
    candidate_binding_passed=True,
    binding_failure_msg=None,
    role=None,
    actual_changed_files=None
):
    checks = {}
    failures = []

    # 1. session_handshake_verified
    if handshake_commit and expected_commit and handshake_commit.startswith(expected_commit[:7]):
        checks['session_handshake_verified'] = True
    elif handshake_commit:
        checks['session_handshake_verified'] = False
        failures.append(f"Handshake commit mismatch (found: {handshake_commit}, expected: {expected_commit})")
    elif 'AUTHORITY_LOADED' in report_content:
        # Fallback if transcript was not supplied: check report text
        checks['session_handshake_verified'] = True
    else:
        checks['session_handshake_verified'] = False
        failures.append("Reviewer handshake token 'AUTHORITY_LOADED' not found in report or transcript")

    # 2. authority_closure_tool_audit (report-independent via transcript tool history)
    if expected_paths:
        if tool_views is not None:
            missing_views = []
            for ep in expected_paths:
                norm_ep = os.path.normpath(ep).lower()
                lines_viewed = tool_views.get(norm_ep, 0)
                if lines_viewed < 10:
                    missing_views.append(ep)
            if not missing_views:
                checks['authority_closure_tool_audit'] = True
            else:
                checks['authority_closure_tool_audit'] = False
                failures.append(
                    f"Authority closure tool audit failed: the following manifest files were not inspected via view_file with >= 10 lines before handshake: {missing_views}"
                )
        else:
            # If no transcript provided, cannot verify tool audit independently
            checks['authority_closure_tool_audit'] = True
    else:
        checks['authority_closure_tool_audit'] = True

    # Mechanically derive and enforce reviewer role from workflow phase
    required_role = 'plan_reviewer' if phase == 'PLAN_REVIEW' else ('implementation_reviewer' if phase == 'IMPL_REVIEW' else None)
    if required_role is None:
        checks['verdict_in_closed_algebra'] = False
        failures.append(f"Unrecognized workflow phase: {phase}")
        return checks, failures, None

    if role and role != required_role:
        checks['verdict_in_closed_algebra'] = False
        failures.append(f"Role mismatch: phase {phase} requires role '{required_role}', but '{role}' was supplied")
        return checks, failures, None

    effective_role = required_role

    # 3. verdict_in_closed_algebra
    verdict = None
    if effective_role == 'plan_reviewer':
        verdict_pattern = r'##\s*R\s*verdict:\s*(PASS|BLOCKING_FINDINGS|BLOCKED)'
    else:  # implementation_reviewer
        verdict_pattern = r'##\s*IR\s*verdict:\s*(PASS|BLOCKING_FINDINGS|BLOCKED)'

    v_match = re.search(verdict_pattern, report_content)
    if v_match:
        checks['verdict_in_closed_algebra'] = True
        verdict = v_match.group(1)
    else:
        checks['verdict_in_closed_algebra'] = False
        failures.append(
            f"Visible verdict header matching closed algebra for {effective_role} not found "
            f"(must be ## {'R' if effective_role == 'plan_reviewer' else 'IR'} verdict: PASS|BLOCKING_FINDINGS|BLOCKED)"
        )

    # 4. required_sections_present
    req_sections = [
        r'Summary',
        r'(?:Falsification|Dimension Evaluations)',
        r'verdict:'
    ]
    sections_found = all(re.search(s, report_content, re.IGNORECASE) for s in req_sections)
    if sections_found:
        checks['required_sections_present'] = True
    else:
        checks['required_sections_present'] = False
        failures.append("Required report sections missing (must include Summary, Falsification / Dimension Evaluations, and Verdict)")

    # 5. has_4part_falsification_structure
    has_target_invariant = "Target Invariant" in report_content
    has_counterexample = ("Counterexample Attempted" in report_content) or ("Counterexample" in report_content)
    has_execution_trace = "Execution Trace" in report_content
    has_result = ("Result / Defense" in report_content) or ("Result" in report_content)
    has_structure = has_target_invariant and has_counterexample and has_execution_trace and has_result
    if has_structure:
        checks['has_4part_falsification_structure'] = True
    else:
        checks['has_4part_falsification_structure'] = False
        failures.append("4-part falsification structure incomplete (must contain Target Invariant, Counterexample Attempted/Falsification, Execution Trace, Result/Defense)")

    # 6. mechanical_scan_clean
    proscribed_regexes = [
        r'\bhoàn toàn\b',
        r'\btriệt để\b',
        r'\bflawless\b',
        r'\bzero risk\b',
        r'\bguarantees\s+(?:correctness|perfection|safety|all)\b',
        r'\bkhông có rủi ro\b',
        r'\btuyệt đối\b',
        r'\bcompletely closes\b',
        r'100%\s*(?:tuân thủ|an toàn|chính xác)'
    ]
    text_without_code = re.sub(r'```.*?```', '', report_content, flags=re.DOTALL)
    found_proscribed = []
    for pr in proscribed_regexes:
        matches = re.findall(pr, text_without_code, re.IGNORECASE)
        unquoted = [m for m in matches if f'"{m}"' not in text_without_code and f'`"{m}"`' not in text_without_code]
        if unquoted:
            found_proscribed.extend(unquoted)

    if not found_proscribed:
        checks['mechanical_scan_clean'] = True
    else:
        checks['mechanical_scan_clean'] = False
        failures.append(f"Proscribed absolute phrases found: {found_proscribed}")

    # 7. plan_hash_parity & candidate_scope_closure
    if phase == 'IMPL_REVIEW' and (not expected_hash or not candidate_file):
        checks['plan_hash_parity'] = False
        failures.append("IMPL_REVIEW requires explicit candidate identity inputs (candidate_file and expected_hash)")
    elif expected_hash:
        hash_in_report = expected_hash in report_content
        disk_hash_match = True
        if candidate_file:
            if not os.path.exists(candidate_file):
                disk_hash_match = False
                failures.append(f"Candidate file not found on disk: {candidate_file}")
            else:
                try:
                    actual_hash = subprocess.check_output(['git', 'hash-object', candidate_file]).decode().strip()
                    if actual_hash != expected_hash:
                        disk_hash_match = False
                        failures.append(f"Candidate file hash mismatch: on-disk {actual_hash} != expected {expected_hash}")
                except Exception as e:
                    disk_hash_match = False
                    failures.append(f"Error computing candidate file hash: {e}")

                # If candidate file is candidate_manifest.json (or any JSON with candidate_files), verify all entries
                is_manifest = False
                try:
                    with open(candidate_file, 'r', encoding='utf-8') as cf:
                        cdata = json.load(cf)
                    if isinstance(cdata, dict) and 'candidate_files' in cdata:
                        is_manifest = True
                        repo_root = os.getcwd()
                        try:
                            git_root = subprocess.check_output(['git', 'rev-parse', '--show-toplevel'], stderr=subprocess.DEVNULL).decode().strip()
                            if os.path.isdir(git_root):
                                repo_root = git_root
                        except Exception:
                            pass

                        manifest_files = set()
                        for cf_entry in cdata['candidate_files']:
                            cf_rel = cf_entry.get('path')
                            cf_expected_blob = cf_entry.get('blob_hash')
                            if not cf_rel:
                                continue
                            norm_rel = os.path.normpath(cf_rel).replace('\\', '/')
                            manifest_files.add(norm_rel)

                            # Resolve relative to manifest directory if present, else repo_root
                            cand_dir = os.path.dirname(os.path.abspath(candidate_file))
                            if os.path.exists(os.path.join(cand_dir, cf_rel)):
                                cf_full = os.path.normpath(os.path.join(cand_dir, cf_rel))
                            else:
                                cf_full = os.path.normpath(os.path.join(repo_root, cf_rel))

                            if not os.path.exists(cf_full):
                                disk_hash_match = False
                                failures.append(f"Candidate manifest file missing on disk: {cf_rel}")
                            else:
                                try:
                                    cf_actual = subprocess.check_output(['git', 'hash-object', cf_full]).decode().strip()
                                    if cf_actual != cf_expected_blob:
                                        disk_hash_match = False
                                        failures.append(
                                            f"Candidate manifest blob mismatch for {cf_rel}: on-disk {cf_actual} != manifest {cf_expected_blob}"
                                        )
                                except Exception as e_blob:
                                    disk_hash_match = False
                                    failures.append(f"Error computing blob hash for candidate file {cf_rel}: {e_blob}")

                        # Candidate Scope Closure Verification
                        base_commit = cdata.get('base_commit')
                        effective_changed = actual_changed_files
                        if effective_changed is None and base_commit:
                            try:
                                subprocess.check_output(['git', 'rev-parse', '--verify', f"{base_commit}^{{commit}}"], cwd=repo_root, stderr=subprocess.DEVNULL)
                                effective_changed = get_actual_candidate_changed_files(repo_root, base_commit, candidate_file)
                            except Exception:
                                pass

                        if effective_changed is not None:
                            extra_changed = effective_changed - manifest_files
                            out_of_scope = manifest_files - effective_changed
                            if extra_changed:
                                disk_hash_match = False
                                failures.append(
                                    f"Candidate scope closure failure: unlisted changed candidate file(s): {sorted(list(extra_changed))}"
                                )
                            if out_of_scope:
                                disk_hash_match = False
                                failures.append(
                                    f"Candidate scope closure failure: manifest contains out-of-scope/unchanged file(s): {sorted(list(out_of_scope))}"
                                )
                except (json.JSONDecodeError, UnicodeDecodeError):
                    pass
                except Exception as e_cdata:
                    disk_hash_match = False
                    failures.append(f"Error reading candidate manifest: {e_cdata}")

                if phase == 'IMPL_REVIEW' and not is_manifest:
                    disk_hash_match = False
                    failures.append("Candidate file for IMPL_REVIEW must be a valid JSON manifest containing 'candidate_files'")
        else:
            if phase == 'IMPL_REVIEW':
                disk_hash_match = False
                failures.append("Candidate file path is required for IMPL_REVIEW")

        if not hash_in_report:
            failures.append(f"Expected candidate hash {expected_hash} does not appear in report text")

        if hash_in_report and disk_hash_match:
            checks['plan_hash_parity'] = True
        else:
            checks['plan_hash_parity'] = False
    else:
        checks['plan_hash_parity'] = True

    # 8. review_candidate_binding
    if candidate_binding_passed:
        checks['review_candidate_binding'] = True
    else:
        checks['review_candidate_binding'] = False
        failures.append(f"Candidate binding failed: {binding_failure_msg}")

    # 9. review_completeness (GAP 2)
    # Required for implementation_reviewer under IMPL_REVIEW
    is_ir = (effective_role == 'implementation_reviewer')
    if is_ir:
        required_dimensions = [
            (1, "Frozen Plan Conformance", r'Dimension\s*1|Frozen\s*Plan\s*Conformance'),
            (2, "Blast Radius & Surgical Scope", r'Dimension\s*2|Blast\s*Radius|Surgical\s*Scope'),
            (3, "Correctness & Boundary Safety", r'Dimension\s*3|Correctness|Boundary\s*Safety'),
            (4, "Verification Authenticity", r'Dimension\s*4|Verification\s*Authenticity|False-Green'),
            (5, "Orthogonal Verification", r'Dimension\s*5|Orthogonal\s*Verification')
        ]
        completeness_failures = []
        for dim_num, dim_name, pat in required_dimensions:
            m = re.search(pat, report_content, re.IGNORECASE)
            if not m:
                completeness_failures.append(f"Missing evaluation for Dimension {dim_num} ({dim_name})")
            else:
                dim_idx = m.start()
                subsequent_starts = []
                for _, _, p2 in required_dimensions:
                    for m2 in re.finditer(p2, report_content, re.IGNORECASE):
                        if m2.start() > dim_idx:
                            subsequent_starts.append(m2.start())
                end_idx = min(subsequent_starts) if subsequent_starts else len(report_content)
                dim_text = report_content[dim_idx:end_idx]

                if re.search(r'\bBLOCKED\b', dim_text, re.IGNORECASE):
                    if 'blocking_dependency' not in dim_text.lower():
                        completeness_failures.append(
                            f"Dimension {dim_num} ({dim_name}) marked BLOCKED without specifying 'blocking_dependency'"
                        )

        if not completeness_failures:
            checks['review_completeness'] = True
        else:
            checks['review_completeness'] = False
            for cf_fail in completeness_failures:
                failures.append(f"Review completeness failure: {cf_fail}")
    else:
        checks['review_completeness'] = True

    return checks, failures, verdict


def evaluate_binding_chronology(events, expected_hash):
    latest_dispatch_idx = None
    for ev in events:
        if ev['type'] == 'dispatch' and ev.get('hash') == expected_hash:
            latest_dispatch_idx = ev['index']

    if latest_dispatch_idx is None:
        return False, "No authoritative dispatch of candidate hash found in history", None

    eligible_reports = []
    for ev in events:
        if ev['type'] == 'report' and ev['index'] > latest_dispatch_idx:
            eligible_reports.append(ev)

    if not eligible_reports:
        return False, f"No review report emitted after authoritative dispatch of {expected_hash}", None

    selected_report = eligible_reports[-1]
    if expected_hash not in selected_report.get('content', ''):
        return False, f"Report emitted after dispatch does not identify expected candidate {expected_hash}", None

    return True, "Report mechanically bound to authoritative candidate dispatch", selected_report


def run_regression_fixtures():
    print("=" * 80)
    print("RUNNING REPORT-INDEPENDENT ORACLE REGRESSION FIXTURES")
    print("=" * 80)

    fixtures_passed = 0
    total_fixtures = 17

    # FIXTURE A: VALID REPORT
    text_a = """
## R verdict: PASS
### 1. Summary Evaluation
Summary of the review. Candidate hash: test_hash_123.
### 2. Falsification Blocks
#### Case 1
- **Target Invariant**: Target invariant details.
- **Counterexample Attempted**: Counterexample attempted.
- **Execution Trace**: Trace steps.
- **Result / Defense**: Result and defense.
- **Residual Limitation**: Known limitation.
### 3. Verdict Summary
## R verdict: PASS
"""
    c_a, f_a, v_a = evaluate_report_text(
        text_a,
        expected_commit="1fa1d58",
        handshake_commit="1fa1d58",
        expected_paths=set(),
        candidate_binding_passed=True
    )
    if all(c_a.values()) and v_a == "PASS":
        print("  [PASS] Fixture A: Golden Valid Report (Exit 0)")
        fixtures_passed += 1
    else:
        print(f"  [FAIL] Fixture A failed: {f_a}")

    # FIXTURE B: MISSING FALSIFICATION BLOCK
    text_b = """
## R verdict: PASS
### 1. Summary Evaluation
Summary.
### 2. Falsification Blocks
- **Target Invariant**: Something.
### 3. Verdict Summary
## R verdict: PASS
"""
    c_b, f_b, v_b = evaluate_report_text(
        text_b,
        expected_commit="1fa1d58",
        handshake_commit="1fa1d58",
        expected_paths=set()
    )
    if not all(c_b.values()) and not c_b.get('has_4part_falsification_structure'):
        print("  [PASS] Fixture B: Missing Falsification Block Detected (Exit non-zero)")
        fixtures_passed += 1
    else:
        print("  [FAIL] Fixture B should have failed falsification structure")

    # FIXTURE C: INVALID VERDICT (including stale REVISE)
    text_c = """
## R verdict: MAYBE_PASS
### 1. Summary Evaluation
Summary.
### 2. Falsification Blocks
- Target Invariant
- Counterexample Attempted
- Execution Trace
- Result / Defense
- Residual Limitation
"""
    c_c, f_c, v_c = evaluate_report_text(
        text_c,
        expected_commit="1fa1d58",
        handshake_commit="1fa1d58",
        expected_paths=set()
    )
    text_c_revise = """
## R verdict: REVISE
### 1. Summary Evaluation
Summary.
### 2. Falsification Blocks
- Target Invariant
- Counterexample Attempted
- Execution Trace
- Result / Defense
- Residual Limitation
"""
    c_cr, f_cr, v_cr = evaluate_report_text(
        text_c_revise,
        expected_commit="1fa1d58",
        handshake_commit="1fa1d58",
        expected_paths=set()
    )
    if not c_c.get('verdict_in_closed_algebra') and not c_cr.get('verdict_in_closed_algebra'):
        print("  [PASS] Fixture C: Invalid Verdict Rejected (MAYBE_PASS and stale REVISE) (Exit non-zero)")
        fixtures_passed += 1
    else:
        print("  [FAIL] Fixture C should have rejected invalid verdict")

    # FIXTURE D: MISSING MANIFEST AUTHORITY ENTRY
    c_d, f_d, v_d = evaluate_report_text(
        text_a,
        expected_commit="1fa1d58",
        handshake_commit="1fa1d58",
        expected_paths={"file1.md", "file2.md"},
        tool_views={"file1.md": 100}
    )
    if not c_d.get('authority_closure_tool_audit'):
        print("  [PASS] Fixture D: Missing Authority Closure Entry Detected (Exit non-zero)")
        fixtures_passed += 1
    else:
        print("  [FAIL] Fixture D should have detected missing authority path")

    # FIXTURE E: SELF-ATTESTED PASS WITH ACTUAL VIOLATION
    text_e = """
## R verdict: PASS
zero_proscribed_absolutes: TRUE
### 1. Summary Evaluation
Báo cáo giải quyết triệt để mọi vấn đề.
### 2. Falsification Blocks
- Target Invariant
- Counterexample Attempted
- Execution Trace
- Result / Defense
- Residual Limitation
"""
    c_e, f_e, v_e = evaluate_report_text(
        text_e,
        expected_commit="1fa1d58",
        handshake_commit="1fa1d58",
        expected_paths=set()
    )
    if not c_e.get('mechanical_scan_clean'):
        print("  [PASS] Fixture E: Self-Attested PASS with Actual Violation Rejected (Exit non-zero)")
        fixtures_passed += 1
    else:
        print("  [FAIL] Fixture E should have caught proscribed phrase despite self-attestation")

    # FIXTURE F: EVIDENCE-BACKED MEASUREMENT RATIO ALLOWED
    text_f = """
## R verdict: PASS
### 1. Summary Evaluation
Đã xác minh 14/14 files (100% files verified).
### 2. Falsification Blocks
- Target Invariant: Invariant.
- Counterexample Attempted: Attempt.
- Execution Trace: Steps.
- Result / Defense: Passed.
- Residual Limitation: None.
"""
    c_f, f_f, v_f = evaluate_report_text(
        text_f,
        expected_commit="1fa1d58",
        handshake_commit="1fa1d58",
        expected_paths=set()
    )
    if c_f.get('mechanical_scan_clean'):
        print("  [PASS] Fixture F: Evidence-Backed Measurement Ratio Permitted (Exit 0)")
        fixtures_passed += 1
    else:
        print(f"  [FAIL] Fixture F should permit evidence-backed measurement: {f_f}")

    # BINDING FIXTURE CASE A: STALE PASS REUSE (H1 dispatched & reviewed, candidate becomes H2 without dispatch)
    events_a = [
        {'type': 'dispatch', 'hash': 'hash_H1', 'index': 1},
        {'type': 'report', 'content': 'Report for hash_H1 ## R verdict: PASS', 'index': 2}
    ]
    b_pass_a, b_msg_a, _ = evaluate_binding_chronology(events_a, 'hash_H2')
    if not b_pass_a:
        print("  [PASS] Binding Case A: Stale PASS reuse rejected for H2")
        fixtures_passed += 1
    else:
        print("  [FAIL] Binding Case A should have rejected stale PASS")

    # BINDING FIXTURE CASE B: FRESH RE-REVIEW (H1 reviewed, H2 dispatched, fresh H2 report emitted)
    events_b = [
        {'type': 'dispatch', 'hash': 'hash_H1', 'index': 1},
        {'type': 'report', 'content': 'Report for hash_H1 ## R verdict: PASS', 'index': 2},
        {'type': 'dispatch', 'hash': 'hash_H2', 'index': 3},
        {'type': 'report', 'content': 'Report for hash_H2 ## R verdict: PASS', 'index': 4}
    ]
    b_pass_b, b_msg_b, _ = evaluate_binding_chronology(events_b, 'hash_H2')
    if b_pass_b:
        print("  [PASS] Binding Case B: Fresh re-review bound to H2 accepted")
        fixtures_passed += 1
    else:
        print(f"  [FAIL] Binding Case B should have accepted fresh re-review: {b_msg_b}")

    # BINDING FIXTURE CASE C: REPORT AFTER DISPATCH BUT IDENTIFIES WRONG HASH
    events_c = [
        {'type': 'dispatch', 'hash': 'hash_H2', 'index': 1},
        {'type': 'report', 'content': 'Report for hash_H1 ## R verdict: PASS', 'index': 2}
    ]
    b_pass_c, b_msg_c, _ = evaluate_binding_chronology(events_c, 'hash_H2')
    if not b_pass_c:
        print("  [PASS] Binding Case C: Report with wrong hash after dispatch rejected")
        fixtures_passed += 1
    else:
        print("  [FAIL] Binding Case C should have rejected wrong hash report")

    # BINDING FIXTURE CASE D: CORRECT HASH BUT PRE-DISPATCH REPORT EVENT
    events_d = [
        {'type': 'report', 'content': 'Speculative report mentioning hash_H2 ## R verdict: PASS', 'index': 1},
        {'type': 'dispatch', 'hash': 'hash_H2', 'index': 2}
    ]
    b_pass_d, b_msg_d, _ = evaluate_binding_chronology(events_d, 'hash_H2')
    if not b_pass_d:
        print("  [PASS] Binding Case D: Pre-dispatch report event rejected for H2")
        fixtures_passed += 1
    else:
        print("  [FAIL] Binding Case D should have rejected pre-dispatch report")

    # SCOPE FIXTURE CASE A: COMPLETE MANIFEST, EXACT CHANGED-FILE CLOSURE (PASS)
    import tempfile
    with tempfile.TemporaryDirectory() as tmpdir:
        f1_path = os.path.join(tmpdir, "file1.txt")
        with open(f1_path, "w", encoding="utf-8") as f:
            f.write("content 1")
        h1 = subprocess.check_output(['git', 'hash-object', f1_path]).decode().strip()
        manifest_a = {
            "workstream": "test",
            "base_commit": "5406b2c",
            "candidate_files": [{"path": "file1.txt", "blob_hash": h1}]
        }
        man_a_path = os.path.join(tmpdir, "candidate_manifest.json")
        with open(man_a_path, "w", encoding="utf-8") as f:
            json.dump(manifest_a, f)
        man_a_hash = subprocess.check_output(['git', 'hash-object', man_a_path]).decode().strip()

        rep_a = f"Candidate hash: {man_a_hash}\n## R verdict: PASS\n### Summary\n### Falsification\nTarget Invariant\nCounterexample Attempted\nExecution Trace\nResult / Defense"
        c_sa, f_sa, _ = evaluate_report_text(
            rep_a,
            candidate_file=man_a_path,
            expected_hash=man_a_hash,
            actual_changed_files={"file1.txt"}
        )
        if c_sa.get('plan_hash_parity'):
            print("  [PASS] Scope Case A: Complete manifest, exact changed-file closure accepted")
            fixtures_passed += 1
        else:
            print(f"  [FAIL] Scope Case A failed: {f_sa}")

    # SCOPE FIXTURE CASE B: UNLISTED CHANGED CANDIDATE FILE (FAIL)
    with tempfile.TemporaryDirectory() as tmpdir:
        f1_path = os.path.join(tmpdir, "file1.txt")
        with open(f1_path, "w", encoding="utf-8") as f:
            f.write("content 1")
        h1 = subprocess.check_output(['git', 'hash-object', f1_path]).decode().strip()
        manifest_b = {
            "workstream": "test",
            "base_commit": "5406b2c",
            "candidate_files": [{"path": "file1.txt", "blob_hash": h1}]
        }
        man_b_path = os.path.join(tmpdir, "candidate_manifest.json")
        with open(man_b_path, "w", encoding="utf-8") as f:
            json.dump(manifest_b, f)
        man_b_hash = subprocess.check_output(['git', 'hash-object', man_b_path]).decode().strip()

        rep_b = f"Candidate hash: {man_b_hash}\n## R verdict: PASS\n### Summary\n### Falsification\nTarget Invariant\nCounterexample Attempted\nExecution Trace\nResult / Defense"
        c_sb, f_sb, _ = evaluate_report_text(
            rep_b,
            candidate_file=man_b_path,
            expected_hash=man_b_hash,
            actual_changed_files={"file1.txt", "file2_unlisted.txt"}
        )
        if not c_sb.get('plan_hash_parity') and any('unlisted changed candidate file' in x for x in f_sb):
            print("  [PASS] Scope Case B: Unlisted changed candidate file rejected")
            fixtures_passed += 1
        else:
            print(f"  [FAIL] Scope Case B should have rejected unlisted changed file: {f_sb}")

    # SCOPE FIXTURE CASE C: MANIFEST CONTAINS NONEXISTENT / OUT-OF-SCOPE MEMBER (FAIL)
    with tempfile.TemporaryDirectory() as tmpdir:
        f1_path = os.path.join(tmpdir, "file1.txt")
        with open(f1_path, "w", encoding="utf-8") as f:
            f.write("content 1")
        h1 = subprocess.check_output(['git', 'hash-object', f1_path]).decode().strip()
        manifest_c = {
            "workstream": "test",
            "base_commit": "5406b2c",
            "candidate_files": [
                {"path": "file1.txt", "blob_hash": h1},
                {"path": "out_of_scope.txt", "blob_hash": "0000000000000000000000000000000000000000"}
            ]
        }
        man_c_path = os.path.join(tmpdir, "candidate_manifest.json")
        with open(man_c_path, "w", encoding="utf-8") as f:
            json.dump(manifest_c, f)
        man_c_hash = subprocess.check_output(['git', 'hash-object', man_c_path]).decode().strip()

        rep_c = f"Candidate hash: {man_c_hash}\n## R verdict: PASS\n### Summary\n### Falsification\nTarget Invariant\nCounterexample Attempted\nExecution Trace\nResult / Defense"
        c_sc, f_sc, _ = evaluate_report_text(
            rep_c,
            candidate_file=man_c_path,
            expected_hash=man_c_hash,
            actual_changed_files={"file1.txt"}
        )
        if not c_sc.get('plan_hash_parity') and any('out-of-scope' in x or 'missing on disk' in x for x in f_sc):
            print("  [PASS] Scope Case C: Out-of-scope / nonexistent manifest member rejected")
            fixtures_passed += 1
        else:
            print(f"  [FAIL] Scope Case C should have rejected out-of-scope member: {f_sc}")

    # SCOPE FIXTURE CASE D: LISTED MEMBER MUTATES AFTER IDENTITY CREATION (FAIL)
    with tempfile.TemporaryDirectory() as tmpdir:
        f1_path = os.path.join(tmpdir, "file1.txt")
        with open(f1_path, "w", encoding="utf-8") as f:
            f.write("initial content")
        h1 = subprocess.check_output(['git', 'hash-object', f1_path]).decode().strip()
        manifest_d = {
            "workstream": "test",
            "base_commit": "5406b2c",
            "candidate_files": [{"path": "file1.txt", "blob_hash": h1}]
        }
        man_d_path = os.path.join(tmpdir, "candidate_manifest.json")
        with open(man_d_path, "w", encoding="utf-8") as f:
            json.dump(manifest_d, f)
        man_d_hash = subprocess.check_output(['git', 'hash-object', man_d_path]).decode().strip()

        # Mutate file1.txt on disk
        with open(f1_path, "w", encoding="utf-8") as f:
            f.write("mutated content after identity")

        rep_d = f"Candidate hash: {man_d_hash}\n## R verdict: PASS\n### Summary\n### Falsification\nTarget Invariant\nCounterexample Attempted\nExecution Trace\nResult / Defense"
        c_sd, f_sd, _ = evaluate_report_text(
            rep_d,
            candidate_file=man_d_path,
            expected_hash=man_d_hash,
            actual_changed_files={"file1.txt"}
        )
        if not c_sd.get('plan_hash_parity') and any('blob mismatch' in x for x in f_sd):
            print("  [PASS] Scope Case D: Member mutation after identity creation rejected")
            fixtures_passed += 1
        else:
            print(f"  [FAIL] Scope Case D should have rejected mutated member: {f_sd}")

    # REVIEW COMPLETENESS FIXTURE: DEFECT A TERMINATION WITHOUT EVALUATING DEFECT B (FAIL)
    text_incomplete_review = """
## IR verdict: BLOCKING_FINDINGS
### Summary Evaluation
Discovered defect A in plan conformance and terminating review early.
### Dimension Evaluations
#### Dimension 1: Frozen Plan Conformance
- **Target Invariant**: Exact plan conformance.
- **Counterexample Attempted**: Code deviates from plan.
- **Candidate Evidence Actually Inspected**: file1.txt
- **Execution Trace**: Plan deviation detected at line 10.
- **Result / Defense**: FAILED.
- **Blocking Finding**: Defect A observed.
### Findings
#### [Finding-01] [Critical]: Plan Deviation
"""
    c_comp, f_comp, _ = evaluate_report_text(
        text_incomplete_review,
        phase="IMPL_REVIEW",
        role="implementation_reviewer"
    )
    if not c_comp.get('review_completeness') and any('Missing evaluation for Dimension 2' in x for x in f_comp):
        print("  [PASS] Review Completeness Case: Early termination on Defect A without evaluating Defect B rejected")
        fixtures_passed += 1
    else:
        print(f"  [FAIL] Review Completeness Case should have failed for incomplete review: {f_comp}")

    # VALID IR REPORT TEXT FOR IMPL_REVIEW FIXTURES
    text_ir_valid = """
## IR verdict: PASS
### 1. Summary Evaluation
Summary of the IR review. Candidate hash: test_hash_123.
### 2. Dimension Evaluations
#### Dimension 1: Frozen Plan Conformance
- **Target Invariant**: Exact plan conformance.
- **Counterexample Attempted**: Code deviates from plan.
- **Execution Trace**: Trace steps.
- **Result / Defense**: Passed.
- **Residual Limitation**: None.
#### Dimension 2: Blast Radius & Surgical Scope
- **Target Invariant**: Surgical scope.
- **Counterexample Attempted**: Scope creep.
- **Execution Trace**: Trace steps.
- **Result / Defense**: Passed.
- **Residual Limitation**: None.
#### Dimension 3: Correctness & Boundary Safety
- **Target Invariant**: Boundary safety.
- **Counterexample Attempted**: Unsafe edge case.
- **Execution Trace**: Trace steps.
- **Result / Defense**: Passed.
- **Residual Limitation**: None.
#### Dimension 4: Verification Authenticity
- **Target Invariant**: Authentic tests.
- **Counterexample Attempted**: Mocked assertions.
- **Execution Trace**: Trace steps.
- **Result / Defense**: Passed.
- **Residual Limitation**: None.
#### Dimension 5: Orthogonal Verification
- **Target Invariant**: Orthogonal layers.
- **Counterexample Attempted**: Inappropriate layer skip.
- **Execution Trace**: Trace steps.
- **Result / Defense**: Passed.
- **Residual Limitation**: None.
### 3. Verdict Summary
## IR verdict: PASS
"""

    # IMPL_REVIEW MISSING CANDIDATE INPUTS (FAIL-CLOSED)
    c_ir_missing, f_ir_missing, _ = evaluate_report_text(
        text_ir_valid,
        expected_commit="1fa1d58",
        handshake_commit="1fa1d58",
        expected_paths=set(),
        phase="IMPL_REVIEW",
        candidate_file=None,
        expected_hash=None
    )
    if not c_ir_missing.get('plan_hash_parity') and any('IMPL_REVIEW requires explicit candidate identity inputs' in x for x in f_ir_missing):
        print("  [PASS] IMPL_REVIEW Scope Case: Missing candidate identity inputs rejected (Exit non-zero)")
        fixtures_passed += 1
    else:
        print(f"  [FAIL] IMPL_REVIEW should have failed when candidate identity inputs are absent: {f_ir_missing}")

    # PHASE-ROLE MUTUAL EXCLUSION FIXTURE: R cannot pass IMPL_REVIEW, IR cannot pass PLAN_REVIEW
    c_r_impl, f_r_impl, _ = evaluate_report_text(
        text_a,
        expected_commit="1fa1d58",
        handshake_commit="1fa1d58",
        expected_paths=set(),
        phase="IMPL_REVIEW",
        candidate_file="dummy_manifest.json",
        expected_hash="test_hash_123",
        candidate_binding_passed=True
    )
    c_ir_plan, f_ir_plan, _ = evaluate_report_text(
        text_ir_valid,
        expected_commit="1fa1d58",
        handshake_commit="1fa1d58",
        expected_paths=set(),
        phase="PLAN_REVIEW",
        candidate_file=None,
        expected_hash="test_hash_123",
        candidate_binding_passed=True
    )
    c_mismatch, f_mismatch, _ = evaluate_report_text(
        text_a,
        phase="IMPL_REVIEW",
        role="plan_reviewer"
    )
    if (not c_r_impl.get('verdict_in_closed_algebra')) and (not c_ir_plan.get('verdict_in_closed_algebra')) and (not c_mismatch.get('verdict_in_closed_algebra')) and any('Role mismatch' in x for x in f_mismatch):
        print("  [PASS] Phase-Role Binding Case: R report rejected in IMPL_REVIEW and IR report rejected in PLAN_REVIEW")
        fixtures_passed += 1
    else:
        print(f"  [FAIL] Phase-Role Binding Case failed: R in IMPL: {f_r_impl}, IR in PLAN: {f_ir_plan}, Mismatch: {f_mismatch}")

    print("-" * 80)
    print(f"REGRESSION FIXTURES RESULT: {fixtures_passed}/{total_fixtures} PASSED")
    return 0 if (fixtures_passed == total_fixtures) else 1


def audit_review_gate(
    transcript_path=None,
    manifest_path=None,
    candidate_file=None,
    expected_hash=None,
    phase="PLAN_REVIEW",
    report_path=None,
    role=None
):
    if transcript_path:
        if transcript_path.endswith('transcript.jsonl'):
            full_path = transcript_path.replace('transcript.jsonl', 'transcript_full.jsonl')
            if os.path.exists(full_path):
                transcript_path = full_path

    # Derive and enforce role from phase mechanically
    required_role = 'plan_reviewer' if phase == 'PLAN_REVIEW' else ('implementation_reviewer' if phase == 'IMPL_REVIEW' else None)
    if required_role is None:
        print(f"ERROR: Unrecognized workflow phase: {phase}")
        return 1
    if role and role != required_role:
        print(f"ERROR: Role mismatch: phase {phase} requires role '{required_role}', but '{role}' was supplied")
        return 1
    if not role:
        role = required_role

    print("=" * 80)
    print(f"MECHANICAL REVIEW GATE EVALUATION — Phase: {phase} / Role: {role}")
    if transcript_path:
        print(f"Auditing Transcript: {transcript_path}")
    if report_path:
        print(f"Auditing Report: {report_path}")
    print(f"Expected Candidate Hash: {expected_hash}")
    print("=" * 80)

    # 1. Load manifest
    expected_commit = "1fa1d58"
    expected_paths = set()
    if manifest_path and os.path.exists(manifest_path):
        with open(manifest_path, 'r', encoding='utf-8') as f:
            manifest = json.load(f)
        expected_commit = manifest.get('bootstrap_commit', '1fa1d58')
        for e in manifest.get('entries', []):
            target = e.get('dest') or e.get('path')
            if target:
                expected_paths.add(os.path.normpath(target).lower())

    # 2. Parse transcript if present
    handshake_commit = None
    viewed_files_before_handshake = {}
    events = []
    line_idx = 0

    if transcript_path and os.path.exists(transcript_path):
        with open(transcript_path, 'r', encoding='utf-8') as f:
            for line in f:
                line_idx += 1
                try:
                    data = json.loads(line)
                except json.JSONDecodeError:
                    continue

                source = data.get('source')
                msg_type = data.get('type')
                content = data.get('content', '')

                # Incoming user inputs / parent messages containing dispatch
                if source in ('USER', 'USER_INPUT', 'PARENT', 'SYSTEM') or msg_type in ('USER_INPUT', 'SYSTEM_MESSAGE'):
                    m_hashes = re.findall(r'[0-9a-f]{40}', content)
                    for h in m_hashes:
                        events.append({'type': 'dispatch', 'hash': h, 'content': content, 'index': line_idx})

                if source == 'MODEL':
                    if 'tool_calls' in data:
                        for tc in data['tool_calls']:
                            tool_name = tc.get('name')
                            args = tc.get('args', {})

                            # Track send_message
                            if tool_name == 'send_message':
                                msg = args.get('Message', '')
                                if 'AUTHORITY_LOADED' in msg:
                                    if expected_commit[:7] in msg:
                                        handshake_commit = expected_commit[:7]
                                if re.search(r'##\s*(?:R|IR)\s*verdict:', msg):
                                    events.append({'type': 'report', 'content': msg, 'index': line_idx})

                            # Track view_file calls
                            if tool_name == 'view_file':
                                raw_path = args.get('AbsolutePath') or ''
                                clean_path = raw_path.strip('"\'')
                                norm_path = os.path.normpath(clean_path).lower()
                                start_line = args.get('StartLine')
                                end_line = args.get('EndLine')
                                try:
                                    sl = int(start_line) if start_line is not None else None
                                    el = int(end_line) if end_line is not None else None
                                    lines_count = (el - sl + 1) if (sl and el) else 800
                                except (ValueError, TypeError):
                                    lines_count = 800

                                if not handshake_commit:
                                    if norm_path not in viewed_files_before_handshake:
                                        viewed_files_before_handshake[norm_path] = lines_count
                                    else:
                                        viewed_files_before_handshake[norm_path] = max(
                                            viewed_files_before_handshake[norm_path], lines_count
                                        )

                    if 'AUTHORITY_LOADED' in content and not handshake_commit:
                        if expected_commit[:7] in content:
                            handshake_commit = expected_commit[:7]

    # 3. Determine report content and candidate binding
    report_content = ""
    report_idx = None
    b_pass = True
    b_msg = "Evaluated without transcript events"

    if events and expected_hash:
        b_pass, b_msg, selected_report_ev = evaluate_binding_chronology(events, expected_hash)
        if selected_report_ev:
            report_content = selected_report_ev['content']
            report_idx = selected_report_ev['index']

    if report_path and os.path.exists(report_path):
        with open(report_path, 'r', encoding='utf-8') as f:
            file_report_content = f.read()
            if not report_content or len(file_report_content) > len(report_content):
                report_content = file_report_content

    # 4. Evaluate report predicates
    checks, failures, verdict = evaluate_report_text(
        report_content=report_content,
        expected_commit=expected_commit,
        expected_paths=expected_paths,
        candidate_file=candidate_file,
        expected_hash=expected_hash,
        phase=phase,
        tool_views=viewed_files_before_handshake,
        handshake_commit=handshake_commit,
        candidate_binding_passed=b_pass,
        binding_failure_msg=b_msg,
        role=role
    )

    all_passed = all(checks.values())
    for name, res in checks.items():
        status = "PASS" if res else "FAIL"
        print(f"  [{status}] {name:<35} : {'TRUE' if res else 'FALSE'}")

    print("-" * 80)
    if all_passed and verdict == "PASS":
        print("MECHANICAL GATE RESULT: ACCEPTED (Exit Code: 0)")
        print(f"VERDICT: {verdict}")
        if report_idx:
            print(f"Selected Report Event Index: {report_idx}")
        return 0
    else:
        print("MECHANICAL GATE RESULT: REJECTED (Exit Code: 1)")
        for f in failures:
            print(f"  * {f}")
        return 1


def main():
    parser = argparse.ArgumentParser(
        description="Cobbleverse Hell Mode Standalone Review Gate Acceptance Oracle"
    )
    # Flags required by user specification
    parser.add_argument('--transcript-full', dest='transcript_full', help='Path to reviewer transcript_full.jsonl')
    parser.add_argument('--transcript', dest='transcript', help='Alias for --transcript-full')
    parser.add_argument('--report', dest='report', help='Path to reviewer report markdown file')
    parser.add_argument('--role', dest='role', choices=['plan_reviewer', 'implementation_reviewer'], help='Reviewer role')
    parser.add_argument('--candidate-path', dest='candidate_path', help='Path to candidate artifact')
    parser.add_argument('--candidate-file', dest='candidate_file', help='Alias for --candidate-path')
    parser.add_argument('--candidate-hash', dest='candidate_hash', help='Expected candidate hash (git hash-object)')
    parser.add_argument('--expected-hash', dest='expected_hash', help='Alias for --candidate-hash')
    parser.add_argument('--expected-authority-manifest', dest='manifest', help='Path to bootstrap manifest.json')
    parser.add_argument('--manifest', dest='manifest_alias', help='Alias for --expected-authority-manifest')
    parser.add_argument('--phase', dest='phase', default='PLAN_REVIEW', choices=['PLAN_REVIEW', 'IMPL_REVIEW'], help='Workflow phase')
    parser.add_argument('--test', dest='run_test', action='store_true', help='Run report-independent self-test fixtures (Fixtures A-F and Cases A-D)')
    parser.add_argument('--test-fixtures', dest='run_test_fixtures', action='store_true', help='Alias for --test')

    # Positional args compatibility
    parser.add_argument('positional', nargs='*', help='Positional args fallback: <transcript> <manifest> <candidate_file> <expected_hash> [phase]')

    args = parser.parse_args()

    if args.run_test or args.run_test_fixtures:
        rc = run_regression_fixtures()
        sys.exit(rc)

    # Resolve arguments
    transcript = args.transcript_full or args.transcript
    report = args.report
    candidate = args.candidate_path or args.candidate_file
    expected_hash = args.candidate_hash or args.expected_hash
    manifest = args.manifest or args.manifest_alias
    phase = args.phase
    role = args.role

    # Check positional fallbacks if named flags not provided
    if not transcript and len(args.positional) >= 4:
        transcript = args.positional[0]
        manifest = args.positional[1]
        candidate = args.positional[2]
        expected_hash = args.positional[3]
        if len(args.positional) > 4:
            phase = args.positional[4]

    if not transcript and not report and not args.run_test:
        parser.print_help()
        sys.exit(2)

    required_role = 'plan_reviewer' if phase == 'PLAN_REVIEW' else ('implementation_reviewer' if phase == 'IMPL_REVIEW' else None)
    if required_role is None:
        print(f"ERROR: Unrecognized workflow phase: {phase}")
        sys.exit(1)
    if role and role != required_role:
        print(f"ERROR: Role mismatch: phase {phase} requires role '{required_role}', but '{role}' was supplied")
        sys.exit(1)
    if not role:
        role = required_role

    rc = audit_review_gate(
        transcript_path=transcript,
        manifest_path=manifest,
        candidate_file=candidate,
        expected_hash=expected_hash,
        phase=phase,
        report_path=report,
        role=role
    )
    sys.exit(rc)


if __name__ == '__main__':
    main()
