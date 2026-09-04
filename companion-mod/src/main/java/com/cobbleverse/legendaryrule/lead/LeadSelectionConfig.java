package com.cobbleverse.legendaryrule.lead;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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
 * Runtime-independent configuration loader and strict structural validator.
 * Relies exclusively on standard Java and Gson (no Fabric, Minecraft, or logging dependencies).
 */
public final class LeadSelectionConfig {
    public static final String CONFIG_FILENAME = "cobbleverse-hell-mode-leads.json";

    public record ConfigLoadResult(boolean success, int loadedTrainersCount, String errorMessage) {}

    private static volatile boolean enabled = true;
    private static volatile Map<String, TrainerLeadConfig> trainerConfigs = Collections.emptyMap();

    private LeadSelectionConfig() {}

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

    public static synchronized ConfigLoadResult load(Path path) {
        if (path == null) {
            enabled = true;
            trainerConfigs = Collections.emptyMap();
            return new ConfigLoadResult(true, 0, "Null path provided");
        }

        if (!Files.exists(path)) {
            enabled = true;
            trainerConfigs = Collections.emptyMap();
            return new ConfigLoadResult(true, 0, "Config file not found");
        }

        try (Reader reader = Files.newBufferedReader(path)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            loadFromJson(root);
            return new ConfigLoadResult(true, trainerConfigs.size(), null);
        } catch (Exception e) {
            enabled = false;
            trainerConfigs = Collections.emptyMap();
            return new ConfigLoadResult(false, 0, e.getMessage());
        }
    }

    public static synchronized void loadFromJson(JsonObject root) {
        if (root.has("enabled")) {
            JsonElement enElem = root.get("enabled");
            if (enElem.isJsonPrimitive() && enElem.getAsJsonPrimitive().isBoolean()) {
                enabled = enElem.getAsBoolean();
            } else {
                enabled = true;
            }
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
                    } catch (Exception ignored) {
                        // Per-attempt isolation: skip malformed attempt, preserve valid siblings
                    }
                }

                if (!validAttempts.isEmpty()) {
                    newConfigs.put(trainerId, new TrainerLeadConfig(validAttempts));
                }
            }
        }
        trainerConfigs = Collections.unmodifiableMap(newConfigs);
    }

    private static int parseExactInt(JsonElement elem, String fieldName) {
        if (elem == null || !elem.isJsonPrimitive() || !elem.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException(fieldName + " must be a numeric integer, got: " + elem);
        }
        String raw = elem.getAsString();
        if (raw.contains(".") || raw.contains("e") || raw.contains("E")) {
            throw new IllegalArgumentException(fieldName + " must not contain fractional or floating-point components, got: " + raw);
        }
        try {
            return elem.getAsBigDecimal().intValueExact();
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException(fieldName + " must be an exact integer, got: " + raw);
        }
    }

    private static String parseNonBlankString(JsonElement elem, String fieldName) {
        if (elem == null || !elem.isJsonPrimitive() || !elem.getAsJsonPrimitive().isString() || elem.getAsString().isBlank()) {
            throw new IllegalArgumentException(fieldName + " must be a non-blank string, got: " + elem);
        }
        return elem.getAsString().trim();
    }

    private static LeadAttempt parseAttempt(JsonObject obj) {
        if (!obj.has("id")) {
            throw new IllegalArgumentException("Attempt missing required 'id'");
        }
        String id = parseNonBlankString(obj.get("id"), "Attempt id");

        if (!obj.has("leadSlots") || !obj.get("leadSlots").isJsonArray()) {
            throw new IllegalArgumentException("Attempt '" + id + "' missing required 'leadSlots' array");
        }
        JsonArray slotsArr = obj.getAsJsonArray("leadSlots");
        if (slotsArr.size() != 2) {
            throw new IllegalArgumentException("Attempt '" + id + "' leadSlots must contain exactly 2 indices, got: " + slotsArr.size());
        }

        int slot0 = parseExactInt(slotsArr.get(0), "Attempt '" + id + "' leadSlots[0]");
        int slot1 = parseExactInt(slotsArr.get(1), "Attempt '" + id + "' leadSlots[1]");
        if (slot0 < 0 || slot1 < 0) {
            throw new IllegalArgumentException("Attempt '" + id + "' leadSlots indices must be >= 0, got: [" + slot0 + ", " + slot1 + "]");
        }
        if (slot0 == slot1) {
            throw new IllegalArgumentException("Attempt '" + id + "' leadSlots indices must be distinct, got: [" + slot0 + ", " + slot1 + "]");
        }

        int baseWeight = 0;
        if (obj.has("baseWeight") && !obj.get("baseWeight").isJsonNull()) {
            baseWeight = parseExactInt(obj.get("baseWeight"), "Attempt '" + id + "' baseWeight");
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
                if (!expObj.has("species")) {
                    throw new IllegalArgumentException("ExpectedLeadMember in '" + id + "' missing required 'species'");
                }
                String species = parseNonBlankString(expObj.get("species"), "ExpectedLeadMember species in '" + id + "'").toLowerCase(Locale.ROOT);
                String form = null;
                if (expObj.has("form") && !expObj.get("form").isJsonNull()) {
                    form = parseNonBlankString(expObj.get("form"), "ExpectedLeadMember form in '" + id + "'").toLowerCase(Locale.ROOT);
                }

                List<String> aspects = new ArrayList<>();
                if (expObj.has("requiredAspects") && !expObj.get("requiredAspects").isJsonNull()) {
                    if (!expObj.get("requiredAspects").isJsonArray()) {
                        throw new IllegalArgumentException("ExpectedLeadMember in '" + id + "' requiredAspects must be an array");
                    }
                    for (JsonElement aspElem : expObj.getAsJsonArray("requiredAspects")) {
                        String aspect = parseNonBlankString(aspElem, "ExpectedLeadMember aspect in '" + id + "'").toLowerCase(Locale.ROOT);
                        aspects.add(aspect);
                    }
                }
                expectedMembers.add(new ExpectedLeadMember(species, form, aspects));
            }
        }

        String description = "";
        if (obj.has("description") && !obj.get("description").isJsonNull()) {
            if (obj.get("description").isJsonPrimitive() && obj.get("description").getAsJsonPrimitive().isString()) {
                description = obj.get("description").getAsString().trim();
            }
        }
        return new LeadAttempt(id, new int[]{slot0, slot1}, baseWeight, expectedMembers, description);
    }
}
