package com.cobbleverse.legendaryrule.lead;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class TypeChartResourceLoaderTest {

    private Map<String, Map<String, Double>> createValid18x18Matrix() {
        Map<String, Map<String, Double>> matrix = new HashMap<>();
        for (String att : TypeChartData.CANONICAL_TYPES) {
            Map<String, Double> inner = new HashMap<>();
            for (String def : TypeChartData.CANONICAL_TYPES) {
                inner.put(def, 1.0);
            }
            matrix.put(att, inner);
        }
        return matrix;
    }

    private InputStream matrixToStream(Map<String, Map<String, Double>> matrix) {
        String json = new Gson().toJson(matrix);
        return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void testLoadDefaultProductionResource() {
        Optional<TypeChartData> opt = TypeChartResourceLoader.loadDefault();
        assertTrue(opt.isPresent(), "Production /typechart_gen9.json must be present and load successfully");
        TypeChartData data = opt.get();
        assertEquals(2.0, data.getMultiplier("water", "fire"));
        assertEquals(0.0, data.getMultiplier("electric", "ground"));
        assertEquals(0.0, data.getMultiplier("ghost", "normal"));
    }

    @Test
    void testLoadValidComplete18x18Chart() {
        Map<String, Map<String, Double>> matrix = createValid18x18Matrix();
        matrix.get("fire").put("grass", 2.0);
        matrix.get("fire").put("water", 0.5);
        matrix.get("electric").put("ground", 0.0);

        Optional<TypeChartData> opt = TypeChartResourceLoader.loadFromStream(matrixToStream(matrix));
        assertTrue(opt.isPresent(), "Complete 18x18 chart with valid multipliers must be accepted");
        TypeChartData data = opt.get();
        assertEquals(2.0, data.getMultiplier("fire", "grass"));
        assertEquals(0.5, data.getMultiplier("fire", "water"));
        assertEquals(0.0, data.getMultiplier("electric", "ground"));
        assertEquals(1.0, data.getMultiplier("normal", "normal"));
    }

    @Test
    void testRejectMissingAttackRow() {
        Map<String, Map<String, Double>> matrix = createValid18x18Matrix();
        matrix.remove("fairy"); // Only 17 attack rows

        Optional<TypeChartData> opt = TypeChartResourceLoader.loadFromStream(matrixToStream(matrix));
        assertTrue(opt.isEmpty(), "Chart missing an attack row must be rejected");
    }

    @Test
    void testRejectMissingDefenderPair() {
        Map<String, Map<String, Double>> matrix = createValid18x18Matrix();
        matrix.get("fire").remove("water"); // Fire attack row missing defender 'water'

        Optional<TypeChartData> opt = TypeChartResourceLoader.loadFromStream(matrixToStream(matrix));
        assertTrue(opt.isEmpty(), "Chart missing a defender pair in an attack row must be rejected");
    }

    @Test
    void testRejectExtraUnknownAttackType() {
        Map<String, Map<String, Double>> matrix = createValid18x18Matrix();
        Map<String, Double> extraRow = new HashMap<>();
        for (String def : TypeChartData.CANONICAL_TYPES) {
            extraRow.put(def, 1.0);
        }
        matrix.put("shadow", extraRow); // Unknown extra attack type

        Optional<TypeChartData> opt = TypeChartResourceLoader.loadFromStream(matrixToStream(matrix));
        assertTrue(opt.isEmpty(), "Chart with unknown extra attack type must be rejected");
    }

    @Test
    void testRejectExtraUnknownDefenderType() {
        Map<String, Map<String, Double>> matrix = createValid18x18Matrix();
        matrix.get("fire").put("bird", 1.0); // Unknown extra defender type

        Optional<TypeChartData> opt = TypeChartResourceLoader.loadFromStream(matrixToStream(matrix));
        assertTrue(opt.isEmpty(), "Chart with unknown extra defender type must be rejected");
    }

    @Test
    void testRejectInvalidMultiplier() {
        Map<String, Map<String, Double>> matrix = createValid18x18Matrix();
        matrix.get("fire").put("grass", 1.5); // 1.5 is not in {0.0, 0.25, 0.5, 1.0, 2.0, 4.0}

        Optional<TypeChartData> opt1 = TypeChartResourceLoader.loadFromStream(matrixToStream(matrix));
        assertTrue(opt1.isEmpty(), "Multiplier 1.5 must be rejected");

        matrix.get("fire").put("grass", -1.0);
        Optional<TypeChartData> opt2 = TypeChartResourceLoader.loadFromStream(matrixToStream(matrix));
        assertTrue(opt2.isEmpty(), "Negative multiplier must be rejected");
    }

    @Test
    void testTypeChartDataThrowsOnUnmappedOrMissingPair() {
        Map<String, Map<String, Double>> matrix = createValid18x18Matrix();
        TypeChartData data = new TypeChartData(matrix);

        assertThrows(IllegalArgumentException.class, () -> data.getMultiplier("unknown_type", "normal"));
        assertThrows(IllegalArgumentException.class, () -> data.getMultiplier("fire", "unknown_type"));
        assertThrows(IllegalArgumentException.class, () -> data.getMultiplier(null, "normal"));
        assertThrows(IllegalArgumentException.class, () -> data.getMultiplier("fire", null));
    }

    @Test
    void testLoadNullOrMissingStream() {
        Optional<TypeChartData> opt1 = TypeChartResourceLoader.loadFromStream(null);
        assertTrue(opt1.isEmpty());

        Optional<TypeChartData> opt2 = TypeChartResourceLoader.loadFromClasspath("/non_existent_typechart.json");
        assertTrue(opt2.isEmpty());
    }

    @Test
    void testLoadMalformedJsonHandlesGracefully() {
        String badJson = "{ not-valid-json ]";
        InputStream stream = new ByteArrayInputStream(badJson.getBytes(StandardCharsets.UTF_8));
        Optional<TypeChartData> opt = TypeChartResourceLoader.loadFromStream(stream);
        assertTrue(opt.isEmpty(), "Malformed JSON should return Optional.empty() without crashing");
    }

    @Test
    void testLoadEmptyJsonReturnsEmpty() {
        String emptyJson = "{}";
        InputStream stream = new ByteArrayInputStream(emptyJson.getBytes(StandardCharsets.UTF_8));
        Optional<TypeChartData> opt = TypeChartResourceLoader.loadFromStream(stream);
        assertTrue(opt.isEmpty(), "Empty JSON object should return Optional.empty()");
    }

    @Test
    void testRejectNearCanonicalMultiplier() {
        Map<String, Map<String, Double>> matrix = createValid18x18Matrix();
        matrix.get("fire").put("grass", 1.0000005);
        Optional<TypeChartData> opt = TypeChartResourceLoader.loadFromStream(matrixToStream(matrix));
        assertTrue(opt.isEmpty(), "Multiplier 1.0000005 must be rejected without epsilon tolerance");
    }

    @Test
    void testRejectCaseVariantAttackKey() {
        Map<String, Map<String, Double>> matrix = createValid18x18Matrix();
        Map<String, Double> row = matrix.remove("fire");
        matrix.put("Fire", row); // Cased attack key
        Optional<TypeChartData> opt = TypeChartResourceLoader.loadFromStream(matrixToStream(matrix));
        assertTrue(opt.isEmpty(), "Case-variant attack key 'Fire' must be rejected");
    }

    @Test
    void testRejectCaseVariantDefenderKey() {
        Map<String, Map<String, Double>> matrix = createValid18x18Matrix();
        matrix.get("fire").remove("water");
        matrix.get("fire").put("Water", 0.5); // Cased defender key
        Optional<TypeChartData> opt = TypeChartResourceLoader.loadFromStream(matrixToStream(matrix));
        assertTrue(opt.isEmpty(), "Case-variant defender key 'Water' must be rejected");
    }
}
