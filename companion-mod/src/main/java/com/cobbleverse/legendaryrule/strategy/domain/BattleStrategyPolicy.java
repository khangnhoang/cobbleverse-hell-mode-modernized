package com.cobbleverse.legendaryrule.strategy.domain;

import java.util.Optional;

/**
 * Interface for domain battle strategy policies.
 * Zero external dependencies.
 */
@FunctionalInterface
public interface BattleStrategyPolicy {
    Optional<StrategicDecision> evaluate(BattleTurnContext context);
}
