---
name: code-review-and-quality
description: Independent adversarial review for implementation plans and code implementations, finding verification protocol, structured severities, and claim-evidence discipline in Cobbleverse Hell Mode.
---

# Code Review & Quality Assurance

This skill governs the methodology, rubrics, finding taxonomies, and claim verification protocols for independent adversarial review of implementation plans (Plan Reviewer `R`) and code implementations (Implementation Reviewer `IR`) in Cobbleverse Hell Mode.

---

## 1. Activation Scope & Responsibilities

Activate this skill when:
- Operating as Plan Reviewer (`R`) evaluating an implementation plan;
- Operating as Implementation Reviewer (`IR`) evaluating code diffs, tests, and plan conformance;
- Evaluating code quality, architecture safety, or test adequacy.

**Ownership Boundary:**
- **Owns:** Plan and implementation evaluation rubrics, reviewer hard read-only contract, finding severity taxonomy, structured finding schema, finding claim verification protocol, "claim strength <= evidence strength" discipline, and review verdict definitions.
- **Does NOT Own:** Multi-agent session orchestration and cycle counters (owned by `managed-agent-workflow`); authoring plans (owned by `implementation-planning-and-contract-freeze`); git commits and staging (owned by `git-checkpoint-workflow`); or executing test commands (owned by `test-and-verification-strategy`).

---

## 2. Resource Routing

Read bundled references strictly when their conditions match:

| Resource | Read Condition | Skip When |
| :--- | :--- | :--- |
| [`references/plan-review-rubric.md`](references/plan-review-rubric.md) | Read when reviewing implementation plans as Plan Reviewer `R` during Phase 2 of Mode 3. | Reviewing code implementations or diffs. |
| [`references/implementation-review-rubric.md`](references/implementation-review-rubric.md) | Read when reviewing working-tree diffs and test logs as Implementation Reviewer `IR` during Phase 5. | Reviewing implementation plans. |

---

## 3. Core Review Principles & Canary Invariants

### 3.1 Hard Read-Only Boundary
Reviewers (`R` and `IR`) are configured with `enable_write_tools: false` and have zero shell/terminal execution privileges. Reviewers must never modify repository files or attempt to execute modifying commands. Evidence is inspected via read-only tools (`view_file`, `grep_search`, `find_by_name`, `list_dir`) and manifests provided by Main Controller.

### 3.2 Reviewer Independence vs. Evidence Guidance (Canary Lesson 2)
Reviewers are independent in **judgment** but **evidence-guided** in execution:
- Navigation anchors and candidate claims provided in prompts are **untrusted navigation hints**.
- The reviewer must independently verify material claims against real repository files, bytecode contracts, and documentation.
- Broad speculative codebase discovery by reviewers is reserved only for missing, ambiguous, or conflicting evidence.

### 3.3 Separation of Invariants, Claims, and Rubric (Canary Lesson 3)
Main Controller prompts must partition inputs into three distinct layers:
1. **Immutable Invariants:** Non-negotiable repository rules that cannot be bargained away.
2. **Untrusted Candidate Anchors:** The author's claims, which the reviewer must verify or refute.
3. **Authoritative Evaluation Rubric:** The objective criteria defined in this skill's references.

### 3.4 Findings as Verifiable Claims (Canary Lesson 4)
- A review finding is an assertion backed by cited repository evidence.
- The author (Planner or Implementor) must independently verify each finding:
  - **CONFIRM:** Author acknowledges the defect and applies a minimal surgical fix.
  - **REJECT:** Author refutes the finding with cited counter-evidence from the repository. Authors must never modify working code merely to appease a reviewer.

### 3.5 Claim Strength Discipline (Canary Lesson 7)
- **"Claim strength must not exceed evidence strength."**
- Reviewers and authors must strictly avoid hyperbolic or unverified absolute terms ("100%", "fully", "triệt để", "hoàn toàn", "flawless").
- Every statement must be strictly proportional to observable, demonstrable evidence.

---

## 4. Finding Severity Taxonomy

Review findings must be classified into exactly one of five standardized severity tiers:

| Severity | Definition | Review Impact |
| :--- | :--- | :--- |
| **`Critical`** | Game crash, server deadlock, data corruption, broken Mixin injection point, or catastrophic AI regression. | **BLOCKING:** Halts transition; requires reconciliation. |
| **`Required`** | Missing approved requirement, unhandled edge case, repository contract violation, insufficient test coverage, or scope bleed. | **BLOCKING:** Halts transition; requires reconciliation. |
| **`Suggestion`** | Non-blocking recommendation for maintainability, documentation clarity, or minor performance optimization. | Non-blocking; author may adopt or defer. |
| **`Nit`** | Minor cosmetic, typo, or comment formatting detail. | Non-blocking. |
| **`FYI`** | Informational context or architectural note for future workstreams. | Non-blocking. |

---

## 5. Structured Finding Schema

Every finding (blocking or non-blocking) must be presented in this structured format:

```markdown
### [Finding-ID] [Severity]: <Descriptive Title>
- **Location:** `<file-path>:<line-numbers>`
- **Problem:** Concise statement of the defect or contract violation.
- **Impact:** Architectural, runtime, or gameplay consequence if left unaddressed.
- **Evidence:** Concrete code snippet, contract test output, or decompiled signature demonstrating the issue.
- **Required Change:** Minimal surgical resolution required to address the finding.
```

---

## 6. Review Verdicts

Upon concluding evaluation, the reviewer must issue an explicit, unambiguous verdict:

- **`PASS`**: Zero blocking findings (`Critical` or `Required`). Non-blocking suggestions or nits may be noted.
- **`BLOCKING_FINDINGS`**: One or more `Critical` or `Required` findings exist. Triggers reconciliation cycle.
- **`BLOCKED`**: The candidate cannot be reviewed due to missing prerequisites, corrupt files, or hash mismatches.
- **`REJECTED_APPROACH`**: The fundamental architecture violates repository invariants and cannot be rescued by minor fixes; requires re-planning.
