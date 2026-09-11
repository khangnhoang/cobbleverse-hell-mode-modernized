package com.cobbleverse.legendaryrule.lead.simulation.model;

import java.util.Objects;

/**
 * Immutable representation of an action declared or executed in Turn 1.
 */
public record Turn1Action(
        String actorSlot,
        MoveProfile move,
        String targetSlot,
        int priority,
        int effectiveSpeed
) {
    public Turn1Action {
        Objects.requireNonNull(actorSlot, "actorSlot must not be null");
        Objects.requireNonNull(move, "move must not be null");
        Objects.requireNonNull(targetSlot, "targetSlot must not be null");
    }

    public boolean isActorPlayer() {
        return actorSlot.startsWith("player");
    }

    public boolean isActorKoga() {
        return actorSlot.startsWith("koga");
    }
}
