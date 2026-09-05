package com.cobbleverse.legendaryrule.lead;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Runtime-independent loader for the type chart JSON resource.
 * Relies exclusively on standard Java and Gson (no Minecraft/Fabric/Cobblemon/RCT dependencies).
 */
public final class TypeChartResourceLoader {
    public static final String DEFAULT_RESOURCE_PATH = "/typechart_gen9.json";

    private TypeChartResourceLoader() {}

    public static Optional<TypeChartData> load() {
        return loadDefault();
    }

    public static Optional<TypeChartData> loadDefault() {
        return loadFromClasspath(DEFAULT_RESOURCE_PATH);
    }

    public static Optional<TypeChartData> loadFromClasspath(String resourcePath) {
        if (resourcePath == null || resourcePath.isBlank()) {
            return Optional.empty();
        }
        InputStream in = TypeChartResourceLoader.class.getResourceAsStream(resourcePath);
        if (in == null) {
            return Optional.empty();
        }
        return loadFromStream(in);
    }

    private static final java.util.Set<Double> VALID_MULTIPLIERS = java.util.Set.of(0.0, 0.25, 0.5, 1.0, 2.0, 4.0);

    public static Optional<TypeChartData> loadFromStream(InputStream stream) {
        if (stream == null) {
            return Optional.empty();
        }
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (!root.isJsonObject()) {
                return Optional.empty();
            }
            JsonObject rootObj = root.getAsJsonObject();

            // Validate attack types: raw keys must match CANONICAL_TYPES exactly (no case variants or extras)
            if (!rootObj.keySet().equals(TypeChartData.CANONICAL_TYPES)) {
                return Optional.empty();
            }

            Map<String, Map<String, Double>> table = new HashMap<>();

            for (String att : TypeChartData.CANONICAL_TYPES) {
                JsonElement defElem = rootObj.get(att);
                if (defElem == null || !defElem.isJsonObject()) {
                    return Optional.empty();
                }
                JsonObject defObj = defElem.getAsJsonObject();

                // Validate defender types: raw keys must match CANONICAL_TYPES exactly (no case variants or extras)
                if (!defObj.keySet().equals(TypeChartData.CANONICAL_TYPES)) {
                    return Optional.empty();
                }

                Map<String, Double> inner = new HashMap<>();
                for (String def : TypeChartData.CANONICAL_TYPES) {
                    JsonElement valElem = defObj.get(def);
                    if (valElem == null || !valElem.isJsonPrimitive() || !valElem.getAsJsonPrimitive().isNumber()) {
                        return Optional.empty();
                    }
                    double mult = valElem.getAsDouble();
                    if (!VALID_MULTIPLIERS.contains(mult)) {
                        return Optional.empty();
                    }
                    inner.put(def, mult);
                }
                table.put(att, inner);
            }

            return Optional.of(new TypeChartData(table));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
