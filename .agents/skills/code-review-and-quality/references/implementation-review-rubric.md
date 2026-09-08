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
- **Repo-Global Standards:**
  - Are null values handled safely without unintended NPEs?
  - Are mathematical formulas protected against division-by-zero, underflow, overflow, and clamped appropriately?
  - Is public contract backward compatibility maintained?
  - Are code changes surgical and free from opportunistic refactoring?
- **Routed Domain Standards:**
  - *Battle AI Tasks:* Fair-AI boundaries preserved (no hidden state leaks; Run & Bun AI respects information boundaries).
  - *Fabric Mixin Tasks:* Injections cleanly returning or calling through without breaking target logic; descriptors match bytecode.
  - *Trainer Datapack Tasks:* Cobbleverse 1.7.42 schema rules and economy constraints strictly respected; zero bag healing items.
  - *Doubles Team Design:* Coherent battle plan, weather/Trick Room/Tailwind synergy, Turn-1 gimmick safety per `competitive-pokemon-doubles-team-design`.
- **Red Flags:** Unchecked casts, raw types, hardcoded magic values without explanation, leaking opponent moves.

### Dimension 4: Verification Authenticity & False-Green Prevention
- **Verification:** Do the test execution logs in the manifest prove that the change succeeds?
- **Red Flags:**
  - Mocked-away assertions where the test validates mock behavior rather than the real calculation;
  - Tests that pass trivially without exercising modified code paths;
  - Missing negative/boundary test cases for bug fixes;
  - Stale verification evidence that predates code modifications (violating Artifact Freshness Protocol).
- **Rubric Standard:** Verification evidence must be authentic, fresh, and directly exercise the modified behavior.

### Dimension 5: Proportional Verification Evaluation
- **Verification:** Did the implementation execute all verification layers applicable to the change's risk profile and modified subsystems, as determined by `test-and-verification-strategy` and the approved frozen plan?
  - For Layer 0 changes (Markdown, governance, skills): Require Layer 0 suite (route resolution, link integrity, frontmatter schema validation, file line bounds < 500 lines).
  - For Layer 1 changes (datapacks): Require `validate_repo.py` and `check_legacy_baseline.py`.
  - For Layer 2 changes (Java logic): Require `./gradlew test`.
  - For Layer 3 changes (Mixins): Require `test_rct_runtime_contract.py`.
  - For Layer 4/5: Smoke/canary only when applicable.
- **Red Flags:** Claiming a change is verified using inapplicable tests; omitting required layer tests for modified subsystems; executing heavy suites when only documentation was modified.
- **Rubric Standard:** Verification must be strictly proportional to modified subsystems and risk boundaries.

---

## 3. Verdict Determination & Output Schema

### Verdict Rules
- Issue **`PASS`** if and only if there are **zero** `Critical` and **zero** `Required` findings. Emits explicit `## Verdict: PASS`.
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
