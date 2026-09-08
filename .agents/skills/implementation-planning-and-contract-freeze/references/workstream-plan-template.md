# Canonical Workstream Plan Template & Author Self-Checklist

This reference defines the authoritative structure for Mode 3 workstream plans in `docs/workstreams/<id>/plan.md` and provides the author pre-submission self-checklist.

> [!NOTE]
> **Ownership Boundary & Static Candidate Contract (Finding I):** This document defines the canonical document structure and the author's internal self-check before submitting for review. The workstream plan is an immutable historical contract at author handoff identity with static `Author Submission State: Candidate Plan (Ready for Review)`. Eliminate post-freeze mutable fields (`Plan Status: Frozen`, dynamic cycle counters, implementation progress). Dynamic state belongs to runtime memory and git commits. The authoritative evaluation rubric used by independent reviewers is owned exclusively by [`code-review-and-quality/references/plan-review-rubric.md`](../../code-review-and-quality/references/plan-review-rubric.md).

---

## 1. Canonical Plan Structure

Mode 3 workstream plans must follow this standardized section structure:

```markdown
# Workstream Plan: [Feature / Fix Title] (Phase [X] — [Subtitle])

## Status & Ownership

| Field | Value |
| :--- | :--- |
| **Workstream ID** | `<workstream-id>` |
| **Document Role** | Candidate Workstream Plan (Ready for Independent Plan Review) |
| **Baseline Commit** | `<SHA-1>` (`<branch-name>`) |
| **Target Branch** | `<branch-name>` |
| **Canary Lineage** | `<SHA-1>` (if tracking prior verified lineage) |
| **Author Submission State** | `Candidate Plan (Ready for Review)` |

---

## 1. Context & Problem Statement
[Background, user intent, failure modes observed, or architectural motivations. Concise retrospective on prior phases if applicable.]

---

## 2. Concrete Facts vs. Assumptions vs. Conflicts

| Category | Item Description | Evidence / Citation | Impact / Resolution |
| :--- | :--- | :--- | :--- |
| **Confirmed Fact** | Target method or class exists with descriptor | `path/to/Source.java:42` | Concrete anchor for implementation |
| **Confirmed Fact** | Configuration or schema version requirement | `path/to/schema.json:10` | Must satisfy repository validators |
| **Assumption** | Runtime state persists across invocation boundary | Needs test verification | Add test in isolated test suite |
| **Conflict** | Upstream event order differs from requirement | Decompiled source / log | Address in boundary adapter slice |
| **Open Question** | Configuration default value | Pending Owner decision | Default to safe established baseline |

---

## 3. Scope Boundaries & Blast Radius
### 3.1 Strict In-Scope
- Minimal set of files, classes, and configurations to modify.

### 3.2 Explicit Out-of-Scope
- Cosmetic cleanup, unrelated refactoring, unrequested subsystems, or speculative abstractions.

---

## 4. Architectural & Invariant Analysis
- **Repository-Global Invariants:** Surgical scope, simplicity first, read before write, zero opportunistic refactoring, public contract stability, claim strength discipline.
- **Applicable Domain Invariants:**
  - Route to matching domain skills where applicable (e.g., `competitive-pokemon-doubles-team-design`).
  - If no dedicated domain skill exists, route to actual repository documentation/source evidence or state that none exists.
  - Zero hardcoding of domain-specific mechanics in generic architecture sections.

---

## 5. Target Architecture & Slicing Strategy
[Component breakdown, architectural slicing patterns (Contract/Model Slices, Core Logic Slices, Boundary/Adapter Slices, Verification Slices), and dependency ordering.]

---

## 6. Exact Files to Create / Modify / Delete
### Files to Create
1. `<path/to/new/file>`

### Files to Modify
1. `<path/to/existing/file>`: [Summary of changes]

### Files to Delete / Prune
1. `<path/to/obsolete/file>`

---

## 7. Orthogonal Verification Strategy (Finding E)
- **Minimal Covering Verification Set:** Specify the minimal orthogonal set of verification layers directly covering affected contracts.
- **Verification Commands:** Exact, runnable commands for each selected layer.
- **Offline != Production Invariant:** Explicit acknowledgement of offline test limits vs live environment reality.
- **Provenance-First Artifact Freshness (Finding F):** Criteria for establishing freshness via source revision and content hashes without redundant rebuilds.

---

## 8. Implementation Checkpoints (Task-Derived)
Implementation checkpoints are derived dynamically from the workstream's affected contracts and architectural slices (tailored for code, datapack, or governance tasks):
- **Checkpoint 1: Plan Freeze Checkpoint** (Main Controller Identity & Hash Gate).
- **Checkpoint 2: Surgical Implementation of Task Slices** (Derived dynamically from Section 5/6 architectural slicing).
- **Checkpoint 3: Proportional Verification & Manifest Assembly** (Execution of applicable minimal orthogonal layers and evidence assembly in `docs/workstreams/<id>/verification.md`).
- **Checkpoint 4: Implementation Review Gate & Verified Implementation Checkpoint** (Independent review verdict `PASS` and local checkpoint commit).

---

## 9. Residual Risks & Fallback Boundaries
[Known risks, mitigation plans, and explicit fallback instructions if edge cases fail.]
```

---

## 2. Author Pre-Submission Self-Checklist

Before notifying Main Controller that a plan is ready for review, Planner `P` must audit the draft against this self-checklist:

1. **Grounded Signatures & Contracts:**
   - [ ] Every class name, method signature, descriptor, or schema path cited has been verified via repository files, documentation, or decompiler output.
   - [ ] No methods, APIs, or behaviors have been assumed from memory.
2. **Fact vs. Assumption Discipline:**
   - [ ] Every substantive assertion has an accompanying file path, line number, or contract test reference.
   - [ ] Unverified hypotheses are explicitly labeled as `Assumption` or `Open Question`.
3. **Surgical Scope & Non-Goals:**
   - [ ] Out-of-Scope boundaries are clearly defined.
   - [ ] No unrelated files, bulk reformatting, or speculative infrastructure are included.
4. **Invariant Protections Addressed:**
   - [ ] Repository-global invariants evaluated (read before write, surgical scope, simplicity first, claim strength discipline, backward compatibility).
   - [ ] Applicable domain invariants addressed via routed domain skills or repository evidence without contaminating generic planning rules.
5. **Concrete Orthogonal Verification Commands:**
   - [ ] Minimal orthogonal verification set selected covering affected contracts directly.
   - [ ] Exact, runnable verification commands specified for each selected layer.
   - [ ] Expected pass criteria are clearly stated without hyperbolic sufficiency claims ("sole applicable automated repository check").
6. **Line Count & Proportionality:**
   - [ ] Document is concise, structured, and avoids repetitive narration.
   - [ ] Meets repository line bounds (< 500 lines per file where applicable).
