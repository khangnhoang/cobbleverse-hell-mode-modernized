package com.cobbleverse.legendaryrule.strategy.decorator;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.ai.BattleAI;
import com.cobblemon.mod.common.battles.ActiveBattlePokemon;
import com.cobblemon.mod.common.battles.BattleSide;
import com.cobblemon.mod.common.battles.MoveActionResponse;
import com.cobblemon.mod.common.battles.ShowdownActionResponse;
import com.cobblemon.mod.common.battles.ShowdownMoveset;
import com.cobblemon.mod.common.net.messages.client.battle.BattleHealthChangePacket;
import com.cobbleverse.legendaryrule.strategy.domain.BattleStrategyPolicy;
import com.cobbleverse.legendaryrule.strategy.domain.BattleTurnContext;
import com.cobbleverse.legendaryrule.strategy.domain.StrategicDecision;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;

/**
 * Decorator wrapping a native BattleAI (e.g. Run & Bun AI) to selectively rewrite
 * decisions according to a BattleStrategyPolicy.
 * Enforces Delegate-First, Conservative Rewrite, Tera Gimmick Preservation, and
 * Fail-Safe Native-AI Fallback.
 */
public class StrategicBattleAIDecorator implements BattleAI {
    private static final Logger LOGGER = LoggerFactory.getLogger("StrategicBattleAI");

    private final BattleAI delegate;
    private final BattleStrategyPolicy policy;
    private final TurnContextExtractor contextExtractor;

    public StrategicBattleAIDecorator(
        @NotNull BattleAI delegate,
        @NotNull BattleStrategyPolicy policy,
        @NotNull TurnContextExtractor contextExtractor
    ) {
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
        this.policy = Objects.requireNonNull(policy, "policy must not be null");
        this.contextExtractor = Objects.requireNonNull(contextExtractor, "contextExtractor must not be null");
    }

    @Override
    public void onHealthChange(@NotNull BattleHealthChangePacket packet) {
        delegate.onHealthChange(packet);
    }

    @NotNull
    @Override
    public ShowdownActionResponse choose(
        @NotNull ActiveBattlePokemon activeBattlePokemon,
        @NotNull PokemonBattle battle,
        @NotNull BattleSide aiSide,
        @NotNull ShowdownMoveset moveset,
        boolean forceSwitch
    ) {
        // Contract 1: Delegate-First
        ShowdownActionResponse baseResponse = delegate.choose(activeBattlePokemon, battle, aiSide, moveset, forceSwitch);

        // Contract 2: Conservative Rewrite
        if (forceSwitch) {
            return baseResponse;
        }
        if (!(baseResponse instanceof MoveActionResponse baseMove)) {
            return baseResponse;
        }

        // Fail-Safe Native-AI Fallback
        try {
            Optional<BattleTurnContext> optContext = contextExtractor.extract(activeBattlePokemon, battle, aiSide, moveset);
            if (optContext.isEmpty()) {
                return baseResponse;
            }

            Optional<StrategicDecision> optDecision = policy.evaluate(optContext.get());
            if (optDecision.isEmpty()) {
                return baseResponse;
            }

            StrategicDecision decision = optDecision.get();

            // Contract 3 & 4: Preserve gimmick, spread targetPnx = null
            return new MoveActionResponse(
                decision.moveName(),
                decision.targetPnx(),
                baseMove.getGimmickID()
            );
        } catch (Throwable t) {
            LOGGER.warn("[StrategicBattleAI] Exception during strategy evaluation; failing safe to native AI response.", t);
            return baseResponse;
        }
    }

    @NotNull
    public BattleAI getDelegate() {
        return delegate;
    }

    @NotNull
    public BattleStrategyPolicy getPolicy() {
        return policy;
    }
}
