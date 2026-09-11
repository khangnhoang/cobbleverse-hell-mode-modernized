package com.cobbleverse.legendaryrule.lead.simulation.calculator;

import com.cobbleverse.legendaryrule.lead.simulation.model.ActualStats;
import com.cobbleverse.legendaryrule.lead.simulation.model.CompetitivePokemonProfile.Stat;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Pure arithmetic calculator implementing canonical Gen 9 stat formulas.
 */
public final class Turn1StatCalculator {

    private Turn1StatCalculator() {}

    /**
     * Calculates the canonical Gen 9 actual stats at the specified level.
     *
     * HP: floor(((2 * Base + IV + floor(EV / 4) + 100) * Level) / 100) + 10
     * Other: floor(floor(((2 * Base + IV + floor(EV / 4)) * Level) / 100 + 5) * NatureMod)
     */
    public static ActualStats calculateStats(
            Map<Stat, Integer> baseStats,
            Map<Stat, Integer> ivs,
            Map<Stat, Integer> evs,
            String nature,
            int level
    ) {
        Objects.requireNonNull(baseStats, "baseStats must not be null");
        Objects.requireNonNull(ivs, "ivs must not be null");
        Objects.requireNonNull(evs, "evs must not be null");
        Objects.requireNonNull(nature, "nature must not be null");

        int hp = calculateHp(
                baseStats.getOrDefault(Stat.HP, 1),
                ivs.getOrDefault(Stat.HP, 31),
                evs.getOrDefault(Stat.HP, 0),
                level
        );

        int atk = calculateStat(
                baseStats.getOrDefault(Stat.ATK, 1),
                ivs.getOrDefault(Stat.ATK, 31),
                evs.getOrDefault(Stat.ATK, 0),
                level,
                getNatureMultiplier(nature, Stat.ATK)
        );

        int def = calculateStat(
                baseStats.getOrDefault(Stat.DEF, 1),
                ivs.getOrDefault(Stat.DEF, 31),
                evs.getOrDefault(Stat.DEF, 0),
                level,
                getNatureMultiplier(nature, Stat.DEF)
        );

        int spa = calculateStat(
                baseStats.getOrDefault(Stat.SPA, 1),
                ivs.getOrDefault(Stat.SPA, 31),
                evs.getOrDefault(Stat.SPA, 0),
                level,
                getNatureMultiplier(nature, Stat.SPA)
        );

        int spd = calculateStat(
                baseStats.getOrDefault(Stat.SPD, 1),
                ivs.getOrDefault(Stat.SPD, 31),
                evs.getOrDefault(Stat.SPD, 0),
                level,
                getNatureMultiplier(nature, Stat.SPD)
        );

        int spe = calculateStat(
                baseStats.getOrDefault(Stat.SPE, 1),
                ivs.getOrDefault(Stat.SPE, 31),
                evs.getOrDefault(Stat.SPE, 0),
                level,
                getNatureMultiplier(nature, Stat.SPE)
        );

        return new ActualStats(hp, atk, def, spa, spd, spe);
    }

    public static int calculateHp(int base, int iv, int ev, int level) {
        return ((2 * base + iv + (ev / 4) + 100) * level) / 100 + 10;
    }

    public static int calculateStat(int base, int iv, int ev, int level, double natureMod) {
        int raw = ((2 * base + iv + (ev / 4)) * level) / 100 + 5;
        return (int) Math.floor(raw * natureMod);
    }

    public static double getNatureMultiplier(String nature, Stat stat) {
        if (nature == null || stat == Stat.HP) {
            return 1.0;
        }
        String nat = nature.toLowerCase(Locale.ROOT);

        Stat boosted = switch (nat) {
            case "lonely", "brave", "adamant", "naughty" -> Stat.ATK;
            case "bold", "relaxed", "impish", "lax" -> Stat.DEF;
            case "timid", "hasty", "jolly", "naive" -> Stat.SPE;
            case "modest", "mild", "quiet", "rash" -> Stat.SPA;
            case "calm", "gentle", "sassy", "careful" -> Stat.SPD;
            default -> null;
        };

        Stat hindered = switch (nat) {
            case "bold", "timid", "modest", "calm" -> Stat.ATK;
            case "lonely", "hasty", "mild", "gentle" -> Stat.DEF;
            case "brave", "relaxed", "quiet", "sassy" -> Stat.SPE;
            case "adamant", "impish", "jolly", "careful" -> Stat.SPA;
            case "naughty", "lax", "naive", "rash" -> Stat.SPD;
            default -> null;
        };

        if (boosted == stat) return 1.1;
        if (hindered == stat) return 0.9;
        return 1.0;
    }
}
