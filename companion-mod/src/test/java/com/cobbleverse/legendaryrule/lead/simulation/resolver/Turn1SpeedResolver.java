package com.cobbleverse.legendaryrule.lead.simulation.resolver;

import com.cobbleverse.legendaryrule.lead.simulation.model.CompetitivePokemonProfile;
import com.cobbleverse.legendaryrule.lead.simulation.model.Turn1BattleState;

import java.util.Objects;

/**
 * Resolves effective speed and action ordering adhering to Gen 9 mechanics.
 */
public final class Turn1SpeedResolver {

    private Turn1SpeedResolver() {}

    /**
     * Calculates the effective in-battle Speed of a Pokémon in Turn 1.
     */
    public static int calculateEffectiveSpeed(
            CompetitivePokemonProfile mon,
            Turn1BattleState state,
            String slot
    ) {
        Objects.requireNonNull(mon, "mon must not be null");
        Objects.requireNonNull(state, "state must not be null");

        int baseSpeed = mon.actualStats().spe();

        // 1. Stat Stage
        int stage = slot != null ? state.getStatStage(slot, "spe") : 0;
        double stageMod = stage > 0 ? (2.0 + stage) / 2.0 : (stage < 0 ? 2.0 / (2.0 - stage) : 1.0);

        // 2. Ability
        double abilityMod = 1.0;
        if (mon.hasAbility("chlorophyll") && state.getWeather() == Turn1BattleState.Weather.SUN) {
            abilityMod = 2.0;
        } else if (mon.hasAbility("swiftswim") && state.getWeather() == Turn1BattleState.Weather.RAIN) {
            abilityMod = 2.0;
        } else if (mon.hasAbility("unburden") && slot != null && state.isItemConsumed(slot)) {
            abilityMod = 2.0;
        }

        // 3. Item
        double itemMod = 1.0;
        if (mon.hasItem("choice_scarf") && (slot == null || !state.isItemConsumed(slot))) {
            itemMod = 1.5;
        } else if (mon.hasItem("iron_ball")) {
            itemMod = 0.5;
        }

        // 4. Field (Tailwind)
        double fieldMod = 1.0;
        if (slot != null) {
            boolean isPlayer = slot.startsWith("player");
            if (isPlayer && state.isTailwindPlayer()) {
                fieldMod = 2.0;
            } else if (!isPlayer && state.isTailwindKoga()) {
                fieldMod = 2.0;
            }
        }

        double totalSpeed = baseSpeed * stageMod * abilityMod * itemMod * fieldMod;
        return Math.max(1, (int) Math.floor(totalSpeed));
    }

    /**
     * Compares two effective speeds for action order.
     * Returns negative if speed1 moves before speed2, positive if speed2 moves before speed1, 0 if tie.
     */
    public static int compareSpeed(int speed1, int speed2, boolean trickRoom) {
        if (trickRoom) {
            return Integer.compare(speed1, speed2); // lower speed moves first
        } else {
            return Integer.compare(speed2, speed1); // higher speed moves first
        }
    }
}
