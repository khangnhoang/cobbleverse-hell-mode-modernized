package com.cobbleverse.legendaryrule;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class CompanionConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("rct_legendary_rule");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final String CONFIG_FILENAME = "cobbleverse-hell-mode-companion.json";
    public static final int DEFAULT_LIMIT = 1;
    public static final int MIN_LIMIT = 0;
    public static final int MAX_LIMIT = 6;

    private static volatile int maxLegendaryMythical = DEFAULT_LIMIT;
    private static Path configPath = null;

    public static int getMaxLegendaryMythical() {
        return maxLegendaryMythical;
    }

    public static void setMaxLegendaryMythical(int limit) {
        if (limit < MIN_LIMIT || limit > MAX_LIMIT) {
            throw new IllegalArgumentException("Legendary/Mythical party limit must be between 0 and 6 inclusive, got: " + limit);
        }
        maxLegendaryMythical = limit;
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
            maxLegendaryMythical = DEFAULT_LIMIT;
            return;
        }

        if (!Files.exists(path)) {
            maxLegendaryMythical = DEFAULT_LIMIT;
            save(path);
            return;
        }

        try (Reader reader = Files.newBufferedReader(path)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            if (json.has("maxLegendaryMythical")) {
                int val = json.get("maxLegendaryMythical").getAsInt();
                if (val >= MIN_LIMIT && val <= MAX_LIMIT) {
                    maxLegendaryMythical = val;
                    LOGGER.info("Loaded companion config from {}: maxLegendaryMythical = {}", path, maxLegendaryMythical);
                    return;
                } else {
                    LOGGER.warn("Config file {} had out-of-range value {}. Resetting to default {}.", path, val, DEFAULT_LIMIT);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to read companion config from {}, resetting to default {}: {}", path, DEFAULT_LIMIT, e.getMessage());
        }

        maxLegendaryMythical = DEFAULT_LIMIT;
        save(path);
    }

    public static synchronized void save() {
        save(getConfigPath());
    }

    public static synchronized void save(Path path) {
        if (path == null) return;
        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            JsonObject json = new JsonObject();
            json.addProperty("maxLegendaryMythical", maxLegendaryMythical);
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(json, writer);
            }
            LOGGER.info("Saved companion config to {}: maxLegendaryMythical = {}", path, maxLegendaryMythical);
        } catch (Exception e) {
            LOGGER.error("Failed to write companion config to {}: {}", path, e.getMessage());
        }
    }
}
