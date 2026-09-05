package com.cobbleverse.legendaryrule.lead;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class DynamicLeadFallbackBoundaryTest {

    @Test
    void testSuccessfulDynamicActionReturnsDynamicResult() {
        String original = "original_trainer_npc";
        String dynamicClone = "reordered_per_battle_clone";

        String result = DynamicLeadFallbackBoundary.execute(original, () -> dynamicClone);

        assertSame(dynamicClone, result, "Successful dynamic action must return the reordered clone");
    }

    @Test
    void testNullResultFallsBackToOriginal() {
        String original = "original_trainer_npc";

        String result = DynamicLeadFallbackBoundary.execute(original, () -> null);

        assertSame(original, result, "Null result must fall back to original");
    }

    @Test
    void testExceptionCaughtAndFallsBackToOriginal() {
        String original = "original_trainer_npc";
        AtomicBoolean handlerInvoked = new AtomicBoolean(false);

        // Any Exception during dynamic execution must fall back to original and invoke optional handler
        String result = DynamicLeadFallbackBoundary.execute(original, () -> {
            throw new RuntimeException("Unexpected error during selection/reordering");
        }, e -> handlerInvoked.set(true));

        assertSame(original, result, "Exception must be caught and return original TrainerNPC");
        assertTrue(handlerInvoked.get(), "Exception handler callback must be invoked");
    }

    @Test
    void testErrorPropagatesAndIsNotCaught() {
        String original = "original_trainer_npc";

        // JVM/Linkage/Assertion Errors must NOT be caught
        assertThrows(LinkageError.class, () -> {
            DynamicLeadFallbackBoundary.execute(original, () -> {
                throw new LinkageError("Fatal linkage failure");
            });
        }, "Fatal Error must propagate and never be swallowed by the fallback boundary");

        assertThrows(OutOfMemoryError.class, () -> {
            DynamicLeadFallbackBoundary.execute(original, () -> {
                throw new OutOfMemoryError("Simulated OOM");
            });
        }, "Fatal Error must propagate and never be swallowed by the fallback boundary");
    }
}
