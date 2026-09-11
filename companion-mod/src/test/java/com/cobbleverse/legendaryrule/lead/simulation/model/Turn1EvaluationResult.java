package com.cobbleverse.legendaryrule.lead.simulation.model;

import java.util.List;
import java.util.Objects;

/**
 * Result of simulating Turn 1 for a player lead pair against a Koga lead preset.
 */
public record Turn1EvaluationResult(
        String playerPairName,
        String presetId,
        Turn1Verdict verdict,
        String summary,
        int kogaKnockoutsScored,
        int playerKnockoutsScored,
        int kogaCasualties,
        int playerCasualties,
        double minKogaHpRemainingPercent,
        double maxPlayerHpRemainingPercent,
        List<String> actionLog,
        Turn1BattleState finalState
) {
    public Turn1EvaluationResult {
        Objects.requireNonNull(playerPairName, "playerPairName must not be null");
        Objects.requireNonNull(presetId, "presetId must not be null");
        Objects.requireNonNull(verdict, "verdict must not be null");
        Objects.requireNonNull(summary, "summary must not be null");
        actionLog = List.copyOf(actionLog);
    }

    public int kogaKOs() {
        return kogaKnockoutsScored;
    }

    public int playerKOs() {
        return playerKnockoutsScored;
    }
}
