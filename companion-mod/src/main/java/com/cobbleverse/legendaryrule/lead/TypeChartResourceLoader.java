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
            Map<String, Map<String, Double>> table = new HashMap<>();

            for (Map.Entry<String, JsonElement> attEntry : rootObj.entrySet()) {
                String att = attEntry.getKey().toLowerCase();
                if (!attEntry.getValue().isJsonObject()) {
                    continue;
                }
                JsonObject defObj = attEntry.getValue().getAsJsonObject();
                Map<String, Double> inner = new HashMap<>();
                for (Map.Entry<String, JsonElement> defEntry : defObj.entrySet()) {
                    String def = defEntry.getKey().toLowerCase();
                    try {
                        double mult = defEntry.getValue().getAsDouble();
                        inner.put(def, mult);
                    } catch (Exception ignored) {}
                }
                table.put(att, inner);
            }

            if (table.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new TypeChartData(table));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
