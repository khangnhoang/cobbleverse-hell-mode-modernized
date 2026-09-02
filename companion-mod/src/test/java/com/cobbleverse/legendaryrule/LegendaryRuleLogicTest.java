package com.cobbleverse.legendaryrule;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class LegendaryRuleLogicTest {

    static class MockPokemon {
        final boolean isLegendary;
        final boolean isMythical;

        MockPokemon(boolean isLegendary, boolean isMythical) {
            this.isLegendary = isLegendary;
            this.isMythical = isMythical;
        }

        boolean isRestricted() {
            return isLegendary || isMythical;
        }
    }

    private static int countRestricted(List<MockPokemon> party) {
        if (party == null) return 0;
        int count = 0;
        for (MockPokemon p : party) {
            if (p != null && p.isRestricted()) {
                count++;
            }
        }
        return count;
    }

    private static boolean isPartyAllowed(List<MockPokemon> party, int configuredLimit) {
        return countRestricted(party) <= configuredLimit;
    }

    private static String getExpectedMessage(int limit) {
        if (limit == 0) {
            return "§cTrainer rules permit no Legendary or Mythical Pokémon!";
        } else {
            return "§cTrainer rules permit at most " + limit + " Legendary or Mythical Pokémon!";
        }
    }

    @Test
    public void testConfiguredLimit0() {
        int limit = 0;
        assertEquals("§cTrainer rules permit no Legendary or Mythical Pokémon!", getExpectedMessage(limit));

        List<MockPokemon> zeroRestricted = Arrays.asList(
            new MockPokemon(false, false),
            new MockPokemon(false, false)
        );
        assertTrue(isPartyAllowed(zeroRestricted, limit));

        List<MockPokemon> oneRestricted = Arrays.asList(
            new MockPokemon(true, false),
            new MockPokemon(false, false)
        );
        assertFalse(isPartyAllowed(oneRestricted, limit));
    }

    @Test
    public void testConfiguredLimit1Default() {
        int limit = 1;
        assertEquals("§cTrainer rules permit at most 1 Legendary or Mythical Pokémon!", getExpectedMessage(limit));

        List<MockPokemon> zeroRestricted = Arrays.asList(
            new MockPokemon(false, false),
            new MockPokemon(false, false)
        );
        assertTrue(isPartyAllowed(zeroRestricted, limit));

        List<MockPokemon> oneLegendary = Arrays.asList(
            new MockPokemon(true, false),
            new MockPokemon(false, false)
        );
        assertTrue(isPartyAllowed(oneLegendary, limit));

        List<MockPokemon> oneMythical = Arrays.asList(
            new MockPokemon(false, true),
            new MockPokemon(false, false)
        );
        assertTrue(isPartyAllowed(oneMythical, limit));

        List<MockPokemon> twoRestricted = Arrays.asList(
            new MockPokemon(true, false),
            new MockPokemon(false, true)
        );
        assertFalse(isPartyAllowed(twoRestricted, limit));
    }

    @Test
    public void testConfiguredLimit2() {
        int limit = 2;
        assertEquals("§cTrainer rules permit at most 2 Legendary or Mythical Pokémon!", getExpectedMessage(limit));

        List<MockPokemon> twoRestricted = Arrays.asList(
            new MockPokemon(true, false),
            new MockPokemon(true, false)
        );
        assertTrue(isPartyAllowed(twoRestricted, limit));

        List<MockPokemon> threeRestricted = Arrays.asList(
            new MockPokemon(true, false),
            new MockPokemon(false, true),
            new MockPokemon(true, false)
        );
        assertFalse(isPartyAllowed(threeRestricted, limit));
    }

    @Test
    public void testDualFlagCountedOnce() {
        // Pokemon flagged as both legendary and mythical
        MockPokemon dual = new MockPokemon(true, true);
        List<MockPokemon> party = Arrays.asList(dual, new MockPokemon(false, false));
        assertEquals(1, countRestricted(party));
        assertTrue(isPartyAllowed(party, 1));
    }

    @Test
    public void testNullSlotSafe() {
        List<MockPokemon> party = Arrays.asList(
            new MockPokemon(true, false),
            null,
            new MockPokemon(false, false)
        );
        assertEquals(1, countRestricted(party));
        assertTrue(isPartyAllowed(party, 1));
    }
}
