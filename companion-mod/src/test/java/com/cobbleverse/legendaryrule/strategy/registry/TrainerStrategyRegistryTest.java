package com.cobbleverse.legendaryrule.strategy.registry;

import com.cobbleverse.legendaryrule.strategy.domain.BattleStrategyPolicy;
import com.cobbleverse.legendaryrule.strategy.domain.ThroatSpraySoundPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class TrainerStrategyRegistryTest {

    @BeforeEach
    void setUp() {
        TrainerStrategyRegistry.reset();
    }

    @Test
    @DisplayName("T14: Trainer kanto_ltsurge resolves ThroatSpraySoundPolicy configured for Toxtricity")
    void testLtSurgeResolvesCorrectPolicy() {
        Optional<BattleStrategyPolicy> optPolicy = TrainerStrategyRegistry.getPolicy("kanto_ltsurge");
        assertTrue(optPolicy.isPresent(), "kanto_ltsurge must resolve a policy");

        assertInstanceOf(ThroatSpraySoundPolicy.class, optPolicy.get());
        ThroatSpraySoundPolicy policy = (ThroatSpraySoundPolicy) optPolicy.get();

        assertEquals("toxtricitylowkey", policy.getTargetPokemonShowdownId());
        assertEquals("throatspray", policy.getTargetHeldItemShowdownId());
        assertEquals("overdrive", policy.getSoundMoveName());
    }

    @Test
    @DisplayName("T14b: Case insensitivity and whitespace trimming for trainer ID")
    void testCaseInsensitivityAndTrimming() {
        Optional<BattleStrategyPolicy> optPolicy1 = TrainerStrategyRegistry.getPolicy("  kanto_ltsurge  ");
        assertTrue(optPolicy1.isPresent());

        Optional<BattleStrategyPolicy> optPolicy2 = TrainerStrategyRegistry.getPolicy("KANTO_LTSURGE");
        assertTrue(optPolicy2.isPresent());
    }

    @Test
    @DisplayName("T14c: Trainer mismatch returns empty")
    void testTrainerMismatchReturnsEmpty() {
        assertFalse(TrainerStrategyRegistry.getPolicy("kanto_brock").isPresent());
        assertFalse(TrainerStrategyRegistry.getPolicy("kanto_sabrina").isPresent());
        assertFalse(TrainerStrategyRegistry.getPolicy("unknown_trainer").isPresent());
        assertFalse(TrainerStrategyRegistry.getPolicy(null).isPresent());
    }
}
