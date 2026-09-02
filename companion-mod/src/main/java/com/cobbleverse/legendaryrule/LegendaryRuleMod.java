package com.cobbleverse.legendaryrule;

import com.cobbleverse.legendaryrule.command.HellModeCommand;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LegendaryRuleMod implements DedicatedServerModInitializer {
    public static final String MOD_ID = "rct_legendary_rule";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeServer() {
        CompanionConfig.init();
        CommandRegistrationCallback.EVENT.register(HellModeCommand::register);
        LOGGER.info("RCT Legendary Rule Companion initialized (active limit: {}).", CompanionConfig.getMaxLegendaryMythical());
    }
}
