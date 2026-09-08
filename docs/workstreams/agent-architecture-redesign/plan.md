# Workstream Plan: Agent Architecture Redesign (Phase 2 — Owner-Review Correction & Governance Hardening)

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

## 1. Context, Bootstrap Governance & The 9 Owner Correction Findings

In Phase 1 of this workstream, `cobbleverse-hell-mode-modernized` established `AGENTS.md` as the repository Single Source of Truth (SSOT), modularized the multi-agent workflow into specialized skills, and demonstrated execution on the `fix/weight-based-move-damage` canary (`2a329a5`, `8ee3d27`, `17c9e79`).

Following Owner review of Architecture Phase 2 at commit `1fa1d58`, the Owner established this **Correction Gate** with nine [Required] Architectural Findings (Findings A through I).

### Bootstrap Governance Authority Invariant
Because this task modifies the governance system itself (`AGENTS.md` and `.agents/skills/`), **governance rules active at TASK START (`bootstrap_commit = 1fa1d58`) remain authoritative for this entire run.** Working-tree candidate governance files are untrusted review subjects and can NEVER become authority for reviewing themselves. The candidate architecture is generalized around: `bootstrap_governance_manifest`.

### Correction Philosophy: Subtraction First
The corrected architecture reduces conceptual ambiguity, eliminates ceremony, and closes semantic loopholes using the smallest set of strong invariants:
1. Identify the root invariant preventing each bug class.
2. Delete, consolidate, or simplify conflicting rules.
3. Do NOT create new states, fields, files, protocols, checklists, or exceptions unless existing primitives cannot express the invariant.
4. Remove obsolete rules after introducing stronger invariants.
5. Ensure exactly ONE owner remains for each fact and procedure.

### The 9 Owner Correction Findings (A–I)

- **FINDING A — Bootstrap circular authority:** Current design snapshots `AGENTS.md` but may still resolve rubrics/skills from the working tree. All governance authority used during a run must resolve exclusively from `bootstrap_governance_manifest`. Reviewer invocation prompts must strictly partition: (1) Immutable Authority / Invariants from bootstrap manifest, (2) Untrusted Candidate Anchors / Claims from working tree, and (3) Authoritative Rubrics from bootstrap manifest.
- **FINDING B — Main owns runtime identity, not Planner:** Planner outputs purely semantic candidate content: `{ candidate_path, ready: true }`. Main Controller owns 100% of runtime identity, candidate hashing (`git hash-object`), assertions, session tracking, cycle counting, bootstrap manifest, and checkpoint commits. Reviewer does NOT compute hashes; Planner does NOT compute hashes.
- **FINDING C — Verdict algebra must be TOTAL:** Reviewer outcomes are total over `{ PASS, BLOCKING_FINDINGS, BLOCKED }` for every review phase (`PLAN_REVIEW` and `IMPL_REVIEW`). Define explicit transitions for all three outcomes, formalize prerequisite repair boundaries, and enforce mandatory explicit audit verdicts (`R verdict: PASS`, `IR verdict: PASS`) before any dependent transition.
- **FINDING D — Generic workflow skills must be genuinely generic:** Remove Pokémon, Doubles, Fair-AI formulas, lead/pivot/item rules, mandatory Mixin architecture, mandatory pure-Java extraction before Mixin changes, trainer assumptions, and bytecode mechanics from generic workflow methodology. Generic skills state: "apply routed domain contracts where applicable. If no dedicated domain skill exists, route to actual repository docs/source evidence or state that none exists."
- **FINDING E — Verification layers are a SET, not a LADDER:** Verification selection chooses the minimal orthogonal set of verification layers that directly cover contracts affected by the diff. No numeric layer implies another automatically. Markdown structural automation != semantic correctness. Use wording such as "sole applicable automated repository check", NEVER "fully sufficient", "semantically sufficient", or "complete proof".
- **FINDING F — Artifact freshness = provenance/content identity first:** Order of freshness evidence: recorded source revision/commit identity -> source/content hashes -> expected artifact contents/embedded identity -> build metadata -> timestamps only as supporting/fallback. Rebuild required only when available evidence cannot establish freshness. Eliminate cumbersome dependency-cone scraping.
- **FINDING G — Commit creation != audit promotion:** Local workflow checkpoints are internal coordination records, distinct from promoted audit milestones. Audit promotion requires an explicit external event (Owner acceptance, merged historical evidence, accepted production canary evidence). Internal Reviewer PASS alone does NOT promote a checkpoint. Provisional local commits may be rewritten with Owner approval; promoted audit milestones are immutable. `merge --no-ff` is an audit recommendation, not universal dogma. Gated operations: `commit != push != PR != merge != rebase != rewrite != force-push`.
- **FINDING H — Root governance is policy/router, not procedure manual:** `AGENTS.md` is the lean root SSOT for universal invariants, preflight/mode routing, permissions, authority boundaries, and skill routing. Remove duplicated detailed mechanics (staging commands, diff procedures, commit templates, checkpoint types, verification command matrices). Exactly one owner per fact/procedure.
- **FINDING I — Frozen plan is historical contract, not mutable workflow state:** Candidate plan is reviewed at exact identity. After `PASS`, Main records candidate identity and creates the Plan Freeze Checkpoint. Do NOT mutate the frozen artifact afterward to update status, progress, cycle counters, or review outcomes.

---

## 2. Root-Invariant Map

| # | Root Invariant | Owning File / Skill | Redundant Rules, Duplications & Mechanics Removed |
| :--- | :--- | :--- | :--- |
| **1** | **Bootstrap Governance Authority:** All governance authority used during a run resolves exclusively from `bootstrap_governance_manifest`. Candidate files are untrusted review subjects and can never govern their own implementation. | `managed-agent-workflow` (`references/controller-state-machine.md`) | Circular loading of working-tree rubrics; ad-hoc `governance_modified_in_task` state branching; floating rubric links in prompt contracts. |
| **2** | **Single Identity & State Owner:** Main Controller owns 100% of runtime identity, candidate hashing, assertions, session tracking, and state transitions. Planner outputs purely semantic content `{ candidate_path, ready }`. | `managed-agent-workflow` (`SKILL.md`) | Planner-side hash calculation instructions in `implementation-planning...`; reviewer hash verification steps in `code-review-and-quality`; contradictory identity claims in plan templates. |
| **3** | **Total Reviewer Outcome Algebra:** Review outcomes are total over `{ PASS, BLOCKING_FINDINGS, BLOCKED }` for every review phase, with deterministic transitions, prerequisite repair boundaries, and mandatory audit verdicts before dependent transitions. | `managed-agent-workflow` (`references/controller-state-machine.md`) & `code-review-and-quality` (`SKILL.md`) | Partial state machine transitions missing `BLOCKED`; `REJECTED_APPROACH` pseudo-verdict; unclosed error handling; transitions without explicit audit verdicts. |
| **4** | **Domain Decoupling:** Generic workflow skills define pure process, contract lifecycle, and verification algebra; domain-specific rules are routed exclusively to matching domain skills or repository evidence. | `implementation-planning...` & `code-review-and-quality` | Hardcoded Pokémon Doubles tables, Fair-AI formulas, lead/pivot rules, mandatory Mixin architectures, mandatory pure-Java extraction rules, and datapack economy checks in generic workflow skills. |
| **5** | **Orthogonal Verification Sets:** Verification layers are an orthogonal set of domain authorities selected minimally per diff; no layer implies another; Layer 0 checks are the "sole applicable automated repository check" without overclaiming semantic completeness. | `test-and-verification-strategy` (`SKILL.md`) | Vertical verification ladder inheritance table (Low -> Med -> High -> Crit); "fully sufficient" / "semantically sufficient" overclaims for syntax/link checks. |
| **6** | **Provenance-First Artifact Freshness:** Freshness is established via recorded source revision, content hashes, and embedded identity; timestamps are supporting fallbacks; rebuilds occur only when evidence cannot establish freshness. | `test-and-verification-strategy` (`SKILL.md` & `references/verification-layers-and-tooling.md`) | Timestamp-first freshness criteria; massive dependency-cone manifest scanning; rebuilds "merely for certainty". |
| **7** | **Two-Phase Commit Governance:** Local checkpoints are provisional multi-agent coordination records; audit promotion requires an explicit external event; pre-promotion history may be rewritten with Owner approval; post-promotion identity is immutable. | `git-checkpoint-workflow` (`SKILL.md` & `references/checkpoint-lifecycle-and-lineage.md`) | Immediate immutability of provisional local commits; conflation of Reviewer PASS with audit promotion; dogmatic repo-wide `--no-ff` enforcement. |
| **8** | **Lean Root Policy & SSOT:** `AGENTS.md` is the lean root SSOT for universal principles, preflight, mode routing, authority boundaries, and permissions; all procedural mechanics are owned exclusively by delegated skills. | `AGENTS.md` | Duplicated staging commands (`git add`), diff procedures, commit templates, detailed checkpoint types, and test command matrices in `AGENTS.md`. |
| **9** | **Static Candidate Plan Contract:** Workstream plans are immutable candidate contracts fixed at author handoff identity; zero mutable workflow fields post-freeze; dynamic state belongs to runtime memory and git commits. | `implementation-planning...` (`references/workstream-plan-template.md`) | Dynamic status headers (`Frozen`, `Cycle 1`, `Awaiting Review`); in-place editing of plan status after freeze; hash invalidation caused by progress updates. |

---

## 3. Subtraction Report (Subtraction First)

In accordance with the Subtraction First mandate, this redesign removes ceremony and eliminates ambiguity:

### 3.1 Concepts & Rules Removed
1. **Circular Review Authority:** Removed reliance on working-tree governance files or rubrics during review. All authority is resolved strictly from `bootstrap_governance_manifest`.
2. **Planner/Reviewer Hash Ownership:** Removed all instructions assigning hash generation or verification to Planner or Reviewer.
3. **`REJECTED_APPROACH` Verdict:** Eliminated redundant pseudo-verdict; fatal defects are unified under `Critical` blocking findings.
4. **Hardcoded Domain Mechanics in Generic Skills:** Removed Pokémon battle rules, Fair-AI info boundaries, lead/pivot/item rules, mandatory Mixin rules, mandatory pure-Java extraction rules, and trainer schema rules from generic skills.
5. **Sequential Verification Ladder:** Removed the rigid hierarchical ladder where higher layers automatically implied lower layers.
6. **Hyperbolic Sufficiency Claims:** Removed claims that Layer 0 checks are "fully sufficient" or "complete proof" of semantic correctness.
7. **Timestamp-First & Dependency-Cone Scraping:** Removed fragile timestamp comparisons and directory-crawling dependency manifests.
8. **Immediate Immutability of Provisional Local Commits:** Removed policy treating every local checkpoint as an unalterable milestone prior to audit promotion.
9. **Universal `--no-ff` Dogma:** Reframed `--no-ff` as an audit recommendation rather than an inflexible repository dogma.
10. **Procedural Duplication in `AGENTS.md`:** Removed detailed command listings, staging procedures, commit templates, and verification matrices from root governance.
11. **Mutable Post-Freeze Plan State:** Removed dynamic workflow tracking fields from `plan.md`.

### 3.2 Duplicated Authority Removed
- **Git Mechanics:** Exact staging procedures (`git add`), diff rules, and commit message templates removed from `AGENTS.md` and consolidated 100% into `git-checkpoint-workflow`.
- **Verification Commands:** Exact test commands and layer scripts removed from `AGENTS.md` and consolidated 100% into `test-and-verification-strategy`.
- **Identity & Hashing:** Cryptographic hashing instructions removed from `implementation-planning...` and `code-review-and-quality` and consolidated 100% into `managed-agent-workflow`.
- **Evaluation Rubrics:** Rubric criteria removed from prompt contracts and consolidated 100% into `code-review-and-quality`.

### 3.3 States & Fields Avoided
- Avoided adding new state machine phases (unified under `IDLE`, `MODE_3_SELECTED`, `PLANNING`, `PLAN_REVIEW`, `PLAN_RECONCILING`, `FROZEN`, `IMPLEMENTING`, `IMPL_REVIEW`, `IMPL_RECONCILING`, `COMPLETED`, `ESCALATED`).
- Avoided ad-hoc boolean state flags (`governance_modified_in_task`) by establishing the universal `bootstrap_governance_manifest`.
- Avoided complex dependency-cone manifest schemas.
- Avoided mutable status fields in `plan.md` headers.

### 3.4 Files NOT Created
- Zero new files created. The existing 5 modular skills + `AGENTS.md` provide complete, cohesive coverage without creating speculative frameworks, eval harnesses, or custom tooling.

---

## 4. Deep Architectural Resolution for Findings A through I

### 4.1 Finding A: Bootstrap Circular Authority (`bootstrap_governance_manifest`)
- **Problem:** When a workstream modifies `AGENTS.md` or `.agents/skills/`, evaluating candidate files against the working tree creates circular authority where unapproved changes authorize themselves.
- **Root Invariant:** All governance authority used during a run resolves exclusively from `bootstrap_governance_manifest`. Candidate governance files describe future behavior but cannot govern the run implementing them.
- **Implementation Design:**
  1. Upon transitioning to `MODE_3_SELECTED`, Main Controller records `bootstrap_commit` (`git rev-parse HEAD`).
  2. Main Controller establishes `bootstrap_governance_manifest`, mapping all governance documents, skills, and rubrics to their immutable state at `bootstrap_commit`:
     - `AGENTS.md` -> `<bootstrap_commit>:AGENTS.md`
     - Plan Review Rubric -> `<bootstrap_commit>:.agents/skills/code-review-and-quality/references/plan-review-rubric.md`
     - Implementation Review Rubric -> `<bootstrap_commit>:.agents/skills/code-review-and-quality/references/implementation-review-rubric.md`
  3. **Reviewer Invocation Prompt Contract:**
     - *Partition 1 (Immutable Authority & Baseline):* Points exclusively to `bootstrap_governance_manifest`.
     - *Partition 2 (Untrusted Candidate Anchors & Claims):* Working-tree candidate files are passed strictly as untrusted navigation hints.
     - *Partition 3 (Authoritative Rubric):* Content extracted/referenced from `bootstrap_governance_manifest`, never from working tree.

### 4.2 Finding B: Main Ownership of Runtime Identity & State
- **Problem:** Ambiguous ownership across skills led to instructions where Planner computed hashes or Reviewer verified git blobs without terminal access.
- **Root Invariant:** Main Controller strictly owns all runtime identity, session orchestration, and state transitions. Planner outputs purely semantic candidate content `{ candidate_path, ready: true }`.
- **Implementation Design:**
  1. Planner handoff payload contains only `{ candidate_path: "docs/workstreams/<id>/plan.md", ready: true }`.
  2. Main Controller computes `candidate_plan_hash = git hash-object <plan_path>` using `run_command`.
  3. Reviewers receive candidate path and hash as context in Partition 2; reviewers use read tools (`view_file`) and do NOT compute hashes.
  4. Post-Review: Upon receiving `PASS`, Main re-evaluates `post_review_hash = git hash-object <plan_path>`, asserts equality, records `frozen_plan_hash`, and creates the Plan Freeze Checkpoint.
  5. Contradictory text removed from `implementation-planning...` and `code-review-and-quality`.

### 4.3 Finding C: Total Reviewer Outcome Algebra & Prerequisite Repair
- **Problem:** State machine only handled `PASS` and `BLOCKING_FINDINGS`. `BLOCKED` was undefined, creating unclosed transitions.
- **Root Invariant:** Reviewer outcome algebra is total over `{ PASS, BLOCKING_FINDINGS, BLOCKED }` for both `PLAN_REVIEW` and `IMPL_REVIEW`. Mandatory explicit audit verdicts (`R verdict: PASS`, `IR verdict: PASS`) are required before dependent transitions.
- **Implementation Design:**
  1. **Total Transition Matrix:**
     - **`PASS`:**
       - `PLAN_REVIEW` -> `FROZEN`: Main records `R verdict: PASS`, asserts hash, creates Plan Freeze Checkpoint.
       - `IMPL_REVIEW` -> `COMPLETED`: Main records `IR verdict: PASS`, creates Verified Implementation Checkpoint.
     - **`BLOCKING_FINDINGS`:**
       - Content defect in candidate. Increments cycle counter (`reconciliation_count += 1`).
       - If `<= 2`: routes back to author session (`PLAN_RECONCILING` or `IMPL_RECONCILING`).
       - If `> 2`: halts loop, transitions to `ESCALATED`, delivers dossier to Owner.
     - **`BLOCKED`:**
       - Review cannot proceed due to external or prerequisite failure (file unreadable, missing manifest, tool failure).
       - **Prerequisite Repair Protocol:**
         - *Main-Repairable Prerequisite:* If prerequisite can be repaired by Main without changing candidate design (e.g., re-running manifest assembly, fixing file path in prompt), Main repairs it and re-dispatches to existing reviewer session without incrementing candidate reconciliation cycles.
         - *Author-Repairable Prerequisite:* If prerequisite requires author correction (e.g., candidate file missing or empty), Main routes back to author for a repair turn.
         - *External / Unresolvable Blocker:* If prerequisite is external (corrupt environment, contradictory prompt instructions), author and reviewer sessions remain alive/paused; Main halts to `ESCALATED` and delivers blocker dossier to Owner.

### 4.4 Finding D: Domain Decoupling from Generic Workflow Skills
- **Problem:** Generic workflow skills hardcoded Pokémon Doubles, Fair-AI info boundaries, Mixin bytecode rules, and datapack schemas, preventing reuse and contaminating generic methodology.
- **Root Invariant:** Generic workflow skills define pure process, contract lifecycle, and verification algebra; they contain zero domain mechanics and delegate all domain constraints to routed domain contracts.
- **Implementation Design:**
  1. `implementation-planning-and-contract-freeze/SKILL.md`: Replace domain table with generic instruction: "Evaluate and enforce all repository-global invariants (surgical scope, simplicity first, read before write, claim strength discipline, backward compatibility), plus applicable domain invariants routed from matching domain skills. If no dedicated domain skill exists, route to actual repository docs/source evidence or state that none exists."
  2. `slicing-and-dependency-strategies.md`: Delete domain ladder and rules (mandatory Mixin architecture, mandatory pure Java extraction). Replace with generic architectural slicing patterns (Contract/Model Slices, Core Logic Slices, Boundary/Adapter Slices, Verification Slices).
  3. `workstream-plan-template.md`: Generalize template prompts, removing domain-specific examples.
  4. `plan-review-rubric.md` & `implementation-review-rubric.md`: Dimensions 3 & 4 evaluate repository-global invariants and routed domain contracts without hardcoding game mechanics.

### 4.5 Finding E: Verification Layers as an Orthogonal Set & Claim Rigor
- **Problem:** Verification layers were framed as an escalating ladder where higher layers implied lower layers. Automated Layer 0 checks were hyperbolically called "fully sufficient".
- **Root Invariant:** Verification layers are an orthogonal set of domain authorities; diff verification activates the minimal set directly covering affected contracts. Layer 0 checks are the "sole applicable automated repository check", but do not establish semantic correctness.
- **Implementation Design:**
  1. Refactor `test-and-verification-strategy/SKILL.md` to define verification layers as an orthogonal set:
     - Diff -> Identify affected contracts -> Select minimal covering verification layer set.
     - No numeric layer implies another automatically.
  2. Enforce Claim Strength Discipline:
     - Replace "fully sufficient", "semantically sufficient", or "complete proof" with "sole applicable automated repository check".
     - Explicitly state: Markdown structural automation (links, frontmatter, line bounds) verifies only mechanical syntax; it does NOT establish authority correctness, routing correctness, state-machine closure, or ownership.

### 4.6 Finding F: Artifact Freshness via Provenance & Content Identity
- **Problem:** Freshness required crawling large "dependency cones" and relied on timestamps, leading to redundant rebuilds or stale results.
- **Root Invariant:** Artifact freshness is established primarily through provenance and content identity, with timestamps serving only as supporting fallback. Rebuilds occur only when available evidence cannot establish freshness.
- **Implementation Design:**
  1. **Order of Freshness Evidence:**
     1. Recorded source revision / commit identity (exact commit SHA where verification occurred).
     2. Source / content hashes of inputs.
     3. Expected artifact contents / embedded identity.
     4. Build metadata / tool provenance.
     5. Timestamps (supporting fallback only).
  2. Freshness established if working-tree status is clean for the component and recorded commit/content identity matches.
  3. Rebuilds triggered only when inputs changed or provenance is absent. Zero rebuilds merely "for certainty".

### 4.7 Finding G: Local Checkpoint vs. Promoted Audit Milestone Lifecycle
- **Problem:** Checkpoint commits were dogmatically treated as immutable immediately upon creation, conflating local checkpoints with promoted audit milestones, and `--no-ff` was mandated repo-wide.
- **Root Invariant:** Local checkpoint creation is an internal coordination mechanism; audit promotion is an explicit external lifecycle event.
- **Implementation Design:**
  1. **Two-Phase Commit Lifecycle:**
     - *Provisional Local Checkpoints:* Internal coordination commits created during Mode 3 (Plan Freeze, Verified Implementation, Correction). Unpromoted local commits on a working branch may be amended, squashed, or rewritten if explicitly authorized by Owner, reconciling affected references.
     - *Promoted Audit Milestones:* Commits that have received explicit external validation (Owner acceptance, accepted/merged branch, accepted live production canary evidence, or historical canary lineage `2a329a5`, `8ee3d27`, `17c9e79`). Promoted milestones are immutable: never rebased, squashed, or deleted; prefer forward integration.
  2. Internal Reviewer `PASS` alone does NOT promote a checkpoint to immutable audit status.
  3. Forward merges (`git merge --no-ff`) are an audit lineage recommendation when preserving multi-agent history is required, NOT universal dogma.
  4. Gated operations: `commit != push != PR != merge != rebase != rewrite != force-push`.

### 4.8 Finding H: Lean Root Governance (`AGENTS.md` as Policy/Router)
- **Problem:** `AGENTS.md` duplicated staging commands, diff procedures, commit templates, and verification matrices, violating SSOT.
- **Root Invariant:** `AGENTS.md` is the lean root SSOT for universal principles, preflight/mode routing, authority boundaries, permissions, and skill routing; detailed procedures are owned exclusively by delegated skills.
- **Implementation Design:**
  1. Streamline Section 6 of `AGENTS.md`: Retain 6-layer authority summary, Core Invariant (Offline != Production), and claim discipline. Delegate exact commands and freshness protocol to `test-and-verification-strategy`.
  2. Streamline Section 7 of `AGENTS.md`: Retain permission gates (Mode 2 vs 3 commit authority, remote action gates), surgical staging principle, and diff reporting policy. Delegate commit templates and staging mechanics to `git-checkpoint-workflow`.
  3. Exactly ONE owner per fact and procedure across the repository.

### 4.9 Finding I: Static Candidate Plan Contract & Zero Post-Freeze Mutation
- **Problem:** Plans contained mutable lifecycle fields (`Status: Frozen`, `Cycle: 1`) that tempted post-review edits, invalidating candidate cryptographic hashes.
- **Root Invariant:** A workstream plan is a static candidate contract fixed at author handoff identity; zero mutable workflow fields post-freeze; dynamic state belongs to runtime memory and git commits.
- **Implementation Design:**
  1. Standardize plan header on static candidate identity:
     ```markdown
     | Field | Value |
     | :--- | :--- |
     | **Workstream ID** | `<workstream-id>` |
     | **Document Role** | Candidate Workstream Plan (Ready for Independent Plan Review) |
     | **Baseline Commit** | `<SHA-1>` (`<branch-name>`) |
     | **Target Branch** | `<branch-name>` |
     | **Author Submission State** | `Candidate Plan (Ready for Review)` |
     ```
  2. Remove mutable status options (`Frozen`, `Implementing`, etc.) from `workstream-plan-template.md`.
  3. Post-freeze progress tracked via git commit history and optional living progress notes (`progress.md`), never inside the frozen plan.

---

## 5. Scope Boundaries & Blast Radius

### 5.1 Strict In-Scope
- `AGENTS.md`: Lean SSOT streamlining, remove procedural duplication, enforce bootstrap authority rules.
- `.agents/skills/managed-agent-workflow/`:
  - `SKILL.md`: Bootstrap governance manifest, 100% Main Controller hash ownership, closed total 3-verdict model, explicit audit gates.
  - `references/controller-state-machine.md`: Codify `bootstrap_governance_manifest`; total state transitions for `{ PASS, BLOCKING_FINDINGS, BLOCKED }`; Prerequisite Repair Protocol; update Reviewer Invocation Prompt Contract.
  - `references/reconciliation-protocol.md`: Align verdicts with total 3-verdict model and prerequisite repair.
- `.agents/skills/implementation-planning-and-contract-freeze/`:
  - `SKILL.md`: Domain decoupling, remove hash calculation ownership, reference static plan identity.
  - `references/workstream-plan-template.md`: Static candidate contract header, sanitize domain examples, remove post-freeze mutable fields.
  - `references/slicing-and-dependency-strategies.md`: Remove ladder representation and domain-specific rules; provide generic layer slicing patterns.
- `.agents/skills/code-review-and-quality/`:
  - `SKILL.md`: Total 3-verdict model (`PASS`, `BLOCKING_FINDINGS`, `BLOCKED`), reviewer read-only boundary regarding hashes, bootstrap authority separation.
  - `references/plan-review-rubric.md`: Preflight uses `bootstrap_governance_manifest`; remove hash calculation; generalize Dimension 4 to repo-global + routed domain invariants; update Dimension 6 to orthogonal verification sets.
  - `references/implementation-review-rubric.md`: Update Dimension 5 to orthogonal verification sets; generalize Dimension 3; total 3 verdicts.
- `.agents/skills/git-checkpoint-workflow/`:
  - `SKILL.md`: Two-phase commit lifecycle (provisional local checkpoint vs. promoted audit milestone); `--no-ff` as audit recommendation; remote action gates.
  - `references/checkpoint-lifecycle-and-lineage.md`: Explicit `R verdict: PASS` and `IR verdict: PASS` requirements; proportional commit templates.
- `.agents/skills/test-and-verification-strategy/`:
  - `SKILL.md`: Orthogonal verification sets (not a ladder); claim strength discipline ("sole applicable automated repository check"); provenance-first artifact freshness.
  - `references/verification-layers-and-tooling.md`: Update freshness protocol to provenance/content identity; update manifest template.
- `docs/workstreams/agent-architecture-redesign/plan.md`: This workstream plan.

### 5.2 Explicit Out-of-Scope
- Zero modifications to Java code (`src/`), Cobblemon Mixins, or datapack JSONs (`data/`).
- Zero modifications to `competitive-pokemon-doubles-team-design` domain skill logic.
- Zero remote Git operations (`git push`, PR creation, remote branch manipulation).

---

## 6. Exact Files to Create / Modify / Delete

### Files to Modify

| Target File | Modifying Rationale & Scope | Related Findings |
| :--- | :--- | :--- |
| `AGENTS.md` | Lean SSOT streamlining; remove duplicated staging/diff procedures and commit templates; delegate commands to skills; enforce bootstrap authority baseline. | Findings A, G, H |
| `.agents/skills/managed-agent-workflow/SKILL.md` | Codify `bootstrap_governance_manifest`; Main 100% hash ownership; total 3-verdict model; explicit audit gates before transitions. | Findings A, B, C |
| `.agents/skills/managed-agent-workflow/references/controller-state-machine.md` | Add `bootstrap_governance_manifest` to schema; complete total state machine transitions for `{ PASS, BLOCKING_FINDINGS, BLOCKED }`; codify Prerequisite Repair Protocol; update Reviewer Invocation Prompt Contract. | Findings A, B, C |
| `.agents/skills/managed-agent-workflow/references/reconciliation-protocol.md` | Align verdicts with total 3-verdict model and prerequisite repair protocol. | Finding C |
| `.agents/skills/implementation-planning-and-contract-freeze/SKILL.md` | Domain decoupling (route to domain skills); remove hash calculation ownership; reference static candidate plan contract. | Findings B, D, I |
| `.agents/skills/implementation-planning-and-contract-freeze/references/workstream-plan-template.md` | Static candidate contract header; sanitize domain examples; remove post-freeze mutable fields. | Findings D, I |
| `.agents/skills/implementation-planning-and-contract-freeze/references/slicing-and-dependency-strategies.md` | Remove ladder representation and domain-specific rules (Mixin/pure-Java); provide generic layer slicing patterns. | Findings D, E |
| `.agents/skills/code-review-and-quality/SKILL.md` | Total 3-verdict model; reviewer read-only boundary (no hashes); bootstrap governance authority separation. | Findings A, B, C |
| `.agents/skills/code-review-and-quality/references/plan-review-rubric.md` | Preflight uses `bootstrap_governance_manifest`; remove hash calculation; generalize Dimension 4 to repo-global + routed domain invariants; update Dimension 6 to orthogonal verification sets; enforce explicit `R verdict`. | Findings A, B, C, D, E |
| `.agents/skills/code-review-and-quality/references/implementation-review-rubric.md` | Update Dimension 5 to orthogonal verification sets; generalize Dimension 3; total 3 verdicts; enforce explicit `IR verdict`. | Findings C, D, E |
| `.agents/skills/git-checkpoint-workflow/SKILL.md` | Two-phase commit lifecycle (provisional local checkpoint vs. promoted audit milestone); `--no-ff` as audit recommendation; remote action gates. | Finding G |
| `.agents/skills/git-checkpoint-workflow/references/checkpoint-lifecycle-and-lineage.md` | Update Plan Freeze section for explicit `R verdict: PASS`; update Verified Implementation template for proportional verification; update Canary template to avoid overclaiming; two-phase commit rules. | Findings C, G |
| `.agents/skills/test-and-verification-strategy/SKILL.md` | Orthogonal verification sets (not a ladder); claim strength discipline ("sole applicable automated repository check"); provenance-first artifact freshness. | Findings E, F |
| `.agents/skills/test-and-verification-strategy/references/verification-layers-and-tooling.md` | Update freshness protocol to provenance/content identity; update manifest template for orthogonal verification. | Findings E, F |
| `docs/workstreams/agent-architecture-redesign/plan.md` | This candidate implementation plan. | Findings A–I |

### Files to Create / Delete
- **Zero new files to create** (existing 5 modular skills + `AGENTS.md` provide complete, cohesive coverage).
- **Zero files to delete**.

---

## 7. Slicing & Implementation Strategy

Implementation will proceed across 3 surgical, cohesive slices:

```text
Slice 1: Root Policy & Orchestration State Machine
- AGENTS.md (SSOT streamlining, remove procedural duplication, bootstrap authority)
- managed-agent-workflow (SKILL.md, controller-state-machine.md, reconciliation-protocol.md)
  [Resolves Findings A, B, C, H]
      │
Slice 2: Planning, Review Rubrics & Domain Decoupling
- implementation-planning-and-contract-freeze (SKILL.md, workstream-plan-template.md, slicing-and-dependency-strategies.md)
- code-review-and-quality (SKILL.md, plan-review-rubric.md, implementation-review-rubric.md)
  [Resolves Findings A, B, C, D, E, I]
      │
Slice 3: Git Lifecycle & Verification Strategy
- git-checkpoint-workflow (SKILL.md, checkpoint-lifecycle-and-lineage.md)
- test-and-verification-strategy (SKILL.md, verification-layers-and-tooling.md)
  [Resolves Findings E, F, G]
```

---

## 8. Proportional Verification Plan (Layer 0 Authoritative)

In strict accordance with the Orthogonal Verification Principle (Finding E), this workstream modifies **only** Markdown governance documentation and skill definitions. Zero Java classes, zero Mixin bytecode, and zero datapack JSONs are created or modified.

Therefore, **Layer 0 is the sole applicable automated repository check**:

### Automated Verification Checks (Layer 0)
1. **Skill Route Integrity Check:**
   Verify that all skill routes referenced in `AGENTS.md` Section 5 resolve to existing files on disk.
2. **Internal Reference Link Integrity Check:**
   Verify that every relative markdown link in `references/*.md` across all 6 skills resolves to an existing file.
3. **YAML Frontmatter Schema Validation:**
   Verify that every `SKILL.md` contains valid YAML frontmatter with `name` (matching directory name in kebab-case) and a non-empty `description`.
4. **File Line Count Bounds Check:**
   Verify that every `SKILL.md` and reference file remains within concise bounds (< 500 lines per file).
5. **Git Diff Confinement Audit:**
   `git diff --stat` confirms modifications are strictly confined to `AGENTS.md`, `.agents/skills/`, and `docs/workstreams/agent-architecture-redesign/plan.md`. Zero drift in `src/` or `data/`.

### Crucial Semantic Grounding (Canary Lesson 7)
- Automated Layer 0 structural checks (links, frontmatter, line bounds) verify mechanical syntax only. They do **NOT** prove authority correctness, routing correctness, state-machine closure, or ownership.
- Semantic correctness is established through independent adversarial review by Plan Reviewer `R` and Implementation Reviewer `IR` evaluating the architecture against `bootstrap_governance_manifest`.
- Terminology discipline: Layer 0 checks are designated as the "sole applicable automated repository check", NEVER "fully sufficient", "semantically sufficient", or "complete proof".

### Inapplicable Higher Layers (Explicitly Skipped per Orthogonal Selection)
- **Layer 1 (`validate_repo.py`):** Skipped (0 datapack JSONs modified).
- **Layer 2 (`./gradlew test`):** Skipped (0 Java classes modified).
- **Layer 3 (`test_rct_runtime_contract.py`):** Skipped (0 Mixins or bytecode modified).
- **Layer 4 (`./gradlew runServer`):** Skipped (no runtime bootstrap risk).
- **Layer 5 (Canary Host):** Skipped (no gameplay changes).

---

## 9. Implementation Checkpoints & Review Lifecycle

- **Checkpoint 1: Candidate Plan Handoff & Plan Review Gate (Current State)**
  - Planner `P` completes plan authoring and signals ready with semantic handoff `{ candidate_path, ready: true }`.
  - Main Controller computes `candidate_plan_hash`, passes it in Partition 2 to Plan Reviewer `R` citing `bootstrap_governance_manifest`.
  - Plan Reviewer `R` evaluates plan against `plan-review-rubric.md` from `bootstrap_governance_manifest`.
  - Upon explicit `R verdict: PASS`, Main Controller re-evaluates post-review hash, asserts equality, records `frozen_plan_hash`, and creates the Plan Freeze Checkpoint.
- **Checkpoint 2: Surgical Implementation of Slices 1–3**
  - Implementor `I` executes Slices 1, 2, and 3.
- **Checkpoint 3: Layer 0 Verification & Manifest Assembly**
  - Implementor runs Layer 0 automated checks and prepares Verification Evidence Manifest.
- **Checkpoint 4: Implementation Review Gate & Verified Implementation Checkpoint**
  - Implementation Reviewer `IR` conducts review against `implementation-review-rubric.md` from `bootstrap_governance_manifest`.
  - Upon explicit `IR verdict: PASS`, Main Controller creates the Verified Implementation Checkpoint.

---

## 10. Residual Risks & Safeguards

| Risk Category | Risk Level | Mitigation Safeguard |
| :--- | :--- | :--- |
| **Bootstrap Circular Drift** | Zero | `bootstrap_governance_manifest` snapshot at task start guarantees candidate governance files cannot evaluate or authorize themselves. |
| **Reviewer Privilege Bleed** | Zero | Reviewers remain strictly read-only (`enable_write_tools: false`, zero terminal tools). Hash computation owned 100% by Main Controller. |
| **State Machine Deadlock on Blocker** | Zero | Total 3-verdict algebra with explicit Prerequisite Repair Protocol guarantees deterministic handling for all review outcomes. |
| **Premature Audit Immutability** | Zero | Two-phase commit model distinguishes provisional local checkpoints from promoted audit milestones. |
| **Documentation Bloat / Drift** | Very Low | Line count bounds (< 500 lines per file) and SSOT delegation from `AGENTS.md` eliminate procedural duplication and drift. |

---

## 11. Candidate Plan Handoff & Review Readiness

Planner `P` has completed substantive discovery, incorporated the Subtraction First philosophy, and authored this exhaustive candidate plan resolving all 9 Owner findings (A through I).

- **Canonical Plan Path:** `docs/workstreams/agent-architecture-redesign/plan.md`
- **Handoff State:** Semantic candidate handoff `{ candidate_path: "docs/workstreams/agent-architecture-redesign/plan.md", ready: true }`.
- **Status:** Ready for Main Controller to compute `candidate_plan_hash` and dispatch to Plan Reviewer `R` citing `bootstrap_governance_manifest`.
