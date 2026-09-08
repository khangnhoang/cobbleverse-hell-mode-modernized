package com.cobbleverse.legendaryrule.lead;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Datapack reload listener that scans winning trainer resources for embedded leadPresets
 * and updates the atomic trainerConfigs map in LeadSelectionConfig.
 */
public final class DynamicLeadResourceListener implements SimpleSynchronousResourceReloadListener {
    private static final Logger LOGGER = LoggerFactory.getLogger("rct_legendary_rule");
    private static final Identifier ID = Identifier.of("rct_legendary_rule", "dynamic_lead_presets");
    private static final String TRAINER_PREFIX = "trainers/";
    private static final String JSON_SUFFIX = ".json";

    @Override
    public Identifier getFabricId() {
        return ID;
    }

    @Override
    public void reload(ResourceManager manager) {
        Map<String, TrainerLeadConfig> newConfigs = new HashMap<>();

        try {
            Map<Identifier, Resource> resources = manager.findResources("trainers",
                    id -> id.getNamespace().equals("rctmod") && id.getPath().endsWith(JSON_SUFFIX));

            for (Map.Entry<Identifier, Resource> entry : resources.entrySet()) {
                Identifier resId = entry.getKey();
                String path = resId.getPath();
                if (!path.startsWith(TRAINER_PREFIX) || !path.endsWith(JSON_SUFFIX)) {
                    continue;
                }

                String trainerId = path.substring(TRAINER_PREFIX.length(), path.length() - JSON_SUFFIX.length())
                        .toLowerCase(Locale.ROOT);

                try (Reader reader = entry.getValue().getReader()) {
                    JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                    if (root.has("leadPresets") && root.get("leadPresets").isJsonArray()) {
                        JsonArray presetsArray = root.getAsJsonArray("leadPresets");
                        List<LeadAttempt> validAttempts = LeadSelectionConfig.parseAttemptsArray(presetsArray);
                        if (!validAttempts.isEmpty()) {
                            newConfigs.put(trainerId, new TrainerLeadConfig(validAttempts));
                        }
                    }
                } catch (Exception e) {
                    LOGGER.warn("[HellMode-Lead] Failed to parse lead presets for trainer '{}' from {}: {}",
                            trainerId, resId, e.getMessage());
                }
            }

            LeadSelectionConfig.setDatapackTrainerConfigs(newConfigs);
            LOGGER.info("[HellMode-Lead] Reloaded dynamic lead presets for {} trainers from datapacks: {}",
                    newConfigs.size(), newConfigs.keySet());
        } catch (Exception e) {
            LOGGER.error("[HellMode-Lead] Critical failure reloading dynamic lead presets: {}", e.getMessage(), e);
        }
    }
}
