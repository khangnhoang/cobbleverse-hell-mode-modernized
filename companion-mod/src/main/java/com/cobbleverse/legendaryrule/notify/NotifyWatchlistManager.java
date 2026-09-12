package com.cobbleverse.legendaryrule.notify;

import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import com.cobbleverse.legendaryrule.LegendaryRuleMod;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import us.timinc.mc.cobblemon.spawnnotification.SpawnNotification;
import us.timinc.mc.cobblemon.spawnnotification.api.broadcast.BroadcastContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class NotifyWatchlistManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("rct_legendary_rule/watchlist");

    private static final ConcurrentHashMap<Identifier, WatchMode> WATCHLIST = new ConcurrentHashMap<>();

    private static final Set<Identifier> ELIGIBLE_TRIGGERS = Set.of(
        Identifier.of("spawn_notification", "spawned"),
        Identifier.of("spawn_notification", "fished"),
        Identifier.of("spawn_notification", "snacked")
    );

    public static WatchMode add(Identifier speciesId, WatchMode mode) {
        if (speciesId == null || mode == null) {
            return null;
        }
        return WATCHLIST.put(speciesId, mode);
    }

    public static WatchMode remove(Identifier speciesId) {
        if (speciesId == null) {
            return null;
        }
        return WATCHLIST.remove(speciesId);
    }

    public static WatchMode get(Identifier speciesId) {
        if (speciesId == null) {
            return null;
        }
        return WATCHLIST.get(speciesId);
    }

    public static Map<Identifier, WatchMode> getWatchlist() {
        return Collections.unmodifiableMap(WATCHLIST);
    }

    public static int clearAll() {
        int count = WATCHLIST.size();
        WATCHLIST.clear();
        return count;
    }

    public static void handleBroadcast(BroadcastContext broadcastContext, Identifier trigger) {
        if (broadcastContext == null || trigger == null) {
            return;
        }

        // 1. Only process eligible triggers: spawned, fished, snacked
        if (!ELIGIBLE_TRIGGERS.contains(trigger)) {
            return;
        }

        Pokemon pokemon = broadcastContext.getPokemon();
        if (pokemon == null) {
            return;
        }

        Species species = pokemon.getSpecies();
        if (species == null) {
            return;
        }

        Identifier speciesId = species.getResourceIdentifier();
        if (speciesId == null) {
            return;
        }

        // 2. Check if species is watched
        WatchMode mode = WATCHLIST.get(speciesId);
        if (mode == null) {
            return;
        }

        // 3. Resolve sound recipients according to SpawnNotification config
        List<ServerPlayerEntity> recipients = resolveRecipients(broadcastContext);
        if (recipients.isEmpty()) {
            // ONE delivery semantics: only consume ONE when there is at least one eligible recipient.
            // If recipient list is empty, keep the ONE entry.
            return;
        }

        // 4. Atomic consumption for ONE mode
        if (mode == WatchMode.ONE) {
            boolean removed = WATCHLIST.remove(speciesId, WatchMode.ONE);
            if (!removed) {
                // Another concurrent event already consumed this ONE entry
                return;
            }
            LOGGER.info("[Watchlist] Consumed ONE watch entry for species '{}' upon alert delivery to {} recipient(s).",
                    speciesId, recipients.size());
        }

        // 5. Play alert sound to all eligible recipients
        playAlertSound(recipients);
    }

    private static List<ServerPlayerEntity> resolveRecipients(BroadcastContext context) {
        ServerWorld world = context.getWorld();
        Vec3d pos = context.getPosition();
        if (world == null || pos == null) {
            return Collections.emptyList();
        }

        SpawnNotification.SpawnNotificationConfig config = SpawnNotification.INSTANCE.getConfig();
        boolean broadcastAcrossDimensions = config != null && config.getBroadcastAcrossDimensions();
        int broadcastRange = config != null ? config.getBroadcastRange() : -1;
        boolean ignoreSpectators = config == null || config.getIgnoreSpectators();

        List<ServerPlayerEntity> candidates;
        if (broadcastAcrossDimensions && world.getServer() != null) {
            candidates = world.getServer().getPlayerManager().getPlayerList();
        } else {
            candidates = world.getPlayers();
        }

        double maxDistSq = (broadcastRange > 0) ? (double) broadcastRange * broadcastRange : -1.0;

        List<ServerPlayerEntity> recipients = new ArrayList<>();
        for (ServerPlayerEntity player : candidates) {
            if (ignoreSpectators && player.isSpectator()) {
                continue;
            }
            if (maxDistSq > 0 && player.squaredDistanceTo(pos) > maxDistSq) {
                continue;
            }
            recipients.add(player);
        }
        return recipients;
    }

    private static void playAlertSound(List<ServerPlayerEntity> recipients) {
        for (ServerPlayerEntity player : recipients) {
            player.playSoundToPlayer(
                LegendaryRuleMod.ALERT_SOUND_EVENT,
                SoundCategory.MASTER,
                1.0f,
                1.0f
            );
        }
    }
}
