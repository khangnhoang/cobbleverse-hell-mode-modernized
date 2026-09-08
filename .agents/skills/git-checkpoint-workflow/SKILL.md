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
   - **Trigger:** Plan Reviewer `R` issues `PASS` and candidate hash matches post-review hash.
   - **Format:** `docs(plan): freeze implementation plan for <feature>`
   - **Significance:** Cryptographically records the reviewed specification. The commit SHA becomes permanent audit evidence.
2. **Verified Implementation Checkpoint:**
   - **Trigger:** Implementation Reviewer `IR` issues `PASS` following successful test verification.
   - **Format:** `feat(<scope>): <summary>` or `fix(<scope>): <summary>`
3. **Correction Checkpoint (if needed):**
   - **Trigger:** Applying verified fixes during a reconciliation cycle.
   - **Format:** `fix(<scope>): address review finding <id>`
4. **Production Canary Checkpoint:**
   - **Trigger:** Canary evidence observed and recorded from dedicated live host.
   - **Format:** `docs(workstream): record observed production canary evidence for <feature>`

---

## 6. Audit Lineage & SHA Preservation (Canary Lesson 10)

In Cobbleverse Hell Mode, checkpoint commits serve as durable cryptographic audit trails:
1. **Never Rewrite Audit History:** Commits establishing plan freezes, verified implementations, or production canary records (e.g., canary lineage `2a329a5`, `8ee3d27`, `17c9e79`) must **NEVER** be rebased, squashed, amended, or deleted.
2. **Merge-Forward Strategy:** When integrating workstream branches into long-lived branches, use forward merges (`git merge --no-ff`) to preserve historical commit SHAs intact.
3. **Auditability:** Anyone inspecting git history must be able to trace the exact sequence of plan freeze -> implementation -> verification.

---

## 7. Strict Remote Actions Gate

Creating local commits does NOT grant permission to mutate remote repositories:
- **`commit != push != PR != merge`**
- Under NO circumstances may an agent execute the following without separate, explicit Owner authorization:
  - `git push` to any remote (origin, upstream);
  - Pull request creation, update, or merging via GitHub CLI (`gh pr`);
  - Fast-forward or rebase merges on base branches;
  - Force-pushing (`--force`, `+<branch>`);
  - Branch deletion on remotes.
