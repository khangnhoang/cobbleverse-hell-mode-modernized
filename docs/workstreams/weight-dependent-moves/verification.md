# Verification & Production Canary: Weight-Dependent Moves

**Workstream:** `weight-dependent-moves`  
**Target Workstream Directory:** `docs/workstreams/weight-dependent-moves/`  
**Reference Frozen Plan:** [`plan.md`](plan.md) (commit `2a329a5`, hash `d2b137443c742b83f15281011b8af069069aea09`)  
**Implementation Commit:** `8ee3d27 feat(ai): resolve dynamic base power for weight-dependent moves in damage estimation`  

---

## 1. Automated Verification Summary (Offline / Local)

All automated test layers defined in the frozen implementation plan passed prior to deployment:
1. **Layer 1 & 2 (Unit & Boundary Suites):**
   - Command: `./gradlew test --tests com.cobbleverse.legendaryrule.strategy.weight.WeightDependentMove*`
   - Result: 32 tests passed (0 failures).
2. **Layer 3 (Damage & Bytecode Contracts):**
   - Command: `./gradlew test --rerun-tasks`
   - Result: 31 suites, 231 tests executed, 0 failures, 0 errors across companion-mod.
3. **Layer 4 (CI & Bytecode Verification):**
   - `python scripts/runtime-contract/test_rct_runtime_contract.py` $\rightarrow$ PASSED (including `FormData.getWeight()` and `Species.getStandardForm()` descriptors).
   - `python scripts/ci/validate_repo.py` $\rightarrow$ PASSED (1714 modernized trainers verified).

---

## 2. Observed Production Canary Evidence

Following deployment of the built companion mod JAR (`rct-legendary-rule-companion-1.0.0.jar`) to the live environment, actual server runtime behavior was observed in live battle scenarios:

- **Target 1 (Swampert - $81.9\text{ kg}$, Water/Ground, $4\times$ weak to Grass):**
  - Observed AI Damage Evaluation: `grassknot -> Swampert -> Damage: 164`
  - Prior behavior: evaluated to $\approx 2\text{--}8$ damage (base power $0.0$).
  - Observed delta: base power dynamically resolved to $80\text{ BP}$ tier ($50\text{--}100\text{ kg}$ range) entering the AI damage calculation path.
- **Target 2 (Lucario - $54.0\text{ kg}$, Steel/Fighting, resisted):**
  - Observed AI Damage Evaluation: `grassknot -> Lucario -> Damage: 30`
  - Prior behavior: evaluated to $\approx 2$ damage.
  - Observed delta: base power dynamically resolved entering the AI damage calculation path.
- **Runtime Stability:**
  - Headless server bootstrap and battle lifecycle completed with no startup or runtime crashes observed during the canary test session.

---

## 3. Evidence Boundary & Scope Qualifications

In accordance with repository engineering standards:
- The canary observation confirms that dynamic weight-dependent power successfully enters the Run & Bun AI damage estimation pipeline in the production runtime for the tested move and targets.
- These results reflect the observed test scenarios; they do not imply exhaustive coverage of all possible combat permutations, network conditions, or unobserved battle states.
