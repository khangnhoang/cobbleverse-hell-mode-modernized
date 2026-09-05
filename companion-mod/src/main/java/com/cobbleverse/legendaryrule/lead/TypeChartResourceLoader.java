package com.cobbleverse.legendaryrule.lead;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
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

    private static final BigDecimal BD_0 = BigDecimal.ZERO;
    private static final BigDecimal BD_0_25 = new BigDecimal("0.25");
    private static final BigDecimal BD_0_5 = new BigDecimal("0.5");
    private static final BigDecimal BD_1 = BigDecimal.ONE;
    private static final BigDecimal BD_2 = new BigDecimal("2");
    private static final BigDecimal BD_4 = new BigDecimal("4");

    private static boolean isCanonicalMultiplier(BigDecimal bd) {
        if (bd == null) {
            return false;
        }
        return bd.compareTo(BD_0) == 0
            || bd.compareTo(BD_0_25) == 0
            || bd.compareTo(BD_0_5) == 0
            || bd.compareTo(BD_1) == 0
            || bd.compareTo(BD_2) == 0
            || bd.compareTo(BD_4) == 0;
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
                    BigDecimal bd;
                    try {
                        bd = valElem.getAsBigDecimal();
                    } catch (Exception e) {
                        return Optional.empty();
                    }
                    if (!isCanonicalMultiplier(bd)) {
                        return Optional.empty();
                    }
                    double mult = bd.doubleValue();
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
