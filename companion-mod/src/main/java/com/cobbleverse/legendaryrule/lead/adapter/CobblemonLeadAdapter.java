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

    public static int resolveSpeed(Pokemon pokemon) {
        if (pokemon == null) {
            return 0;
        }
        int speed = 0;
        try {
            speed = pokemon.getSpeed();
        } catch (Throwable ignored) {
        }
        if (speed <= 0) {
            try {
                if (pokemon.getForm() != null && pokemon.getForm().getBaseStats() != null) {
                    Integer spe = pokemon.getForm().getBaseStats().get(com.cobblemon.mod.common.api.pokemon.stats.Stats.SPEED);
                    if (spe != null) {
                        speed = spe;
                    }
                } else if (pokemon.getSpecies() != null && pokemon.getSpecies().getBaseStats() != null) {
                    Integer spe = pokemon.getSpecies().getBaseStats().get(com.cobblemon.mod.common.api.pokemon.stats.Stats.SPEED);
                    if (spe != null) {
                        speed = spe;
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        if (isHoldingChoiceScarf(pokemon)) {
            speed = (int) Math.floor(speed * 1.5);
        }
        return Math.max(0, speed);
    }

    public static boolean isHoldingChoiceScarf(Pokemon pokemon) {
        if (pokemon == null) {
            return false;
        }
        try {
            net.minecraft.item.ItemStack stack = pokemon.heldItem();
            if (stack == null || stack.isEmpty()) {
                return false;
            }
            net.minecraft.util.Identifier id = net.minecraft.registry.Registries.ITEM.getId(stack.getItem());
            if (id != null) {
                String path = id.getPath().toLowerCase(Locale.ROOT);
                return path.equals("choice_scarf") || path.equals("choicescarf");
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    public static PlayerLeadTyping toPlayerLeadTyping(Pokemon pokemon) {
        if (pokemon == null || pokemon.getSpecies() == null) {
            return new PlayerLeadTyping("unknown", Collections.emptyList(), 0);
        }
        String species = pokemon.getSpecies().getName().toLowerCase(Locale.ROOT);
        int speed = resolveSpeed(pokemon);
        return new PlayerLeadTyping(species, extractTypes(pokemon), speed);
    }

    public static RosterMemberTyping toRosterMemberTyping(int slot, Pokemon pokemon) {
        if (pokemon == null || pokemon.getSpecies() == null) {
            return new RosterMemberTyping(slot, "unknown", Collections.emptyList(), 0);
        }
        String species = pokemon.getSpecies().getName().toLowerCase(Locale.ROOT);
        int speed = resolveSpeed(pokemon);
        return new RosterMemberTyping(slot, species, extractTypes(pokemon), speed);
    }
}
