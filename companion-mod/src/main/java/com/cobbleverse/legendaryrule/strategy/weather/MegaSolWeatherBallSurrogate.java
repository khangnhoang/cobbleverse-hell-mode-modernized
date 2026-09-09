package com.cobbleverse.legendaryrule.strategy.weather;

import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.types.ElementalTypes;
import com.cobbleverse.legendaryrule.strategy.dynamic.DynamicMoveSurrogate;

/**
 * Surrogate Move instance representing Weather Ball resolved to Fire with 100 Base Power
 * for an active Mega Sol attacker.
 *
 * Guarantees preservation of:
 * - template metadata (MoveTarget.normal, priority, accuracy, category, etc.)
 * - currentPp
 * - raisedPpStages
 *
 * Overrides effective move attributes via custom surrogate template:
 * - name: "weatherball_fire_resolved" (bypasses upstream PokeMathMax weatherBallType checks)
 * - elementalType: ElementalTypes.FIRE
 * - power: 100.0d
 */
public class MegaSolWeatherBallSurrogate extends DynamicMoveSurrogate {

    public static final String RESOLVED_MOVE_NAME = "weatherball_fire_resolved";
    public static final double RESOLVED_BASE_POWER = 100.0d;

    public MegaSolWeatherBallSurrogate(Move original) {
        super(
            original,
            RESOLVED_MOVE_NAME,
            ElementalTypes.FIRE,
            RESOLVED_BASE_POWER,
            original != null ? original.getDamageCategory() : null
        );
    }
}
