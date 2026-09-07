package com.cobbleverse.legendaryrule.mixin;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.battles.ActiveBattlePokemon;
import com.cobblemon.mod.common.battles.BattleSide;
import com.cobblemon.mod.common.battles.ShowdownActionResponse;
import com.cobblemon.mod.common.battles.ShowdownMoveset;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobbleverse.legendaryrule.fair.FairBattleContext;
import com.cobbleverse.legendaryrule.fair.FairShadowPokemonBuilder;
import com.gitlab.surilexa.rbrctai.api.ai.RunBunAI;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.spongepowered.asm.mixin.Mixin;

import java.util.HashMap;
import java.util.Map;

/**
 * Lifecycle wrapper around {@link RunBunAI#choose} to manage the {@link FairBattleContext}.
 *
 * <p>Constructs sanitized shadow mappings strictly for opposing/player active Pokémon,
 * leaving the NPC's own Pokémon and team data real. Guarantees exception-safe and
 * re-entrant restoration via {@link FairBattleContext.Scope} and {@link WrapMethod}.</p>
 */
@Mixin(value = RunBunAI.class, remap = false)
public abstract class RunBunAIFairLifecycleMixin {

    @WrapMethod(method = "choose")
    private ShowdownActionResponse cobbleverse$wrapChooseWithFairContext(
        ActiveBattlePokemon activeBattlePokemon,
        PokemonBattle battle,
        BattleSide side,
        ShowdownMoveset moveset,
        boolean forceSwitch,
        Operation<ShowdownActionResponse> original
    ) {
        Map<ActiveBattlePokemon, BattlePokemon> shadowMap = new HashMap<>();
        if (battle != null && battle.getActivePokemon() != null) {
            try {
                for (ActiveBattlePokemon abp : battle.getActivePokemon()) {
                    if (abp != null && abp.getSide() != null && abp.getSide() != side) {
                        BattlePokemon shadow = FairShadowPokemonBuilder.build(abp);
                        if (shadow != null) {
                            shadowMap.put(abp, shadow);
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
        }

        try (FairBattleContext.Scope ignored = FairBattleContext.open(shadowMap)) {
            return original.call(activeBattlePokemon, battle, side, moveset, forceSwitch);
        }
    }
}
