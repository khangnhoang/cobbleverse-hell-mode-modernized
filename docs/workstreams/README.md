# Workstreams Index & Contract

This directory organizes all active, completed, and in-flight engineering workstreams for `cobbleverse-hell-mode-modernized`.

## Workstream Contract

Each workstream directory represents an independent, cohesive initiative (e.g., a major feature, strategic AI behavior, or modernization phase).

A workstream folder adheres to the following layout contract:

```text
docs/workstreams/<workstream-name>/
├── plan.md                     # Canonical implementation plan, requirements, scope, verification strategy
├── owner-review-brief.md       # Owner review briefing, completion report, deliverables, verification record
└── investigations/             # (Optional) Deep-dive discovery, diagnostic traces, spike reports
```

### Source-of-Truth Ownership
- **`plan.md`**: Authoritative specification of the workstream's goals, non-goals, architectural approach, and verification matrix.
- **`owner-review-brief.md`**: Authoritative record of review checkpoints, acceptance evidence, and owner decisions upon delivery.
- **`investigations/`**: Focused technical discovery documents (e.g., runtime bytecode decompilation, live failure triage, diagnostic traces). Investigations inform the plan but do not override it.
- **Cross-workstream decisions**: Durable architectural decisions that span across multiple workstreams are recorded in `docs/decisions/` (ADRs), not buried within a single workstream.
- **Current architecture**: Durable, living system architecture is documented in `docs/architecture/`, reflecting verified production code rather than forward-looking plans.

---

## Existing Workstreams

| Workstream | Status | Key Artifacts | Description |
| :--- | :--- | :--- | :--- |
| **`hell-mode-modernization`** | Completed | [`plan.md`](./hell-mode-modernization/plan.md), [`owner-review-brief.md`](./hell-mode-modernization/owner-review-brief.md) | Phases A–D: Datapack structure audit, baseline freeze, trainer reconciliation (1,714 trainers), deterministic content normalization. |
| **`dynamic-trainer-lead-presets`** | Completed (PR #14) | [`plan.md`](./dynamic-trainer-lead-presets/plan.md) | Mixin injection into RCTMod `makeBattle` enabling deterministic lead selection per trainer via preset tags. |
| **`surge-toxtricity-strategy`** | Completed (PR #15) | [`plan.md`](./surge-toxtricity-strategy/plan.md), [`investigations/`](./surge-toxtricity-strategy/investigations/cp4-live-runtime-failure-discovery.md) | Sound-move prioritization under Throat Spray and CP4 runtime item-state tracker. |
| **`agent-architecture-redesign`** | In-flight | [`plan.md`](./agent-architecture-redesign/plan.md) | Migration to adaptive agent architecture, Universal Lightweight Preflight, progressive disclosure skills, and bounded managed-agent workflows. |
