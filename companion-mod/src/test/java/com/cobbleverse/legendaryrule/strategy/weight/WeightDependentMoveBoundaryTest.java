package com.cobbleverse.legendaryrule.strategy.weight;

import com.cobblemon.mod.common.api.abilities.Ability;
import com.cobblemon.mod.common.api.abilities.AbilityTemplate;
import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.api.moves.categories.DamageCategories;
import com.cobblemon.mod.common.api.pokemon.helditem.HeldItemManager;
import com.cobblemon.mod.common.api.types.ElementalTypes;
import com.cobblemon.mod.common.battles.MoveTarget;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.pokemon.FormData;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import kotlin.LazyKt;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.*;

class WeightDependentMoveBoundaryTest {

    private static Unsafe unsafe;
    private static Field formWeightField;
    private static Field bpEffectedPokemonField;
    private static Field bpActorField;
    private static Field bpHeldItemDelegateField;
    private static Field pokemonFormField;
    private static Field pokemonSpeciesField;
    private static Field pokemonAbilityField;
    private static Field abilityTemplateField;
    private static Field abilityTemplateNameField;
    private static Field speciesStandardFormDelegateField;

    @BeforeAll
    static void setUpAll() throws Exception {
        try {
            net.minecraft.SharedConstants.createGameVersion();
            java.lang.reflect.Method init = Class.forName("net.minecraft.Bootstrap").getDeclaredMethod("initialize");
            init.setAccessible(true);
            init.invoke(null);
        } catch (Throwable ignored) {
        }

        Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        unsafe = (Unsafe) unsafeField.get(null);

        formWeightField = FormData.class.getDeclaredField("_weight");
        formWeightField.setAccessible(true);

        bpEffectedPokemonField = BattlePokemon.class.getDeclaredField("effectedPokemon");
        bpEffectedPokemonField.setAccessible(true);

        bpActorField = BattlePokemon.class.getDeclaredField("actor");
        bpActorField.setAccessible(true);

        bpHeldItemDelegateField = BattlePokemon.class.getDeclaredField("heldItemManager$delegate");
        bpHeldItemDelegateField.setAccessible(true);

        pokemonFormField = Pokemon.class.getDeclaredField("form");
        pokemonFormField.setAccessible(true);

        pokemonSpeciesField = Pokemon.class.getDeclaredField("species");
        pokemonSpeciesField.setAccessible(true);

        pokemonAbilityField = Pokemon.class.getDeclaredField("ability");
        pokemonAbilityField.setAccessible(true);

        abilityTemplateField = Ability.class.getDeclaredField("template");
        abilityTemplateField.setAccessible(true);

        abilityTemplateNameField = AbilityTemplate.class.getDeclaredField("name");
        abilityTemplateNameField.setAccessible(true);

        speciesStandardFormDelegateField = Species.class.getDeclaredField("standardForm$delegate");
        speciesStandardFormDelegateField.setAccessible(true);
    }

    private BattlePokemon createBattlePokemon(float weightHg, String abilityName, String heldItemId) throws Exception {
        FormData formData = (FormData) unsafe.allocateInstance(FormData.class);
        formWeightField.set(formData, weightHg);

        Pokemon pokemon = (Pokemon) unsafe.allocateInstance(Pokemon.class);
        pokemonFormField.set(pokemon, formData);

        if (abilityName != null) {
            AbilityTemplate abilityTemplate = (AbilityTemplate) unsafe.allocateInstance(AbilityTemplate.class);
            abilityTemplateNameField.set(abilityTemplate, abilityName);

            Ability ability = (Ability) unsafe.allocateInstance(Ability.class);
            abilityTemplateField.set(ability, abilityTemplate);

            pokemonAbilityField.set(pokemon, ability);
        }

        BattlePokemon bp = (BattlePokemon) unsafe.allocateInstance(BattlePokemon.class);
        bpEffectedPokemonField.set(bp, pokemon);

        if (heldItemId != null) {
            HeldItemManager mockItemManager = (HeldItemManager) Proxy.newProxyInstance(
                HeldItemManager.class.getClassLoader(),
                new Class<?>[]{HeldItemManager.class},
                (proxy, method, args) -> {
                    if ("showdownId".equals(method.getName())) {
                        return heldItemId;
                    }
                    return null;
                }
            );
            bpHeldItemDelegateField.set(bp, LazyKt.lazyOf(mockItemManager));
        }

        return bp;
    }

    private Move createMove(String name) {
        MoveTemplate template = new MoveTemplate(
            name,
            1,
            ElementalTypes.NORMAL,
            DamageCategories.INSTANCE.getPHYSICAL(),
            0.0d,
            MoveTarget.normal,
            1.0d,
            20,
            0,
            1.0d,
            new Double[0]
        );
        return new Move(template, 20, 0);
    }

    @ParameterizedTest(name = "Target weight {0} hg ({1} kg) -> {2} BP")
    @CsvSource({
        "99.0,   9.9,   20.0",
        "100.0,  10.0,  40.0",
        "249.0,  24.9,  40.0",
        "250.0,  25.0,  60.0",
        "499.0,  49.9,  60.0",
        "500.0,  50.0,  80.0",
        "999.0,  99.9,  80.0",
        "1000.0, 100.0, 100.0",
        "1999.0, 199.9, 100.0",
        "2000.0, 200.0, 120.0",
        "9500.0, 950.0, 120.0"
    })
    @DisplayName("Canonical Target-Weight Tier Boundaries (Grass Knot / Low Kick)")
    void testTargetWeightTierBoundaries(double weightHg, double weightKg, double expectedBp) throws Exception {
        assertEquals(expectedBp, WeightDependentMoveResolver.calculateTargetWeightBasePower(weightHg), 0.001,
            "Base power mismatch for target weight " + weightKg + " kg");

        BattlePokemon attacker = createBattlePokemon(500.0f, null, null);
        BattlePokemon defender = createBattlePokemon((float) weightHg, null, null);

        Move grassKnot = createMove("grassknot");
        Move resolvedGk = WeightDependentMoveResolver.resolveEffectiveMove(grassKnot, attacker, defender);
        assertEquals(expectedBp, resolvedGk.getPower(), 0.001, "Grass Knot resolved BP mismatch");

        Move lowKick = createMove("lowkick");
        Move resolvedLk = WeightDependentMoveResolver.resolveEffectiveMove(lowKick, attacker, defender);
        assertEquals(expectedBp, resolvedLk.getPower(), 0.001, "Low Kick resolved BP mismatch");
    }

    @ParameterizedTest(name = "Attacker {0} hg vs Defender {1} hg (Ratio {2}x) -> {3} BP")
    @CsvSource({
        "999.0,  500.0, 1.998, 40.0",
        "1000.0, 500.0, 2.000, 60.0",
        "1499.0, 500.0, 2.998, 60.0",
        "1500.0, 500.0, 3.000, 80.0",
        "1999.0, 500.0, 3.998, 80.0",
        "2000.0, 500.0, 4.000, 100.0",
        "2499.0, 500.0, 4.998, 100.0",
        "2500.0, 500.0, 5.000, 120.0",
        "5000.0, 500.0, 10.00, 120.0"
    })
    @DisplayName("Canonical Weight-Ratio Tier Boundaries (Heavy Slam / Heat Crash)")
    void testWeightRatioTierBoundaries(double attackerHg, double defenderHg, double ratio, double expectedBp) throws Exception {
        assertEquals(expectedBp, WeightDependentMoveResolver.calculateWeightRatioBasePower(attackerHg, defenderHg), 0.001,
            "Base power mismatch for ratio " + ratio);

        BattlePokemon attacker = createBattlePokemon((float) attackerHg, null, null);
        BattlePokemon defender = createBattlePokemon((float) defenderHg, null, null);

        Move heavySlam = createMove("heavyslam");
        Move resolvedHs = WeightDependentMoveResolver.resolveEffectiveMove(heavySlam, attacker, defender);
        assertEquals(expectedBp, resolvedHs.getPower(), 0.001, "Heavy Slam resolved BP mismatch");

        Move heatCrash = createMove("heatcrash");
        Move resolvedHc = WeightDependentMoveResolver.resolveEffectiveMove(heatCrash, attacker, defender);
        assertEquals(expectedBp, resolvedHc.getPower(), 0.001, "Heat Crash resolved BP mismatch");
    }

    @Test
    @DisplayName("Heavy Metal doubles effective weight and promotes move tier")
    void testHeavyMetalDoublesWeight() throws Exception {
        // Base weight 500 hg (50 kg) -> 80 BP
        BattlePokemon normalDefender = createBattlePokemon(500.0f, null, null);
        assertEquals(500.0, WeightDependentMoveResolver.getEffectiveWeight(normalDefender), 0.001);

        // Heavy Metal doubles to 1000 hg (100 kg) -> 100 BP
        BattlePokemon heavyMetalDefender = createBattlePokemon(500.0f, "heavymetal", null);
        assertEquals(1000.0, WeightDependentMoveResolver.getEffectiveWeight(heavyMetalDefender), 0.001);

        BattlePokemon attacker = createBattlePokemon(500.0f, null, null);
        Move grassKnot = createMove("grassknot");

        Move normalResolved = WeightDependentMoveResolver.resolveEffectiveMove(grassKnot, attacker, normalDefender);
        assertEquals(80.0, normalResolved.getPower(), 0.001);

        Move heavyResolved = WeightDependentMoveResolver.resolveEffectiveMove(grassKnot, attacker, heavyMetalDefender);
        assertEquals(100.0, heavyResolved.getPower(), 0.001);
    }

    @Test
    @DisplayName("Light Metal halves effective weight and demotes move tier")
    void testLightMetalHalvesWeight() throws Exception {
        // Base weight 1000 hg (100 kg) -> 100 BP
        BattlePokemon normalDefender = createBattlePokemon(1000.0f, null, null);
        assertEquals(1000.0, WeightDependentMoveResolver.getEffectiveWeight(normalDefender), 0.001);

        // Light Metal halves to 500 hg (50 kg) -> 80 BP
        BattlePokemon lightMetalDefender = createBattlePokemon(1000.0f, "lightmetal", null);
        assertEquals(500.0, WeightDependentMoveResolver.getEffectiveWeight(lightMetalDefender), 0.001);

        BattlePokemon attacker = createBattlePokemon(500.0f, null, null);
        Move grassKnot = createMove("grassknot");

        Move normalResolved = WeightDependentMoveResolver.resolveEffectiveMove(grassKnot, attacker, normalDefender);
        assertEquals(100.0, normalResolved.getPower(), 0.001);

        Move lightResolved = WeightDependentMoveResolver.resolveEffectiveMove(grassKnot, attacker, lightMetalDefender);
        assertEquals(80.0, lightResolved.getPower(), 0.001);
    }

    @Test
    @DisplayName("Float Stone halves effective weight")
    void testFloatStoneHalvesWeight() throws Exception {
        BattlePokemon floatStoneDefender = createBattlePokemon(1000.0f, null, "floatstone");
        assertEquals(500.0, WeightDependentMoveResolver.getEffectiveWeight(floatStoneDefender), 0.001);
    }

    @Test
    @DisplayName("Light Metal and Float Stone together halve weight twice (floor division)")
    void testLightMetalAndFloatStoneStack() throws Exception {
        // 1000 hg -> Light Metal halves to 500 hg -> Float Stone halves to 250 hg
        BattlePokemon stackedDefender = createBattlePokemon(1000.0f, "lightmetal", "floatstone");
        assertEquals(250.0, WeightDependentMoveResolver.getEffectiveWeight(stackedDefender), 0.001);
    }

    @Test
    @DisplayName("Standard form fallback is used when Pokemon form is null")
    void testStandardFormFallback() throws Exception {
        FormData standardForm = (FormData) unsafe.allocateInstance(FormData.class);
        formWeightField.set(standardForm, 800.0f); // 80 kg

        Species species = (Species) unsafe.allocateInstance(Species.class);
        speciesStandardFormDelegateField.set(species, LazyKt.lazyOf(standardForm));

        Pokemon pokemon = (Pokemon) unsafe.allocateInstance(Pokemon.class);
        pokemonFormField.set(pokemon, null); // Explicitly null form
        pokemonSpeciesField.set(pokemon, species);

        BattlePokemon bp = (BattlePokemon) unsafe.allocateInstance(BattlePokemon.class);
        bpEffectedPokemonField.set(bp, pokemon);

        assertEquals(800.0, WeightDependentMoveResolver.getEffectiveWeight(bp), 0.001,
            "Must fall back to species.getStandardForm().getWeight()");
    }

    @Test
    @DisplayName("Minimum effective weight is clamped to 1.0 hg")
    void testMinimumWeightClamp() throws Exception {
        BattlePokemon tinyDefender = createBattlePokemon(0.5f, null, null);
        assertEquals(1.0, WeightDependentMoveResolver.getEffectiveWeight(tinyDefender), 0.001,
            "Effective weight must be clamped to at least 1.0 hg");

        BattlePokemon zeroDefender = createBattlePokemon(0.0f, null, null);
        assertEquals(1.0, WeightDependentMoveResolver.getEffectiveWeight(zeroDefender), 0.001,
            "Effective weight must be clamped to at least 1.0 hg");
    }
}
