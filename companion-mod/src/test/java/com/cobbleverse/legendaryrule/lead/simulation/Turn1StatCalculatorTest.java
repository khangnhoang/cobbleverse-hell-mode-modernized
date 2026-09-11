package com.cobbleverse.legendaryrule.lead.simulation;

import com.cobbleverse.legendaryrule.lead.simulation.calculator.Turn1StatCalculator;
import com.cobbleverse.legendaryrule.lead.simulation.model.ActualStats;
import com.cobbleverse.legendaryrule.lead.simulation.model.CompetitivePokemonProfile.Stat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Turn1StatCalculatorTest {

    @Test
    @DisplayName("Assert canonical Level-55 stats for Koga's 6 team members")
    void testKogaTeamStatsLevel55() {
        // 1. Mega Beedrill (Level 55, Jolly, HP 4 / Atk 252 / Spe 252, IVs 31)
        ActualStats beedrill = Turn1StatCalculator.calculateStats(
                Map.of(Stat.HP, 65, Stat.ATK, 150, Stat.DEF, 40, Stat.SPA, 15, Stat.SPD, 80, Stat.SPE, 145),
                Map.of(Stat.HP, 31, Stat.ATK, 31, Stat.DEF, 31, Stat.SPA, 31, Stat.SPD, 31, Stat.SPE, 31),
                Map.of(Stat.HP, 4, Stat.ATK, 252, Stat.DEF, 0, Stat.SPA, 0, Stat.SPD, 0, Stat.SPE, 252),
                "jolly",
                55
        );
        assertEquals(154, beedrill.hp());
        assertEquals(221, beedrill.atk());
        assertEquals(66, beedrill.def());
        assertEquals(34, beedrill.spa());
        assertEquals(110, beedrill.spd());
        assertEquals(237, beedrill.spe());

        // 2. Venusaur (Level 55, Modest, SpA 252 / SpD 4 / Spe 252, IVs 31)
        ActualStats venusaur = Turn1StatCalculator.calculateStats(
                Map.of(Stat.HP, 80, Stat.ATK, 82, Stat.DEF, 83, Stat.SPA, 100, Stat.SPD, 100, Stat.SPE, 80),
                Map.of(Stat.HP, 31, Stat.ATK, 31, Stat.DEF, 31, Stat.SPA, 31, Stat.SPD, 31, Stat.SPE, 31),
                Map.of(Stat.HP, 0, Stat.ATK, 0, Stat.DEF, 0, Stat.SPA, 252, Stat.SPD, 4, Stat.SPE, 252),
                "modest",
                55
        );
        assertEquals(170, venusaur.hp());
        assertEquals(100, venusaur.atk());
        assertEquals(113, venusaur.def());
        assertEquals(182, venusaur.spa());
        assertEquals(132, venusaur.spd());
        assertEquals(144, venusaur.spe());

        // 3. Okidogi (Level 55, Adamant, HP 220 / Atk 108 / Def 4 / SpD 60 / Spe 116, IVs 31)
        ActualStats okidogi = Turn1StatCalculator.calculateStats(
                Map.of(Stat.HP, 88, Stat.ATK, 128, Stat.DEF, 115, Stat.SPA, 58, Stat.SPD, 86, Stat.SPE, 80),
                Map.of(Stat.HP, 31, Stat.ATK, 31, Stat.DEF, 31, Stat.SPA, 31, Stat.SPD, 31, Stat.SPE, 31),
                Map.of(Stat.HP, 220, Stat.ATK, 108, Stat.DEF, 4, Stat.SPA, 0, Stat.SPD, 60, Stat.SPE, 116),
                "adamant",
                55
        );
        assertEquals(209, okidogi.hp());
        assertEquals(194, okidogi.atk());
        assertEquals(149, okidogi.def());
        assertEquals(76, okidogi.spa());
        assertEquals(124, okidogi.spd());
        assertEquals(126, okidogi.spe());

        // 4. Sneasler (Level 55, Adamant, Atk 252 / SpD 4 / Spe 252, IVs 31)
        ActualStats sneasler = Turn1StatCalculator.calculateStats(
                Map.of(Stat.HP, 80, Stat.ATK, 130, Stat.DEF, 60, Stat.SPA, 40, Stat.SPD, 80, Stat.SPE, 120),
                Map.of(Stat.HP, 31, Stat.ATK, 31, Stat.DEF, 31, Stat.SPA, 31, Stat.SPD, 31, Stat.SPE, 31),
                Map.of(Stat.HP, 0, Stat.ATK, 252, Stat.DEF, 0, Stat.SPA, 0, Stat.SPD, 4, Stat.SPE, 252),
                "adamant",
                55
        );
        assertEquals(170, sneasler.hp());
        assertEquals(218, sneasler.atk());
        assertEquals(88, sneasler.def());
        assertEquals(59, sneasler.spa());
        assertEquals(110, sneasler.spd());
        assertEquals(188, sneasler.spe());

        // 5. Amoonguss (Level 55, Bold, HP 236 / Def 236 / SpD 36, IVs 31)
        ActualStats amoonguss = Turn1StatCalculator.calculateStats(
                Map.of(Stat.HP, 114, Stat.ATK, 85, Stat.DEF, 70, Stat.SPA, 85, Stat.SPD, 80, Stat.SPE, 30),
                Map.of(Stat.HP, 31, Stat.ATK, 31, Stat.DEF, 31, Stat.SPA, 31, Stat.SPD, 31, Stat.SPE, 31),
                Map.of(Stat.HP, 236, Stat.ATK, 0, Stat.DEF, 236, Stat.SPA, 0, Stat.SPD, 36, Stat.SPE, 0),
                "bold",
                55
        );
        assertEquals(239, amoonguss.hp());
        assertEquals(103, amoonguss.atk());
        assertEquals(144, amoonguss.def());
        assertEquals(115, amoonguss.spa());
        assertEquals(115, amoonguss.spd());
        assertEquals(55, amoonguss.spe());

        // 6. Galarian Slowking (Level 55, Relaxed, HP 252 / Def 132 / SpD 124, Spe IV 0, other IVs 31)
        ActualStats slowking = Turn1StatCalculator.calculateStats(
                Map.of(Stat.HP, 95, Stat.ATK, 65, Stat.DEF, 80, Stat.SPA, 110, Stat.SPD, 110, Stat.SPE, 30),
                Map.of(Stat.HP, 31, Stat.ATK, 31, Stat.DEF, 31, Stat.SPA, 31, Stat.SPD, 31, Stat.SPE, 0),
                Map.of(Stat.HP, 252, Stat.ATK, 0, Stat.DEF, 132, Stat.SPA, 0, Stat.SPD, 124, Stat.SPE, 0),
                "relaxed",
                55
        );
        assertEquals(221, slowking.hp());
        assertEquals(93, slowking.atk());
        assertEquals(140, slowking.def());
        assertEquals(143, slowking.spa());
        assertEquals(160, slowking.spd());
        assertEquals(34, slowking.spe());
    }

    @Test
    @DisplayName("Verify nature multipliers for all categories")
    void testNatureMultipliers() {
        assertEquals(1.1, Turn1StatCalculator.getNatureMultiplier("adamant", Stat.ATK));
        assertEquals(0.9, Turn1StatCalculator.getNatureMultiplier("adamant", Stat.SPA));
        assertEquals(1.0, Turn1StatCalculator.getNatureMultiplier("adamant", Stat.SPE));

        assertEquals(1.1, Turn1StatCalculator.getNatureMultiplier("jolly", Stat.SPE));
        assertEquals(0.9, Turn1StatCalculator.getNatureMultiplier("jolly", Stat.SPA));

        assertEquals(1.1, Turn1StatCalculator.getNatureMultiplier("bold", Stat.DEF));
        assertEquals(0.9, Turn1StatCalculator.getNatureMultiplier("bold", Stat.ATK));

        assertEquals(1.0, Turn1StatCalculator.getNatureMultiplier("hardy", Stat.ATK));
    }
}
