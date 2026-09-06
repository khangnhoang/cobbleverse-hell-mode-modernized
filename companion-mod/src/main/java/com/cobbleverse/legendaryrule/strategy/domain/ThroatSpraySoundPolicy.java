package com.cobbleverse.legendaryrule.strategy.domain;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Generic domain policy guaranteeing that a designated ace
 * holding Throat Spray executes a sound-based spread move (Overdrive)
 * on each eligible decision while Throat Spray remains unconsumed,
 * setting up a +1 SpA boost through Showdown resolution.
 * Zero external dependencies.
 */
public class ThroatSpraySoundPolicy implements BattleStrategyPolicy {
    private final String targetPokemonShowdownId;
    private final String targetHeldItemShowdownId;
    private final String soundMoveName;

    public ThroatSpraySoundPolicy(String targetPokemonShowdownId, String targetHeldItemShowdownId, String soundMoveName) {
        this.targetPokemonShowdownId = Objects.requireNonNull(targetPokemonShowdownId, "targetPokemonShowdownId must not be null").trim().toLowerCase();
        this.targetHeldItemShowdownId = Objects.requireNonNull(targetHeldItemShowdownId, "targetHeldItemShowdownId must not be null").trim().toLowerCase();
        this.soundMoveName = Objects.requireNonNull(soundMoveName, "soundMoveName must not be null").trim().toLowerCase();
    }

    public static ThroatSpraySoundPolicy surgeToxtricity() {
        return new ThroatSpraySoundPolicy("toxtricitylowkey", "throatspray", "overdrive");
    }

    @Override
    public Optional<StrategicDecision> evaluate(BattleTurnContext context) {
        if (context == null) {
            return Optional.empty();
        }

        // 1. Active Pokémon match
        if (!targetPokemonShowdownId.equals(context.activePokemonShowdownId())) {
            return Optional.empty();
        }

        // 2. Held item check: MUST hold unconsumed Throat Spray
        if (!targetHeldItemShowdownId.equals(context.heldItemShowdownId())) {
            return Optional.empty();
        }

        // 3. Move availability & usability check
        StrategicMoveContext soundMove = null;
        for (StrategicMoveContext move : context.moves()) {
            if (soundMoveName.equals(move.name())) {
                soundMove = move;
                break;
            }
        }
        if (soundMove == null || !soundMove.usable()) {
            return Optional.empty();
        }

        // 4. Board viability primitive:
        // Must have at least one active opponent that is NOT confirmed immune
        List<StrategicOpponentContext> opponents = context.activeOpponents();
        if (opponents.isEmpty()) {
            return Optional.empty();
        }

        boolean hasVulnerableOpponent = false;
        for (StrategicOpponentContext opp : opponents) {
            if (opp.active() && !opp.confirmedImmune()) {
                hasVulnerableOpponent = true;
                break;
            }
        }

        if (!hasVulnerableOpponent) {
            // All active opponents are confirmed immune to the preferred move
            // Strategy aborts to let Run & Bun choose a coverage move
            return Optional.empty();
        }

        // 5. Valid strategy decision: Spread move with targetPnx = null
        return Optional.of(StrategicDecision.rewriteSpread(soundMoveName));
    }

    public String getTargetPokemonShowdownId() {
        return targetPokemonShowdownId;
    }

    public String getTargetHeldItemShowdownId() {
        return targetHeldItemShowdownId;
    }

    public String getSoundMoveName() {
        return soundMoveName;
    }
}
