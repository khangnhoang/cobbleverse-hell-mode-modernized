package com.cobbleverse.legendaryrule;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class LegendaryPartyRule {

    public static final Identifier ULTRA_NECROZIUM_Z_ID = Identifier.of("mega_showdown", "ultranecrozium_z");
    public static final String NECROZMA_SPECIES = "necrozma";

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
            count += getRestrictedSlots(pokemon);
        }
        return count;
    }

    public static int getRestrictedCount(Iterable<Pokemon> party) {
        if (party == null) {
            return 0;
        }
        int count = 0;
        for (Pokemon pokemon : party) {
            count += getRestrictedSlots(pokemon);
        }
        return count;
    }

    public static int getRestrictedSlots(Pokemon pokemon) {
        if (pokemon == null) {
            return 0;
        }
        int slots = 0;
        if (isRestricted(pokemon)) {
            slots++;
        }
        if (hasUltraNecrozmaSurcharge(pokemon)) {
            slots++;
        }
        return slots;
    }

    public static boolean isRestricted(Pokemon pokemon) {
        return pokemon != null && (pokemon.isLegendary() || pokemon.isMythical());
    }

    public static boolean isNecrozma(Pokemon pokemon) {
        if (pokemon == null) {
            return false;
        }
        try {
            Species species = pokemon.getSpecies();
            if (species == null) {
                return false;
            }
            Identifier id = species.getResourceIdentifier();
            if (id != null && NECROZMA_SPECIES.equalsIgnoreCase(id.getPath())) {
                return true;
            }
            String showdownId = species.showdownId();
            if (showdownId != null && NECROZMA_SPECIES.equalsIgnoreCase(showdownId)) {
                return true;
            }
            String name = species.getName();
            return name != null && NECROZMA_SPECIES.equalsIgnoreCase(name);
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean isHoldingUltraNecroziumZ(Pokemon pokemon) {
        if (pokemon == null) {
            return false;
        }
        try {
            ItemStack stack = pokemon.heldItem();
            if (stack == null || stack.isEmpty()) {
                return false;
            }
            Identifier itemId = Registries.ITEM.getId(stack.getItem());
            return ULTRA_NECROZIUM_Z_ID.equals(itemId);
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean hasUltraNecrozmaSurcharge(Pokemon pokemon) {
        return isNecrozma(pokemon) && isHoldingUltraNecroziumZ(pokemon);
    }

    public static boolean isAllowed(ServerPlayerEntity player) {
        return getRestrictedCount(player) <= CompanionConfig.getMaxLegendaryMythical();
    }

    public static boolean isAllowed(Iterable<Pokemon> party) {
        return getRestrictedCount(party) <= CompanionConfig.getMaxLegendaryMythical();
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
