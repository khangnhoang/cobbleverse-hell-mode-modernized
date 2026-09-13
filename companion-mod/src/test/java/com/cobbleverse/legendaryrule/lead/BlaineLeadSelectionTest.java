package com.cobbleverse.legendaryrule.lead;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Validates Blaine Hell Mode dynamic lead selection rules, precedence,
 * fast threat speed-based matching, and ExpectedLeadMember roster drift guards.
 */
class BlaineLeadSelectionTest {

    private static TypeMatchupScorer scorer;
    private static LeadSelectionEngine engine;

    @BeforeAll
    static void setUp() {
        TypeChartData data = TypeChartResourceLoader.load().orElseThrow(
                () -> new IllegalStateException("Failed to load canonical Gen 9 type chart for tests"));
        scorer = new TypeMatchupScorer(data);
        engine = new LeadSelectionEngine(scorer);
    }

    public static List<RosterMemberTyping> createBlaineRoster() {
        return List.of(
                new RosterMemberTyping(0, "arcanine", List.of("fire")),
                new RosterMemberTyping(1, "charizard", List.of("fire", "flying")),
                new RosterMemberTyping(2, "moltres", List.of("fire", "flying")),
                new RosterMemberTyping(3, "rillaboom", List.of("grass")),
                new RosterMemberTyping(4, "golisopod", List.of("bug", "water")),
                new RosterMemberTyping(5, "zeraora", List.of("electric"))
        );
    }

    public static List<LeadAttempt> loadBlainePresetsFromDatapack() throws Exception {
        Path path = Path.of("../datapacks/hell-mode/data/rctmod/trainers/kanto_blaine.json");
        try (Reader reader = Files.newBufferedReader(path)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            return LeadSelectionConfig.parseAttemptsArray(root.getAsJsonArray("leadPresets"));
        }
    }

    @Test
    void test1_PelipperRainCoreSelectsZeraoraRillaboom() throws Exception {
        List<LeadAttempt> presets = loadBlainePresetsFromDatapack();
        List<RosterMemberTyping> roster = createBlaineRoster();

        // Player leads Pelipper (water/flying) + Swampert (water/ground)
        List<PlayerLeadTyping> rainLeads = List.of(
                new PlayerLeadTyping("pelipper", List.of("water", "flying"), 65),
                new PlayerLeadTyping("swampert", List.of("water", "ground"), 60)
        );

        LeadSelectionResult result = engine.select(presets, rainLeads, roster);
        assertEquals("anti_rain_core", result.selectedAttempt().id(),
                "Rule A (anti_rain_core: Zeraora + Rillaboom) MUST be selected against Rain core");
        assertArrayEquals(new int[]{5, 3}, result.selectedAttempt().leadSlots(),
                "Lead slots must be [5, 3] for Zeraora + Rillaboom");

        AttemptScore rainScore = result.evaluatedScores().stream()
                .filter(s -> s.attemptId().equals("anti_rain_core"))
                .findFirst().orElseThrow();
        assertTrue(rainScore.totalScore() > 0, "Rain core score must be strongly positive");
    }

    @Test
    void test2_WaterGroundNonRainSelectsMegaCharizardRillaboom() throws Exception {
        List<LeadAttempt> presets = loadBlainePresetsFromDatapack();
        List<RosterMemberTyping> roster = createBlaineRoster();

        // Player leads Gastrodon (water/ground) + Landorus (ground/flying)
        List<PlayerLeadTyping> waterGroundLeads = List.of(
                new PlayerLeadTyping("gastrodon", List.of("water", "ground"), 39),
                new PlayerLeadTyping("landorus", List.of("ground", "flying"), 101)
        );

        LeadSelectionResult result = engine.select(presets, waterGroundLeads, roster);
        assertEquals("anti_water_ground", result.selectedAttempt().id(),
                "Rule B (anti_water_ground: Mega Charizard Y + Rillaboom) MUST be selected against Water+Ground threat");
        assertArrayEquals(new int[]{1, 3}, result.selectedAttempt().leadSlots(),
                "Lead slots must be [1, 3] for Mega Charizard Y + Rillaboom");
    }

    @Test
    void test3_DoubleFastThreatsSelectsMegaCharizardMoltres() throws Exception {
        List<LeadAttempt> presets = loadBlainePresetsFromDatapack();
        List<RosterMemberTyping> roster = createBlaineRoster();

        // Player leads two fast threats: Dragapult (spe 142) + Flutter Mane (spe 135)
        List<PlayerLeadTyping> fastLeads = List.of(
                new PlayerLeadTyping("dragapult", List.of("dragon", "ghost"), 142),
                new PlayerLeadTyping("fluttermane", List.of("ghost", "fairy"), 135)
        );

        LeadSelectionResult result = engine.select(presets, fastLeads, roster);
        assertEquals("anti_fast_threats", result.selectedAttempt().id(),
                "Rule C (anti_fast_threats: Mega Charizard Y + Moltres) MUST be selected against double fast threats");
        assertArrayEquals(new int[]{1, 2}, result.selectedAttempt().leadSlots(),
                "Lead slots must be [1, 2] for Mega Charizard Y + Moltres");

        AttemptScore fastScore = result.evaluatedScores().stream()
                .filter(s -> s.attemptId().equals("anti_fast_threats"))
                .findFirst().orElseThrow();
        assertEquals(4, fastScore.fastBonus(), "Fast bonus of 4 must be awarded when both leads are >= 100 Speed");
    }

    @Test
    void test4_NeutralTeamSelectsDefaultMegaCharizardArcanine() throws Exception {
        List<LeadAttempt> presets = loadBlainePresetsFromDatapack();
        List<RosterMemberTyping> roster = createBlaineRoster();

        // Neutral leads with speed < 100: Corviknight (spe 67) + Clefable (spe 60)
        List<PlayerLeadTyping> neutralLeads = List.of(
                new PlayerLeadTyping("corviknight", List.of("steel", "flying"), 67),
                new PlayerLeadTyping("clefable", List.of("fairy"), 60)
        );

        LeadSelectionResult result = engine.select(presets, neutralLeads, roster);
        assertEquals("default_sun_intimidate", result.selectedAttempt().id(),
                "Rule D (default_sun_intimidate: Mega Charizard Y + Arcanine) MUST be selected against neutral team");
        assertArrayEquals(new int[]{1, 0}, result.selectedAttempt().leadSlots(),
                "Lead slots must be [1, 0] for Mega Charizard Y + Arcanine");
    }

    @Test
    void test4b_SingleFastOpponentDoesNotTriggerAntiFastThreats() throws Exception {
        List<LeadAttempt> presets = loadBlainePresetsFromDatapack();
        List<RosterMemberTyping> roster = createBlaineRoster();

        // Only 1 fast opponent: Dragapult (spe 142) + Clefable (spe 60)
        List<PlayerLeadTyping> mixedLeads = List.of(
                new PlayerLeadTyping("dragapult", List.of("dragon", "ghost"), 142),
                new PlayerLeadTyping("clefable", List.of("fairy"), 60)
        );

        LeadSelectionResult result = engine.select(presets, mixedLeads, roster);
        assertEquals("default_sun_intimidate", result.selectedAttempt().id(),
                "Only 1 fast threat must NOT trigger anti_fast_threats (requires minFastOpponents=2)");
    }

    @Test
    void test5_ExpectedLeadMemberValidationMatchesActualBlaineRoster() throws Exception {
        List<LeadAttempt> presets = loadBlainePresetsFromDatapack();
        assertEquals(4, presets.size(), "Blaine must have exactly 4 authored presets");

        // Actual identities in Blaine's 6-mon roster
        PokemonIdentity arcanine = new PokemonIdentity("arcanine");
        PokemonIdentity charizard = new PokemonIdentity("charizard", null, Set.of("mega_y"));
        PokemonIdentity moltres = new PokemonIdentity("moltres");
        PokemonIdentity rillaboom = new PokemonIdentity("rillaboom");
        PokemonIdentity golisopod = new PokemonIdentity("golisopod", null, Set.of("mega"));
        PokemonIdentity zeraora = new PokemonIdentity("zeraora");

        List<PokemonIdentity> actualRoster = List.of(arcanine, charizard, moltres, rillaboom, golisopod, zeraora);

        for (LeadAttempt attempt : presets) {
            int slotA = attempt.leadSlots()[0];
            int slotB = attempt.leadSlots()[1];

            List<ExpectedLeadMember> expected = attempt.expectedLeadMembers();
            assertNotNull(expected, "Expected members must not be null for attempt: " + attempt.id());
            assertEquals(2, expected.size(), "Expected members must contain 2 entries for attempt: " + attempt.id());

            assertTrue(expected.get(0).matches(actualRoster.get(slotA)),
                    "Expected member 0 of '" + attempt.id() + "' must match actual mon at slot " + slotA);
            assertTrue(expected.get(1).matches(actualRoster.get(slotB)),
                    "Expected member 1 of '" + attempt.id() + "' must match actual mon at slot " + slotB);
        }
    }
}
