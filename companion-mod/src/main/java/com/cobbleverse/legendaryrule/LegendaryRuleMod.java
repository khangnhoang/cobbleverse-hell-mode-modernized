package com.cobbleverse.legendaryrule;

import com.cobbleverse.legendaryrule.command.HellModeCommand;
import com.cobbleverse.legendaryrule.lead.LeadSelectionConfig;
import com.cobbleverse.legendaryrule.lead.LeadSelectionService;
import com.cobbleverse.legendaryrule.lead.TypeChartData;
import com.cobbleverse.legendaryrule.lead.TypeChartResourceLoader;
import com.cobbleverse.legendaryrule.lead.TypeMatchupScorer;
import com.cobbleverse.legendaryrule.mega.OutsideMegaBattleNormalizer;
import com.cobbleverse.legendaryrule.notify.command.NotifyWatchlistCommand;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class LegendaryRuleMod implements ModInitializer, DedicatedServerModInitializer {
    public static final String MOD_ID = "rct_legendary_rule";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final Identifier ALERT_SOUND_ID = Identifier.of(MOD_ID, "notify_alert");
    public static final SoundEvent ALERT_SOUND_EVENT = SoundEvent.of(ALERT_SOUND_ID);

    private static boolean initialized = false;

    @Override
    public void onInitialize() {
        init();
    }

    @Override
    public void onInitializeServer() {
        init();
    }

    private static synchronized void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        Registry.register(Registries.SOUND_EVENT, ALERT_SOUND_ID, ALERT_SOUND_EVENT);

        CompanionConfig.init();
        CommandRegistrationCallback.EVENT.register(HellModeCommand::register);
        CommandRegistrationCallback.EVENT.register(NotifyWatchlistCommand::register);
        OutsideMegaBattleNormalizer.register();
        LOGGER.info("RCT Legendary Rule Companion initialized (active limit: {}).", CompanionConfig.getMaxLegendaryMythical());

        // Register Dynamic Trainer Lead Presets Datapack Reload Listener
        net.fabricmc.fabric.api.resource.ResourceManagerHelper.get(net.minecraft.resource.ResourceType.SERVER_DATA)
                .registerReloadListener(new com.cobbleverse.legendaryrule.lead.DynamicLeadResourceListener());

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
