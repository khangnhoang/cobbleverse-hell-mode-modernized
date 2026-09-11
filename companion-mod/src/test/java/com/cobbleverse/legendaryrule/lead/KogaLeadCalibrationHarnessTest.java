package com.cobbleverse.legendaryrule.lead;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class KogaLeadCalibrationHarnessTest {

    private static TypeMatchupScorer scorer;
    private static LeadSelectionEngine engine;

    @BeforeAll
    static void setUp() {
        TypeChartData data = TypeChartResourceLoader.load().orElseThrow(
                () -> new IllegalStateException("Failed to load canonical Gen 9 type chart"));
        scorer = new TypeMatchupScorer(data);
        engine = new LeadSelectionEngine(scorer);
    }

    public static List<RosterMemberTyping> createKogaRoster() {
        return List.of(
                new RosterMemberTyping(0, "beedrill", List.of("bug", "poison")),
                new RosterMemberTyping(1, "venusaur", List.of("grass", "poison")),
                new RosterMemberTyping(2, "okidogi", List.of("poison", "fighting")),
                new RosterMemberTyping(3, "sneasler", List.of("fighting", "poison")),
                new RosterMemberTyping(4, "amoonguss", List.of("grass", "poison")),
                new RosterMemberTyping(5, "slowking", List.of("poison", "psychic"))
        );
    }

    public static List<LeadAttempt> createRound1Presets() {
        List<String> psychicSpecies = List.of(
                "indeedee", "armarouge", "hatterene", "alakazam", "farigiraf", "espathra", "cresselia", "ironcrown"
        );
        List<String> darkSteelSpecies = List.of(
                "heatran", "kingambit", "chiyu", "chienpao", "incineroar", "archaludon"
        );
        List<String> gholdengoSpecies = List.of(
                "gholdengo"
        );

        return List.of(
                new LeadAttempt("default_sun", new int[]{5, 1}, 1, List.of(), "Slowking + Venusaur", List.of(), List.of()),
                new LeadAttempt("anti_psychic", new int[]{0, 5}, 0, List.of(), "Beedrill + Slowking", List.of(), psychicSpecies),
                new LeadAttempt("anti_dark_steel", new int[]{2, 3}, 0, List.of(), "Okidogi + Sneasler", List.of(), darkSteelSpecies),
                new LeadAttempt("anti_gholdengo", new int[]{0, 2}, 0, List.of(), "Beedrill + Okidogi", List.of(), gholdengoSpecies)
        );
    }

    public record PlayerPair(String name1, List<String> types1, String name2, List<String> types2, String category) {}

    public static List<PlayerPair> createTestPool() {
        List<PlayerPair> pool = new ArrayList<>();

        // 1. Psychic / Psyspam
        pool.add(new PlayerPair("indeedee", List.of("psychic", "normal"), "armarouge", List.of("fire", "psychic"), "Psychic"));
        pool.add(new PlayerPair("indeedee", List.of("psychic", "normal"), "hatterene", List.of("psychic", "fairy"), "Psychic"));
        pool.add(new PlayerPair("farigiraf", List.of("normal", "psychic"), "heatran", List.of("fire", "steel"), "Psychic/Steel"));
        pool.add(new PlayerPair("cresselia", List.of("psychic"), "heatran", List.of("fire", "steel"), "Psychic/Steel"));
        pool.add(new PlayerPair("ironcrown", List.of("steel", "psychic"), "indeedee", List.of("psychic", "normal"), "Psychic"));
        pool.add(new PlayerPair("alakazam", List.of("psychic"), "gholdengo", List.of("steel", "ghost"), "Psychic/Ghost"));

        // 2. Dark / Ghost
        pool.add(new PlayerPair("chiyu", List.of("dark", "fire"), "fluttermane", List.of("ghost", "fairy"), "Dark/Ghost"));
        pool.add(new PlayerPair("chienpao", List.of("dark", "ice"), "dragonite", List.of("dragon", "flying"), "Dark/Flying"));
        pool.add(new PlayerPair("incineroar", List.of("fire", "dark"), "gholdengo", List.of("steel", "ghost"), "Dark/Ghost"));
        pool.add(new PlayerPair("kingambit", List.of("dark", "steel"), "cresselia", List.of("psychic"), "Dark/Psychic"));
        pool.add(new PlayerPair("gholdengo", List.of("steel", "ghost"), "chiyu", List.of("dark", "fire"), "Ghost/Dark"));

        // 3. Steel
        pool.add(new PlayerPair("heatran", List.of("fire", "steel"), "landorus", List.of("ground", "flying"), "Steel/Ground"));
        pool.add(new PlayerPair("gholdengo", List.of("steel", "ghost"), "landorus", List.of("ground", "flying"), "Steel/Ground"));
        pool.add(new PlayerPair("archaludon", List.of("steel", "dragon"), "pelipper", List.of("water", "flying"), "Steel/Rain"));
        pool.add(new PlayerPair("kingambit", List.of("dark", "steel"), "indeedee", List.of("psychic", "normal"), "Steel/Psychic"));
        pool.add(new PlayerPair("ironcrown", List.of("steel", "psychic"), "tornadus", List.of("flying"), "Steel/Flying"));
        pool.add(new PlayerPair("magnezone", List.of("electric", "steel"), "heatran", List.of("fire", "steel"), "Steel"));
        pool.add(new PlayerPair("corviknight", List.of("flying", "steel"), "archaludon", List.of("steel", "dragon"), "Steel"));
        pool.add(new PlayerPair("skarmory", List.of("steel", "flying"), "gholdengo", List.of("steel", "ghost"), "Steel"));

        // 4. Ground
        pool.add(new PlayerPair("garchomp", List.of("dragon", "ground"), "greattusk", List.of("ground", "fighting"), "Ground"));
        pool.add(new PlayerPair("gliscor", List.of("ground", "flying"), "heatran", List.of("fire", "steel"), "Ground/Steel"));
        pool.add(new PlayerPair("mamoswine", List.of("ice", "ground"), "chienpao", List.of("dark", "ice"), "Ground/Ice"));
        pool.add(new PlayerPair("tinglu", List.of("dark", "ground"), "fluttermane", List.of("ghost", "fairy"), "Ground/Ghost"));
        pool.add(new PlayerPair("excadrill", List.of("ground", "steel"), "tyranitar", List.of("rock", "dark"), "Ground/Sand"));
        pool.add(new PlayerPair("gastrodon", List.of("water", "ground"), "pelipper", List.of("water", "flying"), "Ground/Rain"));
        pool.add(new PlayerPair("swampert", List.of("water", "ground"), "pelipper", List.of("water", "flying"), "Ground/Rain"));
        pool.add(new PlayerPair("ursaluna", List.of("ground", "normal"), "cresselia", List.of("psychic"), "Ground/TrickRoom"));
        pool.add(new PlayerPair("garchomp", List.of("dragon", "ground"), "tinglu", List.of("dark", "ground"), "Ground"));

        // 5. Flying
        pool.add(new PlayerPair("talonflame", List.of("fire", "flying"), "garchomp", List.of("dragon", "ground"), "Flying/Ground"));
        pool.add(new PlayerPair("charizard", List.of("fire", "flying"), "venusaur", List.of("grass", "poison"), "Flying/Fire"));
        pool.add(new PlayerPair("dragonite", List.of("dragon", "flying"), "chienpao", List.of("dark", "ice"), "Flying/Dark"));
        pool.add(new PlayerPair("salamence", List.of("dragon", "flying"), "incineroar", List.of("fire", "dark"), "Flying/Dark"));
        pool.add(new PlayerPair("tornadus", List.of("flying"), "gholdengo", List.of("steel", "ghost"), "Flying/Ghost"));
        pool.add(new PlayerPair("talonflame", List.of("fire", "flying"), "charizard", List.of("fire", "flying"), "Flying/Fire"));

        // 6. Weather
        pool.add(new PlayerPair("pelipper", List.of("water", "flying"), "archaludon", List.of("steel", "dragon"), "Weather/Rain"));
        pool.add(new PlayerPair("politoed", List.of("water"), "urshifu", List.of("fighting", "dark"), "Weather/Rain"));
        pool.add(new PlayerPair("tyranitar", List.of("rock", "dark"), "excadrill", List.of("ground", "steel"), "Weather/Sand"));
        pool.add(new PlayerPair("hippowdon", List.of("ground"), "garchomp", List.of("dragon", "ground"), "Weather/Sand"));
        pool.add(new PlayerPair("abomasnow", List.of("grass", "ice"), "ironcrown", List.of("steel", "psychic"), "Weather/Snow"));
        pool.add(new PlayerPair("ninetales", List.of("ice", "fairy"), "baxcalibur", List.of("dragon", "ice"), "Weather/Snow"));
        pool.add(new PlayerPair("torkoal", List.of("fire"), "lilligant", List.of("grass"), "Weather/Sun"));
        pool.add(new PlayerPair("pelipper", List.of("water", "flying"), "swampert", List.of("water", "ground"), "Weather/Rain"));

        // 7. Neutral / Control
        pool.add(new PlayerPair("rillaboom", List.of("grass"), "incineroar", List.of("fire", "dark"), "Neutral"));
        pool.add(new PlayerPair("lucario", List.of("fighting", "steel"), "rotom", List.of("electric", "water"), "Neutral"));
        pool.add(new PlayerPair("metagross", List.of("steel", "psychic"), "milotic", List.of("water"), "Neutral"));
        pool.add(new PlayerPair("amoonguss", List.of("grass", "poison"), "arcanine", List.of("fire"), "Neutral"));
        pool.add(new PlayerPair("raichu", List.of("electric"), "gyarados", List.of("water", "flying"), "Neutral"));
        pool.add(new PlayerPair("clefable", List.of("fairy"), "arcanine", List.of("fire"), "Neutral"));
        pool.add(new PlayerPair("snorlax", List.of("normal"), "gengar", List.of("ghost", "poison"), "Neutral"));
        pool.add(new PlayerPair("lapras", List.of("water", "ice"), "machamp", List.of("fighting"), "Neutral"));
        pool.add(new PlayerPair("togekiss", List.of("fairy", "flying"), "dragapult", List.of("dragon", "ghost"), "Neutral"));

        return pool;
    }

    public static String runCalibration(String roundName, List<LeadAttempt> presets) {
        StringBuilder sb = new StringBuilder();
        List<RosterMemberTyping> roster = createKogaRoster();
        List<PlayerPair> pool = createTestPool();

        sb.append("======================================================================\n");
        sb.append("KOGA DYNAMIC LEAD CALIBRATION: ").append(roundName).append("\n");
        sb.append("Presets count: ").append(presets.size()).append("\n");
        for (LeadAttempt a : presets) {
            sb.append("  [").append(a.id()).append("] slots=").append(Arrays.toString(a.leadSlots()))
                    .append(" bw=").append(a.baseWeight())
                    .append(" speciesFavored=").append(a.favoredAgainstSpecies()).append("\n");
        }
        sb.append("======================================================================\n\n");

        int totalCases = pool.size();
        int defaultWins = 0;
        int specializedWins = 0;
        int tieCount = 0;
        double totalMargin = 0.0;
        int minMargin = Integer.MAX_VALUE;

        Map<String, Integer> winCountByPreset = new LinkedHashMap<>();
        for (LeadAttempt a : presets) {
            winCountByPreset.put(a.id(), 0);
        }

        List<String> caseSummaries = new ArrayList<>();

        for (int i = 0; i < pool.size(); i++) {
            PlayerPair pair = pool.get(i);
            List<PlayerLeadTyping> playerLeads = List.of(
                    new PlayerLeadTyping(pair.name1, pair.types1),
                    new PlayerLeadTyping(pair.name2, pair.types2)
            );

            LeadSelectionResult result = engine.select(presets, playerLeads, roster);
            LeadAttempt winner = result.selectedAttempt();
            List<AttemptScore> scores = result.evaluatedScores();

            // Sort scores to determine ranks and margin
            List<AttemptScore> sortedScores = new ArrayList<>(scores);
            sortedScores.sort((a, b) -> {
                int cmp = Integer.compare(b.totalScore(), a.totalScore());
                if (cmp != 0) return cmp;
                int bwCmp = Integer.compare(b.baseWeight(), a.baseWeight());
                if (bwCmp != 0) return bwCmp;
                return 0; // engine uses declaration index as 3rd tie-breaker
            });

            AttemptScore first = sortedScores.get(0);
            AttemptScore second = sortedScores.size() > 1 ? sortedScores.get(1) : null;
            int margin = second != null ? first.totalScore() - second.totalScore() : 0;

            if (second != null && first.totalScore() == second.totalScore()) {
                tieCount++;
            }
            totalMargin += margin;
            if (margin < minMargin) {
                minMargin = margin;
            }

            if ("default_sun".equals(winner.id())) {
                defaultWins++;
            } else {
                specializedWins++;
            }
            winCountByPreset.put(winner.id(), winCountByPreset.getOrDefault(winner.id(), 0) + 1);

            sb.append(String.format("CASE #%02d [%s] PLAYER: %s (%s) + %s (%s)\n",
                    (i + 1), pair.category, pair.name1, String.join("/", pair.types1),
                    pair.name2, String.join("/", pair.types2)));

            for (int r = 0; r < sortedScores.size(); r++) {
                AttemptScore s = sortedScores.get(r);
                sb.append(String.format("  [%d] %-20s total=%+2d (off=%+2d, def=%+2d, bw=%+2d, type=%+2d, spec=%+2d)\n",
                        (r + 1), s.attemptId(), s.totalScore(), s.offensiveScore(), s.defensiveScore(),
                        s.baseWeight(), s.typeFavoredBonus(), s.speciesFavoredBonus()));
            }

            sb.append(String.format("  -> WINNER: %s (margin=%+d)\n\n", winner.id(), margin));

            caseSummaries.add(String.format("#%02d %s+%s -> %s (margin %d)",
                    (i + 1), pair.name1, pair.name2, winner.id(), margin));
        }

        double avgMargin = totalCases > 0 ? totalMargin / totalCases : 0.0;

        sb.append("======================================================================\n");
        sb.append("CALIBRATION SUMMARY METRICS: ").append(roundName).append("\n");
        sb.append("======================================================================\n");
        sb.append("Total cases:           ").append(totalCases).append("\n");
        sb.append("Default wins:          ").append(defaultWins).append(" (")
                .append(String.format("%.1f%%", defaultWins * 100.0 / totalCases)).append(")\n");
        sb.append("Specialized wins:      ").append(specializedWins).append(" (")
                .append(String.format("%.1f%%", specializedWins * 100.0 / totalCases)).append(")\n");
        sb.append("Wins by preset:\n");
        for (Map.Entry<String, Integer> e : winCountByPreset.entrySet()) {
            sb.append(String.format("  - %-20s: %d\n", e.getKey(), e.getValue()));
        }
        sb.append("Tie count (raw score): ").append(tieCount).append("\n");
        sb.append("Average winner margin: ").append(String.format("%.2f", avgMargin)).append("\n");
        sb.append("Minimum winner margin: ").append(minMargin).append("\n");
        sb.append("======================================================================\n");

        return sb.toString();
    }

    public static List<LeadAttempt> createRound2Presets() {
        List<String> psychicSpecies = List.of(
                "indeedee", "armarouge", "hatterene", "alakazam", "farigiraf", "espathra", "cresselia", "ironcrown"
        );
        List<String> darkSteelSpecies = List.of(
                "heatran", "kingambit", "chiyu", "chienpao", "incineroar"
        );

        return List.of(
                new LeadAttempt("default_sun", new int[]{5, 1}, 1, List.of(), "Slowking + Venusaur", List.of(), List.of()),
                new LeadAttempt("anti_psychic", new int[]{0, 5}, 0, List.of(), "Beedrill + Slowking", List.of(), psychicSpecies),
                new LeadAttempt("anti_dark_steel", new int[]{2, 3}, -1, List.of(), "Okidogi + Sneasler", List.of(), darkSteelSpecies)
        );
    }

    public static List<LeadAttempt> createRound3Presets() {
        List<String> psychicSpecies = List.of(
                "indeedee", "armarouge", "hatterene", "alakazam", "farigiraf", "cresselia", "ironcrown"
        );
        List<String> darkSteelSpecies = List.of(
                "heatran", "kingambit", "chiyu", "chienpao", "incineroar"
        );
        List<String> rainSpecies = List.of(
                "pelipper", "politoed"
        );

        return List.of(
                new LeadAttempt("default_sun", new int[]{5, 1}, 2, List.of(), "Slowking + Venusaur", List.of(), rainSpecies),
                new LeadAttempt("anti_psychic", new int[]{0, 5}, 0, List.of(), "Beedrill + Slowking", List.of(), psychicSpecies),
                new LeadAttempt("anti_dark_steel", new int[]{2, 3}, -1, List.of(), "Okidogi + Sneasler", List.of(), darkSteelSpecies)
        );
    }

    @Test
    void testCalibrationRound1() throws IOException {
        List<LeadAttempt> r1Presets = createRound1Presets();
        String output = runCalibration("ROUND 1 (Initial Proposal)", r1Presets);
        Path logPath = Path.of("../reports/koga_calibration_round1.txt");
        Files.createDirectories(logPath.getParent());
        Files.writeString(logPath, output);
        assertNotNull(output);
    }

    @Test
    void testCalibrationRound2() throws IOException {
        List<LeadAttempt> r2Presets = createRound2Presets();
        String output = runCalibration("ROUND 2 (Dropped P3, BW=-1 on P2, Removed Archaludon)", r2Presets);
        System.out.println(output);
        Path logPath = Path.of("../reports/koga_calibration_round2.txt");
        Files.createDirectories(logPath.getParent());
        Files.writeString(logPath, output);
        assertNotNull(output);
    }

    @Test
    void testCalibrationRound3() throws IOException {
        List<LeadAttempt> r3Presets = createRound3Presets();
        String output = runCalibration("ROUND 3 (Default Sun BW=2 + Rain Species, BW=-1 on P2)", r3Presets);
        System.out.println(output);
        Path logPath = Path.of("../reports/koga_calibration_round3.txt");
        Files.createDirectories(logPath.getParent());
        Files.writeString(logPath, output);
        assertNotNull(output);
    }
}
