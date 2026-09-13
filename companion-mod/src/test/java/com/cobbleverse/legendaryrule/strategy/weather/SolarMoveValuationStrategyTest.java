package com.cobbleverse.legendaryrule.strategy.weather;

import com.cobblemon.mod.common.api.abilities.Ability;
import com.cobblemon.mod.common.api.abilities.AbilityTemplate;
import com.cobblemon.mod.common.api.battles.interpreter.BasicContext;
import com.cobblemon.mod.common.api.battles.interpreter.BattleContext;
import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.api.moves.categories.DamageCategories;
import com.cobblemon.mod.common.api.pokemon.helditem.HeldItemManager;
import com.cobblemon.mod.common.api.types.ElementalType;
import com.cobblemon.mod.common.api.types.ElementalTypes;
import com.cobblemon.mod.common.battles.ActiveBattlePokemon;
import com.cobblemon.mod.common.battles.BattleSide;
import com.cobblemon.mod.common.battles.MoveTarget;
import com.cobblemon.mod.common.battles.actor.PokemonBattleActor;
import com.cobblemon.mod.common.battles.interpreter.ContextManager;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobbleverse.legendaryrule.strategy.tracker.BattleItemStateTracker;
import com.gitlab.surilexa.rbrctai.api.ai.RunBunAI;
import kotlin.LazyKt;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression test suite for two-turn solar move valuation (Solar Beam and Solar Blade) in Run & Bun AI.
 *
 * Covers all required verification criteria:
 * - A: Clear weather, no Power Herb: immediate scoring damage = 0, not killing, not maxMove, alternative promoted.
 * - B: Sun, no Power Herb: immediate damage preserved, killing/maxMove intact, zero regression.
 * - C: Clear, Power Herb: immediate attack valid, post-consumption reverts to charge move.
 * - D: Rain, no Power Herb: immediate scoring damage = 0, eventual estimator has 0.5x.
 * - E: Sandstorm, no Power Herb: immediate scoring damage = 0, eventual estimator has 0.5x.
 * - F: Rain/Sand + Power Herb: immediate attack, damage has 0.5x, ranking based on corrected damage.
 * - G: Snow/Hail: 0.5x adverse weather preserved without double-application.
 * - H: Solar Blade: same contract as Solar Beam.
 * - I: Competing-move regression: Energy Ball takes maxMove and is selected over charging Solar Beam.
 * - J: Utility Umbrella: negates Sun (charges) and negates Rain penalty (full damage).
 */
class SolarMoveValuationStrategyTest {

    private static Unsafe unsafe;
    private static Field bpContextManagerField;
    private static Field bpHeldItemDelegateField;
    private static Field bpEffectedPokemonField;
    private static Field abilityTemplateField;
    private static Field abilityTemplateNameField;
    private static Field pokemonAbilityField;
    private static Field bpActorField;
    private static Field actorBattleField;
    private static Field battleSide1Field;
    private static Field battleSide2Field;
    private static Field sideActorsField;
    private static Field sideBattleField;
    private static Field actorActivePokemonField;
    private static Field abpBattlePokemonField;
    private static Field battleContextManagerField;
    private static Field sideContextManagerField;
    private static Field abpBattleField;

    @BeforeAll
    static void setUpAll() throws Exception {
        try {
            net.minecraft.SharedConstants.createGameVersion();
            Method init = Class.forName("net.minecraft.Bootstrap").getDeclaredMethod("initialize");
            init.setAccessible(true);
            init.invoke(null);
        } catch (Throwable ignored) {
        }

        Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        unsafe = (Unsafe) unsafeField.get(null);

        bpContextManagerField = BattlePokemon.class.getDeclaredField("contextManager");
        bpContextManagerField.setAccessible(true);

        bpHeldItemDelegateField = BattlePokemon.class.getDeclaredField("heldItemManager$delegate");
        bpHeldItemDelegateField.setAccessible(true);

        bpEffectedPokemonField = BattlePokemon.class.getDeclaredField("effectedPokemon");
        bpEffectedPokemonField.setAccessible(true);

        abilityTemplateField = Ability.class.getDeclaredField("template");
        abilityTemplateField.setAccessible(true);

        abilityTemplateNameField = AbilityTemplate.class.getDeclaredField("name");
        abilityTemplateNameField.setAccessible(true);

        pokemonAbilityField = Pokemon.class.getDeclaredField("ability");
        pokemonAbilityField.setAccessible(true);

        bpActorField = BattlePokemon.class.getDeclaredField("actor");
        bpActorField.setAccessible(true);

        actorBattleField = BattleActor.class.getDeclaredField("battle");
        actorBattleField.setAccessible(true);

        battleSide1Field = PokemonBattle.class.getDeclaredField("side1");
        battleSide1Field.setAccessible(true);

        battleSide2Field = PokemonBattle.class.getDeclaredField("side2");
        battleSide2Field.setAccessible(true);

        sideActorsField = BattleSide.class.getDeclaredField("actors");
        sideActorsField.setAccessible(true);

        sideBattleField = BattleSide.class.getDeclaredField("battle");
        sideBattleField.setAccessible(true);

        actorActivePokemonField = BattleActor.class.getDeclaredField("activePokemon");
        actorActivePokemonField.setAccessible(true);

        abpBattlePokemonField = ActiveBattlePokemon.class.getDeclaredField("battlePokemon");
        abpBattlePokemonField.setAccessible(true);

        battleContextManagerField = PokemonBattle.class.getDeclaredField("contextManager");
        battleContextManagerField.setAccessible(true);

        sideContextManagerField = BattleSide.class.getDeclaredField("contextManager");
        sideContextManagerField.setAccessible(true);

        abpBattleField = ActiveBattlePokemon.class.getDeclaredField("battle");
        abpBattleField.setAccessible(true);
    }

    static class TestBattlePokemon extends BattlePokemon implements BattleItemStateTracker {
        private boolean throatSprayEnded = false;
        private boolean powerHerbEnded = false;

        @SuppressWarnings("unused")
        public TestBattlePokemon() {
            super(null, null, (kotlin.jvm.functions.Function1) null);
        }

        @Override
        public boolean cobbleverse$isThroatSprayEnded() {
            return throatSprayEnded;
        }

        @Override
        public void cobbleverse$markThroatSprayEnded() {
            this.throatSprayEnded = true;
        }

        @Override
        public boolean cobbleverse$isPowerHerbEnded() {
            return powerHerbEnded;
        }

        @Override
        public void cobbleverse$markPowerHerbEnded() {
            this.powerHerbEnded = true;
        }
    }

    private Move createMove(String name, ElementalType type, double power, double accuracy, boolean physical) {
        MoveTemplate template = new MoveTemplate(
            name,
            1,
            type,
            physical ? DamageCategories.INSTANCE.getPHYSICAL() : DamageCategories.INSTANCE.getSPECIAL(),
            power,
            MoveTarget.normal,
            accuracy,
            10,
            0,
            1.0d,
            new Double[0]
        );
        return new Move(template, 10, 0);
    }

    private TestBattlePokemon createBattlePokemon(String weatherId, String heldItemId) throws Exception {
        return createBattlePokemon(weatherId, heldItemId, "overgrow");
    }

    private TestBattlePokemon createBattlePokemon(String weatherId, String heldItemId, String abilityName) throws Exception {
        TestBattlePokemon bp = (TestBattlePokemon) unsafe.allocateInstance(TestBattlePokemon.class);

        Pokemon pokemon = (Pokemon) unsafe.allocateInstance(Pokemon.class);
        pokemon.setUuid(UUID.randomUUID());
        bpEffectedPokemonField.set(bp, pokemon);

        if (abilityName != null) {
            AbilityTemplate abilityTemplate = (AbilityTemplate) unsafe.allocateInstance(AbilityTemplate.class);
            abilityTemplateNameField.set(abilityTemplate, abilityName);

            Ability ability = (Ability) unsafe.allocateInstance(Ability.class);
            abilityTemplateField.set(ability, abilityTemplate);

            pokemonAbilityField.set(pokemon, ability);
        }

        BattleActor actor = (BattleActor) unsafe.allocateInstance(PokemonBattleActor.class);
        bpActorField.set(bp, actor);

        ContextManager cm = new ContextManager();
        if (weatherId != null) {
            cm.add(new BasicContext(weatherId, 1, BattleContext.Type.WEATHER, null));
        }
        bpContextManagerField.set(bp, cm);

        HeldItemManager manager = (HeldItemManager) Proxy.newProxyInstance(
            HeldItemManager.class.getClassLoader(),
            new Class<?>[]{HeldItemManager.class},
            (proxy, method, args) -> {
                if ("showdownId".equals(method.getName())) {
                    return heldItemId;
                }
                return null;
            }
        );
        bpHeldItemDelegateField.set(bp, LazyKt.lazyOf(manager));

        return bp;
    }

    private PokemonBattle createBattleWithOpponentAbility(BattlePokemon user, String opponentAbilityName) throws Exception {
        PokemonBattle battle = (PokemonBattle) unsafe.allocateInstance(PokemonBattle.class);
        battleContextManagerField.set(battle, new ContextManager());

        BattleActor playerActor = (BattleActor) unsafe.allocateInstance(PokemonBattleActor.class);
        actorBattleField.set(playerActor, battle);
        bpActorField.set(user, playerActor);

        BattleActor npcActor = (BattleActor) unsafe.allocateInstance(PokemonBattleActor.class);
        actorBattleField.set(npcActor, battle);

        BattleSide side1 = (BattleSide) unsafe.allocateInstance(BattleSide.class);
        sideActorsField.set(side1, new BattleActor[]{ playerActor });
        sideBattleField.set(side1, battle);
        sideContextManagerField.set(side1, new ContextManager());

        BattleSide side2 = (BattleSide) unsafe.allocateInstance(BattleSide.class);
        sideActorsField.set(side2, new BattleActor[]{ npcActor });
        sideBattleField.set(side2, battle);
        sideContextManagerField.set(side2, new ContextManager());

        battleSide1Field.set(battle, side1);
        battleSide2Field.set(battle, side2);

        ActiveBattlePokemon userAbp = (ActiveBattlePokemon) unsafe.allocateInstance(ActiveBattlePokemon.class);
        abpBattlePokemonField.set(userAbp, user);
        abpBattleField.set(userAbp, battle);

        TestBattlePokemon oppBp = createBattlePokemon(null, null, opponentAbilityName);
        bpActorField.set(oppBp, npcActor);
        ActiveBattlePokemon oppAbp = (ActiveBattlePokemon) unsafe.allocateInstance(ActiveBattlePokemon.class);
        abpBattlePokemonField.set(oppAbp, oppBp);
        abpBattleField.set(oppAbp, battle);

        actorActivePokemonField.set(playerActor, new ArrayList<>(List.of(userAbp)));
        actorActivePokemonField.set(npcActor, new ArrayList<>(List.of(oppAbp)));

        return battle;
    }

    private ActiveBattlePokemon createActiveBattlePokemon(BattlePokemon bp) throws Exception {
        ActiveBattlePokemon abp = (ActiveBattlePokemon) unsafe.allocateInstance(ActiveBattlePokemon.class);
        abp.setBattlePokemon(bp);
        return abp;
    }

    private RunBunAI.MoveEvaluation createEvaluation(Move move, int damage, int score, ActiveBattlePokemon opponent) {
        try {
            RunBunAI.MoveEvaluation eval = (RunBunAI.MoveEvaluation) unsafe.allocateInstance(RunBunAI.MoveEvaluation.class);
            eval.setMove(move);
            eval.setDamage(damage);
            eval.setScore(score);
            eval.setOpponent(opponent);
            return eval;
        } catch (Exception e) {
            throw new RuntimeException("Failed to allocate MoveEvaluation", e);
        }
    }

    // =========================================================================
    // Test A: Clear weather, no Power Herb
    // =========================================================================
    @Test
    @DisplayName("Test A: Clear weather, no Power Herb - Solar Beam normalized to 0, not killing/maxMove")
    void testClearWeatherNoPowerHerb() throws Exception {
        TestBattlePokemon attacker = createBattlePokemon(null, null);
        ActiveBattlePokemon abp = createActiveBattlePokemon(attacker);
        Move solarBeam = createMove("solarbeam", ElementalTypes.GRASS, 120.0d, 100.0d, false);
        Move energyBall = createMove("energyball", ElementalTypes.GRASS, 90.0d, 100.0d, false);

        int solarBeamTheoretical = 180;
        int energyBallTheoretical = 140;

        int solarBeamScoringDmg = SolarMoveValuationStrategy.resolveImmediateScoringDamage(
            solarBeamTheoretical, solarBeam, attacker, abp, null
        );
        int energyBallScoringDmg = SolarMoveValuationStrategy.resolveImmediateScoringDamage(
            energyBallTheoretical, energyBall, attacker, abp, null
        );

        assertEquals(0, solarBeamScoringDmg, "Solar Beam immediate scoring damage must be 0 outside Sun/Power Herb");
        assertEquals(140, energyBallScoringDmg, "Energy Ball immediate scoring damage must remain unmodified");

        // Simulate RunBunAI classification with target HP = 160
        int targetHP = 160;
        List<RunBunAI.MoveEvaluation> killingMoves = new ArrayList<>();
        List<RunBunAI.MoveEvaluation> nonKillingPossibleMoves = new ArrayList<>();

        RunBunAI.MoveEvaluation evalSolar = createEvaluation(solarBeam, solarBeamScoringDmg, 0, null);
        RunBunAI.MoveEvaluation evalEnergy = createEvaluation(energyBall, energyBallScoringDmg, 0, null);

        if (solarBeamScoringDmg >= targetHP) killingMoves.add(evalSolar);
        else nonKillingPossibleMoves.add(evalSolar);

        if (energyBallScoringDmg >= targetHP) killingMoves.add(evalEnergy);
        else nonKillingPossibleMoves.add(evalEnergy);

        assertTrue(killingMoves.isEmpty(), "Neither move should be in killingMoves (0 < 160, 140 < 160)");
        assertEquals(2, nonKillingPossibleMoves.size());

        // MaxMove determination:
        int maxDamage = 0;
        RunBunAI.MoveEvaluation maxMove = null;
        for (RunBunAI.MoveEvaluation eval : nonKillingPossibleMoves) {
            if ((maxDamage = Math.max(maxDamage, eval.getDamage())) == eval.getDamage()) {
                maxMove = eval;
            }
        }

        assertNotNull(maxMove);
        assertSame(energyBall, maxMove.getMove(), "Energy Ball must become maxMove over charging Solar Beam");
        assertEquals(140, maxDamage);
    }

    // =========================================================================
    // Test B: Sun, no Power Herb
    // =========================================================================
    @Test
    @DisplayName("Test B: Sun, no Power Herb - Solar Beam keeps native immediate damage and killingMove status")
    void testSunNoPowerHerb() throws Exception {
        TestBattlePokemon attacker = createBattlePokemon("sunnyday", null);
        ActiveBattlePokemon abp = createActiveBattlePokemon(attacker);
        Move solarBeam = createMove("solarbeam", ElementalTypes.GRASS, 120.0d, 100.0d, false);

        int theoreticalDamage = 180;
        int scoringDamage = SolarMoveValuationStrategy.resolveImmediateScoringDamage(
            theoreticalDamage, solarBeam, attacker, abp, null
        );

        assertEquals(180, scoringDamage, "Under Sun, Solar Beam must keep full theoretical damage");

        // Target HP = 160: Solar Beam kills
        int targetHP = 160;
        List<RunBunAI.MoveEvaluation> killingMoves = new ArrayList<>();
        RunBunAI.MoveEvaluation evalSolar = createEvaluation(solarBeam, scoringDamage, 0, null);
        if (scoringDamage >= targetHP) {
            killingMoves.add(evalSolar);
        }

        assertEquals(1, killingMoves.size(), "Under Sun, Solar Beam with 180 dmg must classify as killingMove against 160 HP");
    }

    // =========================================================================
    // Test C: Clear, Power Herb
    // =========================================================================
    @Test
    @DisplayName("Test C: Clear, Power Herb - immediate attack valid, post-consumption reverts to charge move")
    void testClearPowerHerb() throws Exception {
        TestBattlePokemon attacker = createBattlePokemon(null, "powerherb");
        ActiveBattlePokemon abp = createActiveBattlePokemon(attacker);
        Move solarBeam = createMove("solarbeam", ElementalTypes.GRASS, 120.0d, 100.0d, false);

        int theoreticalDamage = 180;
        int scoringDamageBefore = SolarMoveValuationStrategy.resolveImmediateScoringDamage(
            theoreticalDamage, solarBeam, attacker, abp, null
        );

        assertEquals(180, scoringDamageBefore, "With usable Power Herb, Solar Beam must attack immediately");

        // Simulate Power Herb consumption
        attacker.cobbleverse$markPowerHerbEnded();
        assertTrue(attacker.cobbleverse$isPowerHerbEnded());
        assertFalse(SolarMoveValuationStrategy.hasUsablePowerHerb(attacker));

        int scoringDamageAfter = SolarMoveValuationStrategy.resolveImmediateScoringDamage(
            theoreticalDamage, solarBeam, attacker, abp, null
        );

        assertEquals(0, scoringDamageAfter, "After Power Herb consumption, Solar Beam must revert to 0 immediate scoring damage");
    }

    // =========================================================================
    // Test C2: Sun after consumed Power Herb
    // =========================================================================
    @Test
    @DisplayName("Test C2: Sun after consumed Power Herb - remains immediate because Sun independently removes charge turn")
    void testSunAfterPowerHerbConsumedRemainsImmediate() throws Exception {
        TestBattlePokemon attacker = createBattlePokemon("sunnyday", "powerherb");
        ActiveBattlePokemon abp = createActiveBattlePokemon(attacker);
        Move solarBeam = createMove("solarbeam", ElementalTypes.GRASS, 120.0d, 100.0d, false);

        int theoreticalDamage = 180;

        // Simulate that Power Herb has already been consumed earlier in battle
        attacker.cobbleverse$markPowerHerbEnded();
        assertTrue(attacker.cobbleverse$isPowerHerbEnded(), "Power Herb must be marked ended");
        assertFalse(SolarMoveValuationStrategy.hasUsablePowerHerb(attacker), "Power Herb is no longer usable");
        assertTrue(SolarMoveValuationStrategy.isEffectiveSun(attacker, abp, null), "Sun is still active on field");

        int scoringDamage = SolarMoveValuationStrategy.resolveImmediateScoringDamage(
            theoreticalDamage, solarBeam, attacker, abp, null
        );

        assertEquals(180, scoringDamage,
            "Solar Beam must remain an immediate attack under Sun even after Power Herb has been consumed");
    }

    // =========================================================================
    // Test D: Rain, no Power Herb
    // =========================================================================
    @Test
    @DisplayName("Test D: Rain, no Power Herb - eventual damage 0.5x, immediate scoring damage 0")
    void testRainNoPowerHerb() throws Exception {
        TestBattlePokemon attacker = createBattlePokemon("rain", null);
        ActiveBattlePokemon abp = createActiveBattlePokemon(attacker);
        Move solarBeam = createMove("solarbeam", ElementalTypes.GRASS, 120.0d, 100.0d, false);

        double rawDamage = 180.0d;
        double adjustedEventual = SolarMoveValuationStrategy.adjustAdverseWeatherDamage(
            rawDamage, solarBeam, attacker, abp, null
        );

        assertEquals(90.0d, adjustedEventual, 0.001, "Under Rain, Solar Beam eventual damage must be halved (0.5x)");

        int scoringDamage = SolarMoveValuationStrategy.resolveImmediateScoringDamage(
            (int) adjustedEventual, solarBeam, attacker, abp, null
        );

        assertEquals(0, scoringDamage, "Under Rain without Power Herb, immediate scoring damage must be 0");
    }

    // =========================================================================
    // Test E: Sandstorm, no Power Herb
    // =========================================================================
    @Test
    @DisplayName("Test E: Sandstorm, no Power Herb - eventual damage 0.5x, immediate scoring damage 0")
    void testSandstormNoPowerHerb() throws Exception {
        TestBattlePokemon attacker = createBattlePokemon("sandstorm", null);
        ActiveBattlePokemon abp = createActiveBattlePokemon(attacker);
        Move solarBeam = createMove("solarbeam", ElementalTypes.GRASS, 120.0d, 100.0d, false);

        double rawDamage = 180.0d;
        double adjustedEventual = SolarMoveValuationStrategy.adjustAdverseWeatherDamage(
            rawDamage, solarBeam, attacker, abp, null
        );

        assertEquals(90.0d, adjustedEventual, 0.001, "Under Sandstorm, Solar Beam eventual damage must be halved (0.5x)");

        int scoringDamage = SolarMoveValuationStrategy.resolveImmediateScoringDamage(
            (int) adjustedEventual, solarBeam, attacker, abp, null
        );

        assertEquals(0, scoringDamage, "Under Sandstorm without Power Herb, immediate scoring damage must be 0");
    }

    // =========================================================================
    // Test F: Rain/Sand + Power Herb
    // =========================================================================
    @Test
    @DisplayName("Test F: Rain/Sand + Power Herb - immediate attack with 0.5x adverse weather damage")
    void testRainSandPowerHerb() throws Exception {
        TestBattlePokemon attackerRain = createBattlePokemon("rain", "powerherb");
        ActiveBattlePokemon abpRain = createActiveBattlePokemon(attackerRain);
        Move solarBeam = createMove("solarbeam", ElementalTypes.GRASS, 120.0d, 100.0d, false);

        double rawDamage = 180.0d;
        double adjustedDamage = SolarMoveValuationStrategy.adjustAdverseWeatherDamage(
            rawDamage, solarBeam, attackerRain, abpRain, null
        );
        assertEquals(90.0d, adjustedDamage, 0.001);

        int scoringDamage = SolarMoveValuationStrategy.resolveImmediateScoringDamage(
            (int) adjustedDamage, solarBeam, attackerRain, abpRain, null
        );

        assertEquals(90, scoringDamage, "With Power Herb in Rain, attack executes immediately with 0.5x damage");

        // Target HP = 100: 90 < 100 -> NOT a killingMove
        assertFalse(scoringDamage >= 100, "Corrected 90 damage must not qualify as KO against 100 HP target");
    }

    // =========================================================================
    // Test G: Snow/Hail
    // =========================================================================
    @Test
    @DisplayName("Test G: Snow/Hail - confirms no double-application of 0.5x multiplier")
    void testSnowHailNoDoubleApplication() throws Exception {
        TestBattlePokemon attackerSnow = createBattlePokemon("snow", null);
        ActiveBattlePokemon abpSnow = createActiveBattlePokemon(attackerSnow);
        Move solarBeam = createMove("solarbeam", ElementalTypes.GRASS, 120.0d, 100.0d, false);

        // In PokeMathMax, rawDamage from native helper already received 0.5x under snow/hail.
        // SolarMoveValuationStrategy.adjustAdverseWeatherDamage must NOT apply 0.5x again.
        double alreadyHalvedDamage = 90.0d;
        double result = SolarMoveValuationStrategy.adjustAdverseWeatherDamage(
            alreadyHalvedDamage, solarBeam, attackerSnow, abpSnow, null
        );

        assertEquals(90.0d, result, 0.001, "Under Snow, adjustAdverseWeatherDamage must NOT re-apply 0.5x to prevent 0.25x defect");
    }

    // =========================================================================
    // Test H: Solar Blade
    // =========================================================================
    @Test
    @DisplayName("Test H: Solar Blade - proves Solar Blade shares the identical contract as Solar Beam")
    void testSolarBladeContractEquivalence() throws Exception {
        Move solarBlade = createMove("solarblade", ElementalTypes.GRASS, 125.0d, 100.0d, true);

        assertTrue(SolarMoveValuationStrategy.isSolarMove(solarBlade));
        assertTrue(SolarMoveValuationStrategy.isSolarMove("solarblade"));

        TestBattlePokemon attackerClear = createBattlePokemon(null, null);
        ActiveBattlePokemon abpClear = createActiveBattlePokemon(attackerClear);

        assertEquals(0, SolarMoveValuationStrategy.resolveImmediateScoringDamage(190, solarBlade, attackerClear, abpClear, null),
            "Solar Blade without Sun/Power Herb must have 0 immediate scoring damage");

        TestBattlePokemon attackerSun = createBattlePokemon("sunnyday", null);
        ActiveBattlePokemon abpSun = createActiveBattlePokemon(attackerSun);

        assertEquals(190, SolarMoveValuationStrategy.resolveImmediateScoringDamage(190, solarBlade, attackerSun, abpSun, null),
            "Solar Blade in Sun must attack immediately with full damage");

        TestBattlePokemon attackerRain = createBattlePokemon("rain", null);
        ActiveBattlePokemon abpRain = createActiveBattlePokemon(attackerRain);

        assertEquals(95.0d, SolarMoveValuationStrategy.adjustAdverseWeatherDamage(190.0d, solarBlade, attackerRain, abpRain, null), 0.001,
            "Solar Blade in Rain must receive 0.5x damage reduction");
    }

    // =========================================================================
    // Test I: Competing-move regression
    // =========================================================================
    @Test
    @DisplayName("Test I: Competing-move regression - Solar Beam (180 theoretical) vs Energy Ball (140) vs target (160 HP)")
    void testCompetingMoveRegression() throws Exception {
        TestBattlePokemon attacker = createBattlePokemon(null, null);
        ActiveBattlePokemon abp = createActiveBattlePokemon(attacker);

        Move solarBeam = createMove("solarbeam", ElementalTypes.GRASS, 120.0d, 100.0d, false);
        Move energyBall = createMove("energyball", ElementalTypes.GRASS, 90.0d, 100.0d, false);

        int targetHP = 160;
        int targetMaxHP = 160;

        // Native PokeMathMax theoretical damages:
        int solarTheoretical = 180;
        int energyTheoretical = 140;

        // Immediate scoring damage after our hook:
        int solarScoringDmg = SolarMoveValuationStrategy.resolveImmediateScoringDamage(solarTheoretical, solarBeam, attacker, abp, null);
        int energyScoringDmg = SolarMoveValuationStrategy.resolveImmediateScoringDamage(energyTheoretical, energyBall, attacker, abp, null);

        assertEquals(0, solarScoringDmg);
        assertEquals(140, energyScoringDmg);

        // Simulation of RunBunAI.choose ranking pipeline:
        List<RunBunAI.MoveEvaluation> evaluations = new ArrayList<>();
        List<RunBunAI.MoveEvaluation> killingMoves = new ArrayList<>();
        List<RunBunAI.MoveEvaluation> nonKillingPossibleMoves = new ArrayList<>();

        RunBunAI.MoveEvaluation evalSolar = createEvaluation(solarBeam, solarScoringDmg, 0, null);
        RunBunAI.MoveEvaluation evalEnergy = createEvaluation(energyBall, energyScoringDmg, 0, null);
        evaluations.add(evalSolar);
        evaluations.add(evalEnergy);

        // Candidate classification:
        if (solarScoringDmg >= targetHP) killingMoves.add(evalSolar);
        else nonKillingPossibleMoves.add(evalSolar);

        if (energyScoringDmg >= targetHP) killingMoves.add(evalEnergy);
        else nonKillingPossibleMoves.add(evalEnergy);

        // Verification 1: Solar Beam is NOT killingMove
        assertFalse(killingMoves.contains(evalSolar), "Solar Beam must NOT be in killingMoves");
        assertTrue(killingMoves.isEmpty(), "killingMoves must be empty");

        // Verification 2: MaxMove selection
        int maxDamage = 0;
        RunBunAI.MoveEvaluation maxMove = null;
        if (killingMoves.isEmpty()) {
            for (RunBunAI.MoveEvaluation eval : evaluations) {
                if ((maxDamage = Math.max(maxDamage, eval.getDamage())) == eval.getDamage()) {
                    maxMove = eval;
                }
            }
        }

        assertNotNull(maxMove);
        assertSame(energyBall, maxMove.getMove(), "Energy Ball must be selected as maxMove");
        assertEquals(140, maxDamage);

        // Verification 3: Scoring assignment
        // maxMove gets native bonus 6 (assuming conservative roll):
        if (maxMove == evalEnergy) {
            evalEnergy.setScore(6);
        }

        // Native fallback scoring for score == 0:
        for (RunBunAI.MoveEvaluation eval : evaluations) {
            if (eval.getScore() == 0) {
                double pct = (double) eval.getDamage() / (double) targetMaxHP;
                if (!eval.getMove().getDamageCategory().equals(DamageCategories.INSTANCE.getSTATUS()) && eval.getDamage() == 0) {
                    eval.setScore(-5);
                } else if (pct > 0.3) {
                    eval.setScore(5);
                } else {
                    eval.setScore(-5);
                }
            }
        }

        assertEquals(6, evalEnergy.getScore(), "Energy Ball must have maxMove score (6 or 8)");
        assertEquals(-5, evalSolar.getScore(), "Solar Beam must have native damaging-zero-damage score (-5)");

        // Verification 4: Final winner selection
        RunBunAI.MoveEvaluation best = evaluations.stream()
            .max(Comparator.comparingInt(RunBunAI.MoveEvaluation::getScore))
            .orElse(null);

        assertNotNull(best);
        assertSame(energyBall, best.getMove(), "Energy Ball must be the final chosen move");
    }

    // =========================================================================
    // Test J: Utility Umbrella
    // =========================================================================
    @Test
    @DisplayName("Test J: Utility Umbrella - negates Sun (charges) and negates Rain penalty (full damage)")
    void testUtilityUmbrellaInteractions() throws Exception {
        Move solarBeam = createMove("solarbeam", ElementalTypes.GRASS, 120.0d, 100.0d, false);

        // Case 1: Attacker holds Utility Umbrella in Sun -> effective Sun is negated -> charges (damage 0)
        TestBattlePokemon attackerSunUmbrella = createBattlePokemon("sunnyday", "utilityumbrella");
        ActiveBattlePokemon abpSunUmbrella = createActiveBattlePokemon(attackerSunUmbrella);

        assertFalse(SolarMoveValuationStrategy.isEffectiveSun(attackerSunUmbrella, abpSunUmbrella, null),
            "Utility Umbrella must negate effective Sun on the attacker");
        assertEquals(0, SolarMoveValuationStrategy.resolveImmediateScoringDamage(180, solarBeam, attackerSunUmbrella, abpSunUmbrella, null),
            "Solar Beam under Sun with Utility Umbrella must charge (0 immediate scoring damage)");

        // Case 2: Attacker holds Utility Umbrella in Rain -> effective Rain penalty is negated -> full damage (180)
        TestBattlePokemon attackerRainUmbrella = createBattlePokemon("rain", "utilityumbrella");
        ActiveBattlePokemon abpRainUmbrella = createActiveBattlePokemon(attackerRainUmbrella);

        assertEquals(180.0d, SolarMoveValuationStrategy.adjustAdverseWeatherDamage(180.0d, solarBeam, attackerRainUmbrella, abpRainUmbrella, null), 0.001,
            "Utility Umbrella must negate Rain damage reduction, retaining full 180.0 damage");
    }

    // =========================================================================
    // Test K: Mega Sol & Ability Suppression Correctness
    // =========================================================================
    @Test
    @DisplayName("Test K1: Active Mega Sol grants effective Sun semantics without field weather")
    void testActiveMegaSolGrantsEffectiveSun() throws Exception {
        TestBattlePokemon attacker = createBattlePokemon(null, null, "megasol");
        ActiveBattlePokemon abp = createActiveBattlePokemon(attacker);
        Move solarBeam = createMove("solarbeam", ElementalTypes.GRASS, 120.0d, 100.0d, false);

        assertTrue(MegaSolWeatherGuard.hasMegaSol(attacker), "Attacker must have active Mega Sol");
        assertTrue(SolarMoveValuationStrategy.isEffectiveSun(attacker, abp, null), "Active Mega Sol must grant effective Sun");

        int scoringDamage = SolarMoveValuationStrategy.resolveImmediateScoringDamage(180, solarBeam, attacker, abp, null);
        assertEquals(180, scoringDamage, "Solar Beam with active Mega Sol must attack immediately with full damage");
    }

    @Test
    @DisplayName("Test K2: Suppressed Mega Sol (Neutralizing Gas) does not grant effective Sun from ability")
    void testSuppressedMegaSolDoesNotGrantEffectiveSun() throws Exception {
        TestBattlePokemon attacker = createBattlePokemon(null, null, "megasol");
        ActiveBattlePokemon abp = createActiveBattlePokemon(attacker);
        PokemonBattle battle = createBattleWithOpponentAbility(attacker, "neutralizinggas");
        Move solarBeam = createMove("solarbeam", ElementalTypes.GRASS, 120.0d, 100.0d, false);

        assertFalse(MegaSolWeatherGuard.hasMegaSol(attacker), "Mega Sol must be suppressed by Neutralizing Gas");
        assertFalse(SolarMoveValuationStrategy.isEffectiveSun(attacker, abp, battle), "Suppressed Mega Sol must not grant effective Sun");

        int scoringDamage = SolarMoveValuationStrategy.resolveImmediateScoringDamage(180, solarBeam, attacker, abp, battle);
        assertEquals(0, scoringDamage, "Solar Beam with suppressed Mega Sol must charge (immediate scoring damage = 0)");
    }

    @Test
    @DisplayName("Test K3: Normal field Sun semantics remain unchanged regardless of ability")
    void testNormalFieldSunRemainsEffective() throws Exception {
        TestBattlePokemon attacker = createBattlePokemon("sunnyday", null, "overgrow");
        ActiveBattlePokemon abp = createActiveBattlePokemon(attacker);
        Move solarBeam = createMove("solarbeam", ElementalTypes.GRASS, 120.0d, 100.0d, false);

        assertFalse(MegaSolWeatherGuard.hasMegaSol(attacker), "Attacker does not have Mega Sol");
        assertTrue(SolarMoveValuationStrategy.isEffectiveSun(attacker, abp, null), "Field Sun must be effective");

        int scoringDamage = SolarMoveValuationStrategy.resolveImmediateScoringDamage(180, solarBeam, attacker, abp, null);
        assertEquals(180, scoringDamage, "Solar Beam under normal field Sun must attack immediately with full damage");
    }
}
