package com.cobbleverse.legendaryrule.lead.simulation.model;

import java.util.Locale;
import java.util.Objects;

/**
 * Immutable profile of a Pokémon move for Turn-1 simulation.
 */
public record MoveProfile(
        String id,
        String type,
        Category category,
        int basePower,
        int priority,
        Target target,
        boolean isContact,
        boolean isSpread,
        int accuracy
) {
    public enum Category {
        PHYSICAL,
        SPECIAL,
        STATUS
    }

    public enum Target {
        SINGLE_OPPONENT,
        ALL_ADJACENT_OPPONENTS,
        ALL_ADJACENT,
        ALLY,
        SELF
    }

    public MoveProfile(
            String id,
            String type,
            Category category,
            int basePower,
            int priority,
            Target target,
            boolean isContact,
            boolean isSpread
    ) {
        this(id, type, category, basePower, priority, target, isContact, isSpread, 100);
    }

    public MoveProfile(
            String id,
            String type,
            Category category,
            int basePower,
            int priority,
            Target target,
            boolean isContact,
            boolean isSpread,
            int accuracy
    ) {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(category, "category must not be null");
        Objects.requireNonNull(target, "target must not be null");
        this.id = id.toLowerCase(Locale.ROOT);
        this.type = type.toLowerCase(Locale.ROOT);
        this.category = category;
        this.basePower = basePower;
        this.priority = priority;
        this.target = target;
        this.isContact = isContact;
        this.isSpread = isSpread;
        this.accuracy = Math.max(1, Math.min(100, accuracy));
    }

    public boolean isStatus() {
        return category == Category.STATUS;
    }

    public boolean isAttacking() {
        return category == Category.PHYSICAL || category == Category.SPECIAL;
    }
}
