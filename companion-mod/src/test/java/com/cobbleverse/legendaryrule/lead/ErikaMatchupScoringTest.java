package com.cobbleverse.legendaryrule.lead;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ErikaMatchupScoringTest {

    private static TypeMatchupScorer scorer;
    private static LeadSelectionEngine engine;

    @BeforeAll
    static void setUp() {
        TypeChartData data = TypeChartResourceLoader.load().orElseThrow(
                () -> new IllegalStateException("Failed to load canonical Gen 9 type chart for tests"));
        scorer = new TypeMatchupScorer(data);
        engine = new LeadSelectionEngine(scorer);
    }

    private List<RosterMemberTyping> createErikaRoster() {
        return List.of(
                new RosterMemberTyping(0, "hydrapple", List.of("grass", "dragon")),
                new RosterMemberTyping(1, "ludicolo", List.of("water", "grass")),
                new RosterMemberTyping(2, "ogerpon", List.of("grass", "water")),
                new RosterMemberTyping(3, "electrode", List.of("electric", "grass")),
                new RosterMemberTyping(4, "rillaboom", List.of("grass")),
                new RosterMemberTyping(5, "meganium", List.of("grass"))
        );
    }

    private List<LeadAttempt> createErikaPresets() {
        return List.of(
                new LeadAttempt("rain_swift_swim", new int[]{0, 1}, 1, List.of(), "Hydrapple + Ludicolo", List.of("fire")),
                new LeadAttempt("rain_redirection", new int[]{0, 2}, 0, List.of(), "Hydrapple + Ogerpon-Wellspring"),
                new LeadAttempt("fast_volt_switch", new int[]{3, 1}, 0, List.of(), "Electrode-H + Ludicolo"),
                new LeadAttempt("double_fake_out", new int[]{1, 4}, 0, List.of(), "Ludicolo + Rillaboom"),
                new LeadAttempt("mega_sol_sun", new int[]{4, 5}, 0, List.of(), "Rillaboom + Meganium"),
                new LeadAttempt("grassy_glide_follow_me", new int[]{4, 2}, 0, List.of(), "Rillaboom + Ogerpon-Wellspring"),
                new LeadAttempt("sun_specs_coverage", new int[]{3, 5}, -1, List.of(), "Electrode-H + Meganium")
        );
    }

    @Test
    void testAgainstPureFireLeadsRainSwiftSwimWinsDecisively() {
        // Double pure Fire leads: Arcanine + Ninetales
        List<PlayerLeadTyping> fireLeads = List.of(
                new PlayerLeadTyping("arcanine", List.of("fire")),
                new PlayerLeadTyping("ninetales", List.of("fire"))
        );

        LeadSelectionResult result = engine.select(createErikaPresets(), fireLeads, createErikaRoster());
        assertEquals("rain_swift_swim", result.selectedAttempt().id(),
                "Rain Swift Swim core MUST be selected against Fire leads due to favoredAgainst bonus");

        AttemptScore rainScore = result.evaluatedScores().stream()
                .filter(s -> s.attemptId().equals("rain_swift_swim"))
                .findFirst().orElseThrow();
        assertEquals(4, rainScore.typeFavoredBonus(), "Both Fire leads grant +2 -> +4 total favored bonus");
        assertEquals(1, rainScore.baseWeight());

        // Compare against rain_redirection which has Ogerpon instead of Ludicolo
        AttemptScore redirScore = result.evaluatedScores().stream()
                .filter(s -> s.attemptId().equals("rain_redirection"))
                .findFirst().orElseThrow();
        assertEquals(0, redirScore.typeFavoredBonus());
        assertTrue(rainScore.totalScore() > redirScore.totalScore(),
                "rain_swift_swim total (" + rainScore.totalScore() + ") must beat rain_redirection (" + redirScore.totalScore() + ")");
    }

    @Test
    void testAgainstWaterFlyingLeadsFastVoltSwitchExcels() {
        // Player leads Pelipper (water/flying) and Gyarados (water/flying)
        List<PlayerLeadTyping> flyingLeads = List.of(
                new PlayerLeadTyping("pelipper", List.of("water", "flying")),
                new PlayerLeadTyping("gyarados", List.of("water", "flying"))
        );

        LeadSelectionResult result = engine.select(createErikaPresets(), flyingLeads, createErikaRoster());
        // Hisuian Electrode has Electric STAB hitting 4x vs Water/Flying
        assertEquals("fast_volt_switch", result.selectedAttempt().id(),
                "fast_volt_switch core (Electrode-H) must win against 4x Electric weak leads");
    }

    @Test
    void testAgainstWaterGroundLeadsOffensiveGrassCoresCompete() {
        // Player leads Gastrodon (water/ground) and Swampert (water/ground)
        List<PlayerLeadTyping> waterGroundLeads = List.of(
                new PlayerLeadTyping("gastrodon", List.of("water", "ground")),
                new PlayerLeadTyping("swampert", List.of("water", "ground"))
        );

        LeadSelectionResult result = engine.select(createErikaPresets(), waterGroundLeads, createErikaRoster());
        assertNotNull(result.selectedAttempt());
        // All attempts with dual Grass STAB get massive offensive bonuses (+4 per mon = +16 total)
        AttemptScore winnerScore = result.evaluatedScores().stream()
                .filter(s -> s.attemptId().equals(result.selectedAttempt().id()))
                .findFirst().orElseThrow();
        assertTrue(winnerScore.offensiveScore() >= 10, "Grass STAB against Water/Ground must yield high offensive score");
    }
}
