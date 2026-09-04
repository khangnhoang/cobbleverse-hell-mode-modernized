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
        TypeChartData data = TypeChartResourceLoader.load().orElseThrow(
                () -> new IllegalStateException("Failed to load canonical Gen 9 type chart for tests"));
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
    void testUnsupportedMultipliersRejected() {
        double[] invalidValues = {3.2, 1.7, 0.7, 1.5, -1.0, 5.0, 0.1};
        for (double inv : invalidValues) {
            assertThrows(IllegalArgumentException.class, () -> scorer.mapOffensiveScore(inv),
                    "Offensive mapping must throw for unsupported multiplier: " + inv);
            assertThrows(IllegalArgumentException.class, () -> scorer.mapDefensiveScore(inv),
                    "Defensive mapping must throw for unsupported multiplier: " + inv);
        }
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
