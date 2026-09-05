package com.cobbleverse.legendaryrule.lead;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Pure generic exception fallback boundary for dynamic lead selection.
 * Wraps dynamic computation, catches {@link Exception}, returns the original value,
 * and passes any caught exception to an optional callback (e.g. for composition logging).
 * Fatal {@link Error} and JVM linkage failures are intentionally NOT caught.
 */
public final class DynamicLeadFallbackBoundary {

    private DynamicLeadFallbackBoundary() {}

    public static <T> T execute(T original, Supplier<T> dynamicAction) {
        return execute(original, dynamicAction, null);
    }

    public static <T> T execute(T original, Supplier<T> dynamicAction, Consumer<Exception> exceptionHandler) {
        try {
            T result = dynamicAction.get();
            return result != null ? result : original;
        } catch (Exception e) {
            if (exceptionHandler != null) {
                exceptionHandler.accept(e);
            }
            return original;
        }
    }
}
