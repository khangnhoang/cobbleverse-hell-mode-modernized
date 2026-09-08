package com.cobbleverse.legendaryrule.strategy.weather;

import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.gitlab.surilexa.rbrctai.api.ai.utils.PokeMathMax;

/**
 * Canonical guard and resolver for Mega Sol personal weather interactions in Run & Bun AI.
 *
 * Provides a single point of truth to evaluate whether an attacker has active Mega Sol
 * and to resolve dynamic moves like Weather Ball to their true battle mechanics state.
 */
public final class MegaSolWeatherGuard {

    private static final String WEATHER_BALL_NAME = "weatherball";
    private static final String MEGA_SOL_ABILITY = "megasol";

    private MegaSolWeatherGuard() {
    }

    /**
     * Checks if the given attacker has the ability Mega Sol and it is not suppressed.
     * Uses PokeMathMax.hasAbility to respect Neutralizing Gas and Gastro Acid suppression.
     *
     * @param attacker the attacking BattlePokemon
     * @return true if attacker possesses unsuppressed Mega Sol, false otherwise
     */
    public static boolean hasMegaSol(BattlePokemon attacker) {
        if (attacker == null) {
            return false;
        }
        return PokeMathMax.hasAbility(MEGA_SOL_ABILITY, attacker);
    }

    /**
     * Resolves the effective Move instance for scoring calculations.
     * If the move is Weather Ball and the attacker has active Mega Sol, returns a
     * MegaSolWeatherBallSurrogate (Fire type, 100 Base Power).
     * Otherwise, returns the original Move unchanged.
     *
     * @param move the original Move
     * @param attacker the attacking BattlePokemon
     * @return the effective Move for AI calculation
     */
    public static Move resolveEffectiveMove(Move move, BattlePokemon attacker) {
        if (move == null || attacker == null) {
            return move;
        }
        if (move instanceof MegaSolWeatherBallSurrogate) {
            return move;
        }
        if (!WEATHER_BALL_NAME.equalsIgnoreCase(move.getName())) {
            return move;
        }
        if (!hasMegaSol(attacker)) {
            return move;
        }
        return new MegaSolWeatherBallSurrogate(move);
    }
}
