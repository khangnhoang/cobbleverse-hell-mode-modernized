# RCT Legendary Rule Companion Mod (Phase G)

A minimal, server-only Fabric companion mod that enforces a competitive restriction on RCT trainer battles:
**A player's active party may contain at most 1 Legendary or Mythical Pokémon.**

---

## 1. Architectural Overview

- **Server-Only:** Zero client-side installation required. Clients running standard Cobblemon/Cobbleverse can connect without this mod installed.
- **Pure Rule Enforcement:**
  - `restrictedCount <= 1` $ightarrow$ Admission proceeds through native RCT validation.
  - `restrictedCount >= 2` $ightarrow$ Admission is rejected cleanly before any battle state or entity initialization occurs.
- **Non-Invasive Dialog Composition:**
  - If rejected due to having $\ge 2$ Legendaries/Mythicals, the trainer displays:
    `§cTrainer rules permit at most 1 Legendary or Mythical Pokémon!`
  - Native RCT dialogue and rejections (level-cap gating, cooldowns, missing series/trainer progression, post-defeat text) are 100% preserved.
- **Native Cobblemon Detection:**
  - Evaluates `Pokemon.isLegendary()` and `Pokemon.isMythical()` on active party members (`Cobblemon.INSTANCE.getStorage().getParty(player)`).
  - Automatically respects form labels (e.g. Megas, Primals, Fusions).
  - Evaluates active roster slots (including fainted members); excludes boxed PC storage.

---

## 2. Build Requirements & Instructions

- **Java Version:** JDK 21
- **Build System:** Gradle 8.10+ / Fabric Loom 1.7+

### Dependency Configuration
This mod compiles against the exact runtime stack installed in the target modpack:
- Minecraft `1.21.1`
- Fabric Loader `0.16.9+`
- Cobblemon `1.7.3`
- RCT `0.18.1-beta`
- RCT API `0.15.2-beta`

Because RCT and local Cobblemon builds are packaged in the local modpack instance, Gradle resolves them from the local directory defined by either:
1. Gradle property: `-PcobbleverseModsDir=<path_to_mods_folder>`
2. Environment variable: `COBBLEVERSE_MODS_DIR`
3. Default fallback: `c:/Users/khang/curseforge/minecraft/Instances/COBBLEVERSE - Pokemon Adventure [Cobblemon]/mods`

### Building the JAR
From the `companion-mod/` directory:
```bash
gradle build
```
The deployable server JAR will be produced at:
```
companion-mod/build/libs/rct-legendary-rule-companion-1.0.0.jar
```
