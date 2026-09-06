package com.cobbleverse.legendaryrule.strategy.adapter;

import com.cobblemon.mod.common.api.battles.interpreter.BattleMessage;
import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.pokemon.helditem.HeldItemManager;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobbleverse.legendaryrule.strategy.tracker.BattleItemStateTracker;
import kotlin.Lazy;
import kotlin.LazyKt;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class CobblemonTurnContextAdapterTest {

    private final CobblemonTurnContextAdapter adapter = CobblemonTurnContextAdapter.INSTANCE;

    private static Unsafe getUnsafe() throws Exception {
        Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        return (Unsafe) f.get(null);
    }

    static class StubHeldItemManager implements HeldItemManager {
        private final String id;

        StubHeldItemManager(String id) {
            this.id = id;
        }

        @Override
        public String showdownId(@NotNull BattlePokemon pokemon) {
            return id;
        }

        @NotNull
        @Override
        public Text nameOf(@NotNull String id) {
            return null;
        }

        @Override
        public void handleStartInstruction(@NotNull BattlePokemon pokemon, @NotNull PokemonBattle battle, @NotNull BattleMessage battleMessage) {}

        @Override
        public void handleEndInstruction(@NotNull BattlePokemon pokemon, @NotNull PokemonBattle battle, @NotNull BattleMessage battleMessage) {}

        @Override
        public void give(@NotNull BattlePokemon pokemon, @NotNull String id) {}

        @Override
        public void take(@NotNull BattlePokemon pokemon, @NotNull String id) {}

        @Override
        public boolean shouldConsumeItem(@NotNull BattlePokemon pokemon, @NotNull PokemonBattle battle, @NotNull String showdownId) {
            return false;
        }
    }

    static class TrackedBattlePokemon extends BattlePokemon implements BattleItemStateTracker {
        private boolean ended = false;

        public TrackedBattlePokemon() {
            super(null, null, (kotlin.jvm.functions.Function1) null);
        }

        @Override
        public boolean cobbleverse$isThroatSprayEnded() {
            return ended;
        }

        @Override
        public void cobbleverse$markThroatSprayEnded() {
            ended = true;
        }
    }

    static class UntrackedBattlePokemon extends BattlePokemon {
        public UntrackedBattlePokemon() {
            super(null, null, (kotlin.jvm.functions.Function1) null);
        }
    }

    private BattlePokemon createPokemonWithItem(Class<? extends BattlePokemon> clazz, String itemId) throws Exception {
        BattlePokemon p = (BattlePokemon) getUnsafe().allocateInstance(clazz);
        Field field = BattlePokemon.class.getDeclaredField("heldItemManager$delegate");
        field.setAccessible(true);
        Lazy<HeldItemManager> lazy = LazyKt.lazyOf(new StubHeldItemManager(itemId));
        field.set(p, lazy);
        return p;
    }

    @Test
    @DisplayName("T17a: Ground typing sets confirmedImmune = true")
    void testGroundTypingIsImmune() {
        assertTrue(adapter.isImmune(List.of("ground"), null));
        assertTrue(adapter.isImmune(List.of("water", "ground"), null));
        assertTrue(adapter.isImmune(List.of("Ground"), "keeneye"));
        assertTrue(adapter.isImmune(List.of("GROUND"), null));
    }

    @Test
    @DisplayName("T17b: Non-immune target sets confirmedImmune = false")
    void testNonImmuneTargetNotImmune() {
        assertFalse(adapter.isImmune(List.of("water", "flying"), null));
        assertFalse(adapter.isImmune(List.of("fire"), "blaze"));
        assertFalse(adapter.isImmune(List.of("grass", "poison"), "overgrow"));
        assertFalse(adapter.isImmune(List.of(), null));
    }

    @Test
    @DisplayName("T17c: Effective runtime Soundproof / Volt Absorb / Motor Drive / Lightning Rod sets confirmedImmune = true")
    void testAbilitiesAreImmune() {
        assertTrue(adapter.isImmune(List.of("normal"), "soundproof"));
        assertTrue(adapter.isImmune(List.of("electric"), "voltabsorb"));
        assertTrue(adapter.isImmune(List.of("electric"), "volt_absorb"));
        assertTrue(adapter.isImmune(List.of("electric"), "motordrive"));
        assertTrue(adapter.isImmune(List.of("electric"), "motor-drive"));
        assertTrue(adapter.isImmune(List.of("electric"), "lightningrod"));
        assertTrue(adapter.isImmune(List.of("electric"), "lightning_rod"));
    }

    @Test
    @DisplayName("T17d: Null safety on isImmune and isImmuneToPreferredMove")
    void testNullSafety() {
        assertFalse(adapter.isImmune(null, null));
        assertFalse(adapter.isImmuneToPreferredMove(null));
        assertEquals(Optional.empty(), adapter.extract(null, null, null, null));
        assertNull(adapter.resolveEffectiveHeldItemId(null));
    }

    @Test
    @DisplayName("T20a: Throat Spray unended exposes 'throatspray'")
    void testThroatSprayUnendedExposesItem() throws Exception {
        BattlePokemon p = createPokemonWithItem(TrackedBattlePokemon.class, "throatspray");
        assertEquals("throatspray", adapter.resolveEffectiveHeldItemId(p));
    }

    @Test
    @DisplayName("T20b: Throat Spray ended masks to null (drop-out)")
    void testThroatSprayEndedMasksToNull() throws Exception {
        BattlePokemon p = createPokemonWithItem(TrackedBattlePokemon.class, "throatspray");
        ((BattleItemStateTracker) p).cobbleverse$markThroatSprayEnded();
        assertNull(adapter.resolveEffectiveHeldItemId(p));
    }

    @Test
    @DisplayName("T20c: Missing BattleItemStateTracker fails safe to null for Throat Spray")
    void testMissingTrackerFailsSafeToNull() throws Exception {
        BattlePokemon p = createPokemonWithItem(UntrackedBattlePokemon.class, "throatspray");
        assertNull(adapter.resolveEffectiveHeldItemId(p), "Missing tracker must fail-safe to native AI (null)");
    }

    @Test
    @DisplayName("T20d: Non-Throat-Spray item passes through unchanged")
    void testNonThroatSprayItemPassesThrough() throws Exception {
        BattlePokemon p = createPokemonWithItem(TrackedBattlePokemon.class, "leftovers");
        assertEquals("leftovers", adapter.resolveEffectiveHeldItemId(p));
    }
}
