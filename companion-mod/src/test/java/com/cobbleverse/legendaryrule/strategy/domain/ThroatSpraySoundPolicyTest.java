package com.cobbleverse.legendaryrule.strategy.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ThroatSpraySoundPolicyTest {

    private final ThroatSpraySoundPolicy policy = ThroatSpraySoundPolicy.surgeToxtricity();

    private StrategicMoveContext createOverdrive(boolean usable) {
        return new StrategicMoveContext("overdrive", usable);
    }

    private StrategicMoveContext createSludgeBomb() {
        return new StrategicMoveContext("sludgebomb", true);
    }

    private StrategicOpponentContext createVulnerableOpponent(String id) {
        return new StrategicOpponentContext(id, true, false);
    }

    private StrategicOpponentContext createImmuneOpponent(String id) {
        return new StrategicOpponentContext(id, true, true);
    }

    @Test
    @DisplayName("T01: Target Pokémon + unused Throat Spray + usable Overdrive + vulnerable opponent -> rewrite to Overdrive")
    void testStandardSurgeToxtricitySuccess() {
        BattleTurnContext context = new BattleTurnContext(
            "toxtricitylowkey",
            "throatspray",
            List.of(createOverdrive(true), createSludgeBomb()),
            List.of(createVulnerableOpponent("pelipper"), createVulnerableOpponent("gyarados")),
            true
        );

        Optional<StrategicDecision> decision = policy.evaluate(context);
        assertTrue(decision.isPresent(), "Strategy should produce a decision for valid context");
        assertEquals("overdrive", decision.get().moveName());
        assertNull(decision.get().targetPnx(), "Spread move must have null targetPnx");
    }

    @Test
    @DisplayName("T02: Throat Spray consumed (heldItem = null or other item) -> returns empty")
    void testThroatSprayConsumedReturnsEmpty() {
        // null item (consumed)
        BattleTurnContext contextNull = new BattleTurnContext(
            "toxtricitylowkey",
            null,
            List.of(createOverdrive(true)),
            List.of(createVulnerableOpponent("pelipper")),
            true
        );
        assertTrue(policy.evaluate(contextNull).isEmpty(), "Consumed spray must return empty");

        // different item (e.g. choicespecs)
        BattleTurnContext contextDifferent = new BattleTurnContext(
            "toxtricitylowkey",
            "choicespecs",
            List.of(createOverdrive(true)),
            List.of(createVulnerableOpponent("pelipper")),
            true
        );
        assertTrue(policy.evaluate(contextDifferent).isEmpty(), "Non-Throat Spray item must return empty");
    }

    @Test
    @DisplayName("T03: Non-target Pokémon -> returns empty")
    void testNonToxtricityPokemonReturnsEmpty() {
        BattleTurnContext context = new BattleTurnContext(
            "pincurchin",
            "throatspray",
            List.of(createOverdrive(true)),
            List.of(createVulnerableOpponent("pelipper")),
            true
        );
        assertTrue(policy.evaluate(context).isEmpty(), "Pincurchin should not trigger Toxtricity policy");
    }

    @Test
    @DisplayName("T04: Overdrive unusable (usable = false or missing from moveset) -> returns empty")
    void testOverdriveUnusableReturnsEmpty() {
        // Missing from moveset
        BattleTurnContext contextMissing = new BattleTurnContext(
            "toxtricitylowkey",
            "throatspray",
            List.of(createSludgeBomb()),
            List.of(createVulnerableOpponent("pelipper")),
            true
        );
        assertTrue(policy.evaluate(contextMissing).isEmpty(), "Missing move must return empty");

        // Legally unusable (e.g. 0 PP, disabled, lock states)
        BattleTurnContext contextUnusable = new BattleTurnContext(
            "toxtricitylowkey",
            "throatspray",
            List.of(createOverdrive(false)),
            List.of(createVulnerableOpponent("pelipper")),
            true
        );
        assertTrue(policy.evaluate(contextUnusable).isEmpty(), "Unusable move must return empty");
    }

    @Test
    @DisplayName("T05: All active opponents confirmedImmune = true -> returns empty")
    void testAllOpponentsImmuneReturnsEmpty() {
        BattleTurnContext contextAllImmune = new BattleTurnContext(
            "toxtricitylowkey",
            "throatspray",
            List.of(createOverdrive(true)),
            List.of(createImmuneOpponent("dugtrio"), createImmuneOpponent("nidoking")),
            true
        );
        assertTrue(policy.evaluate(contextAllImmune).isEmpty(), "All immune opponents must return empty");
    }

    @Test
    @DisplayName("T06: One opponent confirmedImmune = true, one confirmedImmune = false -> returns Overdrive decision")
    void testPartialImmunityProducesOverdriveDecision() {
        BattleTurnContext context = new BattleTurnContext(
            "toxtricitylowkey",
            "throatspray",
            List.of(createOverdrive(true)),
            List.of(createImmuneOpponent("dugtrio"), createVulnerableOpponent("pelipper")),
            true
        );

        Optional<StrategicDecision> decision = policy.evaluate(context);
        assertTrue(decision.isPresent(), "Partial immunity should still trigger spread Overdrive for vulnerable foe");
        assertEquals("overdrive", decision.get().moveName());
        assertNull(decision.get().targetPnx());
    }

    @Test
    @DisplayName("T07: Empty opponents list or null context -> returns empty")
    void testEdgeCases() {
        assertTrue(policy.evaluate(null).isEmpty(), "Null context must return empty");

        BattleTurnContext contextNoOpponents = new BattleTurnContext(
            "toxtricitylowkey",
            "throatspray",
            List.of(createOverdrive(true)),
            List.of(),
            true
        );
        assertTrue(policy.evaluate(contextNoOpponents).isEmpty(), "Empty opponents list must return empty");
    }
}
