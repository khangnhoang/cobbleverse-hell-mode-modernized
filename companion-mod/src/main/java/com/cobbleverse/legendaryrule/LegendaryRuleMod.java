package com.cobbleverse.legendaryrule;

import com.cobbleverse.legendaryrule.command.HellModeCommand;
import com.cobbleverse.legendaryrule.lead.LeadSelectionConfig;
import com.cobbleverse.legendaryrule.lead.LeadSelectionService;
import com.cobbleverse.legendaryrule.lead.TypeChartData;
import com.cobbleverse.legendaryrule.lead.TypeChartResourceLoader;
import com.cobbleverse.legendaryrule.lead.TypeMatchupScorer;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class LegendaryRuleMod implements DedicatedServerModInitializer {
    public static final String MOD_ID = "rct_legendary_rule";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeServer() {
        CompanionConfig.init();
        CommandRegistrationCallback.EVENT.register(HellModeCommand::register);
        LOGGER.info("RCT Legendary Rule Companion initialized (active limit: {}).", CompanionConfig.getMaxLegendaryMythical());

        // Initialize Dynamic Trainer Lead Selection Presets
        try {
            java.nio.file.Path configPath = net.fabricmc.loader.api.FabricLoader.getInstance()
                    .getConfigDir().resolve(LeadSelectionConfig.CONFIG_FILENAME);
            LeadSelectionConfig.ConfigLoadResult configResult = LeadSelectionConfig.load(configPath);
            if (!configResult.success()) {
                LOGGER.warn("Failed to load dynamic lead presets from {}: {}. Dynamic lead presets inactive.", configPath, configResult.errorMessage());
            } else {
                LOGGER.info("Loaded dynamic lead presets for {} trainers from {}", configResult.loadedTrainersCount(), configPath);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to initialize lead selection configuration: {}", e.getMessage(), e);
        }

        Optional<TypeChartData> typeChartOpt = TypeChartResourceLoader.loadDefault();
        if (typeChartOpt.isPresent()) {
            TypeMatchupScorer scorer = new TypeMatchupScorer(typeChartOpt.get());
            LeadSelectionService.initialize(scorer);
            LOGGER.info("Dynamic Trainer Lead Selection Presets initialized successfully.");
        } else {
            LeadSelectionService.setUnavailable();
            LOGGER.error("[HellMode-Lead] ERROR: Failed to load type chart resource '{}'. Dynamic lead selection is DISABLED. Preserving native trainer ordering.",
                    TypeChartResourceLoader.DEFAULT_RESOURCE_PATH);
        }
    }
}
