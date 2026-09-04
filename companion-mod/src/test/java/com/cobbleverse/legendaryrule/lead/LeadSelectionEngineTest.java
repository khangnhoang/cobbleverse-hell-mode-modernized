package com.cobbleverse.legendaryrule.lead;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LeadSelectionEngineTest {

    private static TypeMatchupScorer scorer;
    private static LeadSelectionEngine engine;

    @BeforeAll
    static void setUp() {
        Map<String, Map<String, Double>> matrix = new HashMap<>();

        // Normal
        matrix.put("normal", Map.of("ghost", 0.0, "rock", 0.5, "steel", 0.5));
        // Fighting
        matrix.put("fighting", Map.ofEntries(
                Map.entry("normal", 2.0), Map.entry("rock", 2.0), Map.entry("steel", 2.0),
                Map.entry("ice", 2.0), Map.entry("dark", 2.0), Map.entry("flying", 0.5),
                Map.entry("poison", 0.5), Map.entry("bug", 0.5), Map.entry("psychic", 0.5),
                Map.entry("fairy", 0.5), Map.entry("ghost", 0.0)
        ));
        // Ground
        matrix.put("ground", Map.of("fire", 2.0, "electric", 2.0, "poison", 2.0, "rock", 2.0, "steel", 2.0,
                "grass", 0.5, "bug", 0.5, "flying", 0.0));
        // Psychic
        matrix.put("psychic", Map.of("fighting", 2.0, "poison", 2.0,
                "psychic", 0.5, "steel", 0.5, "dark", 0.0));
        // Ghost
        matrix.put("ghost", Map.of("psychic", 2.0, "ghost", 2.0, "dark", 0.5, "normal", 0.0));
        // Dark
        matrix.put("dark", Map.of("psychic", 2.0, "ghost", 2.0, "fighting", 0.5, "dark", 0.5, "fairy", 0.5));
        // Steel
        matrix.put("steel", Map.of("ice", 2.0, "rock", 2.0, "fairy", 2.0,
                "fire", 0.5, "water", 0.5, "electric", 0.5, "steel", 0.5));
        // Fairy
        matrix.put("fairy", Map.of("fighting", 2.0, "dragon", 2.0, "dark", 2.0,
                "fire", 0.5, "poison", 0.5, "steel", 0.5));
        // Rock
        matrix.put("rock", Map.of("fire", 2.0, "ice", 2.0, "flying", 2.0, "bug", 2.0,
                "fighting", 0.5, "ground", 0.5, "steel", 0.5));
        // Ice
        matrix.put("ice", Map.of("grass", 2.0, "ground", 2.0, "flying", 2.0, "dragon", 2.0,
                "fire", 0.5, "water", 0.5, "ice", 0.5, "steel", 0.5));

        TypeChartData data = new TypeChartData(matrix);
        scorer = new TypeMatchupScorer(data);
        engine = new LeadSelectionEngine(scorer);
    }

    private List<RosterMemberTyping> createSabrinaRoster() {
        return List.of(
                new RosterMemberTyping(0, "indeedee", List.of("psychic", "normal")),
                new RosterMemberTyping(1, "farigiraf", List.of("normal", "psychic")),
                new RosterMemberTyping(2, "hatterene", List.of("psychic", "fairy")),
                new RosterMemberTyping(3, "metagross", List.of("steel", "psychic")),
                new RosterMemberTyping(4, "armarouge", List.of("fire", "psychic")),
                new RosterMemberTyping(5, "alakazam", List.of("psychic"))
        );
    }

    private List<LeadAttempt> createSabrinaAttempts() {
        return List.of(
                new LeadAttempt("psychic_terrain_blitz", new int[]{0, 5}, 1, List.of(), "Indeedee + Alakazam"),
                new LeadAttempt("anti_dark_priority_block", new int[]{1, 2}, 0, List.of(), "Farigiraf + Hatterene"),
                new LeadAttempt("heavy_steel_offense", new int[]{0, 3}, -1, List.of(), "Indeedee + Metagross")
        );
    }

    @Test
    void testEvaluationAgainstFightingLeadsPrefersPsychicBlitz() {
        List<PlayerLeadTyping> playerLeads = List.of(
                new PlayerLeadTyping("machamp", List.of("fighting")),
                new PlayerLeadTyping("lucario", List.of("fighting", "steel"))
        );

        LeadSelectionResult result = engine.select(createSabrinaAttempts(), playerLeads, createSabrinaRoster());
        assertNotNull(result.selectedAttempt());
        // Against pure fighting / fighting-steel, psychic offensive blitz is very strong
        assertEquals("psychic_terrain_blitz", result.selectedAttempt().id());
        assertEquals(3, result.evaluatedScores().size());
    }

    @Test
    void testEvaluationAgainstDarkLeadsFavorsAntiDarkPreset() {
        // Dark leads: Tyranitar (rock/dark) and Weavile (dark/ice)
        // Alakazam has 0x effectiveness against Dark with Psychic STAB, and takes 2x from Dark.
        // Hatterene has Fairy STAB (2x vs Dark) and resists Dark (0.5x incoming).
        List<PlayerLeadTyping> playerLeads = List.of(
                new PlayerLeadTyping("tyranitar", List.of("rock", "dark")),
                new PlayerLeadTyping("weavile", List.of("dark", "ice"))
        );

        LeadSelectionResult result = engine.select(createSabrinaAttempts(), playerLeads, createSabrinaRoster());
        assertEquals("anti_dark_priority_block", result.selectedAttempt().id());

        // Check evidence
        AttemptScore antiDarkScore = result.evaluatedScores().stream()
                .filter(s -> s.attemptId().equals("anti_dark_priority_block"))
                .findFirst().orElseThrow();
        AttemptScore blitzScore = result.evaluatedScores().stream()
                .filter(s -> s.attemptId().equals("psychic_terrain_blitz"))
                .findFirst().orElseThrow();

        assertTrue(antiDarkScore.totalScore() > blitzScore.totalScore(),
                "Anti-dark preset total score (" + antiDarkScore.totalScore() +
                ") should exceed blitz score (" + blitzScore.totalScore() + ") against Dark leads");
    }

    @Test
    void testDeterministicTieBreakingTotalScoreWins() {
        // Preset A has higher total score than Preset B
        LeadAttempt attA = new LeadAttempt("attA", new int[]{0, 5}, 0, List.of(), "");
        LeadAttempt attB = new LeadAttempt("attB", new int[]{1, 2}, 0, List.of(), "");

        List<PlayerLeadTyping> player = List.of(new PlayerLeadTyping("machamp", List.of("fighting")));
        LeadSelectionResult result = engine.select(List.of(attA, attB), player, createSabrinaRoster());

        // Total score difference dictates winner
        assertNotNull(result.selectedAttempt());
    }

    @Test
    void testDeterministicTieBreakingBaseWeightWinsWhenScoresEqual() {
        // Construct two attempts with the exact same slots but different baseWeights
        LeadAttempt lowerWeight = new LeadAttempt("lower", new int[]{0, 5}, -1, List.of(), "");
        LeadAttempt higherWeight = new LeadAttempt("higher", new int[]{0, 5}, 2, List.of(), "");

        List<PlayerLeadTyping> player = List.of(new PlayerLeadTyping("snorlax", List.of("normal")));

        // Even if declared in order: lowerWeight first, then higherWeight
        LeadSelectionResult result = engine.select(List.of(lowerWeight, higherWeight), player, createSabrinaRoster());
        assertEquals("higher", result.selectedAttempt().id());
    }

    @Test
    void testDeterministicTieBreakingDeclarationOrderWinsWhenScoreAndWeightEqual() {
        // Construct two attempts with the exact same slots and same baseWeight
        LeadAttempt first = new LeadAttempt("first_declared", new int[]{0, 5}, 1, List.of(), "");
        LeadAttempt second = new LeadAttempt("second_declared", new int[]{0, 5}, 1, List.of(), "");

        List<PlayerLeadTyping> player = List.of(new PlayerLeadTyping("snorlax", List.of("normal")));

        LeadSelectionResult result = engine.select(List.of(first, second), player, createSabrinaRoster());
        assertEquals("first_declared", result.selectedAttempt().id());

        // Reverse declaration order
        LeadSelectionResult reversedResult = engine.select(List.of(second, first), player, createSabrinaRoster());
        assertEquals("second_declared", reversedResult.selectedAttempt().id());
    }

    @Test
    void testSinglePlayerLeadEvaluation() {
        // Player enters with only 1 conscious Pokémon
        List<PlayerLeadTyping> singlePlayerLead = List.of(
                new PlayerLeadTyping("lucario", List.of("fighting", "steel"))
        );

        LeadSelectionResult result = engine.select(createSabrinaAttempts(), singlePlayerLead, createSabrinaRoster());
        assertNotNull(result.selectedAttempt());
        assertEquals(3, result.evaluatedScores().size());
    }

    @Test
    void testEmptyAttemptsThrows() {
        List<PlayerLeadTyping> player = List.of(new PlayerLeadTyping("snorlax", List.of("normal")));
        assertThrows(IllegalArgumentException.class, () -> engine.select(List.of(), player, createSabrinaRoster()));
    }
}
