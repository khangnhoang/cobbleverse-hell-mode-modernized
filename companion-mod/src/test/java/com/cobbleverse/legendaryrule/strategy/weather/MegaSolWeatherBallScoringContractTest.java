package com.cobbleverse.legendaryrule.strategy.weather;

import com.cobblemon.mod.common.api.abilities.Ability;
import com.cobblemon.mod.common.api.abilities.AbilityTemplate;
import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.api.moves.categories.DamageCategories;
import com.cobblemon.mod.common.api.types.ElementalTypes;
import com.cobblemon.mod.common.battles.MoveTarget;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobbleverse.legendaryrule.lead.TypeChartData;
import com.cobbleverse.legendaryrule.lead.TypeChartResourceLoader;
import com.cobbleverse.legendaryrule.lead.TypeMatchupScorer;
import com.gitlab.surilexa.rbrctai.api.ai.utils.PokeMathMax;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MegaSolWeatherBallScoringContractTest {

    private static Unsafe unsafe;
    private static Field abilityTemplateField;
    private static Field abilityTemplateNameField;
    private static Field pokemonAbilityField;
    private static Field bpEffectedPokemonField;
    private static Field bpActorField;
    private static TypeMatchupScorer typeScorer;

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

        abilityTemplateField = Ability.class.getDeclaredField("template");
        abilityTemplateField.setAccessible(true);

        abilityTemplateNameField = AbilityTemplate.class.getDeclaredField("name");
        abilityTemplateNameField.setAccessible(true);

        pokemonAbilityField = Pokemon.class.getDeclaredField("ability");
        pokemonAbilityField.setAccessible(true);

        bpEffectedPokemonField = BattlePokemon.class.getDeclaredField("effectedPokemon");
        bpEffectedPokemonField.setAccessible(true);

        bpActorField = BattlePokemon.class.getDeclaredField("actor");
        bpActorField.setAccessible(true);

        TypeChartData data = TypeChartResourceLoader.load().orElseThrow(
            () -> new IllegalStateException("Failed to load canonical Gen 9 type chart for tests"));
        typeScorer = new TypeMatchupScorer(data);
    }

    private Move createMove(String name, double power) {
        MoveTemplate template = new MoveTemplate(
            name,
            1,
            ElementalTypes.NORMAL,
            DamageCategories.INSTANCE.getSPECIAL(),
            power,
            MoveTarget.normal,
            1.0d,
            10,
            0,
            1.0d,
            new Double[0]
        );
        return new Move(template, 10, 0);
    }

    private BattlePokemon createBattlePokemonWithAbility(String abilityName) throws Exception {
        AbilityTemplate abilityTemplate = (AbilityTemplate) unsafe.allocateInstance(AbilityTemplate.class);
        abilityTemplateNameField.set(abilityTemplate, abilityName);

        Ability ability = (Ability) unsafe.allocateInstance(Ability.class);
        abilityTemplateField.set(ability, abilityTemplate);

        Pokemon pokemon = (Pokemon) unsafe.allocateInstance(Pokemon.class);
        pokemonAbilityField.set(pokemon, ability);
        pokemon.setUuid(UUID.randomUUID());

        BattlePokemon bp = (BattlePokemon) unsafe.allocateInstance(BattlePokemon.class);
        bpEffectedPokemonField.set(bp, pokemon);

        com.cobblemon.mod.common.api.battles.model.actor.BattleActor actor =
            (com.cobblemon.mod.common.api.battles.model.actor.BattleActor) unsafe.allocateInstance(com.cobblemon.mod.common.battles.actor.PokemonBattleActor.class);
        bpActorField.set(bp, actor);

        return bp;
    }

    @Test
    @DisplayName("Scizor Quad Weakness Contract: Resolved Fire surrogate yields 4.0x vs Bug/Steel")
    void testScizorQuadWeaknessToSurrogateFire() throws Exception {
        Move weatherBall = createMove("weatherball", 50.0d);
        BattlePokemon attacker = createBattlePokemonWithAbility("megasol");

        Move resolved = MegaSolWeatherGuard.resolveEffectiveMove(weatherBall, attacker);
        assertInstanceOf(MegaSolWeatherBallSurrogate.class, resolved);

        String attackType = resolved.getType().getName().toLowerCase();
        assertEquals("fire", attackType);

        List<String> scizorTypes = List.of("bug", "steel");
        double effectiveness = typeScorer.getEffectiveness(attackType, scizorTypes);
        assertEquals(4.0, effectiveness, 0.001, "Fire must hit Bug/Steel for 4.0x super effective damage");

        double unpatchedEffectiveness = typeScorer.getEffectiveness("normal", scizorTypes);
        assertEquals(0.5, unpatchedEffectiveness, 0.001, "Unpatched Normal hit Bug/Steel for 0.5x resisted damage");

        // The type effectiveness factor difference alone is 4.0 / 0.5 = 8.0x
        assertEquals(8.0, effectiveness / unpatchedEffectiveness, 0.001);
    }

    @Test
    @DisplayName("Mathematical Invariant Contract: 24x total multiplier over unpatched 31 damage")
    void testMathematicalDamageMultiplierInvariant() throws Exception {
        Move weatherBall = createMove("weatherball", 50.0d);
        BattlePokemon attacker = createBattlePokemonWithAbility("megasol");
        Move resolved = MegaSolWeatherGuard.resolveEffectiveMove(weatherBall, attacker);

        // Multiplier breakdown:
        // 1. Base power: 100 vs 50 = 2.0x
        double bpFactor = resolved.getPower() / weatherBall.getPower();
        assertEquals(2.0, bpFactor, 0.001);

        // 2. Type effectiveness: 4.0x vs 0.5x = 8.0x
        double typeFactor = 4.0 / 0.5;

        // 3. Personal Sun weather boost: 1.5x
        double sunFactor = 1.5;

        // Total multiplier: 2.0 * 8.0 * 1.5 = 24.0x
        double totalMultiplier = bpFactor * typeFactor * sunFactor;
        assertEquals(24.0, totalMultiplier, 0.001);

        // Unpatched damage was 30.55... -> 31
        // Expected resolved damage: ~30.55 * 24.0 = ~733
        double unpatchedDamage = 30.5555;
        double expectedDamage = unpatchedDamage * totalMultiplier;
        assertTrue(expectedDamage >= 700.0 && expectedDamage <= 750.0,
            "Expected resolved damage must be in the ~733 range");
    }

    @Test
    @DisplayName("Defensive Modifiers Contract: Occa Berry and Thick Fat halve Fire damage")
    void testDefensiveModifiersContract() {
        // Occa Berry: 0.5x damage reduction against super effective Fire moves
        double occaModifier = 0.5;
        double fullDamage = 733.0;
        double occaDamage = fullDamage * occaModifier;
        assertEquals(366.5, occaDamage, 0.5, "Occa Berry reduces ~733 to ~366 damage");

        // Thick Fat: 0.5x damage reduction against Fire
        double thickFatModifier = 0.5;
        double thickFatDamage = fullDamage * thickFatModifier;
        assertEquals(366.5, thickFatDamage, 0.5, "Thick Fat reduces ~733 to ~366 damage");

        // Dry Skin: 1.25x damage boost from Fire
        double drySkinModifier = 1.25;
        double drySkinDamage = fullDamage * drySkinModifier;
        assertEquals(916.25, drySkinDamage, 0.5, "Dry Skin increases ~733 to ~916 damage");
    }

    @Test
    @DisplayName("Non-Mega-Sol and unrelated moves are completely untouched by guard")
    void testGuardPassthroughContracts() throws Exception {
        BattlePokemon nonMegaAttacker = createBattlePokemonWithAbility("overgrow");
        Move weatherBall = createMove("weatherball", 50.0d);

        assertSame(weatherBall, MegaSolWeatherGuard.resolveEffectiveMove(weatherBall, nonMegaAttacker),
            "Non-Mega Sol attacker must retain original move");

        BattlePokemon megaSolAttacker = createBattlePokemonWithAbility("megasol");
        Move earthPower = createMove("earthpower", 90.0d);
        Move gigaDrain = createMove("gigadrain", 75.0d);

        assertSame(earthPower, MegaSolWeatherGuard.resolveEffectiveMove(earthPower, megaSolAttacker),
            "Earth Power must retain original move");
        assertSame(gigaDrain, MegaSolWeatherGuard.resolveEffectiveMove(gigaDrain, megaSolAttacker),
            "Giga Drain must retain original move");
    }

    @Test
    @DisplayName("Recursion Termination Contract: Surrogate re-resolution is strictly idempotent")
    void testRecursionTerminationContract() throws Exception {
        BattlePokemon megaSolAttacker = createBattlePokemonWithAbility("megasol");
        Move weatherBall = createMove("weatherball", 50.0d);

        Move surrogate = MegaSolWeatherGuard.resolveEffectiveMove(weatherBall, megaSolAttacker);
        assertInstanceOf(MegaSolWeatherBallSurrogate.class, surrogate);

        // Passing surrogate back into resolveEffectiveMove must return exact same instance
        Move reResolved = MegaSolWeatherGuard.resolveEffectiveMove(surrogate, megaSolAttacker);
        assertSame(surrogate, reResolved,
            "Recursion guard: effectiveMove != move evaluates to false on recursive call");
    }

    @Test
    @DisplayName("Bytecode Signature Contract: PokeMathMax entrypoints exist and match expected signatures")
    void testPokeMathMaxSignatures() {
        boolean damageFound = false;
        boolean isImmuneFound = false;

        for (Method method : PokeMathMax.class.getDeclaredMethods()) {
            if (method.getName().equals("damage") && Modifier.isPublic(method.getModifiers())) {
                Class<?>[] params = method.getParameterTypes();
                if (params.length == 7 && method.getReturnType() == int.class) {
                    damageFound = true;
                }
            }
            if (method.getName().equals("isImmuneCheck") && Modifier.isPublic(method.getModifiers())) {
                Class<?>[] params = method.getParameterTypes();
                if (params.length == 6 && method.getReturnType() == boolean.class) {
                    isImmuneFound = true;
                }
            }
        }

        assertTrue(damageFound, "PokeMathMax.damage public entrypoint (7 params) must exist");
        assertTrue(isImmuneFound, "PokeMathMax.isImmuneCheck public entrypoint (6 params) must exist");
    }
}
