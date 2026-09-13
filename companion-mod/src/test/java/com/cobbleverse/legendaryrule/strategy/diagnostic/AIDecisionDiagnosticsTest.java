package com.cobbleverse.legendaryrule.strategy.diagnostic;

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
import com.cobbleverse.legendaryrule.strategy.spread.SpreadFriendlyFireValuationStrategy;
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

class AIDecisionDiagnosticsTest {

    private static Unsafe unsafe;
    private static final Map<Pokemon, Integer> pokemonHpMap = new ConcurrentHashMap<>();

    private static Field bpEffectedPokemonField;
    private static Field pokemonAbilityField;
    private static Field abilityTemplateField;
    private static Field abilityTemplateNameField;
    private static Field abpBattlePokemonField;
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
    @DisplayName("Tie detection finds all candidates sharing the max score")
    void testFindTieCandidatesWhenMultipleMaxScoresExist() throws Exception {
        Move protect = createMove("protect", MoveTarget.allAdjacent, ElementalTypes.NORMAL, 0, true);
        Move sacredSword = createMove("sacredsword", MoveTarget.normal, ElementalTypes.FIGHTING, 90, false);
        Move psychoCut = createMove("psychocut", MoveTarget.normal, ElementalTypes.PSYCHIC, 70, false);

        ActiveBattlePokemon enemyA = createActiveMon("Miraidon", 100, 100, "hadronengine", ElementalTypes.ELECTRIC, ElementalTypes.DRAGON);
        ActiveBattlePokemon enemyB = createActiveMon("Incineroar", 100, 100, "intimidate", ElementalTypes.FIRE, ElementalTypes.DARK);

        RunBunAI.MoveEvaluation eval1 = createEval(protect, enemyA, 0, 6);
        RunBunAI.MoveEvaluation eval2 = createEval(sacredSword, enemyB, 85, 6);
        RunBunAI.MoveEvaluation eval3 = createEval(sacredSword, enemyA, 60, 6);
        RunBunAI.MoveEvaluation eval4 = createEval(psychoCut, enemyA, 40, 4);

        List<RunBunAI.MoveEvaluation> evals = List.of(eval1, eval2, eval3, eval4);

        List<RunBunAI.MoveEvaluation> tied = AIDecisionDiagnostics.findTieCandidates(evals, 6);
        assertEquals(3, tied.size(), "All 3 candidates with maxScore=6 must be detected");
        assertTrue(tied.contains(eval1));
        assertTrue(tied.contains(eval2));
        assertTrue(tied.contains(eval3));
        assertFalse(tied.contains(eval4));
    }

    @Test
    @DisplayName("Tie detection returns single candidate when there is a clear winner")
    void testFindTieCandidatesWhenSingleWinner() throws Exception {
        Move dragonClaw = createMove("dragonclaw", MoveTarget.normal, ElementalTypes.DRAGON, 80, false);
        Move earthquake = createMove("earthquake", MoveTarget.allAdjacent, ElementalTypes.GROUND, 100, false);

        ActiveBattlePokemon enemy = createActiveMon("Miraidon", 100, 100, "pressure", ElementalTypes.DRAGON, null);

        RunBunAI.MoveEvaluation evalDragonClaw = createEval(dragonClaw, enemy, 212, 11);
        RunBunAI.MoveEvaluation evalEarthquake = createEval(earthquake, enemy, 197, -3);

        List<RunBunAI.MoveEvaluation> evals = List.of(evalDragonClaw, evalEarthquake);

        List<RunBunAI.MoveEvaluation> tied = AIDecisionDiagnostics.findTieCandidates(evals, 11);
        assertEquals(1, tied.size(), "Only the single winner must be returned when there is no tie");
        assertEquals(evalDragonClaw, tied.getFirst());
    }

    @Test
    @DisplayName("Invariance: Logging methods never mutate MoveEvaluation state or list ordering")
    void testLoggingDoesNotMutateEvaluations() throws Exception {
        Move protect = createMove("protect", MoveTarget.allAdjacent, ElementalTypes.NORMAL, 0, true);
        Move sacredSword = createMove("sacredsword", MoveTarget.normal, ElementalTypes.FIGHTING, 90, false);

        ActiveBattlePokemon attacker = createActiveMon("Gallade", 100, 100, "sharpness", ElementalTypes.PSYCHIC, ElementalTypes.FIGHTING);
        ActiveBattlePokemon enemy = createActiveMon("Miraidon", 100, 100, "hadronengine", ElementalTypes.ELECTRIC, ElementalTypes.DRAGON);

        RunBunAI.MoveEvaluation eval1 = createEval(protect, enemy, 0, 6);
        RunBunAI.MoveEvaluation eval2 = createEval(sacredSword, enemy, 85, 6);

        List<RunBunAI.MoveEvaluation> evals = new ArrayList<>(List.of(eval1, eval2));

        // Snapshot original states
        int origScore1 = eval1.getScore();
        int origDamage1 = eval1.getDamage();
        int origScore2 = eval2.getScore();
        int origDamage2 = eval2.getDamage();

        // Run diagnostic logs
        AIDecisionDiagnostics.logFinalCandidates(evals, attacker.getBattlePokemon());
        AIDecisionDiagnostics.logChosenAndTie(evals, eval1, attacker.getBattlePokemon());

        // Invariant assertions: scores, damages, and ordering must be completely unaltered
        assertEquals(origScore1, eval1.getScore(), "eval1 score must not be mutated");
        assertEquals(origDamage1, eval1.getDamage(), "eval1 damage must not be mutated");
        assertEquals(origScore2, eval2.getScore(), "eval2 score must not be mutated");
        assertEquals(origDamage2, eval2.getDamage(), "eval2 damage must not be mutated");

        assertEquals(eval1, evals.get(0), "List ordering must be preserved");
        assertEquals(eval2, evals.get(1), "List ordering must be preserved");
    }

    @Test
    @DisplayName("Utility moves format correctly with zero damage in diagnostics")
    void testUtilityMovesVisibility() throws Exception {
        Move followMe = createMove("followme", MoveTarget.self, ElementalTypes.NORMAL, 0, true);
        ActiveBattlePokemon target = createActiveMon("Miraidon", 100, 100, "pressure", ElementalTypes.DRAGON, null);

        RunBunAI.MoveEvaluation eval = createEval(followMe, target, 0, 6);

        assertEquals("followme", AIDecisionDiagnostics.getMoveName(eval.getMove()));
        assertEquals("Miraidon", AIDecisionDiagnostics.getTargetName(eval));
        assertEquals(6, eval.getScore());
        assertEquals(0, eval.getDamage());
    }

    @Test
    @DisplayName("evaluateFriendlyFire returns accurate intermediate values without mutating state")
    void testFriendlyFireResultPreservesValues() throws Exception {
        Move earthquake = createMove("earthquake", MoveTarget.allAdjacent, ElementalTypes.GROUND, 100, false);
        ActiveBattlePokemon attacker = createActiveMon("garchomp", 100, 100, "roughskin", ElementalTypes.DRAGON, ElementalTypes.GROUND);
        ActiveBattlePokemon partnerRotom = createActiveMon("rotom", 100, 100, "levitate", ElementalTypes.ELECTRIC, ElementalTypes.WATER);

        SpreadFriendlyFireValuationStrategy.FriendlyFireResult result =
            SpreadFriendlyFireValuationStrategy.evaluateFriendlyFire(
                earthquake,
                attacker.getBattlePokemon(),
                partnerRotom,
                attacker,
                null
            );

        assertTrue(result.immune, "Levitate partner must be identified as immune");
        assertEquals(0, result.allyDamage, "Immune partner allyDamage must be 0");
        assertEquals(0, result.penalty, "Immune partner penalty must be 0");
        assertEquals(100, result.currentHp);
        assertEquals(100, result.maxHp);
    }
}
