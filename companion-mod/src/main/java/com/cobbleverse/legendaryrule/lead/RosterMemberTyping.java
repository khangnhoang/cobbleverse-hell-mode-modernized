package com.cobbleverse.legendaryrule.lead;

import java.util.List;

/**
 * Pure domain representation of a trainer roster member's typing.
 */
public record RosterMemberTyping(int slot, String species, List<String> types) {
    public RosterMemberTyping {
        species = species != null ? species.toLowerCase() : "unknown";
        types = types != null ? List.copyOf(types) : List.of();
    }
}
