package com.cobbleverse.legendaryrule.mixin;

import com.cobblemon.mod.common.battles.ActiveBattlePokemon;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobbleverse.legendaryrule.fair.FairBattleContext;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Getter firewall on {@link ActiveBattlePokemon#getBattlePokemon()}.
 *
 * <p>When a fair-information battle scope is active (during AI decision calculation),
 * returns the sanitized shadow {@link BattlePokemon} for mapped opposing Pokémon.
 * Outside of an active fair scope, or for unmapped/own Pokémon, the original
 * real BattlePokemon is returned unchanged without field mutation or object detachment.</p>
 */
@Mixin(value = ActiveBattlePokemon.class, remap = false)
public abstract class ActiveBattlePokemonMixin {

    @ModifyReturnValue(method = "getBattlePokemon", at = @At("RETURN"))
    private BattlePokemon cobbleverse$substituteFairBattlePokemon(BattlePokemon original) {
        return FairBattleContext.resolve((ActiveBattlePokemon) (Object) this, original);
    }
}
