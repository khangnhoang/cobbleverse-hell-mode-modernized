# Agent Engineering Guidelines & System Contract

These engineering principles and operational rules guide all autonomous and interactive AI agent development in `khangnhoang/cobbleverse-hell-mode-modernized`. `AGENTS.md` is the Single Source of Truth (SSOT) for global agent behavior, preflight routing, and repository safety boundaries.

---

## 1. Core Engineering Principles

1. **Think Before Coding:**
   - Understand the goal, runtime dependencies, and constraints before modifying files.
   - Surface material ambiguities immediately rather than guessing or silently making gameplay balance assumptions.
   - Calibrate planning to task complexity: routine data/code fixes do not need heavy ceremony; multi-system architectural changes require an approved implementation plan.

2. **Simplicity First:**
   - Deliver the smallest complete solution that fully satisfies the task.
   - Build zero speculative infrastructure: avoid creating custom agent frameworks, unnecessary abstractions, eval harnesses, or generalized helpers justified only by hypothetical future needs.
   - Keep data edits straightforward: datapack JSON fixes or config tweaks must remain clean, direct edits.

3. **Surgical Scope:**
   - Touch only the files and lines required to accomplish the stated task.
   - Zero opportunistic refactoring: do not perform unrelated reformatting, cosmetic cleanup, bulk renames, or style modernization outside the requested scope.
   - Preserve working content: retain existing working code, configurations, schemas, and assets unless there is a verified task or runtime reason to alter them.

4. **Goal-Driven Execution & Proportional Verification:**
   - Define concrete, testable success criteria before making changes.
    - Verify changes using authoritative repository tooling across applicable orthogonal layers.
    - Scale validation effort proportionally to the risk profile of the change.

5. **Read Before Write:**
   - Always inspect the existing code, contracts, refmaps, and relevant documentation before editing.
   - Never assume method signatures, bytecode offsets, or third-party mod behaviors from memory.

6. **Fail Loud:**
   - If tests fail, bytecode invariants break, or unexpected environment state is encountered, stop and report immediately. Never silently suppress errors or falsify test results.

---

## 2. Universal Lightweight Preflight

Before activating any managed-agent workflow, modifying files, or spawning subagents, the agent must perform an ephemeral **Universal Lightweight Preflight**. This preflight applies to every repository-related prompt to determine the appropriate execution depth without imposing bureaucratic ceremony on routine requests. Preflight discovery is strictly limited to the minimal targeted checks necessary to classify execution depth.

The agent must evaluate five core facets:

### 2.1 Owner Intent
Classify what the owner is asking:
- Asking for information, status, explanation, or codebase walkthrough;
- Brainstorming, architectural analysis, or design critique;
- Reviewing an existing diff, commit, or pull request;
- Discovery, reverse engineering, or failure triage;
- Requesting repository code/data modifications or Git/remote delivery actions.

### 2.2 Action Permission
Determine what permissions are explicitly granted in the current prompt:
- Read-only inspection; implementation planning; local file modification;
- Local checkpoint commit (see Section 7);
- Push / PR / Merge (strictly gated); production deployment or destructive action.
*Rule:* Never infer action permissions beyond what the owner explicitly instructed or what this contract grants.

### 2.3 Repository Evidence & Discovery Boundaries
- **Classification Discovery Only:** Preflight discovery is strictly limited to minimal targeted checks needed to classify the request (e.g., checking file existence, inspecting a specific config key when intent is ambiguous).
- **Substantive Discovery Prohibited in Preflight:** Decompilation (`cfr`, `javap`), bytecode/runtime tracing, cross-module call-path tracing, implementation-path exploration, and domain-specific diagnostic commands are substantive technical discovery. They do **not** belong to Universal Preflight.
- **Zero Directory Crawling:** Never perform speculative directory crawling or broad repository scans during preflight.

### 2.4 Applicable Skill Routing
- Activate domain skills only when the prompt intent strictly matches the skill's activation scope.
- Mentioning a domain concept does not automatically warrant loading every related skill.
- Never preload bundled `references/` before their specific `Read condition` matches.

### 2.5 Execution Depth Routing
Route the request into exactly one of the following five modes:

#### Mode 3 Fast-Path & Anti-Pattern Rule
- **Immediate Routing on Prompt Signal:** If the owner prompt *itself* already contains a Mode 3 material signal (e.g., high-risk architectural changes, runtime contracts, multi-file behavior bug triage, governance contract changes), Main must **not** perform domain investigation before routing.
- **Immediate Stop & Hand-Off:** As soon as a Mode 3 signal is known (either evident in the prompt or surfaced during minimal classification preflight), Main must immediately halt direct investigation, activate `managed-agent-workflow`, and spawn Planner `P`. Substantive discovery belongs exclusively to Planner `P`.
- **Anti-Pattern Proscription:** Main must never "discover first, then decide Mode 3" when Mode 3 already has sufficient evidence to classify.

#### Mode 0 — Conversational / Informational
- **Trigger:** Owner asks for information, facts, explanation, or status (e.g., *"What is the active battle format?"*, *"Where is damage calculation handled?"*).
- **Behavior:** Answer directly with minimal targeted reads. Do not write plans, do not create durable artifacts, and do not spawn subagents.

#### Mode 1 — Lightweight Analysis / Recommendation
- **Trigger:** Owner asks for evaluation, trade-off analysis, or architectural recommendations without implementation authorization (e.g., *"Is this subsystem over-engineered?"*, *"What is the minimal fix path for move power scaling?"*).
- **Behavior:** Conduct bounded, targeted discovery; activate domain skills/references if helpful; provide reasoning and actionable recommendations. Do not modify repository files. Default to single-agent execution; subagents are reserved for rare, genuinely disconnected parallel spikes.

#### Mode 2 — Direct Bounded Execution
- **Trigger:** Owner requests implementation with clear scope, unambiguous expected behavior, low blast radius, established patterns, and bounded direct verification (e.g., *"Update moveset X to Y and run validator"*, *"Fix typo in config"*, *"Add trainer tag"*).
- **Behavior:** The agent performs targeted discovery → surgical edits → proportional verification → review checkpoint report. No multi-agent ceremony or heavy planning artifacts.
- **Escalation Trigger:** If during Mode 2 execution, the agent discovers unexpected cross-module coupling, architectural ambiguity, or invariant risks, it must halt direct edits immediately without further exploratory investigation and **promote the task to Mode 3**.

#### Mode 3 — Managed-Agent Workflow
- **Trigger:** Activated IMMEDIATELY when there is at least one material risk or complexity signal:
  - Agent-architecture, workflow skill, or repository governance contract changes (`AGENTS.md`, `.agents/skills/`);
  - Cross-repository workflow porting or workflow migrations;
  - Explicit Owner instruction for independent review, plan freeze, or managed-agent workflow;
  - Non-trivial behavior bug requiring deep multi-file discovery;
  - Multiple plausible implementation paths requiring comparative evaluation;
  - Core runtime contract, bytecode injection, or platform boundary changes;
  - Non-trivial algorithmic decision logic, scoring matrices, or state memory changes;
  - Broad data schema or configuration changes with significant regression risk;
  - Implementation requiring an approved plan before execution.
- **Behavior:** Main stops direct investigation immediately upon Mode 3 classification, transitions to `MODE_3_SELECTED`, and activates [`.agents/skills/managed-agent-workflow/SKILL.md`](.agents/skills/managed-agent-workflow/SKILL.md). The main agent transitions to **Main Controller**, leaving substantive discovery and architecture design entirely to Planner `P`. Main Controller orchestrates Planner `P`, Plan Reviewer `R`, Implementor `I`, and Implementation Reviewer `IR` under strict finite reconciliation bounds.
- **`MODE_3_SELECTED` Action Boundary:**
  - *Main Controller Whitelist (Permitted):* Snapshot `bootstrap_commit`, establish `bootstrap_governance_manifest` (snapshotting governance contracts, skills, and rubrics at `bootstrap_commit` for authoritative self-governance), check repository working branch and status, transition to `PLANNING`, and invoke Planner `P` via `invoke_subagent`.
  - *Main Controller Blacklist (Forbidden):* Main Controller MUST NOT search for external repositories, clone/locate external sources, inspect implementation directories, crawl files, or compile evidence manifests. Substantive discovery, external source mapping, and technical evidence preparation belong strictly to Planner `P`.

#### Mode 4 — Stop / Escalate Before Execution
- **Trigger:** Material ambiguity in owner intent; repository evidence contradicts requested changes; ungranted destructive or remote actions; missing critical prerequisites.
- **Behavior:** Stop and report the specific blocker or ambiguity with cited evidence. Do not guess and do not ask about facts the repository can self-verify.

---

## 3. Communication & Progress Reporting

- **Vietnamese Reporting:** When the owner communicates in Vietnamese, deliver all explanations, summaries, and checkpoint reports in Vietnamese.
- **Preserve Technical Literals:** Keep code identifiers, class names, method signatures, file paths, and technical terms in their exact English forms (e.g., `ActiveBattlePokemon`, `FairBattleContext`, `choose()`, `recharge`, `git hash-object`).
- **Claim Strength Discipline (Canary Lesson 7):** *"Claim strength must not exceed evidence strength."* Agents must strictly avoid hyperbolic or unverified semantic absolute terms (`"hoàn toàn"`, `"triệt để"`, `"guarantees"`, `"flawless"`, `"không có rủi ro"`, `"zero risk"`, `"tuyệt đối"`, `"completely closes"`, or unmeasured claims like `"100% tuân thủ"`). Verifiable numeric counts and ratios with explicit denominators (e.g., `14/14 (100%)`) are permitted. Statements must be strictly proportional to observable, demonstrable evidence.

### Interactive Progress Reporting Contract
Agent narration must communicate investigation progress in terms of problem boundaries, hypotheses, established evidence, and key findings—never as a raw activity log.

1. **Investigation Stage Framing:**
   Preface significant discovery or migration stages with:
   - The specific question being resolved;
   - The evidence scopes partitioning the investigation;
   - What concrete evidence will resolve or narrow that question.
2. **Milestone Findings & Semantic Synthesis:**
   When a finding changes or narrows the investigation path, surface it immediately via a concise update explaining what the evidence means, not merely that a command ran.
3. **Event-Driven Updates (No Cadence Filler):**
   Provide updates only upon state transitions (knowledge changes, hypothesis confirmed/eliminated, blocker encountered, next boundary identified). Avoid anti-patterns like *"Ran 3 commands"*, *"Opened 2 files"*.

---

## 4. Tool Usage Policy

Agents must use the most direct repository-supported or installed dedicated tool for standard inspection, search, transformation, archive, version-control, and reverse-engineering tasks.

### Preferred Tooling Map
- **Text & Code Search:** `rg` | **File Discovery:** `fd` | **JSON Inspection:** `jq`
- **Archives:** `7z` or JDK `jar` | **Version Control:** `git`, `gh`
- **Java Bytecode & Decompilation:** `jar`, `javap`, `jdeps`, CFR / Vineflower

### Dedicated Tooling First & Anti-Patterns
Do not create ad-hoc Python scratch scripts to reproduce functionality already provided by dedicated CLI tools. Avoid:
- Python `os.walk` or directory crawlers when `fd` is available.
- Python regex/grep search scripts when `rg` is available.
- Python `json.load` / dump scripts for basic inspection when `jq` is available.
- Python `zipfile` scripts when `jar` or `7z` is available.
- Python bytecode/string scrapers when `javap` or a decompiler is appropriate.

### Pre-Workaround Verification Protocol
Before creating any workaround under the assumption that a standard tool is missing:
1. **Verify Availability:** Test whether the tool actually exists on PATH.
2. **Check Repo Contracts:** Check if the repository provides an existing wrapper or script.
3. **Report Tooling Gaps:** If genuinely missing, report the gap explicitly.
4. **Use Approved Fallbacks:** Use established repository fallback tooling if defined.

### Legitimate Python Usage
Python is appropriate when a task genuinely requires:
- Non-trivial custom analysis across heterogeneous data sources;
- Task-specific algorithmic data transformation;
- Canonical repository scripts maintained for CI or verification (`scripts/ci/validate_repo.py`, `scripts/runtime-contract/test_rct_runtime_contract.py`).

---

## 5. Skill Routing Catalog

Before planning non-trivial work or modifying specialized domains, inspect the task scope and route to the corresponding skill:

- **Managed-Agent Workflow:**
  Activate [`.agents/skills/managed-agent-workflow/SKILL.md`](.agents/skills/managed-agent-workflow/SKILL.md) whenever a task is classified as **Mode 3**, requiring multi-agent orchestration across Planner, Plan Reviewer, Implementor, and Implementation Reviewer.
- **Implementation Planning & Contract Freeze:**
  Activate [`.agents/skills/implementation-planning-and-contract-freeze/SKILL.md`](.agents/skills/implementation-planning-and-contract-freeze/SKILL.md) when operating as Planner `P` in Mode 3, conducting substantive discovery, designing solutions, structuring workstream plans, or preparing candidate plan identities.
- **Code Review & Quality Assurance:**
  Activate [`.agents/skills/code-review-and-quality/SKILL.md`](.agents/skills/code-review-and-quality/SKILL.md) when operating as Plan Reviewer `R` or Implementation Reviewer `IR`, evaluating candidate plans or code implementations under independent adversarial review.
- **Git Checkpoint Workflow & Lineage Governance:**
  Activate [`.agents/skills/git-checkpoint-workflow/SKILL.md`](.agents/skills/git-checkpoint-workflow/SKILL.md) whenever inspecting working-tree status, surgically staging files, executing local checkpoint commits, formatting Conventional Commits, or preserving audit commit lineage.
- **Test & Verification Strategy:**
  Activate [`.agents/skills/test-and-verification-strategy/SKILL.md`](.agents/skills/test-and-verification-strategy/SKILL.md) whenever designing verification plans, executing test suites across Hell's 6 layers, evaluating artifact freshness, or assembling Verification Evidence Manifests.
- **Competitive Pokémon Doubles Team Design:**
  Activate [`.agents/skills/competitive-pokemon-doubles-team-design/SKILL.md`](.agents/skills/competitive-pokemon-doubles-team-design/SKILL.md) whenever creating, modernizing, reviewing, or balancing 6-mon NPC Doubles rosters, assigning held items/moves/abilities, establishing weather/Trick Room/Tailwind strategies, or evaluating turn-1 gimmick safety for Run & Bun AI.

---

## 6. Verification Authority & Environment Reality

Cobbleverse Hell Mode enforces six explicit layers of verification authority:
- **Layer 0 (Markdown Structural Authority):** Relative link integrity, YAML frontmatter schemas, file bounds, and documentation cross-links. Authoritative solely for structural syntax; semantic governance authority belongs to independent contract review.
- **Layer 1 (Datapack & Trainer Schema Validation):** Trainer JSON schemas, battle formats, and economy rules.
- **Layer 2 (Java Unit & Boundary Tests):** Fast, isolated tests authoritative for pure algorithms, calculation formulas, and boundary math.
- **Layer 3 (Bytecode & Shadow Runtime Contracts):** Offline contracts authoritative for Fabric Mixin target classes, method descriptors, and shadow field offsets.
- **Layer 4 (Headless Server Bootstrap Smoke):** Startup/Mixin smoke only, verifying Knot bootstrap and Cobblemon mod initialization. Not authoritative for battle AI logic or multiplayer gameplay.
- **Layer 5 (Production Host Canary & Live Gameplay):** Dedicated live production host. The only authoritative verification for multiplayer stability, battle AI decisions, and player progression.

### Orthogonal Verification Sets
- Verification layers form an **orthogonal set** of domain authorities directly covering affected contracts; no layer implies another automatically.
- For pure Markdown, governance, and skill modifications, Layer 0 is the **sole applicable automated repository check**. Automated structural checks verify syntax only and do not establish semantic correctness. Semantic governance authority belongs exclusively to independent contract review.
- Verification commands, freshness rules, and manifest formats are delegated to [`.agents/skills/test-and-verification-strategy/SKILL.md`](.agents/skills/test-and-verification-strategy/SKILL.md).

### Core Invariant: Offline PASS != Production Semantic PASS (Canary Lessons 6 & 7)
- Automated test passes locally or in CI (Layers 0–4) are necessary but **never sufficient** to claim that gameplay integration is GREEN.
- Never refer to a local CurseForge instance or development client as "production".
- Statements regarding test results must be strictly proportional to observable evidence without unverified semantic absolutes (`"hoàn toàn"`, `"triệt để"`, `"guarantees"`, `"flawless"`).

---

## 7. Git Safety & Local Checkpoint Contract

### Two-Phase Commit Lifecycle
- **Commit Creation != Audit Promotion:** Local checkpoint commits are internal multi-agent coordination records, distinct from promoted audit milestones.
- **Audit Promotion Boundary:** A checkpoint is promoted to an immutable audit milestone only through an explicit external event (Owner acceptance, merged historical evidence, or accepted live production canary evidence). Internal Reviewer `PASS` alone does not promote a commit to an immutable audit milestone.
- **Provisional Local Commits:** Unpromoted local commits on a working branch may be amended, squashed, or rewritten if explicitly authorized by Owner.
- **Promoted Audit Milestones:** Commits promoted to audit status must never be rebased, squashed, amended, or deleted. Forward merges are recommended when preserving multi-agent audit lineage is required. Detailed lifecycle rules and commit templates are delegated to [`.agents/skills/git-checkpoint-workflow/SKILL.md`](.agents/skills/git-checkpoint-workflow/SKILL.md).

### Local Checkpoint Permission Contract
- **Mode 2 (Direct Bounded Tasks):** Do NOT create git commits unless the owner explicitly requests or approves a commit for the current task.
- **Mode 3 (Managed-Agent Workflow):** When the owner has **explicitly authorized implementation through the managed-agent workflow**, Main Controller is authorized to create necessary **local checkpoint commits** without re-prompting the owner for every individual commit (Plan Freeze, Verified Implementation, Correction, Production Canary).

### Surgical Staging Discipline
- Stage only explicitly owned files using targeted paths. NEVER use blind staging. Always inspect status and staged diffs before committing.

### Strict Remote Actions Gate
Under NO circumstances may an agent perform the following without separate, explicit Owner authorization:
- `commit != push != PR != merge != rebase != rewrite != force-push`
- Remote push to any repository (`origin`, `upstream`);
- Pull request creation, update, or comment via GitHub CLI;
- Branch merge or rebase onto base branches;
- Force-push (`--force`);
- Branch deletion;
- Production deployment or remote server mutation.

### Review Checkpoint Report
Each completed implementation task must conclude with a structured review checkpoint containing:
1. Changed / created files and modified sections.
2. Semantic change summary (behavior, contract, or design decisions changed).
3. Concrete verification commands run and their exact outputs.
4. Residual gaps, risks, or unverified assumptions.
5. Git state (uncommitted changes, branch status).
6. Recommended English Conventional Commit message (e.g., `feat(ai): ...`, `fix(companion): ...`).

### Diff & Change Reporting Policy
- **No Full Diff Dumps:** Do not paste the complete repository diff into user-facing reports by default.
- **Core Invariant:** *"User-facing reports summarize the change; Git diff remains the review artifact."*
- **Agent Self-Review vs. User Report:** Agents must still run and inspect diffs internally to verify modifications before finalizing, but the raw diff output must not be mirrored wholesale into the report.
- **High-Level Change Metrics:** Provide concise scope metrics such as diff stat, changed file lists, or hunk line counts.
- **Selective Snippets Only:** Include focused diff hunks or exact code snippets only when concise and materially useful for user review, or when explicitly requested.

