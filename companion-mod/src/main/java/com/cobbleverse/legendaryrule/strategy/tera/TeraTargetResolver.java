package com.cobbleverse.legendaryrule.strategy.tera;

import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.pokemon.Pokemon;

import java.util.List;

/**
 * Resolves whether a configured teraTarget is present and alive in the actor's party.
 * <p>
 * Fixes the Doubles teraTarget reservation issue where an active partner has {@code canBeSentOut() == false},
 * which led RunBunAI's native {@code aliveParty} filter to falsely conclude the target was fainted.
 */
public final class TeraTargetResolver {

    private TeraTargetResolver() {
    }

    /**
     * Finds the configured teraTarget in the actor's full Pokemon list if present and not fainted.
     * Both active and benched targets reserve Tera as long as they are alive.
     *
     * @param party      the actor's full Pokemon list
     * @param teraTarget the configured teraTarget showdown ID (e.g., "toxtricitylowkey")
     * @return the alive BattlePokemon matching teraTarget, or {@code null} if absent or fainted
     */
    public static BattlePokemon resolveAliveTeraTarget(List<BattlePokemon> party, String teraTarget) {
        if (party == null || teraTarget == null || teraTarget.isEmpty()) {
            return null;
        }
        for (BattlePokemon bp : party) {
            if (bp == null) {
                continue;
            }
            if (isAlive(bp)) {
                Pokemon pokemon = bp.getEffectedPokemon();
                if (pokemon != null && teraTarget.equalsIgnoreCase(pokemon.showdownId())) {
                    return bp;
                }
            }
        }
        return null;
    }

    /**
     * Determines whether a BattlePokemon is alive (not fainted).
     *
     * @param bp the BattlePokemon to check
     * @return true if healthy and not fainted, false otherwise
     */
    public static boolean isAlive(BattlePokemon bp) {
        if (bp == null) {
            return false;
        }
        if (bp.getHealth() <= 0) {
            return false;
        }
        Pokemon pokemon = bp.getEffectedPokemon();
        return pokemon != null && !pokemon.isFainted() && pokemon.getCurrentHealth() > 0;
    }
}
