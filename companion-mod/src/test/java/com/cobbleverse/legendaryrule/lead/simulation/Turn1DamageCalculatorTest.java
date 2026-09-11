package com.cobbleverse.legendaryrule.lead.simulation;

import com.cobbleverse.legendaryrule.lead.simulation.calculator.Turn1DamageCalculator;
import com.cobbleverse.legendaryrule.lead.simulation.calculator.Turn1DamageCalculator.DamageRange;
import com.cobbleverse.legendaryrule.lead.simulation.calculator.Turn1StatCalculator;
import com.cobbleverse.legendaryrule.lead.simulation.model.ActualStats;
import com.cobbleverse.legendaryrule.lead.simulation.model.CompetitivePokemonProfile;
import com.cobbleverse.legendaryrule.lead.simulation.model.CompetitivePokemonProfile.Stat;
import com.cobbleverse.legendaryrule.lead.simulation.model.MoveProfile;
import com.cobbleverse.legendaryrule.lead.simulation.model.Turn1BattleState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class Turn1DamageCalculatorTest {

    private static CompetitivePokemonProfile createMon(
            String species,
            List<String> types,
            String ability,
            String heldItem,
            int level,
            Map<Stat, Integer> baseStats,
            String nature,
            Map<Stat, Integer> evs
    ) {
        Map<Stat, Integer> ivs = Map.of(Stat.HP, 31, Stat.ATK, 31, Stat.DEF, 31, Stat.SPA, 31, Stat.SPD, 31, Stat.SPE, 31);
        ActualStats stats = Turn1StatCalculator.calculateStats(baseStats, ivs, evs, nature, level);
        return new CompetitivePokemonProfile(
                species,
                List.of(),
                types,
                level,
                baseStats,
                nature,
                ivs,
                evs,
                ability,
                heldItem,
                List.of(),
                stats
        );
    }

    private static final MoveProfile POISON_JAB = new MoveProfile(
            "poisonjab", "poison", MoveProfile.Category.PHYSICAL, 80, 0, MoveProfile.Target.SINGLE_OPPONENT, true, false
    );
    private static final MoveProfile DRILL_RUN = new MoveProfile(
            "drillrun", "ground", MoveProfile.Category.PHYSICAL, 80, 0, MoveProfile.Target.SINGLE_OPPONENT, true, false
    );
    private static final MoveProfile FLAMETHROWER = new MoveProfile(
            "flamethrower", "fire", MoveProfile.Category.SPECIAL, 90, 0, MoveProfile.Target.SINGLE_OPPONENT, false, false
    );
    private static final MoveProfile HEAT_WAVE = new MoveProfile(
            "heatwave", "fire", MoveProfile.Category.SPECIAL, 95, 0, MoveProfile.Target.ALL_ADJACENT_OPPONENTS, false, true
    );
    private static final MoveProfile PSYCHIC_MOVE = new MoveProfile(
            "psychic", "psychic", MoveProfile.Category.SPECIAL, 90, 0, MoveProfile.Target.SINGLE_OPPONENT, false, false
    );
    private static final MoveProfile EXPANDING_FORCE = new MoveProfile(
            "expandingforce", "psychic", MoveProfile.Category.SPECIAL, 80, 0, MoveProfile.Target.SINGLE_OPPONENT, false, false
    );

    @Test
    @DisplayName("STAB and Adaptability: Mega Beedrill gets 2.0x STAB vs standard 1.5x")
    void testStabAndAdaptability() {
        // Mega Beedrill (Bug/Poison, Adaptability, 150 Base Atk, Level 55)
        CompetitivePokemonProfile beedrill = createMon(
                "beedrill", List.of("bug", "poison"), "adaptability", "", 55,
                Map.of(Stat.HP, 65, Stat.ATK, 150, Stat.DEF, 40, Stat.SPA, 15, Stat.SPD, 80, Stat.SPE, 145),
                "jolly", Map.of(Stat.HP, 4, Stat.ATK, 252, Stat.DEF, 0, Stat.SPA, 0, Stat.SPD, 0, Stat.SPE, 252)
        );

        // Same stats but without Adaptability (Swarm)
        CompetitivePokemonProfile swarmBeedrill = createMon(
                "beedrill", List.of("bug", "poison"), "swarm", "", 55,
                Map.of(Stat.HP, 65, Stat.ATK, 150, Stat.DEF, 40, Stat.SPA, 15, Stat.SPD, 80, Stat.SPE, 145),
                "jolly", Map.of(Stat.HP, 4, Stat.ATK, 252, Stat.DEF, 0, Stat.SPA, 0, Stat.SPD, 0, Stat.SPE, 252)
        );

        // Neutral target: Normal type with 100 HP, 100 Def
        CompetitivePokemonProfile target = createMon(
                "target", List.of("normal"), "none", "", 55,
                Map.of(Stat.HP, 100, Stat.ATK, 100, Stat.DEF, 100, Stat.SPA, 100, Stat.SPD, 100, Stat.SPE, 100),
                "hardy", Map.of(Stat.HP, 0, Stat.ATK, 0, Stat.DEF, 0, Stat.SPA, 0, Stat.SPD, 0, Stat.SPE, 0)
        );

        Turn1BattleState state = new Turn1BattleState(List.of(target, target), List.of(beedrill, swarmBeedrill));

        DamageRange adaptDmg = Turn1DamageCalculator.calculateDamage(beedrill, target, POISON_JAB, state, "koga_0", "player_0");
        DamageRange swarmDmg = Turn1DamageCalculator.calculateDamage(swarmBeedrill, target, POISON_JAB, state, "koga_1", "player_0");

        // Adaptability STAB is 2.0x vs Swarm STAB 1.5x -> ratio should be exactly 2.0 / 1.5 = 1.333x
        double ratio = (double) adaptDmg.maxDamage() / (double) swarmDmg.maxDamage();
        assertTrue(ratio >= 1.30 && ratio <= 1.36, "Adaptability should be ~1.33x standard STAB damage, got ratio=" + ratio);
    }

    @Test
    @DisplayName("Type effectiveness and Immunities (Levitate, Flash Fire)")
    void testTypeEffectivenessAndImmunities() {
        CompetitivePokemonProfile attacker = createMon(
                "beedrill", List.of("bug", "poison"), "adaptability", "", 55,
                Map.of(Stat.HP, 65, Stat.ATK, 150, Stat.DEF, 40, Stat.SPA, 15, Stat.SPD, 80, Stat.SPE, 145),
                "jolly", Map.of(Stat.HP, 4, Stat.ATK, 252, Stat.DEF, 0, Stat.SPA, 0, Stat.SPD, 0, Stat.SPE, 252)
        );

        CompetitivePokemonProfile cresselia = createMon(
                "cresselia", List.of("psychic"), "levitate", "", 55,
                Map.of(Stat.HP, 120, Stat.ATK, 70, Stat.DEF, 120, Stat.SPA, 75, Stat.SPD, 130, Stat.SPE, 85),
                "calm", Map.of(Stat.HP, 252, Stat.ATK, 0, Stat.DEF, 156, Stat.SPA, 0, Stat.SPD, 100, Stat.SPE, 0)
        );

        CompetitivePokemonProfile heatran = createMon(
                "heatran", List.of("fire", "steel"), "flashfire", "", 55,
                Map.of(Stat.HP, 91, Stat.ATK, 90, Stat.DEF, 106, Stat.SPA, 130, Stat.SPD, 106, Stat.SPE, 77),
                "modest", Map.of(Stat.HP, 252, Stat.ATK, 0, Stat.DEF, 0, Stat.SPA, 252, Stat.SPD, 0, Stat.SPE, 0)
        );

        Turn1BattleState state = new Turn1BattleState(List.of(cresselia, heatran), List.of(attacker, attacker));

        // Ground move vs Levitate -> 0 damage
        DamageRange levitateDmg = Turn1DamageCalculator.calculateDamage(attacker, cresselia, DRILL_RUN, state, "koga_0", "player_0");
        assertEquals(0, levitateDmg.minDamage());
        assertEquals(0, levitateDmg.maxDamage());

        // Fire move vs Flash Fire -> 0 damage
        DamageRange flashFireDmg = Turn1DamageCalculator.calculateDamage(attacker, heatran, FLAMETHROWER, state, "koga_0", "player_1");
        assertEquals(0, flashFireDmg.minDamage());
        assertEquals(0, flashFireDmg.maxDamage());

        // Ground move vs Heatran (Fire/Steel) -> 4x super effective!
        DamageRange groundVsHeatran = Turn1DamageCalculator.calculateDamage(attacker, heatran, DRILL_RUN, state, "koga_0", "player_1");
        assertTrue(groundVsHeatran.minDamage() > 150, "4x Drill Run should deal heavy damage to Heatran, got " + groundVsHeatran.minDamage());
    }

    @Test
    @DisplayName("Assault Vest reduces Special damage by 1.5x SpD")
    void testAssaultVestReduction() {
        CompetitivePokemonProfile attacker = createMon(
                "slowking", List.of("poison", "psychic"), "drought", "", 55,
                Map.of(Stat.HP, 95, Stat.ATK, 65, Stat.DEF, 80, Stat.SPA, 110, Stat.SPD, 110, Stat.SPE, 30),
                "modest", Map.of(Stat.HP, 252, Stat.ATK, 0, Stat.DEF, 0, Stat.SPA, 252, Stat.SPD, 0, Stat.SPE, 0)
        );

        CompetitivePokemonProfile okidogiWithAv = createMon(
                "okidogi", List.of("poison", "fighting"), "guarddog", "assault_vest", 55,
                Map.of(Stat.HP, 88, Stat.ATK, 128, Stat.DEF, 115, Stat.SPA, 58, Stat.SPD, 86, Stat.SPE, 80),
                "adamant", Map.of(Stat.HP, 220, Stat.ATK, 108, Stat.DEF, 4, Stat.SPA, 0, Stat.SPD, 60, Stat.SPE, 116)
        );

        CompetitivePokemonProfile okidogiNoAv = createMon(
                "okidogi", List.of("poison", "fighting"), "guarddog", "", 55,
                Map.of(Stat.HP, 88, Stat.ATK, 128, Stat.DEF, 115, Stat.SPA, 58, Stat.SPD, 86, Stat.SPE, 80),
                "adamant", Map.of(Stat.HP, 220, Stat.ATK, 108, Stat.DEF, 4, Stat.SPA, 0, Stat.SPD, 60, Stat.SPE, 116)
        );

        Turn1BattleState state = new Turn1BattleState(List.of(okidogiWithAv, okidogiNoAv), List.of(attacker, attacker));

        DamageRange dmgAv = Turn1DamageCalculator.calculateDamage(attacker, okidogiWithAv, PSYCHIC_MOVE, state, "koga_0", "player_0");
        DamageRange dmgNoAv = Turn1DamageCalculator.calculateDamage(attacker, okidogiNoAv, PSYCHIC_MOVE, state, "koga_0", "player_1");

        // Assault Vest should reduce damage by ~33% (1.0 / 1.5 = 0.667)
        double ratio = (double) dmgAv.maxDamage() / (double) dmgNoAv.maxDamage();
        assertTrue(ratio >= 0.63 && ratio <= 0.70, "Assault Vest should reduce damage by ~1/1.5, got ratio=" + ratio);
    }

    @Test
    @DisplayName("Weather, Spread 0.75x, and Helping Hand 1.5x modifiers")
    void testWeatherSpreadHelpingHand() {
        CompetitivePokemonProfile attacker = createMon(
                "charizard", List.of("fire", "flying"), "solarpower", "", 55,
                Map.of(Stat.HP, 78, Stat.ATK, 84, Stat.DEF, 78, Stat.SPA, 109, Stat.SPD, 85, Stat.SPE, 100),
                "timid", Map.of(Stat.HP, 0, Stat.ATK, 0, Stat.DEF, 0, Stat.SPA, 252, Stat.SPD, 4, Stat.SPE, 252)
        );

        CompetitivePokemonProfile target = createMon(
                "target", List.of("normal"), "none", "", 55,
                Map.of(Stat.HP, 100, Stat.ATK, 100, Stat.DEF, 100, Stat.SPA, 100, Stat.SPD, 100, Stat.SPE, 100),
                "hardy", Map.of(Stat.HP, 0, Stat.ATK, 0, Stat.DEF, 0, Stat.SPA, 0, Stat.SPD, 0, Stat.SPE, 0)
        );

        Turn1BattleState state = new Turn1BattleState(List.of(attacker, attacker), List.of(target, target));

        // Neutral single target Flamethrower
        DamageRange neutralDmg = Turn1DamageCalculator.calculateDamage(attacker, target, FLAMETHROWER, state, "player_0", "koga_0");

        // Sun boosted Flamethrower (1.5x)
        state.setWeather(Turn1BattleState.Weather.SUN);
        DamageRange sunDmg = Turn1DamageCalculator.calculateDamage(attacker, target, FLAMETHROWER, state, "player_0", "koga_0");
        double sunRatio = (double) sunDmg.maxDamage() / (double) neutralDmg.maxDamage();
        assertTrue(sunRatio >= 1.45 && sunRatio <= 1.55, "Sun should boost Fire moves by 1.5x, got ratio=" + sunRatio);

        // Rain reduced Flamethrower (0.5x)
        state.setWeather(Turn1BattleState.Weather.RAIN);
        DamageRange rainDmg = Turn1DamageCalculator.calculateDamage(attacker, target, FLAMETHROWER, state, "player_0", "koga_0");
        double rainRatio = (double) rainDmg.maxDamage() / (double) neutralDmg.maxDamage();
        assertTrue(rainRatio >= 0.45 && rainRatio <= 0.55, "Rain should reduce Fire moves by 0.5x, got ratio=" + rainRatio);

        // Spread move (Heat Wave 95 BP spread 0.75x)
        state.setWeather(Turn1BattleState.Weather.NONE);
        DamageRange heatWaveDmg = Turn1DamageCalculator.calculateDamage(attacker, target, HEAT_WAVE, state, "player_0", "koga_0");
        // Heat Wave (95 BP * 0.75 = 71.25 effective BP) vs Flamethrower (90 BP * 1.0 = 90 BP) -> ratio ~ 71.25 / 90 = 0.79
        double spreadRatio = (double) heatWaveDmg.maxDamage() / (double) neutralDmg.maxDamage();
        assertTrue(spreadRatio >= 0.75 && spreadRatio <= 0.83, "Spread modifier should scale Heat Wave down by 0.75x, got ratio=" + spreadRatio);

        // Helping Hand (1.5x)
        state.setHelpingHandBoosted("player_0");
        DamageRange hhDmg = Turn1DamageCalculator.calculateDamage(attacker, target, FLAMETHROWER, state, "player_0", "koga_0");
        double hhRatio = (double) hhDmg.maxDamage() / (double) neutralDmg.maxDamage();
        assertTrue(hhRatio >= 1.45 && hhRatio <= 1.55, "Helping Hand should boost damage by 1.5x, got ratio=" + hhRatio);
    }

    @Test
    @DisplayName("Expanding Force in Psychic Terrain: 80 -> 120 BP, spread 0.75x, terrain 1.3x")
    void testExpandingForceInPsychicTerrain() {
        CompetitivePokemonProfile indeedee = createMon(
                "indeedee", List.of("psychic", "normal"), "psychicsurge", "", 55,
                Map.of(Stat.HP, 60, Stat.ATK, 65, Stat.DEF, 55, Stat.SPA, 105, Stat.SPD, 95, Stat.SPE, 95),
                "modest", Map.of(Stat.HP, 0, Stat.ATK, 0, Stat.DEF, 0, Stat.SPA, 252, Stat.SPD, 4, Stat.SPE, 252)
        );

        CompetitivePokemonProfile target = createMon(
                "target", List.of("normal"), "none", "", 55,
                Map.of(Stat.HP, 100, Stat.ATK, 100, Stat.DEF, 100, Stat.SPA, 100, Stat.SPD, 100, Stat.SPE, 100),
                "hardy", Map.of(Stat.HP, 0, Stat.ATK, 0, Stat.DEF, 0, Stat.SPA, 0, Stat.SPD, 0, Stat.SPE, 0)
        );

        Turn1BattleState state = new Turn1BattleState(List.of(indeedee, indeedee), List.of(target, target));

        // Expanding Force without terrain: 80 BP, single target (1.0x spread, 1.0x terrain)
        state.setTerrain(Turn1BattleState.Terrain.NONE);
        DamageRange noTerrainDmg = Turn1DamageCalculator.calculateDamage(indeedee, target, EXPANDING_FORCE, state, "player_0", "koga_0");

        // Expanding Force with Psychic Terrain:
        // Base BP: 80 * 1.5 = 120 BP (1.5x)
        // Spread modifier: 0.75x
        // Terrain modifier: 1.3x
        // Total expected scaling: 1.5 * 0.75 * 1.3 = 1.4625x
        state.setTerrain(Turn1BattleState.Terrain.PSYCHIC);
        DamageRange terrainDmg = Turn1DamageCalculator.calculateDamage(indeedee, target, EXPANDING_FORCE, state, "player_0", "koga_0");

        double ratio = (double) terrainDmg.maxDamage() / (double) noTerrainDmg.maxDamage();
        assertTrue(ratio >= 1.40 && ratio <= 1.52, "Expanding Force under Psychic Terrain should scale by ~1.4625x, got ratio=" + ratio);
    }

    @Test
    @DisplayName("Steel type effectiveness against Poison is strictly 1.0x neutral (Make It Rain)")
    void testMakeItRainTypeEffectivenessOnPoison() {
        CompetitivePokemonProfile slowking = createMon("slowking", List.of("poison", "psychic"), "drought", "", 55,
                Map.of(Stat.HP, 95, Stat.ATK, 65, Stat.DEF, 80, Stat.SPA, 110, Stat.SPD, 110, Stat.SPE, 30), "relaxed", Map.of());
        CompetitivePokemonProfile venusaur = createMon("venusaur", List.of("grass", "poison"), "chlorophyll", "", 55,
                Map.of(Stat.HP, 80, Stat.ATK, 82, Stat.DEF, 83, Stat.SPA, 100, Stat.SPD, 100, Stat.SPE, 80), "modest", Map.of());
        CompetitivePokemonProfile beedrill = createMon("beedrill", List.of("bug", "poison"), "adaptability", "", 55,
                Map.of(Stat.HP, 65, Stat.ATK, 150, Stat.DEF, 40, Stat.SPA, 15, Stat.SPD, 80, Stat.SPE, 145), "jolly", Map.of());
        CompetitivePokemonProfile okidogi = createMon("okidogi", List.of("poison", "fighting"), "guarddog", "", 55,
                Map.of(Stat.HP, 88, Stat.ATK, 128, Stat.DEF, 115, Stat.SPA, 58, Stat.SPD, 86, Stat.SPE, 80), "adamant", Map.of());
        CompetitivePokemonProfile sneasler = createMon("sneasler", List.of("fighting", "poison"), "unburden", "", 55,
                Map.of(Stat.HP, 80, Stat.ATK, 130, Stat.DEF, 60, Stat.SPA, 40, Stat.SPD, 80, Stat.SPE, 120), "adamant", Map.of());
        CompetitivePokemonProfile amoonguss = createMon("amoonguss", List.of("grass", "poison"), "regenerator", "", 55,
                Map.of(Stat.HP, 114, Stat.ATK, 85, Stat.DEF, 70, Stat.SPA, 85, Stat.SPD, 80, Stat.SPE, 30), "bold", Map.of());

        // Steel is neutral (1.0x) against pure Poison and ALL dual-types across Koga's 6 team members
        assertEquals(1.0, Turn1DamageCalculator.calculateTypeEffectiveness("steel", slowking), "Steel vs Poison/Psychic is 1.0x neutral");
        assertEquals(1.0, Turn1DamageCalculator.calculateTypeEffectiveness("steel", venusaur), "Steel vs Grass/Poison is 1.0x neutral");
        assertEquals(1.0, Turn1DamageCalculator.calculateTypeEffectiveness("steel", beedrill), "Steel vs Bug/Poison is 1.0x neutral");
        assertEquals(1.0, Turn1DamageCalculator.calculateTypeEffectiveness("steel", okidogi), "Steel vs Poison/Fighting is 1.0x neutral");
        assertEquals(1.0, Turn1DamageCalculator.calculateTypeEffectiveness("steel", sneasler), "Steel vs Fighting/Poison is 1.0x neutral");
        assertEquals(1.0, Turn1DamageCalculator.calculateTypeEffectiveness("steel", amoonguss), "Steel vs Grass/Poison is 1.0x neutral");
    }

    @Test
    @DisplayName("Sun Venusaur move damage against Pelipper: Sludge Bomb > Giga Drain > Weather Ball")
    void testVenusaurMoveDamageAgainstPelipper() {
        CompetitivePokemonProfile venusaur = createMon("venusaur", List.of("grass", "poison"), "chlorophyll", "leftovers", 55,
                Map.of(Stat.HP, 80, Stat.ATK, 82, Stat.DEF, 83, Stat.SPA, 100, Stat.SPD, 100, Stat.SPE, 80),
                "modest", Map.of(Stat.HP, 0, Stat.ATK, 0, Stat.DEF, 0, Stat.SPA, 252, Stat.SPD, 4, Stat.SPE, 252));

        CompetitivePokemonProfile pelipper = createMon("pelipper", List.of("water", "flying"), "drizzle", "focus_sash", 55,
                Map.of(Stat.HP, 60, Stat.ATK, 50, Stat.DEF, 100, Stat.SPA, 95, Stat.SPD, 70, Stat.SPE, 65),
                "modest", Map.of(Stat.HP, 252, Stat.ATK, 0, Stat.DEF, 0, Stat.SPA, 252, Stat.SPD, 4, Stat.SPE, 0));

        Turn1BattleState state = new Turn1BattleState(List.of(pelipper, pelipper), List.of(venusaur, venusaur));
        state.setWeather(Turn1BattleState.Weather.SUN); // Sun active

        MoveProfile sludgeBomb = new MoveProfile("sludgebomb", "poison", MoveProfile.Category.SPECIAL, 90, 0, MoveProfile.Target.SINGLE_OPPONENT, false, false, 100);
        MoveProfile gigaDrain = new MoveProfile("gigadrain", "grass", MoveProfile.Category.SPECIAL, 75, 0, MoveProfile.Target.SINGLE_OPPONENT, false, false, 100);
        MoveProfile weatherBall = new MoveProfile("weatherball", "normal", MoveProfile.Category.SPECIAL, 50, 0, MoveProfile.Target.SINGLE_OPPONENT, false, false, 100);

        DamageRange sludgeDmg = Turn1DamageCalculator.calculateDamage(venusaur, pelipper, sludgeBomb, state, "koga_0", "player_0");
        DamageRange gigaDmg = Turn1DamageCalculator.calculateDamage(venusaur, pelipper, gigaDrain, state, "koga_0", "player_0");
        DamageRange wbDmg = Turn1DamageCalculator.calculateDamage(venusaur, pelipper, weatherBall, state, "koga_0", "player_0");

        // Mathematical verification:
        // Sludge Bomb: 90 BP * 1.5 STAB * 1.0 type = 135 effective BP
        // Giga Drain: 75 BP * 1.5 STAB * 1.0 (2x Water * 0.5x Flying) = 112.5 effective BP
        // Weather Ball in Sun: 100 BP * 1.5 Sun * 0.5 (Water resistance) = 75 effective BP
        assertTrue(sludgeDmg.maxDamage() > gigaDmg.maxDamage(),
                String.format("Sludge Bomb (%d) must deal more damage than Giga Drain (%d) on Pelipper", sludgeDmg.maxDamage(), gigaDmg.maxDamage()));
        assertTrue(gigaDmg.maxDamage() > wbDmg.maxDamage(),
                String.format("Giga Drain (%d) must deal more damage than Weather Ball in Sun (%d) on Pelipper", gigaDmg.maxDamage(), wbDmg.maxDamage()));
    }
}
