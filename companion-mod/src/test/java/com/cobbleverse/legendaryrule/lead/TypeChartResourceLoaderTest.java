package com.cobbleverse.legendaryrule.lead;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class TypeChartResourceLoaderTest {

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
    void testLoadFromValidStream() {
        String json = """
        {
          "fire": { "grass": 2.0, "water": 0.5 },
          "water": { "fire": 2.0, "grass": 0.5 }
        }
        """;
        InputStream stream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        Optional<TypeChartData> opt = TypeChartResourceLoader.loadFromStream(stream);
        assertTrue(opt.isPresent());
        TypeChartData data = opt.get();
        assertEquals(2.0, data.getMultiplier("fire", "grass"));
        assertEquals(0.5, data.getMultiplier("fire", "water"));
        assertEquals(1.0, data.getMultiplier("fire", "rock")); // default unmapped
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
}
