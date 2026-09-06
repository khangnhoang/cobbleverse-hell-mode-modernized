package com.cobbleverse.legendaryrule.mixin;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.battles.ActiveBattlePokemon;
import com.cobblemon.mod.common.battles.BattleSide;
import com.cobblemon.mod.common.battles.ShowdownActionResponse;
import com.cobblemon.mod.common.battles.ShowdownMoveset;
import com.cobbleverse.legendaryrule.strategy.spread.SpreadMoveValuationContext;
import com.gitlab.surilexa.rbrctai.api.ai.RunBunAI;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Mixin into RunBunAI.choose() to provide invocation-local board-value aggregation
 * for allAdjacentFoes spread moves in Doubles without mutating MoveEvaluation.damage.
 */
@Mixin(value = RunBunAI.class, remap = false)
public abstract class RunBunAIChooseMixin {

    /**
     * Precomputes invocation-local rankingDamage and normalizedPressure maps for allAdjacentFoes moves
     * right at the start of the candidate-ranking loop (first call to getDamage()).
     */
    @Inject(
        method = "choose(Lcom/cobblemon/mod/common/battles/ActiveBattlePokemon;Lcom/cobblemon/mod/common/api/battles/model/PokemonBattle;Lcom/cobblemon/mod/common/battles/BattleSide;Lcom/cobblemon/mod/common/battles/ShowdownMoveset;Z)Lcom/cobblemon/mod/common/battles/ShowdownActionResponse;",
        at = @At(
            value = "INVOKE",
            target = "Lcom/gitlab/surilexa/rbrctai/api/ai/RunBunAI$MoveEvaluation;getDamage()I",
            ordinal = 0
        ),
        remap = false
    )
    private void cobbleverse$precomputeSpreadValuation(
        ActiveBattlePokemon activeBattlePokemon,
        PokemonBattle battle,
        BattleSide side,
        ShowdownMoveset moveset,
        boolean forceSwitch,
        CallbackInfoReturnable<ShowdownActionResponse> cir,
        @Local(name = "evaluations") List<RunBunAI.MoveEvaluation> evaluations,
        @Share("spreadContext") LocalRef<SpreadMoveValuationContext> spreadContextRef
    ) {
        spreadContextRef.set(SpreadMoveValuationContext.fromEvaluations(evaluations));
    }

    /**
     * Redirects MoveEvaluation.getDamage() reads participating in candidate ranking (maxDamage & max-damage equality checks)
     * to return aggregated board ranking damage for allAdjacentFoes moves, while passing through native target-local damage
     * for single-target and allAdjacent moves.
     */
    @Redirect(
        method = "choose(Lcom/cobblemon/mod/common/battles/ActiveBattlePokemon;Lcom/cobblemon/mod/common/api/battles/model/PokemonBattle;Lcom/cobblemon/mod/common/battles/BattleSide;Lcom/cobblemon/mod/common/battles/ShowdownMoveset;Z)Lcom/cobblemon/mod/common/battles/ShowdownActionResponse;",
        at = @At(
            value = "INVOKE",
            target = "Lcom/gitlab/surilexa/rbrctai/api/ai/RunBunAI$MoveEvaluation;getDamage()I"
        ),
        remap = false
    )
    private int cobbleverse$redirectRankingDamage(
        RunBunAI.MoveEvaluation eval,
        @Share("spreadContext") LocalRef<SpreadMoveValuationContext> spreadContextRef
    ) {
        SpreadMoveValuationContext ctx = spreadContextRef.get();
        return ctx != null ? ctx.getRankingDamage(eval) : eval.getDamage();
    }

    /**
     * Substitutes normalized board pressure for allAdjacentFoes spread moves at the percentChange definition site.
     * For single-target and allAdjacent moves, percentChange remains unmodified (damage / target.maxHP).
     * For allAdjacentFoes spread moves, percentChange is substituted with SUM(damage_i / maxHP_i).
     */
    @ModifyVariable(
        method = "choose(Lcom/cobblemon/mod/common/battles/ActiveBattlePokemon;Lcom/cobblemon/mod/common/api/battles/model/PokemonBattle;Lcom/cobblemon/mod/common/battles/BattleSide;Lcom/cobblemon/mod/common/battles/ShowdownMoveset;Z)Lcom/cobblemon/mod/common/battles/ShowdownActionResponse;",
        at = @At(value = "STORE"),
        name = "percentChange",
        remap = false
    )
    private double cobbleverse$substituteBoardPressure(
        double percentChange,
        @Local(name = "move") RunBunAI.MoveEvaluation move,
        @Share("spreadContext") LocalRef<SpreadMoveValuationContext> spreadContextRef
    ) {
        SpreadMoveValuationContext ctx = spreadContextRef.get();
        if (ctx != null && move != null && ctx.isAggregatedSpreadMove(move.getMove())) {
            return ctx.getNormalizedPressure(move.getMove(), percentChange);
        }
        return percentChange;
    }
}