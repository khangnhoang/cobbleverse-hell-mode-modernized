package com.cobbleverse.legendaryrule.notify.command;

import com.cobblemon.mod.common.command.argument.SpeciesArgumentType;
import com.cobblemon.mod.common.pokemon.Species;
import com.cobbleverse.legendaryrule.notify.NotifyWatchlistManager;
import com.cobbleverse.legendaryrule.notify.WatchMode;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Comparator;
import java.util.Map;

public class NotifyWatchlistCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher,
                                CommandRegistryAccess registryAccess,
                                CommandManager.RegistrationEnvironment environment) {

        dispatcher.register(CommandManager.literal("notify")
            .then(CommandManager.literal("add")
                .then(CommandManager.literal("one")
                    .then(CommandManager.argument("species", SpeciesArgumentType.Companion.species())
                        .executes(context -> executeAdd(context, WatchMode.ONE))
                    )
                )
                .then(CommandManager.literal("any")
                    .then(CommandManager.argument("species", SpeciesArgumentType.Companion.species())
                        .executes(context -> executeAdd(context, WatchMode.ANY))
                    )
                )
            )
            .then(CommandManager.literal("remove")
                .then(CommandManager.literal("all")
                    .executes(NotifyWatchlistCommand::executeRemoveAll)
                )
                .then(CommandManager.argument("species", SpeciesArgumentType.Companion.species())
                    .executes(NotifyWatchlistCommand::executeRemove)
                )
            )
            .then(CommandManager.literal("list")
                .executes(NotifyWatchlistCommand::executeList)
            )
        );
    }

    private static int executeAdd(CommandContext<ServerCommandSource> context, WatchMode mode) {
        Species species = SpeciesArgumentType.Companion.getPokemon(context, "species");
        Identifier speciesId = species.getResourceIdentifier();
        WatchMode oldMode = NotifyWatchlistManager.add(speciesId, mode);

        if (oldMode == null) {
            context.getSource().sendFeedback(
                () -> Text.literal("§6[Notify]§r Added §e" + species.getName() + "§r to watchlist (§b" + mode + "§r)."),
                false
            );
        } else if (oldMode == mode) {
            context.getSource().sendFeedback(
                () -> Text.literal("§6[Notify]§r §e" + species.getName() + "§r is already watched in mode §b" + mode + "§r."),
                false
            );
        } else {
            context.getSource().sendFeedback(
                () -> Text.literal("§6[Notify]§r Updated §e" + species.getName() + "§r mode: §7" + oldMode + "§r -> §b" + mode + "§r."),
                false
            );
        }
        return 1;
    }

    private static int executeRemove(CommandContext<ServerCommandSource> context) {
        Species species = SpeciesArgumentType.Companion.getPokemon(context, "species");
        Identifier speciesId = species.getResourceIdentifier();
        WatchMode removedMode = NotifyWatchlistManager.remove(speciesId);

        if (removedMode != null) {
            context.getSource().sendFeedback(
                () -> Text.literal("§6[Notify]§r Removed §e" + species.getName() + "§r from watchlist (was §b" + removedMode + "§r)."),
                false
            );
            return 1;
        } else {
            context.getSource().sendFeedback(
                () -> Text.literal("§c[Notify]§r §e" + species.getName() + "§r was not in the watchlist."),
                false
            );
            return 0;
        }
    }

    private static int executeRemoveAll(CommandContext<ServerCommandSource> context) {
        int count = NotifyWatchlistManager.clearAll();
        if (count > 0) {
            context.getSource().sendFeedback(
                () -> Text.literal("§6[Notify]§r Cleared all §e" + count + "§r Pokémon from watchlist."),
                false
            );
        } else {
            context.getSource().sendFeedback(
                () -> Text.literal("§6[Notify]§r Watchlist was already empty."),
                false
            );
        }
        return count;
    }

    private static int executeList(CommandContext<ServerCommandSource> context) {
        Map<Identifier, WatchMode> current = NotifyWatchlistManager.getWatchlist();
        if (current.isEmpty()) {
            context.getSource().sendFeedback(
                () -> Text.literal("§6[Notify]§r Watchlist is empty."),
                false
            );
            return 0;
        }

        context.getSource().sendFeedback(
            () -> Text.literal("§6[Notify]§r Watched Pokémon (" + current.size() + "):"),
            false
        );

        current.entrySet().stream()
            .sorted(Map.Entry.comparingByKey(Comparator.comparing(Identifier::toString)))
            .forEach(entry -> {
                Identifier id = entry.getKey();
                WatchMode mode = entry.getValue();
                context.getSource().sendFeedback(
                    () -> Text.literal(" - §e" + id + "§r [§b" + mode + "§r]"),
                    false
                );
            });

        return current.size();
    }
}
