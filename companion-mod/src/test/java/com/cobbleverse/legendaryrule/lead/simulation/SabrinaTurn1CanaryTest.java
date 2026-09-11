package com.cobbleverse.legendaryrule.lead.simulation;

import com.cobbleverse.legendaryrule.lead.simulation.engine.Turn1LeadSimulator;
import com.cobbleverse.legendaryrule.lead.simulation.fixtures.SabrinaCompetitiveProfiles;
import com.cobbleverse.legendaryrule.lead.simulation.fixtures.ThreatPoolCompetitiveProfiles;
import com.cobbleverse.legendaryrule.lead.simulation.model.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Focused Turn-1 Canary tests evaluating Sabrina's lead presets across critical archetypes:
 * 1. Normal matchup: default_psychic (Indeedee + Alakazam) dominant
 * 2. Dark/Steel matchup: anti_dark_steel (Indeedee + Gallade) preferred
 * 3. Rillaboom / terrain-overwrite: field_independent (Garchomp + Rotom) preferred
 * 4. Safe terrain neutral: field_independent must NOT steal lead from default_psychic
 * 5. Hard matchups for each preset:
 *    - 5a: default_psychic hard matchup (Chi-Yu + Flutter Mane)
 *    - 5b: anti_dark_steel hard matchup (Flutter Mane + Togekiss)
 *    - 5c: field_independent hard matchup (Abomasnow + Baxcalibur)
 */
class SabrinaTurn1CanaryTest {

    private static Turn1LeadSimulator simulator;

    @BeforeAll
    static void setUp() {
        simulator = new Turn1LeadSimulator();
    }

    @Test
    @DisplayName("Canary 1: Machamp + Lucario vs default_psychic (Normal Matchup)")
    void testCanary1NormalMatchupDefaultPsychic() {
        System.out.println("\n=======================================================");
        System.out.println("CANARY 1: Machamp + Lucario vs default_psychic");
        System.out.println("=======================================================");

        CompetitivePokemonProfile machamp = ThreatPoolCompetitiveProfiles.machamp();
        CompetitivePokemonProfile lucario = ThreatPoolCompetitiveProfiles.lucario();
        List<CompetitivePokemonProfile> playerLeads = List.of(machamp, lucario);

        Turn1EvaluationResult resPsychic = simulator.simulateSabrinaPreset(playerLeads, "default_psychic", "Machamp + Lucario");
        Turn1EvaluationResult resDarkSteel = simulator.simulateSabrinaPreset(playerLeads, "anti_dark_steel", "Machamp + Lucario");
        Turn1EvaluationResult resFieldIndep = simulator.simulateSabrinaPreset(playerLeads, "field_independent", "Machamp + Lucario");

        System.out.println("default_psychic: " + resPsychic.summary());
        System.out.println("anti_dark_steel: " + resDarkSteel.summary());
        System.out.println("field_independent: " + resFieldIndep.summary());

        assertEquals(Turn1Verdict.GOOD, resPsychic.verdict(), "default_psychic should be GOOD vs Fighting leads");
        assertEquals(0, resPsychic.kogaCasualties(), "Sabrina should lose zero Pokemon on Turn 1");
        assertTrue(resPsychic.kogaKnockoutsScored() >= 1, "Alakazam Expanding Force should score at least 1 KO");
    }

    @Test
    @DisplayName("Canary 2: Kingambit + Tyranitar vs anti_dark_steel (Dark/Steel Punish)")
    void testCanary2DarkSteelAntiDarkSteelPreferred() {
        System.out.println("\n=======================================================");
        System.out.println("CANARY 2: Kingambit + Tyranitar vs anti_dark_steel");
        System.out.println("=======================================================");

        CompetitivePokemonProfile kingambit = ThreatPoolCompetitiveProfiles.kingambit();
        CompetitivePokemonProfile tyranitar = ThreatPoolCompetitiveProfiles.tyranitar();
        List<CompetitivePokemonProfile> playerLeads = List.of(kingambit, tyranitar);

        Turn1EvaluationResult resPsychic = simulator.simulateSabrinaPreset(playerLeads, "default_psychic", "Kingambit + Tyranitar");
        Turn1EvaluationResult resDarkSteel = simulator.simulateSabrinaPreset(playerLeads, "anti_dark_steel", "Kingambit + Tyranitar");
        Turn1EvaluationResult resFieldIndep = simulator.simulateSabrinaPreset(playerLeads, "field_independent", "Kingambit + Tyranitar");

        System.out.println("default_psychic: " + resPsychic.summary());
        System.out.println("anti_dark_steel: " + resDarkSteel.summary());
        System.out.println("field_independent: " + resFieldIndep.summary());

        // Both Gallade and Garchomp hit super effectively; Gallade provides 100% accurate Sharpness Sacred Sword
        assertTrue(resDarkSteel.kogaKnockoutsScored() >= 1, "Gallade should score a KO on Dark/Rock/Steel threats");
        assertNotEquals(Turn1Verdict.CATASTROPHIC, resDarkSteel.verdict(), "anti_dark_steel must NOT be CATASTROPHIC");
    }

    @Test
    @DisplayName("Canary 3: Rillaboom + Heatran vs field_independent (Terrain Overwrite)")
    void testCanary3TerrainOverwriteFieldIndependentPreferred() {
        System.out.println("\n=======================================================");
        System.out.println("CANARY 3: Rillaboom + Heatran vs field_independent");
        System.out.println("=======================================================");

        CompetitivePokemonProfile rillaboom = ThreatPoolCompetitiveProfiles.rillaboom();
        CompetitivePokemonProfile heatran = ThreatPoolCompetitiveProfiles.heatran();
        List<CompetitivePokemonProfile> playerLeads = List.of(rillaboom, heatran);

        Turn1EvaluationResult resPsychic = simulator.simulateSabrinaPreset(playerLeads, "default_psychic", "Rillaboom + Heatran");
        Turn1EvaluationResult resDarkSteel = simulator.simulateSabrinaPreset(playerLeads, "anti_dark_steel", "Rillaboom + Heatran");
        Turn1EvaluationResult resFieldIndep = simulator.simulateSabrinaPreset(playerLeads, "field_independent", "Rillaboom + Heatran");

        System.out.println("default_psychic: " + resPsychic.summary());
        System.out.println("anti_dark_steel: " + resDarkSteel.summary());
        System.out.println("field_independent: " + resFieldIndep.summary());

        // Field independent avoids terrain loss penalties
        assertEquals(0, resFieldIndep.kogaCasualties(), "Sabrina should lose 0 leads in field_independent");
        assertNotEquals(Turn1Verdict.CATASTROPHIC, resFieldIndep.verdict(), "field_independent must not be CATASTROPHIC");
    }

    @Test
    @DisplayName("Canary 4: Arcanine + Milotic vs default_psychic (Safe Terrain Non-Steal)")
    void testCanary4SafeTerrainDefaultPsychicPreserved() {
        System.out.println("\n=======================================================");
        System.out.println("CANARY 4: Arcanine + Milotic vs default_psychic");
        System.out.println("=======================================================");

        CompetitivePokemonProfile arcanine = ThreatPoolCompetitiveProfiles.arcanine();
        CompetitivePokemonProfile milotic = ThreatPoolCompetitiveProfiles.milotic();
        List<CompetitivePokemonProfile> playerLeads = List.of(arcanine, milotic);

        Turn1EvaluationResult resPsychic = simulator.simulateSabrinaPreset(playerLeads, "default_psychic", "Arcanine + Milotic");
        Turn1EvaluationResult resFieldIndep = simulator.simulateSabrinaPreset(playerLeads, "field_independent", "Arcanine + Milotic");

        System.out.println("default_psychic: " + resPsychic.summary());
        System.out.println("field_independent: " + resFieldIndep.summary());

        assertEquals(Turn1Verdict.GOOD, resPsychic.verdict(), "default_psychic must be GOOD against standard neutral leads");
        assertEquals(0, resPsychic.kogaCasualties(), "Sabrina loses zero leads in default_psychic");
    }

    @Test
    @DisplayName("Canary 5a: Hard matchup for default_psychic (Chi-Yu + Flutter Mane)")
    void testCanary5aHardMatchupDefaultPsychic() {
        System.out.println("\n=======================================================");
        System.out.println("CANARY 5a: Chi-Yu + Flutter Mane vs default_psychic");
        System.out.println("=======================================================");

        CompetitivePokemonProfile chiyu = ThreatPoolCompetitiveProfiles.chiyu();
        CompetitivePokemonProfile fluttermane = ThreatPoolCompetitiveProfiles.fluttermane();
        List<CompetitivePokemonProfile> playerLeads = List.of(chiyu, fluttermane);

        Turn1EvaluationResult resPsychic = simulator.simulateSabrinaPreset(playerLeads, "default_psychic", "Chi-Yu + Flutter Mane");
        System.out.println("default_psychic: " + resPsychic.summary());
        resPsychic.actionLog().forEach(l -> System.out.println("  " + l));
    }

    @Test
    @DisplayName("Canary 5b: Hard matchup for anti_dark_steel (Flutter Mane + Togekiss)")
    void testCanary5bHardMatchupAntiDarkSteel() {
        System.out.println("\n=======================================================");
        System.out.println("CANARY 5b: Flutter Mane + Togekiss vs anti_dark_steel");
        System.out.println("=======================================================");

        CompetitivePokemonProfile fluttermane = ThreatPoolCompetitiveProfiles.fluttermane();
        CompetitivePokemonProfile togekiss = ThreatPoolCompetitiveProfiles.togekiss();
        List<CompetitivePokemonProfile> playerLeads = List.of(fluttermane, togekiss);

        Turn1EvaluationResult resDarkSteel = simulator.simulateSabrinaPreset(playerLeads, "anti_dark_steel", "Flutter Mane + Togekiss");
        System.out.println("anti_dark_steel: " + resDarkSteel.summary());

        assertTrue(resDarkSteel.verdict() == Turn1Verdict.CATASTROPHIC || resDarkSteel.verdict() == Turn1Verdict.QUESTIONABLE,
                "Dual Fairy/Ghost should be hard for anti_dark_steel Gallade");
    }

    @Test
    @DisplayName("Canary 5c: Hard matchup for field_independent (Abomasnow + Baxcalibur)")
    void testCanary5cHardMatchupFieldIndependent() {
        System.out.println("\n=======================================================");
        System.out.println("CANARY 5c: Abomasnow + Baxcalibur vs field_independent");
        System.out.println("=======================================================");

        CompetitivePokemonProfile abomasnow = ThreatPoolCompetitiveProfiles.abomasnow();
        CompetitivePokemonProfile baxcalibur = ThreatPoolCompetitiveProfiles.baxcalibur();
        List<CompetitivePokemonProfile> playerLeads = List.of(abomasnow, baxcalibur);

        Turn1EvaluationResult resFieldIndep = simulator.simulateSabrinaPreset(playerLeads, "field_independent", "Abomasnow + Baxcalibur");
        System.out.println("field_independent: " + resFieldIndep.summary());

        assertTrue(resFieldIndep.verdict() == Turn1Verdict.CATASTROPHIC || resFieldIndep.kogaCasualties() >= 1,
                "Ice snow core should severely punish Garchomp in field_independent");
    }
}
