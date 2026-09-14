package com.cobbleverse.legendaryrule.mechanic;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SideMechanicTrackerTest {

    private UUID battleId;

    @BeforeEach
    void setUp() {
        battleId = UUID.randomUUID();
        BattleSideMechanicTracker.clearBattle(battleId);
    }

    @AfterEach
    void tearDown() {
        BattleSideMechanicTracker.clearBattle(battleId);
    }

    @Test
    @DisplayName("Initially both sides are unlocked and return null mechanic")
    void testInitialState() {
        assertFalse(BattleSideMechanicTracker.isMechanicLocked(battleId, 0));
        assertFalse(BattleSideMechanicTracker.isMechanicLocked(battleId, 1));
        assertNull(BattleSideMechanicTracker.getUsedMechanic(battleId, 0));
        assertNull(BattleSideMechanicTracker.getUsedMechanic(battleId, 1));
    }

    @Test
    @DisplayName("Locking side 0 isolates side 1")
    void testSideIsolation() {
        BattleSideMechanicTracker.commitMechanic(battleId, 0, MajorBattleMechanic.MEGA);

        assertTrue(BattleSideMechanicTracker.isMechanicLocked(battleId, 0));
        assertEquals(MajorBattleMechanic.MEGA, BattleSideMechanicTracker.getUsedMechanic(battleId, 0));

        assertFalse(BattleSideMechanicTracker.isMechanicLocked(battleId, 1));
        assertNull(BattleSideMechanicTracker.getUsedMechanic(battleId, 1));
    }

    @Test
    @DisplayName("Write-once & idempotency: same mechanic is no-op, conflicting mechanic cannot overwrite")
    void testWriteOnceAndIdempotency() {
        BattleSideMechanicTracker.commitMechanic(battleId, 0, MajorBattleMechanic.MEGA);
        assertEquals(MajorBattleMechanic.MEGA, BattleSideMechanicTracker.getUsedMechanic(battleId, 0));

        // Idempotent duplicate commit of same mechanic
        BattleSideMechanicTracker.commitMechanic(battleId, 0, MajorBattleMechanic.MEGA);
        assertEquals(MajorBattleMechanic.MEGA, BattleSideMechanicTracker.getUsedMechanic(battleId, 0));

        // Attempted overwrite by different mechanic (e.g. TERA) is ignored
        BattleSideMechanicTracker.commitMechanic(battleId, 0, MajorBattleMechanic.TERA);
        assertEquals(MajorBattleMechanic.MEGA, BattleSideMechanicTracker.getUsedMechanic(battleId, 0));

        // Attempted overwrite by DYNAMAX is ignored
        BattleSideMechanicTracker.commitMechanic(battleId, 0, MajorBattleMechanic.DYNAMAX);
        assertEquals(MajorBattleMechanic.MEGA, BattleSideMechanicTracker.getUsedMechanic(battleId, 0));
    }

    @Test
    @DisplayName("clearBattle cleans all side tracking state for that battleId")
    void testClearBattle() {
        BattleSideMechanicTracker.commitMechanic(battleId, 0, MajorBattleMechanic.MEGA);
        BattleSideMechanicTracker.commitMechanic(battleId, 1, MajorBattleMechanic.TERA);

        assertTrue(BattleSideMechanicTracker.isMechanicLocked(battleId, 0));
        assertTrue(BattleSideMechanicTracker.isMechanicLocked(battleId, 1));

        BattleSideMechanicTracker.clearBattle(battleId);

        assertFalse(BattleSideMechanicTracker.isMechanicLocked(battleId, 0));
        assertFalse(BattleSideMechanicTracker.isMechanicLocked(battleId, 1));
        assertNull(BattleSideMechanicTracker.getUsedMechanic(battleId, 0));
        assertNull(BattleSideMechanicTracker.getUsedMechanic(battleId, 1));
    }

    @Test
    @DisplayName("Invalid side indices and null battleId fail safely")
    void testInvalidIndices() {
        assertFalse(BattleSideMechanicTracker.isMechanicLocked(battleId, -1));
        assertFalse(BattleSideMechanicTracker.isMechanicLocked(battleId, 2));
        assertFalse(BattleSideMechanicTracker.isMechanicLocked(null, 0));

        BattleSideMechanicTracker.commitMechanic(battleId, -1, MajorBattleMechanic.MEGA);
        BattleSideMechanicTracker.commitMechanic(battleId, 2, MajorBattleMechanic.MEGA);
        BattleSideMechanicTracker.commitMechanic(null, 0, MajorBattleMechanic.MEGA);

        assertFalse(BattleSideMechanicTracker.isMechanicLocked(battleId, 0));
    }
}
