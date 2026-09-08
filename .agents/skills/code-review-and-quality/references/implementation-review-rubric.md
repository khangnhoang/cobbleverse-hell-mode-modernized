# Authoritative Implementation Review Rubric for Implementation Reviewer (IR)

This reference defines the authoritative evaluation rubric, verification dimensions, and verdict criteria used by Implementation Reviewer `IR` during Phase 5 of Mode 3 in Cobbleverse Hell Mode.

---

## 1. Implementation Review Preflight & Authority Closure Handshake

### 1.1 Two-Phase Reviewer Boot Handshake (Honest Protocol & Observable Gate)
To ensure candidate diffs and test logs cannot be evaluated prior to consuming authoritative baseline rules:
- **Phase 1 (`REVIEWER_BOOT`):** Implementation Reviewer `IR` is invoked with Partition 1 (Materialized Authority Closure in `scratch/bootstrap-governance/`) and Partition 3 (Authoritative Rubric). **Partition 2 (Working-Tree Diff & Evidence Manifest) is contractually WITHHELD.** Reviewer is contractually forbidden from searching, listing, or reading working-tree candidate files prior to emitting `AUTHORITY_LOADED`. Reviewer reads all baseline files in `bootstrap_governance_manifest.entries` via `view_file`, verifies integrity against `scratch/bootstrap-governance/manifest.json`, and emits an explicit `## AUTHORITY_LOADED` handshake message via `send_message` back to Main Controller.
- **Phase 2 (`REVIEW_ACTIVE`):** Upon Main Controller verification of the handshake against `bootstrap_governance_manifest.entries`, Main dispatches Partition 2 via `send_message`. Reviewer evaluates the implementation against baseline authority and the approved frozen plan (`docs/workstreams/<id>/plan.md`), and emits the review report.

### 1.2 Manifest-Derived Authority Closure
When reviewing governance changes (`AGENTS.md` or `.agents/skills/`), working-tree governance files are untrusted review subjects. Baseline files at `scratch/bootstrap-governance/` represent the immutable authority closure. Expected authority closure is dynamically defined from `bootstrap_governance_manifest.entries`:
`expected_authority_closure := [entry.path for entry in bootstrap_governance_manifest.entries]`

> [!CAUTION]
> **Zero Omission Invariant:** Both `## AUTHORITY_LOADED` and the report's `### Proof of Authority Consumption` must enumerate and confirm all manifest entries (14 baseline files for this bootstrap commit). Omitting any manifest entry causes deterministic rejection by Main Controller via `audit_review_gate.py`.

### 1.3 Evidence Manifest Guidance & Candidate Anchors
Because Implementation Reviewer is strictly read-only (`enable_write_tools: false`, zero terminal commands):
1. **Evidence Manifest Guidance:** Main Controller provides a Verification Evidence Manifest containing working-tree status, tracked diffs, test logs, and command outputs.
2. **Untrusted Anchor Rule:** The manifest and author claims are **untrusted navigation hints**. You must independently inspect the actual modified files on disk using read-only tools (`view_file`, `grep_search`, `find_by_name`). Reviewers do not compute git hashes.
3. **Plan Anchor Check:** Compare the changes against the approved, frozen plan (`docs/workstreams/<id>/plan.md`).

---

## 2. Core Evaluation Dimensions (Adversarial Falsification & Review Completeness)

### 2.1 Review Completeness Invariant: `blocking_finding != automatic_end_of_review`
A review finding (even a Critical defect or blocking finding) does NOT terminate the review. Implementation Reviewer `IR` must evaluate EVERY material dimension that remains independently reviewable and aggregate all findings before issuing a final verdict. Early termination upon encountering a defect is strictly prohibited.

- **Prerequisite Block Exception:** Early termination of an individual dimension is permitted ONLY when a concrete prerequisite failure makes that specific dimension genuinely impossible to evaluate. In that case, the dimension must NOT be omitted; it must be marked `BLOCKED` and explicitly provide:
  - `blocked_dimension`: The name of the dimension that cannot be evaluated.
  - `blocking_dependency`: The concrete missing prerequisite or broken dependency preventing evaluation.
  - `why further evaluation is impossible`: Precise technical explanation of why evaluation cannot proceed.
- **Generic Failure Insufficient:** An existing `FAIL` or `BLOCKING_FINDINGS` in one dimension is NEVER sufficient reason to abort or omit other independently reviewable dimensions.
- **Correction Delta Inspection Protocol for Candidate $H_2$:** When evaluating a corrected candidate identity $H_2$ (succeeding $H_1$), `IR` must substantively inspect:
  1. The $H_1 \to H_2$ correction delta;
  2. Every invariant directly affected by that delta;
  3. All remaining independently reviewable dimensions across the complete candidate file set.

### 2.2 Per-Dimension Substantive Review Structure
Implementation Reviewer must evaluate all 5 core dimensions. Passive checklist inspection or selective truncation is prohibited. For each dimension, the reviewer must provide complete substantive coverage:
- **Dimension:** [Dimension Name]
- **Target Invariant:** Exact rule or invariant evaluated.
- **Counterexample / Falsification Attempt:** Concrete failure scenario, adversarial edge case, or evasion pattern formulated by the reviewer.
- **Candidate Evidence Actually Inspected:** Specific candidate files, line numbers, git diffs, or execution logs actually inspected.
- **Execution Trace:** Step-by-step trace through diff hunks, code paths, and test outputs evaluating candidate behavior.
- **Result / Defense:** "The implementation demonstrably blocks this counterexample." (If the defense fails, raise a structured `Critical` or `Required` finding).
- **Blocking Finding or Residual Limitation:** Structured defect finding or documented residual limitation.

If a dimension cannot be evaluated due to a concrete blocker, mark it `BLOCKED` with `blocked_dimension`, `blocking_dependency`, and rationale; never omit any dimension.

### Dimension 1: Frozen Plan Conformance
- **Target Invariant:** Implementor must execute the approved frozen plan without unauthorized deviations, omissions, or scope creep.
- **Counterexample Focus:** Architectural redesign during coding without owner approval; omitting planned unit tests; altered component responsibilities.
- **Evidence Defense Standard:** The diff implements the exact structures, contracts, and tests committed in the frozen plan.

### Dimension 2: Blast Radius & Surgical Scope
- **Target Invariant:** Every modified line must be directly justified by the approved task; zero opportunistic refactoring or collateral edits.
- **Counterexample Focus:** Opportunistic cleanup of adjacent code, bulk formatting changes, reordering unrelated imports, or editing unowned data files.
- **Evidence Defense Standard:** `git diff` confirms changes are confined strictly to owned files and lines.

### Dimension 3: Correctness, Robustness & Boundary Safety
- **Target Invariant:** Safe boundary handling (null-safety, math clamping, division-by-zero protection), public contract backward compatibility, and adherence to routed domain invariants.
- **Counterexample Focus:** Unhandled null battle contexts, unchecked casts, off-by-one errors, math overflow/underflow, or breaking public APIs.
- **Evidence Defense Standard:** Code inspects and validates bounds, includes guard conditions, and satisfies applicable repository contracts.

### Dimension 4: Verification Authenticity & False-Green Prevention
- **Target Invariant:** Verification evidence must be authentic, fresh, and directly exercise modified behavior without trivial or mocked-away assertions.
- **Counterexample Focus:** Mocked assertions validating mock behavior instead of real calculations, tests passing trivially without covering modified paths, missing boundary cases, or stale pre-modification evidence.
- **Evidence Defense Standard:** Raw test execution logs in the manifest demonstrate that modified paths are actively executed and assertions pass legitimately.

### Dimension 5: Orthogonal Verification Evaluation
- **Target Invariant:** Execute the minimal orthogonal set of verification layers directly covering affected contracts; automated Layer 0 checks verify structural syntax only and do not prove semantic correctness.
- **Counterexample Focus:** Claiming verification using inapplicable higher suites, forcing vertical ladder execution, omitting required layer tests for modified subsystems, or claiming Layer 0 structural checks prove semantic governance correctness.
- **Evidence Defense Standard:** The manifest records clean execution of the minimal orthogonal set with valid exit codes and justifiable skips for unaffected layers.

---

## 3. Verdict Determination & Output Schema

### Verdict Rules
- Issue **`PASS`** if and only if there are **zero** `Critical` and **zero** `Required` findings. Emits explicit `## IR verdict: PASS`.
- Issue **`BLOCKING_FINDINGS`** if there is at least one `Critical` or `Required` finding.
- Issue **`BLOCKED`** if evidence manifest is incomplete, test logs are missing, or files cannot be inspected. Handled via the Prerequisite Repair Protocol.

### Review Report Calibrated Claim Discipline
Canary Lesson 7 applies strictly to review reports:
- **Proscribed Narrow Absolute Phrases:** The following phrases are strictly forbidden in report prose: `"hoàn toàn"`, `"triệt để"`, `"guarantees"`, `"flawless"`, `"không có rủi ro"`, `"zero risk"`, `"tuyệt đối"`, `"completely closes"`, or unsupported semantic perfection claims (e.g., `"100% tuân thủ"`).
- **Calibrated Claim Exception:** Verifiable numeric counts and ratios with explicit denominators (e.g. `14/14 (100%)`, `0 broken links`) are permitted.
- **Independent Mechanical Evaluation:** Main Controller mechanically scans report text via `audit_review_gate.py`. Self-attestation checkboxes and declarations are eliminated and ignored. Statements must be strictly proportional to observable, demonstrable evidence. Reports containing forbidden absolutes fail the mechanical gate.

### Review Report Schema
```markdown
# Implementation Review Report: <Task Title>

## IR verdict: PASS | BLOCKING_FINDINGS | BLOCKED

### Proof of Authority Consumption
- **Baseline Commit:** <bootstrap_commit>
- **Materialized Authority Path:** <scratch/bootstrap-governance/...>
- **Candidate Diff / Manifest Hash:** <candidate_hash>
- **Consumed Files (<entries_count>/<entries_count>):**
  [List of all consumed baseline files matching bootstrap_governance_manifest.entries]

### Dimension Evaluations (Adversarial Falsification & Review Completeness)

#### Dimension 1: Frozen Plan Conformance
- **Target Invariant:** <Exact invariant>
- **Counterexample / Falsification Attempt:** <Concrete failure scenario formulated by reviewer>
- **Candidate Evidence Actually Inspected:** <Specific candidate files, lines, or test outputs inspected>
- **Execution Trace:** <Step-by-step trace through diff and evidence>
- **Result / Defense:** The implementation demonstrably blocks this counterexample.
- **Blocking Finding or Residual Limitation:** [Structured finding or noted limitation]

[Repeat structure for Dimension 2: Blast Radius, Dimension 3: Correctness, Dimension 4: Verification Authenticity, and Dimension 5: Orthogonal Verification. If any dimension is genuinely blocked by prerequisite failure, state BLOCKED with blocked_dimension, blocking_dependency, and reason; do not omit.]

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
