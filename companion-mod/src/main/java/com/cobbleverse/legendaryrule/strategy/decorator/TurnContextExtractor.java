package com.cobbleverse.legendaryrule.strategy.decorator;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.battles.ActiveBattlePokemon;
import com.cobblemon.mod.common.battles.BattleSide;
import com.cobblemon.mod.common.battles.ShowdownMoveset;
import com.cobbleverse.legendaryrule.strategy.domain.BattleTurnContext;

import java.util.Optional;

/**
 * Functional interface to extract a BattleTurnContext from runtime Cobblemon objects.
 * Decouples the decorator from Cobblemon adapter details.
 */
@FunctionalInterface
public interface TurnContextExtractor {
    Optional<BattleTurnContext> extract(
        ActiveBattlePokemon activeBattlePokemon,
        PokemonBattle battle,
        BattleSide aiSide,
        ShowdownMoveset moveset
    );
}
