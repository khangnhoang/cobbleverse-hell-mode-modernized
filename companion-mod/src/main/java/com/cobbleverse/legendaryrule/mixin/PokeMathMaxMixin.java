package com.cobbleverse.legendaryrule.mixin;

import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.types.ElementalType;
import com.cobblemon.mod.common.battles.ActiveBattlePokemon;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobbleverse.legendaryrule.strategy.dynamic.DynamicMoveResolver;
import com.cobbleverse.legendaryrule.strategy.guard.RedirectAbilityGuard;
import com.gitlab.surilexa.rbrctai.api.ai.utils.PokeMathMax;
import com.gitlab.surilexa.rbrctai.api.ai.utils.RBStatStages;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin into PokeMathMax:
 * 1. Fixes Run & Bun AI spread damage calculation bug where spread damage
 *    was multiplied twice by targets (0.75 * 0.75 = 0.5625).
 * 2. Vetoes redirected single-target moves (Storm Drain / Lightning Rod) by returning
 *    0.0 damage and marking candidate immunity as true.
 * 3. Resolves effective dynamic moves (Ivy Cudgel, Raging Bull, Weather Ball, Tera Blast, Revelation Dance).
 */
@Mixin(value = PokeMathMax.class, remap = false)
public abstract class PokeMathMaxMixin {

    @Shadow(remap = false)
    private static double damage(Move move, boolean physical, boolean multiTarget, boolean parentalBond, boolean glaiveRush, boolean burn, boolean zmove, boolean reflect, boolean lightscreen, BattlePokemon attacker, BattlePokemon defender, RBStatStages statStages, ActiveBattlePokemon activeBattlePokemon, boolean predictTera, boolean isAttacker) {
        throw new AssertionError();
    }

    @Redirect(
        method = "damage(Lcom/cobblemon/mod/common/battles/pokemon/BattlePokemon;Lcom/cobblemon/mod/common/battles/pokemon/BattlePokemon;Lcom/cobblemon/mod/common/api/moves/Move;Lcom/cobblemon/mod/common/battles/ActiveBattlePokemon;ZZLcom/gitlab/surilexa/rbrctai/api/ai/utils/RBStatStages;)I",
        at = @At(
            value = "INVOKE",
            target = "Lcom/gitlab/surilexa/rbrctai/api/ai/utils/PokeMathMax;damage(Lcom/cobblemon/mod/common/api/moves/Move;ZZZZZZZZLcom/cobblemon/mod/common/battles/pokemon/BattlePokemon;Lcom/cobblemon/mod/common/battles/pokemon/BattlePokemon;Lcom/gitlab/surilexa/rbrctai/api/ai/utils/RBStatStages;Lcom/cobblemon/mod/common/battles/ActiveBattlePokemon;ZZ)D"
        ),
        remap = false
    )
    private static double cobbleverse$correctSpreadMultiplier(
        Move move, boolean physical, boolean multiTarget, boolean parentalBond, boolean glaiveRush,
        boolean burn, boolean zmove, boolean reflect, boolean lightscreen, BattlePokemon attacker,
        BattlePokemon defender, RBStatStages statStages, ActiveBattlePokemon activeBattlePokemon,
        boolean predictTera, boolean isAttacker
    ) {
        Move effectiveMove = DynamicMoveResolver.resolveEffectiveMove(move, attacker, activeBattlePokemon, predictTera);

        if (RedirectAbilityGuard.isMoveRedirected(effectiveMove, attacker, defender, activeBattlePokemon)) {
            return 0.0d;
        }

        double rawDamage = damage(effectiveMove, physical, false, parentalBond, glaiveRush, burn, zmove, reflect, lightscreen, attacker, defender, statStages, activeBattlePokemon, predictTera, isAttacker);
        return multiTarget ? rawDamage * 0.75d : rawDamage;
    }

    @Inject(
        method = "isImmuneCheck(Lcom/cobblemon/mod/common/api/moves/Move;Lcom/cobblemon/mod/common/battles/pokemon/BattlePokemon;Lcom/cobblemon/mod/common/battles/pokemon/BattlePokemon;Lcom/cobblemon/mod/common/battles/ActiveBattlePokemon;Lcom/cobblemon/mod/common/api/types/ElementalType;Z)Z",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    private static void cobbleverse$guardRedirectImmunity(
        Move move,
        BattlePokemon attacker,
        BattlePokemon defender,
        ActiveBattlePokemon abp,
        ElementalType teraType,
        boolean predictTera,
        CallbackInfoReturnable<Boolean> cir
    ) {
        Move effectiveMove = DynamicMoveResolver.resolveEffectiveMove(move, attacker, abp, predictTera);

        if (RedirectAbilityGuard.isMoveRedirected(effectiveMove, attacker, defender, abp)) {
            cir.setReturnValue(true);
            return;
        }

        if (effectiveMove != move) {
            cir.setReturnValue(PokeMathMax.isImmuneCheck(effectiveMove, attacker, defender, abp, teraType, predictTera));
        }
    }
}