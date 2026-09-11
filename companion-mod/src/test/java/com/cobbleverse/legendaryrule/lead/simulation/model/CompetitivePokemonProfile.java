package com.cobbleverse.legendaryrule.lead.simulation.model;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable profile of a competitive Pokémon configured for Turn-1 simulation.
 */
public record CompetitivePokemonProfile(
        String species,
        List<String> aspects,
        List<String> types,
        int level,
        Map<Stat, Integer> baseStats,
        String nature,
        Map<Stat, Integer> ivs,
        Map<Stat, Integer> evs,
        String ability,
        String heldItem,
        List<MoveProfile> moves,
        ActualStats actualStats
) {
    public enum Stat {
        HP, ATK, DEF, SPA, SPD, SPE
    }

    public CompetitivePokemonProfile {
        Objects.requireNonNull(species, "species must not be null");
        Objects.requireNonNull(types, "types must not be null");
        Objects.requireNonNull(baseStats, "baseStats must not be null");
        Objects.requireNonNull(nature, "nature must not be null");
        Objects.requireNonNull(ivs, "ivs must not be null");
        Objects.requireNonNull(evs, "evs must not be null");
        Objects.requireNonNull(ability, "ability must not be null");
        Objects.requireNonNull(moves, "moves must not be null");
        Objects.requireNonNull(actualStats, "actualStats must not be null");

        species = species.toLowerCase(Locale.ROOT);
        aspects = aspects == null ? List.of() : List.copyOf(aspects);
        types = types.stream().map(t -> t.toLowerCase(Locale.ROOT)).toList();
        nature = nature.toLowerCase(Locale.ROOT);
        ability = ability.toLowerCase(Locale.ROOT);
        heldItem = heldItem == null ? "" : heldItem.toLowerCase(Locale.ROOT);
        moves = List.copyOf(moves);
        baseStats = Collections.unmodifiableMap(baseStats);
        ivs = Collections.unmodifiableMap(ivs);
        evs = Collections.unmodifiableMap(evs);
    }

    public boolean hasType(String type) {
        if (type == null) return false;
        String lower = type.toLowerCase(Locale.ROOT);
        return types.contains(lower);
    }

    public boolean hasAbility(String abilityName) {
        return ability.equalsIgnoreCase(abilityName);
    }

    public boolean hasItem(String itemName) {
        return heldItem.equalsIgnoreCase(itemName);
    }

    public boolean hasMove(String moveId) {
        if (moveId == null) return false;
        String lower = moveId.toLowerCase(Locale.ROOT);
        return moves.stream().anyMatch(m -> m.id().equals(lower));
    }

    public MoveProfile getMove(String moveId) {
        if (moveId == null) return null;
        String lower = moveId.toLowerCase(Locale.ROOT);
        return moves.stream().filter(m -> m.id().equals(lower)).findFirst().orElse(null);
    }
}
