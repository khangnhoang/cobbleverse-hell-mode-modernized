package com.cobbleverse.legendaryrule.lead;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TypeMatchupScorerTest {

    private static TypeMatchupScorer scorer;

    @BeforeAll
    static void setUp() {
        Map<String, Map<String, Double>> matrix = new HashMap<>();

        // Setup test type chart subset
        // Normal
        matrix.put("normal", Map.of("ghost", 0.0, "rock", 0.5, "steel", 0.5));
        // Fighting
        matrix.put("fighting", Map.ofEntries(
                Map.entry("normal", 2.0), Map.entry("rock", 2.0), Map.entry("steel", 2.0),
                Map.entry("ice", 2.0), Map.entry("dark", 2.0), Map.entry("flying", 0.5),
                Map.entry("poison", 0.5), Map.entry("bug", 0.5), Map.entry("psychic", 0.5),
                Map.entry("fairy", 0.5), Map.entry("ghost", 0.0)
        ));
        // Flying
        matrix.put("flying", Map.of("grass", 2.0, "fighting", 2.0, "bug", 2.0,
                "electric", 0.5, "rock", 0.5, "steel", 0.5));
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

        TypeChartData data = new TypeChartData(matrix);
        scorer = new TypeMatchupScorer(data);
    }

    @Test
    void testMapOffensiveScoreDiscreteValues() {
        assertEquals(4, scorer.mapOffensiveScore(4.0));
        assertEquals(2, scorer.mapOffensiveScore(2.0));
        assertEquals(0, scorer.mapOffensiveScore(1.0));
        assertEquals(-1, scorer.mapOffensiveScore(0.5));
        assertEquals(-2, scorer.mapOffensiveScore(0.25));
        assertEquals(-4, scorer.mapOffensiveScore(0.0));
    }

    @Test
    void testMapDefensiveScoreDiscreteValues() {
        assertEquals(-4, scorer.mapDefensiveScore(4.0));
        assertEquals(-2, scorer.mapDefensiveScore(2.0));
        assertEquals(0, scorer.mapDefensiveScore(1.0));
        assertEquals(1, scorer.mapDefensiveScore(0.5));
        assertEquals(2, scorer.mapDefensiveScore(0.25));
        assertEquals(4, scorer.mapDefensiveScore(0.0));
    }

    @Test
    void testGetEffectivenessSingleType() {
        assertEquals(2.0, scorer.getEffectiveness("psychic", List.of("fighting")));
        assertEquals(0.5, scorer.getEffectiveness("psychic", List.of("steel")));
        assertEquals(0.0, scorer.getEffectiveness("psychic", List.of("dark")));
        assertEquals(1.0, scorer.getEffectiveness("psychic", List.of("normal")));
    }

    @Test
    void testGetEffectivenessDualType() {
        // Psychic attacking Fighting/Poison: 2.0 * 2.0 = 4.0
        assertEquals(4.0, scorer.getEffectiveness("psychic", List.of("fighting", "poison")));

        // Fighting attacking Flying/Poison: 0.5 * 0.5 = 0.25
        assertEquals(0.25, scorer.getEffectiveness("fighting", List.of("flying", "poison")));

        // Ground attacking Flying/Steel: 0.0 * 2.0 = 0.0 (immunity overrides weakness)
        assertEquals(0.0, scorer.getEffectiveness("ground", List.of("flying", "steel")));
    }

    @Test
    void testGetEffectivenessNullOrEmpty() {
        assertEquals(1.0, scorer.getEffectiveness(null, List.of("fighting")));
        assertEquals(1.0, scorer.getEffectiveness("fire", null));
        assertEquals(1.0, scorer.getEffectiveness("fire", List.of()));
    }

    @Test
    void testScoreNpcVsPlayerBestStab() {
        // NPC has Psychic + Fairy
        // Player is Fighting/Poison:
        // Psychic -> 4.0 (score +4)
        // Fairy -> 2.0 * 0.5 = 1.0 (score 0)
        // Best STAB is 4.0 -> score 4
        int score = scorer.scoreNpcVsPlayer(List.of("psychic", "fairy"), List.of("fighting", "poison"));
        assertEquals(4, score);
    }

    @Test
    void testScorePlayerVsNpcWorstIncomingThreat() {
        // Player has Dark + Fighting
        // NPC is Psychic/Steel:
        // Dark -> 2.0 * 1.0 = 2.0 (score -2)
        // Fighting -> 0.5 * 2.0 = 1.0 (score 0)
        // Worst incoming threat is 2.0 -> defensive score -2
        int defScore = scorer.scorePlayerVsNpc(List.of("dark", "fighting"), List.of("psychic", "steel"));
        assertEquals(-2, defScore);
    }

    @Test
    void testScoreNpcVsPlayerEmptyStab() {
        assertEquals(0, scorer.scoreNpcVsPlayer(List.of(), List.of("fighting")));
        assertEquals(0, scorer.scoreNpcVsPlayer(null, List.of("fighting")));
    }

    @Test
    void testScorePlayerVsNpcEmptyStab() {
        assertEquals(0, scorer.scorePlayerVsNpc(List.of(), List.of("psychic")));
        assertEquals(0, scorer.scorePlayerVsNpc(null, List.of("psychic")));
    }
}
