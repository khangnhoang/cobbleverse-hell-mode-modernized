# Verification Layers, Tooling & Evidence Manifests

This reference provides exact commands, execution procedures, artifact freshness rules, and evidence manifest templates across Cobbleverse Hell Mode's 6 verification layers.

---

## 1. Tooling & Commands by Verification Layer

### Layer 0: Markdown & Governance Authority
Validates skill definitions, YAML frontmatters, and documentation cross-links:
- **Link & Route Verification:** Verify that all skill routes referenced in `AGENTS.md` and relative links in `references/*.md` resolve to valid files on disk.
- **YAML Frontmatter Integrity:** Verify `name` matches folder name (kebab-case) and `description` is non-empty.

### Layer 1: Datapack & Trainer Schema Validation
Validates all 1,714 trainer JSON files, battle formats, and item restrictions:
```powershell
# Run full datapack and trainer validation suite
python scripts/ci/validate_repo.py

# Check against legacy baseline invariants
python scripts/ci/check_legacy_baseline.py
```
- **Pass Criteria:** 0 schema errors, 0 missing move/item references, 0 unapproved bag healing items.

### Layer 2: Java Unit & Boundary Tests
Runs fast, isolated JUnit 5 unit tests for pure algorithms and boundary math:
```powershell
# Execute JUnit test suite
./gradlew test --info
```
- **Pass Criteria:** All tests pass; test reports generated in `build/reports/tests/test/index.html`.

### Layer 3: Bytecode & Shadow Runtime Contracts
Verifies Fabric Mixin target classes, method descriptors, and shadow field offsets offline without launching Minecraft:
```powershell
# Verify Mixin bytecode contracts
python scripts/runtime-contract/test_rct_runtime_contract.py
```
- **Pass Criteria:** 41/41 bytecode checks verified, 0 contract regressions.

### Layer 4: Headless Server Bootstrap Smoke
Validates Knot classloader bootstrap, Mixin transformation, and Cobblemon mod initialization:
```powershell
# Run headless dedicated server smoke test
./gradlew runServer
```
- **Smoke Criteria:** Server completes initialization and reaches the Minecraft command prompt without Mixin crash or class-loading deadlock.
- **Important Note:** Layer 4 confirms ONLY that the server boots; it does NOT prove battle AI logic or multiplayer stability.

### Layer 5: Production Canary Host & Live Gameplay
The sole authoritative layer for multiplayer stability, battle AI decisions, and player progression:
- Conducted on the dedicated live production host.
- Inspects real-time battle logs, AI move selection telemetry, and multiplayer session persistence.
- Findings recorded as a Canary Checkpoint (`docs/workstreams/<id>/canary-evidence.md`).

---

## 2. Artifact Freshness Protocol (Canary Lesson 9)

### Freshness Evaluation Criteria
An agent may reuse existing verification output without re-running tests ONLY when all three conditions are met:
1. **Source Hash Stability:** `git hash-object` on every source file in the subsystem's dependency cone matches the recorded test run.
2. **Clean Status in Subsystem:** `git status --short <subsystem-path>` returns zero modified or untracked files.
3. **Artifact Timestamps:** The timestamp of the test report or log file is strictly newer than the newest source file modified in the subsystem.

### Mandatory Re-Execution Triggers
A fresh test run must be executed immediately whenever:
1. Any Java source, Mixin class, trainer JSON, or test fixture in the dependency cone is created or edited;
2. `git diff` shows modifications relative to the last verified commit;
3. An author applies a correction in response to a review finding during a Reconciliation Cycle.

---

## 3. Verification Evidence Manifest Template

When preparing evidence for Implementation Reviewer `IR`, Main Controller formats the manifest as follows:

```markdown
## Verification Evidence Manifest

- **Workstream ID:** `<workstream-id>`
- **Baseline Commit:** `<SHA-1>`
- **Current Branch:** `<branch-name>`
- **Candidate Hash:** `<SHA-1>`

### Working-Tree Status
```text
<Output of git status --short>
```

### Tracked Diff Summary
```text
<Output of git diff --stat <baseline-commit>>
```

### Verification Suite Results

#### Layer 0: Markdown & Governance
- **Command:** Route & link check
- **Exit Code:** 0
- **Result:** All routes and links verified.

#### Layer 1: Datapack Schema Validation
- **Command:** `python scripts/ci/validate_repo.py`
- **Exit Code:** 0
- **Log Excerpt:** `PASSED (1714 trainers validated, 0 errors)`

#### Layer 2: Java Unit Tests
- **Command:** `./gradlew test --info`
- **Exit Code:** 0
- **Log Excerpt:** `BUILD SUCCESSFUL - 18 tests completed, 0 failed`

#### Layer 3: Bytecode Runtime Contracts
- **Command:** `python scripts/runtime-contract/test_rct_runtime_contract.py`
- **Exit Code:** 0
- **Log Excerpt:** `41/41 bytecode invariants verified`

### Freshness Status
- **Evaluation:** Freshly executed on current working tree.

### Unverified / Skipped Items
- **Layer 5 Live Canary:** Requires live server canary deployment.
```
