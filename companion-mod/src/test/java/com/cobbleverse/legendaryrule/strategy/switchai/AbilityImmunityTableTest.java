package com.cobbleverse.legendaryrule.strategy.switchai;

import com.cobblemon.mod.common.api.types.ElementalTypes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbilityImmunityTableTest {

    @Test
    @DisplayName("Water immunities: Storm Drain, Water Absorb, Dry Skin")
    void testWaterImmunities() {
        assertTrue(AbilityImmunityTable.isImmune("stormdrain", ElementalTypes.WATER));
        assertTrue(AbilityImmunityTable.isImmune("waterabsorb", ElementalTypes.WATER));
        assertTrue(AbilityImmunityTable.isImmune("dryskin", ElementalTypes.WATER));

        // Case insensitivity
        assertTrue(AbilityImmunityTable.isImmune("StormDrain", ElementalTypes.WATER));
        assertTrue(AbilityImmunityTable.isImmune("WATERABSORB", ElementalTypes.WATER));

        // Non-matching types
        assertFalse(AbilityImmunityTable.isImmune("stormdrain", ElementalTypes.FIRE));
        assertFalse(AbilityImmunityTable.isImmune("waterabsorb", ElementalTypes.GRASS));
    }

    @Test
    @DisplayName("Electric immunities: Volt Absorb, Lightning Rod, Motor Drive")
    void testElectricImmunities() {
        assertTrue(AbilityImmunityTable.isImmune("voltabsorb", ElementalTypes.ELECTRIC));
        assertTrue(AbilityImmunityTable.isImmune("lightningrod", ElementalTypes.ELECTRIC));
        assertTrue(AbilityImmunityTable.isImmune("motordrive", ElementalTypes.ELECTRIC));

        assertFalse(AbilityImmunityTable.isImmune("lightningrod", ElementalTypes.GROUND));
    }

    @Test
    @DisplayName("Fire immunities: Flash Fire, Well-Baked Body")
    void testFireImmunities() {
        assertTrue(AbilityImmunityTable.isImmune("flashfire", ElementalTypes.FIRE));
        assertTrue(AbilityImmunityTable.isImmune("wellbakedbody", ElementalTypes.FIRE));

        assertFalse(AbilityImmunityTable.isImmune("flashfire", ElementalTypes.WATER));
    }

    @Test
    @DisplayName("Grass immunities: Sap Sipper")
    void testGrassImmunities() {
        assertTrue(AbilityImmunityTable.isImmune("sapsipper", ElementalTypes.GRASS));
        assertFalse(AbilityImmunityTable.isImmune("sapsipper", ElementalTypes.BUG));
    }

    @Test
    @DisplayName("Ground immunities: Levitate, Earth Eater")
    void testGroundImmunities() {
        assertTrue(AbilityImmunityTable.isImmune("levitate", ElementalTypes.GROUND));
        assertTrue(AbilityImmunityTable.isImmune("eartheater", ElementalTypes.GROUND));

        assertFalse(AbilityImmunityTable.isImmune("levitate", ElementalTypes.FLYING));
    }

    @Test
    @DisplayName("Null or unrecognized abilities return false")
    void testNullOrUnrecognized() {
        assertFalse(AbilityImmunityTable.isImmune(null, ElementalTypes.WATER));
        assertFalse(AbilityImmunityTable.isImmune("stormdrain", null));
        assertFalse(AbilityImmunityTable.isImmune(null, null));
        assertFalse(AbilityImmunityTable.isImmune("intimidate", ElementalTypes.WATER));
        assertFalse(AbilityImmunityTable.isImmune("hugepower", ElementalTypes.NORMAL));
    }
}
