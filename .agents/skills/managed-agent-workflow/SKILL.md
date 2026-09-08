---
name: managed-agent-workflow
description: Bounded multi-agent orchestration workflow for Mode 3 complex, multi-file, or high-risk implementation tasks in Cobbleverse Hell Mode. Orchestrates Main Controller, persistent Planner, hard read-only Plan Reviewer, fresh Implementor, and hard read-only Implementation Reviewer with a strict 2-cycle reconciliation limit.
---

# Managed-Agent Workflow

This skill defines the operational protocol, session topology, and governance boundaries for executing complex, multi-system, or high-risk tasks through a managed multi-agent architecture in Cobbleverse Hell Mode.

---

## 1. Activation Scope & Ownership

Activate this skill **only** when the Universal Lightweight Preflight classifies a request as **Mode 3 (Managed-Agent Workflow)** due to material risk or complexity signals:
- Non-trivial behavior bugs requiring deep multi-file discovery;
- Multiple plausible implementation architectures requiring independent evaluation;
- Companion mod Mixin, bytecode injection, or Fabric runtime contract changes;
- Run & Bun AI decision algorithms, scoring matrices, or fair-play information boundaries;
- High regression risk across trainer battle formats or datapack dependencies;
- Implementation requiring an authoritative implementation plan before coding.

**Exclusions:** Do NOT activate this skill for Mode 0 (informational questions), Mode 1 (architectural analysis without coding), or Mode 2 (direct bounded implementation fixes).

This skill owns the multi-agent session lifecycle, reconciliation protocol, candidate hashing, and evidence preparation contracts. Global behavior, preflight routing, and repository permissions remain owned by `AGENTS.md`.

---

## 2. Resource Routing

Read bundled references strictly when their conditions match:

| Resource | Read Condition | Skip When |
| :--- | :--- | :--- |
| [`references/planning-and-plan-review.md`](references/planning-and-plan-review.md) | Read before spawning Planner `P`, setting up candidate plan hashing, spawning Plan Reviewer `R`, or formatting plan review rubrics. | Planning phase is already complete and plan is frozen. |
| [`references/reconciliation-protocol.md`](references/reconciliation-protocol.md) | Read before dispatching blocking review findings to an author, verifying findings, handling author rebuttals, or formatting Owner escalation reports. | Reviewer issues an unconditioned `PASS` verdict. |
| [`references/implementation-and-review.md`](references/implementation-and-review.md) | Read before spawning Implementor `I`, verifying frozen plan identity, preparing evidence packages for read-only IR, or conducting code review. | Task is currently in Phase 1–3 (planning/review). |
| [`references/controller-state-machine.md`](references/controller-state-machine.md) | Read when managing ephemeral controller state transitions, tracking reconciliation counters, terminating subagent sessions, or executing local checkpoint commits. | Routine stateless inspection or direct Mode 2 execution. |

---

## 3. Session Topology & Core Roles

```text
Main Controller
├── Planner P (write-capable, persistent session across plan corrections)
├── Plan Reviewer R (hard read-only, persistent session across plan re-reviews)
├── Implementor I (write-capable, fresh session relative to P/R, persistent across code corrections)
└── Implementation Reviewer IR (hard read-only, fresh session relative to I, persistent across code re-reviews)
```

1. **Main Controller:** Orchestrator. Maintains ephemeral state, prepares evidence for read-only reviewers, enforces state machine transitions, and executes local checkpoint commits. Keeps its own context window thin.
2. **Planner (`P`):** Fresh session spawned via `invoke_subagent` (`Workspace: inherit`, `enable_write_tools: true`). Discovers codebase, designs solution, writes plan. Remains `idle` across plan reconciliation.
3. **Plan Reviewer (`R`):** Fresh session spawned via `define_subagent` (`enable_write_tools: false`). Reviews candidate plan against repository evidence. Strictly read-only. Remains `idle` across plan re-reviews.
4. **Implementor (`I`):** Fresh session spawned after plan freeze (`Workspace: inherit`, `enable_write_tools: true`). Reads exact frozen plan, verifies blob hash, executes changes, runs local verifications. Remains `idle` across code reconciliation.
5. **Implementation Reviewer (`IR`):** Fresh session spawned via `define_subagent` (`enable_write_tools: false`). Evaluates working-tree diff, status, and test logs against frozen plan. Strictly read-only. Remains `idle` across code re-reviews.

---

## 4. Core Invariants

1. **Separation of Author & Reviewer:** A reviewer is always a fresh session independent of the author. An author never reviews its own work.
2. **Persistent Author Context:** Corrections always route back to the original author session via `send_message`. Never spawn a fresh author for a correction turn; preserving discovery and implementation context is mandatory.
3. **Persistent Reviewer Context:** Re-reviews always route back to the existing reviewer session via `send_message`.
4. **Hard Reviewer Read-Only Boundary:** Reviewers (`R` and `IR`) must be configured with `enable_write_tools: false`. Reviewers cannot modify repository files and have no `run_command` access. Main Controller is responsible for executing shell commands and providing evidence to the reviewer.
5. **Exact Artifact Freeze via Cryptographic Identity:**
   - Before review: Main generates `candidate_plan_hash = git hash-object <plan_path>`.
   - After review `PASS`: Main recomputes `post_review_plan_hash`.
   - Mandatory check: `candidate_plan_hash == post_review_plan_hash`.
   - Implementor must verify that the on-disk plan matches `frozen_plan_hash` before writing any code.
6. **Finite Loop Guarantee (Max 2 Cycles):**
   - `MAX_PLAN_RECONCILIATION_CYCLES = 2`
   - `MAX_IMPLEMENTATION_RECONCILIATION_CYCLES = 2`
   - A cycle is counted whether the author accepts the finding or rebuts it with evidence.
   - If blocking findings remain after cycle 2, the loop halts immediately and escalates to the Owner with an objective dossier. No infinite retries.
7. **Local Checkpoint Commit Contract:** When the Owner has authorized Mode 3 implementation, Main Controller is permitted to create necessary local checkpoint commits (plan freeze checkpoint, verified implementation checkpoint) without re-prompting for every commit. Remote actions (push, PR, merge, force-push) strictly require separate Owner approval.
8. **Zero Speculative Infrastructure:** No durable database stores, no eval suites, no specialist swarms, and no multi-agent daemon scripts in repository files.
