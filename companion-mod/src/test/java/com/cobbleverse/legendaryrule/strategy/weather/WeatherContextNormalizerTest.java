package com.cobbleverse.legendaryrule.strategy.weather;

import com.cobblemon.mod.common.api.battles.interpreter.BasicContext;
import com.cobblemon.mod.common.api.battles.interpreter.BattleContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class WeatherContextNormalizerTest {

    private BasicContext createContext(String id) {
        return new BasicContext(id, 1, BattleContext.Type.WEATHER, null);
    }

    @Test
    @DisplayName("Sunny Day: sunnyday generates harshsunlight and sunny aliases while preserving head")
    void testSunnyDayNormalization() {
        BasicContext raw = createContext("sunnyday");
        Collection<BattleContext> normalized = WeatherContextNormalizer.normalize(List.of(raw));

        assertNotNull(normalized);
        assertEquals(3, normalized.size(), "Should have raw sunnyday + harshsunlight + sunny");

        BattleContext head = normalized.iterator().next();
        assertEquals("sunnyday", head.getId(), "Canonical head must remain original Showdown token");

        Set<String> ids = normalized.stream().map(BattleContext::getId).collect(Collectors.toSet());
        assertTrue(ids.contains("sunnyday"));
        assertTrue(ids.contains("harshsunlight"));
        assertTrue(ids.contains("sunny"));
    }

    @Test
    @DisplayName("Desolate Land: desolateland generates extremelyharshsunlight, harshsunlight, and sunny")
    void testDesolateLandNormalization() {
        BasicContext raw = createContext("desolateland");
        Collection<BattleContext> normalized = WeatherContextNormalizer.normalize(List.of(raw));

        assertNotNull(normalized);
        assertEquals(4, normalized.size());

        BattleContext head = normalized.iterator().next();
        assertEquals("desolateland", head.getId(), "Canonical head must remain desolateland");

        Set<String> ids = normalized.stream().map(BattleContext::getId).collect(Collectors.toSet());
        assertTrue(ids.contains("desolateland"));
        assertTrue(ids.contains("extremelyharshsunlight"));
        assertTrue(ids.contains("harshsunlight"));
        assertTrue(ids.contains("sunny"));
    }

    @Test
    @DisplayName("Rain Dance: raindance generates rain alias while preserving head")
    void testRainDanceNormalization() {
        BasicContext raw = createContext("raindance");
        Collection<BattleContext> normalized = WeatherContextNormalizer.normalize(List.of(raw));

        assertNotNull(normalized);
        assertEquals(2, normalized.size());

        BattleContext head = normalized.iterator().next();
        assertEquals("raindance", head.getId());

        Set<String> ids = normalized.stream().map(BattleContext::getId).collect(Collectors.toSet());
        assertTrue(ids.contains("raindance"));
        assertTrue(ids.contains("rain"));
    }

    @Test
    @DisplayName("Primordial Sea: primordialsea generates heavyrain and rain")
    void testPrimordialSeaNormalization() {
        BasicContext raw = createContext("primordialsea");
        Collection<BattleContext> normalized = WeatherContextNormalizer.normalize(List.of(raw));

        assertNotNull(normalized);
        assertEquals(3, normalized.size());

        BattleContext head = normalized.iterator().next();
        assertEquals("primordialsea", head.getId());

        Set<String> ids = normalized.stream().map(BattleContext::getId).collect(Collectors.toSet());
        assertTrue(ids.contains("primordialsea"));
        assertTrue(ids.contains("heavyrain"));
        assertTrue(ids.contains("rain"));
    }

    @Test
    @DisplayName("Delta Stream: deltastream generates strongwinds")
    void testDeltaStreamNormalization() {
        BasicContext raw = createContext("deltastream");
        Collection<BattleContext> normalized = WeatherContextNormalizer.normalize(List.of(raw));

        assertNotNull(normalized);
        assertEquals(2, normalized.size());

        BattleContext head = normalized.iterator().next();
        assertEquals("deltastream", head.getId());

        Set<String> ids = normalized.stream().map(BattleContext::getId).collect(Collectors.toSet());
        assertTrue(ids.contains("deltastream"));
        assertTrue(ids.contains("strongwinds"));
    }

    @Test
    @DisplayName("Preservation of metadata: turn, type, origin are preserved across aliases")
    void testMetadataPreservation() {
        BasicContext raw = new BasicContext("sunnyday", 4, BattleContext.Type.WEATHER, null);
        Collection<BattleContext> normalized = WeatherContextNormalizer.normalize(List.of(raw));

        for (BattleContext ctx : normalized) {
            assertEquals(4, ctx.getTurn(), "Turn must be preserved");
            assertEquals(BattleContext.Type.WEATHER, ctx.getType(), "Context type must be WEATHER");
            assertNull(ctx.getOrigin(), "Origin must be preserved");
        }
    }

    @Test
    @DisplayName("Idempotence: normalizing twice produces no duplicate alias IDs")
    void testIdempotenceAndDeduplication() {
        BasicContext raw = createContext("sunnyday");
        Collection<BattleContext> once = WeatherContextNormalizer.normalize(List.of(raw));
        Collection<BattleContext> twice = WeatherContextNormalizer.normalize(once);

        assertEquals(once.size(), twice.size(), "Normalizing already normalized list must not add duplicates");
        Set<String> ids = twice.stream().map(BattleContext::getId).collect(Collectors.toSet());
        assertEquals(once.size(), ids.size(), "All IDs in normalized collection must be unique");
    }

    @Test
    @DisplayName("Null and empty handling: returns raw collection safely")
    void testNullAndEmptyHandling() {
        assertNull(WeatherContextNormalizer.normalize(null));
        Collection<BattleContext> empty = Collections.emptyList();
        assertSame(empty, WeatherContextNormalizer.normalize(empty));
    }
}
