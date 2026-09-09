package com.cobbleverse.legendaryrule.strategy.weight;

import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.moves.MoveTemplate;

/**
 * Surrogate Move instance representing a weight-dependent move whose base power
 * has been resolved dynamically for Run & Bun AI damage calculations.
 *
 * Guarantees preservation of:
 * - original move name ("grassknot", "lowkick", "heavyslam", "heatcrash") to maintain
 *   upstream contact, punching, biting, and slice move property checks.
 * - template metadata (num, elementalType, damageCategory, target, accuracy, pp,
 *   priority, critRatio, effectChances).
 * - currentPp and raisedPpStages from original Move.
 *
 * Overrides effective move base power with the dynamically calculated value.
 */
public class WeightDependentMoveSurrogate extends Move {

    private final Move original;
    private final double resolvedBasePower;

    public WeightDependentMoveSurrogate(Move original, double resolvedBasePower) {
        super(
            createSurrogateTemplate(original.getTemplate(), resolvedBasePower),
            original.getCurrentPp(),
            original.getRaisedPpStages()
        );
        this.original = original;
        this.resolvedBasePower = resolvedBasePower;
    }

    private static MoveTemplate createSurrogateTemplate(MoveTemplate original, double power) {
        return new MoveTemplate(
            original.getName(),
            original.getNum(),
            original.getElementalType(),
            original.getDamageCategory(),
            power,
            original.getTarget(),
            original.getAccuracy(),
            original.getPp(),
            original.getPriority(),
            original.getCritRatio(),
            original.getEffectChances()
        );
    }

    public Move getOriginal() {
        return original;
    }

    public double getResolvedBasePower() {
        return resolvedBasePower;
    }
}
