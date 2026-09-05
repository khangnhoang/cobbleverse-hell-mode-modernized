package com.cobbleverse.legendaryrule.lead;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Pure domain representation of an observed Pokémon identity.
 * Zero external dependencies.
 */
public record PokemonIdentity(String species, String form, Set<String> aspects) {
    public PokemonIdentity {
        species = Objects.requireNonNull(species, "species must not be null").trim().toLowerCase();
        form = form != null && !form.isBlank() ? form.trim().toLowerCase() : null;
        if (aspects == null || aspects.isEmpty()) {
            aspects = Collections.emptySet();
        } else {
            Set<String> clean = new HashSet<>();
            for (String a : aspects) {
                if (a != null && !a.isBlank()) {
                    clean.add(a.trim().toLowerCase());
                }
            }
            aspects = Collections.unmodifiableSet(clean);
        }
    }

    public PokemonIdentity(String species) {
        this(species, null, Collections.emptySet());
    }

    public PokemonIdentity(String species, String form) {
        this(species, form, Collections.emptySet());
    }
}
