package com.cobbleverse.legendaryrule.strategy.switchai;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.api.pokemon.stats.StatProvider;
import com.cobblemon.mod.common.battles.ActiveBattlePokemon;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.gitlab.surilexa.rbrctai.api.ai.RunBunAI;
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
}
