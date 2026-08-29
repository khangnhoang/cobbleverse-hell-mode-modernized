# Engineering Principles

These four lightweight principles guide all development in `khangnhoang/cobbleverse-hell-mode-modernized`. They prioritize data integrity, gameplay correctness, and minimal friction.

## 1. Think Before Coding
- **Understand the goal and constraints:** Before editing any files, inspect the existing implementation, relevant schemas, runtime capabilities, and upstream dependencies.
- **Surface material ambiguities:** Do not silently invent assumptions when an ambiguity materially impacts battle behavior, schema compliance, or gameplay balance. Ask or investigate.
- **Calibrate planning to complexity:** Do not create elaborate design reports or heavy planning ceremony for routine, bounded edits. Plan deeply only when making architectural or multi-system changes.

## 2. Simplicity First
- **Smallest complete solution:** Prefer the most direct, concise implementation that fully satisfies the task requirements.
- **Zero speculative infrastructure:** Avoid building custom agent frameworks, unnecessary abstractions, evaluation harnesses, or generalized helpers justified only by hypothetical future needs.
- **Keep data edits straightforward:** A datapack JSON modification, move correction, or config tweak should remain a simple, direct file edit.

## 3. Surgical Changes
- **Strictly scoped modifications:** Touch only the files and lines required to accomplish the stated task.
- **Zero opportunistic refactoring:** Do not perform unrelated reformatting, cosmetic cleanup, bulk renames, or style modernization outside the requested scope.
- **Preserve working content:** Retain existing working trainer definitions, battle formats, and configurations unless there is a verified gameplay, runtime, schema, or explicit task reason to alter them.

## 4. Goal-Driven Execution
- **Establish concrete success criteria:** Translate requirements into clear, verifiable outcomes (e.g., schema validation passes, valid Showdown move IDs, no runtime errors, CI green).
- **Verify against the criteria:** Validate the actual resulting state using authoritative repository tools (`validate_repo.py`, unit tests, baseline checks).
- **Proportional validation:** Scale testing effort to the risk profile of the change. Routine content fixes should be verified cleanly without unnecessary process overhead.
