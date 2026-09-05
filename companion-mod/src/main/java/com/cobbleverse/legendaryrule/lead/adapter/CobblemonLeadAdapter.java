package com.cobbleverse.legendaryrule.lead.adapter;

import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobbleverse.legendaryrule.lead.PlayerLeadTyping;
import com.cobbleverse.legendaryrule.lead.PokemonIdentity;
import com.cobbleverse.legendaryrule.lead.RosterMemberTyping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Runtime adapter converting Cobblemon Pokemon entities into pure domain identities and typing models.
 */
public final class CobblemonLeadAdapter {
    private CobblemonLeadAdapter() {}

    public static PokemonIdentity toIdentity(Pokemon pokemon) {
        if (pokemon == null || pokemon.getSpecies() == null) {
            return new PokemonIdentity("unknown", null, Collections.emptySet());
        }
        String species = pokemon.getSpecies().getName().toLowerCase(Locale.ROOT);
        String form = pokemon.getForm() != null ? pokemon.getForm().getName().toLowerCase(Locale.ROOT) : null;
        Set<String> aspects = new HashSet<>();
        if (pokemon.getAspects() != null) {
            for (String a : pokemon.getAspects()) {
                if (a != null && !a.isBlank()) {
                    aspects.add(a.trim().toLowerCase(Locale.ROOT));
                }
            }
        }
        return new PokemonIdentity(species, form, aspects);
    }

    public static List<String> extractTypes(Pokemon pokemon) {
        if (pokemon == null) {
            return Collections.emptyList();
        }
        List<String> types = new ArrayList<>(2);
        if (pokemon.getPrimaryType() != null) {
            types.add(pokemon.getPrimaryType().getName().toLowerCase(Locale.ROOT));
        }
        if (pokemon.getSecondaryType() != null) {
            types.add(pokemon.getSecondaryType().getName().toLowerCase(Locale.ROOT));
        }
        return types;
    }

    public static PlayerLeadTyping toPlayerLeadTyping(Pokemon pokemon) {
        if (pokemon == null || pokemon.getSpecies() == null) {
            return new PlayerLeadTyping("unknown", Collections.emptyList());
        }
        String species = pokemon.getSpecies().getName().toLowerCase(Locale.ROOT);
        return new PlayerLeadTyping(species, extractTypes(pokemon));
    }

    public static RosterMemberTyping toRosterMemberTyping(int slot, Pokemon pokemon) {
        if (pokemon == null || pokemon.getSpecies() == null) {
            return new RosterMemberTyping(slot, "unknown", Collections.emptyList());
        }
        String species = pokemon.getSpecies().getName().toLowerCase(Locale.ROOT);
        return new RosterMemberTyping(slot, species, extractTypes(pokemon));
    }
}
