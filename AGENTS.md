# Agent Engineering Guidelines & System Contract

These engineering principles and operational rules guide all autonomous and interactive AI agent development in `khangnhoang/cobbleverse-hell-mode-modernized`.

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
   - Preserve working content: retain existing working trainer definitions, battle formats, and configurations unless there is a verified gameplay, runtime, or task reason to alter them.

4. **Goal-Driven Execution & Proportional Verification:**
   - Define concrete, testable success criteria before making changes.
   - Verify changes using authoritative repository tooling (`scripts/ci/validate_repo.py`, unit tests, offline bytecode contracts).
   - Scale validation effort proportionally to the risk profile of the change.

5. **Read Before Write:**
   - Always inspect the existing code, contracts, refmaps, and relevant documentation before editing.
   - Never assume method signatures, bytecode offsets, or third-party mod behaviors from memory.

6. **Fail Loud:**
   - If tests fail, bytecode invariants break, or unexpected environment state is encountered, stop and report immediately. Never silently suppress errors or falsify test results.

---

## 2. Communication & Reporting Rules

- **Vietnamese Reporting:** When the owner communicates in Vietnamese, deliver all explanations, summaries, and checkpoint reports in Vietnamese.
- **Preserve Technical Literals:** Keep code identifiers, class names, method signatures, file paths, and technical terms in their exact English forms (e.g., `ActiveBattlePokemon`, `FairBattleContext`, `choose()`, `recharge`).

---

## 3. Skill Routing

Before planning non-trivial work or modifying specialized domains, inspect the task scope and route to the corresponding skill:

- **Competitive Pokémon Doubles Team Design:**
  Activate [`.agents/skills/competitive-pokemon-doubles-team-design/SKILL.md`](.agents/skills/competitive-pokemon-doubles-team-design/SKILL.md) whenever creating, modernizing, reviewing, or balancing 6-mon NPC Doubles rosters, assigning held items/moves/abilities, establishing weather/Trick Room/Tailwind strategies, or evaluating turn-1 gimmick safety for Run & Bun AI.

---

## 4. Verification Authority & Environment Reality

Always respect the distinction between local verification and production host reality:
- **Local Authoritative:** Unit tests (`./gradlew test`), repository data validation (`scripts/ci/validate_repo.py`), baseline checks (`scripts/ci/check_legacy_baseline.py`), and offline bytecode contracts (`scripts/runtime-contract/test_rct_runtime_contract.py`). Authoritative only for the explicit invariants they test.
- **Local Dev Server Smoke:** `./gradlew runServer` is startup/Mixin smoke only, useful for verifying Mixin application and headless server bootstrap, but is **NOT authoritative** for gameplay integration.
- **Production Host Canary:** Real trainer battle gameplay, player progression, and multiplayer stability can **only** be verified via manual canary testing on the live dedicated production host.
- **Never Claim Parity:** Never refer to a local CurseForge instance as "production," and never claim gameplay integration is GREEN solely based on local automated test passes.

---

## 5. Git Safety & Review Checkpoint Contract

- **No Unauthorized Commits:** Do not create git commits unless the owner explicitly requests or approves a commit for the current task.
- **No Push / PR / Merge:** Never run `git push`, open a pull request, or merge branches unless explicitly commanded by the owner.
- **Review Checkpoint Report:** Each completed implementation task must conclude with a structured review checkpoint containing:
  1. Changed / created files.
  2. Concrete verification commands run and their exact outputs.
  3. Residual gaps, risks, or unverified assumptions.
  4. Recommended English Conventional Commit message (e.g., `feat(ai): ...`, `fix(companion): ...`).
