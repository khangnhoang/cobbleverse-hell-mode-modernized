package com.cobbleverse.legendaryrule.strategy.domain;

import java.util.Objects;

/**
 * Pure domain representation of an active opponent on the battlefield.
 * Evaluates confirmed immunity exclusively via boolean flag resolved by adapter.
 * Zero external dependencies.
 */
public record StrategicOpponentContext(
    String showdownId,
    boolean active,
    boolean confirmedImmune
) {
    public StrategicOpponentContext {
        showdownId = Objects.requireNonNull(showdownId, "showdownId must not be null").trim().toLowerCase();
    }
}
