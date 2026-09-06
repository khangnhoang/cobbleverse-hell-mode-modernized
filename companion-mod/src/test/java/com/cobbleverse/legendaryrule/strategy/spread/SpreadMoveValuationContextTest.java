package com.cobbleverse.legendaryrule.strategy.spread;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.battles.ActiveBattlePokemon;
import com.cobblemon.mod.common.battles.MoveTarget;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.gitlab.surilexa.rbrctai.api.ai.RunBunAI;
import com.cobblemon.mod.common.api.pokemon.stats.StatProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class SpreadMoveValuationContextTest {

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
            // ignore if already initialized or running in environment where Bootstrap is unavailable
        }

        Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        unsafe = (Unsafe) f.get(null);

        // Configure stat provider proxy to return controlled max HP for test Pokemon instances
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

    private Move createMove(String name, MoveTarget target) throws Exception {
        MoveTemplate template = (MoveTemplate) unsafe.allocateInstance(MoveTemplate.class);
        Field nameField = MoveTemplate.class.getDeclaredField("name");
        nameField.setAccessible(true);
        nameField.set(template, name);

        Field targetField = MoveTemplate.class.getDeclaredField("target");
        targetField.setAccessible(true);
        targetField.set(template, target);

        Move move = (Move) unsafe.allocateInstance(Move.class);
        Field templateField = Move.class.getDeclaredField("template");
        templateField.setAccessible(true);
        templateField.set(move, template);

        return move;
    }

    private ActiveBattlePokemon createOpponent(int maxHp) throws Exception {
        Pokemon pokemon = (Pokemon) unsafe.allocateInstance(Pokemon.class);
        pokemonHpMap.put(pokemon, maxHp);

        BattlePokemon battlePokemon = (BattlePokemon) unsafe.allocateInstance(BattlePokemon.class);
        Field effectedField = BattlePokemon.class.getDeclaredField("effectedPokemon");
        effectedField.setAccessible(true);
        effectedField.set(battlePokemon, pokemon);

        ActiveBattlePokemon activePokemon = (ActiveBattlePokemon) unsafe.allocateInstance(ActiveBattlePokemon.class);
        Field bpField = ActiveBattlePokemon.class.getDeclaredField("battlePokemon");
        bpField.setAccessible(true);
        bpField.set(activePokemon, battlePokemon);

        return activePokemon;
    }

    private RunBunAI.MoveEvaluation createEval(Move move, ActiveBattlePokemon opponent, int damage, int initialScore) throws Exception {
        RunBunAI.MoveEvaluation eval = (RunBunAI.MoveEvaluation) unsafe.allocateInstance(RunBunAI.MoveEvaluation.class);
        eval.setMove(move);
        eval.setOpponent(opponent);
        eval.setDamage(damage);
        eval.setScore(initialScore);
        return eval;
    }

    @Test
    @DisplayName("CASE C: allAdjacentFoes vs two neutral opponents -> rankingDamage = dmgA + dmgB")
    void testSpreadMoveTwoNeutralOpponentsSumsRankingDamage() throws Exception {
        Move electroweb = createMove("electroweb", MoveTarget.allAdjacentFoes);
        ActiveBattlePokemon oppA = createOpponent(100);
        ActiveBattlePokemon oppB = createOpponent(100);

        RunBunAI.MoveEvaluation evalA = createEval(electroweb, oppA, 45, 0);
        RunBunAI.MoveEvaluation evalB = createEval(electroweb, oppB, 55, 0);

        SpreadMoveValuationContext ctx = SpreadMoveValuationContext.fromEvaluations(List.of(evalA, evalB));

        assertTrue(ctx.isAggregatedSpreadMove(electroweb));
        assertEquals(100, ctx.getRankingDamage(evalA));
        assertEquals(100, ctx.getRankingDamage(evalB));
    }

    @Test
    @DisplayName("CASE D: Second opponent immune -> rankingDamage = dmgA + 0 = dmgA")
    void testSpreadMoveWithImmuneOpponentPreservesPartialDamage() throws Exception {
        Move electroweb = createMove("electroweb", MoveTarget.allAdjacentFoes);
        ActiveBattlePokemon oppA = createOpponent(100);
        ActiveBattlePokemon oppBImmune = createOpponent(100);

        RunBunAI.MoveEvaluation evalA = createEval(electroweb, oppA, 60, 0);
        RunBunAI.MoveEvaluation evalB = createEval(electroweb, oppBImmune, 0, 0); // 0 damage due to Ground immunity

        SpreadMoveValuationContext ctx = SpreadMoveValuationContext.fromEvaluations(List.of(evalA, evalB));

        assertTrue(ctx.isAggregatedSpreadMove(electroweb));
        assertEquals(60, ctx.getRankingDamage(evalA));
        assertEquals(60, ctx.getRankingDamage(evalB));
    }

    @Test
    @DisplayName("CASE E: Asymmetric defenses -> independently calculated A/B damages correctly summed")
    void testSpreadMoveAsymmetricDefensesSummed() throws Exception {
        Move icyWind = createMove("icywind", MoveTarget.allAdjacentFoes);
        ActiveBattlePokemon dragonTarget = createOpponent(200); // 4x weak
        ActiveBattlePokemon steelTarget = createOpponent(150);  // 2x resist

        RunBunAI.MoveEvaluation evalDragon = createEval(icyWind, dragonTarget, 140, 0);
        RunBunAI.MoveEvaluation evalSteel = createEval(icyWind, steelTarget, 25, 0);

        SpreadMoveValuationContext ctx = SpreadMoveValuationContext.fromEvaluations(List.of(evalDragon, evalSteel));

        assertEquals(165, ctx.getRankingDamage(evalDragon));
        assertEquals(165, ctx.getRankingDamage(evalSteel));
    }

    @Test
    @DisplayName("CASE F: Normalized pressure = dmgA/maxHpA + dmgB/maxHpB")
    void testSpreadMoveNormalizedPressureCalculation() throws Exception {
        Move snarl = createMove("snarl", MoveTarget.allAdjacentFoes);
        ActiveBattlePokemon oppA = createOpponent(200);
        ActiveBattlePokemon oppB = createOpponent(100);

        // oppA takes 40/200 = 0.20 pressure
        // oppB takes 20/100 = 0.20 pressure
        // Total board pressure = 0.40 >= 0.30 threshold
        RunBunAI.MoveEvaluation evalA = createEval(snarl, oppA, 40, -5);
        RunBunAI.MoveEvaluation evalB = createEval(snarl, oppB, 20, -5);

        SpreadMoveValuationContext ctx = SpreadMoveValuationContext.fromEvaluations(List.of(evalA, evalB));

        double pressure = ctx.getNormalizedPressure(snarl, 0.0);
        assertEquals(0.40, pressure, 0.001);

        // Applying pressure adjustment should promote both from -5 to +5
        ctx.adjustSpreadPressureScore(evalA);
        ctx.adjustSpreadPressureScore(evalB);

        assertEquals(5, evalA.getScore(), "evalA should be promoted to +5 due to board pressure");
        assertEquals(5, evalB.getScore(), "evalB should be promoted to +5 due to board pressure");
    }

    @Test
    @DisplayName("CASE G: MoveEvaluation target-local damage remains strictly unchanged (ZERO mutation)")
    void testMoveEvaluationDamageNeverMutated() throws Exception {
        Move overdrive = createMove("overdrive", MoveTarget.allAdjacentFoes);
        ActiveBattlePokemon oppA = createOpponent(100);
        ActiveBattlePokemon oppB = createOpponent(100);

        RunBunAI.MoveEvaluation evalA = createEval(overdrive, oppA, 70, 0);
        RunBunAI.MoveEvaluation evalB = createEval(overdrive, oppB, 80, 0);

        SpreadMoveValuationContext ctx = SpreadMoveValuationContext.fromEvaluations(List.of(evalA, evalB));

        // Ranking damage is aggregated
        assertEquals(150, ctx.getRankingDamage(evalA));
        assertEquals(150, ctx.getRankingDamage(evalB));

        // Crucial invariant: original target-local damage MUST NOT be mutated
        assertEquals(70, evalA.getDamage(), "evalA target-local damage must remain exactly 70");
        assertEquals(80, evalB.getDamage(), "evalB target-local damage must remain exactly 80");
    }

    @Test
    @DisplayName("CASE H: Target with zero damage (immunity) is NOT promoted to +5 even if board pressure >= 0.30")
    void testImmuneTargetNotPromotedByPressure() throws Exception {
        Move electroweb = createMove("electroweb", MoveTarget.allAdjacentFoes);
        ActiveBattlePokemon oppA = createOpponent(100);
        ActiveBattlePokemon oppBImmune = createOpponent(100);

        // oppA takes 50/100 = 0.50 (board pressure >= 0.30)
        // oppB takes 0/100 = 0 (immune)
        RunBunAI.MoveEvaluation evalA = createEval(electroweb, oppA, 50, -5);
        RunBunAI.MoveEvaluation evalBImmune = createEval(electroweb, oppBImmune, 0, -5);

        SpreadMoveValuationContext ctx = SpreadMoveValuationContext.fromEvaluations(List.of(evalA, evalBImmune));

        ctx.adjustSpreadPressureScore(evalA);
        ctx.adjustSpreadPressureScore(evalBImmune);

        assertEquals(5, evalA.getScore(), "Non-immune target should be promoted to +5");
        assertEquals(-5, evalBImmune.getScore(), "Immune target (dmg <= 0) must remain penalized at -5");
    }

    @Test
    @DisplayName("CASE I: allAdjacent moves (Earthquake, Surf, Sludge Wave) receive NO aggregation")
    void testAllAdjacentMovesStrictlyExcluded() throws Exception {
        Move earthquake = createMove("earthquake", MoveTarget.allAdjacent);
        ActiveBattlePokemon oppA = createOpponent(100);
        ActiveBattlePokemon oppB = createOpponent(100);

        RunBunAI.MoveEvaluation evalA = createEval(earthquake, oppA, 90, 0);
        RunBunAI.MoveEvaluation evalB = createEval(earthquake, oppB, 95, 0);

        SpreadMoveValuationContext ctx = SpreadMoveValuationContext.fromEvaluations(List.of(evalA, evalB));

        assertFalse(ctx.isAggregatedSpreadMove(earthquake), "allAdjacent moves must not be treated as aggregated spread");
        assertEquals(90, ctx.getRankingDamage(evalA), "evalA ranking damage must remain native 90");
        assertEquals(95, ctx.getRankingDamage(evalB), "evalB ranking damage must remain native 95");
    }

    @Test
    @DisplayName("Single target on board: allAdjacentFoes move does not aggregate when only 1 opponent")
    void testSingleOpponentDoesNotAggregate() throws Exception {
        Move electroweb = createMove("electroweb", MoveTarget.allAdjacentFoes);
        ActiveBattlePokemon oppSingle = createOpponent(100);

        RunBunAI.MoveEvaluation evalSingle = createEval(electroweb, oppSingle, 45, 0);

        SpreadMoveValuationContext ctx = SpreadMoveValuationContext.fromEvaluations(List.of(evalSingle));

        assertFalse(ctx.isAggregatedSpreadMove(electroweb), "Single target on board should not trigger spread aggregation");
        assertEquals(45, ctx.getRankingDamage(evalSingle));
    }

    @Test
    @DisplayName("Null safety and empty context behavior")
    void testNullAndEmptyContextSafety() throws Exception {
        SpreadMoveValuationContext emptyCtx = SpreadMoveValuationContext.fromEvaluations(Collections.emptyList());
        assertNotNull(emptyCtx);
        assertEquals(42, emptyCtx.getRankingDamage(createEval(null, null, 42, 0)));

        SpreadMoveValuationContext nullCtx = SpreadMoveValuationContext.fromEvaluations(null);
        assertNotNull(nullCtx);
        assertFalse(nullCtx.isAggregatedSpreadMove(null));
        assertEquals(0.0, nullCtx.getNormalizedPressure(null, 0.0));
    }
}
