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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MegaSolWeatherGuardTest {

    private static Unsafe unsafe;
    private static Field abilityTemplateField;
    private static Field abilityTemplateNameField;
    private static Field pokemonAbilityField;
    private static Field bpEffectedPokemonField;
    private static Field bpActorField;

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
    }

    private Move createMove(String name) {
        MoveTemplate template = new MoveTemplate(
            name,
            1,
            ElementalTypes.NORMAL,
            DamageCategories.INSTANCE.getSPECIAL(),
            50.0d,
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
    @DisplayName("Null safety: null move or null attacker returns original unchanged")
    void testNullSafety() throws Exception {
        Move move = createMove("weatherball");
        BattlePokemon attacker = createBattlePokemonWithAbility("megasol");

        assertSame(move, MegaSolWeatherGuard.resolveEffectiveMove(move, null));
        assertNull(MegaSolWeatherGuard.resolveEffectiveMove(null, attacker));
        assertFalse(MegaSolWeatherGuard.hasMegaSol(null));
    }

    @Test
    @DisplayName("Non-Weather-Ball moves return original Move instance")
    void testNonWeatherBallMoveUntouched() throws Exception {
        Move earthPower = createMove("earthpower");
        BattlePokemon attacker = createBattlePokemonWithAbility("megasol");

        Move resolved = MegaSolWeatherGuard.resolveEffectiveMove(earthPower, attacker);
        assertSame(earthPower, resolved, "Non-weather-ball move must return original instance");
    }

    @Test
    @DisplayName("Attacker without Mega Sol (e.g. overgrow) returns original Move instance")
    void testNonMegaSolAttackerUntouched() throws Exception {
        Move weatherBall = createMove("weatherball");
        BattlePokemon attacker = createBattlePokemonWithAbility("overgrow");

        assertFalse(MegaSolWeatherGuard.hasMegaSol(attacker));
        Move resolved = MegaSolWeatherGuard.resolveEffectiveMove(weatherBall, attacker);
        assertSame(weatherBall, resolved, "Attacker without Mega Sol must retain original move");
    }

    @Test
    @DisplayName("Attacker with unsuppressed Mega Sol resolves Weather Ball to surrogate")
    void testMegaSolAttackerResolvesSurrogate() throws Exception {
        Move weatherBall = createMove("weatherball");
        BattlePokemon attacker = createBattlePokemonWithAbility("megasol");

        assertTrue(MegaSolWeatherGuard.hasMegaSol(attacker));
        Move resolved = MegaSolWeatherGuard.resolveEffectiveMove(weatherBall, attacker);

        assertNotSame(weatherBall, resolved, "Should return a surrogate instance");
        assertInstanceOf(MegaSolWeatherBallSurrogate.class, resolved);
        assertEquals(MegaSolWeatherBallSurrogate.RESOLVED_MOVE_NAME, resolved.getName());
        assertEquals(ElementalTypes.FIRE, resolved.getType());
        assertEquals(100.0d, resolved.getPower(), 0.001);
    }

    @Test
    @DisplayName("Idempotence: passing an already resolved surrogate returns the identical instance")
    void testIdempotenceAndRecursionSafety() throws Exception {
        Move weatherBall = createMove("weatherball");
        BattlePokemon attacker = createBattlePokemonWithAbility("megasol");

        Move surrogate = MegaSolWeatherGuard.resolveEffectiveMove(weatherBall, attacker);
        assertInstanceOf(MegaSolWeatherBallSurrogate.class, surrogate);

        Move reResolved = MegaSolWeatherGuard.resolveEffectiveMove(surrogate, attacker);
        assertSame(surrogate, reResolved, "Re-resolving a surrogate must be strictly idempotent");
    }
}
