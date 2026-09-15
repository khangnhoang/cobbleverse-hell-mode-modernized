package com.cobbleverse.legendaryrule.lead;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Generic regressions for the {@link OpponentMatch} per-opponent conjunction primitive (G1–G12).
 * <p>
 * The roster is declared explicitly in this file so no assertion depends on datapack drift.
 */
class OpponentMatchTest {

    private static LeadSelectionEngine engine;

    @BeforeAll
    static void setUp() {
        TypeChartData data = TypeChartResourceLoader.load().orElseThrow(
                () -> new IllegalStateException("Failed to load canonical Gen 9 type chart for tests"));
        engine = new LeadSelectionEngine(new TypeMatchupScorer(data));
    }

    /** slot 0 speed 194, slot 1 speed 210 (the "faster" reference), slot 2 speed 0 (degenerate), slot 3 speed 120. */
    private static List<RosterMemberTyping> roster() {
        return List.of(
                new RosterMemberTyping(0, "npc_slot0", List.of("fire"), 194),
                new RosterMemberTyping(1, "npc_slot1", List.of("fire", "flying"), 210),
                new RosterMemberTyping(2, "npc_slot2", List.of("grass"), 0),
                new RosterMemberTyping(3, "npc_slot3", List.of("water"), 120)
        );
    }

    private static LeadAttempt matcherAttempt(String id, int[] slots, OpponentMatch match) {
        return new LeadAttempt(id, slots, 0, List.of(), "", List.of(), List.of(), 0, 0, false, match);
    }

    private static LeadAttempt plainAttempt(String id, int[] slots) {
        return new LeadAttempt(id, slots, 0, List.of(), "");
    }

    private static AttemptScore scoreOf(LeadSelectionResult result, String id) {
        return result.evaluatedScores().stream()
                .filter(s -> s.attemptId().equals(id))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No evidence recorded for attempt " + id));
    }

    @Test
    void g1_typeOnlyPropertyAwardsAuthoredBonus() {
        OpponentMatch match = new OpponentMatch("water", null, null, 5);
        List<PlayerLeadTyping> leads = List.of(
                new PlayerLeadTyping("pelipper", List.of("water", "flying"), 65));

        LeadSelectionResult result = engine.select(List.of(matcherAttempt("m_type", new int[]{0, 1}, match)), leads, roster());

        assertEquals(5, scoreOf(result, "m_type").opponentMatchBonus(),
                "A satisfied type-only matcher must award exactly the authored bonus");
    }

    @Test
    void g2_damagingMoveTypeOnlyPropertyAwardsAuthoredBonus() {
        OpponentMatch match = new OpponentMatch(null, "ground", null, 6);
        List<PlayerLeadTyping> leads = List.of(
                new PlayerLeadTyping("gastrodon", List.of("water", "ground"), 39, List.of("ground")));

        LeadSelectionResult result = engine.select(List.of(matcherAttempt("m_dmg", new int[]{0, 1}, match)), leads, roster());

        assertEquals(6, scoreOf(result, "m_dmg").opponentMatchBonus());
    }

    @Test
    void g3_fasterThanRosterSlotOnlyPropertyAwardsAuthoredBonus() {
        OpponentMatch match = new OpponentMatch(null, null, 1, 7);
        List<PlayerLeadTyping> leads = List.of(
                new PlayerLeadTyping("regieleki", List.of("electric"), 250));

        LeadSelectionResult result = engine.select(List.of(matcherAttempt("m_speed", new int[]{0, 1}, match)), leads, roster());

        assertEquals(7, scoreOf(result, "m_speed").opponentMatchBonus());
    }

    @Test
    void g4_allThreePropertiesOnOneOpponentAwardBonusExactlyOnce() {
        OpponentMatch match = new OpponentMatch("electric", "electric", 1, 11);
        // Both opposing leads satisfy all three properties; the break must still yield a single bonus.
        List<PlayerLeadTyping> leads = List.of(
                new PlayerLeadTyping("regieleki", List.of("electric"), 250, List.of("electric")),
                new PlayerLeadTyping("zeraora", List.of("electric"), 298, List.of("electric")));

        LeadSelectionResult result = engine.select(List.of(matcherAttempt("m_all", new int[]{0, 1}, match)), leads, roster());

        assertEquals(11, scoreOf(result, "m_all").opponentMatchBonus(),
                "The exists-quantifier must award the bonus once, never once per qualifying lead");
    }

    @Test
    void g5_splitConditionFalsePositiveDoesNotAwardBonus() {
        OpponentMatch match = new OpponentMatch("electric", "electric", 1, 13);
        // Opponent A is Electric with a damaging Electric move but is slower than roster slot 1 (210).
        // Opponent B is faster than 210 but is not Electric. No single lead satisfies the conjunction.
        List<PlayerLeadTyping> leads = List.of(
                new PlayerLeadTyping("ampharos", List.of("electric"), 180, List.of("electric")),
                new PlayerLeadTyping("aerodactyl", List.of("rock", "flying"), 230, List.of("rock")));

        LeadSelectionResult result = engine.select(List.of(matcherAttempt("m_split", new int[]{0, 1}, match)), leads, roster());

        assertEquals(0, scoreOf(result, "m_split").opponentMatchBonus(),
                "Split conditions distributed across two different opposing leads must NOT satisfy the conjunction");
    }

    @Test
    void g6_statusMoveOfTheRequestedTypeDoesNotSatisfyDamagingMoveType() {
        OpponentMatch match = new OpponentMatch(null, "electric", null, 9);
        // The only Electric source on this board is a status move, so the adapter reports no damaging Electric type.
        List<PlayerLeadTyping> leads = List.of(
                new PlayerLeadTyping("ampharos", List.of("electric"), 180, List.of()));

        LeadSelectionResult result = engine.select(List.of(matcherAttempt("m_status", new int[]{0, 1}, match)), leads, roster());

        assertEquals(0, scoreOf(result, "m_status").opponentMatchBonus());
    }

    @Test
    void g7_absentReferencedRosterSlotMakesPropertyUnsatisfiable() {
        OpponentMatch match = new OpponentMatch(null, null, 9, 9);
        List<PlayerLeadTyping> leads = List.of(
                new PlayerLeadTyping("regieleki", List.of("electric"), 999));

        LeadSelectionResult result = engine.select(List.of(matcherAttempt("m_absent", new int[]{0, 1}, match)), leads, roster());

        assertEquals(0, scoreOf(result, "m_absent").opponentMatchBonus(),
                "An absent referenced roster slot must make the property unsatisfiable, not vacuously true");
    }

    @Test
    void g8_zeroSpeedReferencedRosterSlotMakesPropertyUnsatisfiable() {
        OpponentMatch match = new OpponentMatch(null, null, 2, 9); // slot 2 resolves to speed 0
        List<PlayerLeadTyping> leads = List.of(
                new PlayerLeadTyping("regieleki", List.of("electric"), 999));

        LeadSelectionResult result = engine.select(List.of(matcherAttempt("m_zero", new int[]{0, 1}, match)), leads, roster());

        assertEquals(0, scoreOf(result, "m_zero").opponentMatchBonus(),
                "A referenced slot with non-positive speed must make the property unsatisfiable");
    }

    @Test
    void g9_equalSpeedDoesNotSatisfyStrictInequality() {
        OpponentMatch match = new OpponentMatch(null, null, 1, 9); // slot 1 resolves to speed 210
        List<PlayerLeadTyping> leads = List.of(
                new PlayerLeadTyping("tie_lead", List.of("normal"), 210));

        LeadSelectionResult result = engine.select(List.of(matcherAttempt("m_tie", new int[]{0, 1}, match)), leads, roster());

        assertEquals(0, scoreOf(result, "m_tie").opponentMatchBonus(),
                "Speed comparison must be strictly greater-than");
    }

    @Test
    void g10_absentMatcherIsInertAndLeavesExistingComponentsUnchanged() {
        LeadAttempt plain = plainAttempt("plain", new int[]{0, 1});
        List<PlayerLeadTyping> leads = List.of(
                new PlayerLeadTyping("pelipper", List.of("water", "flying"), 65));

        LeadSelectionResult result = engine.select(List.of(plain), leads, roster());
        AttemptScore score = scoreOf(result, "plain");

        assertEquals(0, score.opponentMatchBonus());
        assertNull(result.selectedAttempt().opponentMatch(), "An attempt without opponentMatch must carry a null matcher");
        assertEquals(
                score.offensiveScore() + score.defensiveScore() + score.baseWeight()
                        + score.typeFavoredBonus() + score.speciesFavoredBonus() + score.fastBonus()
                        + score.opponentMatchBonus(),
                score.totalScore(),
                "totalScore must remain the plain sum of every component");
    }

    @Test
    void g11_satisfiedMatcherWinsStrictlyOverIdenticalMatcherlessPreset() {
        OpponentMatch match = new OpponentMatch("water", null, null, 5);
        LeadAttempt withoutMatcher = plainAttempt("without_matcher", new int[]{0, 1});
        LeadAttempt withMatcher = matcherAttempt("with_matcher", new int[]{0, 1}, match);
        List<PlayerLeadTyping> leads = List.of(
                new PlayerLeadTyping("pelipper", List.of("water", "flying"), 65));

        LeadSelectionResult result = engine.select(List.of(withoutMatcher, withMatcher), leads, roster());

        assertEquals("with_matcher", result.selectedAttempt().id(),
                "Identical presets differing only by a satisfied matcher must separate strictly on totalScore");
    }

    @Test
    void g12_defaultResolutionRule2SkipsMatcherBearingPresetInFavourOfConditionlessSuccessor() {
        List<RosterMemberTyping> g12Roster = List.of(
                new RosterMemberTyping(0, "fast0", List.of("fire"), 400),
                new RosterMemberTyping(1, "fast1", List.of("fire"), 350),
                new RosterMemberTyping(2, "slow2", List.of("fire"), 200),
                new RosterMemberTyping(3, "slow3", List.of("fire"), 150));

        OpponentMatch satisfied = new OpponentMatch("fire", null, null, 5);
        // Declared FIRST and carrying no condition other than a satisfied matcher: without the rule-2 guard
        // this attempt would be mis-resolved as "unconditional" and would supply dynamicThreatSpeed = 400.
        LeadAttempt matcherBearingFirst = matcherAttempt("matcher_bearing_first", new int[]{0, 1}, satisfied);
        // Declared behind it, genuinely conditionless: supplies dynamicThreatSpeed = max(200, 150) = 200.
        LeadAttempt conditionlessSecond = plainAttempt("conditionless_second", new int[]{2, 3});
        // Probe: observes dynamicThreatSpeed through the dynamic fast-threshold path.
        LeadAttempt probe = new LeadAttempt("probe", new int[]{0, 1}, 0, List.of(), "",
                List.of(), List.of(), 1, 0, false);

        // 300 > 200 (conditionless source) but NOT > 400 (matcher-bearing source).
        List<PlayerLeadTyping> leads = List.of(new PlayerLeadTyping("opp", List.of("fire"), 300));

        LeadSelectionResult result = engine.select(List.of(matcherBearingFirst, conditionlessSecond, probe), leads, g12Roster);

        assertEquals(5, scoreOf(result, "matcher_bearing_first").opponentMatchBonus(),
                "Precondition: the first attempt's matcher IS satisfied on this board");
        assertEquals(4, scoreOf(result, "probe").fastBonus(),
                "Rule 2 must resolve the conditionless successor, so dynamicThreatSpeed is 200 and the 300-speed lead is fast");
    }

    // ---------------------------------------------------------------------------------------------
    // Eligibility gate (G13–G19): an attempt carrying a matcher is a CONDITIONAL preset. While its
    // matcher is unsatisfied the attempt must not be selectable, and no structural score may rescue it.
    // ---------------------------------------------------------------------------------------------

    @Test
    void g13_unsatisfiedMatcherMakesTheAttemptIneligibleAndUnselectable() {
        OpponentMatch unsatisfied = new OpponentMatch("electric", "electric", 1, 13);
        LeadAttempt gated = matcherAttempt("gated", new int[]{0, 1}, unsatisfied);
        LeadAttempt unconditional = plainAttempt("unconditional", new int[]{0, 1});
        // Neither lead is Electric-typed, so the matcher is unsatisfied and awards nothing.
        List<PlayerLeadTyping> leads = List.of(
                new PlayerLeadTyping("tyranitar", List.of("rock", "dark"), 81),
                new PlayerLeadTyping("garchomp", List.of("dragon", "ground"), 134));

        LeadSelectionResult result = engine.select(List.of(gated, unconditional), leads, roster());

        assertFalse(scoreOf(result, "gated").eligible(),
                "A matcher-bearing attempt whose matcher is unsatisfied must be ineligible");
        assertTrue(scoreOf(result, "unconditional").eligible(),
                "An attempt with no matcher is unconditional and stays eligible");
        assertEquals("unconditional", result.selectedAttempt().id(),
                "The ineligible attempt must not be selected even though it is declared first");
    }

    @Test
    void g14_satisfiedMatcherKeepsTheAttemptEligibleAndStillAwardsTheAuthoredBonus() {
        // Regression 2: the positive board the preset was authored for must keep winning.
        OpponentMatch satisfied = new OpponentMatch("electric", "electric", 1, 11);
        LeadAttempt gated = matcherAttempt("gated", new int[]{0, 1}, satisfied);
        LeadAttempt unconditional = new LeadAttempt("unconditional", new int[]{0, 1}, 2, List.of(), "");
        List<PlayerLeadTyping> leads = List.of(
                new PlayerLeadTyping("pelipper", List.of("water", "flying"), 65),
                new PlayerLeadTyping("regieleki", List.of("electric"), 250, List.of("electric")));

        LeadSelectionResult result = engine.select(List.of(unconditional, gated), leads, roster());

        AttemptScore gatedScore = scoreOf(result, "gated");
        assertTrue(gatedScore.eligible(), "A satisfied matcher must leave the attempt eligible");
        assertEquals(11, gatedScore.opponentMatchBonus(),
                "A satisfied matcher must still add exactly the authored bonus on top of the structural score");
        assertEquals("gated", result.selectedAttempt().id());
    }

    @Test
    void g15_splitConditionBoardStaysIneligible() {
        // Regression 3: Ampharos is Electric with a damaging Electric move but slower than 210;
        // Aerodactyl is faster than 210 but not Electric. No single lead satisfies the conjunction.
        OpponentMatch match = new OpponentMatch("electric", "electric", 1, 13);
        LeadAttempt gated = matcherAttempt("gated", new int[]{0, 1}, match);
        LeadAttempt unconditional = plainAttempt("unconditional", new int[]{0, 1});
        List<PlayerLeadTyping> leads = List.of(
                new PlayerLeadTyping("ampharos", List.of("electric"), 180, List.of("electric")),
                new PlayerLeadTyping("aerodactyl", List.of("rock", "flying"), 230, List.of("rock")));

        LeadSelectionResult result = engine.select(List.of(gated, unconditional), leads, roster());

        assertEquals(0, scoreOf(result, "gated").opponentMatchBonus());
        assertFalse(scoreOf(result, "gated").eligible());
        assertEquals("unconditional", result.selectedAttempt().id());
    }

    @Test
    void g16_equalSpeedBoundaryStaysIneligible() {
        // Regression 4: the strict > comparison against the 210 reference must not be relaxed by the gate.
        OpponentMatch match = new OpponentMatch("electric", null, 1, 9);
        LeadAttempt gated = matcherAttempt("gated", new int[]{0, 1}, match);
        LeadAttempt unconditional = plainAttempt("unconditional", new int[]{0, 1});
        List<PlayerLeadTyping> leads = List.of(
                new PlayerLeadTyping("tie_lead", List.of("electric"), 210, List.of("electric")));

        LeadSelectionResult result = engine.select(List.of(gated, unconditional), leads, roster());

        assertEquals(0, scoreOf(result, "gated").opponentMatchBonus());
        assertFalse(scoreOf(result, "gated").eligible());
        assertEquals("unconditional", result.selectedAttempt().id());
    }

    @Test
    void g17_unsatisfiedMatcherIsNotRescuedByAVeryHighStructuralScore() {
        // Regression 7 (load-bearing for the live bug): the gated attempt carries the maximum authored
        // baseWeight plus both favored-opponent bonuses, so it out-scores the matcherless attempt by a
        // deterministic margin that does not depend on the type chart. Both attempts still resolve their
        // offensive/defensive components from the same roster slots, so the gap is structural, not typing.
        OpponentMatch unsatisfied = new OpponentMatch("electric", "electric", 1, 16);
        LeadAttempt gated = new LeadAttempt("gated_high", new int[]{0, 1}, 2, List.of(), "",
                List.of("water"), List.of("pelipper"), 0, 0, false, unsatisfied);
        LeadAttempt plain = new LeadAttempt("plain_low", new int[]{0, 1}, -2, List.of(), "");
        List<PlayerLeadTyping> leads = List.of(
                new PlayerLeadTyping("pelipper", List.of("water", "flying"), 65));

        LeadSelectionResult result = engine.select(List.of(plain, gated), leads, roster());

        AttemptScore gatedScore = scoreOf(result, "gated_high");
        AttemptScore plainScore = scoreOf(result, "plain_low");

        assertFalse(gatedScore.eligible(), "Precondition: the matcher is unsatisfied on this board");
        assertEquals(0, gatedScore.opponentMatchBonus());
        assertTrue(gatedScore.totalScore() > plainScore.totalScore(),
                "Precondition: the gated attempt genuinely out-scores the matcherless attempt ("
                        + gatedScore.totalScore() + " vs " + plainScore.totalScore() + ")");
        assertEquals("plain_low", result.selectedAttempt().id(),
                "A very high structural score must not let an ineligible attempt win");
    }

    @Test
    void g18_matcherlessAttemptsKeepTheirExistingOrderingSemantics() {
        // Regression 6: with no matcher in play the tie-break chain is untouched, and every attempt
        // is eligible so the gate is inert.
        LeadAttempt lowWeight = new LeadAttempt("low_weight", new int[]{0, 1}, 0, List.of(), "");
        LeadAttempt highWeight = new LeadAttempt("high_weight", new int[]{0, 1}, 2, List.of(), "");
        List<PlayerLeadTyping> leads = List.of(
                new PlayerLeadTyping("pelipper", List.of("water", "flying"), 65));

        LeadSelectionResult result = engine.select(List.of(lowWeight, highWeight), leads, roster());

        assertEquals("high_weight", result.selectedAttempt().id(),
                "Identical matcherless presets must still separate on baseWeight descending");
        result.evaluatedScores().forEach(s -> assertTrue(s.eligible(),
                "No attempt carries a matcher, so every attempt must remain eligible: " + s.attemptId()));
    }

    @Test
    void g19_boardWhereEveryPresetIsGatedStillReturnsATotalEvidenceResult() {
        // The engine contract stays total: it always returns a winner plus complete evidence. When no
        // preset is eligible the evidence flags every attempt ineligible, which is the signal
        // LeadSelectionService uses to reject the selection and fall back to native ordering.
        OpponentMatch unsatisfied = new OpponentMatch("electric", null, null, 4);
        List<PlayerLeadTyping> leads = List.of(
                new PlayerLeadTyping("tyranitar", List.of("rock", "dark"), 81));

        LeadSelectionResult result = engine.select(List.of(
                matcherAttempt("gated_a", new int[]{0, 1}, unsatisfied),
                matcherAttempt("gated_b", new int[]{2, 3}, unsatisfied)), leads, roster());

        assertEquals(2, result.evaluatedScores().size(), "Every attempt must still be recorded as evidence");
        assertTrue(result.evaluatedScores().stream().noneMatch(AttemptScore::eligible),
                "No attempt on this board is eligible");
        assertNotNull(result.selectedAttempt(), "The engine must still return a total result");
    }

    // ---------------------------------------------------------------------------------------------
    // G20–G28: the generic `typeAnyOf` set primitive and the `slowerThanRosterSlot` symmetric
    // counterpart to `fasterThanRosterSlot`. Both are same-opponent conjuncts, both strict.
    // ---------------------------------------------------------------------------------------------

    @Test
    void g20_typeAnyOfMatchesAPokemonCarryingAtLeastOneOfTheListedTypes() {
        // The load-bearing property for the Blaine Water/Ground split: the set is satisfied by a
        // Pokémon that has EITHER type, not only by one that is dual Water/Ground.
        OpponentMatch anyOf = new OpponentMatch(null, List.of("water", "ground"), null, null, null, 7);

        LeadSelectionResult monoWater = engine.select(List.of(matcherAttempt("m_anyof", new int[]{0, 1}, anyOf)),
                List.of(new PlayerLeadTyping("blastoise", List.of("water"), 78)), roster());
        assertEquals(7, scoreOf(monoWater, "m_anyof").opponentMatchBonus(),
                "A mono-Water lead must satisfy typeAnyOf [water, ground]");

        LeadSelectionResult monoGround = engine.select(List.of(matcherAttempt("m_anyof", new int[]{0, 1}, anyOf)),
                List.of(new PlayerLeadTyping("sandslash", List.of("ground"), 65)), roster());
        assertEquals(7, scoreOf(monoGround, "m_anyof").opponentMatchBonus(),
                "A mono-Ground lead must satisfy typeAnyOf [water, ground]");

        LeadSelectionResult dual = engine.select(List.of(matcherAttempt("m_anyof", new int[]{0, 1}, anyOf)),
                List.of(new PlayerLeadTyping("swampert", List.of("water", "ground"), 60)), roster());
        assertEquals(7, scoreOf(dual, "m_anyof").opponentMatchBonus(),
                "A dual Water/Ground lead must satisfy typeAnyOf [water, ground]");

        LeadSelectionResult neither = engine.select(List.of(matcherAttempt("m_anyof", new int[]{0, 1}, anyOf)),
                List.of(new PlayerLeadTyping("scizor", List.of("bug", "steel"), 65)), roster());
        assertEquals(0, scoreOf(neither, "m_anyof").opponentMatchBonus(),
                "A lead with neither type must not satisfy typeAnyOf [water, ground]");
        assertFalse(scoreOf(neither, "m_anyof").eligible(),
                "An unsatisfied typeAnyOf matcher must leave the attempt ineligible");
    }

    @Test
    void g21_typeAnyOfIsCaseInsensitiveAndCanonicalisedLikeType() {
        OpponentMatch anyOf = new OpponentMatch(null, List.of("Water", "GROUND"), null, null, null, 7);
        assertEquals(List.of("water", "ground"), anyOf.typeAnyOf(),
                "typeAnyOf entries must be lower-cased and canonicalised like the singular type property");
    }

    @Test
    void g22_slowerThanRosterSlotUsesAStrictLessThanComparison() {
        OpponentMatch slower = new OpponentMatch(null, List.of("water"), null, null, 1, 6);

        // Reference is roster slot 1 at speed 210.
        LeadSelectionResult below = engine.select(List.of(matcherAttempt("m_slow", new int[]{0, 1}, slower)),
                List.of(new PlayerLeadTyping("slowbro", List.of("water"), 209)), roster());
        assertEquals(6, scoreOf(below, "m_slow").opponentMatchBonus(),
                "209 < 210 must satisfy slowerThanRosterSlot 1");

        LeadSelectionResult equal = engine.select(List.of(matcherAttempt("m_slow", new int[]{0, 1}, slower)),
                List.of(new PlayerLeadTyping("slowbro", List.of("water"), 210)), roster());
        assertEquals(0, scoreOf(equal, "m_slow").opponentMatchBonus(),
                "Speed equality must NOT satisfy the strict < comparison");
        assertFalse(scoreOf(equal, "m_slow").eligible(), "An equal-speed board must leave the attempt ineligible");

        LeadSelectionResult above = engine.select(List.of(matcherAttempt("m_slow", new int[]{0, 1}, slower)),
                List.of(new PlayerLeadTyping("barraskewda", List.of("water"), 211)), roster());
        assertEquals(0, scoreOf(above, "m_slow").opponentMatchBonus(),
                "211 > 210 must not satisfy slowerThanRosterSlot 1");
        assertFalse(scoreOf(above, "m_slow").eligible());
    }

    @Test
    void g23_slowerAndFasterThanRosterSlotAreExactMirrorsAroundTheReferenceSpeed() {
        OpponentMatch slower = new OpponentMatch(null, List.of("water"), null, null, 1, 6);
        OpponentMatch faster = new OpponentMatch(null, List.of("water"), null, 1, null, 6);

        for (int speed : List.of(120, 209, 210, 211, 304)) {
            List<PlayerLeadTyping> leads = List.of(new PlayerLeadTyping("subject", List.of("water"), speed));
            int slowBonus = scoreOf(engine.select(List.of(matcherAttempt("s", new int[]{0, 1}, slower)), leads, roster()), "s")
                    .opponentMatchBonus();
            int fastBonus = scoreOf(engine.select(List.of(matcherAttempt("f", new int[]{0, 1}, faster)), leads, roster()), "f")
                    .opponentMatchBonus();
            assertTrue(slowBonus == 0 || fastBonus == 0,
                    "No speed can be both strictly slower and strictly faster than 210; speed=" + speed);
            assertEquals(210 == speed ? 0 : 6, slowBonus + fastBonus,
                    "Exactly one side must fire for any non-equal speed; speed=" + speed);
        }
    }

    @Test
    void g24_typeAndSpeedConditionsMustHoldOnTheSameOpposingPokemon() {
        // A fast non-Water lead plus a slow Water lead must NOT satisfy typeAnyOf + slowerThan.
        OpponentMatch conjunction = new OpponentMatch(null, List.of("water", "ground"), null, null, 1, 9);

        LeadSelectionResult split = engine.select(List.of(matcherAttempt("m_conj", new int[]{0, 1}, conjunction)),
                List.of(new PlayerLeadTyping("dragapult", List.of("dragon", "ghost"), 304),
                        new PlayerLeadTyping("blastoise", List.of("water"), 78)), roster());
        assertEquals(9, scoreOf(split, "m_conj").opponentMatchBonus(),
                "blastoise alone satisfies both conjuncts (water AND 78 < 210), so the conjunction holds");

        LeadSelectionResult splitInverted = engine.select(List.of(matcherAttempt("m_conj", new int[]{0, 1}, conjunction)),
                List.of(new PlayerLeadTyping("barraskewda", List.of("water"), 250),
                        new PlayerLeadTyping("sandslash", List.of("ground"), 65)), roster());
        assertEquals(9, scoreOf(splitInverted, "m_conj").opponentMatchBonus(),
                "sandslash alone satisfies both conjuncts (ground AND 65 < 210)");

        LeadSelectionResult neither = engine.select(List.of(matcherAttempt("m_conj", new int[]{0, 1}, conjunction)),
                List.of(new PlayerLeadTyping("barraskewda", List.of("water"), 250),
                        new PlayerLeadTyping("dragapult", List.of("dragon", "ghost"), 304)), roster());
        assertEquals(0, scoreOf(neither, "m_conj").opponentMatchBonus(),
                "A fast Water lead and a slow non-Water lead must not satisfy the conjunction");
        assertFalse(scoreOf(neither, "m_conj").eligible());
    }

    @Test
    void g25_omittedSpeedPropertyLeavesSpeedUnconstrained() {
        OpponentMatch typeOnly = new OpponentMatch(null, List.of("water"), null, null, null, 4);
        for (int speed : List.of(1, 210, 999)) {
            LeadSelectionResult result = engine.select(List.of(matcherAttempt("m", new int[]{0, 1}, typeOnly)),
                    List.of(new PlayerLeadTyping("subject", List.of("water"), speed)), roster());
            assertEquals(4, scoreOf(result, "m").opponentMatchBonus(),
                    "An omitted speed property must not constrain the match; speed=" + speed);
        }
    }

    @Test
    void g26_degenerateSlowerReferenceSpeedIsUnsatisfiableRatherThanVacuouslyTrue() {
        // Roster slot 2 has speed 0; a strict < against a non-positive reference must never match.
        OpponentMatch slower = new OpponentMatch(null, List.of("water"), null, null, 2, 6);
        LeadSelectionResult result = engine.select(List.of(matcherAttempt("m", new int[]{0, 1}, slower)),
                List.of(new PlayerLeadTyping("subject", List.of("water"), 1)), roster());

        assertEquals(0, scoreOf(result, "m").opponentMatchBonus(),
                "A non-positive reference speed must make slowerThanRosterSlot unsatisfiable");
        assertFalse(scoreOf(result, "m").eligible());
    }

    @Test
    void g27_absentSlowerReferenceSlotIsUnsatisfiableRatherThanVacuouslyTrue() {
        OpponentMatch slower = new OpponentMatch(null, List.of("water"), null, null, 99, 6);
        LeadSelectionResult result = engine.select(List.of(matcherAttempt("m", new int[]{0, 1}, slower)),
                List.of(new PlayerLeadTyping("subject", List.of("water"), 1)), roster());

        assertEquals(0, scoreOf(result, "m").opponentMatchBonus(),
                "An absent referenced slot must make slowerThanRosterSlot unsatisfiable");
        assertFalse(scoreOf(result, "m").eligible());
    }

    @Test
    void g28_typeAnyOfCombinesWithDamagingMoveTypeOnTheSameOpponent() {
        OpponentMatch conjunction = new OpponentMatch(
                null, List.of("water", "ground"), "electric", null, null, 8);

        LeadSelectionResult satisfied = engine.select(List.of(matcherAttempt("m", new int[]{0, 1}, conjunction)),
                List.of(new PlayerLeadTyping("rotomwash", List.of("electric", "water"), 200, List.of("electric"))),
                roster());
        assertEquals(8, scoreOf(satisfied, "m").opponentMatchBonus(),
                "A Water lead carrying a damaging Electric move satisfies both conjuncts");

        LeadSelectionResult noMove = engine.select(List.of(matcherAttempt("m", new int[]{0, 1}, conjunction)),
                List.of(new PlayerLeadTyping("rotomwash", List.of("electric", "water"), 200)), roster());
        assertEquals(0, scoreOf(noMove, "m").opponentMatchBonus(),
                "Without the damaging move the conjunction fails");
        assertFalse(scoreOf(noMove, "m").eligible());
    }

    // ---------------------------------------------------------------------------------------------
    // G29–G32: constructor guards. These keep the authoring surface honest so an unsatisfiable or
    // ambiguous matcher cannot be authored by accident.
    // ---------------------------------------------------------------------------------------------

    @Test
    void g29_declaringBothTypeAndTypeAnyOfIsRejected() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new OpponentMatch("water", List.of("ground"), null, null, null, 4));
        assertTrue(ex.getMessage().contains("typeAnyOf"), "The rejection must name the conflicting keys");
    }

    @Test
    void g30_declaringBothSpeedReferencesIsRejected() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new OpponentMatch(null, List.of("water"), null, 1, 1, 4));
        assertTrue(ex.getMessage().contains("slowerThanRosterSlot"),
                "The rejection must name the conflicting speed references");
    }

    @Test
    void g31_anEntirelyEmptyMatcherIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new OpponentMatch(null, List.of(), null, null, null, 4),
                "A matcher with no condition at all would be vacuously true and must be rejected");
    }

    @Test
    void g32_negativeSpeedReferenceSlotsAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new OpponentMatch(null, List.of("water"), null, -1, null, 4));
        assertThrows(IllegalArgumentException.class,
                () -> new OpponentMatch(null, List.of("water"), null, null, -1, 4));
    }
}
