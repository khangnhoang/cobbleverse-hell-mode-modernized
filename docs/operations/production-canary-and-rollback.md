# Production Canary & Rollback SOP

This document defines the Standard Operating Procedure (SOP) for canary deploying runtime artifacts (companion mod JAR and datapacks) to the live Production Host, monitoring battle execution, and executing rollbacks.

> [!NOTE]
> **Task Scope Exception**:
> The repository restructuring and documentation architecture task does **not** alter runtime gameplay or companion mod behavior. No canary deployment is required for pure structural or documentation changes.

---

## 1. Pre-Canary Verification Gate

Before any artifact is deployed to the Production Host, all local automated gates must pass:
1. `python scripts/ci/check_legacy_baseline.py` $\rightarrow$ `PASS`
2. `python scripts/ci/validate_repo.py` $\rightarrow$ `PASS`
3. `python scripts/runtime-contract/test_rct_runtime_contract.py` $\rightarrow$ `PASS`
4. `./gradlew test --rerun-tasks` $\rightarrow$ `100% PASS`
5. `./gradlew build` $\rightarrow$ `BUILD SUCCESSFUL`

---

## 2. Canary Deployment Procedure

### A. Deploying the Companion Mod
1. Build the release JAR:
   ```bash
   cd companion-mod && ./gradlew build
   ```
2. The output artifact is located at:
   `companion-mod/build/libs/rct-legendary-rule-companion-1.0.0.jar`
3. Back up the existing companion JAR on the server host:
   ```bash
   cp <server_root>/mods/rct-legendary-rule-companion-1.0.0.jar <server_root>/mods/rct-legendary-rule-companion-1.0.0.jar.bak
   ```
4. Transfer the new JAR into `<server_root>/mods/`.

### B. Deploying Modernized Datapacks
1. The canonical datapack source is:
   `datapacks/hell-mode/`
2. Sync the folder or update the zipped pack inside `<world_dir>/datapacks/`.
3. Execute `/datapack enable "file/hell-mode"` or restart the server.

---

## 3. Canary Observation Protocol

1. **Server Boot Inspection:**
   Tail `<server_root>/logs/latest.log` during startup:
   - Verify Fabric Loader applies companion mixins without `InvalidSliceException` or `MixinApplyError`.
   - Confirm Cobblemon and RCTMod bootstrap normally.
2. **Targeted In-Game Battle Smoke:**
   - Engage a key NPC trainer relevant to the change (e.g., Lt. Surge for Doubles strategy and recharge guards).
   - Observe Turn 1 and Turn 2 decision logs in `latest.log`.
   - Confirm:
     - `RunBunAI.choose()` calculates without `NoSuchElementException` or unhandled exceptions.
     - Move selections, lead choices, and damage scoring operate smoothly.
     - Battle completes cleanly with normal win/loss triggers.

---

## 4. Rollback Procedure

If an unhandled exception, crash, or broken battle behavior occurs during canary testing:
1. **Immediate Mod Rollback:**
   - Stop server.
   - Delete `<server_root>/mods/rct-legendary-rule-companion-1.0.0.jar`.
   - Restore the `.bak` copy:
     ```bash
     mv <server_root>/mods/rct-legendary-rule-companion-1.0.0.jar.bak <server_root>/mods/rct-legendary-rule-companion-1.0.0.jar
     ```
2. **Log Extraction & Incident Brief:**
   - Preserve `<server_root>/logs/latest.log` and crash reports under `<server_root>/crash-reports/`.
   - Isolate the stack trace and failing bytecode line number.
   - Recreate the failure trace in a focused investigation brief under `docs/workstreams/<workstream>/investigations/`.
