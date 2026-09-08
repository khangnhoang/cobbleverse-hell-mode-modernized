package com.cobbleverse.legendaryrule.mega;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.battles.BattleStartedEvent;
import com.cobblemon.mod.common.api.pokemon.feature.SpeciesFeature;
import com.cobblemon.mod.common.api.pokemon.feature.StringSpeciesFeature;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.pokemon.Pokemon;
import net.minecraft.nbt.NbtCompound;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OutsideMegaBattleNormalizer {
    private static final Logger LOGGER = LoggerFactory.getLogger(OutsideMegaBattleNormalizer.class);

    public static void register() {
        CobblemonEvents.BATTLE_STARTED_PRE.subscribe(Priority.HIGHEST, OutsideMegaBattleNormalizer::onBattleStartedPre);
        LOGGER.info("[OutsideMegaNormalizer] Registered BATTLE_STARTED_PRE listener with Priority.HIGHEST.");
    }

    private static void onBattleStartedPre(BattleStartedEvent.Pre event) {
        if (event == null) {
            return;
        }
        processBattle(event.getBattle());
    }

    static int processBattle(PokemonBattle battle) {
        if (battle == null) {
            return 0;
        }

        int totalNormalized = 0;
        for (BattleActor actor : battle.getActors()) {
            if (!(actor instanceof PlayerBattleActor playerActor)) {
                continue;
            }

            int playerNormalizedCount = 0;
            for (BattlePokemon battlePokemon : playerActor.getPokemonList()) {
                Pokemon effectedPokemon = battlePokemon.getEffectedPokemon();
                Pokemon originalPokemon = battlePokemon.getOriginalPokemon();

                boolean effectedNormalized = normalizeOutsideMega(effectedPokemon);
                boolean originalNormalized = false;
                if (originalPokemon != null && originalPokemon != effectedPokemon) {
                    originalNormalized = normalizeOutsideMega(originalPokemon);
                }

                if (effectedNormalized || originalNormalized) {
                    playerNormalizedCount++;
                }
            }

            if (playerNormalizedCount > 0) {
                LOGGER.info("[OutsideMegaNormalizer] Normalized {} outside-Mega Pokémon for player '{}' before Showdown startup (battle: {}).",
                        playerNormalizedCount, getPlayerNameSafely(playerActor), battle.getBattleId());
                totalNormalized += playerNormalizedCount;
            }
        }
        return totalNormalized;
    }

    private static String getPlayerNameSafely(PlayerBattleActor playerActor) {
        try {
            if (playerActor.getUuid() != null) {
                net.minecraft.text.Text text = playerActor.getName();
                if (text != null) {
                    return text.getString();
                }
            }
        } catch (Throwable ignored) {
        }
        return playerActor.getUuid() != null ? playerActor.getUuid().toString() : "unknown";
    }

    static boolean normalizeOutsideMega(Pokemon pokemon) {
        if (pokemon == null) {
            return false;
        }

        NbtCompound nbt = pokemon.getPersistentData();
        if (!nbt.getBoolean("is_mega")) {
            return false;
        }

        SpeciesFeature feature = pokemon.getFeature("mega_evolution");
        if (!(feature instanceof StringSpeciesFeature stringFeature)) {
            LOGGER.warn("[OutsideMegaNormalizer] Outside-Mega marker exists but mega_evolution feature is missing/invalid for Pokémon species '{}' (UUID: {})",
                    pokemon.getSpecies().getName(), pokemon.getUuid());
            return false;
        }

        nbt.remove("is_mega");
        nbt.remove("form_changing");
        pokemon.setTradeable(true);
        stringFeature.setValue("none");
        pokemon.updateAspects();

        LOGGER.debug("[OutsideMegaNormalizer] Reverted outside-Mega Pokémon '{}' (UUID: {}) to base form.",
                pokemon.getSpecies().getName(), pokemon.getUuid());
        return true;
    }
}
