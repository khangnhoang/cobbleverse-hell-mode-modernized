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

### Pattern A: Layer-Ascending Pipeline
For multi-layer features bridging data, core logic, and external interfaces:
1. **Slice 1 (Data & Models):** Define data structures, schemas, and static fixtures (Layers 1/2).
2. **Slice 2 (Pure Core Logic):** Implement standalone algorithmic calculations, decision rules, or state machines with exhaustive unit tests (Layer 2).
3. **Slice 3 (Adapter / Surrogate Interface):** Create boundary adapters, surrogate interfaces, or hooks decoupling internal logic from external runtime systems (Layers 2/3).
4. **Slice 4 (Contract Verification):** Validate offline bytecode, schema, or integration contracts (Layer 3 / Layer 0).
5. **Slice 5 (Consumer Integration):** Wire components into runtime entry points or consumer listeners.

### Pattern B: Subsystem Component Slicing
For horizontal architectural refactorings or multi-module upgrades:
1. **Slice 1 (Core Contracts & Invariants):** Define or update core governance, interface definitions, and base schemas.
2. **Slice 2 (Isolated Component Slices):** Implement changes across modular subsystems independently, ensuring each module compiles and validates in isolation.
3. **Slice 3 (Inter-Module Contract Verification):** Execute integration tests and cross-component consistency validators.
4. **Slice 4 (Tooling & Verification Manifest):** Finalize verification manifests, CI/CD checks, and audit documentation.

> [!NOTE]
> **Domain Team Composition Slicing:** For competitive NPC Doubles team design (weather setters, pivots, Trick Room, lead pairs, held items), refer to the domain skill: [`competitive-pokemon-doubles-team-design/references/`](../../competitive-pokemon-doubles-team-design/references/).

---

## 4. Anti-Patterns to Avoid

- **The Monolithic PR / Mega-Diff:** Combining governance updates, datapack JSON renames, Mixin bytecode injections, and server config tweaks in a single un-sliced diff.
- **Speculative Abstraction Slicing:** Creating abstract interfaces, reflection wrappers, or generic registries for a feature that only has one concrete implementation.
- **Skipping Offline Contracts:** Proceeding directly from Java editing to `./gradlew runServer` or production deployment without running offline contract verifications.
