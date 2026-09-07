package com.cobbleverse.legendaryrule.mixin;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.battles.ActiveBattlePokemon;
import com.cobblemon.mod.common.battles.BattleSide;
import com.cobblemon.mod.common.battles.ShowdownActionResponse;
import com.cobblemon.mod.common.battles.ShowdownMoveset;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobbleverse.legendaryrule.fair.FairShadowPokemonBuilder;
import com.cobbleverse.legendaryrule.strategy.spread.SpreadMoveValuationContext;
import com.cobbleverse.legendaryrule.strategy.tera.TeraTargetResolver;
import com.gitlab.surilexa.rbrctai.api.ai.RunBunAI;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Mixin into RunBunAI.choose() to provide invocation-local board-value aggregation
 * for allAdjacentFoes spread moves in Doubles without mutating MoveEvaluation.damage,
 * and ensure alive Doubles active partner teraTarget reserves the side's Terastallization.
 */
@Mixin(value = RunBunAI.class, remap = false)
public abstract class RunBunAIChooseMixin {

    @Shadow
    private String teraTarget;

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

    /**
     * Resolves whether the configured teraTarget is present and alive across the actor's full Pokemon list.
     * In Doubles, an active partner Pokemon has canBeSentOut() == false, which causes native
     * aliveParty filtering to incorrectly assume the teraTarget is absent/fainted.
     * This hook ensures active and benched targets both correctly reserve the team's Terastallization.
     */
    @ModifyVariable(
        method = "choose(Lcom/cobblemon/mod/common/battles/ActiveBattlePokemon;Lcom/cobblemon/mod/common/api/battles/model/PokemonBattle;Lcom/cobblemon/mod/common/battles/BattleSide;Lcom/cobblemon/mod/common/battles/ShowdownMoveset;Z)Lcom/cobblemon/mod/common/battles/ShowdownActionResponse;",
        at = @At(value = "STORE"),
        name = "teraMatch",
        remap = false
    )
    private BattlePokemon cobbleverse$resolveAliveTeraTarget(
        BattlePokemon originalTeraMatch,
        ActiveBattlePokemon activeBattlePokemon
    ) {
        if (activeBattlePokemon == null || activeBattlePokemon.getActor() == null || this.teraTarget == null || this.teraTarget.isEmpty()) {
            return originalTeraMatch;
        }
        return TeraTargetResolver.resolveAliveTeraTarget(
            activeBattlePokemon.getActor().getPokemonList(),
            this.teraTarget
        );
    }

    /**
     * Guards native RunBunAI.choose() against crashing with NoSuchElementException when checking
     * whether the opponent is in recharge mode.
     * Native RunBunAI line 1779 unconditionally calls oppMoves.getFirst().getName().equals("recharge")
     * assuming every opponent has at least one move in battle. When the fair-information boundary hides
     * unrevealed moves, oppMoves is empty.
     * This wrap returns a safe sentinel move when the list is empty, preventing the crash and correctly
     * evaluating isRecharging as false without fabricating real or playable moves.
     */
    @WrapOperation(
        method = "choose(Lcom/cobblemon/mod/common/battles/ActiveBattlePokemon;Lcom/cobblemon/mod/common/api/battles/model/PokemonBattle;Lcom/cobblemon/mod/common/battles/BattleSide;Lcom/cobblemon/mod/common/battles/ShowdownMoveset;Z)Lcom/cobblemon/mod/common/battles/ShowdownActionResponse;",
        slice = @Slice(
            from = @At(value = "CONSTANT", args = "stringValue=truant"),
            to = @At(value = "CONSTANT", args = "stringValue=recharge")
        ),
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/List;getFirst()Ljava/lang/Object;",
            ordinal = 0
        ),
        remap = false
    )
    private Object cobbleverse$guardRechargeOppMovesGetFirst(List<?> list, Operation<Object> original) {
        if (list == null || list.isEmpty()) {
            return FairShadowPokemonBuilder.getSafeSentinelMove();
        }
        return original.call(list);
    }

    /**
     * Fallback defense: if the move evaluated at the recharge check is null, safely return an empty
     * name to ensure "".equals("recharge") evaluates to false without throwing NullPointerException.
     */
    @WrapOperation(
        method = "choose(Lcom/cobblemon/mod/common/battles/ActiveBattlePokemon;Lcom/cobblemon/mod/common/api/battles/model/PokemonBattle;Lcom/cobblemon/mod/common/battles/BattleSide;Lcom/cobblemon/mod/common/battles/ShowdownMoveset;Z)Lcom/cobblemon/mod/common/battles/ShowdownActionResponse;",
        slice = @Slice(
            from = @At(value = "CONSTANT", args = "stringValue=truant"),
            to = @At(value = "CONSTANT", args = "stringValue=recharge")
        ),
        at = @At(
            value = "INVOKE",
            target = "Lcom/cobblemon/mod/common/api/moves/Move;getName()Ljava/lang/String;",
            ordinal = 0
        ),
        remap = false
    )
    private String cobbleverse$guardRechargeMoveGetName(Move move, Operation<String> original) {
        if (move == null) {
            return "";
        }
        return original.call(move);
    }
}