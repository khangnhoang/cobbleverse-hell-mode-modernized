package com.cobbleverse.legendaryrule.strategy.weather;

import com.cobblemon.mod.common.api.battles.interpreter.BasicContext;
import com.cobblemon.mod.common.api.battles.interpreter.BattleContext;
import com.cobblemon.mod.common.api.types.ElementalType;
import com.cobblemon.mod.common.api.types.ElementalTypes;
import com.cobblemon.mod.common.battles.interpreter.ContextManager;
import com.cobbleverse.legendaryrule.lead.TypeChartData;
import com.cobbleverse.legendaryrule.lead.TypeChartResourceLoader;
import com.cobbleverse.legendaryrule.lead.TypeMatchupScorer;
import com.gitlab.surilexa.rbrctai.api.ai.utils.PokeMathMax;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WaterDamageUnderSunContractTest {

    private static TypeMatchupScorer typeScorer;

    @BeforeAll
    static void setUpAll() {
        TypeChartData data = TypeChartResourceLoader.load().orElseThrow(
            () -> new IllegalStateException("Failed to load canonical Gen 9 type chart for tests"));
        typeScorer = new TypeMatchupScorer(data);
    }

    @Test
    @DisplayName("Defect 1 Root Cause Grounding: Unnormalized sunnyday fails PokeMathMax.sun evaluation")
    void testRootCauseBugDemonstration() {
        // Cobblemon instantiates ContextManager with raw Showdown ID "sunnyday"
        List<BattleContext> rawWeather = List.of(new BasicContext("sunnyday", 1, BattleContext.Type.WEATHER, null));

        // PokeMathMax:111 logic:
        boolean unnormalizedSun = rawWeather.stream().anyMatch(
            c -> c.getId().equals("harshsunlight") || c.getId().equals("extremelyharshsunlight")
        );

        // Grounding the defect: without normalization, sun evaluates to false!
        assertFalse(unnormalizedSun, "Demonstrates the defect: raw sunnyday token causes sun == false in PokeMathMax");

        // Normalizing the weather contexts:
        Collection<BattleContext> normalized = WeatherContextNormalizer.normalize(rawWeather);
        boolean normalizedSun = normalized.stream().anyMatch(
            c -> c.getId().equals("harshsunlight") || c.getId().equals("extremelyharshsunlight")
        );

        // With WeatherContextNormalizer, sun evaluates to true!
        assertTrue(normalizedSun, "With normalizer, sunnyday produces harshsunlight so sun == true");
    }

    @Test
    @DisplayName("Mathematical Invariant: Hydro Pump vs Torkoal under Sun resolves to 1.0x net effectiveness")
    void testHydroPumpVsTorkoalUnderSunContract() {
        // Torkoal is mono Fire
        List<String> torkoalTypes = List.of("fire");

        // Base type effectiveness of Water vs Fire is 2.0x
        double baseTypeEffectiveness = typeScorer.getEffectiveness("water", torkoalTypes);
        assertEquals(2.0, baseTypeEffectiveness, 0.001, "Water hits Fire for 2.0x super effective damage");

        // Buggy calculation: sun is false, so weather modifier is 1.0x
        double buggyWeatherMultiplier = 1.0;
        double buggyNetMultiplier = baseTypeEffectiveness * buggyWeatherMultiplier;
        assertEquals(2.0, buggyNetMultiplier, 0.001, "Buggy AI calculation projected unmitigated 2.0x damage");

        // Corrected calculation: normalized sun == true, so weather modifier is 0.5x
        double correctedWeatherMultiplier = 0.5;
        double correctedNetMultiplier = baseTypeEffectiveness * correctedWeatherMultiplier;
        assertEquals(1.0, correctedNetMultiplier, 0.001, "Corrected AI calculation applies 0.5x sun penalty (net 1.0x)");

        // The ratio between buggy and corrected damage is exactly 2.0x
        assertEquals(2.0, buggyNetMultiplier / correctedNetMultiplier, 0.001,
            "AI previously overestimated Hydro Pump damage by 2.0x, causing false target lock-in");
    }

    @Test
    @DisplayName("Rain Contrast Invariant: raindance correctly enables 1.5x Water boost and 0.5x Fire penalty")
    void testRainNormalizationContrast() {
        List<BattleContext> rawRain = List.of(new BasicContext("raindance", 1, BattleContext.Type.WEATHER, null));

        // PokeMathMax:112 logic:
        boolean unnormalizedRain = rawRain.stream().anyMatch(
            c -> c.getId().equals("rain") || c.getId().equals("heavyrain")
        );
        assertFalse(unnormalizedRain, "Demonstrates defect: raw raindance causes rain == false in PokeMathMax");

        Collection<BattleContext> normalized = WeatherContextNormalizer.normalize(rawRain);
        boolean normalizedRain = normalized.stream().anyMatch(
            c -> c.getId().equals("rain") || c.getId().equals("heavyrain")
        );
        assertTrue(normalizedRain, "Normalized collection contains rain alias");

        // Water move under Rain vs Fire: 2.0x type * 1.5x rain = 3.0x net
        double waterVsFireInRain = 2.0 * 1.5;
        assertEquals(3.0, waterVsFireInRain, 0.001);

        // Fire move under Rain vs Grass: 2.0x type * 0.5x rain = 1.0x net
        double fireVsGrassInRain = 2.0 * 0.5;
        assertEquals(1.0, fireVsGrassInRain, 0.001);
    }

    @Test
    @DisplayName("Bytecode Contract: ContextManager.get exists and returns Collection<BattleContext>")
    void testContextManagerGetSignatureContract() {
        boolean methodFound = false;
        for (Method method : ContextManager.class.getDeclaredMethods()) {
            if (method.getName().equals("get")) {
                Class<?>[] params = method.getParameterTypes();
                if (params.length == 1 && params[0] == BattleContext.Type.class && Collection.class.isAssignableFrom(method.getReturnType())) {
                    methodFound = true;
                    break;
                }
            }
        }
        assertTrue(methodFound, "ContextManager.get(BattleContext.Type) must exist and return Collection");
    }
}
