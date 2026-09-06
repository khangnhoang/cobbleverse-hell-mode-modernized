package com.cobbleverse.legendaryrule.strategy.domain;

import java.util.Objects;

/**
 * Pure domain representation of a strategic move rewrite decision.
 * Zero external dependencies.
 */
public record StrategicDecision(
    String moveName,
    String targetPnx
) {
    public StrategicDecision {
        moveName = Objects.requireNonNull(moveName, "moveName must not be null").trim().toLowerCase();
    }

    public static StrategicDecision rewriteSpread(String moveName) {
        return new StrategicDecision(moveName, null);
    }
}
