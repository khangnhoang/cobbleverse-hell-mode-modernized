package com.cobbleverse.legendaryrule.strategy.terrain;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.battles.interpreter.BasicContext;
import com.cobblemon.mod.common.api.battles.interpreter.BattleContext;
import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.battles.actor.PokemonBattleActor;
import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.moves.MoveSet;
import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.api.moves.categories.DamageCategories;
import com.cobblemon.mod.common.api.pokemon.stats.StatProvider;
import com.cobblemon.mod.common.api.types.ElementalType;
import com.cobblemon.mod.common.api.types.ElementalTypes;
import com.cobblemon.mod.common.battles.ActiveBattlePokemon;
import com.cobblemon.mod.common.battles.MoveTarget;
import com.cobblemon.mod.common.battles.interpreter.ContextManager;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.pokemon.FormData;
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

class GrassyGlideValuationStrategyTest {

    private static Unsafe unsafe;
    private static final Map<Pokemon, Integer> pokemonSpeedMap = new ConcurrentHashMap<>();

    private static Field bpEffectedPokemonField;
    private static Field bpContextManagerField;
    private static Field bpActorField;
    private static Field actorBattleField;
    private static Field battleContextManagerField;
    private static Field pokemonMoveSetField;
    private static Field moveSetMovesField;
    private static Field pokemonCurrentHealthField;
    private static Field pokemonFormField;
    private static Field formNameField;
    private static Field formPrimaryTypeField;

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

        bpContextManagerField = BattlePokemon.class.getDeclaredField("contextManager");
        bpContextManagerField.setAccessible(true);

        bpActorField = BattlePokemon.class.getDeclaredField("actor");
        bpActorField.setAccessible(true);

        actorBattleField = BattleActor.class.getDeclaredField("battle");
        actorBattleField.setAccessible(true);

        battleContextManagerField = PokemonBattle.class.getDeclaredField("contextManager");
        battleContextManagerField.setAccessible(true);

        pokemonMoveSetField = Pokemon.class.getDeclaredField("moveSet");
        pokemonMoveSetField.setAccessible(true);

        moveSetMovesField = MoveSet.class.getDeclaredField("moves");
        moveSetMovesField.setAccessible(true);

        pokemonCurrentHealthField = Pokemon.class.getDeclaredField("currentHealth");
        pokemonCurrentHealthField.setAccessible(true);

        pokemonFormField = Pokemon.class.getDeclaredField("form");
        pokemonFormField.setAccessible(true);

        formNameField = FormData.class.getDeclaredField("name");
        formNameField.setAccessible(true);

        formPrimaryTypeField = FormData.class.getDeclaredField("_primaryType");
        formPrimaryTypeField.setAccessible(true);

        StatProvider proxy = (StatProvider) Proxy.newProxyInstance(
            StatProvider.class.getClassLoader(),
            new Class<?>[]{StatProvider.class},
            (proxyInstance, method, args) -> {
                if ("getStatForPokemon".equals(method.getName())) {
                    Pokemon pkmn = (Pokemon) args[0];
                    return pokemonSpeedMap.getOrDefault(pkmn, 100);
                }
                return null;
            }
        );
        Cobblemon.INSTANCE.setStatProvider(proxy);
    }

    private Move createMove(String name, double power) throws Exception {
        MoveTemplate template = (MoveTemplate) unsafe.allocateInstance(MoveTemplate.class);

        Field nameField = MoveTemplate.class.getDeclaredField("name");
        nameField.setAccessible(true);
        nameField.set(template, name);

        Field targetField = MoveTemplate.class.getDeclaredField("target");
        targetField.setAccessible(true);
        targetField.set(template, MoveTarget.normal);

        Field typeField = MoveTemplate.class.getDeclaredField("elementalType");
        typeField.setAccessible(true);
        typeField.set(template, ElementalTypes.GRASS);

        Field powerField = MoveTemplate.class.getDeclaredField("power");
        powerField.setAccessible(true);
        powerField.set(template, power);

        Field categoryField = MoveTemplate.class.getDeclaredField("damageCategory");
        categoryField.setAccessible(true);
        categoryField.set(template, DamageCategories.INSTANCE.getPHYSICAL());

        Move move = (Move) unsafe.allocateInstance(Move.class);
        Field templateField = Move.class.getDeclaredField("template");
        templateField.setAccessible(true);
        templateField.set(move, template);

        return move;
    }

    private BattlePokemon createBattlePokemon(int currentHp, int speed, boolean grassyTerrainActive) throws Exception {
        return createBattlePokemon(currentHp, speed, grassyTerrainActive, false);
    }

    private BattlePokemon createBattlePokemon(int currentHp, int speed, boolean grassyTerrainActive, boolean isRaised) throws Exception {
        BattlePokemon bp = (BattlePokemon) unsafe.allocateInstance(BattlePokemon.class);

        Pokemon pokemon = (Pokemon) unsafe.allocateInstance(Pokemon.class);
        pokemon.setUuid(UUID.randomUUID());
        pokemonCurrentHealthField.set(pokemon, currentHp);
        pokemonSpeedMap.put(pokemon, speed);

        FormData form = new FormData();
        formNameField.set(form, isRaised ? "flying_form" : "grass_form");
        formPrimaryTypeField.set(form, isRaised ? ElementalTypes.FLYING : ElementalTypes.GRASS);
        pokemonFormField.set(pokemon, form);

        bpEffectedPokemonField.set(bp, pokemon);

        ContextManager cm = new ContextManager();
        if (grassyTerrainActive) {
            cm.add(new BasicContext("grassyterrain", 1, BattleContext.Type.TERRAIN, null));
        }
        bpContextManagerField.set(bp, cm);

        PokemonBattle battle = (PokemonBattle) unsafe.allocateInstance(PokemonBattle.class);
        battleContextManagerField.set(battle, cm);

        BattleActor actor = (BattleActor) unsafe.allocateInstance(PokemonBattleActor.class);
        actorBattleField.set(actor, battle);
        bpActorField.set(bp, actor);

        MoveSet moveSet = (MoveSet) unsafe.allocateInstance(MoveSet.class);
        moveSetMovesField.set(moveSet, new Move[0]);
        pokemonMoveSetField.set(pokemon, moveSet);

        return bp;
    }

    private ActiveBattlePokemon createActiveBattlePokemon(BattlePokemon bp) throws Exception {
        ActiveBattlePokemon abp = (ActiveBattlePokemon) unsafe.allocateInstance(ActiveBattlePokemon.class);
        abp.setBattlePokemon(bp);
        return abp;
    }

    private RunBunAI.MoveEvaluation createEvaluation(Move move, int damage, int score, ActiveBattlePokemon opponent) throws Exception {
        RunBunAI.MoveEvaluation eval = (RunBunAI.MoveEvaluation) unsafe.allocateInstance(RunBunAI.MoveEvaluation.class);
        eval.setMove(move);
        eval.setDamage(damage);
        eval.setScore(score);
        eval.setOpponent(opponent);
        return eval;
    }

    @Test
    @DisplayName("Case A1: Grassy Terrain + both moves OHKO, target faster -> Grassy Glide must beat Wood Hammer")
    void testCaseABothOHKOTargetFasterGlideMustWin() throws Exception {
        BattlePokemon rillaboom = createBattlePokemon(200, 100, true);
        BattlePokemon fastTarget = createBattlePokemon(80, 200, true); // Target HP 80, Speed 200 > 100

        ActiveBattlePokemon rillaActive = createActiveBattlePokemon(rillaboom);
        ActiveBattlePokemon targetActive = createActiveBattlePokemon(fastTarget);

        Move glide = createMove("grassyglide", 55.0d);
        Move hammer = createMove("woodhammer", 120.0d);

        // Both moves deal >= 80 damage (OHKO)
        RunBunAI.MoveEvaluation evalGlide = createEvaluation(glide, 90, 9, targetActive); // Slower KO native score 9
        RunBunAI.MoveEvaluation evalHammer = createEvaluation(hammer, 180, 9, targetActive); // Slower KO native score 9

        List<RunBunAI.MoveEvaluation> evals = new ArrayList<>(List.of(evalHammer, evalGlide));

        GrassyGlideValuationStrategy.adjustMoveValuations(evals, rillaboom, rillaActive, null, null);

        assertTrue(evalGlide.getScore() > evalHammer.getScore(),
            "Under Grassy Terrain, Grassy Glide priority OHKO must strictly beat Wood Hammer normal OHKO");
        assertEquals(11, evalGlide.getScore(), "Glide score should be maxOtherKillingScore (9) + 2 = 11");
    }

    @Test
    @DisplayName("Case A2: Grassy Terrain + both moves OHKO, Rillaboom faster -> Grassy Glide must beat Wood Hammer")
    void testCaseABothOHKOAttackerFasterGlideMustWin() throws Exception {
        BattlePokemon rillaboom = createBattlePokemon(200, 200, true);
        BattlePokemon slowTarget = createBattlePokemon(80, 100, true); // Target HP 80, Speed 100 < 200

        ActiveBattlePokemon rillaActive = createActiveBattlePokemon(rillaboom);
        ActiveBattlePokemon targetActive = createActiveBattlePokemon(slowTarget);

        Move glide = createMove("grassyglide", 55.0d);
        Move hammer = createMove("woodhammer", 120.0d);

        // Both moves deal >= 80 damage (OHKO), faster KO native score 12
        RunBunAI.MoveEvaluation evalGlide = createEvaluation(glide, 90, 12, targetActive);
        RunBunAI.MoveEvaluation evalHammer = createEvaluation(hammer, 180, 12, targetActive);

        List<RunBunAI.MoveEvaluation> evals = new ArrayList<>(List.of(evalHammer, evalGlide));

        GrassyGlideValuationStrategy.adjustMoveValuations(evals, rillaboom, rillaActive, null, null);

        assertTrue(evalGlide.getScore() > evalHammer.getScore(),
            "Under Grassy Terrain, Grassy Glide priority OHKO must beat Wood Hammer normal OHKO even when faster");
        assertEquals(14, evalGlide.getScore(), "Glide score should be maxOtherKillingScore (12) + 2 = 14");
    }

    @Test
    @DisplayName("Case B: Grassy Terrain + Glide OHKO, Hammer non-KO -> Glide wins naturally (score >= 12)")
    void testCaseBGlideOHKOHammerNonKOGlideWins() throws Exception {
        BattlePokemon rillaboom = createBattlePokemon(200, 100, true);
        BattlePokemon target = createBattlePokemon(60, 200, true);

        ActiveBattlePokemon rillaActive = createActiveBattlePokemon(rillaboom);
        ActiveBattlePokemon targetActive = createActiveBattlePokemon(target);

        Move glide = createMove("grassyglide", 55.0d);
        Move hammer = createMove("woodhammer", 120.0d);

        RunBunAI.MoveEvaluation evalGlide = createEvaluation(glide, 70, 9, targetActive); // Glide OHKOs (70 >= 60)
        RunBunAI.MoveEvaluation evalHammer = createEvaluation(hammer, 50, 6, targetActive); // Hammer does not OHKO (50 < 60)

        List<RunBunAI.MoveEvaluation> evals = new ArrayList<>(List.of(evalHammer, evalGlide));

        GrassyGlideValuationStrategy.adjustMoveValuations(evals, rillaboom, rillaActive, null, null);

        assertTrue(evalGlide.getScore() > evalHammer.getScore());
        assertTrue(evalGlide.getScore() >= 12, "Glide OHKO under Grassy Terrain should have priority score >= 12");
    }

    @Test
    @DisplayName("Case C1: Hammer OHKO, Glide non-KO, Rillaboom faster -> Wood Hammer must win")
    void testCaseCHammerOHKOGlideNonKOAttackerFasterHammerWins() throws Exception {
        BattlePokemon rillaboom = createBattlePokemon(200, 200, true); // Speed 200
        BattlePokemon target = createBattlePokemon(100, 100, true); // HP 100, Speed 100

        ActiveBattlePokemon rillaActive = createActiveBattlePokemon(rillaboom);
        ActiveBattlePokemon targetActive = createActiveBattlePokemon(target);

        Move glide = createMove("grassyglide", 55.0d);
        Move hammer = createMove("woodhammer", 120.0d);

        RunBunAI.MoveEvaluation evalGlide = createEvaluation(glide, 50, 5, targetActive); // Non-KO, score 5
        RunBunAI.MoveEvaluation evalHammer = createEvaluation(hammer, 150, 12, targetActive); // Faster KO, score 12

        List<RunBunAI.MoveEvaluation> evals = new ArrayList<>(List.of(evalGlide, evalHammer));

        GrassyGlideValuationStrategy.adjustMoveValuations(evals, rillaboom, rillaActive, null, null);

        assertTrue(evalHammer.getScore() > evalGlide.getScore(),
            "When Rillaboom is faster, normal OHKO (Wood Hammer) must strictly beat non-killing Glide");
        assertEquals(12, evalHammer.getScore());
        assertEquals(5, evalGlide.getScore(), "Glide should not be boosted when normal KO can safely execute");
    }

    @Test
    @DisplayName("Case C2: Hammer OHKO, Glide non-KO, target faster but cannot lethal-KO -> Wood Hammer must win")
    void testCaseCHammerOHKOGlideNonKOTargetFasterTargetCannotOHKOHammerWins() throws Exception {
        BattlePokemon rillaboom = createBattlePokemon(200, 100, true); // Speed 100, HP 200
        BattlePokemon target = createBattlePokemon(100, 200, true); // Speed 200, HP 100 (target moves set has no lethal moves)

        ActiveBattlePokemon rillaActive = createActiveBattlePokemon(rillaboom);
        ActiveBattlePokemon targetActive = createActiveBattlePokemon(target);

        Move glide = createMove("grassyglide", 55.0d);
        Move hammer = createMove("woodhammer", 120.0d);

        RunBunAI.MoveEvaluation evalGlide = createEvaluation(glide, 50, 5, targetActive); // Non-KO, score 5
        RunBunAI.MoveEvaluation evalHammer = createEvaluation(hammer, 150, 9, targetActive); // Slower KO, score 9

        List<RunBunAI.MoveEvaluation> evals = new ArrayList<>(List.of(evalGlide, evalHammer));

        GrassyGlideValuationStrategy.adjustMoveValuations(evals, rillaboom, rillaActive, null, null);

        assertTrue(evalHammer.getScore() > evalGlide.getScore(),
            "When target cannot OHKO, Rillaboom survives and normal OHKO executes; Hammer must win");
        assertEquals(9, evalHammer.getScore());
        assertEquals(5, evalGlide.getScore());
    }

    @Test
    @DisplayName("Case C3: Hammer OHKO, Glide non-KO, target faster AND can lethal-KO -> Glide wins realized priority action")
    void testCaseCHammerOHKOGlideNonKOTargetFasterTargetCanOHKOGlideWinsRealized() throws Exception {
        BattlePokemon rillaboom = createBattlePokemon(200, 100, true);
        BattlePokemon lethalTarget = createBattlePokemon(150, 250, true);

        ActiveBattlePokemon rillaActive = createActiveBattlePokemon(rillaboom);
        ActiveBattlePokemon targetActive = createActiveBattlePokemon(lethalTarget);

        Move glide = createMove("grassyglide", 55.0d);
        Move hammer = createMove("woodhammer", 120.0d);

        RunBunAI.MoveEvaluation evalGlide = createEvaluation(glide, 60, 5, targetActive); // Glide chips 60 < 150
        RunBunAI.MoveEvaluation evalHammer = createEvaluation(hammer, 160, 9, targetActive); // Hammer 160 >= 150 (OHKO, score 9)

        List<RunBunAI.MoveEvaluation> evals = new ArrayList<>(List.of(evalGlide, evalHammer));

        // Speed: target faster (isFaster = false); targetCanOHKO = true
        GrassyGlideValuationStrategy.adjustMoveValuations(
            evals, rillaboom, rillaActive, null, null,
            (atk, tgt) -> false,
            (tgt, atk) -> true
        );

        assertTrue(evalGlide.getScore() > evalHammer.getScore(),
            "When target is faster and can lethal-KO, Wood Hammer never executes; Glide priority damage wins");
        assertEquals(10, evalGlide.getScore(), "Glide should be maxOtherScore (9) + 1 = 10");
    }

    @Test
    @DisplayName("Case D1: Neither move OHKOs, Rillaboom faster -> Wood Hammer (maxMove) must win")
    void testCaseDNeitherOHKOAttackerFasterHammerWins() throws Exception {
        BattlePokemon rillaboom = createBattlePokemon(200, 200, true); // Speed 200
        BattlePokemon target = createBattlePokemon(300, 100, true); // HP 300, Speed 100

        ActiveBattlePokemon rillaActive = createActiveBattlePokemon(rillaboom);
        ActiveBattlePokemon targetActive = createActiveBattlePokemon(target);

        Move glide = createMove("grassyglide", 55.0d);
        Move hammer = createMove("woodhammer", 120.0d);

        RunBunAI.MoveEvaluation evalGlide = createEvaluation(glide, 70, 5, targetActive); // Non-KO chip, score 5
        RunBunAI.MoveEvaluation evalHammer = createEvaluation(hammer, 160, 8, targetActive); // maxMove, score 8

        List<RunBunAI.MoveEvaluation> evals = new ArrayList<>(List.of(evalGlide, evalHammer));

        GrassyGlideValuationStrategy.adjustMoveValuations(evals, rillaboom, rillaActive, null, null);

        assertTrue(evalHammer.getScore() > evalGlide.getScore(),
            "When Rillaboom is faster and neither move kills, large normal damage (Wood Hammer) must win over Glide chip");
        assertEquals(8, evalHammer.getScore());
        assertEquals(5, evalGlide.getScore());
    }

    @Test
    @DisplayName("Case D2: Neither move OHKOs, target faster but cannot lethal-KO -> Wood Hammer must win (protects live Miraidon regression)")
    void testCaseDNeitherOHKOTargetFasterTargetCannotOHKOHammerWins() throws Exception {
        BattlePokemon rillaboom = createBattlePokemon(200, 100, true); // Speed 100, HP 200
        BattlePokemon miraidon = createBattlePokemon(300, 250, true); // Speed 250, HP 300 (cannot OHKO Rillaboom)

        ActiveBattlePokemon rillaActive = createActiveBattlePokemon(rillaboom);
        ActiveBattlePokemon targetActive = createActiveBattlePokemon(miraidon);

        Move glide = createMove("grassyglide", 55.0d);
        Move hammer = createMove("woodhammer", 120.0d);

        RunBunAI.MoveEvaluation evalGlide = createEvaluation(glide, 70, 5, targetActive);
        RunBunAI.MoveEvaluation evalHammer = createEvaluation(hammer, 160, 6, targetActive); // maxMove score 6

        List<RunBunAI.MoveEvaluation> evals = new ArrayList<>(List.of(evalGlide, evalHammer));

        GrassyGlideValuationStrategy.adjustMoveValuations(evals, rillaboom, rillaActive, null, null);

        assertTrue(evalHammer.getScore() > evalGlide.getScore(),
            "Preserves high-damage normal move choice (Wood Hammer) over weak priority chip when not threatened with lethal KO");
        assertEquals(6, evalHammer.getScore());
        assertEquals(5, evalGlide.getScore());
    }

    @Test
    @DisplayName("Case D3: Neither move OHKOs, target faster AND can lethal-KO -> Glide wins realized priority damage")
    void testCaseDNeitherOHKOTargetFasterTargetCanOHKOGlideWinsRealized() throws Exception {
        BattlePokemon rillaboom = createBattlePokemon(200, 100, true);
        BattlePokemon lethalTarget = createBattlePokemon(300, 250, true);

        ActiveBattlePokemon rillaActive = createActiveBattlePokemon(rillaboom);
        ActiveBattlePokemon targetActive = createActiveBattlePokemon(lethalTarget);

        Move glide = createMove("grassyglide", 55.0d);
        Move hammer = createMove("woodhammer", 120.0d);

        RunBunAI.MoveEvaluation evalGlide = createEvaluation(glide, 70, 5, targetActive);
        RunBunAI.MoveEvaluation evalHammer = createEvaluation(hammer, 160, 8, targetActive); // maxMove score 8

        List<RunBunAI.MoveEvaluation> evals = new ArrayList<>(List.of(evalGlide, evalHammer));

        // Speed: target faster (isFaster = false); targetCanOHKO = true
        GrassyGlideValuationStrategy.adjustMoveValuations(
            evals, rillaboom, rillaActive, null, null,
            (atk, tgt) -> false,
            (tgt, atk) -> true
        );

        assertTrue(evalGlide.getScore() > evalHammer.getScore(),
            "When target is faster and can lethal-KO, Wood Hammer never executes; Glide priority chip wins");
        assertEquals(9, evalGlide.getScore(), "Glide should be maxOtherScore (8) + 1 = 9");
    }

    @Test
    @DisplayName("Invariant 1: Outside Grassy Terrain, Grassy Glide is NOT treated as priority (zero adjustments)")
    void testNoGrassyTerrainNoGlideBoost() throws Exception {
        BattlePokemon rillaboom = createBattlePokemon(200, 100, false); // Grassy Terrain false
        BattlePokemon target = createBattlePokemon(80, 200, false);

        ActiveBattlePokemon rillaActive = createActiveBattlePokemon(rillaboom);
        ActiveBattlePokemon targetActive = createActiveBattlePokemon(target);

        Move glide = createMove("grassyglide", 55.0d);
        Move hammer = createMove("woodhammer", 120.0d);

        RunBunAI.MoveEvaluation evalGlide = createEvaluation(glide, 90, 9, targetActive);
        RunBunAI.MoveEvaluation evalHammer = createEvaluation(hammer, 180, 9, targetActive);

        List<RunBunAI.MoveEvaluation> evals = new ArrayList<>(List.of(evalHammer, evalGlide));

        GrassyGlideValuationStrategy.adjustMoveValuations(evals, rillaboom, rillaActive, null, null);

        assertEquals(9, evalGlide.getScore(), "Outside Grassy Terrain, Glide score must remain unmodified");
        assertEquals(9, evalHammer.getScore(), "Outside Grassy Terrain, Hammer score must remain unmodified");
    }

    @Test
    @DisplayName("Safeguard: Grassy Terrain active but attacker raised (Flying-type) -> zero adjustments")
    void testAttackerRaisedNoGrassyGlidePriority() throws Exception {
        BattlePokemon flyingAttacker = createBattlePokemon(200, 100, true, true); // isRaised = true (Flying-type)
        ActiveBattlePokemon rillaActive = createActiveBattlePokemon(flyingAttacker);
        ActiveBattlePokemon targetActive = createActiveBattlePokemon(createBattlePokemon(80, 200, true));

        Move glide = createMove("grassyglide", 55.0d);
        Move hammer = createMove("woodhammer", 120.0d);

        RunBunAI.MoveEvaluation evalGlide = createEvaluation(glide, 90, 9, targetActive);
        RunBunAI.MoveEvaluation evalHammer = createEvaluation(hammer, 180, 9, targetActive);

        List<RunBunAI.MoveEvaluation> evals = new ArrayList<>(List.of(evalHammer, evalGlide));

        GrassyGlideValuationStrategy.adjustMoveValuations(evals, flyingAttacker, rillaActive, null, null);

        assertFalse(GrassyGlideValuationStrategy.isGrassyGlidePriority(flyingAttacker, rillaActive, null),
            "Raised attacker must not receive Grassy Glide priority");
        assertEquals(9, evalGlide.getScore(), "Raised attacker Glide score must remain unmodified");
        assertEquals(9, evalHammer.getScore(), "Raised attacker Hammer score must remain unmodified");
    }

    @Test
    @DisplayName("Safeguard: Target faster + can lethal-KO, but Glide deals 0 damage (immune) -> do NOT boost Glide")
    void testZeroDamageGlideWhenDyingNotBoosted() throws Exception {
        BattlePokemon rillaboom = createBattlePokemon(200, 100, true);
        BattlePokemon lethalTarget = createBattlePokemon(300, 250, true);

        ActiveBattlePokemon rillaActive = createActiveBattlePokemon(rillaboom);
        ActiveBattlePokemon targetActive = createActiveBattlePokemon(lethalTarget);

        Move glide = createMove("grassyglide", 55.0d);
        Move hammer = createMove("woodhammer", 120.0d);

        RunBunAI.MoveEvaluation evalGlide = createEvaluation(glide, 0, -50, targetActive); // 0 damage / immune
        RunBunAI.MoveEvaluation evalHammer = createEvaluation(hammer, 160, 8, targetActive);

        List<RunBunAI.MoveEvaluation> evals = new ArrayList<>(List.of(evalGlide, evalHammer));

        GrassyGlideValuationStrategy.adjustMoveValuations(
            evals, rillaboom, rillaActive, null, null,
            (atk, tgt) -> false,
            (tgt, atk) -> true
        );

        assertEquals(-50, evalGlide.getScore(), "Immune/0-damage Glide must never be boosted over positive moves");
        assertEquals(8, evalHammer.getScore());
    }
}
