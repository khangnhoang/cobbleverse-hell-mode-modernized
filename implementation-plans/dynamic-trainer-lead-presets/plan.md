# Dynamic Trainer Lead Selection Presets (Master Plan)

Implement a server-side dynamic lead selection system for Cobbleverse Hell Mode boss trainers. Prior to battle instantiation, the companion mod inspects the player's effective active leads, evaluates a deterministic type-based heuristic against human-authored `LeadAttempt` presets, and deploys the optimal counter-lead pair for that battle instance without mutating global trainer data.

---

## User Review Required

> [!IMPORTANT]
> **Zero Global Mutation Guarantee (Proven in A2 & A3):**
> Decompilation of RCTMod 0.18.1 confirms `TrainerRegistry` maintains a shared singleton `TrainerNPC` per trainer ID. Intercepting `RCTMod.makeBattle` via a narrow `@ModifyVariable` injector allows us to clone the instance via `new TrainerNPC(singleton)` and apply the reordered roster into `perBattleNPC.getTeam()` via `System.arraycopy`. The global singleton cache remains 100% immutable across all players and battles.

> [!NOTE]
> **Effective Player Lead Resolution (Proven in A4):**
> In Cobblemon 1.7.3 Doubles, `BattleManager.toBattlePokemons` skips fainted Pokémon. If a player enters battle with fainted Pokémon in leading slots, the engine automatically resolves the first two Pokémon where `!pokemon.isFainted()`. If only 1 Pokémon is conscious, the scorer adapts to evaluate against a single effective lead.

> [!TIP]
> **Strict Pure Domain vs. Infrastructure / Composition Boundaries:**
> Pure domain classes (`TypeChartData`, `TypeMatchupScorer`, `LeadSelectionEngine`, `LeadSelectionResult`, `AttemptScore`, `RosterOrderer`, `LeadAttempt`, `ExpectedLeadMember`, `PokemonIdentity`, `RosterMemberTyping`, `PlayerLeadTyping`) have zero dependencies on Minecraft, Fabric, Cobblemon, RCT, Gson, or logging frameworks, and are 100% unit-testable in JUnit 5. Configuration storage and resource loading (`LeadSelectionConfig`, `TypeChartResourceLoader`) reside in runtime-independent infrastructure. Runtime composition and Mixin adapters own trainer ID lookups, trainer-bound validation, logging, and fallback decisions.

---

## Local Runtime Investigation Findings

### A1. Exact RCTMod Interception Target
- **Target Class:** `com.gitlab.srcmc.rctmod.api.RCTMod`
- **Target Method:** `public boolean makeBattle(com.gitlab.srcmc.rctmod.world.entities.TrainerMob mob, net.minecraft.entity.player.PlayerEntity player)`
- **Intermediary Descriptor:** `(Lcom/gitlab/srcmc/rctmod/world/entities/TrainerMob;Lnet/minecraft/class_1657;)Z`
- **Call Chain:** `TrainerMob.startBattleWith(PlayerEntity)` $\rightarrow$ passes cooldowns & validation $\rightarrow$ delegates to `RCTMod.getInstance().makeBattle(this, player)`.

### A2. Concrete Mixin Interception Mechanism (Frozen Decision)
Inside `RCTMod.makeBattle`:
1. Lines 28–41 retrieve the trainer:
   - Bytecode offset 35: `invokevirtual TrainerRegistry.getById:(Ljava/lang/String;Ljava/lang/Class;)Lcom/gitlab/srcmc/rctapi/api/trainer/Trainer;` (passing `TrainerNPC.class`)
   - Bytecode offset 38: `checkcast com/gitlab/srcmc/rctapi/api/trainer/TrainerNPC`
   - Bytecode offset 41: `astore 5` (stored in local variable slot 5 `trNPC`)
2. Lines 122–157 configure and start the battle:
   - `trNPC.setEntity(mob);`
   - `BattleManager.startBattle(List.of(trPlayer), List.of(trNPC), format, rules);`
   - `TBCSCompat.registerWinCommands(..., Map.of(trPlayer, player, trNPC, mob), battleId);`

**Frozen Mixin Injector:**
- **Target Owner:** `com.gitlab.srcmc.rctmod.api.RCTMod`
- **Target Method:** `makeBattle(Lcom/gitlab/srcmc/rctmod/world/entities/TrainerMob;Lnet/minecraft/class_1657;)Z`
- **Target Invocation for preceding `getById`:** `getById(Ljava/lang/String;Ljava/lang/Class;)Lcom/gitlab/srcmc/rctapi/api/trainer/Trainer;` on `com.gitlab.srcmc.rctapi.api.trainer.TrainerRegistry`
- **Chosen Injector Annotation:** `@ModifyVariable`
- **Injection Point:** `@At(value = "STORE", ordinal = 0)` targeting the first store of type `com.gitlab.srcmc.rctapi.api.trainer.TrainerNPC` (bytecode offset 41, `astore 5`).
- **Handler Signature:**
  ```java
  @ModifyVariable(
      method = "makeBattle(Lcom/gitlab/srcmc/rctmod/world/entities/TrainerMob;Lnet/minecraft/class_1657;)Z",
      at = @At(value = "STORE", ordinal = 0),
      ordinal = 0
  )
  private TrainerNPC modifyTrainerNPC(TrainerNPC original, TrainerMob mob, PlayerEntity player)
  ```
- **Argument Availability:** Mixin automatically captures the enclosing method's parameters `TrainerMob mob` and `PlayerEntity player` by appending them as trailing parameters to the handler method.
- **Why `@ModifyVariable` is Chosen Over `@Redirect`:**
  - `@Redirect` replaces the `INVOKEVIRTUAL` call site of `TrainerRegistry.getById`. Since `makeBattle` calls `getById` twice (first for `TrainerPlayer.class`, then for `TrainerNPC.class`), a redirector requires fragile ordinal/slice discrimination or runtime type branching, and easily conflicts with any other mod inspecting registry lookups.
  - `@ModifyVariable` is the narrowest possible value substitution: it intercepts local variable 5 (`trNPC`) immediately upon assignment, leaving method invocation opcodes untouched.
  - Once substituted, all downstream consumers (`trNPC.setEntity(mob)`, `BattleManager.startBattle`, and `TBCSCompat.registerWinCommands`) consistently receive the per-battle cloned instance.
- **Unconfigured Trainers & Failure Fallbacks:** When a trainer has no authored presets, lead selection is disabled, or a validation/resource failure occurs, the interceptor immediately returns the `original` singleton. This ensures **native-path preservation with minimal interceptor overhead** (no player-party scan, no scoring, no cloning, no roster rewrite, original singleton returned, native RCT battle behavior preserved).

### A3. `TrainerNPC` Cloning & Roster Mutation Semantics
- **Copy Constructor:** `public TrainerNPC(TrainerNPC other)` exists.
  - Deep-copies `Pokemon[] team` using `copyTeam(Pokemon[] team)`: allocates `new Pokemon[team.length]` and for each element instantiates `new Pokemon()` and invokes `copyFrom(old)`.
  - Deep-copies `gimmicks` (`new TrainerNPC.GimmicksMap(other.gimmicks)`).
  - Copies `bag`, `battleTheme`, `battleAI`, and `entity`.
- **Team Container:** `private Pokemon[] team;`.
- **Getter / Setter:** `public Pokemon[] getTeam()` returns the internal array directly; no `setTeam` setter exists.
- **Safe Application Flow:**
  ```java
  TrainerNPC perBattleNPC = new TrainerNPC(singleton);
  Pokemon[] reordered = RosterOrderer.reorder(perBattleNPC.getTeam(), selectedAttempt.leadSlots(), Pokemon[]::new);
  System.arraycopy(reordered, 0, perBattleNPC.getTeam(), 0, reordered.length);
  ```
  This overwrites the internal array of `perBattleNPC` with zero reflection, zero field modification, and zero contamination of the global singleton.

### A4. Player Battle Admission & Active Lead Deployment
- `BattleManager.toBattlePokemons` iterates the player's team and filters with `!pokemon.isFainted()`.
- **$\ge 2$ Conscious Pokémon:** The first two conscious Pokémon become active field leads; scoring evaluates all 4 pairwise interactions ($2 \times 2$).
- **$1$ Conscious Pokémon:** Only 1 Pokémon deploys to the field; scoring evaluates 2 pairwise interactions ($2 \times 1$).
- **$0$ Conscious Pokémon:** Native Cobblemon validator throws `NoHealthyPokemonError` / aborts battle start before deployment.

### A5. Proven Lifecycle vs. Proposed Config Architecture
- **Proven Current State:**
  - `LegendaryRuleMod.onInitializeServer()` runs during dedicated server bootstrap.
  - `CompanionConfig.init()` resolves config path using `FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILENAME)` with fallback to `Path.of("config", CONFIG_FILENAME)`.
  - Proven Gson-based config lifecycle with try/catch error logging and default fallbacks.
- **Proposed Feature Architecture:**
  - `LeadSelectionConfig` and `TypeChartResourceLoader` follow the same bootstrap lifecycle, initialized during `LegendaryRuleMod.onInitializeServer()`.
  - New configuration file `config/cobbleverse-hell-mode-leads.json` resolved via the Fabric config directory.
  - Runtime-independent infrastructure classes (`LeadSelectionConfig`, `TypeChartResourceLoader`) have no Minecraft/Fabric/Cobblemon/RCT dependencies (relying exclusively on standard Java and Gson).
  - Pure domain core (`TypeChartData`, `TypeMatchupScorer`, `LeadSelectionEngine`, `LeadSelectionResult`, `AttemptScore`, `RosterOrderer`, `LeadAttempt`, `ExpectedLeadMember`, `PokemonIdentity`, `RosterMemberTyping`, `PlayerLeadTyping`) has zero external dependencies (no Gson, no Minecraft, no Fabric, no Cobblemon, no RCT, no SLF4J).

### A6. Grounded Pilot Trainer Content (`kanto_sabrina.json`)
Inspection of `pack/data/rctmod/trainers/kanto_sabrina.json` verified her actual 6-Pokémon team:
- Slot 0: `indeedee` (Psychic/Normal, female) — Psychic Surge, Follow Me, Helping Hand
- Slot 1: `farigiraf` (Normal/Psychic) — Armor Tail, Expanding Force
- Slot 2: `hatterene` (Psychic/Fairy) — Magic Bounce, Dazzling Gleam, Expanding Force
- Slot 3: `metagross` (Steel/Psychic) — Clear Body, Assault Vest, Meteor Mash
- Slot 4: `armarouge` (Fire/Psychic) — Flash Fire, Armor Cannon, Wide Guard, Expanding Force
- Slot 5: `alakazam` (Psychic) — Mega Alakazite, Trace, Expanding Force, Dazzling Gleam
*(Note: Incineroar is not present on Sabrina's team; previous mentions were placeholder draft examples. The pilot is now grounded in her exact roster).*

### A7. Cobblemon Type System, Parity Verification & Decoupled Resource Architecture
- Cobblemon delegates all in-battle damage and type calculations to Pokémon Showdown JS bundled at `data/cobblemon/showdown.zip` (`data/typechart.js`) via Truffle JS. Cobblemon exposes no public Java `TypeChart` calculator.
- **Canonical Resource Solution:** A single canonical data resource `companion-mod/src/main/resources/typechart_gen9.json` is packaged with the companion mod.
- **Decoupled Architecture:**
  - `TypeChartResourceLoader` (infrastructure) reads `/typechart_gen9.json` via Gson and returns `Optional<TypeChartData>`.
  - `TypeChartData` (pure domain) holds the immutable 18x18 directed multiplier matrix.
  - `TypeMatchupScorer` (pure domain service) receives `TypeChartData` via constructor and performs purely mathematical evaluations without I/O or logging.
- **Resource Failure Semantics:**
  - If `/typechart_gen9.json` is missing, unreadable, or fails to parse during server bootstrap:
    1. The exception is caught cleanly inside `TypeChartResourceLoader`; server startup and battle initialization do NOT crash.
    2. `TypeChartResourceLoader.loadDefault()` returns `Optional.empty()`.
    3. A clear high-severity error is logged by the bootstrap composition:
       `[HellMode-Lead] ERROR: Failed to load type chart resource '/typechart_gen9.json'. Dynamic lead selection is DISABLED. Preserving native trainer ordering.`
    4. Dynamic lead selection is marked unavailable at the composition level.
    5. The composition will **never** construct or use a scorer with fabricated neutral data.
    6. Native trainer ordering and battle startup proceed with 100% stability.
- **Parity Test (`scripts/runtime-contract/test_typechart_parity.py`):**
  1. Unzips `data/cobblemon/showdown.zip` from `Cobblemon-fabric-1.7.3+1.21.1.jar` and parses Showdown's `data/typechart.js`.
  2. Reads the exact production resource `companion-mod/src/main/resources/typechart_gen9.json`.
  3. Compares all 18x18 = 324 directed pairs $(att, def)$ between Showdown and the production JSON.
  4. Test fails if the production resource diverges from Showdown in any way.
- This eliminates any second handwritten 18x18 matrix in Python: production Java and the contract test observe the exact same JSON source of truth.

---

## Architectural Workflow

```text
Server Initialization (LegendaryRuleMod.onInitializeServer)
                    │
                    ├─ 1. Load LeadSelectionConfig (Gson)
                    ├─ 2. Load /typechart_gen9.json via TypeChartResourceLoader
                    │     Success ──► TypeChartData ──► TypeMatchupScorer ──► LeadSelectionEngine (ACTIVE)
                    │     Failure ──► Log error ──► Mark LeadSelectionEngine UNAVAILABLE
                    ▼
Player challenges Trainer / Trainer aggros Player
                    │
                    ▼
         TrainerMob.startBattleWith()
                    │
                    ▼
         TrainerMob.canBattleAgainst()
         (Passes: level cap, cooldowns, legendary limit)
                    │
                    ▼
              RCTMod.makeBattle()
                    │
                    ├─ 1. Query Runtime Composition & Config Status:
                    │     Engine ACTIVE AND Trainer configured & enabled?
                    │     No ──► Return original singleton (native-path preservation with minimal overhead)
                    │     Yes
                    │      │
                    ├─ 2. Resolve Effective Player Leads:
                    │     Iterate player party ──► collect first 2 where !isFainted()
                    │     Count == 0 ──► Return original singleton (native abort)
                    │     Convert to List<PlayerLeadTyping>
                    │      │
                    ├─ 3. Validate Candidate Presets (Trainer-Bound Validation):
                    │     For each LeadAttempt:
                    │       Check slots < team.length
                    │       Verify expectedLeadMembers match team[slot] via CobblemonLeadAdapter
                    │     Any violation ──► Filter out attempt / log warning
                    │     No valid attempts remaining ──► Return original singleton
                    │      │
                    ├─ 4. Evaluate Pure Domain Engine:
                    │     LeadSelectionResult result = engine.select(validAttempts, playerLeads, npcRosterTyping)
                    │     Runtime Composition logs result evidence:
                    │       LOGGER.debug("[HellMode-Lead] Trainer={}, Selected={}, Scores={}", ...)
                    │      │
                    ├─ 5. Clone & Reorder:
                    │     perBattleNPC = new TrainerNPC(singleton)
                    │     RosterOrderer.reorder(perBattleNPC.getTeam(), result.selectedAttempt().leadSlots(), Pokemon[]::new)
                    │     System.arraycopy into perBattleNPC.getTeam()
                    │
                    ▼
      BattleManager.startBattle(player, perBattleNPC, format, rules)
                    │
                    ▼
     Cobblemon deploys Slot 0 & Slot 1 to field
                    │
                    ▼
     Run & Bun AI ("rb") takes over normal Turn 1+ decision making
```

---

## Detailed Design Contracts (B1 – B10)

| ID | Contract Name | Specification |
|---|---|---|
| **B1** | **Authored Presets Only** | Engine only evaluates author-defined `leadSlots` pairs. It never synthesizes combinations not explicitly declared. |
| **B2** | **Offensive Scoring** | For each NPC lead, find the maximum effectiveness multiplier of its STAB types against each active player lead. Score mapped: `4.0x` $\rightarrow +4$, `2.0x` $\rightarrow +2$, `1.0x` $\rightarrow 0$, `0.5x` $\rightarrow -1$, `0.25x` $\rightarrow -2$, `0.0x` $\rightarrow -4$. |
| **B3** | **Defensive Scoring** | For each active player lead, find the maximum effectiveness multiplier of its STAB types against each NPC lead's typing. Score mapped from NPC perspective: `4.0x` $\rightarrow -4$, `2.0x` $\rightarrow -2$, `1.0x` $\rightarrow 0$, `0.5x` $\rightarrow +1$, `0.25x` $\rightarrow +2$, `0.0x` $\rightarrow +4$. |
| **B4** | **Strictly Validated Base Weight** | `baseWeight` must be an integer within `[-2, +2]`. Out-of-range values FAIL structural validation (they are NOT clamped) and trigger safe native-order fallback according to the config-validation policy. |
| **B5** | **Deterministic Tie-Breaking** | Winner chosen by: `Total Score` (descending) $\rightarrow$ `baseWeight` (descending) $\rightarrow$ `authored declaration order` (first in JSON). |
| **B6** | **Directed Matchup Evaluation** | Type evaluations are directed (e.g. Ground attacking Flying is 0x, Flying attacking Ground is 1x). Dual typings multiply multipliers before score mapping. |
| **B7** | **Two-Phase Slot Validation** | **Phase 1 (Structural):** Exactly 2 slots, slots distinct, each slot $\ge 0$, baseWeight $\in [-2, +2]$ (no team size upper bound hardcoded).<br>**Phase 2 (Runtime):** Each slot $< \text{resolved team.length}$ and `expectedLeadMembers` match actual `PokemonIdentity`. Mismatch triggers safe native fallback. |
| **B8** | **Native Path Preservation** | Unconfigured trainers, disabled configs, or resource/validation failures skip party scanning, scoring, and cloning completely. Native singleton is preserved with minimal interceptor overhead. |
| **B9** | **RosterOrderer Invariants** | Given array of size $N$ and `leadSlots: [A, B]`: index 0 is element $A$, index 1 is element $B$, remaining $N-2$ elements maintain their original relative order. Pure function; original array unmodified. |
| **B10** | **Semantic Drift Guard (`expectedLeadMembers`)** | Each `LeadAttempt` declares `expectedLeadMembers` using explicit canonical objects. If a datapack update changes roster slots without updating config, the guard rejects the preset and falls back to native order. |

---

## Scoring Formula & Math Specification

Given:
- NPC Lead Attempt with Pokémon $N_1, N_2$
- Active Player Leads $P_1, \dots, P_k$ ($k \in \{1, 2\}$)

### 1. Offensive STAB Score
For each NPC lead $N \in \{N_1, N_2\}$ and player lead $P \in \{P_1, \dots, P_k\}$:
$$\text{BestSTAB}(N \to P) = \max_{t \in \text{STAB}(N)} \text{Multiplier}(t, P)$$
$$\text{Offense}(N_1, N_2) = \sum_{N} \sum_{P} \text{ScoreMap}(\text{BestSTAB}(N \to P))$$

### 2. Defensive Resistance Score
For each player lead $P \in \{P_1, \dots, P_k\}$ and NPC lead $N \in \{N_1, N_2\}$:
$$\text{WorstThreat}(P \to N) = \max_{u \in \text{STAB}(P)} \text{Multiplier}(u, N)$$
$$\text{Defense}(N_1, N_2) = \sum_{N} \sum_{P} \text{DefScoreMap}(\text{WorstThreat}(P \to N))$$

### 3. Total Score
$$\text{TotalScore} = \text{Offense}(N_1, N_2) + \text{Defense}(N_1, N_2) + \text{baseWeight}$$

---

## Proposed Code Structure & Changes

### Component 1: Pure Domain Core (`companion-mod/src/main/java/com/cobbleverse/legendaryrule/lead/`)

Zero-dependency classes with NO Minecraft, Fabric, Cobblemon, RCT, Gson, or logging imports:

#### [NEW] `PokemonIdentity.java`
- Pure domain representation of observed Pokémon identity:
  - `String species`: lowercase species name (e.g. `"indeedee"`, `"tauros"`, `"rotom"`)
  - `String form`: optional lowercase form name (nullable, e.g. `"f"`, `"alola"`, `"wash"`)
  - `Set<String> aspects`: set of lowercase aspect strings (e.g. `{"female"}`, `{"alolan"}`)

#### [NEW] `ExpectedLeadMember.java`
- Immutable record/class representing canonical authored lead expectation:
  - `String species`: required lowercase species identifier
  - `String form`: optional lowercase form identifier (nullable)
  - `List<String> requiredAspects`: optional list of lowercase aspect strings (nullable/empty)
- Pure matching method:
  `public boolean matches(PokemonIdentity actual)`
  - Species check: `actual.species() != null && actual.species().equalsIgnoreCase(this.species)`
  - Form check (if `form != null`): `actual.form() != null && actual.form().equalsIgnoreCase(this.form)`
  - Aspect check (if `requiredAspects != null && !requiredAspects.isEmpty()`): `actual.aspects() != null && actual.aspects().containsAll(this.requiredAspects)`
- *Documented API Limitation:* Held item, moves, IV/EV, and mutable battle state are excluded to keep validation deterministic and lightweight; species + form/aspect provides the strongest stable authored identity available without runtime overhead.

#### [NEW] `LeadAttempt.java`
- Immutable record/class:
  - `String id`
  - `int[] leadSlots` (length 2, each $\ge 0$, distinct)
  - `int baseWeight` (strictly validated $\in [-2, +2]$, not clamped)
  - `List<ExpectedLeadMember> expectedLeadMembers` (length 2 when present)

#### [NEW] `TypeChartData.java`
- Pure immutable domain representation of the 18x18 directed type chart:
  - `double getMultiplier(String attackType, String defenseType)`
  - Encapsulates type advantage values directly. Zero external dependencies.

#### [NEW] `TypeMatchupScorer.java`
- Pure domain service:
  - Constructor: `public TypeMatchupScorer(TypeChartData typeChart)`
  - `double getEffectiveness(String attackType, List<String> defenderTypes)`
  - `int mapOffensiveScore(double multiplier)`
  - `int mapDefensiveScore(double multiplier)`
  - Evaluates outgoing best STAB and incoming worst STAB using its encapsulated `TypeChartData`.
  - Zero classpath resource loading, zero logging, zero static state.

#### [NEW] `PlayerLeadTyping.java` & `RosterMemberTyping.java`
- Pure domain representations of lead typing inputs:
  - `PlayerLeadTyping(String species, List<String> types)`
  - `RosterMemberTyping(int slot, String species, List<String> stabTypes)`

#### [NEW] `AttemptScore.java` & `LeadSelectionResult.java`
- Pure immutable evaluation evidence objects returned by `LeadSelectionEngine`:
  - `AttemptScore(String attemptId, int offensiveScore, int defensiveScore, int baseWeight, int totalScore)`
  - `LeadSelectionResult(LeadAttempt selectedAttempt, List<AttemptScore> evaluatedScores)`

#### [NEW] `LeadSelectionEngine.java`
- Pure domain service orchestrating candidate attempt evaluation:
  - Constructor: `public LeadSelectionEngine(TypeMatchupScorer scorer)`
  - Evaluation signature:
    `public LeadSelectionResult select(List<LeadAttempt> attempts, List<PlayerLeadTyping> playerLeads, List<RosterMemberTyping> npcRoster)`
  - Does NOT import `LeadSelectionConfig`, Gson, Fabric, Minecraft, Cobblemon, RCT, or Logger.
  - Does NOT perform trainer ID lookups or emit log messages.
  - Scores valid candidate attempts, resolves deterministic tie-breaking (Score $\rightarrow$ baseWeight $\rightarrow$ declaration order), and returns `LeadSelectionResult`.

#### [NEW] `RosterOrderer.java`
- Pure generic utility method:
  `public static <T> T[] reorder(T[] original, int[] leadSlots, java.util.function.IntFunction<T[]> generator)`
- Guarantees lead slots at indices 0 and 1, stable relative ordering for backline. Pure function; input array unmodified.

---

### Component 2: Runtime-Independent Infrastructure (`companion-mod/src/main/java/com/cobbleverse/legendaryrule/lead/`)

Classes relying exclusively on standard Java and Gson (no Minecraft/Fabric/Cobblemon/RCT dependencies):

#### [NEW] `TypeChartResourceLoader.java`
- Infrastructure loader for the type chart JSON:
  - `public static Optional<TypeChartData> loadDefault()` (loads `/typechart_gen9.json` via ClassLoader)
  - `public static Optional<TypeChartData> loadFromStream(InputStream stream)`
  - Parses JSON into immutable `TypeChartData`.
  - On missing, unreadable, or malformed resource: logs clear error and returns `Optional.empty()` without throwing unhandled exceptions.

#### [NEW] `LeadSelectionConfig.java`
- Runtime-independent config manager:
  - Loads `config/cobbleverse-hell-mode-leads.json`.
  - Owns trainer ID lookup: `Optional<TrainerLeadConfig> getTrainerConfig(String trainerId)`.
  - Structural validation (Phase 1): `leadSlots.length == 2`, `leadSlots[0] != leadSlots[1]`, each slot $\ge 0$, `baseWeight \in [-2, +2]`.
  - Safe native-order fallback on malformed inputs or out-of-range weights.

---

### Component 3: Fabric Runtime Integration & Composition (`companion-mod/src/main/java`)

#### [NEW] `com.cobbleverse.legendaryrule.lead.adapter.CobblemonLeadAdapter.java`
- Converts Cobblemon `Pokemon` to pure domain `PokemonIdentity`:
  ```java
  public static PokemonIdentity toIdentity(Pokemon pokemon) {
      String species = pokemon.getSpecies().getName().toLowerCase(Locale.ROOT);
      String form = pokemon.getForm() != null ? pokemon.getForm().getName().toLowerCase(Locale.ROOT) : null;
      Set<String> aspects = pokemon.getAspects().stream()
          .map(s -> s.toLowerCase(Locale.ROOT))
          .collect(Collectors.toSet());
      return new PokemonIdentity(species, form, aspects);
  }
  ```
- Extracts primary and secondary type strings safely for scoring.

#### [NEW] `com.cobbleverse.legendaryrule.lead.PlayerLeadResolver.java`
- Receives `ServerPlayerEntity`.
- Queries `Cobblemon.INSTANCE.getStorage().getParty(player)`.
- Collects first 2 conscious Pokémon (`!pokemon.isFainted()`).
- Uses `CobblemonLeadAdapter` to convert to `List<PlayerLeadTyping>`.

#### [NEW] `com.cobbleverse.legendaryrule.lead.LeadSelectionService.java`
- Runtime composition orchestrator owning:
  1. Trainer ID lookup in `LeadSelectionConfig`.
  2. Trainer-bound runtime validation: ensures `leadSlots < team.length` and validates `expectedLeadMembers` against `team[slot]` via `CobblemonLeadAdapter`.
  3. Invoking `LeadSelectionEngine.select(validAttempts, playerLeads, npcRosterTyping)`.
  4. Structured logging of `LeadSelectionResult`:
     `LOGGER.debug("[HellMode-Lead] Trainer={}, PlayerLeads={}, Scores={}, Selected={}", trainerId, playerLeads, result.evaluatedScores(), result.selectedAttempt().id());`
  5. Fallback decisions: returns empty if unconfigured, disabled, or validation fails.

#### [NEW] `com.cobbleverse.legendaryrule.mixin.RCTModMakeBattleMixin.java`
- Mixin targeting `com.gitlab.srcmc.rctmod.api.RCTMod.makeBattle(TrainerMob, PlayerEntity)`.
- Exact injector: `@ModifyVariable(method = "makeBattle(Lcom/gitlab/srcmc/rctmod/world/entities/TrainerMob;Lnet/minecraft/class_1657;)Z", at = @At(value = "STORE", ordinal = 0), ordinal = 0)`.
- Intercepts local variable 5 (`TrainerNPC trNPC`).
- Delegates to `LeadSelectionService`:
  - If service returns empty (unconfigured, disabled, or failure): returns `original` directly (native-path preservation with minimal interceptor overhead).
  - If preset selected:
    1. Clones `new TrainerNPC(singleton)`.
    2. Reorders `perBattleNPC.getTeam()` via `RosterOrderer`.
    3. Overwrites via `System.arraycopy`.
    4. Returns `perBattleNPC`, which is stored in slot 5 and passed to `setEntity`, `startBattle`, and `registerWinCommands`.

#### [MODIFY] `com.cobbleverse.legendaryrule.LegendaryRuleMod.java`
- Service composition during `onInitializeServer()`:
  1. Initializes `LeadSelectionConfig`.
  2. Invokes `TypeChartResourceLoader.loadDefault()`.
  3. If present: constructs `TypeMatchupScorer`, `LeadSelectionEngine`, and initializes `LeadSelectionService` (active).
  4. If absent: logs error and marks `LeadSelectionService` inactive/bypassed (preserving native ordering; never constructs a scorer with fabricated neutral data).

---

### Component 4: Pilot Configuration (`config/cobbleverse-hell-mode-leads.json`)

Single canonical object schema for `expectedLeadMembers`:

```json
{
  "enabled": true,
  "trainers": {
    "kanto_sabrina": {
      "attempts": [
        {
          "id": "psychic_terrain_blitz",
          "leadSlots": [0, 5],
          "expectedLeadMembers": [
            { "species": "indeedee", "form": "f", "requiredAspects": ["female"] },
            { "species": "alakazam" }
          ],
          "baseWeight": 1,
          "description": "Psychic Surge + Follow Me with fast Mega Alakazam Expanding Force"
        },
        {
          "id": "anti_dark_priority_block",
          "leadSlots": [1, 2],
          "expectedLeadMembers": [
            { "species": "farigiraf" },
            { "species": "hatterene" }
          ],
          "baseWeight": 0,
          "description": "Armor Tail blocks priority moves; Hatterene Fairy typing counters Dark"
        },
        {
          "id": "heavy_steel_offense",
          "leadSlots": [0, 3],
          "expectedLeadMembers": [
            { "species": "indeedee", "form": "f", "requiredAspects": ["female"] },
            { "species": "metagross" }
          ],
          "baseWeight": -1,
          "description": "Redirection + Assault Vest Metagross bulky physical coverage"
        }
      ]
    }
  }
}
```

---

### Component 5: Test Suites & Verification Tools

#### Pure JUnit 5 Tests (`companion-mod/src/test/java/com/cobbleverse/legendaryrule/lead/`)
1. `TypeChartResourceLoaderTest.java`: Validates successful loading from valid JSON stream, returns `Optional.empty()` on missing resource, and handles corrupted JSON without crashing.
2. `TypeMatchupScorerTest.java`: Receives deterministic in-memory `TypeChartData` and tests multiplier mappings, dual types, immunities, and offensive/defensive scoring strictly in memory (no I/O).
3. `LeadSelectionEngineTest.java`: Tests pure domain `LeadSelectionEngine.select(...)` passing domain attempts and player/NPC typing inputs directly; validates scoring calculations, deterministic tie-breaking (Score $\rightarrow$ baseWeight $\rightarrow$ declaration order), single-lead player cases, and `LeadSelectionResult` evidence structure with zero I/O, zero config, and zero logging.
4. `ExpectedLeadMemberTest.java`: Validates pure `ExpectedLeadMember.matches(PokemonIdentity)` matching logic across species, forms, and required aspects with zero Cobblemon dependencies.
5. `RosterOrdererTest.java`: Validates immutability of inputs, exact lead slot positioning, and stable preservation of remaining roster order.
6. `LeadSelectionConfigTest.java`: Tests structural JSON parsing, validation (rejection of duplicate slots, out-of-bound weights, slots $\ge 0$), and trainer ID lookup.
7. `LeadSelectionServiceTest.java`: Tests composition boundary: config lookup $\rightarrow$ trainer-bound validation $\rightarrow$ engine invocation $\rightarrow$ logging $\rightarrow$ fallback/native-path preservation.

#### Offline Contract Tests (`scripts/runtime-contract/`)
1. [NEW] `test_rct_runtime_contract.py`: Offline bytecode verification of `RCTMod.makeBattle`, `TrainerNPC(TrainerNPC)`, `BattleManager.startBattle`, and `Pokemon` type/form/species accessors.
2. [NEW] `test_typechart_parity.py`: Extracts `data/typechart.js` from Cobblemon's `data/cobblemon/showdown.zip` and verifies 100% parity across all 324 directed type interactions against the production source-of-truth resource `companion-mod/src/main/resources/typechart_gen9.json`.

---

## Verification Plan

### Automated Tests
1. **JUnit 5 Suite:**
   ```powershell
   cd companion-mod
   .\gradlew test --info
   ```
2. **Bytecode & TypeChart Parity Tests:**
   ```powershell
   python scripts/runtime-contract/test_rct_runtime_contract.py
   python scripts/runtime-contract/test_typechart_parity.py
   ```
3. **Repository CI Sanity:**
   ```powershell
   python scripts/ci/check_legacy_baseline.py
   python scripts/ci/validate_repo.py
   python -m unittest scripts/compat-audit/test_audit.py -v
   ```

### Manual / Staging Scenarios
- **Scenario 1 (Neutral / Fighting leads):** Challenge Sabrina with Machamp + Lucario $\rightarrow$ Engine deploys `psychic_terrain_blitz` (Indeedee-F + Alakazam).
- **Scenario 2 (Dark / Ghost leads):** Challenge Sabrina with Tyranitar + Weavile $\rightarrow$ Engine deploys `anti_dark_priority_block` (Farigiraf + Hatterene).
- **Scenario 3 (Fainted lead in party slot 0):** Enter battle with fainted slot 0 $\rightarrow$ Engine correctly identifies slot 1 and slot 2 as active leads.
- **Scenario 4 (Unconfigured trainer):** Challenge Brock or Misty $\rightarrow$ Native singleton preserved with minimal interceptor overhead.
- **Scenario 5 (Type chart resource failure):** Simulate missing `/typechart_gen9.json` $\rightarrow$ Composition logs error, dynamic lead selection is marked unavailable, and native trainer ordering is preserved with 100% stability.
