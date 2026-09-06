package com.cobbleverse.legendaryrule.strategy.tera;

import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.pokemon.Pokemon;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TeraTargetResolverTest {

    private static Unsafe unsafe;
    private static Field currentHealthField;
    private static Field effectedPokemonField;

    public static class StubPokemon extends Pokemon {
        private String stubShowdownId;

        @Override
        public String showdownId() {
            return stubShowdownId;
        }
    }

    @BeforeAll
    static void setUpAll() throws Exception {
        Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        unsafe = (Unsafe) f.get(null);

        currentHealthField = Pokemon.class.getDeclaredField("currentHealth");
        currentHealthField.setAccessible(true);

        effectedPokemonField = BattlePokemon.class.getDeclaredField("effectedPokemon");
        effectedPokemonField.setAccessible(true);
    }

    private BattlePokemon createBattlePokemon(String showdownId, int health) throws Exception {
        StubPokemon pokemon = (StubPokemon) unsafe.allocateInstance(StubPokemon.class);
        pokemon.stubShowdownId = showdownId;
        currentHealthField.set(pokemon, health);

        BattlePokemon bp = (BattlePokemon) unsafe.allocateInstance(BattlePokemon.class);
        effectedPokemonField.set(bp, pokemon);

        return bp;
    }

    @Test
    @DisplayName("Scenario 1: Target active + non-target choose => non-target cannot Tera")
    void testTargetActiveNonTargetChoose() throws Exception {
        // In Doubles, Toxtricity is active alongside Hisuian Electrode.
        // Even though Toxtricity cannot be sent out again (canBeSentOut == false),
        // it is present and alive in the actor's party.
        BattlePokemon toxtricity = createBattlePokemon("toxtricitylowkey", 100);
        BattlePokemon electrode = createBattlePokemon("electrodehisuian", 80);

        List<BattlePokemon> party = List.of(electrode, toxtricity);

        BattlePokemon resolved = TeraTargetResolver.resolveAliveTeraTarget(party, "toxtricitylowkey");

        // Resolved must NOT be null, so RunBunAI sees teraMatch != null and does NOT grant fallback Tera to Electrode.
        assertNotNull(resolved, "Active alive target must be found");
        assertSame(toxtricity, resolved);
        assertTrue(TeraTargetResolver.isAlive(resolved));
        assertEquals(100, resolved.getHealth());
        assertFalse(resolved.getEffectedPokemon().isFainted());
    }

    @Test
    @DisplayName("Scenario 2: Target benched/alive + non-target choose => non-target cannot Tera")
    void testTargetBenchedAliveNonTargetChoose() throws Exception {
        // Toxtricity is waiting on the bench, healthy and alive.
        BattlePokemon toxtricity = createBattlePokemon("toxtricitylowkey", 100);
        BattlePokemon electrode = createBattlePokemon("electrodehisuian", 80);
        BattlePokemon zapdos = createBattlePokemon("zapdos", 120);

        List<BattlePokemon> party = List.of(electrode, zapdos, toxtricity);

        BattlePokemon resolved = TeraTargetResolver.resolveAliveTeraTarget(party, "toxtricitylowkey");

        assertNotNull(resolved, "Benched alive target must be found");
        assertSame(toxtricity, resolved);
        assertTrue(TeraTargetResolver.isAlive(resolved));
    }

    @Test
    @DisplayName("Scenario 3: Target itself choose => target can Tera")
    void testTargetItselfChoose() throws Exception {
        // Toxtricity itself is choosing.
        BattlePokemon toxtricity = createBattlePokemon("toxtricitylowkey", 100);
        BattlePokemon electrode = createBattlePokemon("electrodehisuian", 80);

        List<BattlePokemon> party = List.of(toxtricity, electrode);

        // In RunBunAI.choose(), target itself checks:
        // this.teraTarget.equalsIgnoreCase(activeBattlePokemon.getBattlePokemon().getEffectedPokemon().showdownId())
        String teraTarget = "toxtricitylowkey";
        assertTrue(teraTarget.equalsIgnoreCase(toxtricity.getEffectedPokemon().showdownId()),
                "Target's own showdownId matches teraTarget directly");

        // The resolver also finds it as alive
        BattlePokemon resolved = TeraTargetResolver.resolveAliveTeraTarget(party, teraTarget);
        assertNotNull(resolved);
        assertSame(toxtricity, resolved);
    }

    @Test
    @DisplayName("Scenario 4: Target fainted => preserve native fallback behavior")
    void testTargetFaintedPreservesFallbackBehavior() throws Exception {
        // Toxtricity was KO'd (0 HP / fainted).
        BattlePokemon toxtricityFainted = createBattlePokemon("toxtricitylowkey", 0);
        BattlePokemon electrode = createBattlePokemon("electrodehisuian", 80);

        List<BattlePokemon> party = List.of(electrode, toxtricityFainted);

        BattlePokemon resolved = TeraTargetResolver.resolveAliveTeraTarget(party, "toxtricitylowkey");

        // Must be null so RunBunAI sees teraMatch == null, allowing fallback Tera
        assertNull(resolved, "Fainted target must return null so native fallback Tera is available");
        assertFalse(TeraTargetResolver.isAlive(toxtricityFainted));
        assertEquals(0, toxtricityFainted.getHealth());
        assertTrue(toxtricityFainted.getEffectedPokemon().isFainted());
    }

    @Test
    @DisplayName("Edge cases: null or empty inputs, case-insensitivity")
    void testEdgeCases() throws Exception {
        BattlePokemon toxtricity = createBattlePokemon("toxtricitylowkey", 100);

        // Case insensitivity
        BattlePokemon resolvedMixedCase = TeraTargetResolver.resolveAliveTeraTarget(
                List.of(toxtricity), "ToXtRiCiTyLoWkEy");
        assertNotNull(resolvedMixedCase);
        assertSame(toxtricity, resolvedMixedCase);

        // Null and empty inputs
        assertNull(TeraTargetResolver.resolveAliveTeraTarget(null, "toxtricitylowkey"));
        assertNull(TeraTargetResolver.resolveAliveTeraTarget(Collections.emptyList(), "toxtricitylowkey"));
        assertNull(TeraTargetResolver.resolveAliveTeraTarget(List.of(toxtricity), null));
        assertNull(TeraTargetResolver.resolveAliveTeraTarget(List.of(toxtricity), ""));

        // List containing null entries
        List<BattlePokemon> partyWithNulls = new ArrayList<>();
        partyWithNulls.add(null);
        partyWithNulls.add(toxtricity);
        assertSame(toxtricity, TeraTargetResolver.resolveAliveTeraTarget(partyWithNulls, "toxtricitylowkey"));

        // isAlive null check
        assertFalse(TeraTargetResolver.isAlive(null));
    }
}
