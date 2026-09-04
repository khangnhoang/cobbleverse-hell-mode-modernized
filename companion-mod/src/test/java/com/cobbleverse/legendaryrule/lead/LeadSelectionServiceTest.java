package com.cobbleverse.legendaryrule.lead;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class LeadSelectionServiceTest {

    private TypeMatchupScorer scorer;

    @BeforeEach
    void setUp() {
        TypeChartData typeChart = new TypeChartData(Map.of(
                "psychic", Map.of("fighting", 2.0)
        ));
        scorer = new TypeMatchupScorer(typeChart);
        LeadSelectionConfig.setEnabled(true);
    }

    @AfterEach
    void tearDown() {
        LeadSelectionService.setUnavailable();
        LeadSelectionConfig.setEnabled(true);
    }

    @Test
    void testLifecycleAndAvailability() {
        LeadSelectionService.setUnavailable();
        assertFalse(LeadSelectionService.isAvailable());

        LeadSelectionService.initialize(scorer);
        assertTrue(LeadSelectionService.isAvailable());

        // When config is disabled, service reports unavailable
        LeadSelectionConfig.setEnabled(false);
        assertFalse(LeadSelectionService.isAvailable());

        LeadSelectionConfig.setEnabled(true);
        assertTrue(LeadSelectionService.isAvailable());

        LeadSelectionService.initialize(null);
        assertFalse(LeadSelectionService.isAvailable());
    }

    @Test
    void testSelectLeadReturnsEmptyWhenUnavailable() {
        LeadSelectionService.setUnavailable();
        Optional<LeadSelectionResult> result = LeadSelectionService.selectLead("kanto_sabrina", null, null);
        assertTrue(result.isEmpty(), "Must return empty when service is unavailable");
    }

    @Test
    void testSelectLeadReturnsEmptyOnNullOrEmptyParameters() {
        LeadSelectionService.initialize(scorer);

        assertTrue(LeadSelectionService.selectLead(null, null, null).isEmpty());
        assertTrue(LeadSelectionService.selectLead("kanto_sabrina", null, null).isEmpty());
        assertTrue(LeadSelectionService.selectLead("kanto_sabrina", new com.cobblemon.mod.common.pokemon.Pokemon[0], null).isEmpty());
    }

    @Test
    void testSelectLeadReturnsEmptyForUnconfiguredTrainer() {
        LeadSelectionService.initialize(scorer);

        // Brock has no configuration in LeadSelectionConfig
        // Even with a dummy array, it should immediately return empty without accessing player
        com.cobblemon.mod.common.pokemon.Pokemon[] dummyTeam = new com.cobblemon.mod.common.pokemon.Pokemon[1];
        Optional<LeadSelectionResult> result = LeadSelectionService.selectLead("kanto_brock", dummyTeam, null);
        assertTrue(result.isEmpty(), "Unconfigured trainer must immediately return empty to preserve native ordering");
    }
}
