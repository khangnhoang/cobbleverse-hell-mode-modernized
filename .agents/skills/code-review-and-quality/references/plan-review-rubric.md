# Authoritative Plan Review Rubric for Plan Reviewer (R)

This reference defines the authoritative evaluation rubric, verification dimensions, and verdict criteria used by Plan Reviewer `R` during Phase 2 of Mode 3 in Cobbleverse Hell Mode.

---

## 1. Plan Review Preflight & Identity Check

Before evaluating plan content, Plan Reviewer must verify candidate identity:
1. **Candidate Identity Check:** Verify that the on-disk plan at the specified path corresponds to the `candidate_plan_hash` provided in the prompt.
2. **Untrusted Anchor Rule:** All file paths, line numbers, and architectural assertions provided by the candidate author are **untrusted navigation hints**. You must independently verify material claims against actual repository files using read-only inspection tools (`view_file`, `grep_search`, `find_by_name`).

---

## 2. Core Evaluation Dimensions

Plan Reviewer must evaluate the candidate plan against six core dimensions:

### Dimension 1: Grounded Discovery & Real Repository Evidence
- **Verification:** Are all referenced Java classes, method signatures, bytecode descriptors, and Mixin targets verified against actual repository files or decompiler output?
- **Red Flags:** Hallucinated Fabric/Cobblemon APIs; methods assumed from memory; speculative class names without citations.
- **Rubric Standard:** Every technical assertion must be grounded in observable repository reality.

### Dimension 2: Fact vs. Assumption Discipline
- **Verification:** Does the plan explicitly separate Confirmed Facts from Assumptions and Open Questions?
- **Red Flags:** Unverified hypotheses presented as established facts; unstated runtime dependencies.
- **Rubric Standard:** Hypotheses must be designated as assumptions with planned test verification.

### Dimension 3: Scope Confinement & Anti-Speculation
- **Verification:** Are In-Scope and Out-of-Scope boundaries clearly articulated?
- **Red Flags:** Opportunistic refactoring of adjacent files; speculative abstractions, evaluators, or plugin frameworks justified only by hypothetical future needs; edits outside prompt ownership.
- **Rubric Standard:** Minimal complete solution strictly bounded to the problem.

### Dimension 4: Repository Invariant Protection
- **Fair-AI Information Boundary:** Does the plan prevent Run & Bun battle AI from inspecting unrevealed opponent data (movesets, held items, team slots)?
- **Companion Fabric Mixin Safety:** Are Mixin target classes and injection descriptors valid? Are injections placed at stable anchors (`@Inject` at `HEAD` or verified call points)?
- **Datapack Format & Economy:** Does the plan preserve Cobbleverse 1.7.42 schema rules and prohibit in-battle bag healing items on NPC trainers?

### Dimension 5: Architectural Slicing & Dependency Order
- **Verification:** Does the plan follow layer-aware decomposition? Are pure algorithmic calculations (Layer 2) tested in isolation before wiring into Fabric Mixins (Layer 3)?
- **Red Flags:** Monolithic mega-diffs coupling datapack changes, Java math, and Mixin bytecode injections without staging.

### Dimension 6: Proportional Verification Rigor
- **Verification:** Are concrete, runnable verification commands specified across Hell's 6 layers?
- **Red Flags:** Vague assertions like "will test thoroughly"; missing offline bytecode contract checks (`test_rct_runtime_contract.py`) for Mixin changes; conflating local test passes with production canary reality.
- **Rubric Standard:** Commands must be exact, runnable, and proportional to change risk.

---

## 3. Verdict Determination & Output Schema

### Verdict Rules
- Issue **`PASS`** if and only if there are **zero** `Critical` and **zero** `Required` findings.
- Issue **`BLOCKING_FINDINGS`** if there is at least one `Critical` or `Required` finding.
- Issue **`BLOCKED`** if candidate files are missing, corrupt, or hash-mismatched.

### Review Report Schema
```markdown
# Plan Review Report: <Plan Title>

## Verdict: PASS | BLOCKING_FINDINGS | BLOCKED

### Summary Evaluation
[Concise executive evaluation summarizing strengths, risk profile, and review conclusion.]

### Findings

#### [Finding-01] [Severity]: <Title>
- **Location:** `<file-path>:<line-numbers>`
- **Problem:** <Explanation of defect>
- **Impact:** <Architectural or runtime consequence>
- **Evidence:** <Cited code snippet or contract reference>
- **Required Change:** <Minimal surgical correction>

[Additional findings as needed, or "Zero blocking findings."]
```
