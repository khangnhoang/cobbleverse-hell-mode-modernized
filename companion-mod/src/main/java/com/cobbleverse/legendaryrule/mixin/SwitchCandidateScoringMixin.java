package com.cobbleverse.legendaryrule.mixin;

import com.cobblemon.mod.common.battles.ActiveBattlePokemon;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobbleverse.legendaryrule.strategy.switchai.SwitchCandidateScorer;
import com.gitlab.surilexa.rbrctai.api.ai.RunBunAI;
import com.gitlab.surilexa.rbrctai.api.ai.utils.RBStatStages;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;
import java.util.Map;

/**
 * Phase 2 Mixin into RunBunAI.choose():
 * Intercepts Map.put(possibleSwitch, switchScore) on switchingScores map (offset 1618).
 * Upgrades native switchScore to the deterministic packed score calculated by SwitchCandidateScorer.
 */
@Mixin(value = RunBunAI.class, remap = false)
public abstract class SwitchCandidateScoringMixin {

    @Shadow(remap = false)
    private RBStatStages battleStatStages;

    @WrapOperation(
        method = "choose(Lcom/cobblemon/mod/common/battles/ActiveBattlePokemon;Lcom/cobblemon/mod/common/api/battles/model/PokemonBattle;Lcom/cobblemon/mod/common/battles/BattleSide;Lcom/cobblemon/mod/common/battles/ShowdownMoveset;Z)Lcom/cobblemon/mod/common/battles/ShowdownActionResponse;",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
            ordinal = 0
        ),
        require = 1,
        remap = false
    )
    private Object cobbleverse$wrapSwitchCandidateScore(
        Map<BattlePokemon, Integer> switchingScores,
        Object possibleSwitchKey,
        Object nativeBoxedScore,
        Operation<Object> original,
        @Local(name = "possibleSwitch", index = 38) BattlePokemon possibleSwitch,
        @Local(name = "allOpponentActiveBattlePokemon", index = 26) List<ActiveBattlePokemon> opponents,
        @Local(name = "activeBattlePokemon", index = 1, argsOnly = true) ActiveBattlePokemon activeBattlePokemon,
        @Local(name = "battlePokemon", index = 13) BattlePokemon currentActive,
        @Local(name = "switchScore", index = 32) int nativeLocalScore
    ) {
        if (possibleSwitchKey != possibleSwitch) {
            throw new IllegalStateException("Mixin contract violation: possibleSwitchKey != possibleSwitch");
        }
        if (!(nativeBoxedScore instanceof Integer) || ((Integer) nativeBoxedScore).intValue() != nativeLocalScore) {
            throw new IllegalStateException("Mixin contract violation: nativeBoxedScore does not match nativeLocalScore");
        }

        int packedScore = SwitchCandidateScorer.score(
            possibleSwitch,
            opponents,
            activeBattlePokemon,
            this.battleStatStages,
            currentActive,
            nativeLocalScore
        );

        return original.call(switchingScores, possibleSwitchKey, Integer.valueOf(packedScore));
    }
}
