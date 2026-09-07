package com.cobbleverse.legendaryrule.mixin;

import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.battles.ActiveBattlePokemon;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.gitlab.surilexa.rbrctai.api.ai.utils.PokeMathMax;
import com.gitlab.surilexa.rbrctai.api.ai.utils.RBStatStages;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Fixes the Run & Bun AI spread damage calculation bug where spread damage
 * was multiplied twice by targets (0.75 * 0.75 = 0.5625).
 * Invokes private damage(...) with multiTarget=false so internal targets multiplier
 * remains 1.0, and applies the 0.75 spread multiplier exactly once.
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
        double rawDamage = damage(move, physical, false, parentalBond, glaiveRush, burn, zmove, reflect, lightscreen, attacker, defender, statStages, activeBattlePokemon, predictTera, isAttacker);
        return multiTarget ? rawDamage * 0.75d : rawDamage;
    }
}