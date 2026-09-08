# Workstream Plan: Agent Architecture Redesign (Phase 2 — Modular Workflow Skills & Governance Hardening)

## Status & Ownership

| Field | Value |
| :--- | :--- |
| **Workstream ID** | `agent-architecture-redesign` |
| **Document Role** | Candidate Workstream Plan (Ready for Independent Plan Review) |
| **Baseline Commit** | `d6a051a` (`feat/agent-architecture-redesign`) |
| **Target Branch** | `feat/agent-architecture-redesign` |
| **Canary Lineage** | `2a329a5` (plan freeze), `8ee3d27` (implementation), `17c9e79` (canary verification) on `fix/weight-based-move-damage` |
| **Author Submission State** | `Candidate Plan (Ready for Review)` |

---

## 1. Context, Retrospective & The 9 Owner Architectural Findings

In Phase 1 of this workstream, `cobbleverse-hell-mode-modernized` established `AGENTS.md` as the repository Single Source of Truth (SSOT), created the initial `managed-agent-workflow`, and validated the multi-agent topology through the `fix/weight-based-move-damage` canary (`2a329a5`, `8ee3d27`, `17c9e79`).

In Phase 2, the workflow was decomposed from a monolithic skill into specialized modular skills:
- `managed-agent-workflow`: Orchestration-only state machine.
- `implementation-planning-and-contract-freeze`: Substantive discovery & planning.
- `code-review-and-quality`: Independent adversarial review.
- `git-checkpoint-workflow`: Staging, local checkpoints, and lineage governance.
- `test-and-verification-strategy`: Proportional verification and evidence manifest.

Following candidate review of Architecture v2, the Owner issued **9 [Required] Architectural Findings** that must be rigorously resolved in this plan:

1. **Self-Modifying Governance Bootstrap Contract Missing:** When a task modifies `AGENTS.md` or `.agents/skills/`, the reviewer prompt cannot use current working-tree `AGENTS.md` as authority (circular authority). Main Controller must snapshot `bootstrap_commit` at task start and designate working-tree governance as untrusted candidate claims.
2. **Mode 3 Classifier & Handoff Boundary Gaps:** `AGENTS.md` lacks explicit hard Mode 3 signals for governance/architecture changes, workflow migrations, or explicit review requests. Main Controller lacked an explicit `MODE_3_SELECTED` state and action whitelist/blacklist, leading to Main speculatively exploring external repositories.
3. **Review Rubrics Breaking Proportional Verification:** Review rubrics hardcoded Layer 1 (`validate_repo.py`) and Layer 3 (`test_rct_runtime_contract.py`) for all implementations, contradicting the proportional verification rule that Markdown/governance requires only Layer 0.
4. **Generic Workflow Skills Contaminated by Domain Knowledge:** Generic skills and rubrics hardcoded Pokémon Doubles concepts (Fair-AI, Mixins, lead pairs, pivots, held items). Generic skills must enforce repo-global invariants and route domain-specific invariants to matching domain skills.
5. **Frozen Plan Stale Upon Freeze:** Dynamic workflow statuses (`Awaiting Re-Review`, `Cycle 1`, `Frozen`) inside `plan.md` become stale upon freeze, and modifying them invalidates the cryptographic hash. The plan must be a static candidate contract; dynamic workflow state belongs to controller state and commit metadata.
6. **Reviewer Assigned Impossible Tooling (Cryptographic Hashing):** Hard read-only reviewers have no terminal and cannot run `git hash-object`. Cryptographic identity computation and assertion belong 100% to Main Controller.
7. **Unclosed Verdict Contract & Half-Way Audit Verdicts:** `REJECTED_APPROACH` was defined in the skill but missing from rubrics and controller state transitions; `controller-state-machine.md` froze plans without recording an explicit `R verdict: PASS`.
8. **Git Lineage Policy Overfitting Canary Rules:** Immutability was dogmatically applied to all checkpoints and `--no-ff` was mandated repo-wide; commit templates hardcoded inapplicable test suites and overclaimed canary stability.
9. **`AGENTS.md` Duplicating Delegated Methodology:** `AGENTS.md` duplicated staging commands, checkpoint types, commit templates, and verification commands, violating SSOT.

---

## 2. Concrete Facts vs. Assumptions vs. Conflicts

| Category | Item Description | Evidence / Citation | Impact / Resolution |
| :--- | :--- | :--- | :--- |
| **Confirmed Fact** | Read-only subagents possess zero terminal access | Subagent tool manifests do not include `run_command` | Reviewers cannot run `git hash-object`; Main Controller must own 100% of hash calculation and assertion. |
| **Confirmed Fact** | `d6a051a` modularized skills but left domain and methodology duplication | `git log -n 1 d6a051a`, inspect `.agents/skills/` | Requires surgical restructuring across all 5 workflow skills and `AGENTS.md`. |
| **Confirmed Fact** | Layer 0 is authoritative for Markdown and governance | `test-and-verification-strategy/SKILL.md:41` | Review rubrics and plan verification must NOT mandate Layer 1/3 suites for pure Markdown/skill tasks. |
| **Confirmed Fact** | Historical canary lineage must remain intact | `git log -n 3 --oneline fix/weight-based-move-damage` (`2a329a5`, `8ee3d27`, `17c9e79`) | Promoted audit SHAs are preserved; provisional local commits may be rewritten if approved by Owner. |
| **Assumption** | Deleting `REJECTED_APPROACH` simplifies state machine without losing rigor | Owner Finding 7 explicitly permits pruning `REJECTED_APPROACH` in favor of `Critical` blocking findings | Standardize on 3 closed verdicts (`PASS`, `BLOCKING_FINDINGS`, `BLOCKED`). |
| **Conflict** | `controller-state-machine.md` cited root `AGENTS.md` as SSOT in review prompt while task modifies `AGENTS.md` | `controller-state-machine.md:63` | Resolved by introducing `bootstrap_commit` snapshot as authoritative governance baseline. |

---

## 3. Scope Boundaries & Blast Radius

### 3.1 Strict In-Scope
- `AGENTS.md`: Update Mode 3 classifier, add `MODE_3_SELECTED` boundary & whitelist, streamline Sections 6 & 7 to high-level policies/invariants, prune duplicated mechanics.
- `.agents/skills/managed-agent-workflow/`:
  - `SKILL.md`: Document bootstrap snapshot, Main Controller hash ownership, 3 closed verdicts, explicit verdicts for all gates.
  - `references/controller-state-machine.md`: Add `bootstrap_commit` and `governance_contract_ref` to state schema; codify `MODE_3_SELECTED` state and transitions; update Reviewer Invocation Prompt Contract; require explicit `R verdict: PASS` before Plan Freeze Checkpoint; eliminate `REJECTED_APPROACH`.
  - `references/reconciliation-protocol.md`: Align verdicts and cycle handling.
- `.agents/skills/implementation-planning-and-contract-freeze/`:
  - `SKILL.md`: Replace hardcoded domain invariants with repo-global invariants + domain routing; update sequence diagram to show Main Controller owns hashing.
  - `references/workstream-plan-template.md`: Update metadata header to static candidate contract (`Candidate Plan (Ready for Review)`); remove dynamic lifecycle statuses; generalize domain examples.
  - `references/slicing-and-dependency-strategies.md`: Cleanse Pokémon Doubles examples (lead pairs, pivots, held items); route to `competitive-pokemon-doubles-team-design`; provide generic layer slicing patterns.
- `.agents/skills/code-review-and-quality/`:
  - `SKILL.md`: Remove `REJECTED_APPROACH` (standardize on `PASS`, `BLOCKING_FINDINGS`, `BLOCKED`); clarify reviewer read-only boundary regarding hashes; clarify bootstrap governance authority.
  - `references/plan-review-rubric.md`: Preflight uses `bootstrap_commit` authority for governance reviews; remove candidate hash verification from reviewer; generalize Dimension 4 to repo-global + routed domain invariants; update Dimension 6 to proportional verification.
  - `references/implementation-review-rubric.md`: Update Dimension 5 to evaluate applicable verification layers proportionally; generalize Dimension 3 to repo-global + routed domain invariants; standardize 3 verdicts.
- `.agents/skills/git-checkpoint-workflow/`:
  - `SKILL.md`: Refine lineage policy (preserve promoted audit SHAs; allow provisional local rewrites if authorized); clarify `--no-ff` is recommended for audit lineages, not a repo-wide dogma.
  - `references/checkpoint-lifecycle-and-lineage.md`: Update Verified Implementation template for proportional verification; update Canary template to avoid overclaiming ("STABLE"); require explicit `R verdict: PASS` in Plan Freeze Checkpoint.
- `.agents/skills/test-and-verification-strategy/`:
  - `SKILL.md` & `references/verification-layers-and-tooling.md`: Reinforce Layer 0 sufficiency for Markdown/governance; update manifest template for proportional reporting.
- `docs/workstreams/agent-architecture-redesign/plan.md`: This document.

### 3.2 Explicit Out-of-Scope
- Zero modifications to Java code (`src/`), Cobblemon Mixins, or datapack JSONs (`data/`).
- Zero modifications to `competitive-pokemon-doubles-team-design` domain skill logic.
- Zero remote Git operations (`git push`, PR creation, remote branch manipulation).

---

## 4. Deep Resolution Architecture for the 9 Findings

### 4.1 Finding 1: Self-Modifying Governance Bootstrap Contract
- **Problem:** When `AGENTS.md` or `.agents/skills/` are modified, the reviewer prompt instructed that root `AGENTS.md` is authority. The reviewer could evaluate candidate governance against the unapproved candidate itself.
- **Architecture Solution:**
  1. **Ephemeral State Schema Enhancement:** Add `bootstrap_commit` and `governance_contract_ref` to `controller-state.json`:
     ```json
     {
       "phase": "MODE_3_SELECTED | PLANNING | ...",
       "bootstrap_commit": "<SHA-1>",
       "governance_contract_ref": "<bootstrap_commit>:AGENTS.md",
       "governance_modified_in_task": true,
       ...
     }
     ```
  2. **Task Start Snapshot:** Upon transitioning from `IDLE` to `MODE_3_SELECTED`, Main Controller snapshots `bootstrap_commit = git rev-parse HEAD`.
  3. **Reviewer Invocation Prompt Contract (Partition 1 & 2 Demarcation):**
     - *Partition 1 (Immutable Invariants & Governance Authority):* Explicitly references `<bootstrap_commit>:AGENTS.md` as the authoritative baseline for repository rules and reviewer contract.
     - *Partition 2 (Untrusted Candidate Anchors & Claims):* If the candidate modifies `AGENTS.md` or `.agents/skills/`, these working-tree files are explicitly demarcated as UNTRUSTED candidate artifacts under evaluation.
  4. **Rubric Guidance:** `plan-review-rubric.md` and `code-review-and-quality/SKILL.md` explicitly instruct reviewers that candidate governance files cannot self-authorize deviations from the bootstrap governance baseline.

### 4.2 Finding 2: Mode 3 Classifier & Handoff Boundary Hardening
- **Problem:** `AGENTS.md` lacked hard Mode 3 signals for governance/architecture tasks, and Main Controller lacked an explicit post-selection boundary, leading to premature external exploration.
- **Architecture Solution:**
  1. **Hard Mode 3 Signals in `AGENTS.md`:**
     - Agent-architecture, workflow skill, or repository governance contract changes (`AGENTS.md`, `.agents/skills/`);
     - Cross-repository workflow porting or workflow migrations;
     - Explicit Owner instruction for independent review, plan freeze, or managed-agent workflow.
  2. **Explicit State Transition:**
     `IDLE` → `MODE_3_SELECTED` → `PLANNING`
  3. **Main Controller Whitelist (Strictly Limited Actions):**
     Upon entering `MODE_3_SELECTED`, Main Controller is permitted ONLY to:
     - Snapshot `bootstrap_commit` (`git rev-parse HEAD`);
     - Check repository status (`git status --short`, `git branch --show-current`);
     - Read the governance contract at `bootstrap_commit`;
     - Transition to `PLANNING` and invoke Planner `P` via `invoke_subagent`.
  4. **Main Controller Blacklist (Strictly Forbidden):**
     Main Controller MUST NOT search for external repositories, clone/locate external sources, inspect implementation directories, crawl files, or compile evidence manifests. Substantive discovery, external source mapping, and technical evidence preparation belong strictly to Planner `P`.

### 4.3 Finding 3: Proportional Verification in Review Rubrics
- **Problem:** `implementation-review-rubric.md` Dimension 5 hardcoded `validate_repo.py` and `test_rct_runtime_contract.py` for all tasks, breaking proportional verification for documentation/governance work.
- **Architecture Solution:**
  1. **Refactor Dimension 5 in `implementation-review-rubric.md`:**
     - Replace hardcoded Layer 1/3 checks with **Proportional Verification Evaluation**:
       *"Did the implementation execute all verification layers applicable to the change's risk profile and modified subsystems, as determined by `test-and-verification-strategy` and the approved frozen plan?"*
     - For Layer 0 changes (Markdown, governance, skills): Require Layer 0 suite (route resolution, relative link integrity, frontmatter schema validation, file line bounds < 500 lines).
     - For Layer 1 changes (datapacks): Require `validate_repo.py` and `check_legacy_baseline.py`.
     - For Layer 2 changes (Java logic): Require `./gradlew test`.
     - For Layer 3 changes (Mixins): Require `test_rct_runtime_contract.py`.
     - For Layer 4/5: Require smoke/canary only when applicable.
  2. **Refactor Dimension 6 in `plan-review-rubric.md`:**
     - Reviewer checks that the plan specifies verification suites proportional to the affected layers, and does NOT mandate inapplicable suites for bounded changes.

### 4.4 Finding 4: Domain Knowledge Decoupling from Generic Skills
- **Problem:** Generic workflow skills (`implementation-planning...`, `code-review-and-quality`, `slicing-and-dependency-strategies.md`) hardcoded Pokémon Doubles terminology (Fair-AI, Mixins, datapack economy, lead pairs, pivots, held items).
- **Architecture Solution:**
  1. **Generic Invariant Rule in `implementation-planning-and-contract-freeze/SKILL.md`:**
     - Replace hardcoded domain checks with:
       *"Evaluate and enforce all repository-global invariants (read before write, surgical scope, claim strength discipline, zero opportunistic refactoring, public contract backward compatibility), PLUS applicable domain invariants routed from matching domain skills."*
     - Domain Invariant Routing Table:
       - Battle AI tasks → Route to `Fair-AI Information Boundary` rules.
       - Fabric Companion tasks → Route to `Fabric Mixin & Bytecode Contract` rules.
       - Trainer Datapack tasks → Route to `Cobbleverse Schema & Economy` rules.
       - Team Design tasks → Route to `competitive-pokemon-doubles-team-design`.
  2. **Cleanse `slicing-and-dependency-strategies.md`:**
     - Remove "Pattern B: Vertical Slices by Battle Domain" (lead pairs, pivots, held items).
     - Provide generic architectural layer slicing patterns:
       - *Pattern A: Layer-Ascending Pipeline:* Model/Data (Layer 1/2) → Core Logic (Layer 2) → Adapter/Surrogate (Layer 2/3) → Contract Verification (Layer 3) → Consumer Integration.
       - *Pattern B: Subsystem Component Slicing:* Isolated Unit Slices → Inter-Module Contract Slices → Verification & Tooling Slices.
     - Add explicit pointer: Domain-specific team composition slicing belongs to `competitive-pokemon-doubles-team-design/references/`.
  3. **Cleanse Review Rubrics:**
     - Update `plan-review-rubric.md` Dimension 4 and `implementation-review-rubric.md` Dimension 3 to evaluate repo-global invariants and applicable routed domain invariants.

### 4.5 Finding 5: Static Candidate Plan Contract & Dynamic State Separation
- **Problem:** Dynamic workflow states (`Awaiting Re-Review`, `Cycle 1`, `Frozen`) embedded in `plan.md` become stale upon freeze, and updating them after `PASS` invalidates the cryptographic hash (`candidate_plan_hash == post_review_plan_hash`).
- **Architecture Solution:**
  1. **Ontological Model of Workstream Plans:**
     - A workstream plan is an **immutable historical candidate contract** authored by Planner `P` and approved by Plan Reviewer `R`.
     - The document header records the author's submission identity:
       ```markdown
       | **Field** | **Value** |
       | :--- | :--- |
       | **Workstream ID** | `<workstream-id>` |
       | **Document Role** | Candidate Workstream Plan |
       | **Baseline Commit** | `<SHA-1>` |
       | **Target Branch** | `<branch-name>` |
       | **Author Submission State** | `Candidate Plan (Ready for Review)` |
       ```
  2. **Dynamic Workflow State Separation:**
     - Dynamic lifecycle phases (`PLAN_REVIEW`, `FROZEN`, `IMPLEMENTING`, `COMPLETED`), active reconciliation cycles, and subagent IDs belong exclusively to:
       - Main Controller session memory / ephemeral scratch (`controller-state.json`);
       - Git commit metadata (e.g., `docs(plan): freeze implementation plan for <scope>`);
       - Optional living workstream tracking documents (`docs/workstreams/<id>/progress.md`), NOT inside the frozen plan artifact.
  3. **Template Update:** Remove mutable lifecycle options (`Status = Frozen`, etc.) from `workstream-plan-template.md`.

### 4.6 Finding 6: Main Controller Ownership of Cryptographic Hashing
- **Problem:** `code-review-and-quality/SKILL.md` and `plan-review-rubric.md` instructed Plan Reviewer to verify candidate hash against the on-disk plan, but read-only reviewers have no terminal tool (`run_command`).
- **Architecture Solution:**
  1. **100% Main Controller Hash Ownership:**
     - *Pre-Review:* Main Controller executes `candidate_plan_hash = git hash-object <plan_path>` using `run_command`. Main passes the candidate path and candidate hash into Partition 2 of the reviewer prompt.
     - *Review Execution:* Reviewer reads the artifact at `plan_path` using read tools (`view_file`), evaluates content against repository evidence, and issues a verdict. Reviewer does NOT execute git commands.
     - *Post-Review Handshake:* Upon receiving `PASS`, Main Controller executes `post_review_plan_hash = git hash-object <plan_path>`. Main asserts `candidate_plan_hash == post_review_plan_hash`. If hashes match, Main records `frozen_plan_hash` and commits the plan. If they mismatch, the review is void.
  2. **Rubric & Skill Edits:**
     - In `plan-review-rubric.md`: Preflight check instructs reviewer to verify that the candidate plan file exists and is readable at the specified path, removing hash calculation.
     - In `implementation-planning.../SKILL.md`: Update sequence diagram and text to show Main Controller computes and asserts hashes.

### 4.7 Finding 7: Closed 3-Verdict Model & Explicit Audit Verdicts
- **Problem:** `REJECTED_APPROACH` was defined in `code-review-and-quality/SKILL.md` but missing from reviewer prompts, rubrics, and controller state machine. Also, `controller-state-machine.md` omitted recording an explicit `R verdict: PASS` before committing Plan Freeze Checkpoint.
- **Architecture Solution:**
  1. **Standardize on Exactly 3 Closed Verdicts:**
     Across all skills, rubrics, prompt contracts, and controller state machines:
     - `PASS`: Zero `Critical` and zero `Required` findings.
     - `BLOCKING_FINDINGS`: One or more `Critical` or `Required` findings. Triggers a reconciliation cycle (up to `MAX_CYCLES = 2`).
     - `BLOCKED`: Review cannot proceed due to missing prerequisite artifacts, unreadable files, or missing dependencies.
     - *Prune `REJECTED_APPROACH`:* Fatal design defects are classified as `Critical` severity blocking findings with clear evidence and required architectural resolution.
  2. **Mandatory Explicit Audit Verdicts for ALL Review Gates:**
     - *Plan Review Gate:* Reviewer emits `## Verdict: PASS`. Main Controller must record explicit `R verdict: PASS` in controller state and in the Plan Freeze commit body before freezing or spawning Implementor.
     - *Implementation Review Gate:* Reviewer emits `## Verdict: PASS`. Main Controller must record explicit `IR verdict: PASS` in controller state and in the Verified Implementation commit body before marking completed.
  3. **State Machine Transitions:**
     Update `controller-state-machine.md` transition table to explicitly enforce verdict recording at both gates.

### 4.8 Finding 8: Proportional Git Lineage Policy & Commit Templates
- **Problem:** `AGENTS.md` and `git-checkpoint-workflow` made every checkpoint immutable, dogmatically mandated `--no-ff` repo-wide, hardcoded test suites in commit templates, and used hyperbolic claims ("STABLE") in canary templates.
- **Architecture Solution:**
  1. **Promoted Audit Checkpoints vs. Provisional Local Commits:**
     - *Promoted Audit Milestones:* Commits that have been promoted or referenced as audit evidence (e.g., Plan Freeze Checkpoint referenced in implementation prompts, Verified Implementation Checkpoints referenced in review reports, or historical canary commits `2a329a5`, `8ee3d27`, `17c9e79`) must **never** be rebased, squashed, amended, or deleted.
     - *Provisional Local Commits:* Local, unpromoted commits on a working branch prior to audit promotion may be amended, squashed, or rewritten if explicitly requested or approved by the Owner.
     - *Branch Integration:* Forward merges (`git merge --no-ff`) are recommended when preserving multi-agent audit lineage is required for the workstream, but are not an inflexible dogma that forbids standard Git operations across the repository.
  2. **Proportional Verified Implementation Commit Template:**
     ```text
     <type>(<scope>): <summary in lowercase imperative>

     - Implemented <component / feature details>
     - Verification: <applicable suites and exit codes per verification strategy>
     - Review Verdict: PASS (by Implementation Reviewer IR)
     - Plan Reference: docs/workstreams/<id>/plan.md@<frozen-hash>
     ```
  3. **Proportional Canary Checkpoint Template:**
     Eliminate hyperbolic claims ("confirms gameplay stability / STABLE"). Replace with observable empirical evidence:
     ```text
     docs(workstream): record observed production canary evidence for <feature>

     - Server: dedicated canary host
     - Canary scope: <scenarios and battle formats tested>
     - Observed telemetry: <concrete observed metrics, player interactions, ticks without crash>
     - Observed limitations: <untested edge cases or remaining risks>
     ```

### 4.9 Finding 9: Single Source of Truth (SSOT) Cleanliness in `AGENTS.md`
- **Problem:** `AGENTS.md` duplicated staging commands, checkpoint types, commit templates, and verification commands, violating SSOT and risking documentation drift.
- **Architecture Solution:**
  1. **`AGENTS.md` Retains High-Level Authority & Invariants:**
     - *Section 6 (Verification Authority & Environment Reality):* Retains high-level 6-layer authority summary, Core Invariant (Offline PASS != Production Semantic PASS), and claim strength discipline. Delegates layer commands, tooling, and freshness protocols to `test-and-verification-strategy`.
     - *Section 7 (Git Safety & Local Checkpoint Contract):* Retains permission boundaries (Mode 2 vs Mode 3 local checkpoint authorization), remote action gates (`commit != push != PR != merge`), surgical staging principle, audit lineage preservation principle, and diff reporting policy. Delegates exact checkpoint types, commit message templates, and staging commands to `git-checkpoint-workflow`.
  2. **Detailed Mechanics Owned Exclusively by Skills:**
     - `git-checkpoint-workflow`: Owns exact git commands, staging commands, checkpoint lifecycle mechanics, Conventional Commit templates, and merge mechanics.
     - `test-and-verification-strategy`: Owns exact layer execution commands, test scripts, artifact freshness conditions, and evidence manifest formats.

---

## 5. Exact Files to Create / Modify / Delete

### Files to Modify

| Target File | Modifying Rationale & Scope | Related Findings |
| :--- | :--- | :--- |
| `AGENTS.md` | Add hard Mode 3 triggers; codify `MODE_3_SELECTED` state and Main whitelist/blacklist; streamline Section 6 (delegate commands to test skill); streamline Section 7 (delegate checkpoint types & templates to git skill); refine lineage policy. | Findings 2, 8, 9 |
| `.agents/skills/managed-agent-workflow/SKILL.md` | Document bootstrap governance snapshot; Main Controller hash ownership; closed 3-verdict model; explicit audit verdicts for both gates. | Findings 1, 6, 7 |
| `.agents/skills/managed-agent-workflow/references/controller-state-machine.md` | Add `bootstrap_commit` and `governance_contract_ref` to state schema; codify `MODE_3_SELECTED` state and transitions; update Reviewer Invocation Prompt Contract; require explicit `R verdict: PASS` before Plan Freeze; remove `REJECTED_APPROACH`. | Findings 1, 2, 6, 7 |
| `.agents/skills/managed-agent-workflow/references/reconciliation-protocol.md` | Align verdicts with closed 3-verdict model; ensure arbitration dossier uses structured schema. | Finding 7 |
| `.agents/skills/implementation-planning-and-contract-freeze/SKILL.md` | Replace hardcoded domain invariants with repo-global invariants + domain routing; update sequence diagram to show Main Controller owns hashing. | Findings 4, 6 |
| `.agents/skills/implementation-planning-and-contract-freeze/references/workstream-plan-template.md` | Update metadata header to static candidate contract (`Candidate Plan (Ready for Review)`); remove dynamic lifecycle statuses; generalize domain examples. | Findings 4, 5 |
| `.agents/skills/implementation-planning-and-contract-freeze/references/slicing-and-dependency-strategies.md` | Cleanse Pokémon Doubles examples; route domain team slicing to domain skill; provide generic layer slicing patterns. | Finding 4 |
| `.agents/skills/code-review-and-quality/SKILL.md` | Remove `REJECTED_APPROACH` (standardize on `PASS`, `BLOCKING_FINDINGS`, `BLOCKED`); clarify reviewer read-only boundary regarding hashes; clarify bootstrap governance authority. | Findings 1, 6, 7 |
| `.agents/skills/code-review-and-quality/references/plan-review-rubric.md` | Preflight uses `bootstrap_commit` authority; remove candidate hash verification from reviewer; generalize Dimension 4 to repo-global + routed domain invariants; update Dimension 6 to proportional verification; enforce explicit `R verdict`. | Findings 1, 3, 4, 6, 7 |
| `.agents/skills/code-review-and-quality/references/implementation-review-rubric.md` | Update Dimension 5 to proportional verification; generalize Dimension 3 to repo-global + routed domain invariants; standardize 3 verdicts; enforce explicit `IR verdict`. | Findings 3, 4, 7 |
| `.agents/skills/git-checkpoint-workflow/SKILL.md` | Refine lineage policy (preserve promoted audit SHAs; allow provisional local rewrites if authorized); clarify `--no-ff` is recommended for audit lineages, not a repo-wide dogma. | Finding 8 |
| `.agents/skills/git-checkpoint-workflow/references/checkpoint-lifecycle-and-lineage.md` | Update Plan Freeze section for explicit `R verdict: PASS`; update Verified Implementation template for proportional verification; update Canary template to avoid overclaiming ("STABLE"); update lineage rules. | Findings 7, 8 |
| `.agents/skills/test-and-verification-strategy/SKILL.md` | Reinforce Layer 0 sufficiency for Markdown/governance tasks. | Finding 3 |
| `.agents/skills/test-and-verification-strategy/references/verification-layers-and-tooling.md` | Update manifest template for proportional reporting. | Finding 3 |
| `docs/workstreams/agent-architecture-redesign/plan.md` | This implementation plan. | Findings 1–9 |

### Files to Create / Delete
- Zero new files to create (the 5 modular skills and references already exist from initial modularization; we are surgically hardening them).
- Zero files to delete (obsolete Phase 1 references were already pruned in `d6a051a`).

---

## 6. Slicing & Implementation Strategy

Implementation will proceed across 3 surgical, cohesive slices:

```text
Slice 1: Governance & Orchestration Core
- AGENTS.md (Mode 3 triggers, MODE_3_SELECTED whitelist/blacklist, SSOT streamlining)
- managed-agent-workflow (bootstrap commit snapshot, state transitions, prompt contract, 3 verdicts)
  [Resolves Findings 1, 2, 7, 9]
      │
Slice 2: Planning & Review Hardening
- implementation-planning-and-contract-freeze (domain decoupling, generic slicing, template freeze identity, hash ownership)
- code-review-and-quality (bootstrap review authority, hash check removal, proportional rubrics, 3 verdicts)
  [Resolves Findings 1, 3, 4, 5, 6, 7]
      │
Slice 3: Git Governance & Proportional Verification
- git-checkpoint-workflow (lineage policy refinement, proportional commit templates, explicit R verdict)
- test-and-verification-strategy (Layer 0 sufficiency reinforcement, manifest template alignment)
  [Resolves Findings 3, 8, 9]
```

---

## 7. Proportional Verification Plan (Layer 0 Authoritative)

In strict accordance with the Proportional Verification Principle (Finding 3), this workstream modifies **only** Markdown governance documentation and skill definitions. Zero Java classes, zero Mixin bytecode, and zero datapack JSONs are created or modified.

Therefore, **Layer 0 is the sole authoritative verification layer for this implementation**:

### Automated Verification Checks (Layer 0)
1. **Skill Route Integrity Check:**
   Verify that all skill routes referenced in `AGENTS.md` Section 5 resolve to existing files on disk:
   - `.agents/skills/managed-agent-workflow/SKILL.md`
   - `.agents/skills/implementation-planning-and-contract-freeze/SKILL.md`
   - `.agents/skills/code-review-and-quality/SKILL.md`
   - `.agents/skills/git-checkpoint-workflow/SKILL.md`
   - `.agents/skills/test-and-verification-strategy/SKILL.md`
   - `.agents/skills/competitive-pokemon-doubles-team-design/SKILL.md`
2. **Internal Reference Link Integrity Check:**
   Verify that every relative markdown link in `references/*.md` across all 6 skills resolves to an existing file.
3. **YAML Frontmatter Schema Validation:**
   Verify that every `SKILL.md` contains valid YAML frontmatter with `name` (matching directory name in kebab-case) and a non-empty `description`.
4. **File Line Count Bounds Check:**
   Verify that every `SKILL.md` and reference file remains within concise bounds (< 500 lines per file).
5. **Git Diff Confinement Audit:**
   `git diff --stat` confirms modifications are strictly confined to `AGENTS.md`, `.agents/skills/`, and `docs/workstreams/agent-architecture-redesign/plan.md`. Zero drift in `src/` or `data/`.

### Inapplicable Higher Layers (Explicitly Skipped per Verification Strategy)
- **Layer 1 (`validate_repo.py`):** Inapplicable (0 datapack JSONs modified).
- **Layer 2 (`./gradlew test`):** Inapplicable (0 Java classes modified).
- **Layer 3 (`test_rct_runtime_contract.py`):** Inapplicable (0 Mixins or bytecode modified).
- **Layer 4 (`./gradlew runServer`):** Inapplicable (no runtime bootstrap risk).
- **Layer 5 (Canary Host):** Inapplicable (no gameplay changes).

---

## 8. Migration & Implementation Checkpoints

### Checkpoint 1: Planning Freeze & Candidate Hash (Current Turn)
- Planner `P` completes substantive discovery and authors this comprehensive plan.
- Calculates `candidate_plan_hash = git hash-object docs/workstreams/agent-architecture-redesign/plan.md`.
- Delivers candidate plan to Main Controller and declares readiness for Independent Plan Review.

### Checkpoint 2: Surgical Implementation of Slices 1–3
- Following Plan Freeze Checkpoint by Main Controller, Implementor `I` executes:
  - Slice 1: Core Governance & Orchestration (`AGENTS.md`, `managed-agent-workflow`).
  - Slice 2: Planning & Review Hardening (`implementation-planning...`, `code-review-and-quality`).
  - Slice 3: Git Governance & Verification (`git-checkpoint-workflow`, `test-and-verification-strategy`).

### Checkpoint 3: Layer 0 Proportional Verification & Manifest Assembly
- Implementor executes all Layer 0 automated checks (routes, links, frontmatters, line bounds).
- Assembles structured Verification Evidence Manifest.

### Checkpoint 4: Implementation Review & Verified Local Checkpoint
- Implementation Reviewer `IR` conducts review against `implementation-review-rubric.md`.
- Emits explicit `IR verdict: PASS`.
- Main Controller records explicit `IR verdict: PASS` and creates Verified Implementation Checkpoint.

---

## 9. Residual Risks & Safeguards

| Risk Category | Risk Level | Mitigation Safeguard |
| :--- | :--- | :--- |
| **Self-Modifying Governance Drift** | Very Low | Bootstrap commit snapshot ensures candidate governance changes are reviewed against the baseline authority. |
| **Reviewer Privilege Bleed** | Zero | Reviewers remain strictly read-only (`enable_write_tools: false`, zero terminal tools). Hash computation is owned 100% by Main. |
| **Documentation Bloat / Drift** | Very Low | Line count bounds (< 500 lines per file) and SSOT delegation from `AGENTS.md` prevent drift and bloat. |
| **Audit History Loss** | Zero | Promoted audit commits (`2a329a5`, `8ee3d27`, `17c9e79`, and future freeze commits) are protected against rebasing or rewriting. |

---

## 10. Candidate Plan Handoff & Review Readiness

Planner `P` has completed substantive discovery and authored this exhaustive implementation plan resolving all 9 [Required] Owner findings.
The candidate plan is positioned at canonical path:
`docs/workstreams/agent-architecture-redesign/plan.md`

Ready for Main Controller to compute `candidate_plan_hash` and dispatch to Plan Reviewer `R` for independent review.
