package com.cobbleverse.legendaryrule.fair;

import com.cobblemon.mod.common.battles.ActiveBattlePokemon;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class FairBattleContextTest {

    private static Unsafe unsafe;
    private static Field abpBattlePokemonField;

    @BeforeAll
    static void setUpAll() throws Exception {
        Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        unsafe = (Unsafe) f.get(null);

        abpBattlePokemonField = ActiveBattlePokemon.class.getDeclaredField("battlePokemon");
        abpBattlePokemonField.setAccessible(true);
    }

    @AfterEach
    void tearDown() {
        // Ensure ThreadLocal is completely cleaned up after every test
        while (FairBattleContext.isActive()) {
            FairBattleContext.open(null).close();
        }
    }

    private ActiveBattlePokemon createMockAbp(BattlePokemon bp) throws Exception {
        ActiveBattlePokemon abp = (ActiveBattlePokemon) unsafe.allocateInstance(ActiveBattlePokemon.class);
        if (bp != null) {
            abpBattlePokemonField.set(abp, bp);
        }
        return abp;
    }

    private BattlePokemon createMockBp() throws Exception {
        return (BattlePokemon) unsafe.allocateInstance(BattlePokemon.class);
    }

    @Test
    @DisplayName("Outside active scope, resolve returns original BattlePokemon")
    void testNoActiveScopeReturnsOriginal() throws Exception {
        BattlePokemon realBp = createMockBp();
        ActiveBattlePokemon abp = createMockAbp(realBp);

        assertFalse(FairBattleContext.isActive());
        assertSame(realBp, FairBattleContext.resolve(abp, realBp));
        assertSame(realBp, FairBattleContext.resolve(abp));
    }

    @Test
    @DisplayName("Inside active scope, mapped ABP returns shadow BattlePokemon")
    void testMappedAbpReturnsShadow() throws Exception {
        BattlePokemon realBp = createMockBp();
        BattlePokemon shadowBp = createMockBp();
        ActiveBattlePokemon abp = createMockAbp(realBp);

        Map<ActiveBattlePokemon, BattlePokemon> map = new HashMap<>();
        map.put(abp, shadowBp);

        try (FairBattleContext.Scope ignored = FairBattleContext.open(map)) {
            assertTrue(FairBattleContext.isActive());
            assertSame(shadowBp, FairBattleContext.resolve(abp, realBp));
            assertSame(shadowBp, FairBattleContext.resolve(abp));
        }

        assertFalse(FairBattleContext.isActive());
        assertSame(realBp, FairBattleContext.resolve(abp, realBp));
        assertSame(realBp, FairBattleContext.resolve(abp));
    }

    @Test
    @DisplayName("Inside active scope, unmapped ABP returns original BattlePokemon")
    void testUnmappedAbpReturnsOriginal() throws Exception {
        BattlePokemon realBp1 = createMockBp();
        BattlePokemon shadowBp1 = createMockBp();
        ActiveBattlePokemon abp1 = createMockAbp(realBp1);

        BattlePokemon realBp2 = createMockBp();
        ActiveBattlePokemon abp2 = createMockAbp(realBp2);

        Map<ActiveBattlePokemon, BattlePokemon> map = new HashMap<>();
        map.put(abp1, shadowBp1);

        try (FairBattleContext.Scope ignored = FairBattleContext.open(map)) {
            assertTrue(FairBattleContext.isActive());
            assertSame(shadowBp1, FairBattleContext.resolve(abp1, realBp1));
            assertSame(realBp2, FairBattleContext.resolve(abp2, realBp2));
            assertSame(realBp2, FairBattleContext.resolve(abp2));
        }

        assertFalse(FairBattleContext.isActive());
        assertSame(realBp1, FairBattleContext.resolve(abp1, realBp1));
        assertSame(realBp2, FairBattleContext.resolve(abp2, realBp2));
    }

    @Test
    @DisplayName("Null ABP returns original BattlePokemon or null safely")
    void testNullAbpHandling() throws Exception {
        BattlePokemon fallback = createMockBp();
        assertSame(fallback, FairBattleContext.resolve(null, fallback));
        assertNull(FairBattleContext.resolve(null));

        try (FairBattleContext.Scope ignored = FairBattleContext.open(Collections.emptyMap())) {
            assertSame(fallback, FairBattleContext.resolve(null, fallback));
            assertNull(FairBattleContext.resolve(null));
        }
    }

    @Test
    @DisplayName("Nested scopes preserve and restore previous mapping on close")
    void testNestedScopeRestoration() throws Exception {
        BattlePokemon realBp1 = createMockBp();
        BattlePokemon shadowOuter1 = createMockBp();
        BattlePokemon shadowInner1 = createMockBp();
        ActiveBattlePokemon abp1 = createMockAbp(realBp1);

        BattlePokemon realBp2 = createMockBp();
        BattlePokemon shadowOuter2 = createMockBp();
        ActiveBattlePokemon abp2 = createMockAbp(realBp2);

        Map<ActiveBattlePokemon, BattlePokemon> outerMap = new HashMap<>();
        outerMap.put(abp1, shadowOuter1);
        outerMap.put(abp2, shadowOuter2);

        try (FairBattleContext.Scope outer = FairBattleContext.open(outerMap)) {
            assertSame(shadowOuter1, FairBattleContext.resolve(abp1, realBp1));
            assertSame(shadowOuter2, FairBattleContext.resolve(abp2, realBp2));

            Map<ActiveBattlePokemon, BattlePokemon> innerMap = new HashMap<>();
            innerMap.put(abp1, shadowInner1);

            try (FairBattleContext.Scope inner = FairBattleContext.open(innerMap)) {
                assertSame(shadowInner1, FairBattleContext.resolve(abp1, realBp1));
                // abp2 not mapped in inner scope -> returns realBp2
                assertSame(realBp2, FairBattleContext.resolve(abp2, realBp2));
            }

            // Inner closed: outer scope mappings restored
            assertTrue(FairBattleContext.isActive());
            assertSame(shadowOuter1, FairBattleContext.resolve(abp1, realBp1));
            assertSame(shadowOuter2, FairBattleContext.resolve(abp2, realBp2));
        }

        // Outer closed: context completely inactive
        assertFalse(FairBattleContext.isActive());
        assertSame(realBp1, FairBattleContext.resolve(abp1, realBp1));
        assertSame(realBp2, FairBattleContext.resolve(abp2, realBp2));
    }

    @Test
    @DisplayName("Exception thrown inside try-with-resources cleanly restores outer state")
    void testExceptionSafetyWithTryWithResources() throws Exception {
        BattlePokemon realBp = createMockBp();
        BattlePokemon shadowBp = createMockBp();
        ActiveBattlePokemon abp = createMockAbp(realBp);

        Map<ActiveBattlePokemon, BattlePokemon> map = new HashMap<>();
        map.put(abp, shadowBp);

        assertThrows(IllegalStateException.class, () -> {
            try (FairBattleContext.Scope ignored = FairBattleContext.open(map)) {
                assertTrue(FairBattleContext.isActive());
                assertSame(shadowBp, FairBattleContext.resolve(abp, realBp));
                throw new IllegalStateException("Simulated AI calculation failure");
            }
        });

        assertFalse(FairBattleContext.isActive());
        assertSame(realBp, FairBattleContext.resolve(abp, realBp));
    }

    @Test
    @DisplayName("ThreadLocal state is isolated across threads")
    void testThreadIsolation() throws Exception {
        BattlePokemon realBp = createMockBp();
        BattlePokemon shadowA = createMockBp();
        BattlePokemon shadowB = createMockBp();
        ActiveBattlePokemon abp = createMockAbp(realBp);

        Map<ActiveBattlePokemon, BattlePokemon> mapA = new HashMap<>();
        mapA.put(abp, shadowA);

        try (FairBattleContext.Scope ignoredA = FairBattleContext.open(mapA)) {
            assertSame(shadowA, FairBattleContext.resolve(abp, realBp));

            AtomicReference<BattlePokemon> resolvedOnThreadBBefore = new AtomicReference<>();
            AtomicReference<BattlePokemon> resolvedOnThreadBDuring = new AtomicReference<>();
            AtomicReference<BattlePokemon> resolvedOnThreadBAfter = new AtomicReference<>();
            AtomicBoolean threadBIsActiveBefore = new AtomicBoolean();
            AtomicBoolean threadBIsActiveAfter = new AtomicBoolean();

            Thread threadB = new Thread(() -> {
                threadBIsActiveBefore.set(FairBattleContext.isActive());
                resolvedOnThreadBBefore.set(FairBattleContext.resolve(abp, realBp));

                Map<ActiveBattlePokemon, BattlePokemon> mapB = new HashMap<>();
                mapB.put(abp, shadowB);

                try (FairBattleContext.Scope ignoredB = FairBattleContext.open(mapB)) {
                    resolvedOnThreadBDuring.set(FairBattleContext.resolve(abp, realBp));
                }

                threadBIsActiveAfter.set(FairBattleContext.isActive());
                resolvedOnThreadBAfter.set(FairBattleContext.resolve(abp, realBp));
            });

            threadB.start();
            threadB.join();

            // Thread B results
            assertFalse(threadBIsActiveBefore.get());
            assertSame(realBp, resolvedOnThreadBBefore.get());
            assertSame(shadowB, resolvedOnThreadBDuring.get());
            assertFalse(threadBIsActiveAfter.get());
            assertSame(realBp, resolvedOnThreadBAfter.get());

            // Main thread remained unaffected
            assertTrue(FairBattleContext.isActive());
            assertSame(shadowA, FairBattleContext.resolve(abp, realBp));
        }

        assertFalse(FairBattleContext.isActive());
    }

    @Test
    @DisplayName("Closing a scope multiple times is idempotent")
    void testDoubleCloseIdempotence() throws Exception {
        BattlePokemon realBp = createMockBp();
        BattlePokemon shadowBp = createMockBp();
        ActiveBattlePokemon abp = createMockAbp(realBp);

        Map<ActiveBattlePokemon, BattlePokemon> map = new HashMap<>();
        map.put(abp, shadowBp);

        FairBattleContext.Scope scope = FairBattleContext.open(map);
        assertTrue(FairBattleContext.isActive());
        assertSame(shadowBp, FairBattleContext.resolve(abp, realBp));

        scope.close();
        assertFalse(FairBattleContext.isActive());
        assertSame(realBp, FairBattleContext.resolve(abp, realBp));

        // Second close should be no-op
        assertDoesNotThrow(scope::close);
        assertFalse(FairBattleContext.isActive());
    }

    @Test
    @DisplayName("Opening scope with null or empty map activates scope but preserves originals")
    void testNullOrEmptyMap() throws Exception {
        BattlePokemon realBp = createMockBp();
        ActiveBattlePokemon abp = createMockAbp(realBp);

        try (FairBattleContext.Scope ignored = FairBattleContext.open(null)) {
            assertTrue(FairBattleContext.isActive());
            assertSame(realBp, FairBattleContext.resolve(abp, realBp));
        }
        assertFalse(FairBattleContext.isActive());

        try (FairBattleContext.Scope ignored = FairBattleContext.open(Collections.emptyMap())) {
            assertTrue(FairBattleContext.isActive());
            assertSame(realBp, FairBattleContext.resolve(abp, realBp));
        }
        assertFalse(FairBattleContext.isActive());
    }
}
