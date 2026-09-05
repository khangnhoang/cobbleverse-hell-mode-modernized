package com.cobbleverse.legendaryrule.strategy;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.ai.BattleAI;
import com.cobblemon.mod.common.battles.ActiveBattlePokemon;
import com.cobblemon.mod.common.battles.BattleSide;
import com.cobblemon.mod.common.battles.ShowdownActionResponse;
import com.cobblemon.mod.common.battles.ShowdownMoveset;
import com.cobblemon.mod.common.net.messages.client.battle.BattleHealthChangePacket;
import com.cobbleverse.legendaryrule.lead.DynamicLeadFallbackBoundary;
import com.cobbleverse.legendaryrule.lead.RosterOrderer;
import com.cobbleverse.legendaryrule.strategy.adapter.CobblemonTurnContextAdapter;
import com.cobbleverse.legendaryrule.strategy.decorator.StrategicBattleAIDecorator;
import com.cobbleverse.legendaryrule.strategy.domain.ThroatSpraySoundPolicy;
import com.gitlab.srcmc.rctapi.api.trainer.TrainerNPC;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class TrainerNPCMutationIsolationTest {

    static class StubBattleAI implements BattleAI {
        @NotNull
        @Override
        public ShowdownActionResponse choose(
            @NotNull ActiveBattlePokemon activeBattlePokemon,
            @NotNull PokemonBattle battle,
            @NotNull BattleSide aiSide,
            @NotNull ShowdownMoveset moveset,
            boolean forceSwitch
        ) {
            return null;
        }

        @Override
        public void onHealthChange(@NotNull BattleHealthChangePacket packet) {}
    }

    private Unsafe getUnsafe() throws Exception {
        Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        return (Unsafe) f.get(null);
    }

    @Test
    @DisplayName("T16a: Reference isolation and mutation invariance of GimmicksMap and BattleAI")
    void testTrainerNPCStateIsolation() throws Exception {
        Unsafe unsafe = getUnsafe();
        TrainerNPC original = (TrainerNPC) unsafe.allocateInstance(TrainerNPC.class);
        TrainerNPC perBattle = (TrainerNPC) unsafe.allocateInstance(TrainerNPC.class);

        Field gimmicksField = TrainerNPC.class.getDeclaredField("gimmicks");
        gimmicksField.setAccessible(true);
        Field aiField = TrainerNPC.class.getDeclaredField("battleAI");
        aiField.setAccessible(true);

        TrainerNPC.GimmicksMap origGimmicks = new TrainerNPC.GimmicksMap();
        TrainerNPC.GimmicksMap clonedGimmicks = new TrainerNPC.GimmicksMap(origGimmicks);

        StubBattleAI origAI = new StubBattleAI();
        StrategicBattleAIDecorator perBattleAI = new StrategicBattleAIDecorator(
            origAI,
            ThroatSpraySoundPolicy.surgeToxtricity(),
            CobblemonTurnContextAdapter.INSTANCE
        );

        gimmicksField.set(original, origGimmicks);
        gimmicksField.set(perBattle, clonedGimmicks);

        aiField.set(original, origAI);
        aiField.set(perBattle, perBattleAI);

        // 1. Reference isolation
        assertNotSame(original.getGimmicks(), perBattle.getGimmicks(), "GimmicksMap reference must be isolated");
        assertNotSame(original.getBattleAI(), perBattle.getBattleAI(), "BattleAI reference must be isolated");

        // 2. Original AI invariant
        assertSame(origAI, original.getBattleAI(), "Original singleton AI must remain undecorated");
        assertSame(perBattleAI, perBattle.getBattleAI(), "Per-battle instance must hold decorated AI");
        assertSame(origAI, ((StrategicBattleAIDecorator) perBattle.getBattleAI()).getDelegate(), "Decorator must delegate to original AI");
    }

    @Test
    @DisplayName("T16b: Team array isolation and reordering invariance")
    void testTeamArrayIsolation() {
        String[] originalTeam = new String[]{"raichu", "electrode", "magneton", "electabuzz", "jolteon", "toxtricitylowkey"};
        String[] originalCopy = originalTeam.clone();

        // Simulate per-battle reorder: slot [5, 0] lead
        String[] perBattleTeam = RosterOrderer.reorder(originalTeam, new int[]{5, 0}, String[]::new);

        assertNotSame(originalTeam, perBattleTeam, "Per-battle team array must be a new array reference");
        assertEquals("toxtricitylowkey", perBattleTeam[0], "Lead 1 must be Toxtricity");
        assertEquals("raichu", perBattleTeam[1], "Lead 2 must be Raichu");

        // Assert original singleton team is 100% unmutated
        assertArrayEquals(originalCopy, originalTeam, "Original singleton team array must remain completely unmutated");
    }

    @Test
    @DisplayName("T16c: DynamicLeadFallbackBoundary returns original singleton on customization exception")
    void testFallbackBoundaryPreservesOriginal() throws Exception {
        Unsafe unsafe = getUnsafe();
        TrainerNPC original = (TrainerNPC) unsafe.allocateInstance(TrainerNPC.class);

        TrainerNPC result = DynamicLeadFallbackBoundary.execute(original, () -> {
            throw new RuntimeException("Simulated unexpected error during strategy/lead customization");
        });

        assertSame(original, result, "Boundary must safely fall back to original singleton without modification");
    }
}
