package com.cobbleverse.legendaryrule.lead;

import java.util.List;

/**
 * Pure domain representation of an active player lead's typing.
 */
public record PlayerLeadTyping(String species, List<String> types) {
    public PlayerLeadTyping {
        species = species != null ? species.toLowerCase() : "unknown";
        types = types != null ? List.copyOf(types) : List.of();
    }
}
