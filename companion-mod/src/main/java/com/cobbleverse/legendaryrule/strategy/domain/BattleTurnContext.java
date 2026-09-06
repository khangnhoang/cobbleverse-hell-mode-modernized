package com.cobbleverse.legendaryrule.strategy.domain;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable domain snapshot of the current battle turn decision context.
 * Zero dependencies on Minecraft, Fabric, or Cobblemon.
 */
public record BattleTurnContext(
    String activePokemonShowdownId,
    String heldItemShowdownId,
    List<StrategicMoveContext> moves,
    List<StrategicOpponentContext> activeOpponents,
    boolean isDoubles
) {
    public BattleTurnContext {
        activePokemonShowdownId = Objects.requireNonNull(activePokemonShowdownId, "activePokemonShowdownId must not be null").trim().toLowerCase();
        heldItemShowdownId = heldItemShowdownId != null && !heldItemShowdownId.isBlank() ? heldItemShowdownId.trim().toLowerCase() : null;
        moves = moves != null ? Collections.unmodifiableList(moves) : Collections.emptyList();
        activeOpponents = activeOpponents != null ? Collections.unmodifiableList(activeOpponents) : Collections.emptyList();
    }
}
