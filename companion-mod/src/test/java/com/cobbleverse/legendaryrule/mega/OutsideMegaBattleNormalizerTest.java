package com.cobbleverse.legendaryrule.mega;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.pokemon.feature.SpeciesFeature;
import com.cobblemon.mod.common.api.pokemon.feature.StringSpeciesFeature;
import com.cobblemon.mod.common.api.reactive.SettableObservable;
import com.cobblemon.mod.common.battles.BattleSide;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import com.cobblemon.mod.common.battles.actor.PokemonBattleActor;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.pokemon.FormData;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import net.minecraft.nbt.NbtCompound;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class OutsideMegaBattleNormalizerTest {

    private static Unsafe unsafe;
    private Species testSpecies;
    private FormData standardForm;

    @BeforeAll
    static void setUpAll() {
        try {
            net.minecraft.SharedConstants.createGameVersion();
            java.lang.reflect.Method init = Class.forName("net.minecraft.Bootstrap").getDeclaredMethod("initialize");
            init.setAccessible(true);
            init.invoke(null);
        } catch (Throwable ignored) {
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        unsafe = (Unsafe) f.get(null);

        standardForm = (FormData) unsafe.allocateInstance(FormData.class);
        setField(standardForm, "name", "Normal");
        setField(standardForm, "aspects", Collections.emptyList());

        testSpecies = (Species) unsafe.allocateInstance(Species.class);
        setField(testSpecies, "name", "Charizard");
        setField(testSpecies, "resourceIdentifier", net.minecraft.util.Identifier.of("cobblemon", "charizard"));
        setField(testSpecies, "forms", List.of(standardForm));
        setField(testSpecies, "standardForm$delegate", kotlin.LazyKt.lazyOf(standardForm));

        setField(standardForm, "species", testSpecies);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = null;
        Class<?> current = target.getClass();
        while (current != null && field == null) {
            try {
                field = current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        if (field == null) {
            throw new NoSuchFieldException(fieldName + " not found on " + target.getClass());
        }
        field.setAccessible(true);
        field.set(target, value);
    }

    private Pokemon createTestPokemon(boolean isMega, boolean addMegaFeature, String megaFeatureValue) throws Exception {
        Pokemon pokemon = (Pokemon) unsafe.allocateInstance(Pokemon.class);
        setField(pokemon, "uuid", UUID.randomUUID());
        setField(pokemon, "species", testSpecies);
        setField(pokemon, "form", standardForm);
        setField(pokemon, "isClient", false);
        setField(pokemon, "tradeable", !isMega);
        setField(pokemon, "storeCoordinates", new SettableObservable<>(null));

        NbtCompound nbt = new NbtCompound();
        if (isMega) {
            nbt.putBoolean("is_mega", true);
            nbt.putBoolean("form_changing", true);
        }
        setField(pokemon, "persistentData", nbt);

        List<SpeciesFeature> features = new ArrayList<>();
        if (addMegaFeature) {
            features.add(new StringSpeciesFeature("mega_evolution", megaFeatureValue));
        }
        setField(pokemon, "features", features);

        Set<String> aspects = new HashSet<>();
        if (isMega) {
            aspects.add("mega_evolution=" + megaFeatureValue);
        }
        setField(pokemon, "aspects", aspects);
        setField(pokemon, "forcedAspects", new HashSet<String>());

        return pokemon;
    }

    @Test
    @DisplayName("T1: normalizeOutsideMega returns false for null pokemon")
    void testNormalize_nullSafe() {
        assertFalse(OutsideMegaBattleNormalizer.normalizeOutsideMega(null));
    }

    @Test
    @DisplayName("T2: normalizeOutsideMega returns false when is_mega marker is absent")
    void testNormalize_noMarker() throws Exception {
        Pokemon pokemon = createTestPokemon(false, false, "none");
        assertFalse(OutsideMegaBattleNormalizer.normalizeOutsideMega(pokemon));
        assertFalse(pokemon.getPersistentData().contains("is_mega"));
        assertTrue(pokemon.getTradeable());
    }

    @Test
    @DisplayName("T3: fail without mutation when is_mega=true but mega_evolution feature is missing")
    void testNormalize_missingFeature_failsWithoutMutation() throws Exception {
        Pokemon pokemon = createTestPokemon(true, false, "none");
        assertFalse(OutsideMegaBattleNormalizer.normalizeOutsideMega(pokemon));

        // Invariant: Marker and tradeable must remain completely unmutated to preserve diagnostic evidence
        assertTrue(pokemon.getPersistentData().getBoolean("is_mega"), "is_mega must NOT be removed on feature failure");
        assertTrue(pokemon.getPersistentData().getBoolean("form_changing"), "form_changing must NOT be removed on feature failure");
        assertFalse(pokemon.getTradeable(), "tradeable must NOT be set to true on feature failure");
    }

    @Test
    @DisplayName("T4: successful normalization of outside-Mega pokemon")
    void testNormalize_successfulRevert() throws Exception {
        Pokemon pokemon = createTestPokemon(true, true, "mega");
        assertTrue(OutsideMegaBattleNormalizer.normalizeOutsideMega(pokemon));

        // Authoritative marker & transformation flag removed
        assertFalse(pokemon.getPersistentData().contains("is_mega"), "is_mega marker must be removed");
        assertFalse(pokemon.getPersistentData().contains("form_changing"), "form_changing marker must be removed");

        // Tradeable restored
        assertTrue(pokemon.getTradeable(), "tradeable must be restored to true");

        // In-place feature mutated to 'none'
        SpeciesFeature feature = pokemon.getFeature("mega_evolution");
        assertInstanceOf(StringSpeciesFeature.class, feature);
        assertEquals("none", ((StringSpeciesFeature) feature).getValue(), "mega_evolution value must be mutated to none");
    }

    @Test
    @DisplayName("T5: NPC isolation contract - processBattle ignores non-player actors entirely")
    void testProcessBattle_npcIsolation() throws Exception {
        PokemonBattle battle = (PokemonBattle) unsafe.allocateInstance(PokemonBattle.class);
        setField(battle, "battleId", UUID.randomUUID());

        // Create an NPC BattleActor (PokemonBattleActor is concrete, not PlayerBattleActor)
        PokemonBattleActor npcActor = (PokemonBattleActor) unsafe.allocateInstance(PokemonBattleActor.class);
        Pokemon npcPokemon = createTestPokemon(true, true, "mega");
        BattlePokemon battlePokemon = (BattlePokemon) unsafe.allocateInstance(BattlePokemon.class);
        setField(battlePokemon, "effectedPokemon", npcPokemon);
        setField(battlePokemon, "originalPokemon", npcPokemon);
        setField(npcActor, "pokemonList", List.of(battlePokemon));

        setField(battle, "side1", new BattleSide(npcActor));
        setField(battle, "side2", new BattleSide());

        int normalized = OutsideMegaBattleNormalizer.processBattle(battle);
        assertEquals(0, normalized, "Must normalize 0 pokemon for NPC actors");

        // NPC Pokémon must remain untouched
        assertTrue(npcPokemon.getPersistentData().getBoolean("is_mega"), "NPC pokemon is_mega must remain true");
        assertEquals("mega", ((StringSpeciesFeature) npcPokemon.getFeature("mega_evolution")).getValue(),
                "NPC pokemon feature must remain 'mega'");
    }

    @Test
    @DisplayName("T6: Scaled battle dual-instance contract - both effected and original are normalized")
    void testProcessBattle_dualInstanceScaled() throws Exception {
        PokemonBattle battle = (PokemonBattle) unsafe.allocateInstance(PokemonBattle.class);
        setField(battle, "battleId", UUID.randomUUID());

        PlayerBattleActor playerActor = (PlayerBattleActor) unsafe.allocateInstance(PlayerBattleActor.class);
        setField(playerActor, "uuid", UUID.randomUUID());

        Pokemon originalPokemon = createTestPokemon(true, true, "mega");
        Pokemon effectedPokemon = createTestPokemon(true, true, "mega");
        assertNotSame(originalPokemon, effectedPokemon, "Instances must be distinct in scaled battle");

        BattlePokemon battlePokemon = (BattlePokemon) unsafe.allocateInstance(BattlePokemon.class);
        setField(battlePokemon, "effectedPokemon", effectedPokemon);
        setField(battlePokemon, "originalPokemon", originalPokemon);
        setField(playerActor, "pokemonList", List.of(battlePokemon));

        setField(battle, "side1", new BattleSide(playerActor));
        setField(battle, "side2", new BattleSide());

        int normalized = OutsideMegaBattleNormalizer.processBattle(battle);
        assertEquals(1, normalized, "Must normalize 1 roster slot");

        // Check effected clone invariants
        assertFalse(effectedPokemon.getPersistentData().contains("is_mega"), "effected.is_mega must be removed");
        assertTrue(effectedPokemon.getTradeable(), "effected.tradeable must be true");
        assertEquals("none", ((StringSpeciesFeature) effectedPokemon.getFeature("mega_evolution")).getValue(),
                "effected.mega_evolution must be 'none'");

        // Check original persistent invariants
        assertFalse(originalPokemon.getPersistentData().contains("is_mega"), "original.is_mega must be removed");
        assertTrue(originalPokemon.getTradeable(), "original.tradeable must be true");
        assertEquals("none", ((StringSpeciesFeature) originalPokemon.getFeature("mega_evolution")).getValue(),
                "original.mega_evolution must be 'none'");
    }

    @Test
    @DisplayName("T7: Single instance contract - effected == original is only normalized once")
    void testProcessBattle_singleInstance() throws Exception {
        PokemonBattle battle = (PokemonBattle) unsafe.allocateInstance(PokemonBattle.class);
        setField(battle, "battleId", UUID.randomUUID());

        PlayerBattleActor playerActor = (PlayerBattleActor) unsafe.allocateInstance(PlayerBattleActor.class);
        setField(playerActor, "uuid", UUID.randomUUID());

        Pokemon singlePokemon = createTestPokemon(true, true, "mega");
        BattlePokemon battlePokemon = (BattlePokemon) unsafe.allocateInstance(BattlePokemon.class);
        setField(battlePokemon, "effectedPokemon", singlePokemon);
        setField(battlePokemon, "originalPokemon", singlePokemon);
        setField(playerActor, "pokemonList", List.of(battlePokemon));

        setField(battle, "side1", new BattleSide(playerActor));
        setField(battle, "side2", new BattleSide());

        int normalized = OutsideMegaBattleNormalizer.processBattle(battle);
        assertEquals(1, normalized);

        assertFalse(singlePokemon.getPersistentData().contains("is_mega"));
        assertTrue(singlePokemon.getTradeable());
        assertEquals("none", ((StringSpeciesFeature) singlePokemon.getFeature("mega_evolution")).getValue());
    }
}
