package com.cobbleverse.legendaryrule.lead;

import java.util.List;

/**
 * Pure domain representation of a trainer roster member's typing.
 */
public record RosterMemberTyping(int slot, String species, List<String> types, int speed) {
    public RosterMemberTyping {
        species = species != null ? species.toLowerCase() : "unknown";
        types = types != null ? List.copyOf(types) : List.of();
        if (speed < 0) {
            speed = 0;
        }
    }

    public RosterMemberTyping(int slot, String species, List<String> types) {
        this(slot, species, types, 0);
    }
}
