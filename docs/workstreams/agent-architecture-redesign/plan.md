# Workstream Plan: Agent Architecture Redesign & Managed Workflow Migration

## Status & Ownership

| Field | Value |
| :--- | :--- |
| **Workstream ID** | `agent-architecture-redesign` |
| **Status** | In-flight / Migration Complete (Awaiting Owner Review) |
| **Source of Truth** | This document (`docs/workstreams/agent-architecture-redesign/plan.md`) |
| **Baseline Commit** | `8b2633483c8b56b903890c2c596b4434be49ddbd` (`main`) |
| **Target Branch** | `feat/agent-architecture-redesign` |

---

## 1. Context & Background

The repository `cobbleverse-hell-mode-modernized` relies on AI coding agents for complex gameplay modernization, datapack refactoring, companion Fabric mod bytecode engineering, and battle AI tuning.
Previously, the repository operated under a monolithic 72-line `AGENTS.md` and a single unpartitioned domain skill (`competitive-pokemon-doubles-team-design`). As task complexity increased, this minimal structure revealed three key scaling bottlenecks:
1. **Lack of Adaptive Preflight:** Routine questions, minor config tweaks, and complex multi-system Mixin refactors were treated under the same flat operational rules.
2. **Context Window Inefficiency:** Domain skills loaded all procedural checklists, case studies, and format matrices directly into the agent's core context regardless of task relevance.
3. **Absence of Bounded Multi-Agent Governance:** Complex tasks had no formal protocol for independent adversarial review, persistent author context during corrections, or finite reconciliation bounds.

---

## 2. Core Problems Addressed

- **Problem 1 (All-or-Nothing Workflow):** Simple queries risked heavy bureaucratic planning, while high-risk cross-module tasks lacked rigorous independent review.
- **Problem 2 (Context Bloat):** Monolithic skill files crowded out task-specific instruction and diffs.
- **Problem 3 (Unbounded Loops):** Without strict reconciliation limits, author-reviewer debates could cycle indefinitely.
- **Problem 4 (Reviewer Write-Privilege Bleed):** Reviewers given write tools to run shell commands risked modifying files during review turns.
- **Problem 5 (Plan Drift):** Lack of cryptographic plan verification allowed subtle deviations between reviewed plans and actual implementations.

---

## 3. Architectural Decisions

### Architectural Decision 1: Universal Lightweight Preflight (AGENTS.md)
Every prompt passes through an ephemeral 5-facet preflight routing into exactly one execution depth:
- **Mode 0 (Conversational / Informational):** Direct answer, minimal targeted reads, zero artifacts, no subagents.
- **Mode 1 (Lightweight Analysis / Recommendation):** Bounded discovery, design advice, no edits without authorization, default single agent.
- **Mode 2 (Direct Bounded Execution):** Clear scope, low blast radius, single path. Direct edit → verify → report. Escalates to Mode 3 if hidden complexity emerges.
- **Mode 3 (Managed-Agent Workflow):** High-risk/complexity tasks (Mixins, RunBun AI, cross-module behavior, runtime contracts). Activates managed multi-agent orchestration.
- **Mode 4 (Stop / Escalate):** Material ambiguity, ungranted destructive actions, or conflicting repository evidence.

### Architectural Decision 2: Bounded Managed-Agent Topology (Mode 3)
```text
Main Controller (orchestrator, thin context, owns state machine & local commits)
├── Planner P (write-capable, persistent across plan corrections)
├── Plan Reviewer R (hard read-only enable_write_tools: false, persistent across plan re-reviews)
├── Implementor I (fresh session, write-capable, persistent across code corrections)
└── Implementation Reviewer IR (fresh session, hard read-only, persistent across code re-reviews)
```
- **Independent Sessions:** Fresh session between author and reviewer creates genuine independence.
- **Persistent Context:** Corrections route back to the existing author session; re-reviews route back to the existing reviewer session.
- **Finite Reconciliation Bounds:** Exactly `MAX_RECONCILIATION_CYCLES = 2` per review phase. Hard stop and escalation to Owner upon exhaustion.
- **Exact Artifact Freeze:** Cryptographic verification (`candidate_plan_hash == post_review_plan_hash` via `git hash-object`) before implementation commences.
- **Hard Read-Only Reviewers:** Reviewers possess zero write tools and no `run_command`. Main Controller prepares evidence manifests (diff, status, test logs) for reviewers.

### Architectural Decision 3: Progressive Disclosure Skill Architecture
Domain skills follow the `SKILL.md` (Core Policy < 500 lines) + `references/*.md` (Conditional Procedures) pattern. Each reference has an explicit `Read Condition` and `Skip When` rule.

### Architectural Decision 4: Local Checkpoint Permission Contract
- In Mode 2: No commits without explicit owner request.
- In Mode 3: Owner authorization of managed implementation covers necessary local checkpoint commits (plan freeze checkpoint, verified implementation checkpoint).
- Remote operations (`push`, `PR`, `merge`, `force-push`, branch deletion) remain **strictly gated** behind explicit Owner authorization.

---

## 4. Scope & Explicit Non-Goals

### In-Scope
- Redesign of root `AGENTS.md` as global SSOT.
- Refactoring `competitive-pokemon-doubles-team-design` into progressive disclosure (`SKILL.md` + 5 references).
- Creation of `managed-agent-workflow` skill (`SKILL.md` + 4 references).
- Registration of `agent-architecture-redesign` in `docs/workstreams/README.md`.
- Creation of lightweight `CLAUDE.md` pointer.

### Explicit Non-Goals
- No `.agents/evals` or synthetic test harness suites.
- No multi-agent background daemons or specialist swarms.
- No automated git push, PR creation, or branch merging.
- No duplication of authoritative repository documentation (`docs/architecture/`, `docs/operations/`).
- No modifications to companion Java source or trainer datapack JSON files.

---

## 5. Migration Execution

1. **Root Contract Modernization:** Rewrote `AGENTS.md` incorporating Core Principles, Universal Lightweight Preflight (Modes 0–4), Vietnamese/English reporting rules, Tool Usage Policy, Skill Routing, Verification Authority, and Local Checkpoint Contract.
2. **Competitive Doubles Skill Refactoring:** Extracted procedural, gimmick, and case-study content from monolithic `SKILL.md` into 5 modular references (`doubles-archetypes-and-synergies.md`, `gimmick-architecture-and-runtime-rules.md`, `failure-modes-and-plan-b.md`, `historical-case-studies.md`, `trainer-audit-checklist.md`).
3. **Managed-Agent Workflow Creation:** Created `.agents/skills/managed-agent-workflow/` with core policy and 4 references (`planning-and-plan-review.md`, `reconciliation-protocol.md`, `implementation-and-review.md`, `controller-state-machine.md`).
4. **Compatibility Pointer:** Added minimal `CLAUDE.md` routing to `AGENTS.md`.

---

## 6. Verification Matrix

| Verification Dimension | Inspection / Command | Result |
| :--- | :--- | :--- |
| **Datapack & Repository Data** | `python scripts/ci/validate_repo.py` | `PASS` (1,714 trainers, 10 audit reports verified) |
| **Bytecode Runtime Contracts** | `python scripts/runtime-contract/test_rct_runtime_contract.py` | `PASS` (41/41 bytecode checks verified) |
| **Route Integrity** | Direct inspection of `AGENTS.md` skill paths | Valid (`.agents/skills/managed-agent-workflow/SKILL.md`, `.agents/skills/competitive-pokemon-doubles-team-design/SKILL.md`) |
| **Reference Link Existence** | Inspection of all relative Markdown links in `references/` | Valid (9/9 references exist, contained within bundles) |
| **Skill Frontmatters** | Schema check (`name`, `description`) on both `SKILL.md` files | Valid YAML frontmatter, kebab-case names |
| **SSOT Boundary & Non-Duplication** | Diff audit vs `docs/architecture/` and `docs/operations/` | Zero duplicated architecture facts; links route to existing docs |

---

## 7. Residual Risks & Status

- **Residual Risk:** Low. Changes are strictly confined to agent documentation, skill definitions, and workstream tracking. No production code, Mixins, or datapacks are affected.
- **Next Milestone:** Await Owner review of isolated branch `feat/agent-architecture-redesign` and proceed with commit upon explicit instruction.
