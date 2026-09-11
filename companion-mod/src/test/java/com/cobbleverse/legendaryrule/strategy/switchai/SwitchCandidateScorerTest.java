package com.cobbleverse.legendaryrule.strategy.switchai;

import com.cobblemon.mod.common.api.abilities.Ability;
import com.cobblemon.mod.common.api.abilities.AbilityTemplate;
import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.api.battles.interpreter.BasicContext;
import com.cobblemon.mod.common.api.battles.interpreter.BattleContext;
import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.api.moves.categories.DamageCategory;
import com.cobblemon.mod.common.api.pokemon.helditem.HeldItemManager;
import com.cobblemon.mod.common.api.types.ElementalType;
import com.cobblemon.mod.common.api.types.ElementalTypes;
import com.cobblemon.mod.common.battles.ActiveBattlePokemon;
import com.cobblemon.mod.common.battles.BattleSide;
import com.cobblemon.mod.common.battles.actor.PokemonBattleActor;
import com.cobblemon.mod.common.battles.interpreter.ContextManager;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.pokemon.FormData;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobbleverse.legendaryrule.strategy.switchai.SwitchCandidateScorer.SurvivabilityResult;
import com.gitlab.surilexa.rbrctai.api.ai.utils.RBStatStages;
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

import static org.junit.jupiter.api.Assertions.*;

class SwitchCandidateScorerTest {

    private static Unsafe unsafe;
    private static Field moveTemplateField;
    private static Field moveNameField;
    private static Field moveElementalTypeField;
    private static Field moveCategoryField;
    private static Field categoryNameField;
    private static Field abilityTemplateField;
    private static Field abilityTemplateNameField;
    private static Field pokemonAbilityField;
    private static Field pokemonHeldItemField;
    private static Field pokemonCurrentHealthField;
    private static Field bpEffectedPokemonField;
    private static Field bpActorField;
    private static Field bpHeldItemDelegateField;
    private static Field pokemonMoveSetField;
    private static Field abpBattlePokemonField;
    private static Field abpActorField;
    private static Field abpBattleField;
    private static Field actorBattleField;
    private static Field battleSide1Field;
    private static Field battleSide2Field;
    private static Field bpContextManagerField;

    @BeforeAll
    static void setUpAll() throws Exception {
        net.minecraft.SharedConstants.createGameVersion();
        Field initializedField = net.minecraft.Bootstrap.class.getDeclaredField("initialized");
        initializedField.setAccessible(true);
        initializedField.setBoolean(null, true);

        Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        unsafe = (Unsafe) f.get(null);

        moveTemplateField = Move.class.getDeclaredField("template");
        moveTemplateField.setAccessible(true);

        moveNameField = MoveTemplate.class.getDeclaredField("name");
        moveNameField.setAccessible(true);

        moveElementalTypeField = MoveTemplate.class.getDeclaredField("elementalType");
        moveElementalTypeField.setAccessible(true);

        moveCategoryField = MoveTemplate.class.getDeclaredField("damageCategory");
        moveCategoryField.setAccessible(true);

        categoryNameField = DamageCategory.class.getDeclaredField("name");
        categoryNameField.setAccessible(true);

        abilityTemplateField = Ability.class.getDeclaredField("template");
        abilityTemplateField.setAccessible(true);

        abilityTemplateNameField = AbilityTemplate.class.getDeclaredField("name");
        abilityTemplateNameField.setAccessible(true);

        pokemonAbilityField = Pokemon.class.getDeclaredField("ability");
        pokemonAbilityField.setAccessible(true);

        pokemonHeldItemField = Pokemon.class.getDeclaredField("heldItem");
        pokemonHeldItemField.setAccessible(true);

        pokemonCurrentHealthField = Pokemon.class.getDeclaredField("currentHealth");
        pokemonCurrentHealthField.setAccessible(true);

        bpEffectedPokemonField = BattlePokemon.class.getDeclaredField("effectedPokemon");
        bpEffectedPokemonField.setAccessible(true);

        bpActorField = BattlePokemon.class.getDeclaredField("actor");
        bpActorField.setAccessible(true);

        bpHeldItemDelegateField = BattlePokemon.class.getDeclaredField("heldItemManager$delegate");
        bpHeldItemDelegateField.setAccessible(true);

        pokemonMoveSetField = Pokemon.class.getDeclaredField("moveSet");
        pokemonMoveSetField.setAccessible(true);

        abpBattlePokemonField = ActiveBattlePokemon.class.getDeclaredField("battlePokemon");
        abpBattlePokemonField.setAccessible(true);

        abpActorField = ActiveBattlePokemon.class.getDeclaredField("actor");
        abpActorField.setAccessible(true);

        abpBattleField = ActiveBattlePokemon.class.getDeclaredField("battle");
        abpBattleField.setAccessible(true);

        actorBattleField = BattleActor.class.getDeclaredField("battle");
        actorBattleField.setAccessible(true);

        battleSide1Field = PokemonBattle.class.getDeclaredField("side1");
        battleSide1Field.setAccessible(true);

        battleSide2Field = PokemonBattle.class.getDeclaredField("side2");
        battleSide2Field.setAccessible(true);

        bpContextManagerField = BattlePokemon.class.getDeclaredField("contextManager");
        bpContextManagerField.setAccessible(true);
    }

    private Move createMove(String name, ElementalType type, String categoryName) throws Exception {
        DamageCategory category = (DamageCategory) unsafe.allocateInstance(DamageCategory.class);
        categoryNameField.set(category, categoryName);

        MoveTemplate template = (MoveTemplate) unsafe.allocateInstance(MoveTemplate.class);
        moveNameField.set(template, name);
        moveElementalTypeField.set(template, type);
        moveCategoryField.set(template, category);
        Field movePowerField = MoveTemplate.class.getDeclaredField("power");
        movePowerField.setAccessible(true);
        movePowerField.setDouble(template, 100.0d);

        Field moveTargetField = MoveTemplate.class.getDeclaredField("target");
        moveTargetField.setAccessible(true);
        moveTargetField.set(template, com.cobblemon.mod.common.battles.MoveTarget.normal);

        Field moveAccuracyField = MoveTemplate.class.getDeclaredField("accuracy");
        moveAccuracyField.setAccessible(true);
        moveAccuracyField.setDouble(template, 100.0d);

        Field effectChancesField = MoveTemplate.class.getDeclaredField("effectChances");
        effectChancesField.setAccessible(true);
        effectChancesField.set(template, new Double[0]);

        return new Move(template, 10, 0);
    }

    private Move createMove(String name, ElementalType type, String categoryName, double power) throws Exception {
        Move m = createMove(name, type, categoryName);
        Field movePowerField = MoveTemplate.class.getDeclaredField("power");
        movePowerField.setAccessible(true);
        movePowerField.setDouble(m.getTemplate(), power);
        return m;
    }

    private BattlePokemon createBattlePokemon(String abilityName, String heldItemId, int currentHp, boolean withBattleContext) throws Exception {
        AbilityTemplate abilityTemplate = (AbilityTemplate) unsafe.allocateInstance(AbilityTemplate.class);
        abilityTemplateNameField.set(abilityTemplate, abilityName);

        Ability ability = (Ability) unsafe.allocateInstance(Ability.class);
        abilityTemplateField.set(ability, abilityTemplate);

        Pokemon pokemon = (Pokemon) unsafe.allocateInstance(Pokemon.class);
        pokemonAbilityField.set(pokemon, ability);
        pokemonHeldItemField.set(pokemon, net.minecraft.item.ItemStack.EMPTY);
        pokemon.setUuid(UUID.randomUUID());
        pokemonCurrentHealthField.setInt(pokemon, currentHp);

        // Stats setup for PokeMathMax
        FormData formData = (FormData) unsafe.allocateInstance(FormData.class);
        Field baseStatsField = FormData.class.getDeclaredField("_baseStats");
        baseStatsField.setAccessible(true);
        Map<com.cobblemon.mod.common.api.pokemon.stats.Stat, Integer> baseStats = Map.of(
            com.cobblemon.mod.common.api.pokemon.stats.Stats.HP, 100,
            com.cobblemon.mod.common.api.pokemon.stats.Stats.ATTACK, 100,
            com.cobblemon.mod.common.api.pokemon.stats.Stats.DEFENCE, 100,
            com.cobblemon.mod.common.api.pokemon.stats.Stats.SPECIAL_ATTACK, 100,
            com.cobblemon.mod.common.api.pokemon.stats.Stats.SPECIAL_DEFENCE, 100,
            com.cobblemon.mod.common.api.pokemon.stats.Stats.SPEED, 100
        );
        baseStatsField.set(formData, baseStats);

        com.cobblemon.mod.common.pokemon.Species species = (com.cobblemon.mod.common.pokemon.Species) unsafe.allocateInstance(com.cobblemon.mod.common.pokemon.Species.class);
        Field speciesNameField = com.cobblemon.mod.common.pokemon.Species.class.getDeclaredField("name");
        speciesNameField.setAccessible(true);
        speciesNameField.set(species, "Charizard");
        Field primaryTypeField = com.cobblemon.mod.common.pokemon.Species.class.getDeclaredField("primaryType");
        primaryTypeField.setAccessible(true);
        primaryTypeField.set(species, ElementalTypes.FIRE);
        species.setResourceIdentifier(net.minecraft.util.Identifier.of("cobblemon", "charizard"));
        formData.setSpecies(species);

        Field pokemonSpeciesField = Pokemon.class.getDeclaredField("species");
        pokemonSpeciesField.setAccessible(true);
        pokemonSpeciesField.set(pokemon, species);

        Field pokemonFormField = Pokemon.class.getDeclaredField("form");
        pokemonFormField.setAccessible(true);
        pokemonFormField.set(pokemon, formData);

        Field pokemonIvsField = Pokemon.class.getDeclaredField("ivs");
        pokemonIvsField.setAccessible(true);
        com.cobblemon.mod.common.pokemon.IVs ivs = new com.cobblemon.mod.common.pokemon.IVs();
        com.cobblemon.mod.common.pokemon.EVs evs = new com.cobblemon.mod.common.pokemon.EVs();
        for (com.cobblemon.mod.common.api.pokemon.stats.Stats stat : com.cobblemon.mod.common.api.pokemon.stats.Stats.values()) {
            ivs.set(stat, 31);
            evs.set(stat, 0);
        }
        pokemonIvsField.set(pokemon, ivs);

        Field pokemonEvsField = Pokemon.class.getDeclaredField("evs");
        pokemonEvsField.setAccessible(true);
        pokemonEvsField.set(pokemon, evs);

        Field pokemonLevelField = Pokemon.class.getDeclaredField("level");
        pokemonLevelField.setAccessible(true);
        pokemonLevelField.setInt(pokemon, 50);

        Field pokemonNatureField = Pokemon.class.getDeclaredField("nature");
        pokemonNatureField.setAccessible(true);
        pokemonNatureField.set(pokemon, com.cobblemon.mod.common.api.pokemon.Natures.INSTANCE.getHARDY());

        BattlePokemon bp = (BattlePokemon) unsafe.allocateInstance(BattlePokemon.class);
        bpEffectedPokemonField.set(bp, pokemon);

        if (withBattleContext) {
            PokemonBattle battle = (PokemonBattle) unsafe.allocateInstance(PokemonBattle.class);
            BattleActor actor = (BattleActor) unsafe.allocateInstance(PokemonBattleActor.class);
            
            BattleSide side1 = (BattleSide) unsafe.allocateInstance(com.cobblemon.mod.common.battles.BattleSide.class);
            BattleSide side2 = (BattleSide) unsafe.allocateInstance(com.cobblemon.mod.common.battles.BattleSide.class);
            Field actorsField = com.cobblemon.mod.common.battles.BattleSide.class.getDeclaredField("actors");
            actorsField.setAccessible(true);
            actorsField.set(side1, new BattleActor[]{ actor });
            actorsField.set(side2, new BattleActor[0]);

            Field sideContextManagerField = com.cobblemon.mod.common.battles.BattleSide.class.getDeclaredField("contextManager");
            sideContextManagerField.setAccessible(true);
            sideContextManagerField.set(side1, new ContextManager());
            sideContextManagerField.set(side2, new ContextManager());

            battleSide1Field.set(battle, side1);
            battleSide2Field.set(battle, side2);

            Field battleContextManagerField = PokemonBattle.class.getDeclaredField("contextManager");
            battleContextManagerField.setAccessible(true);
            battleContextManagerField.set(battle, new ContextManager());

            actorBattleField.set(actor, battle);
            Field actorActivePokemonField = BattleActor.class.getDeclaredField("activePokemon");
            actorActivePokemonField.setAccessible(true);
            actorActivePokemonField.set(actor, new java.util.ArrayList<ActiveBattlePokemon>());
            bpActorField.set(bp, actor);

            ContextManager cm = new ContextManager();
            bpContextManagerField.set(bp, cm);
        }

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

    private ActiveBattlePokemon createActiveBattlePokemon(BattlePokemon bp) throws Exception {
        ActiveBattlePokemon active = (ActiveBattlePokemon) unsafe.allocateInstance(ActiveBattlePokemon.class);
        abpBattlePokemonField.set(active, bp);
        if (bp.getActor() != null) {
            abpActorField.set(active, bp.getActor());
            abpBattleField.set(active, bp.getActor().getBattle());
            bp.getActor().getActivePokemon().add(active);
        }
        return active;
    }

    private void setMoves(BattlePokemon bp, Move... moves) throws Exception {
        Field movesArrayField = com.cobblemon.mod.common.api.moves.MoveSet.class.getDeclaredField("moves");
        movesArrayField.setAccessible(true);
        com.cobblemon.mod.common.api.moves.MoveSet ms = (com.cobblemon.mod.common.api.moves.MoveSet) unsafe.allocateInstance(com.cobblemon.mod.common.api.moves.MoveSet.class);
        movesArrayField.set(ms, moves);
        pokemonMoveSetField.set(bp.getEffectedPokemon(), ms);
    }

    private static class BattleFixture {
        final PokemonBattle battle;
        final BattleActor aiActor;
        final BattleActor oppActor;
        final BattleSide aiSide;
        final BattleSide oppSide;
        final ActiveBattlePokemon aiActive;
        final BattlePokemon aiActiveBP;

        BattleFixture(PokemonBattle battle, BattleActor aiActor, BattleActor oppActor, BattleSide aiSide, BattleSide oppSide, ActiveBattlePokemon aiActive, BattlePokemon aiActiveBP) {
            this.battle = battle;
            this.aiActor = aiActor;
            this.oppActor = oppActor;
            this.aiSide = aiSide;
            this.oppSide = oppSide;
            this.aiActive = aiActive;
            this.aiActiveBP = aiActiveBP;
        }
    }

    private BattleFixture createBattleFixture() throws Exception {
        PokemonBattle battle = (PokemonBattle) unsafe.allocateInstance(PokemonBattle.class);
        BattleActor aiActor = (BattleActor) unsafe.allocateInstance(PokemonBattleActor.class);
        BattleActor oppActor = (BattleActor) unsafe.allocateInstance(PokemonBattleActor.class);

        BattleSide side1 = (BattleSide) unsafe.allocateInstance(BattleSide.class);
        BattleSide side2 = (BattleSide) unsafe.allocateInstance(BattleSide.class);

        Field actorsField = BattleSide.class.getDeclaredField("actors");
        actorsField.setAccessible(true);
        actorsField.set(side1, new BattleActor[]{ aiActor });
        actorsField.set(side2, new BattleActor[]{ oppActor });

        Field sideContextManagerField = BattleSide.class.getDeclaredField("contextManager");
        sideContextManagerField.setAccessible(true);
        sideContextManagerField.set(side1, new ContextManager());
        sideContextManagerField.set(side2, new ContextManager());

        battleSide1Field.set(battle, side1);
        battleSide2Field.set(battle, side2);

        Field battleContextManagerField = PokemonBattle.class.getDeclaredField("contextManager");
        battleContextManagerField.setAccessible(true);
        battleContextManagerField.set(battle, new ContextManager());

        actorBattleField.set(aiActor, battle);
        actorBattleField.set(oppActor, battle);

        Field actorActivePokemonField = BattleActor.class.getDeclaredField("activePokemon");
        actorActivePokemonField.setAccessible(true);
        actorActivePokemonField.set(aiActor, new java.util.ArrayList<ActiveBattlePokemon>());
        actorActivePokemonField.set(oppActor, new java.util.ArrayList<ActiveBattlePokemon>());

        BattlePokemon aiBP = createBattlePokemon("blaze", null, 100, false);
        bpActorField.set(aiBP, aiActor);
        bpContextManagerField.set(aiBP, new ContextManager());

        ActiveBattlePokemon aiActive = (ActiveBattlePokemon) unsafe.allocateInstance(ActiveBattlePokemon.class);
        abpBattlePokemonField.set(aiActive, aiBP);
        abpActorField.set(aiActive, aiActor);
        abpBattleField.set(aiActive, battle);
        aiActor.getActivePokemon().add(aiActive);

        return new BattleFixture(battle, aiActor, oppActor, side1, side2, aiActive, aiBP);
    }

    private BattlePokemon createCandidate(BattleFixture fixture, String ability, int currentHp, Move... moves) throws Exception {
        BattlePokemon bp = createBattlePokemon(ability, null, currentHp, false);
        bpActorField.set(bp, fixture.aiActor);
        bpContextManagerField.set(bp, new ContextManager());
        if (moves != null && moves.length > 0) {
            setMoves(bp, moves);
        }
        return bp;
    }

    private ActiveBattlePokemon createOpponent(BattleFixture fixture, String ability, int currentHp, Move... moves) throws Exception {
        BattlePokemon bp = createBattlePokemon(ability, null, currentHp, false);
        bpActorField.set(bp, fixture.oppActor);
        bpContextManagerField.set(bp, new ContextManager());
        if (moves != null && moves.length > 0) {
            setMoves(bp, moves);
        }
        ActiveBattlePokemon active = (ActiveBattlePokemon) unsafe.allocateInstance(ActiveBattlePokemon.class);
        abpBattlePokemonField.set(active, bp);
        abpActorField.set(active, fixture.oppActor);
        abpBattleField.set(active, fixture.battle);
        fixture.oppActor.getActivePokemon().add(active);
        return active;
    }

    // ==========================================
    // 1. Packed Score Ranking & Precedence Tests
    // ==========================================

    @Test
    @DisplayName("Ranking Invariant: Min Tier 1 strictly beats Max Tier 2")
    void testTier1StrictlyBeatsTier2() {
        // Lowest possible Tier 1: coverage=0, defense=10000, native=-1
        int minTier1 = SwitchCandidateScorer.packScore(SwitchCandidateScorer.TIER1, 0, 10000, -1);

        // Highest possible Tier 2: coverage=2000, defense=10000, native=10
        int maxTier2 = SwitchCandidateScorer.packScore(SwitchCandidateScorer.TIER2, 2000, 10000, 10);

        assertTrue(minTier1 > maxTier2, "Any Tier 1 candidate must strictly beat any Tier 2 candidate (" + minTier1 + " > " + maxTier2 + ")");
    }

    @Test
    @DisplayName("Ranking Invariant: Within same tier, higher coverage beats lower coverage")
    void testCoveragePriorityWithinTier() {
        int tier1HighCoverage = SwitchCandidateScorer.packScore(SwitchCandidateScorer.TIER1, 1500, 10000, 0);
        int tier1LowCoverage = SwitchCandidateScorer.packScore(SwitchCandidateScorer.TIER1, 1000, 10000, 0);

        assertTrue(tier1HighCoverage > tier1LowCoverage, "Higher coverage must beat lower coverage within same tier");

        int tier2HighCoverage = SwitchCandidateScorer.packScore(SwitchCandidateScorer.TIER2, 1200, 8000, 0);
        int tier2LowCoverage = SwitchCandidateScorer.packScore(SwitchCandidateScorer.TIER2, 800, 8000, 0);

        assertTrue(tier2HighCoverage > tier2LowCoverage, "Higher coverage must beat lower coverage within Tier 2");
    }

    @Test
    @DisplayName("Ranking Invariant: Tie-break F-REV-02: 101% incoming beats 200% incoming in Tier 2")
    void testDefenseSeverityTieBreak() {
        // Candidate A: 101% incoming ratio -> rank = 10000 - 1010 = 8990
        int candidateA = SwitchCandidateScorer.packScore(SwitchCandidateScorer.TIER2, 1000, 8990, 0);

        // Candidate B: 200% incoming ratio -> rank = 10000 - 2000 = 8000
        int candidateB = SwitchCandidateScorer.packScore(SwitchCandidateScorer.TIER2, 1000, 8000, 0);

        assertTrue(candidateA > candidateB, "101% incoming candidate (rank 8990) must beat 200% incoming (rank 8000) when coverage is tied");
    }

    @Test
    @DisplayName("Ranking Invariant: Native bonus tie-breaks only when higher fields are tied")
    void testNativeBonusTieBreak() {
        int higherNative = SwitchCandidateScorer.packScore(SwitchCandidateScorer.TIER1, 1000, 10000, 1);
        int lowerNative = SwitchCandidateScorer.packScore(SwitchCandidateScorer.TIER1, 1000, 10000, 0);
        int lowestNative = SwitchCandidateScorer.packScore(SwitchCandidateScorer.TIER1, 1000, 10000, -1);

        assertTrue(higherNative > lowerNative);
        assertTrue(lowerNative > lowestNative);
    }

    @Test
    @DisplayName("Fail-loud F-REV-04: Native scores outside contract bounds [-1, 10] throw IllegalStateException")
    void testNativeScoreBoundsFailLoud() {
        assertThrows(IllegalStateException.class, () ->
            SwitchCandidateScorer.packScore(SwitchCandidateScorer.TIER1, 0, 10000, -2)
        );
        assertThrows(IllegalStateException.class, () ->
            SwitchCandidateScorer.packScore(SwitchCandidateScorer.TIER1, 0, 10000, 11)
        );
    }

    // ==========================================
    // 2. Doubles Survivability Tests
    // ==========================================

    @Test
    @DisplayName("F-P2-01: Single opponent with lethal move results in Tier 2 via evaluateSurvivability")
    void testSingleOpponentOHKOWithRealDamageResultsInTier2() throws Exception {
        BattleFixture fixture = createBattleFixture();
        Move flamethrower = createMove("flamethrower", ElementalTypes.FIRE, "special");
        // Candidate with 20 HP (damage ~35 > 20 HP)
        BattlePokemon candidate = createCandidate(fixture, "blaze", 20);
        ActiveBattlePokemon opp = createOpponent(fixture, "blaze", 100, flamethrower);

        SurvivabilityResult result = SwitchCandidateScorer.evaluateSurvivability(
            candidate, List.of(opp), fixture.aiActive, new RBStatStages()
        );

        assertEquals(SwitchCandidateScorer.TIER2, result.tier, "Candidate with 20 HP taking 35 damage must be Tier 2");
        assertTrue(result.combinedDamage >= 20, "Combined damage (" + result.combinedDamage + ") must exceed current HP");
        assertFalse(result.isUnknown, "Context is complete, isUnknown must be false");
    }

    @Test
    @DisplayName("F-P2-01: Candidate with Levitate takes 0 damage from Earthquake and classifies as Tier 1")
    void testRealImmunityRendersDamageZeroAndTier1() throws Exception {
        BattleFixture fixture = createBattleFixture();
        Move earthquake = createMove("earthquake", ElementalTypes.GROUND, "physical");
        BattlePokemon candidate = createCandidate(fixture, "levitate", 100);
        ActiveBattlePokemon opp = createOpponent(fixture, "blaze", 100, earthquake);

        SurvivabilityResult result = SwitchCandidateScorer.evaluateSurvivability(
            candidate, List.of(opp), fixture.aiActive, new RBStatStages()
        );

        assertEquals(SwitchCandidateScorer.TIER1, result.tier, "Levitate candidate taking 0 damage from Ground move must be Tier 1");
        assertEquals(0, result.combinedDamage, "Combined damage from Ground move must be 0");
        assertFalse(result.isUnknown);
    }

    @Test
    @DisplayName("F-P2-01: Opp1 (35 dmg) + Opp2 (35 dmg) vs 50 HP candidate results in Tier 2 via evaluateSurvivability")
    void testCombinedLethalWithRealMovesResultsInTier2() throws Exception {
        BattleFixture fixture = createBattleFixture();
        Move flamethrower1 = createMove("flamethrower", ElementalTypes.FIRE, "special");
        Move flamethrower2 = createMove("flamethrower", ElementalTypes.FIRE, "special");
        BattlePokemon candidate = createCandidate(fixture, "blaze", 50);
        ActiveBattlePokemon opp1 = createOpponent(fixture, "blaze", 100, flamethrower1);
        ActiveBattlePokemon opp2 = createOpponent(fixture, "blaze", 100, flamethrower2);

        SurvivabilityResult result = SwitchCandidateScorer.evaluateSurvivability(
            candidate, List.of(opp1, opp2), fixture.aiActive, new RBStatStages()
        );

        assertEquals(SwitchCandidateScorer.TIER2, result.tier, "Combined damage 70 vs 50 HP must be Tier 2");
        assertEquals(70, result.combinedDamage, "Combined damage must be 35 + 35 = 70");
        assertFalse(result.isUnknown);
    }

    @Test
    @DisplayName("F-P2-01: Opp1 (35 dmg) + Opp2 (35 dmg) vs 100 HP candidate results in Tier 1 via evaluateSurvivability")
    void testSafeCombinedWithRealMovesResultsInTier1() throws Exception {
        BattleFixture fixture = createBattleFixture();
        Move flamethrower1 = createMove("flamethrower", ElementalTypes.FIRE, "special");
        Move flamethrower2 = createMove("flamethrower", ElementalTypes.FIRE, "special");
        BattlePokemon candidate = createCandidate(fixture, "blaze", 100);
        ActiveBattlePokemon opp1 = createOpponent(fixture, "blaze", 100, flamethrower1);
        ActiveBattlePokemon opp2 = createOpponent(fixture, "blaze", 100, flamethrower2);

        SurvivabilityResult result = SwitchCandidateScorer.evaluateSurvivability(
            candidate, List.of(opp1, opp2), fixture.aiActive, new RBStatStages()
        );

        assertEquals(SwitchCandidateScorer.TIER1, result.tier, "Combined damage 70 vs 100 HP must be Tier 1 Safe");
        assertEquals(70, result.combinedDamage, "Combined damage must be 70");
        assertFalse(result.isUnknown);
    }

    @Test
    @DisplayName("Survivability: Fainted candidate (HP <= 0) classifies as Tier 2")
    void testFaintedCandidateClassifiesAsTier2() throws Exception {
        BattlePokemon deadCandidate = createBattlePokemon("levitate", null, 0, false);
        SurvivabilityResult result = SwitchCandidateScorer.evaluateSurvivability(deadCandidate, Collections.emptyList(), null, null);
        assertEquals(SwitchCandidateScorer.TIER2, result.tier, "Fainted candidate must be Tier 2");
    }

    @Test
    @DisplayName("F-P2-03: Missing evidence (null opponents, null moveset) results in Tier 2 and defenseRank 0")
    void testMissingEvidencePropagatesUnknownAndTier2() throws Exception {
        BattleFixture fixture = createBattleFixture();
        Move flamethrower = createMove("flamethrower", ElementalTypes.FIRE, "special");
        BattlePokemon candidate = createCandidate(fixture, "blaze", 100, flamethrower);

        // 1. opponents == null -> Tier 2, defenseRank = 0, coverage = 0
        int scoreNullOpponents = SwitchCandidateScorer.score(
            candidate, null, fixture.aiActive, new RBStatStages(), fixture.aiActiveBP, 0
        );
        int expectedNullOpponents = SwitchCandidateScorer.packScore(SwitchCandidateScorer.TIER2, 0, 0, 0);
        assertEquals(expectedNullOpponents, scoreNullOpponents, "Null opponents must evaluate to Tier 2 with defenseRank = 0");

        // 2. Opponent with null moveset -> isUnknown = true -> Tier 2, defenseRank = 0
        ActiveBattlePokemon oppNullMoves = createOpponent(fixture, "blaze", 100); // no moves set -> moveSet is null
        SurvivabilityResult resultNullMoves = SwitchCandidateScorer.evaluateSurvivability(
            candidate, List.of(oppNullMoves), fixture.aiActive, new RBStatStages()
        );
        assertTrue(resultNullMoves.isUnknown, "Null opponent moveset must set isUnknown = true");
        assertEquals(SwitchCandidateScorer.TIER2, resultNullMoves.tier, "Null opponent moveset must result in Tier 2");

        int scoreNullMoves = SwitchCandidateScorer.score(
            candidate, List.of(oppNullMoves), fixture.aiActive, new RBStatStages(), fixture.aiActiveBP, 0
        );
        int expectedCoverage = (int) Math.min(1000, (long) Math.ceil(1000.0 * 35 / oppNullMoves.getBattlePokemon().getMaxHealth()));
        int expectedNullMoves = SwitchCandidateScorer.packScore(SwitchCandidateScorer.TIER2, expectedCoverage, 0, 0);
        assertEquals(expectedNullMoves, scoreNullMoves, "Candidate with coverage against uninitialized opponent must receive defenseRank = 0 in Tier 2");
    }

    @Test
    @DisplayName("F-P2-01: calculateCoverage with single target normalizes by target max HP")
    void testCalculateCoverageSingleTarget() throws Exception {
        BattleFixture fixture = createBattleFixture();
        Move flamethrower = createMove("flamethrower", ElementalTypes.FIRE, "special");
        BattlePokemon candidate = createCandidate(fixture, "blaze", 100, flamethrower);
        ActiveBattlePokemon opp = createOpponent(fixture, "blaze", 100);

        long coverage = SwitchCandidateScorer.calculateCoverage(
            candidate, List.of(opp), fixture.aiActive, new RBStatStages()
        );

        long expectedCoverage = Math.min(1000, (long) Math.ceil(1000.0 * 35 / opp.getBattlePokemon().getMaxHealth()));
        assertEquals(expectedCoverage, coverage, "Coverage for 35 dmg / target max HP must match calculated expected coverage");
    }

    @Test
    @DisplayName("F-P2-01: calculateCoverage with two targets clamps each to 1000 and total to 2000")
    void testCalculateCoverageDoubleTargetClamping() throws Exception {
        BattleFixture fixture = createBattleFixture();
        // Move with 600 base power deals > 200 damage (> 175 max HP), achieving > 100% OHKO damage
        Move megaMove = createMove("blastburn", ElementalTypes.FIRE, "special", 600.0d);
        BattlePokemon candidate = createCandidate(fixture, "blaze", 100, megaMove);
        ActiveBattlePokemon opp1 = createOpponent(fixture, "blaze", 100);
        ActiveBattlePokemon opp2 = createOpponent(fixture, "blaze", 100);

        long coverage = SwitchCandidateScorer.calculateCoverage(
            candidate, List.of(opp1, opp2), fixture.aiActive, new RBStatStages()
        );

        // Target 1 clamped to 1000, Target 2 clamped to 1000 -> Total 2000
        assertEquals(2000, coverage, "Two OHKO targets must clamp to maximum total coverage of 2000");
    }

    @Test
    @DisplayName("F-P2-01: calculateCoverage ignores status moves and evaluates immune moves to 0")
    void testCalculateCoverageWithStatusAndImmuneMoves() throws Exception {
        BattleFixture fixture = createBattleFixture();
        Move statusMove = createMove("protect", ElementalTypes.NORMAL, "status");
        Move groundMove = createMove("earthquake", ElementalTypes.GROUND, "physical");
        BattlePokemon candidate = createCandidate(fixture, "blaze", 100, statusMove, groundMove);
        // Opponent with Levitate (immune to Ground)
        ActiveBattlePokemon opp = createOpponent(fixture, "levitate", 100);

        long coverage = SwitchCandidateScorer.calculateCoverage(
            candidate, List.of(opp), fixture.aiActive, new RBStatStages()
        );

        assertEquals(0, coverage, "Status moves and immune moves must yield 0 coverage");
    }

    @Test
    @DisplayName("F-P2-01: score() end-to-end with valid candidate packs Tier, Coverage, Defense, and Native")
    void testScoreEndToEndWithValidCandidate() throws Exception {
        BattleFixture fixture = createBattleFixture();
        Move flamethrower = createMove("flamethrower", ElementalTypes.FIRE, "special");
        BattlePokemon candidate = createCandidate(fixture, "blaze", 100, flamethrower);
        ActiveBattlePokemon opp1 = createOpponent(fixture, "blaze", 100, flamethrower);
        ActiveBattlePokemon opp2 = createOpponent(fixture, "blaze", 100, flamethrower);

        int nativeScore = 2;
        int packedScore = SwitchCandidateScorer.score(
            candidate, List.of(opp1, opp2), fixture.aiActive, new RBStatStages(), fixture.aiActiveBP, nativeScore
        );

        // Combined damage = 70 < 100 -> Tier 1 (tierRank = 1)
        long target1Coverage = Math.min(1000, (long) Math.ceil(1000.0 * 35 / opp1.getBattlePokemon().getMaxHealth()));
        long target2Coverage = Math.min(1000, (long) Math.ceil(1000.0 * 35 / opp2.getBattlePokemon().getMaxHealth()));
        long totalCoverage = target1Coverage + target2Coverage;
        // Tier 1 defenseRank = DEFENSE_RATIO_MAX = 10000
        // nativeScore = 2
        int expected = SwitchCandidateScorer.packScore(SwitchCandidateScorer.TIER1, totalCoverage, 10000, nativeScore);
        assertEquals(expected, packedScore, "Score must match deterministic packed value for Tier 1, coverage, max defense, native 2");
    }

    @Test
    @DisplayName("F-P2-01: DynamicMoveResolver integration through SwitchCandidateScorer (Weather Ball in Rain)")
    void testDynamicMoveIntegrationThroughScorer() throws Exception {
        BattleFixture fixture = createBattleFixture();
        fixture.battle.getContextManager().add(
            new BasicContext("rain", 1, BattleContext.Type.WEATHER, null)
        );

        Move weatherBall = createMove("weatherball", ElementalTypes.NORMAL, "special");
        Field movePowerField = MoveTemplate.class.getDeclaredField("power");
        movePowerField.setAccessible(true);
        movePowerField.setDouble(weatherBall.getTemplate(), 50.0d);

        BattlePokemon candidate = createCandidate(fixture, "blaze", 100, weatherBall);
        ActiveBattlePokemon opp = createOpponent(fixture, "blaze", 100);

        int damage = SwitchCandidateScorer.evaluateDamage(candidate, opp.getBattlePokemon(), weatherBall, fixture.aiActive, new RBStatStages());
        assertTrue(damage > 0, "Weather Ball in Rain must resolve and calculate damage > 0 (was " + damage + ")");
    }

    // ==========================================
    // 4. Ability Semantics & Guard Interactions
    // ==========================================

    @Test
    @DisplayName("Ability Semantics: Storm Drain / Water Absorb grants Water immunity")
    void testWaterImmunityActive() {
        assertTrue(SwitchCandidateScorer.isAbilityImmune("stormdrain", ElementalTypes.WATER, false, false, false));
        assertTrue(SwitchCandidateScorer.isAbilityImmune("waterabsorb", ElementalTypes.WATER, false, false, false));
    }

    @Test
    @DisplayName("Ability Semantics: Neutralizing Gas suppresses immunity unless protected by Ability Shield")
    void testNeutralizingGasSuppression() {
        // Suppressed without Ability Shield -> NOT immune
        assertFalse(SwitchCandidateScorer.isAbilityImmune("stormdrain", ElementalTypes.WATER, true, false, false));
        assertFalse(SwitchCandidateScorer.isAbilityImmune("levitate", ElementalTypes.GROUND, true, false, false));

        // Suppressed WITH Ability Shield -> IMMUNITY PRESERVED
        assertTrue(SwitchCandidateScorer.isAbilityImmune("stormdrain", ElementalTypes.WATER, true, false, true));
        assertTrue(SwitchCandidateScorer.isAbilityImmune("levitate", ElementalTypes.GROUND, true, false, true));
    }

    @Test
    @DisplayName("Ability Semantics: Mold Breaker bypasses immunity unless protected by Ability Shield")
    void testMoldBreakerBypass() {
        // Attacker has Mold Breaker (attackerIgnoresAbilities = true) without Ability Shield -> NOT immune
        assertFalse(SwitchCandidateScorer.isAbilityImmune("levitate", ElementalTypes.GROUND, false, true, false));
        assertFalse(SwitchCandidateScorer.isAbilityImmune("voltabsorb", ElementalTypes.ELECTRIC, false, true, false));

        // Attacker has Mold Breaker WITH Ability Shield -> IMMUNITY PRESERVED
        assertTrue(SwitchCandidateScorer.isAbilityImmune("levitate", ElementalTypes.GROUND, false, true, true));
        assertTrue(SwitchCandidateScorer.isAbilityImmune("voltabsorb", ElementalTypes.ELECTRIC, false, true, true));
    }

    @Test
    @DisplayName("Ability Semantics: Levitate and Earth Eater grant Ground immunity")
    void testGroundImmunityActive() {
        assertTrue(SwitchCandidateScorer.isAbilityImmune("levitate", ElementalTypes.GROUND, false, false, false));
        assertTrue(SwitchCandidateScorer.isAbilityImmune("eartheater", ElementalTypes.GROUND, false, false, false));
    }

    @Test
    @DisplayName("Score calculation handles null candidate safely")
    void testScoreNullCandidate() {
        int score = SwitchCandidateScorer.score(null, Collections.emptyList(), null, null, null, 0);
        int expected = SwitchCandidateScorer.packScore(SwitchCandidateScorer.TIER2, 0, 0, 0);
        assertEquals(expected, score, "Null candidate must evaluate to minimum Tier 2 score");
    }

    @Test
    @DisplayName("scoreWithDetails exposes identical packed score and accurate component breakdown")
    void testScoreWithDetailsExposesComponentsIdenticalToScore() throws Exception {
        BattleFixture fixture = createBattleFixture();
        Move flamethrower = createMove("flamethrower", ElementalTypes.FIRE, "special");
        BattlePokemon candidate = createCandidate(fixture, "blaze", 100, flamethrower);
        ActiveBattlePokemon opp = createOpponent(fixture, "blaze", 100, flamethrower);

        int nativeScore = 2;
        int directScore = SwitchCandidateScorer.score(
            candidate, List.of(opp), fixture.aiActive, new RBStatStages(), fixture.aiActiveBP, nativeScore
        );

        SwitchCandidateScorer.CandidateScoreDetails details = SwitchCandidateScorer.scoreWithDetails(
            candidate, List.of(opp), fixture.aiActive, new RBStatStages(), fixture.aiActiveBP, nativeScore
        );

        assertEquals(directScore, details.packedScore, "packedScore in details must exactly match score()");
        assertEquals(SwitchCandidateScorer.TIER1, details.tier, "Safe candidate must have Tier 1");
        long expectedCoverage = Math.min(1000, (long) Math.ceil(1000.0 * 35 / opp.getBattlePokemon().getMaxHealth()));
        assertEquals(expectedCoverage, details.coverage, "coverage in details must match calculated expected coverage");
        assertEquals(SwitchCandidateScorer.DEFENSE_RATIO_MAX, details.defenseRank, "Tier 1 must have max defenseRank");
        assertEquals(nativeScore, details.nativeScore, "nativeScore must match input");

        // Null candidate check
        SwitchCandidateScorer.CandidateScoreDetails nullDetails = SwitchCandidateScorer.scoreWithDetails(
            null, Collections.emptyList(), null, null, null, 0
        );
        assertEquals(SwitchCandidateScorer.score(null, Collections.emptyList(), null, null, null, 0), nullDetails.packedScore);
        assertEquals(SwitchCandidateScorer.TIER2, nullDetails.tier);
        assertEquals(0, nullDetails.coverage);
        assertEquals(0, nullDetails.defenseRank);
        assertEquals(0, nullDetails.nativeScore);
    }
}
