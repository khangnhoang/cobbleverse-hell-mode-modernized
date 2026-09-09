# Architectural Slicing & Dependency Strategies

This reference defines layer-aware decomposition, dependency ordering, and architectural slicing patterns for complex tasks in Cobbleverse Hell Mode.

---

## 1. Architectural Slicing Principles

1. **Cohesive, Minimal Slices:** Decompose complex multi-system tasks into distinct, reviewable slices with clear responsibilities and minimal blast radius.
2. **Dependency Ordering:** Order slices such that foundational contracts and models are established and verified before dependent logic or boundary adapters are built.
3. **No Monolithic Mega-Diffs:** Avoid commingling governance, data schemas, core logic, and runtime wiring in a single un-sliced diff.
4. **Orthogonal Verification Boundaries (Finding E):** Each slice maps to the minimal orthogonal set of verification layers directly covering the affected contracts. Higher layers do not inherit lower layers automatically.

---

## 2. Generic Architectural Slicing Patterns (Finding D)

For complex multi-system tasks, structure workstreams using these standard architectural slice patterns:

### Slice Pattern 1: Contract & Model Slices
- **Focus:** Define or update public interfaces, data structures, immutable schemas, configuration definitions, or baseline contracts.
- **Characteristics:** Minimal executable logic; pure specifications, types, and schema files.
- **Verification:** Schema validation, contract syntax checks, or documentation route checks.

### Slice Pattern 2: Core Logic & Pure Algorithmic Slices
- **Focus:** Implement standalone calculation routines, decision logic, state machines, or algorithmic transformations.
- **Characteristics:** Pure logic isolated from external platform side-effects or framework lifecycles.
- **Verification:** Fast, deterministic, isolated unit tests covering nominal, boundary, and negative cases.

### Slice Pattern 3: Boundary & Adapter Slices
- **Focus:** Connect verified core logic to external systems, platform event loops, third-party APIs, or injection hooks via adapters or surrogate interfaces.
- **Characteristics:** Minimal business logic; pure translation, event handling, and parameter adaptation.
- **Verification:** Offline integration contracts, bytecode contracts, or boundary tests.

### Slice Pattern 4: Tooling, Governance & Verification Slices
- **Focus:** Automation scripts, CI/CD validation checks, governance contracts, and Verification Evidence Manifest assembly.
- **Characteristics:** Process integrity, documentation alignment, and audit trails.
- **Verification:** Automated verification suite execution, link integrity, and manifest completeness.

---

## 3. Dependency Ordering Invariants

1. **Contract Before Implementation:** Define and freeze component interfaces and contracts before implementing dependent logic.
2. **Isolated Logic Before Boundary Wiring:** Verify core calculation logic in isolation before wiring into platform hooks or runtime injection boundaries.
3. **Non-Breaking Data / Schema Co-Evolution:** Ensure configurations, datapacks, and schemas remain valid during intermediate stages. Migrate schemas with backward compatibility.
4. **Domain Routing:** Generic slicing methodology contains zero game-specific rules. For competitive Doubles team design (weather, Trick Room, pivots, held items), route exclusively to [`competitive-pokemon-doubles-team-design`](../../competitive-pokemon-doubles-team-design/SKILL.md). For other domains, route to matching domain skills or repository evidence.

---

## 4. Anti-Patterns to Avoid

- **The Monolithic PR / Mega-Diff:** Combining governance updates, data schema edits, core algorithms, and platform boundary wiring in a single un-sliced diff.
- **Speculative Abstraction Slicing:** Creating abstract interfaces, reflection wrappers, or generic registries for a feature that only has one concrete implementation.
- **Unverified Boundary Wiring:** Wiring unproven calculations directly into runtime hooks without isolated unit tests.
- **Vertical Ladder Presumption:** Assuming that because higher-level tests pass, lower-level unit contracts or link integrity can be skipped. Always verify the minimal orthogonal set covering the diff.
