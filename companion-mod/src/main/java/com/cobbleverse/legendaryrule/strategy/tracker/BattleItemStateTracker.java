package com.cobbleverse.legendaryrule.strategy.tracker;

import com.cobblemon.mod.common.api.battles.interpreter.BattleMessage;
import com.cobblemon.mod.common.api.battles.interpreter.Effect;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;

/**
 * Duck interface mixed into {@code com.cobblemon.mod.common.battles.pokemon.BattlePokemon}
 * to track in-battle item termination states (such as Throat Spray consumption)
 * directly on the per-battle runtime Pokémon instance.
 */
public interface BattleItemStateTracker {

    /**
     * Checks if Throat Spray has ended (been consumed or removed) for this battle instance.
     *
     * @return true if Throat Spray is no longer available in this battle.
     */
    boolean cobbleverse$isThroatSprayEnded();

    /**
     * Marks Throat Spray as ended for this battle instance.
     */
    void cobbleverse$markThroatSprayEnded();

    /**
     * Package-private helper to process Showdown {@code |-enditem|} messages and mark
     * the target Pokémon as ended if the item is Throat Spray.
     *
     * @param pokemon the target battle Pokémon
     * @param battleMessage the Showdown battle message
     * @return true if the message was for Throat Spray and the target was marked; false otherwise.
     */
    static boolean markEndedIfThroatSpray(BattlePokemon pokemon, BattleMessage battleMessage) {
        if (pokemon == null || battleMessage == null) {
            return false;
        }
        try {
            Effect effect = battleMessage.effectAt(1);
            if (effect != null && "throatspray".equalsIgnoreCase(effect.getId())) {
                if (pokemon instanceof BattleItemStateTracker tracker) {
                    tracker.cobbleverse$markThroatSprayEnded();
                    return true;
                }
            }
        } catch (Exception ignored) {
            // Defensive guard against malformed or unexpected battle messages
        }
        return false;
    }
}
