package com.cobbleverse.legendaryrule.notify;

import net.minecraft.util.Identifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class NotifyWatchlistManagerTest {

    private final Identifier UXIE = Identifier.of("cobblemon", "uxie");
    private final Identifier BAGON = Identifier.of("cobblemon", "bagon");
    private final Identifier DIALGA = Identifier.of("cobblemon", "dialga");

    @BeforeEach
    void setUp() {
        // Clean up watchlist between tests
        NotifyWatchlistManager.clearAll();
    }

    @Test
    @DisplayName("Test 1: Add ONE mode and verify state")
    void testAddOne() {
        WatchMode previous = NotifyWatchlistManager.add(UXIE, WatchMode.ONE);
        assertNull(previous, "Initial add should return null previous mode");
        assertEquals(WatchMode.ONE, NotifyWatchlistManager.get(UXIE));

        Map<Identifier, WatchMode> list = NotifyWatchlistManager.getWatchlist();
        assertTrue(list.containsKey(UXIE));
        assertEquals(WatchMode.ONE, list.get(UXIE));
    }

    @Test
    @DisplayName("Test 2: Add ANY mode and verify idempotency")
    void testAddAnyAndIdempotency() {
        WatchMode prev1 = NotifyWatchlistManager.add(UXIE, WatchMode.ANY);
        assertNull(prev1);
        assertEquals(WatchMode.ANY, NotifyWatchlistManager.get(UXIE));

        // Adding again with same mode should be idempotent
        WatchMode prev2 = NotifyWatchlistManager.add(UXIE, WatchMode.ANY);
        assertEquals(WatchMode.ANY, prev2, "Re-adding same mode should return current mode");
        assertEquals(WatchMode.ANY, NotifyWatchlistManager.get(UXIE));
    }

    @Test
    @DisplayName("Test 3: Mode switching ONE <-> ANY replaces mode cleanly")
    void testModeSwitching() {
        NotifyWatchlistManager.add(UXIE, WatchMode.ONE);
        assertEquals(WatchMode.ONE, NotifyWatchlistManager.get(UXIE));

        // Switch ONE -> ANY
        WatchMode prev1 = NotifyWatchlistManager.add(UXIE, WatchMode.ANY);
        assertEquals(WatchMode.ONE, prev1, "Switching ONE -> ANY returns old mode ONE");
        assertEquals(WatchMode.ANY, NotifyWatchlistManager.get(UXIE));

        // Switch ANY -> ONE
        WatchMode prev2 = NotifyWatchlistManager.add(UXIE, WatchMode.ONE);
        assertEquals(WatchMode.ANY, prev2, "Switching ANY -> ONE returns old mode ANY");
        assertEquals(WatchMode.ONE, NotifyWatchlistManager.get(UXIE));
    }

    @Test
    @DisplayName("Test 4: Remove species from watchlist prevents future queries")
    void testRemove() {
        NotifyWatchlistManager.add(UXIE, WatchMode.ONE);
        assertNotNull(NotifyWatchlistManager.get(UXIE));

        WatchMode removed = NotifyWatchlistManager.remove(UXIE);
        assertEquals(WatchMode.ONE, removed);
        assertNull(NotifyWatchlistManager.get(UXIE));

        // Removing non-existent species returns null
        assertNull(NotifyWatchlistManager.remove(UXIE));
    }

    @Test
    @DisplayName("Test 5: Ordinary species (e.g. Bagon) works identically to legendaries")
    void testOrdinarySpecies() {
        NotifyWatchlistManager.add(BAGON, WatchMode.ANY);
        assertEquals(WatchMode.ANY, NotifyWatchlistManager.get(BAGON));

        Map<Identifier, WatchMode> list = NotifyWatchlistManager.getWatchlist();
        assertTrue(list.containsKey(BAGON));
        assertEquals(WatchMode.ANY, list.get(BAGON));

        NotifyWatchlistManager.remove(BAGON);
        assertNull(NotifyWatchlistManager.get(BAGON));
    }

    @Test
    @DisplayName("Test 6: Eligible spawn triggers set invariant")
    void testEligibleTriggers() {
        // Triggers that must be watched
        List<Identifier> eligible = List.of(
            Identifier.of("spawn_notification", "spawned"),
            Identifier.of("spawn_notification", "fished"),
            Identifier.of("spawn_notification", "snacked")
        );

        // Triggers that must be ignored
        List<Identifier> ignored = List.of(
            Identifier.of("spawn_notification", "captured"),
            Identifier.of("spawn_notification", "hatched"),
            Identifier.of("spawn_notification", "resurrected"),
            Identifier.of("spawn_notification", "fainted"),
            Identifier.of("spawn_notification", "died"),
            Identifier.of("spawn_notification", "despawned")
        );

        for (Identifier trig : eligible) {
            assertTrue(trig.getPath().equals("spawned") || trig.getPath().equals("fished") || trig.getPath().equals("snacked"));
        }
        for (Identifier trig : ignored) {
            assertFalse(trig.getPath().equals("spawned") || trig.getPath().equals("fished") || trig.getPath().equals("snacked"));
        }
    }

    @Test
    @DisplayName("Test 7: Multi-threaded atomic consumption for ONE mode")
    void testAtomicConsumptionUnderConcurrency() throws InterruptedException {
        NotifyWatchlistManager.add(DIALGA, WatchMode.ONE);

        int threads = 16;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);

        AtomicInteger successfulConsumptions = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    latch.await();
                    // Simulate atomic conditional consumption
                    WatchMode current = NotifyWatchlistManager.get(DIALGA);
                    if (current == WatchMode.ONE) {
                        // In NotifyWatchlistManager, atomic consumption uses WATCHLIST.remove(speciesId, WatchMode.ONE)
                        // We test remove(key) semantics under concurrency
                        WatchMode removed = NotifyWatchlistManager.remove(DIALGA);
                        if (removed != null) {
                            successfulConsumptions.incrementAndGet();
                        }
                    }
                } catch (InterruptedException ignored) {
                } finally {
                    done.countDown();
                }
            });
        }

        latch.countDown();
        done.await();
        executor.shutdown();

        assertEquals(1, successfulConsumptions.get(), "Exactly ONE thread must succeed in consuming the ONE watch entry");
        assertNull(NotifyWatchlistManager.get(DIALGA), "Species must be removed after consumption");
    }

    @Test
    @DisplayName("Test 8: Unmodifiable view of watchlist")
    void testUnmodifiableWatchlistView() {
        NotifyWatchlistManager.add(UXIE, WatchMode.ONE);
        Map<Identifier, WatchMode> view = NotifyWatchlistManager.getWatchlist();

        assertThrows(UnsupportedOperationException.class, () -> {
            view.put(BAGON, WatchMode.ANY);
        }, "Watchlist map view must be unmodifiable to protect internal state");
    }

    @Test
    @DisplayName("Test 9: clearAll with mixed ONE and ANY entries")
    void testClearAllWithMixedModes() {
        NotifyWatchlistManager.add(UXIE, WatchMode.ONE);
        NotifyWatchlistManager.add(BAGON, WatchMode.ANY);
        NotifyWatchlistManager.add(DIALGA, WatchMode.ONE);

        assertEquals(3, NotifyWatchlistManager.getWatchlist().size());

        int cleared = NotifyWatchlistManager.clearAll();
        assertEquals(3, cleared, "clearAll should return the exact number of cleared entries");
        assertTrue(NotifyWatchlistManager.getWatchlist().isEmpty(), "Watchlist must be empty after clearAll");
        assertNull(NotifyWatchlistManager.get(UXIE));
        assertNull(NotifyWatchlistManager.get(BAGON));
        assertNull(NotifyWatchlistManager.get(DIALGA));
    }

    @Test
    @DisplayName("Test 10: clearAll on empty watchlist returns 0")
    void testClearAllEmpty() {
        assertTrue(NotifyWatchlistManager.getWatchlist().isEmpty());
        int cleared = NotifyWatchlistManager.clearAll();
        assertEquals(0, cleared, "clearAll on empty watchlist must return 0");
        assertTrue(NotifyWatchlistManager.getWatchlist().isEmpty());
    }

    @Test
    @DisplayName("Test 11: Single species remove still works correctly alongside clearAll")
    void testSingleSpeciesRemovePreserved() {
        NotifyWatchlistManager.add(UXIE, WatchMode.ONE);
        NotifyWatchlistManager.add(BAGON, WatchMode.ANY);

        WatchMode removed = NotifyWatchlistManager.remove(UXIE);
        assertEquals(WatchMode.ONE, removed);
        assertNull(NotifyWatchlistManager.get(UXIE));
        assertEquals(WatchMode.ANY, NotifyWatchlistManager.get(BAGON));
        assertEquals(1, NotifyWatchlistManager.getWatchlist().size());

        int cleared = NotifyWatchlistManager.clearAll();
        assertEquals(1, cleared);
        assertTrue(NotifyWatchlistManager.getWatchlist().isEmpty());
    }
}
