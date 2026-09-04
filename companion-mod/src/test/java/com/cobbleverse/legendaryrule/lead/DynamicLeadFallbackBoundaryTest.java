package com.cobbleverse.legendaryrule.lead;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DynamicLeadFallbackBoundaryTest {

    @Test
    void testSuccessfulDynamicActionReturnsDynamicResult() {
        String original = "original_trainer_npc";
        String dynamicClone = "reordered_per_battle_clone";

        String result = DynamicLeadFallbackBoundary.execute(original, "kanto_sabrina", () -> dynamicClone);

        assertSame(dynamicClone, result, "Successful dynamic action must return the reordered clone");
    }

    @Test
    void testNullResultFallsBackToOriginal() {
        String original = "original_trainer_npc";

        String result = DynamicLeadFallbackBoundary.execute(original, "kanto_sabrina", () -> null);

        assertSame(original, result, "Null result must fall back to original");
    }

    @Test
    void testExceptionCaughtAndFallsBackToOriginal() {
        String original = "original_trainer_npc";

        // Any Exception (Checked or RuntimeException) during dynamic execution must fall back to original
        String result = DynamicLeadFallbackBoundary.execute(original, "kanto_sabrina", () -> {
            throw new RuntimeException("Unexpected error during selection/reordering");
        });

        assertSame(original, result, "Exception must be caught and return original TrainerNPC");
    }

    @Test
    void testErrorPropagatesAndIsNotCaught() {
        String original = "original_trainer_npc";

        // JVM/Linkage/Assertion Errors must NOT be caught
        assertThrows(LinkageError.class, () -> {
            DynamicLeadFallbackBoundary.execute(original, "kanto_sabrina", () -> {
                throw new LinkageError("Fatal linkage failure");
            });
        }, "Fatal Error must propagate and never be swallowed by the fallback boundary");

        assertThrows(OutOfMemoryError.class, () -> {
            DynamicLeadFallbackBoundary.execute(original, "kanto_sabrina", () -> {
                throw new OutOfMemoryError("Simulated OOM");
            });
        }, "Fatal Error must propagate and never be swallowed by the fallback boundary");
    }
}
