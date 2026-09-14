package com.cobbleverse.legendaryrule.mechanic;

import com.cobblemon.mod.common.api.battles.model.actor.AIBattleActor;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.battles.ActiveBattlePokemon;
import com.cobblemon.mod.common.battles.InBattleMove;
import com.cobblemon.mod.common.battles.MoveActionResponse;
import com.cobblemon.mod.common.battles.MoveTarget;
import com.cobblemon.mod.common.battles.ShowdownActionRequest;
import com.cobblemon.mod.common.battles.ShowdownActionResponse;
import com.cobblemon.mod.common.battles.ShowdownMoveset;
import com.cobblemon.mod.common.battles.Targetable;
import com.cobblemon.mod.common.exception.IllegalActionChoiceException;
import kotlin.Pair;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

public class SideMechanicValidator {
    private static final Logger LOGGER = LoggerFactory.getLogger(SideMechanicValidator.class);

    public static void validateAndSanitizeResponses(
            BattleActor actor,
            UUID battleId,
            int sideIndex,
            List<? extends ShowdownActionResponse> responses
    ) throws IllegalActionChoiceException {
        if (actor == null || responses == null || responses.isEmpty() || battleId == null) {
            return;
        }

        boolean isAI = (actor instanceof AIBattleActor);
        List<ActiveBattlePokemon> activeMonList = actor.getActivePokemon();
        ShowdownActionRequest request = actor.getRequest();
        List<ShowdownMoveset> movesetList = (request != null) ? request.getActive() : null;

        boolean isSideLocked = BattleSideMechanicTracker.isMechanicLocked(battleId, sideIndex);
        MajorBattleMechanic pendingMechanicInBatch = null;

        for (int index = 0; index < responses.size(); index++) {
            ShowdownActionResponse response = responses.get(index);
            if (!(response instanceof MoveActionResponse moveResp)) {
                continue;
            }

            String rawGimmick = moveResp.getGimmickID();
            MajorBattleMechanic requestedMechanic = MajorBattleMechanic.fromGimmickId(rawGimmick);
            if (requestedMechanic == null) {
                continue;
            }

            ActiveBattlePokemon activeMon = (activeMonList != null && index < activeMonList.size())
                    ? activeMonList.get(index) : null;
            ShowdownMoveset moveset = (movesetList != null && index < movesetList.size())
                    ? movesetList.get(index) : null;

            boolean isViolation = isSideLocked || (pendingMechanicInBatch != null);

            if (isViolation) {
                if (!isAI) {
                    throw new IllegalActionChoiceException(
                            actor,
                            "Chỉ được kích hoạt tối đa 1 Major Battle Mechanic mỗi bên trong một trận đấu!"
                    );
                } else {
                    demoteToSafeBaseMove(moveResp, activeMon, moveset);
                    LOGGER.warn("[SideMechanicValidator] AI on side {} attempted illegal mechanic {} (locked={}, batchPending={}). Demoted to base move {}.",
                            sideIndex, requestedMechanic, isSideLocked, pendingMechanicInBatch, moveResp.getMoveName());
                }
            } else {
                pendingMechanicInBatch = requestedMechanic;
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static void demoteToSafeBaseMove(
            MoveActionResponse moveResp,
            @Nullable ActiveBattlePokemon activeMon,
            @Nullable ShowdownMoveset moveset
    ) {
        if (moveResp == null) {
            return;
        }

        // 1. Remove gimmick ID
        moveResp.setGimmickID(null);

        // 2. Preserve original moveName; do not perform speculative fallback
        if (moveset == null || moveset.getMoves() == null) {
            return;
        }

        InBattleMove baseMove = null;
        for (InBattleMove m : moveset.getMoves()) {
            if (m.getId().equals(moveResp.getMoveName())) {
                baseMove = m;
                break;
            }
        }
        if (baseMove == null) {
            return;
        }

        // 3. Normalize targeting based on base move target definition
        MoveTarget target = baseMove.getTarget();
        if (isSpreadOrUntargeted(target)) {
            // Spread or self/side move: clear single-target pnx from Max Move selection
            moveResp.setTargetPnx(null);
            return;
        }

        if (activeMon == null) {
            return;
        }

        List<Targetable> availableTargets = null;
        if (target != null && target.getTargetList() != null) {
            try {
                availableTargets = (List<Targetable>) target.getTargetList().invoke(activeMon);
            } catch (Exception ignored) {
            }
        }

        if (availableTargets == null || availableTargets.isEmpty()) {
            moveResp.setTargetPnx(null);
        } else {
            // Targeted move: ensure targetPnx points to a valid target in availableTargets
            boolean valid = false;
            String currentPnx = moveResp.getTargetPnx();
            if (currentPnx != null && activeMon.getActor() != null && activeMon.getActor().getBattle() != null) {
                Pair<?, ActiveBattlePokemon> pair = activeMon.getActor().getBattle().getActorAndActiveSlotFromPNX(currentPnx);
                if (pair != null && pair.getSecond() != null && availableTargets.contains(pair.getSecond())) {
                    valid = true;
                }
            }
            if (!valid) {
                Targetable fallbackTarget = availableTargets.get(0);
                if (fallbackTarget instanceof ActiveBattlePokemon targetMon) {
                    moveResp.setTargetPnx(targetMon.getPNX());
                } else {
                    moveResp.setTargetPnx(null);
                }
            }
        }
    }

    private static boolean isSpreadOrUntargeted(@Nullable MoveTarget target) {
        if (target == null) {
            return false;
        }
        return target == MoveTarget.allAdjacentFoes
                || target == MoveTarget.allAdjacent
                || target == MoveTarget.all
                || target == MoveTarget.self
                || target == MoveTarget.randomNormal
                || target == MoveTarget.allies
                || target == MoveTarget.allySide
                || target == MoveTarget.allyTeam
                || target == MoveTarget.foeSide
                || target == MoveTarget.scripted;
    }
}
