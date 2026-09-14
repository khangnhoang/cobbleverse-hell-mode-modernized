package com.cobbleverse.legendaryrule.lead.adapter;

import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.moves.MoveSet;
import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.api.moves.categories.DamageCategories;
import com.cobblemon.mod.common.api.moves.categories.DamageCategory;
import com.cobblemon.mod.common.api.types.ElementalTypes;
import com.cobblemon.mod.common.pokemon.Pokemon;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pins the damaging-move classification used by
 * {@link CobblemonLeadAdapter#extractDamagingMoveTypes(Pokemon)}.
 * <p>
 * This classification is deliberately divergent from the pre-existing
 * {@code SpreadFriendlyFireValuationStrategy.isDamaging(Move)} predicate: a move whose damage category
 * cannot be read is treated as "not established as damaging" (skipped) rather than falling back to a
 * power estimate, because the matcher's subject is the move's <em>type</em>, not a damage figure.
 */
class CobblemonLeadAdapterMovesetTest {

    private static Unsafe unsafe;
    private static Field moveTemplateField;
    private static Field moveSetMovesField;
    private static Field pokemonMoveSetField;

    @BeforeAll
    static void setUpAll() throws Exception {
        try {
            net.minecraft.SharedConstants.createGameVersion();
            java.lang.reflect.Method init = Class.forName("net.minecraft.Bootstrap").getDeclaredMethod("initialize");
            init.setAccessible(true);
            init.invoke(null);
        } catch (Throwable ignored) {
            // Already initialised (other suites in this JVM may have bootstrapped first).
        }

        Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        unsafe = (Unsafe) f.get(null);

        moveTemplateField = Move.class.getDeclaredField("template");
        moveTemplateField.setAccessible(true);
        moveSetMovesField = MoveSet.class.getDeclaredField("moves");
        moveSetMovesField.setAccessible(true);
        pokemonMoveSetField = Pokemon.class.getDeclaredField("moveSet");
        pokemonMoveSetField.setAccessible(true);
    }

    /** categoryKey is one of "physical", "special", "status", or "unreadable". */
    private static Move createMove(String name, String typeName, String categoryKey) throws Exception {
        MoveTemplate template = (MoveTemplate) unsafe.allocateInstance(MoveTemplate.class);
        setField(MoveTemplate.class, "name", template, name);
        setField(MoveTemplate.class, "elementalType", template, resolveType(typeName));

        DamageCategory category = switch (categoryKey) {
            case "physical" -> DamageCategories.INSTANCE.getPHYSICAL();
            case "special" -> DamageCategories.INSTANCE.getSPECIAL();
            case "status" -> DamageCategories.INSTANCE.getSTATUS();
            default -> null;
        };
        setField(MoveTemplate.class, "damageCategory", template, category);
        setField(MoveTemplate.class, "power", template, "status".equals(categoryKey) ? 0.0 : 90.0);

        Move move = (Move) unsafe.allocateInstance(Move.class);
        moveTemplateField.set(move, template);
        return move;
    }

    private static Object resolveType(String typeName) throws Exception {
        return ElementalTypes.class.getField(typeName.toUpperCase(java.util.Locale.ROOT)).get(null);
    }

    private static void setField(Class<?> owner, String name, Object target, Object value) throws Exception {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static Pokemon createPokemon(Move... moves) throws Exception {
        Pokemon pokemon = (Pokemon) unsafe.allocateInstance(Pokemon.class);
        MoveSet moveSet = (MoveSet) unsafe.allocateInstance(MoveSet.class);
        moveSetMovesField.set(moveSet, moves);
        pokemonMoveSetField.set(pokemon, moveSet);
        return pokemon;
    }

    @Test
    void damagingElectricMoveYieldsElectric() throws Exception {
        Pokemon pokemon = createPokemon(createMove("thunderbolt", "electric", "special"));

        assertEquals(List.of("electric"), CobblemonLeadAdapter.extractDamagingMoveTypes(pokemon));
    }

    @Test
    void electricStatusMoveDoesNotYieldElectric() throws Exception {
        Pokemon pokemon = createPokemon(createMove("thunderwave", "electric", "status"));

        assertEquals(List.of(), CobblemonLeadAdapter.extractDamagingMoveTypes(pokemon),
                "A status move of the requested type must NOT satisfy damagingMoveType");
    }

    @Test
    void normalStatusMoveYieldsNothing() throws Exception {
        Pokemon pokemon = createPokemon(createMove("protect", "normal", "status"));

        assertEquals(List.of(), CobblemonLeadAdapter.extractDamagingMoveTypes(pokemon));
    }

    @Test
    void duplicateDamagingTypesAreDeduplicatedAndStatusIsExcluded() throws Exception {
        Pokemon pokemon = createPokemon(
                createMove("voltswitch", "electric", "special"),
                createMove("wildcharge", "electric", "physical"),
                createMove("protect", "normal", "status"));

        assertEquals(List.of("electric"), CobblemonLeadAdapter.extractDamagingMoveTypes(pokemon));
    }

    @Test
    void lightningRodStyleBoardKeepsOnlyTheDamagingGroundType() throws Exception {
        Pokemon pokemon = createPokemon(
                createMove("earthquake", "ground", "physical"),
                createMove("thunderwave", "electric", "status"));

        assertEquals(List.of("ground"), CobblemonLeadAdapter.extractDamagingMoveTypes(pokemon));
    }

    @Test
    void unreadableDamageCategoryIsTreatedAsNotEstablishedAsDamaging() throws Exception {
        Pokemon pokemon = createPokemon(createMove("mysterymove", "electric", "unreadable"));

        assertEquals(List.of(), CobblemonLeadAdapter.extractDamagingMoveTypes(pokemon),
                "A null damage category must be skipped rather than resolved through a power fallback");
    }

    @Test
    void nullInputsYieldEmptyList() throws Exception {
        assertEquals(List.of(), CobblemonLeadAdapter.extractDamagingMoveTypes(null));

        Pokemon pokemonWithoutMoveSet = (Pokemon) unsafe.allocateInstance(Pokemon.class);
        assertEquals(List.of(), CobblemonLeadAdapter.extractDamagingMoveTypes(pokemonWithoutMoveSet));
    }
}
