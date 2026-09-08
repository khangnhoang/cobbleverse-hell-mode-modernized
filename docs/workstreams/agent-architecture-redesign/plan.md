# Workstream Plan: Agent Architecture Redesign (Phase 2 — Owner-Review Correction Iteration 2)

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

Following Owner review of Architecture Phase 2 at baseline commit `1fa1d58` and Correction Iteration 1, the Owner established this **Correction Iteration 2 Gate** identifying 7 Required Findings and 2 Suggestions:

1. **[Required] Encode Counterexample / Falsification into Rubrics:** Rubrics currently permit passive checklist verification. Reviewers must actively attempt to falsify candidate claims and invariants using explicit counterexample scenarios and execution traces.
2. **[Required] Operationalize `bootstrap_governance_manifest`:** Read-only reviewers cannot execute git commands to inspect commit history. The bootstrap baseline must be materialized as real files on disk (`scratch/bootstrap-governance/`) at task start, cited in prompt Partition 1, and verified via a mandatory `Proof of Authority Consumption` section in reviewer reports.
3. **[Required] Resolve Main Ownership & Author Commit Contradictions:** Clarify that Main Controller's prohibition on assembling manifests applies strictly during `MODE_3_SELECTED` pre-planning discovery (Main Controller does assemble the Verification Evidence Manifest in Phase 5). Resolve contradiction in `reconciliation-protocol.md` where authors were instructed to commit artifacts.
4. **[Required] Fix "SET not LADDER" Presentation & Layer 0 Authority Contradiction:** Remove the remaining ascending ladder diagram (`▲`) in `test-and-verification-strategy/SKILL.md`. Clarify that automated Layer 0 checks are authoritative solely for structural/syntactic integrity; semantic governance authority belongs exclusively to independent contract review.
5. **[Required] Lean Root SSOT (`AGENTS.md` Line Budget):** Strip remaining procedural Git commands and detailed test command listings from `AGENTS.md`, delegating them to specialized skills to keep root policy lean (< 250 lines, target ~200–220 lines).
6. **[Required] Generic Planning Checkpoint Decoupling:** Decouple rigid code checkpoints ("Core Logic", "Integration") in `workstream-plan-template.md` into task-specific implementation checkpoints derived from affected contracts.
7. **[Required] Anti-Overclaim Audit in Plan & Candidate Files:** Purge hyperbolic and absolute claims ("100%", "fully", "Risk Level: Zero", "guarantees", "exhaustive") across the candidate plan and skill documents.
- **Suggestion 1:** Align verification evidence path in `verification-layers-and-tooling.md` to `docs/workstreams/<id>/verification.md`.
- **Suggestion 2:** Explicitly define `COMPLETED = internal multi-agent workflow complete; awaiting Owner review and disposition at Owner Review Gate` in `controller-state-machine.md`.

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
| **2** | **Materialized Bootstrap Governance Authority:** All governance authority used during a run resolves from materialized baseline files in `scratch/bootstrap-governance/` at `bootstrap_commit`. Reviewers cite baseline files in a mandatory `Proof of Authority Consumption` section. | `managed-agent-workflow` (`SKILL.md`, `controller-state-machine.md`) & `code-review-and-quality` (`SKILL.md`) | Unmaterialized git blob references in reviewer prompts that read-only reviewers could not inspect; circular evaluation against working-tree candidate files. |
| **3** | **Single Identity & Git Commit Owner:** Main Controller owns runtime identity, candidate hashing (`git hash-object`), session tracking, state transitions, manifest assembly in Phase 5, and all local checkpoint commits. Authors and reviewers never execute git commits or compute hashes. | `managed-agent-workflow` (`SKILL.md`, `reconciliation-protocol.md`) | Author commit instructions in reconciliation protocol; ambiguous manifest assembly prohibitions during Phase 5; reviewer hash computation steps. |
| **4** | **Orthogonal Verification Sets & Structural vs Semantic Authority:** Verification layers form an orthogonal set of domain authorities directly covering affected diffs. Automated Layer 0 checks are authoritative solely for structural syntax (links, schemas, line limits); semantic correctness belongs to independent contract review. | `test-and-verification-strategy` (`SKILL.md`, `verification-layers-and-tooling.md`) & `AGENTS.md` | Ascending ladder diagram (`▲`); "Hierarchy" title; conflating Layer 0 structural automation with semantic governance correctness. |
| **5** | **Lean Root Policy & SSOT:** `AGENTS.md` is the lean root SSOT for universal principles, preflight, mode routing, authority boundaries, and permissions (< 250 lines); detailed procedural mechanics are owned exclusively by delegated skills. | `AGENTS.md` | Procedural git staging commands (`git add <file>`), diff procedures, commit templates, and detailed test command listings in `AGENTS.md`. |
| **6** | **Decoupled Task-Specific Implementation Checkpoints:** Implementation checkpoints are derived dynamically from affected contracts and architectural slices, rather than forcing rigid Java-centric checkpoints. | `implementation-planning-and-contract-freeze` (`workstream-plan-template.md`) | Rigid "Core Logic / Model Implementation" and "Integration Verification" checkpoints hardcoded in generic plan templates. |
| **7** | **Anti-Overclaim & Claim-Evidence Discipline:** Statements must remain proportional to observable, demonstrable evidence. Prohibit absolute and hyperbolic claims across all governance, planning, and review documents. | Repository-wide (`AGENTS.md`, all `.agents/skills/`, `plan.md`) | Absolute terms ("100%", "fully", "consolidated 100%", "Risk Level: Zero", "guarantees", "exhaustive candidate plan", "resolving all findings"). |
| **8** | **Total Reviewer Outcome Algebra:** Review outcomes are total over `{ PASS, BLOCKING_FINDINGS, BLOCKED }` for every review phase, with deterministic transitions, prerequisite repair boundaries, and explicit audit verdicts. | `managed-agent-workflow` (`controller-state-machine.md`) & `code-review-and-quality` (`SKILL.md`) | Partial state machine transitions missing `BLOCKED`; `REJECTED_APPROACH` pseudo-verdict; unhandled review blockers. |
| **9** | **Static Candidate Plan Contract:** Workstream plans are immutable candidate contracts fixed at author handoff identity with static `Author Submission State: Candidate Plan (Ready for Review)`; dynamic state belongs to runtime memory and git commits. | `implementation-planning-and-contract-freeze` (`workstream-plan-template.md`) | Dynamic status headers (`Frozen`, `Cycle 1`); in-place modification of plan status after freeze; hash invalidation caused by progress updates. |
| **10** | **Explicit Internal Completion Semantics:** The `COMPLETED` controller state explicitly represents internal multi-agent workflow completion, parking at Owner Review Gate for Owner review and disposition. | `managed-agent-workflow` (`controller-state-machine.md`) | Ambiguity over whether `COMPLETED` implies remote release or external promotion. |

---

## 3. Subtraction Report (Subtraction First)

### 3.1 Concepts & Rules Removed
1. **Passive Checklist Review:** Removed passive checklist verification. Reviewers must actively construct counterexamples and trace failure modes against candidate text.
2. **Abstract Git Commit Blob References:** Removed abstract revision syntax (e.g. `<bootstrap_commit>:AGENTS.md`) from reviewer prompt instructions that read-only subagents could not directly read. Replaced with concrete filesystem paths in `scratch/bootstrap-governance/`.
3. **Contradictory Manifest Assembly Prohibition:** Removed broad prohibition that prevented Main Controller from compiling Verification Evidence Manifests during Phase 5. Confined prohibition strictly to `MODE_3_SELECTED` pre-planning discovery.
4. **Author Commit Instructions:** Removed contradictory text in `reconciliation-protocol.md` instructing authors to commit missing artifacts.
5. **Ascending Verification Ladder Diagram:** Removed the ascending diagram (`Layer 5 ▲ Layer 4 ▲ ...`) and "Hierarchy" title in `test-and-verification-strategy/SKILL.md`.
6. **Layer 0 Semantic Authority Overclaim:** Removed wording attributing semantic governance authority to Layer 0 automated checks. Automated checks verify structural syntax only.
7. **Procedural Git Commands in Root Policy:** Removed procedural commands (`git add <file1> <file2>`, `git status --short`, `git diff --cached`, `git merge --no-ff`), detailed commit lifecycle descriptions, and 6-layer test command descriptions from `AGENTS.md`.
8. **Rigid Implementation Checkpoints in Plan Template:** Removed hardcoded code-centric checkpoints ("Core Logic", "Integration") from generic plan template.
9. **Hyperbolic & Absolute Terminology:** Purged ungrounded absolute terminology ("100%", "fully", "consolidated 100%", "Risk Level: Zero", "guarantees", "exhaustive") across candidate documents.
10. **Arbitrary Evidence Locations:** Replaced floating or unstandardized manifest locations with canonical `docs/workstreams/<id>/verification.md`.

### 3.2 Duplicated Authority Removed
- **Git Mechanics:** Exact staging syntax, diff rules, and commit message templates removed from `AGENTS.md` and consolidated into `git-checkpoint-workflow`.
- **Verification Commands:** Exact test commands and script descriptions removed from `AGENTS.md` and consolidated into `test-and-verification-strategy`.
- **Identity & Hashing:** Hashing instructions removed from `implementation-planning...` and `code-review-and-quality` and consolidated into `managed-agent-workflow`.
- **Evaluation Rubrics:** Rubric criteria removed from prompt contracts and consolidated into `code-review-and-quality`.

### 3.3 States & Fields Avoided
- Avoided adding new controller phases. Maintained lean set (`IDLE`, `MODE_3_SELECTED`, `PLANNING`, `PLAN_REVIEW`, `PLAN_RECONCILING`, `FROZEN`, `IMPLEMENTING`, `IMPL_REVIEW`, `IMPL_RECONCILING`, `COMPLETED`, `ESCALATED`).
- Avoided adding speculative reviewer sub-roles; unified adversarial falsification directly into Plan Reviewer `R` and Implementation Reviewer `IR`.
- Avoided runtime git commands for read-only reviewers by materializing baseline blobs at task start.
- Avoided mutable status fields in `plan.md`.

### 3.4 Files NOT Created
- Zero new files created. The existing 5 modular skills + `AGENTS.md` provide complete coverage without creating speculative frameworks or extra scripts.

---

## 4. Deep Architectural Resolution for Findings 1–7 and Suggestions 1–2

### 4.1 Finding 1: Encode Counterexample & Falsification into Rubrics
- **Problem:** Reviewers evaluating candidate plans or implementations could perform passive checklist reviews (marking "PASS" against vague criteria without proving that failure modes are blocked).
- **Root Invariant:** Independent review requires active adversarial falsification. For every evaluation dimension, reviewers must formulate concrete counterexamples and trace candidate behavior against them.
- **Implementation Design:**
  1. **Update `plan-review-rubric.md`:** For each of the six evaluation dimensions (Grounded Discovery, Fact vs. Assumption, Scope Confinement, Repository & Domain Invariants, Architectural Slicing, Orthogonal Verification), mandate the following structured falsification block:
     - `Target Invariant:` Exact rule or invariant evaluated.
     - `Counterexample Attempted:` Concrete failure scenario, adversarial edge case, or evasion pattern formulated by the reviewer.
     - `Execution Trace:` Step-by-step trace through candidate plan text or architecture to evaluate how the candidate responds.
     - `Result / Defense:` Specific candidate architectural mechanism that blocks the counterexample (or a structured `Critical`/`Required` finding if broken).
  2. **Update `implementation-review-rubric.md`:** For each of the five evaluation dimensions (Frozen Plan Conformance, Blast Radius, Correctness & Robustness, Verification Authenticity, Orthogonal Verification), mandate the same 4-part falsification structure.
  3. **Update `code-review-and-quality/SKILL.md`:** In Section 3, define "Adversarial Falsification & Counterexample Discipline" as a foundational review principle. Reviewers must actively seek to refute candidate claims through concrete counterexamples before issuing a `PASS` verdict.

### 4.2 Finding 2: Operationalize `bootstrap_governance_manifest`
- **Problem:** In Correction Iteration 1, `bootstrap_governance_manifest` was referenced via git blob notation (`<bootstrap_commit>:AGENTS.md`). Read-only reviewers (`enable_write_tools: false`, zero terminal commands) cannot run `git show`, creating an unexecutable contract.
- **Root Invariant:** Baseline governance authority must be physically accessible to read-only tools on the local filesystem. Reviewers must demonstrate authority consumption before evaluating untrusted candidate files.
- **Implementation Design:**
  1. **Materialization Primitive:** Upon entering `MODE_3_SELECTED`, Main Controller creates `<appDataDir>\brain\<conversationId>\scratch\bootstrap-governance\` and extracts baseline governance files from `bootstrap_commit` using `git show` (or copies existing materialized baseline if already prepared).
  2. **Update `controller-state-machine.md`:**
     - Add `bootstrap_governance_path` to ephemeral state schema pointing to `scratch/bootstrap-governance/`.
     - Update Reviewer Invocation Prompt Contract Partition 1 to supply direct absolute and relative filesystem paths to materialized baseline files.
  3. **Mandatory Proof of Authority Consumption:** Update `code-review-and-quality/SKILL.md`, `plan-review-rubric.md`, and `implementation-review-rubric.md` to require a mandatory section at the very top of reviewer reports:
     ```markdown
     ### Proof of Authority Consumption
     - **Baseline Commit:** <bootstrap_commit>
     - **Materialized Authority Path:** <scratch/bootstrap-governance/...>
     - **Inspected Baseline Files:** [List of baseline governance files read via `view_file` before inspecting candidate files]
     ```
     Reviewers must not evaluate untrusted candidate files without first inspecting and citing their baseline authority.

### 4.3 Finding 3: Main Ownership & Author Commit Contradictions
- **Problem:** `managed-agent-workflow/SKILL.md` line 69 forbade Main Controller from assembling evidence manifests, contradicting line 56 where Main Controller prepares evidence manifests. In `reconciliation-protocol.md` line 74, authors were told to "provide or commit" missing artifacts, violating Main's sole commit ownership.
- **Root Invariant:** Main Controller owns all Git checkpoint commits and prepares verification packages for read-only reviewers during review phases. Main's discovery prohibition applies strictly during pre-planning discovery.
- **Implementation Design:**
  1. **Update `managed-agent-workflow/SKILL.md` (Section 4.1):**
     - Clarify that the Blacklist prohibition against assembling evidence manifests applies strictly during `MODE_3_SELECTED` (pre-Planner handoff boundary) to prevent Main from performing speculative pre-planning discovery.
     - In Phase 5 (`IMPLEMENTING` -> `IMPL_REVIEW`), Main Controller does assemble the Verification Evidence Manifest using test outputs and working-tree status provided by Implementor.
  2. **Update `reconciliation-protocol.md` (Line 74):**
     - Replace: `"Author provides or commits the missing artifact"`
     - With: `"Author provides/corrects the missing artifact; Main Controller owns all Git checkpoint commits."`

### 4.4 Finding 4: Orthogonal Verification Sets ("SET not LADDER") & Layer 0 Authority
- **Problem:** `test-and-verification-strategy/SKILL.md` contained an ascending ladder diagram (`▲`) under "Hierarchy" that contradicted the Orthogonal Verification Principle. Additionally, Layer 0 was described as authoritative for governance without clarifying the boundary between automated syntax checks and semantic authority.
- **Root Invariant:** Verification layers form an orthogonal set of domain authorities selected minimally per diff. Automated Layer 0 checks verify structural syntax only; semantic governance authority belongs to independent contract review.
- **Implementation Design:**
  1. **Update `test-and-verification-strategy/SKILL.md`:**
     - Delete the ascending ladder diagram (`Layer 5 ▲ Layer 4 ▲ ...`) and remove "Hierarchy" from section headings.
     - Replace with an **Orthogonal Verification Domain Matrix** explicitly showing orthogonal domain coverage without vertical inheritance.
  2. **Update `AGENTS.md` (Section 6) and `test-and-verification-strategy/SKILL.md` (Section 3.2):**
     - Redefine Layer 0 authority: Automated repository checks verify structural integrity (link validity, YAML frontmatter schema conformance, line count bounds).
     - Explicitly state: Automated Layer 0 checks do **NOT** establish authority correctness, routing correctness, state-machine closure, or invariant preservation. Semantic governance authority belongs to independent contract review (human owner or adversarial reviewer).

### 4.5 Finding 5: Lean Root SSOT (`AGENTS.md` Line Budget < 250 lines)
- **Problem:** `AGENTS.md` contained detailed git staging procedures, commit templates, and test commands, inflating file size to 256 lines and violating SSOT.
- **Root Invariant:** `AGENTS.md` is the lean root SSOT for universal principles, preflight routing, permissions, authority boundaries, and skill catalog (< 250 lines, target ~200–220 lines). Procedural mechanics are owned by delegated skills.
- **Implementation Design:**
  1. **Streamline Section 6 (Verification Authority):** Retain domain authority definitions, orthogonal selection rule, and the Offline != Production invariant. Delegate detailed test command matrices and script options to `test-and-verification-strategy`.
  2. **Streamline Section 7 (Git Safety & Checkpoints):** Retain core permission gates (Mode 2 vs Mode 3 commit authority, remote action gates), surgical staging principle, and review checkpoint reporting. Strip out procedural commands (`git add <file1> <file2>`, `git status --short`, `git diff --cached`, `git merge --no-ff`) and detailed commit body templates, delegating them to `git-checkpoint-workflow`.
  3. Verify resulting line count is strictly under 250 lines (target ~210–225 lines).

### 4.6 Finding 6: Generic Planning Checkpoint Decoupling
- **Problem:** `workstream-plan-template.md` specified rigid Java-centric implementation checkpoints ("Core Logic / Model Implementation", "Integration & Boundary Verification") unsuited for documentation, datapack, or architectural workstreams.
- **Root Invariant:** Implementation checkpoints must be derived dynamically from the workstream's affected contracts and architectural slices.
- **Implementation Design:**
  1. **Update `workstream-plan-template.md` (Section 8):**
     - Replace rigid checkpoints with contract-derived structure:
       - `Checkpoint 1: Plan Freeze Checkpoint (Main Controller Identity & Hash Gate)`
       - `Checkpoint 2: Surgical Implementation of Task Slices (Derived from Section 5/6 Slicing)`
       - `Checkpoint 3: Proportional Verification & Manifest Assembly (Applicable Orthogonal Layers)`
       - `Checkpoint 4: Implementation Review Gate & Verified Implementation Checkpoint`
     - Provide guidance for tailoring checkpoints to specific workstream types (code vs data vs governance).

### 4.7 Finding 7: Anti-Overclaim Purge across Plan & Candidate Files
- **Problem:** Documents contained hyperbolic and absolute terms ("100%", "fully", "consolidated 100%", "Risk Level: Zero", "guarantees", "exhaustive candidate plan", "resolving all 9 Owner findings"), violating Canary Lesson 7.
- **Root Invariant:** Claim strength must not exceed evidence strength. All claims must be disciplined, proportional, and grounded in demonstrable reality.
- **Implementation Design:**
  1. **Purge Targets across Candidate Files:**
     - Replace `"100% Main Controller Hash Ownership"` -> `"Sole Main Controller Hash Ownership"` or `"Main Controller owns candidate hashing"`.
     - Replace `"consolidated 100% into"` -> `"consolidated into"`.
     - Replace `"Risk Level: Zero"` -> `"Low"` or `"Mitigated"`.
     - Replace `"guarantees deterministic handling"` -> `"provides deterministic handling"`.
     - Replace `"exhaustive candidate plan resolving all 9 Owner findings"` -> `"candidate implementation plan addressing the 7 Required Owner Findings and 2 Suggestions"`.
     - Audit and purge `"fully"`, `"triệt để"`, and `"hoàn toàn"` where unverified absolutes are implied.
  2. Maintain strict adherence to this discipline throughout this candidate plan itself.

### 4.8 Suggestion 1: Standardized Verification Evidence Path
- **Problem:** Verification evidence was referenced generally without a standardized file path.
- **Root Invariant:** Verification evidence prepared during Mode 3 should follow a predictable convention.
- **Implementation Design:**
  - In `verification-layers-and-tooling.md`, align the canonical verification evidence path to `docs/workstreams/<id>/verification.md` (with optional ephemeral fallback in controller scratch memory).

### 4.9 Suggestion 2: Clarify COMPLETED Phase Semantics
- **Problem:** The `COMPLETED` controller state could be misinterpreted as automatic release or external audit promotion.
- **Root Invariant:** Internal multi-agent workflow completion is distinct from external Owner acceptance.
- **Implementation Design:**
  - In `controller-state-machine.md`, define phase `COMPLETED` explicitly:
    `COMPLETED = internal multi-agent workflow complete; awaiting Owner review and disposition at Owner Review Gate`.

---

## 5. Scope Boundaries & Blast Radius

### 5.1 Strict In-Scope
- `AGENTS.md`: Lean SSOT streamlining, remove procedural git/test duplication, clarify Layer 0 structural authority, keep line count < 250 lines.
- `.agents/skills/managed-agent-workflow/`:
  - `SKILL.md`: Operationalized materialized bootstrap authority, sole Main hash/commit ownership, clarify manifest assembly in Phase 5, total 3-verdict model, anti-overclaim purge.
  - `references/controller-state-machine.md`: Add `bootstrap_governance_path` to state schema; update Reviewer Invocation Prompt Contract Partition 1 to materialized paths; define `COMPLETED` semantics at Owner Review Gate; anti-overclaim purge.
  - `references/reconciliation-protocol.md`: Fix author commit contradiction; align with total 3-verdict model.
- `.agents/skills/code-review-and-quality/`:
  - `SKILL.md`: Encode counterexample/falsification principle; require Proof of Authority Consumption; anti-overclaim purge.
  - `references/plan-review-rubric.md`: Require 4-part counterexample/falsification structure for all 6 dimensions; require Proof of Authority Consumption; anti-overclaim purge.
  - `references/implementation-review-rubric.md`: Require 4-part counterexample/falsification structure for all 5 dimensions; require Proof of Authority Consumption; anti-overclaim purge.
- `.agents/skills/test-and-verification-strategy/`:
  - `SKILL.md`: Remove ascending ladder diagram (`▲`) and "Hierarchy"; add Orthogonal Verification Domain Matrix; clarify Layer 0 structural vs semantic authority; anti-overclaim purge.
  - `references/verification-layers-and-tooling.md`: Standardize verification path to `docs/workstreams/<id>/verification.md`; provenance-first freshness; anti-overclaim purge.
- `.agents/skills/implementation-planning-and-contract-freeze/`:
  - `SKILL.md`: Domain decoupling, sole Main hash ownership reference, anti-overclaim purge.
  - `references/workstream-plan-template.md`: Decouple rigid code checkpoints into task-specific contract checkpoints; static candidate header; anti-overclaim purge.
  - `references/slicing-and-dependency-strategies.md`: Generic layer slicing patterns; anti-overclaim purge.
- `.agents/skills/git-checkpoint-workflow/`:
  - `SKILL.md`: Two-phase commit lifecycle; sole Main commit ownership; anti-overclaim purge.
  - `references/checkpoint-lifecycle-and-lineage.md`: Proportional commit templates; explicit audit gates; anti-overclaim purge.
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
| `AGENTS.md` | Lean SSOT streamlining; strip procedural git commands and detailed test matrices; define Layer 0 structural vs semantic authority; keep lines < 250 (target ~210–225). | Findings 4, 5, 7 |
| `.agents/skills/managed-agent-workflow/SKILL.md` | Operationalize materialized bootstrap authority; clarify Phase 5 manifest assembly; confirm sole Main hash/commit ownership; anti-overclaim purge. | Findings 2, 3, 7 |
| `.agents/skills/managed-agent-workflow/references/controller-state-machine.md` | Add `bootstrap_governance_path` to state schema; update prompt Partition 1 to point to `scratch/bootstrap-governance/`; define `COMPLETED` semantics at Owner Review Gate. | Findings 2, 7, Suggestion 2 |
| `.agents/skills/managed-agent-workflow/references/reconciliation-protocol.md` | Fix author commit contradiction (line 74); align with total 3-verdict model and prerequisite repair. | Finding 3 |
| `.agents/skills/code-review-and-quality/SKILL.md` | Mandate counterexample/falsification review; require Proof of Authority Consumption; anti-overclaim purge. | Findings 1, 2, 7 |
| `.agents/skills/code-review-and-quality/references/plan-review-rubric.md` | Require 4-part counterexample/falsification block across all 6 dimensions; mandate Proof of Authority Consumption header; anti-overclaim purge. | Findings 1, 2, 7 |
| `.agents/skills/code-review-and-quality/references/implementation-review-rubric.md` | Require 4-part counterexample/falsification block across all 5 dimensions; mandate Proof of Authority Consumption header; anti-overclaim purge. | Findings 1, 2, 7 |
| `.agents/skills/test-and-verification-strategy/SKILL.md` | Remove ascending ladder diagram (`▲`) and "Hierarchy"; replace with Orthogonal Verification Domain Matrix; clarify Layer 0 structural authority; anti-overclaim purge. | Findings 4, 7 |
| `.agents/skills/test-and-verification-strategy/references/verification-layers-and-tooling.md` | Standardize verification evidence path to `docs/workstreams/<id>/verification.md`; provenance-first freshness; anti-overclaim purge. | Findings 4, 7, Suggestion 1 |
| `.agents/skills/implementation-planning-and-contract-freeze/SKILL.md` | Domain decoupling; sole Main hash ownership reference; anti-overclaim purge. | Finding 7 |
| `.agents/skills/implementation-planning-and-contract-freeze/references/workstream-plan-template.md` | Decouple rigid checkpoints into contract-derived checkpoints; static candidate header; anti-overclaim purge. | Findings 6, 7 |
| `.agents/skills/implementation-planning-and-contract-freeze/references/slicing-and-dependency-strategies.md` | Generic layer slicing patterns; anti-overclaim purge. | Finding 7 |
| `.agents/skills/git-checkpoint-workflow/SKILL.md` | Two-phase commit lifecycle; sole Main commit ownership; anti-overclaim purge. | Finding 7 |
| `.agents/skills/git-checkpoint-workflow/references/checkpoint-lifecycle-and-lineage.md` | Proportional commit templates; explicit audit gates; anti-overclaim purge. | Finding 7 |
| `docs/workstreams/agent-architecture-redesign/plan.md` | This candidate implementation plan. | Findings 1–7, Suggestions 1–2 |

### Files to Create / Delete
- **Zero new files to create** (all functionality cleanly housed within existing 5 skills and root contract).
- **Zero files to delete**.

---

## 7. Slicing & Implementation Strategy

Implementation will proceed across 3 surgical slices:

```text
Slice 1: Root Policy, State Machine & Orchestration Boundaries
- AGENTS.md (Lean SSOT streamlining, strip procedural commands, Layer 0 structural definition)
- managed-agent-workflow (SKILL.md, controller-state-machine.md, reconciliation-protocol.md)
  [Resolves Findings 2, 3, 4, 5, Suggestion 2]
      │
Slice 2: Review Rubrics, Falsification Architecture & Planning Decoupling
- code-review-and-quality (SKILL.md, plan-review-rubric.md, implementation-review-rubric.md)
- implementation-planning-and-contract-freeze (SKILL.md, workstream-plan-template.md, slicing-and-dependency-strategies.md)
  [Resolves Findings 1, 2, 6, 7]
      │
Slice 3: Verification Strategy, Orthogonal Sets & Git Lineage
- test-and-verification-strategy (SKILL.md, verification-layers-and-tooling.md)
- git-checkpoint-workflow (SKILL.md, checkpoint-lifecycle-and-lineage.md)
  [Resolves Findings 4, 7, Suggestion 1]
```

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
  - Main Controller computes `candidate_plan_hash`, passes it in Partition 2 to Plan Reviewer `R` citing materialized baseline at `scratch/bootstrap-governance/`.
  - Plan Reviewer `R` inspects baseline authority, provides `Proof of Authority Consumption`, and evaluates candidate plan using the 4-part counterexample/falsification method.
  - Upon explicit `R verdict: PASS`, Main Controller recomputes post-review hash, asserts equality, records `frozen_plan_hash`, and creates the Plan Freeze Checkpoint.
- **Checkpoint 2: Surgical Implementation of Slices 1–3**
  - Implementor `I` applies changes for Slices 1, 2, and 3.
- **Checkpoint 3: Proportional Verification & Manifest Assembly**
  - Implementor executes Layer 0 structural checks and records outputs in `docs/workstreams/agent-architecture-redesign/verification.md`.
  - Main Controller assembles Verification Evidence Manifest.
- **Checkpoint 4: Implementation Review Gate & Verified Implementation Checkpoint**
  - Implementation Reviewer `IR` inspects baseline authority, provides `Proof of Authority Consumption`, and evaluates implementation using the 4-part counterexample/falsification method.
  - Upon explicit `IR verdict: PASS`, Main Controller creates the Verified Implementation Checkpoint and parks in `COMPLETED` awaiting Owner disposition.

---

## 10. Residual Risks & Mitigation Safeguards

| Risk Category | Assessed Risk Level | Mitigation Safeguard |
| :--- | :--- | :--- |
| **Bootstrap Circular Authority** | Mitigated | Materialized baseline files at `scratch/bootstrap-governance/` cited in prompt Partition 1; mandatory `Proof of Authority Consumption` header in review reports. |
| **Passive Reviewer Approvals** | Mitigated | Rubrics mandate 4-part counterexample and falsification structure for all evaluation dimensions. |
| **Reviewer Privilege Bleed** | Controlled | Reviewers configured with `enable_write_tools: false` and zero terminal commands; Main Controller owns all hash computations and git commits. |
| **State Machine Deadlock on Blocker** | Controlled | Total 3-verdict algebra with explicit Prerequisite Repair Protocol governs review outcomes deterministically. |
| **Premature Audit Immutability** | Mitigated | Two-phase commit model distinguishes provisional local coordination checkpoints from promoted immutable audit milestones. |
| **SSOT Drift & Documentation Bloat** | Mitigated | `AGENTS.md` line budget strictly enforced (< 250 lines); procedural git and test mechanics delegated to specialized skills. |

---

## 11. Candidate Plan Handoff & Review Readiness

Planner `P` has completed substantive discovery, applied the Subtraction First discipline, and authored this candidate implementation plan addressing the 7 Required Owner Findings and 2 Suggestions.

- **Canonical Plan Path:** `docs/workstreams/agent-architecture-redesign/plan.md`
- **Handoff State:** Semantic candidate handoff `{ candidate_path: "docs/workstreams/agent-architecture-redesign/plan.md", ready: true }`.
- **Status:** Ready for Main Controller to compute `candidate_plan_hash` and dispatch to Plan Reviewer `R` citing the materialized baseline at `scratch/bootstrap-governance/`.
