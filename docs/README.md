# Documentation Index & Source-of-Truth Routing

Welcome to the documentation for `cobbleverse-hell-mode-modernized`. This repository modernizes the Cobbleverse Hell Mode Double Battle experience, pairing modern datapacks with a native Fabric companion mod (`rct_legendary_rule`) for competitive Doubles AI and mechanics.

## Source-of-Truth (SSOT) Architecture

To prevent stale information, fragmented decision-making, and documentation sprawl, every category of project knowledge has a strictly defined, non-overlapping owner:

```text
docs/
├── README.md               # Documentation entrypoint, taxonomy definitions, and SSOT routing
├── roadmap.md              # High-level sequence of past, current, and upcoming engineering initiatives
├── progress.md             # Authoritative delivery status reflecting actual repository and commit history
├── architecture/           # Durable system architecture of VERIFIED, active production code
│   ├── repository-layout.md
│   ├── companion-mod.md
│   └── fair-ai-information-boundary.md
├── workstreams/            # Per-initiative implementation plans, review briefs, and investigations
│   ├── README.md
│   ├── hell-mode-modernization/
│   ├── dynamic-trainer-lead-presets/
│   └── surge-toxtricity-strategy/
├── operations/             # Environment topology, verification rules, canary, and rollback SOPs
│   ├── verification-environments.md
│   └── production-canary-and-rollback.md
└── decisions/              # Architecture Decision Records (ADRs) for cross-cutting decisions
    └── 0001-scoped-shadow-fair-information-boundary.md
```

---

## Documentation Class Ownership

| Class / Path | Primary Purpose | What It Owns | What It Excludes |
| :--- | :--- | :--- | :--- |
| **`docs/roadmap.md`** | Future & sequence | Strategic ordering of workstreams, broad milestones, program-level dependencies, and explicit exclusions. | Detailed implementation designs, step-by-step coding plans, or tactical task lists. |
| **`docs/progress.md`** | Current reality | Current delivery state of all completed, in-flight, and queued initiatives, mapped to PRs and commits. | Speculative schedules, micro-task tickets, or unapproved owner directions. |
| **`docs/architecture/`** | Living system state | Durable, tested architecture of the repository, companion mod, mixin call-graphs, and data flows. | Speculative features, obsolete designs, or in-flight experimental workstream plans. |
| **`docs/workstreams/`** | Tactical initiatives | Workstream-specific `plan.md`, `owner-review-brief.md`, and technical `investigations/`. | Cross-cutting architectural policies (which belong in `architecture/` or `decisions/`). |
| **`docs/operations/`** | Procedures & testing | Authoritative environments definition (local vs. prod host), verification contracts, canary testing, rollback. | Code architecture, battle logic details, or strategy algorithms. |
| **`docs/decisions/`** | Architectural choices | Architecture Decision Records (ADRs) capturing context, alternatives, trade-offs, and final decisions. | Routine bug fixes, transient task notes, or isolated per-trainer data edits. |
| **`AGENTS.md`** | Agent guidelines | Engineering principles, surgical scope rules, git safety, Vietnamese reporting rules, skill routing. | Domain methodology (lives in `.agents/skills/`) or project documentation. |

---

## Navigating the Documentation

1. **Understanding the overall project status?** Read [`docs/progress.md`](./progress.md).
2. **Checking what's next on the horizon?** Consult [`docs/roadmap.md`](./roadmap.md).
3. **Exploring the companion mod or anti-cheat AI boundary?** Review [`docs/architecture/`](./architecture/).
4. **Running tests, verifying locally, or canary deploying?** See [`docs/operations/`](./operations/).
5. **Contributing or pairing with AI agents?** Read the root [`AGENTS.md`](../AGENTS.md).
