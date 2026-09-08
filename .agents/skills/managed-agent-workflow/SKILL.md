---
name: managed-agent-workflow
description: Bounded multi-agent orchestration workflow for Mode 3 complex, multi-file, or high-risk implementation tasks in Cobbleverse Hell Mode. Orchestrates Main Controller, persistent Planner, hard read-only Plan Reviewer, fresh Implementor, and hard read-only Implementation Reviewer with a strict 2-cycle reconciliation limit.
---

# Managed-Agent Workflow

This skill defines the operational orchestration protocol, session topology, and governance boundaries for executing complex, multi-system, or high-risk tasks through a managed multi-agent architecture in Cobbleverse Hell Mode.

---

## 1. Activation Scope & Responsibilities

Activate this skill **only** when the Universal Lightweight Preflight classifies a request as **Mode 3 (Managed-Agent Workflow)** due to material risk or complexity signals:
- Agent-architecture, workflow skill, or repository governance contract changes (`AGENTS.md`, `.agents/skills/`);
- Cross-repository workflow porting or workflow migrations;
- Explicit Owner instruction for independent review, plan freeze, or managed-agent workflow;
- Non-trivial behavior bugs requiring deep multi-file discovery;
- Multiple plausible implementation architectures requiring independent evaluation;
- Core runtime contract, bytecode injection, or platform boundary changes;
- Non-trivial algorithmic decision logic, scoring matrices, or state memory changes;
- High regression risk across core game systems, data schemas, or runtime dependencies;
- Implementation requiring an authoritative implementation plan before coding.

**Orchestration Ownership Boundary:**
- **Owns:** Ephemeral controller state machine (`IDLE` -> `MODE_3_SELECTED` -> `PLANNING` -> ...), `bootstrap_commit` snapshot, `bootstrap_governance_manifest` establishment, Reviewer Invocation Prompt Contract (3-part partition), 100% ownership of cryptographic candidate hashing handshake and assertion, closed 3-verdict model (`PASS`, `BLOCKING_FINDINGS`, `BLOCKED`), explicit audit verdicts before dependent transitions, finite reconciliation counting (`MAX_CYCLES = 2`), subagent termination, and Owner escalation dossiers.
- **Delegates to Specialized Skills:**
  - Substantive technical discovery and workstream plan authoring: [`implementation-planning-and-contract-freeze`](../implementation-planning-and-contract-freeze/SKILL.md)
  - Independent adversarial review rubrics and finding verification: [`code-review-and-quality`](../code-review-and-quality/SKILL.md)
  - Working-tree inspection, surgical staging, and local checkpoint commits: [`git-checkpoint-workflow`](../git-checkpoint-workflow/SKILL.md)
  - Proportional test execution, authority hierarchy, and evidence manifests: [`test-and-verification-strategy`](../test-and-verification-strategy/SKILL.md)

---

## 2. Resource Routing

Read bundled references strictly when their conditions match:

| Resource | Read Condition | Skip When |
| :--- | :--- | :--- |
| [`references/controller-state-machine.md`](references/controller-state-machine.md) | Read when managing ephemeral controller state transitions, tracking phase events, structuring reviewer invocation prompts, or executing subagent termination. | Routine stateless inspection or direct Mode 2 execution. |
| [`references/reconciliation-protocol.md`](references/reconciliation-protocol.md) | Read when handling blocking review findings, tracking reconciliation cycle counters, mediating author rebuttals, or compiling Owner escalation dossiers. | Reviewer issues an unconditioned `PASS` verdict. |

---

## 3. Session Topology & Core Roles

```text
Main Controller
├── Planner P (write-capable, persistent session across plan corrections)
├── Plan Reviewer R (hard read-only, persistent session across plan re-reviews)
├── Implementor I (write-capable, fresh session relative to P/R, persistent across code corrections)
└── Implementation Reviewer IR (hard read-only, fresh session relative to I, persistent across code re-reviews)
```

1. **Main Controller:** Pure state machine orchestrator. Maintains ephemeral state, snapshots `bootstrap_commit`, establishes `bootstrap_governance_manifest`, prepares evidence manifests for read-only reviewers, enforces state machine transitions, owns 100% of runtime identity, computes and asserts cryptographic hashes (`git hash-object`), records explicit audit verdicts (`PASS`), and executes local checkpoint commits. Keeps its own context window thin. Does not perform substantive discovery.
2. **Planner (`P`):** Session spawned via `invoke_subagent` (`enable_write_tools: true`). Governed by `implementation-planning-and-contract-freeze`. Performs substantive discovery, designs solution, writes plan. Outputs purely semantic candidate content `{ candidate_path, ready: true }` without computing hashes. Remains `idle` across plan reconciliation.
3. **Plan Reviewer (`R`):** Session spawned via `define_subagent` (`enable_write_tools: false`). Governed by `code-review-and-quality`. Evaluates candidate plan against repository evidence and `bootstrap_governance_manifest`. Strictly read-only; does NOT compute hashes. Remains `idle` across plan re-reviews.
4. **Implementor (`I`):** Fresh session spawned after plan freeze (`enable_write_tools: true`). Verifies blob hash, executes changes, runs local verifications governed by `test-and-verification-strategy`. Remains `idle` across code reconciliation.
5. **Implementation Reviewer (`IR`):** Fresh session spawned via `define_subagent` (`enable_write_tools: false`). Governed by `code-review-and-quality`. Evaluates working-tree diff, status, and test logs against frozen plan and `bootstrap_governance_manifest`. Strictly read-only; does NOT compute hashes. Remains `idle` across code re-reviews.

---

## 4. Core Orchestration Invariants

1. **Immediate Hand-Off & Zero Main Pre-Discovery (Canary Lesson 1):**
   Main Controller terminates direct probing immediately upon classifying Mode 3 and enters `MODE_3_SELECTED`.
   - *Whitelist (Permitted):* Snapshot `bootstrap_commit` (`git rev-parse HEAD`), establish `bootstrap_governance_manifest`, check git status and branch (`git status --short`, `git branch --show-current`), transition to `PLANNING`, and invoke Planner `P`.
   - *Blacklist (Forbidden):* Main Controller MUST NOT search external repositories, clone/locate external sources, inspect implementation directories, crawl files, or assemble evidence manifests. Substantive discovery belongs strictly to Planner `P`.
2. **Separation of Author & Reviewer:**
   A reviewer is always a fresh session independent of the author. An author never reviews its own work.
3. **Persistent Author Context:**
   Corrections always route back to the original author session (`P` or `I`) via `send_message`. Never spawn a fresh author for a correction turn.
4. **Persistent Reviewer Context:**
   Re-reviews always route back to the existing reviewer session (`R` or `IR`) via `send_message`.
5. **Hard Reviewer Read-Only Boundary:**
   Reviewers (`R` and `IR`) must be configured with `enable_write_tools: false` and have zero terminal command access. Main Controller compiles and passes evidence packages.
6. **Reviewer Invocation Prompt Contract (Canary Lessons 2 & 3):**
   Main Controller structures reviewer prompts into 3 distinct partitions: (1) Immutable Authority & Invariants resolved from `bootstrap_governance_manifest`, (2) Untrusted Candidate Anchors & Claims, and (3) Authoritative Evaluation Rubric resolved from `bootstrap_governance_manifest`. Candidate claims are explicit untrusted navigation hints.
7. **Exact Cryptographic Artifact Freeze (Canary Lesson 5):**
   - 100% Main Controller Hash Ownership: Planner outputs purely semantic handoff `{ candidate_path, ready: true }`. Hard read-only reviewers have zero command tools and cannot compute hashes. Main Controller computes `candidate_plan_hash = git hash-object <plan_path>` before review, passes it in Partition 2, recomputes `post_review_plan_hash` after `PASS`, asserts `candidate_plan_hash == post_review_plan_hash`, and records `frozen_plan_hash`.
   - Implementor verifies on-disk plan hash before modifying any files.
8. **Closed 3-Verdict Model & Explicit Audit Gates:**
   - Standardized closed verdicts: `PASS`, `BLOCKING_FINDINGS`, `BLOCKED`.
   - Mandatory explicit audit verdicts: Main Controller requires an explicit `R verdict: PASS` recorded before committing Plan Freeze Checkpoint or spawning Implementor, and an explicit `IR verdict: PASS` recorded before committing Verified Implementation Checkpoint or completing task.
9. **Finite Loop Guarantee (Max 2 Cycles):**
   - `MAX_PLAN_RECONCILIATION_CYCLES = 2`
   - `MAX_IMPLEMENTATION_RECONCILIATION_CYCLES = 2`
   - Initial review is Turn 0 (not counted as a cycle). Cycle increments on correction-and-re-review turns.
   - If blocking findings remain after cycle 2, the loop halts immediately and escalates to Owner.
10. **Clean Subagent Termination:**
    Main Controller terminates subagent pairs at phase boundaries (Phase 3 terminates `P` & `R`; Phase 5 terminates `I` & `IR`).
11. **Self-Modifying Governance Bootstrap Protocol (Finding A):**
    When a task modifies `AGENTS.md` or `.agents/skills/`, working-tree governance files are untrusted candidate claims. All governance authority, rubrics, and skills used during the run resolve exclusively from `bootstrap_governance_manifest` established at `bootstrap_commit`.
