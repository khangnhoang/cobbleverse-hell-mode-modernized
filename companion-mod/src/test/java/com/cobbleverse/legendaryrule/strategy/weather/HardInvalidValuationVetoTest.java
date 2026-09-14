package com.cobbleverse.legendaryrule.strategy.weather;

import com.cobblemon.mod.common.api.battles.interpreter.BasicContext;
import com.cobblemon.mod.common.api.battles.interpreter.BattleContext;
import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.api.moves.categories.DamageCategories;
import com.cobblemon.mod.common.api.pokemon.helditem.HeldItemManager;
import com.cobblemon.mod.common.api.types.ElementalType;
import com.cobblemon.mod.common.api.types.ElementalTypes;
import com.cobblemon.mod.common.battles.ActiveBattlePokemon;
import com.cobblemon.mod.common.battles.MoveTarget;
import com.cobblemon.mod.common.battles.interpreter.ContextManager;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.gitlab.surilexa.rbrctai.api.ai.RunBunAI;
import kotlin.LazyKt;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regressions for the hard-invalid terminal veto (H1–H6).
 * <p>
 * Run &amp; Bun writes {@link WeatherAccuracyValuationStrategy#HARD_INVALID_SCORE} when it has ruled a move
 * unusable against a specific target. The companion's comparative corrections used to treat that sentinel as
 * an ordinary rank: on a live Blaine board, Mega Golisopod's First Impression (Bug, 90 BP) sits in the same
 * dominance pair as Leech Life (Bug, 80 BP), so the same-type branch of
 * {@link WeatherAccuracyValuationStrategy#compareWeatherMoveDominance} declared First Impression dominant and
 * the enforcement step rewrote its sentinel to {@code leechLifeScore + 1} — observed on the canary as
 * {@code firstimpression -> Tyranitar score=10} and {@code -> Garchomp score=12} from a raw {@code -50}.
 * <p>
 * The move identities below reproduce that exact shape because the defect is about score state, not about the
 * move: any same-type candidate pair produces it.
 */
class HardInvalidValuationVetoTest {

    private static Unsafe unsafe;
    private static Field bpContextManagerField;
    private static Field bpHeldItemDelegateField;
    private static Field bpEffectedPokemonField;

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
    }

    private Move createMove(String name, ElementalType type, double power, double accuracy) {
        MoveTemplate template = new MoveTemplate(
            name,
            1,
            type,
            DamageCategories.INSTANCE.getPHYSICAL(),
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

    private BattlePokemon createBattlePokemon(String weatherId) throws Exception {
        BattlePokemon bp = (BattlePokemon) unsafe.allocateInstance(BattlePokemon.class);

        Pokemon pokemon = (Pokemon) unsafe.allocateInstance(Pokemon.class);
        pokemon.setUuid(UUID.randomUUID());
        bpEffectedPokemonField.set(bp, pokemon);

        ContextManager cm = new ContextManager();
        if (weatherId != null) {
            cm.add(new BasicContext(weatherId, 1, BattleContext.Type.WEATHER, null));
        }
        bpContextManagerField.set(bp, cm);

        HeldItemManager manager = (HeldItemManager) Proxy.newProxyInstance(
            HeldItemManager.class.getClassLoader(),
            new Class<?>[]{HeldItemManager.class},
            (proxy, method, args) -> "showdownId".equals(method.getName()) ? "" : null
        );
        bpHeldItemDelegateField.set(bp, LazyKt.lazyOf(manager));

        return bp;
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

    private Move firstImpression() {
        return createMove("firstimpression", ElementalTypes.BUG, 90.0d, 100.0d);
    }

    private Move leechLife() {
        return createMove("leechlife", ElementalTypes.BUG, 80.0d, 100.0d);
    }

    private static RunBunAI.MoveEvaluation byMoveName(List<RunBunAI.MoveEvaluation> evaluations, String moveName) {
        return evaluations.stream()
            .filter(e -> e.getMove() != null && moveName.equalsIgnoreCase(e.getMove().getName()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("No evaluation for move " + moveName));
    }

    /** H1 — the first active turn, where Run & Bun considers First Impression usable, must be untouched. */
    @Test
    @DisplayName("H1: a usable First Impression on its first active turn is not suppressed")
    void h1_usableFirstImpressionIsNotSuppressed() throws Exception {
        BattlePokemon attacker = createBattlePokemon(null);
        ActiveBattlePokemon abp = createActiveBattlePokemon(attacker);
        ActiveBattlePokemon target = createActiveBattlePokemon(createBattlePokemon(null));

        RunBunAI.MoveEvaluation impression = createEvaluation(firstImpression(), 40, 12, target);
        RunBunAI.MoveEvaluation leech = createEvaluation(leechLife(), 30, 10, target);
        List<RunBunAI.MoveEvaluation> evaluations = new ArrayList<>(List.of(impression, leech));

        WeatherAccuracyValuationStrategy.adjustMoveValuations(evaluations, attacker, abp, null);

        assertEquals(12, impression.getScore(),
            "A usable First Impression must keep its raw score and must not be vetoed");
        assertFalse(WeatherAccuracyValuationStrategy.isHardInvalidEvaluation(impression),
            "A usable candidate must not be classified as hard-invalid");
        assertEquals(12, evaluations.stream().mapToInt(RunBunAI.MoveEvaluation::getScore).max().orElseThrow(),
            "First Impression must remain the highest-scoring candidate on its first active turn");
    }

    /**
     * H2 — the live canary reproduction. Two opposing targets, First Impression hard-invalid against both, the
     * companion's dominance step rewrote the sentinel per target into {@code leechLifeScore + 1}: 9 -> 10 and
     * 11 -> 12, exactly the {@code score=10} / {@code score=12} pair the canary logged before choosing it.
     */
    @Test
    @DisplayName("H2: a hard-invalid First Impression on a later turn is not resurrected into a winner")
    void h2_hardInvalidIsNotResurrected() throws Exception {
        BattlePokemon attacker = createBattlePokemon(null);
        ActiveBattlePokemon abp = createActiveBattlePokemon(attacker);
        ActiveBattlePokemon tyranitar = createActiveBattlePokemon(createBattlePokemon(null));
        ActiveBattlePokemon garchomp = createActiveBattlePokemon(createBattlePokemon(null));

        RunBunAI.MoveEvaluation impressionVsTyranitar = createEvaluation(firstImpression(), 0, -50, tyranitar);
        RunBunAI.MoveEvaluation leechVsTyranitar = createEvaluation(leechLife(), 30, 9, tyranitar);
        RunBunAI.MoveEvaluation impressionVsGarchomp = createEvaluation(firstImpression(), 0, -50, garchomp);
        RunBunAI.MoveEvaluation leechVsGarchomp = createEvaluation(leechLife(), 30, 11, garchomp);

        List<RunBunAI.MoveEvaluation> evaluations = new ArrayList<>(List.of(
            impressionVsTyranitar, leechVsTyranitar, impressionVsGarchomp, leechVsGarchomp));

        // Precondition: the same-type dominance branch does declare First Impression dominant over Leech Life,
        // which is what made the enforcement step rewrite the sentinel.
        assertTrue(WeatherAccuracyValuationStrategy.compareWeatherMoveDominance(
                firstImpression(), leechLife(), (String) null) > 0,
            "Precondition: First Impression dominates Leech Life on paper (Bug 90 BP vs Bug 80 BP)");

        WeatherAccuracyValuationStrategy.adjustMoveValuations(evaluations, attacker, abp, null);

        assertEquals(-50, impressionVsTyranitar.getScore(),
            "The raw hard-invalid state must survive the dominance step (previously rewritten to 10)");
        assertEquals(-50, impressionVsGarchomp.getScore(),
            "The raw hard-invalid state must survive the dominance step (previously rewritten to 12)");
        assertEquals(11, evaluations.stream().mapToInt(RunBunAI.MoveEvaluation::getScore).max().orElseThrow(),
            "The best remaining candidate must be the usable Leech Life line");
        assertNotEquals(11, impressionVsGarchomp.getScore(),
            "First Impression must not become a winning candidate again");
    }

    /** H3 — the veto holds in both dominance directions, so a sentinel is neither raised nor donated. */
    @Test
    @DisplayName("H3: a hard-invalid candidate is never raised, and never donates its sentinel as a baseline")
    void h3_hardInvalidIsNeverRaisedInEitherDirection() throws Exception {
        BattlePokemon attacker = createBattlePokemon(null);
        ActiveBattlePokemon abp = createActiveBattlePokemon(attacker);
        ActiveBattlePokemon target = createActiveBattlePokemon(createBattlePokemon(null));

        // Direction 1: the hard-invalid move is the dominant side (the canary direction).
        RunBunAI.MoveEvaluation dominantButInvalid = createEvaluation(firstImpression(), 0, -50, target);
        RunBunAI.MoveEvaluation usableLoser = createEvaluation(leechLife(), 30, 4, target);
        List<RunBunAI.MoveEvaluation> direction1 = new ArrayList<>(List.of(dominantButInvalid, usableLoser));
        WeatherAccuracyValuationStrategy.adjustMoveValuations(direction1, attacker, abp, null);
        assertEquals(-50, dominantButInvalid.getScore(),
            "A hard-invalid candidate must not be raised by dominating a usable move");

        // Direction 2: the hard-invalid move is the dominated side; it must not be raised either, and a usable
        // move must not be pulled down to the sentinel's neighbourhood as a baseline.
        Move thunder = createMove("thunder", ElementalTypes.ELECTRIC, 110.0d, 70.0d);
        Move thunderbolt = createMove("thunderbolt", ElementalTypes.ELECTRIC, 90.0d, 100.0d);
        RunBunAI.MoveEvaluation invalidLoser = createEvaluation(thunder, 0, -50, target);
        RunBunAI.MoveEvaluation usableWinner = createEvaluation(thunderbolt, 30, 4, target);
        List<RunBunAI.MoveEvaluation> direction2 = new ArrayList<>(List.of(invalidLoser, usableWinner));
        WeatherAccuracyValuationStrategy.adjustMoveValuations(direction2, attacker, abp, null);
        assertEquals(-50, invalidLoser.getScore(),
            "A hard-invalid candidate must not be raised when the usable move dominates it");
        assertEquals(4, usableWinner.getScore(),
            "A usable move must not be re-scored from a hard-invalid baseline");
    }

    /** H4 — the post-raw weather bonus must not lift a hard-invalid move (Thunder vs a Ground-type target). */
    @Test
    @DisplayName("H4: a hard-invalid Thunder receives no rain bonus")
    void h4_hardInvalidMoveReceivesNoWeatherBonus() throws Exception {
        BattlePokemon attacker = createBattlePokemon("rain");
        ActiveBattlePokemon abp = createActiveBattlePokemon(attacker);
        ActiveBattlePokemon groundedTarget = createActiveBattlePokemon(createBattlePokemon(null));

        Move thunder = createMove("thunder", ElementalTypes.ELECTRIC, 110.0d, 70.0d);
        Move thunderbolt = createMove("thunderbolt", ElementalTypes.ELECTRIC, 90.0d, 100.0d);

        // Thunder is immune against this target, so Run & Bun scored it hard-invalid; the rain accuracy bonus
        // must not pull it back toward contention.
        RunBunAI.MoveEvaluation invalidThunder = createEvaluation(thunder, 0, -50, groundedTarget);
        RunBunAI.MoveEvaluation usableThunderbolt = createEvaluation(thunderbolt, 30, 6, groundedTarget);
        List<RunBunAI.MoveEvaluation> evaluations = new ArrayList<>(List.of(invalidThunder, usableThunderbolt));

        WeatherAccuracyValuationStrategy.adjustMoveValuations(evaluations, attacker, abp, null);

        assertEquals(-50, invalidThunder.getScore(),
            "The rain bonus must not be applied to a hard-invalid candidate (previously -50 + 7 = -43)");
        assertEquals(6, usableThunderbolt.getScore(),
            "The usable alternative must keep its raw score; nothing here makes it dominate");
    }

    /** H5 — the veto is narrow: ordinary weather bonuses and dominance enforcement still work. */
    @Test
    @DisplayName("H5: usable candidates keep their weather bonus and dominance enforcement")
    void h5_vetoIsNarrowAndDoesNotDisableExistingCorrections() throws Exception {
        BattlePokemon attacker = createBattlePokemon("rain");
        ActiveBattlePokemon abp = createActiveBattlePokemon(attacker);
        ActiveBattlePokemon target = createActiveBattlePokemon(createBattlePokemon(null));

        Move thunder = createMove("thunder", ElementalTypes.ELECTRIC, 110.0d, 70.0d);
        Move thunderbolt = createMove("thunderbolt", ElementalTypes.ELECTRIC, 90.0d, 100.0d);

        // The pre-existing RNG-inversion scenario, unchanged: Thunder rolled lower and is not hard-invalid.
        RunBunAI.MoveEvaluation evalThunder = createEvaluation(thunder, 80, 12, target);
        RunBunAI.MoveEvaluation evalThunderbolt = createEvaluation(thunderbolt, 60, 14, target);
        List<RunBunAI.MoveEvaluation> evaluations = new ArrayList<>(List.of(evalThunder, evalThunderbolt));

        WeatherAccuracyValuationStrategy.adjustMoveValuations(evaluations, attacker, abp, null);

        assertEquals(19, evalThunder.getScore(),
            "A usable Thunder must still receive the +7 rain bonus (12 + 7)");
        assertTrue(evalThunder.getScore() > evalThunderbolt.getScore(),
            "Dominance enforcement must still eliminate the RNG inversion for usable candidates");
    }

    /**
     * H6 — the veto is stateless and keyed on Run &amp; Bun's own verdict, so a move that becomes usable again
     * after switching out and back in is neither remembered as invalid nor penalized.
     */
    @Test
    @DisplayName("H6: a move that becomes usable again after re-entry is left alone")
    void h6_reEntryBecomesUsableAgain() throws Exception {
        BattlePokemon attacker = createBattlePokemon(null);
        ActiveBattlePokemon abp = createActiveBattlePokemon(attacker);
        ActiveBattlePokemon target = createActiveBattlePokemon(createBattlePokemon(null));

        RunBunAI.MoveEvaluation impression = createEvaluation(firstImpression(), 0, -50, target);
        RunBunAI.MoveEvaluation leech = createEvaluation(leechLife(), 30, 9, target);
        List<RunBunAI.MoveEvaluation> evaluations = new ArrayList<>(List.of(impression, leech));

        WeatherAccuracyValuationStrategy.adjustMoveValuations(evaluations, attacker, abp, null);
        assertEquals(-50, impression.getScore(), "On the turn it is unusable the sentinel is preserved");

        // Switch out and back in: Run & Bun re-evaluates and now considers the move usable again.
        impression.setScore(12);
        impression.setDamage(40);
        leech.setScore(10);

        WeatherAccuracyValuationStrategy.adjustMoveValuations(evaluations, attacker, abp, null);

        assertEquals(12, impression.getScore(),
            "The veto must not persist: once Run & Bun says usable, the companion must not suppress or lower it");
        assertFalse(WeatherAccuracyValuationStrategy.isHardInvalidEvaluation(impression));
        assertEquals(12, evaluations.stream().mapToInt(RunBunAI.MoveEvaluation::getScore).max().orElseThrow(),
            "The re-entered move must be able to win again");
    }
}
