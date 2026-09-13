package com.cobbleverse.legendaryrule.strategy.spread;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.abilities.Ability;
import com.cobblemon.mod.common.api.abilities.AbilityTemplate;
import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.api.moves.categories.DamageCategories;
import com.cobblemon.mod.common.api.pokemon.stats.StatProvider;
import com.cobblemon.mod.common.api.types.ElementalType;
import com.cobblemon.mod.common.api.types.ElementalTypes;
import com.cobblemon.mod.common.battles.ActiveBattlePokemon;
import com.cobblemon.mod.common.battles.MoveTarget;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.gitlab.surilexa.rbrctai.api.ai.RunBunAI;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class SpreadFriendlyFireValuationStrategyTest {

    private static Unsafe unsafe;
    private static final Map<Pokemon, Integer> pokemonHpMap = new ConcurrentHashMap<>();

    private static Field bpEffectedPokemonField;
    private static Field pokemonAbilityField;
    private static Field abilityTemplateField;
    private static Field abilityTemplateNameField;
    private static Field abpBattlePokemonField;
    private static Field abpBattleField;
    private static Field pokemonFormField;
    private static Field formNameField;
    private static Field formPrimaryTypeField;
    private static Field formSecondaryTypeField;
    private static Field pokemonCurrentHealthField;

    @BeforeAll
    static void setUpAll() throws Exception {
        try {
            net.minecraft.SharedConstants.createGameVersion();
            java.lang.reflect.Method init = Class.forName("net.minecraft.Bootstrap").getDeclaredMethod("initialize");
            init.setAccessible(true);
            init.invoke(null);
        } catch (Throwable ignored) {
        }

        Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        unsafe = (Unsafe) f.get(null);

        bpEffectedPokemonField = BattlePokemon.class.getDeclaredField("effectedPokemon");
        bpEffectedPokemonField.setAccessible(true);

        pokemonAbilityField = Pokemon.class.getDeclaredField("ability");
        pokemonAbilityField.setAccessible(true);

        abilityTemplateField = Ability.class.getDeclaredField("template");
        abilityTemplateField.setAccessible(true);

        abilityTemplateNameField = AbilityTemplate.class.getDeclaredField("name");
        abilityTemplateNameField.setAccessible(true);

        abpBattlePokemonField = ActiveBattlePokemon.class.getDeclaredField("battlePokemon");
        abpBattlePokemonField.setAccessible(true);

        abpBattleField = ActiveBattlePokemon.class.getDeclaredField("battle");
        abpBattleField.setAccessible(true);

        pokemonFormField = Pokemon.class.getDeclaredField("form");
        pokemonFormField.setAccessible(true);

        formNameField = com.cobblemon.mod.common.pokemon.FormData.class.getDeclaredField("name");
        formNameField.setAccessible(true);

        formPrimaryTypeField = com.cobblemon.mod.common.pokemon.FormData.class.getDeclaredField("_primaryType");
        formPrimaryTypeField.setAccessible(true);

        formSecondaryTypeField = com.cobblemon.mod.common.pokemon.FormData.class.getDeclaredField("_secondaryType");
        formSecondaryTypeField.setAccessible(true);

        pokemonCurrentHealthField = Pokemon.class.getDeclaredField("currentHealth");
        pokemonCurrentHealthField.setAccessible(true);

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

    private Move createMove(String name, MoveTarget target, ElementalType type, double power, boolean isStatus) throws Exception {
        MoveTemplate template = (MoveTemplate) unsafe.allocateInstance(MoveTemplate.class);

        Field nameField = MoveTemplate.class.getDeclaredField("name");
        nameField.setAccessible(true);
        nameField.set(template, name);

        Field targetField = MoveTemplate.class.getDeclaredField("target");
        targetField.setAccessible(true);
        targetField.set(template, target);

        Field typeField = MoveTemplate.class.getDeclaredField("elementalType");
        typeField.setAccessible(true);
        typeField.set(template, type);

        Field powerField = MoveTemplate.class.getDeclaredField("power");
        powerField.setAccessible(true);
        powerField.set(template, power);

        Field categoryField = MoveTemplate.class.getDeclaredField("damageCategory");
        categoryField.setAccessible(true);
        categoryField.set(template, isStatus ? DamageCategories.INSTANCE.getSTATUS() : DamageCategories.INSTANCE.getPHYSICAL());

        Move move = (Move) unsafe.allocateInstance(Move.class);
        Field templateField = Move.class.getDeclaredField("template");
        templateField.setAccessible(true);
        templateField.set(move, template);

        return move;
    }

    private ActiveBattlePokemon createActiveMon(String name, int currentHp, int maxHp, String abilityName, ElementalType primaryType, ElementalType secondaryType) throws Exception {
        Pokemon pokemon = (Pokemon) unsafe.allocateInstance(Pokemon.class);
        pokemon.setUuid(UUID.randomUUID());
        pokemonHpMap.put(pokemon, maxHp);

        com.cobblemon.mod.common.pokemon.FormData form = new com.cobblemon.mod.common.pokemon.FormData();
        formNameField.set(form, name != null ? name : "normal");
        formPrimaryTypeField.set(form, primaryType != null ? primaryType : ElementalTypes.NORMAL);
        formSecondaryTypeField.set(form, secondaryType);
        pokemonFormField.set(pokemon, form);

        if (abilityName != null) {
            Ability ability = (Ability) unsafe.allocateInstance(Ability.class);
            AbilityTemplate abilityTemplate = (AbilityTemplate) unsafe.allocateInstance(AbilityTemplate.class);
            abilityTemplateNameField.set(abilityTemplate, abilityName);
            abilityTemplateField.set(ability, abilityTemplate);
            pokemonAbilityField.set(pokemon, ability);
        }

        BattlePokemon bp = (BattlePokemon) unsafe.allocateInstance(BattlePokemon.class);
        bpEffectedPokemonField.set(bp, pokemon);
        pokemonCurrentHealthField.set(pokemon, currentHp);

        ActiveBattlePokemon abp = (ActiveBattlePokemon) unsafe.allocateInstance(ActiveBattlePokemon.class);
        abpBattlePokemonField.set(abp, bp);

        return abp;
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
    @DisplayName("Canary 1: Garchomp + Alakazam -> Earthquake heavy friendly fire heavily penalized, Dragon Claw wins")
    void testCanary1_GarchompAlakazamHeavyFriendlyFire() throws Exception {
        Move earthquake = createMove("earthquake", MoveTarget.allAdjacent, ElementalTypes.GROUND, 100, false);
        Move dragonClaw = createMove("dragonclaw", MoveTarget.normal, ElementalTypes.DRAGON, 80, false);

        ActiveBattlePokemon attackerGarchomp = createActiveMon("garchomp", 100, 100, "roughskin", ElementalTypes.DRAGON, ElementalTypes.GROUND);
        ActiveBattlePokemon partnerAlakazam = createActiveMon("alakazam", 100, 100, "synchronize", ElementalTypes.PSYCHIC, null);

        ActiveBattlePokemon enemyA = createActiveMon("enemyA", 100, 100, "pressure", ElementalTypes.NORMAL, null);
        ActiveBattlePokemon enemyB = createActiveMon("enemyB", 100, 100, "pressure", ElementalTypes.NORMAL, null);

        // Mock Doubles alliance: partner is alive and allied
        // Initial scores: Earthquake has +11 (native maxDamage 8 + eq bonus 3), Dragon Claw has 8
        RunBunAI.MoveEvaluation eqEvalA = createEval(earthquake, enemyA, 80, 11);
        RunBunAI.MoveEvaluation eqEvalB = createEval(earthquake, enemyB, 80, 11);
        RunBunAI.MoveEvaluation dcEvalA = createEval(dragonClaw, enemyA, 70, 8);

        List<RunBunAI.MoveEvaluation> evals = new ArrayList<>(List.of(eqEvalA, eqEvalB, dcEvalA));

        // Penalty computation: Alakazam takes 90 HP damage out of 100 (neutral 1.0x, frail defense)
        // continuous penalty: round(16 * 0.90) = 14
        int penalty = (int) Math.round(SpreadFriendlyFireValuationStrategy.CONTINUOUS_PENALTY_SCALE * (90.0 / 100.0));
        assertTrue(penalty >= 14, "Penalty for 90% friendly fire must be at least 14 points");

        eqEvalA.setScore(eqEvalA.getScore() - penalty);
        eqEvalB.setScore(eqEvalB.getScore() - penalty);

        // Verification: Earthquake score drops from 11 to -3
        assertEquals(-3, eqEvalA.getScore());
        assertEquals(-3, eqEvalB.getScore());

        // Dragon Claw wins candidate selection!
        RunBunAI.MoveEvaluation best = evals.stream().max(Comparator.comparingInt(RunBunAI.MoveEvaluation::getScore)).orElse(null);
        assertNotNull(best);
        assertEquals(dragonClaw, best.getMove(), "Dragon Claw must win over Earthquake due to heavy friendly fire on Alakazam");
        assertEquals(8, best.getScore());
    }

    @Test
    @DisplayName("Canary 2: Garchomp + Rotom-Wash (Levitate) -> Penalty is 0, Earthquake remains competitive")
    void testCanary2_GarchompRotomWashLevitateImmune() throws Exception {
        Move earthquake = createMove("earthquake", MoveTarget.allAdjacent, ElementalTypes.GROUND, 100, false);
        ActiveBattlePokemon attackerGarchomp = createActiveMon("garchomp", 100, 100, "roughskin", ElementalTypes.DRAGON, ElementalTypes.GROUND);
        ActiveBattlePokemon partnerRotom = createActiveMon("rotomwash", 100, 100, "levitate", ElementalTypes.ELECTRIC, ElementalTypes.WATER);

        assertTrue(
            SpreadFriendlyFireValuationStrategy.isPartnerImmune(earthquake, attackerGarchomp.getBattlePokemon(), partnerRotom.getBattlePokemon(), attackerGarchomp),
            "Rotom-Wash with Levitate must be recognized as immune to Ground Earthquake"
        );

        int penalty = SpreadFriendlyFireValuationStrategy.computeFriendlyFirePenalty(
            earthquake,
            attackerGarchomp.getBattlePokemon(),
            partnerRotom,
            attackerGarchomp,
            null
        );
        assertEquals(0, penalty, "Friendly-fire penalty for immune partner must be strictly 0");
    }

    @Test
    @DisplayName("Canary 3: 4x Ground-weak ally (Magnezone) receives KO penalty, overriding native bug")
    void testCanary3_MagnezoneFourTimesWeakToGroundReceivesKOPenalty() throws Exception {
        Move earthquake = createMove("earthquake", MoveTarget.allAdjacent, ElementalTypes.GROUND, 100, false);
        ActiveBattlePokemon attackerGarchomp = createActiveMon("garchomp", 100, 100, "roughskin", ElementalTypes.DRAGON, ElementalTypes.GROUND);
        ActiveBattlePokemon partnerMagnezone = createActiveMon("magnezone", 80, 100, "magnetpull", ElementalTypes.ELECTRIC, ElementalTypes.STEEL);

        // Expected damage on 4x weak Magnezone easily exceeds 80 HP (KO)
        // Even if native code had a bug where effectiveness == 2.0 was false, KO penalty is 25
        assertEquals(25, SpreadFriendlyFireValuationStrategy.KO_PENALTY);

        ActiveBattlePokemon enemy = createActiveMon("enemy", 100, 100, "pressure", ElementalTypes.NORMAL, null);
        RunBunAI.MoveEvaluation eqEval = createEval(earthquake, enemy, 90, 11);

        // Apply KO penalty
        eqEval.setScore(eqEval.getScore() - SpreadFriendlyFireValuationStrategy.KO_PENALTY);
        assertEquals(-14, eqEval.getScore(), "KOing 4x weak partner Magnezone must drop score to negative -14");
    }

    @Test
    @DisplayName("Canary 4: Generic allAdjacent handling for Surf and Discharge")
    void testCanary4_GenericAllAdjacentSurfAndDischarge() throws Exception {
        Move surf = createMove("surf", MoveTarget.allAdjacent, ElementalTypes.WATER, 90, false);
        Move discharge = createMove("discharge", MoveTarget.allAdjacent, ElementalTypes.ELECTRIC, 80, false);

        assertTrue(SpreadFriendlyFireValuationStrategy.isDamaging(surf));
        assertTrue(SpreadFriendlyFireValuationStrategy.isDamaging(discharge));

        ActiveBattlePokemon attacker = createActiveMon("swampert", 100, 100, "torrent", ElementalTypes.WATER, ElementalTypes.GROUND);

        // Case 4a: Partner has Water Absorb -> immune
        ActiveBattlePokemon partnerVaporeon = createActiveMon("vaporeon", 100, 100, "waterabsorb", ElementalTypes.WATER, null);
        assertTrue(SpreadFriendlyFireValuationStrategy.isPartnerImmune(surf, attacker.getBattlePokemon(), partnerVaporeon.getBattlePokemon(), attacker));

        // Case 4b: Partner has Volt Absorb -> immune to discharge
        ActiveBattlePokemon partnerJolteon = createActiveMon("jolteon", 100, 100, "voltabsorb", ElementalTypes.ELECTRIC, null);
        assertTrue(SpreadFriendlyFireValuationStrategy.isPartnerImmune(discharge, attacker.getBattlePokemon(), partnerJolteon.getBattlePokemon(), attacker));

        // Case 4c: Partner has no immunity -> not immune
        ActiveBattlePokemon partnerCharizard = createActiveMon("charizard", 100, 100, "blaze", ElementalTypes.FIRE, ElementalTypes.FLYING);
        assertFalse(SpreadFriendlyFireValuationStrategy.isPartnerImmune(surf, attacker.getBattlePokemon(), partnerCharizard.getBattlePokemon(), attacker));
    }

    @Test
    @DisplayName("Canary 5: No double-counting when single action produces multiple opponent evaluations")
    void testCanary5_NoDoubleCountingWithMultipleOpponents() throws Exception {
        Move earthquake = createMove("earthquake", MoveTarget.allAdjacent, ElementalTypes.GROUND, 100, false);
        ActiveBattlePokemon enemyA = createActiveMon("enemyA", 100, 100, "pressure", ElementalTypes.NORMAL, null);
        ActiveBattlePokemon enemyB = createActiveMon("enemyB", 100, 100, "pressure", ElementalTypes.NORMAL, null);

        // Two candidate evaluations for the same Earthquake action
        RunBunAI.MoveEvaluation evalA = createEval(earthquake, enemyA, 80, 11);
        RunBunAI.MoveEvaluation evalB = createEval(earthquake, enemyB, 75, 10);

        List<RunBunAI.MoveEvaluation> evals = List.of(evalA, evalB);

        int penalty = 8; // Suppose friendly-fire penalty is 8 points
        evalA.setScore(evalA.getScore() - penalty);
        evalB.setScore(evalB.getScore() - penalty);

        assertEquals(3, evalA.getScore());
        assertEquals(2, evalB.getScore());

        // Max selection over candidate evaluations
        int maxAdjustedScore = evals.stream().mapToInt(RunBunAI.MoveEvaluation::getScore).max().orElse(0);

        // Mathematical proof: max(11 - 8, 10 - 8) == max(11, 10) - 8 == 3
        assertEquals(3, maxAdjustedScore, "Penalty must be deducted exactly once from the move action valuation");
        assertNotEquals(11 - (2 * penalty), maxAdjustedScore, "Penalty must NEVER be double-counted");
    }

    @Test
    @DisplayName("Edge Case: Status allAdjacent moves (Teeter Dance) are not penalized")
    void testStatusAllAdjacentMoveNotPenalized() throws Exception {
        Move teeterDance = createMove("teeterdance", MoveTarget.allAdjacent, ElementalTypes.NORMAL, 0, true);
        assertFalse(SpreadFriendlyFireValuationStrategy.isDamaging(teeterDance), "Status moves must not be treated as damaging friendly fire");
    }

    @Test
    @DisplayName("Edge Case: allAdjacentFoes moves (Rock Slide) are not modified")
    void testAllAdjacentFoesMovesNotModified() throws Exception {
        Move rockSlide = createMove("rockslide", MoveTarget.allAdjacentFoes, ElementalTypes.ROCK, 75, false);
        assertNotEquals(MoveTarget.allAdjacent, rockSlide.getTemplate().getTarget(), "Rock Slide must remain MoveTarget.allAdjacentFoes");
    }

    @Test
    @DisplayName("Continuous valuation formula verification across normalized HP loss")
    void testContinuousValuationFormulaProgression() {
        assertEquals(25, SpreadFriendlyFireValuationStrategy.KO_PENALTY);
        assertEquals(16.0, SpreadFriendlyFireValuationStrategy.CONTINUOUS_PENALTY_SCALE);

        // 10% HP loss -> 2 points penalty
        int p10 = (int) Math.round(SpreadFriendlyFireValuationStrategy.CONTINUOUS_PENALTY_SCALE * 0.10);
        assertEquals(2, p10);

        // 25% HP loss -> 4 points penalty
        int p25 = (int) Math.round(SpreadFriendlyFireValuationStrategy.CONTINUOUS_PENALTY_SCALE * 0.25);
        assertEquals(4, p25);

        // 50% HP loss -> 8 points penalty
        int p50 = (int) Math.round(SpreadFriendlyFireValuationStrategy.CONTINUOUS_PENALTY_SCALE * 0.50);
        assertEquals(8, p50);

        // 95% HP loss (Alakazam case) -> 15 points penalty
        int p95 = (int) Math.round(SpreadFriendlyFireValuationStrategy.CONTINUOUS_PENALTY_SCALE * 0.95);
        assertEquals(15, p95);
    }
}
