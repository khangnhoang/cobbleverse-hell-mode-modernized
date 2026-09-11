package com.cobbleverse.legendaryrule.lead.simulation;

import com.cobbleverse.legendaryrule.lead.simulation.calculator.Turn1StatCalculator;
import com.cobbleverse.legendaryrule.lead.simulation.model.ActualStats;
import com.cobbleverse.legendaryrule.lead.simulation.model.CompetitivePokemonProfile;
import com.cobbleverse.legendaryrule.lead.simulation.model.CompetitivePokemonProfile.Stat;
import com.cobbleverse.legendaryrule.lead.simulation.model.Turn1BattleState;
import com.cobbleverse.legendaryrule.lead.simulation.resolver.Turn1EntryResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Turn1EntryResolverTest {

    private static CompetitivePokemonProfile createMon(
            String species,
            List<String> types,
            String ability,
            int speed,
            int atk
    ) {
        ActualStats stats = new ActualStats(100, atk, 100, 100, 100, speed);
        Map<Stat, Integer> baseStats = Map.of(Stat.HP, 100, Stat.ATK, 100, Stat.DEF, 100, Stat.SPA, 100, Stat.SPD, 100, Stat.SPE, 100);
        Map<Stat, Integer> ivs = Map.of(Stat.HP, 31, Stat.ATK, 31, Stat.DEF, 31, Stat.SPA, 31, Stat.SPD, 31, Stat.SPE, 31);
        Map<Stat, Integer> evs = Map.of(Stat.HP, 0, Stat.ATK, 0, Stat.DEF, 0, Stat.SPA, 0, Stat.SPD, 0, Stat.SPE, 0);

        return new CompetitivePokemonProfile(
                species,
                List.of(),
                types,
                55,
                baseStats,
                "hardy",
                ivs,
                evs,
                ability,
                "",
                List.of(),
                stats
        );
    }

    @Test
    @DisplayName("Weather entry order: slower weather setter overrides faster setter (slower wins)")
    void testWeatherEntryOrderSlowkingVsPelipper() {
        // Pelipper (faster setter: 85 Spe, Drizzle)
        CompetitivePokemonProfile pelipper = createMon("pelipper", List.of("water", "flying"), "drizzle", 85, 100);
        CompetitivePokemonProfile neutralPlayer = createMon("neutral", List.of("normal"), "none", 100, 100);

        // Galarian Slowking (slower setter: 34 Spe, Drought)
        CompetitivePokemonProfile slowking = createMon("slowking", List.of("poison", "psychic"), "drought", 34, 100);
        CompetitivePokemonProfile neutralKoga = createMon("neutral", List.of("normal"), "none", 100, 100);

        Turn1BattleState state = new Turn1BattleState(List.of(pelipper, neutralPlayer), List.of(slowking, neutralKoga));

        Turn1EntryResolver.resolveEntry(state);

        // Pelipper (85) triggers first setting RAIN, then Slowking (34) triggers last setting SUN.
        // Slowest setter wins -> final weather is SUN!
        assertEquals(Turn1BattleState.Weather.SUN, state.getWeather(), "Slower setter (Slowking) should win weather war over Pelipper");
    }

    @Test
    @DisplayName("Psychic Surge: sets Psychic Terrain on entry")
    void testPsychicSurgeEntry() {
        CompetitivePokemonProfile indeedee = createMon("indeedee", List.of("psychic", "normal"), "psychicsurge", 100, 100);
        CompetitivePokemonProfile partner = createMon("partner", List.of("normal"), "none", 100, 100);
        CompetitivePokemonProfile koga1 = createMon("koga1", List.of("poison"), "none", 100, 100);
        CompetitivePokemonProfile koga2 = createMon("koga2", List.of("poison"), "none", 100, 100);

        Turn1BattleState state = new Turn1BattleState(List.of(indeedee, partner), List.of(koga1, koga2));

        Turn1EntryResolver.resolveEntry(state);

        assertEquals(Turn1BattleState.Terrain.PSYCHIC, state.getTerrain());
    }

    @Test
    @DisplayName("Intimidate: lowers normal target by -1, Guard Dog gains +1, Good as Gold receives -1")
    void testIntimidateInteractions() {
        CompetitivePokemonProfile lando = createMon("landorus", List.of("ground", "flying"), "intimidate", 120, 150);
        CompetitivePokemonProfile partner = createMon("partner", List.of("normal"), "none", 100, 100);

        // Koga side: Okidogi (Guard Dog) and Gholdengo (Good as Gold) or standard mon
        CompetitivePokemonProfile okidogi = createMon("okidogi", List.of("poison", "fighting"), "guarddog", 100, 120);
        CompetitivePokemonProfile gholdengo = createMon("gholdengo", List.of("steel", "ghost"), "goodasgold", 100, 100);

        Turn1BattleState state = new Turn1BattleState(List.of(lando, partner), List.of(okidogi, gholdengo));

        Turn1EntryResolver.resolveEntry(state);

        // Okidogi has Guard Dog: blocks Intimidate and gains +1 Attack stage
        assertEquals(1, state.getStatStage("koga_0", "atk"), "Okidogi Guard Dog should gain +1 Atk on Intimidate");

        // Gholdengo has Good as Gold: Good as Gold only blocks status moves, NOT Intimidate. So Atk dropped by -1!
        assertEquals(-1, state.getStatStage("koga_1", "atk"), "Gholdengo Good as Gold does NOT block Intimidate");
    }
}
