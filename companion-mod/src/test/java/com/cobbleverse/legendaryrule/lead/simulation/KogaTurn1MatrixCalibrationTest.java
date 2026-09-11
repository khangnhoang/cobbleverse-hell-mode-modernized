package com.cobbleverse.legendaryrule.lead.simulation;

import com.cobbleverse.legendaryrule.lead.*;
import com.cobbleverse.legendaryrule.lead.simulation.engine.Turn1LeadSimulator;
import com.cobbleverse.legendaryrule.lead.simulation.fixtures.KogaCompetitiveProfiles;
import com.cobbleverse.legendaryrule.lead.simulation.fixtures.ThreatPoolCompetitiveProfiles;
import com.cobbleverse.legendaryrule.lead.simulation.model.CompetitivePokemonProfile;
import com.cobbleverse.legendaryrule.lead.simulation.model.Turn1EvaluationResult;
import com.cobbleverse.legendaryrule.lead.simulation.model.Turn1Verdict;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class KogaTurn1MatrixCalibrationTest {

    private static TypeMatchupScorer scorer;
    private static LeadSelectionEngine engine;
    private static Turn1LeadSimulator simulator;
    private static List<RosterMemberTyping> kogaRoster;
    private static List<LeadAttempt> currentPresets;

    @BeforeAll
    static void setUp() {
        TypeChartData data = TypeChartResourceLoader.load().orElseThrow(
                () -> new IllegalStateException("Failed to load canonical Gen 9 type chart"));
        scorer = new TypeMatchupScorer(data);
        engine = new LeadSelectionEngine(scorer);
        simulator = new Turn1LeadSimulator();
        kogaRoster = KogaLeadCalibrationHarnessTest.createKogaRoster();

        // Calibrated presets from kanto_koga.json (Optimal Candidate 9)
        currentPresets = List.of(
                new LeadAttempt("default_sun", new int[]{5, 1}, -2, List.of(), "Slowking + Venusaur",
                        List.of(), List.of("pelipper", "politoed")),
                new LeadAttempt("anti_psychic", new int[]{0, 5}, -2, List.of(), "Beedrill + Slowking",
                        List.of(), List.of("indeedee", "armarouge", "hatterene", "alakazam", "farigiraf", "talonflame")),
                new LeadAttempt("anti_dark_steel", new int[]{2, 3}, 2, List.of(), "Okidogi + Sneasler",
                        List.of(), List.of("heatran", "kingambit", "chiyu", "chienpao", "incineroar", "gholdengo", "cresselia"))
        );
    }

    public record CaseRecord(
            int index,
            String category,
            String pairName,
            List<CompetitivePokemonProfile> playerProfiles,
            String engineWinner,
            String simPreferred,
            Map<String, Turn1EvaluationResult> simResults,
            String status, // AGREED, ACCEPTABLE_TIE, SUBOPTIMAL, CATASTROPHIC_SELECTION
            String decisiveReason
    ) {}

    public record SearchResult(
            String label,
            int bwSun, int bwPsy, int bwDs,
            int[] speciesAssignment, // 0: none, 1: sun, 2: psy, 3: ds
            int avoidableCatastrophic,
            int totalCatastrophic,
            int suboptimal,
            int viable,
            int agreed,
            int ties,
            int triggerCount,
            boolean is2Preset
    ) {}


    @Test
    @DisplayName("Run 51-Pair Turn-1 Calibration against current presets")
    void run51PairCalibration() throws IOException {
        List<KogaLeadCalibrationHarnessTest.PlayerPair> pool = KogaLeadCalibrationHarnessTest.createTestPool();
        List<CaseRecord> records = new ArrayList<>();

        StringBuilder sb = new StringBuilder();
        sb.append("========================================================================================\n");
        sb.append("KOGA 51-PAIR TURN-1 CALIBRATION REPORT (CURRENT PRESETS)\n");
        sb.append("========================================================================================\n\n");

        int agreementCount = 0;
        int acceptableTieCount = 0;
        int suboptimalCount = 0;
        int catastrophicCount = 0;

        Map<String, Integer> enginePicks = new LinkedHashMap<>();
        Map<String, Integer> simPicks = new LinkedHashMap<>();
        for (LeadAttempt a : currentPresets) {
            enginePicks.put(a.id(), 0);
            simPicks.put(a.id(), 0);
        }

        Map<String, Map<Turn1Verdict, Integer>> verdictCounts = new LinkedHashMap<>();
        for (LeadAttempt a : currentPresets) {
            verdictCounts.put(a.id(), new EnumMap<>(Turn1Verdict.class));
            for (Turn1Verdict v : Turn1Verdict.values()) {
                verdictCounts.get(a.id()).put(v, 0);
            }
        }

        for (int i = 0; i < pool.size(); i++) {
            KogaLeadCalibrationHarnessTest.PlayerPair pair = pool.get(i);
            String pairName = pair.name1() + " + " + pair.name2();

            // 1. Resolve competitive profiles
            CompetitivePokemonProfile prof1 = ThreatPoolCompetitiveProfiles.getProfile(pair.name1());
            CompetitivePokemonProfile prof2 = ThreatPoolCompetitiveProfiles.getProfile(pair.name2());
            assertNotNull(prof1, "Profile missing for: " + pair.name1());
            assertNotNull(prof2, "Profile missing for: " + pair.name2());
            List<CompetitivePokemonProfile> playerLeads = List.of(prof1, prof2);

            // 2. Run LeadSelectionEngine
            List<PlayerLeadTyping> playerTypings = List.of(
                    new PlayerLeadTyping(pair.name1(), pair.types1()),
                    new PlayerLeadTyping(pair.name2(), pair.types2())
            );
            LeadSelectionResult selResult = engine.select(currentPresets, playerTypings, kogaRoster);
            String engineWinner = selResult.selectedAttempt().id();
            enginePicks.put(engineWinner, enginePicks.get(engineWinner) + 1);

            // 3. Run Turn1LeadSimulator for all 3 presets
            Map<String, Turn1EvaluationResult> simResults = new LinkedHashMap<>();
            for (LeadAttempt preset : currentPresets) {
                Turn1EvaluationResult r = simulator.simulatePreset(playerLeads, preset.id(), pairName);
                simResults.put(preset.id(), r);
                verdictCounts.get(preset.id()).put(r.verdict(), verdictCounts.get(preset.id()).get(r.verdict()) + 1);
            }

            // 4. Find simulator preferred preset
            String simPreferred = currentPresets.get(0).id();
            Turn1EvaluationResult bestResult = simResults.get(simPreferred);
            for (int p = 1; p < currentPresets.size(); p++) {
                String candidateId = currentPresets.get(p).id();
                Turn1EvaluationResult candidateResult = simResults.get(candidateId);
                if (isBetter(candidateResult, bestResult)) {
                    simPreferred = candidateId;
                    bestResult = candidateResult;
                }
            }
            simPicks.put(simPreferred, simPicks.get(simPreferred) + 1);

            // 5. Classify agreement / discrepancy
            Turn1EvaluationResult engineResult = simResults.get(engineWinner);
            String status;
            if (engineWinner.equals(simPreferred)) {
                status = "AGREED";
                agreementCount++;
            } else if (engineResult.verdict() == bestResult.verdict() && engineResult.kogaCasualties() == bestResult.kogaCasualties()) {
                status = "ACCEPTABLE_TIE";
                acceptableTieCount++;
            } else if (engineResult.verdict() == Turn1Verdict.CATASTROPHIC && bestResult.verdict() != Turn1Verdict.CATASTROPHIC) {
                status = "CATASTROPHIC_SELECTION";
                catastrophicCount++;
            } else {
                status = "SUBOPTIMAL";
                suboptimalCount++;
            }

            // 6. Decisive reason
            String decisiveReason = deriveDecisiveReason(pair, playerLeads, simResults, engineWinner, simPreferred);

            CaseRecord record = new CaseRecord(
                    i + 1, pair.category(), pairName, playerLeads,
                    engineWinner, simPreferred, simResults, status, decisiveReason
            );
            records.add(record);

            // Format case log
            sb.append(String.format("Case #%02d [%s] %s\n", i + 1, pair.category(), pairName));
            sb.append(String.format("  Engine Pick: %s | Sim Preferred: %s | Status: %s\n", engineWinner, simPreferred, status));
            for (Map.Entry<String, Turn1EvaluationResult> entry : simResults.entrySet()) {
                Turn1EvaluationResult r = entry.getValue();
                sb.append(String.format("    - %-16s -> Verdict: %-12s | KOs: %d, Loss: %d | Min HP: %5.1f%%\n",
                        entry.getKey(), r.verdict(), r.kogaKnockoutsScored(), r.kogaCasualties(), r.minKogaHpRemainingPercent()));
            }
            sb.append("  Decisive Reason: ").append(decisiveReason).append("\n\n");
        }

        // Summary Aggregations
        sb.append("========================================================================================\n");
        sb.append("CALIBRATION SUMMARY METRICS\n");
        sb.append("========================================================================================\n");
        sb.append(String.format("Total Cases:                 %d\n", pool.size()));
        sb.append(String.format("Strict Agreement:            %d (%.1f%%)\n", agreementCount, agreementCount * 100.0 / pool.size()));
        sb.append(String.format("Acceptable Tie:              %d (%.1f%%)\n", acceptableTieCount, acceptableTieCount * 100.0 / pool.size()));
        sb.append(String.format("Combined Viable Rate:        %d (%.1f%%)\n", (agreementCount + acceptableTieCount), (agreementCount + acceptableTieCount) * 100.0 / pool.size()));
        sb.append(String.format("Suboptimal Selections:       %d (%.1f%%)\n", suboptimalCount, suboptimalCount * 100.0 / pool.size()));
        sb.append(String.format("Catastrophic Selections:     %d (%.1f%%)\n\n", catastrophicCount, catastrophicCount * 100.0 / pool.size()));

        sb.append("--- PRESET SELECTION COUNTS ---\n");
        for (LeadAttempt a : currentPresets) {
            sb.append(String.format("  %-16s: Engine = %2d picks | Sim = %2d picks\n",
                    a.id(), enginePicks.get(a.id()), simPicks.get(a.id())));
        }

        sb.append("\n--- PRESET VERDICT DISTRIBUTIONS ---\n");
        for (LeadAttempt a : currentPresets) {
            Map<Turn1Verdict, Integer> vMap = verdictCounts.get(a.id());
            sb.append(String.format("  %-16s: GOOD = %2d | QUESTIONABLE = %2d | CATASTROPHIC = %2d\n",
                    a.id(), vMap.get(Turn1Verdict.GOOD), vMap.get(Turn1Verdict.QUESTIONABLE), vMap.get(Turn1Verdict.CATASTROPHIC)));
        }

        // Problematic cases
        sb.append("\n========================================================================================\n");
        sb.append("PROBLEMATIC CASES (SUBOPTIMAL OR CATASTROPHIC SELECTION)\n");
        sb.append("========================================================================================\n");
        for (CaseRecord r : records) {
            if ("SUBOPTIMAL".equals(r.status()) || "CATASTROPHIC_SELECTION".equals(r.status())) {
                Turn1EvaluationResult engRes = r.simResults().get(r.engineWinner());
                Turn1EvaluationResult simRes = r.simResults().get(r.simPreferred());
                sb.append(String.format("Case #%02d [%s] %s:\n", r.index(), r.category(), r.pairName()));
                sb.append(String.format("  Engine picked [%s] (%s, %d KOs, %d loss, %.1f%% HP)\n",
                        r.engineWinner(), engRes.verdict(), engRes.kogaKnockoutsScored(), engRes.kogaCasualties(), engRes.minKogaHpRemainingPercent()));
                sb.append(String.format("  Better option [%s] (%s, %d KOs, %d loss, %.1f%% HP)\n",
                        r.simPreferred(), simRes.verdict(), simRes.kogaKnockoutsScored(), simRes.kogaCasualties(), simRes.minKogaHpRemainingPercent()));
                sb.append(String.format("  Status: %s | Reason: %s\n\n", r.status(), r.decisiveReason()));
            }
        }

        String report = sb.toString();
        System.out.println(report);

        Path outPath = Path.of("../reports/koga_51pair_turn1_matrix_report.txt");
        Files.writeString(outPath, report);
    }

    private boolean isBetter(Turn1EvaluationResult a, Turn1EvaluationResult b) {
        if (a.verdict() != b.verdict()) {
            return rankVerdict(a.verdict()) > rankVerdict(b.verdict());
        }
        if (a.kogaCasualties() != b.kogaCasualties()) {
            return a.kogaCasualties() < b.kogaCasualties();
        }
        if (a.kogaKnockoutsScored() != b.kogaKnockoutsScored()) {
            return a.kogaKnockoutsScored() > b.kogaKnockoutsScored();
        }
        return a.minKogaHpRemainingPercent() > b.minKogaHpRemainingPercent();
    }

    private int rankVerdict(Turn1Verdict v) {
        return switch (v) {
            case CATASTROPHIC -> 0;
            case QUESTIONABLE -> 1;
            case GOOD -> 2;
        };
    }

    private String deriveDecisiveReason(
            KogaLeadCalibrationHarnessTest.PlayerPair pair,
            List<CompetitivePokemonProfile> leads,
            Map<String, Turn1EvaluationResult> results,
            String enginePick,
            String simPick
    ) {
        boolean hasSun = pair.name1().equals("torkoal") || pair.name2().equals("torkoal");
        boolean hasRain = pair.name1().equals("pelipper") || pair.name2().equals("pelipper") || pair.name1().equals("politoed") || pair.name2().equals("politoed");
        boolean hasTrickRoom = pair.name1().equals("cresselia") || pair.name2().equals("cresselia") || pair.name1().equals("hatterene") || pair.name2().equals("hatterene");
        boolean hasPsychicTerrain = pair.name1().startsWith("indeedee") || pair.name2().startsWith("indeedee");
        boolean hasGroundThreat = pair.types1().contains("ground") || pair.types2().contains("ground");
        boolean hasSteelThreat = pair.types1().contains("steel") || pair.types2().contains("steel");

        if (hasTrickRoom && results.get("default_sun").verdict() == Turn1Verdict.CATASTROPHIC) {
            return "Trick Room setter uninterrupted vs default_sun; anti_dark_steel offers immediate offensive pressure or anti_psychic bug STAB";
        }
        if (hasRain) {
            return "Weather war: Galarian Slowking (34 Spe) Drought overrides Drizzle, enabling Sun Venusaur (288 Spe)";
        }
        if (hasPsychicTerrain) {
            return "Psychic Terrain blocks priority; Fighting leads face heavy Psychic pressure unless single-target redirection is overwhelmed";
        }
        if (hasGroundThreat && results.get("anti_dark_steel").verdict() == Turn1Verdict.CATASTROPHIC) {
            return "Ground weakness on Okidogi/Sneasler exploited; default_sun provides Grass STAB and Chlorophyll speed advantage";
        }
        if (hasSteelThreat) {
            return "Steel typing resists Poison STAB; Okidogi/Sneasler provide Close Combat/Drain Punch Fighting answers";
        }
        return "Type matchup and base damage output determine board control";
    }

    @Test
    @DisplayName("Evaluate candidate preset configurations for minimal safe design")
    void testOptimizeConfiguration() {
        List<KogaLeadCalibrationHarnessTest.PlayerPair> pool = KogaLeadCalibrationHarnessTest.createTestPool();

        // Precompute simulation results for all 51 pairs
        List<Map<String, Turn1EvaluationResult>> simResultsList = new ArrayList<>();
        List<List<PlayerLeadTyping>> typingsList = new ArrayList<>();
        for (KogaLeadCalibrationHarnessTest.PlayerPair pair : pool) {
            CompetitivePokemonProfile prof1 = ThreatPoolCompetitiveProfiles.getProfile(pair.name1());
            CompetitivePokemonProfile prof2 = ThreatPoolCompetitiveProfiles.getProfile(pair.name2());
            List<CompetitivePokemonProfile> playerLeads = List.of(prof1, prof2);
            Map<String, Turn1EvaluationResult> rMap = new LinkedHashMap<>();
            for (String presetId : List.of("default_sun", "anti_psychic", "anti_dark_steel")) {
                rMap.put(presetId, simulator.simulatePreset(playerLeads, presetId, pair.name1() + " + " + pair.name2()));
            }
            simResultsList.add(rMap);
            typingsList.add(List.of(
                    new PlayerLeadTyping(pair.name1(), pair.types1()),
                    new PlayerLeadTyping(pair.name2(), pair.types2())
            ));
        }

        // Test Candidate Configurations
        record CandidateConfig(String name, List<LeadAttempt> presets) {}
        List<CandidateConfig> candidates = new ArrayList<>();

        // Candidate 0: Baseline (current)
        candidates.add(new CandidateConfig("Baseline (Current)", currentPresets));

        // Candidate 1: Equalize base weights (0, 0, 0)
        candidates.add(new CandidateConfig("Equal Weights (0, 0, 0)", List.of(
                new LeadAttempt("default_sun", new int[]{5, 1}, 0, List.of(), "", List.of(), List.of("pelipper", "politoed")),
                new LeadAttempt("anti_psychic", new int[]{0, 5}, 0, List.of(), "", List.of(), List.of("indeedee", "armarouge", "hatterene", "alakazam", "farigiraf", "cresselia", "ironcrown")),
                new LeadAttempt("anti_dark_steel", new int[]{2, 3}, 0, List.of(), "", List.of(), List.of("heatran", "kingambit", "chiyu", "chienpao", "incineroar"))
        )));

        // Candidate 2: Equal weights + Add gholdengo to anti_dark_steel
        candidates.add(new CandidateConfig("Equal Weights + gholdengo", List.of(
                new LeadAttempt("default_sun", new int[]{5, 1}, 0, List.of(), "", List.of(), List.of("pelipper", "politoed")),
                new LeadAttempt("anti_psychic", new int[]{0, 5}, 0, List.of(), "", List.of(), List.of("indeedee", "armarouge", "hatterene", "alakazam", "farigiraf", "cresselia", "ironcrown")),
                new LeadAttempt("anti_dark_steel", new int[]{2, 3}, 0, List.of(), "", List.of(), List.of("heatran", "kingambit", "chiyu", "chienpao", "incineroar", "gholdengo"))
        )));

        // Candidate 3: Equal weights + Add gholdengo + remove cresselia/ironcrown traps from anti_psychic
        candidates.add(new CandidateConfig("Equal Weights + gholdengo - cresselia/ironcrown", List.of(
                new LeadAttempt("default_sun", new int[]{5, 1}, 0, List.of(), "", List.of(), List.of("pelipper", "politoed")),
                new LeadAttempt("anti_psychic", new int[]{0, 5}, 0, List.of(), "", List.of(), List.of("indeedee", "armarouge", "hatterene", "alakazam", "farigiraf")),
                new LeadAttempt("anti_dark_steel", new int[]{2, 3}, 0, List.of(), "", List.of(), List.of("heatran", "kingambit", "chiyu", "chienpao", "incineroar", "gholdengo"))
        )));

        // Candidate 4: anti_dark_steel weighted higher (1, 0, 0)
        candidates.add(new CandidateConfig("anti_dark_steel bw=1 + gholdengo - cresselia/ironcrown", List.of(
                new LeadAttempt("default_sun", new int[]{5, 1}, 0, List.of(), "", List.of(), List.of("pelipper", "politoed")),
                new LeadAttempt("anti_psychic", new int[]{0, 5}, 0, List.of(), "", List.of(), List.of("indeedee", "armarouge", "hatterene", "alakazam", "farigiraf")),
                new LeadAttempt("anti_dark_steel", new int[]{2, 3}, 1, List.of(), "", List.of(), List.of("heatran", "kingambit", "chiyu", "chienpao", "incineroar", "gholdengo"))
        )));

        // Candidate 7: anti_dark_steel bw=2 (2, 0, 0)
        candidates.add(new CandidateConfig("anti_dark_steel bw=2 + gholdengo - cresselia/ironcrown", List.of(
                new LeadAttempt("default_sun", new int[]{5, 1}, 0, List.of(), "", List.of(), List.of("pelipper", "politoed")),
                new LeadAttempt("anti_psychic", new int[]{0, 5}, 0, List.of(), "", List.of(), List.of("indeedee", "armarouge", "hatterene", "alakazam", "farigiraf")),
                new LeadAttempt("anti_dark_steel", new int[]{2, 3}, 2, List.of(), "", List.of(), List.of("heatran", "kingambit", "chiyu", "chienpao", "incineroar", "gholdengo"))
        )));

        // Candidate 8: anti_dark_steel bw=1, anti_psychic bw=-1 (1, -1, 0)
        candidates.add(new CandidateConfig("anti_dark_steel bw=1, anti_psychic bw=-1", List.of(
                new LeadAttempt("default_sun", new int[]{5, 1}, 0, List.of(), "", List.of(), List.of("pelipper", "politoed")),
                new LeadAttempt("anti_psychic", new int[]{0, 5}, -1, List.of(), "", List.of(), List.of("indeedee", "armarouge", "hatterene", "alakazam", "farigiraf")),
                new LeadAttempt("anti_dark_steel", new int[]{2, 3}, 1, List.of(), "", List.of(), List.of("heatran", "kingambit", "chiyu", "chienpao", "incineroar", "gholdengo"))
        )));

        // Candidate 9: Candidate 4 + gholdengo & landorus in anti_dark_steel
        candidates.add(new CandidateConfig("Candidate 4 + landorus to anti_dark_steel", List.of(
                new LeadAttempt("default_sun", new int[]{5, 1}, 0, List.of(), "", List.of(), List.of("pelipper", "politoed")),
                new LeadAttempt("anti_psychic", new int[]{0, 5}, 0, List.of(), "", List.of(), List.of("indeedee", "armarouge", "hatterene", "alakazam", "farigiraf")),
                new LeadAttempt("anti_dark_steel", new int[]{2, 3}, 1, List.of(), "", List.of(), List.of("heatran", "kingambit", "chiyu", "chienpao", "incineroar", "gholdengo", "landorus"))
        )));

        System.out.println("\n========================================================================================");
        System.out.println("CANDIDATE CONFIGURATIONS COMPARISON ON 51 PAIRS");
        System.out.println("========================================================================================");
        System.out.printf("%-50s | Agreed | Ties | Viable | Suboptimal | Catastrophic\n", "Configuration");
        System.out.println("----------------------------------------------------------------------------------------");

        for (CandidateConfig cand : candidates) {
            int agreed = 0;
            int ties = 0;
            int sub = 0;
            int cat = 0;

            for (int i = 0; i < pool.size(); i++) {
                List<PlayerLeadTyping> typings = typingsList.get(i);
                Map<String, Turn1EvaluationResult> simResults = simResultsList.get(i);

                LeadSelectionResult res = engine.select(cand.presets(), typings, kogaRoster);
                String pick = res.selectedAttempt().id();

                String simPref = cand.presets().get(0).id();
                Turn1EvaluationResult best = simResults.get(simPref);
                for (int p = 1; p < cand.presets().size(); p++) {
                    String candId = cand.presets().get(p).id();
                    Turn1EvaluationResult cr = simResults.get(candId);
                    if (isBetter(cr, best)) {
                        simPref = candId;
                        best = cr;
                    }
                }

                Turn1EvaluationResult pickRes = simResults.get(pick);
                if (pick.equals(simPref)) {
                    agreed++;
                } else if (pickRes.verdict() == best.verdict() && pickRes.kogaCasualties() == best.kogaCasualties()) {
                    ties++;
                } else if (pickRes.verdict() == Turn1Verdict.CATASTROPHIC && best.verdict() != Turn1Verdict.CATASTROPHIC) {
                    cat++;
                } else {
                    sub++;
                }
            }

            System.out.printf("%-50s |   %2d   |  %2d  |  %2d (%.1f%%) |     %2d     |      %2d\n",
                    cand.name(), agreed, ties, (agreed + ties), (agreed + ties) * 100.0 / pool.size(), sub, cat);

            if (cand.name().startsWith("anti_dark_steel bw=1 + gholdengo")) {
                System.out.println("\n--- DETAILED CATASTROPHIC CASES FOR CANDIDATE 4 ---");
                for (int i = 0; i < pool.size(); i++) {
                    List<PlayerLeadTyping> typings = typingsList.get(i);
                    Map<String, Turn1EvaluationResult> simResults = simResultsList.get(i);
                    LeadSelectionResult res = engine.select(cand.presets(), typings, kogaRoster);
                    String pick = res.selectedAttempt().id();
                    Turn1EvaluationResult pickRes = simResults.get(pick);

                    String simPref = cand.presets().get(0).id();
                    Turn1EvaluationResult best = simResults.get(simPref);
                    for (int p = 1; p < cand.presets().size(); p++) {
                        String candId = cand.presets().get(p).id();
                        Turn1EvaluationResult cr = simResults.get(candId);
                        if (isBetter(cr, best)) {
                            simPref = candId;
                            best = cr;
                        }
                    }

                    if (pickRes.verdict() == Turn1Verdict.CATASTROPHIC && best.verdict() != Turn1Verdict.CATASTROPHIC) {
                        KogaLeadCalibrationHarnessTest.PlayerPair p = pool.get(i);
                        System.out.printf("  Case #%02d [%s] %s + %s: Picked [%s] (CATASTROPHIC) vs Better [%s] (%s)\n",
                                i + 1, p.category(), p.name1(), p.name2(), pick, simPref, best.verdict());
                        for (AttemptScore score : res.evaluatedScores()) {
                            System.out.printf("      Attempt [%s]: off=%d, def=%d, bw=%d, tf=%d, sf=%d => total=%d\n",
                                    score.attemptId(), score.offensiveScore(), score.defensiveScore(), score.baseWeight(), score.typeFavoredBonus(), score.speciesFavoredBonus(), score.totalScore());
                        }
                    }
                }
            }
        }
    }

    @Test
    @DisplayName("2-Preset Ablation Audit: prove all 3 presets are required")
    void test2PresetAblations() {
        List<KogaLeadCalibrationHarnessTest.PlayerPair> pool = KogaLeadCalibrationHarnessTest.createTestPool();

        // Precompute simulation results for all 51 pairs
        List<Map<String, Turn1EvaluationResult>> simResultsList = new ArrayList<>();
        List<List<PlayerLeadTyping>> typingsList = new ArrayList<>();
        for (KogaLeadCalibrationHarnessTest.PlayerPair pair : pool) {
            CompetitivePokemonProfile prof1 = ThreatPoolCompetitiveProfiles.getProfile(pair.name1());
            CompetitivePokemonProfile prof2 = ThreatPoolCompetitiveProfiles.getProfile(pair.name2());
            List<CompetitivePokemonProfile> playerLeads = List.of(prof1, prof2);
            Map<String, Turn1EvaluationResult> rMap = new LinkedHashMap<>();
            for (String presetId : List.of("default_sun", "anti_psychic", "anti_dark_steel")) {
                rMap.put(presetId, simulator.simulatePreset(playerLeads, presetId, pair.name1() + " + " + pair.name2()));
            }
            simResultsList.add(rMap);
            typingsList.add(List.of(
                    new PlayerLeadTyping(pair.name1(), pair.types1()),
                    new PlayerLeadTyping(pair.name2(), pair.types2())
            ));
        }

        LeadAttempt sun = new LeadAttempt("default_sun", new int[]{5, 1}, 0, List.of(), "", List.of(), List.of("pelipper", "politoed"));
        LeadAttempt psychic = new LeadAttempt("anti_psychic", new int[]{0, 5}, -1, List.of(), "", List.of(), List.of("indeedee", "armarouge", "hatterene", "alakazam", "farigiraf"));
        LeadAttempt darkSteel = new LeadAttempt("anti_dark_steel", new int[]{2, 3}, 1, List.of(), "", List.of(), List.of("heatran", "kingambit", "chiyu", "chienpao", "incineroar", "gholdengo"));

        record AblationConfig(String name, List<LeadAttempt> presets) {}
        List<AblationConfig> ablations = List.of(
                new AblationConfig("Candidate 8 (Full 3-Preset Core)", List.of(sun, psychic, darkSteel)),
                new AblationConfig("Ablation A: Drop anti_psychic (Sun + DarkSteel)", List.of(sun, darkSteel)),
                new AblationConfig("Ablation B: Drop default_sun (Psychic + DarkSteel)", List.of(psychic, darkSteel)),
                new AblationConfig("Ablation C: Drop anti_dark_steel (Sun + Psychic)", List.of(sun, psychic))
        );

        System.out.println("\n========================================================================================");
        System.out.println("2-PRESET ABLATION AUDIT RESULTS (51 PAIRS)");
        System.out.println("========================================================================================");
        System.out.printf("%-50s | Agreed | Ties | Viable | Suboptimal | Catastrophic\n", "Configuration");
        System.out.println("----------------------------------------------------------------------------------------");

        for (AblationConfig abl : ablations) {
            int agreed = 0;
            int ties = 0;
            int sub = 0;
            int cat = 0;

            for (int i = 0; i < pool.size(); i++) {
                List<PlayerLeadTyping> typings = typingsList.get(i);
                Map<String, Turn1EvaluationResult> simResults = simResultsList.get(i);

                LeadSelectionResult res = engine.select(abl.presets(), typings, kogaRoster);
                String pick = res.selectedAttempt().id();

                // Best achievable among the presets available in this ablation
                String simPref = abl.presets().get(0).id();
                Turn1EvaluationResult best = simResults.get(simPref);
                for (int p = 1; p < abl.presets().size(); p++) {
                    String candId = abl.presets().get(p).id();
                    Turn1EvaluationResult cr = simResults.get(candId);
                    if (isBetter(cr, best)) {
                        simPref = candId;
                        best = cr;
                    }
                }

                Turn1EvaluationResult pickRes = simResults.get(pick);
                if (pick.equals(simPref)) {
                    agreed++;
                } else if (pickRes.verdict() == best.verdict() && pickRes.kogaCasualties() == best.kogaCasualties()) {
                    ties++;
                } else if (pickRes.verdict() == Turn1Verdict.CATASTROPHIC && best.verdict() != Turn1Verdict.CATASTROPHIC) {
                    cat++;
                } else {
                    sub++;
                }
            }

            System.out.printf("%-50s |   %2d   |  %2d  |  %2d (%.1f%%) |     %2d     |      %2d\n",
                    abl.name(), agreed, ties, (agreed + ties), (agreed + ties) * 100.0 / pool.size(), sub, cat);
        }

        // Audit the 5 residual catastrophic cases of Candidate 8
        System.out.println("\n========================================================================================");
        System.out.println("EXACT AUDIT OF CANDIDATE 8's 5 RESIDUAL CATASTROPHIC CASES");
        System.out.println("========================================================================================");
        List<LeadAttempt> c8Presets = List.of(sun, psychic, darkSteel);
        for (int i = 0; i < pool.size(); i++) {
            List<PlayerLeadTyping> typings = typingsList.get(i);
            Map<String, Turn1EvaluationResult> simResults = simResultsList.get(i);

            LeadSelectionResult res = engine.select(c8Presets, typings, kogaRoster);
            String pick = res.selectedAttempt().id();
            Turn1EvaluationResult pickRes = simResults.get(pick);

            String simPref = c8Presets.get(0).id();
            Turn1EvaluationResult best = simResults.get(simPref);
            for (int p = 1; p < c8Presets.size(); p++) {
                String candId = c8Presets.get(p).id();
                Turn1EvaluationResult cr = simResults.get(candId);
                if (isBetter(cr, best)) {
                    simPref = candId;
                    best = cr;
                }
            }

            if (pickRes.verdict() == Turn1Verdict.CATASTROPHIC && best.verdict() != Turn1Verdict.CATASTROPHIC) {
                KogaLeadCalibrationHarnessTest.PlayerPair p = pool.get(i);
                Turn1EvaluationResult rSun = simResults.get("default_sun");
                Turn1EvaluationResult rPsychic = simResults.get("anti_psychic");
                Turn1EvaluationResult rDarkSteel = simResults.get("anti_dark_steel");

                boolean allCatastrophic = rSun.verdict() == Turn1Verdict.CATASTROPHIC &&
                        rPsychic.verdict() == Turn1Verdict.CATASTROPHIC &&
                        rDarkSteel.verdict() == Turn1Verdict.CATASTROPHIC;

                String classification = allCatastrophic ? "UNAVOIDABLE" : "AVOIDABLE (Alternative: " + simPref + " is " + best.verdict() + ")";

                System.out.printf("\nCase #%02d [%s] %s + %s\n", i + 1, p.category(), p.name1(), p.name2());
                System.out.printf("  Engine Pick: %s (CATASTROPHIC)\n", pick);
                System.out.printf("  3-Preset Verdicts: default_sun=%s, anti_psychic=%s, anti_dark_steel=%s\n",
                        rSun.verdict(), rPsychic.verdict(), rDarkSteel.verdict());
                System.out.printf("  Classification: %s\n", classification);
                System.out.println("  Scoring Evidence:");
                for (AttemptScore sc : res.evaluatedScores()) {
                    System.out.printf("    - [%s]: off=%d, def=%d, bw=%d, tf=%d, sf=%d => total=%d\n",
                            sc.attemptId(), sc.offensiveScore(), sc.defensiveScore(), sc.baseWeight(), sc.typeFavoredBonus(), sc.speciesFavoredBonus(), sc.totalScore());
                }
            }
        }
    }

    @Test
    @DisplayName("Per-case Delta Audit & Systematic Configuration Optimizer")
    void testPerCaseDeltasAndOptimizer() {
        List<KogaLeadCalibrationHarnessTest.PlayerPair> pool = KogaLeadCalibrationHarnessTest.createTestPool();

        // 1. Precompute simulation results and typings for all 51 pairs
        List<Map<String, Turn1EvaluationResult>> simResultsList = new ArrayList<>();
        List<List<PlayerLeadTyping>> typingsList = new ArrayList<>();
        for (KogaLeadCalibrationHarnessTest.PlayerPair pair : pool) {
            CompetitivePokemonProfile prof1 = ThreatPoolCompetitiveProfiles.getProfile(pair.name1());
            CompetitivePokemonProfile prof2 = ThreatPoolCompetitiveProfiles.getProfile(pair.name2());
            List<CompetitivePokemonProfile> playerLeads = List.of(prof1, prof2);
            Map<String, Turn1EvaluationResult> rMap = new LinkedHashMap<>();
            for (String presetId : List.of("default_sun", "anti_psychic", "anti_dark_steel")) {
                rMap.put(presetId, simulator.simulatePreset(playerLeads, presetId, pair.name1() + " + " + pair.name2()));
            }
            simResultsList.add(rMap);
            typingsList.add(List.of(
                    new PlayerLeadTyping(pair.name1(), pair.types1()),
                    new PlayerLeadTyping(pair.name2(), pair.types2())
            ));
        }

        // =========================================================================
        // PART 1: PER-CASE DELTA AUDIT
        // =========================================================================
        System.out.println("========================================================================================");
        System.out.println("PER-CASE DELTA AUDIT (51 PAIRS)");
        System.out.println("========================================================================================");

        // A. Inspect all 11 cases where default_sun is GOOD
        System.out.println("\n--- 11 CASES WHERE DEFAULT_SUN IS GOOD ---");
        int sunGoodCount = 0;
        int sunUniqueSaviorCount = 0; // Cases where sun is GOOD/QUEST but both psychic and dark_steel are CATASTROPHIC
        for (int i = 0; i < pool.size(); i++) {
            Map<String, Turn1EvaluationResult> r = simResultsList.get(i);
            Turn1EvaluationResult rSun = r.get("default_sun");
            Turn1EvaluationResult rPsy = r.get("anti_psychic");
            Turn1EvaluationResult rDs = r.get("anti_dark_steel");
            KogaLeadCalibrationHarnessTest.PlayerPair p = pool.get(i);

            if (rSun.verdict() == Turn1Verdict.GOOD) {
                sunGoodCount++;
                System.out.printf("  Case #%02d [%s] %-25s | sun=%-12s | psy=%-12s | ds=%-12s\n",
                        i + 1, p.category(), p.name1() + " + " + p.name2(),
                        rSun.verdict(), rPsy.verdict(), rDs.verdict());
            }

            if (rSun.verdict() != Turn1Verdict.CATASTROPHIC &&
                rPsy.verdict() == Turn1Verdict.CATASTROPHIC &&
                rDs.verdict() == Turn1Verdict.CATASTROPHIC) {
                sunUniqueSaviorCount++;
            }
        }
        System.out.printf("Total Sun GOOD cases: %d\n", sunGoodCount);
        System.out.printf("Cases where default_sun is the SOLE NON-CATASTROPHIC preset (psy & ds both CATASTROPHIC): %d\n", sunUniqueSaviorCount);

        // B. Inspect cases where anti_psychic is unique savior
        int psyUniqueSaviorCount = 0;
        System.out.println("\n--- CASES WHERE ANTI_PSYCHIC IS UNIQUE SAVIOR (sun & ds both CATASTROPHIC) ---");
        for (int i = 0; i < pool.size(); i++) {
            Map<String, Turn1EvaluationResult> r = simResultsList.get(i);
            Turn1EvaluationResult rSun = r.get("default_sun");
            Turn1EvaluationResult rPsy = r.get("anti_psychic");
            Turn1EvaluationResult rDs = r.get("anti_dark_steel");
            KogaLeadCalibrationHarnessTest.PlayerPair p = pool.get(i);

            if (rPsy.verdict() != Turn1Verdict.CATASTROPHIC &&
                rSun.verdict() == Turn1Verdict.CATASTROPHIC &&
                rDs.verdict() == Turn1Verdict.CATASTROPHIC) {
                psyUniqueSaviorCount++;
                System.out.printf("  Case #%02d [%s] %-25s | sun=%-12s | psy=%-12s | ds=%-12s\n",
                        i + 1, p.category(), p.name1() + " + " + p.name2(),
                        rSun.verdict(), rPsy.verdict(), rDs.verdict());
            }
        }
        System.out.printf("Total Anti-Psychic unique savior cases: %d\n", psyUniqueSaviorCount);

        // C. Inspect cases where anti_dark_steel is unique savior
        int dsUniqueSaviorCount = 0;
        System.out.println("\n--- CASES WHERE ANTI_DARK_STEEL IS UNIQUE SAVIOR (sun & psy both CATASTROPHIC) ---");
        for (int i = 0; i < pool.size(); i++) {
            Map<String, Turn1EvaluationResult> r = simResultsList.get(i);
            Turn1EvaluationResult rSun = r.get("default_sun");
            Turn1EvaluationResult rPsy = r.get("anti_psychic");
            Turn1EvaluationResult rDs = r.get("anti_dark_steel");
            KogaLeadCalibrationHarnessTest.PlayerPair p = pool.get(i);

            if (rDs.verdict() != Turn1Verdict.CATASTROPHIC &&
                rSun.verdict() == Turn1Verdict.CATASTROPHIC &&
                rPsy.verdict() == Turn1Verdict.CATASTROPHIC) {
                dsUniqueSaviorCount++;
                System.out.printf("  Case #%02d [%s] %-25s | sun=%-12s | psy=%-12s | ds=%-12s\n",
                        i + 1, p.category(), p.name1() + " + " + p.name2(),
                        rSun.verdict(), rPsy.verdict(), rDs.verdict());
            }
        }
        System.out.printf("Total Anti-Dark/Steel unique savior cases: %d\n", dsUniqueSaviorCount);

        // =========================================================================
        // PART 2: SYSTEMATIC CONFIGURATION SEARCH
        // =========================================================================
        System.out.println("\n========================================================================================");
        System.out.println("SYSTEMATIC CONFIGURATION SEARCH");
        System.out.println("========================================================================================");

        // Precompute baseOffDef for each pair and preset (baseWeight = 0, no triggers)
        LeadAttempt dummySun = new LeadAttempt("default_sun", new int[]{5, 1}, 0, List.of(), "", List.of(), List.of());
        LeadAttempt dummyPsy = new LeadAttempt("anti_psychic", new int[]{0, 5}, 0, List.of(), "", List.of(), List.of());
        LeadAttempt dummyDs = new LeadAttempt("anti_dark_steel", new int[]{2, 3}, 0, List.of(), "", List.of(), List.of());
        List<LeadAttempt> dummyPresets = List.of(dummySun, dummyPsy, dummyDs);

        int[][] baseOffDef = new int[pool.size()][3];
        for (int i = 0; i < pool.size(); i++) {
            LeadSelectionResult r = engine.select(dummyPresets, typingsList.get(i), kogaRoster);
            for (int p = 0; p < 3; p++) {
                baseOffDef[i][p] = r.evaluatedScores().get(p).offensiveScore() + r.evaluatedScores().get(p).defensiveScore();
            }
        }

        // Candidate species from the 5 avoidable cases that exist in canonical Cobblemon pokedex
        List<String> candidateSpecies = List.of(
                "gholdengo", "landorus", "cresselia",
                "talonflame", "garchomp", "charizard"
        );

        // Baseline triggers
        List<String> baseSunSpecies = List.of("pelipper", "politoed");
        List<String> basePsySpecies = List.of("indeedee", "armarouge", "hatterene", "alakazam", "farigiraf");
        List<String> baseDsSpecies = List.of("heatran", "kingambit", "chiyu", "chienpao", "incineroar");

        // Compute baseline species bonus for each pair and preset
        int[][] baseSpeciesBonus = new int[pool.size()][3];
        for (int i = 0; i < pool.size(); i++) {
            KogaLeadCalibrationHarnessTest.PlayerPair p = pool.get(i);
            for (String spec : List.of(p.name1(), p.name2())) {
                if (baseSunSpecies.contains(spec)) baseSpeciesBonus[i][0] += 2;
                if (basePsySpecies.contains(spec)) baseSpeciesBonus[i][1] += 2;
                if (baseDsSpecies.contains(spec)) baseSpeciesBonus[i][2] += 2;
            }
        }

        // Map which pairs contain each candidate species
        // candPairMatches[candIdx] is int array of bonuses (+2 for each match)
        int[][] candPairMatches = new int[candidateSpecies.size()][pool.size()];
        for (int c = 0; c < candidateSpecies.size(); c++) {
            String cSpec = candidateSpecies.get(c);
            for (int i = 0; i < pool.size(); i++) {
                KogaLeadCalibrationHarnessTest.PlayerPair p = pool.get(i);
                int count = 0;
                if (p.name1().equals(cSpec)) count++;
                if (p.name2().equals(cSpec)) count++;
                candPairMatches[c][i] = count * 2;
            }
        }

        // Absolute best result across all 3 presets for each pair
        Turn1EvaluationResult[] absoluteBest = new Turn1EvaluationResult[pool.size()];
        String[] presetNames = new String[]{"default_sun", "anti_psychic", "anti_dark_steel"};
        for (int i = 0; i < pool.size(); i++) {
            Turn1EvaluationResult best = simResultsList.get(i).get("default_sun");
            for (int p = 1; p < 3; p++) {
                Turn1EvaluationResult cand = simResultsList.get(i).get(presetNames[p]);
                if (isBetter(cand, best)) {
                    best = cand;
                }
            }
            absoluteBest[i] = best;
        }

        List<SearchResult> all3PresetResults = new ArrayList<>();
        List<SearchResult> all2PresetResults = new ArrayList<>();

        // Candidate assignment options for each species:
        // 0: None, 1: default_sun, 2: anti_psychic, 3: anti_dark_steel
        // We test all 4 options for all 8 species: 4^8 = 65,536 combinations
        // Across base weights: bwSun in [-3..3], bwPsy in [-3..3], bwDs in [-3..3]
        // To keep search fast: bwDs = 1 (normalize), bwSun in [-3..3], bwPsy in [-3..3] => 49 weight combos
        // 49 * 65536 = 3,211,264 configs!
        int[] weights = new int[]{-2, -1, 0, 1, 2};

        long startTime = System.currentTimeMillis();
        int totalEvaluated3Preset = 0;
        int totalEvaluated2Preset = 0;

        SearchResult best3Preset = null;
        SearchResult best2Preset = null;
        int numSpecies = candidateSpecies.size();
        SearchResult[] best3ByTriggers = new SearchResult[numSpecies + 1];
        SearchResult[] best2ByTriggers = new SearchResult[numSpecies + 1];

        // Recursive generator for species assignments
        int[] currentAssignment = new int[numSpecies];

        // Search 3-Preset Core
        // Generate all 65,536 assignments
        int totalAssignments = (int) Math.pow(4, numSpecies);
        int[][] assignments = new int[totalAssignments][numSpecies];
        for (int a = 0; a < totalAssignments; a++) {
            int temp = a;
            for (int s = 0; s < numSpecies; s++) {
                assignments[a][s] = temp % 4;
                temp /= 4;
            }
        }

        System.out.printf("Searching %d species assignments x %d weight combos = %d configurations (3-Preset)...\n",
                totalAssignments, weights.length * weights.length * weights.length, totalAssignments * weights.length * weights.length * weights.length);

        for (int a = 0; a < totalAssignments; a++) {
            int[] assign = assignments[a];
            int triggersAdded = 0;
            for (int s = 0; s < numSpecies; s++) {
                if (assign[s] != 0) triggersAdded++;
            }

            // Precompute bonus for each preset from this assignment
            int[][] assignBonus = new int[pool.size()][3];
            for (int s = 0; s < numSpecies; s++) {
                int target = assign[s];
                if (target == 1) { // sun
                    for (int i = 0; i < pool.size(); i++) assignBonus[i][0] += candPairMatches[s][i];
                } else if (target == 2) { // psy
                    for (int i = 0; i < pool.size(); i++) assignBonus[i][1] += candPairMatches[s][i];
                } else if (target == 3) { // ds
                    for (int i = 0; i < pool.size(); i++) assignBonus[i][2] += candPairMatches[s][i];
                }
            }

            for (int bwDs : weights) {
                for (int bwSun : weights) {
                    for (int bwPsy : weights) {
                        totalEvaluated3Preset++;
                        int avoidableCat = 0;
                        int totalCat = 0;
                        int subopt = 0;
                        int viable = 0;
                        int agreed = 0;
                        int ties = 0;

                        for (int i = 0; i < pool.size(); i++) {
                            int score0 = baseOffDef[i][0] + bwSun + baseSpeciesBonus[i][0] + assignBonus[i][0];
                            int score1 = baseOffDef[i][1] + bwPsy + baseSpeciesBonus[i][1] + assignBonus[i][1];
                            int score2 = baseOffDef[i][2] + bwDs + baseSpeciesBonus[i][2] + assignBonus[i][2];

                            // Winner selection
                            int winner = 0;
                            int maxScore = score0;
                            int maxBw = bwSun;

                            if (score1 > maxScore || (score1 == maxScore && bwPsy > maxBw)) {
                                winner = 1;
                                maxScore = score1;
                                maxBw = bwPsy;
                            }
                            if (score2 > maxScore || (score2 == maxScore && bwDs > maxBw)) {
                                winner = 2;
                                maxScore = score2;
                                maxBw = bwDs;
                            }

                            Turn1EvaluationResult pickRes = simResultsList.get(i).get(presetNames[winner]);
                            Turn1EvaluationResult absBest = absoluteBest[i];

                            if (pickRes.verdict() == Turn1Verdict.CATASTROPHIC) {
                                totalCat++;
                                if (absBest.verdict() != Turn1Verdict.CATASTROPHIC) {
                                    avoidableCat++;
                                }
                            } else {
                                viable++;
                            }

                            if (pickRes.verdict() == absBest.verdict()) {
                                if (pickRes.kogaCasualties() == absBest.kogaCasualties()) {
                                    if (winner == 0 && absBest == simResultsList.get(i).get("default_sun")) agreed++;
                                    else ties++;
                                } else {
                                    subopt++;
                                }
                            } else {
                                subopt++;
                            }
                        }

                        // Check if better than best3Preset
                        SearchResult res = new SearchResult(
                                "3-Preset", bwSun, bwPsy, bwDs, assign,
                                avoidableCat, totalCat, subopt, viable, agreed, ties, triggersAdded, false
                        );

                        if (best3Preset == null || isBetterConfig(res, best3Preset)) {
                            best3Preset = res;
                        }
                        if (best3ByTriggers[triggersAdded] == null || isBetterWithinTriggerCount(res, best3ByTriggers[triggersAdded])) {
                            best3ByTriggers[triggersAdded] = res;
                        }
                    }
                }
            }
        }

        // =========================================================================
        // PART 3: SEARCH 2-PRESET CORE (DROP DEFAULT_SUN: PSYCHIC + DARK_STEEL)
        // =========================================================================
        System.out.println("Searching 2-Preset Core (Psychic + DarkSteel)...");
        // For 2-preset core, species can only be assigned to: 0: None, 2: anti_psychic, 3: anti_dark_steel (3^8 = 6,561)
        int total2PresetAssignments = (int) Math.pow(3, numSpecies);
        for (int a = 0; a < total2PresetAssignments; a++) {
            int temp = a;
            int[] assign = new int[numSpecies];
            int triggersAdded = 0;
            for (int s = 0; s < numSpecies; s++) {
                int val = temp % 3;
                temp /= 3;
                if (val == 0) assign[s] = 0; // None
                else if (val == 1) { assign[s] = 2; triggersAdded++; } // anti_psychic
                else { assign[s] = 3; triggersAdded++; } // anti_dark_steel
            }

            int[][] assignBonus = new int[pool.size()][3];
            for (int s = 0; s < numSpecies; s++) {
                int target = assign[s];
                if (target == 2) {
                    for (int i = 0; i < pool.size(); i++) assignBonus[i][1] += candPairMatches[s][i];
                } else if (target == 3) {
                    for (int i = 0; i < pool.size(); i++) assignBonus[i][2] += candPairMatches[s][i];
                }
            }

            int bwDs = 1;
            for (int bwPsy : weights) {
                totalEvaluated2Preset++;
                int avoidableCat = 0;
                int totalCat = 0;
                int subopt = 0;
                int viable = 0;
                int agreed = 0;
                int ties = 0;

                for (int i = 0; i < pool.size(); i++) {
                    int score1 = baseOffDef[i][1] + bwPsy + baseSpeciesBonus[i][1] + assignBonus[i][1];
                    int score2 = baseOffDef[i][2] + bwDs + baseSpeciesBonus[i][2] + assignBonus[i][2];

                    int winner = 1;
                    if (score2 > score1 || (score2 == score1 && bwDs > bwPsy)) {
                        winner = 2;
                    }

                    Turn1EvaluationResult pickRes = simResultsList.get(i).get(presetNames[winner]);
                    Turn1EvaluationResult absBest = absoluteBest[i];

                    if (pickRes.verdict() == Turn1Verdict.CATASTROPHIC) {
                        totalCat++;
                        if (absBest.verdict() != Turn1Verdict.CATASTROPHIC) {
                            avoidableCat++;
                        }
                    } else {
                        viable++;
                    }

                    if (pickRes.verdict() == absBest.verdict()) {
                        if (pickRes.kogaCasualties() == absBest.kogaCasualties()) {
                            agreed++;
                        } else {
                            subopt++;
                        }
                    } else {
                        subopt++;
                    }
                }

                SearchResult res = new SearchResult(
                        "2-Preset (No Sun)", 0, bwPsy, bwDs, assign,
                        avoidableCat, totalCat, subopt, viable, agreed, ties, triggersAdded, true
                );

                if (best2Preset == null || isBetterConfig(res, best2Preset)) {
                    best2Preset = res;
                }
                if (best2ByTriggers[triggersAdded] == null || isBetterWithinTriggerCount(res, best2ByTriggers[triggersAdded])) {
                    best2ByTriggers[triggersAdded] = res;
                }
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        System.out.printf("Search completed in %d ms! Evaluated %d (3-preset) + %d (2-preset) configurations.\n",
                duration, totalEvaluated3Preset, totalEvaluated2Preset);

        // =========================================================================
        // PART 4: COMPARATIVE REPORT & PARETO FRONTIER
        // =========================================================================
        System.out.println("\n========================================================================================");
        System.out.println("OPTIMIZATION RESULTS SUMMARY");
        System.out.println("========================================================================================");

        printConfigDetails("GLOBAL BEST 3-PRESET CONFIGURATION", best3Preset, candidateSpecies);
        printConfigDetails("GLOBAL BEST 2-PRESET CONFIGURATION (NO SUN)", best2Preset, candidateSpecies);

        System.out.println("\n========================================================================================");
        System.out.println("PARETO FRONTIER: 3-PRESET CORE BY TRIGGER COUNT");
        System.out.println("========================================================================================");
        System.out.printf("%-15s | Weights (S/P/D) | Avoidable Cat | Total Cat | Subopt | Viable | Added Triggers\n", "Triggers Added");
        System.out.println("------------------------------------------------------------------------------------------------------------------------");
        for (int t = 0; t <= numSpecies; t++) {
            SearchResult r = best3ByTriggers[t];
            if (r != null) {
                printParetoRow(t, r, candidateSpecies);
            }
        }

        System.out.println("\n========================================================================================");
        System.out.println("PARETO FRONTIER: 2-PRESET CORE (NO SUN) BY TRIGGER COUNT");
        System.out.println("========================================================================================");
        System.out.printf("%-15s | Weights (P/D)   | Avoidable Cat | Total Cat | Subopt | Viable | Added Triggers\n", "Triggers Added");
        System.out.println("------------------------------------------------------------------------------------------------------------------------");
        for (int t = 0; t <= numSpecies; t++) {
            SearchResult r = best2ByTriggers[t];
            if (r != null) {
                printParetoRow(t, r, candidateSpecies);
            }
        }
    }

    private boolean isBetterConfig(SearchResult a, SearchResult b) {
        if (a.avoidableCatastrophic() != b.avoidableCatastrophic()) {
            return a.avoidableCatastrophic() < b.avoidableCatastrophic();
        }
        if (a.totalCatastrophic() != b.totalCatastrophic()) {
            return a.totalCatastrophic() < b.totalCatastrophic();
        }
        if (a.suboptimal() != b.suboptimal()) {
            return a.suboptimal() < b.suboptimal();
        }
        if (a.viable() != b.viable()) {
            return a.viable() > b.viable();
        }
        return a.triggerCount() < b.triggerCount();
    }

    private boolean isBetterWithinTriggerCount(SearchResult a, SearchResult b) {
        if (a.avoidableCatastrophic() != b.avoidableCatastrophic()) {
            return a.avoidableCatastrophic() < b.avoidableCatastrophic();
        }
        if (a.totalCatastrophic() != b.totalCatastrophic()) {
            return a.totalCatastrophic() < b.totalCatastrophic();
        }
        if (a.suboptimal() != b.suboptimal()) {
            return a.suboptimal() < b.suboptimal();
        }
        return a.viable() > b.viable();
    }

    private void printParetoRow(int t, SearchResult r, List<String> candidateSpecies) {
        StringBuilder trList = new StringBuilder();
        for (int s = 0; s < candidateSpecies.size(); s++) {
            int target = r.speciesAssignment()[s];
            if (target != 0) {
                if (!trList.isEmpty()) trList.append(", ");
                trList.append(candidateSpecies.get(s)).append("->").append(target == 1 ? "sun" : (target == 2 ? "psy" : "ds"));
            }
        }
        if (r.is2Preset()) {
            System.out.printf("  %2d triggers    |   P=%2d, D=%2d   |     %2d / 51    |  %2d / 51  |   %2d   | %2d (%.1f%%) | %s\n",
                    t, r.bwPsy(), r.bwDs(), r.avoidableCatastrophic(), r.totalCatastrophic(), r.suboptimal(), r.viable(), r.viable() * 100.0 / 51, trList);
        } else {
            System.out.printf("  %2d triggers    | S=%2d,P=%2d,D=%2d |     %2d / 51    |  %2d / 51  |   %2d   | %2d (%.1f%%) | %s\n",
                    t, r.bwSun(), r.bwPsy(), r.bwDs(), r.avoidableCatastrophic(), r.totalCatastrophic(), r.suboptimal(), r.viable(), r.viable() * 100.0 / 51, trList);
        }
    }

    private void printConfigDetails(String title, SearchResult cfg, List<String> candidateSpecies) {
        System.out.println("----------------------------------------------------------------------------------------");
        System.out.println(title);
        System.out.println("----------------------------------------------------------------------------------------");
        System.out.printf("  Weights: sun=%d, psychic=%d, dark_steel=%d\n", cfg.bwSun(), cfg.bwPsy(), cfg.bwDs());
        System.out.printf("  Avoidable Catastrophic: %d / 51\n", cfg.avoidableCatastrophic());
        System.out.printf("  Total Catastrophic:     %d / 51\n", cfg.totalCatastrophic());
        System.out.printf("  Suboptimal Selections:  %d / 51\n", cfg.suboptimal());
        System.out.printf("  Viable Outcomes:        %d / 51 (%.1f%%)\n", cfg.viable(), cfg.viable() * 100.0 / 51);
        System.out.printf("  Candidate Triggers (%d added):\n", cfg.triggerCount());
        for (int s = 0; s < candidateSpecies.size(); s++) {
            int t = cfg.speciesAssignment()[s];
            String tName = switch (t) {
                case 1 -> "default_sun";
                case 2 -> "anti_psychic";
                case 3 -> "anti_dark_steel";
                default -> "NONE";
            };
            if (t != 0) {
                System.out.printf("    - %-12s -> %s\n", candidateSpecies.get(s), tName);
            }
        }
        System.out.println();
    }
}
