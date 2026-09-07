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
        TypeChartData data = TypeChartResourceLoader.load().orElseThrow(
                () -> new IllegalStateException("Failed to load canonical Gen 9 type chart for tests"));
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
        // Preset A (Indeedee + Alakazam) vs Preset B (Farigiraf + Hatterene) against pure Fighting lead
        LeadAttempt attA = new LeadAttempt("attA", new int[]{0, 5}, 0, List.of(), "");
        LeadAttempt attB = new LeadAttempt("attB", new int[]{1, 2}, 0, List.of(), "");

        List<PlayerLeadTyping> player = List.of(new PlayerLeadTyping("machamp", List.of("fighting")));
        // Declared with attA first:
        LeadSelectionResult result = engine.select(List.of(attA, attB), player, createSabrinaRoster());

        // Prove attB is selected because its totalScore (6) > attA's totalScore (5)
        assertEquals("attB", result.selectedAttempt().id());

        AttemptScore scoreA = result.evaluatedScores().stream().filter(s -> s.attemptId().equals("attA")).findFirst().orElseThrow();
        AttemptScore scoreB = result.evaluatedScores().stream().filter(s -> s.attemptId().equals("attB")).findFirst().orElseThrow();
        assertEquals(5, scoreA.totalScore());
        assertEquals(6, scoreB.totalScore());
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

    @Test
    void testTypeFavoredBonusSingleAndDoubleMatch() {
        LeadAttempt attNoBonus = new LeadAttempt("no_bonus", new int[]{0, 5}, 0, List.of(), "");
        LeadAttempt attFireBonus = new LeadAttempt("fire_bonus", new int[]{0, 5}, 0, List.of(), "", List.of("fire"));

        // Case 1: single fire mon
        List<PlayerLeadTyping> singleFire = List.of(
                new PlayerLeadTyping("arcanine", List.of("fire")),
                new PlayerLeadTyping("snorlax", List.of("normal"))
        );
        LeadSelectionResult res1 = engine.select(List.of(attNoBonus, attFireBonus), singleFire, createSabrinaRoster());
        AttemptScore score1 = res1.evaluatedScores().stream().filter(s -> s.attemptId().equals("fire_bonus")).findFirst().orElseThrow();
        assertEquals(2, score1.typeFavoredBonus());
        assertEquals(0, score1.speciesFavoredBonus());
        assertEquals(score1.offensiveScore() + score1.defensiveScore() + score1.baseWeight() + 2, score1.totalScore());

        // Case 2: double fire mons
        List<PlayerLeadTyping> doubleFire = List.of(
                new PlayerLeadTyping("arcanine", List.of("fire")),
                new PlayerLeadTyping("charizard", List.of("fire", "flying"))
        );
        LeadSelectionResult res2 = engine.select(List.of(attNoBonus, attFireBonus), doubleFire, createSabrinaRoster());
        AttemptScore score2 = res2.evaluatedScores().stream().filter(s -> s.attemptId().equals("fire_bonus")).findFirst().orElseThrow();
        assertEquals(4, score2.typeFavoredBonus());
        assertEquals(0, score2.speciesFavoredBonus());

        // Case 3: dual-type match only counts once per conscious Pokémon
        LeadAttempt attFireFlyingBonus = new LeadAttempt("fire_flying_bonus", new int[]{0, 5}, 0, List.of(), "", List.of("fire", "flying"));
        List<PlayerLeadTyping> charizardOnly = List.of(
                new PlayerLeadTyping("charizard", List.of("fire", "flying"))
        );
        LeadSelectionResult res3 = engine.select(List.of(attFireFlyingBonus), charizardOnly, createSabrinaRoster());
        AttemptScore score3 = res3.evaluatedScores().get(0);
        assertEquals(2, score3.typeFavoredBonus(), "A single dual-type matching multiple favored types must only grant +2 once");
    }

    @Test
    void testSpeciesFavoredBonusMatching() {
        LeadAttempt attGastrodonBonus = new LeadAttempt("gastro_counter", new int[]{0, 5}, 0, List.of(), "", List.of(), List.of("gastrodon"));

        // Single gastrodon match
        List<PlayerLeadTyping> singleGastro = List.of(
                new PlayerLeadTyping("gastrodon", List.of("water", "ground")),
                new PlayerLeadTyping("tyranitar", List.of("rock", "dark"))
        );
        LeadSelectionResult res1 = engine.select(List.of(attGastrodonBonus), singleGastro, createSabrinaRoster());
        AttemptScore score1 = res1.evaluatedScores().get(0);
        assertEquals(0, score1.typeFavoredBonus());
        assertEquals(2, score1.speciesFavoredBonus());

        // Double gastrodon match
        List<PlayerLeadTyping> doubleGastro = List.of(
                new PlayerLeadTyping("gastrodon", List.of("water", "ground")),
                new PlayerLeadTyping("gastrodon", List.of("water", "ground"))
        );
        LeadSelectionResult res2 = engine.select(List.of(attGastrodonBonus), doubleGastro, createSabrinaRoster());
        AttemptScore score2 = res2.evaluatedScores().get(0);
        assertEquals(4, score2.speciesFavoredBonus());

        // Mismatch species
        List<PlayerLeadTyping> noGastro = List.of(
                new PlayerLeadTyping("swampert", List.of("water", "ground")),
                new PlayerLeadTyping("quagsire", List.of("water", "ground"))
        );
        LeadSelectionResult res3 = engine.select(List.of(attGastrodonBonus), noGastro, createSabrinaRoster());
        AttemptScore score3 = res3.evaluatedScores().get(0);
        assertEquals(0, score3.speciesFavoredBonus(), "Swampert and Quagsire must NOT trigger gastrodon species bonus");
    }

    @Test
    void testIndependentTypeAndSpeciesBonusCombination() {
        // Attempt with both type bonus (water) and species bonus (gastrodon)
        LeadAttempt dualBonusAttempt = new LeadAttempt(
                "dual_bonus",
                new int[]{0, 5},
                0,
                List.of(),
                "",
                List.of("water"),
                List.of("gastrodon")
        );

        // Player has Gastrodon (water/ground) and Arcanine (fire)
        List<PlayerLeadTyping> playerLeads = List.of(
                new PlayerLeadTyping("gastrodon", List.of("water", "ground")),
                new PlayerLeadTyping("arcanine", List.of("fire"))
        );

        LeadSelectionResult res = engine.select(List.of(dualBonusAttempt), playerLeads, createSabrinaRoster());
        AttemptScore score = res.evaluatedScores().get(0);
        // Gastrodon gives +2 (water in favoredAgainst) and +2 (gastrodon in favoredAgainstSpecies) -> +4 total
        assertEquals(2, score.typeFavoredBonus());
        assertEquals(2, score.speciesFavoredBonus());
        assertEquals(score.offensiveScore() + score.defensiveScore() + 4, score.totalScore());
    }
}
