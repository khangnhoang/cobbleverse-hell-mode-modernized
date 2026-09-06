package com.cobbleverse.legendaryrule.mixin;

import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobbleverse.legendaryrule.strategy.tracker.BattleItemStateTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Mixin into {@link BattlePokemon} to attach a battle-local {@link BattleItemStateTracker}.
 * <p>
 * This state is owned by the per-battle {@link BattlePokemon} instance, persisting
 * across switches and re-entry within the battle, while naturally resetting for any
 * new battle (avoiding singleton leakage and avoiding mutating {@code Pokemon.heldItem}).
 */
@Mixin(value = BattlePokemon.class, remap = false)
public class BattlePokemonMixin implements BattleItemStateTracker {

    @Unique
    private boolean cobbleverse$throatSprayEnded = false;

    @Override
    public boolean cobbleverse$isThroatSprayEnded() {
        return this.cobbleverse$throatSprayEnded;
    }

    @Override
    public void cobbleverse$markThroatSprayEnded() {
        this.cobbleverse$throatSprayEnded = true;
    }
}
