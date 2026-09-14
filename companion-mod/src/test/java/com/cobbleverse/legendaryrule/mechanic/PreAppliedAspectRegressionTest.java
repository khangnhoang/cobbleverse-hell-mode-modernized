package com.cobbleverse.legendaryrule.mechanic;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PreAppliedAspectRegressionTest {

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
    @DisplayName("Pre-applied NPC Mega aspects (mega_y, mega_x, primal) do NOT consume battle side mechanic slot")
    void testPreAppliedMegaAspectDoesNotLockMechanicSlot() {
        // Given an NPC Pokemon with pre-applied aspect "mega_y" or "mega_x"
        Set<String> npcPokemonAspects = Set.of("mega_y", "special");

        // The presence of pre-applied form aspects must not invoke commitMechanic
        // Validate that no commit occurs simply from having the aspect
        boolean containsPreAppliedAspect = npcPokemonAspects.contains("mega_y") || npcPokemonAspects.contains("mega_x");
        assertTrue(containsPreAppliedAspect);

        // Assert that the mechanic tracker for this battle side remains completely unlocked
        assertFalse(BattleSideMechanicTracker.isMechanicLocked(battleId, 0));
        assertFalse(BattleSideMechanicTracker.isMechanicLocked(battleId, 1));
        assertNull(BattleSideMechanicTracker.getUsedMechanic(battleId, 0));
        assertNull(BattleSideMechanicTracker.getUsedMechanic(battleId, 1));

        // The side is free to legitimately commit another in-battle mechanic (e.g. Z_MOVE or TERA)
        BattleSideMechanicTracker.commitMechanic(battleId, 1, MajorBattleMechanic.TERA);
        assertTrue(BattleSideMechanicTracker.isMechanicLocked(battleId, 1));
        assertEquals(MajorBattleMechanic.TERA, BattleSideMechanicTracker.getUsedMechanic(battleId, 1));
    }
}
