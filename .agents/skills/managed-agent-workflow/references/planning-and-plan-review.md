# Phase 1 & 2: Planning & Independent Plan Review

This reference specifies the operational protocol for Phase 1 (Planning) and Phase 2 (Independent Plan Review) within the Managed-Agent Workflow.

---

## 1. Phase 1: Planning Execution

### Subagent Specification
- **Role:** Planner (`P`)
- **Invocation:**
  ```json
  {
    "TypeName": "self",
    "Role": "Planner",
    "Prompt": "<Task requirements, scope, exclusions, and instruction to produce a detailed implementation plan>",
    "Model": "inherit",
    "Workspace": "inherit"
  }
  ```
- **Tool Capabilities:** Write-capable (`enable_write_tools: true`).
- **Context:** Fresh context window. Inherits tool configuration, model, and system prompt from Main Controller.

### Planner Deliverables
Planner must discover the repository, design the solution, and write the plan to an authoritative path (e.g., `docs/workstreams/<workstream>/plan.md` or task-specific plan file). The plan must contain:
1. Goal, scope, and explicit non-goals.
2. Confirmed repository facts vs. assumptions.
3. Component/file breakdown with proposed changes.
4. Concrete verification plan (test commands, script validations).
5. Invariants and risk assessment.

### Phase 1 Completion Handshake
When complete, Planner sends a message to Main Controller containing:
- Exact path to the written plan.
- Concise summary of the plan.
- Key material assumptions or trade-offs.
- Declaration of ready-for-review state.

**Rule:** Main Controller must NOT kill Planner `P`. Planner's session remains `idle` to receive potential correction feedback.

---

## 2. Phase 2: Independent Plan Review

### Candidate Hashing
Before spawning Plan Reviewer, Main Controller generates a stable cryptographic identity for the candidate plan:
```powershell
git hash-object <plan-path>
```
Store this value in ephemeral state as `candidate_plan_hash`.

### Subagent Specification
- **Role:** Plan Reviewer (`R`)
- **Definition:** Defined via `define_subagent` if not already registered:
  ```json
  {
    "name": "plan-reviewer",
    "description": "Independent read-only reviewer for implementation plans.",
    "system_prompt": "You are an independent, highly critical technical plan reviewer...",
    "enable_write_tools": false,
    "enable_subagent_tools": false,
    "enable_mcp_tools": false
  }
  ```
- **Invocation:**
  ```json
  {
    "TypeName": "plan-reviewer",
    "Role": "Plan Reviewer",
    "Prompt": "<Exact plan path, candidate_plan_hash, requirements, and review rubric>",
    "Model": "inherit",
    "Workspace": "inherit"
  }
  ```
- **Tool Capabilities:** Hard read-only (`enable_write_tools: false`). Reviewer has only file-viewing and search tools (`view_file`, `grep_search`, `find_by_name`, `list_dir`).
- **Context:** Fresh context window, completely independent of Planner `P`.

### Plan Review Rubric
Plan Reviewer must evaluate:
1. **Plan Identity:** Does the plan content match `candidate_plan_hash`?
2. **Repository Evidence:** Are all referenced files, class signatures, method names, and datapack schemas verified against real repository files?
3. **Completeness & Scope:** Does the plan solve the problem without unnecessary refactoring or speculative infrastructure?
4. **Safety & Invariants:** Does the plan respect repository contracts, Mixin rules, and gameplay constraints?
5. **Verification Rigor:** Are proposed tests authoritative and sufficient?

### Review Verdict Format
Reviewer returns either `PASS` or `BLOCKING_FINDINGS`.
If blocking findings exist, each finding must adhere to this structured format:
```markdown
### Finding [ID]
- **Severity:** BLOCKING (Critical / Required)
- **Claim:** Precise statement of what is wrong or unverified.
- **Repository Evidence:** Exact file paths, line numbers, or test outputs demonstrating the issue.
- **Impact:** Architectural, runtime, or gameplay consequence if unaddressed.
- **Required Resolution:** Actionable direction for Planner to correct the defect.
```

**Rule:** Plan Reviewer must NOT edit any repository files. Reviewer remains `idle` to participate in potential re-reviews.
