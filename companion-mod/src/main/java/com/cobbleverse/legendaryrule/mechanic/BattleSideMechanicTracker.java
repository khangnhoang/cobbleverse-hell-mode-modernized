package com.cobbleverse.legendaryrule.mechanic;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BattleSideMechanicTracker {
    private static final Logger LOGGER = LoggerFactory.getLogger(BattleSideMechanicTracker.class);
    private static final Map<UUID, MajorBattleMechanic[]> BATTLE_MECHANICS = new ConcurrentHashMap<>();

    public static boolean isMechanicLocked(UUID battleId, int sideIndex) {
        if (battleId == null || sideIndex < 0 || sideIndex > 1) {
            return false;
        }
        MajorBattleMechanic[] sides = BATTLE_MECHANICS.get(battleId);
        return sides != null && sides[sideIndex] != null;
    }

    @Nullable
    public static MajorBattleMechanic getUsedMechanic(UUID battleId, int sideIndex) {
        if (battleId == null || sideIndex < 0 || sideIndex > 1) {
            return null;
        }
        MajorBattleMechanic[] sides = BATTLE_MECHANICS.get(battleId);
        return sides != null ? sides[sideIndex] : null;
    }

    public static synchronized void commitMechanic(UUID battleId, int sideIndex, MajorBattleMechanic mechanic) {
        if (battleId == null || mechanic == null || sideIndex < 0 || sideIndex > 1) {
            return;
        }
        MajorBattleMechanic[] sides = BATTLE_MECHANICS.computeIfAbsent(battleId, k -> new MajorBattleMechanic[2]);
        MajorBattleMechanic current = sides[sideIndex];
        if (current == null) {
            sides[sideIndex] = mechanic;
            LOGGER.info("[BattleSideMechanicTracker] Committed {} for battle {} side {}", mechanic, battleId, sideIndex);
        } else if (current == mechanic) {
            // Idempotent no-op
        } else {
            // Write-once invariant: A different later mechanic must never overwrite the original committed mechanic
            LOGGER.warn("[BattleSideMechanicTracker] Ignored attempt to overwrite committed {} with {} for battle {} side {}",
                    current, mechanic, battleId, sideIndex);
        }
    }

    public static void clearBattle(UUID battleId) {
        if (battleId == null) {
            return;
        }
        BATTLE_MECHANICS.remove(battleId);
        LOGGER.debug("[BattleSideMechanicTracker] Cleared battle {}", battleId);
    }
}
