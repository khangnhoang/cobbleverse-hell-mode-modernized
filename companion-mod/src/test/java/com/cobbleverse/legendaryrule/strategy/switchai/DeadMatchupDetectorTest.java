package com.cobbleverse.legendaryrule.strategy.switchai;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.abilities.Ability;
import com.cobblemon.mod.common.api.abilities.AbilityTemplate;
import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.moves.MoveSet;
import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.api.moves.categories.DamageCategories;
import com.cobblemon.mod.common.api.moves.categories.DamageCategory;
import com.cobblemon.mod.common.api.pokemon.helditem.HeldItemManager;
import com.cobblemon.mod.common.api.pokemon.stats.StatProvider;
import com.cobblemon.mod.common.api.types.ElementalType;
import com.cobblemon.mod.common.api.types.ElementalTypes;
import com.cobblemon.mod.common.battles.ActiveBattlePokemon;
import com.cobblemon.mod.common.battles.MoveTarget;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.pokemon.FormData;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.gitlab.surilexa.rbrctai.api.ai.RunBunAI;
import com.gitlab.surilexa.rbrctai.api.ai.utils.RBStatStages;
import kotlin.Lazy;
import kotlin.LazyKt;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class DeadMatchupDetectorTest {

    private static Unsafe unsafe;
    private static final Map<Pokemon, Integer> pokemonHpMap = new ConcurrentHashMap<>();

    @BeforeAll
    static void setUpAll() throws Exception {
        try {
            net.minecraft.SharedConstants.createGameVersion();
            java.lang.reflect.Method init = Class.forName("net.minecraft.Bootstrap").getDeclaredMethod("initialize");
            init.setAccessible(true);
            init.invoke(null);
        } catch (Throwable t) {
            // ignore if already initialized
        }

        Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        unsafe = (Unsafe) f.get(null);

        StatProvider proxy = (StatProvider) Proxy.newProxyInstance(
            StatProvider.class.getClassLoader(),
            new Class<?>[]{StatProvider.class},
            (proxyInstance, method, args) -> {
                if ("getStatForPokemon".equals(method.getName())) {
                    Pokemon pkmn = (Pokemon) args[0];
                    return pokemonHpMap.getOrDefault(pkmn, 100);
                }
                return null;
            }
        );
        Cobblemon.INSTANCE.setStatProvider(proxy);
    }

    private ActiveBattlePokemon createOpponent(int maxHp, int currentHp, boolean isGone) throws Exception {
        Pokemon pokemon = (Pokemon) unsafe.allocateInstance(Pokemon.class);
        pokemon.setUuid(UUID.randomUUID());
        pokemonHpMap.put(pokemon, maxHp);
        Field healthField = Pokemon.class.getDeclaredField("currentHealth");
        healthField.setAccessible(true);
        healthField.setInt(pokemon, currentHp);

        FormData form = new FormData();
        Field formNameField = FormData.class.getDeclaredField("name");
        formNameField.setAccessible(true);
        formNameField.set(form, "normal");
        Field formPrimaryTypeField = FormData.class.getDeclaredField("_primaryType");
        formPrimaryTypeField.setAccessible(true);
        formPrimaryTypeField.set(form, ElementalTypes.NORMAL);
        Field pokemonFormField = Pokemon.class.getDeclaredField("form");
        pokemonFormField.setAccessible(true);
        pokemonFormField.set(pokemon, form);

        BattlePokemon battlePokemon = (BattlePokemon) unsafe.allocateInstance(BattlePokemon.class);
        Field effectedField = BattlePokemon.class.getDeclaredField("effectedPokemon");
        effectedField.setAccessible(true);
        effectedField.set(battlePokemon, pokemon);

        ActiveBattlePokemon activePokemon = (ActiveBattlePokemon) unsafe.allocateInstance(ActiveBattlePokemon.class);
        Field bpField = ActiveBattlePokemon.class.getDeclaredField("battlePokemon");
        bpField.setAccessible(true);
        bpField.set(activePokemon, battlePokemon);

        battlePokemon.setGone(isGone);

        return activePokemon;
    }

    private ActiveBattlePokemon createOpponent(int maxHp, boolean isGone) throws Exception {
        return createOpponent(maxHp, maxHp, isGone);
    }

    private RunBunAI.MoveEvaluation createEval(ActiveBattlePokemon opponent, int damage, int initialScore) throws Exception {
        MoveTemplate template = (MoveTemplate) unsafe.allocateInstance(MoveTemplate.class);
        Field nameField = MoveTemplate.class.getDeclaredField("name");
        nameField.setAccessible(true);
        nameField.set(template, "testMove");

        Move move = (Move) unsafe.allocateInstance(Move.class);
        Field templateField = Move.class.getDeclaredField("template");
        templateField.setAccessible(true);
        templateField.set(move, template);

        RunBunAI.MoveEvaluation eval = (RunBunAI.MoveEvaluation) unsafe.allocateInstance(RunBunAI.MoveEvaluation.class);
        eval.setMove(move);
        eval.setOpponent(opponent);
        eval.setDamage(damage);
        eval.setScore(initialScore);
        return eval;
    }

    private Move createMove(String name, double power, MoveTarget target, DamageCategory category) throws Exception {
        MoveTemplate template = (MoveTemplate) unsafe.allocateInstance(MoveTemplate.class);
        Field nameField = MoveTemplate.class.getDeclaredField("name");
        nameField.setAccessible(true);
        nameField.set(template, name);

        Field powerField = MoveTemplate.class.getDeclaredField("power");
        powerField.setAccessible(true);
        powerField.setDouble(template, power);

        Field targetField = MoveTemplate.class.getDeclaredField("target");
        targetField.setAccessible(true);
        targetField.set(template, target != null ? target : MoveTarget.normal);

        Field categoryField = MoveTemplate.class.getDeclaredField("damageCategory");
        categoryField.setAccessible(true);
        categoryField.set(template, category != null ? category : DamageCategories.INSTANCE.getPHYSICAL());

        Field effectChancesField = MoveTemplate.class.getDeclaredField("effectChances");
        effectChancesField.setAccessible(true);
        effectChancesField.set(template, new Double[0]);

        Move move = (Move) unsafe.allocateInstance(Move.class);
        Field templateField = Move.class.getDeclaredField("template");
        templateField.setAccessible(true);
        templateField.set(move, template);

        return move;
    }

    private RunBunAI.MoveEvaluation createEval(String moveName, ActiveBattlePokemon opponent, int damage, int initialScore) throws Exception {
        DamageCategory cat = (damage > 0) ? DamageCategories.INSTANCE.getSPECIAL() : DamageCategories.INSTANCE.getSTATUS();
        Move move = createMove(moveName, damage > 0 ? 90.0 : 0.0, MoveTarget.normal, cat);

        RunBunAI.MoveEvaluation eval = (RunBunAI.MoveEvaluation) unsafe.allocateInstance(RunBunAI.MoveEvaluation.class);
        eval.setMove(move);
        eval.setOpponent(opponent);
        eval.setDamage(damage);
        eval.setScore(initialScore);
        return eval;
    }

    private ActiveBattlePokemon createActiveBattlePokemon(
        int maxHp,
        int currentHp,
        ElementalType primaryType,
        ElementalType secondaryType,
        String abilityName,
        String heldItemId,
        List<Move> moves
    ) throws Exception {
        Pokemon pokemon = (Pokemon) unsafe.allocateInstance(Pokemon.class);
        pokemon.setUuid(UUID.randomUUID());
        pokemonHpMap.put(pokemon, maxHp);

        Field healthField = Pokemon.class.getDeclaredField("currentHealth");
        healthField.setAccessible(true);
        healthField.setInt(pokemon, currentHp);

        FormData form = new FormData();
        Field formNameField = FormData.class.getDeclaredField("name");
        formNameField.setAccessible(true);
        formNameField.set(form, "normal");

        Field ptField = FormData.class.getDeclaredField("_primaryType");
        ptField.setAccessible(true);
        ptField.set(form, primaryType != null ? primaryType : ElementalTypes.NORMAL);

        Field stField = FormData.class.getDeclaredField("_secondaryType");
        stField.setAccessible(true);
        stField.set(form, secondaryType);

        Field formField = Pokemon.class.getDeclaredField("form");
        formField.setAccessible(true);
        formField.set(pokemon, form);

        if (abilityName != null) {
            AbilityTemplate at = (AbilityTemplate) unsafe.allocateInstance(AbilityTemplate.class);
            Field atNameField = AbilityTemplate.class.getDeclaredField("name");
            atNameField.setAccessible(true);
            atNameField.set(at, abilityName);

            Ability ability = (Ability) unsafe.allocateInstance(Ability.class);
            Field aTemplateField = Ability.class.getDeclaredField("template");
            aTemplateField.setAccessible(true);
            aTemplateField.set(ability, at);

            Field abField = Pokemon.class.getDeclaredField("ability");
            abField.setAccessible(true);
            abField.set(pokemon, ability);
        }

        BattlePokemon bp = (BattlePokemon) unsafe.allocateInstance(BattlePokemon.class);
        Field effectedField = BattlePokemon.class.getDeclaredField("effectedPokemon");
        effectedField.setAccessible(true);
        effectedField.set(bp, pokemon);

        if (moves != null) {
            MoveSet ms = (MoveSet) unsafe.allocateInstance(MoveSet.class);
            Field movesArrayField = MoveSet.class.getDeclaredField("moves");
            movesArrayField.setAccessible(true);
            movesArrayField.set(ms, moves.toArray(new Move[0]));

            Field pmsField = Pokemon.class.getDeclaredField("moveSet");
            pmsField.setAccessible(true);
            pmsField.set(pokemon, ms);
        }

        if (heldItemId != null) {
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
            Field himField = BattlePokemon.class.getDeclaredField("heldItemManager$delegate");
            himField.setAccessible(true);
            himField.set(bp, LazyKt.lazyOf(manager));
        }

        ActiveBattlePokemon activePokemon = (ActiveBattlePokemon) unsafe.allocateInstance(ActiveBattlePokemon.class);
        Field bpField = ActiveBattlePokemon.class.getDeclaredField("battlePokemon");
        bpField.setAccessible(true);
        bpField.set(activePokemon, bp);

        return activePokemon;
    }

    @Test
    @DisplayName("Empty or null evaluations/opponents return false")
    void testLowPressureNullOrEmpty() throws Exception {
        ActiveBattlePokemon opp = createOpponent(100, false);
        RunBunAI.MoveEvaluation eval = createEval(opp, 15, 0);

        assertFalse(DeadMatchupDetector.isLowOffensivePressure(null, List.of(opp)));
        assertFalse(DeadMatchupDetector.isLowOffensivePressure(Collections.emptyList(), List.of(opp)));
        assertFalse(DeadMatchupDetector.isLowOffensivePressure(List.of(eval), null));
        assertFalse(DeadMatchupDetector.isLowOffensivePressure(List.of(eval), Collections.emptyList()));
    }

    @Test
    @DisplayName("All evaluations have damage 0 (status moves/immune) -> low pressure is true (ratio = 0.0 < 0.20)")
    void testLowPressureZeroDamage() throws Exception {
        ActiveBattlePokemon opp = createOpponent(100, false);
        RunBunAI.MoveEvaluation evalStatus1 = createEval(opp, 0, 6);
        RunBunAI.MoveEvaluation evalStatus2 = createEval(opp, 0, 6);

        assertTrue(DeadMatchupDetector.isLowOffensivePressure(List.of(evalStatus1, evalStatus2), List.of(opp)));
    }

    @Test
    @DisplayName("Damage 15% (15/100) is strictly below 20% threshold -> low pressure is true")
    void testLowPressureUnderThreshold() throws Exception {
        ActiveBattlePokemon opp = createOpponent(100, false);
        RunBunAI.MoveEvaluation eval = createEval(opp, 15, 0);

        assertTrue(DeadMatchupDetector.isLowOffensivePressure(List.of(eval), List.of(opp)));
    }

    @Test
    @DisplayName("Damage 20% (20/100) is exactly at threshold -> strict < 0.20 returns false")
    void testLowPressureExactThreshold() throws Exception {
        ActiveBattlePokemon opp = createOpponent(100, false);
        RunBunAI.MoveEvaluation eval = createEval(opp, 20, 0);

        assertFalse(DeadMatchupDetector.isLowOffensivePressure(List.of(eval), List.of(opp)));
    }

    @Test
    @DisplayName("Damage 35% (35/100) is above threshold -> low pressure returns false")
    void testLowPressureOverThreshold() throws Exception {
        ActiveBattlePokemon opp = createOpponent(100, false);
        RunBunAI.MoveEvaluation eval = createEval(opp, 35, 0);

        assertFalse(DeadMatchupDetector.isLowOffensivePressure(List.of(eval), List.of(opp)));
    }

    @Test
    @DisplayName("Doubles: one target takes 10% but other target takes 25% -> max is 25% >= 20% -> returns false")
    void testLowPressureDoublesMixed() throws Exception {
        ActiveBattlePokemon oppA = createOpponent(100, false);
        ActiveBattlePokemon oppB = createOpponent(100, false);

        RunBunAI.MoveEvaluation evalA = createEval(oppA, 10, 0);
        RunBunAI.MoveEvaluation evalB = createEval(oppB, 25, 0);

        assertFalse(DeadMatchupDetector.isLowOffensivePressure(List.of(evalA, evalB), List.of(oppA, oppB)));
    }

    @Test
    @DisplayName("Doubles: both targets take under 20% (12% and 18%) -> max is 18% < 20% -> returns true")
    void testLowPressureDoublesBothLow() throws Exception {
        ActiveBattlePokemon oppA = createOpponent(100, false);
        ActiveBattlePokemon oppB = createOpponent(100, false);

        RunBunAI.MoveEvaluation evalA = createEval(oppA, 12, 0);
        RunBunAI.MoveEvaluation evalB = createEval(oppB, 18, 0);

        assertTrue(DeadMatchupDetector.isLowOffensivePressure(List.of(evalA, evalB), List.of(oppA, oppB)));
    }

    @Test
    @DisplayName("Opponent is gone -> skipped, returns false if no active target remaining")
    void testLowPressureGoneOpponentSkipped() throws Exception {
        ActiveBattlePokemon goneOpp = createOpponent(100, true);
        RunBunAI.MoveEvaluation eval = createEval(goneOpp, 10, 0);

        assertFalse(DeadMatchupDetector.isLowOffensivePressure(List.of(eval), List.of(goneOpp)));
    }

    @Test
    @DisplayName("Evaluation opponent does not match active opponents list -> returns false")
    void testLowPressureUnmatchedOpponent() throws Exception {
        ActiveBattlePokemon activeOpp = createOpponent(100, false);
        ActiveBattlePokemon thirdPartyOpp = createOpponent(100, false);
        RunBunAI.MoveEvaluation eval = createEval(thirdPartyOpp, 10, 0);

        assertFalse(DeadMatchupDetector.isLowOffensivePressure(List.of(eval), List.of(activeOpp)));
    }

    @Test
    @DisplayName("Critical threat null or empty inputs return false")
    void testCriticalThreatNullOrEmpty() throws Exception {
        ActiveBattlePokemon opp = createOpponent(100, false);
        BattlePokemon self = opp.getBattlePokemon();

        assertFalse(DeadMatchupDetector.isUnderCriticalThreat(null, List.of(opp), opp, null));
        assertFalse(DeadMatchupDetector.isUnderCriticalThreat(self, null, opp, null));
        assertFalse(DeadMatchupDetector.isUnderCriticalThreat(self, Collections.emptyList(), opp, null));
    }

    @Test
    @DisplayName("Critical threat with all opponents gone returns false")
    void testCriticalThreatAllOpponentsGone() throws Exception {
        ActiveBattlePokemon goneOpp1 = createOpponent(100, true);
        ActiveBattlePokemon goneOpp2 = createOpponent(100, true);
        BattlePokemon self = goneOpp1.getBattlePokemon();

        assertFalse(DeadMatchupDetector.isUnderCriticalThreat(self, List.of(goneOpp1, goneOpp2), goneOpp1, null));
    }

    // ==========================================
    // Gate 1: isMeaningfulOffensiveMove Tests
    // ==========================================

    @Test
    @DisplayName("Case A: Status move with damage 0 and score 6 -> isMeaningfulOffensiveMove is false (no switch veto)")
    void testMeaningfulOffensiveMoveStatusMove() throws Exception {
        ActiveBattlePokemon opp = createOpponent(100, false);
        RunBunAI.MoveEvaluation evalProtect = createEval(opp, 0, 6);

        assertFalse(DeadMatchupDetector.isMeaningfulOffensiveMove(evalProtect));
    }

    @Test
    @DisplayName("Case B: Weak damaging move (36/185 = 19.4% < 20%) with score 6 -> isMeaningfulOffensiveMove is false (no switch veto)")
    void testMeaningfulOffensiveMoveWeakChip() throws Exception {
        ActiveBattlePokemon swampert = createOpponent(185, false);
        RunBunAI.MoveEvaluation evalShadowBall = createEval(swampert, 36, 6);

        assertFalse(DeadMatchupDetector.isMeaningfulOffensiveMove(evalShadowBall));
    }

    @Test
    @DisplayName("Case C: Strong damaging move (118/150 = 78.6% >= 20%) with score 9 -> isMeaningfulOffensiveMove is true (vetoes switch)")
    void testMeaningfulOffensiveMoveStrongPressure() throws Exception {
        ActiveBattlePokemon talonflame = createOpponent(150, false);
        RunBunAI.MoveEvaluation evalVoltSwitch = createEval(talonflame, 118, 9);

        assertTrue(DeadMatchupDetector.isMeaningfulOffensiveMove(evalVoltSwitch));
    }

    @Test
    @DisplayName("Case D: Threshold boundaries (19.9%, 20.0%, 25.0%, 32.9%, 33.0%, 35.0%)")
    void testDecoupledThresholdBoundaries() throws Exception {
        ActiveBattlePokemon opp = createOpponent(1000, false);

        RunBunAI.MoveEvaluation eval199 = createEval(opp, 199, 6); // 19.9%
        RunBunAI.MoveEvaluation eval200 = createEval(opp, 200, 6); // 20.0%
        RunBunAI.MoveEvaluation eval250 = createEval(opp, 250, 6); // 25.0%
        RunBunAI.MoveEvaluation eval329 = createEval(opp, 329, 6); // 32.9%
        RunBunAI.MoveEvaluation eval330 = createEval(opp, 330, 6); // 33.0%
        RunBunAI.MoveEvaluation eval350 = createEval(opp, 350, 6); // 35.0%

        // Stay justification (Gate 1: >= 33.0%)
        assertFalse(DeadMatchupDetector.isMeaningfulOffensiveMove(eval199), "19.9% must not justify staying");
        assertFalse(DeadMatchupDetector.isMeaningfulOffensiveMove(eval200), "20.0% must not justify staying");
        assertFalse(DeadMatchupDetector.isMeaningfulOffensiveMove(eval250), "25.0% must not justify staying");
        assertFalse(DeadMatchupDetector.isMeaningfulOffensiveMove(eval329), "32.9% must not justify staying");
        assertTrue(DeadMatchupDetector.isMeaningfulOffensiveMove(eval330), "33.0% must justify staying");
        assertTrue(DeadMatchupDetector.isMeaningfulOffensiveMove(eval350), ">33% must justify staying");

        // Low offensive pressure (Gate 2: < 20.0%)
        assertTrue(DeadMatchupDetector.isLowOffensivePressure(List.of(eval199), List.of(opp)), "19.9% must be low pressure");
        assertFalse(DeadMatchupDetector.isLowOffensivePressure(List.of(eval200), List.of(opp)), "20.0% must NOT be low pressure");
        assertFalse(DeadMatchupDetector.isLowOffensivePressure(List.of(eval250), List.of(opp)), "25.0% must NOT be low pressure");
        assertFalse(DeadMatchupDetector.isLowOffensivePressure(List.of(eval329), List.of(opp)), "32.9% must NOT be low pressure");
    }

    @Test
    @DisplayName("Lethal KO on a living target justifies staying even if damage is below 20% max HP")
    void testMeaningfulOffensiveMoveLethalKO() throws Exception {
        // Target has 200 max HP, but only 15 current HP
        ActiveBattlePokemon opp = createOpponent(200, 15, false);
        // Damage 18 is 9% max HP (< 20%), but 18 >= 15 (lethal KO)
        RunBunAI.MoveEvaluation evalKO = createEval(opp, 18, 6);

        assertTrue(DeadMatchupDetector.isMeaningfulOffensiveMove(evalKO));
    }

    @Test
    @DisplayName("REV-P1-01: Fainted target (current health <= 0) must return false (no false veto on dead target)")
    void testMeaningfulOffensiveMoveFaintedTarget() throws Exception {
        // Target max HP 100, but current health is 0 (fainted)
        ActiveBattlePokemon faintedOpp = createOpponent(100, 0, false);
        RunBunAI.MoveEvaluation eval = createEval(faintedOpp, 30, 6);

        assertFalse(DeadMatchupDetector.isMeaningfulOffensiveMove(eval));
    }

    @Test
    @DisplayName("Target isGone returns false")
    void testMeaningfulOffensiveMoveGoneTarget() throws Exception {
        ActiveBattlePokemon goneOpp = createOpponent(100, 100, true);
        RunBunAI.MoveEvaluation eval = createEval(goneOpp, 50, 6);

        assertFalse(DeadMatchupDetector.isMeaningfulOffensiveMove(eval));
    }

    @Test
    @DisplayName("Move with score < 6 does not satisfy isMeaningfulOffensiveMove even with high damage")
    void testMeaningfulOffensiveMoveScoreUnder6() throws Exception {
        ActiveBattlePokemon opp = createOpponent(100, false);
        RunBunAI.MoveEvaluation eval = createEval(opp, 50, 5);

        assertFalse(DeadMatchupDetector.isMeaningfulOffensiveMove(eval));
    }

    @Test
    @DisplayName("Null evaluations and null opponents safely return false")
    void testMeaningfulOffensiveMoveNulls() throws Exception {
        assertFalse(DeadMatchupDetector.isMeaningfulOffensiveMove(null));

        RunBunAI.MoveEvaluation evalNoOpp = createEval(null, 50, 6);
        assertFalse(DeadMatchupDetector.isMeaningfulOffensiveMove(evalNoOpp));
    }

    // ==========================================
    // Full Rotom-W Live Canary Reproduction Test
    // ==========================================

    @Test
    @DisplayName("Rotom-W vs Swampert + Gastrodon full live reproduction: Gate 1 does not veto, Gate 2 opens switch eligibility")
    void testRotomWLiveCanaryReproduction() throws Exception {
        ActiveBattlePokemon swampert = createOpponent(185, 185, false);
        ActiveBattlePokemon gastrodon = createOpponent(200, 200, false);
        List<ActiveBattlePokemon> opponents = List.of(swampert, gastrodon);

        // 8 evaluations matching live canary log exactly:
        RunBunAI.MoveEvaluation sbSwampert = createEval(swampert, 36, 6);
        RunBunAI.MoveEvaluation vsSwampert = createEval(swampert, 0, -50);
        RunBunAI.MoveEvaluation hpSwampert = createEval(swampert, 0, -50);
        RunBunAI.MoveEvaluation prSwampert = createEval(swampert, 0, 6);

        RunBunAI.MoveEvaluation sbGastrodon = createEval(gastrodon, 38, 6);
        RunBunAI.MoveEvaluation vsGastrodon = createEval(gastrodon, 0, -50);
        RunBunAI.MoveEvaluation hpGastrodon = createEval(gastrodon, 0, -50);
        RunBunAI.MoveEvaluation prGastrodon = createEval(gastrodon, 0, 6);

        List<RunBunAI.MoveEvaluation> evals = List.of(
            sbSwampert, vsSwampert, hpSwampert, prSwampert,
            sbGastrodon, vsGastrodon, hpGastrodon, prGastrodon
        );

        // 1. Gate 1: Stream.anyMatch(isMeaningfulOffensiveMove) MUST be FALSE (does NOT veto switch!)
        boolean gate1Veto = evals.stream().anyMatch(DeadMatchupDetector::isMeaningfulOffensiveMove);
        assertFalse(gate1Veto, "Gate 1 must not hard-veto switch on weak 36-38 dmg Shadow Ball");

        // 2. Gate 2: isLowOffensivePressure MUST be TRUE (max ratio is 38/200 = 19.0% < 20%)
        boolean lowPressure = DeadMatchupDetector.isLowOffensivePressure(evals, opponents);
        assertTrue(lowPressure, "Rotom-W must be detected as having low offensive pressure (< 20%)");

        // 3. Adjusted hasLowScore (nativeHasLowScore || lowPressure || criticalThreat)
        boolean nativeHasLowScore = false; // in live case, 8 - 4 = 4 > 2 -> false
        boolean criticalThreat = false;   // Swampert and Gastrodon do not OHKO Rotom-W
        boolean adjustedHasLowScore = nativeHasLowScore || lowPressure || criticalThreat;
        assertTrue(adjustedHasLowScore, "Rotom-W switch consideration must be opened (hasLowScore = true)");
    }

    // ==========================================
    // Phase 1 Gate 3 & Full Flow Behavioral Tests (Cases A - G)
    // ==========================================

    public static boolean simulateIsSwitchingFlow(
        List<RunBunAI.MoveEvaluation> evaluations,
        List<ActiveBattlePokemon> opponents,
        boolean nativeHasLowScore,
        boolean criticalThreat,
        double rawRng,
        double percentHp,
        boolean partySurvivabilityPass
    ) {
        return simulateIsSwitchingFlow(
            evaluations, null, Collections.emptyList(), opponents, null, null,
            nativeHasLowScore, criticalThreat, rawRng, percentHp, partySurvivabilityPass
        );
    }

    public static boolean simulateIsSwitchingFlow(
        List<RunBunAI.MoveEvaluation> evaluations,
        BattlePokemon self,
        List<ActiveBattlePokemon> allies,
        List<ActiveBattlePokemon> opponents,
        ActiveBattlePokemon activeBattlePokemon,
        RBStatStages stages,
        boolean nativeHasLowScore,
        boolean criticalThreat,
        double rawRng,
        double percentHp,
        boolean partySurvivabilityPass
    ) {
        // Gate 1: Meaningful stay veto
        boolean meaningfulStay = evaluations.stream().anyMatch(DeadMatchupDetector::isMeaningfulOffensiveMove);
        if (meaningfulStay) {
            return false;
        }

        // Gate 2: Adjust hasLowScore
        boolean deadPosition = DeadMatchupDetector.isDeadPosition(evaluations, self, allies, opponents, activeBattlePokemon, stages);
        boolean hasLowScore = nativeHasLowScore || deadPosition || criticalThreat;

        // Gate 3: Random Gate with deadPosition bypass
        double effectiveRng = DeadMatchupDetector.resolveGate3RandomValue(rawRng, deadPosition);
        if (effectiveRng >= 0.75d) {
            return false;
        }

        // Gate 4: HP Gate (> 50%)
        if (Math.ceil(percentHp) <= 50.0d) {
            return false;
        }

        // Gate 5: Party Survivability Gate
        if (partySurvivabilityPass) {
            return hasLowScore;
        }
        return false;
    }

    @Test
    @DisplayName("Case A: Live regression — lowPressure=true and rng=0.8111489023246552 bypasses Gate 3 random veto")
    void testCaseALiveRegressionGate3Bypass() {
        double liveRng = 0.8111489023246552;
        double effectiveRng = DeadMatchupDetector.resolveGate3RandomValue(liveRng, true);
        assertTrue(effectiveRng < 0.75d, "Gate 3 random veto must be bypassed for lowPressure=true");
        assertEquals(0.0d, effectiveRng);
    }

    @Test
    @DisplayName("Case B: Preserve native RNG outside dead offense — lowPressure=false and rng>=0.75 returns native veto")
    void testCaseBPreserveNativeRngOutsideDeadOffense() {
        double liveRng = 0.8111489023246552;
        double effectiveRng = DeadMatchupDetector.resolveGate3RandomValue(liveRng, false);
        assertTrue(effectiveRng >= 0.75d, "Native random veto must be preserved when lowPressure is false");
        assertEquals(liveRng, effectiveRng, 1e-9);
    }

    @Test
    @DisplayName("Case C: HP gate preserved — lowPressure=true, rng=0.811, but HP <= 50% rejects switch")
    void testCaseCHpGatePreserved() throws Exception {
        ActiveBattlePokemon opp = createOpponent(100, false);
        RunBunAI.MoveEvaluation weakEval = createEval(opp, 15, 6); // 15% < 20%
        List<RunBunAI.MoveEvaluation> evals = List.of(weakEval);

        boolean switchResult = simulateIsSwitchingFlow(
            evals, List.of(opp), false, false,
            0.8111489023246552, 45.0 /* HP <= 50% */, true
        );
        assertFalse(switchResult, "HP <= 50% must reject switch even when lowPressure bypasses Gate 3");
    }

    @Test
    @DisplayName("Case D: Party gate preserved — lowPressure=true, HP > 50%, but no survivable candidate rejects switch")
    void testCaseDPartyGatePreserved() throws Exception {
        ActiveBattlePokemon opp = createOpponent(100, false);
        RunBunAI.MoveEvaluation weakEval = createEval(opp, 15, 6);
        List<RunBunAI.MoveEvaluation> evals = List.of(weakEval);

        boolean switchResult = simulateIsSwitchingFlow(
            evals, List.of(opp), false, false,
            0.8111489023246552, 100.0 /* HP > 50% */, false /* party fail */
        );
        assertFalse(switchResult, "Party gate failure must reject switch even when lowPressure bypasses Gate 3");
    }

    @Test
    @DisplayName("Case E: Successful dead-matchup switch — lowPressure=true, HP > 50%, party pass -> switch true")
    void testCaseESuccessfulDeadMatchupSwitch() throws Exception {
        ActiveBattlePokemon opp = createOpponent(100, false);
        RunBunAI.MoveEvaluation weakEval = createEval(opp, 15, 6);
        List<RunBunAI.MoveEvaluation> evals = List.of(weakEval);

        boolean switchResult = simulateIsSwitchingFlow(
            evals, List.of(opp), false, false,
            0.8111489023246552, 100.0, true
        );
        assertTrue(switchResult, "Low pressure with HP>50% and party pass must successfully switch");
    }

    @Test
    @DisplayName("Case F: Strong offense regression — move >= 33% or lethal vetoes switch at Gate 1 before Gate 3")
    void testCaseFStrongOffenseVetoesAtGate1() throws Exception {
        ActiveBattlePokemon opp = createOpponent(100, false);
        RunBunAI.MoveEvaluation strongEval = createEval(opp, 40, 9); // 40% >= 33%
        List<RunBunAI.MoveEvaluation> evals = List.of(strongEval);

        boolean switchResult = simulateIsSwitchingFlow(
            evals, List.of(opp), false, false,
            0.50 /* rng pass */, 100.0, true
        );
        assertFalse(switchResult, "Strong move >= 33% must veto switch at Gate 1");
    }

    @Test
    @DisplayName("Case G: Gray zone (20-33% offense) — lowPressure=false, not meaningful stay, native RNG preserved")
    void testCaseGGrayZonePreservesNativeRng() throws Exception {
        ActiveBattlePokemon opp = createOpponent(100, false);
        RunBunAI.MoveEvaluation grayEval = createEval(opp, 25, 6); // 25% is in [20%, 33%)
        List<RunBunAI.MoveEvaluation> evals = List.of(grayEval);

        assertFalse(DeadMatchupDetector.isLowOffensivePressure(evals, List.of(opp)), "25% must NOT be low pressure");
        assertFalse(DeadMatchupDetector.isMeaningfulOffensiveMove(grayEval), "25% must NOT be meaningful stay");

        // When RNG >= 0.75, native veto applies
        boolean rejectWithHighRng = simulateIsSwitchingFlow(
            evals, List.of(opp), false, false,
            0.80, 100.0, true
        );
        assertFalse(rejectWithHighRng, "Gray zone with RNG >= 0.75 must be rejected by native random gate");

        // If nativeHasLowScore was true (e.g. status hole) and RNG < 0.75, passes
        boolean passWithLowRngAndNativeLowScore = simulateIsSwitchingFlow(
            evals, List.of(opp), true, false,
            0.50, 100.0, true
        );
        assertTrue(passWithLowRngAndNativeLowScore, "Gray zone with nativeHasLowScore and RNG < 0.75 passes");
    }

    // ==========================================
    // Section 8: Support-Stay Utility Regression Suite (Cases 8A - 8G)
    // ==========================================

    @Test
    @DisplayName("Case 8A: Indeedee live repro — low pressure, high ally value, Follow Me + Helping Hand -> stay")
    void testCase8A_IndeedeeLiveRepro() throws Exception {
        // Opponents: Tyranitar and Sneasel (100 HP each)
        ActiveBattlePokemon tyranitar = createActiveBattlePokemon(100, 100, ElementalTypes.INSTANCE.getROCK(), ElementalTypes.INSTANCE.getDARK(), "sandstream", null, List.of(createMove("crunch", 80.0, MoveTarget.normal, DamageCategories.INSTANCE.getPHYSICAL())));
        ActiveBattlePokemon sneasel = createActiveBattlePokemon(100, 100, ElementalTypes.INSTANCE.getDARK(), ElementalTypes.INSTANCE.getICE(), "innerfocus", null, List.of(createMove("knockoff", 65.0, MoveTarget.normal, DamageCategories.INSTANCE.getPHYSICAL())));
        List<ActiveBattlePokemon> opponents = List.of(tyranitar, sneasel);

        // Ally: Mega Alakazam with Focus Blast (120 power, lethal KO)
        Move focusBlast = createMove("focusblast", 120.0, MoveTarget.normal, DamageCategories.INSTANCE.getSPECIAL());
        ActiveBattlePokemon megaAlakazam = createActiveBattlePokemon(100, 100, ElementalTypes.INSTANCE.getPSYCHIC(), null, "trace", null, List.of(focusBlast));
        List<ActiveBattlePokemon> allies = List.of(megaAlakazam);

        // Indeedee-F: Follow Me, Helping Hand, Psychic, Protect
        Move followMe = createMove("followme", 0.0, MoveTarget.self, DamageCategories.INSTANCE.getSTATUS());
        Move helpingHand = createMove("helpinghand", 0.0, MoveTarget.adjacentAlly, DamageCategories.INSTANCE.getSTATUS());
        Move psychic = createMove("psychic", 90.0, MoveTarget.normal, DamageCategories.INSTANCE.getSPECIAL());
        Move protect = createMove("protect", 0.0, MoveTarget.self, DamageCategories.INSTANCE.getSTATUS());
        ActiveBattlePokemon indeedee = createActiveBattlePokemon(100, 100, ElementalTypes.INSTANCE.getPSYCHIC(), ElementalTypes.INSTANCE.getNORMAL(), "psychicsurge", null, List.of(followMe, helpingHand, psychic, protect));

        // Evaluations for Indeedee-F:
        // Psychic -> Tyranitar: score -50, damage 0
        // Psychic -> Sneasel: score -50, damage 0
        // Follow Me: score 6, damage 0
        // Helping Hand: score 6, damage 0
        // Protect: score 6, damage 0
        RunBunAI.MoveEvaluation evalPsychicTtar = createEval("psychic", tyranitar, 0, -50);
        RunBunAI.MoveEvaluation evalPsychicSneasel = createEval("psychic", sneasel, 0, -50);
        RunBunAI.MoveEvaluation evalFollowMe = createEval("followme", tyranitar, 0, 6);
        RunBunAI.MoveEvaluation evalHelpingHand = createEval("helpinghand", megaAlakazam, 0, 6);
        RunBunAI.MoveEvaluation evalProtect = createEval("protect", tyranitar, 0, 6);
        List<RunBunAI.MoveEvaluation> evals = List.of(evalPsychicTtar, evalPsychicSneasel, evalFollowMe, evalHelpingHand, evalProtect);

        // 1. Low offensive pressure must be true (< 20% max HP on all opponents)
        assertTrue(DeadMatchupDetector.isLowOffensivePressure(evals, opponents), "Indeedee must have low offensive pressure");

        // 2. Meaningful support utility must be true
        assertTrue(DeadMatchupDetector.hasMeaningfulSupportUtility(evals, indeedee.getBattlePokemon(), allies, opponents, indeedee, null), "Indeedee must have meaningful support utility");

        // 3. Dead position must be FALSE (lowPressure && !support)
        assertFalse(DeadMatchupDetector.isDeadPosition(evals, indeedee.getBattlePokemon(), allies, opponents, indeedee, null), "Indeedee must NOT be classified as dead position");

        // 4. In Phase 1 simulation:
        // nativeHasLowScore = false (in doubles, failCount = 2, total = 5, 5 - 2 = 3 > 2)
        // HP > 50%, party pass
        boolean switchResult = simulateIsSwitchingFlow(
            evals, indeedee.getBattlePokemon(), allies, opponents, indeedee, null,
            false /* nativeHasLowScore */, false /* criticalThreat */,
            0.50 /* rawRng */, 100.0 /* percentHp */, true /* partySurvivabilityPass */
        );
        assertFalse(switchResult, "Indeedee-F must NOT switch out; stay justification must be preserved");
    }

    @Test
    @DisplayName("Case 8B: Protect-only regression — low damage + Protect available does NOT justify staying")
    void testCase8B_ProtectOnlyRegression() throws Exception {
        ActiveBattlePokemon opp = createActiveBattlePokemon(100, 100, ElementalTypes.INSTANCE.getNORMAL(), null, null, null, List.of(createMove("tackle", 40.0, MoveTarget.normal, DamageCategories.INSTANCE.getPHYSICAL())));
        List<ActiveBattlePokemon> opponents = List.of(opp);

        Move protect = createMove("protect", 0.0, MoveTarget.self, DamageCategories.INSTANCE.getSTATUS());
        Move weakTackle = createMove("tackle", 10.0, MoveTarget.normal, DamageCategories.INSTANCE.getPHYSICAL());
        ActiveBattlePokemon self = createActiveBattlePokemon(100, 100, ElementalTypes.INSTANCE.getNORMAL(), null, null, null, List.of(protect, weakTackle));

        RunBunAI.MoveEvaluation evalProtect = createEval("protect", opp, 0, 6);
        RunBunAI.MoveEvaluation evalWeak = createEval("tackle", opp, 10, -5); // 10% < 20%
        List<RunBunAI.MoveEvaluation> evals = List.of(evalProtect, evalWeak);

        // Protect must NOT create support stay justification
        assertFalse(DeadMatchupDetector.hasMeaningfulSupportUtility(evals, self.getBattlePokemon(), Collections.emptyList(), opponents, self, null));
        assertTrue(DeadMatchupDetector.isDeadPosition(evals, self.getBattlePokemon(), Collections.emptyList(), opponents, self, null));

        // Gate 3 RNG veto must be bypassed for dead position
        double effectiveRng = DeadMatchupDetector.resolveGate3RandomValue(0.85, true);
        assertEquals(0.0d, effectiveRng);

        // Phase 1 must allow switch when HP > 50% and party pass
        boolean switchResult = simulateIsSwitchingFlow(
            evals, self.getBattlePokemon(), Collections.emptyList(), opponents, self, null,
            false, false, 0.85, 100.0, true
        );
        assertTrue(switchResult, "Protect-only dead position must switch out");
    }

    @Test
    @DisplayName("Case 8C: Helping Hand without useful ally offense — ally has no meaningful attack -> dead position")
    void testCase8C_HelpingHandWithoutUsefulAllyOffense() throws Exception {
        ActiveBattlePokemon opp = createActiveBattlePokemon(100, 100, ElementalTypes.INSTANCE.getROCK(), null, null, null, List.of(createMove("tackle", 40.0, MoveTarget.normal, DamageCategories.INSTANCE.getPHYSICAL())));
        List<ActiveBattlePokemon> opponents = List.of(opp);

        // Ally has only 0 power / status move (e.g. splash)
        Move splash = createMove("splash", 0.0, MoveTarget.self, DamageCategories.INSTANCE.getSTATUS());
        ActiveBattlePokemon ally = createActiveBattlePokemon(100, 100, ElementalTypes.INSTANCE.getWATER(), null, null, null, List.of(splash));
        List<ActiveBattlePokemon> allies = List.of(ally);

        Move helpingHand = createMove("helpinghand", 0.0, MoveTarget.adjacentAlly, DamageCategories.INSTANCE.getSTATUS());
        Move weakTackle = createMove("tackle", 5.0, MoveTarget.normal, DamageCategories.INSTANCE.getPHYSICAL());
        ActiveBattlePokemon self = createActiveBattlePokemon(100, 100, ElementalTypes.INSTANCE.getNORMAL(), null, null, null, List.of(helpingHand, weakTackle));

        RunBunAI.MoveEvaluation evalHH = createEval("helpinghand", ally, 0, 6);
        RunBunAI.MoveEvaluation evalWeak = createEval("tackle", opp, 5, -5);
        List<RunBunAI.MoveEvaluation> evals = List.of(evalHH, evalWeak);

        assertFalse(DeadMatchupDetector.hasMeaningfulAllyOffense(ally.getBattlePokemon(), opponents, self, null), "Ally with splash must have no meaningful offense");
        assertFalse(DeadMatchupDetector.hasMeaningfulSupportUtility(evals, self.getBattlePokemon(), allies, opponents, self, null), "Helping Hand with useless ally must NOT justify stay");
        assertTrue(DeadMatchupDetector.isDeadPosition(evals, self.getBattlePokemon(), allies, opponents, self, null), "Must be classified as dead position");
    }

    @Test
    @DisplayName("Case 8D: Follow Me meaningful — ally has high-value offense and redirectable threat exists -> support utility true")
    void testCase8D_FollowMeMeaningful() throws Exception {
        ActiveBattlePokemon opp = createActiveBattlePokemon(100, 100, ElementalTypes.INSTANCE.getNORMAL(), null, null, null, List.of(createMove("tackle", 40.0, MoveTarget.normal, DamageCategories.INSTANCE.getPHYSICAL())));
        List<ActiveBattlePokemon> opponents = List.of(opp);

        Move hyperBeam = createMove("hyperbeam", 150.0, MoveTarget.normal, DamageCategories.INSTANCE.getSPECIAL());
        ActiveBattlePokemon ally = createActiveBattlePokemon(100, 100, ElementalTypes.INSTANCE.getNORMAL(), null, null, null, List.of(hyperBeam));
        List<ActiveBattlePokemon> allies = List.of(ally);

        Move followMe = createMove("followme", 0.0, MoveTarget.self, DamageCategories.INSTANCE.getSTATUS());
        Move weakTackle = createMove("tackle", 5.0, MoveTarget.normal, DamageCategories.INSTANCE.getPHYSICAL());
        ActiveBattlePokemon self = createActiveBattlePokemon(100, 100, ElementalTypes.INSTANCE.getNORMAL(), null, null, null, List.of(followMe, weakTackle));

        RunBunAI.MoveEvaluation evalFollowMe = createEval("followme", opp, 0, 6);
        RunBunAI.MoveEvaluation evalWeak = createEval("tackle", opp, 5, -5);
        List<RunBunAI.MoveEvaluation> evals = List.of(evalFollowMe, evalWeak);

        assertTrue(DeadMatchupDetector.hasMeaningfulSupportUtility(evals, self.getBattlePokemon(), allies, opponents, self, null), "Follow Me with high value ally must be meaningful support");
        assertFalse(DeadMatchupDetector.isDeadPosition(evals, self.getBattlePokemon(), allies, opponents, self, null));
    }

    @Test
    @DisplayName("Case 8E: Rage Powder versus normal redirectable threats -> support utility true")
    void testCase8E_RagePowderVsNormalRedirectableThreats() throws Exception {
        ActiveBattlePokemon normalOpp = createActiveBattlePokemon(100, 100, ElementalTypes.INSTANCE.getFIRE(), null, null, null, List.of(createMove("flamethrower", 90.0, MoveTarget.normal, DamageCategories.INSTANCE.getSPECIAL())));
        List<ActiveBattlePokemon> opponents = List.of(normalOpp);

        Move hydroPump = createMove("hydropump", 110.0, MoveTarget.normal, DamageCategories.INSTANCE.getSPECIAL());
        ActiveBattlePokemon ally = createActiveBattlePokemon(100, 100, ElementalTypes.INSTANCE.getWATER(), null, null, null, List.of(hydroPump));
        List<ActiveBattlePokemon> allies = List.of(ally);

        Move ragePowder = createMove("ragepowder", 0.0, MoveTarget.self, DamageCategories.INSTANCE.getSTATUS());
        Move weakMove = createMove("absorb", 10.0, MoveTarget.normal, DamageCategories.INSTANCE.getSPECIAL());
        ActiveBattlePokemon self = createActiveBattlePokemon(100, 100, ElementalTypes.INSTANCE.getBUG(), null, null, null, List.of(ragePowder, weakMove));

        RunBunAI.MoveEvaluation evalRP = createEval("ragepowder", normalOpp, 0, 6);
        RunBunAI.MoveEvaluation evalWeak = createEval("absorb", normalOpp, 10, -5);
        List<RunBunAI.MoveEvaluation> evals = List.of(evalRP, evalWeak);

        assertTrue(DeadMatchupDetector.canRedirectOpponent(normalOpp.getBattlePokemon(), true), "Fire-type opponent must be redirectable by Rage Powder");
        assertTrue(DeadMatchupDetector.hasMeaningfulSupportUtility(evals, self.getBattlePokemon(), allies, opponents, self, null), "Rage Powder against normal threat must be meaningful");
        assertFalse(DeadMatchupDetector.isDeadPosition(evals, self.getBattlePokemon(), allies, opponents, self, null));
    }

    @Test
    @DisplayName("Case 8F: Rage Powder versus Grass-type threat(s) -> Grass immune to powder, support utility false")
    void testCase8F_RagePowderVsGrassTypeThreats() throws Exception {
        // Both opponents are Grass-type (Venusaur and Amoonguss)
        ActiveBattlePokemon venusaur = createActiveBattlePokemon(100, 100, ElementalTypes.INSTANCE.getGRASS(), ElementalTypes.INSTANCE.getPOISON(), "overgrow", null, List.of(createMove("sludgebomb", 90.0, MoveTarget.normal, DamageCategories.INSTANCE.getSPECIAL())));
        ActiveBattlePokemon amoonguss = createActiveBattlePokemon(100, 100, ElementalTypes.INSTANCE.getGRASS(), ElementalTypes.INSTANCE.getPOISON(), "effectspore", null, List.of(createMove("energyball", 90.0, MoveTarget.normal, DamageCategories.INSTANCE.getSPECIAL())));
        List<ActiveBattlePokemon> opponents = List.of(venusaur, amoonguss);

        // Grass type check
        assertTrue(DeadMatchupDetector.isGrassType(venusaur.getBattlePokemon()), "Venusaur must be Grass type");
        assertTrue(DeadMatchupDetector.isGrassType(amoonguss.getBattlePokemon()), "Amoonguss must be Grass type");

        // Neither can be redirected by Rage Powder
        assertFalse(DeadMatchupDetector.canRedirectOpponent(venusaur.getBattlePokemon(), true), "Venusaur must be immune to Rage Powder redirection");
        assertFalse(DeadMatchupDetector.canRedirectOpponent(amoonguss.getBattlePokemon(), true), "Amoonguss must be immune to Rage Powder redirection");

        // Ally has strong offense
        Move psychic = createMove("psychic", 90.0, MoveTarget.normal, DamageCategories.INSTANCE.getSPECIAL());
        ActiveBattlePokemon ally = createActiveBattlePokemon(100, 100, ElementalTypes.INSTANCE.getPSYCHIC(), null, null, null, List.of(psychic));
        List<ActiveBattlePokemon> allies = List.of(ally);

        // User has only Rage Powder
        Move ragePowder = createMove("ragepowder", 0.0, MoveTarget.self, DamageCategories.INSTANCE.getSTATUS());
        Move weakMove = createMove("tackle", 5.0, MoveTarget.normal, DamageCategories.INSTANCE.getPHYSICAL());
        ActiveBattlePokemon self = createActiveBattlePokemon(100, 100, ElementalTypes.INSTANCE.getBUG(), null, null, null, List.of(ragePowder, weakMove));

        RunBunAI.MoveEvaluation evalRP = createEval("ragepowder", venusaur, 0, 6);
        RunBunAI.MoveEvaluation evalWeak1 = createEval("tackle", venusaur, 5, -5);
        RunBunAI.MoveEvaluation evalWeak2 = createEval("tackle", amoonguss, 5, -5);
        List<RunBunAI.MoveEvaluation> evals = List.of(evalRP, evalWeak1, evalWeak2);

        // Rage Powder alone must NOT create meaningful support utility when all opponents are Grass-type
        assertFalse(DeadMatchupDetector.hasMeaningfulSupportUtility(evals, self.getBattlePokemon(), allies, opponents, self, null), "Rage Powder against 100% Grass opponents must NOT provide support utility");
        assertTrue(DeadMatchupDetector.isDeadPosition(evals, self.getBattlePokemon(), allies, opponents, self, null), "Must be classified as dead position");
    }

    @Test
    @DisplayName("Case 8G: Existing Rotom dead-matchup regression — Swampert + Gastrodon vs Rotom-W remains dead position")
    void testCase8G_ExistingRotomDeadMatchupRegression() throws Exception {
        ActiveBattlePokemon swampert = createActiveBattlePokemon(100, 100, ElementalTypes.INSTANCE.getWATER(), ElementalTypes.INSTANCE.getGROUND(), "torrent", null, List.of(createMove("earthquake", 100.0, MoveTarget.allAdjacent, DamageCategories.INSTANCE.getPHYSICAL())));
        ActiveBattlePokemon gastrodon = createActiveBattlePokemon(100, 100, ElementalTypes.INSTANCE.getWATER(), ElementalTypes.INSTANCE.getGROUND(), "stormdrain", null, List.of(createMove("earthpower", 90.0, MoveTarget.normal, DamageCategories.INSTANCE.getSPECIAL())));
        List<ActiveBattlePokemon> opponents = List.of(swampert, gastrodon);

        // Rotom-W: Hydro Pump, Thunderbolt, Volt Switch, Will-O-Wisp (all 0 damage)
        Move hydroPump = createMove("hydropump", 110.0, MoveTarget.normal, DamageCategories.INSTANCE.getSPECIAL());
        Move thunderbolt = createMove("thunderbolt", 90.0, MoveTarget.normal, DamageCategories.INSTANCE.getSPECIAL());
        ActiveBattlePokemon rotom = createActiveBattlePokemon(100, 100, ElementalTypes.INSTANCE.getELECTRIC(), ElementalTypes.INSTANCE.getWATER(), "levitate", null, List.of(hydroPump, thunderbolt));

        RunBunAI.MoveEvaluation evalTBoltSwampert = createEval("thunderbolt", swampert, 0, -50);
        RunBunAI.MoveEvaluation evalTBoltGastrodon = createEval("thunderbolt", gastrodon, 0, -50);
        RunBunAI.MoveEvaluation evalHydroSwampert = createEval("hydropump", swampert, 0, -50);
        RunBunAI.MoveEvaluation evalHydroGastrodon = createEval("hydropump", gastrodon, 0, -50);
        List<RunBunAI.MoveEvaluation> evals = List.of(evalTBoltSwampert, evalTBoltGastrodon, evalHydroSwampert, evalHydroGastrodon);

        assertTrue(DeadMatchupDetector.isLowOffensivePressure(evals, opponents), "Rotom-W must have low offensive pressure");
        assertFalse(DeadMatchupDetector.hasMeaningfulSupportUtility(evals, rotom.getBattlePokemon(), Collections.emptyList(), opponents, rotom, null), "Rotom-W has no support moves");
        assertTrue(DeadMatchupDetector.isDeadPosition(evals, rotom.getBattlePokemon(), Collections.emptyList(), opponents, rotom, null), "Rotom-W must be dead position");

        // Gate 3 RNG bypass preserved
        double effectiveRng = DeadMatchupDetector.resolveGate3RandomValue(0.8111489023246552, true);
        assertEquals(0.0d, effectiveRng, "Gate 3 random veto must be bypassed for dead position");

        // Phase 1 switch preserved
        boolean switchResult = simulateIsSwitchingFlow(
            evals, rotom.getBattlePokemon(), Collections.emptyList(), opponents, rotom, null,
            false, false, 0.8111489023246552, 100.0, true
        );
        assertTrue(switchResult, "Rotom-W must switch out");
    }

    @Test
    @DisplayName("Case 8H: Disabled/Taunted support move — evaluated with score <= -5 must NOT fall through to raw moveset")
    void testCase8H_DisabledSupportMoveDoesNotFallThroughToMoveset() throws Exception {
        ActiveBattlePokemon opp = createActiveBattlePokemon(100, 100, ElementalTypes.INSTANCE.getNORMAL(), null, null, null, List.of(createMove("tackle", 40.0, MoveTarget.normal, DamageCategories.INSTANCE.getPHYSICAL())));
        List<ActiveBattlePokemon> opponents = List.of(opp);

        // High-value offensive ally
        Move hyperBeam = createMove("hyperbeam", 150.0, MoveTarget.normal, DamageCategories.INSTANCE.getSPECIAL());
        ActiveBattlePokemon ally = createActiveBattlePokemon(100, 100, ElementalTypes.INSTANCE.getNORMAL(), null, null, null, List.of(hyperBeam));
        List<ActiveBattlePokemon> allies = List.of(ally);

        // Self knows Follow Me in raw moveset, plus weak attack
        Move followMe = createMove("followme", 0.0, MoveTarget.self, DamageCategories.INSTANCE.getSTATUS());
        Move weakTackle = createMove("tackle", 5.0, MoveTarget.normal, DamageCategories.INSTANCE.getPHYSICAL());
        ActiveBattlePokemon self = createActiveBattlePokemon(100, 100, ElementalTypes.INSTANCE.getNORMAL(), null, null, null, List.of(followMe, weakTackle));

        // Follow Me was evaluated by AI but scored -50 (e.g. Taunt / Torment / Disable / Choice-locked)
        RunBunAI.MoveEvaluation evalFollowMeTaunted = createEval("followme", opp, 0, -50);
        RunBunAI.MoveEvaluation evalWeak = createEval("tackle", opp, 5, -5);
        List<RunBunAI.MoveEvaluation> evals = List.of(evalFollowMeTaunted, evalWeak);

        // Evaluated unusable support move must NOT count as available support utility
        assertFalse(DeadMatchupDetector.hasMeaningfulSupportUtility(evals, self.getBattlePokemon(), allies, opponents, self, null),
            "Taunted/disabled Follow Me (score <= -5) must NOT grant support utility despite raw moveset presence");
        assertTrue(DeadMatchupDetector.isDeadPosition(evals, self.getBattlePokemon(), allies, opponents, self, null),
            "Pokemon with only unusable support moves and low damage must be classified as dead position");

        // Phase 1 switch preserved when HP > 50% and party pass
        boolean switchResult = simulateIsSwitchingFlow(
            evals, self.getBattlePokemon(), allies, opponents, self, null,
            false, false, 0.85, 100.0, true
        );
        assertTrue(switchResult, "Taunted dead-position support Pokemon must switch out");
    }
}
