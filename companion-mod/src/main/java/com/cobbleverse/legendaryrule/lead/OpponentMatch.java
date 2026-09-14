package com.cobbleverse.legendaryrule.lead;

import java.util.Locale;

/**
 * Pure domain record describing a per-opponent conjunction condition.
 * <p>
 * Unlike the board-aggregate conditions ({@code favoredAgainst}, {@code favoredAgainstSpecies},
 * {@code minFastOpponents}), every present property here must be satisfied by the <em>same</em>
 * opposing Pokémon for the bonus to be awarded (exists-quantifier over the opposing leads).
 * <p>
 * All three condition properties are independently optional; {@code bonus} is a mandatory weight,
 * not a condition. Zero Cubblemon/Minecraft dependencies: this record is part of the pure
 * {@code lead} domain and is compared only against {@link PlayerLeadTyping} and
 * {@link RosterMemberTyping} values produced by the runtime adapter.
 */
public record OpponentMatch(String type, String damagingMoveType, Integer fasterThanRosterSlot, int bonus) {

    /** Policy guard lower bound for {@code bonus}; not an arithmetic derivation. See the authoring contract. */
    public static final int MIN_BONUS = 1;

    /** Policy guard upper bound for {@code bonus}; not an arithmetic derivation. See the authoring contract. */
    public static final int MAX_BONUS = 16;

    public OpponentMatch {
        type = normalizeType(type, "type");
        damagingMoveType = normalizeType(damagingMoveType, "damagingMoveType");
        if (fasterThanRosterSlot != null && fasterThanRosterSlot < 0) {
            throw new IllegalArgumentException(
                    "opponentMatch.fasterThanRosterSlot must be non-negative when present, got: " + fasterThanRosterSlot);
        }
        if (bonus < MIN_BONUS || bonus > MAX_BONUS) {
            throw new IllegalArgumentException(
                    "opponentMatch.bonus must be between " + MIN_BONUS + " and " + MAX_BONUS + ", got: " + bonus);
        }
        if (type == null && damagingMoveType == null && fasterThanRosterSlot == null) {
            throw new IllegalArgumentException(
                    "opponentMatch must declare at least one of type, damagingMoveType, fasterThanRosterSlot");
        }
    }

    private static String normalizeType(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (!TypeChartData.CANONICAL_TYPES.contains(normalized)) {
            throw new IllegalArgumentException(
                    "opponentMatch." + fieldName + " must be a canonical Gen 9 type, got: " + value);
        }
        return normalized;
    }
}
