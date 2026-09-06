package com.cobbleverse.legendaryrule.strategy.domain;

import java.util.Objects;

/**
 * Pure domain representation of a move available in the current turn.
 * Zero external dependencies.
 */
public record StrategicMoveContext(
    String name,
    boolean usable
) {
    public StrategicMoveContext {
        name = Objects.requireNonNull(name, "name must not be null").trim().toLowerCase();
    }
}
