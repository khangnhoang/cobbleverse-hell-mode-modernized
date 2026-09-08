---
name: test-and-verification-strategy
description: Proportional verification, Hell 6 orthogonal verification domains, offline vs production reality distinction, false-green prevention, and artifact freshness discipline in Cobbleverse Hell Mode.
---

# Test & Verification Strategy

This skill defines the orthogonal verification domains, proportional test allocation, false-green prevention, artifact freshness protocol, and evidence manifest assembly for Cobbleverse Hell Mode.

---

## 1. Activation Scope & Responsibilities

Activate this skill when:
- Operating as Implementor (`I`) verifying changes or preparing evidence manifests;
- Operating as Planner (`P`) designing verification strategies across Hell's 6 layers;
- Auditing test coverage, evaluating artifact freshness, or verifying repository contracts.

**Ownership Boundary:**
- **Owns:** Proportional verification principle, Hell 6 Orthogonal Verification Domains, offline PASS vs. production semantic PASS distinction, false-green prevention, regression reproduction, assembling Verification Evidence Manifests for hard read-only reviewers, and the Artifact Freshness Protocol.
- **Does NOT Own:** Subagent session orchestration (owned by `managed-agent-workflow`); code review rubrics (owned by `code-review-and-quality`); or git staging and commits (owned by `git-checkpoint-workflow`).

---

## 2. Resource Routing

Read bundled references strictly when their conditions match:

| Resource | Read Condition | Skip When |
| :--- | :--- | :--- |
| [`references/verification-layers-and-tooling.md`](references/verification-layers-and-tooling.md) | Read when executing commands for specific layers (Layers 0–5), verifying artifact freshness criteria, or assembling a Verification Evidence Manifest. | Inspecting high-level verification principles or planning scope boundaries. |

---

## 3. Orthogonal Verification Selection (Finding E)

Verification layers are an **orthogonal set of domain authorities**, NOT a hierarchical ladder where higher layers inherit lower layers.
- **Selection Principle:** Diff -> Identify affected contracts -> Select the minimal orthogonal set of verification layers that directly cover those contracts.
- **No Hierarchical Inheritance:** Running Layer 3 does not automatically mandate Layer 2 or Layer 1 unless bytecode, pure logic, and datapack schemas are each modified in the diff. No numeric layer implies another automatically.

### 3.1 Orthogonal Verification Mapping Table

| Modified Subsystem / Contract | Governing Authority | Applicable Verification Checks |
| :--- | :--- | :--- |
| **Governance, Markdown, Skills** | Layer 0 (Markdown Structural Authority) | Route resolution, relative links, YAML frontmatter schemas, file bounds (< 500 lines; `AGENTS.md` < 250 lines). (Structural syntax only; semantic governance belongs to independent review). |
| **Datapacks & Trainer Schemas** | Layer 1 (Datapack & Trainer Schema) | `python scripts/ci/validate_repo.py` + `check_legacy_baseline.py`. |
| **Pure Java Logic & Boundary Math** | Layer 2 (Java Unit & Boundary Tests) | `./gradlew test --info` (JUnit 5 isolated tests). |
| **Fabric Mixins & Shadow Bytecode** | Layer 3 (Bytecode & Shadow Contracts) | `python scripts/runtime-contract/test_rct_runtime_contract.py`. |
| **Server Bootstrap & Mixin Smoke** | Layer 4 (Headless Server Smoke) | `./gradlew runServer` (Mixin initialization smoke). |
| **Multiplayer Gameplay & Live Progression** | Layer 5 (Production Host Canary) | Dedicated live production canary testing. |

### 3.2 Layer 0 Claim Rigor for Governance & Documentation
When a task modifies exclusively Markdown files, agent documentation, or workflow skill configurations:
- Layer 0 is the **sole applicable automated repository check**.
- Passing Layer 0 automated checks verifies structural and syntactic integrity (valid links, well-formed YAML frontmatter, line count bounds).
- **Claim Strength Discipline (Canary Lesson 7):** Automated Layer 0 checks verify structural syntax only and do **NOT** establish semantic governance correctness, authority validity, routing correctness, or invariant preservation. Semantic governance authority belongs exclusively to independent contract review.
- Inapplicable layers (Layers 1–5) are skipped because no datapacks, Java source, Mixins, or gameplay systems are modified.

---

## 4. The Hell 6 Orthogonal Verification Domains

Cobbleverse Hell Mode enforces six explicit, orthogonal domains of verification authority:

```text
┌─────────────────────────────────────────────────────────────────────────────┐
│                       ORTHOGONAL VERIFICATION DOMAINS                       │
│    (Selected minimally per diff — no vertical ladder or auto-inheritance)    │
├─────────┬──────────────────────────────────┬────────────────────────────────┤
│ Domain  │ Authority Scope                  │ Verification Target / Tool     │
├─────────┼──────────────────────────────────┼────────────────────────────────┤
│ Layer 0 │ Markdown Structural Authority    │ Links, YAML Frontmatter, Bounds│
│ Layer 1 │ Datapack & Trainer Schema        │ validate_repo.py, baseline.py  │
│ Layer 2 │ Java Unit & Boundary Math        │ ./gradlew test (Isolated JUnit)│
│ Layer 3 │ Bytecode & Shadow Contracts      │ test_rct_runtime_contract.py   │
│ Layer 4 │ Headless Server Bootstrap Smoke  │ ./gradlew runServer (Smoke)    │
│ Layer 5 │ Production Host Canary           │ Live Dedicated Host Telemetry  │
└─────────┴──────────────────────────────────┴────────────────────────────────┘
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

## 7. Artifact Freshness Protocol (Finding F)

To avoid redundant rebuilds while preventing stale test results, artifact freshness is established primarily through **provenance and content identity**, with timestamps serving only as supporting fallback:

### 7.1 Hierarchy of Freshness Evidence
1. **Recorded Source Revision / Commit Identity:** Exact commit SHA where verification occurred. If the working tree is clean for the component and recorded commit matches, the result is fresh.
2. **Source / Content Hashes of Inputs:** Hash of constituent source files matches recorded execution hashes.
3. **Expected Artifact Contents / Embedded Identity:** Artifact contains embedded version, hash, or build identifier matching source revision.
4. **Build Metadata / Tool Provenance:** Build tool records matching execution metadata.
5. **Timestamps (Supporting Fallback Only):** Artifact timestamp is strictly newer than constituent source files (used only when cryptographic provenance is unavailable).

### 7.2 Freshness Evaluation & Re-Execution Triggers
- **Freshness Established:** If available provenance/identity evidence confirms the artifact matches current sources, reuse existing results without re-execution.
- **Mandatory Re-Execution Triggers:** Re-execution is required ONLY when:
  1. Input source files or contracts in the component have changed;
  2. Recorded commit/content identity does not match current state;
  3. Working-tree modifications invalidate prior test evidence;
  4. An author applies fixes during a Reconciliation Cycle.
- **Proscription:** Rebuilds merely "for certainty" are prohibited. Rebuild only when evidence cannot establish freshness.

---

## 8. Verification Evidence Manifest Assembly

Because Implementation Reviewer `IR` is strictly read-only, Main Controller or Implementor compiles an evidence manifest:
- Baseline commit and current branch;
- Working-tree status (`git status --short`);
- Tracked diff (`git diff <baseline>`);
- Executed commands with raw exit codes and log excerpts for applicable layers;
- Explicit statement of skipped/inapplicable layers with clear technical justification (e.g., Layer 1–5 skipped for pure Layer 0 governance changes);
- Explicit list of unverified items requiring Layer 5 Canary testing (if applicable).
