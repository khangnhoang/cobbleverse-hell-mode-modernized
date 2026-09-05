package com.cobbleverse.legendaryrule.lead;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Pure domain representation of an authored lead expectation.
 * Used as a semantic drift guard to verify roster identity before deployment.
 */
public record ExpectedLeadMember(String species, String form, List<String> requiredAspects) {
    public ExpectedLeadMember {
        species = Objects.requireNonNull(species, "species must not be null").trim().toLowerCase();
        form = form != null && !form.isBlank() ? form.trim().toLowerCase() : null;
        if (requiredAspects == null || requiredAspects.isEmpty()) {
            requiredAspects = Collections.emptyList();
        } else {
            requiredAspects = requiredAspects.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(String::toLowerCase)
                    .toList();
        }
    }

    public ExpectedLeadMember(String species) {
        this(species, null, Collections.emptyList());
    }

    public ExpectedLeadMember(String species, String form) {
        this(species, form, Collections.emptyList());
    }

    /**
     * Matches this expectation against an observed Pokémon identity.
     */
    public boolean matches(PokemonIdentity actual) {
        if (actual == null || actual.species() == null) {
            return false;
        }
        if (!this.species.equalsIgnoreCase(actual.species())) {
            return false;
        }
        if (this.form != null && !this.form.isBlank()) {
            if (actual.form() == null || !this.form.equalsIgnoreCase(actual.form())) {
                return false;
            }
        }
        if (this.requiredAspects != null && !this.requiredAspects.isEmpty()) {
            if (actual.aspects() == null) {
                return false;
            }
            for (String req : this.requiredAspects) {
                boolean found = actual.aspects().stream().anyMatch(a -> a.equalsIgnoreCase(req));
                if (!found) {
                    return false;
                }
            }
        }
        return true;
    }
}
