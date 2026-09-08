# Canonical Workstream Plan Template & Author Self-Checklist

This reference defines the authoritative structure for Mode 3 workstream plans in `docs/workstreams/<id>/plan.md` and provides the author pre-submission self-checklist.

> [!NOTE]
> **Ownership Boundary:** This document defines the canonical document structure and the author's internal self-check before submitting for review. The authoritative evaluation rubric used by independent reviewers is owned exclusively by [`code-review-and-quality/references/plan-review-rubric.md`](../../code-review-and-quality/references/plan-review-rubric.md).

---

## 1. Canonical Plan Structure

Mode 3 workstream plans must follow this standardized section structure:

```markdown
# Workstream Plan: [Feature / Fix Title] (Phase [X] — [Subtitle])

## Status & Ownership

| Field | Value |
| :--- | :--- |
| **Workstream ID** | `<workstream-id>` |
| **Status** | `Draft` \| `Ready for Review` \| `Reconciled` \| `Frozen` |
| **Source of Truth** | This document (`docs/workstreams/<id>/plan.md`) |
| **Baseline Commit** | `<SHA-1>` (`<branch-name>`) |
| **Canary Lineage** | `<SHA-1>` (if tracking prior verified lineage) |
| **Target Branch** | `<branch-name>` |
| **Reconciliation Cycle** | `Turn 0 (Initial)` \| `Cycle 1` \| `Cycle 2` |

---

## 1. Context & Problem Statement
[Background, user intent, failure modes observed, or architectural motivations. Concise retrospective on prior phases if applicable.]

---

## 2. Concrete Facts vs. Assumptions vs. Conflicts

| Category | Item Description | Evidence / Citation | Impact / Resolution |
| :--- | :--- | :--- | :--- |
| **Confirmed Fact** | Target method exists with descriptor | `net/fabricmc/.../Foo.class:42` | Anchor for Mixin injection |
| **Confirmed Fact** | Datapack requires schema v1.7.42 | `data/cobblemon/trainers/` | Must pass validate_repo.py |
| **Assumption** | Shadow variable retains value across ticks | Needs test verification | Add test in Layer 2 or Layer 3 |
| **Conflict** | Cobblemon event fires after AI scoring | Decompiled `Battle.java:120` | Must inject at action selection |
| **Open Question** | Target IV spread for Gym 4 leader | Pending Owner decision | Default to standard 31 IVs |

---

## 3. Scope Boundaries & Blast Radius
### 3.1 Strict In-Scope
- Minimal set of files, classes, and configurations to modify.

### 3.2 Explicit Out-of-Scope
- Cosmetic cleanup, unrelated refactoring, other battle formats, or unrequested trainers.

---

## 4. Architectural & Invariant Analysis
- **Fair-AI Information Boundary:** Verification that no secret opponent state is leaked.
- **Companion Fabric Mixin Safety:** Bytecode stability, target descriptors, `@Slice` or `@At` anchors.
- **Datapack Format & Economy:** Cobbleverse 1.7.42 schema compliance, zero bag healing items.

---

## 5. Target Architecture & Slicing Strategy
[Component breakdown, dependency order across Hell's 6 layers, and design patterns (e.g., surrogate, adapter).]

---

## 6. Exact Files to Create / Modify / Delete
### Files to Create
1. `<path/to/new/file>`

### Files to Modify
1. `<path/to/existing/file>`: [Summary of changes]

### Files to Delete / Prune
1. `<path/to/obsolete/file>`

---

## 7. Verification Strategy across Hell's 6 Layers
- **Layer 0 (Markdown & Governance):** Route checks, link integrity, frontmatter schema.
- **Layer 1 (Datapack & Trainers):** `python scripts/ci/validate_repo.py`, `check_legacy_baseline.py`.
- **Layer 2 (Java Logic & Boundary Math):** `./gradlew test` (JUnit 5 unit tests).
- **Layer 3 (Bytecode & Shadow Contracts):** `python scripts/runtime-contract/test_rct_runtime_contract.py`.
- **Layer 4 (Headless Smoke):** `./gradlew runServer` (Mixin bootstrap and initialization).
- **Layer 5 (Production Host Canary):** Live dedicated server verification (telemetry, progression).
- **Offline != Production Invariant:** Explicit acknowledgement of verification limits.
- **Artifact Freshness Rules:** Criteria for timestamp/hash validity vs. mandatory re-run.

---

## 8. Implementation Checkpoints
- Checkpoint 1: Planning Freeze & Hash Verification.
- Checkpoint 2: Core Algorithm / Model Implementation & Unit Tests.
- Checkpoint 3: Integration / Mixin Binding & Bytecode Contracts.
- Checkpoint 4: Verification Manifest Assembly & Review Report.

---

## 9. Residual Risks & Fallback Boundaries
[Known risks, mitigation plans, and explicit fallback instructions if edge cases fail.]
```

---

## 2. Author Pre-Submission Self-Checklist

Before notifying Main Controller that a plan is ready for review, Planner `P` must audit the draft against this self-checklist:

1. **Grounded Signatures:**
   - [ ] Every Java class name, method signature, and bytecode descriptor cited has been verified via repository files, `javap`, or decompiler output.
   - [ ] No methods or APIs have been assumed from memory.
2. **Fact vs. Assumption Discipline:**
   - [ ] Every substantive assertion has an accompanying file path, line number, or contract test reference.
   - [ ] Unverified hypotheses are explicitly labeled as `Assumption` or `Open Question`.
3. **Surgical Scope & Non-Goals:**
   - [ ] Out-of-scope boundaries are clearly defined.
   - [ ] No unrelated files or opportunistic cleanup are included.
4. **Invariant Protections Addressed:**
   - [ ] Fair-AI boundary evaluated (zero opponent secret leaks).
   - [ ] Fabric Mixin stability and bytecode contracts evaluated.
   - [ ] Datapack 1.7.42 schema and economy rules verified.
5. **Concrete Verification Commands:**
   - [ ] Exact, runnable commands are specified for each applicable verification layer.
   - [ ] Expected pass criteria are clearly stated.
6. **Line Count & Proportionality:**
   - [ ] Document is concise, structured, and avoids repetitive narration.
   - [ ] Meets repository line bounds (< 500 lines per file where applicable).
