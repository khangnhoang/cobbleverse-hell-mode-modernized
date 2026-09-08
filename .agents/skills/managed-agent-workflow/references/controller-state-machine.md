# Main Controller Ephemeral State Machine & Orchestration Protocol

This reference specifies the ephemeral state schema, state transitions, Reviewer Invocation Prompt Contract, and subagent termination boundaries managed by Main Controller during Mode 3 orchestration.

---

## 1. Ephemeral Controller State Schema

Main Controller maintains orchestration state purely within session memory (or an ephemeral scratch note in `<appDataDir>\brain\<conversationId>\scratch\controller-state.json`).

> [!CAUTION]
> **Repository Hygiene:** Orchestration state files must NEVER be committed into the git repository.

```json
{
  "phase": "IDLE | PLANNING | PLAN_REVIEW | PLAN_RECONCILING | FROZEN | IMPLEMENTING | IMPL_REVIEW | IMPL_RECONCILING | COMPLETED | ESCALATED",
  "baseline_commit": "<SHA-1>",
  "plan_path": "docs/workstreams/<id>/plan.md",
  
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
| `IDLE` | Task classified as Mode 3 | `PLANNING` | Record `baseline_commit`. Spawn Planner `P` via `invoke_subagent` (`enable_write_tools: true`) governed by `implementation-planning-and-contract-freeze`. Store `planner_id`. |
| `PLANNING` | Planner reports ready | `PLAN_REVIEW` | Compute `candidate_plan_hash = git hash-object <plan_path>`. Format 3-part review prompt. Spawn Plan Reviewer `R` (`enable_write_tools: false`). Store `plan_reviewer_id`. |
| `PLAN_REVIEW` | Reviewer issues `BLOCKING_FINDINGS` | `PLAN_RECONCILING` | Increment `plan_reconciliation_count += 1`. If `> 2`, halt to `ESCALATED`. Else dispatch findings to `planner_id` via `send_message`. |
| `PLAN_RECONCILING` | Planner updates plan | `PLAN_REVIEW` | Recompute candidate hash. Dispatch updated candidate and author response to `plan_reviewer_id` via `send_message`. |
| `PLAN_REVIEW` / `PLAN_RECONCILING` | `plan_reconciliation_count > 2` with blocking findings | `ESCALATED` | Halt loop immediately. Compile and deliver structured finding dossier to Owner. Do not spawn additional agents. |
| `PLAN_REVIEW` | Reviewer issues `PASS` | `FROZEN` | Recompute `post_review_hash = git hash-object <plan_path>`. Assert `post_review_hash == candidate_plan_hash`. Store `frozen_plan_hash`. Create Plan Freeze Checkpoint. Terminate `planner_id` and `plan_reviewer_id`. |
| `FROZEN` | Implementation authorized | `IMPLEMENTING` | Spawn Implementor `I` (`enable_write_tools: true`) with frozen plan path and hash. Store `implementor_id`. |
| `IMPLEMENTING` | Implementor reports ready | `IMPL_REVIEW` | Assemble Verification Evidence Manifest via `test-and-verification-strategy`. Format 3-part review prompt. Spawn Implementation Reviewer `IR` (`enable_write_tools: false`). Store `impl_reviewer_id`. |
| `IMPL_REVIEW` | Reviewer issues `BLOCKING_FINDINGS` | `IMPL_RECONCILING` | Increment `impl_reconciliation_count += 1`. If `> 2`, halt to `ESCALATED`. Else dispatch findings to `implementor_id` via `send_message`. |
| `IMPL_RECONCILING` | Implementor corrects code | `IMPL_REVIEW` | Refresh Verification Evidence Manifest. Dispatch updated evidence to `impl_reviewer_id` via `send_message`. |
| `IMPL_REVIEW` / `IMPL_RECONCILING` | `impl_reconciliation_count > 2` with blocking findings | `ESCALATED` | Halt loop immediately. Deliver structured implementation dossier to Owner. |
| `IMPL_REVIEW` | Reviewer issues `PASS` | `COMPLETED` | Record explicit `IR verdict: PASS`. Create Verified Implementation Checkpoint via `git-checkpoint-workflow`. Terminate `implementor_id` and `impl_reviewer_id`. Deliver review checkpoint report to Owner. |

---

## 3. Reviewer Invocation Prompt Contract (Canary Lessons 2 & 3)

When invoking or dispatching review requests to Plan Reviewer `R` or Implementation Reviewer `IR`, Main Controller must structure the prompt into exactly three distinct partitions:

```markdown
# Review Request: [Candidate Plan | Implementation Diff]

## Partition 1: Immutable Invariants & Governance Authority
- Repository Single Source of Truth: root `AGENTS.md`.
- Operating under Mode 3 Managed-Agent Workflow.
- Role: Independent adversarial reviewer with hard read-only tools (`enable_write_tools: false`).
- Discipline: Claim strength must not exceed evidence strength. Prohibit unverified absolute statements.

## Partition 2: Untrusted Candidate Anchors & Claims
- Candidate Artifact: `<path/to/plan.md>` (Hash: `<SHA-1>`) | Working-Tree Diff & Evidence Manifest.
- Candidate Claims: [Summary of author's claims, modified paths, and rationale].
> [!IMPORTANT]
> **Mandatory Reviewer Instruction:** Anchors and candidate claims are UNTRUSTED navigation hints. You must independently verify material claims against real repository files and contracts. Perform broad exploratory discovery only when evidence is missing, ambiguous, or conflicting.

## Partition 3: Authoritative Evaluation Rubric
- Evaluate strictly against the criteria defined in:
  - For Plan Reviewer R: `.agents/skills/code-review-and-quality/references/plan-review-rubric.md`
  - For Implementation Reviewer IR: `.agents/skills/code-review-and-quality/references/implementation-review-rubric.md`
- Issue an explicit verdict: `PASS` | `BLOCKING_FINDINGS` | `BLOCKED`.
- Structure any findings using the Standard Finding Schema (Critical, Required, Suggestion, Nit, FYI).
```

---

## 4. Subagent Lifecycle & Termination Boundaries

To maintain session cleanliness and prevent zombie processes or token waste, Main Controller terminates subagent pairs strictly at phase gates:

### Phase 3 Termination (Plan Freeze Handshake)
Upon successful plan freeze:
```json
{
  "Action": "kill",
  "ConversationIds": ["<planner_id>", "<plan_reviewer_id>"]
}
```

### Phase 5 Termination (Completion / Escalation Handshake)
Upon implementation completion or final escalation:
```json
{
  "Action": "kill",
  "ConversationIds": ["<implementor_id>", "<impl_reviewer_id>"]
}
```

---

## 5. Local Checkpoint & Remote Gating Integration

All git operations executed by Main Controller must follow [`git-checkpoint-workflow`](../../git-checkpoint-workflow/SKILL.md):
- Staging must be surgical (`git add <file>`);
- Local checkpoints recorded with Conventional Commit format;
- Audit lineage SHAs preserved without rebasing;
- Remote operations (`git push`, PR creation, merge) remain strictly gated behind explicit Owner authorization.
