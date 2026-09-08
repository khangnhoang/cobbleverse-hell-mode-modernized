# Workstream Plan: Agent Architecture Redesign (Phase 2 — Owner-Review Correction Iteration 3)

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

Following Owner review of Architecture Phase 2 at baseline commit `1fa1d58` and Correction Iterations 1 & 2, the Owner established this **Correction Iteration 3 Gate** identifying 4 Required Operational Findings:

1. **[Required] Full Authority Closure Enforcement:**
   - A manifest mechanism alone does not prove execution. In previous review execution, Implementation Reviewer IR omitted 3 baseline files (`slicing-and-dependency-strategies.md`, `git-checkpoint-workflow/SKILL.md`, `checkpoint-lifecycle-and-lineage.md`).
   - The required baseline authority closure must be formally defined as the complete set of 14 baseline governance files for this task.
   - The consumption proof must verify that all 14 baseline files in the closure were actually read prior to candidate evaluation.
2. **[Required] Two-Phase Reviewer Boot Handshake (Enforcing Temporal Ordering):**
   - A static header at the top of a final report demonstrates only presentation layout, not temporal execution order (an unconstrained agent could inspect candidate diffs first and construct the header retroactively).
   - Codify the operational Two-Phase Reviewer Boot:
     * Phase 1 (`REVIEWER_BOOT`): Main invokes Reviewer with Partition 1 (Materialized Authority Closure in scratch) and Partition 3 (Rubric). Partition 2 (candidate files/diffs) is explicitly WITHHELD. Reviewer reads all 14 baseline files in the closure, verifies `manifest.json`, and emits an explicit `AUTHORITY_LOADED` handshake message listing all 14 consumed files and commit `1fa1d58`.
     * Phase 2 (`REVIEW_ACTIVE`): Main Controller verifies the handshake against the required authority closure. Upon verification, Main sends Partition 2 (Untrusted Candidate Anchors) to the SAME reviewer session via `send_message`. Reviewer evaluates candidates and emits the review report.
     * This enforces that reading candidate files cannot precede authority consumption.
3. **[Required] Review Report Anti-Overclaim Discipline:**
   - Anti-overclaim (Canary Lesson 7) applies to review reports as strictly as to code and plans.
   - Reviewer text must not contain proscribed absolutes (forbidden tokens: `"hoàn toàn"`, `"triệt để"`, `"100%"`, `"đảm bảo tính khép kín"`, `"guarantees"`, `"flawless"`).
   - A review report that uses forbidden absolutes is self-refuting. Reviewers must self-audit report outputs before submission.
4. **[Required] Deterministic Main Review Acceptance Predicate:**
   - Main Controller evaluates a formal boolean acceptance predicate on every review report before accepting the verdict or advancing state:
     `review_gate_accepts_report(report) iff:`
     1. Reviewer Boot Handshake was satisfied (`AUTHORITY_LOADED` received and verified before candidate dispatch).
     2. Full required authority closure was consumed (all 14 baseline files verified read).
     3. Every material invariant has a complete 4-part falsification block (`Target Invariant`, `Counterexample Attempted`, `Execution Trace`, `Result / Defense`).
     4. Zero proscribed hyperbolic/absolute terms in report.
     5. Verdict in `{ PASS, BLOCKING_FINDINGS, BLOCKED }`.
     If the predicate evaluates to false, the report is rejected and the controller cannot transition.

### Bootstrap Governance Authority Invariant
Because this task modifies the governance system itself (`AGENTS.md` and `.agents/skills/`), **governance rules active at TASK START (`bootstrap_commit = 1fa1d58`) remain authoritative for this entire run.** Working-tree candidate governance files are untrusted review subjects and can never become authority for reviewing themselves. The materialized baseline files at `scratch/bootstrap-governance/` represent the immutable authority for this run.

### Correction Philosophy: Subtraction First
The corrected architecture reduces conceptual ambiguity, eliminates ceremony, and closes semantic loopholes using the smallest set of strong invariants:
1. Identify the root invariant preventing each defect class.
2. Delete, consolidate, or simplify conflicting rules.
3. Do NOT create new states, fields, files, protocols, checklists, or exceptions unless existing primitives cannot express the invariant.
4. Remove obsolete rules after introducing stronger invariants.
5. Ensure exactly ONE owner remains for each fact and procedure.

---

## 2. Root-Invariant Map

| # | Root Invariant | Owning File / Skill | Redundant Rules, Duplications & Mechanics Removed |
| :--- | :--- | :--- | :--- |
| **1** | **Counterexample & Falsification Review:** Independent reviewers must actively attempt to falsify candidate claims through concrete counterexample scenarios and traces, rather than passive checklist inspection. | `code-review-and-quality` (`SKILL.md`, `plan-review-rubric.md`, `impl-review-rubric.md`) | Passive checklist-style rubric items; ungrounded high-level evaluations; generic approvals lacking adversarial testing. |
| **2** | **Materialized Bootstrap Governance Authority:** All governance authority used during a run resolves from materialized baseline files in `scratch/bootstrap-governance/` at `bootstrap_commit`. | `managed-agent-workflow` (`SKILL.md`, `controller-state-machine.md`) & `code-review-and-quality` (`SKILL.md`) | Unmaterialized git blob references in reviewer prompts that read-only reviewers could not inspect; circular evaluation against working-tree candidate files. |
| **3** | **Full Authority Closure Enforcement:** The baseline authority closure for governance modification comprises all 14 baseline files. Reviewers must consume every file in the closure; omitting any file invalidates the review. | `managed-agent-workflow` (`SKILL.md`, `controller-state-machine.md`) & `code-review-and-quality` (`SKILL.md`, rubrics) | Partial authority consumption; omitting references during review; unverified manifest existence without proof of read execution. |
| **4** | **Two-Phase Reviewer Boot Handshake:** Temporal precedence of authority over candidate evaluation is operationally enforced via two phases: Phase 1 (`REVIEWER_BOOT`) with Partition 2 withheld, verified `AUTHORITY_LOADED` handshake emission, transitioning to Phase 2 (`REVIEW_ACTIVE`) via `send_message`. | `managed-agent-workflow` (`SKILL.md`, `controller-state-machine.md`) & `code-review-and-quality` (`SKILL.md`) | Single-shot reviewer invocation allowing out-of-order candidate reads; presentation-order headers masquerading as execution-order enforcement. |
| **5** | **Review Report Anti-Overclaim Discipline & Self-Audit:** Reviewers must self-audit against and eliminate proscribed absolutes (`"hoàn toàn"`, `"triệt để"`, `"100%"`, `"đảm bảo tính khép kín"`, `"guarantees"`, `"flawless"`). Report claims must remain strictly proportional to evidence. | `code-review-and-quality` (`SKILL.md`, rubrics) & repository-wide | Unverified hyperbolic praise; self-refuting review reports containing forbidden absolute claims. |
| **6** | **Deterministic Main Review Acceptance Predicate:** Main Controller accepts review reports strictly when the formal boolean predicate `review_gate_accepts_report(report)` evaluates to true across all 5 conditions. | `managed-agent-workflow` (`SKILL.md`, `controller-state-machine.md`) | Informal reviewer report acceptance; skipping verification of reviewer preconditions; unvalidated review transitions. |
| **7** | **Single Identity & Git Commit Owner:** Main Controller owns runtime identity, candidate hashing (`git hash-object`), session tracking, state transitions, manifest assembly in Phase 5, and all local checkpoint commits. Authors and reviewers never execute git commits or compute hashes. | `managed-agent-workflow` (`SKILL.md`, `reconciliation-protocol.md`) | Author commit instructions in reconciliation protocol; ambiguous manifest assembly prohibitions during Phase 5; reviewer hash computation steps. |
| **8** | **Orthogonal Verification Sets & Structural vs Semantic Authority:** Verification layers form an orthogonal set of domain authorities directly covering affected diffs. Automated Layer 0 checks are authoritative solely for structural syntax; semantic correctness belongs to independent contract review. | `test-and-verification-strategy` (`SKILL.md`, `verification-layers-and-tooling.md`) & `AGENTS.md` | Ascending ladder diagram (`▲`); "Hierarchy" title; conflating Layer 0 structural automation with semantic governance correctness. |
| **9** | **Lean Root Policy & SSOT:** `AGENTS.md` is the lean root SSOT for universal principles, preflight, mode routing, authority boundaries, and permissions (< 250 lines); detailed procedural mechanics are owned exclusively by delegated skills. | `AGENTS.md` | Procedural git staging commands (`git add <file>`), diff procedures, commit templates, and detailed test command listings in `AGENTS.md`. |
| **10** | **Decoupled Task-Specific Implementation Checkpoints:** Implementation checkpoints are derived dynamically from affected contracts and architectural slices, rather than forcing rigid Java-centric checkpoints. | `implementation-planning-and-contract-freeze` (`workstream-plan-template.md`) | Rigid "Core Logic / Model Implementation" and "Integration Verification" checkpoints hardcoded in generic plan templates. |
| **11** | **Total Reviewer Outcome Algebra:** Review outcomes are total over `{ PASS, BLOCKING_FINDINGS, BLOCKED }` for every review phase, with deterministic transitions, prerequisite repair boundaries, and explicit audit verdicts. | `managed-agent-workflow` (`controller-state-machine.md`) & `code-review-and-quality` (`SKILL.md`) | Partial state machine transitions missing `BLOCKED`; `REJECTED_APPROACH` pseudo-verdict; unhandled review blockers. |
| **12** | **Static Candidate Plan Contract:** Workstream plans are immutable candidate contracts fixed at author handoff identity with static `Author Submission State: Candidate Plan (Ready for Review)`; dynamic state belongs to runtime memory and git commits. | `implementation-planning-and-contract-freeze` (`workstream-plan-template.md`) | Dynamic status headers (`Frozen`, `Cycle 1`); in-place modification of plan status after freeze; hash invalidation caused by progress updates. |
| **13** | **Explicit Internal Completion Semantics:** The `COMPLETED` controller state explicitly represents internal multi-agent workflow completion, parking at Owner Review Gate for Owner review and disposition. | `managed-agent-workflow` (`controller-state-machine.md`) | Ambiguity over whether `COMPLETED` implies remote release or external promotion. |

---

## 3. Subtraction Report (Subtraction First)

### 3.1 Concepts & Rules Removed
1. **Single-Shot Reviewer Prompting:** Removed single-shot invocation that passed baseline authority, candidate text, and rubrics simultaneously, allowing unconstrained agents to inspect candidates before authority. Replaced with operational Two-Phase Reviewer Boot Handshake.
2. **Presentation-Order Header Assumption:** Removed reliance on static report headers as proof of temporal precedence. Replaced with mandatory runtime `AUTHORITY_LOADED` handshake emitted prior to receiving Partition 2.
3. **Informal Review Acceptance:** Removed unvalidated controller acceptance of reviewer reports. Replaced with deterministic boolean acceptance predicate `review_gate_accepts_report(report)`.
4. **Partial Authority Consumption:** Removed ambiguous authority references that allowed reviewers to skip relevant baseline documents. Replaced with explicit 14-file baseline authority closure.
5. **Passive Checklist Review:** Removed passive checklist verification. Reviewers must actively construct counterexamples and trace failure modes against candidate text.
6. **Abstract Git Commit Blob References:** Removed abstract revision syntax (e.g. `<bootstrap_commit>:AGENTS.md`) from reviewer prompt instructions that read-only subagents could not directly read. Replaced with concrete filesystem paths in `scratch/bootstrap-governance/`.
7. **Contradictory Manifest Assembly Prohibition:** Removed broad prohibition that prevented Main Controller from compiling Verification Evidence Manifests during Phase 5. Confined prohibition strictly to `MODE_3_SELECTED` pre-planning discovery.
8. **Author Commit Instructions:** Removed contradictory text in `reconciliation-protocol.md` instructing authors to commit missing artifacts.
9. **Ascending Verification Ladder Diagram:** Removed ascending diagram (`Layer 5 ▲ Layer 4 ▲ ...`) and "Hierarchy" title in `test-and-verification-strategy/SKILL.md`.
10. **Layer 0 Semantic Authority Overclaim:** Removed wording attributing semantic governance authority to Layer 0 automated checks. Automated checks verify structural syntax only.
11. **Procedural Git Commands in Root Policy:** Removed procedural commands (`git add <file1> <file2>`, `git status --short`, `git diff --cached`, `git merge --no-ff`), detailed commit lifecycle descriptions, and 6-layer test command descriptions from `AGENTS.md`.
12. **Rigid Implementation Checkpoints in Plan Template:** Removed hardcoded code-centric checkpoints ("Core Logic", "Integration") from generic plan template.
13. **Hyperbolic & Absolute Terminology:** Purged ungrounded absolute terminology (forbidden tokens: `"hoàn toàn"`, `"triệt để"`, `"100%"`, `"đảm bảo tính khép kín"`, `"guarantees"`, `"flawless"`) across candidate documents and review reports.
14. **Arbitrary Evidence Locations:** Replaced unstandardized manifest locations with canonical `docs/workstreams/<id>/verification.md`.

### 3.2 Duplicated Authority Removed
- **Git Mechanics:** Exact staging syntax, diff rules, and commit message templates removed from `AGENTS.md` and consolidated into `git-checkpoint-workflow`.
- **Verification Commands:** Exact test commands and script descriptions removed from `AGENTS.md` and consolidated into `test-and-verification-strategy`.
- **Identity & Hashing:** Hashing instructions removed from `implementation-planning...` and `code-review-and-quality` and consolidated into `managed-agent-workflow`.
- **Evaluation Rubrics:** Rubric criteria removed from prompt contracts and consolidated into `code-review-and-quality`.

### 3.3 States & Fields Avoided
- Avoided adding separate daemon or supervisor subagents; operationalized two-phase boot directly within Main Controller's message dispatcher and reviewer prompt contract.
- Avoided persistent disk state files; controller state remains strictly ephemeral in session memory.
- Maintained lean controller phases (`IDLE`, `MODE_3_SELECTED`, `PLANNING`, `PLAN_REVIEW`, `PLAN_RECONCILING`, `FROZEN`, `IMPLEMENTING`, `IMPL_REVIEW`, `IMPL_RECONCILING`, `COMPLETED`, `ESCALATED`).
- Avoided mutable status fields in `plan.md`.

### 3.4 Files NOT Created
- Zero new files created. The existing 5 modular skills + `AGENTS.md` provide complete coverage without creating speculative frameworks or extra scripts.

---

## 4. Deep Architectural Resolution for Owner Findings

### 4.1 Finding 1: Full Authority Closure Enforcement
- **Problem:** Reviewer IR previously missed 3 baseline files (`slicing-and-dependency-strategies.md`, `git-checkpoint-workflow/SKILL.md`, `checkpoint-lifecycle-and-lineage.md`). Having files in `manifest.json` does not prove they were read.
- **Root Invariant:** For governance modifications, the authoritative baseline closure comprises all 14 materialized files. The review is invalid unless every file in the closure is demonstrably read.
- **Authoritative 14-File Baseline Closure:**
  1. `AGENTS.md`
  2. `.agents/skills/managed-agent-workflow/SKILL.md`
  3. `.agents/skills/managed-agent-workflow/references/controller-state-machine.md`
  4. `.agents/skills/managed-agent-workflow/references/reconciliation-protocol.md`
  5. `.agents/skills/implementation-planning-and-contract-freeze/SKILL.md`
  6. `.agents/skills/implementation-planning-and-contract-freeze/references/workstream-plan-template.md`
  7. `.agents/skills/implementation-planning-and-contract-freeze/references/slicing-and-dependency-strategies.md`
  8. `.agents/skills/code-review-and-quality/SKILL.md`
  9. `.agents/skills/code-review-and-quality/references/plan-review-rubric.md`
  10. `.agents/skills/code-review-and-quality/references/implementation-review-rubric.md`
  11. `.agents/skills/git-checkpoint-workflow/SKILL.md`
  12. `.agents/skills/git-checkpoint-workflow/references/checkpoint-lifecycle-and-lineage.md`
  13. `.agents/skills/test-and-verification-strategy/SKILL.md`
  14. `.agents/skills/test-and-verification-strategy/references/verification-layers-and-tooling.md`
- **Implementation Design:**
  1. **Specification in `controller-state-machine.md`:** Partition 1 explicitly enumerates the full 14-file authority closure and references `scratch/bootstrap-governance/manifest.json`.
  2. **Specification in `code-review-and-quality/SKILL.md` & Rubrics:** Mandate that the reviewer's `Proof of Authority Consumption` header must list all 14 baseline files. The omission of any file (such as the 3 previously missed files) causes immediate rejection by Main Controller.

### 4.2 Finding 2: Two-Phase Reviewer Boot Handshake (Enforcing Temporal Ordering)
- **Problem:** A static header at the top of a final report only proves presentation order, not execution order. An unconstrained agent could read candidate files first and write the header afterwards.
- **Root Invariant:** Temporal precedence must be enforced by message routing and information withholding. The reviewer must not receive candidate anchors until baseline authority is consumed and acknowledged.
- **Implementation Design:**
  1. **Phase 1 (`REVIEWER_BOOT`):**
     - Main Controller invokes Reviewer (`R` or `IR`) via `invoke_subagent` (or `define_subagent`).
     - The initial prompt contains **Partition 1** (Materialized Authority Closure in scratch) and **Partition 3** (Authoritative Rubric).
     - **Partition 2 (Candidate Anchors & Claims) is explicitly WITHHELD.**
     - The Reviewer reads all 14 baseline files in the closure, verifies their integrity against `manifest.json`, and emits an explicit `AUTHORITY_LOADED` handshake message via `send_message` back to Main Controller:
       ```markdown
       AUTHORITY_LOADED
       - Baseline Commit: 1fa1d58
       - Materialized Path: <scratch/bootstrap-governance/...>
       - Consumed Files (14/14):
         1. AGENTS.md
         2. .agents/skills/managed-agent-workflow/SKILL.md
         3. .agents/skills/managed-agent-workflow/references/controller-state-machine.md
         4. .agents/skills/managed-agent-workflow/references/reconciliation-protocol.md
         5. .agents/skills/implementation-planning-and-contract-freeze/SKILL.md
         6. .agents/skills/implementation-planning-and-contract-freeze/references/workstream-plan-template.md
         7. .agents/skills/implementation-planning-and-contract-freeze/references/slicing-and-dependency-strategies.md
         8. .agents/skills/code-review-and-quality/SKILL.md
         9. .agents/skills/code-review-and-quality/references/plan-review-rubric.md
         10. .agents/skills/code-review-and-quality/references/implementation-review-rubric.md
         11. .agents/skills/git-checkpoint-workflow/SKILL.md
         12. .agents/skills/git-checkpoint-workflow/references/checkpoint-lifecycle-and-lineage.md
         13. .agents/skills/test-and-verification-strategy/SKILL.md
         14. .agents/skills/test-and-verification-strategy/references/verification-layers-and-tooling.md
       - Status: Ready for Partition 2 Candidate Anchors
       ```
  2. **Phase 2 (`REVIEW_ACTIVE`):**
     - Main Controller receives `AUTHORITY_LOADED` and validates that all 14 files and commit `1fa1d58` are present.
     - Upon successful verification, Main Controller sends Partition 2 (candidate plan path + `candidate_plan_hash`, or implementation diff + evidence manifest) to the SAME reviewer session via `send_message`.
     - The reviewer evaluates candidate text against baseline authority and emits the review report.
     - Because candidate anchors are physically withheld until after `AUTHORITY_LOADED` is emitted, out-of-order evaluation is prevented.

### 4.3 Finding 3: Review Report Anti-Overclaim Discipline
- **Problem:** Review reports frequently lapsed into unverified, hyperbolic praise (e.g. claiming an architecture provides absolute protection or total coverage), violating Canary Lesson 7.
- **Root Invariant:** Claim strength must not exceed evidence strength. A review report that uses forbidden absolutes is self-refuting.
- **Implementation Design:**
  1. **Proscribed Absolute Tokens:**
     The following tokens are strictly forbidden in reviewer reports and candidate planning documents:
     - `"hoàn toàn"`
     - `"triệt để"`
     - `"100%"`
     - `"đảm bảo tính khép kín"`
     - `"guarantees"`
     - `"flawless"`
  2. **Reviewer Self-Audit Requirement:** In `code-review-and-quality/SKILL.md` and both rubrics, mandate that reviewers must inspect their drafted report text prior to submission and confirm that zero proscribed absolute tokens are present.
  3. **Rejection Trigger:** If a review report contains any of the proscribed tokens, Main Controller immediately rejects the report under the acceptance predicate.

### 4.4 Finding 4: Deterministic Main Review Acceptance Predicate
- **Problem:** Main Controller previously accepted review verdicts informally without verifying that the review conformed to all structural and behavioral invariants.
- **Root Invariant:** State transitions dependent on review verdicts require formal predicate evaluation.
- **Formal Predicate Definition:**
  ```text
  review_gate_accepts_report(report) :=
    boot_handshake_verified(report) ∧
    authority_closure_complete(report, expected_closure_14) ∧
    all_invariants_falsified_4part(report) ∧
    zero_proscribed_absolutes(report) ∧
    valid_closed_verdict(report.verdict)
  ```
  Where:
  1. `boot_handshake_verified`: The Two-Phase Reviewer Boot Handshake was executed (`AUTHORITY_LOADED` was emitted and verified by Main Controller before Partition 2 was dispatched).
  2. `authority_closure_complete`: The report's Proof of Authority Consumption verifies that all 14 baseline files in the closure were read via read tools.
  3. `all_invariants_falsified_4part`: Every evaluated dimension/invariant contains a complete 4-part falsification block (`Target Invariant`, `Counterexample Attempted`, `Execution Trace`, `Result / Defense`).
  4. `zero_proscribed_absolutes`: The report contains zero occurrences of the proscribed absolute tokens.
  5. `valid_closed_verdict`: The verdict belongs strictly to `{ PASS, BLOCKING_FINDINGS, BLOCKED }`.
- **Implementation Design:**
  - Codified in `controller-state-machine.md` (Section 2 & Section 3) and `managed-agent-workflow/SKILL.md` (Section 4).
  - If `review_gate_accepts_report(report) == false`, the verdict is void. Main Controller rejects the report, remains in the review phase, and requires the reviewer to rectify the defect.

### 4.5 Continuity of Prior Architectural Resolutions
- **Finding 1 (Counterexample / Falsification):** Retained in `code-review-and-quality` and both rubrics. Every evaluation dimension mandates the 4-part structure.
- **Finding 2 (Materialized Bootstrap Authority):** Retained in `managed-agent-workflow` and `scratch/bootstrap-governance/`.
- **Finding 3 (Main vs Author Ownership):** Main Controller owns all hashes and commits; authors never commit; Main compiles evidence manifest in Phase 5.
- **Finding 4 (Orthogonal Verification Sets & Layer 0):** Retained in `test-and-verification-strategy`. Layer 0 structural syntax != semantic authority.
- **Finding 5 (Lean Root SSOT Line Budget):** `AGENTS.md` strictly < 250 lines (target ~200–220 lines).
- **Finding 6 (Decoupled Implementation Checkpoints):** Contract-derived checkpoints in `workstream-plan-template.md`.
- **Finding 7 (Anti-Overclaim Purge across Documents):** Disciplined, proportional claims repository-wide.
- **Suggestion 1 (Evidence Path):** Canonical path `docs/workstreams/<id>/verification.md`.
- **Suggestion 2 (`COMPLETED` Semantics):** Internal completion parking at Owner Review Gate.

---

## 5. Scope Boundaries & Blast Radius

### 5.1 Strict In-Scope
- `AGENTS.md`: Lean SSOT streamlining, remove procedural git/test duplication, clarify Layer 0 structural authority, keep line count < 250 lines.
- `.agents/skills/managed-agent-workflow/`:
  - `SKILL.md`: Two-Phase Reviewer Boot Handshake invariant, Deterministic Review Acceptance Predicate, 14-file authority closure, sole Main hash/commit ownership, Phase 5 manifest assembly, total 3-verdict model, anti-overclaim purge.
  - `references/controller-state-machine.md`: Add Two-Phase Boot sub-phases (`REVIEWER_BOOT` -> `REVIEW_ACTIVE`), formal boolean acceptance predicate `review_gate_accepts_report(report)`, 14-file authority closure in state schema and Partition 1 prompt contract, `COMPLETED` semantics at Owner Review Gate.
  - `references/reconciliation-protocol.md`: Author commit contradiction fix, alignment with Two-Phase Boot and acceptance predicate.
- `.agents/skills/code-review-and-quality/`:
  - `SKILL.md`: Two-Phase Reviewer Boot protocol, Full Authority Closure verification (all 14 files), Review Report Anti-Overclaim Discipline (proscribed tokens & self-audit), 4-part counterexample/falsification method.
  - `references/plan-review-rubric.md`: Require 14-file authority closure in Proof of Authority Consumption, reviewer self-audit against proscribed absolutes, 4-part falsification block across all 6 dimensions.
  - `references/implementation-review-rubric.md`: Require 14-file authority closure in Proof of Authority Consumption, reviewer self-audit against proscribed absolutes, 4-part falsification block across all 5 dimensions.
- `.agents/skills/test-and-verification-strategy/`:
  - `SKILL.md`: Orthogonal Verification Domain Matrix, Layer 0 structural authority vs semantic authority, anti-overclaim purge.
  - `references/verification-layers-and-tooling.md`: Canonical verification path `docs/workstreams/<id>/verification.md`, Layer 0 token scanning for proscribed absolutes, provenance freshness.
- `.agents/skills/implementation-planning-and-contract-freeze/`:
  - `SKILL.md`: Domain decoupling, sole Main hash ownership reference, anti-overclaim purge.
  - `references/workstream-plan-template.md`: Decouple rigid checkpoints into contract-derived checkpoints, static candidate header, reference 14-file authority closure.
  - `references/slicing-and-dependency-strategies.md`: Generic layer slicing patterns, anti-overclaim purge.
- `.agents/skills/git-checkpoint-workflow/`:
  - `SKILL.md`: Two-phase commit lifecycle, sole Main commit ownership, anti-overclaim purge.
  - `references/checkpoint-lifecycle-and-lineage.md`: Proportional commit templates, integration with review acceptance gates, anti-overclaim purge.
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
| `.agents/skills/managed-agent-workflow/SKILL.md` | Codify Two-Phase Reviewer Boot Handshake invariant; codify Deterministic Review Acceptance Predicate; enforce 14-file authority closure; clarify Phase 5 manifest assembly; confirm sole Main hash/commit ownership. | Findings 1, 2, 4 |
| `.agents/skills/managed-agent-workflow/references/controller-state-machine.md` | Codify Two-Phase Boot sub-phases (`REVIEWER_BOOT` and `REVIEW_ACTIVE`); add formal boolean acceptance predicate `review_gate_accepts_report(report)`; enumerate 14-file authority closure in Partition 1 prompt contract; define `COMPLETED` semantics at Owner Review Gate. | Findings 1, 2, 4 |
| `.agents/skills/managed-agent-workflow/references/reconciliation-protocol.md` | Clarify author artifact correction vs Main commit ownership; align with Two-Phase Boot and review acceptance predicate during re-reviews. | Findings 2, 4 |
| `.agents/skills/code-review-and-quality/SKILL.md` | Codify reviewer Two-Phase Boot protocol (read 14 baseline files, verify manifest, emit `AUTHORITY_LOADED`, await Partition 2); codify Full Authority Closure enforcement; codify Review Report Anti-Overclaim Discipline (proscribed tokens & self-audit); mandate 4-part counterexample/falsification review. | Findings 1, 2, 3 |
| `.agents/skills/code-review-and-quality/references/plan-review-rubric.md` | Mandate listing and consuming all 14 baseline files in Proof of Authority Consumption; mandate reviewer self-audit against proscribed absolutes; mandate 4-part falsification blocks across all 6 dimensions. | Findings 1, 3 |
| `.agents/skills/code-review-and-quality/references/implementation-review-rubric.md` | Mandate listing and consuming all 14 baseline files in Proof of Authority Consumption; mandate reviewer self-audit against proscribed absolutes; mandate 4-part falsification blocks across all 5 dimensions. | Findings 1, 3 |
| `.agents/skills/test-and-verification-strategy/SKILL.md` | Remove ascending ladder diagram (`▲`); replace with Orthogonal Verification Domain Matrix; clarify Layer 0 structural syntax vs semantic authority; anti-overclaim purge. | Layer 0 Authority, Anti-Overclaim |
| `.agents/skills/test-and-verification-strategy/references/verification-layers-and-tooling.md` | Standardize verification evidence path to `docs/workstreams/<id>/verification.md`; add Layer 0 proscribed token scanner check; provenance freshness. | Finding 3, Evidence Path |
| `.agents/skills/implementation-planning-and-contract-freeze/SKILL.md` | Domain decoupling; sole Main hash ownership reference; anti-overclaim purge. | Anti-Overclaim |
| `.agents/skills/implementation-planning-and-contract-freeze/references/workstream-plan-template.md` | Decouple rigid checkpoints into contract-derived checkpoints; static candidate header; reference 14-file baseline authority closure. | Finding 1, Checkpoint Decoupling |
| `.agents/skills/implementation-planning-and-contract-freeze/references/slicing-and-dependency-strategies.md` | Generic layer slicing patterns; anti-overclaim purge. | Finding 1, Anti-Overclaim |
| `.agents/skills/git-checkpoint-workflow/SKILL.md` | Two-phase commit lifecycle; sole Main commit ownership; anti-overclaim purge. | Finding 1, Git Safety |
| `.agents/skills/git-checkpoint-workflow/references/checkpoint-lifecycle-and-lineage.md` | Proportional commit templates; gate commits behind Deterministic Review Acceptance Predicate; anti-overclaim purge. | Finding 4, Git Safety |
| `docs/workstreams/agent-architecture-redesign/plan.md` | This candidate implementation plan. | Findings 1–4 |

### Files to Create / Delete
- **Zero new files to create** (all functionality cleanly housed within existing 5 skills and root contract).
- **Zero files to delete**.

---

## 7. Slicing & Implementation Strategy

Implementation will proceed across 3 surgical slices, with each slice integrating the operational improvements relevant to its domain:

```text
Slice 1: Root Policy, State Machine & Orchestration Boundaries
- AGENTS.md (Lean SSOT streamlining, strip procedural commands, Layer 0 structural definition)
- managed-agent-workflow (SKILL.md, controller-state-machine.md, reconciliation-protocol.md)
  [Integrates Finding 1 (14-file closure definition), Finding 2 (Two-Phase Boot Handshake),
   Finding 4 (Deterministic Acceptance Predicate), Lean SSOT, and Layer 0 Authority]
      │
Slice 2: Review Rubrics, Falsification Architecture, Authority Closure & Anti-Overclaim
- code-review-and-quality (SKILL.md, plan-review-rubric.md, implementation-review-rubric.md)
- implementation-planning-and-contract-freeze (SKILL.md, workstream-plan-template.md, slicing-and-dependency-strategies.md)
  [Integrates Finding 1 (14-file consumption proof), Finding 2 (Reviewer Boot protocol),
   Finding 3 (Anti-overclaim discipline & self-audit), and Falsification rubrics]
      │
Slice 3: Verification Strategy, Orthogonal Sets, Git Lineage & Automated Quality Gates
- test-and-verification-strategy (SKILL.md, verification-layers-and-tooling.md)
- git-checkpoint-workflow (SKILL.md, checkpoint-lifecycle-and-lineage.md)
  [Integrates Finding 3 (Layer 0 proscribed token scanning), Finding 4 (Commit gate integration
   with Acceptance Predicate), Orthogonal Domain Matrix, and Audit Lineage]
```

### 7.1 Detailed Scope per Slice

#### Slice 1: Root Policy, State Machine & Orchestration Boundaries
- **`AGENTS.md`:** Streamline Sections 6 and 7 to remove procedural commands and detailed test command tables; clarify Layer 0 structural vs semantic authority; maintain strict line budget (< 250 lines).
- **`managed-agent-workflow/SKILL.md`:**
  - Add Invariant 6: Two-Phase Reviewer Boot Handshake (Phase 1 `REVIEWER_BOOT` with Partition 2 withheld; receipt & verification of `AUTHORITY_LOADED` with 14 files; Phase 2 `REVIEW_ACTIVE` candidate dispatch via `send_message`).
  - Add Invariant 8: Deterministic Main Review Acceptance Predicate (`review_gate_accepts_report(report)` evaluating 5 boolean conditions).
  - Add Invariant 11: Full Authority Closure Enforcement (14 files defined in manifest and required to be consumed).
- **`controller-state-machine.md`:**
  - Ephemeral state schema: add `reviewer_boot_handshake_verified`, `consumed_authority_files`, `review_report_predicate_evaluated`.
  - State transitions: Codify `REVIEWER_BOOT` and `REVIEW_ACTIVE` sub-phases in `PLAN_REVIEW` and `IMPL_REVIEW`.
  - Prompt contract: Partition 1 explicitly enumerates the 14 baseline files in `scratch/bootstrap-governance/`; Partition 2 withheld at spawn and sent only via `send_message` after handshake verification.
  - Acceptance predicate: Formal specification of `review_gate_accepts_report(report)`.
  - Completion semantics: Define `COMPLETED` at Owner Review Gate.
- **`reconciliation-protocol.md`:** Align author response rules with Main commit ownership, Two-Phase Boot, and acceptance predicate.

#### Slice 2: Review Rubrics, Falsification Architecture, Authority Closure & Anti-Overclaim
- **`code-review-and-quality/SKILL.md`:**
  - Section 3.4: Codify reviewer Two-Phase Boot protocol (read 14 baseline files, verify manifest, emit `AUTHORITY_LOADED`, await Partition 2).
  - Section 3.5: Full Authority Closure enforcement (Proof of Authority Consumption must verify all 14 files, specifically including `slicing-and-dependency-strategies.md`, `git-checkpoint-workflow/SKILL.md`, `checkpoint-lifecycle-and-lineage.md`).
  - Section 3.7: Review Report Anti-Overclaim Discipline (proscribe `"hoàn toàn"`, `"triệt để"`, `"100%"`, `"đảm bảo tính khép kín"`, `"guarantees"`, `"flawless"`; require mandatory reviewer self-audit).
  - Section 3.8: 4-part adversarial falsification structure (`Target Invariant`, `Counterexample Attempted`, `Execution Trace`, `Result / Defense`).
- **`plan-review-rubric.md`:** Mandate Proof of Authority Consumption covering all 14 files; mandate self-audit; require 4-part falsification blocks across all 6 dimensions.
- **`implementation-review-rubric.md`:** Mandate Proof of Authority Consumption covering all 14 files; mandate self-audit; require 4-part falsification blocks across all 5 dimensions.
- **`implementation-planning-and-contract-freeze/`:** Decouple checkpoints into contract-derived structure; maintain static candidate header; reference 14-file authority closure.

#### Slice 3: Verification Strategy, Orthogonal Sets, Git Lineage & Automated Quality Gates
- **`test-and-verification-strategy/`:**
  - Replace ladder diagram with Orthogonal Verification Domain Matrix.
  - Define Layer 0 automated structural checks (link validation, schema checks, line limits, and proscribed token scanning).
  - Standardize verification path to `docs/workstreams/<id>/verification.md`.
- **`git-checkpoint-workflow/`:**
  - Two-phase commit lifecycle (commit != audit promotion).
  - Enforce that local checkpoint commits occur only after Main Controller verifies `review_gate_accepts_report(report) == true`.
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
6. **Proscribed Absolute Token Scan:**
   Execute a text scan across candidate files and review logs verifying zero occurrences of forbidden tokens outside of explicit definition blocks:
   - Tokens: `"hoàn toàn"`, `"triệt để"`, `"100%"`, `"đảm bảo tính khép kín"`, `"guarantees"`, `"flawless"`.
7. **Authority Closure Verification:**
   Verify that all 14 baseline files exist in `scratch/bootstrap-governance/` and match hashes recorded in `manifest.json`.

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
  - **Two-Phase Reviewer Boot Handshake (Phase 1):** Main Controller invokes Plan Reviewer `R` with Partition 1 (14 materialized baseline files in scratch) and Partition 3 (Plan Review Rubric). Partition 2 is WITHHELD.
  - Plan Reviewer `R` reads all 14 baseline files, verifies `manifest.json`, and emits `AUTHORITY_LOADED` listing all 14 files and commit `1fa1d58`.
  - Main Controller verifies the handshake.
  - **Review Active (Phase 2):** Main dispatches Partition 2 (candidate plan path and `candidate_plan_hash`) to `R` via `send_message`.
  - Plan Reviewer `R` evaluates candidate plan using the 4-part counterexample/falsification method, conducts self-audit, and emits report.
  - Main Controller evaluates `review_gate_accepts_report(report)`.
  - Upon acceptance and explicit `R verdict: PASS`, Main Controller recomputes post-review hash, asserts equality, records `frozen_plan_hash`, and creates the Plan Freeze Checkpoint.
- **Checkpoint 2: Surgical Implementation of Slices 1–3**
  - Implementor `I` applies changes for Slices 1, 2, and 3.
- **Checkpoint 3: Proportional Verification & Manifest Assembly**
  - Implementor executes Layer 0 structural checks and records outputs in `docs/workstreams/agent-architecture-redesign/verification.md`.
  - Main Controller assembles Verification Evidence Manifest.
- **Checkpoint 4: Implementation Review Gate & Verified Implementation Checkpoint**
  - **Two-Phase Reviewer Boot Handshake (Phase 1):** Main Controller invokes Implementation Reviewer `IR` with Partition 1 (14 materialized baseline files) and Partition 3 (Implementation Review Rubric). Partition 2 is WITHHELD.
  - Reviewer `IR` reads all 14 baseline files, verifies `manifest.json`, and emits `AUTHORITY_LOADED`.
  - Main Controller verifies the handshake.
  - **Review Active (Phase 2):** Main dispatches Partition 2 (working-tree diff and Verification Evidence Manifest) to `IR` via `send_message`.
  - Reviewer `IR` evaluates implementation using the 4-part counterexample/falsification method, conducts self-audit, and emits report.
  - Main Controller evaluates `review_gate_accepts_report(report)`.
  - Upon acceptance and explicit `IR verdict: PASS`, Main Controller creates the Verified Implementation Checkpoint and parks in `COMPLETED` awaiting Owner disposition.

---

## 10. Residual Risks & Mitigation Safeguards

| Risk Category | Assessed Risk Level | Mitigation Safeguard |
| :--- | :--- | :--- |
| **Out-of-Order Candidate Evaluation** | Mitigated | Two-Phase Reviewer Boot Handshake physically withholds Partition 2 candidate anchors until after `AUTHORITY_LOADED` is emitted and verified by Main. |
| **Incomplete Authority Consumption** | Mitigated | Full 14-file authority closure defined; omission of any file causes deterministic rejection by Main Controller under `review_gate_accepts_report`. |
| **Reviewer Overclaim & Self-Refutation** | Mitigated | Proscribed absolute tokens strictly forbidden; reviewers required to self-audit report text before submission; reports with forbidden tokens are rejected. |
| **Informal Review Acceptance** | Mitigated | Deterministic 5-condition boolean predicate `review_gate_accepts_report(report)` blocks state transitions on non-compliant reports. |
| **Passive Reviewer Approvals** | Mitigated | Rubrics mandate 4-part counterexample and falsification structure for all evaluation dimensions. |
| **Reviewer Privilege Bleed** | Controlled | Reviewers configured with `enable_write_tools: false` and zero terminal commands; Main Controller owns all hash computations and git commits. |
| **State Machine Deadlock on Blocker** | Controlled | Total 3-verdict algebra with explicit Prerequisite Repair Protocol governs review outcomes deterministically. |
| **Premature Audit Immutability** | Mitigated | Two-phase commit model distinguishes provisional local coordination checkpoints from promoted immutable audit milestones. |
| **SSOT Drift & Documentation Bloat** | Mitigated | `AGENTS.md` line budget strictly enforced (< 250 lines); procedural git and test mechanics delegated to specialized skills. |

---

## 11. Candidate Plan Handoff & Review Readiness

Planner `P` has completed substantive discovery, applied the Subtraction First discipline, and authored this candidate implementation plan addressing the 4 Owner Findings of Correction Iteration 3.

- **Canonical Plan Path:** `docs/workstreams/agent-architecture-redesign/plan.md`
- **Handoff State:** Semantic candidate handoff `{ candidate_path: "docs/workstreams/agent-architecture-redesign/plan.md", ready: true }`.
- **Status:** Ready for Main Controller to compute `candidate_plan_hash` and initiate the Two-Phase Reviewer Boot Handshake with Plan Reviewer `R` citing the materialized baseline at `scratch/bootstrap-governance/`.
