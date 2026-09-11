package com.cobbleverse.legendaryrule.lead.simulation;

import com.cobbleverse.legendaryrule.lead.simulation.calculator.Turn1DamageCalculator;
import com.cobbleverse.legendaryrule.lead.simulation.resolver.Turn1ActionResolver;
import com.cobbleverse.legendaryrule.lead.simulation.resolver.Turn1EntryResolver;
import com.cobbleverse.legendaryrule.lead.simulation.resolver.Turn1SpeedResolver;
import com.cobbleverse.legendaryrule.lead.simulation.engine.Turn1LeadSimulator;
import com.cobbleverse.legendaryrule.lead.simulation.fixtures.KogaCompetitiveProfiles;
import com.cobbleverse.legendaryrule.lead.simulation.fixtures.ThreatPoolCompetitiveProfiles;
import com.cobbleverse.legendaryrule.lead.simulation.model.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Six Canary tests evaluating Turn-1 lead presets across critical archetypes as neutral empirical questions.
 */
class KogaTurn1CanaryTest {

    private static Turn1LeadSimulator simulator;

    @BeforeAll
    static void setUp() {
        simulator = new Turn1LeadSimulator();
    }

    @Test
    @DisplayName("Canary 1: Kingambit + Indeedee-F vs Okidogi + Sneasler (anti_dark_steel)")
    void testCanary1KingambitIndeedeeFVsAntiDarkSteel() {
        System.out.println("\n=======================================================");
        System.out.println("CANARY 1: Kingambit + Indeedee-F vs anti_dark_steel");
        System.out.println("=======================================================");

        CompetitivePokemonProfile indeedeeF = ThreatPoolCompetitiveProfiles.indeedeeF();
        CompetitivePokemonProfile kingambit = ThreatPoolCompetitiveProfiles.kingambit();
        List<CompetitivePokemonProfile> playerLeads = List.of(indeedeeF, kingambit);

        CompetitivePokemonProfile okidogi = KogaCompetitiveProfiles.okidogi();
        CompetitivePokemonProfile sneasler = KogaCompetitiveProfiles.sneasler();
        List<CompetitivePokemonProfile> kogaLeads = List.of(okidogi, sneasler);

        // Line (a): Follow Me + Kingambit attack
        Turn1BattleState stateA = new Turn1BattleState(playerLeads, kogaLeads);
        int speedIndeedee = Turn1SpeedResolver.calculateEffectiveSpeed(indeedeeF, stateA, "player_0");
        int speedKingambit = Turn1SpeedResolver.calculateEffectiveSpeed(kingambit, stateA, "player_1");
        int speedOkidogi = Turn1SpeedResolver.calculateEffectiveSpeed(okidogi, stateA, "koga_0");
        int speedSneasler = Turn1SpeedResolver.calculateEffectiveSpeed(sneasler, stateA, "koga_1");

        Turn1Action followMe = new Turn1Action("player_0", indeedeeF.getMove("followme"), "player_0", 2, speedIndeedee);
        Turn1Action kingambitAtk = new Turn1Action("player_1", kingambit.getMove("kowtowcleave"), "koga_1", 0, speedKingambit);
        Turn1Action sneaslerAtk = new Turn1Action("koga_1", sneasler.getMove("closecombat"), "player_1", 0, speedSneasler);
        Turn1Action okidogiAtk = new Turn1Action("koga_0", okidogi.getMove("drainpunch"), "player_1", 0, speedOkidogi);

        Turn1EvaluationResult resultLineA = simulator.simulateSpecificLine(
                playerLeads, kogaLeads,
                List.of(followMe, kingambitAtk),
                List.of(sneaslerAtk, okidogiAtk),
                "Kingambit + Indeedee-F (Follow Me Line)", "anti_dark_steel"
        );

        System.out.println("Line (a) [Follow Me] Log:");
        resultLineA.actionLog().forEach(l -> System.out.println("  " + l));
        System.out.println("Line (a) Outcome: " + resultLineA.summary());

        // Line (b): Helping Hand + Kingambit attack
        Turn1Action helpingHand = new Turn1Action("player_0", indeedeeF.getMove("helpinghand"), "ally", 5, speedIndeedee);
        Turn1EvaluationResult resultLineB = simulator.simulateSpecificLine(
                playerLeads, kogaLeads,
                List.of(helpingHand, kingambitAtk),
                List.of(sneaslerAtk, okidogiAtk),
                "Kingambit + Indeedee-F (Helping Hand Line)", "anti_dark_steel"
        );

        System.out.println("Line (b) [Helping Hand] Log:");
        resultLineB.actionLog().forEach(l -> System.out.println("  " + l));
        System.out.println("Line (b) Outcome: " + resultLineB.summary());

        // Full adversarial evaluation
        Turn1EvaluationResult fullSimResult = simulator.simulatePreset(playerLeads, "anti_dark_steel", "Kingambit + Indeedee-F");
        System.out.println("Adversarial Worst-Case Simulation: " + fullSimResult.summary());

        // Invariant assertions:
        // 1. Follow Me redirects Sneasler and Okidogi away from Kingambit: Kingambit survives at 100% HP
        assertEquals(kingambit.actualStats().hp(), resultLineA.finalState().getHp("player_1"),
                "Line (a): Kingambit must be completely unharmed at 100% HP due to Follow Me redirection");
        assertTrue(resultLineA.finalState().getHp("player_0") < indeedeeF.actualStats().hp(),
                "Line (a): Indeedee-F must absorb the redirected Fighting attacks");
        assertEquals(0, resultLineA.kogaCasualties(),
                "Line (a): Koga suffered 0 casualties on Turn 1");

        // 2. Line (b) without redirection: Sneasler 4x Close Combat hits Kingambit
        assertTrue(resultLineB.finalState().isFainted("player_1") || resultLineB.finalState().getHp("player_1") < 50,
                "Line (b): Without redirection, Kingambit takes lethal 4x Fighting damage");

        // 3. Adversarial simulation must choose optimal player line (Follow Me) keeping Kingambit alive
        assertEquals(kingambit.actualStats().hp(), fullSimResult.finalState().getHp("player_1"),
                "Adversarial worst case for Koga: Kingambit survives at 100% HP under Psychic Terrain");
        assertEquals(Turn1Verdict.QUESTIONABLE, fullSimResult.verdict(),
                "Verdict must be QUESTIONABLE: Even with Indeedee-F fainted, Kingambit is at full HP threatening next turn");
    }

    @Test
    @DisplayName("Canary 2: Kingambit + Indeedee-M vs Okidogi + Sneasler (anti_dark_steel)")
    void testCanary2KingambitIndeedeeMVsAntiDarkSteel() {
        System.out.println("\n=======================================================");
        System.out.println("CANARY 2: Kingambit + Indeedee-M vs anti_dark_steel");
        System.out.println("=======================================================");

        CompetitivePokemonProfile indeedeeM = ThreatPoolCompetitiveProfiles.indeedeeM();
        CompetitivePokemonProfile kingambit = ThreatPoolCompetitiveProfiles.kingambit();
        List<CompetitivePokemonProfile> playerLeads = List.of(indeedeeM, kingambit);

        Turn1EvaluationResult result = simulator.simulatePreset(playerLeads, "anti_dark_steel", "Kingambit + Indeedee-M");
        System.out.println("Simulation Outcome: " + result.summary());
        result.actionLog().forEach(l -> System.out.println("  " + l));

        // Invariant assertions:
        // Indeedee-M lacks Follow Me redirection. Sneasler (188 Spe) outspeeds Kingambit (50 Spe).
        // Sneasler's 4x super-effective Close Combat must KO Kingambit.
        assertTrue(result.finalState().isFainted("player_1"),
                "Kingambit must faint from 4x super-effective Close Combat without redirection");
        assertTrue(result.kogaKnockoutsScored() >= 1,
                "Koga must score at least 1 KO on Kingambit on Turn 1");
        assertEquals(0, result.kogaCasualties(),
                "Koga must suffer 0 casualties on Turn 1");
        assertEquals(Turn1Verdict.GOOD, result.verdict(),
                "anti_dark_steel preset must achieve GOOD verdict against Kingambit + Indeedee-M by eliminating Kingambit");
    }

    @Test
    @DisplayName("Canary 3: Heatran + Cresselia candidate preset comparison")
    void testCanary3HeatranCresseliaComparison() {
        System.out.println("\n=======================================================");
        System.out.println("CANARY 3: Heatran + Cresselia Lead Comparison");
        System.out.println("=======================================================");

        CompetitivePokemonProfile heatran = ThreatPoolCompetitiveProfiles.heatran();
        CompetitivePokemonProfile cresselia = ThreatPoolCompetitiveProfiles.cresselia();
        List<CompetitivePokemonProfile> playerLeads = List.of(heatran, cresselia);

        Turn1EvaluationResult rSun = simulator.simulatePreset(playerLeads, "default_sun", "Heatran + Cresselia");
        Turn1EvaluationResult rPsychic = simulator.simulatePreset(playerLeads, "anti_psychic", "Heatran + Cresselia");
        Turn1EvaluationResult rDarkSteel = simulator.simulatePreset(playerLeads, "anti_dark_steel", "Heatran + Cresselia");

        System.out.println("Preset default_sun:       " + rSun.summary());
        System.out.println("Preset anti_psychic:      " + rPsychic.summary());
        System.out.println("Preset anti_dark_steel:   " + rDarkSteel.summary());

        // Invariant assertions:
        // 1. Trick Room line: Cresselia sets Trick Room against default_sun because Koga cannot KO it Turn 1
        Turn1BattleState testStateTR = new Turn1BattleState(playerLeads, KogaCompetitiveProfiles.getPreset("default_sun"));
        Turn1EntryResolver.resolveEntry(testStateTR);
        int speedCress = Turn1SpeedResolver.calculateEffectiveSpeed(cresselia, testStateTR, "player_1");
        int speedHeatran = Turn1SpeedResolver.calculateEffectiveSpeed(heatran, testStateTR, "player_0");
        Turn1Action trAction = new Turn1Action("player_1", cresselia.getMove("trickroom"), "player_1", -7, speedCress);
        Turn1Action heatAction = new Turn1Action("player_0", heatran.getMove("heatwave"), "all_opponents", 0, speedHeatran);

        Turn1EvaluationResult resultTR = simulator.simulateSpecificLine(
                playerLeads, KogaCompetitiveProfiles.getPreset("default_sun"),
                List.of(heatAction, trAction),
                simulator.determineKogaActions(testStateTR),
                "Heatran + Cresselia (Trick Room Line)", "default_sun"
        );
        assertTrue(resultTR.finalState().isTrickRoom(), "Cresselia must successfully set Trick Room against default_sun");
        assertEquals(Turn1Verdict.CATASTROPHIC, resultTR.verdict(),
                "default_sun must be CATASTROPHIC against Heatran + Cresselia due to uninterrupted Trick Room");

        // General adversarial simulation also confirms default_sun is CATASTROPHIC
        assertEquals(Turn1Verdict.CATASTROPHIC, rSun.verdict(),
                "default_sun must be CATASTROPHIC against Heatran + Cresselia in adversarial evaluation");

        // 2. Heatran's Flash Fire absorbs Fire moves (Flamethrower from Slowking deals 0)
        Turn1BattleState testState = new Turn1BattleState(playerLeads, KogaCompetitiveProfiles.getPreset("default_sun"));
        Turn1DamageCalculator.DamageRange fireOnHeatran = Turn1DamageCalculator.calculateDamage(
                KogaCompetitiveProfiles.slowking(), heatran,
                KogaCompetitiveProfiles.slowking().getMove("flamethrower"), testState, "koga_0", "player_0"
        );
        assertEquals(0, fireOnHeatran.maxDamage(), "Heatran's Flash Fire must negate Fire damage completely");

        // 3. Cresselia's Levitate grants Ground immunity (Drill Run from Beedrill deals 0)
        Turn1DamageCalculator.DamageRange groundOnCress = Turn1DamageCalculator.calculateDamage(
                KogaCompetitiveProfiles.beedrill(), cresselia,
                KogaCompetitiveProfiles.beedrill().getMove("drillrun"), testState, "koga_0", "player_1"
        );
        assertEquals(0, groundOnCress.maxDamage(), "Cresselia's Levitate must negate Ground damage completely");
    }

    @Test
    @DisplayName("Canary 4: Pelipper + Archaludon vs Slowking + Venusaur (default_sun)")
    void testCanary4PelipperArchaludonVsDefaultSun() {
        System.out.println("\n=======================================================");
        System.out.println("CANARY 4: Pelipper + Archaludon vs default_sun");
        System.out.println("=======================================================");

        CompetitivePokemonProfile pelipper = ThreatPoolCompetitiveProfiles.pelipper();
        CompetitivePokemonProfile archaludon = ThreatPoolCompetitiveProfiles.archaludon();
        List<CompetitivePokemonProfile> playerLeads = List.of(pelipper, archaludon);

        CompetitivePokemonProfile slowking = KogaCompetitiveProfiles.slowking();
        CompetitivePokemonProfile venusaur = KogaCompetitiveProfiles.venusaur();
        List<CompetitivePokemonProfile> kogaLeads = List.of(slowking, venusaur);

        Turn1BattleState state = new Turn1BattleState(playerLeads, kogaLeads);
        Turn1EntryResolver.resolveEntry(state);

        // Verify slower setter wins: Slowking (34 Spe) vs Pelipper (65+ Spe) -> SUN
        assertEquals(Turn1BattleState.Weather.SUN, state.getWeather(), "Slower Drought Galarian Slowking must win weather war over Pelipper");

        // Verify Venusaur effective Speed under Sun (Chlorophyll doubles 144 to 288)
        int venusaurSpeed = Turn1SpeedResolver.calculateEffectiveSpeed(venusaur, state, "koga_1");
        assertEquals(288, venusaurSpeed, "Chlorophyll Venusaur speed should double under Sun to 288");

        Turn1EvaluationResult result = simulator.simulate(playerLeads, kogaLeads, "Pelipper + Archaludon", "default_sun");
        System.out.println("Simulation Outcome: " + result.summary());
        result.actionLog().forEach(l -> System.out.println("  " + l));

        // Invariant assertions:
        // 1. Venusaur (288 Spe) outspeeds both Pelipper and Archaludon under Sun
        assertTrue(venusaurSpeed > pelipper.actualStats().spe(), "Venusaur in Sun must outspeed Pelipper");
        assertTrue(venusaurSpeed > archaludon.actualStats().spe(), "Venusaur in Sun must outspeed Archaludon");

        // 2. Focused attacks on Pelipper break Focus Sash and KO Pelipper
        assertTrue(result.finalState().isFainted("player_0"), "Pelipper must faint from focused attacks on Turn 1");
        assertEquals(1, result.kogaKnockoutsScored(), "Koga must score 1 KO on Pelipper");
        assertEquals(0, result.kogaCasualties(), "Koga must suffer 0 casualties");

        // 3. Hurricane accuracy under Sun drops to 50% and assumption is logged
        assertTrue(result.actionLog().stream().anyMatch(l -> l.contains("hurricane") && l.contains("50%")),
                "Hurricane under Sun must log assumption of 50% accuracy");

        // 4. Koga candidate evaluation is logged and visible
        assertTrue(result.actionLog().stream().anyMatch(l -> l.contains("KOGA AI: venusaur evaluated candidates")),
                "Koga candidate evaluations must be exposed in action log");

        // 5. Outcome verdict is GOOD for Koga
        assertEquals(Turn1Verdict.GOOD, result.verdict(),
                "default_sun must achieve GOOD verdict against Pelipper + Archaludon by winning weather and eliminating Pelipper");
    }

    @Test
    @DisplayName("Canary 5: Landorus-T + Gholdengo candidate lead evaluation")
    void testCanary5LandorusTGholdengo() {
        System.out.println("\n=======================================================");
        System.out.println("CANARY 5: Landorus-T + Gholdengo Lead Comparison");
        System.out.println("=======================================================");

        CompetitivePokemonProfile lando = ThreatPoolCompetitiveProfiles.landorusT();
        CompetitivePokemonProfile gholdengo = ThreatPoolCompetitiveProfiles.gholdengo();
        List<CompetitivePokemonProfile> playerLeads = List.of(lando, gholdengo);

        Turn1EvaluationResult rSun = simulator.simulatePreset(playerLeads, "default_sun", "Landorus-T + Gholdengo");
        Turn1EvaluationResult rPsychic = simulator.simulatePreset(playerLeads, "anti_psychic", "Landorus-T + Gholdengo");
        Turn1EvaluationResult rDarkSteel = simulator.simulatePreset(playerLeads, "anti_dark_steel", "Landorus-T + Gholdengo");

        System.out.println("Preset default_sun:       " + rSun.summary());
        System.out.println("Preset anti_psychic:      " + rPsychic.summary());
        System.out.println("Preset anti_dark_steel:   " + rDarkSteel.summary());

        // Invariant assertions:
        // 1. Intimidate activates on entry: lowers physical attackers' Atk stage
        // In anti_dark_steel, Okidogi has Guard Dog (boosts Atk by +1), Sneasler drops -1 Atk
        Turn1BattleState dsState = new Turn1BattleState(playerLeads, KogaCompetitiveProfiles.getPreset("anti_dark_steel"));
        Turn1EntryResolver.resolveEntry(dsState);
        assertEquals(1, dsState.getStatStage("koga_0", "atk"), "Okidogi Guard Dog must convert Intimidate to +1 Atk stage");
        assertEquals(-1, dsState.getStatStage("koga_1", "atk"), "Sneasler must receive -1 Atk stage from Intimidate");

        // 2. Make It Rain (Steel) is 1.0x neutral against Poison types across all presets
        for (CompetitivePokemonProfile kogaMon : KogaCompetitiveProfiles.getPreset("default_sun")) {
            assertEquals(1.0, Turn1DamageCalculator.calculateTypeEffectiveness("steel", kogaMon),
                    "Make It Rain must be 1.0x neutral against " + kogaMon.species());
        }

        // 3. Grounded Gholdengo is 2x weak to Ground moves; Landorus-T uses single-target Stomping Tantrum
        // to avoid friendly-fire damage on ally Gholdengo.
        assertEquals(2.0, Turn1DamageCalculator.calculateTypeEffectiveness("ground", gholdengo),
                "Grounded Gholdengo is 2x weak to Ground moves");

        // 4. Stomping Tantrum is SINGLE_OPPONENT (safe for ally Gholdengo in Doubles)
        MoveProfile stomping = lando.getMove("stompingtantrum");
        assertNotNull(stomping);
        assertEquals(MoveProfile.Target.SINGLE_OPPONENT, stomping.target(), "Stomping Tantrum must be SINGLE_OPPONENT");
    }

    @Test
    @DisplayName("Canary 6: Talonflame + Charizard offensive lead evaluation")
    void testCanary6TalonflameCharizard() {
        System.out.println("\n=======================================================");
        System.out.println("CANARY 6: Talonflame + Charizard Lead Comparison");
        System.out.println("=======================================================");

        CompetitivePokemonProfile talon = ThreatPoolCompetitiveProfiles.talonflame();
        CompetitivePokemonProfile zard = ThreatPoolCompetitiveProfiles.charizard();
        List<CompetitivePokemonProfile> playerLeads = List.of(talon, zard);

        Turn1EvaluationResult rSun = simulator.simulatePreset(playerLeads, "default_sun", "Talonflame + Charizard");
        Turn1EvaluationResult rPsychic = simulator.simulatePreset(playerLeads, "anti_psychic", "Talonflame + Charizard");
        Turn1EvaluationResult rDarkSteel = simulator.simulatePreset(playerLeads, "anti_dark_steel", "Talonflame + Charizard");

        System.out.println("Preset default_sun:       " + rSun.summary());
        System.out.println("Preset anti_psychic:      " + rPsychic.summary());
        System.out.println("Preset anti_dark_steel:   " + rDarkSteel.summary());

        // Invariant assertions:
        // 1. In default_sun, Galarian Slowking activates Drought, boosting Charizard's Solar Power (+50% SpA)
        // and Fire moves (+50% damage).
        // 2. Koga's team is structurally weak to Fire and Flying:
        //    Mega Beedrill: Bug/Poison (2x Fire, 2x Flying)
        //    Venusaur: Grass/Poison (2x Fire, 2x Flying)
        //    Amoonguss: Grass/Poison (2x Fire, 2x Flying)
        // 3. Confirm that NONE of the CURRENT 3 PRESETS achieves GOOD against Talonflame + Charizard (NOT_SOLVABLE_BY_CURRENT_PRESETS):
        assertNotEquals(Turn1Verdict.GOOD, rSun.verdict(),
                "default_sun cannot achieve GOOD against Talonflame + Charizard");
        assertNotEquals(Turn1Verdict.GOOD, rPsychic.verdict(),
                "anti_psychic cannot achieve GOOD against Talonflame + Charizard");
        assertNotEquals(Turn1Verdict.GOOD, rDarkSteel.verdict(),
                "anti_dark_steel cannot achieve GOOD against Talonflame + Charizard");

        System.out.println("VERIFIED INVARIANT: NOT_SOLVABLE_BY_CURRENT_PRESETS holds true (none of Koga's 3 configured presets achieves GOOD against Talonflame + Charizard)");
    }
}
