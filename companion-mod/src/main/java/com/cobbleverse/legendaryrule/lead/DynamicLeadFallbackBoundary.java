package com.cobbleverse.legendaryrule.lead;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

/**
 * Feature fallback boundary for dynamic lead selection at the Mixin composition seam.
 * Wraps the full dynamic pipeline (service invocation, adaptation, engine selection,
 * clone, reorder, arraycopy).
 * Catches {@link Exception} and logs trainer ID before returning the original unmodified object.
 * Fatal {@link Error} and JVM linkage failures are intentionally not caught.
 */
public final class DynamicLeadFallbackBoundary {
    private static final Logger LOGGER = LoggerFactory.getLogger("rct_legendary_rule");

    private DynamicLeadFallbackBoundary() {}

    public static <T> T execute(T original, String trainerId, Supplier<T> dynamicAction) {
        try {
            T result = dynamicAction.get();
            return result != null ? result : original;
        } catch (Exception e) {
            LOGGER.error("[HellMode-Lead] Unexpected error during dynamic lead selection for trainer '{}'. Falling back to original TrainerNPC.",
                    trainerId != null ? trainerId : "unknown", e);
            return original;
        }
    }
}
