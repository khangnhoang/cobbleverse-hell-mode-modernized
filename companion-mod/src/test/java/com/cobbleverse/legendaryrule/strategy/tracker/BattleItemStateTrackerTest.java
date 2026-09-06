package com.cobbleverse.legendaryrule.strategy.tracker;

import com.cobblemon.mod.common.api.battles.interpreter.BattleMessage;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class BattleItemStateTrackerTest {

    private static Unsafe getUnsafe() throws Exception {
        Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        return (Unsafe) f.get(null);
    }

    static class TestTrackedBattlePokemon extends BattlePokemon implements BattleItemStateTracker {
        private boolean ended = false;

        @SuppressWarnings("unused")
        public TestTrackedBattlePokemon() {
            super(null, null, (kotlin.jvm.functions.Function1) null);
        }

        @Override
        public boolean cobbleverse$isThroatSprayEnded() {
            return ended;
        }

        @Override
        public void cobbleverse$markThroatSprayEnded() {
            ended = true;
        }
    }

    static class UntrackedBattlePokemon extends BattlePokemon {
        @SuppressWarnings("unused")
        public UntrackedBattlePokemon() {
            super(null, null, (kotlin.jvm.functions.Function1) null);
        }
    }

    private TestTrackedBattlePokemon createTrackedPokemon() throws Exception {
        return (TestTrackedBattlePokemon) getUnsafe().allocateInstance(TestTrackedBattlePokemon.class);
    }

    private UntrackedBattlePokemon createUntrackedPokemon() throws Exception {
        return (UntrackedBattlePokemon) getUnsafe().allocateInstance(UntrackedBattlePokemon.class);
    }

    @Test
    @DisplayName("T18a: Default state is unended")
    void testDefaultStateUnended() throws Exception {
        TestTrackedBattlePokemon pokemon = createTrackedPokemon();
        assertFalse(pokemon.cobbleverse$isThroatSprayEnded());
    }

    @Test
    @DisplayName("T18b: Manual mark sets ended state")
    void testMarkEnded() throws Exception {
        TestTrackedBattlePokemon pokemon = createTrackedPokemon();
        pokemon.cobbleverse$markThroatSprayEnded();
        assertTrue(pokemon.cobbleverse$isThroatSprayEnded());
    }

    @Test
    @DisplayName("T18c: Re-entry on same instance preserves ended state")
    void testReEntryPreservesEndedState() throws Exception {
        TestTrackedBattlePokemon pokemon = createTrackedPokemon();
        pokemon.cobbleverse$markThroatSprayEnded();
        assertTrue(pokemon.cobbleverse$isThroatSprayEnded());
        // Simulating switch-out and back in
        assertTrue(pokemon.cobbleverse$isThroatSprayEnded());
    }

    @Test
    @DisplayName("T18d: Distinct instances are isolated (no cross-battle leakage)")
    void testDistinctInstancesIsolated() throws Exception {
        TestTrackedBattlePokemon battle1 = createTrackedPokemon();
        battle1.cobbleverse$markThroatSprayEnded();
        assertTrue(battle1.cobbleverse$isThroatSprayEnded());

        TestTrackedBattlePokemon battle2 = createTrackedPokemon();
        assertFalse(battle2.cobbleverse$isThroatSprayEnded());
    }

    @Test
    @DisplayName("T18e: Null safety on helper")
    void testNullSafety() {
        assertFalse(BattleItemStateTracker.markEndedIfThroatSpray(null, null));
        assertFalse(BattleItemStateTracker.markEndedIfThroatSpray(null, new BattleMessage("|-enditem|p2a: Toxtricity|Throat Spray")));
    }

    @Test
    @DisplayName("T19a: EndItem Throat Spray marks target BattlePokemon tracker as ended")
    void testEndItemThroatSprayMarksTarget() throws Exception {
        TestTrackedBattlePokemon toxtricity = createTrackedPokemon();
        BattleMessage msg = new BattleMessage("|-enditem|p2a: Toxtricity|Throat Spray|[from] move: Overdrive");

        boolean marked = BattleItemStateTracker.markEndedIfThroatSpray(toxtricity, msg);

        assertTrue(marked, "Helper should return true for Throat Spray enditem");
        assertTrue(toxtricity.cobbleverse$isThroatSprayEnded(), "Toxtricity tracker must be marked ended");
    }

    @Test
    @DisplayName("T19b: EndItem other item does NOT mark Throat Spray ended")
    void testEndItemOtherItemDoesNotMark() throws Exception {
        TestTrackedBattlePokemon toxtricity = createTrackedPokemon();
        BattleMessage msg = new BattleMessage("|-enditem|p2a: Toxtricity|Leftovers");

        boolean marked = BattleItemStateTracker.markEndedIfThroatSpray(toxtricity, msg);

        assertFalse(marked, "Helper should return false for non-Throat-Spray item");
        assertFalse(toxtricity.cobbleverse$isThroatSprayEnded(), "Toxtricity tracker must remain unended");
    }

    @Test
    @DisplayName("T19c: Event for Pokémon A does NOT affect Pokémon B tracker")
    void testPokemonIsolation() throws Exception {
        TestTrackedBattlePokemon toxtricityA = createTrackedPokemon();
        TestTrackedBattlePokemon toxtricityB = createTrackedPokemon();
        BattleMessage msg = new BattleMessage("|-enditem|p2a: Toxtricity|Throat Spray");

        boolean marked = BattleItemStateTracker.markEndedIfThroatSpray(toxtricityA, msg);

        assertTrue(marked);
        assertTrue(toxtricityA.cobbleverse$isThroatSprayEnded(), "Target A must be marked ended");
        assertFalse(toxtricityB.cobbleverse$isThroatSprayEnded(), "Pokemon B must remain unended");
    }

    @Test
    @DisplayName("T19d: Untracked BattlePokemon gracefully returns false")
    void testUntrackedPokemonGraceful() throws Exception {
        UntrackedBattlePokemon pokemon = createUntrackedPokemon();
        BattleMessage msg = new BattleMessage("|-enditem|p2a: Toxtricity|Throat Spray");

        boolean marked = BattleItemStateTracker.markEndedIfThroatSpray(pokemon, msg);

        assertFalse(marked, "Untracked pokemon should return false gracefully");
    }
}
