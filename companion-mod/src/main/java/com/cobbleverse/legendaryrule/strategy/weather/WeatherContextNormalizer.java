package com.cobbleverse.legendaryrule.strategy.weather;

import com.cobblemon.mod.common.api.battles.interpreter.BasicContext;
import com.cobblemon.mod.common.api.battles.interpreter.BattleContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Normalizes battle weather contexts between Pokémon Showdown raw condition tokens
 * and Run & Bun AI / RCT API expected tokens.
 *
 * Preserves the original context elements in order at the head of the collection
 * (so Cobblemon's firstOrNull() continues to receive the native Showdown ID),
 * while appending alias BasicContext entries needed by PokeMathMax and BattleEffects.
 */
public final class WeatherContextNormalizer {

    private WeatherContextNormalizer() {
    }

    /**
     * Normalizes a collection of weather BattleContext objects by adding alias tokens
     * for Showdown weather condition IDs.
     *
     * @param rawContexts the original raw weather contexts from ContextManager
     * @return a normalized collection containing original contexts plus standard AI aliases,
     *         or rawContexts if null or empty
     */
    public static Collection<BattleContext> normalize(Collection<BattleContext> rawContexts) {
        if (rawContexts == null || rawContexts.isEmpty()) {
            return rawContexts;
        }

        List<BattleContext> normalized = new ArrayList<>(rawContexts);
        Set<String> existingIds = new LinkedHashSet<>();
        for (BattleContext ctx : rawContexts) {
            if (ctx != null && ctx.getId() != null) {
                existingIds.add(ctx.getId().toLowerCase(Locale.ROOT));
            }
        }

        for (BattleContext ctx : rawContexts) {
            if (ctx == null || ctx.getId() == null) {
                continue;
            }
            String rawId = ctx.getId().toLowerCase(Locale.ROOT);
            List<String> aliases = getAliases(rawId);
            for (String alias : aliases) {
                if (!existingIds.contains(alias)) {
                    existingIds.add(alias);
                    normalized.add(new BasicContext(alias, ctx.getTurn(), ctx.getType(), ctx.getOrigin()));
                }
            }
        }

        return Collections.unmodifiableList(normalized);
    }

    /**
     * Returns standard aliases for a given weather token.
     *
     * @param weatherId the weather ID string
     * @return list of alias IDs
     */
    public static List<String> getAliases(String weatherId) {
        if (weatherId == null) {
            return Collections.emptyList();
        }
        String id = weatherId.toLowerCase(Locale.ROOT).trim();
        return switch (id) {
            case "sunnyday" -> List.of("harshsunlight", "sunny");
            case "desolateland" -> List.of("extremelyharshsunlight", "harshsunlight", "sunny");
            case "raindance" -> List.of("rain");
            case "primordialsea" -> List.of("heavyrain", "rain");
            case "deltastream" -> List.of("strongwinds");
            case "harshsunlight" -> List.of("sunnyday", "sunny");
            case "extremelyharshsunlight" -> List.of("desolateland", "harshsunlight", "sunny");
            case "rain" -> List.of("raindance");
            case "heavyrain" -> List.of("primordialsea", "rain");
            case "strongwinds" -> List.of("deltastream");
            default -> Collections.emptyList();
        };
    }
}
