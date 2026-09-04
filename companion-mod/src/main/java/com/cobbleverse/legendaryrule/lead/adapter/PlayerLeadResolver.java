package com.cobbleverse.legendaryrule.lead.adapter;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobbleverse.legendaryrule.lead.PlayerLeadTyping;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Resolves the player's effective active leads according to Cobblemon Doubles deployment semantics:
 * iterates the party and takes the first two non-fainted Pokémon (!pokemon.isFainted()).
 */
public final class PlayerLeadResolver {
    private PlayerLeadResolver() {}

    public static List<PlayerLeadTyping> resolvePlayerLeads(PlayerEntity player) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return Collections.emptyList();
        }
        PlayerPartyStore party = Cobblemon.INSTANCE.getStorage().getParty(serverPlayer);
        if (party == null) {
            return Collections.emptyList();
        }
        List<PlayerLeadTyping> leads = new ArrayList<>(2);
        int size = party.size();
        for (int i = 0; i < size; i++) {
            Pokemon p = party.get(i);
            if (p != null && !p.isFainted()) {
                leads.add(CobblemonLeadAdapter.toPlayerLeadTyping(p));
                if (leads.size() == 2) {
                    break;
                }
            }
        }
        return leads;
    }
}
