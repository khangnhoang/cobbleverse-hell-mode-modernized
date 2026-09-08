package com.cobbleverse.legendaryrule.lead;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SurgeMatchupScoringTest {

    private static TypeMatchupScorer scorer;
    private static LeadSelectionEngine engine;

    @BeforeAll
    static void setUp() {
        TypeChartData data = TypeChartResourceLoader.load().orElseThrow(
                () -> new IllegalStateException("Failed to load canonical Gen 9 type chart for tests"));
        scorer = new TypeMatchupScorer(data);
        engine = new LeadSelectionEngine(scorer);
    }

    private List<RosterMemberTyping> createSurgeRoster() {
        return List.of(
                new RosterMemberTyping(0, "pincurchin", List.of("electric")),
                new RosterMemberTyping(1, "raichu", List.of("electric", "psychic")),
                new RosterMemberTyping(2, "rotom", List.of("electric", "water")),
                new RosterMemberTyping(3, "electivire", List.of("electric")),
                new RosterMemberTyping(4, "toxtricity", List.of("electric", "poison")),
                new RosterMemberTyping(5, "electrode", List.of("electric", "grass"))
        );
    }

    private List<LeadAttempt> createSurgePresets() {
        return List.of(
                new LeadAttempt("terrain_surfer", new int[]{0, 1}, 1, List.of(), "Pincurchin + Raichu-Alola"),
                new LeadAttempt("terrain_punk", new int[]{0, 4}, 0, List.of(), "Pincurchin + Toxtricity-Low-Key"),
                new LeadAttempt("anti_ground", new int[]{2, 1}, 0, List.of(), "Rotom-Wash + Raichu-Alola"),
                new LeadAttempt("anti_gastrodon", new int[]{5, 1}, 0, List.of(), "Electrode-Hisui + Raichu-Alola", Collections.emptyList(), List.of("gastrodon"))
        );
    }

    @Test
    void testAgainstGastrodonLeadAntiGastrodonPresetWins() {
        // Player leads Gastrodon (water/ground) + Tyranitar (rock/dark)
        List<PlayerLeadTyping> playerLeads = List.of(
                new PlayerLeadTyping("gastrodon", List.of("water", "ground")),
                new PlayerLeadTyping("tyranitar", List.of("rock", "dark"))
        );

        LeadSelectionResult result = engine.select(createSurgePresets(), playerLeads, createSurgeRoster());
        assertEquals("anti_gastrodon", result.selectedAttempt().id(),
                "anti_gastrodon preset MUST win against Gastrodon lead");

        AttemptScore gastroScore = result.evaluatedScores().stream()
                .filter(s -> s.attemptId().equals("anti_gastrodon"))
                .findFirst().orElseThrow();
        assertEquals(2, gastroScore.speciesFavoredBonus(), "Gastrodon grants +2 species favored bonus");
        assertEquals(0, gastroScore.typeFavoredBonus());
    }

    @Test
    void testAgainstNonGastrodonGroundLeadSpeciesBonusIsZero() {
        // Player leads Hippowdon (ground) + Excadrill (ground/steel)
        List<PlayerLeadTyping> playerLeads = List.of(
                new PlayerLeadTyping("hippowdon", List.of("ground")),
                new PlayerLeadTyping("excadrill", List.of("ground", "steel"))
        );

        LeadSelectionResult result = engine.select(createSurgePresets(), playerLeads, createSurgeRoster());

        AttemptScore gastroScore = result.evaluatedScores().stream()
                .filter(s -> s.attemptId().equals("anti_gastrodon"))
                .findFirst().orElseThrow();
        assertEquals(0, gastroScore.speciesFavoredBonus(), "Hippowdon must NOT trigger gastrodon species bonus");
    }

    @Test
    void testGenericMatchupDefaultsToTerrainSurfer() {
        // Player leads neutral match: Snorlax (normal) + Kangaskhan (normal)
        List<PlayerLeadTyping> neutralLeads = List.of(
                new PlayerLeadTyping("snorlax", List.of("normal")),
                new PlayerLeadTyping("kangaskhan", List.of("normal"))
        );

        LeadSelectionResult result = engine.select(createSurgePresets(), neutralLeads, createSurgeRoster());
        assertEquals("terrain_surfer", result.selectedAttempt().id(),
                "Against pure neutral leads, terrain_surfer with baseWeight 1 should win");
    }

    @Test
    void testAgainstFairyLeadTerrainPunkWinsDueToPoisonSTAB() {
        // Player leads Clefable (fairy) + Togekiss (fairy, flying)
        List<PlayerLeadTyping> fairyLeads = List.of(
                new PlayerLeadTyping("clefable", List.of("fairy")),
                new PlayerLeadTyping("togekiss", List.of("fairy", "flying"))
        );

        LeadSelectionResult result = engine.select(createSurgePresets(), fairyLeads, createSurgeRoster());
        assertEquals("terrain_punk", result.selectedAttempt().id(),
                "Against Fairy leads, Toxtricity's Poison typing gives terrain_punk the win");
    }
}
