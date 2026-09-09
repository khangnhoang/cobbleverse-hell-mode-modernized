package com.cobbleverse.legendaryrule.strategy.dynamic;

import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.api.moves.categories.DamageCategory;
import com.cobblemon.mod.common.api.types.ElementalType;

import java.util.Objects;

/**
 * Generic surrogate Move instance overriding effective type, power, and/or damage category
 * for AI damage and immunity evaluation.
 *
 * Guarantees preservation of:
 * - original Move reference
 * - template metadata (num, target, accuracy, pp, priority, critRatio, effectChances)
 * - current PP and raised PP stages
 *
 * Allows safe overriding of:
 * - move name (e.g. for Weather Ball bypass)
 * - elementalType (e.g. Ivy Cudgel masks, Raging Bull breeds, Weather Ball weather)
 * - base power (e.g. Weather Ball doubling to 100)
 * - damage category (e.g. Tera Blast physical/special adaptation)
 */
public class DynamicMoveSurrogate extends Move {

    private final Move original;

    public DynamicMoveSurrogate(
        Move original,
        String resolvedName,
        ElementalType resolvedType,
        double resolvedPower,
        DamageCategory resolvedCategory
    ) {
        super(
            createSurrogateTemplate(
                Objects.requireNonNull(original, "original move must not be null").getTemplate(),
                resolvedName,
                resolvedType,
                resolvedPower,
                resolvedCategory
            ),
            original.getCurrentPp(),
            original.getRaisedPpStages()
        );
        this.original = original;
    }

    public DynamicMoveSurrogate(
        Move original,
        String resolvedName,
        ElementalType resolvedType,
        double resolvedPower
    ) {
        this(original, resolvedName, resolvedType, resolvedPower, original != null ? original.getDamageCategory() : null);
    }

    private static MoveTemplate createSurrogateTemplate(
        MoveTemplate originalTemplate,
        String resolvedName,
        ElementalType resolvedType,
        double resolvedPower,
        DamageCategory resolvedCategory
    ) {
        Objects.requireNonNull(originalTemplate, "original template must not be null");
        return new MoveTemplate(
            resolvedName != null ? resolvedName : originalTemplate.getName(),
            originalTemplate.getNum(),
            resolvedType != null ? resolvedType : originalTemplate.getElementalType(),
            resolvedCategory != null ? resolvedCategory : originalTemplate.getDamageCategory(),
            resolvedPower >= 0 ? resolvedPower : originalTemplate.getPower(),
            originalTemplate.getTarget(),
            originalTemplate.getAccuracy(),
            originalTemplate.getPp(),
            originalTemplate.getPriority(),
            originalTemplate.getCritRatio(),
            originalTemplate.getEffectChances()
        );
    }

    public Move getOriginal() {
        return original;
    }
}
