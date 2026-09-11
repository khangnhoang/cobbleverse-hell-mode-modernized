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
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Phase 1 Mixin into RunBunAI.isSwitching:
 * Hook 1: Relaxes Gate 1 score veto so non-damaging status moves scoring >= 6 do not veto switching.
 * Hook 2: Adjusts Gate 2 hasLowScore to resolve status-move holes (FIX A) and critical threat with low pressure (FIX B).
 * All other native gates (HP > 50%, random 75%, party survivability traversal) remain completely intact.
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
     * Opens switch consideration if native hasLowScore is true, if offensive pressure is low (< 20%),
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
        if (nativeHasLowScore) {
            return true;
        }

        // Low offensive pressure (< 20% max damage across all opponents, includes status moves and chip moves)
        boolean lowPressure = DeadMatchupDetector.isLowOffensivePressure(evaluations, opponents);
        if (lowPressure) {
            return true;
        }

        // Critical threat: all eligible opponents can OHKO self
        boolean criticalThreat = DeadMatchupDetector.isUnderCriticalThreat(
            self, opponents, activeBattlePokemon, battleStatStages
        );
        return criticalThreat;
    }
}
