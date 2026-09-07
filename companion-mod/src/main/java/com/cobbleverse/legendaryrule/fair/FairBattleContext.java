package com.cobbleverse.legendaryrule.fair;

import com.cobblemon.mod.common.battles.ActiveBattlePokemon;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Scoped context providing fair-information battle state during AI decision evaluation.
 *
 * <p>Associates real {@link ActiveBattlePokemon} instances with sanitized shadow
 * {@link BattlePokemon} instances strictly for the duration of a decision calculation
 * (e.g. {@code RunBunAI.choose()}).</p>
 *
 * <p>ThreadLocal isolation ensures parallel battle threads do not collide.
 * Scoped restoration ensures nested evaluations cleanly restore the outer context
 * upon scope exit without accidental global clearing.</p>
 */
public final class FairBattleContext {

    private static final ThreadLocal<Map<ActiveBattlePokemon, BattlePokemon>> CURRENT_MAP = new ThreadLocal<>();

    private FairBattleContext() {
    }

    /**
     * Checks if a fair evaluation scope is currently active on the calling thread.
     *
     * @return true if active, false otherwise
     */
    public static boolean isActive() {
        return CURRENT_MAP.get() != null;
    }

    /**
     * Opens a new fair evaluation scope with the supplied shadow mappings.
     *
     * <p>Mappings use identity comparison ({@link IdentityHashMap}) on the real
     * {@link ActiveBattlePokemon} key. Closing the returned {@link Scope} restores
     * whatever scope was active prior to this call, or clears the ThreadLocal
     * if this was the outermost scope.</p>
     *
     * @param shadowMap map from real ActiveBattlePokemon to sanitized shadow BattlePokemon
     * @return an AutoCloseable Scope to be used with try-with-resources
     */
    public static Scope open(Map<ActiveBattlePokemon, BattlePokemon> shadowMap) {
        Map<ActiveBattlePokemon, BattlePokemon> previous = CURRENT_MAP.get();
        if (shadowMap == null || shadowMap.isEmpty()) {
            CURRENT_MAP.set(Collections.emptyMap());
        } else {
            CURRENT_MAP.set(new IdentityHashMap<>(shadowMap));
        }
        return new Scope(previous);
    }

    /**
     * Resolves the {@link BattlePokemon} for an {@link ActiveBattlePokemon} given its
     * currently known original value.
     *
     * <p>If a fair scope is active and maps the given active battle Pokémon, the mapped
     * shadow {@link BattlePokemon} is returned. Otherwise, {@code original} is returned.</p>
     *
     * @param abp the active battle Pokémon
     * @param original the original BattlePokemon reference from the ABP
     * @return the shadow BattlePokemon if mapped under active scope, otherwise original
     */
    public static BattlePokemon resolve(ActiveBattlePokemon abp, BattlePokemon original) {
        if (abp == null) {
            return original;
        }
        Map<ActiveBattlePokemon, BattlePokemon> current = CURRENT_MAP.get();
        if (current != null) {
            BattlePokemon shadow = current.get(abp);
            if (shadow != null) {
                return shadow;
            }
        }
        return original;
    }

    /**
     * Resolves the {@link BattlePokemon} for an {@link ActiveBattlePokemon}.
     *
     * <p>Convenience overload delegating to {@link #resolve(ActiveBattlePokemon, BattlePokemon)}
     * using {@code abp.getBattlePokemon()} as the fallback original.</p>
     *
     * @param abp the active battle Pokémon
     * @return the shadow BattlePokemon if mapped under active scope, otherwise abp.getBattlePokemon()
     */
    public static BattlePokemon resolve(ActiveBattlePokemon abp) {
        if (abp == null) {
            return null;
        }
        return resolve(abp, abp.getBattlePokemon());
    }

    /**
     * AutoCloseable scope token for fair evaluation contexts.
     * Restores previous context or clears ThreadLocal on close.
     */
    public static final class Scope implements AutoCloseable {
        private final Map<ActiveBattlePokemon, BattlePokemon> previous;
        private boolean closed;

        private Scope(Map<ActiveBattlePokemon, BattlePokemon> previous) {
            this.previous = previous;
        }

        @Override
        public void close() {
            if (!closed) {
                closed = true;
                if (previous == null) {
                    CURRENT_MAP.remove();
                } else {
                    CURRENT_MAP.set(previous);
                }
            }
        }
    }
}
