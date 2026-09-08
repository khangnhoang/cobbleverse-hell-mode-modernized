# Workstream Plan: Agent Architecture Redesign (Phase 2 — Owner-Review Correction Iteration 4)

## Status & Ownership

| Field | Value |
| :--- | :--- |
| **Workstream ID** | `agent-architecture-redesign` |
| **Document Role** | Candidate Workstream Plan (Ready for Independent Plan Review) |
| **Baseline Commit** | `1fa1d58` (`feat/agent-architecture-redesign`) |
| **Target Branch** | `feat/agent-architecture-redesign` |
| **Canary Lineage** | `2a329a5` (plan freeze), `8ee3d27` (implementation), `17c9e79` (canary verification) on `fix/weight-based-move-damage` |
| **Author Submission State** | `Candidate Plan (Ready for Review)` |

---

## 1. Context & Problem Statement

In Phase 1 of this workstream, `cobbleverse-hell-mode-modernized` established `AGENTS.md` as the repository Single Source of Truth (SSOT), modularized the multi-agent workflow into specialized skills, and demonstrated execution on the `fix/weight-based-move-damage` canary (`2a329a5`, `8ee3d27`, `17c9e79`).

Following Owner Review Correction Iteration 4, the previous Implementation Review PASS was **rejected**. While the architecture has progressed from prose-only governance toward operational obligations, execution exposed three deeper structural flaws:
1. `declared predicate != mechanically evaluated predicate`
2. `withheld candidate coordinates != physically inaccessible candidate`
3. `current closure size != generic closure semantics`

This correction applies **Subtraction-First** and **Execution-Mechanics-First** discipline:
- Eliminates self-attestation loopholes by establishing an independent, deterministic mechanical gate evaluated by Main Controller prior to semantic judgment.
- Eliminates false claims of "physical isolation" by defining the honest capability boundary of Antigravity subagents and codifying an observable protocol guarantee with contractual information withholding.
- Eliminates hardcoded instance constants (e.g. `14`, `expected_closure_14`) by establishing `bootstrap_governance_manifest` as the sole generic owner of expected authority closure.

### The 3 Required Architectural Defects

1. **[Required Finding A] Main Acceptance Predicate is Still Self-Attested:**
   - *Counterexample Observed:* An IR report stated `zero_proscribed_absolutes == TRUE`, but simultaneously contained `"Tuân thủ 100% kế hoạch..."`. Main accepted the report because it trusted the reviewer's self-attestation line.
   - *Core Defect:* The review acceptance predicate was defined as an abstract mathematical formula in prose, leaving Main without mechanical execution primitives. Main conflated declared reviewer strings with verified facts.
   - *Required Correction:* Decouple review acceptance into:
     - **Stage 1 (Mechanical Gate Check):** Main independently evaluates raw report text against deterministic structural criteria (verdict header, boot handshake record in session history, manifest-derived authority inclusion, required markdown sections, 4-part falsification blocks, and narrow absolute phrase scan). Self-attestation strings (e.g. `foo == TRUE`) are completely ignored.
     - **Stage 2 (Semantic Reviewer Judgment):** Main interprets substantive review findings only after the mechanical gate evaluates to true.
     - *Calibrated Claim Discipline:* Distinguish measurable exact numeric counts (e.g. `14/14 files consumed`, `0 broken links`, exact hashes) from unmeasured semantic absolutes (`"hoàn toàn"`, `"triệt để"`, `"guarantees"`, `"flawless"`, `"không có rủi ro"`, `"zero risk"`, `"tuyệt đối"`, `"completely closes"`, or unsupported `"100% tuân thủ"`).
     - *Explicit Failure Behavior:* If mechanical gate fails -> verdict is `INVALID` -> state does NOT advance -> zero git commits -> controller remains in review phase. The SAME reviewer repairs report-only defects in session without consuming reconciliation cycles.

2. **[Required Finding B] Two-Phase Boot Overclaims Physical Isolation:**
   - *Counterexample Observed:* The contract claimed candidate anchors are withheld during `REVIEWER_BOOT` and therefore candidate access is "physically impossible" / "physically guaranteeing temporal ordering".
   - *Core Defect:* In Antigravity, subagents inherit workspace read access (`Workspace: 'inherit'`). Read tools (`view_file`, `grep_search`, `find_by_name`) have unconstrained read access to the workspace directory. There is no capability filter or OS-level sandbox isolating `scratch/bootstrap-governance/` from repository working tree files.
   - *Required Correction:* Reject false guarantees. Acknowledge that capability-level sandbox isolation does not exist. Codify the strongest truthful guarantee available:
     - **Contractual Information Withholding:** Main Controller withholds candidate file paths, hashes, and diffs from Partition 1/3 prompts during `REVIEWER_BOOT`.
     - **Contractual Behavioral Boundary:** The prompt contract strictly forbids searching, listing, or viewing repository working-tree candidate files prior to emitting `AUTHORITY_LOADED`.
     - **Observable Protocol Guarantee:** Main audits the emitted `AUTHORITY_LOADED` handshake before dispatching Partition 2 (candidate anchors) via `send_message`. Main can audit tool calls in the subagent transcript to verify no candidate reads preceded the handshake.

3. **[Required Finding C] Authority Closure Must Be Manifest-Derived:**
   - *Counterexample Observed:* Architecture codified `expected_closure_14`, `required_authority_closure_14`, and "all 14 files" throughout skills, state machine, and rubrics.
   - *Core Defect:* 14 is an instance value for this specific governance task at commit `1fa1d58`, not a generic architectural constant.
   - *Required Correction:* `bootstrap_governance_manifest` is the Single Source of Truth for expected authority closure:
     `expected_authority_closure = [entry.path for entry in bootstrap_governance_manifest.entries]`
     `authority_closure_complete(consumed_identities, bootstrap_governance_manifest.entries) := set(expected_authority_closure).issubset(set(consumed_identities))`
     The closure size is dynamic (`len(bootstrap_governance_manifest.entries)`). All hardcoded `14` constants are purged from generic workflow contracts.

### Bootstrap Governance Authority Invariant
Because this task modifies the governance system itself (`AGENTS.md` and `.agents/skills/`), **governance rules active at TASK START (`bootstrap_commit = 1fa1d58`) remain authoritative for this entire run.** Working-tree candidate governance files are untrusted review subjects and can never become authority for reviewing themselves. The materialized baseline files at `scratch/bootstrap-governance/` represent the immutable authority for this run.

---

## 2. Root-Invariant Map

| # | Invariant | Current Owner | Current Failure Mechanism | Smallest Correction | Obsolete Rule/Prose to Delete |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **1** | **Mechanically Evaluated Review Acceptance Gate:** Main acceptance of a review report is determined exclusively by Main's independent mechanical evaluation of structural artifacts and evidence, never by trusting reviewer self-attestation strings. | `managed-agent-workflow` (`SKILL.md`, `references/controller-state-machine.md`) | Predicate declared as abstract mathematical formula in prose. Main accepted report because it parsed reviewer self-declaration `zero_proscribed_absolutes == TRUE` while report contained `"Tuân thủ 100% kế hoạch..."`. | Split into Stage 1 (Mechanical Gate Check executed by Main) and Stage 2 (Semantic Judgment). Mechanical gate asserts verdict regex, session handshake record, manifest path inclusion, required sections, 4-part falsification blocks, and proscribed phrase scan. Self-attestation lines ignored. Failure -> verdict `INVALID`, state holds, zero commits. | Reviewer self-attestation checklist (`zero_proscribed_absolutes: TRUE/FALSE`). Prose implying Main trusts reviewer lines for predicate booleans. |
| **2** | **Semantic Claim Calibration vs Unsupported Absolutes:** Claim strength must not exceed evidence strength. Factual numeric counts are distinguished from unsupported semantic absolutes. | `code-review-and-quality` (`SKILL.md`, rubrics) & repository-wide | Blanket token ban on `"100%"` was brittle (collided with legitimate ratios like `14/14 (100%)`), while unmeasured semantic absolutes escaped detection via self-attestation. | Proscribe narrow unsupported semantic absolutes (`"hoàn toàn"`, `"triệt để"`, `"guarantees"`, `"flawless"`, `"tuyệt đối"`, `"zero risk"`, `"completely closes"`, and unmeasured semantic claims like `"100% tuân thủ"`). Allow verified numeric counts and ratios with explicit denominators. Main mechanically scans report text. | Blanket token ban on `"100%"` regardless of context. Reviewer self-audit checkbox masquerading as verification. |
| **3** | **Truthful Reviewer Boot Protocol vs Overclaimed Physical Isolation:** Candidate artifacts are withheld during boot, and temporal precedence is governed by observable protocol handshake, without claiming nonexistent sandbox capability isolation. | `managed-agent-workflow` (`SKILL.md`, `references/controller-state-machine.md`) & `code-review-and-quality` (`SKILL.md`) | Architecture claimed candidate anchors are "physically withheld" and candidate reading is "physically impossible", overclaiming capability boundaries when subagents inherit workspace read tools. | Honest protocol model: (1) Main withholds candidate anchors from boot prompt; (2) prompt contract forbids working-tree candidate inspection prior to handshake; (3) Main validates `AUTHORITY_LOADED` before dispatching Partition 2; (4) transcript auditable. | All claims of `"physical isolation"`, `"physically withheld"`, `"capability isolation"`, and `"physically guaranteeing temporal ordering"`. |
| **4** | **Manifest-Derived Authority Closure:** Expected authority closure is uniquely owned and dynamically resolved from `bootstrap_governance_manifest.entries` at task start. | `bootstrap_governance_manifest` in `scratch/bootstrap-governance/manifest.json` | Instance value of 14 baseline files for this task was hardcoded into generic skills, state schema, and predicate definitions (`expected_closure_14`, `required_authority_closure_14`). | Generic contract: `expected_authority_closure = [e.path for e in manifest.entries]`. Completeness verified by set inclusion `set(manifest.entries) ⊆ set(report.consumed)`. 14 retained solely as instance count for this run. | Magic constants `expected_closure_14`, `required_authority_closure_14`, and generic rules stating "reviewers always read 14 files". |
| **5** | **Counterexample & Falsification Review:** Independent reviewers must actively attempt to falsify candidate claims through concrete counterexamples, failure traces, and defense evaluations across every dimension. | `code-review-and-quality` (`SKILL.md`, rubrics) | Passive checklist-style rubric items; ungrounded high-level evaluations; generic approvals lacking adversarial testing. | Mandate 4-part structure (`Target Invariant`, `Counterexample Attempted`, `Execution Trace`, `Result / Defense`) for every evaluated dimension. | Passive rubric checklists and superficial approval patterns. |
| **6** | **Materialized Bootstrap Governance Authority:** All governance authority used during a run resolves exclusively from materialized baseline files in `scratch/bootstrap-governance/` at `bootstrap_commit`. | `managed-agent-workflow` (`SKILL.md`, `references/controller-state-machine.md`) | Unmaterialized git blob references in reviewer prompts that read-only reviewers could not inspect; circular evaluation against working-tree candidate files. | Baseline files materialized to `scratch/bootstrap-governance/` with `manifest.json`. Reviewer prompt points to concrete local paths. | Abstract git revision paths (`<commit>:path`) in reviewer prompt contracts. |
| **7** | **Single Identity & Git Commit Owner:** Main Controller owns runtime identity, candidate hashing (`git hash-object`), session tracking, state transitions, manifest assembly, and all local checkpoint commits. Authors and reviewers never execute git commits or compute hashes. | `managed-agent-workflow` (`SKILL.md`, `references/reconciliation-protocol.md`) | Author commit instructions in reconciliation protocol; ambiguous manifest assembly prohibitions during Phase 5; reviewer hash computation steps. | Sole Main ownership codified in all skills. Authors and reviewers operate without write tools or commit permissions. | Author commit instructions in `reconciliation-protocol.md`; reviewer hashing instructions in rubrics. |
| **8** | **Orthogonal Verification Sets & Structural vs Semantic Authority:** Verification layers form an orthogonal set of domain authorities directly covering affected diffs. Automated Layer 0 checks are authoritative solely for structural syntax; semantic correctness belongs to independent contract review. | `test-and-verification-strategy` (`SKILL.md`, `references/verification-layers-and-tooling.md`) & `AGENTS.md` | Ascending ladder diagram (`▲`); "Hierarchy" title; conflating Layer 0 structural automation with semantic governance correctness. | Replace ladder with Orthogonal Domain Matrix. Explicitly state Layer 0 verifies structural markdown syntax only, not semantic governance validity. | Ascending ladder diagram; "Hierarchy" terminology; claiming Layer 0 proves semantic correctness. |
| **9** | **Lean Root Policy & SSOT:** `AGENTS.md` is the lean root SSOT for universal principles, preflight, mode routing, authority boundaries, and permissions (< 250 lines); detailed procedural mechanics are owned exclusively by delegated skills. | `AGENTS.md` | Procedural git staging commands (`git add <file>`), diff procedures, commit templates, and detailed test command listings in `AGENTS.md`. | Strip procedural git and test commands from `AGENTS.md`. Maintain line budget < 250 lines (target ~210–225 lines). | Procedural git staging commands; 6-layer test command tables; detailed commit lifecycle text in `AGENTS.md`. |
| **10** | **Decoupled Task-Specific Implementation Checkpoints:** Implementation checkpoints are derived dynamically from affected contracts and architectural slices, rather than forcing rigid Java-centric checkpoints. | `implementation-planning-and-contract-freeze` (`references/workstream-plan-template.md`) | Rigid "Core Logic / Model Implementation" and "Integration Verification" checkpoints hardcoded in generic plan templates. | Generic template derives checkpoints from affected architectural slices and verification gates. | Hardcoded code-centric checkpoints in generic plan template. |
| **11** | **Total Reviewer Outcome Algebra:** Review outcomes are total over `{ PASS, BLOCKING_FINDINGS, BLOCKED }` for every review phase, with deterministic transitions, prerequisite repair boundaries, and explicit audit verdicts. | `managed-agent-workflow` (`references/controller-state-machine.md`) & `code-review-and-quality` (`SKILL.md`) | Partial state machine transitions missing `BLOCKED`; `REJECTED_APPROACH` pseudo-verdict; unhandled review blockers. | Codify total 3-verdict algebra with deterministic routing for each outcome and explicit Prerequisite Repair Protocol. | Pseudo-verdicts (`REJECTED_APPROACH`); missing `BLOCKED` branches. |
| **12** | **Static Candidate Plan Contract:** Workstream plans are immutable candidate contracts fixed at author handoff identity with static `Author Submission State: Candidate Plan (Ready for Review)`; dynamic state belongs to runtime memory and git commits. | `implementation-planning-and-contract-freeze` (`references/workstream-plan-template.md`) | Dynamic status headers (`Frozen`, `Cycle 1`); in-place modification of plan status after freeze; hash invalidation caused by progress updates. | Plan document status header remains static `Candidate Plan (Ready for Review)`. Freeze state tracked by Main Controller via `frozen_plan_hash` and git commit. | In-place editing of plan status headers; mutable plan lifecycle fields. |
| **13** | **Explicit Internal Completion Semantics:** The `COMPLETED` controller state explicitly represents internal multi-agent workflow completion, parking at Owner Review Gate for Owner review and disposition. | `managed-agent-workflow` (`references/controller-state-machine.md`) | Ambiguity over whether `COMPLETED` implies remote release or external promotion. | Codify that `COMPLETED` is internal completion parking at Owner Review Gate. Remote delivery requires separate explicit Owner authorization. | Ambiguous completion definitions conflating internal completion with external promotion. |

---

## 3. Subtraction Report (Subtraction First)

### 3.1 Rules Removed
1. **Self-Attestation Acceptance Rule:** Removed rule permitting Main Controller to evaluate acceptance conditions from reviewer self-declaration lines (`zero_proscribed_absolutes == TRUE`). Replaced with independent mechanical gate evaluation.
2. **Blanket Word Blacklist on `"100%"`:** Removed indiscriminate token ban on `"100%"` that invalidated legitimate factual measurements (e.g. `14/14 (100%)`). Replaced with semantic claim calibration distinguishing verified numeric ratios from unmeasured semantic absolutes.
3. **Pseudo-Reconciliation for Formatting Defects:** Removed rule incrementing reconciliation cycle count on trivial report formatting defects. Report-only repairs are handled within session without penalizing the reconciliation budget.
4. **Single-Shot Reviewer Prompting:** Removed single-shot invocation passing authority, candidate, and rubrics simultaneously.
5. **Presentation-Order Header Assumption:** Removed assumption that static report headers prove execution order.

### 3.2 Duplicate Statements Removed
1. **Hardcoded Authority Closure Enumeration:** Removed duplicate 14-file lists hardcoded across `controller-state-machine.md`, `SKILL.md`, rubrics, and plan template. Replaced with single source of truth reference to `bootstrap_governance_manifest`.
2. **Duplicated Acceptance Predicate Definitions:** Consolidated scattered acceptance criteria into a single formal specification in `controller-state-machine.md` referenced by other skills.
3. **Procedural Duplications in Root SSOT:** Removed procedural git commands and test tables from `AGENTS.md`, consolidating them into their owning skills.

### 3.3 False Guarantees Weakened
1. **"Physical Isolation" / "Physical Impossibility":** Weakened from false claims of capability-level isolation to the truthful **"Contractual Information Withholding & Observable Boot Protocol"**. Antigravity subagents inherit workspace read access; physical isolation does not exist.
2. **"Zero Risk" / "Guaranteed Correctness":** Weakened hyperbolic claims to evidence-proportional statements reflecting Canary Lesson 7.

### 3.4 Magic Constants Removed
1. **`expected_closure_14`:** Removed hardcoded constant from acceptance predicate. Replaced with dynamic manifest evaluation `authority_closure_complete(report.consumed, bootstrap_governance_manifest.entries)`.
2. **`required_authority_closure_14`:** Removed hardcoded field from ephemeral state schema. Replaced with generic `bootstrap_governance_manifest` referencing.
3. **Fixed "14-File" Rules:** Purged generic rules declaring that all reviewers always read 14 files. 14 is documented strictly as the instance value for this run.

### 3.5 New Primitives Added, with Justification
1. **`evaluate_mechanical_gate(report_text, manifest, session_history)`:** Deterministic evaluation function executed independently by Main Controller. Evaluates 6 concrete mechanical checks on the raw report text before semantic review.
   *Justification:* Directly eliminates the self-attestation vulnerability exposed in Owner Iteration 4 where Main trusted reviewer prose over actual evidence.
2. **Two-Stage Review Acceptance Workflow:** Explicit sequence: `Mechanical Gate Check` -> `Semantic Judgment Review`.
   *Justification:* Prevents invalid reports from advancing controller state or triggering git commits.
3. **`repair_report_defects(defect_list)` Handshake:** In-session messaging protocol allowing the same reviewer to fix mechanical/report formatting defects without triggering state reconciliation or consuming cycle budget.
   *Justification:* Prevents workflow deadlock over presentation issues while maintaining strict gate standards.

---

## 4. Deep Architectural Resolution for Owner Findings

### 4.1 Subagent Capability Boundary Analysis (Finding B)
- **Environment Reality:** In the Antigravity subagent architecture:
  - Subagents are invoked via `invoke_subagent` or defined via `define_subagent`.
  - Read-only reviewers are configured with `enable_write_tools: false`, giving them `view_file`, `grep_search`, `find_by_name`, `list_dir`, `read_url_content`, `search_web`, and `send_message`.
  - Subagents inherit the parent's workspace (`c:\Users\khang\Downloads\Doctors Cobblemon`).
  - Read tools accept absolute paths across the entire workspace. There is no OS sandbox, chroot, or tool capability filter that can make `scratch/bootstrap-governance/` readable while making the working tree unreadable.
- **Truthful Contract:**
  - We explicitly reject the claim that candidate anchors are "physically withheld" or that reading candidates is "physically impossible".
  - We establish the strongest truthful guarantee available:
    1. **Information Withholding:** Main Controller does not supply candidate paths, hashes, diffs, or author claims in the initial `REVIEWER_BOOT` invocation prompt.
    2. **Contractual Behavioral Boundary:** The prompt contract explicitly instructs the reviewer to read ONLY `scratch/bootstrap-governance/` and the rubric. The reviewer is contractually forbidden from searching, listing, or reading working-tree candidate files prior to emitting `AUTHORITY_LOADED`.
    3. **Observable Handshake Audit:** Reviewer emits `AUTHORITY_LOADED` listing consumed authority files.
    4. **Sequential Dispatch:** Main validates `AUTHORITY_LOADED` before dispatching Partition 2 (candidate anchors) via `send_message`.
    5. **Transcript Auditability:** Main can inspect subagent transcript logs (`transcript.jsonl`) to verify that no candidate file was read prior to the `AUTHORITY_LOADED` handshake.

### 4.2 Mechanically Evaluated Review Acceptance Gate (Finding A)
- **Problem:** In Iteration 4, IR self-declared `zero_proscribed_absolutes == TRUE` while writing `"Tuân thủ 100% kế hoạch..."` in the report body. Main accepted the report because it trusted the reviewer's self-declaration.
- **Root Invariant:** Main acceptance is derived from independent mechanical evaluation of evidence, NEVER from reviewer self-attestation lines.
- **Operational Architecture:**
  Main Controller evaluates review reports through a two-stage gate:

  ```text
  Review Report Received
           │
           ▼
  [Stage 1: Mechanical Gate Check] (Deterministic by Main)
     1. has_valid_verdict_header(report)
     2. has_boot_handshake_record(session)
     3. authority_closure_complete(report, manifest)
     4. has_required_sections(report)
     5. has_4part_falsification_structure(report)
     6. mechanical_scan_clean(report)
           │
      ┌────┴────────────────────────┐
      │ Fails                       │ Passes
      ▼                             ▼
  Verdict INVALID            [Stage 2: Semantic Judgment Review]
  State Holds                Interpret Verdict: {PASS, BLOCKING_FINDINGS, BLOCKED}
  No Checkpoint Commit       Evaluate Finding Severities & Evidence
  Notify Reviewer to Repair  Advance State Machine
  ```

- **Detailed Mechanical Checks:**
  1. `has_valid_verdict_header(report)`: Report contains exactly one verdict header matching regex:
     `^(Plan|Implementation) Review Verdict:\s*(PASS|BLOCKING_FINDINGS|BLOCKED)$`
  2. `has_boot_handshake_record(session)`: Session history confirms `AUTHORITY_LOADED` was emitted and verified by Main prior to Partition 2 dispatch.
  3. `authority_closure_complete(report, manifest)`: Main extracts the list of consumed files from the report's `Proof of Authority Consumption` and verifies:
     `set(entry.path for entry in manifest.entries).issubset(set(report.consumed_files))`
     Any missing entry causes immediate mechanical gate failure.
  4. `has_required_sections(report)`: Report contains all required top-level section headers (`Proof of Authority Consumption`, `Invariant Falsification`, `Summary of Findings`).
  5. `has_4part_falsification_structure(report)`: For each evaluated invariant, the report contains non-empty:
     - `Target Invariant:`
     - `Counterexample Attempted:`
     - `Execution Trace:`
     - `Result / Defense:`
  6. `mechanical_scan_clean(report)`: Main mechanically scans raw report text for prohibited phrases:
     - Exact narrow banned phrases: `"hoàn toàn"`, `"triệt để"`, `"guarantees"`, `"flawless"`, `"không có rủi ro"`, `"zero risk"`, `"tuyệt đối"`, `"completely closes"`.
     - Contextual percentage claims: Semantic perfection claims like `"100% tuân thủ"`, `"100% an toàn"`, `"100% correct"`, or `"100% coverage of all risks"` are forbidden. Verifiable numeric counts and ratios with explicit denominators (e.g. `14/14 (100%)`, `100% pass rate (5/5 tests)`) are permitted.
     - *Rule:* Main ignores any reviewer line declaring `zero_proscribed_absolutes: TRUE/FALSE`. The scan is executed by Main.

- **Explicit Failure Behavior:**
  - If `evaluate_mechanical_gate(report) == false`:
    - The review verdict is declared `INVALID`.
    - Controller phase DOES NOT advance (`PLAN_REVIEW` or `IMPL_REVIEW` remains active).
    - ZERO checkpoint commits are executed.
    - Main dispatches a defect notice to the SAME reviewer session via `send_message`:
      `Mechanical Gate Failure: [list of failed checks, e.g. 'Proscribed phrase detected: \"Tuân thủ 100% kế hoạch\" in Section 3']. Self-attestation is ignored. Correct report text.`
    - If the defect is report formatting or phrasing only (semantics unchanged), the reviewer repairs the report within session. Reconciliation cycle count is NOT incremented.
    - If the defect reflects missing analysis or substantive findings, standard reconciliation rules apply.

- **Regression Trace (Iteration 4 Counterexample):**
  1. IR submits report containing `IR verdict: PASS`, all falsification blocks, line `zero_proscribed_absolutes: TRUE`, and text `"Tuân thủ 100% kế hoạch..."`.
  2. Main Controller executes `evaluate_mechanical_gate(report, manifest, session)`:
     - Verdict header: MATCH (`PASS`)
     - Handshake record: MATCH (verified earlier)
     - Authority closure: MATCH (all manifest entries present)
     - Required sections: MATCH
     - 4-part blocks: MATCH
     - Proscribed phrase scan: **FAIL** (detected `"Tuân thủ 100% kế hoạch"`)
  3. Main ignores `zero_proscribed_absolutes: TRUE`.
  4. Mechanical gate evaluates to `FALSE` -> Verdict is `INVALID`.
  5. State remains `IMPL_REVIEW`. Zero git commits executed.
  6. Main sends defect notice to IR. IR removes unsupported phrase. Main re-evaluates -> gate PASSES -> state advances.

### 4.3 Manifest-Derived Authority Closure (Finding C)
- **Problem:** Previous iterations hardcoded `14`, `expected_closure_14`, and `required_authority_closure_14` into generic contracts.
- **Root Invariant:** `bootstrap_governance_manifest` is the Single Source of Truth for expected authority closure.
- **Generic Specification:**
  - At task initialization (`MODE_3_SELECTED`), Main Controller snapshots `bootstrap_commit` and materializes baseline governance files into `scratch/bootstrap-governance/manifest.json`.
  - The manifest schema:
    ```json
    {
      "bootstrap_commit": "<SHA-1>",
      "entries_count": "<N>",
      "entries": [
        { "path": "<repo-relative-path>", "dest": "<scratch-path>", "blob_hash": "<SHA-1>" }
      ]
    }
    ```
  - Expected authority closure is dynamically defined as:
    `expected_authority_closure := [entry.path for entry in bootstrap_governance_manifest.entries]`
  - Closure completeness predicate:
    `authority_closure_complete(consumed_identities, bootstrap_governance_manifest.entries) :=`
    `set(expected_authority_closure).issubset(set(consumed_identities))`
  - Generic workflow skills, controller state schema, prompt contracts, and rubrics refer exclusively to `bootstrap_governance_manifest.entries`.
  - The value 14 is documented strictly as the instance value for this specific run at commit `1fa1d58`.

---

## 5. Scope Boundaries & Blast Radius

### 5.1 Strict In-Scope
- `AGENTS.md`: Lean SSOT streamlining, remove procedural git/test duplication, clarify Layer 0 structural vs semantic authority, maintain line count < 250 lines.
- `.agents/skills/managed-agent-workflow/`:
  - `SKILL.md`: Honest Two-Phase Boot protocol, Deterministic Mechanical Gate Evaluation, manifest-derived authority closure, sole Main commit ownership, Phase 5 manifest assembly, total 3-verdict model, calibrated claim discipline.
  - `references/controller-state-machine.md`: Ephemeral state schema (remove magic constants), Mechanical Gate Check (`evaluate_mechanical_gate`), two-stage review acceptance, honest boot prompt contract without physical isolation overclaims, dynamic closure validation, `COMPLETED` semantics at Owner Review Gate.
  - `references/reconciliation-protocol.md`: Align author response rules with Main commit ownership, report-only repair protocol, and mechanical gate failure handling.
- `.agents/skills/code-review-and-quality/`:
  - `SKILL.md`: Truthful Two-Phase Boot protocol (read manifest baseline, emit `AUTHORITY_LOADED`, await Partition 2), manifest-derived authority closure, Calibrated Claim Discipline (eliminate self-attestation, scan narrow absolutes), 4-part counterexample/falsification method.
  - `references/plan-review-rubric.md`: Require manifest entries in Proof of Authority Consumption, remove self-attestation checklist, enforce 4-part falsification blocks across all 6 dimensions.
  - `references/implementation-review-rubric.md`: Require manifest entries in Proof of Authority Consumption, remove self-attestation checklist, enforce 4-part falsification blocks across all 5 dimensions.
- `.agents/skills/test-and-verification-strategy/`:
  - `SKILL.md`: Orthogonal Verification Domain Matrix, Layer 0 structural authority vs semantic authority, calibrated claim discipline.
  - `references/verification-layers-and-tooling.md`: Standardize verification evidence path to `docs/workstreams/<id>/verification.md`, Layer 0 mechanical scan check, provenance freshness.
- `.agents/skills/implementation-planning-and-contract-freeze/`:
  - `SKILL.md`: Domain decoupling, sole Main hash ownership reference, calibrated claim discipline.
  - `references/workstream-plan-template.md`: Decouple rigid checkpoints into contract-derived checkpoints, static candidate header, reference manifest-derived authority closure.
  - `references/slicing-and-dependency-strategies.md`: Generic layer slicing patterns, calibrated claim discipline.
- `.agents/skills/git-checkpoint-workflow/`:
  - `SKILL.md`: Two-phase commit lifecycle, sole Main commit ownership, calibrated claim discipline.
  - `references/checkpoint-lifecycle-and-lineage.md`: Proportional commit templates, gate commits behind Mechanical Gate evaluation, preserve audit commit lineage.
- `docs/workstreams/agent-architecture-redesign/plan.md`: This candidate plan.

### 5.2 Explicit Out-of-Scope
- Zero modifications to Java code (`src/`), Cobblemon Mixins, or datapack JSONs (`data/`).
- Zero modifications to `competitive-pokemon-doubles-team-design` domain skill logic.
- Zero remote Git operations (`git push`, PR creation, remote branch manipulation).

---

## 6. Exact Files to Modify / Create / Delete

### Files to Modify

| Target File | Modifying Rationale & Scope | Related Findings |
| :--- | :--- | :--- |
| `AGENTS.md` | Lean SSOT streamlining; strip procedural git commands and detailed test matrices; define Layer 0 structural vs semantic authority; keep lines < 250 (target ~210–225). | Lean SSOT, Layer 0 Authority |
| `.agents/skills/managed-agent-workflow/SKILL.md` | Codify honest Two-Phase Boot protocol; codify Mechanical Gate Evaluation; enforce manifest-derived authority closure; clarify Phase 5 manifest assembly; confirm sole Main hash/commit ownership. | Findings A, B, C |
| `.agents/skills/managed-agent-workflow/references/controller-state-machine.md` | Ephemeral state schema without magic constants; add `evaluate_mechanical_gate` and two-stage review acceptance; honest boot prompt contract without physical isolation overclaims; dynamic manifest closure validation; `COMPLETED` semantics at Owner Review Gate. | Findings A, B, C |
| `.agents/skills/managed-agent-workflow/references/reconciliation-protocol.md` | Clarify author artifact correction vs Main commit ownership; add report-only defect repair protocol without cycle increment; align with mechanical gate. | Findings A, B |
| `.agents/skills/code-review-and-quality/SKILL.md` | Codify truthful reviewer Two-Phase Boot protocol (read manifest baseline, emit `AUTHORITY_LOADED`, await Partition 2); codify manifest-derived authority closure; eliminate self-attestation checklist; codify calibrated claim discipline; mandate 4-part counterexample/falsification method. | Findings A, B, C |
| `.agents/skills/code-review-and-quality/references/plan-review-rubric.md` | Require manifest entries in Proof of Authority Consumption; remove self-attestation checklist; require 4-part falsification blocks across all 6 dimensions. | Findings A, C |
| `.agents/skills/code-review-and-quality/references/implementation-review-rubric.md` | Require manifest entries in Proof of Authority Consumption; remove self-attestation checklist; require 4-part falsification blocks across all 5 dimensions. | Findings A, C |
| `.agents/skills/test-and-verification-strategy/SKILL.md` | Remove ascending ladder diagram (`▲`); replace with Orthogonal Verification Domain Matrix; clarify Layer 0 structural syntax vs semantic authority; calibrated claim discipline. | Layer 0 Authority, Anti-Overclaim |
| `.agents/skills/test-and-verification-strategy/references/verification-layers-and-tooling.md` | Standardize verification evidence path to `docs/workstreams/<id>/verification.md`; add Layer 0 mechanical scan check; provenance freshness. | Finding A, Evidence Path |
| `.agents/skills/implementation-planning-and-contract-freeze/SKILL.md` | Domain decoupling; sole Main hash ownership reference; calibrated claim discipline. | Calibrated Claim Discipline |
| `.agents/skills/implementation-planning-and-contract-freeze/references/workstream-plan-template.md` | Decouple rigid checkpoints into contract-derived checkpoints; static candidate header; reference manifest-derived authority closure. | Finding C, Checkpoint Decoupling |
| `.agents/skills/implementation-planning-and-contract-freeze/references/slicing-and-dependency-strategies.md` | Generic layer slicing patterns; calibrated claim discipline. | Calibrated Claim Discipline |
| `.agents/skills/git-checkpoint-workflow/SKILL.md` | Two-phase commit lifecycle; sole Main commit ownership; calibrated claim discipline. | Git Safety |
| `.agents/skills/git-checkpoint-workflow/references/checkpoint-lifecycle-and-lineage.md` | Proportional commit templates; gate commits behind Mechanical Gate evaluation; calibrated claim discipline. | Finding A, Git Safety |
| `docs/workstreams/agent-architecture-redesign/plan.md` | This candidate implementation plan. | Findings A, B, C |

### Files to Create / Delete
- **Zero new files to create** (all operational primitives cleanly housed within existing 5 skills and root contract).
- **Zero files to delete**.

---

## 7. Slicing & Implementation Strategy

Implementation proceeds across 3 surgical slices:

```text
Slice 1: Root Policy, State Machine & Mechanical Orchestration Gates
- AGENTS.md (Lean SSOT streamlining, strip procedural commands, Layer 0 structural definition)
- managed-agent-workflow (SKILL.md, controller-state-machine.md, reconciliation-protocol.md)
  [Integrates Finding A (evaluate_mechanical_gate & two-stage review acceptance),
   Finding B (honest boot protocol & removal of physical isolation overclaims),
   Finding C (manifest-derived closure & removal of magic constants)]
      │
Slice 2: Review Rubrics, Falsification Architecture, Manifest Closure & Calibrated Claim Discipline
- code-review-and-quality (SKILL.md, plan-review-rubric.md, implementation-review-rubric.md)
- implementation-planning-and-contract-freeze (SKILL.md, workstream-plan-template.md, slicing-and-dependency-strategies.md)
  [Integrates Finding A (elimination of self-attestation checklists & calibrated claims),
   Finding B (honest reviewer boot handshake contract),
   Finding C (manifest entry consumption in Proof of Authority),
   and 4-part adversarial falsification rubrics]
      │
Slice 3: Verification Strategy, Orthogonal Sets, Git Lineage & Layer 0 Automated Gates
- test-and-verification-strategy (SKILL.md, verification-layers-and-tooling.md)
- git-checkpoint-workflow (SKILL.md, checkpoint-lifecycle-and-lineage.md)
  [Integrates Finding A (Layer 0 mechanical scan check & commit gating on mechanical pass),
   Orthogonal Verification Domain Matrix, and preserved audit lineage]
```

### 7.1 Detailed Scope per Slice

#### Slice 1: Root Policy, State Machine & Mechanical Orchestration Gates
- **`AGENTS.md`:** Streamline Sections 6 and 7 to remove procedural commands and detailed test command tables; clarify Layer 0 structural vs semantic authority; maintain strict line budget (< 250 lines).
- **`managed-agent-workflow/SKILL.md`:**
  - Codify honest Two-Phase Boot protocol (sequential information release without physical isolation overclaims).
  - Codify Deterministic Mechanical Gate Evaluation (`evaluate_mechanical_gate`).
  - Codify manifest-derived authority closure (dynamic closure based on `bootstrap_governance_manifest.entries`).
- **`controller-state-machine.md`:**
  - Ephemeral state schema: remove `required_authority_closure_14`; reference `bootstrap_governance_manifest`.
  - State transitions: Codify Two-Stage Review Acceptance (`Mechanical Gate Check` -> `Semantic Judgment Review`).
  - Prompt contract: Partition 1 dynamically lists `bootstrap_governance_manifest.entries`; Partition 2 withheld at spawn and sent only via `send_message` after handshake verification. Remove all claims of "physical isolation".
  - Mechanical gate specification: Define concrete deterministic checks for verdict header, session handshake, manifest path inclusion, required sections, 4-part blocks, and proscribed phrase scan.
  - Completion semantics: Define `COMPLETED` at Owner Review Gate.
- **`reconciliation-protocol.md`:** Codify report-only defect repair protocol (in-session repair without cycle count increment) and align with Main commit ownership.

#### Slice 2: Review Rubrics, Falsification Architecture, Manifest Closure & Calibrated Claim Discipline
- **`code-review-and-quality/SKILL.md`:**
  - Codify honest reviewer Two-Phase Boot protocol (read manifest baseline, emit `AUTHORITY_LOADED`, await Partition 2).
  - Manifest-derived authority closure: Proof of Authority Consumption must verify all entries in `bootstrap_governance_manifest`.
  - Eliminate self-attestation checklist: Reviewers do not self-certify booleans (`zero_proscribed_absolutes == TRUE`).
  - Calibrated Claim Discipline: Define narrow unsupported absolutes; permit verified numeric ratios; mandate evidence-proportional language.
  - Mandate 4-part adversarial falsification structure (`Target Invariant`, `Counterexample Attempted`, `Execution Trace`, `Result / Defense`).
- **`plan-review-rubric.md`:** Require all manifest entries in Proof of Authority Consumption; remove self-attestation checklist; require 4-part falsification blocks across all 6 dimensions.
- **`implementation-review-rubric.md`:** Require all manifest entries in Proof of Authority Consumption; remove self-attestation checklist; require 4-part falsification blocks across all 5 dimensions.
- **`implementation-planning-and-contract-freeze/`:** Decouple checkpoints into contract-derived structure; maintain static candidate header; reference manifest-derived authority closure.

#### Slice 3: Verification Strategy, Orthogonal Sets, Git Lineage & Layer 0 Automated Gates
- **`test-and-verification-strategy/`:**
  - Replace ladder diagram with Orthogonal Verification Domain Matrix.
  - Define Layer 0 automated structural checks (link validation, schema checks, line limits, and proscribed phrase scan).
  - Standardize verification path to `docs/workstreams/<id>/verification.md`.
- **`git-checkpoint-workflow/`:**
  - Two-phase commit lifecycle (commit != audit promotion).
  - Enforce that local checkpoint commits occur only after Main Controller verifies `evaluate_mechanical_gate(report) == true` and verdict `PASS`.
  - Preserved audit commit lineage.

---

## 8. Proportional Verification Plan (Layer 0 Structural Authority)

In accordance with the Orthogonal Verification Principle, this workstream modifies **only** Markdown governance documentation and skill definitions. Zero Java classes, zero Mixin bytecode, and zero datapack JSONs are modified.

Therefore, **Layer 0 is the sole applicable automated repository check**:

### Automated Verification Checks (Layer 0)
1. **Skill Route Integrity Check:** Verify all skill routes referenced in `AGENTS.md` Section 5 resolve to valid files.
2. **Internal Reference Link Integrity Check:** Verify all relative markdown links in `references/*.md` resolve to existing files.
3. **YAML Frontmatter Schema Validation:** Verify all `SKILL.md` files contain valid YAML frontmatter (`name` matching directory, non-empty `description`).
4. **File Line Count Bounds Check:**
   - Verify `AGENTS.md` is strictly < 250 lines (target ~210–225 lines).
   - Verify all other `SKILL.md` and reference files remain < 500 lines.
5. **Git Diff Confinement Audit:** `git diff --stat` confirms modifications are strictly confined to `AGENTS.md`, `.agents/skills/`, and `docs/workstreams/agent-architecture-redesign/plan.md`. Zero drift in `src/` or `data/`.
6. **Proscribed Absolute Phrase Scan:**
   Execute a deterministic text scan across candidate files verifying zero occurrences of narrow unsupported absolutes outside of explicit definition/proscription blocks:
   - Proscribed phrases: `"hoàn toàn"`, `"triệt để"`, `"guarantees"`, `"flawless"`, `"không có rủi ro"`, `"zero risk"`, `"tuyệt đối"`, `"completely closes"`, `"100% tuân thủ"`.
7. **Manifest Hash & Completeness Verification:**
   Verify that all entries in `scratch/bootstrap-governance/manifest.json` exist and match their recorded blob hashes.

### Semantic Authority Distinction
- Automated Layer 0 structural checks verify syntactic integrity only. They do **not** prove authority correctness, routing correctness, state-machine closure, or invariant preservation.
- Semantic correctness is established through independent adversarial review by Plan Reviewer `R` and Implementation Reviewer `IR` evaluating the candidate architecture against the materialized baseline at `scratch/bootstrap-governance/`.

### Inapplicable Higher Layers (Explicitly Skipped per Orthogonal Selection)
- **Layer 1 (`validate_repo.py`):** Skipped (0 datapack JSONs modified).
- **Layer 2 (`./gradlew test`):** Skipped (0 Java classes modified).
- **Layer 3 (`test_rct_runtime_contract.py`):** Skipped (0 Mixins or bytecode modified).
- **Layer 4 (`./gradlew runServer`):** Skipped (no server runtime bootstrap risk).
- **Layer 5 (Canary Host):** Skipped (no live gameplay changes).

---

## 9. Task-Specific Implementation Checkpoints & Review Lifecycle

Implementation checkpoints derived from affected governance contracts:

- **Checkpoint 1: Plan Freeze Checkpoint (Current Gate)**
  - Planner `P` submits candidate plan with semantic handoff `{ candidate_path, ready: true }`.
  - Main Controller computes `candidate_plan_hash`.
  - **Honest Reviewer Boot (Phase 1):** Main Controller invokes Plan Reviewer `R` with Partition 1 (materialized manifest baseline in scratch) and Partition 3 (Plan Review Rubric). Partition 2 is WITHHELD. Reviewer is contractually forbidden from repository working-tree discovery.
  - Plan Reviewer `R` reads all manifest baseline entries, verifies `manifest.json`, and emits `AUTHORITY_LOADED` listing consumed manifest paths and commit `1fa1d58`.
  - Main Controller verifies the handshake.
  - **Review Active (Phase 2):** Main dispatches Partition 2 (candidate plan path and `candidate_plan_hash`) to `R` via `send_message`.
  - Plan Reviewer `R` evaluates candidate plan using the 4-part counterexample/falsification method and emits report.
  - **Main Gate Evaluation:** Main evaluates `evaluate_mechanical_gate(report)`.
    - If false -> verdict is `INVALID`, state holds, R repairs report defects.
    - If true and `R verdict: PASS` -> Main recomputes post-review hash, asserts equality, records `frozen_plan_hash`, and creates the Plan Freeze Checkpoint.
- **Checkpoint 2: Surgical Implementation of Slices 1–3**
  - Implementor `I` applies changes for Slices 1, 2, and 3.
- **Checkpoint 3: Proportional Verification & Manifest Assembly**
  - Implementor executes Layer 0 structural checks and records outputs in `docs/workstreams/agent-architecture-redesign/verification.md`.
  - Main Controller assembles Verification Evidence Manifest.
- **Checkpoint 4: Implementation Review Gate & Verified Implementation Checkpoint**
  - **Honest Reviewer Boot (Phase 1):** Main Controller invokes Implementation Reviewer `IR` with Partition 1 (materialized manifest baseline) and Partition 3 (Implementation Review Rubric). Partition 2 is WITHHELD.
  - Reviewer `IR` reads manifest entries, verifies `manifest.json`, and emits `AUTHORITY_LOADED`.
  - Main Controller verifies the handshake.
  - **Review Active (Phase 2):** Main dispatches Partition 2 (working-tree diff and Verification Evidence Manifest) to `IR` via `send_message`.
  - Reviewer `IR` evaluates implementation using the 4-part counterexample/falsification method and emits report.
  - **Main Gate Evaluation:** Main evaluates `evaluate_mechanical_gate(report)`.
    - If false -> verdict is `INVALID`, state holds, IR repairs report defects.
    - If true and `IR verdict: PASS` -> Main creates the Verified Implementation Checkpoint and parks in `COMPLETED` awaiting Owner disposition at Owner Review Gate.

---

## 10. Residual Risks & Mitigation Safeguards

| Risk Category | Assessed Risk Level | Mitigation Safeguard |
| :--- | :--- | :--- |
| **Self-Attestation Bypass** | Mitigated | Independent mechanical evaluation (`evaluate_mechanical_gate`) by Main scans raw report text; self-attestation lines are completely ignored. |
| **Overclaimed Capability Isolation** | Mitigated | Honest protocol model: sequential information release and auditable boot handshake replace false claims of physical isolation. |
| **Hardcoded Instance Constants** | Mitigated | Dynamic manifest derivation: expected authority closure resolved directly from `bootstrap_governance_manifest.entries`. |
| **Out-of-Order Candidate Evaluation** | Mitigated | Partition 2 candidate anchors withheld until after `AUTHORITY_LOADED` is emitted and validated by Main Controller; transcripts auditable. |
| **Reviewer Overclaim & Self-Refutation** | Mitigated | Proscribed absolute phrases mechanically scanned; semantic claims calibrated to evidence; reports with forbidden absolutes fail mechanical gate. |
| **Passive Reviewer Approvals** | Mitigated | Rubrics mandate 4-part counterexample and falsification structure for all evaluation dimensions. |
| **Reviewer Privilege Bleed** | Controlled | Reviewers configured with `enable_write_tools: false` and zero terminal commands; Main Controller owns all hash computations and git commits. |
| **State Machine Deadlock on Blocker** | Controlled | Total 3-verdict algebra with explicit Prerequisite Repair Protocol and in-session report defect repair. |
| **Premature Audit Immutability** | Mitigated | Two-phase commit model distinguishes provisional local coordination checkpoints from promoted immutable audit milestones. |
| **SSOT Drift & Documentation Bloat** | Mitigated | `AGENTS.md` line budget strictly enforced (< 250 lines); procedural git and test mechanics delegated to specialized skills. |

---

## 11. Candidate Plan Handoff & Review Readiness

Planner `P` has completed substantive discovery, applied the Subtraction First discipline, and authored this candidate implementation plan addressing the 3 Required Architectural Defects of Owner Review Correction Iteration 4.

- **Canonical Plan Path:** `docs/workstreams/agent-architecture-redesign/plan.md`
- **Handoff State:** Semantic candidate handoff `{ candidate_path: "docs/workstreams/agent-architecture-redesign/plan.md", ready: true }`.
- **Status:** Ready for Main Controller to compute `candidate_plan_hash` and initiate the Honest Two-Phase Reviewer Boot Handshake with Plan Reviewer `R` citing the materialized baseline at `scratch/bootstrap-governance/`.
