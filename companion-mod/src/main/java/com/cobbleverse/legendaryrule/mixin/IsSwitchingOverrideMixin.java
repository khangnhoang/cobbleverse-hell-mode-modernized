package com.cobbleverse.legendaryrule.mixin;

import com.cobblemon.mod.common.battles.ActiveBattlePokemon;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobbleverse.legendaryrule.strategy.switchai.DeadMatchupDetector;
import com.gitlab.surilexa.rbrctai.api.ai.RunBunAI;
import com.gitlab.surilexa.rbrctai.api.ai.utils.RBStatStages;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.List;
import java.util.Random;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Phase 1 Mixin into RunBunAI.isSwitching:
 * Hook 1: Relaxes Gate 1 score veto so non-damaging status moves scoring >= 6 do not veto switching.
 * Hook 2: Adjusts Gate 2 hasLowScore to resolve status-move holes (FIX A) and critical threat with low pressure (FIX B).
 * Hook 3: Bypasses Gate 3 random veto only when active Pokemon has low offensive pressure (< 20%),
 *         ensuring dead matchups proceed to HP (Gate 4) and party survivability (Gate 5) safety checks.
 * All other native safety gates (HP > 50%, party survivability traversal) remain completely intact.
 */
@Mixin(value = RunBunAI.class, remap = false)
public abstract class IsSwitchingOverrideMixin {

    /**
     * Hook 1: Wraps Stream.anyMatch(Predicate) at bytecode offset 11.
     * Replaces predicate s -> s.score >= 6 with eval.score >= 6 && eval.damage > 0.
     * Status moves with damage == 0 will not match and will not veto switching.
     */
    @WrapOperation(
        method = "isSwitching(Ljava/util/List;Ljava/util/List;Lcom/cobblemon/mod/common/battles/pokemon/BattlePokemon;Ljava/util/List;Lcom/cobblemon/mod/common/battles/ActiveBattlePokemon;Lcom/gitlab/surilexa/rbrctai/api/ai/utils/RBStatStages;Z)Z",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/stream/Stream;anyMatch(Ljava/util/function/Predicate;)Z"
        ),
        require = 1,
        remap = false
    )
    private static boolean cobbleverse$relaxScoreVeto(
        Stream<RunBunAI.MoveEvaluation> stream,
        Predicate<RunBunAI.MoveEvaluation> nativePredicate,
        Operation<Boolean> original
    ) {
        return original.call(stream, (Predicate<RunBunAI.MoveEvaluation>) DeadMatchupDetector::isMeaningfulOffensiveMove);
    }

    /**
     * Hook 2: Modifies hasLowScore at bytecode offset 82 (istore 12).
     * Note: Does NOT use ordinal = 0 to avoid matching isDoubles (slot 6, ordinal 0).
     * Disambiguates explicitly via name = "hasLowScore" and index = 12.
     *
     * Reconciled Contract:
     * Opens switch consideration if native hasLowScore is true, if active position is dead
     * (low offensive pressure < 20% AND no meaningful support utility),
     * or if active Pokemon is under critical incoming threat from all opponents.
     */
    @ModifyVariable(
        method = "isSwitching(Ljava/util/List;Ljava/util/List;Lcom/cobblemon/mod/common/battles/pokemon/BattlePokemon;Ljava/util/List;Lcom/cobblemon/mod/common/battles/ActiveBattlePokemon;Lcom/gitlab/surilexa/rbrctai/api/ai/utils/RBStatStages;Z)Z",
        at = @At(value = "STORE", opcode = Opcodes.ISTORE),
        name = "hasLowScore",
        index = 12,
        require = 1,
        remap = false
    )
    private static boolean cobbleverse$adjustHasLowScore(
        boolean nativeHasLowScore,
        @Local(name = "evaluations", index = 0, argsOnly = true) List<RunBunAI.MoveEvaluation> evaluations,
        @Local(name = "self", index = 2, argsOnly = true) BattlePokemon self,
        @Local(name = "opponents", index = 3, argsOnly = true) List<ActiveBattlePokemon> opponents,
        @Local(name = "activeBattlePokemon", index = 4, argsOnly = true) ActiveBattlePokemon activeBattlePokemon,
        @Local(name = "battleStatStages", index = 5, argsOnly = true) RBStatStages battleStatStages
    ) {
        boolean deadPosition = DeadMatchupDetector.isDeadPosition(
            evaluations, self, opponents, activeBattlePokemon, battleStatStages
        );
        boolean criticalThreat = DeadMatchupDetector.isUnderCriticalThreat(
            self, opponents, activeBattlePokemon, battleStatStages
        );
        return nativeHasLowScore || deadPosition || criticalThreat;
    }

    /**
     * Hook 3: Wraps native Random.nextDouble() at Gate 3.
     * If dead position is active (deadPosition == true), bypasses the 25% random veto
     * by returning 0.0d (< 0.75d), proceeding to Gate 4 (HP) and Gate 5 (Party).
     * If dead position is false (including criticalThreat alone, nativeHasLowScore alone,
     * support position, or gray zone 20-33%), preserves native RNG 100% without modification.
     * Consumes native RNG exactly once.
     */
    @WrapOperation(
        method = "isSwitching(Ljava/util/List;Ljava/util/List;Lcom/cobblemon/mod/common/battles/pokemon/BattlePokemon;Ljava/util/List;Lcom/cobblemon/mod/common/battles/ActiveBattlePokemon;Lcom/gitlab/surilexa/rbrctai/api/ai/utils/RBStatStages;Z)Z",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/Random;nextDouble()D"
        ),
        require = 1,
        remap = false
    )
    private static double cobbleverse$wrapRandomNextDouble(
        Random rng,
        Operation<Double> original,
        @Local(name = "evaluations", index = 0, argsOnly = true) List<RunBunAI.MoveEvaluation> evaluations,
        @Local(name = "self", index = 2, argsOnly = true) BattlePokemon self,
        @Local(name = "opponents", index = 3, argsOnly = true) List<ActiveBattlePokemon> opponents,
        @Local(name = "activeBattlePokemon", index = 4, argsOnly = true) ActiveBattlePokemon activeBattlePokemon,
        @Local(name = "battleStatStages", index = 5, argsOnly = true) RBStatStages battleStatStages
    ) {
        double val = original.call(rng);
        boolean deadPosition = DeadMatchupDetector.isDeadPosition(
            evaluations, self, opponents, activeBattlePokemon, battleStatStages
        );
        return DeadMatchupDetector.resolveGate3RandomValue(val, deadPosition);
    }
}
