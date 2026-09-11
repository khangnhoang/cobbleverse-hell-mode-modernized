package com.cobbleverse.legendaryrule.lead.simulation.fixtures;

import com.cobbleverse.legendaryrule.lead.simulation.calculator.Turn1StatCalculator;
import com.cobbleverse.legendaryrule.lead.simulation.model.ActualStats;
import com.cobbleverse.legendaryrule.lead.simulation.model.CompetitivePokemonProfile;
import com.cobbleverse.legendaryrule.lead.simulation.model.CompetitivePokemonProfile.Stat;
import com.cobbleverse.legendaryrule.lead.simulation.model.MoveProfile;

import java.util.*;

/**
 * Standard VGC Gen 9 competitive profiles scaled to Level 55 for the 51 threat pool species.
 */
public final class ThreatPoolCompetitiveProfiles {

    private ThreatPoolCompetitiveProfiles() {}

    private static CompetitivePokemonProfile build(
            String species,
            List<String> types,
            String ability,
            String heldItem,
            String nature,
            Map<Stat, Integer> baseStats,
            Map<Stat, Integer> evs,
            Map<Stat, Integer> ivs,
            List<MoveProfile> moves
    ) {
        Map<Stat, Integer> cleanIvs = ivs == null ? Map.of(Stat.HP, 31, Stat.ATK, 31, Stat.DEF, 31, Stat.SPA, 31, Stat.SPD, 31, Stat.SPE, 31) : ivs;
        ActualStats stats = Turn1StatCalculator.calculateStats(baseStats, cleanIvs, evs, nature, 55);
        return new CompetitivePokemonProfile(
                species,
                List.of(),
                types,
                55,
                baseStats,
                nature,
                cleanIvs,
                evs,
                ability,
                heldItem,
                moves,
                stats
        );
    }

    private static final Map<String, java.util.function.Supplier<CompetitivePokemonProfile>> REGISTRY = new HashMap<>();

    static {
        // Canaries and Critical Threats
        REGISTRY.put("indeedee", ThreatPoolCompetitiveProfiles::indeedeeF);
        REGISTRY.put("indeedee-f", ThreatPoolCompetitiveProfiles::indeedeeF);
        REGISTRY.put("indeedee-m", ThreatPoolCompetitiveProfiles::indeedeeM);
        REGISTRY.put("kingambit", ThreatPoolCompetitiveProfiles::kingambit);
        REGISTRY.put("heatran", ThreatPoolCompetitiveProfiles::heatran);
        REGISTRY.put("cresselia", ThreatPoolCompetitiveProfiles::cresselia);
        REGISTRY.put("pelipper", ThreatPoolCompetitiveProfiles::pelipper);
        REGISTRY.put("archaludon", ThreatPoolCompetitiveProfiles::archaludon);
        REGISTRY.put("landorus", ThreatPoolCompetitiveProfiles::landorusT);
        REGISTRY.put("gholdengo", ThreatPoolCompetitiveProfiles::gholdengo);
        REGISTRY.put("talonflame", ThreatPoolCompetitiveProfiles::talonflame);
        REGISTRY.put("charizard", ThreatPoolCompetitiveProfiles::charizard);

        // Psyspam / Psychic
        REGISTRY.put("armarouge", ThreatPoolCompetitiveProfiles::armarouge);
        REGISTRY.put("hatterene", ThreatPoolCompetitiveProfiles::hatterene);
        REGISTRY.put("farigiraf", ThreatPoolCompetitiveProfiles::farigiraf);
        REGISTRY.put("ironcrown", ThreatPoolCompetitiveProfiles::ironcrown);
        REGISTRY.put("alakazam", ThreatPoolCompetitiveProfiles::alakazam);

        // Dark / Ghost
        REGISTRY.put("chiyu", ThreatPoolCompetitiveProfiles::chiyu);
        REGISTRY.put("fluttermane", ThreatPoolCompetitiveProfiles::fluttermane);
        REGISTRY.put("chienpao", ThreatPoolCompetitiveProfiles::chienpao);
        REGISTRY.put("dragonite", ThreatPoolCompetitiveProfiles::dragonite);
        REGISTRY.put("incineroar", ThreatPoolCompetitiveProfiles::incineroar);

        // Steel / Flying / Ground
        REGISTRY.put("tornadus", ThreatPoolCompetitiveProfiles::tornadus);
        REGISTRY.put("magnezone", ThreatPoolCompetitiveProfiles::magnezone);
        REGISTRY.put("corviknight", ThreatPoolCompetitiveProfiles::corviknight);
        REGISTRY.put("skarmory", ThreatPoolCompetitiveProfiles::skarmory);
        REGISTRY.put("garchomp", ThreatPoolCompetitiveProfiles::garchomp);
        REGISTRY.put("greattusk", ThreatPoolCompetitiveProfiles::greattusk);
        REGISTRY.put("gliscor", ThreatPoolCompetitiveProfiles::gliscor);
        REGISTRY.put("mamoswine", ThreatPoolCompetitiveProfiles::mamoswine);
        REGISTRY.put("tinglu", ThreatPoolCompetitiveProfiles::tinglu);
        REGISTRY.put("excadrill", ThreatPoolCompetitiveProfiles::excadrill);
        REGISTRY.put("tyranitar", ThreatPoolCompetitiveProfiles::tyranitar);
        REGISTRY.put("gastrodon", ThreatPoolCompetitiveProfiles::gastrodon);
        REGISTRY.put("swampert", ThreatPoolCompetitiveProfiles::swampert);
        REGISTRY.put("ursaluna", ThreatPoolCompetitiveProfiles::ursaluna);
        REGISTRY.put("ursuluna", ThreatPoolCompetitiveProfiles::ursaluna);
        REGISTRY.put("salamence", ThreatPoolCompetitiveProfiles::salamence);

        // Weather
        REGISTRY.put("politoed", ThreatPoolCompetitiveProfiles::politoed);
        REGISTRY.put("urshifu", ThreatPoolCompetitiveProfiles::urshifuRapidStrike);
        REGISTRY.put("hippowdon", ThreatPoolCompetitiveProfiles::hippowdon);
        REGISTRY.put("abomasnow", ThreatPoolCompetitiveProfiles::abomasnow);
        REGISTRY.put("ninetales", ThreatPoolCompetitiveProfiles::ninetalesAlola);
        REGISTRY.put("baxcalibur", ThreatPoolCompetitiveProfiles::baxcalibur);
        REGISTRY.put("torkoal", ThreatPoolCompetitiveProfiles::torkoal);
        REGISTRY.put("lilligant", ThreatPoolCompetitiveProfiles::lilligantHisui);

        // Neutral / Control
        REGISTRY.put("rillaboom", ThreatPoolCompetitiveProfiles::rillaboom);
        REGISTRY.put("lucario", ThreatPoolCompetitiveProfiles::lucario);
        REGISTRY.put("rotom", ThreatPoolCompetitiveProfiles::rotomWash);
        REGISTRY.put("metagross", ThreatPoolCompetitiveProfiles::metagross);
        REGISTRY.put("milotic", ThreatPoolCompetitiveProfiles::milotic);
        REGISTRY.put("amoonguss", ThreatPoolCompetitiveProfiles::amoonguss);
        REGISTRY.put("arcanine", ThreatPoolCompetitiveProfiles::arcanine);
        REGISTRY.put("raichu", ThreatPoolCompetitiveProfiles::raichu);
        REGISTRY.put("gyarados", ThreatPoolCompetitiveProfiles::gyarados);
        REGISTRY.put("clefable", ThreatPoolCompetitiveProfiles::clefable);
        REGISTRY.put("snorlax", ThreatPoolCompetitiveProfiles::snorlax);
        REGISTRY.put("gengar", ThreatPoolCompetitiveProfiles::gengar);
        REGISTRY.put("lapras", ThreatPoolCompetitiveProfiles::lapras);
        REGISTRY.put("machamp", ThreatPoolCompetitiveProfiles::machamp);
        REGISTRY.put("togekiss", ThreatPoolCompetitiveProfiles::togekiss);
        REGISTRY.put("dragapult", ThreatPoolCompetitiveProfiles::dragapult);
        REGISTRY.put("venusaur", ThreatPoolCompetitiveProfiles::venusaur);
    }

    public static CompetitivePokemonProfile getProfile(String species) {
        if (species == null) return null;
        String key = species.toLowerCase(Locale.ROOT).replace(" ", "").replace("-", "").replace("_", "");

        // Try direct lookup
        var supplier = REGISTRY.get(species.toLowerCase(Locale.ROOT));
        if (supplier != null) return supplier.get();

        // Try normalized key lookup
        for (Map.Entry<String, java.util.function.Supplier<CompetitivePokemonProfile>> e : REGISTRY.entrySet()) {
            if (e.getKey().replace("-", "").replace("_", "").equals(key)) {
                return e.getValue().get();
            }
        }

        // Generic fallback profile
        return build(species, List.of("normal"), "none", "", "hardy",
                Map.of(Stat.HP, 100, Stat.ATK, 100, Stat.DEF, 100, Stat.SPA, 100, Stat.SPD, 100, Stat.SPE, 100),
                Map.of(Stat.HP, 0, Stat.ATK, 0, Stat.DEF, 0, Stat.SPA, 0, Stat.SPD, 0, Stat.SPE, 0),
                null,
                List.of(new MoveProfile("tackle", "normal", MoveProfile.Category.PHYSICAL, 40, 0, MoveProfile.Target.SINGLE_OPPONENT, true, false))
        );
    }

    // 1. Indeedee-F (Psychic Surge, Follow Me + Helping Hand)
    public static CompetitivePokemonProfile indeedeeF() {
        return build("indeedee-f", List.of("psychic", "normal"), "psychicsurge", "psychic_seed", "bold",
                Map.of(Stat.HP, 70, Stat.ATK, 55, Stat.DEF, 65, Stat.SPA, 95, Stat.SPD, 105, Stat.SPE, 85),
                Map.of(Stat.HP, 252, Stat.ATK, 0, Stat.DEF, 252, Stat.SPA, 0, Stat.SPD, 4, Stat.SPE, 0),
                null,
                List.of(
                        new MoveProfile("followme", "normal", MoveProfile.Category.STATUS, 0, 2, MoveProfile.Target.SELF, false, false),
                        new MoveProfile("helpinghand", "normal", MoveProfile.Category.STATUS, 0, 5, MoveProfile.Target.ALLY, false, false),
                        new MoveProfile("dazzlinggleam", "fairy", MoveProfile.Category.SPECIAL, 80, 0, MoveProfile.Target.ALL_ADJACENT_OPPONENTS, false, true),
                        new MoveProfile("protect", "normal", MoveProfile.Category.STATUS, 0, 4, MoveProfile.Target.SELF, false, false)
                )
        );
    }

    // 2. Indeedee-M (Psychic Surge, Expanding Force offensive)
    public static CompetitivePokemonProfile indeedeeM() {
        return build("indeedee-m", List.of("psychic", "normal"), "psychicsurge", "choice_specs", "modest",
                Map.of(Stat.HP, 60, Stat.ATK, 65, Stat.DEF, 55, Stat.SPA, 105, Stat.SPD, 95, Stat.SPE, 95),
                Map.of(Stat.HP, 0, Stat.ATK, 0, Stat.DEF, 0, Stat.SPA, 252, Stat.SPD, 4, Stat.SPE, 252),
                null,
                List.of(
                        new MoveProfile("expandingforce", "psychic", MoveProfile.Category.SPECIAL, 80, 0, MoveProfile.Target.SINGLE_OPPONENT, false, false),
                        new MoveProfile("hypervoice", "normal", MoveProfile.Category.SPECIAL, 90, 0, MoveProfile.Target.ALL_ADJACENT_OPPONENTS, false, true),
                        new MoveProfile("dazzlinggleam", "fairy", MoveProfile.Category.SPECIAL, 80, 0, MoveProfile.Target.ALL_ADJACENT_OPPONENTS, false, true),
                        new MoveProfile("protect", "normal", MoveProfile.Category.STATUS, 0, 4, MoveProfile.Target.SELF, false, false)
                )
        );
    }

    // 3. Kingambit (Defiant, Kowtow Cleave / Sucker Punch)
    public static CompetitivePokemonProfile kingambit() {
        return build("kingambit", List.of("dark", "steel"), "defiant", "black_glasses", "adamant",
                Map.of(Stat.HP, 100, Stat.ATK, 135, Stat.DEF, 120, Stat.SPA, 60, Stat.SPD, 85, Stat.SPE, 50),
                Map.of(Stat.HP, 252, Stat.ATK, 252, Stat.DEF, 0, Stat.SPA, 0, Stat.SPD, 4, Stat.SPE, 0),
                null,
                List.of(
                        new MoveProfile("kowtowcleave", "dark", MoveProfile.Category.PHYSICAL, 85, 0, MoveProfile.Target.SINGLE_OPPONENT, true, false),
                        new MoveProfile("suckerpunch", "dark", MoveProfile.Category.PHYSICAL, 70, 1, MoveProfile.Target.SINGLE_OPPONENT, true, false),
                        new MoveProfile("ironhead", "steel", MoveProfile.Category.PHYSICAL, 80, 0, MoveProfile.Target.SINGLE_OPPONENT, true, false),
                        new MoveProfile("protect", "normal", MoveProfile.Category.STATUS, 0, 4, MoveProfile.Target.SELF, false, false)
                )
        );
    }

    // 4. Heatran (Flash Fire, Heat Wave / Earth Power)
    public static CompetitivePokemonProfile heatran() {
        return build("heatran", List.of("fire", "steel"), "flashfire", "life_orb", "modest",
                Map.of(Stat.HP, 91, Stat.ATK, 90, Stat.DEF, 106, Stat.SPA, 130, Stat.SPD, 106, Stat.SPE, 77),
                Map.of(Stat.HP, 252, Stat.ATK, 0, Stat.DEF, 0, Stat.SPA, 252, Stat.SPD, 4, Stat.SPE, 0),
                null,
                List.of(
                        new MoveProfile("heatwave", "fire", MoveProfile.Category.SPECIAL, 95, 0, MoveProfile.Target.ALL_ADJACENT_OPPONENTS, false, true),
                        new MoveProfile("earthpower", "ground", MoveProfile.Category.SPECIAL, 90, 0, MoveProfile.Target.SINGLE_OPPONENT, false, false),
                        new MoveProfile("flashcannon", "steel", MoveProfile.Category.SPECIAL, 80, 0, MoveProfile.Target.SINGLE_OPPONENT, false, false),
                        new MoveProfile("protect", "normal", MoveProfile.Category.STATUS, 0, 4, MoveProfile.Target.SELF, false, false)
                )
        );
    }

    // 5. Cresselia (Levitate, Trick Room)
    public static CompetitivePokemonProfile cresselia() {
        return build("cresselia", List.of("psychic"), "levitate", "sitrus_berry", "calm",
                Map.of(Stat.HP, 120, Stat.ATK, 70, Stat.DEF, 120, Stat.SPA, 75, Stat.SPD, 130, Stat.SPE, 85),
                Map.of(Stat.HP, 252, Stat.ATK, 0, Stat.DEF, 156, Stat.SPA, 0, Stat.SPD, 100, Stat.SPE, 0),
                Map.of(Stat.HP, 31, Stat.ATK, 31, Stat.DEF, 31, Stat.SPA, 31, Stat.SPD, 31, Stat.SPE, 0),
                List.of(
                        new MoveProfile("trickroom", "psychic", MoveProfile.Category.STATUS, 0, -7, MoveProfile.Target.SELF, false, false),
                        new MoveProfile("moonblast", "fairy", MoveProfile.Category.SPECIAL, 95, 0, MoveProfile.Target.SINGLE_OPPONENT, false, false),
                        new MoveProfile("helpinghand", "normal", MoveProfile.Category.STATUS, 0, 5, MoveProfile.Target.ALLY, false, false),
                        new MoveProfile("protect", "normal", MoveProfile.Category.STATUS, 0, 4, MoveProfile.Target.SELF, false, false)
                )
        );
    }

    // 6. Pelipper (Drizzle)
    public static CompetitivePokemonProfile pelipper() {
        return build("pelipper", List.of("water", "flying"), "drizzle", "focus_sash", "modest",
                Map.of(Stat.HP, 60, Stat.ATK, 50, Stat.DEF, 100, Stat.SPA, 95, Stat.SPD, 70, Stat.SPE, 65),
                Map.of(Stat.HP, 252, Stat.ATK, 0, Stat.DEF, 0, Stat.SPA, 252, Stat.SPD, 4, Stat.SPE, 0),
                null,
                List.of(
                        new MoveProfile("weatherball", "normal", MoveProfile.Category.SPECIAL, 50, 0, MoveProfile.Target.SINGLE_OPPONENT, false, false, 100),
                        new MoveProfile("hurricane", "flying", MoveProfile.Category.SPECIAL, 110, 0, MoveProfile.Target.SINGLE_OPPONENT, false, false, 70),
                        new MoveProfile("tailwind", "flying", MoveProfile.Category.STATUS, 0, 0, MoveProfile.Target.SELF, false, false, 100),
                        new MoveProfile("protect", "normal", MoveProfile.Category.STATUS, 0, 4, MoveProfile.Target.SELF, false, false, 100)
                )
        );
    }

    // 7. Archaludon (Stamina, Assault Vest)
    public static CompetitivePokemonProfile archaludon() {
        return build("archaludon", List.of("steel", "dragon"), "stamina", "assault_vest", "modest",
                Map.of(Stat.HP, 90, Stat.ATK, 105, Stat.DEF, 130, Stat.SPA, 125, Stat.SPD, 65, Stat.SPE, 85),
                Map.of(Stat.HP, 252, Stat.ATK, 0, Stat.DEF, 4, Stat.SPA, 252, Stat.SPD, 0, Stat.SPE, 0),
                null,
                List.of(
                        new MoveProfile("flashcannon", "steel", MoveProfile.Category.SPECIAL, 80, 0, MoveProfile.Target.SINGLE_OPPONENT, false, false, 100),
                        new MoveProfile("dracometeor", "dragon", MoveProfile.Category.SPECIAL, 130, 0, MoveProfile.Target.SINGLE_OPPONENT, false, false, 90),
                        new MoveProfile("bodypress", "fighting", MoveProfile.Category.PHYSICAL, 80, 0, MoveProfile.Target.SINGLE_OPPONENT, true, false, 100),
                        new MoveProfile("thunderbolt", "electric", MoveProfile.Category.SPECIAL, 90, 0, MoveProfile.Target.SINGLE_OPPONENT, false, false, 100)
                )
        );
    }

    // 8. Landorus-T (Intimidate)
    public static CompetitivePokemonProfile landorusT() {
        return build("landorus", List.of("ground", "flying"), "intimidate", "choice_scarf", "adamant",
                Map.of(Stat.HP, 89, Stat.ATK, 145, Stat.DEF, 90, Stat.SPA, 105, Stat.SPD, 80, Stat.SPE, 91),
                Map.of(Stat.HP, 4, Stat.ATK, 252, Stat.DEF, 0, Stat.SPA, 0, Stat.SPD, 0, Stat.SPE, 252),
                null,
                List.of(
                        new MoveProfile("stompingtantrum", "ground", MoveProfile.Category.PHYSICAL, 75, 0, MoveProfile.Target.SINGLE_OPPONENT, true, false, 100),
                        new MoveProfile("rockslide", "rock", MoveProfile.Category.PHYSICAL, 75, 0, MoveProfile.Target.ALL_ADJACENT_OPPONENTS, false, true, 90),
                        new MoveProfile("uturn", "bug", MoveProfile.Category.PHYSICAL, 70, 0, MoveProfile.Target.SINGLE_OPPONENT, true, false, 100),
                        new MoveProfile("terablast", "normal", MoveProfile.Category.PHYSICAL, 80, 0, MoveProfile.Target.SINGLE_OPPONENT, true, false, 100)
                )
        );
    }

    // 9. Gholdengo (Good as Gold)
    public static CompetitivePokemonProfile gholdengo() {
        return build("gholdengo", List.of("steel", "ghost"), "goodasgold", "choice_specs", "modest",
                Map.of(Stat.HP, 87, Stat.ATK, 60, Stat.DEF, 95, Stat.SPA, 133, Stat.SPD, 91, Stat.SPE, 84),
                Map.of(Stat.HP, 252, Stat.ATK, 0, Stat.DEF, 0, Stat.SPA, 252, Stat.SPD, 4, Stat.SPE, 0),
                null,
                List.of(
                        new MoveProfile("makeitrain", "steel", MoveProfile.Category.SPECIAL, 120, 0, MoveProfile.Target.ALL_ADJACENT_OPPONENTS, false, true),
                        new MoveProfile("shadowball", "ghost", MoveProfile.Category.SPECIAL, 80, 0, MoveProfile.Target.SINGLE_OPPONENT, false, false),
                        new MoveProfile("thunderbolt", "electric", MoveProfile.Category.SPECIAL, 90, 0, MoveProfile.Target.SINGLE_OPPONENT, false, false),
                        new MoveProfile("protect", "normal", MoveProfile.Category.STATUS, 0, 4, MoveProfile.Target.SELF, false, false)
                )
        );
    }

    // 10. Talonflame (Gale Wings)
    public static CompetitivePokemonProfile talonflame() {
        return build("talonflame", List.of("fire", "flying"), "galewings", "life_orb", "jolly",
                Map.of(Stat.HP, 78, Stat.ATK, 81, Stat.DEF, 71, Stat.SPA, 74, Stat.SPD, 69, Stat.SPE, 126),
                Map.of(Stat.HP, 4, Stat.ATK, 252, Stat.DEF, 0, Stat.SPA, 0, Stat.SPD, 0, Stat.SPE, 252),
                null,
                List.of(
                        new MoveProfile("tailwind", "flying", MoveProfile.Category.STATUS, 0, 0, MoveProfile.Target.SELF, false, false),
                        new MoveProfile("bravebird", "flying", MoveProfile.Category.PHYSICAL, 120, 0, MoveProfile.Target.SINGLE_OPPONENT, true, false),
                        new MoveProfile("flareblitz", "fire", MoveProfile.Category.PHYSICAL, 120, 0, MoveProfile.Target.SINGLE_OPPONENT, true, false),
                        new MoveProfile("protect", "normal", MoveProfile.Category.STATUS, 0, 4, MoveProfile.Target.SELF, false, false)
                )
        );
    }

    // 11. Charizard (Solar Power)
    public static CompetitivePokemonProfile charizard() {
        return build("charizard", List.of("fire", "flying"), "solarpower", "choice_specs", "timid",
                Map.of(Stat.HP, 78, Stat.ATK, 84, Stat.DEF, 78, Stat.SPA, 109, Stat.SPD, 85, Stat.SPE, 100),
                Map.of(Stat.HP, 4, Stat.ATK, 0, Stat.DEF, 0, Stat.SPA, 252, Stat.SPD, 0, Stat.SPE, 252),
                null,
                List.of(
                        new MoveProfile("heatwave", "fire", MoveProfile.Category.SPECIAL, 95, 0, MoveProfile.Target.ALL_ADJACENT_OPPONENTS, false, true),
                        new MoveProfile("airslash", "flying", MoveProfile.Category.SPECIAL, 75, 0, MoveProfile.Target.SINGLE_OPPONENT, false, false),
                        new MoveProfile("solarbeam", "grass", MoveProfile.Category.SPECIAL, 120, 0, MoveProfile.Target.SINGLE_OPPONENT, false, false),
                        new MoveProfile("protect", "normal", MoveProfile.Category.STATUS, 0, 4, MoveProfile.Target.SELF, false, false)
                )
        );
    }

    // Additional VGC species
    public static CompetitivePokemonProfile armarouge() {
        return build("armarouge", List.of("fire", "psychic"), "flashfire", "life_orb", "modest",
                Map.of(Stat.HP, 85, Stat.ATK, 60, Stat.DEF, 100, Stat.SPA, 125, Stat.SPD, 80, Stat.SPE, 75),
                Map.of(Stat.HP, 252, Stat.ATK, 0, Stat.DEF, 0, Stat.SPA, 252, Stat.SPD, 4, Stat.SPE, 0), null,
                List.of(new MoveProfile("expandingforce", "psychic", MoveProfile.Category.SPECIAL, 80, 0, MoveProfile.Target.SINGLE_OPPONENT, false, false),
                        new MoveProfile("armorcanon", "fire", MoveProfile.Category.SPECIAL, 120, 0, MoveProfile.Target.SINGLE_OPPONENT, false, false))
        );
    }

    public static CompetitivePokemonProfile hatterene() {
        return build("hatterene", List.of("psychic", "fairy"), "magicbounce", "life_orb", "quiet",
                Map.of(Stat.HP, 57, Stat.ATK, 90, Stat.DEF, 95, Stat.SPA, 136, Stat.SPD, 103, Stat.SPE, 29),
                Map.of(Stat.HP, 252, Stat.ATK, 0, Stat.DEF, 0, Stat.SPA, 252, Stat.SPD, 4, Stat.SPE, 0),
                Map.of(Stat.HP, 31, Stat.ATK, 31, Stat.DEF, 31, Stat.SPA, 31, Stat.SPD, 31, Stat.SPE, 0),
                List.of(new MoveProfile("trickroom", "psychic", MoveProfile.Category.STATUS, 0, -7, MoveProfile.Target.SELF, false, false),
                        new MoveProfile("dazzlinggleam", "fairy", MoveProfile.Category.SPECIAL, 80, 0, MoveProfile.Target.ALL_ADJACENT_OPPONENTS, false, true))
        );
    }

    public static CompetitivePokemonProfile farigiraf() {
        return build("farigiraf", List.of("normal", "psychic"), "armor_tail", "sitrus_berry", "modest",
                Map.of(Stat.HP, 120, Stat.ATK, 90, Stat.DEF, 70, Stat.SPA, 110, Stat.SPD, 70, Stat.SPE, 60),
                Map.of(Stat.HP, 252, Stat.ATK, 0, Stat.DEF, 100, Stat.SPA, 156, Stat.SPD, 0, Stat.SPE, 0), null,
                List.of(new MoveProfile("psychic", "psychic", MoveProfile.Category.SPECIAL, 90, 0, MoveProfile.Target.SINGLE_OPPONENT, false, false),
                        new MoveProfile("hypervoice", "normal", MoveProfile.Category.SPECIAL, 90, 0, MoveProfile.Target.ALL_ADJACENT_OPPONENTS, false, true))
        );
    }

    public static CompetitivePokemonProfile ironcrown() {
        return build("ironcrown", List.of("steel", "psychic"), "quarkdrive", "booster_energy", "timid",
                Map.of(Stat.HP, 90, Stat.ATK, 72, Stat.DEF, 100, Stat.SPA, 122, Stat.SPD, 108, Stat.SPE, 98),
                Map.of(Stat.HP, 0, Stat.ATK, 0, Stat.DEF, 0, Stat.SPA, 252, Stat.SPD, 4, Stat.SPE, 252), null,
                List.of(new MoveProfile("tachyoncutter", "steel", MoveProfile.Category.SPECIAL, 50, 0, MoveProfile.Target.SINGLE_OPPONENT, false, false),
                        new MoveProfile("expandingforce", "psychic", MoveProfile.Category.SPECIAL, 80, 0, MoveProfile.Target.SINGLE_OPPONENT, false, false))
        );
    }

    public static CompetitivePokemonProfile alakazam() {
        return build("alakazam", List.of("psychic"), "magicguard", "focus_sash", "timid",
                Map.of(Stat.HP, 55, Stat.ATK, 50, Stat.DEF, 45, Stat.SPA, 135, Stat.SPD, 95, Stat.SPE, 120),
                Map.of(Stat.HP, 0, Stat.ATK, 0, Stat.DEF, 0, Stat.SPA, 252, Stat.SPD, 4, Stat.SPE, 252), null,
                List.of(new MoveProfile("psychic", "psychic", MoveProfile.Category.SPECIAL, 90, 0, MoveProfile.Target.SINGLE_OPPONENT, false, false),
                        new MoveProfile("dazzlinggleam", "fairy", MoveProfile.Category.SPECIAL, 80, 0, MoveProfile.Target.ALL_ADJACENT_OPPONENTS, false, true))
        );
    }

    public static CompetitivePokemonProfile chiyu() {
        return build("chiyu", List.of("dark", "fire"), "beadsofruin", "choice_specs", "modest",
                Map.of(Stat.HP, 55, Stat.ATK, 80, Stat.DEF, 80, Stat.SPA, 135, Stat.SPD, 120, Stat.SPE, 100),
                Map.of(Stat.HP, 4, Stat.ATK, 0, Stat.DEF, 0, Stat.SPA, 252, Stat.SPD, 0, Stat.SPE, 252), null,
                List.of(new MoveProfile("heatwave", "fire", MoveProfile.Category.SPECIAL, 95, 0, MoveProfile.Target.ALL_ADJACENT_OPPONENTS, false, true),
                        new MoveProfile("darkpulse", "dark", MoveProfile.Category.SPECIAL, 80, 0, MoveProfile.Target.SINGLE_OPPONENT, false, false))
        );
    }

    public static CompetitivePokemonProfile fluttermane() {
        return build("fluttermane", List.of("ghost", "fairy"), "protosynthesis", "booster_energy", "timid",
                Map.of(Stat.HP, 55, Stat.ATK, 55, Stat.DEF, 55, Stat.SPA, 135, Stat.SPD, 135, Stat.SPE, 135),
                Map.of(Stat.HP, 4, Stat.ATK, 0, Stat.DEF, 0, Stat.SPA, 252, Stat.SPD, 0, Stat.SPE, 252), null,
                List.of(new MoveProfile("moonblast", "fairy", MoveProfile.Category.SPECIAL, 95, 0, MoveProfile.Target.SINGLE_OPPONENT, false, false),
                        new MoveProfile("shadowball", "ghost", MoveProfile.Category.SPECIAL, 80, 0, MoveProfile.Target.SINGLE_OPPONENT, false, false),
                        new MoveProfile("dazzlinggleam", "fairy", MoveProfile.Category.SPECIAL, 80, 0, MoveProfile.Target.ALL_ADJACENT_OPPONENTS, false, true))
        );
    }

    public static CompetitivePokemonProfile chienpao() {
        return build("chienpao", List.of("dark", "ice"), "swordofruin", "focus_sash", "jolly",
                Map.of(Stat.HP, 80, Stat.ATK, 120, Stat.DEF, 80, Stat.SPA, 90, Stat.SPD, 65, Stat.SPE, 135),
                Map.of(Stat.HP, 4, Stat.ATK, 252, Stat.DEF, 0, Stat.SPA, 0, Stat.SPD, 0, Stat.SPE, 252), null,
                List.of(new MoveProfile("iciclespinner", "ice", MoveProfile.Category.PHYSICAL, 80, 0, MoveProfile.Target.SINGLE_OPPONENT, true, false),
                        new MoveProfile("suckerpunch", "dark", MoveProfile.Category.PHYSICAL, 70, 1, MoveProfile.Target.SINGLE_OPPONENT, true, false),
                        new MoveProfile("sacredsword", "fighting", MoveProfile.Category.PHYSICAL, 90, 0, MoveProfile.Target.SINGLE_OPPONENT, true, false))
        );
    }

    public static CompetitivePokemonProfile dragonite() {
        return build("dragonite", List.of("dragon", "flying"), "multiscale", "choice_band", "adamant",
                Map.of(Stat.HP, 91, Stat.ATK, 134, Stat.DEF, 95, Stat.SPA, 100, Stat.SPD, 100, Stat.SPE, 80),
                Map.of(Stat.HP, 252, Stat.ATK, 252, Stat.DEF, 0, Stat.SPA, 0, Stat.SPD, 4, Stat.SPE, 0), null,
                List.of(new MoveProfile("extremespeed", "normal", MoveProfile.Category.PHYSICAL, 80, 2, MoveProfile.Target.SINGLE_OPPONENT, true, false),
                        new MoveProfile("outrage", "dragon", MoveProfile.Category.PHYSICAL, 120, 0, MoveProfile.Target.SINGLE_OPPONENT, true, false))
        );
    }

    public static CompetitivePokemonProfile incineroar() {
        return build("incineroar", List.of("fire", "dark"), "intimidate", "sitrus_berry", "careful",
                Map.of(Stat.HP, 95, Stat.ATK, 115, Stat.DEF, 90, Stat.SPA, 80, Stat.SPD, 90, Stat.SPE, 60),
                Map.of(Stat.HP, 252, Stat.ATK, 0, Stat.DEF, 156, Stat.SPA, 0, Stat.SPD, 100, Stat.SPE, 0), null,
                List.of(new MoveProfile("fakeout", "normal", MoveProfile.Category.PHYSICAL, 40, 3, MoveProfile.Target.SINGLE_OPPONENT, true, false),
                        new MoveProfile("knockoff", "dark", MoveProfile.Category.PHYSICAL, 65, 0, MoveProfile.Target.SINGLE_OPPONENT, true, false),
                        new MoveProfile("flareblitz", "fire", MoveProfile.Category.PHYSICAL, 120, 0, MoveProfile.Target.SINGLE_OPPONENT, true, false),
                        new MoveProfile("partingshot", "dark", MoveProfile.Category.STATUS, 0, 0, MoveProfile.Target.SINGLE_OPPONENT, false, false))
        );
    }

    public static CompetitivePokemonProfile tornadus() {
        return build("tornadus", List.of("flying"), "prankster", "covert_cloak", "timid",
                Map.of(Stat.HP, 79, Stat.ATK, 115, Stat.DEF, 70, Stat.SPA, 125, Stat.SPD, 80, Stat.SPE, 111),
                Map.of(Stat.HP, 252, Stat.ATK, 0, Stat.DEF, 0, Stat.SPA, 4, Stat.SPD, 0, Stat.SPE, 252), null,
                List.of(new MoveProfile("tailwind", "flying", MoveProfile.Category.STATUS, 0, 1, MoveProfile.Target.SELF, false, false),
                        new MoveProfile("bleakwindstorm", "flying", MoveProfile.Category.SPECIAL, 100, 0, MoveProfile.Target.ALL_ADJACENT_OPPONENTS, false, true))
        );
    }

    public static CompetitivePokemonProfile magnezone() {
        return build("magnezone", List.of("electric", "steel"), "magnetpull", "assault_vest", "modest",
                Map.of(Stat.HP, 70, Stat.ATK, 70, Stat.DEF, 115, Stat.SPA, 130, Stat.SPD, 90, Stat.SPE, 60),
                Map.of(Stat.HP, 252, Stat.ATK, 0, Stat.DEF, 0, Stat.SPA, 252, Stat.SPD, 4, Stat.SPE, 0), null,
                List.of(new MoveProfile("thunderbolt", "electric", MoveProfile.Category.SPECIAL, 90, 0, MoveProfile.Target.SINGLE_OPPONENT, false, false),
                        new MoveProfile("flashcannon", "steel", MoveProfile.Category.SPECIAL, 80, 0, MoveProfile.Target.SINGLE_OPPONENT, false, false))
        );
    }

    public static CompetitivePokemonProfile corviknight() {
        return build("corviknight", List.of("flying", "steel"), "mirrorarmor", "leftovers", "careful",
                Map.of(Stat.HP, 98, Stat.ATK, 87, Stat.DEF, 105, Stat.SPA, 53, Stat.SPD, 85, Stat.SPE, 67),
                Map.of(Stat.HP, 252, Stat.ATK, 0, Stat.DEF, 4, Stat.SPA, 0, Stat.SPD, 252, Stat.SPE, 0), null,
                List.of(new MoveProfile("bravebird", "flying", MoveProfile.Category.PHYSICAL, 120, 0, MoveProfile.Target.SINGLE_OPPONENT, true, false),
                        new MoveProfile("ironhead", "steel", MoveProfile.Category.PHYSICAL, 80, 0, MoveProfile.Target.SINGLE_OPPONENT, true, false))
        );
    }

    public static CompetitivePokemonProfile skarmory() {
        return build("skarmory", List.of("steel", "flying"), "sturdy", "rocky_helmet", "impish",
                Map.of(Stat.HP, 65, Stat.ATK, 80, Stat.DEF, 140, Stat.SPA, 40, Stat.SPD, 70, Stat.SPE, 70),
                Map.of(Stat.HP, 252, Stat.ATK, 0, Stat.DEF, 252, Stat.SPA, 0, Stat.SPD, 4, Stat.SPE, 0), null,
                List.of(new MoveProfile("bravebird", "flying", MoveProfile.Category.PHYSICAL, 120, 0, MoveProfile.Target.SINGLE_OPPONENT, true, false),
                        new MoveProfile("bodypress", "fighting", MoveProfile.Category.PHYSICAL, 80, 0, MoveProfile.Target.SINGLE_OPPONENT, true, false))
        );
    }

    public static CompetitivePokemonProfile garchomp() {
        return build("garchomp", List.of("dragon", "ground"), "roughskin", "life_orb", "jolly",
                Map.of(Stat.HP, 108, Stat.ATK, 130, Stat.DEF, 95, Stat.SPA, 80, Stat.SPD, 85, Stat.SPE, 102),
                Map.of(Stat.HP, 4, Stat.ATK, 252, Stat.DEF, 0, Stat.SPA, 0, Stat.SPD, 0, Stat.SPE, 252), null,
                List.of(new MoveProfile("earthquake", "ground", MoveProfile.Category.PHYSICAL, 100, 0, MoveProfile.Target.ALL_ADJACENT_OPPONENTS, false, true),
                        new MoveProfile("dragonclaw", "dragon", MoveProfile.Category.PHYSICAL, 80, 0, MoveProfile.Target.SINGLE_OPPONENT, true, false))
        );
    }

    public static CompetitivePokemonProfile greattusk() {
        return build("greattusk", List.of("ground", "fighting"), "protosynthesis", "booster_energy", "jolly",
                Map.of(Stat.HP, 115, Stat.ATK, 131, Stat.DEF, 131, Stat.SPA, 53, Stat.SPD, 53, Stat.SPE, 87),
                Map.of(Stat.HP, 4, Stat.ATK, 252, Stat.DEF, 0, Stat.SPA, 0, Stat.SPD, 0, Stat.SPE, 252), null,
                List.of(new MoveProfile("headlongrush", "ground", MoveProfile.Category.PHYSICAL, 120, 0, MoveProfile.Target.SINGLE_OPPONENT, true, false),
                        new MoveProfile("closecombat", "fighting", MoveProfile.Category.PHYSICAL, 120, 0, MoveProfile.Target.SINGLE_OPPONENT, true, false))
        );
    }

    public static CompetitivePokemonProfile gliscor() {
        return build("gliscor", List.of("ground", "flying"), "poisonheal", "toxic_orb", "impish",
                Map.of(Stat.HP, 75, Stat.ATK, 95, Stat.DEF, 125, Stat.SPA, 45, Stat.SPD, 75, Stat.SPE, 95),
                Map.of(Stat.HP, 252, Stat.ATK, 0, Stat.DEF, 252, Stat.SPA, 0, Stat.SPD, 4, Stat.SPE, 0), null,
                List.of(new MoveProfile("earthquake", "ground", MoveProfile.Category.PHYSICAL, 100, 0, MoveProfile.Target.ALL_ADJACENT_OPPONENTS, false, true))
        );
    }

    public static CompetitivePokemonProfile mamoswine() {
        return build("mamoswine", List.of("ice", "ground"), "thickfat", "life_orb", "adamant",
                Map.of(Stat.HP, 110, Stat.ATK, 130, Stat.DEF, 80, Stat.SPA, 70, Stat.SPD, 60, Stat.SPE, 80),
                Map.of(Stat.HP, 4, Stat.ATK, 252, Stat.DEF, 0, Stat.SPA, 0, Stat.SPD, 0, Stat.SPE, 252), null,
                List.of(new MoveProfile("earthquake", "ground", MoveProfile.Category.PHYSICAL, 100, 0, MoveProfile.Target.ALL_ADJACENT_OPPONENTS, false, true),
                        new MoveProfile("iciclespear", "ice", MoveProfile.Category.PHYSICAL, 75, 0, MoveProfile.Target.SINGLE_OPPONENT, true, false))
        );
    }

    public static CompetitivePokemonProfile tinglu() {
        return build("tinglu", List.of("dark", "ground"), "vesselofruin", "leftovers", "impish",
                Map.of(Stat.HP, 155, Stat.ATK, 110, Stat.DEF, 125, Stat.SPA, 55, Stat.SPD, 80, Stat.SPE, 45),
                Map.of(Stat.HP, 252, Stat.ATK, 0, Stat.DEF, 252, Stat.SPA, 0, Stat.SPD, 4, Stat.SPE, 0), null,
                List.of(new MoveProfile("stompingtantrum", "ground", MoveProfile.Category.PHYSICAL, 75, 0, MoveProfile.Target.SINGLE_OPPONENT, true, false),
                        new MoveProfile("ruination", "dark", MoveProfile.Category.SPECIAL, 1, 0, MoveProfile.Target.SINGLE_OPPONENT, false, false))
        );
    }

    public static CompetitivePokemonProfile excadrill() {
        return build("excadrill", List.of("ground", "steel"), "sandrush", "life_orb", "adamant",
                Map.of(Stat.HP, 110, Stat.ATK, 135, Stat.DEF, 60, Stat.SPA, 50, Stat.SPD, 65, Stat.SPE, 88),
                Map.of(Stat.HP, 4, Stat.ATK, 252, Stat.DEF, 0, Stat.SPA, 0, Stat.SPD, 0, Stat.SPE, 252), null,
                List.of(new MoveProfile("ironhead", "steel", MoveProfile.Category.PHYSICAL, 80, 0, MoveProfile.Target.SINGLE_OPPONENT, true, false),
                        new MoveProfile("earthquake", "ground", MoveProfile.Category.PHYSICAL, 100, 0, MoveProfile.Target.ALL_ADJACENT_OPPONENTS, false, true))
        );
    }

    public static CompetitivePokemonProfile tyranitar() {
        return build("tyranitar", List.of("rock", "dark"), "sandstream", "choice_band", "adamant",
                Map.of(Stat.HP, 100, Stat.ATK, 134, Stat.DEF, 110, Stat.SPA, 95, Stat.SPD, 100, Stat.SPE, 61),
                Map.of(Stat.HP, 252, Stat.ATK, 252, Stat.DEF, 0, Stat.SPA, 0, Stat.SPD, 4, Stat.SPE, 0), null,
                List.of(new MoveProfile("rockslide", "rock", MoveProfile.Category.PHYSICAL, 75, 0, MoveProfile.Target.ALL_ADJACENT_OPPONENTS, false, true),
                        new MoveProfile("crunch", "dark", MoveProfile.Category.PHYSICAL, 80, 0, MoveProfile.Target.SINGLE_OPPONENT, true, false))
        );
    }

    public static CompetitivePokemonProfile gastrodon() {
        return build("gastrodon", List.of("water", "ground"), "stormdrain", "leftovers", "calm",
                Map.of(Stat.HP, 111, Stat.ATK, 83, Stat.DEF, 68, Stat.SPA, 92, Stat.SPD, 82, Stat.SPE, 39),
                Map.of(Stat.HP, 252, Stat.ATK, 0, Stat.DEF, 156, Stat.SPA, 0, Stat.SPD, 100, Stat.SPE, 0), null,
                List.of(new MoveProfile("earthpower", "ground", MoveProfile.Category.SPECIAL, 90, 0, MoveProfile.Target.SINGLE_OPPONENT, false, false),
                        new MoveProfile("muddywater", "water", MoveProfile.Category.SPECIAL, 90, 0, MoveProfile.Target.ALL_ADJACENT_OPPONENTS, false, true))
        );
    }

    public static CompetitivePokemonProfile swampert() {
        return build("swampert", List.of("water", "ground"), "torrent", "assault_vest", "adamant",
                Map.of(Stat.HP, 100, Stat.ATK, 110, Stat.DEF, 90, Stat.SPA, 85, Stat.SPD, 90, Stat.SPE, 60),
                Map.of(Stat.HP, 252, Stat.ATK, 252, Stat.DEF, 0, Stat.SPA, 0, Stat.SPD, 4, Stat.SPE, 0), null,
                List.of(new MoveProfile("liquidation", "water", MoveProfile.Category.PHYSICAL, 85, 0, MoveProfile.Target.SINGLE_OPPONENT, true, false),
                        new MoveProfile("earthquake", "ground", MoveProfile.Category.PHYSICAL, 100, 0, MoveProfile.Target.ALL_ADJACENT_OPPONENTS, false, true))
        );
    }

    public static CompetitivePokemonProfile ursaluna() {
        return build("ursaluna", List.of("ground", "normal"), "guts", "flame_orb", "brave",
                Map.of(Stat.HP, 130, Stat.ATK, 140, Stat.DEF, 105, Stat.SPA, 45, Stat.SPD, 80, Stat.SPE, 50),
                Map.of(Stat.HP, 252, Stat.ATK, 252, Stat.DEF, 0, Stat.SPA, 0, Stat.SPD, 4, Stat.SPE, 0),
                Map.of(Stat.HP, 31, Stat.ATK, 31, Stat.DEF, 31, Stat.SPA, 31, Stat.SPD, 31, Stat.SPE, 0),
                List.of(new MoveProfile("headlongrush", "ground", MoveProfile.Category.PHYSICAL, 120, 0, MoveProfile.Target.SINGLE_OPPONENT, true, false),
                        new MoveProfile("facade", "normal", MoveProfile.Category.PHYSICAL, 140, 0, MoveProfile.Target.SINGLE_OPPONENT, true, false))
        );
    }

    public static CompetitivePokemonProfile ursuluna() {
        return ursaluna();
    }

    public static CompetitivePokemonProfile salamence() {
        return build("salamence", List.of("dragon", "flying"), "intimidate", "life_orb", "timid",
                Map.of(Stat.HP, 95, Stat.ATK, 135, Stat.DEF, 80, Stat.SPA, 110, Stat.SPD, 80, Stat.SPE, 100),
                Map.of(Stat.HP, 0, Stat.ATK, 0, Stat.DEF, 0, Stat.SPA, 252, Stat.SPD, 4, Stat.SPE, 252), null,
                List.of(new MoveProfile("dracometeor", "dragon", MoveProfile.Category.SPECIAL, 130, 0, MoveProfile.Target.SINGLE_OPPONENT, false, false),
                        new MoveProfile("tailwind", "flying", MoveProfile.Category.STATUS, 0, 0, MoveProfile.Target.SELF, false, false))
        );
    }

    public static CompetitivePokemonProfile politoed() {
        return build("politoed", List.of("water"), "drizzle", "sitrus_berry", "calm",
                Map.of(Stat.HP, 90, Stat.ATK, 75, Stat.DEF, 75, Stat.SPA, 90, Stat.SPD, 100, Stat.SPE, 70),
                Map.of(Stat.HP, 252, Stat.ATK, 0, Stat.DEF, 156, Stat.SPA, 0, Stat.SPD, 100, Stat.SPE, 0), null,
                List.of(new MoveProfile("weatherball", "normal", MoveProfile.Category.SPECIAL, 50, 0, MoveProfile.Target.SINGLE_OPPONENT, false, false),
                        new MoveProfile("helpinghand", "normal", MoveProfile.Category.STATUS, 0, 5, MoveProfile.Target.ALLY, false, false))
        );
    }

    public static CompetitivePokemonProfile urshifuRapidStrike() {
        return build("urshifu", List.of("fighting", "water"), "unseenfist", "choice_scarf", "adamant",
                Map.of(Stat.HP, 100, Stat.ATK, 130, Stat.DEF, 100, Stat.SPA, 63, Stat.SPD, 60, Stat.SPE, 97),
                Map.of(Stat.HP, 4, Stat.ATK, 252, Stat.DEF, 0, Stat.SPA, 0, Stat.SPD, 0, Stat.SPE, 252), null,
                List.of(new MoveProfile("surgingstrikes", "water", MoveProfile.Category.PHYSICAL, 75, 0, MoveProfile.Target.SINGLE_OPPONENT, true, false),
                        new MoveProfile("closecombat", "fighting", MoveProfile.Category.PHYSICAL, 120, 0, MoveProfile.Target.SINGLE_OPPONENT, true, false))
        );
    }

    public static CompetitivePokemonProfile hippowdon() {
        return build("hippowdon", List.of("ground"), "sandstream", "sitrus_berry", "impish",
                Map.of(Stat.HP, 108, Stat.ATK, 112, Stat.DEF, 118, Stat.SPA, 68, Stat.SPD, 72, Stat.SPE, 47),
                Map.of(Stat.HP, 252, Stat.ATK, 0, Stat.DEF, 252, Stat.SPA, 0, Stat.SPD, 4, Stat.SPE, 0), null,
                List.of(new MoveProfile("earthquake", "ground", MoveProfile.Category.PHYSICAL, 100, 0, MoveProfile.Target.ALL_ADJACENT_OPPONENTS, false, true))
        );
    }

    public static CompetitivePokemonProfile abomasnow() {
        return build("abomasnow", List.of("grass", "ice"), "snowwarning", "focus_sash", "quiet",
                Map.of(Stat.HP, 90, Stat.ATK, 92, Stat.DEF, 75, Stat.SPA, 92, Stat.SPD, 85, Stat.SPE, 60),
                Map.of(Stat.HP, 252, Stat.ATK, 0, Stat.DEF, 0, Stat.SPA, 252, Stat.SPD, 4, Stat.SPE, 0), null,
                List.of(new MoveProfile("blizzard", "ice", MoveProfile.Category.SPECIAL, 110, 0, MoveProfile.Target.ALL_ADJACENT_OPPONENTS, false, true))
        );
    }

    public static CompetitivePokemonProfile ninetalesAlola() {
        return build("ninetales", List.of("ice", "fairy"), "snowwarning", "light_clay", "timid",
                Map.of(Stat.HP, 73, Stat.ATK, 67, Stat.DEF, 75, Stat.SPA, 81, Stat.SPD, 100, Stat.SPE, 109),
                Map.of(Stat.HP, 4, Stat.ATK, 0, Stat.DEF, 0, Stat.SPA, 252, Stat.SPD, 0, Stat.SPE, 252), null,
                List.of(new MoveProfile("blizzard", "ice", MoveProfile.Category.SPECIAL, 110, 0, MoveProfile.Target.ALL_ADJACENT_OPPONENTS, false, true),
                        new MoveProfile("auroraveil", "ice", MoveProfile.Category.STATUS, 0, 0, MoveProfile.Target.SELF, false, false))
        );
    }

    public static CompetitivePokemonProfile baxcalibur() {
        return build("baxcalibur", List.of("dragon", "ice"), "thermalexchange", "loaded_dice", "adamant",
                Map.of(Stat.HP, 115, Stat.ATK, 145, Stat.DEF, 92, Stat.SPA, 75, Stat.SPD, 86, Stat.SPE, 87),
                Map.of(Stat.HP, 4, Stat.ATK, 252, Stat.DEF, 0, Stat.SPA, 0, Stat.SPD, 0, Stat.SPE, 252), null,
                List.of(new MoveProfile("iciclecrash", "ice", MoveProfile.Category.PHYSICAL, 85, 0, MoveProfile.Target.SINGLE_OPPONENT, true, false),
                        new MoveProfile("glaiverush", "dragon", MoveProfile.Category.PHYSICAL, 120, 0, MoveProfile.Target.SINGLE_OPPONENT, true, false))
        );
    }

    public static CompetitivePokemonProfile torkoal() {
        return build("torkoal", List.of("fire"), "drought", "charcoal", "quiet",
                Map.of(Stat.HP, 70, Stat.ATK, 85, Stat.DEF, 140, Stat.SPA, 85, Stat.SPD, 70, Stat.SPE, 20),
                Map.of(Stat.HP, 252, Stat.ATK, 0, Stat.DEF, 0, Stat.SPA, 252, Stat.SPD, 4, Stat.SPE, 0),
                Map.of(Stat.HP, 31, Stat.ATK, 31, Stat.DEF, 31, Stat.SPA, 31, Stat.SPD, 31, Stat.SPE, 0),
                List.of(new MoveProfile("eruption", "fire", MoveProfile.Category.SPECIAL, 150, 0, MoveProfile.Target.ALL_ADJACENT_OPPONENTS, false, true),
                        new MoveProfile("heatwave", "fire", MoveProfile.Category.SPECIAL, 95, 0, MoveProfile.Target.ALL_ADJACENT_OPPONENTS, false, true))
        );
    }

    public static CompetitivePokemonProfile lilligantHisui() {
        return build("lilligant", List.of("grass", "fighting"), "chlorophyll", "focus_sash", "adamant",
                Map.of(Stat.HP, 70, Stat.ATK, 105, Stat.DEF, 75, Stat.SPA, 50, Stat.SPD, 75, Stat.SPE, 105),
                Map.of(Stat.HP, 4, Stat.ATK, 252, Stat.DEF, 0, Stat.SPA, 0, Stat.SPD, 0, Stat.SPE, 252), null,
                List.of(new MoveProfile("closecombat", "fighting", MoveProfile.Category.PHYSICAL, 120, 0, MoveProfile.Target.SINGLE_OPPONENT, true, false),
                        new MoveProfile("leafblade", "grass", MoveProfile.Category.PHYSICAL, 90, 0, MoveProfile.Target.SINGLE_OPPONENT, true, false))
        );
    }

    public static CompetitivePokemonProfile rillaboom() {
        return build("rillaboom", List.of("grass"), "grassysurge", "assault_vest", "adamant",
                Map.of(Stat.HP, 100, Stat.ATK, 125, Stat.DEF, 90, Stat.SPA, 60, Stat.SPD, 70, Stat.SPE, 85),
                Map.of(Stat.HP, 252, Stat.ATK, 252, Stat.DEF, 0, Stat.SPA, 0, Stat.SPD, 4, Stat.SPE, 0), null,
                List.of(new MoveProfile("fakeout", "normal", MoveProfile.Category.PHYSICAL, 40, 3, MoveProfile.Target.SINGLE_OPPONENT, true, false),
                        new MoveProfile("grassyglide", "grass", MoveProfile.Category.PHYSICAL, 55, 1, MoveProfile.Target.SINGLE_OPPONENT, true, false),
                        new MoveProfile("woodhammer", "grass", MoveProfile.Category.PHYSICAL, 120, 0, MoveProfile.Target.SINGLE_OPPONENT, true, false))
        );
    }

    public static CompetitivePokemonProfile lucario() {
        return build("lucario", List.of("fighting", "steel"), "innerfocus", "focus_sash", "timid",
                Map.of(Stat.HP, 70, Stat.ATK, 110, Stat.DEF, 70, Stat.SPA, 115, Stat.SPD, 70, Stat.SPE, 90),
                Map.of(Stat.HP, 0, Stat.ATK, 0, Stat.DEF, 0, Stat.SPA, 252, Stat.SPD, 4, Stat.SPE, 252), null,
                List.of(new MoveProfile("aurasphere", "fighting", MoveProfile.Category.SPECIAL, 80, 0, MoveProfile.Target.SINGLE_OPPONENT, false, false),
                        new MoveProfile("flashcannon", "steel", MoveProfile.Category.SPECIAL, 80, 0, MoveProfile.Target.SINGLE_OPPONENT, false, false))
        );
    }

    public static CompetitivePokemonProfile rotomWash() {
        return build("rotom", List.of("electric", "water"), "levitate", "sitrus_berry", "modest",
                Map.of(Stat.HP, 50, Stat.ATK, 65, Stat.DEF, 107, Stat.SPA, 105, Stat.SPD, 107, Stat.SPE, 86),
                Map.of(Stat.HP, 252, Stat.ATK, 0, Stat.DEF, 0, Stat.SPA, 252, Stat.SPD, 4, Stat.SPE, 0), null,
                List.of(new MoveProfile("hydropump", "water", MoveProfile.Category.SPECIAL, 110, 0, MoveProfile.Target.SINGLE_OPPONENT, false, false),
                        new MoveProfile("thunderbolt", "electric", MoveProfile.Category.SPECIAL, 90, 0, MoveProfile.Target.SINGLE_OPPONENT, false, false))
        );
    }

    public static CompetitivePokemonProfile metagross() {
        return build("metagross", List.of("steel", "psychic"), "clearbody", "assault_vest", "adamant",
                Map.of(Stat.HP, 80, Stat.ATK, 135, Stat.DEF, 130, Stat.SPA, 95, Stat.SPD, 90, Stat.SPE, 70),
                Map.of(Stat.HP, 252, Stat.ATK, 252, Stat.DEF, 0, Stat.SPA, 0, Stat.SPD, 4, Stat.SPE, 0), null,
                List.of(new MoveProfile("ironhead", "steel", MoveProfile.Category.PHYSICAL, 80, 0, MoveProfile.Target.SINGLE_OPPONENT, true, false),
                        new MoveProfile("zenheadbutt", "psychic", MoveProfile.Category.PHYSICAL, 80, 0, MoveProfile.Target.SINGLE_OPPONENT, true, false))
        );
    }

    public static CompetitivePokemonProfile milotic() {
        return build("milotic", List.of("water"), "competitive", "leftovers", "bold",
                Map.of(Stat.HP, 95, Stat.ATK, 60, Stat.DEF, 79, Stat.SPA, 100, Stat.SPD, 125, Stat.SPE, 81),
                Map.of(Stat.HP, 252, Stat.ATK, 0, Stat.DEF, 252, Stat.SPA, 0, Stat.SPD, 4, Stat.SPE, 0), null,
                List.of(new MoveProfile("scald", "water", MoveProfile.Category.SPECIAL, 80, 0, MoveProfile.Target.SINGLE_OPPONENT, false, false),
                        new MoveProfile("icebeam", "ice", MoveProfile.Category.SPECIAL, 90, 0, MoveProfile.Target.SINGLE_OPPONENT, false, false))
        );
    }

    public static CompetitivePokemonProfile amoonguss() {
        return KogaCompetitiveProfiles.amoonguss();
    }

    public static CompetitivePokemonProfile arcanine() {
        return build("arcanine", List.of("fire"), "intimidate", "sitrus_berry", "impish",
                Map.of(Stat.HP, 90, Stat.ATK, 110, Stat.DEF, 80, Stat.SPA, 100, Stat.SPD, 80, Stat.SPE, 95),
                Map.of(Stat.HP, 252, Stat.ATK, 0, Stat.DEF, 252, Stat.SPA, 0, Stat.SPD, 4, Stat.SPE, 0), null,
                List.of(new MoveProfile("flareblitz", "fire", MoveProfile.Category.PHYSICAL, 120, 0, MoveProfile.Target.SINGLE_OPPONENT, true, false),
                        new MoveProfile("extremespeed", "normal", MoveProfile.Category.PHYSICAL, 80, 2, MoveProfile.Target.SINGLE_OPPONENT, true, false))
        );
    }

    public static CompetitivePokemonProfile raichu() {
        return build("raichu", List.of("electric"), "lightningrod", "focus_sash", "timid",
                Map.of(Stat.HP, 60, Stat.ATK, 90, Stat.DEF, 55, Stat.SPA, 90, Stat.SPD, 80, Stat.SPE, 110),
                Map.of(Stat.HP, 4, Stat.ATK, 0, Stat.DEF, 0, Stat.SPA, 252, Stat.SPD, 0, Stat.SPE, 252), null,
                List.of(new MoveProfile("fakeout", "normal", MoveProfile.Category.PHYSICAL, 40, 3, MoveProfile.Target.SINGLE_OPPONENT, true, false),
                        new MoveProfile("voltturn", "electric", MoveProfile.Category.SPECIAL, 70, 0, MoveProfile.Target.SINGLE_OPPONENT, false, false))
        );
    }

    public static CompetitivePokemonProfile gyarados() {
        return build("gyarados", List.of("water", "flying"), "intimidate", "sitrus_berry", "adamant",
                Map.of(Stat.HP, 95, Stat.ATK, 125, Stat.DEF, 79, Stat.SPA, 60, Stat.SPD, 100, Stat.SPE, 81),
                Map.of(Stat.HP, 252, Stat.ATK, 252, Stat.DEF, 0, Stat.SPA, 0, Stat.SPD, 4, Stat.SPE, 0), null,
                List.of(new MoveProfile("waterfall", "water", MoveProfile.Category.PHYSICAL, 80, 0, MoveProfile.Target.SINGLE_OPPONENT, true, false),
                        new MoveProfile("thunderwave", "electric", MoveProfile.Category.STATUS, 0, 0, MoveProfile.Target.SINGLE_OPPONENT, false, false))
        );
    }

    public static CompetitivePokemonProfile clefable() {
        return build("clefable", List.of("fairy"), "unaware", "leftovers", "bold",
                Map.of(Stat.HP, 95, Stat.ATK, 70, Stat.DEF, 73, Stat.SPA, 95, Stat.SPD, 90, Stat.SPE, 60),
                Map.of(Stat.HP, 252, Stat.ATK, 0, Stat.DEF, 252, Stat.SPA, 0, Stat.SPD, 4, Stat.SPE, 0), null,
                List.of(new MoveProfile("followme", "normal", MoveProfile.Category.STATUS, 0, 2, MoveProfile.Target.SELF, false, false),
                        new MoveProfile("moonblast", "fairy", MoveProfile.Category.SPECIAL, 95, 0, MoveProfile.Target.SINGLE_OPPONENT, false, false))
        );
    }

    public static CompetitivePokemonProfile snorlax() {
        return build("snorlax", List.of("normal"), "thickfat", "sitrus_berry", "brave",
                Map.of(Stat.HP, 160, Stat.ATK, 110, Stat.DEF, 65, Stat.SPA, 65, Stat.SPD, 110, Stat.SPE, 30),
                Map.of(Stat.HP, 252, Stat.ATK, 252, Stat.DEF, 0, Stat.SPA, 0, Stat.SPD, 4, Stat.SPE, 0),
                Map.of(Stat.HP, 31, Stat.ATK, 31, Stat.DEF, 31, Stat.SPA, 31, Stat.SPD, 31, Stat.SPE, 0),
                List.of(new MoveProfile("bodyslam", "normal", MoveProfile.Category.PHYSICAL, 85, 0, MoveProfile.Target.SINGLE_OPPONENT, true, false))
        );
    }

    public static CompetitivePokemonProfile gengar() {
        return build("gengar", List.of("ghost", "poison"), "cursedbody", "focus_sash", "timid",
                Map.of(Stat.HP, 60, Stat.ATK, 65, Stat.DEF, 60, Stat.SPA, 130, Stat.SPD, 75, Stat.SPE, 110),
                Map.of(Stat.HP, 4, Stat.ATK, 0, Stat.DEF, 0, Stat.SPA, 252, Stat.SPD, 0, Stat.SPE, 252), null,
                List.of(new MoveProfile("shadowball", "ghost", MoveProfile.Category.SPECIAL, 80, 0, MoveProfile.Target.SINGLE_OPPONENT, false, false),
                        new MoveProfile("sludgebomb", "poison", MoveProfile.Category.SPECIAL, 90, 0, MoveProfile.Target.SINGLE_OPPONENT, false, false))
        );
    }

    public static CompetitivePokemonProfile lapras() {
        return build("lapras", List.of("water", "ice"), "waterabsorb", "light_clay", "modest",
                Map.of(Stat.HP, 130, Stat.ATK, 85, Stat.DEF, 80, Stat.SPA, 85, Stat.SPD, 95, Stat.SPE, 60),
                Map.of(Stat.HP, 252, Stat.ATK, 0, Stat.DEF, 0, Stat.SPA, 252, Stat.SPD, 4, Stat.SPE, 0), null,
                List.of(new MoveProfile("freezedry", "ice", MoveProfile.Category.SPECIAL, 70, 0, MoveProfile.Target.SINGLE_OPPONENT, false, false),
                        new MoveProfile("hydropump", "water", MoveProfile.Category.SPECIAL, 110, 0, MoveProfile.Target.SINGLE_OPPONENT, false, false))
        );
    }

    public static CompetitivePokemonProfile machamp() {
        return build("machamp", List.of("fighting"), "noguard", "assault_vest", "adamant",
                Map.of(Stat.HP, 90, Stat.ATK, 130, Stat.DEF, 80, Stat.SPA, 65, Stat.SPD, 85, Stat.SPE, 55),
                Map.of(Stat.HP, 252, Stat.ATK, 252, Stat.DEF, 0, Stat.SPA, 0, Stat.SPD, 4, Stat.SPE, 0), null,
                List.of(new MoveProfile("dynamicpunch", "fighting", MoveProfile.Category.PHYSICAL, 100, 0, MoveProfile.Target.SINGLE_OPPONENT, true, false),
                        new MoveProfile("knockoff", "dark", MoveProfile.Category.PHYSICAL, 65, 0, MoveProfile.Target.SINGLE_OPPONENT, true, false))
        );
    }

    public static CompetitivePokemonProfile togekiss() {
        return build("togekiss", List.of("fairy", "flying"), "serenegrace", "sitrus_berry", "bold",
                Map.of(Stat.HP, 85, Stat.ATK, 50, Stat.DEF, 95, Stat.SPA, 120, Stat.SPD, 115, Stat.SPE, 80),
                Map.of(Stat.HP, 252, Stat.ATK, 0, Stat.DEF, 252, Stat.SPA, 0, Stat.SPD, 4, Stat.SPE, 0), null,
                List.of(new MoveProfile("followme", "normal", MoveProfile.Category.STATUS, 0, 2, MoveProfile.Target.SELF, false, false),
                        new MoveProfile("dazzlinggleam", "fairy", MoveProfile.Category.SPECIAL, 80, 0, MoveProfile.Target.ALL_ADJACENT_OPPONENTS, false, true))
        );
    }

    public static CompetitivePokemonProfile dragapult() {
        return build("dragapult", List.of("dragon", "ghost"), "clearbody", "choice_specs", "timid",
                Map.of(Stat.HP, 88, Stat.ATK, 120, Stat.DEF, 75, Stat.SPA, 100, Stat.SPD, 75, Stat.SPE, 142),
                Map.of(Stat.HP, 4, Stat.ATK, 0, Stat.DEF, 0, Stat.SPA, 252, Stat.SPD, 0, Stat.SPE, 252), null,
                List.of(new MoveProfile("dracometeor", "dragon", MoveProfile.Category.SPECIAL, 130, 0, MoveProfile.Target.SINGLE_OPPONENT, false, false),
                        new MoveProfile("shadowball", "ghost", MoveProfile.Category.SPECIAL, 80, 0, MoveProfile.Target.SINGLE_OPPONENT, false, false))
        );
    }

    public static CompetitivePokemonProfile venusaur() {
        return KogaCompetitiveProfiles.venusaur();
    }
}
