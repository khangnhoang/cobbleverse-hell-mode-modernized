package com.cobbleverse.legendaryrule.lead.simulation.fixtures;

import com.cobbleverse.legendaryrule.lead.simulation.calculator.Turn1StatCalculator;
import com.cobbleverse.legendaryrule.lead.simulation.model.ActualStats;
import com.cobbleverse.legendaryrule.lead.simulation.model.CompetitivePokemonProfile;
import com.cobbleverse.legendaryrule.lead.simulation.model.CompetitivePokemonProfile.Stat;
import com.cobbleverse.legendaryrule.lead.simulation.model.MoveProfile;

import java.util.List;
import java.util.Map;

/**
 * Authoritative Level-55 competitive profiles for Koga's 6 team members matching kanto_koga.json.
 */
public final class KogaCompetitiveProfiles {

    private KogaCompetitiveProfiles() {}

    private static CompetitivePokemonProfile createProfile(
            String species,
            List<String> aspects,
            List<String> types,
            int level,
            Map<Stat, Integer> baseStats,
            String nature,
            Map<Stat, Integer> ivs,
            Map<Stat, Integer> evs,
            String ability,
            String heldItem,
            List<MoveProfile> moves
    ) {
        ActualStats stats = Turn1StatCalculator.calculateStats(baseStats, ivs, evs, nature, level);
        return new CompetitivePokemonProfile(
                species,
                aspects,
                types,
                level,
                baseStats,
                nature,
                ivs,
                evs,
                ability,
                heldItem,
                moves,
                stats
        );
    }

    // Slot 0: Mega Beedrill
    public static CompetitivePokemonProfile beedrill() {
        return createProfile(
                "beedrill",
                List.of("mega"),
                List.of("bug", "poison"),
                55,
                Map.of(Stat.HP, 65, Stat.ATK, 150, Stat.DEF, 40, Stat.SPA, 15, Stat.SPD, 80, Stat.SPE, 145),
                "jolly",
                Map.of(Stat.HP, 31, Stat.ATK, 31, Stat.DEF, 31, Stat.SPA, 31, Stat.SPD, 31, Stat.SPE, 31),
                Map.of(Stat.HP, 4, Stat.ATK, 252, Stat.DEF, 0, Stat.SPA, 0, Stat.SPD, 0, Stat.SPE, 252),
                "adaptability",
                "mega_showdown:beedrillite",
                List.of(
                        new MoveProfile("poisonjab", "poison", MoveProfile.Category.PHYSICAL, 80, 0, MoveProfile.Target.SINGLE_OPPONENT, true, false),
                        new MoveProfile("uturn", "bug", MoveProfile.Category.PHYSICAL, 70, 0, MoveProfile.Target.SINGLE_OPPONENT, true, false),
                        new MoveProfile("knockoff", "dark", MoveProfile.Category.PHYSICAL, 65, 0, MoveProfile.Target.SINGLE_OPPONENT, true, false, 100),
                        new MoveProfile("drillrun", "ground", MoveProfile.Category.PHYSICAL, 80, 0, MoveProfile.Target.SINGLE_OPPONENT, true, false, 95)
                )
        );
    }

    // Slot 1: Sun Venusaur
    public static CompetitivePokemonProfile venusaur() {
        return createProfile(
                "venusaur",
                List.of(),
                List.of("grass", "poison"),
                55,
                Map.of(Stat.HP, 80, Stat.ATK, 82, Stat.DEF, 83, Stat.SPA, 100, Stat.SPD, 100, Stat.SPE, 80),
                "modest",
                Map.of(Stat.HP, 31, Stat.ATK, 31, Stat.DEF, 31, Stat.SPA, 31, Stat.SPD, 31, Stat.SPE, 31),
                Map.of(Stat.HP, 0, Stat.ATK, 0, Stat.DEF, 0, Stat.SPA, 252, Stat.SPD, 4, Stat.SPE, 252),
                "chlorophyll",
                "leftovers",
                List.of(
                        new MoveProfile("weatherball", "normal", MoveProfile.Category.SPECIAL, 50, 0, MoveProfile.Target.SINGLE_OPPONENT, false, false, 100),
                        new MoveProfile("gigadrain", "grass", MoveProfile.Category.SPECIAL, 75, 0, MoveProfile.Target.SINGLE_OPPONENT, false, false, 100),
                        new MoveProfile("growth", "normal", MoveProfile.Category.STATUS, 0, 0, MoveProfile.Target.SELF, false, false, 100),
                        new MoveProfile("sludgebomb", "poison", MoveProfile.Category.SPECIAL, 90, 0, MoveProfile.Target.SINGLE_OPPONENT, false, false, 100)
                )
        );
    }

    // Slot 2: Okidogi
    public static CompetitivePokemonProfile okidogi() {
        return createProfile(
                "okidogi",
                List.of(),
                List.of("poison", "fighting"),
                55,
                Map.of(Stat.HP, 88, Stat.ATK, 128, Stat.DEF, 115, Stat.SPA, 58, Stat.SPD, 86, Stat.SPE, 80),
                "adamant",
                Map.of(Stat.HP, 31, Stat.ATK, 31, Stat.DEF, 31, Stat.SPA, 31, Stat.SPD, 31, Stat.SPE, 31),
                Map.of(Stat.HP, 220, Stat.ATK, 108, Stat.DEF, 4, Stat.SPA, 0, Stat.SPD, 60, Stat.SPE, 116),
                "guarddog",
                "assault_vest",
                List.of(
                        new MoveProfile("knockoff", "dark", MoveProfile.Category.PHYSICAL, 65, 0, MoveProfile.Target.SINGLE_OPPONENT, true, false, 100),
                        new MoveProfile("drainpunch", "fighting", MoveProfile.Category.PHYSICAL, 75, 0, MoveProfile.Target.SINGLE_OPPONENT, true, false, 100),
                        new MoveProfile("poisonjab", "poison", MoveProfile.Category.PHYSICAL, 80, 0, MoveProfile.Target.SINGLE_OPPONENT, true, false, 100),
                        new MoveProfile("highhorsepower", "ground", MoveProfile.Category.PHYSICAL, 95, 0, MoveProfile.Target.SINGLE_OPPONENT, true, false, 95)
                )
        );
    }

    // Slot 3: Sneasler
    public static CompetitivePokemonProfile sneasler() {
        return createProfile(
                "sneasler",
                List.of(),
                List.of("fighting", "poison"),
                55,
                Map.of(Stat.HP, 80, Stat.ATK, 130, Stat.DEF, 60, Stat.SPA, 40, Stat.SPD, 80, Stat.SPE, 120),
                "adamant",
                Map.of(Stat.HP, 31, Stat.ATK, 31, Stat.DEF, 31, Stat.SPA, 31, Stat.SPD, 31, Stat.SPE, 31),
                Map.of(Stat.HP, 0, Stat.ATK, 252, Stat.DEF, 0, Stat.SPA, 0, Stat.SPD, 4, Stat.SPE, 252),
                "unburden",
                "white_herb",
                List.of(
                        new MoveProfile("closecombat", "fighting", MoveProfile.Category.PHYSICAL, 120, 0, MoveProfile.Target.SINGLE_OPPONENT, true, false),
                        new MoveProfile("direclaw", "poison", MoveProfile.Category.PHYSICAL, 80, 0, MoveProfile.Target.SINGLE_OPPONENT, true, false),
                        new MoveProfile("swordsdance", "normal", MoveProfile.Category.STATUS, 0, 0, MoveProfile.Target.SELF, false, false),
                        new MoveProfile("throatchop", "dark", MoveProfile.Category.PHYSICAL, 80, 0, MoveProfile.Target.SINGLE_OPPONENT, true, false)
                )
        );
    }

    // Slot 4: Amoonguss
    public static CompetitivePokemonProfile amoonguss() {
        return createProfile(
                "amoonguss",
                List.of(),
                List.of("grass", "poison"),
                55,
                Map.of(Stat.HP, 114, Stat.ATK, 85, Stat.DEF, 70, Stat.SPA, 85, Stat.SPD, 80, Stat.SPE, 30),
                "bold",
                Map.of(Stat.HP, 31, Stat.ATK, 31, Stat.DEF, 31, Stat.SPA, 31, Stat.SPD, 31, Stat.SPE, 31),
                Map.of(Stat.HP, 236, Stat.ATK, 0, Stat.DEF, 236, Stat.SPA, 0, Stat.SPD, 36, Stat.SPE, 0),
                "regenerator",
                "rocky_helmet",
                List.of(
                        new MoveProfile("spore", "grass", MoveProfile.Category.STATUS, 0, 0, MoveProfile.Target.SINGLE_OPPONENT, false, false),
                        new MoveProfile("ragepowder", "bug", MoveProfile.Category.STATUS, 0, 2, MoveProfile.Target.SELF, false, false),
                        new MoveProfile("pollenpuff", "bug", MoveProfile.Category.SPECIAL, 90, 0, MoveProfile.Target.SINGLE_OPPONENT, false, false),
                        new MoveProfile("protect", "normal", MoveProfile.Category.STATUS, 0, 4, MoveProfile.Target.SELF, false, false)
                )
        );
    }

    // Slot 5: Galarian Slowking
    public static CompetitivePokemonProfile slowking() {
        return createProfile(
                "slowking",
                List.of("galarian"),
                List.of("poison", "psychic"),
                55,
                Map.of(Stat.HP, 95, Stat.ATK, 65, Stat.DEF, 80, Stat.SPA, 110, Stat.SPD, 110, Stat.SPE, 30),
                "relaxed",
                Map.of(Stat.HP, 31, Stat.ATK, 31, Stat.DEF, 31, Stat.SPA, 31, Stat.SPD, 31, Stat.SPE, 0),
                Map.of(Stat.HP, 252, Stat.ATK, 0, Stat.DEF, 132, Stat.SPA, 0, Stat.SPD, 124, Stat.SPE, 0),
                "drought",
                "heat_rock",
                List.of(
                        new MoveProfile("sludgebomb", "poison", MoveProfile.Category.SPECIAL, 90, 0, MoveProfile.Target.SINGLE_OPPONENT, false, false),
                        new MoveProfile("flamethrower", "fire", MoveProfile.Category.SPECIAL, 90, 0, MoveProfile.Target.SINGLE_OPPONENT, false, false),
                        new MoveProfile("psychic", "psychic", MoveProfile.Category.SPECIAL, 90, 0, MoveProfile.Target.SINGLE_OPPONENT, false, false),
                        new MoveProfile("protect", "normal", MoveProfile.Category.STATUS, 0, 4, MoveProfile.Target.SELF, false, false)
                )
        );
    }

    public static List<CompetitivePokemonProfile> getPreset(String presetId) {
        return switch (presetId) {
            case "default_sun" -> List.of(slowking(), venusaur());
            case "anti_psychic" -> List.of(beedrill(), slowking());
            case "anti_dark_steel" -> List.of(okidogi(), sneasler());
            default -> throw new IllegalArgumentException("Unknown Koga preset ID: " + presetId);
        };
    }
}
