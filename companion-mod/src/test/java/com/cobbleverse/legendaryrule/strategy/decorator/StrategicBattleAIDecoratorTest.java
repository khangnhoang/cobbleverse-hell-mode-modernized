package com.cobbleverse.legendaryrule.strategy.decorator;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.ai.BattleAI;
import com.cobblemon.mod.common.battles.ActiveBattlePokemon;
import com.cobblemon.mod.common.battles.BattleSide;
import com.cobblemon.mod.common.battles.MoveActionResponse;
import com.cobblemon.mod.common.battles.PassActionResponse;
import com.cobblemon.mod.common.battles.ShowdownActionResponse;
import com.cobblemon.mod.common.battles.ShowdownMoveset;
import com.cobblemon.mod.common.battles.SwitchActionResponse;
import com.cobblemon.mod.common.net.messages.client.battle.BattleHealthChangePacket;
import com.cobbleverse.legendaryrule.strategy.domain.BattleStrategyPolicy;
import com.cobbleverse.legendaryrule.strategy.domain.BattleTurnContext;
import com.cobbleverse.legendaryrule.strategy.domain.StrategicDecision;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class StrategicBattleAIDecoratorTest {

    static class StubBattleAI implements BattleAI {
        ShowdownActionResponse responseToReturn;
        boolean chooseCalled = false;
        boolean forceSwitchPassed = false;
        boolean healthChangeCalled = false;

        @Override
        public void onHealthChange(@NotNull BattleHealthChangePacket packet) {
            healthChangeCalled = true;
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
            chooseCalled = true;
            forceSwitchPassed = forceSwitch;
            return responseToReturn;
        }
    }

    private BattleTurnContext createDummyContext() {
        return new BattleTurnContext(
            "toxtricitylowkey",
            "throatspray",
            List.of(),
            List.of(),
            true
        );
    }

    @Test
    @DisplayName("T08: forceSwitch == true -> returns exact delegate response without rewriting")
    void testForceSwitchPassthrough() {
        StubBattleAI stubAI = new StubBattleAI();
        SwitchActionResponse forcedSwitch = new SwitchActionResponse(UUID.randomUUID());
        stubAI.responseToReturn = forcedSwitch;

        BattleStrategyPolicy policy = ctx -> Optional.of(StrategicDecision.rewriteSpread("overdrive"));
        TurnContextExtractor extractor = (abp, b, s, m) -> Optional.of(createDummyContext());

        StrategicBattleAIDecorator decorator = new StrategicBattleAIDecorator(stubAI, policy, extractor);

        ShowdownActionResponse result = decorator.choose(null, null, null, null, true);

        assertTrue(stubAI.chooseCalled);
        assertTrue(stubAI.forceSwitchPassed);
        assertSame(forcedSwitch, result, "Must return exact delegate response on forceSwitch");
    }

    @Test
    @DisplayName("T09: Delegate returns SwitchActionResponse -> returns exact delegate response")
    void testTacticalSwitchPassthrough() {
        StubBattleAI stubAI = new StubBattleAI();
        SwitchActionResponse tacticalSwitch = new SwitchActionResponse(UUID.randomUUID());
        stubAI.responseToReturn = tacticalSwitch;

        BattleStrategyPolicy policy = ctx -> Optional.of(StrategicDecision.rewriteSpread("overdrive"));
        TurnContextExtractor extractor = (abp, b, s, m) -> Optional.of(createDummyContext());

        StrategicBattleAIDecorator decorator = new StrategicBattleAIDecorator(stubAI, policy, extractor);

        ShowdownActionResponse result = decorator.choose(null, null, null, null, false);

        assertTrue(stubAI.chooseCalled);
        assertSame(tacticalSwitch, result, "Must preserve tactical SwitchActionResponse without rewriting");
    }

    @Test
    @DisplayName("T10: Delegate returns PassActionResponse -> returns exact delegate response")
    void testPassActionPassthrough() {
        StubBattleAI stubAI = new StubBattleAI();
        PassActionResponse passAction = PassActionResponse.INSTANCE;
        stubAI.responseToReturn = passAction;

        BattleStrategyPolicy policy = ctx -> Optional.of(StrategicDecision.rewriteSpread("overdrive"));
        TurnContextExtractor extractor = (abp, b, s, m) -> Optional.of(createDummyContext());

        StrategicBattleAIDecorator decorator = new StrategicBattleAIDecorator(stubAI, policy, extractor);

        ShowdownActionResponse result = decorator.choose(null, null, null, null, false);

        assertTrue(stubAI.chooseCalled);
        assertSame(passAction, result, "Must preserve PassActionResponse without rewriting");
    }

    @Test
    @DisplayName("T11: Delegate returns normal MoveActionResponse, policy returns empty -> returns exact base response")
    void testPolicyMismatchPreservesBaseMove() {
        StubBattleAI stubAI = new StubBattleAI();
        MoveActionResponse baseMove = new MoveActionResponse("sludgebomb", "p2a", "terastallization");
        stubAI.responseToReturn = baseMove;

        BattleStrategyPolicy policy = ctx -> Optional.empty(); // policy does not match
        TurnContextExtractor extractor = (abp, b, s, m) -> Optional.of(createDummyContext());

        StrategicBattleAIDecorator decorator = new StrategicBattleAIDecorator(stubAI, policy, extractor);

        ShowdownActionResponse result = decorator.choose(null, null, null, null, false);

        assertTrue(stubAI.chooseCalled);
        assertSame(baseMove, result, "Must preserve base MoveActionResponse when policy does not match");
    }

    @Test
    @DisplayName("T12 and T13: Policy matches -> returns rewritten MoveActionResponse with overdrive, target null, gimmick preserved")
    void testPolicyMatchRewritesMoveAndPreservesGimmick() {
        StubBattleAI stubAI = new StubBattleAI();
        MoveActionResponse baseMove = new MoveActionResponse("sludgebomb", "p2a", "terastallization");
        stubAI.responseToReturn = baseMove;

        BattleStrategyPolicy policy = ctx -> Optional.of(StrategicDecision.rewriteSpread("overdrive"));
        TurnContextExtractor extractor = (abp, b, s, m) -> Optional.of(createDummyContext());

        StrategicBattleAIDecorator decorator = new StrategicBattleAIDecorator(stubAI, policy, extractor);

        ShowdownActionResponse result = decorator.choose(null, null, null, null, false);

        assertTrue(stubAI.chooseCalled);
        assertInstanceOf(MoveActionResponse.class, result);
        MoveActionResponse rewritten = (MoveActionResponse) result;

        assertEquals("overdrive", rewritten.getMoveName());
        assertNull(rewritten.getTargetPnx(), "Spread move Overdrive must have null targetPnx");
        assertEquals("terastallization", rewritten.getGimmickID(), "Must preserve base gimmick from delegate");
    }

    @Test
    @DisplayName("T13b: Fail-Safe Native-AI Fallback on context extraction or policy exception")
    void testFailSafeFallbackOnException() {
        StubBattleAI stubAI = new StubBattleAI();
        MoveActionResponse baseMove = new MoveActionResponse("sludgebomb", "p2a", null);
        stubAI.responseToReturn = baseMove;

        // Context extractor throws unexpected exception
        TurnContextExtractor failingExtractor = (abp, b, s, m) -> {
            throw new RuntimeException("Simulated runtime error during entity inspection");
        };
        BattleStrategyPolicy policy = ctx -> Optional.of(StrategicDecision.rewriteSpread("overdrive"));

        StrategicBattleAIDecorator decorator = new StrategicBattleAIDecorator(stubAI, policy, failingExtractor);

        ShowdownActionResponse result = decorator.choose(null, null, null, null, false);

        assertTrue(stubAI.chooseCalled);
        assertSame(baseMove, result, "Must fail-safe fallback to base response when exception occurs");
    }

    @Test
    @DisplayName("Delegate access and onHealthChange delegation")
    void testDelegateAccessAndHealthChange() {
        StubBattleAI stubAI = new StubBattleAI();

        StrategicBattleAIDecorator decorator = new StrategicBattleAIDecorator(
            stubAI,
            ctx -> Optional.empty(),
            (abp, b, s, m) -> Optional.empty()
        );

        assertSame(stubAI, decorator.getDelegate());
        decorator.onHealthChange(null);
        assertTrue(stubAI.healthChangeCalled);
    }
}
