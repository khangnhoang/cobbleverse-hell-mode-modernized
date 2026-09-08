---
name: test-and-verification-strategy
description: Proportional verification, Hell 6-layer authority hierarchy, offline vs production reality distinction, false-green prevention, and artifact freshness discipline in Cobbleverse Hell Mode.
---

# Test & Verification Strategy

This skill defines the verification authority hierarchy, proportional test allocation, false-green prevention, artifact freshness protocol, and evidence manifest assembly for Cobbleverse Hell Mode.

---

## 1. Activation Scope & Responsibilities

Activate this skill when:
- Operating as Implementor (`I`) verifying changes or preparing evidence manifests;
- Operating as Planner (`P`) designing verification strategies across Hell's 6 layers;
- Auditing test coverage, evaluating artifact freshness, or verifying repository contracts.

**Ownership Boundary:**
- **Owns:** Proportional verification principle, Hell 6-Layer Authority Hierarchy, offline PASS vs. production semantic PASS distinction, false-green prevention, regression reproduction, assembling Verification Evidence Manifests for hard read-only reviewers, and the Artifact Freshness Protocol.
- **Does NOT Own:** Subagent session orchestration (owned by `managed-agent-workflow`); code review rubrics (owned by `code-review-and-quality`); or git staging and commits (owned by `git-checkpoint-workflow`).

---

## 2. Resource Routing

Read bundled references strictly when their conditions match:

| Resource | Read Condition | Skip When |
| :--- | :--- | :--- |
| [`references/verification-layers-and-tooling.md`](references/verification-layers-and-tooling.md) | Read when executing commands for specific layers (Layers 0–5), verifying artifact freshness criteria, or assembling a Verification Evidence Manifest. | Inspecting high-level verification principles or planning scope boundaries. |

---

## 3. Proportional Verification Principle

Verification effort must be scaled proportionally to the risk profile and blast radius of the change:

| Risk Tier | Change Types | Required Verification Layers |
| :--- | :--- | :--- |
| **Low Risk** | Documentation, skill definitions, markdown governance | **Layer 0:** Markdown link integrity, YAML frontmatter check, file bounds. |
| **Medium Risk** | Datapack JSONs, trainer rosters, battle formats | **Layer 1:** `validate_repo.py` + `check_legacy_baseline.py`. |
| **High Risk** | Java algorithms, battle AI scoring, damage math | **Layer 2:** `./gradlew test` (JUnit 5) + Layer 1. |
| **Critical Risk** | Fabric Mixins, bytecode injection, shadow fields | **Layer 3:** `test_rct_runtime_contract.py` + Layer 2 + **Layer 4:** `./gradlew runServer`. |
| **Live Balance** | Player progression, tournament balance, multiplayer | **Layer 5:** Production Canary Host live gameplay. |

### 3.1 Layer 0 Sufficiency for Governance & Markdown Tasks
When a task modifies exclusively Markdown files, agent documentation, or workflow skill configurations:
- **Layer 0 is the SOLE authoritative verification layer.**
- Passing Layer 0 automated checks (route resolution, relative link integrity, YAML frontmatter schemas, file line bounds < 500 lines) is **fully sufficient** for implementation verification.
- Higher layer test suites (Layer 1 `validate_repo.py`, Layer 2 `./gradlew test`, Layer 3 `test_rct_runtime_contract.py`, Layer 4 `./gradlew runServer`, Layer 5 Canary) are **inapplicable** and must NOT be executed or mandated.

---

## 4. The Hell 6-Layer Authority Hierarchy

Cobbleverse Hell Mode enforces six explicit layers of verification authority:

```text
Layer 5: Production Canary Host (Live Dedicated Server, Battle Telemetry)
   ▲  [Authoritative ONLY for live multiplayer, progression, real-time AI]
Layer 4: Headless Server Bootstrap Smoke (./gradlew runServer)
   ▲  [Smoke ONLY: Knot bootstrap, Mixin transform, Cobblemon mod init]
Layer 3: Bytecode & Shadow Runtime Contracts (test_rct_runtime_contract.py)
   ▲  [Authoritative for Mixin targets, method descriptors, shadow fields]
Layer 2: Java Unit & Boundary Tests (./gradlew test)
   ▲  [Authoritative for pure algorithmic calculations and isolated math]
Layer 1: Datapack & Trainer Schema Validation (validate_repo.py)
   ▲  [Authoritative for 1,714 trainer JSON schemas and economy rules]
Layer 0: Markdown & Governance Authority (Relative links, YAML frontmatter)
      [Authoritative for skill routing, links, and documentation integrity]
```

---

## 5. Core Invariant: Offline PASS != Production Semantic PASS (Canary Lessons 6 & 7)

1. **Strict Authority Demarcation:** Local automated test passes (Layers 0–4) are necessary but **never sufficient** to claim that gameplay integration is GREEN.
2. **Never Claim Parity:** Never refer to a local development environment or CurseForge client as "production".
3. **Headless Smoke Limits:** `./gradlew runServer` confirms only that the server bootstraps without Mixin crashes; it does NOT prove battle AI decision quality or multiplayer balance.
4. **Live Canary Authority:** Real trainer battles, party level scaling, and player progression can **only** be verified on the live dedicated production host (Layer 5).

---

## 6. False-Green Prevention

- **Demonstrate Failure First:** For bug fixes, verify that the test suite detects the defect or provide clear mathematical proof of the unpatched vulnerability.
- **Observable Behavior over Mocks:** Test real calculations and domain objects; do not mock away the exact mechanism being verified.
- **Negative & Boundary Testing:** Cover boundary values (e.g., minimum move power 20, maximum move power 120, division by zero, null battle contexts).

---

## 7. Artifact Freshness Protocol (Canary Lesson 9)

To avoid redundant rebuilds while preventing stale test results:
- **Freshness Criteria:** A test result or artifact is fresh without re-execution ONLY when:
  1. Cryptographic hashes of all input files in the subsystem cone are unchanged;
  2. `git status --short` confirms zero uncommitted modifications in the subsystem;
  3. Output test report timestamps are strictly newer than all constituent source files.
- **Mandatory Re-Execution Triggers:** Immediate re-execution is mandatory whenever:
  1. Any source code, Mixin, trainer JSON, or test fixture in the dependency cone has changed;
  2. Working-tree modifications invalidate previous test evidence;
  3. An author applies fixes during a Reconciliation Cycle.

---

## 8. Verification Evidence Manifest Assembly

Because Implementation Reviewer `IR` is strictly read-only, Main Controller or Implementor compiles an evidence manifest:
- Baseline commit and current branch;
- Working-tree status (`git status --short`);
- Tracked diff (`git diff <baseline>`);
- Executed commands with raw exit codes and log excerpts for applicable layers;
- Explicit statement of skipped/inapplicable layers with clear technical justification (e.g., Layer 1–5 skipped for pure Layer 0 governance changes);
- Explicit list of unverified items requiring Layer 5 Canary testing (if applicable).
