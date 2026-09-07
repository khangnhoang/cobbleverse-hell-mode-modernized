package com.cobbleverse.legendaryrule.lead;

import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

class DynamicLeadResourceListenerTest {

    private DynamicLeadResourceListener listener;

    @BeforeEach
    void setUp() {
        listener = new DynamicLeadResourceListener();
        LeadSelectionConfig.setDatapackTrainerConfigs(Map.of());
        LeadSelectionConfig.setEnabled(true);
    }

    private Resource createResource(String json) {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        return new Resource(null, () -> new ByteArrayInputStream(bytes));
    }

    private ResourceManager createMockManager(Map<Identifier, Resource> resourceMap) {
        return (ResourceManager) Proxy.newProxyInstance(
                ResourceManager.class.getClassLoader(),
                new Class<?>[]{ResourceManager.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("findResources")) {
                        String prefix = (String) args[0];
                        @SuppressWarnings("unchecked")
                        Predicate<Identifier> predicate = (Predicate<Identifier>) args[1];
                        Map<Identifier, Resource> result = new HashMap<>();
                        for (Map.Entry<Identifier, Resource> entry : resourceMap.entrySet()) {
                            if (entry.getKey().getPath().startsWith(prefix) && predicate.test(entry.getKey())) {
                                result.put(entry.getKey(), entry.getValue());
                            }
                        }
                        return result;
                    }
                    return null;
                }
        );
    }

    @Test
    void testFabricIdentifier() {
        assertEquals("rct_legendary_rule", listener.getFabricId().getNamespace());
        assertEquals("dynamic_lead_presets", listener.getFabricId().getPath());
    }

    @Test
    void testReloadParsesValidTrainerPresets() {
        String erikaJson = """
        {
          "team": [],
          "leadPresets": [
            {
              "id": "rain_swift_swim",
              "leadSlots": [0, 1],
              "baseWeight": 1,
              "favoredAgainst": ["fire"],
              "description": "Rain Swift Swim core"
            },
            {
              "id": "grass_terrain",
              "leadSlots": [4, 5],
              "baseWeight": 0
            }
          ]
        }
        """;

        String surgeJson = """
        {
          "team": [],
          "leadPresets": [
            {
              "id": "anti_gastrodon",
              "leadSlots": [5, 1],
              "baseWeight": 0,
              "favoredAgainstSpecies": ["gastrodon"]
            }
          ]
        }
        """;

        String noPresetsJson = """
        {
          "team": []
        }
        """;

        Map<Identifier, Resource> resources = Map.of(
                Identifier.of("rctmod", "trainers/kanto_erika.json"), createResource(erikaJson),
                Identifier.of("rctmod", "trainers/kanto_ltsurge.json"), createResource(surgeJson),
                Identifier.of("rctmod", "trainers/kanto_brock.json"), createResource(noPresetsJson)
        );

        ResourceManager manager = createMockManager(resources);
        listener.reload(manager);

        Optional<TrainerLeadConfig> erikaOpt = LeadSelectionConfig.getTrainerConfig("kanto_erika");
        assertTrue(erikaOpt.isPresent());
        assertEquals(2, erikaOpt.get().attempts().size());
        assertEquals("rain_swift_swim", erikaOpt.get().attempts().get(0).id());
        assertEquals(List.of("fire"), erikaOpt.get().attempts().get(0).favoredAgainst());

        Optional<TrainerLeadConfig> surgeOpt = LeadSelectionConfig.getTrainerConfig("kanto_ltsurge");
        assertTrue(surgeOpt.isPresent());
        assertEquals(1, surgeOpt.get().attempts().size());
        assertEquals("anti_gastrodon", surgeOpt.get().attempts().get(0).id());
        assertEquals(List.of("gastrodon"), surgeOpt.get().attempts().get(0).favoredAgainstSpecies());

        // Brock has no leadPresets -> must not be configured
        Optional<TrainerLeadConfig> brockOpt = LeadSelectionConfig.getTrainerConfig("kanto_brock");
        assertFalse(brockOpt.isPresent());
    }

    @Test
    void testReloadReplacesOldConfigsAtomically() {
        // Initial reload with trainer A
        Map<Identifier, Resource> initial = Map.of(
                Identifier.of("rctmod", "trainers/kanto_erika.json"), createResource("""
                {
                  "leadPresets": [
                    { "id": "preset_a", "leadSlots": [0, 1] }
                  ]
                }
                """)
        );
        listener.reload(createMockManager(initial));
        assertTrue(LeadSelectionConfig.getTrainerConfig("kanto_erika").isPresent());

        // Second reload without trainer A (e.g. datapack disabled)
        Map<Identifier, Resource> second = Map.of(
                Identifier.of("rctmod", "trainers/kanto_sabrina.json"), createResource("""
                {
                  "leadPresets": [
                    { "id": "preset_b", "leadSlots": [0, 1] }
                  ]
                }
                """)
        );
        listener.reload(createMockManager(second));

        assertFalse(LeadSelectionConfig.getTrainerConfig("kanto_erika").isPresent(), "Old trainer must be removed on atomic swap");
        assertTrue(LeadSelectionConfig.getTrainerConfig("kanto_sabrina").isPresent());
    }

    @Test
    void testReloadHandlesMalformedTrainerJsonGracefully() {
        Map<Identifier, Resource> resources = Map.of(
                Identifier.of("rctmod", "trainers/broken.json"), createResource("{ malformed json :::"),
                Identifier.of("rctmod", "trainers/valid.json"), createResource("""
                {
                  "leadPresets": [
                    { "id": "ok", "leadSlots": [0, 1] }
                  ]
                }
                """)
        );

        listener.reload(createMockManager(resources));

        assertFalse(LeadSelectionConfig.getTrainerConfig("broken").isPresent());
        assertTrue(LeadSelectionConfig.getTrainerConfig("valid").isPresent());
    }
}
