package com.cobbleverse.legendaryrule.strategy.adapter;

import com.cobblemon.mod.common.api.abilities.Ability;
import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.pokemon.helditem.HeldItemManager;
import com.cobblemon.mod.common.api.types.ElementalType;
import com.cobblemon.mod.common.battles.ActiveBattlePokemon;
import com.cobblemon.mod.common.battles.BattleSide;
import com.cobblemon.mod.common.battles.InBattleMove;
import com.cobblemon.mod.common.battles.ShowdownMoveset;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobbleverse.legendaryrule.strategy.decorator.TurnContextExtractor;
import com.cobbleverse.legendaryrule.strategy.domain.BattleTurnContext;
import com.cobbleverse.legendaryrule.strategy.domain.StrategicMoveContext;
import com.cobbleverse.legendaryrule.strategy.domain.StrategicOpponentContext;
import com.cobbleverse.legendaryrule.strategy.tracker.BattleItemStateTracker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class CobblemonTurnContextAdapter implements TurnContextExtractor {
    public static final CobblemonTurnContextAdapter INSTANCE = new CobblemonTurnContextAdapter();

    private static final Set<String> IMMUNITY_ABILITIES = Set.of(
        "soundproof",
        "voltabsorb",
        "motordrive",
        "lightningrod"
    );

    @NotNull
    @Override
    public Optional<BattleTurnContext> extract(
        @Nullable ActiveBattlePokemon activeBattlePokemon,
        @Nullable PokemonBattle battle,
        @Nullable BattleSide aiSide,
        @Nullable ShowdownMoveset moveset
    ) {
        if (activeBattlePokemon == null || moveset == null) {
            return Optional.empty();
        }

        BattlePokemon battlePokemon = activeBattlePokemon.getBattlePokemon();
        if (battlePokemon == null) {
            return Optional.empty();
        }

        Pokemon effectedPokemon = battlePokemon.getEffectedPokemon();
        if (effectedPokemon == null) {
            return Optional.empty();
        }

        // 1. Active Pokemon Showdown ID
        String activePokemonShowdownId = effectedPokemon.showdownId();

        // 2. Held Item Showdown ID
        String heldItemShowdownId = resolveEffectiveHeldItemId(battlePokemon);

        // 3. Moves
        List<StrategicMoveContext> moves = new ArrayList<>();
        if (moveset.getMoves() != null) {
            for (InBattleMove m : moveset.getMoves()) {
                if (m != null) {
                    String moveId = m.getId() != null ? m.getId().trim().toLowerCase() : "";
                    moves.add(new StrategicMoveContext(moveId, m.canBeUsed()));
                }
            }
        }

        // 4. Opponents
        List<StrategicOpponentContext> activeOpponents = new ArrayList<>();
        BattleSide oppSide = (aiSide != null) ? aiSide.getOppositeSide() :
                             (activeBattlePokemon.getSide() != null ? activeBattlePokemon.getSide().getOppositeSide() : null);

        if (oppSide != null && oppSide.getActivePokemon() != null) {
            for (ActiveBattlePokemon oppAbp : oppSide.getActivePokemon()) {
                if (oppAbp != null && oppAbp.hasPokemon() && oppAbp.isAlive() && !oppAbp.isGone()) {
                    BattlePokemon oppBp = oppAbp.getBattlePokemon();
                    if (oppBp != null && oppBp.getEffectedPokemon() != null) {
                        Pokemon oppMon = oppBp.getEffectedPokemon();
                        String oppShowdownId = oppMon.showdownId();
                        boolean confirmedImmune = isImmuneToPreferredMove(oppMon);
                        activeOpponents.add(new StrategicOpponentContext(oppShowdownId, true, confirmedImmune));
                    }
                }
            }
        }

        // 5. Doubles format check
        boolean isDoubles = false;
        if (battle != null && battle.getFormat() != null && battle.getFormat().getBattleType() != null) {
            isDoubles = battle.getFormat().getBattleType().getPokemonPerSide() >= 2;
        } else if (activeBattlePokemon.getFormat() != null && activeBattlePokemon.getFormat().getBattleType() != null) {
            isDoubles = activeBattlePokemon.getFormat().getBattleType().getPokemonPerSide() >= 2;
        }

        return Optional.of(new BattleTurnContext(
            activePokemonShowdownId,
            heldItemShowdownId,
            moves,
            activeOpponents,
            isDoubles
        ));
    }

    public boolean isImmuneToPreferredMove(@Nullable Pokemon oppPokemon) {
        if (oppPokemon == null) {
            return false;
        }

        List<String> typeNames = new ArrayList<>();
        Iterable<ElementalType> types = oppPokemon.getTypes();
        if (types != null) {
            for (ElementalType type : types) {
                if (type != null) {
                    if (type.getName() != null) {
                        typeNames.add(type.getName());
                    }
                    if (type.showdownId() != null) {
                        typeNames.add(type.showdownId());
                    }
                }
            }
        }

        String abilityName = null;
        Ability ability = oppPokemon.getAbility();
        if (ability != null && ability.getName() != null) {
            abilityName = ability.getName();
        }

        return isImmune(typeNames, abilityName);
    }

    public boolean isImmune(@Nullable Iterable<String> typeNames, @Nullable String abilityName) {
        if (typeNames != null) {
            for (String t : typeNames) {
                if (t != null && "ground".equalsIgnoreCase(t.trim())) {
                    return true;
                }
            }
        }

        if (abilityName != null) {
            String sanitized = abilityName.trim().toLowerCase().replace("-", "").replace("_", "").replace(" ", "");
            if (IMMUNITY_ABILITIES.contains(sanitized)) {
                return true;
            }
        }

        return false;
    }

    public String resolveEffectiveHeldItemId(@Nullable BattlePokemon battlePokemon) {
        if (battlePokemon == null) {
            return null;
        }
        HeldItemManager itemManager = battlePokemon.getHeldItemManager();
        if (itemManager == null) {
            return null;
        }
        String rawId = itemManager.showdownId(battlePokemon);
        if ("throatspray".equalsIgnoreCase(rawId)) {
            if (battlePokemon instanceof BattleItemStateTracker tracker && !tracker.cobbleverse$isThroatSprayEnded()) {
                return rawId;
            }
            // Ended OR tracker missing: fail-safe to native AI
            return null;
        }
        return rawId;
    }
}
