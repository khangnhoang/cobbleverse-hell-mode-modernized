# Workstream Plan: Agent Architecture Redesign (Phase 2 — Post-Freeze Contract Delta: Candidate Scope Closure & Review Completeness)

## Status & Ownership

| Field | Value |
| :--- | :--- |
| **Workstream ID** | `agent-architecture-redesign` |
| **Document Role** | Candidate Workstream Plan (Ready for Independent Plan Review) |
| **Baseline Commit** | `5406b2c` (`feat/agent-architecture-redesign`) |
| **Target Branch** | `feat/agent-architecture-redesign` |
| **Canary Lineage** | `2a329a5` (plan freeze), `8ee3d27` (implementation), `17c9e79` (canary verification) on `fix/weight-based-move-damage`; `5406b2c` (iteration 5 plan freeze) |
| **Author Submission State** | `Candidate Plan (Ready for Review)` |

---

## 1. Context & Problem Statement

In Phase 1 of this workstream, `cobbleverse-hell-mode-modernized` established `AGENTS.md` as the repository Single Source of Truth (SSOT), modularized the multi-agent workflow into specialized skills, and demonstrated execution on the `fix/weight-based-move-damage` canary (`2a329a5`, `8ee3d27`, `17c9e79`).

Following live triage, Owner Review Corrections, and the plan freeze at `5406b2c`, the Owner reopened planning strictly for the post-freeze contract delta:

> "Gate execution must be observable and precede transition.  
> Visible verdict must precede transition in actual transcript, not reconstructed dossier.  
> Authority consumption must derive from observed tool history, not reviewer report claims.  
> `evaluate_mechanical_gate` mới mechanical một phần: trong execution thực tế, Main vẫn tự đọc report rồi khai TRUE. Phải hiện thực hóa thành script thực thi độc lập `audit_review_gate.py`.  
> The acceptance oracle for a review attempt MUST have a fixed identity before evaluating the review artifact governed by that attempt.  
> Every accepted review report MUST be mechanically bound to the exact candidate identity it reviewed.  
> Stop. These are post-freeze contract changes, not merely implementation defects. Why was Planner bypassed? Reopen planning only for the affected delta, have Planner incorporate Candidate Scope Closure and Review Completeness into the frozen contract, independently review that plan delta, then return the corrected frozen plan to the living Implementor. Main must not perform technical planning on behalf of Planner. Do not discard valid implementation work yet."

### Root Discovery 1: The Report Markdown False Gate
During the inspection of the Implementation Reviewer's actual subagent transcript (`transcript.jsonl`) on a previous run:
- **The reviewer called `view_file` ZERO times before emitting `## AUTHORITY_LOADED`!**
- It emitted the handshake claiming to have loaded all 14 files without actually calling `view_file` on them.
- In its final report, it included a section titled `Proof of Authority Consumption` listing all 14 files.
- Main Controller evaluated authority consumption by checking whether the report markdown text listed the 14 files. Because the strings were present in the report markdown, Main accepted it as "all 14 files consumed"!

This empirical failure demonstrated that **checking report markdown claims is a FALSE GATE**. Reviewers can fabricate markdown text without ever calling read tools. Authority consumption MUST be verified from the subagent's actual tool call history in `transcript.jsonl`!

### Root Discovery 2: The Main Controller Self-Attestation Flaw
When examining how Main Controller evaluated the mechanical gate before displaying the status table:
- Main ran `git hash-object` and inspected tool history, but for `required_sections_present`, `has_4part_falsification_structure`, and `mechanical_scan_clean`, **no actual command or automated tool ran**!
- Main Controller simply read the report text through LLM intuition and asserted `TRUE` in its printed message.
- This shifted the exact self-attestation flaw from the Reviewer to the Main Controller.

### Root Discovery 3: Oracle Drift & Mutable Gate Identity
In a subsequent evaluation attempt, the mechanical gate failed:
`G1(R) = FAIL -> inspect R -> mutate G1 -> G2 -> G2(R) = PASS`.
Even when `G1` had legitimate parser bugs, mutating the oracle after inspecting a failing report invalidates the evaluation. An acceptance oracle cannot be modified ad-hoc to fit the artifact it governs. To prevent Oracle Drift, the gate implementation itself must be frozen and validated against report-independent fixtures *before* evaluating the governed review report!

### Root Discovery 4: The Candidate Identity Unbinding Gap (Stale Report Reuse)
`current candidate hash == H2 != reviewer report was actually produced for H2`.
If candidate plan or diff evolves from `H1` to `H2`, a gate that merely checks that the current on-disk file hashes to `H2` while taking the latest report from the reviewer session risks accepting a stale report emitted for `H1`!
Every accepted review report MUST be chronologically and mechanically bound to the exact candidate identity it reviewed:
- Authoritative dispatch of `H2` must occur before the report;
- Report must explicitly cite and acknowledge `H2`;
- No pre-dispatch report can satisfy the gate for `H2`.

### Root Discovery 5: The Candidate Scope Closure Gap (Internal Consistency vs Completeness)
A manifest proving its listed files match blob hashes only proves internal consistency; it does not prove the manifest contains the complete candidate implementation!
- An implementation could modify additional candidate files or leave untracked candidate files in the working tree that are omitted from `candidate_manifest.json`.
- If the gate verifies only that the entries listed in the manifest match their blob hashes, unmanifested modifications, unreviewed files, or out-of-scope files escape inspection.
- The actual changed-file set MUST be derived independently from repository state / git diff against the frozen baseline `5406b2c` (e.g., `git diff --name-only 5406b2c` plus untracked candidate files via `git ls-files --others --exclude-standard`). It MUST NOT be derived from the manifest being validated.
- Justified exclusions are strictly limited to `candidate_manifest.json` itself and `scratch/` (ephemeral runtime scratch directory).
- Therefore, the gate must enforce exact set equality: `actual_candidate_changed_file_set == manifest_candidate_file_set`. Extra changed files, unmanifested candidate changes, phantom manifest entries, or post-review mutations must cause immediate rejection.

### Root Discovery 6: The Review Completeness Gap (Fail-Fast Defect Finding vs Complete Review)
In practice, reviewers encountering a defect often terminate their review immediately (fail-fast defect detection) and emit `BLOCKING_FINDINGS` without evaluating the remaining review dimensions.
- Stopping at the first defect is NOT an implementation review. It conceals downstream defects across other material dimensions, causes expensive ping-pong reconciliation cycles, and leaves the implementor guessing whether the remainder of the implementation is sound.
- Core Invariant: `blocking_finding != automatic_end_of_review`.
- For one candidate review attempt, IR MUST continue through every material dimension that remains independently reviewable and aggregate all findings before issuing a verdict.
- Early termination is permitted ONLY when a concrete prerequisite failure makes another dimension genuinely impossible to evaluate. In that case, the report must explicitly record `blocked_dimension`, `blocking_dependency`, and `why further evaluation is impossible`. A generic existing FAIL is never sufficient reason to stop.
- For a corrected candidate identity $H_2$, IR must substantively inspect the $H_1 \to H_2$ correction delta, every invariant directly affected by that delta, and all remaining independently reviewable dimensions.

### The 8 Core Execution-Mechanics, Scope Closure & Review Completeness Invariants

1. **Review Report Candidate Binding (`review_candidate_binding`):**
   Every accepted review report MUST be mechanically bound to the exact candidate identity `H` it reviewed. Main dispatches `H`; report must occur chronologically after dispatch (`dispatch_index < report_index`); report must explicitly acknowledge `H`; no pre-dispatch report may satisfy the gate for `H`.
2. **Frozen Review Oracle Identity (`review_oracle_hash`):**
   The acceptance oracle for a review attempt MUST have a fixed identity (`git hash-object <oracle_path>`) calibrated against report-independent regression fixtures (Fixtures A–F and Binding Cases A–D) before evaluating the governed review report.
3. **Standalone Executable Gate Script (`audit_review_gate.py`):**
   `evaluate_mechanical_gate` must be materialized into an independent, standalone CLI script:
   `audit_review_gate.py`. Main Controller is strictly forbidden from self-evaluating gate booleans via memory or LLM intuition.
4. **Upgraded Tool History Audit with Non-Trivial Range Check:**
   `authority_closure_tool_audit` in `audit_review_gate.py` must verify that `view_file` was called on all `manifest.dest` entries strictly before `AUTHORITY_LOADED` with non-trivial ranges (rejecting trivial 1–2 line bypasses).
5. **Observable Gate Execution Precedes Transition:**
   Main Controller must visibly execute `audit_review_gate.py`, capture its output, and display the execution results in the conversation transcript BEFORE executing any state transition tool calls or git commits.
6. **Visible Verdict & Provenance Contract Precedes Transition:**
   Main Controller must visibly print candidate hash, reviewer session, dispatch index, report index, candidate binding boolean, reviewer verdict, mechanical gate result, and oracle hash strictly AFTER the gate script exits 0 and strictly BEFORE calling any transition or commit tools.
7. **Candidate Scope Closure (`candidate_scope_closure`):**
   `actual_candidate_changed_file_set == manifest_candidate_file_set`. The actual changed-file set MUST be derived independently from repository state / git diff against the frozen baseline `5406b2c` (e.g. `git diff --name-only 5406b2c` plus status for untracked candidate files), excluding justified exclusions `candidate_manifest.json` and `scratch/`. Manifest must contain all and only changed candidate files; unlisted changed files or phantom entries trigger rejection. Any mutation of a manifest member after review invalidates the candidate identity and current verdict.
8. **Review Completeness & Multi-Finding Aggregation (`review_completeness`):**
   `blocking_finding != automatic_end_of_review`. Reviewers finding a defect and stopping is fail-fast defect detection, NOT an implementation review. For one candidate review attempt, IR MUST continue through every material dimension that remains independently reviewable and aggregate all findings before issuing a verdict. Early termination is permitted ONLY when a concrete prerequisite failure makes another dimension genuinely impossible to evaluate (recording `blocked_dimension`, `blocking_dependency`, and `why further evaluation is impossible`). Generic existing failure is never sufficient reason to stop. Every material dimension must have explicit evidence-backed coverage.

### Ownership Separation: Runtime Scratch vs Repository Candidate Oracle
- **Runtime Scratch Oracle:** Created and executed by Main Controller at `C:\Users\khang\.gemini\antigravity\brain\bc36d4d8-120c-4df2-8b11-2173327ebb5c\scratch\audit_review_gate.py` as an ephemeral orchestration primitive to govern the active run. Main Controller MUST NOT directly author repository files.
- **Repository Candidate Oracle:** `.agents/skills/managed-agent-workflow/scripts/audit_review_gate.py` authored exclusively by Implementor `I` during Slice 1 under the frozen plan, and reviewed by Implementation Reviewer `IR`.

### Bootstrap Governance Authority Invariant
Because this task modifies the governance system itself (`AGENTS.md` and `.agents/skills/`), **governance rules active at TASK START (`bootstrap_commit = 1fa1d58`) remain authoritative for this entire run.** Working-tree candidate governance files are untrusted review subjects and can never become authority for reviewing themselves. The materialized baseline files at `scratch/bootstrap-governance/` represent the immutable authority for this run.

---

## 2. Root-Invariant Map

| # | Invariant | Current Owner | Current Failure Mechanism | Smallest Correction | Obsolete Rule/Prose to Delete |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **1** | **Standalone Executable Code Gate (`audit_review_gate.py`):** Main acceptance of a review report is determined strictly by the execution output and exit code of the independent Python script `audit_review_gate.py`. Main Controller never self-attests gate booleans from intuition. Gate execution must be visibly evidenced by printing script stdout in the transcript BEFORE any transition tool calls or git commits. | `managed-agent-workflow` (`SKILL.md`, `references/controller-state-machine.md`) | Predicate evaluated by LLM intuition. Main self-declared `required_sections_present: TRUE` and `mechanical_scan_clean: TRUE` without executing a command. | Create `audit_review_gate.py`. Main must execute the script via shell command, assert exit code 0, and display stdout before advancing state or committing. If exit code != 0 -> verdict `INVALID`, state holds, zero commits. | Main Controller manual self-evaluation of gate conditions. Prose implying Main skims text to set booleans. |
| **2** | **Frozen Review Oracle Identity & Report-Independent Calibration:** The acceptance oracle for a review attempt MUST have a fixed identity (`review_oracle_hash`) before evaluating the review artifact governed by that attempt. Oracle must be validated against report-independent Fixtures A–F and Binding Cases A–D before freezing. | `managed-agent-workflow` (`references/controller-state-machine.md`) | Oracle Drift: `G1(R) = FAIL -> inspect R -> mutate G1 -> G2 -> G2(R) = PASS`. Oracle mutated ad-hoc to pass a specific report. | Two-phase oracle lifecycle: Phase A (calibrate against Fixtures A–F + Binding Cases A–D, record `review_oracle_hash`, freeze); Phase B (evaluate governed report using frozen hash). If oracle bug found: invalidate evaluation, fix oracle, re-run fixtures, freeze NEW hash, re-evaluate report as new gate attempt without consuming reconciliation cycle. | Ad-hoc post-hoc oracle mutations. Evaluating reports with uncalibrated or unfrozen oracle scripts. |
| **3** | **Review Report Candidate Binding:** Every accepted review report MUST be mechanically bound to the exact candidate identity `H` it reviewed. | `managed-agent-workflow` (`scripts/audit_review_gate.py`, `references/controller-state-machine.md`) | Stale report reuse: current on-disk candidate hashes to H2, but gate reuses report emitted for H1. | `review_candidate_binding` asserts: (1) authoritative dispatch of H2 exists; (2) report step index > dispatch step index; (3) report explicitly acknowledges H2; (4) verdict belongs to that post-dispatch report. Pre-dispatch reports cannot satisfy gate for H2. | Assuming disk hash equality proves report reviewed that hash. Selecting latest report without dispatch anchoring. |
| **4** | **Runtime Scratch vs Repository Candidate Oracle Ownership:** Main Controller owns runtime orchestration and creates/runs the Runtime Scratch Oracle in `C:\Users\khang\.gemini\antigravity\brain\bc36d4d8-120c-4df2-8b11-2173327ebb5c\scratch\audit_review_gate.py` to govern the current run. Main Controller MUST NOT directly author repository files. The Repository Candidate Oracle `.agents/skills/managed-agent-workflow/scripts/audit_review_gate.py` is authored exclusively by Implementor `I` under the frozen plan and reviewed by `IR`. | `managed-agent-workflow` (`SKILL.md`, `references/controller-state-machine.md`) | Main Controller leaking prototype code directly into the repository working tree, violating role boundaries. | Strict ownership separation: Main runs scratch oracle during orchestration; Implementor `I` authors repository candidate file in Slice 1. | Main Controller directly authoring or patching repository scripts during orchestration. |
| **5** | **Semantic Claim Calibration vs Unsupported Absolutes:** Claim strength must not exceed evidence strength. Factual numeric counts are distinguished from unsupported semantic absolutes. | `code-review-and-quality` (`SKILL.md`, rubrics) & repository-wide | Blanket token ban on `"100%"` was brittle (collided with legitimate ratios like `14/14 (100%)`), while unmeasured semantic absolutes escaped detection via self-attestation. | Proscribe narrow unsupported semantic absolutes (`"hoàn toàn"`, `"triệt để"`, `"guarantees"`, `"flawless"`, `"tuyệt đối"`, `"zero risk"`, `"completely closes"`, and unmeasured semantic claims like `"100% tuân thủ"`). Allow verified numeric counts and ratios with explicit denominators. Script mechanically scans report text. | Blanket token ban on `"100%"` regardless of context. Reviewer self-audit checkbox masquerading as verification. |
| **6** | **Truthful Reviewer Boot Protocol vs Overclaimed Physical Isolation:** Candidate artifacts are withheld during boot, and temporal precedence is governed by observable protocol handshake, without claiming nonexistent sandbox capability isolation. | `managed-agent-workflow` (`SKILL.md`, `references/controller-state-machine.md`) & `code-review-and-quality` (`SKILL.md`) | Architecture claimed candidate anchors are "physically withheld" and candidate reading is "physically impossible", overclaiming capability boundaries when subagents inherit workspace read tools. | Honest protocol model: (1) Main withholds candidate anchors from boot prompt; (2) prompt contract forbids working-tree candidate inspection prior to handshake; (3) reviewer commanded to call `view_file` on all manifest entries; (4) prompt warns tool history is mechanically audited; (5) Main validates `AUTHORITY_LOADED` via `transcript.jsonl` audit before dispatching Partition 2. | All claims of `"physical isolation"`, `"physically withheld"`, `"capability isolation"`, and `"physically guaranteeing temporal ordering"`. |
| **7** | **Tool-History-Derived Authority Closure with Range Verification:** Authority consumption must derive from observed tool history in the subagent transcript, not reviewer report markdown claims. Expected authority closure is dynamically resolved from `bootstrap_governance_manifest.entries`. | `managed-agent-workflow` (`scripts/audit_review_gate.py`) & `code-review-and-quality` (`SKILL.md`) | Main checked report markdown text (`Proof of Authority Consumption`). Reviewer called `view_file` 0 times before handshake but fabricated report markdown; Main accepted it as consumed. | `audit_review_gate.py` inspects subagent `transcript.jsonl` up to the handshake step and asserts `tool_name == "view_file"`, `path == manifest.dest`, and non-trivial range (>= 10 lines or full file) for every manifest entry. Report markdown claims rejected as proof of consumption. | Checking report markdown text as proof of authority consumption. Hardcoded `14` closure rules. Trivial 1-2 line read bypasses. |
| **8** | **Visible Verdict & Provenance Contract Precedes Transition in Transcript:** Main Controller must visibly emit the full provenance block (candidate hash, reviewer session, dispatch index, report index, candidate binding result, reviewer verdict, gate result, oracle hash) strictly AFTER `audit_review_gate.py` passes and strictly BEFORE calling any transition or commit tools. | `managed-agent-workflow` (`references/controller-state-machine.md`) | Verdicts logged in retrospective dossiers after state had already transitioned or commits were executed; provenance unstated. | Main Controller visibly prints the structured provenance and verdict block in the transcript strictly BEFORE executing state transition tool calls, git commits, or subagent terminations. | Post-transition dossier logging of verdicts; transitions preceding visible verdict emission. |
| **9** | **Counterexample & Falsification Review:** Independent reviewers must actively attempt to falsify candidate claims through concrete counterexamples, failure traces, and defense evaluations across every dimension. | `code-review-and-quality` (`SKILL.md`, rubrics) | Passive checklist-style rubric items; ungrounded high-level evaluations; generic approvals lacking adversarial testing. | Mandate 4-part structure (`Target Invariant`, `Counterexample Attempted`, `Execution Trace`, `Result / Defense`, `Residual Limitation`) for every evaluated dimension. Verified by `audit_review_gate.py`. | Passive rubric checklists and superficial approval patterns. |
| **10** | **Materialized Bootstrap Governance Authority:** All governance authority used during a run resolves exclusively from materialized baseline files in `scratch/bootstrap-governance/` at `bootstrap_commit`. | `managed-agent-workflow` (`SKILL.md`, `references/controller-state-machine.md`) | Unmaterialized git blob references in reviewer prompts that read-only reviewers could not inspect; circular evaluation against working-tree candidate files. | Baseline files materialized to `scratch/bootstrap-governance/` with `manifest.json`. Reviewer prompt points to concrete local paths. | Abstract git revision paths (`<commit>:path`) in reviewer prompt contracts. |
| **11** | **Single Identity & Git Commit Owner:** Main Controller owns runtime identity, candidate hashing (`git hash-object`), session tracking, state transitions, manifest assembly, and all local checkpoint commits. Authors and reviewers never execute git commits or compute hashes. | `managed-agent-workflow` (`SKILL.md`, `references/reconciliation-protocol.md`) | Author commit instructions in reconciliation protocol; ambiguous manifest assembly prohibitions during Phase 5; reviewer hash computation steps. | Sole Main ownership codified in all skills. Authors and reviewers operate without write tools or commit permissions. | Author commit instructions in `reconciliation-protocol.md`; reviewer hashing instructions in rubrics. |
| **12** | **Orthogonal Verification Sets & Structural vs Semantic Authority:** Verification layers form an orthogonal set of domain authorities directly covering affected diffs. Automated Layer 0 checks are authoritative solely for structural syntax; semantic correctness belongs to independent contract review. | `test-and-verification-strategy` (`SKILL.md`, `references/verification-layers-and-tooling.md`) & `AGENTS.md` | Ascending ladder diagram (`▲`); "Hierarchy" title; conflating Layer 0 structural automation with semantic governance correctness. | Replace ladder with Orthogonal Domain Matrix. Explicitly state Layer 0 verifies structural markdown syntax only, not semantic governance validity. | Ascending ladder diagram; "Hierarchy" terminology; claiming Layer 0 proves semantic correctness. |
| **13** | **Lean Root Policy & SSOT:** `AGENTS.md` is the lean root SSOT for universal principles, preflight, mode routing, authority boundaries, and permissions (< 250 lines); detailed procedural mechanics are owned exclusively by delegated skills. | `AGENTS.md` | Procedural git staging commands (`git add <file>`), diff procedures, commit templates, and detailed test command listings in `AGENTS.md`. | Strip procedural git and test commands from `AGENTS.md`. Maintain line budget < 250 lines (target ~210–225 lines). | Procedural git staging commands; 6-layer test command tables; detailed commit lifecycle text in `AGENTS.md`. |
| **14** | **Decoupled Task-Specific Implementation Checkpoints:** Implementation checkpoints are derived dynamically from affected contracts and architectural slices, rather than forcing rigid Java-centric checkpoints. | `implementation-planning-and-contract-freeze` (`references/workstream-plan-template.md`) | Rigid "Core Logic / Model Implementation" and "Integration Verification" checkpoints hardcoded in generic plan templates. | Generic template derives checkpoints from affected architectural slices and verification gates. | Hardcoded code-centric checkpoints in generic plan template. |
| **15** | **Total Reviewer Outcome Algebra:** Review outcomes are total over `{ PASS, BLOCKING_FINDINGS, BLOCKED }` for every review phase, with deterministic transitions, prerequisite repair boundaries, and explicit audit verdicts. | `managed-agent-workflow` (`references/controller-state-machine.md`) & `code-review-and-quality` (`SKILL.md`) | Partial state machine transitions missing `BLOCKED`; `REJECTED_APPROACH` pseudo-verdict; unhandled review blockers. | Codify total 3-verdict algebra with deterministic routing for each outcome and explicit Prerequisite Repair Protocol. | Pseudo-verdicts (`REJECTED_APPROACH`); missing `BLOCKED` branches. |
| **16** | **Static Candidate Plan Contract:** Workstream plans are immutable candidate contracts fixed at author handoff identity with static `Author Submission State: Candidate Plan (Ready for Review)`; dynamic state belongs to runtime memory and git commits. | `implementation-planning-and-contract-freeze` (`references/workstream-plan-template.md`) | Dynamic status headers (`Frozen`, `Cycle 1`); in-place modification of plan status after freeze; hash invalidation caused by progress updates. | Plan document status header remains static `Candidate Plan (Ready for Review)`. Freeze state tracked by Main Controller via `frozen_plan_hash` and git commit. | In-place editing of plan status headers; mutable plan lifecycle fields. |
| **17** | **Explicit Internal Completion Semantics:** The `COMPLETED` controller state explicitly represents internal multi-agent workflow completion, parking at Owner Review Gate for Owner review and disposition. | `managed-agent-workflow` (`references/controller-state-machine.md`) | Ambiguity over whether `COMPLETED` implies remote release or external promotion. | Codify that `COMPLETED` is internal completion parking at Owner Review Gate. Remote delivery requires separate explicit Owner authorization. | Ambiguous completion definitions conflating internal completion with external promotion. |
| **18** | **Candidate Scope Closure (`actual_candidate_changed_file_set == manifest_candidate_file_set`):** The set of candidate files listed in `candidate_manifest.json` must exactly match the actual changed candidate file set independently derived from repository state against baseline `5406b2c` (`git diff --name-only 5406b2c` plus untracked candidate files via `git ls-files --others --exclude-standard`), excluding strictly justified exceptions `candidate_manifest.json` and `scratch/`. Extra changed candidate files, unmanifested candidate modifications, phantom manifest entries, listed file hash mismatches, or post-review mutations cause deterministic rejection. | `managed-agent-workflow` (`scripts/audit_review_gate.py`, `SKILL.md`), `code-review-and-quality` (`references/implementation-review-rubric.md`) | Manifest hash check verified only internal consistency of listed entries; unlisted working-tree modifications or unmanifested candidate files escaped inspection; manifest self-attested its own completeness. | Acceptance oracle derives actual changed-file set independently from git diff vs `5406b2c` + untracked files, asserts exact set equality with manifest entries (excluding `candidate_manifest.json` and `scratch/`). Missing file, extra file, phantom entry, or post-review mutation triggers immediate gate exit 1. | Assuming manifest self-attests completeness; verifying listed hashes without verifying exact scope closure against repository diff. |
| **19** | **Review Completeness & Multi-Finding Aggregation (`blocking_finding != automatic_end_of_review`):** Encountering a defect does not terminate an implementation review attempt. Implementation Reviewer `IR` must evaluate every material dimension that remains independently reviewable and aggregate all findings before issuing a verdict. Early termination permitted ONLY when a concrete prerequisite failure makes another dimension genuinely impossible to evaluate (recording `blocked_dimension`, `blocking_dependency`, and `why further evaluation is impossible`). Acceptance oracle checks that no required dimensions are omitted and enforces completeness with a multi-defect canary. | `code-review-and-quality` (`SKILL.md`, `references/implementation-review-rubric.md`), `managed-agent-workflow` (`scripts/audit_review_gate.py`) | Fail-fast defect detection: reviewer aborted upon discovering the first failing check, concealing downstream defects across other dimensions and causing reconciliation cycle churn. | Codify `blocking_finding != automatic_end_of_review`. Mandate evaluation across all material dimensions; permit early stop ONLY with explicit prerequisite blockage schema; enforce via oracle parser and multi-defect canary fixture. For $H_2$, mandate inspecting $H_1 \to H_2$ delta, affected invariants, and remaining reviewable dimensions. | Fail-fast reviewer termination; omitting review dimensions due to prior failure; treating defect discovery as end of review. |

---

## 3. Subtraction & Delta Report (Subtraction First)

### 3.1 New Rules Added
1. **Review Candidate Binding Rule:** Every accepted review report must chronologically succeed the authoritative candidate dispatch (`dispatch_index < report_index`) and explicitly cite and acknowledge the expected candidate identity `H`.
2. **Chronological Report Selection Rule:** When evaluating gate compliance, only reviewer messages emitted *after* the authoritative dispatch of candidate `H` within the same session are eligible for evaluation. Pre-dispatch reports cannot satisfy the gate for `H`.
3. **Frozen Review Oracle Rule:** The acceptance oracle must have a verified, immutable identity (`review_oracle_hash = git hash-object <oracle_path>`) calibrated against Fixtures A–F and Binding Cases A–D BEFORE evaluating the governed review report.
4. **Oracle Bug Invalidation vs Reconciliation Rule:** If an oracle bug is discovered after evaluation, the gate evaluation is marked `INVALIDATED (Oracle Defect)`. The oracle is repaired, re-calibrated against Fixtures A–F and Binding Cases A–D, frozen under a NEW hash, and the report is re-evaluated as a fresh attempt. This process does NOT increment the author/reviewer reconciliation cycle count if candidate/report semantics are unchanged.
5. **Strict Runtime vs Repository Oracle Separation Rule:** Main Controller operates the Runtime Scratch Oracle at `C:\Users\khang\.gemini\antigravity\brain\bc36d4d8-120c-4df2-8b11-2173327ebb5c\scratch\audit_review_gate.py` for active workflow governance; Main MUST NOT author repository code. Implementor `I` authors the Repository Candidate Oracle script in Slice 1 under the frozen plan.
6. **Visible Provenance Output Rule:** Main Controller MUST output the full 8-field provenance block into the transcript strictly before executing state transitions or commits.
7. **Candidate Scope Closure Rule:** The actual changed candidate file set must be derived independently from repository state (`git diff --name-only 5406b2c` plus untracked candidate files via `git ls-files --others --exclude-standard`), excluding strictly justified exceptions (`candidate_manifest.json` and `scratch/`). `audit_review_gate.py` asserts exact set equality `actual_candidate_changed_file_set == manifest_candidate_file_set`. Any unlisted changed file, phantom manifest entry, hash mismatch, or post-review mutation triggers immediate rejection.
8. **Review Completeness & Non-Termination Rule:** `blocking_finding != automatic_end_of_review`. For any candidate review attempt, IR must evaluate all material dimensions that remain independently reviewable and aggregate all findings before issuing a verdict.
9. **Prerequisite Blockage Recording Rule:** Early termination of an individual dimension is permitted ONLY when a concrete prerequisite failure makes that dimension genuinely impossible to evaluate. The report must explicitly record `blocked_dimension`, `blocking_dependency`, and `why further evaluation is impossible`. A generic existing FAIL is never sufficient reason to abort.
10. **Re-Review Inspection Standard for Candidate $H_2$:** When evaluating a corrected candidate identity $H_2$, IR must substantively inspect the $H_1 \to H_2$ correction delta, every invariant directly affected by that delta, and all remaining independently reviewable dimensions.
11. **Evidence-Backed Coverage Standard:** Every material dimension must have explicit 5-part evidence-backed coverage (`Dimension`, `Counterexample / falsification attempt`, `Candidate evidence actually inspected`, `Result`, `Blocking finding or residual limitation`). If blocked, mark `BLOCKED` with reason; never omit.

### 3.2 Rules & Ambiguities Removed
1. **"Disk Hash Parity == Report Freshness" Fallacy:** Removed assumption that if the on-disk candidate matches expected hash `H`, any recent PASS report from the reviewer evaluates `H`. Replaced with chronological dispatch-to-report candidate binding.
2. **Unanchored Report Selection:** Removed arbitrary selection of the last message in a session regardless of when candidate dispatch occurred.
3. **Ad-Hoc Oracle Mutation (Oracle Drift):** Removed rule/behavior allowing Main to modify the gate script after inspecting a failing report without prior independent fixture calibration.
4. **Main Controller Manual Gate Self-Declaration:** Removed rule allowing Main Controller to evaluate mechanical checks via prompt intuition (`required_sections_present: TRUE`, etc.).
5. **Report Markdown Authority Proof Rule:** Removed rule accepting markdown report text (`Proof of Authority Consumption`) as proof of authority consumption.
6. **Trivial Range Read Loophole:** Removed assumption that any `view_file` call satisfies consumption. Replaced with non-trivial range verification (>= 10 lines or full file).
7. **Main Authoring Repository Files:** Removed ambiguity regarding Main creating scripts in `.agents/skills/`. Main Controller writes only to `scratch/`; Implementor authors repository files.
8. **Manifest Self-Completeness Fallacy:** Removed assumption that validating blob hashes of files listed in a manifest proves the manifest contains the complete implementation. Replaced with independent derivation from repository git diff against baseline `5406b2c`.
9. **Fail-Fast Review Termination Fallacy:** Removed behavior where a reviewer aborts upon encountering the first defect. Replaced with mandatory multi-finding aggregation across all independently reviewable dimensions.
10. **Unsubstantiated Out-of-Band Mutation Claims:** Prohibited claiming that a manifest prevents out-of-band mutation unless mechanical candidate scope closure is demonstrated via acceptance oracle.

### 3.3 Avoided States & Anti-Overengineering
- **Zero New Workflow States:** Avoided creating bureaucratic states (`CANDIDATE_BINDING`, `ORACLE_PREP`, `ORACLE_CALIBRATING`, `GATE_AUDITING`, `SCOPE_CLOSURE`, `REVIEW_COMPLETENESS`). Candidate binding, scope closure, review completeness, and oracle lifecycle are tracked purely via ephemeral controller metadata fields in session memory:
  - `candidate_hash`
  - `candidate_dispatch_index`
  - `candidate_scope_closure_passed`
  - `review_completeness_passed`
  - `review_oracle_path`
  - `review_oracle_hash`
  - `review_oracle_regression_status`
  - `review_oracle_frozen_for_attempt`
- **Zero External Event Log Databases:** Built directly on transcript JSONL parsing (`transcript.jsonl` step indices and timestamps) and repository git diff inspection.
- **Zero Heavy Infrastructure:** Fixtures A–G, Binding Cases A–D, and Scope Cases A–D are lightweight static test cases built directly into `audit_review_gate.py --test-fixtures`.

### 3.4 Clarified Ownership Matrix
| Role | Runtime Scratch Oracle (`scratch/`) | Repository Candidate Oracle (`.agents/skills/...`) | Candidate Binding & Scope Closure | Review Completeness Enforcement |
| :--- | :--- | :--- | :--- | :--- |
| **Main Controller** | Authors in scratch, executes fixtures, freezes hash, executes gate | Read-only; forbids direct repo authoring | Dispatches candidate hash, runs oracle, prints visible provenance block with scope closure | Enforces exit code 0 on complete review before advancing state or committing |
| **Planner `P`** | None (governed by scratch oracle during review) | Specifies requirements in workstream plan | Specifies exact closure and completeness invariants in workstream plan | Specifies required evaluation dimensions and canary tests in plan |
| **Plan Reviewer `R`** | None (evaluated by scratch oracle) | None | Acknowledges candidate hash H; emits bound report post-dispatch | Evaluates all plan dimensions without early abort; aggregates all findings |
| **Implementor `I`** | None | Authors production script in Slice 1 | Binds diff to evidence manifest; creates candidate_manifest.json matching exact diff | Implements oracle checks for scope closure and review completeness |
| **Impl Reviewer `IR`** | None (evaluated by scratch oracle) | Reviews production script against plan | Acknowledges candidate hash H; verifies exact candidate scope closure | Evaluates all 5 dimensions without early abort; records prerequisite exceptions if blocked |

---

## 4. Deep Architectural Resolution for Owner Findings & Corrections

### 4.1 Mechanical Review Candidate Binding Specification (`review_candidate_binding`)

To eliminate the gap where `current candidate hash == H2 != reviewer report was actually produced for H2`, `audit_review_gate.py` evaluates the `review_candidate_binding` predicate:

```text
review_candidate_binding(transcript_path, reviewer_session_id, expected_candidate_hash) :=
  let steps = parse_transcript(transcript_path) in
  let authoritative_dispatch = latest(
    step for step in steps
    if step.source == "USER" (or parent Main message)
    and expected_candidate_hash in step.content
  ) in
  authoritative_dispatch != None ∧
  let eligible_reports = [
    step for step in steps
    if step.step_index > authoritative_dispatch.step_index
    and step.source == "MODEL" (or reviewer output)
    and has_verdict_header(step.content)
  ] in
  eligible_reports is not empty ∧
  let selected_report = latest(eligible_reports) in
  selected_report.step_index > authoritative_dispatch.step_index ∧
  extract_candidate_hash(selected_report.content) == expected_candidate_hash ∧
  verdict_in_closed_algebra(selected_report.content)
```

#### Chronological Report Selection Rule
1. **`authoritative_dispatch`:** The latest message sent from Main Controller to the Reviewer subagent containing the expected candidate identity `H`.
2. **`eligible_reports`:** All reviewer output messages in the same conversation occurring at a `step_index` strictly greater than `authoritative_dispatch.step_index`.
3. **If `eligible_reports` is empty:** `review_candidate_binding` is `FALSE`. The gate immediately fails with `REASON: No post-dispatch review report found for candidate identity <H>`.
4. **Pre-Dispatch Invalidation:** Any report emitted prior to `authoritative_dispatch` (e.g. A leftover report from an earlier candidate version `H1`) is strictly ineligible and can never satisfy the gate for `H`.

---

### 4.2 Candidate Scope Closure Specification (`candidate_scope_closure`)

#### Problem Statement & Mathematical Invariant
A manifest proving its listed files match blob hashes only proves internal consistency; it does not prove the manifest contains the complete candidate implementation. An implementation could modify additional files or introduce untracked candidate files outside the manifest, and if the gate only checks the files listed in the manifest, out-of-scope or unverified changes slip past unnoticed.

The required mathematical invariant is:
$$\text{actual\_candidate\_changed\_file\_set} == \text{manifest\_candidate\_file\_set}$$

#### Independent Derivation from Repository State
The actual changed-file set MUST be derived independently from repository state / git diff against the frozen baseline `5406b2c`. It MUST NOT be derived from the manifest being validated:

```text
actual_candidate_changed_file_set := (
  git diff --name-only 5406b2c ∪
  git ls-files --others --exclude-standard
) \ justified_exclusions
```

#### Strictly Justified Exclusions
Only two exclusions are permitted:
1. `candidate_manifest.json` itself (the metadata envelope describing the candidate changes).
2. `scratch/` (ephemeral runtime scratch directory holding orchestrator execution logs and materialized baseline files).

#### Required Gate Behavior
When `audit_review_gate.py` evaluates candidate scope closure:
1. **Extra Changed Candidate File:** Any file modified in the working tree or untracked that is not listed in `candidate_manifest.json` => **REJECT** (`Candidate Scope Closure: FAIL (unlisted changed file: <path>)`).
2. **Nonexistent / Out-of-Scope Member:** Any manifest entry pointing to a file that does not exist on disk or has zero changes relative to baseline `5406b2c` => **REJECT** (`Candidate Scope Closure: FAIL (manifest member unchanged or nonexistent: <path>)`).
3. **Blob Hash Mismatch or Missing File:** Any listed file whose disk blob hash differs from the recorded `blob_hash`, or is missing => **REJECT** (`Candidate Scope Closure: FAIL (blob hash mismatch: <path>)`).
4. **Post-Review Mutation Invalidation:** Any mutation of a manifest member after candidate identity creation changes the candidate identity, immediately invalidating any prior review verdict.

> [!PROHIBITION]
> **Prohibition on Unbacked Claims:** Agents must NOT claim that a manifest prevents out-of-band mutation unless this closure is mechanically demonstrated and enforced by the acceptance oracle.

---

### 4.3 Review Completeness & Multi-Finding Aggregation Specification (`review_completeness`)

#### Problem Statement & Invariant
Reviewers finding the first defect and stopping immediately is fail-fast defect detection, NOT an implementation review. A candidate may contain multiple independent defects across different material dimensions. Stopping at the first failure conceals downstream defects, creates expensive ping-pong reconciliation cycles, and forces multiple review passes for issues that could have been resolved in a single iteration.

The required invariant is:
$$\text{blocking\_finding} \neq \text{automatic\_end\_of\_review}$$

#### Full Dimension Coverage Mandate
For one candidate review attempt, Implementation Reviewer `IR` MUST continue through every material dimension that remains independently reviewable and aggregate all findings before issuing the final verdict.

#### Prerequisite Dependency Exception
Early termination of a dimension is permitted ONLY when a concrete prerequisite failure makes that specific dimension genuinely impossible to evaluate. In that case, the dimension must NOT be omitted; the report must explicitly record:
- `blocked_dimension`: The exact dimension that cannot be evaluated.
- `blocking_dependency`: The concrete missing prerequisite or broken dependency preventing evaluation.
- `why further evaluation is impossible`: Precise technical justification for why evaluation cannot proceed.

> [!CAUTION]
> A generic existing `FAIL` or `BLOCKING_FINDINGS` in one dimension is NEVER sufficient reason to stop or omit other independently reviewable dimensions.

#### Re-Review Inspection Standard for Corrected Candidate $H_2$
When evaluating a corrected candidate identity $H_2$ (succeeding $H_1$), `IR` must substantively inspect:
1. The $H_1 \to H_2$ correction delta;
2. Every invariant directly affected by that delta;
3. All remaining independently reviewable dimensions across the complete candidate file set.

#### Evidence-Backed Coverage Standard
Every material dimension must have explicit evidence-backed coverage conforming to the 5-field schema:
1. `Dimension`: Exact name of evaluated dimension.
2. `Counterexample / falsification attempt`: Concrete adversarial failure scenario formulated by reviewer.
3. `Candidate evidence actually inspected`: Specific candidate files, line numbers, git diffs, or execution logs inspected.
4. `Result`: Defense outcome ("The implementation demonstrably blocks this counterexample" or defect identified).
5. `Blocking finding or residual limitation`: Structured defect finding or documented residual limitation.

If a dimension cannot be evaluated due to a concrete prerequisite failure, mark it `BLOCKED` with the 3 required prerequisite fields; never omit any dimension.

#### Acceptance Oracle Enforcement & Completeness Canary
`audit_review_gate.py` mechanically checks that no required dimensions are omitted from the review report.
To mechanically enforce this constraint, the oracle includes a built-in regression canary:
- **Completeness Canary (Fixture G):** A candidate review report that discovers Defect A in Dimension 1 and terminates without evaluating independently reviewable Defect B in Dimension 2 must NOT qualify as a complete review attempt. The oracle must reject it with exit code 1 (`review_completeness: FAIL (omitted dimensions)`).

---

### 4.4 Built-in Regression Fixtures in Oracle

The gate script incorporates built-in report-independent regression fixtures executed via `--test-fixtures`:

#### General Fixtures (Fixtures A–F)
| Fixture ID | Description | Target Invariant / Rule Tested | Expected Result |
| :--- | :--- | :--- | :--- |
| **Fixture A** | **Golden Valid Report:** Fully compliant report containing valid verdict header (`PASS`), verified handshake with exact commit SHA, non-trivial `view_file` calls for all manifest entries, all 4 required sections, complete 4-part falsification blocks across all dimensions, valid candidate binding, and zero proscribed phrases. | Baseline validity of all checks. | `PASS` (exit 0) |
| **Fixture B** | **Incomplete Falsification Block:** Report missing `Counterexample Attempted:` or `Execution Trace:` in one evaluated dimension. | Invariant 9: 4-part adversarial falsification structure. | `FAIL` on `falsification_structure` (exit 1) |
| **Fixture C** | **Invalid / Open Verdict:** Report emitting an open, malformed, or ambiguous verdict (e.g. `Verdict: PENDING`, `Verdict: LOOKS_GOOD`, `Verdict: PASS WITH COMMENTS`). | Invariant 15: Total 3-verdict closed algebra (`PASS`, `BLOCKING_FINDINGS`, `BLOCKED`). | `FAIL` on `verdict_in_closed_algebra` (exit 1) |
| **Fixture D** | **Authority Closure Failure / Trivial Range:** Subagent transcript missing a manifest entry, or calling `view_file` with only 2 lines (`StartLine=1, EndLine=2`). | Invariant 7: Tool-history-derived authority closure with non-trivial range verification. | `FAIL` on `authority_closure_tool_audit` (exit 1) |
| **Fixture E** | **Proscribed Absolute Phrase with Self-Attestation:** Report containing `"hoàn toàn"`, `"triệt để"`, `"guarantees"`, `"flawless"`, `"zero risk"`, `"tuyệt đối"`, `"completely closes"`, or `"100% tuân thủ"` outside quotation/definition blocks, accompanied by `zero_proscribed_absolutes: TRUE`. | Invariant 1 & 5: Mechanical absolute scan ignores self-attestation. | `FAIL` on `mechanical_scan_clean` (exit 1) |
| **Fixture F** | **Evidence-Backed Measurement:** Report containing factual measurements with explicit denominators (e.g. `14/14 (100%) consumed`, `5/5 (100%) tests passing`, `0 broken links`). | Invariant 5: Calibrated claim discipline distinguishes ratios from unmeasured absolutes. | `PASS` on `mechanical_scan_clean` (exit 0) |

#### Candidate Binding Fixtures (Cases A–D)
| Case ID | Scenario Description | Expected Outcome |
| :--- | :--- | :--- |
| **Case A (Stale PASS Reuse)** | Candidate `H1` reviewed and PASS emitted at step 10. Candidate modified to `H2`. Main evaluates gate for `H2` without dispatching `H2` or obtaining re-review. | `FAIL` on `review_candidate_binding`: No dispatch found for candidate `H2`; stale report bound to `H1` rejected. |
| **Case B (Fresh Re-Review)** | Candidate `H1` reviewed. Candidate modified to `H2`. Main dispatches `H2` at step 15. Reviewer emits fresh report acknowledging `H2` at step 18. Gate evaluated for `H2`. | `PASS` on `review_candidate_binding`: Authoritative dispatch at step 15 < report at step 18; report explicitly acknowledges `H2`. |
| **Case C (Post-Dispatch Wrong Hash)** | Main dispatches `H2` at step 15. Reviewer emits report at step 18, but report text still identifies candidate as `H1`. Gate evaluated for `H2`. | `FAIL` on `review_candidate_binding`: Report candidate identity mismatch (report has `H1`, expected `H2`). |
| **Case D (Pre-Dispatch Timestamp/Index)** | Transcript contains a report mentioning `H2` at step 12, but authoritative dispatch of `H2` occurred at step 15 (e.g. Out-of-order message or speculative review). | `FAIL` on `review_candidate_binding`: Chronological binding failure (`report_index <= dispatch_index`). |

#### Candidate Scope Closure Fixtures (Scope Cases A–D)
| Case ID | Scenario Description | Target Invariant / Rule Tested | Expected Outcome |
| :--- | :--- | :--- | :--- |
| **Scope Case A (Exact Closure)** | Manifest enumerates all changed candidate files with exact blob hashes. Zero unlisted changed files, zero phantom entries. | Invariant 18: Exact candidate scope closure. | `PASS` on `candidate_scope_closure` (exit 0) |
| **Scope Case B (Unlisted Changed File)** | Repository contains a changed or untracked candidate file omitted from `candidate_manifest.json`. | Invariant 18: Unmanifested file detection. | `FAIL` on `candidate_scope_closure` (exit 1) |
| **Scope Case C (Phantom / Out-of-Scope Member)** | Manifest lists a file that is nonexistent on disk or has no diff relative to baseline `5406b2c`. | Invariant 18: Phantom manifest member detection. | `FAIL` on `candidate_scope_closure` (exit 1) |
| **Scope Case D (Post-Identity Mutation)** | A listed candidate file is modified on disk after candidate identity was computed, mutating its blob hash. | Invariant 18: Invalidation upon out-of-band mutation. | `FAIL` on `candidate_scope_closure` (exit 1) |

#### Review Completeness Canary Fixture (Fixture G)
| Fixture ID | Description | Target Invariant / Rule Tested | Expected Result |
| :--- | :--- | :--- | :--- |
| **Fixture G (Incomplete Review Early Exit)** | Review report finds Defect A in Dimension 1 and terminates immediately without evaluating independently reviewable Defect B in Dimension 2. | Invariant 19: `blocking_finding != automatic_end_of_review`. | `FAIL` on `review_completeness` (exit 1) |

---

### 4.5 Frozen Review Oracle Lifecycle

```text
                  Phase A: Oracle Preparation & Calibration
                  ┌───────────────────────────────────────┐
                  │ 1. Oracle exists in scratch           │
                  │    scratch/audit_review_gate.py       │
                  │                                       │
                  │ 2. Run Fixtures A–G, Binding Cases    │
                  │    A–D & Scope Cases A–D              │
                  │    python scratch/audit_review_gate.py│
                  │           --test-fixtures             │
                  │    Assert: ALL_FIXTURES_PASS          │
                  │                                       │
                  │ 3. Compute Oracle Identity            │
                  │    review_oracle_hash =               │
                  │      git hash-object <oracle_path>    │
                  │                                       │
                  │ 4. Freeze Oracle for Attempt          │
                  │    review_oracle_frozen = true        │
                  └──────────────────┬────────────────────┘
                                     │
                                     ▼
                     Phase B: Governed Review Evaluation
                  ┌───────────────────────────────────────┐
                  │ 1. Verify on-disk oracle hash matches │
                  │    review_oracle_hash (no drift)      │
                  │                                       │
                  │ 2. Execute Frozen Oracle on Report    │
                  │    python scratch/audit_review_gate.py│
                  │      --transcript <path>              │
                  │      --manifest <manifest>            │
                  │      --expected-hash <hash>           │
                  │      --candidate-manifest <c_manifest>│
                  │      --base-commit 5406b2c            │
                  │      --phase <phase>                  │
                  │                                       │
                  │ 3. Display Output in Transcript       │
                  │    Print: Provenance & Verdict block  │
                  │                                       │
                  │ 4. Assert Exit Code                   │
                  │    Exit 0 -> PASS -> Advance State    │
                  │    Exit 1 -> FAIL -> Verdict INVALID  │
                  └───────────────────────────────────────┘
```

#### Handling Oracle Bugs Discovered After Evaluation
If an oracle defect is discovered after a report evaluation (e.g., a regex false positive on valid markdown formatting):
1. **Invalidate Gate Evaluation:** Mark previous evaluation as `INVALIDATED (Oracle Defect)`.
2. **Preserve Audit Trail:** The previous attempt (with `G1` hash and output) remains visible in transcript history; it is never hidden or deleted.
3. **Fix Oracle in Scratch:** Repair the parsing logic in `scratch/audit_review_gate.py`.
4. **Re-Run Regression Fixtures:** Run `python scratch/audit_review_gate.py --test-fixtures` (including a new regression case covering the fixed bug).
5. **Re-Freeze Identity:** Compute `review_oracle_hash_v2 = git hash-object scratch/audit_review_gate.py`. Record new frozen hash.
6. **Re-Evaluate Report:** Execute the newly frozen oracle against the reviewer report as a fresh gate attempt.
7. **Reconciliation Invariant:** Oracle repair cycles do NOT consume author/reviewer reconciliation budget (`plan_reconciliation_count` and `impl_reconciliation_count` remain unchanged) provided candidate and report semantics did not change.

---

### 4.6 Standalone Executable Gate Script Specification (`audit_review_gate.py`)

#### CLI Interface
```bash
python audit_review_gate.py \
  --transcript "<path/to/transcript.jsonl>" \
  --manifest "<path/to/bootstrap-governance/manifest.json>" \
  --phase <PLAN_REVIEW | IMPL_REVIEW> \
  [--expected-hash <candidate_hash>] \
  [--candidate-manifest "<path/to/candidate_manifest.json>"] \
  [--base-commit "5406b2c"] \
  [--verification-manifest "<path/to/verification.md>"] \
  [--test-fixtures]
```

#### The 10 Deterministic Code Checks
1. **`session_handshake_verified`:** Handshake contains exact `bootstrap_commit` SHA.
2. **`authority_closure_tool_audit`:** All manifest entries read via `view_file` before handshake with non-trivial ranges (>= 10 lines or full file).
3. **`review_candidate_binding`:** Authoritative dispatch of expected candidate hash `H` occurred; report emitted post-dispatch (`dispatch_index < report_index`); report explicitly acknowledges `H`.
4. **`candidate_scope_closure`:** `actual_candidate_changed_file_set == manifest_candidate_file_set` independently derived from repository state against baseline `5406b2c` (`git diff --name-only 5406b2c` plus untracked candidate files via `git ls-files --others --exclude-standard`), excluding `candidate_manifest.json` and `scratch/`.
5. **`review_completeness`:** All required review dimensions evaluated without premature termination; `blocking_finding != automatic_end_of_review`; any blocked dimension explicitly provides `blocked_dimension`, `blocking_dependency`, and `why further evaluation is impossible`.
6. **`verdict_in_closed_algebra`:** Post-dispatch report contains valid verdict regex: `PASS | BLOCKING_FINDINGS | BLOCKED`.
7. **`required_sections_present`:** Post-dispatch report contains all required headers.
8. **`falsification_structure`:** All evaluated dimensions have complete 4-part blocks (`Target Invariant`, `Counterexample Attempted`, `Execution Trace`, `Result / Defense`).
9. **`mechanical_scan_clean`:** Raw report text free of unmeasured semantic absolutes outside definition blocks.
10. **`plan_hash_parity` (for `PLAN_REVIEW`):** On-disk candidate plan hash matches expected hash `H`.

---

### 4.7 Observable Gate Execution & Visible Verdict Provenance Contract

Before executing any state transition tool calls or git commits, Main Controller MUST visibly emit the structured Provenance and Verdict Block in the conversation transcript:

```markdown
### Review Gate Provenance & Verdict Evaluation
- **Candidate Reviewed Hash:** `<expected_candidate_hash>`
- **Reviewer Session ID:** `<reviewer_conversation_id>`
- **Authoritative Dispatch Step / Index:** Step `<dispatch_index>`
- **Report Step / Index:** Step `<report_index>`
- **Review Candidate Binding:** PASS (`dispatch_index` < `report_index`, candidate acknowledged)
- **Candidate Scope Closure:** PASS (exact changed-file closure against baseline `5406b2c`)
- **Review Completeness:** PASS (all material dimensions evaluated; zero omitted)
- **Reviewer Verdict:** PASS (or `BLOCKING_FINDINGS` / `BLOCKED`)
- **Mechanical Gate Result:** PASS (exit code 0)
- **Gate Oracle Identity Hash:** `<frozen_review_oracle_hash>`
```

**Strict Invariant:** No state transition tool calls (`manage_subagents`, `run_command` for git commits, `invoke_subagent`) may precede this visible output in the transcript.

---

## 5. Scope Boundaries & Blast Radius

### 5.1 Strict In-Scope
- `AGENTS.md`: Lean SSOT streamlining, remove procedural git/test duplication, clarify Layer 0 structural vs semantic authority, maintain line count < 250 lines.
- `.agents/skills/managed-agent-workflow/`:
  - `scripts/audit_review_gate.py`: **[NEW FILE]** Authoritative standalone executable code gate script implementing 10 deterministic checks (including `review_candidate_binding`, `candidate_scope_closure` against baseline `5406b2c`, and `review_completeness` non-termination) and built-in `--test-fixtures` (Fixtures A–G, Binding Cases A–D, and Scope Cases A–D). Authored by Implementor `I` in Slice 1.
  - `SKILL.md`: Honest Two-Phase Boot protocol, Frozen Review Oracle Lifecycle, Review Candidate Binding, Candidate Scope Closure (`actual_candidate_changed_file_set == manifest_candidate_file_set`), Review Completeness (`blocking_finding != automatic_end_of_review`), mandatory execution of `audit_review_gate.py`, Observable Gate Execution, Visible Verdict Provenance Contract, manifest-derived authority closure, sole Main commit ownership, total 3-verdict model, calibrated claim discipline.
  - `references/controller-state-machine.md`: Ephemeral state schema (adding candidate binding, scope closure, and oracle tracking fields: `candidate_hash`, `candidate_dispatch_index`, `candidate_scope_closure_passed`, `review_completeness_passed`, `review_oracle_path`, `review_oracle_hash`, `review_oracle_regression_status`, `review_oracle_frozen_for_attempt`), mandatory CLI execution of frozen oracle, visible provenance block preceding transitions, two-stage review acceptance, candidate scope closure and review completeness enforcement, honest boot prompt contract with mandatory `view_file` mandate, dynamic closure validation, `COMPLETED` semantics at Owner Review Gate.
  - `references/reconciliation-protocol.md`: Align author response rules with Main commit ownership, report-only repair protocol, oracle bug invalidation protocol without cycle increment, multi-defect aggregation handling (author addresses all aggregated findings per cycle), and mechanical gate failure handling.
- `.agents/skills/code-review-and-quality/`:
  - `SKILL.md`: Truthful Two-Phase Boot protocol, explicit candidate hash acknowledgment in report, Review Completeness invariant (`blocking_finding != automatic_end_of_review`), Prerequisite Dependency Exception schema, Re-Review Inspection Standard for $H_2$, 5-field evidence-backed coverage, manifest-derived authority closure, Calibrated Claim Discipline, 4-part counterexample/falsification method.
  - `references/plan-review-rubric.md`: Require manifest entries in Proof of Authority Consumption, explicit candidate hash cite, remove self-attestation checklist, enforce 4-part falsification blocks across all 6 dimensions.
  - `references/implementation-review-rubric.md`: Require manifest entries in Proof of Authority Consumption, explicit diff/manifest hash cite, candidate scope closure verification against baseline `5406b2c`, enforce Review Completeness across all 5 dimensions without early abort, Prerequisite Dependency Exception schema, remove self-attestation checklist, enforce 5-field evidence-backed coverage across all 5 dimensions.
- `.agents/skills/test-and-verification-strategy/`:
  - `SKILL.md`: Orthogonal Verification Domain Matrix, Layer 0 structural authority vs semantic authority, calibrated claim discipline.
  - `references/verification-layers-and-tooling.md`: Standardize verification evidence path to `docs/workstreams/<id>/verification.md`, Layer 0 mechanical scan check, candidate scope closure verification, provenance freshness.
- `.agents/skills/implementation-planning-and-contract-freeze/`:
  - `SKILL.md`: Domain decoupling, sole Main hash ownership reference, calibrated claim discipline.
  - `references/workstream-plan-template.md`: Decouple rigid checkpoints into contract-derived checkpoints, static candidate header, reference manifest-derived authority closure.
  - `references/slicing-and-dependency-strategies.md`: Generic layer slicing patterns, calibrated claim discipline.
- `.agents/skills/git-checkpoint-workflow/`:
  - `SKILL.md`: Two-phase commit lifecycle, sole Main commit ownership, calibrated claim discipline.
  - `references/checkpoint-lifecycle-and-lineage.md`: Proportional commit templates, gate commits behind observable mechanical gate execution (`audit_review_gate.py` exit 0) and visible provenance block emission, preserve audit commit lineage.
- `docs/workstreams/agent-architecture-redesign/plan.md`: This candidate plan.

### 5.2 Explicit Out-of-Scope
- Zero modifications to Java code (`src/`), Cobblemon Mixins, or datapack JSONs (`data/`).
- Zero modifications to `competitive-pokemon-doubles-team-design` domain skill logic.
- Zero remote Git operations (`git push`, PR creation, remote branch manipulation).

---

## 6. Exact Files to Modify / Create / Delete

### Files to Create
| Target File | Creation Rationale & Scope | Related Invariants |
| :--- | :--- | :--- |
| `.agents/skills/managed-agent-workflow/scripts/audit_review_gate.py` | Standalone executable code gate script implementing 10 deterministic checks (handshake, tool history audit with non-trivial range check, candidate binding, candidate scope closure against baseline `5406b2c`, review completeness, closed verdict algebra, required sections, 4-part falsification structure, proscribed phrase scan, plan hash parity) and built-in `--test-fixtures` (Fixtures A–G, Binding Cases A–D, and Scope Cases A–D). Authored by Implementor `I` in Slice 1. | Invariants 1, 2, 3, 5, 7, 9, 18, 19 |

### Files to Modify
| Target File | Modifying Rationale & Scope | Related Invariants |
| :--- | :--- | :--- |
| `AGENTS.md` | Lean SSOT streamlining; strip procedural git commands and detailed test matrices; define Layer 0 structural vs semantic authority; keep lines < 250 (target ~210–225). | Lean SSOT, Layer 0 Authority |
| `.agents/skills/managed-agent-workflow/SKILL.md` | Codify honest Two-Phase Boot protocol; codify Frozen Review Oracle Lifecycle; codify Review Candidate Binding; codify Candidate Scope Closure and Review Completeness; mandate CLI execution of `audit_review_gate.py`; codify Observable Gate Execution & Visible Provenance Contract; confirm sole Main hash/commit ownership. | Invariants 1, 2, 3, 4, 6, 7, 8, 11, 18, 19 |
| `.agents/skills/managed-agent-workflow/references/controller-state-machine.md` | Ephemeral state schema with candidate binding, scope closure, review completeness & oracle metadata fields; mandate executing frozen oracle via shell command; codify visible provenance block emission strictly preceding state transitions and commits; honest boot prompt contract with mandatory `view_file` mandate; dynamic manifest closure validation; `COMPLETED` semantics at Owner Review Gate. | Invariants 1, 2, 3, 4, 6, 7, 8, 10, 11, 15, 17, 18, 19 |
| `.agents/skills/managed-agent-workflow/references/reconciliation-protocol.md` | Clarify author artifact correction vs Main commit ownership; add report-only defect repair protocol without cycle increment; codify oracle bug invalidation protocol without cycle increment; align multi-defect resolution with aggregated findings; align with `audit_review_gate.py` failure handling. | Invariants 1, 2, 11, 19 |
| `.agents/skills/code-review-and-quality/SKILL.md` | Codify truthful reviewer Two-Phase Boot protocol (mandatory `view_file` on all manifest baseline entries, transcript audit warning, emit `AUTHORITY_LOADED`, await Partition 2); mandate explicit candidate hash acknowledgement in report; codify Review Completeness (`blocking_finding != automatic_end_of_review`); codify Prerequisite Dependency Exception schema; codify Re-Review Inspection Standard for $H_2$; codify manifest-derived authority closure; eliminate self-attestation checklist; codify calibrated claim discipline; mandate 4-part counterexample/falsification method. | Invariants 3, 5, 6, 7, 9, 18, 19 |
| `.agents/skills/code-review-and-quality/references/plan-review-rubric.md` | Require manifest entries in Proof of Authority Consumption; require explicit candidate plan hash cite in report header; remove self-attestation checklist; require 4-part falsification blocks across all 6 dimensions. | Invariants 3, 5, 7, 9 |
| `.agents/skills/code-review-and-quality/references/implementation-review-rubric.md` | Require manifest entries in Proof of Authority Consumption; require explicit diff/manifest hash cite in report header; verify Candidate Scope Closure against baseline `5406b2c`; enforce Review Completeness across all 5 dimensions without early abort; mandate Prerequisite Dependency Exception schema; remove self-attestation checklist; require 5-field evidence-backed coverage across all 5 dimensions. | Invariants 3, 5, 7, 9, 18, 19 |
| `.agents/skills/test-and-verification-strategy/SKILL.md` | Remove ascending ladder diagram (`▲`); replace with Orthogonal Verification Domain Matrix; clarify Layer 0 structural syntax vs semantic authority; calibrated claim discipline. | Invariant 12 |
| `.agents/skills/test-and-verification-strategy/references/verification-layers-and-tooling.md` | Standardize verification evidence path to `docs/workstreams/<id>/verification.md`; add Layer 0 mechanical scan check; candidate scope closure check; provenance freshness. | Invariants 1, 12, 18 |
| `.agents/skills/implementation-planning-and-contract-freeze/SKILL.md` | Domain decoupling; sole Main hash ownership reference; calibrated claim discipline. | Invariants 5, 11 |
| `.agents/skills/implementation-planning-and-contract-freeze/references/workstream-plan-template.md` | Decouple rigid checkpoints into contract-derived checkpoints; static candidate header; reference manifest-derived authority closure. | Invariants 7, 14, 16 |
| `.agents/skills/implementation-planning-and-contract-freeze/references/slicing-and-dependency-strategies.md` | Generic layer slicing patterns; calibrated claim discipline. | Invariant 14 |
| `.agents/skills/git-checkpoint-workflow/SKILL.md` | Two-phase commit lifecycle; sole Main commit ownership; calibrated claim discipline. | Invariant 11 |
| `.agents/skills/git-checkpoint-workflow/references/checkpoint-lifecycle-and-lineage.md` | Proportional commit templates; gate commits behind observable mechanical gate execution (`audit_review_gate.py` exit 0) and visible provenance block emission; calibrated claim discipline. | Invariants 1, 8, 11 |
| `docs/workstreams/agent-architecture-redesign/plan.md` | This candidate implementation plan. | All Invariants |

### Files to Delete
- **Zero files to delete**.

---

## 7. Slicing & Implementation Strategy

Implementation proceeds across 3 surgical slices:

```text
Slice 1: Root Policy, State Machine, Gate Script & Mechanical Orchestration
- Create .agents/skills/managed-agent-workflow/scripts/audit_review_gate.py (with --test-fixtures)
- AGENTS.md (Lean SSOT streamlining, strip procedural commands, Layer 0 structural definition)
- managed-agent-workflow (SKILL.md, controller-state-machine.md, reconciliation-protocol.md)
  [Integrates:
   - Review Candidate Binding (review_candidate_binding & chronological selection)
   - Candidate Scope Closure (actual_candidate_changed_file_set == manifest_candidate_file_set vs baseline 5406b2c)
   - Review Completeness check (blocking_finding != automatic_end_of_review, zero omitted dimensions)
   - Frozen Review Oracle Lifecycle (Phase A calibration against Fixtures A–G, Binding Cases A–D & Scope Cases A–D)
   - Runtime Scratch vs Repository Candidate Oracle Ownership
   - Standalone audit_review_gate.py execution (10 deterministic checks)
   - Observable Gate Execution & Visible Provenance Contract
   - Tool-history-derived authority closure (transcript.jsonl view_file audit + non-trivial range check)
   - Honest boot protocol & removal of physical isolation overclaims
   - Dynamic manifest-derived closure & removal of magic constants]
      │
Slice 2: Review Rubrics, Falsification Architecture, Manifest Closure & Calibrated Claim Discipline
- code-review-and-quality (SKILL.md, plan-review-rubric.md, implementation-review-rubric.md)
- implementation-planning-and-contract-freeze (SKILL.md, workstream-plan-template.md, slicing-and-dependency-strategies.md)
  [Integrates:
   - Explicit candidate hash acknowledgment in reviewer reports
   - Review Completeness mandate across all material dimensions (blocking_finding != automatic_end_of_review)
   - Prerequisite Dependency Exception schema (blocked_dimension, blocking_dependency, why further evaluation is impossible)
   - Re-Review Inspection Standard for candidate H2 (H1->H2 delta, affected invariants, remaining dimensions)
   - 5-field evidence-backed coverage per evaluated dimension
   - Candidate Scope Closure verification in implementation review rubric
   - Mandatory view_file calls for all manifest baseline entries in Two-Phase Boot
   - Transcript audit warning in boot prompt
   - Elimination of self-attestation checklists & calibrated claims
   - 4-part adversarial falsification rubrics matching audit_review_gate.py rules]
      │
Slice 3: Verification Strategy, Orthogonal Sets, Git Lineage & Layer 0 Automated Gates
- test-and-verification-strategy (SKILL.md, verification-layers-and-tooling.md)
- git-checkpoint-workflow (SKILL.md, checkpoint-lifecycle-and-lineage.md)
  [Integrates:
   - Candidate scope closure validation against baseline 5406b2c in verification tooling
   - Commit gating behind visible gate script execution and visible provenance block
   - Layer 0 mechanical scan check
   - Orthogonal Verification Domain Matrix and preserved audit lineage]
```

---

## 8. Proportional Verification Plan (Layer 0 Structural Authority)

In accordance with the Orthogonal Verification Principle, this workstream modifies **only** Markdown governance documentation, skill definitions, and a Python verification utility (`audit_review_gate.py`). Zero Java classes, zero Mixin bytecode, and zero datapack JSONs are modified.

### Automated Verification Checks (Layer 0)
1. **Gate Script Built-in Fixture, Scope Closure & Binding Verification:**
   Execute:
   ```bash
   python .agents/skills/managed-agent-workflow/scripts/audit_review_gate.py --test-fixtures
   ```
   Verifies that all 7 regression fixtures (Fixtures A–G), all 4 Candidate Binding cases (Cases A–D), and all 4 Candidate Scope Closure cases (Scope Cases A–D) pass with exit code 0.
2. **Skill Route Integrity Check:** Verify all skill routes referenced in `AGENTS.md` Section 5 resolve to valid files.
3. **Internal Reference Link Integrity Check:** Verify all relative markdown links in `references/*.md` resolve to existing files.
4. **YAML Frontmatter Schema Validation:** Verify all `SKILL.md` files contain valid YAML frontmatter (`name` matching directory, non-empty `description`).
5. **File Line Count Bounds Check:**
   - Verify `AGENTS.md` is strictly < 250 lines (target ~210–225 lines).
   - Verify all other `SKILL.md` and reference files remain < 500 lines.
6. **Git Diff Confinement Audit:** `git diff --stat` confirms modifications are strictly confined to `AGENTS.md`, `.agents/skills/`, and `docs/workstreams/agent-architecture-redesign/plan.md`. Zero drift in `src/` or `data/`.
7. **Proscribed Absolute Phrase Scan:**
   Execute a deterministic text scan across candidate files verifying zero occurrences of narrow unsupported absolutes outside of explicit definition/proscription blocks:
   - Proscribed phrases: `"hoàn toàn"`, `"triệt để"`, `"guarantees"`, `"flawless"`, `"không có rủi ro"`, `"zero risk"`, `"tuyệt đối"`, `"completely closes"`, `"100% tuân thủ"`.
8. **Manifest Hash & Completeness Verification:**
   Verify that all entries in `scratch/bootstrap-governance/manifest.json` exist and match their recorded blob hashes.
9. **Candidate Scope Closure Verification:**
   Verify that `candidate_manifest.json` exactly matches the actual changed candidate file set derived independently from `git diff --name-only 5406b2c` plus untracked candidate files via `git ls-files --others --exclude-standard`, excluding `candidate_manifest.json` and `scratch/`.

### Semantic Authority Distinction
- Automated Layer 0 structural checks and `audit_review_gate.py` verify syntactic and mechanical compliance only. They do **not** prove authority correctness, routing correctness, or domain invariant preservation.
- Semantic correctness is established through independent adversarial review by Plan Reviewer `R` and Implementation Reviewer `IR` evaluating the candidate architecture against the materialized baseline at `scratch/bootstrap-governance/`.

### Inapplicable Higher Layers (Explicitly Skipped per Orthogonal Selection)
- **Layer 1 (`validate_repo.py`):** Skipped (0 datapack JSONs modified).
- **Layer 2 (`./gradlew test`):** Skipped (0 Java classes modified).
- **Layer 3 (`test_rct_runtime_contract.py`):** Skipped (0 Mixins or bytecode modified).
- **Layer 4 (`./gradlew runServer`):** Skipped (no server runtime bootstrap risk).
- **Layer 5 (Canary Host):** Skipped (no live gameplay changes).

---

## 9. Task-Specific Implementation Checkpoints & Review Lifecycle

Implementation checkpoints derived from affected governance contracts:

- **Checkpoint 1: Plan Freeze Checkpoint (Current Gate)**
  - Planner `P` submits candidate plan with semantic handoff `{ candidate_path, ready: true }`.
  - Main Controller computes `candidate_plan_hash`.
  - **Oracle Preparation (Phase A):** Main prepares `scratch/audit_review_gate.py` (at `C:\Users\khang\.gemini\antigravity\brain\bc36d4d8-120c-4df2-8b11-2173327ebb5c\scratch\audit_review_gate.py`), runs `python scratch/audit_review_gate.py --test-fixtures`, verifies all Fixtures A–G, Cases A–D, and Scope Cases A–D pass, records `review_oracle_hash = git hash-object scratch/audit_review_gate.py`, and sets `review_oracle_frozen_for_attempt = true`.
  - **Honest Reviewer Boot (Phase 1):** Main Controller invokes Plan Reviewer `R` with Partition 1 (materialized manifest baseline in scratch) and Partition 3 (Plan Review Rubric). Partition 2 is WITHHELD. Reviewer is explicitly commanded to call `view_file` on every entry in `bootstrap_governance_manifest.entries` with non-trivial ranges and warned that tool call history in `transcript.jsonl` is mechanically audited.
  - Plan Reviewer `R` executes `view_file` on all manifest baseline entries, verifies `manifest.json`, and emits `AUTHORITY_LOADED`.
  - Main Controller audits `transcript.jsonl` to confirm all entries were read via `view_file`.
  - **Review Active (Phase 2):** Main dispatches Partition 2 (candidate plan path and `candidate_plan_hash`) to `R` via `send_message`. Main records `candidate_dispatch_index`.
  - Plan Reviewer `R` evaluates candidate plan using the 4-part counterexample/falsification method across all required dimensions without early termination and emits report explicitly acknowledging `candidate_plan_hash`.
  - **Governed Review Evaluation (Phase B):**
    1. Main asserts oracle on disk matches `frozen_review_oracle_hash`.
    2. Main executes `python scratch/audit_review_gate.py --transcript <R_transcript> --manifest <manifest> --expected-hash <candidate_plan_hash> --phase PLAN_REVIEW`.
    3. Main visibly emits the Provenance and Verdict Block in the transcript:
       - Candidate Reviewed Hash
       - Reviewer Session ID
       - Authoritative Dispatch Step / Index
       - Report Step / Index
       - Review Candidate Binding result
       - Candidate Scope Closure result
       - Review Completeness result
       - Reviewer Verdict
       - Mechanical Gate Result
       - Gate Oracle Identity Hash
    4. ONLY AFTER Step 3 is visible in transcript:
       - If script exit != 0 -> verdict `INVALID`, state holds, R repairs report defects in session.
       - If script exit == 0 and `R verdict: PASS`: Main recomputes post-review hash, asserts equality, records `frozen_plan_hash`, creates the Plan Freeze Checkpoint commit, and terminates reviewer and planner.
- **Checkpoint 2: Surgical Implementation of Slices 1–3**
  - Implementor `I` creates `.agents/skills/managed-agent-workflow/scripts/audit_review_gate.py` implementing 10 deterministic checks with built-in `--test-fixtures` (Fixtures A–G, Cases A–D, and Scope Cases A–D) and applies changes for Slices 1, 2, and 3.
- **Checkpoint 3: Proportional Verification & Manifest Assembly**
  - Implementor executes Layer 0 structural checks and `audit_review_gate.py --test-fixtures`, recording outputs in `docs/workstreams/agent-architecture-redesign/verification.md`.
  - Implementor generates `candidate_manifest.json` ensuring exact changed-file scope closure against baseline `5406b2c`.
  - Main Controller assembles Verification Evidence Manifest.
- **Checkpoint 4: Implementation Review Gate & Verified Implementation Checkpoint**
  - **Oracle Preparation (Phase A):** Main prepares/freezes `scratch/audit_review_gate.py` and verifies Fixtures A–G, Cases A–D, and Scope Cases A–D pass.
  - **Honest Reviewer Boot (Phase 1):** Main Controller invokes Implementation Reviewer `IR` with Partition 1 (materialized manifest baseline) and Partition 3 (Implementation Review Rubric). Partition 2 is WITHHELD. Reviewer is commanded to call `view_file` on every manifest entry with non-trivial ranges and warned that tool history is mechanically audited.
  - Reviewer `IR` executes `view_file` on all manifest entries, verifies `manifest.json`, and emits `AUTHORITY_LOADED`.
  - Main Controller audits `transcript.jsonl` to confirm all entries were read via `view_file`.
  - **Review Active (Phase 2):** Main dispatches Partition 2 (working-tree diff and Verification Evidence Manifest) to `IR` via `send_message`. Main records `candidate_dispatch_index`.
  - Reviewer `IR` evaluates implementation using the 5-field evidence-backed coverage across all 5 dimensions without early abort (`blocking_finding != automatic_end_of_review`) and emits report explicitly acknowledging candidate identity. If any dimension is genuinely blocked by prerequisite failure, record `blocked_dimension`, `blocking_dependency`, and `why further evaluation is impossible`.
  - **Governed Review Evaluation (Phase B):**
    1. Main asserts oracle on disk matches `frozen_review_oracle_hash`.
    2. Main executes `python scratch/audit_review_gate.py --transcript <IR_transcript> --manifest <manifest> --candidate-manifest <path> --base-commit 5406b2c --verification-manifest <path> --phase IMPL_REVIEW`.
    3. Main visibly emits the Provenance and Verdict Block in the transcript (including Candidate Scope Closure and Review Completeness results).
    4. ONLY AFTER Step 3 is visible in transcript:
       - If script exit != 0 -> verdict `INVALID`, state holds, IR repairs report defects.
       - If script exit == 0 and `IR verdict: PASS`: Main creates Verified Implementation Checkpoint commit, terminates implementor and reviewer, delivers review checkpoint report to Owner, and parks in `COMPLETED` at Owner Review Gate.

---

## 10. Residual Risks & Mitigation Safeguards

| Risk Category | Assessed Risk Level | Mitigation Safeguard |
| :--- | :--- | :--- |
| **Unmanifested Candidate File Leakage** | Mitigated | `candidate_scope_closure` derives actual changed files independently from git diff vs `5406b2c` + untracked files; extra unlisted files trigger immediate gate rejection. |
| **Premature Review Termination / Concealed Defects** | Mitigated | `review_completeness` enforces `blocking_finding != automatic_end_of_review`; oracle verifies all 5 dimensions evaluated; Completeness Canary Fixture G rejects early aborts. |
| **Stale Report Reuse / False Binding** | Mitigated | `review_candidate_binding` predicate enforces that authoritative dispatch occurred before report and that report acknowledges exact candidate hash. |
| **Oracle Drift / Ad-Hoc Mutation** | Mitigated | Frozen Review Oracle Lifecycle: oracle calibrated against Fixtures A–G, Cases A–D, and Scope Cases A–D, frozen (`review_oracle_hash`) BEFORE evaluating governed reports. |
| **Main Controller Role Bleed** | Mitigated | Strict ownership separation: Main runs Runtime Scratch Oracle in `scratch/`; Implementor `I` authors Repository Candidate Oracle. |
| **Main Controller Self-Attestation** | Mitigated | Standalone Python script `audit_review_gate.py` mechanically executes all 10 checks; Main cannot declare booleans from intuition. |
| **Fabricated Authority Consumption** | Mitigated | `audit_review_gate.py` mechanically audits `transcript.jsonl` for actual `view_file` calls prior to handshake; report markdown claims rejected as proof. |
| **Trivial Range Read Loophole** | Mitigated | `audit_review_gate.py` verifies non-trivial read ranges (>= 10 lines or full file) for all manifest entries. |
| **Silent or Post-Hoc Gate Bypass** | Mitigated | Observable gate execution: Main must visibly print script execution stdout and exit code to transcript strictly BEFORE any transition tool calls or git commits. |
| **Hidden or Reconstructed Verdicts** | Mitigated | Visible provenance block: candidate hash, session ID, dispatch index, report index, binding boolean, scope closure, review completeness, verdict, gate result, oracle hash emitted before transition/commit tools. |
| **Overclaimed Capability Isolation** | Mitigated | Honest protocol model: sequential information release and auditable boot handshake replace false claims of physical isolation. |
| **Hardcoded Instance Constants** | Mitigated | Dynamic manifest derivation: expected authority closure resolved directly from `bootstrap_governance_manifest.entries`. |
| **Out-of-Order Candidate Evaluation** | Mitigated | Partition 2 candidate anchors withheld until after `AUTHORITY_LOADED` is emitted and validated by Main Controller; transcripts auditable. |
| **Reviewer Overclaim & Self-Refutation** | Mitigated | Proscribed absolute phrases mechanically scanned by script; semantic claims calibrated to evidence; reports with forbidden absolutes fail gate. |
| **Passive Reviewer Approvals** | Mitigated | Rubrics mandate 4-part counterexample and falsification structure for all evaluation dimensions; verified by script parser. |
| **Reviewer Privilege Bleed** | Controlled | Reviewers configured with `enable_write_tools: false` and zero terminal commands; Main Controller owns all hash computations and git commits. |
| **State Machine Deadlock on Blocker** | Controlled | Total 3-verdict algebra with explicit Prerequisite Repair Protocol and in-session report defect repair. |
| **Premature Audit Immutability** | Mitigated | Two-phase commit model distinguishes provisional local coordination checkpoints from promoted immutable audit milestones. |
| **SSOT Drift & Documentation Bloat** | Mitigated | `AGENTS.md` line budget strictly enforced (< 250 lines); procedural git and test mechanics delegated to specialized skills. |

---

## 11. Candidate Plan Handoff & Review Readiness

Planner `P` has completed substantive discovery, applied the Subtraction First discipline, and authored this candidate implementation plan addressing the Owner's direct operational corrections:
- Mechanical Review Candidate Binding (`review_candidate_binding`) and chronological report selection;
- Candidate Scope Closure (`candidate_scope_closure`: `actual_candidate_changed_file_set == manifest_candidate_file_set` independently derived from git diff vs `5406b2c` + untracked files);
- Review Completeness & Multi-Finding Aggregation (`review_completeness`: `blocking_finding != automatic_end_of_review`, prerequisite blockage schema, H2 re-review standard);
- Built-in Candidate Binding Fixtures (Cases A–D), Scope Closure Fixtures (Scope Cases A–D), Completeness Canary (Fixture G), and General Fixtures (Fixtures A–F);
- Frozen Review Oracle Lifecycle & Report-Independent Calibration;
- Runtime Scratch Oracle (`scratch/audit_review_gate.py`) vs Repository Candidate Oracle Ownership;
- Standalone Executable Code Gate (`audit_review_gate.py`) executing 10 deterministic checks;
- Upgraded Tool-History Authority Closure Audit with non-trivial range verification;
- Observable Gate Execution Preceding Transition;
- Visible Provenance & Verdict Block Preceding Transition in Transcript.

- **Canonical Plan Path:** `docs/workstreams/agent-architecture-redesign/plan.md`
- **Handoff State:** Semantic candidate handoff `{ candidate_path: "docs/workstreams/agent-architecture-redesign/plan.md", ready: true }`.
- **Status:** Ready for Main Controller to compute `candidate_plan_hash`, execute Phase A Oracle Preparation & Freeze in scratch, and initiate the Honest Two-Phase Reviewer Boot Handshake with Plan Reviewer `R` citing the materialized baseline at `scratch/bootstrap-governance/`.

