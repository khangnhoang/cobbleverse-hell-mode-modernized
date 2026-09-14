package com.cobbleverse.legendaryrule.mechanic;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.battles.BattleStartedEvent;
import com.cobblemon.mod.common.api.events.battles.instruction.MegaEvolutionEvent;
import com.cobblemon.mod.common.api.events.battles.instruction.TerastallizationEvent;
import com.cobblemon.mod.common.api.events.battles.instruction.ZMoveUsedEvent;
import com.cobblemon.mod.common.battles.BattleSide;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.github.yajatkaul.mega_showdown.api.event.DynamaxStartCallback;
import com.github.yajatkaul.mega_showdown.api.event.UltraBurstCallback;
import kotlin.Unit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BattleMechanicEventListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(BattleMechanicEventListener.class);

    public static void register() {
        // 1. Mega Evolution commit hook
        CobblemonEvents.MEGA_EVOLUTION.subscribe(Priority.NORMAL, BattleMechanicEventListener::onMegaEvolution);

        // 2. Terastallization commit hook
        CobblemonEvents.TERASTALLIZATION.subscribe(Priority.NORMAL, BattleMechanicEventListener::onTerastallization);

        // 3. Dynamax commit hook (void callback)
        DynamaxStartCallback.EVENT.register(BattleMechanicEventListener::onDynamaxStart);

        // 4. Z-Move commit hook
        CobblemonEvents.ZPOWER_USED.subscribe(Priority.NORMAL, BattleMechanicEventListener::onZMoveUsed);

        // 5. Ultra Burst commit hook (void callback)
        UltraBurstCallback.EVENT.register(BattleMechanicEventListener::onUltraBurst);

        // 6. Full lifecycle cleanup via BATTLE_STARTED_POST and battle.getOnEndHandlers()
        CobblemonEvents.BATTLE_STARTED_POST.subscribe(Priority.NORMAL, BattleMechanicEventListener::onBattleStartedPost);

        LOGGER.info("[BattleMechanicEventListener] Registered all 5 major mechanic commit hooks and lifecycle cleanup.");
    }

    private static void onMegaEvolution(MegaEvolutionEvent event) {
        if (event == null) return;
        commitFromBattlePokemon(event.getBattle(), event.getPokemon(), MajorBattleMechanic.MEGA);
    }

    private static void onTerastallization(TerastallizationEvent event) {
        if (event == null) return;
        commitFromBattlePokemon(event.getBattle(), event.getPokemon(), MajorBattleMechanic.TERA);
    }

    private static void onDynamaxStart(PokemonBattle battle, BattlePokemon battlePokemon, Boolean containsGmax) {
        commitFromBattlePokemon(battle, battlePokemon, MajorBattleMechanic.DYNAMAX);
    }

    private static void onZMoveUsed(ZMoveUsedEvent event) {
        if (event == null) return;
        commitFromBattlePokemon(event.getBattle(), event.getPokemon(), MajorBattleMechanic.Z_MOVE);
    }

    private static void onUltraBurst(PokemonBattle battle, BattlePokemon battlePokemon) {
        commitFromBattlePokemon(battle, battlePokemon, MajorBattleMechanic.ULTRA_BURST);
    }

    private static void commitFromBattlePokemon(PokemonBattle battle, BattlePokemon battlePokemon, MajorBattleMechanic mechanic) {
        if (battle == null || battlePokemon == null) return;
        BattleActor actor = battlePokemon.getActor();
        if (actor == null) return;
        BattleSide side = actor.getSide();
        if (side == null) return;
        int sideIndex = (side == battle.getSide1()) ? 0 : 1;
        BattleSideMechanicTracker.commitMechanic(battle.getBattleId(), sideIndex, mechanic);
    }

    private static void onBattleStartedPost(BattleStartedEvent.Post event) {
        if (event == null || event.getBattle() == null) return;
        PokemonBattle battle = event.getBattle();
        battle.getOnEndHandlers().add(b -> {
            if (b != null) {
                BattleSideMechanicTracker.clearBattle(b.getBattleId());
            }
            return Unit.INSTANCE;
        });
    }
}
