# Authoritative Implementation Review Rubric for Implementation Reviewer (IR)

This reference defines the authoritative evaluation rubric, verification dimensions, and verdict criteria used by Implementation Reviewer `IR` during Phase 5 of Mode 3 in Cobbleverse Hell Mode.

---

## 1. Implementation Review Preflight & Evidence Guidance

Because Implementation Reviewer is strictly read-only (`enable_write_tools: false`, zero terminal commands):
1. **Evidence Manifest Guidance:** Main Controller provides a Verification Evidence Manifest containing working-tree status, tracked diffs, test logs, and command outputs.
2. **Untrusted Anchor Rule:** The manifest and author claims are **untrusted navigation hints**. You must independently inspect the actual modified files on disk using read-only tools (`view_file`, `grep_search`, `find_by_name`).
3. **Plan Anchor Check:** Compare the changes against the approved, frozen plan (`docs/workstreams/<id>/plan.md`).

---

## 2. Core Evaluation Dimensions

Implementation Reviewer must evaluate the working tree and verification evidence against five core dimensions:

### Dimension 1: Frozen Plan Conformance
- **Verification:** Did Implementor execute the exact frozen plan without unauthorized deviations or omissions?
- **Red Flags:** Architectural redesign during coding without owner approval; missing planned unit tests; altered class responsibilities.
- **Rubric Standard:** Implementation must match the approved plan in architecture, scope, and intent.

### Dimension 2: Blast Radius & Surgical Scope
- **Verification:** Are all modified and created files strictly within the approved task scope?
- **Red Flags:** Opportunistic cleanup, bulk formatting changes, reordering unrelated imports, editing unowned trainer JSONs, or modifying Loom cache files.
- **Rubric Standard:** Every modified line must be directly justified by the approved task.

### Dimension 3: Correctness, Robustness & Boundary Safety
- **Verification:**
  - Are null values handled safely without unintended NPEs?
  - Are mathematical formulas protected against division-by-zero, underflow, overflow, and clamped appropriately (e.g., base power bounds)?
  - Are Fabric Mixin injections cleanly returning or calling through without breaking target logic?
  - Are Fair-AI boundaries preserved (no hidden state leaks)?
- **Red Flags:** Unchecked casts, raw types, hardcoded magic values without explanation, leaking opponent moves.

### Dimension 4: Verification Authenticity & False-Green Prevention
- **Verification:** Do the test execution logs in the manifest prove that the change succeeds?
- **Red Flags:**
  - Mocked-away assertions where the test validates mock behavior rather than the real calculation;
  - Tests that pass trivially without exercising modified code paths;
  - Missing negative/boundary test cases for bug fixes;
  - Stale verification evidence that predates code modifications (violating Artifact Freshness Protocol).
- **Rubric Standard:** Verification evidence must be authentic, fresh, and directly exercise the modified behavior.

### Dimension 5: Architectural & Datapack Integrity
- **Verification:**
  - Did `python scripts/ci/validate_repo.py` pass with 0 errors across 1,714 trainer files?
  - Did `python scripts/runtime-contract/test_rct_runtime_contract.py` pass all bytecode checks?
  - Are Cobbleverse 1.7.42 trainer formats and economy rules strictly respected?

---

## 3. Verdict Determination & Output Schema

### Verdict Rules
- Issue **`PASS`** if and only if there are **zero** `Critical` and **zero** `Required` findings.
- Issue **`BLOCKING_FINDINGS`** if there is at least one `Critical` or `Required` finding.
- Issue **`BLOCKED`** if evidence manifest is incomplete, test logs are missing, or files cannot be inspected.

### Review Report Schema
```markdown
# Implementation Review Report: <Task Title>

## Verdict: PASS | BLOCKING_FINDINGS | BLOCKED

### Summary Evaluation
[Concise executive evaluation summarizing implementation quality, plan conformance, verification authenticity, and review verdict.]

### Findings

#### [Finding-01] [Severity]: <Title>
- **Location:** `<file-path>:<line-numbers>`
- **Problem:** <Explanation of defect or plan divergence>
- **Impact:** <Runtime, contract, or gameplay consequence>
- **Evidence:** <Cited code snippet, diff hunk, or test log>
- **Required Change:** <Minimal surgical fix>

[Additional findings as needed, or "Zero blocking findings."]
```
