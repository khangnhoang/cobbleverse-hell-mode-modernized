package com.cobbleverse.legendaryrule.lead;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Runtime-independent configuration loader and structural validator.
 * Relies on standard Java and Gson (with runtime-safe fallback for FabricLoader).
 */
public final class LeadSelectionConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("rct_legendary_rule");
    public static final String CONFIG_FILENAME = "cobbleverse-hell-mode-leads.json";

    private static volatile boolean enabled = true;
    private static volatile Map<String, TrainerLeadConfig> trainerConfigs = Collections.emptyMap();
    private static Path configPath = null;

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    public static Optional<TrainerLeadConfig> getTrainerConfig(String trainerId) {
        if (!enabled || trainerId == null) {
            return Optional.empty();
        }
        TrainerLeadConfig config = trainerConfigs.get(trainerId.toLowerCase(Locale.ROOT));
        return Optional.ofNullable(config);
    }

    public static Path getConfigPath() {
        if (configPath == null) {
            try {
                configPath = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILENAME);
            } catch (Throwable t) {
                // Fallback for tests running outside Fabric Loader
                configPath = Path.of("config", CONFIG_FILENAME);
            }
        }
        return configPath;
    }

    public static void setConfigPath(Path path) {
        configPath = path;
    }

    public static synchronized void init() {
        load(getConfigPath());
    }

    public static synchronized void load(Path path) {
        if (path == null) {
            enabled = true;
            trainerConfigs = Collections.emptyMap();
            return;
        }

        if (!Files.exists(path)) {
            LOGGER.info("Lead selection config file {} not found; dynamic lead presets inactive.", path);
            enabled = true;
            trainerConfigs = Collections.emptyMap();
            return;
        }

        try (Reader reader = Files.newBufferedReader(path)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            loadFromJson(root);
            LOGGER.info("Loaded dynamic lead presets for {} trainers from {}", trainerConfigs.size(), path);
        } catch (Exception e) {
            LOGGER.error("Failed to load lead selection config from {}: {}. Falling back to native ordering.", path, e.getMessage());
            enabled = false;
            trainerConfigs = Collections.emptyMap();
        }
    }

    public static synchronized void loadFromJson(JsonObject root) {
        if (root.has("enabled")) {
            enabled = root.get("enabled").getAsBoolean();
        } else {
            enabled = true;
        }

        Map<String, TrainerLeadConfig> newConfigs = new HashMap<>();
        if (root.has("trainers") && root.get("trainers").isJsonObject()) {
            JsonObject trainersObj = root.getAsJsonObject("trainers");
            for (Map.Entry<String, JsonElement> entry : trainersObj.entrySet()) {
                String trainerId = entry.getKey().toLowerCase(Locale.ROOT);
                if (!entry.getValue().isJsonObject()) {
                    continue;
                }
                JsonObject trainerObj = entry.getValue().getAsJsonObject();
                if (!trainerObj.has("attempts") || !trainerObj.get("attempts").isJsonArray()) {
                    continue;
                }

                JsonArray attemptsArr = trainerObj.getAsJsonArray("attempts");
                List<LeadAttempt> validAttempts = new ArrayList<>();

                for (JsonElement attemptElem : attemptsArr) {
                    if (!attemptElem.isJsonObject()) {
                        continue;
                    }
                    JsonObject attObj = attemptElem.getAsJsonObject();
                    try {
                        LeadAttempt attempt = parseAttempt(attObj);
                        validAttempts.add(attempt);
                    } catch (Exception e) {
                        LOGGER.warn("Skipping structurally invalid lead attempt in trainer '{}': {}", trainerId, e.getMessage());
                    }
                }

                if (!validAttempts.isEmpty()) {
                    newConfigs.put(trainerId, new TrainerLeadConfig(validAttempts));
                }
            }
        }
        trainerConfigs = Collections.unmodifiableMap(newConfigs);
    }

    private static LeadAttempt parseAttempt(JsonObject obj) {
        if (!obj.has("id") || obj.get("id").getAsString().isBlank()) {
            throw new IllegalArgumentException("Attempt missing required 'id'");
        }
        String id = obj.get("id").getAsString().trim();

        if (!obj.has("leadSlots") || !obj.get("leadSlots").isJsonArray()) {
            throw new IllegalArgumentException("Attempt '" + id + "' missing required 'leadSlots' array");
        }
        JsonArray slotsArr = obj.getAsJsonArray("leadSlots");
        if (slotsArr.size() != 2) {
            throw new IllegalArgumentException("Attempt '" + id + "' leadSlots must contain exactly 2 indices, got: " + slotsArr.size());
        }

        int slot0 = slotsArr.get(0).getAsInt();
        int slot1 = slotsArr.get(1).getAsInt();
        if (slot0 < 0 || slot1 < 0) {
            throw new IllegalArgumentException("Attempt '" + id + "' leadSlots indices must be >= 0, got: [" + slot0 + ", " + slot1 + "]");
        }
        if (slot0 == slot1) {
            throw new IllegalArgumentException("Attempt '" + id + "' leadSlots indices must be distinct, got: [" + slot0 + ", " + slot1 + "]");
        }

        int baseWeight = 0;
        if (obj.has("baseWeight")) {
            baseWeight = obj.get("baseWeight").getAsInt();
            if (baseWeight < -2 || baseWeight > 2) {
                throw new IllegalArgumentException("Attempt '" + id + "' baseWeight must be between -2 and +2, got: " + baseWeight);
            }
        }

        List<ExpectedLeadMember> expectedMembers = new ArrayList<>();
        if (obj.has("expectedLeadMembers") && !obj.get("expectedLeadMembers").isJsonNull()) {
            if (!obj.get("expectedLeadMembers").isJsonArray()) {
                throw new IllegalArgumentException("Attempt '" + id + "' expectedLeadMembers must be a JSON array");
            }
            JsonArray expArr = obj.getAsJsonArray("expectedLeadMembers");
            if (expArr.size() != 2) {
                throw new IllegalArgumentException("Attempt '" + id + "' expectedLeadMembers if present must contain exactly 2 members, got: " + expArr.size());
            }
            for (JsonElement expElem : expArr) {
                if (!expElem.isJsonObject()) {
                    throw new IllegalArgumentException("ExpectedLeadMember in '" + id + "' must be a JSON object");
                }
                JsonObject expObj = expElem.getAsJsonObject();
                if (!expObj.has("species") || expObj.get("species").getAsString().isBlank()) {
                    throw new IllegalArgumentException("ExpectedLeadMember in '" + id + "' missing required 'species'");
                }
                String species = expObj.get("species").getAsString().trim().toLowerCase(Locale.ROOT);
                String form = expObj.has("form") && !expObj.get("form").isJsonNull()
                        ? expObj.get("form").getAsString().trim().toLowerCase(Locale.ROOT)
                        : null;

                List<String> aspects = new ArrayList<>();
                if (expObj.has("requiredAspects") && expObj.get("requiredAspects").isJsonArray()) {
                    for (JsonElement aspElem : expObj.getAsJsonArray("requiredAspects")) {
                        if (aspElem.isJsonPrimitive() && !aspElem.getAsString().isBlank()) {
                            aspects.add(aspElem.getAsString().trim().toLowerCase(Locale.ROOT));
                        }
                    }
                }
                expectedMembers.add(new ExpectedLeadMember(species, form, aspects));
            }
        }

        String description = obj.has("description") ? obj.get("description").getAsString() : "";
        return new LeadAttempt(id, new int[]{slot0, slot1}, baseWeight, expectedMembers, description);
    }
}
