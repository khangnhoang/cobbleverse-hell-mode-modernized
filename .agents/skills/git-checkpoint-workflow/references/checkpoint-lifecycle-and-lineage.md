# Checkpoint Lifecycle, Conventional Commits & Audit Lineage

This reference details the mechanics of local checkpoint commits, English Conventional Commit formatting, and audit lineage preservation in Cobbleverse Hell Mode.

---

## 1. Local Checkpoint Commit Types

Cobbleverse Hell Mode defines four canonical checkpoint commit types for Mode 3 workstreams:

### 1.1 Plan Freeze Checkpoint
- **When:** Immediately after Plan Reviewer `R` issues an explicit `PASS` verdict (`R verdict: PASS`), and Main Controller confirms that `candidate_plan_hash == post_review_plan_hash`.
- **Purpose:** Freezes the reviewed workstream plan into git history before any code is written.
- **Commit Message Format:**
  ```text
  docs(plan): freeze implementation plan for <workstream-or-feature>

  Plan Hash: <SHA-1 from git hash-object>
  Review Verdict: PASS (by Plan Reviewer R)
  Baseline Commit: <SHA-1>
  ```

### 1.2 Verified Implementation Checkpoint
- **When:** After Implementation Reviewer `IR` issues an explicit `PASS` verdict (`IR verdict: PASS`) and all applicable verification suites pass.
- **Purpose:** Records the verified code and configuration changes.
- **Commit Message Format:**
  ```text
  <type>(<scope>): <summary in lowercase imperative>

  - Implemented <component / feature details>
  - Verification: <applicable suites and exit codes per verification strategy>
  - Review Verdict: PASS (by Implementation Reviewer IR)
  - Plan Reference: docs/workstreams/<id>/plan.md@<frozen-hash>
  ```

### 1.3 Correction Checkpoint
- **When:** Applied during Reconciliation Cycle 1 or 2 to address blocking review findings.
- **Purpose:** Records surgical fixes without commingling them with baseline work.
- **Commit Message Format:**
  ```text
  fix(<scope>): address review finding <finding-id>

  - <Surgical fix description>
  - Re-verification: <command and exit code>
  ```

### 1.4 Production Canary Checkpoint
- **When:** After live dedicated server canary testing records observed runtime performance and telemetry.
- **Purpose:** Records empirical production evidence and runtime performance observations without unverified absolute claims.
- **Commit Message Format:**
  ```text
  docs(workstream): record observed production canary evidence for <feature>

  - Server: dedicated canary host
  - Canary scope: <scenarios and battle formats tested>
  - Observed telemetry: <concrete observed metrics, player interactions, ticks without crash>
  - Observed limitations: <untested edge cases or remaining risks>
  ```

---

## 2. English Conventional Commit Standards

All commit messages must adhere to the Conventional Commits specification:
- **Types:**
  - `feat`: New gameplay mechanic, AI algorithm, or datapack capability.
  - `fix`: Bug fix, calculation correction, or contract alignment.
  - `docs`: Workstream plans, governance docs, or architecture documentation.
  - `test`: Unit tests, contract test suites, or CI scripts.
  - `refactor`: Structural changes without behavioral modifications.
  - `chore`: Maintenance, dependency bumps, or tooling adjustments.
- **Scope:** Kebab-case subsystem identifier (e.g., `ai`, `companion`, `trainer`, `datapack`, `plan`, `workstream`).
- **Subject:** Imperative mood, lowercase, no ending period, maximum 72 characters (e.g., `resolve dynamic base power for weight-dependent moves`).
- **Body:** Separated from subject by a blank line; details context, verification results, and audit references.

---

## 3. Audit Lineage Preservation Protocol (Canary Lesson 10)

The integrity of multi-agent development depends on traceable, immutable commit history:

1. **Promoted Audit Milestones (Immutable):** Once a commit is promoted or referenced as an audit milestone (such as canary lineage `2a329a5`, `8ee3d27`, `17c9e79`, or Plan Freeze and Verified Implementation Checkpoints), its SHA must never be altered. Do NOT run `git rebase -i` or squash promoted audit commits.
2. **Provisional Local Commits (Permitted Rewrites):** Local, unpromoted commits on a working branch prior to audit promotion may be amended, squashed, or rewritten if explicitly requested or approved by the Owner.
3. **Merge-Forward Strategy:** When integrating workstream branches where preserving multi-agent audit lineage is required, forward merges (`git merge --no-ff`) are recommended to preserve historical commit SHAs intact. However, `--no-ff` is an audit lineage recommendation rather than an inflexible repo-wide dogma.
4. **Working-Tree Reset Protection:** Never run `git reset --hard` across checkpoint commits without explicit Owner directive.
