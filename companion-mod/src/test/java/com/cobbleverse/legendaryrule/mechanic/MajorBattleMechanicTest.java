package com.cobbleverse.legendaryrule.mechanic;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MajorBattleMechanicTest {

    @Test
    @DisplayName("MajorBattleMechanic parses all 5 mechanics across all aliases")
    void testParsingAllMechanics() {
        assertEquals(MajorBattleMechanic.MEGA, MajorBattleMechanic.fromGimmickId("mega"));
        assertEquals(MajorBattleMechanic.MEGA, MajorBattleMechanic.fromGimmickId("MEGA"));
        assertEquals(MajorBattleMechanic.MEGA, MajorBattleMechanic.fromGimmickId("  mega  "));

        assertEquals(MajorBattleMechanic.TERA, MajorBattleMechanic.fromGimmickId("terastallize"));
        assertEquals(MajorBattleMechanic.TERA, MajorBattleMechanic.fromGimmickId("terastal"));
        assertEquals(MajorBattleMechanic.TERA, MajorBattleMechanic.fromGimmickId("TERASTALLIZE"));

        assertEquals(MajorBattleMechanic.DYNAMAX, MajorBattleMechanic.fromGimmickId("dynamax"));
        assertEquals(MajorBattleMechanic.DYNAMAX, MajorBattleMechanic.fromGimmickId("DYNAMAX"));

        assertEquals(MajorBattleMechanic.Z_MOVE, MajorBattleMechanic.fromGimmickId("z-power"));
        assertEquals(MajorBattleMechanic.Z_MOVE, MajorBattleMechanic.fromGimmickId("zmove"));
        assertEquals(MajorBattleMechanic.Z_MOVE, MajorBattleMechanic.fromGimmickId("Z-POWER"));

        assertEquals(MajorBattleMechanic.ULTRA_BURST, MajorBattleMechanic.fromGimmickId("ultra"));
        assertEquals(MajorBattleMechanic.ULTRA_BURST, MajorBattleMechanic.fromGimmickId("ULTRA"));
    }

    @Test
    @DisplayName("MajorBattleMechanic returns null on unknown or null inputs")
    void testParsingInvalid() {
        assertNull(MajorBattleMechanic.fromGimmickId(null));
        assertNull(MajorBattleMechanic.fromGimmickId(""));
        assertNull(MajorBattleMechanic.fromGimmickId("   "));
        assertNull(MajorBattleMechanic.fromGimmickId("unknown_gimmick"));
    }
}
