---
name: git-checkpoint-workflow
description: Git working-tree inspection, surgical staging, local checkpoint commit lifecycle, English Conventional Commits, and audit lineage preservation in Cobbleverse Hell Mode.
---

# Git Checkpoint Workflow & Lineage Governance

This skill governs repository working-tree inspection, surgical file staging, local checkpoint commit lifecycles, Conventional Commit standards, and audit lineage preservation in Cobbleverse Hell Mode.

---

## 1. Activation Scope & Responsibilities

Activate this skill when:
- Creating local checkpoint commits during Mode 3 Managed-Agent Workflows;
- Staging files and inspecting git working-tree diffs or status;
- Preserving historical audit commit SHAs or merging workstreams forward.

**Ownership Boundary:**
- **Owns:** Baseline status checks, dirty working tree inspection, surgical file staging (`git add <file>`), local checkpoint commit types, Conventional Commit message formatting, audit lineage immutability, and local-vs-remote action gating.
- **Does NOT Own:** Subagent session orchestration (owned by `managed-agent-workflow`); code review rubrics (owned by `code-review-and-quality`); or test execution strategy (owned by `test-and-verification-strategy`).

---

## 2. Resource Routing

Read bundled references strictly when their conditions match:

| Resource | Read Condition | Skip When |
| :--- | :--- | :--- |
| [`references/checkpoint-lifecycle-and-lineage.md`](references/checkpoint-lifecycle-and-lineage.md) | Read before executing any local checkpoint commit, verifying commit lineage, or preparing merge-forward operations. | Inspecting git status or running read-only diff checks. |

---

## 3. Baseline & Working-Tree Inspection

Before staging any file or creating a commit, the agent must inspect the repository working tree:
1. **Status Inspection:** Run `git status --short` to identify modified, untracked, and deleted files.
2. **Untracked Artifact Hygiene:** Ensure temporary scratch scripts, build outputs (`build/`, `.gradle/`), or editor files are not staged.
3. **Dirty Tree Isolation:** Verify that changes belong strictly to the active task. If unowned modifications exist, stop and assess before touching them.

---

## 4. Surgical Staging Discipline

- **Explicit Staging Only:** Stage only the exact files owned by the approved plan using targeted paths:
  ```powershell
  git add path/to/file1.java path/to/file2.json
  ```
- **Anti-Pattern Proscription:** NEVER run `git add .`, `git add -A`, or `git commit -a`. Broad staging risks capturing unintended edits across the 1,714 trainer files or Loom cache artifacts.
- **Staged Diff Verification:** Always inspect `git diff --cached` before committing to verify that only expected changes are staged.

---

## 5. Local Checkpoint Commit Lifecycle (Mode 3)

When the Owner has authorized implementation under Mode 3, Main Controller is authorized to create necessary local checkpoint commits without re-prompting for every individual commit:

1. **Plan Freeze Checkpoint:**
   - **Trigger:** Plan Reviewer `R` issues an explicit `PASS` (`R verdict: PASS`) and candidate hash matches post-review hash.
   - **Format:** `docs(plan): freeze implementation plan for <feature>`
   - **Significance:** Cryptographically records the reviewed specification.
2. **Verified Implementation Checkpoint:**
   - **Trigger:** Implementation Reviewer `IR` issues an explicit `PASS` (`IR verdict: PASS`) following successful test verification.
   - **Format:** `feat(<scope>): <summary>` or `fix(<scope>): <summary>`
3. **Correction Checkpoint (if needed):**
   - **Trigger:** Applying verified fixes during a reconciliation cycle.
   - **Format:** `fix(<scope>): address review finding <id>`
4. **Production Canary Checkpoint:**
   - **Trigger:** Canary evidence observed and recorded from dedicated live host.
   - **Format:** `docs(workstream): record observed production canary evidence for <feature>`

---

## 6. Two-Phase Commit Lifecycle & Audit Lineage (Finding G)

In Cobbleverse Hell Mode, commit creation is decoupled from audit promotion:

1. **Commit Creation != Audit Promotion:** Local checkpoint commits are internal multi-agent coordination records, distinct from promoted audit milestones. An internal Reviewer `PASS` verdict alone does **not** promote a checkpoint to an immutable audit milestone.
2. **Audit Promotion Boundary:** A checkpoint is promoted to an immutable audit milestone only through an explicit external event (Owner acceptance, accepted PR / merge, accepted live production canary evidence, or explicitly promoted historical audit lineage).
3. **Provisional Local Commits (Permitted Rewrites):** Local, unpromoted commits on a working branch prior to audit promotion may be amended, squashed, or rewritten if explicitly requested or approved by the Owner, reconciling affected references.
4. **Promoted Audit Milestones (Immutable):** Once a commit is promoted to audit status, its SHA must **never** be rebased, squashed, amended, or deleted.
5. **Merge-Forward Strategy:** When integrating workstream branches where preserving multi-agent audit lineage is required, forward merges (`git merge --no-ff`) are recommended to preserve historical commit SHAs intact. However, `--no-ff` is an audit lineage recommendation rather than an inflexible repo-wide dogma.

---

## 7. Strict Remote Actions Gate

Creating local commits does NOT grant permission to mutate remote repositories or base lineage:
- **`commit != push != PR != merge != rebase != rewrite != force-push`**
- Under NO circumstances may an agent execute the following without separate, explicit Owner authorization:
  - `git push` to any remote (origin, upstream);
  - Pull request creation, update, or merging via GitHub CLI (`gh pr`);
  - Fast-forward, rebase, or merge onto base branches;
  - Rebase, squash, or history rewriting on promoted audit commits;
  - Force-pushing (`--force`, `+<branch>`);
  - Branch deletion on local base or remotes.
