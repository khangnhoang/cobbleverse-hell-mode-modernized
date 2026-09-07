# Verification Environments & Authority Matrix

This document defines the testing environments for `cobbleverse-hell-mode-modernized` and establishes the authoritative boundary between automated local verification and production canary testing.

---

## The Environment Reality

The local development environment **does not have production parity** with the live multiplayer server.

Specifically:
- The local development machine lacks complete world data, progression save states, full server side-mods, and live player battle contexts.
- Local instances (such as a local CurseForge test client) are useful for isolated smoke checks, but **must never be referred to as "the production environment."**
- The true **Production Host** is the dedicated server running the live multiplayer campaign where the owner and players actively play.

---

## Authority & Responsibility Matrix

| Verification Tier | Scope / Mechanism | Authoritative For | Non-Authoritative For |
| :--- | :--- | :--- | :--- |
| **Tier 1: Local Automated Tests** | - `./gradlew test`<br>- `scripts/ci/validate_repo.py`<br>- `scripts/ci/check_legacy_baseline.py`<br>- `scripts/compat-audit/test_audit.py` | **Authoritative for explicit invariants tested** (unit behavior, datapack JSON schemas, baseline checksums). | Real Fabric Mixin transformation during live gameplay or multiplayer engine compatibility. |
| **Tier 2: Offline Bytecode Contracts** | - `scripts/runtime-contract/test_rct_runtime_contract.py` | **Authoritative for expected production-jar bytecode/call-site assumptions** (method descriptors, LVT slots, slice opcodes in third-party mod JARs). | Runtime bytecode injection conflicts, dynamic classloading issues, or live gameplay integration. |
| **Tier 3: Local Dev Server Smoke** | - `./gradlew runServer` | **Authoritative for startup/Mixin smoke only** (Mixin application, refmap resolution, headless server bootstrap). | **NOT authoritative** for gameplay integration, battle AI feel, or multiplayer campaign progression. |
| **Tier 4: Production Host Canary** | - Deploy to live dedicated server host.<br>- Live NPC battle canary testing by owner. | **Authoritative for live gameplay integration** (real player battle mechanics, progression safety, battle AI feel, multiplayer stability). | Rapid local regression feedback or syntax verification. |

> [!CAUTION]
> **Strict Verification Rule**:
> Passing local automated tests or observing clean `./gradlew runServer` startup does **NOT** warrant a claim that gameplay integration is GREEN.
> Gameplay integration is strictly verified via explicit manual canary testing on the Production Host.


---

## Local Verification Commands

```bash
# 1. Verify legacy baseline immutability
python scripts/ci/check_legacy_baseline.py

# 2. Run repository data validation (datapack syntax & schemas)
python scripts/ci/validate_repo.py

# 3. Run audit regression tests
python -m unittest scripts/compat-audit/test_audit.py -v

# 4. Run offline bytecode contract checks across all third-party mod JARs
python scripts/runtime-contract/test_rct_runtime_contract.py

# 5. Run companion mod unit tests & build
cd companion-mod
./gradlew test --rerun-tasks
./gradlew build
```
