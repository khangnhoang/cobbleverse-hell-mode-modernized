package com.cobbleverse.legendaryrule.lead;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Datapack-loaded regressions for Blaine's {@code anti_fast_electric} preset (B1–B12) and the
 * Checkpoint-3 re-measurement of the precedence calibration table against the real engine.
 * <p>
 * Presets and roster are loaded through {@link BlaineLeadSelectionTest#loadBlainePresetsFromDatapack()}
 * and {@link BlaineLeadSelectionTest#loadBlaineRosterFromDatapack()} so these assertions exercise the
 * authored JSON rather than a hand-built fixture.
 */
class BlaineFastElectricLeadPresetTest {

    private static final String TARGET = "anti_fast_electric";

    private static LeadSelectionEngine engine;

    @BeforeAll
    static void setUp() {
        TypeChartData data = TypeChartResourceLoader.load().orElseThrow(
                () -> new IllegalStateException("Failed to load canonical Gen 9 type chart for tests"));
        engine = new LeadSelectionEngine(new TypeMatchupScorer(data));
    }

    private static LeadSelectionResult run(List<PlayerLeadTyping> leads) throws Exception {
        return engine.select(
                BlaineLeadSelectionTest.loadBlainePresetsFromDatapack(),
                leads,
                BlaineLeadSelectionTest.loadBlaineRosterFromDatapack());
    }

    private static AttemptScore scoreOf(LeadSelectionResult result, String id) {
        return result.evaluatedScores().stream()
                .filter(s -> s.attemptId().equals(id))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No evidence recorded for attempt " + id));
    }

    private static PlayerLeadTyping lead(String species, List<String> types, int speed, String... damagingMoveTypes) {
        return new PlayerLeadTyping(species, types, speed, List.of(damagingMoveTypes));
    }

    private static PlayerLeadTyping pelipper() {
        return lead("pelipper", List.of("water", "flying"), 65);
    }

    private static PlayerLeadTyping regieleki() {
        return lead("regieleki", List.of("electric"), 250, "electric");
    }

    @Test
    void b1_fastElectricStabLeadSelectsAntiFastElectric() throws Exception {
        LeadSelectionResult result = run(List.of(pelipper(), regieleki()));

        assertEquals(TARGET, result.selectedAttempt().id(),
                "A water lead plus a fast Electric STAB lead must select anti_fast_electric");
        assertArrayEquals(new int[]{3, 4}, result.selectedAttempt().leadSlots(),
                "anti_fast_electric must deploy Rillaboom (slot 3) + Mega Golisopod (slot 4)");
        assertEquals(14, scoreOf(result, TARGET).opponentMatchBonus());
    }

    @Test
    void b2_sameBoardWithoutADamagingElectricMoveDoesNotSelectAntiFastElectric() throws Exception {
        LeadSelectionResult result = run(List.of(pelipper(), lead("regieleki", List.of("electric"), 250)));

        assertNotEquals(TARGET, result.selectedAttempt().id());
        assertEquals("anti_rain_core", result.selectedAttempt().id(),
                "Without a damaging Electric move the measured board is won by anti_rain_core");
        assertEquals(0, scoreOf(result, TARGET).opponentMatchBonus());
    }

    @Test
    void b3_electricLeadSlowerThanTheReferencedSlotDoesNotSelectAntiFastElectric() throws Exception {
        LeadSelectionResult result = run(List.of(pelipper(), lead("ampharos", List.of("electric"), 180, "electric")));

        assertNotEquals(TARGET, result.selectedAttempt().id());
        assertEquals(0, scoreOf(result, TARGET).opponentMatchBonus());
    }

    @Test
    void b3b_electricLeadExactlyAtTheReferencedSpeedDoesNotSelectAntiFastElectric() throws Exception {
        // Speed equality must NOT satisfy the strict > comparison against roster slot 1 (210).
        LeadSelectionResult result = run(List.of(pelipper(), lead("regieleki", List.of("electric"), 210, "electric")));

        assertNotEquals(TARGET, result.selectedAttempt().id());
        assertEquals(0, scoreOf(result, TARGET).opponentMatchBonus());
    }

    @Test
    void b4_fastLeadWithOnlyAnOffTypeElectricCoverageMoveDoesNotSelectAntiFastElectric() throws Exception {
        // Dragapult carries an Electric coverage move and is fast, but is not itself an Electric-type lead.
        LeadSelectionResult result = run(List.of(pelipper(), lead("dragapult", List.of("dragon", "ghost"), 304, "electric")));

        assertNotEquals(TARGET, result.selectedAttempt().id());
        assertEquals(0, scoreOf(result, TARGET).opponentMatchBonus());
    }

    @Test
    void b5_slowElectricPlusFastNonElectricDoesNotSelectAntiFastElectric() throws Exception {
        LeadSelectionResult result = run(List.of(
                lead("ampharos", List.of("electric"), 180, "electric"),
                lead("dragapult", List.of("dragon", "ghost"), 304, "dragon")));

        assertNotEquals(TARGET, result.selectedAttempt().id(),
                "The conjunction must not fire when its conditions are split across two different leads");
        assertEquals("default_sun_intimidate", result.selectedAttempt().id());
        assertEquals(0, scoreOf(result, TARGET).opponentMatchBonus());
    }

    @Test
    void b6_waterGroundPartnerStillSelectsAntiFastElectric() throws Exception {
        LeadSelectionResult result = run(List.of(
                lead("swampert", List.of("water", "ground"), 60),
                regieleki()));

        assertEquals(TARGET, result.selectedAttempt().id());
        assertArrayEquals(new int[]{3, 4}, result.selectedAttempt().leadSlots());
    }

    @Test
    void b7_waterAndGroundResistantPairOutscoresLegacyAntiFastThreats() throws Exception {
        LeadSelectionResult result = run(List.of(
                regieleki(),
                lead("dragapult", List.of("dragon", "ghost"), 304, "dragon")));

        assertEquals(TARGET, result.selectedAttempt().id());
        AttemptScore legacy = scoreOf(result, "anti_fast_threats");
        assertEquals(4, legacy.fastBonus(),
                "Legacy anti_fast_threats must still satisfy its own minFastOpponents: 2 on this board");
        assertEquals(0, legacy.totalScore(),
                "Legacy anti_fast_threats scores 0 on this board (offScore + defScore = -4, fastBonus = +4)");
        assertTrue(scoreOf(result, TARGET).totalScore() > legacy.totalScore(),
                "anti_fast_electric must outscore the legacy anti_fast_threats preset");
    }

    @Test
    void b8_neutralBoardStillSelectsDefaultSunIntimidate() throws Exception {
        LeadSelectionResult result = run(List.of(
                new PlayerLeadTyping("corviknight", List.of("steel", "flying"), 67),
                new PlayerLeadTyping("clefable", List.of("fairy"), 60)));

        assertEquals("default_sun_intimidate", result.selectedAttempt().id());
        assertEquals(0, scoreOf(result, TARGET).opponentMatchBonus());
    }

    @Test
    void b9_existingBoardsKeepTheirWinnersAndTotals() throws Exception {
        record Board(String name, List<PlayerLeadTyping> leads, String winner, int total) {}

        List<Board> boards = List.of(
                new Board("true rain core", List.of(
                        pelipper(),
                        lead("swampert", List.of("water", "ground"), 60)), "anti_rain_core", 7),
                new Board("single water + Scizor", List.of(
                        pelipper(),
                        lead("scizor", List.of("bug", "steel"), 65)), "default_sun_intimidate", 7),
                new Board("water/ground pair", List.of(
                        lead("gastrodon", List.of("water", "ground"), 39),
                        lead("landorus", List.of("ground", "flying"), 101)), "anti_water_ground", 7),
                new Board("single ground + Scizor", List.of(
                        lead("gastrodon", List.of("water", "ground"), 39),
                        lead("scizor", List.of("bug", "steel"), 65)), "default_sun_intimidate", 7),
                new Board("double fast non-Electric", List.of(
                        lead("fluttermane", List.of("ghost", "fairy"), 293),
                        lead("dragapult", List.of("dragon", "ghost"), 304)), "anti_fast_threats", 4),
                new Board("single fast + Clefable", List.of(
                        lead("dragapult", List.of("dragon", "ghost"), 304),
                        new PlayerLeadTyping("clefable", List.of("fairy"), 100)), "default_sun_intimidate", 3),
                new Board("mid-speed pair", List.of(
                        new PlayerLeadTyping("corviknight", List.of("steel", "flying"), 200),
                        new PlayerLeadTyping("clefable", List.of("fairy"), 205)), "default_sun_intimidate", 8),
                new Board("Miraidon + Iron Hands", List.of(
                        new PlayerLeadTyping("miraidon", List.of("electric", "dragon"), 205),
                        new PlayerLeadTyping("ironhands", List.of("fighting", "electric"), 80)), "default_sun_intimidate", -4),
                new Board("swift-swim pair", List.of(
                        lead("swampert", List.of("water", "ground"), 130),
                        lead("landorus", List.of("ground", "flying"), 140)), "anti_water_ground", 7),
                new Board("Choice-Scarf Gengar + Dragapult", List.of(
                        new PlayerLeadTyping("gengar", List.of("ghost", "poison"), (int) Math.floor(150 * 1.5)),
                        lead("dragapult", List.of("dragon", "ghost"), 304)), "anti_fast_threats", 4));

        for (Board board : boards) {
            LeadSelectionResult result = run(board.leads());
            assertEquals(board.winner(), result.selectedAttempt().id(),
                    "Winner changed on the pre-existing board: " + board.name());
            assertEquals(board.total(), scoreOf(result, board.winner()).totalScore(),
                    "Winner totalScore changed on the pre-existing board: " + board.name());
            assertEquals(0, scoreOf(result, TARGET).opponentMatchBonus(),
                    "The matcher must stay inert on the pre-existing board: " + board.name());
        }
    }

    @Test
    void b10_choiceScarfResolvedSpeedIsUsedVerbatimWithoutASecondApplication() throws Exception {
        // Adapter authority: raw 150 with Choice Scarf resolves to floor(150 * 1.5) = 225 > 210.
        LeadSelectionResult scarfed = run(List.of(pelipper(), lead("gengar", List.of("electric"), 225, "electric")));
        assertEquals(TARGET, scarfed.selectedAttempt().id(),
                "A scarf-resolved Electric lead at 225 must satisfy the matcher against the 210 reference");
        assertTrue(scoreOf(scarfed, TARGET).eligible(),
                "A satisfied matcher must leave anti_fast_electric eligible");

        // A second x1.5 application would turn 141 into 211 (> 210) and fire the matcher. The engine
        // compares PlayerLeadTyping.speed() verbatim, so 141 must NOT satisfy it.
        LeadSelectionResult notScarfed = run(List.of(pelipper(), lead("gengar", List.of("electric"), 141, "electric")));
        assertNotEquals(TARGET, notScarfed.selectedAttempt().id(),
                "The engine must not re-apply the Choice Scarf multiplier to speed it is already given resolved");
        assertEquals(0, scoreOf(notScarfed, TARGET).opponentMatchBonus());
        assertFalse(scoreOf(notScarfed, TARGET).eligible(),
                "A double-counted scarf would have made this board eligible; verbatim speed keeps it ineligible");
    }

    @Test
    void b11_presetInventoryDeclaresAntiFastElectricLastWithASingleDefault() throws Exception {
        List<LeadAttempt> presets = BlaineLeadSelectionTest.loadBlainePresetsFromDatapack();

        assertEquals(5, presets.size(), "Blaine must declare exactly 5 authored presets");
        assertEquals(TARGET, presets.get(presets.size() - 1).id(),
                "anti_fast_electric must be declared last so pre-existing presets keep winning exact ties");
        assertEquals(1, presets.stream().filter(LeadAttempt::isDefault).count(),
                "Exactly one preset may declare default: true");
    }

    @Test
    void b12_expectedLeadMembersOfTheNewPresetMatchTheRealRosterIdentity() throws Exception {
        List<LeadAttempt> presets = BlaineLeadSelectionTest.loadBlainePresetsFromDatapack();
        LeadAttempt target = presets.stream()
                .filter(p -> TARGET.equals(p.id()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("anti_fast_electric preset must exist"));

        assertArrayEquals(new int[]{3, 4}, target.leadSlots());
        List<ExpectedLeadMember> expected = target.expectedLeadMembers();
        assertEquals(2, expected.size());

        List<PokemonIdentity> actualRoster = List.of(
                new PokemonIdentity("arcanine"),
                new PokemonIdentity("charizard", null, Set.of("mega_y")),
                new PokemonIdentity("moltres"),
                new PokemonIdentity("rillaboom"),
                new PokemonIdentity("golisopod", null, Set.of("mega")),
                new PokemonIdentity("zeraora"));

        assertTrue(expected.get(0).matches(actualRoster.get(target.leadSlots()[0])),
                "Rillaboom must match the real roster at slot 3");
        assertTrue(expected.get(1).matches(actualRoster.get(target.leadSlots()[1])),
                "Mega Golisopod (requiredAspects: [mega]) must match the real roster at slot 4");
    }

    /**
     * Live-canary reproduction of the eligibility-gate bug (regressions 1 and 7).
     * <p>
     * The canary logged {@code Selected=anti_fast_electric} with
     * {@code anti_fast_electric=4(off=4,def=0,bw=0,type=0,spec=0,match=0)} beating
     * {@code default_sun_intimidate=-9}. Because {@code match=0} the matcher never fired, so the preset
     * won purely on its structural offensive score. Neither Tyranitar (rock/dark) nor Garchomp
     * (dragon/ground) is Electric-typed, so the {@code type: electric} property is decisive at any speed;
     * the listed speeds are representative resolved values kept below the 210 reference.
     */
    @Test
    void b13_liveCanaryBoardTyranitarPlusGarchompIsNotWonByAnUnsatisfiedMatcher() throws Exception {
        LeadSelectionResult result = run(List.of(
                new PlayerLeadTyping("tyranitar", List.of("rock", "dark"), 81),
                new PlayerLeadTyping("garchomp", List.of("dragon", "ground"), 134)));

        AttemptScore target = scoreOf(result, TARGET);
        assertEquals(0, target.opponentMatchBonus(),
                "Live canary precondition: the matcher is unsatisfied on this board (match=0)");
        assertFalse(target.eligible(),
                "An unsatisfied matcher must make anti_fast_electric ineligible on the live canary board");
        assertNotEquals(TARGET, result.selectedAttempt().id(),
                "The live canary board must not select anti_fast_electric");
        assertTrue(scoreOf(result, result.selectedAttempt().id()).eligible(),
                "Whatever wins must itself be an eligible attempt");
    }

    /**
     * Real-config invariant (regression 6): on every pre-existing board the gate must be exactly
     * equivalent to the matcher outcome — ineligible if and only if the matcher awarded nothing — and it
     * must never disturb which preset wins.
     */
    @Test
    void b14_ineligibilityOnRealConfigIsExactlyTheUnsatisfiedMatcher() throws Exception {
        List<List<PlayerLeadTyping>> boards = List.of(
                List.of(pelipper(), regieleki()),
                List.of(pelipper(), lead("regieleki", List.of("electric"), 250)),
                List.of(pelipper(), lead("ampharos", List.of("electric"), 180, "electric")),
                List.of(pelipper(), lead("dragapult", List.of("dragon", "ghost"), 304, "electric")),
                List.of(lead("ampharos", List.of("electric"), 180, "electric"),
                        lead("dragapult", List.of("dragon", "ghost"), 304, "dragon")),
                List.of(new PlayerLeadTyping("corviknight", List.of("steel", "flying"), 67),
                        new PlayerLeadTyping("clefable", List.of("fairy"), 60)),
                List.of(new PlayerLeadTyping("miraidon", List.of("electric", "dragon"), 205),
                        new PlayerLeadTyping("ironhands", List.of("fighting", "electric"), 80)),
                // The live canary board: the only listed board where an ungated matcher actually wins.
                List.of(new PlayerLeadTyping("tyranitar", List.of("rock", "dark"), 81),
                        new PlayerLeadTyping("garchomp", List.of("dragon", "ground"), 134)));

        for (List<PlayerLeadTyping> leads : boards) {
            LeadSelectionResult result = run(leads);
            AttemptScore target = scoreOf(result, TARGET);
            boolean matched = target.opponentMatchBonus() > 0;
            assertEquals(matched, target.eligible(),
                    "Ineligibility must be exactly the unsatisfied matcher; board: "
                            + leads.stream().map(PlayerLeadTyping::species).toList());
            if (!matched) {
                assertNotEquals(TARGET, result.selectedAttempt().id(),
                        "An ineligible preset must not be selected on board: "
                                + leads.stream().map(PlayerLeadTyping::species).toList());
            }
        }
    }

    /**
     * Checkpoint-3 re-measurement of the plan's precedence calibration table against the real engine.
     * <p>
     * Rows 1–3 reproduce the plan's printed values. Row 4 is recorded here with the corrected winner cell:
     * structural 5 + bonus 14 = 19 (the plan printed 17, which is 5 + 12), and the margin over
     * {@code anti_water_ground} (4) is therefore 15, not 13.
     */
    @Test
    void checkpoint3_precedenceCalibrationMeasuredAgainstTheRealEngine() throws Exception {
        record Row(String name, List<PlayerLeadTyping> leads, int targetTotal, String runnerUp, int runnerUpTotal) {}

        List<Row> rows = List.of(
                new Row("pelipper + fast Electric", List.of(pelipper(), regieleki()), 8, "anti_rain_core", 5),
                new Row("barraskewda + fast Electric", List.of(
                        lead("barraskewda", List.of("water"), 250), regieleki()), 17, "anti_rain_core", 8),
                new Row("fast Electric + dragapult", List.of(
                        regieleki(), lead("dragapult", List.of("dragon", "ghost"), 304)), 11, "anti_fast_threats", 0),
                new Row("swampert + fast Electric", List.of(
                        lead("swampert", List.of("water", "ground"), 60), regieleki()), 19, "anti_water_ground", 4));

        for (Row row : rows) {
            LeadSelectionResult result = run(row.leads());
            assertEquals(TARGET, result.selectedAttempt().id(),
                    "Required board must be won by anti_fast_electric: " + row.name());
            assertEquals(row.targetTotal(), scoreOf(result, TARGET).totalScore(),
                    "anti_fast_electric totalScore on board: " + row.name());
            assertEquals(row.runnerUpTotal(), scoreOf(result, row.runnerUp()).totalScore(),
                    "Runner-up totalScore on board: " + row.name());
            int margin = scoreOf(result, TARGET).totalScore() - scoreOf(result, row.runnerUp()).totalScore();
            assertTrue(margin > 0, "Precedence must be strict on board: " + row.name());
        }
    }
}
