package com.cobbleverse.legendaryrule;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class LegendaryRuleLogicTest {

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

    static class MockPokemon {
        final boolean isLegendary;
        final boolean isMythical;
        final String species;
        final String heldItemId;

        MockPokemon(boolean isLegendary, boolean isMythical) {
            this(isLegendary, isMythical, null, null);
        }

        MockPokemon(boolean isLegendary, boolean isMythical, String species, String heldItemId) {
            this.isLegendary = isLegendary;
            this.isMythical = isMythical;
            this.species = species;
            this.heldItemId = heldItemId;
        }

        boolean isRestricted() {
            return isLegendary || isMythical;
        }

        boolean hasUltraNecrozmaSurcharge() {
            return "necrozma".equalsIgnoreCase(species)
                    && "mega_showdown:ultranecrozium_z".equalsIgnoreCase(heldItemId);
        }

        int getRestrictedSlots() {
            int slots = 0;
            if (isRestricted()) {
                slots++;
            }
            if (hasUltraNecrozmaSurcharge()) {
                slots++;
            }
            return slots;
        }
    }

    private static int countRestricted(List<MockPokemon> party) {
        if (party == null) return 0;
        int count = 0;
        for (MockPokemon p : party) {
            if (p != null) {
                count += p.getRestrictedSlots();
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

    @Test
    public void testOrdinaryNecrozmaConsumesOneSlot() {
        MockPokemon ordinaryNecrozma = new MockPokemon(true, false, "necrozma", null);
        List<MockPokemon> party = Arrays.asList(ordinaryNecrozma, new MockPokemon(false, false));
        assertEquals(1, countRestricted(party));
    }

    @Test
    public void testUltraNecrozmaLoadoutConsumesTwoSlots() {
        MockPokemon ultraNecrozma = new MockPokemon(true, false, "necrozma", "mega_showdown:ultranecrozium_z");
        List<MockPokemon> party = Arrays.asList(ultraNecrozma, new MockPokemon(false, false));
        assertEquals(2, countRestricted(party));
    }

    @Test
    public void testNecrozmaWithOtherHeldItemConsumesOneSlot() {
        MockPokemon leftoversNecrozma = new MockPokemon(true, false, "necrozma", "minecraft:leftovers");
        List<MockPokemon> party = Arrays.asList(leftoversNecrozma, new MockPokemon(false, false));
        assertEquals(1, countRestricted(party));
    }

    @Test
    public void testNonNecrozmaWithUltraNecroziumZNoSurcharge() {
        // Non-legendary with Ultranecrozium-Z has 0 restricted slots
        MockPokemon pikachu = new MockPokemon(false, false, "pikachu", "mega_showdown:ultranecrozium_z");
        assertEquals(0, countRestricted(List.of(pikachu)));

        // Ordinary Legendary with Ultranecrozium-Z gets standard +1 but NO surcharge (+0)
        MockPokemon mewtwo = new MockPokemon(true, false, "mewtwo", "mega_showdown:ultranecrozium_z");
        assertEquals(1, countRestricted(List.of(mewtwo)));
    }

    @Test
    public void testOrdinaryLegendaryPlusUltraNecrozmaLoadout() {
        MockPokemon ultraNecrozma = new MockPokemon(true, false, "necrozma", "mega_showdown:ultranecrozium_z");
        MockPokemon rayquaza = new MockPokemon(true, false, "rayquaza", null);
        List<MockPokemon> party = Arrays.asList(ultraNecrozma, rayquaza, new MockPokemon(false, false));
        // 2 from ultraNecrozma + 1 from rayquaza = 3
        assertEquals(3, countRestricted(party));
    }

    @Test
    public void testUltraNecrozmaMaxLimitBehavior() {
        MockPokemon ordinaryNecrozma = new MockPokemon(true, false, "necrozma", null);
        MockPokemon ultraNecrozma = new MockPokemon(true, false, "necrozma", "mega_showdown:ultranecrozium_z");
        MockPokemon ordinaryLegendary = new MockPokemon(true, false, "mewtwo", null);
        MockPokemon ordinaryLegendary2 = new MockPokemon(true, false, "rayquaza", null);

        // max = 1
        // ordinary Necrozma -> allowed
        assertTrue(isPartyAllowed(List.of(ordinaryNecrozma), 1));
        // Necrozma + Ultranecrozium-Z -> blocked (consumes 2)
        assertFalse(isPartyAllowed(List.of(ultraNecrozma), 1));

        // max = 2
        // Necrozma + Ultranecrozium-Z -> allowed, consumes entire restricted budget
        assertTrue(isPartyAllowed(List.of(ultraNecrozma), 2));
        // Ultra-Necrozma loadout + another Legendary -> blocked (2 + 1 = 3 > 2)
        assertFalse(isPartyAllowed(Arrays.asList(ultraNecrozma, ordinaryLegendary), 2));

        // max = 3
        // Ultra-Necrozma loadout + one ordinary Legendary -> allowed (2 + 1 = 3 <= 3)
        assertTrue(isPartyAllowed(Arrays.asList(ultraNecrozma, ordinaryLegendary), 3));
        // Ultra-Necrozma loadout + two ordinary Legendaries -> blocked (2 + 1 + 1 = 4 > 3)
        assertFalse(isPartyAllowed(Arrays.asList(ultraNecrozma, ordinaryLegendary, ordinaryLegendary2), 3));
    }

    @Test
    public void testLegendaryPartyRuleNullSafety() {
        assertEquals(0, LegendaryPartyRule.getRestrictedSlots(null));
        assertFalse(LegendaryPartyRule.isRestricted(null));
        assertFalse(LegendaryPartyRule.isNecrozma(null));
        assertFalse(LegendaryPartyRule.isHoldingUltraNecroziumZ(null));
        assertFalse(LegendaryPartyRule.hasUltraNecrozmaSurcharge(null));
        assertEquals(0, LegendaryPartyRule.getRestrictedCount((Iterable<com.cobblemon.mod.common.pokemon.Pokemon>) null));
        assertTrue(LegendaryPartyRule.isAllowed((Iterable<com.cobblemon.mod.common.pokemon.Pokemon>) null));
    }

    @Test
    public void testLegendaryPartyRuleConstants() {
        assertEquals("mega_showdown", LegendaryPartyRule.ULTRA_NECROZIUM_Z_ID.getNamespace());
        assertEquals("ultranecrozium_z", LegendaryPartyRule.ULTRA_NECROZIUM_Z_ID.getPath());
        assertEquals("necrozma", LegendaryPartyRule.NECROZMA_SPECIES);
    }

    @Test
    public void testLegendaryPartyRuleSpeciesDetection() throws Exception {
        Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        Unsafe unsafe = (Unsafe) f.get(null);

        com.cobblemon.mod.common.pokemon.Pokemon necrozmaPokemon =
                (com.cobblemon.mod.common.pokemon.Pokemon) unsafe.allocateInstance(com.cobblemon.mod.common.pokemon.Pokemon.class);
        com.cobblemon.mod.common.pokemon.Species necrozmaSpecies =
                (com.cobblemon.mod.common.pokemon.Species) unsafe.allocateInstance(com.cobblemon.mod.common.pokemon.Species.class);
        setField(com.cobblemon.mod.common.pokemon.Species.class, "name", necrozmaSpecies, "Necrozma");
        setField(com.cobblemon.mod.common.pokemon.Species.class, "resourceIdentifier", necrozmaSpecies,
                net.minecraft.util.Identifier.of("cobblemon", "necrozma"));
        setField(com.cobblemon.mod.common.pokemon.Pokemon.class, "species", necrozmaPokemon, necrozmaSpecies);

        assertTrue(LegendaryPartyRule.isNecrozma(necrozmaPokemon));

        com.cobblemon.mod.common.pokemon.Pokemon otherPokemon =
                (com.cobblemon.mod.common.pokemon.Pokemon) unsafe.allocateInstance(com.cobblemon.mod.common.pokemon.Pokemon.class);
        com.cobblemon.mod.common.pokemon.Species otherSpecies =
                (com.cobblemon.mod.common.pokemon.Species) unsafe.allocateInstance(com.cobblemon.mod.common.pokemon.Species.class);
        setField(com.cobblemon.mod.common.pokemon.Species.class, "name", otherSpecies, "Charizard");
        setField(com.cobblemon.mod.common.pokemon.Species.class, "resourceIdentifier", otherSpecies,
                net.minecraft.util.Identifier.of("cobblemon", "charizard"));
        setField(com.cobblemon.mod.common.pokemon.Pokemon.class, "species", otherPokemon, otherSpecies);

        assertFalse(LegendaryPartyRule.isNecrozma(otherPokemon));
    }

    @Test
    public void testLegendaryPartyRuleHeldItemDetection() throws Exception {
        Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        Unsafe unsafe = (Unsafe) f.get(null);

        com.cobblemon.mod.common.pokemon.Pokemon pokemon =
                (com.cobblemon.mod.common.pokemon.Pokemon) unsafe.allocateInstance(com.cobblemon.mod.common.pokemon.Pokemon.class);
        com.cobblemon.mod.common.pokemon.Species necrozmaSpecies =
                (com.cobblemon.mod.common.pokemon.Species) unsafe.allocateInstance(com.cobblemon.mod.common.pokemon.Species.class);
        setField(com.cobblemon.mod.common.pokemon.Species.class, "name", necrozmaSpecies, "Necrozma");
        setField(com.cobblemon.mod.common.pokemon.Species.class, "resourceIdentifier", necrozmaSpecies,
                net.minecraft.util.Identifier.of("cobblemon", "necrozma"));
        setField(com.cobblemon.mod.common.pokemon.Pokemon.class, "species", pokemon, necrozmaSpecies);

        // Empty held item -> no surcharge
        setField(com.cobblemon.mod.common.pokemon.Pokemon.class, "heldItem", pokemon, net.minecraft.item.ItemStack.EMPTY);
        assertFalse(LegendaryPartyRule.isHoldingUltraNecroziumZ(pokemon));
        assertFalse(LegendaryPartyRule.hasUltraNecrozmaSurcharge(pokemon));

        // Null held item -> no surcharge
        setField(com.cobblemon.mod.common.pokemon.Pokemon.class, "heldItem", pokemon, null);
        assertFalse(LegendaryPartyRule.isHoldingUltraNecroziumZ(pokemon));
        assertFalse(LegendaryPartyRule.hasUltraNecrozmaSurcharge(pokemon));
    }

    private static void setField(Class<?> clazz, String fieldName, Object target, Object value) throws Exception {
        Field field = null;
        Class<?> current = clazz;
        while (current != null && field == null) {
            try {
                field = current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        if (field == null) {
            throw new NoSuchFieldException(fieldName + " on " + clazz);
        }
        field.setAccessible(true);
        field.set(target, value);
    }
}
