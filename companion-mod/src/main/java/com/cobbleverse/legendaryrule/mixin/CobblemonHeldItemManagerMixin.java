package com.cobbleverse.legendaryrule.mixin;

import com.cobblemon.mod.common.api.battles.interpreter.BattleMessage;
import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.pokemon.helditem.CobblemonHeldItemManager;
import com.cobbleverse.legendaryrule.strategy.tracker.BattleItemStateTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin into {@link CobblemonHeldItemManager} to capture Showdown {@code |-enditem|} messages
 * at the authoritative execution point.
 * <p>
 * Injected at {@code @At("HEAD")} before Cobblemon's inventory persistence check
 * ({@code shouldConsumeItem(...)}) is evaluated. When a Throat Spray consumption event is received,
 * marks the target {@link BattlePokemon}'s {@link BattleItemStateTracker} as ended.
 */
@Mixin(value = CobblemonHeldItemManager.class, remap = false)
public class CobblemonHeldItemManagerMixin {

    @Inject(method = "handleEndInstruction", at = @At("HEAD"), remap = false)
    private void onHandleEndInstruction(
            BattlePokemon pokemon,
            PokemonBattle battle,
            BattleMessage battleMessage,
            CallbackInfo ci
    ) {
        BattleItemStateTracker.markEndedIfThroatSpray(pokemon, battleMessage);
    }
}
