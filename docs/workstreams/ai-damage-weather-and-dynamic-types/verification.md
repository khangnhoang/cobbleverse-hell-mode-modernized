# Verification Evidence Manifest: Dynamic Move Typing, Field Weather Resolution & Weather Accuracy Valuation

- **Workstream ID:** `ai-damage-weather-and-dynamic-types`
- **Baseline Commit:** `2b7c493e2e8ddea7be53179d269d2811e1ab61d0`
- **Plan Freeze Hash:** `4e922495860ae8f58d99a05ee4937efc14cd02c1`
- **Current Branch:** `fix/ai-damage-weather-and-dynamic-types`
- **Author Role:** Implementor (`I`)
- **Candidate Manifest Path:** `docs/workstreams/ai-damage-weather-and-dynamic-types/candidate_manifest.json`

---

## 1. Candidate Scope Closure & Working Tree Status

The candidate scope comprises the following files across the workstream:

### Delta Files (Defect 3 - Weather Accuracy Valuation & Move Dominance):
1. `companion-mod/src/main/java/com/cobbleverse/legendaryrule/strategy/weather/WeatherAccuracyValuationStrategy.java` (New static strategy utility)
2. `companion-mod/src/main/java/com/cobbleverse/legendaryrule/mixin/RunBunAIChooseMixin.java` (Injected `cobbleverse$applyWeatherAccuracyValuation` at candidate move ranking)
3. `companion-mod/src/test/java/com/cobbleverse/legendaryrule/strategy/weather/ThunderUnderRainValuationTest.java` (New comprehensive Layer 2 unit test suite)
4. `docs/workstreams/ai-damage-weather-and-dynamic-types/verification.md` (Updated verification manifest)

### Workstream Baseline Files (Defect 1 & Defect 2, committed at `33462f3`):
- `companion-mod/src/main/java/com/cobbleverse/legendaryrule/strategy/weather/WeatherContextNormalizer.java`
- `companion-mod/src/main/java/com/cobbleverse/legendaryrule/strategy/dynamic/DynamicMoveSurrogate.java`
- `companion-mod/src/main/java/com/cobbleverse/legendaryrule/strategy/dynamic/DynamicMoveResolver.java`
- `companion-mod/src/main/java/com/cobbleverse/legendaryrule/strategy/weather/MegaSolWeatherGuard.java`
- `companion-mod/src/main/java/com/cobbleverse/legendaryrule/strategy/weather/MegaSolWeatherBallSurrogate.java`
- `companion-mod/src/main/java/com/cobbleverse/legendaryrule/mixin/ContextManagerMixin.java`
- `companion-mod/src/main/java/com/cobbleverse/legendaryrule/mixin/PokeMathMaxMixin.java`
- `companion-mod/src/main/resources/rct_legendary_rule.mixins.json`
- `companion-mod/src/test/java/com/cobbleverse/legendaryrule/strategy/weather/WeatherContextNormalizerTest.java`
- `companion-mod/src/test/java/com/cobbleverse/legendaryrule/strategy/dynamic/DynamicMoveResolverTest.java`
- `companion-mod/src/test/java/com/cobbleverse/legendaryrule/strategy/weather/WaterDamageUnderSunContractTest.java`

---

## 2. Minimal Orthogonal Verification Set Results

### Applicable Layer: Layer 2 (Java Unit & Boundary Tests)
- **Authority Scope:** Java unit tests, mathematical damage formulas, surrogate move attributes, weather context normalization, effective move accuracy valuation tables, and mixin method bytecode contracts.

### Verification Command 1: Targeted Defect 3 Test Suite
```powershell
cd companion-mod
./gradlew test --tests "com.cobbleverse.legendaryrule.strategy.weather.ThunderUnderRainValuationTest"
```
**Result:** `BUILD SUCCESSFUL` (9/9 tests passed, 0 failures, 0 errors, 0 skipped).
- `testRawDamageUnchangedByWeather`: PASSED
- `testEffectiveAccuracyTable`: PASSED
- `testMoveDominanceUnderRain`: PASSED
- `testMoveDominanceOutsideRainAndSun`: PASSED
- `testKillingMovesRNGInversionEliminatedUnderRain`: PASSED
- `testTieBreakEliminatedUnderRain`: PASSED
- `testValuationOutsideRainMaintainsThunderboltReliability`: PASSED
- `testValuationInSunPenalizesWeatherMoves`: PASSED
- `testUtilityUmbrellaIgnoresRain`: PASSED

### Verification Command 2: Domain Weather and Dynamic Test Suites
```powershell
cd companion-mod
./gradlew test --tests "com.cobbleverse.legendaryrule.strategy.weather.*" --tests "com.cobbleverse.legendaryrule.strategy.dynamic.*"
```
**Result:** `BUILD SUCCESSFUL` (All targeted dynamic move, weather normalization, and weather accuracy valuation tests passed with 0 failures).

### Verification Command 3: Full Repository Unit Suite
```powershell
cd companion-mod
./gradlew test
```
**Result:** `BUILD SUCCESSFUL` (All unit tests across companion-mod executed, 0 failures, 0 errors).

### Verification Command 4: Repository Clean Build Check
```powershell
cd companion-mod
./gradlew build -x test
```
**Result:** `BUILD SUCCESSFUL` (Artifacts jar, remapJar, remapSourcesJar generated cleanly without errors).

---

## 3. Mathematical & Mechanical Invariants Verified

1. **Defect 1 Invariant (Water-Type Damage Estimation Under Sun):**
   - Under `"sunnyday"` weather context, `WeatherContextNormalizer` generates `"harshsunlight"`.
   - In `PokeMathMax`, `sun` evaluates to `true`.
   - Hydro Pump against Torkoal applies 0.5x weather reduction alongside 2.0x base type effectiveness, resulting in net 1.0x effectiveness instead of buggy unmitigated 2.0x.
   - Prevents NPC AI double-targeting lock-in bug.

2. **Defect 2 Invariant (Ivy Cudgel Dynamic Typings):**
   - Wellspring Ogerpon resolves Ivy Cudgel to Water type (100 BP, Physical).
   - Hearthflame Ogerpon resolves Ivy Cudgel to Fire type (100 BP, Physical).
   - Cornerstone Ogerpon resolves Ivy Cudgel to Rock type (100 BP, Physical).
   - Teal Mask / Base Ogerpon resolves Ivy Cudgel to Grass type (100 BP, Physical).
   - Move name remains `"ivycudgel"` in `MoveTemplate`, preserving contact move status for Tough Claws and Fluffy.

3. **Dynamic Move Typings (Raging Bull & Weather Ball):**
   - Raging Bull: Paldea-Combat resolves to Fighting (90 BP), Paldea-Blaze resolves to Fire (90 BP), Paldea-Aqua resolves to Water (90 BP), Base Tauros resolves to Normal (90 BP).
   - Weather Ball: Sun -> Fire (100 BP), Rain -> Water (100 BP), Sandstorm -> Rock (100 BP), Snow/Hail -> Ice (100 BP), Mega Sol -> Fire (100 BP), Neutral -> Normal (50 BP).
   - Utility Umbrella held item correctly retains neutral 50 BP Normal type.

4. **Defect 3 Invariants (Weather Accuracy Valuation & Move Dominance under Rain):**
   - **Effective Accuracy Lookup:** In Rain (`rain`, `heavyrain`, `primordialsea`, `raindance`), Thunder and Hurricane resolve to 1.0 (100% accuracy). In Sun (`sunnyday`, `harshsunlight`, `desolateland`), they resolve to 0.5 (50% accuracy). Outside weather, they resolve to 0.7 (70% accuracy). In Snow/Hail, Blizzard resolves to 1.0; in Sun, 0.5; neutral, 0.7.
   - **Move Dominance Invariant:** Under active Rain, Thunder strictly dominates Thunderbolt (`compareWeatherMoveDominance > 0`). Outside Rain, Thunderbolt dominates Thunder for reliability (`compareWeatherMoveDominance < 0`). Under Sun, Thunderbolt dominates Thunder (`compareWeatherMoveDominance < 0`).
   - **Elimination of `killingMoves` Independent RNG Inversion:** When target HP is within kill range for both moves, `WeatherAccuracyValuationStrategy.adjustMoveValuations()` boosts Thunder's score to strictly exceed Thunderbolt (`score(Thunder) >= score(Thunderbolt) + 1`), preventing the independent random roll from inverting selection.
   - **Elimination of 50/50 Coin Flip Tie-Break:** Equal scores between Thunder and Thunderbolt under Rain are broken deterministically in favor of Thunder.
   - **Adverse Sun Penalty:** In Sun, 50% accurate moves receive an explicit penalty (-6 score reduction) and yield dominance to reliable alternatives.
   - **Utility Umbrella Invariant:** A Pokémon holding Utility Umbrella ignores the Rain accuracy boost (effective accuracy remains 0.7), and Thunderbolt correctly retains reliability dominance.

5. **Claim Strength Discipline (Canary Lesson 7):**
   - All claims are bounded strictly by verified Layer 2 execution evidence.
   - 9/9 (100%) tests in `ThunderUnderRainValuationTest` passed.
   - 0 test failures across all test suites.
   - No hyperbolic or unverified semantic absolute terms are used.

6. **Offline vs. Production Reality Invariant (Canary Lesson 6):**
   - Automated test passes locally verify offline bytecode contracts, data structures, and valuation algorithms in isolation.
   - Offline verification does not execute live Minecraft server world ticking or live player battles (Layer 5 authority).
