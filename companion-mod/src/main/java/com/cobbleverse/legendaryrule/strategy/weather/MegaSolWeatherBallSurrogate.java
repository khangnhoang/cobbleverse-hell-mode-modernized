package com.cobbleverse.legendaryrule.strategy.weather;

import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.api.types.ElementalTypes;

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
public class MegaSolWeatherBallSurrogate extends Move {

    public static final String RESOLVED_MOVE_NAME = "weatherball_fire_resolved";
    public static final double RESOLVED_BASE_POWER = 100.0d;

    public MegaSolWeatherBallSurrogate(Move original) {
        super(
            createSurrogateTemplate(original.getTemplate()),
            original.getCurrentPp(),
            original.getRaisedPpStages()
        );
    }

    private static MoveTemplate createSurrogateTemplate(MoveTemplate original) {
        return new MoveTemplate(
            RESOLVED_MOVE_NAME,
            original.getNum(),
            ElementalTypes.FIRE,
            original.getDamageCategory(),
            RESOLVED_BASE_POWER,
            original.getTarget(),
            original.getAccuracy(),
            original.getPp(),
            original.getPriority(),
            original.getCritRatio(),
            original.getEffectChances()
        );
    }
}
