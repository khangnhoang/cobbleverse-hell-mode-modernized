package com.cobbleverse.legendaryrule.lead.simulation.model;

/**
 * Immutable record representing the six canonical Gen 9 battle stats.
 */
public record ActualStats(
        int hp,
        int atk,
        int def,
        int spa,
        int spd,
        int spe
) {
    public ActualStats {
        if (hp < 1 || atk < 1 || def < 1 || spa < 1 || spd < 1 || spe < 1) {
            throw new IllegalArgumentException("Stats must be positive values: " +
                    "hp=" + hp + ", atk=" + atk + ", def=" + def +
                    ", spa=" + spa + ", spd=" + spd + ", spe=" + spe);
        }
    }
}
