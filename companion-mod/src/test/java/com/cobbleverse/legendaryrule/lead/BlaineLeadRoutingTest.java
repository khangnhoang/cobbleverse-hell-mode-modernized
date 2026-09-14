package com.cobbleverse.legendaryrule.lead;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Blaine lead-routing regressions (R1–R12) for the Water/Ground/Rock split.
 * <p>
 * These pin the <em>selected preset and selected lead slots</em>, not individual bonus components, so a
 * future change to scoring magnitudes cannot silently re-route a board. Presets and roster are loaded
 * from the real datapack through {@link BlaineLeadSelectionTest}, so the authored JSON is what is under
 * test.
 * <p>
 * Roster speeds (level 70, from the datapack IV/EV/nature values): arcanine 194, charizard 210,
 * moltres 215, rillaboom 145, golisopod 82, zeraora 298. Slot 1 is Mega Charizard Y at 210 and is the
 * speed reference for both branches.
 */
class BlaineLeadRoutingTest {

    private static final String SLOW = "anti_water_ground_slow";
    private static final String FAST = "anti_water_ground_fast";
    private static final String ROCK = "anti_rock";
    private static final String DEFAULT = "default_sun_intimidate";

    /** Mega Charizard Y + Rillaboom. */
    private static final int[] CHARIZARD_RILLABOOM = {1, 3};
    /** Rillaboom + Mega Golisopod. */
    private static final int[] RILLABOOM_GOLISOPOD = {3, 4};

    private static LeadSelectionEngine engine;

    @BeforeAll
    static void setUp() {
        TypeChartData data = TypeChartResourceLoader.load().orElseThrow(
                () -> new IllegalStateException("Failed to load canonical Gen 9 type chart for tests"));
        engine = new LeadSelectionEngine(new TypeMatchupScorer(data));
    }

    private static PlayerLeadTyping lead(String species, List<String> types, int speed, String... moves) {
        return new PlayerLeadTyping(species, types, speed, List.of(moves));
    }

    private static LeadSelectionResult run(List<PlayerLeadTyping> leads) throws Exception {
        return engine.select(
                BlaineLeadSelectionTest.loadBlainePresetsFromDatapack(),
                leads,
                BlaineLeadSelectionTest.loadBlaineRosterFromDatapack());
    }

    /** Asserts both the selected preset and the exact deployed lead slots. */
    private static void assertRouted(LeadSelectionResult result, String presetId, int[] slots, String board) {
        assertEquals(presetId, result.selectedAttempt().id(), "Selected preset on board: " + board);
        assertArrayEquals(slots, result.selectedAttempt().leadSlots(),
                "Selected lead slots on board: " + board);
    }

    private static AttemptScore scoreOf(LeadSelectionResult result, String id) {
        return result.evaluatedScores().stream()
                .filter(s -> s.attemptId().equals(id))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No evidence recorded for attempt " + id));
    }

    /**
     * R1 — the load-bearing live regression. Tyranitar is a Rock threat and Garchomp is a Ground
     * threat slower than Mega Charizard Y, so BOTH the Rock branch and the slow Water/Ground branch are
     * eligible here. The Rock branch must outrank the slow branch, because leading Charizard into a
     * Rock + Ground board is the tactical error this task exists to prevent.
     */
    @Test
    void r1_loadBearingTyranitarGarchompRoutesToTheSafeCoreAndNeverCharizard() throws Exception {
        LeadSelectionResult result = run(List.of(
                lead("tyranitar", List.of("rock", "dark"), 81),
                lead("garchomp", List.of("dragon", "ground"), 134)));

        assertRouted(result, ROCK, RILLABOOM_GOLISOPOD, "Tyranitar + Garchomp");

        // The Rock-safe core and the slow Charizard core share Rillaboom but differ on slot 1 vs slot 4.
        assertTrue(scoreOf(result, ROCK).eligible(),
                "Tyranitar is a Rock threat, so the anti-Rock branch must be eligible");
        assertTrue(scoreOf(result, SLOW).eligible(),
                "Garchomp is a Ground lead slower than Mega Charizard Y, so the slow branch is also eligible");
        assertTrue(scoreOf(result, ROCK).totalScore() > scoreOf(result, SLOW).totalScore(),
                "The Rock branch must strictly outrank the slow Charizard branch on the load-bearing board");

        assertNotEquals(SLOW, result.selectedAttempt().id(),
                "The slow Charizard branch must not win a Rock + Ground board");
        assertFalse(java.util.Arrays.stream(result.selectedAttempt().leadSlots()).anyMatch(s -> s == 1),
                "Mega Charizard Y (slot 1) must not be deployed into a Rock + Ground board");
    }

    /** R2 — a mono-Water lead slower than Mega Charizard Y keeps the Charizard + Rillaboom core. */
    @Test
    void r2_slowMonoWaterLeadKeepsCharizardAndRillaboom() throws Exception {
        LeadSelectionResult result = run(List.of(
                lead("blastoise", List.of("water"), 78),
                lead("clefable", List.of("fairy"), 60)));

        assertRouted(result, SLOW, CHARIZARD_RILLABOOM, "slow mono Water");
    }

    /** R3 — a mono-Ground lead slower than Mega Charizard Y keeps the Charizard + Rillaboom core. */
    @Test
    void r3_slowMonoGroundLeadKeepsCharizardAndRillaboom() throws Exception {
        LeadSelectionResult result = run(List.of(
                lead("sandslash", List.of("ground"), 65),
                lead("clefable", List.of("fairy"), 60)));

        assertRouted(result, SLOW, CHARIZARD_RILLABOOM, "slow mono Ground");
    }

    /** R4 — a Water lead FASTER than Mega Charizard Y routes to the safe core, not Charizard. */
    @Test
    void r4_fastWaterLeadRoutesToTheSafeCore() throws Exception {
        LeadSelectionResult result = run(List.of(
                lead("barraskewda", List.of("water"), 250),
                lead("clefable", List.of("fairy"), 60)));

        assertRouted(result, FAST, RILLABOOM_GOLISOPOD, "fast mono Water");
    }

    /** R5 — a Ground lead FASTER than Mega Charizard Y routes to the safe core, not Charizard. */
    @Test
    void r5_fastGroundLeadRoutesToTheSafeCore() throws Exception {
        LeadSelectionResult result = run(List.of(
                lead("excadrill", List.of("ground", "steel"), 288),
                lead("clefable", List.of("fairy"), 60)));

        assertRouted(result, FAST, RILLABOOM_GOLISOPOD, "fast mono Ground");
    }

    /**
     * R6 — when a board carries both a slow and a fast Water/Ground threat, the fast branch must win.
     * Asserted in both declaration orders so the outcome cannot depend on which threat is listed first.
     */
    @Test
    void r6_fastBranchWinsWhenBothASlowAndAFastThreatArePresent() throws Exception {
        LeadSelectionResult slowFirst = run(List.of(
                lead("gastrodon", List.of("water", "ground"), 39),
                lead("barraskewda", List.of("water"), 250)));
        assertRouted(slowFirst, FAST, RILLABOOM_GOLISOPOD, "slow then fast");

        LeadSelectionResult fastFirst = run(List.of(
                lead("barraskewda", List.of("water"), 250),
                lead("gastrodon", List.of("water", "ground"), 39)));
        assertRouted(fastFirst, FAST, RILLABOOM_GOLISOPOD, "fast then slow");

        assertNotEquals(SLOW, slowFirst.selectedAttempt().id());
        assertNotEquals(SLOW, fastFirst.selectedAttempt().id());
    }

    /** R7 — Rock pressure alongside a slow Water/Ground threat must win with the Rock-safe branch. */
    @Test
    void r7_rockPressureWithASlowWaterGroundThreatRoutesToTheSafeCore() throws Exception {
        LeadSelectionResult result = run(List.of(
                lead("tyranitar", List.of("rock", "dark"), 81),
                lead("gastrodon", List.of("water", "ground"), 39)));

        assertRouted(result, ROCK, RILLABOOM_GOLISOPOD, "Rock + slow Water/Ground");
        assertNotEquals(SLOW, result.selectedAttempt().id());
    }

    /**
     * R8 — the equal-speed boundary. A Water/Ground lead at exactly 210 (Mega Charizard Y's speed)
     * satisfies neither branch: strict {@code <} and strict {@code >} both fail.
     */
    @Test
    void r8_equalSpeedBoundarySatisfiesNeitherBranch() throws Exception {
        LeadSelectionResult water = run(List.of(
                lead("slowbro", List.of("water"), 210),
                lead("clefable", List.of("fairy"), 60)));
        assertEquals(0, scoreOf(water, SLOW).opponentMatchBonus(),
                "Speed equality must not satisfy slowerThanRosterSlot 1");
        assertEquals(0, scoreOf(water, FAST).opponentMatchBonus(),
                "Speed equality must not satisfy fasterThanRosterSlot 1");
        assertNotEquals(SLOW, water.selectedAttempt().id());
        assertNotEquals(FAST, water.selectedAttempt().id());

        LeadSelectionResult ground = run(List.of(
                lead("sandslash", List.of("ground"), 210),
                lead("clefable", List.of("fairy"), 60)));
        assertEquals(0, scoreOf(ground, SLOW).opponentMatchBonus());
        assertEquals(0, scoreOf(ground, FAST).opponentMatchBonus());
        assertNotEquals(SLOW, ground.selectedAttempt().id());
        assertNotEquals(FAST, ground.selectedAttempt().id());
    }

    /**
     * R9 — Choice Scarf speed is resolved exactly once by the adapter and compared verbatim. A raw 150
     * resolves to 225 (> 210) and fires the fast branch; 141 would only exceed 210 if the multiplier
     * were applied a second time, so it must not fire.
     */
    @Test
    void r9_choiceScarfSpeedIsResolvedExactlyOnce() throws Exception {
        int scarfed = (int) Math.floor(150 * 1.5);
        assertEquals(225, scarfed, "The Choice Scarf multiplier must resolve 150 to 225");

        LeadSelectionResult resolved = run(List.of(
                lead("gengar", List.of("water"), scarfed),
                lead("clefable", List.of("fairy"), 60)));
        assertRouted(resolved, FAST, RILLABOOM_GOLISOPOD, "scarf-resolved 225");

        LeadSelectionResult raw = run(List.of(
                lead("gengar", List.of("water"), 141),
                lead("clefable", List.of("fairy"), 60)));
        assertEquals(0, scoreOf(raw, FAST).opponentMatchBonus(),
                "141 must not satisfy fasterThanRosterSlot 1 unless the scarf multiplier is double-counted");
        assertNotEquals(FAST, raw.selectedAttempt().id());
    }

    /** R10 — the pre-existing fast-Electric route must still win its positive board. */
    @Test
    void r10_existingFastElectricRouteStillWinsItsBoard() throws Exception {
        LeadSelectionResult result = run(List.of(
                lead("pelipper", List.of("water", "flying"), 65),
                lead("regieleki", List.of("electric"), 250, "electric")));

        assertRouted(result, "anti_fast_electric", RILLABOOM_GOLISOPOD, "pelipper + fast Electric STAB");
    }

    /** R11 — the pre-existing rain route must keep its precedence on the rain boards. */
    @Test
    void r11_existingRainRouteKeepsItsPrecedence() throws Exception {
        assertRouted(run(List.of(
                        lead("pelipper", List.of("water", "flying"), 65),
                        lead("politoed", List.of("water"), 70))),
                "anti_rain_core", new int[]{5, 3}, "pelipper + politoed");

        assertRouted(run(List.of(
                        lead("pelipper", List.of("water", "flying"), 65),
                        lead("kingdra", List.of("water", "dragon"), 85))),
                "anti_rain_core", new int[]{5, 3}, "pelipper + kingdra");

        // A fast rain lead must still lose to anti_rain_core rather than being pulled into the
        // Water/Ground fast branch.
        assertRouted(run(List.of(
                        lead("pelipper", List.of("water", "flying"), 65),
                        lead("barraskewda", List.of("water"), 250))),
                "anti_rain_core", new int[]{5, 3}, "pelipper + fast rain abuser");
    }

    /** R12 — a neutral board with no Water/Ground/Rock pressure must still fall through to the default. */
    @Test
    void r12_neutralBoardStillFallsThroughToTheDefault() throws Exception {
        LeadSelectionResult result = run(List.of(
                lead("corviknight", List.of("steel", "flying"), 67),
                lead("clefable", List.of("fairy"), 60)));

        assertRouted(result, DEFAULT, new int[]{1, 0}, "neutral board");
        assertEquals(0, scoreOf(result, SLOW).opponentMatchBonus());
        assertEquals(0, scoreOf(result, FAST).opponentMatchBonus());
        assertEquals(0, scoreOf(result, ROCK).opponentMatchBonus());
    }

    /**
     * R13 — the ineligibility invariant. On every board where the Water/Ground matcher is unsatisfied,
     * the attempt must be ineligible, and an ineligible attempt must never be selected.
     */
    @Test
    void r13_unsatisfiedWaterGroundMatchersAreIneligibleAndNeverSelected() throws Exception {
        record Board(String name, List<PlayerLeadTyping> leads) {}

        List<Board> boards = List.of(
                new Board("neutral", List.of(lead("corviknight", List.of("steel", "flying"), 67),
                        lead("clefable", List.of("fairy"), 60))),
                new Board("fast Electric only", List.of(lead("pelipper", List.of("water", "flying"), 65),
                        lead("regieleki", List.of("electric"), 250, "electric"))),
                new Board("rain core", List.of(lead("pelipper", List.of("water", "flying"), 65),
                        lead("politoed", List.of("water"), 70))),
                new Board("non Water/Ground pair", List.of(lead("tyranitar", List.of("rock", "dark"), 81),
                        lead("clefable", List.of("fairy"), 60))));

        for (Board board : boards) {
            LeadSelectionResult result = run(board.leads());
            for (String id : List.of(SLOW, FAST)) {
                AttemptScore score = scoreOf(result, id);
                assertEquals(score.opponentMatchBonus() > 0, score.eligible(),
                        "Ineligibility must be exactly the unsatisfied matcher on board: " + board.name()
                                + " for preset " + id);
                if (!score.eligible()) {
                    assertNotEquals(id, result.selectedAttempt().id(),
                            "An ineligible preset must never be selected on board: " + board.name());
                }
            }
        }
    }

    /**
     * R14 — Rock no longer belongs to the Water/Ground branches. A Rock lead with no Water/Ground
     * typing must satisfy anti_rock and must leave both Water/Ground matchers unsatisfied.
     */
    @Test
    void r14_rockAloneDrivesAntiRockAndNotTheWaterGroundBranches() throws Exception {
        LeadSelectionResult result = run(List.of(
                lead("tyranitar", List.of("rock", "dark"), 81),
                lead("clefable", List.of("fairy"), 60)));

        assertRouted(result, ROCK, RILLABOOM_GOLISOPOD, "Rock threat alone");
        assertEquals(0, scoreOf(result, SLOW).opponentMatchBonus(),
                "A Rock-only lead must not satisfy the Water/Ground slow matcher");
        assertEquals(0, scoreOf(result, FAST).opponentMatchBonus(),
                "A Rock-only lead must not satisfy the Water/Ground fast matcher");
        assertFalse(scoreOf(result, SLOW).eligible());
        assertFalse(scoreOf(result, FAST).eligible());
    }
}
