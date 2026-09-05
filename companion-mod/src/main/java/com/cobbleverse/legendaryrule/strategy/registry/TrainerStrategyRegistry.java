package com.cobbleverse.legendaryrule.strategy.registry;

import com.cobbleverse.legendaryrule.strategy.domain.BattleStrategyPolicy;
import com.cobbleverse.legendaryrule.strategy.domain.ThroatSpraySoundPolicy;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry mapping trainer encounter identifiers (e.g. "kanto_ltsurge")
 * to their respective BattleStrategyPolicy.
 */
public final class TrainerStrategyRegistry {
    private static final Map<String, BattleStrategyPolicy> STRATEGIES = new ConcurrentHashMap<>();

    static {
        STRATEGIES.put("kanto_ltsurge", ThroatSpraySoundPolicy.surgeToxtricity());
    }

    private TrainerStrategyRegistry() {}

    public static Optional<BattleStrategyPolicy> getPolicy(String trainerId) {
        if (trainerId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(STRATEGIES.get(trainerId.trim().toLowerCase()));
    }

    public static void register(String trainerId, BattleStrategyPolicy policy) {
        if (trainerId != null && policy != null) {
            STRATEGIES.put(trainerId.trim().toLowerCase(), policy);
        }
    }

    public static void reset() {
        STRATEGIES.clear();
        STRATEGIES.put("kanto_ltsurge", ThroatSpraySoundPolicy.surgeToxtricity());
    }
}
