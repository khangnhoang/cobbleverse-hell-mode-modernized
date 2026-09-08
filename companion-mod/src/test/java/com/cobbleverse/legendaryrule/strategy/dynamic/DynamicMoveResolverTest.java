package com.cobbleverse.legendaryrule.strategy.dynamic;

import com.cobblemon.mod.common.api.abilities.Ability;
import com.cobblemon.mod.common.api.abilities.AbilityTemplate;
import com.cobblemon.mod.common.api.battles.interpreter.BasicContext;
import com.cobblemon.mod.common.api.battles.interpreter.BattleContext;
import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.api.moves.categories.DamageCategories;
import com.cobblemon.mod.common.api.types.ElementalType;
import com.cobblemon.mod.common.api.types.ElementalTypes;
import com.cobblemon.mod.common.api.types.tera.TeraType;
import com.cobblemon.mod.common.api.types.tera.TeraTypes;
import com.cobblemon.mod.common.battles.MoveTarget;
import com.cobblemon.mod.common.battles.interpreter.ContextManager;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.pokemon.FormData;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobbleverse.legendaryrule.strategy.weather.MegaSolWeatherBallSurrogate;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DynamicMoveResolverTest {

    private static Unsafe unsafe;
    private static Field abilityTemplateField;
    private static Field abilityTemplateNameField;
    private static Field pokemonAbilityField;
    private static Field bpEffectedPokemonField;
    private static Field bpActorField;
    private static Field bpContextManagerField;
    private static Field pokemonFormField;
    private static Field formNameField;
    private static Field formPrimaryTypeField;
    private static Field formSecondaryTypeField;
    private static Field pokemonAspectsField;
    private static Field pokemonTeraTypeField;

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

        bpContextManagerField = BattlePokemon.class.getDeclaredField("contextManager");
        bpContextManagerField.setAccessible(true);

        pokemonFormField = Pokemon.class.getDeclaredField("form");
        pokemonFormField.setAccessible(true);

        formNameField = FormData.class.getDeclaredField("name");
        formNameField.setAccessible(true);

        formPrimaryTypeField = FormData.class.getDeclaredField("_primaryType");
        formPrimaryTypeField.setAccessible(true);

        formSecondaryTypeField = FormData.class.getDeclaredField("_secondaryType");
        formSecondaryTypeField.setAccessible(true);

        pokemonAspectsField = Pokemon.class.getDeclaredField("aspects");
        pokemonAspectsField.setAccessible(true);

        pokemonTeraTypeField = Pokemon.class.getDeclaredField("teraType");
        pokemonTeraTypeField.setAccessible(true);
    }

    private Move createMove(String name, ElementalType type, double power, boolean physical) {
        MoveTemplate template = new MoveTemplate(
            name,
            1,
            type,
            physical ? DamageCategories.INSTANCE.getPHYSICAL() : DamageCategories.INSTANCE.getSPECIAL(),
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

    private BattlePokemon createAttacker(
        String abilityName,
        String formName,
        ElementalType primaryType,
        ElementalType secondaryType,
        Set<String> aspects,
        TeraType teraType,
        String weatherId
    ) throws Exception {
        Pokemon pokemon = (Pokemon) unsafe.allocateInstance(Pokemon.class);
        pokemon.setUuid(UUID.randomUUID());

        if (abilityName != null) {
            AbilityTemplate at = (AbilityTemplate) unsafe.allocateInstance(AbilityTemplate.class);
            abilityTemplateNameField.set(at, abilityName);
            Ability ability = (Ability) unsafe.allocateInstance(Ability.class);
            abilityTemplateField.set(ability, at);
            pokemonAbilityField.set(pokemon, ability);
        }

        FormData form = new FormData();
        formNameField.set(form, formName != null ? formName : "normal");
        formPrimaryTypeField.set(form, primaryType != null ? primaryType : ElementalTypes.NORMAL);
        formSecondaryTypeField.set(form, secondaryType);
        pokemonFormField.set(pokemon, form);

        if (aspects != null) {
            pokemonAspectsField.set(pokemon, aspects);
        }
        if (teraType != null) {
            pokemonTeraTypeField.set(pokemon, teraType);
        }

        BattlePokemon bp = (BattlePokemon) unsafe.allocateInstance(BattlePokemon.class);
        bpEffectedPokemonField.set(bp, pokemon);

        com.cobblemon.mod.common.api.battles.model.actor.BattleActor actor =
            (com.cobblemon.mod.common.api.battles.model.actor.BattleActor) unsafe.allocateInstance(com.cobblemon.mod.common.battles.actor.PokemonBattleActor.class);
        bpActorField.set(bp, actor);

        ContextManager cm = new ContextManager();
        if (weatherId != null) {
            cm.add(new BasicContext(weatherId, 1, BattleContext.Type.WEATHER, null));
        }
        bpContextManagerField.set(bp, cm);

        return bp;
    }

    @Test
    @DisplayName("Ivy Cudgel: Wellspring form resolves to Water 100 BP Physical retaining name")
    void testIvyCudgelWellspring() throws Exception {
        BattlePokemon attacker = createAttacker("waterabsorb", "Wellspring", ElementalTypes.GRASS, ElementalTypes.WATER, Set.of("wellspring-mask"), null, null);
        Move ivyCudgel = createMove("ivycudgel", ElementalTypes.GRASS, 100.0d, true);

        Move resolved = DynamicMoveResolver.resolveEffectiveMove(ivyCudgel, attacker);

        assertNotSame(ivyCudgel, resolved);
        assertInstanceOf(DynamicMoveSurrogate.class, resolved);
        assertEquals(DynamicMoveResolver.IVY_CUDGEL_NAME, resolved.getName(), "Must retain ivycudgel name for contact checks");
        assertEquals(ElementalTypes.WATER, resolved.getType(), "Wellspring Ivy Cudgel must resolve to Water");
        assertEquals(100.0d, resolved.getPower(), 0.001);
        assertEquals(DamageCategories.INSTANCE.getPHYSICAL(), resolved.getDamageCategory());
    }

    @Test
    @DisplayName("Ivy Cudgel: Hearthflame form resolves to Fire 100 BP Physical retaining name")
    void testIvyCudgelHearthflame() throws Exception {
        BattlePokemon attacker = createAttacker("moldbreaker", "Hearthflame", ElementalTypes.GRASS, ElementalTypes.FIRE, Set.of("hearthflame-mask"), null, null);
        Move ivyCudgel = createMove("ivycudgel", ElementalTypes.GRASS, 100.0d, true);

        Move resolved = DynamicMoveResolver.resolveEffectiveMove(ivyCudgel, attacker);

        assertEquals(DynamicMoveResolver.IVY_CUDGEL_NAME, resolved.getName());
        assertEquals(ElementalTypes.FIRE, resolved.getType(), "Hearthflame Ivy Cudgel must resolve to Fire");
        assertEquals(100.0d, resolved.getPower(), 0.001);
    }

    @Test
    @DisplayName("Ivy Cudgel: Cornerstone form resolves to Rock 100 BP Physical retaining name")
    void testIvyCudgelCornerstone() throws Exception {
        BattlePokemon attacker = createAttacker("sturdy", "Cornerstone", ElementalTypes.GRASS, ElementalTypes.ROCK, Set.of("cornerstone-mask"), null, null);
        Move ivyCudgel = createMove("ivycudgel", ElementalTypes.GRASS, 100.0d, true);

        Move resolved = DynamicMoveResolver.resolveEffectiveMove(ivyCudgel, attacker);

        assertEquals(DynamicMoveResolver.IVY_CUDGEL_NAME, resolved.getName());
        assertEquals(ElementalTypes.ROCK, resolved.getType(), "Cornerstone Ivy Cudgel must resolve to Rock");
        assertEquals(100.0d, resolved.getPower(), 0.001);
    }

    @Test
    @DisplayName("Ivy Cudgel: Teal Mask base form resolves to Grass 100 BP Physical")
    void testIvyCudgelTealMask() throws Exception {
        BattlePokemon attacker = createAttacker("defiant", "Teal-Mask", ElementalTypes.GRASS, null, Set.of("teal-mask"), null, null);
        Move ivyCudgel = createMove("ivycudgel", ElementalTypes.GRASS, 100.0d, true);

        Move resolved = DynamicMoveResolver.resolveEffectiveMove(ivyCudgel, attacker);

        assertEquals(DynamicMoveResolver.IVY_CUDGEL_NAME, resolved.getName());
        assertEquals(ElementalTypes.GRASS, resolved.getType(), "Teal Mask Ivy Cudgel must resolve to Grass");
        assertEquals(100.0d, resolved.getPower(), 0.001);
    }

    @Test
    @DisplayName("Raging Bull: Paldea-Blaze resolves to Fire 90 BP Physical retaining name")
    void testRagingBullBlaze() throws Exception {
        BattlePokemon attacker = createAttacker("intimidate", "Paldea-Blaze", ElementalTypes.FIGHTING, ElementalTypes.FIRE, Set.of("blaze"), null, null);
        Move ragingBull = createMove("ragingbull", ElementalTypes.NORMAL, 90.0d, true);

        Move resolved = DynamicMoveResolver.resolveEffectiveMove(ragingBull, attacker);

        assertEquals(DynamicMoveResolver.RAGING_BULL_NAME, resolved.getName(), "Must retain ragingbull name for contact checks");
        assertEquals(ElementalTypes.FIRE, resolved.getType());
        assertEquals(90.0d, resolved.getPower(), 0.001);
    }

    @Test
    @DisplayName("Raging Bull: Paldea-Aqua resolves to Water 90 BP Physical retaining name")
    void testRagingBullAqua() throws Exception {
        BattlePokemon attacker = createAttacker("intimidate", "Paldea-Aqua", ElementalTypes.FIGHTING, ElementalTypes.WATER, Set.of("aqua"), null, null);
        Move ragingBull = createMove("ragingbull", ElementalTypes.NORMAL, 90.0d, true);

        Move resolved = DynamicMoveResolver.resolveEffectiveMove(ragingBull, attacker);

        assertEquals(DynamicMoveResolver.RAGING_BULL_NAME, resolved.getName());
        assertEquals(ElementalTypes.WATER, resolved.getType());
        assertEquals(90.0d, resolved.getPower(), 0.001);
    }

    @Test
    @DisplayName("Raging Bull: Paldea-Combat resolves to Fighting 90 BP Physical retaining name")
    void testRagingBullCombat() throws Exception {
        BattlePokemon attacker = createAttacker("intimidate", "Paldea-Combat", ElementalTypes.FIGHTING, null, Set.of("combat"), null, null);
        Move ragingBull = createMove("ragingbull", ElementalTypes.NORMAL, 90.0d, true);

        Move resolved = DynamicMoveResolver.resolveEffectiveMove(ragingBull, attacker);

        assertEquals(DynamicMoveResolver.RAGING_BULL_NAME, resolved.getName());
        assertEquals(ElementalTypes.FIGHTING, resolved.getType());
        assertEquals(90.0d, resolved.getPower(), 0.001);
    }

    @Test
    @DisplayName("Weather Ball: Field Sun resolves to Fire 100 BP Special with resolved name")
    void testWeatherBallSun() throws Exception {
        BattlePokemon attacker = createAttacker("blaze", "Charizard", ElementalTypes.FIRE, ElementalTypes.FLYING, null, null, "sunnyday");
        Move weatherBall = createMove("weatherball", ElementalTypes.NORMAL, 50.0d, false);

        Move resolved = DynamicMoveResolver.resolveEffectiveMove(weatherBall, attacker);

        assertNotSame(weatherBall, resolved);
        assertEquals(DynamicMoveResolver.WEATHER_BALL_FIRE, resolved.getName());
        assertEquals(ElementalTypes.FIRE, resolved.getType());
        assertEquals(100.0d, resolved.getPower(), 0.001);
    }

    @Test
    @DisplayName("Weather Ball: Field Rain resolves to Water 100 BP Special with resolved name")
    void testWeatherBallRain() throws Exception {
        BattlePokemon attacker = createAttacker("torrent", "Blastoise", ElementalTypes.WATER, null, null, null, "raindance");
        Move weatherBall = createMove("weatherball", ElementalTypes.NORMAL, 50.0d, false);

        Move resolved = DynamicMoveResolver.resolveEffectiveMove(weatherBall, attacker);

        assertEquals(DynamicMoveResolver.WEATHER_BALL_WATER, resolved.getName());
        assertEquals(ElementalTypes.WATER, resolved.getType());
        assertEquals(100.0d, resolved.getPower(), 0.001);
    }

    @Test
    @DisplayName("Weather Ball: Field Sandstorm resolves to Rock 100 BP Special")
    void testWeatherBallSandstorm() throws Exception {
        BattlePokemon attacker = createAttacker("sandstream", "Tyranitar", ElementalTypes.ROCK, ElementalTypes.DARK, null, null, "sandstorm");
        Move weatherBall = createMove("weatherball", ElementalTypes.NORMAL, 50.0d, false);

        Move resolved = DynamicMoveResolver.resolveEffectiveMove(weatherBall, attacker);

        assertEquals(DynamicMoveResolver.WEATHER_BALL_ROCK, resolved.getName());
        assertEquals(ElementalTypes.ROCK, resolved.getType());
        assertEquals(100.0d, resolved.getPower(), 0.001);
    }

    @Test
    @DisplayName("Weather Ball: Field Snow resolves to Ice 100 BP Special")
    void testWeatherBallSnow() throws Exception {
        BattlePokemon attacker = createAttacker("snowwarning", "Abomasnow", ElementalTypes.GRASS, ElementalTypes.ICE, null, null, "snow");
        Move weatherBall = createMove("weatherball", ElementalTypes.NORMAL, 50.0d, false);

        Move resolved = DynamicMoveResolver.resolveEffectiveMove(weatherBall, attacker);

        assertEquals(DynamicMoveResolver.WEATHER_BALL_ICE, resolved.getName());
        assertEquals(ElementalTypes.ICE, resolved.getType());
        assertEquals(100.0d, resolved.getPower(), 0.001);
    }

    @Test
    @DisplayName("Weather Ball: Mega Sol personal weather resolves to MegaSolWeatherBallSurrogate (Fire 100 BP)")
    void testWeatherBallMegaSol() throws Exception {
        BattlePokemon attacker = createAttacker("megasol", "Solgaleo", ElementalTypes.PSYCHIC, ElementalTypes.STEEL, null, null, null);
        Move weatherBall = createMove("weatherball", ElementalTypes.NORMAL, 50.0d, false);

        Move resolved = DynamicMoveResolver.resolveEffectiveMove(weatherBall, attacker);

        assertInstanceOf(MegaSolWeatherBallSurrogate.class, resolved);
        assertEquals(MegaSolWeatherBallSurrogate.RESOLVED_MOVE_NAME, resolved.getName());
        assertEquals(ElementalTypes.FIRE, resolved.getType());
        assertEquals(100.0d, resolved.getPower(), 0.001);
    }

    @Test
    @DisplayName("Weather Ball: No active weather returns original move (Normal 50 BP)")
    void testWeatherBallNoWeather() throws Exception {
        BattlePokemon attacker = createAttacker("levitate", "Gengar", ElementalTypes.GHOST, ElementalTypes.POISON, null, null, null);
        Move weatherBall = createMove("weatherball", ElementalTypes.NORMAL, 50.0d, false);

        Move resolved = DynamicMoveResolver.resolveEffectiveMove(weatherBall, attacker);

        assertSame(weatherBall, resolved, "Without weather, Weather Ball must remain untouched");
    }

    @Test
    @DisplayName("Tera Blast: Predicted Fire Tera Type resolves to Fire 80 BP retaining name")
    void testTeraBlastPredicted() throws Exception {
        TeraType fireTera = TeraTypes.forElementalType(ElementalTypes.FIRE);
        BattlePokemon attacker = createAttacker("blaze", "Cinderace", ElementalTypes.FIRE, null, null, fireTera, null);
        Move teraBlast = createMove("terablast", ElementalTypes.NORMAL, 80.0d, false);

        Move resolved = DynamicMoveResolver.resolveEffectiveMove(teraBlast, attacker, null, true);

        assertEquals(DynamicMoveResolver.TERA_BLAST_NAME, resolved.getName());
        assertEquals(ElementalTypes.FIRE, resolved.getType());
        assertEquals(80.0d, resolved.getPower(), 0.001);
    }

    @Test
    @DisplayName("Revelation Dance: Resolves to attacker primary typing (Fire 90 BP Special)")
    void testRevelationDance() throws Exception {
        BattlePokemon attacker = createAttacker("dancer", "Oricorio-Baile", ElementalTypes.FIRE, ElementalTypes.FLYING, null, null, null);
        Move revDance = createMove("revelationdance", ElementalTypes.NORMAL, 90.0d, false);

        Move resolved = DynamicMoveResolver.resolveEffectiveMove(revDance, attacker);

        assertEquals(DynamicMoveResolver.REVELATION_DANCE_NAME, resolved.getName());
        assertEquals(ElementalTypes.FIRE, resolved.getType());
        assertEquals(90.0d, resolved.getPower(), 0.001);
    }

    @Test
    @DisplayName("Idempotence: Re-resolving an already-resolved surrogate is strictly idempotent")
    void testIdempotence() throws Exception {
        BattlePokemon attacker = createAttacker("waterabsorb", "Wellspring", ElementalTypes.GRASS, ElementalTypes.WATER, Set.of("wellspring-mask"), null, null);
        Move ivyCudgel = createMove("ivycudgel", ElementalTypes.GRASS, 100.0d, true);

        Move surrogate = DynamicMoveResolver.resolveEffectiveMove(ivyCudgel, attacker);
        assertInstanceOf(DynamicMoveSurrogate.class, surrogate);

        Move reResolved = DynamicMoveResolver.resolveEffectiveMove(surrogate, attacker);
        assertSame(surrogate, reResolved, "Surrogate resolution must terminate recursion immediately");
    }
}
