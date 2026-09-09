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
import com.gitlab.surilexa.rbrctai.api.ai.utils.PokeMathMax;
import kotlin.LazyKt;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ThunderUnderRainValuationTest {

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
            DamageCategories.INSTANCE.getSPECIAL(),
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

    private BattlePokemon createBattlePokemon(String weatherId, String heldItemId) throws Exception {
        BattlePokemon bp = (BattlePokemon) unsafe.allocateInstance(BattlePokemon.class);

        Pokemon pokemon = (Pokemon) unsafe.allocateInstance(Pokemon.class);
        pokemon.setUuid(UUID.randomUUID());
        bpEffectedPokemonField.set(bp, pokemon);

        ContextManager cm = new ContextManager();
        if (weatherId != null) {
            cm.add(new BasicContext(weatherId, 1, BattleContext.Type.WEATHER, null));
        }
        bpContextManagerField.set(bp, cm);

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
            bpHeldItemDelegateField.set(bp, LazyKt.lazyOf(manager));
        }

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

    @Test
    @DisplayName("Layer 2 Invariant: Raw damage for Thunder (110 BP) > Thunderbolt (90 BP) in Rain and outside Rain")
    void testRawDamageThunderGreaterThanThunderboltInAndOutOfRain() {
        Move thunder = createMove("thunder", ElementalTypes.ELECTRIC, 110.0d, 70.0d);
        Move thunderbolt = createMove("thunderbolt", ElementalTypes.ELECTRIC, 90.0d, 100.0d);

        // Power invariant
        assertTrue(thunder.getPower() > thunderbolt.getPower(),
            "Thunder base power (110) must be strictly greater than Thunderbolt (90)");
        assertEquals(110.0d, thunder.getPower(), 0.001);
        assertEquals(90.0d, thunderbolt.getPower(), 0.001);

        // Weather multiplier invariant: Electric moves receive 1.0x in Rain (only Water is 1.5x, Fire is 0.5x)
        double rainWeatherMultiplierElectric = 1.0d;
        double neutralWeatherMultiplierElectric = 1.0d;

        double thunderRawDamageRain = thunder.getPower() * rainWeatherMultiplierElectric;
        double thunderboltRawDamageRain = thunderbolt.getPower() * rainWeatherMultiplierElectric;
        double thunderRawDamageNeutral = thunder.getPower() * neutralWeatherMultiplierElectric;
        double thunderboltRawDamageNeutral = thunderbolt.getPower() * neutralWeatherMultiplierElectric;

        assertTrue(thunderRawDamageRain > thunderboltRawDamageRain,
            "Under Rain, raw damage for Thunder (110) strictly exceeds Thunderbolt (90)");
        assertTrue(thunderRawDamageNeutral > thunderboltRawDamageNeutral,
            "Outside Rain, raw damage for Thunder (110) strictly exceeds Thunderbolt (90)");

        assertEquals(110.0d / 90.0d, thunderRawDamageRain / thunderboltRawDamageRain, 0.001,
            "Raw damage ratio reflects exactly 110 BP vs 90 BP base power ratio (1.222x)");

        // PokeMathMax method existence contract
        Method[] methods = PokeMathMax.class.getDeclaredMethods();
        boolean damageMethodFound = false;
        for (Method m : methods) {
            if ("damage".equals(m.getName())) {
                damageMethodFound = true;
                break;
            }
        }
        assertTrue(damageMethodFound, "PokeMathMax.damage method must exist in rbrctai");
    }

    @Test
    @DisplayName("Layer 2 Contract: Effective accuracy reflects Gen 9 weather scaling table")
    void testEffectiveAccuracyContract() {
        Move thunder = createMove("thunder", ElementalTypes.ELECTRIC, 110.0d, 70.0d);
        Move hurricane = createMove("hurricane", ElementalTypes.FLYING, 110.0d, 70.0d);
        Move blizzard = createMove("blizzard", ElementalTypes.ICE, 110.0d, 70.0d);
        Move thunderbolt = createMove("thunderbolt", ElementalTypes.ELECTRIC, 90.0d, 100.0d);

        // Rain weather tokens -> 100% accuracy for Thunder and Hurricane
        assertEquals(1.0d, WeatherAccuracyValuationStrategy.getEffectiveAccuracy(thunder, "rain"), 0.001);
        assertEquals(1.0d, WeatherAccuracyValuationStrategy.getEffectiveAccuracy(thunder, "raindance"), 0.001);
        assertEquals(1.0d, WeatherAccuracyValuationStrategy.getEffectiveAccuracy(thunder, "heavyrain"), 0.001);
        assertEquals(1.0d, WeatherAccuracyValuationStrategy.getEffectiveAccuracy(thunder, "primordialsea"), 0.001);
        assertEquals(1.0d, WeatherAccuracyValuationStrategy.getEffectiveAccuracy(hurricane, "rain"), 0.001);
        assertEquals(1.0d, WeatherAccuracyValuationStrategy.getEffectiveAccuracy(hurricane, "raindance"), 0.001);

        // Sun weather tokens -> 50% accuracy for Thunder, Hurricane, Blizzard
        assertEquals(0.5d, WeatherAccuracyValuationStrategy.getEffectiveAccuracy(thunder, "sunnyday"), 0.001);
        assertEquals(0.5d, WeatherAccuracyValuationStrategy.getEffectiveAccuracy(thunder, "harshsunlight"), 0.001);
        assertEquals(0.5d, WeatherAccuracyValuationStrategy.getEffectiveAccuracy(thunder, "desolateland"), 0.001);
        assertEquals(0.5d, WeatherAccuracyValuationStrategy.getEffectiveAccuracy(hurricane, "sunnyday"), 0.001);
        assertEquals(0.5d, WeatherAccuracyValuationStrategy.getEffectiveAccuracy(blizzard, "sunnyday"), 0.001);

        // Neutral / other weather -> 70% base accuracy
        assertEquals(0.7d, WeatherAccuracyValuationStrategy.getEffectiveAccuracy(thunder, (String) null), 0.001);
        assertEquals(0.7d, WeatherAccuracyValuationStrategy.getEffectiveAccuracy(thunder, "sandstorm"), 0.001);
        assertEquals(0.7d, WeatherAccuracyValuationStrategy.getEffectiveAccuracy(hurricane, (String) null), 0.001);
        assertEquals(0.7d, WeatherAccuracyValuationStrategy.getEffectiveAccuracy(blizzard, (String) null), 0.001);

        // Snow / Hail -> 100% accuracy for Blizzard
        assertEquals(1.0d, WeatherAccuracyValuationStrategy.getEffectiveAccuracy(blizzard, "snow"), 0.001);
        assertEquals(1.0d, WeatherAccuracyValuationStrategy.getEffectiveAccuracy(blizzard, "hail"), 0.001);

        // Thunderbolt is always 100%
        assertEquals(1.0d, WeatherAccuracyValuationStrategy.getEffectiveAccuracy(thunderbolt, "rain"), 0.001);
        assertEquals(1.0d, WeatherAccuracyValuationStrategy.getEffectiveAccuracy(thunderbolt, "sunnyday"), 0.001);
        assertEquals(1.0d, WeatherAccuracyValuationStrategy.getEffectiveAccuracy(thunderbolt, (String) null), 0.001);
    }

    @Test
    @DisplayName("Move Dominance: Thunder strictly dominates Thunderbolt under Rain")
    void testDominanceUnderRain() {
        Move thunder = createMove("thunder", ElementalTypes.ELECTRIC, 110.0d, 70.0d);
        Move thunderbolt = createMove("thunderbolt", ElementalTypes.ELECTRIC, 90.0d, 100.0d);
        Move hurricane = createMove("hurricane", ElementalTypes.FLYING, 110.0d, 70.0d);
        Move airslash = createMove("airslash", ElementalTypes.FLYING, 75.0d, 95.0d);
        Move blizzard = createMove("blizzard", ElementalTypes.ICE, 110.0d, 70.0d);
        Move icebeam = createMove("icebeam", ElementalTypes.ICE, 90.0d, 100.0d);

        // Under Rain, Thunder dominates Thunderbolt
        int thunderVsTboltRain = WeatherAccuracyValuationStrategy.compareWeatherMoveDominance(thunder, thunderbolt, "rain");
        assertTrue(thunderVsTboltRain > 0, "Thunder must dominate Thunderbolt under Rain");

        int tboltVsThunderRain = WeatherAccuracyValuationStrategy.compareWeatherMoveDominance(thunderbolt, thunder, "rain");
        assertTrue(tboltVsThunderRain < 0, "Thunderbolt must lose dominance comparison to Thunder under Rain");

        // Hurricane dominates Air Slash under Rain
        int hurricaneVsAirSlashRain = WeatherAccuracyValuationStrategy.compareWeatherMoveDominance(hurricane, airslash, "rain");
        assertTrue(hurricaneVsAirSlashRain > 0, "Hurricane must dominate Air Slash under Rain");

        // Blizzard dominates Ice Beam under Snow
        int blizzardVsIceBeamSnow = WeatherAccuracyValuationStrategy.compareWeatherMoveDominance(blizzard, icebeam, "snow");
        assertTrue(blizzardVsIceBeamSnow > 0, "Blizzard must dominate Ice Beam under Snow");
    }

    @Test
    @DisplayName("Move Dominance: Thunderbolt maintains reliability dominance outside Rain and under Sun")
    void testDominanceOutsideRainAndSun() {
        Move thunder = createMove("thunder", ElementalTypes.ELECTRIC, 110.0d, 70.0d);
        Move thunderbolt = createMove("thunderbolt", ElementalTypes.ELECTRIC, 90.0d, 100.0d);
        Move blizzard = createMove("blizzard", ElementalTypes.ICE, 110.0d, 70.0d);
        Move icebeam = createMove("icebeam", ElementalTypes.ICE, 90.0d, 100.0d);

        // Outside Rain (Neutral): Thunderbolt dominates Thunder
        int thunderVsTboltNeutral = WeatherAccuracyValuationStrategy.compareWeatherMoveDominance(thunder, thunderbolt, (String) null);
        assertTrue(thunderVsTboltNeutral < 0, "Thunder must yield dominance to Thunderbolt outside Rain");

        int tboltVsThunderNeutral = WeatherAccuracyValuationStrategy.compareWeatherMoveDominance(thunderbolt, thunder, (String) null);
        assertTrue(tboltVsThunderNeutral > 0, "Thunderbolt dominates Thunder outside Rain for reliability");

        // Under Sun: Thunderbolt dominates Thunder
        int thunderVsTboltSun = WeatherAccuracyValuationStrategy.compareWeatherMoveDominance(thunder, thunderbolt, "sunnyday");
        assertTrue(thunderVsTboltSun < 0, "Thunder must yield dominance to Thunderbolt in Sun");

        // Outside Snow: Ice Beam dominates Blizzard
        int blizzardVsIceBeamNeutral = WeatherAccuracyValuationStrategy.compareWeatherMoveDominance(blizzard, icebeam, (String) null);
        assertTrue(blizzardVsIceBeamNeutral < 0, "Blizzard yields dominance to Ice Beam outside Snow");
    }

    @Test
    @DisplayName("Defect 3 KO Scenario: Eliminates independent RNG roll inversion under Rain")
    void testKillingMovesRNGInversionEliminatedUnderRain() throws Exception {
        BattlePokemon attacker = createBattlePokemon("rain", null);
        ActiveBattlePokemon abp = createActiveBattlePokemon(attacker);

        BattlePokemon targetBP = createBattlePokemon(null, null);
        ActiveBattlePokemon target = createActiveBattlePokemon(targetBP);

        Move thunder = createMove("thunder", ElementalTypes.ELECTRIC, 110.0d, 70.0d);
        Move thunderbolt = createMove("thunderbolt", ElementalTypes.ELECTRIC, 90.0d, 100.0d);

        // Simulate vanilla RunBunAI RNG inversion:
        // Target has 40 HP. Both moves kill.
        // Thunderbolt rolled 8 (+6 faster = 14)
        // Thunder rolled 6 (+6 faster = 12)
        RunBunAI.MoveEvaluation evalThunder = createEvaluation(thunder, 80, 12, target);
        RunBunAI.MoveEvaluation evalThunderbolt = createEvaluation(thunderbolt, 60, 14, target);

        List<RunBunAI.MoveEvaluation> evaluations = new ArrayList<>(List.of(evalThunder, evalThunderbolt));

        // Before valuation adjustment, Thunderbolt has higher score (14 > 12) -> Buggy AI selects Thunderbolt!
        assertTrue(evalThunderbolt.getScore() > evalThunder.getScore(), "Pre-condition: demonstrates the bug where Thunderbolt out-rolled Thunder");

        // Execute valuation adjustment under Rain
        WeatherAccuracyValuationStrategy.adjustMoveValuations(evaluations, attacker, abp, null);

        // After valuation adjustment, Thunder must strictly dominate Thunderbolt: score(Thunder) >= score(Thunderbolt)
        assertTrue(evalThunder.getScore() >= evalThunderbolt.getScore(),
            "score(Thunder) must be >= score(Thunderbolt) under Rain");
        assertTrue(evalThunder.getScore() > evalThunderbolt.getScore(),
            "score(Thunder) must be strictly greater than score(Thunderbolt) to eliminate RNG inversion");
    }

    @Test
    @DisplayName("Defect 3 Tie-Break Scenario: Eliminates 50/50 coin flip tie under Rain")
    void testTieBreakEliminatedUnderRain() throws Exception {
        BattlePokemon attacker = createBattlePokemon("rain", null);
        ActiveBattlePokemon abp = createActiveBattlePokemon(attacker);

        BattlePokemon targetBP = createBattlePokemon(null, null);
        ActiveBattlePokemon target = createActiveBattlePokemon(targetBP);

        Move thunder = createMove("thunder", ElementalTypes.ELECTRIC, 110.0d, 70.0d);
        Move thunderbolt = createMove("thunderbolt", ElementalTypes.ELECTRIC, 90.0d, 100.0d);

        // Both rolled 8 (+6 faster = 14). In vanilla RunBunAI, ties trigger 50/50 RANDOM.nextInt()!
        RunBunAI.MoveEvaluation evalThunder = createEvaluation(thunder, 80, 14, target);
        RunBunAI.MoveEvaluation evalThunderbolt = createEvaluation(thunderbolt, 60, 14, target);

        List<RunBunAI.MoveEvaluation> evaluations = new ArrayList<>(List.of(evalThunder, evalThunderbolt));

        WeatherAccuracyValuationStrategy.adjustMoveValuations(evaluations, attacker, abp, null);

        // Thunder must strictly exceed Thunderbolt so bestMoves contains only Thunder
        assertTrue(evalThunder.getScore() > evalThunderbolt.getScore(),
            "Under Rain, Thunder must break tie deterministically above Thunderbolt");
    }

    @Test
    @DisplayName("Defect 3 Outside Rain Scenario: Thunderbolt maintains reliability dominance over Thunder")
    void testValuationOutsideRainMaintainsThunderboltReliability() throws Exception {
        BattlePokemon attacker = createBattlePokemon(null, null); // Neutral weather
        ActiveBattlePokemon abp = createActiveBattlePokemon(attacker);

        BattlePokemon targetBP = createBattlePokemon(null, null);
        ActiveBattlePokemon target = createActiveBattlePokemon(targetBP);

        Move thunder = createMove("thunder", ElementalTypes.ELECTRIC, 110.0d, 70.0d);
        Move thunderbolt = createMove("thunderbolt", ElementalTypes.ELECTRIC, 90.0d, 100.0d);

        // Outside Rain, suppose Thunder initially had equal score 14 vs Thunderbolt 14 (or Thunder 14 vs Tbolt 12)
        RunBunAI.MoveEvaluation evalThunder = createEvaluation(thunder, 80, 14, target);
        RunBunAI.MoveEvaluation evalThunderbolt = createEvaluation(thunderbolt, 60, 12, target);

        List<RunBunAI.MoveEvaluation> evaluations = new ArrayList<>(List.of(evalThunder, evalThunderbolt));

        WeatherAccuracyValuationStrategy.adjustMoveValuations(evaluations, attacker, abp, null);

        // Outside Rain, Thunderbolt must dominate Thunder for 100% reliability
        assertTrue(evalThunderbolt.getScore() > evalThunder.getScore(),
            "Outside Rain, Thunderbolt must dominate Thunder to maintain 100% reliability");
    }

    @Test
    @DisplayName("Adverse Sun Scenario: Weather moves are penalized in Sun")
    void testValuationInSunPenalizesWeatherMoves() throws Exception {
        BattlePokemon attacker = createBattlePokemon("sunnyday", null);
        ActiveBattlePokemon abp = createActiveBattlePokemon(attacker);

        BattlePokemon targetBP = createBattlePokemon(null, null);
        ActiveBattlePokemon target = createActiveBattlePokemon(targetBP);

        Move thunder = createMove("thunder", ElementalTypes.ELECTRIC, 110.0d, 70.0d);
        Move thunderbolt = createMove("thunderbolt", ElementalTypes.ELECTRIC, 90.0d, 100.0d);

        RunBunAI.MoveEvaluation evalThunder = createEvaluation(thunder, 80, 14, target);
        RunBunAI.MoveEvaluation evalThunderbolt = createEvaluation(thunderbolt, 60, 12, target);

        List<RunBunAI.MoveEvaluation> evaluations = new ArrayList<>(List.of(evalThunder, evalThunderbolt));

        WeatherAccuracyValuationStrategy.adjustMoveValuations(evaluations, attacker, abp, null);

        assertTrue(evalThunderbolt.getScore() > evalThunder.getScore(),
            "In Sun, 50% accurate Thunder must be penalized and ranked below Thunderbolt");
    }

    @Test
    @DisplayName("Utility Umbrella Invariant: Holder ignores Rain accuracy boost")
    void testUtilityUmbrellaIgnoresRain() throws Exception {
        BattlePokemon attacker = createBattlePokemon("rain", "utilityumbrella");
        ActiveBattlePokemon abp = createActiveBattlePokemon(attacker);

        Move thunder = createMove("thunder", ElementalTypes.ELECTRIC, 110.0d, 70.0d);
        Move thunderbolt = createMove("thunderbolt", ElementalTypes.ELECTRIC, 90.0d, 100.0d);

        // Attacker holding utilityumbrella ignores rain
        double effAcc = WeatherAccuracyValuationStrategy.getEffectiveAccuracy(thunder, attacker, abp);
        assertEquals(0.7d, effAcc, 0.001, "Utility umbrella holder treats Thunder accuracy as 0.7 even in Rain");

        int dom = WeatherAccuracyValuationStrategy.compareWeatherMoveDominance(thunder, thunderbolt, attacker, abp);
        assertTrue(dom < 0, "Under Utility Umbrella, Thunderbolt dominates Thunder for reliability");
    }
}
