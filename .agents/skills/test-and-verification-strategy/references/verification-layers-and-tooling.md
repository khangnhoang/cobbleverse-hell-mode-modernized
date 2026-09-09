# Verification Layers, Tooling & Evidence Manifests

This reference provides exact commands, execution procedures, artifact freshness rules, and evidence manifest templates across Cobbleverse Hell Mode's 6 verification layers.

---

## 1. Tooling & Commands by Verification Layer

### Layer 0: Markdown Structural Authority
Authoritative solely for structural and syntactic integrity of skill definitions, governance documents, YAML frontmatters, file line bounds, and documentation cross-links:
- **Link & Route Verification:** Verify that all skill routes referenced in `AGENTS.md` and relative links in `references/*.md` resolve to valid files on disk.
- **YAML Frontmatter Integrity:** Verify `name` matches folder name (kebab-case) and `description` is non-empty.
- **File Line Count Bounds:** Verify that skill definitions and references remain concise (< 500 lines per file; `AGENTS.md` strictly < 250 lines).
- **Proscribed Absolute Phrase Scan:** Verify zero occurrences of forbidden narrow absolutes (`"hoàn toàn"`, `"triệt để"`, `"guarantees"`, `"flawless"`, `"không có rủi ro"`, `"zero risk"`, `"tuyệt đối"`, `"completely closes"`, or unsupported `"100% tuân thủ"`) outside explicit definition blocks. Calibrated claim exception permits verified numeric counts and ratios with explicit denominators (e.g., `14/14 (100%)`, `0 broken links`).
- **Sole Applicable Automated Repository Check:** For pure Markdown, governance, and skill changes, Layer 0 is the sole applicable automated repository check. Higher layer suites are inapplicable and skipped. Automated structural checks verify syntax only and do not establish semantic correctness. Semantic governance authority belongs exclusively to independent contract review. Never claim Layer 0 checks are "fully sufficient" or "complete proof".

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

## 2. Artifact Freshness Protocol (Canary Lesson 9 & Finding F)

To avoid redundant rebuilds while preventing stale test results, artifact freshness is established primarily through **provenance and content identity**, with timestamps serving only as supporting fallback:

### 2.1 Hierarchy of Freshness Evidence
1. **Recorded Source Revision / Commit Identity:** Exact commit SHA where verification occurred. If the working tree is clean for the component and recorded commit matches, the result is fresh.
2. **Source / Content Hashes of Inputs:** Hash of constituent source files matches recorded execution hashes.
3. **Expected Artifact Contents / Embedded Identity:** Artifact contains embedded version, hash, or build identifier matching source revision.
4. **Build Metadata / Tool Provenance:** Build tool records matching execution metadata.
5. **Timestamps (Supporting Fallback Only):** Artifact timestamp is strictly newer than constituent source files (used only when cryptographic provenance is unavailable).

### 2.2 Freshness Evaluation & Re-Execution Triggers
- **Freshness Established:** If available provenance/identity evidence confirms the artifact matches current sources, reuse existing results without re-execution.
- **Mandatory Re-Execution Triggers:** Re-execution is required ONLY when:
  1. Input source files or contracts in the component have changed;
  2. Recorded commit/content identity does not match current state;
  3. Working-tree modifications invalidate prior test evidence;
  4. An author applies fixes during a Reconciliation Cycle.
- **Proscription:** Rebuilds merely "for certainty" are prohibited. Rebuild only when evidence cannot establish freshness. Eliminate cumbersome dependency-cone directory crawling.

---

## 3. Verification Evidence Manifest Template

When preparing evidence for Implementation Reviewer `IR`, the Verification Evidence Manifest is canonically documented in `docs/workstreams/<workstream-id>/verification.md` (or maintained in ephemeral scratch memory if uncommitted):

```markdown
## Verification Evidence Manifest

- **Workstream ID:** `<workstream-id>`
- **Baseline Commit:** `<SHA-1>`
- **Current Branch:** `<branch-name>`
- **Candidate Hash:** `<SHA-1>` (if applicable)

### Working-Tree Status
```text
<Output of git status --short>
```

### Tracked Diff Summary
```text
<Output of git diff --stat <baseline-commit>>
```

### Verification Suite Results (Minimal Orthogonal Set)

#### Executed Applicable Layers:
[Include only the layers directly covering affected contracts]

##### Layer 0: Markdown & Governance (Sole Applicable Automated Check)
- **Checks:** Route integrity, relative links, YAML frontmatter, line count bounds (< 500 lines)
- **Exit Code:** 0
- **Result:** All routes and links verified; schema valid. Note: structural automation verifies syntax only; does not establish semantic correctness.

##### Layer 1: Datapack Schema Validation [If applicable]
- **Command:** `python scripts/ci/validate_repo.py`
- **Exit Code:** 0
- **Log Excerpt:** `PASSED (1714 trainers validated, 0 errors)`

##### Layer 2: Java Unit Tests [If applicable]
- **Command:** `./gradlew test --info`
- **Exit Code:** 0
- **Log Excerpt:** `BUILD SUCCESSFUL`

##### Layer 3: Bytecode Runtime Contracts [If applicable]
- **Command:** `python scripts/runtime-contract/test_rct_runtime_contract.py`
- **Exit Code:** 0
- **Log Excerpt:** `41/41 bytecode invariants verified`

#### Inapplicable / Skipped Layers:
- **Layer [X]:** Skipped (Justification: e.g., 0 datapack JSONs / 0 Java classes / 0 Mixin bytecode modified; Layer 0 sole applicable automated check).

### Freshness Status
- **Evaluation:** Freshly executed on current working tree via provenance/content identity.

### Unverified / Skipped Items Requiring Live Verification
- **Layer 5 Live Canary:** Requires dedicated server canary testing (if applicable).
```
