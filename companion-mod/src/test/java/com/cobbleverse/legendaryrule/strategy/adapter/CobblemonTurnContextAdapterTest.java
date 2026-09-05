package com.cobbleverse.legendaryrule.strategy.adapter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class CobblemonTurnContextAdapterTest {

    private final CobblemonTurnContextAdapter adapter = CobblemonTurnContextAdapter.INSTANCE;

    @Test
    @DisplayName("T17a: Ground typing sets confirmedImmune = true")
    void testGroundTypingIsImmune() {
        assertTrue(adapter.isImmune(List.of("ground"), null));
        assertTrue(adapter.isImmune(List.of("water", "ground"), null));
        assertTrue(adapter.isImmune(List.of("Ground"), "keeneye"));
        assertTrue(adapter.isImmune(List.of("GROUND"), null));
    }

    @Test
    @DisplayName("T17b: Non-immune target sets confirmedImmune = false")
    void testNonImmuneTargetNotImmune() {
        assertFalse(adapter.isImmune(List.of("water", "flying"), null));
        assertFalse(adapter.isImmune(List.of("fire"), "blaze"));
        assertFalse(adapter.isImmune(List.of("grass", "poison"), "overgrow"));
        assertFalse(adapter.isImmune(List.of(), null));
    }

    @Test
    @DisplayName("T17c: Effective runtime Soundproof / Volt Absorb / Motor Drive / Lightning Rod sets confirmedImmune = true")
    void testAbilitiesAreImmune() {
        assertTrue(adapter.isImmune(List.of("normal"), "soundproof"));
        assertTrue(adapter.isImmune(List.of("electric"), "voltabsorb"));
        assertTrue(adapter.isImmune(List.of("electric"), "volt_absorb"));
        assertTrue(adapter.isImmune(List.of("electric"), "motordrive"));
        assertTrue(adapter.isImmune(List.of("electric"), "motor-drive"));
        assertTrue(adapter.isImmune(List.of("electric"), "lightningrod"));
        assertTrue(adapter.isImmune(List.of("electric"), "lightning_rod"));
    }

    @Test
    @DisplayName("T17d: Null safety on isImmune and isImmuneToPreferredMove")
    void testNullSafety() {
        assertFalse(adapter.isImmune(null, null));
        assertFalse(adapter.isImmuneToPreferredMove(null));
        assertEquals(Optional.empty(), adapter.extract(null, null, null, null));
    }
}
