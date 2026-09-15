package com.cobbleverse.legendaryrule.lead;

import java.util.List;
import java.util.Locale;

/**
 * Pure domain representation of an active player lead's typing and speed.
 * <p>
 * {@code damagingMoveTypes} holds the distinct elemental types of the lead's <em>damaging</em>
 * (non-{@code status}) equipped moves, as classified by the runtime adapter. It is the subject of
 * {@link OpponentMatch#damagingMoveType()} and is never populated from a move-name allowlist.
 */
public record PlayerLeadTyping(String species, List<String> types, int speed, List<String> damagingMoveTypes) {
    public PlayerLeadTyping {
        species = species != null ? species.toLowerCase(Locale.ROOT) : "unknown";
        types = types != null ? List.copyOf(types) : List.of();
        if (speed < 0) {
            speed = 0;
        }
        damagingMoveTypes = damagingMoveTypes != null ? normalizeTypes(damagingMoveTypes) : List.of();
    }

    private static List<String> normalizeTypes(List<String> list) {
        return list.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(s -> s.trim().toLowerCase(Locale.ROOT))
                .distinct()
                .toList();
    }

    public PlayerLeadTyping(String species, List<String> types, int speed) {
        this(species, types, speed, List.of());
    }

    public PlayerLeadTyping(String species, List<String> types) {
        this(species, types, 0, List.of());
    }

    public int baseSpeed() {
        return speed;
    }
}

