package com.cobbleverse.legendaryrule.lead;

import java.util.List;
import java.util.Locale;

/**
 * Pure domain record describing a per-opponent conjunction condition.
 * <p>
 * Unlike the board-aggregate conditions ({@code favoredAgainst}, {@code favoredAgainstSpecies},
 * {@code minFastOpponents}), every present property here must be satisfied by the <em>same</em>
 * opposing Pokémon for the bonus to be awarded (exists-quantifier over the opposing leads).
 * <p>
 * Every condition property is independently optional; {@code bonus} is a mandatory weight,
 * not a condition. Zero Cubblemon/Minecraft dependencies: this record is part of the pure
 * {@code lead} domain and is compared only against {@link PlayerLeadTyping} and
 * {@link RosterMemberTyping} values produced by the runtime adapter.
 *
 * @param type                  a single required elemental type, or {@code null} when unconstrained
 * @param typeAnyOf             a set of elemental types, satisfied when the opponent has at least one of
 *                              them, or empty when unconstrained. Mutually exclusive with {@code type}:
 *                              the two would express the same property in two ways, so authoring both is
 *                              rejected rather than silently ANDed.
 * @param damagingMoveType      a required type among the opponent's damaging moves, or {@code null}
 * @param fasterThanRosterSlot  roster slot the opponent must strictly out-speed, or {@code null}
 * @param slowerThanRosterSlot  roster slot the opponent must be strictly slower than, or {@code null}.
 *                              Mutually exclusive with {@code fasterThanRosterSlot}, since no speed can be
 *                              both strictly greater and strictly less than the same reference.
 * @param bonus                 authored score bonus, applied once when the conjunction is satisfied
 */
public record OpponentMatch(
        String type,
        List<String> typeAnyOf,
        String damagingMoveType,
        Integer fasterThanRosterSlot,
        Integer slowerThanRosterSlot,
        int bonus
) {

    /** Policy guard lower bound for {@code bonus}; not an arithmetic derivation. See the authoring contract. */
    public static final int MIN_BONUS = 1;

    /** Policy guard upper bound for {@code bonus}; not an arithmetic derivation. See the authoring contract. */
    public static final int MAX_BONUS = 16;

    public OpponentMatch {
        type = normalizeType(type, "type");
        typeAnyOf = normalizeTypeSet(typeAnyOf, "typeAnyOf");
        damagingMoveType = normalizeType(damagingMoveType, "damagingMoveType");
        if (type != null && !typeAnyOf.isEmpty()) {
            throw new IllegalArgumentException(
                    "opponentMatch must not declare both 'type' and 'typeAnyOf'; use 'typeAnyOf' alone for a set");
        }
        if (fasterThanRosterSlot != null && fasterThanRosterSlot < 0) {
            throw new IllegalArgumentException(
                    "opponentMatch.fasterThanRosterSlot must be non-negative when present, got: " + fasterThanRosterSlot);
        }
        if (slowerThanRosterSlot != null && slowerThanRosterSlot < 0) {
            throw new IllegalArgumentException(
                    "opponentMatch.slowerThanRosterSlot must be non-negative when present, got: " + slowerThanRosterSlot);
        }
        if (fasterThanRosterSlot != null && slowerThanRosterSlot != null) {
            throw new IllegalArgumentException(
                    "opponentMatch must not declare both 'fasterThanRosterSlot' and 'slowerThanRosterSlot'; "
                            + "no speed can be strictly greater and strictly less than the same reference");
        }
        if (bonus < MIN_BONUS || bonus > MAX_BONUS) {
            throw new IllegalArgumentException(
                    "opponentMatch.bonus must be between " + MIN_BONUS + " and " + MAX_BONUS + ", got: " + bonus);
        }
        if (type == null && typeAnyOf.isEmpty() && damagingMoveType == null
                && fasterThanRosterSlot == null && slowerThanRosterSlot == null) {
            throw new IllegalArgumentException(
                    "opponentMatch must declare at least one of type, typeAnyOf, damagingMoveType, "
                            + "fasterThanRosterSlot, slowerThanRosterSlot");
        }
    }

    /** Convenience form for the single-type / damaging-move / faster-than conjunction. */
    public OpponentMatch(String type, String damagingMoveType, Integer fasterThanRosterSlot, int bonus) {
        this(type, List.of(), damagingMoveType, fasterThanRosterSlot, null, bonus);
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

    private static List<String> normalizeTypeSet(List<String> values, String fieldName) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .map(v -> normalizeType(v, fieldName))
                .filter(v -> v != null)
                .distinct()
                .toList();
    }
}
