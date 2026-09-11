package com.cobbleverse.legendaryrule.lead.simulation.fixtures;

import com.cobbleverse.legendaryrule.lead.simulation.calculator.Turn1StatCalculator;
import com.cobbleverse.legendaryrule.lead.simulation.model.ActualStats;
import com.cobbleverse.legendaryrule.lead.simulation.model.CompetitivePokemonProfile;
import com.cobbleverse.legendaryrule.lead.simulation.model.CompetitivePokemonProfile.Stat;
import com.cobbleverse.legendaryrule.lead.simulation.model.MoveProfile;

import java.util.List;
import java.util.Map;

/**
 * Authoritative Level-60 competitive profiles for Sabrina's 6 team members matching kanto_sabrina.json.
 */
public final class SabrinaCompetitiveProfiles {

    private SabrinaCompetitiveProfiles() {}

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

    // Slot 0: Indeedee-F
    public static CompetitivePokemonProfile indeedee() {
        return createProfile(
                "indeedee",
                List.of("female"),
                List.of("psychic", "normal"),
                60,
                Map.of(Stat.HP, 70, Stat.ATK, 55, Stat.DEF, 65, Stat.SPA, 95, Stat.SPD, 105, Stat.SPE, 85),
                "relaxed",
                Map.of(Stat.HP, 31, Stat.ATK, 0, Stat.DEF, 31, Stat.SPA, 31, Stat.SPD, 31, Stat.SPE, 0),
                Map.of(Stat.HP, 252, Stat.ATK, 0, Stat.DEF, 252, Stat.SPA, 0, Stat.SPD, 4, Stat.SPE, 0),
                "psychicsurge",
                "psychic_seed",
                List.of(
                        new MoveProfile("followme", "normal", MoveProfile.Category.STATUS, 0, 2, MoveProfile.Target.SELF, false, false),
                        new MoveProfile("helpinghand", "normal", MoveProfile.Category.STATUS, 0, 5, MoveProfile.Target.ALLY, false, false),
                        new MoveProfile("psychic", "psychic", MoveProfile.Category.SPECIAL, 90, 0, MoveProfile.Target.SINGLE_OPPONENT, false, false, 100),
                        new MoveProfile("protect", "normal", MoveProfile.Category.STATUS, 0, 4, MoveProfile.Target.SELF, false, false)
                )
        );
    }

    // Slot 1: Gallade
    public static CompetitivePokemonProfile gallade() {
        return createProfile(
                "gallade",
                List.of(),
                List.of("psychic", "fighting"),
                60,
                Map.of(Stat.HP, 68, Stat.ATK, 125, Stat.DEF, 65, Stat.SPA, 65, Stat.SPD, 115, Stat.SPE, 80),
                "jolly",
                Map.of(Stat.HP, 31, Stat.ATK, 31, Stat.DEF, 31, Stat.SPA, 31, Stat.SPD, 31, Stat.SPE, 31),
                Map.of(Stat.HP, 4, Stat.ATK, 252, Stat.DEF, 0, Stat.SPA, 0, Stat.SPD, 0, Stat.SPE, 252),
                "sharpness",
                "clear_amulet",
                List.of(
                        new MoveProfile("sacredsword", "fighting", MoveProfile.Category.PHYSICAL, 90, 0, MoveProfile.Target.SINGLE_OPPONENT, true, false, 100),
                        new MoveProfile("psychocut", "psychic", MoveProfile.Category.PHYSICAL, 70, 0, MoveProfile.Target.SINGLE_OPPONENT, false, false, 100),
                        new MoveProfile("nightslash", "dark", MoveProfile.Category.PHYSICAL, 70, 0, MoveProfile.Target.SINGLE_OPPONENT, true, false, 100),
                        new MoveProfile("protect", "normal", MoveProfile.Category.STATUS, 0, 4, MoveProfile.Target.SELF, false, false)
                )
        );
    }

    // Slot 2: Garchomp
    public static CompetitivePokemonProfile garchomp() {
        return createProfile(
                "garchomp",
                List.of(),
                List.of("dragon", "ground"),
                60,
                Map.of(Stat.HP, 108, Stat.ATK, 130, Stat.DEF, 95, Stat.SPA, 80, Stat.SPD, 85, Stat.SPE, 102),
                "jolly",
                Map.of(Stat.HP, 31, Stat.ATK, 31, Stat.DEF, 31, Stat.SPA, 31, Stat.SPD, 31, Stat.SPE, 31),
                Map.of(Stat.HP, 4, Stat.ATK, 252, Stat.DEF, 0, Stat.SPA, 0, Stat.SPD, 0, Stat.SPE, 252),
                "roughskin",
                "soft_sand",
                List.of(
                        new MoveProfile("earthquake", "ground", MoveProfile.Category.PHYSICAL, 100, 0, MoveProfile.Target.ALL_ADJACENT, false, true, 100),
                        new MoveProfile("rockslide", "rock", MoveProfile.Category.PHYSICAL, 75, 0, MoveProfile.Target.ALL_ADJACENT_OPPONENTS, false, true, 90),
                        new MoveProfile("dragonclaw", "dragon", MoveProfile.Category.PHYSICAL, 80, 0, MoveProfile.Target.SINGLE_OPPONENT, true, false, 100),
                        new MoveProfile("protect", "normal", MoveProfile.Category.STATUS, 0, 4, MoveProfile.Target.SELF, false, false)
                )
        );
    }

    // Slot 3: Mega Metagross (backline)
    public static CompetitivePokemonProfile metagross() {
        return createProfile(
                "metagross",
                List.of("mega"),
                List.of("steel", "psychic"),
                60,
                Map.of(Stat.HP, 80, Stat.ATK, 145, Stat.DEF, 150, Stat.SPA, 105, Stat.SPD, 110, Stat.SPE, 110),
                "adamant",
                Map.of(Stat.HP, 31, Stat.ATK, 31, Stat.DEF, 31, Stat.SPA, 31, Stat.SPD, 31, Stat.SPE, 31),
                Map.of(Stat.HP, 4, Stat.ATK, 252, Stat.DEF, 0, Stat.SPA, 0, Stat.SPD, 0, Stat.SPE, 252),
                "toughclaws",
                "weakness_policy",
                List.of(
                        new MoveProfile("meteormash", "steel", MoveProfile.Category.PHYSICAL, 90, 0, MoveProfile.Target.SINGLE_OPPONENT, true, false, 90),
                        new MoveProfile("psychicfangs", "psychic", MoveProfile.Category.PHYSICAL, 85, 0, MoveProfile.Target.SINGLE_OPPONENT, true, false, 100),
                        new MoveProfile("stompingtantrum", "ground", MoveProfile.Category.PHYSICAL, 75, 0, MoveProfile.Target.SINGLE_OPPONENT, true, false, 100),
                        new MoveProfile("protect", "normal", MoveProfile.Category.STATUS, 0, 4, MoveProfile.Target.SELF, false, false)
                )
        );
    }

    // Slot 4: Rotom-Wash
    public static CompetitivePokemonProfile rotomWash() {
        return createProfile(
                "rotom",
                List.of("wash-appliance"),
                List.of("electric", "water"),
                60,
                Map.of(Stat.HP, 50, Stat.ATK, 65, Stat.DEF, 107, Stat.SPA, 105, Stat.SPD, 107, Stat.SPE, 86),
                "timid",
                Map.of(Stat.HP, 31, Stat.ATK, 0, Stat.DEF, 31, Stat.SPA, 31, Stat.SPD, 31, Stat.SPE, 31),
                Map.of(Stat.HP, 4, Stat.ATK, 0, Stat.DEF, 0, Stat.SPA, 252, Stat.SPD, 0, Stat.SPE, 252),
                "levitate",
                "sitrus_berry",
                List.of(
                        new MoveProfile("voltswitch", "electric", MoveProfile.Category.SPECIAL, 70, 0, MoveProfile.Target.SINGLE_OPPONENT, false, false, 100),
                        new MoveProfile("thunderbolt", "electric", MoveProfile.Category.SPECIAL, 90, 0, MoveProfile.Target.SINGLE_OPPONENT, false, false, 100),
                        new MoveProfile("hydropump", "water", MoveProfile.Category.SPECIAL, 110, 0, MoveProfile.Target.SINGLE_OPPONENT, false, false, 80),
                        new MoveProfile("protect", "normal", MoveProfile.Category.STATUS, 0, 4, MoveProfile.Target.SELF, false, false)
                )
        );
    }

    // Slot 5: Mega Alakazam
    public static CompetitivePokemonProfile alakazam() {
        return createProfile(
                "alakazam",
                List.of("mega"),
                List.of("psychic"),
                60,
                Map.of(Stat.HP, 55, Stat.ATK, 50, Stat.DEF, 65, Stat.SPA, 175, Stat.SPD, 105, Stat.SPE, 150),
                "timid",
                Map.of(Stat.HP, 31, Stat.ATK, 0, Stat.DEF, 31, Stat.SPA, 31, Stat.SPD, 31, Stat.SPE, 31),
                Map.of(Stat.HP, 4, Stat.ATK, 0, Stat.DEF, 0, Stat.SPA, 252, Stat.SPD, 0, Stat.SPE, 252),
                "disguise",
                "life_orb",
                List.of(
                        new MoveProfile("expandingforce", "psychic", MoveProfile.Category.SPECIAL, 80, 0, MoveProfile.Target.SINGLE_OPPONENT, false, false, 100),
                        new MoveProfile("focusblast", "fighting", MoveProfile.Category.SPECIAL, 120, 0, MoveProfile.Target.SINGLE_OPPONENT, false, false, 70),
                        new MoveProfile("shadowball", "ghost", MoveProfile.Category.SPECIAL, 80, 0, MoveProfile.Target.SINGLE_OPPONENT, false, false, 100),
                        new MoveProfile("protect", "normal", MoveProfile.Category.STATUS, 0, 4, MoveProfile.Target.SELF, false, false)
                )
        );
    }

    public static List<CompetitivePokemonProfile> getPreset(String presetId) {
        return switch (presetId) {
            case "default_psychic" -> List.of(indeedee(), alakazam());
            case "anti_dark_steel" -> List.of(indeedee(), gallade());
            case "field_independent" -> List.of(garchomp(), rotomWash());
            default -> throw new IllegalArgumentException("Unknown Sabrina preset ID: " + presetId);
        };
    }
}
