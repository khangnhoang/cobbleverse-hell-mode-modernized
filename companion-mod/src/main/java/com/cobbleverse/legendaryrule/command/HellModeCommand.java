package com.cobbleverse.legendaryrule.command;

import com.cobbleverse.legendaryrule.CompanionConfig;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class HellModeCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher,
                               CommandRegistryAccess registryAccess,
                               CommandManager.RegistrationEnvironment environment) {

        dispatcher.register(CommandManager.literal("hellmode")
            .requires(source -> source.hasPermissionLevel(2))
            .then(CommandManager.literal("legendary-limit")
                .executes(context -> {
                    int current = CompanionConfig.getMaxLegendaryMythical();
                    context.getSource().sendFeedback(
                        () -> Text.literal("§6[HellMode]§r Current Legendary/Mythical party limit: §e" + current),
                        false
                    );
                    return current;
                })
                .then(CommandManager.argument("limit", IntegerArgumentType.integer(0, 6))
                    .executes(context -> {
                        int oldVal = CompanionConfig.getMaxLegendaryMythical();
                        int newVal = IntegerArgumentType.getInteger(context, "limit");
                        CompanionConfig.setMaxLegendaryMythical(newVal);
                        CompanionConfig.save();
                        context.getSource().sendFeedback(
                            () -> Text.literal("§6[HellMode]§r Legendary/Mythical party limit changed: §e" + oldVal + " §7-> §a" + newVal),
                            true
                        );
                        return newVal;
                    })
                )
            )
        );
    }
}
