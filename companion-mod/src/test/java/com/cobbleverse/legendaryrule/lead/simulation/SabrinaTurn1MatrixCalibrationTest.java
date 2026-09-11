package com.cobbleverse.legendaryrule.lead.simulation;

import com.cobbleverse.legendaryrule.lead.*;
import com.cobbleverse.legendaryrule.lead.simulation.engine.Turn1LeadSimulator;
import com.cobbleverse.legendaryrule.lead.simulation.fixtures.SabrinaCompetitiveProfiles;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SabrinaTurn1MatrixCalibrationTest {

    private static TypeMatchupScorer scorer;
    private static LeadSelectionEngine engine;
    private static Turn1LeadSimulator simulator;
    private static List<RosterMemberTyping> sabrinaRoster;
    private static List<LeadAttempt> seedPresets;

    @BeforeAll
    static void setUp() {
        TypeChartData data = TypeChartResourceLoader.load().orElseThrow(
                () -> new IllegalStateException("Failed to load canonical Gen 9 type chart"));
        scorer = new TypeMatchupScorer(data);
        engine = new LeadSelectionEngine(scorer);
        simulator = new Turn1LeadSimulator();
        sabrinaRoster = createSabrinaRoster();

        seedPresets = List.of(
                new LeadAttempt("default_psychic", new int[]{0, 5}, 1, List.of(), "Indeedee-F + Mega Alakazam",
                        List.of(), List.of("machamp", "lucario")),
                new LeadAttempt("anti_dark_steel", new int[]{0, 1}, 0, List.of(), "Indeedee-F + Gallade",
                        List.of(), List.of("tyranitar", "weavile")),
                new LeadAttempt("field_independent", new int[]{2, 4}, -1, List.of(), "Garchomp + Rotom-Wash",
                        List.of(), List.of())
        );
    }

    public static List<RosterMemberTyping> createSabrinaRoster() {
        return List.of(
                new RosterMemberTyping(0, "indeedee", List.of("psychic", "normal")),
                new RosterMemberTyping(1, "gallade", List.of("psychic", "fighting")),
                new RosterMemberTyping(2, "garchomp", List.of("dragon", "ground")),
                new RosterMemberTyping(3, "metagross", List.of("steel", "psychic")),
                new RosterMemberTyping(4, "rotom", List.of("electric", "water")),
                new RosterMemberTyping(5, "alakazam", List.of("psychic"))
        );
    }

    public record PlayerPair(String name1, List<String> types1, String name2, List<String> types2, String category) {}

    public static List<PlayerPair> createRepresentativePool() {
        List<PlayerPair> pool = new ArrayList<>();

        // 1. Neutral Teams (4)
        pool.add(new PlayerPair("arcanine", List.of("fire"), "milotic", List.of("water"), "Neutral"));
        pool.add(new PlayerPair("snorlax", List.of("normal"), "clefable", List.of("fairy"), "Neutral"));
        pool.add(new PlayerPair("gyarados", List.of("water", "flying"), "raichu", List.of("electric"), "Neutral"));
        pool.add(new PlayerPair("machamp", List.of("fighting"), "lucario", List.of("fighting", "steel"), "Neutral/Fighting"));

        // 2. Dark Pressure (4)
        pool.add(new PlayerPair("chiyu", List.of("dark", "fire"), "fluttermane", List.of("ghost", "fairy"), "Dark/Special"));
        pool.add(new PlayerPair("chienpao", List.of("dark", "ice"), "dragonite", List.of("dragon", "flying"), "Dark/Physical"));
        pool.add(new PlayerPair("tinglu", List.of("dark", "ground"), "incineroar", List.of("fire", "dark"), "Dark/Bulky"));
        pool.add(new PlayerPair("kingambit", List.of("dark", "steel"), "chienpao", List.of("dark", "ice"), "Dark/Heavy"));

        // 3. Steel Pressure (4)
        pool.add(new PlayerPair("heatran", List.of("fire", "steel"), "gholdengo", List.of("steel", "ghost"), "Steel/Special"));
        pool.add(new PlayerPair("archaludon", List.of("steel", "dragon"), "corviknight", List.of("flying", "steel"), "Steel/Defensive"));
        pool.add(new PlayerPair("magnezone", List.of("electric", "steel"), "heatran", List.of("fire", "steel"), "Steel/Electric"));
        pool.add(new PlayerPair("kingambit", List.of("dark", "steel"), "archaludon", List.of("steel", "dragon"), "Steel/Mixed"));

        // 4. Rillaboom / Terrain Overwrite (4)
        pool.add(new PlayerPair("rillaboom", List.of("grass"), "heatran", List.of("fire", "steel"), "Terrain Overwrite/Grassy"));
        pool.add(new PlayerPair("rillaboom", List.of("grass"), "urshifu", List.of("fighting", "water"), "Terrain Overwrite/Grassy"));
        pool.add(new PlayerPair("rillaboom", List.of("grass"), "incineroar", List.of("fire", "dark"), "Terrain Overwrite/Grassy"));
        pool.add(new PlayerPair("pincurchin", List.of("electric"), "raichu", List.of("electric"), "Terrain Overwrite/Electric"));

        // 5. Anti-Psychic Compositions (4)
        pool.add(new PlayerPair("kingambit", List.of("dark", "steel"), "chiyu", List.of("dark", "fire"), "Anti-Psychic/Dark"));
        pool.add(new PlayerPair("dragapult", List.of("dragon", "ghost"), "gengar", List.of("ghost", "poison"), "Anti-Psychic/Ghost"));
        pool.add(new PlayerPair("incineroar", List.of("fire", "dark"), "tyranitar", List.of("rock", "dark"), "Anti-Psychic/Dark"));
        pool.add(new PlayerPair("tinglu", List.of("dark", "ground"), "gholdengo", List.of("steel", "ghost"), "Anti-Psychic/Bulky"));

        // 6. Ground Threats (4)
        pool.add(new PlayerPair("greattusk", List.of("ground", "fighting"), "garchomp", List.of("dragon", "ground"), "Ground/Offense"));
        pool.add(new PlayerPair("landorus", List.of("ground", "flying"), "excadrill", List.of("ground", "steel"), "Ground/Sand"));
        pool.add(new PlayerPair("gliscor", List.of("ground", "flying"), "mamoswine", List.of("ice", "ground"), "Ground/Ice"));
        pool.add(new PlayerPair("gastrodon", List.of("water", "ground"), "swampert", List.of("water", "ground"), "Ground/Rain"));

        // 7. Fairy Threats (3)
        pool.add(new PlayerPair("fluttermane", List.of("ghost", "fairy"), "togekiss", List.of("fairy", "flying"), "Fairy/Special"));
        pool.add(new PlayerPair("clefable", List.of("fairy"), "ninetales", List.of("ice", "fairy"), "Fairy/Snow"));
        pool.add(new PlayerPair("fluttermane", List.of("ghost", "fairy"), "clefable", List.of("fairy"), "Fairy/Bulky"));

        // 8. Fast Offensive Leads (3)
        pool.add(new PlayerPair("dragapult", List.of("dragon", "ghost"), "urshifu", List.of("fighting", "water"), "Fast Offense"));
        pool.add(new PlayerPair("tornadus", List.of("flying"), "urshifu", List.of("fighting", "water"), "Fast Offense/Tailwind"));
        pool.add(new PlayerPair("talonflame", List.of("fire", "flying"), "garchomp", List.of("dragon", "ground"), "Fast Offense/Tailwind"));

        // 9. Trick Room (3)
        pool.add(new PlayerPair("cresselia", List.of("psychic"), "ursaluna", List.of("ground", "normal"), "Trick Room"));
        pool.add(new PlayerPair("hatterene", List.of("psychic", "fairy"), "torkoal", List.of("fire"), "Trick Room/Sun"));
        pool.add(new PlayerPair("farigiraf", List.of("normal", "psychic"), "ursaluna", List.of("ground", "normal"), "Trick Room"));

        // 10. Priority-Heavy Teams (2)
        pool.add(new PlayerPair("dragonite", List.of("dragon", "flying"), "arcanine", List.of("fire"), "Priority/ESpeed"));
        pool.add(new PlayerPair("chienpao", List.of("dark", "ice"), "dragonite", List.of("dragon", "flying"), "Priority/Ruin"));

        // 11. Common Mixed Cores (4)
        pool.add(new PlayerPair("pelipper", List.of("water", "flying"), "archaludon", List.of("steel", "dragon"), "Rain"));
        pool.add(new PlayerPair("torkoal", List.of("fire"), "lilligant", List.of("grass", "fighting"), "Sun"));
        pool.add(new PlayerPair("tyranitar", List.of("rock", "dark"), "excadrill", List.of("ground", "steel"), "Sand"));
        pool.add(new PlayerPair("abomasnow", List.of("grass", "ice"), "baxcalibur", List.of("dragon", "ice"), "Snow"));

        return pool;
    }

    public record CaseRecord(
            int index,
            String category,
            String pairName,
            List<CompetitivePokemonProfile> playerProfiles,
            String engineWinner,
            String simPreferred,
            Map<String, Turn1EvaluationResult> simResults,
            String status,
            String decisiveReason
    ) {}

    public static boolean isBetter(Turn1EvaluationResult cand, Turn1EvaluationResult currentBest) {
        if (cand == null) return false;
        if (currentBest == null) return true;

        if (cand.verdict() != currentBest.verdict()) {
            return rankVerdict(cand.verdict()) > rankVerdict(currentBest.verdict());
        }
        if (cand.kogaCasualties() != currentBest.kogaCasualties()) {
            return cand.kogaCasualties() < currentBest.kogaCasualties();
        }
        if (cand.kogaKnockoutsScored() != currentBest.kogaKnockoutsScored()) {
            return cand.kogaKnockoutsScored() > currentBest.kogaKnockoutsScored();
        }
        return cand.minKogaHpRemainingPercent() > currentBest.minKogaHpRemainingPercent();
    }

    private static int rankVerdict(Turn1Verdict v) {
        return switch (v) {
            case GOOD -> 2;
            case QUESTIONABLE -> 1;
            case CATASTROPHIC -> 0;
        };
    }

    @Test
    @DisplayName("Run 39-Pair Simulation and Systematic Calibration Grid Search")
    void run39PairMatrixAndGridSearch() throws IOException {
        List<PlayerPair> pool = createRepresentativePool();
        List<Map<String, Turn1EvaluationResult>> simResultsList = new ArrayList<>();
        List<List<PlayerLeadTyping>> typingsList = new ArrayList<>();

        System.out.println("========================================================================================");
        System.out.println("PRECOMPUTING TURN-1 SIMULATION MATRIX FOR ALL 39 REPRESENTATIVE PAIRS");
        System.out.println("========================================================================================");

        List<String> presetIds = List.of("default_psychic", "anti_dark_steel", "field_independent");

        for (int i = 0; i < pool.size(); i++) {
            PlayerPair pair = pool.get(i);
            CompetitivePokemonProfile prof1 = ThreatPoolCompetitiveProfiles.getProfile(pair.name1());
            CompetitivePokemonProfile prof2 = ThreatPoolCompetitiveProfiles.getProfile(pair.name2());
            assertNotNull(prof1, "Profile missing for: " + pair.name1());
            assertNotNull(prof2, "Profile missing for: " + pair.name2());
            List<CompetitivePokemonProfile> playerLeads = List.of(prof1, prof2);

            Map<String, Turn1EvaluationResult> rMap = new LinkedHashMap<>();
            for (String pid : presetIds) {
                rMap.put(pid, simulator.simulateSabrinaPreset(playerLeads, pid, pair.name1() + " + " + pair.name2()));
            }
            simResultsList.add(rMap);
            typingsList.add(List.of(
                    new PlayerLeadTyping(pair.name1(), pair.types1()),
                    new PlayerLeadTyping(pair.name2(), pair.types2())
            ));
        }

        // Evaluate Seed Configuration
        System.out.println("\n--- EVALUATING SEED CONFIGURATION ---");
        evaluateConfig("Seed Config", seedPresets, pool, typingsList, simResultsList, true);

        // Systematic Search: Base Weights & Species Triggers
        System.out.println("\n========================================================================================");
        System.out.println("SYSTEMATIC GRID SEARCH FOR OPTIMAL SABRINA PRESET CALIBRATION");
        System.out.println("========================================================================================");

        record EvaluatedConfig(
                String label,
                int bwDefault, int bwDarkSteel, int bwFieldIndep,
                List<String> dsSpecies,
                List<String> fiSpecies,
                int avoidableCatastrophic,
                int totalCatastrophic,
                int suboptimal,
                int viable,
                int agreed,
                int ties,
                int defaultPicks,
                int dsPicks,
                int fiPicks
        ) {}

        List<EvaluatedConfig> searchResults = new ArrayList<>();

        // Search base weights in valid engine range [-2, 2]
        for (int bwDef = 0; bwDef <= 2; bwDef++) {
            for (int bwDs = -1; bwDs <= 1; bwDs++) {
                for (int bwFi = -2; bwFi <= 0; bwFi++) {

                    List<List<String>> dsTriggerSets = List.of(
                            List.of(),
                            List.of("tyranitar", "kingambit"),
                            List.of("tyranitar", "kingambit", "chienpao"),
                            List.of("tyranitar", "kingambit", "incineroar"),
                            List.of("tyranitar", "kingambit", "chienpao", "incineroar")
                    );

                    List<List<String>> fiTriggerSets = List.of(
                            List.of(),
                            List.of("rillaboom"),
                            List.of("rillaboom", "heatran"),
                            List.of("rillaboom", "heatran", "pincurchin")
                    );

                    for (List<String> dsTriggers : dsTriggerSets) {
                        for (List<String> fiTriggers : fiTriggerSets) {
                            List<LeadAttempt> testPresets = List.of(
                                    new LeadAttempt("default_psychic", new int[]{0, 5}, bwDef, List.of(), "", List.of(), List.of()),
                                    new LeadAttempt("anti_dark_steel", new int[]{0, 1}, bwDs, List.of(), "", List.of(), dsTriggers),
                                    new LeadAttempt("field_independent", new int[]{2, 4}, bwFi, List.of(), "", List.of(), fiTriggers)
                            );

                            int agreed = 0, ties = 0, sub = 0, cat = 0, avoidCat = 0;
                            int defP = 0, dsP = 0, fiP = 0;

                            for (int i = 0; i < pool.size(); i++) {
                                List<PlayerLeadTyping> typings = typingsList.get(i);
                                Map<String, Turn1EvaluationResult> simMap = simResultsList.get(i);

                                LeadSelectionResult sel = engine.select(testPresets, typings, sabrinaRoster);
                                String pick = sel.selectedAttempt().id();
                                if ("default_psychic".equals(pick)) defP++;
                                else if ("anti_dark_steel".equals(pick)) dsP++;
                                else if ("field_independent".equals(pick)) fiP++;

                                String simPref = "default_psychic";
                                Turn1EvaluationResult best = simMap.get(simPref);
                                for (String pid : List.of("anti_dark_steel", "field_independent")) {
                                    Turn1EvaluationResult cr = simMap.get(pid);
                                    if (isBetter(cr, best)) {
                                        simPref = pid;
                                        best = cr;
                                    }
                                }

                                Turn1EvaluationResult pickRes = simMap.get(pick);
                                if (pick.equals(simPref)) {
                                    agreed++;
                                } else if (pickRes.verdict() == best.verdict() && pickRes.kogaCasualties() == best.kogaCasualties()) {
                                    ties++;
                                } else if (pickRes.verdict() == Turn1Verdict.CATASTROPHIC) {
                                    cat++;
                                    if (best.verdict() != Turn1Verdict.CATASTROPHIC) {
                                        avoidCat++;
                                    }
                                } else {
                                    sub++;
                                }
                            }

                            String label = String.format("bw=(%d,%d,%d) dsTrig=%d fiTrig=%d", bwDef, bwDs, bwFi, dsTriggers.size(), fiTriggers.size());
                            searchResults.add(new EvaluatedConfig(
                                    label, bwDef, bwDs, bwFi, dsTriggers, fiTriggers,
                                    avoidCat, cat, sub, (agreed + ties), agreed, ties, defP, dsP, fiP
                            ));
                        }
                    }
                }
            }
        }

        // Sort by optimization priority:
        searchResults.sort(Comparator
                .comparingInt(EvaluatedConfig::avoidableCatastrophic)
                .thenComparingInt(EvaluatedConfig::totalCatastrophic)
                .thenComparingInt(EvaluatedConfig::suboptimal)
                .thenComparing(Comparator.comparingInt(EvaluatedConfig::viable).reversed())
                .thenComparingInt(c -> c.dsSpecies().size() + c.fiSpecies().size())
                .thenComparing(Comparator.comparingInt(EvaluatedConfig::defaultPicks).reversed())
        );

        System.out.printf("%-40s | AvoidCat | TotCat | Subopt | Viable (%%) | Agreed | Ties | Picks (Def/DS/FI) | Triggers\n", "Configuration");
        System.out.println("----------------------------------------------------------------------------------------------------------------");
        for (int i = 0; i < Math.min(15, searchResults.size()); i++) {
            EvaluatedConfig c = searchResults.get(i);
            System.out.printf("%-40s |    %2d    |   %2d   |   %2d   | %2d (%.1f%%) |   %2d   |  %2d  | (%2d / %2d / %2d)  | DS:%s FI:%s\n",
                    c.label(), c.avoidableCatastrophic(), c.totalCatastrophic(), c.suboptimal(),
                    c.viable(), c.viable() * 100.0 / pool.size(), c.agreed(), c.ties(),
                    c.defaultPicks(), c.dsPicks(), c.fiPicks(),
                    c.dsSpecies(), c.fiSpecies());
        }

        EvaluatedConfig top = searchResults.get(0);
        System.out.println("\n=== TOP RECOMMENDED CANDIDATE ===");
        System.out.println("Label: " + top.label());
        System.out.println("BaseWeights: default_psychic=" + top.bwDefault() + ", anti_dark_steel=" + top.bwDarkSteel() + ", field_independent=" + top.bwFieldIndep());
        System.out.println("anti_dark_steel favoredAgainstSpecies: " + top.dsSpecies());
        System.out.println("field_independent favoredAgainstSpecies: " + top.fiSpecies());
        System.out.println("Avoidable Catastrophic: " + top.avoidableCatastrophic());
        System.out.println("Suboptimal: " + top.suboptimal());
        System.out.println("Viable: " + top.viable() + " / " + pool.size());

        List<LeadAttempt> topPresets = List.of(
                new LeadAttempt("default_psychic", new int[]{0, 5}, top.bwDefault(), List.of(), "Indeedee-F + Mega Alakazam", List.of(), List.of()),
                new LeadAttempt("anti_dark_steel", new int[]{0, 1}, top.bwDarkSteel(), List.of(), "Indeedee-F + Gallade", List.of(), top.dsSpecies()),
                new LeadAttempt("field_independent", new int[]{2, 4}, top.bwFieldIndep(), List.of(), "Garchomp + Rotom-Wash", List.of(), top.fiSpecies())
        );

        evaluateConfig("Top Recommended Config", topPresets, pool, typingsList, simResultsList, false);

        assertEquals(0, top.avoidableCatastrophic(), "Top candidate MUST achieve 0 avoidable catastrophic picks!");
    }

    private void evaluateConfig(
            String title,
            List<LeadAttempt> presets,
            List<PlayerPair> pool,
            List<List<PlayerLeadTyping>> typingsList,
            List<Map<String, Turn1EvaluationResult>> simResultsList,
            boolean printCases
    ) {
        int agreed = 0, ties = 0, sub = 0, cat = 0, avoidCat = 0;
        Map<String, Integer> pickCounts = new LinkedHashMap<>();
        for (LeadAttempt a : presets) pickCounts.put(a.id(), 0);

        for (int i = 0; i < pool.size(); i++) {
            PlayerPair pair = pool.get(i);
            List<PlayerLeadTyping> typings = typingsList.get(i);
            Map<String, Turn1EvaluationResult> simMap = simResultsList.get(i);

            LeadSelectionResult sel = engine.select(presets, typings, sabrinaRoster);
            String pick = sel.selectedAttempt().id();
            pickCounts.put(pick, pickCounts.get(pick) + 1);

            String simPref = presets.get(0).id();
            Turn1EvaluationResult best = simMap.get(simPref);
            for (int p = 1; p < presets.size(); p++) {
                String pid = presets.get(p).id();
                Turn1EvaluationResult cr = simMap.get(pid);
                if (isBetter(cr, best)) {
                    simPref = pid;
                    best = cr;
                }
            }

            Turn1EvaluationResult pickRes = simMap.get(pick);
            String status;
            if (pick.equals(simPref)) {
                status = "AGREED";
                agreed++;
            } else if (pickRes.verdict() == best.verdict() && pickRes.kogaCasualties() == best.kogaCasualties()) {
                status = "ACCEPTABLE_TIE";
                ties++;
            } else if (pickRes.verdict() == Turn1Verdict.CATASTROPHIC) {
                cat++;
                if (best.verdict() != Turn1Verdict.CATASTROPHIC) {
                    avoidCat++;
                    status = "AVOIDABLE_CATASTROPHIC";
                } else {
                    status = "UNAVOIDABLE_CATASTROPHIC";
                }
            } else {
                status = "SUBOPTIMAL";
                sub++;
            }

            if (printCases || "AVOIDABLE_CATASTROPHIC".equals(status)) {
                System.out.printf("  Case #%02d [%s] %s + %s -> Picked: %s (Verdict: %s) | SimPref: %s (Verdict: %s) [%s]\n",
                        i + 1, pair.category(), pair.name1(), pair.name2(), pick, pickRes.verdict(), simPref, best.verdict(), status);
            }
        }

        System.out.printf("[%s Summary] Viable: %d/39 (%.1f%%) | Agreed: %d | Ties: %d | Suboptimal: %d | AvoidCat: %d | TotCat: %d | Picks: %s\n",
                title, (agreed + ties), (agreed + ties) * 100.0 / pool.size(), agreed, ties, sub, avoidCat, cat, pickCounts);
    }
}
