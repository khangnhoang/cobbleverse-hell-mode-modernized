package com.cobbleverse.legendaryrule;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.pokemon.Pokemon;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class LegendaryPartyRule {

    public static int getRestrictedCount(ServerPlayerEntity player) {
        if (player == null) {
            return 0;
        }
        PlayerPartyStore party = Cobblemon.INSTANCE.getStorage().getParty(player);
        if (party == null) {
            return 0;
        }
        int count = 0;
        int size = party.size();
        for (int i = 0; i < size; i++) {
            Pokemon pokemon = party.get(i);
            if (isRestricted(pokemon)) {
                count++;
            }
        }
        return count;
    }

    public static boolean isRestricted(Pokemon pokemon) {
        return pokemon != null && (pokemon.isLegendary() || pokemon.isMythical());
    }

    public static boolean isAllowed(ServerPlayerEntity player) {
        return getRestrictedCount(player) <= CompanionConfig.getMaxLegendaryMythical();
    }

    public static String getRejectionMessage() {
        int limit = CompanionConfig.getMaxLegendaryMythical();
        if (limit == 0) {
            return "§cTrainer rules permit no Legendary or Mythical Pokémon!";
        } else {
            return "§cTrainer rules permit at most " + limit + " Legendary or Mythical Pokémon!";
        }
    }

    public static void sendRejectionMessage(ServerPlayerEntity player) {
        if (player != null) {
            player.sendMessage(Text.literal(getRejectionMessage()), false);
        }
    }
}
