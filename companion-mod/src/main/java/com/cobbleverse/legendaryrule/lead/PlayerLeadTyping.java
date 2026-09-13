package com.cobbleverse.legendaryrule.lead;

import java.util.List;
import java.util.Locale;

/**
 * Pure domain representation of an active player lead's typing and speed.
 */
public record PlayerLeadTyping(String species, List<String> types, int baseSpeed) {
    public PlayerLeadTyping {
        species = species != null ? species.toLowerCase(Locale.ROOT) : "unknown";
        types = types != null ? List.copyOf(types) : List.of();
        if (baseSpeed < 0) {
            baseSpeed = 0;
        }
    }

    public PlayerLeadTyping(String species, List<String> types) {
        this(species, types, 0);
    }
}

