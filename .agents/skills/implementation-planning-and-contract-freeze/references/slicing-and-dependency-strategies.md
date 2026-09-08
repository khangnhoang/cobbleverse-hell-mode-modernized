# Slicing & Dependency Strategies across Hell Mode Layers

This reference defines layer-aware decomposition and dependency ordering strategies tailored to Cobbleverse Hell Mode's architectural layers.

---

## 1. Architectural Layers & Boundaries

Hell Mode projects span six distinct architectural layers, ordered from lowest runtime risk to highest live environment sensitivity:

```text
Layer 0: Markdown Governance & Documentation (AGENTS.md, workstreams, skill contracts)
   │
Layer 1: Datapack Data & Trainer JSONs (data/cobblemon/trainers/, validate_repo.py)
   │
Layer 2: Java Pure Logic & Battle Algorithms (WeightDependentMoveResolver, JUnit tests)
   │
Layer 3: Fabric Mixin Bytecode & Shadow Boundaries (test_rct_runtime_contract.py)
   │
Layer 4: Headless Server Bootstrap Smoke (./gradlew runServer, Knot bootstrap)
   │
Layer 5: Production Host Canary & Live Gameplay (Dedicated server, multiplayer telemetry)
```

---

## 2. Dependency Ordering Rules

When designing implementations, Planner must adhere to strict ordering invariants:

### Rule 1: Contract-First Design (Layer 2 before Layer 3)
- Always implement core calculation algorithms, scoring formulas, and data models as pure, isolated Java components (Layer 2) covered by JUnit tests **before** wiring them into Fabric Mixin injection points (Layer 3).
- **Rationale:** Debugging bytecode injection bugs when the underlying calculation logic is also unproven creates combinatorial failure modes. Pure algorithms can be tested rapidly and deterministically in isolation.

### Rule 2: Non-Breaking Data Co-Evolution (Layer 1 before or atomic with Layer 2/3)
- When introducing new battle properties, movesets, or trainer attributes, ensure datapack JSONs remain valid under existing schemas during intermediate stages.
- Datapack changes must run atomically through `python scripts/ci/validate_repo.py`.

### Rule 3: Mixin Injection Isolation (Layer 3 before Layer 4/5)
- Verify Mixin target classes, method descriptors, and shadow field offsets offline via `python scripts/runtime-contract/test_rct_runtime_contract.py` before running server smoke or live canaries.
- Avoid deploying unverified bytecode patches directly to server instances.

---

## 3. Slicing Patterns for Complex Tasks

### Pattern A: The Surrogate / Adapter Pattern
When modifying Cobblemon internal mechanics (e.g., dynamic move damage, weight calculations, AI scoring):
1. **Slice 1 (Pure Logic):** Create a dedicated Resolver/Calculator class (e.g., `WeightDependentMoveResolver`) handling all formulas, clamping, and edge cases with exhaustive unit tests.
2. **Slice 2 (Surrogate Interface):** Create an adapter interface (e.g., `WeightDependentMoveSurrogate`) allowing game objects to provide needed attributes without invasive class hierarchy mutations.
3. **Slice 3 (Mixin Injection):** Write surgical Mixin injection hooks that delegate immediately to the Resolver/Surrogate.
4. **Slice 4 (Contract Verification):** Validate bytecode stability via runtime contract test scripts.

### Pattern B: Vertical Slices by Battle Domain
For multi-trainer or multi-archetype balance updates:
1. **Slice 1 (Lead Pairs & Archetype Foundation):** Define core leads, weather/Trick Room setters, and primary synergy lines.
2. **Slice 2 (Mid-Game & Pivot Coverage):** Implement defensive pivots, redirectors, and switch-in answers.
3. **Slice 3 (Endgame Cleaners & Held Items):** Assign specific damage-boosting or survival items (Focus Sash, Choice items).
4. **Slice 4 (Datapack Batch Validation):** Validate all affected JSONs in batch using repo validators.

---

## 4. Anti-Patterns to Avoid

- **The Monolithic PR / Mega-Diff:** Combining governance updates, datapack JSON renames, Mixin bytecode injections, and server config tweaks in a single un-sliced diff.
- **Speculative Abstraction Slicing:** Creating abstract interfaces, reflection wrappers, or generic registries for a feature that only has one concrete implementation.
- **Skipping Offline Contracts:** Proceeding directly from Java editing to `./gradlew runServer` or production deployment without running offline contract verifications.
