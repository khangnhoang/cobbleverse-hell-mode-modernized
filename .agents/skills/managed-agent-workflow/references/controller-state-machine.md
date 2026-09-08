# Main Controller Ephemeral State Machine

This reference specifies the ephemeral state variables, state transitions, subagent session boundaries, and invariant enforcement rules managed by Main Controller during Mode 3 orchestration.

---

## 1. Ephemeral Controller State

Main Controller maintains orchestration state purely within its active session memory (or an ephemeral scratch note in `<appDataDir>\brain\<conversationId>\scratch\controller-state.json`).

**FORBIDDEN:** Do NOT commit orchestration state files into the git repository.

### State Schema
```text
{
  "phase": "IDLE" | "PLANNING" | "PLAN_REVIEW" | "PLAN_RECONCILING" | "FROZEN" | "IMPLEMENTING" | "IMPL_REVIEW" | "IMPL_RECONCILING" | "COMPLETED" | "ESCALATED",
  "baseline_commit": "<SHA-1>",
  "plan_path": "<relative/path/to/plan.md>",
  
  "planner_id": "<conversationId>",
  "plan_reviewer_id": "<conversationId>",
  "plan_reconciliation_count": 0,
  "candidate_plan_hash": "<SHA-1>",
  "frozen_plan_hash": "<SHA-1>",
  "plan_checkpoint_commit": "<SHA-1>",

  "implementor_id": "<conversationId>",
  "impl_reviewer_id": "<conversationId>",
  "impl_reconciliation_count": 0,
  "final_checkpoint_commit": "<SHA-1>"
}
```

---

## 2. State Transition Matrix

| Current Phase | Event / Signal | Target Phase | Actions Executed by Main Controller |
| :--- | :--- | :--- | :--- |
| `IDLE` | Task classified as Mode 3 | `PLANNING` | Record `baseline_commit`. Spawn Planner `P` via `invoke_subagent` (`enable_write_tools: true`). Store `planner_id`. |
| `PLANNING` | Planner reports ready | `PLAN_REVIEW` | Compute `candidate_plan_hash = git hash-object <plan_path>`. Spawn Plan Reviewer `R` (`enable_write_tools: false`). Store `plan_reviewer_id`. |
| `PLAN_REVIEW` | Reviewer issues `BLOCKING_FINDINGS` | `PLAN_RECONCILING` | Increment `plan_reconciliation_count += 1`. Check `<= 2`. Dispatch findings to `planner_id` via `send_message`. |
| `PLAN_RECONCILING` | Planner updates plan | `PLAN_REVIEW` | Recompute candidate hash. Dispatch updated plan to `plan_reviewer_id` via `send_message`. |
| `PLAN_REVIEW` / `PLAN_RECONCILING` | `plan_reconciliation_count > 2` with blocking findings | `ESCALATED` | Halt loop. Deliver full comparative finding dossier to Owner. Do not spawn additional agents. |
| `PLAN_REVIEW` | Reviewer issues `PASS` | `FROZEN` | Verify `post_review_hash == candidate_plan_hash`. Set `frozen_plan_hash`. Create local checkpoint commit. Terminate `planner_id` and `plan_reviewer_id`. |
| `FROZEN` | Implementation authorized | `IMPLEMENTING` | Spawn Implementor `I` (`enable_write_tools: true`) with frozen plan path and hash. Store `implementor_id`. |
| `IMPLEMENTING` | Implementor reports ready | `IMPL_REVIEW` | Prepare evidence manifest (working-tree diff, status, test logs). Spawn Implementation Reviewer `IR` (`enable_write_tools: false`). Store `impl_reviewer_id`. |
| `IMPL_REVIEW` | Reviewer issues `BLOCKING_FINDINGS` | `IMPL_RECONCILING` | Increment `impl_reconciliation_count += 1`. Check `<= 2`. Dispatch findings to `implementor_id` via `send_message`. |
| `IMPL_RECONCILING` | Implementor corrects code | `IMPL_REVIEW` | Refresh evidence package. Dispatch updated evidence to `impl_reviewer_id` via `send_message`. |
| `IMPL_REVIEW` / `IMPL_RECONCILING` | `impl_reconciliation_count > 2` with blocking findings | `ESCALATED` | Halt loop. Deliver full comparative implementation dossier to Owner. |
| `IMPL_REVIEW` | Reviewer issues `PASS` | `COMPLETED` | Create final local checkpoint commit. Terminate `implementor_id` and `impl_reviewer_id`. Deliver review checkpoint report to Owner. |

---

## 3. Subagent Termination Rules

Main Controller terminates subagents strictly at phase transitions to maintain a clean session topology and prevent zombie processes:
- **Phase 3 (Plan Freeze):** Upon successful plan freeze, Planner `P` and Plan Reviewer `R` are terminated:
  ```json
  {
    "Action": "kill",
    "ConversationIds": [planner_id, plan_reviewer_id]
  }
  ```
- **Phase 5 (Completion / Escalation):** Upon completion or final escalation, Implementor `I` and Implementation Reviewer `IR` are terminated:
  ```json
  {
    "Action": "kill",
    "ConversationIds": [implementor_id, impl_reviewer_id]
  }
  ```

---

## 4. Local Checkpoint Commit Contract

In Mode 3 (Managed-Agent Workflow), when the owner has explicitly authorized implementation through the managed workflow, Main Controller is authorized to create necessary local checkpoint commits:
- **Plan Checkpoint Commit:** Records the frozen plan immediately after Plan Reviewer issues `PASS`.
- **Implementation Checkpoint Commit:** Records the completed, verified implementation after Implementation Reviewer issues `PASS`.

**Strict Negative Constraints:**
- NO automatic `git push`.
- NO pull request creation or update via GitHub CLI.
- NO branch merge or rebase onto main/upstream.
- NO force-pushing (`git push --force`).
- NO branch deletion.
- NO destructive deployment or server modifications.
All remote operations require separate, explicit Owner authorization.
