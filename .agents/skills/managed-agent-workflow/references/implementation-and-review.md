# Phase 4 & 5: Implementation & Independent Implementation Review

This reference defines the execution protocol for Phase 4 (Implementation) and Phase 5 (Independent Implementation Review), including evidence preparation for hard read-only reviewers and local checkpointing.

---

## 1. Phase 4: Implementation Execution

### Preconditions for Spawning Implementor
Before spawning Implementor `I`:
1. Plan Reviewer `R` has issued a `PASS` verdict.
2. Main Controller has verified that `post_review_plan_hash == candidate_plan_hash`.
3. Main Controller has created a local checkpoint commit recording the frozen plan (if permitted under the local checkpoint contract).
4. Planner `P` and Plan Reviewer `R` have been terminated via `manage_subagents(Action: 'kill', ConversationIds: [plannerId, reviewerId])`.

### Subagent Specification
- **Role:** Implementor (`I`)
- **Invocation:**
  ```json
  {
    "TypeName": "self",
    "Role": "Implementor",
    "Prompt": "<Frozen plan path, frozen blob hash, checkpoint commit, and task execution parameters>",
    "Model": "inherit",
    "Workspace": "inherit"
  }
  ```
- **Tool Capabilities:** Write-capable (`enable_write_tools: true`). Full edit and terminal execution tools.
- **Context:** Fresh context window. Independent of Planner and Reviewer sessions.

### Implementor Invariants
1. **Plan Identity Verification:** Before modifying any file, Implementor computes `git hash-object <plan_path>`. If the hash does NOT equal `frozen_plan_hash`, Implementor immediately stops and reports a plan drift violation to Main Controller.
2. **Surgical Execution:** Implementor implements strictly what is defined in the frozen plan. Zero opportunistic refactoring.
3. **Self-Verification:** Implementor executes all unit tests, scripts, and local verifications required by the plan.
4. **Handoff Handshake:** Upon completion, Implementor reports:
   - List of changed files.
   - Exact verification commands executed and raw exit codes.
   - Declaration of readiness for implementation review.
   - Implementor session remains `idle` to await review feedback.

---

## 2. Phase 5: Implementation Review

### Subagent Specification
- **Role:** Implementation Reviewer (`IR`)
- **Definition:** Defined via `define_subagent` if not already registered:
  ```json
  {
    "name": "implementation-reviewer",
    "description": "Independent read-only reviewer for codebase diffs, tests, and plan conformance.",
    "system_prompt": "You are an independent, adversarial code reviewer assessing plan conformance, correctness, and regressions...",
    "enable_write_tools": false,
    "enable_subagent_tools": false,
    "enable_mcp_tools": false
  }
  ```
- **Invocation:**
  ```json
  {
    "TypeName": "implementation-reviewer",
    "Role": "Implementation Reviewer",
    "Prompt": "<Frozen plan path, baseline commit, and evidence manifest/content>",
    "Model": "inherit",
    "Workspace": "inherit"
  }
  ```
- **Tool Capabilities:** Hard read-only (`enable_write_tools: false`). Cannot execute terminal commands.
- **Context:** Fresh context window. Independent of Implementor `I`.

### Evidence Preparation Contract for Hard Read-Only Reviewer
Because Implementation Reviewer is hard read-only, it cannot execute `git diff` or test commands directly. Main Controller is responsible for assembling a complete evidence package:

1. **Working-Tree Diff:** Main runs `git diff <baseline-commit>` to capture tracked changes (including uncommitted working tree edits).
2. **Short Status:** Main runs `git status --short` to expose untracked files or deleted items.
3. **Untracked File Inspection:** Main inspects any untracked files and includes their paths in the evidence package.
4. **Verification Logs:** Main provides exact command lines, exit codes, and relevant output excerpts from automated tests (`validate_repo.py`, `./gradlew test`, `test_rct_runtime_contract.py`).

Main places this evidence package into an external scratch file (e.g., `<appDataDir>\brain\<id>\scratch\implementation-evidence.md`) or directly inside the invocation prompt, ensuring IR has full visibility without requiring write privileges.

### Implementation Review Rubric
Implementation Reviewer verifies:
1. **Plan Conformance:** Did Implementor execute the exact frozen plan without unauthorized deviations?
2. **Blast Radius & Scope:** Are there unrelated edits, reformatting, or cosmetic modifications?
3. **Correctness & Edge Cases:** Are nullability, concurrency, RLS, and type invariants respected?
4. **Verification Authenticity:** Do test outputs actually prove that the change succeeds?
5. **Architectural Coherence:** Are Mixin contracts, datapack structures, and Cobblemon schemas intact?

### Review Verdict Format
- `PASS`: Implementation is fully conformant and verified.
- `BLOCKING_FINDINGS`: Formatted according to the structured finding schema. Dispatched through the reconciliation protocol (max 2 cycles).

### Completion Handshake
When IR issues `PASS`:
1. Main Controller creates a final local checkpoint commit if permitted under the Mode 3 contract.
2. Main terminates Implementor `I` and Implementation Reviewer `IR`.
3. Main presents a structured review checkpoint report to the Owner.
