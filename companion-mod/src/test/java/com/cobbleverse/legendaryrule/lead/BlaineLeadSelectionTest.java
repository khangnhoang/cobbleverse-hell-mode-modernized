package com.cobbleverse.legendaryrule.lead;

import com.cobbleverse.legendaryrule.lead.simulation.calculator.Turn1StatCalculator;
import com.cobbleverse.legendaryrule.lead.simulation.model.CompetitivePokemonProfile;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Validates Blaine Hell Mode dynamic lead selection rules, precedence,
 * dynamic threatSpeed baseline derived from canonical default lead,
 * weather ability regression avoidance, Choice Scarf impact, and roster drift guards.
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

    public static int calculateSpeedFromJsonMon(JsonObject mon) {
        int base = switch (mon.get("species").getAsString().toLowerCase()) {
            case "arcanine" -> 95;
            case "charizard" -> 100;
            case "moltres" -> 90;
            case "rillaboom" -> 85;
            case "golisopod" -> 40;
            case "zeraora" -> 143;
            default -> 100;
        };
        int iv = mon.getAsJsonObject("ivs").get("spe").getAsInt();
        int ev = mon.getAsJsonObject("evs").get("spe").getAsInt();
        int level = mon.get("level").getAsInt();
        String nature = mon.get("nature").getAsString();
        double natureMod = Turn1StatCalculator.getNatureMultiplier(nature, CompetitivePokemonProfile.Stat.SPE);
        return Turn1StatCalculator.calculateStat(base, iv, ev, level, natureMod);
    }

    public static List<RosterMemberTyping> loadBlaineRosterFromDatapack() throws Exception {
        Path path = Path.of("../datapacks/hell-mode/data/rctmod/trainers/kanto_blaine.json");
        try (Reader reader = Files.newBufferedReader(path)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            JsonArray team = root.getAsJsonArray("team");
            List<RosterMemberTyping> roster = new ArrayList<>();
            for (int i = 0; i < team.size(); i++) {
                JsonObject mon = team.get(i).getAsJsonObject();
                String species = mon.get("species").getAsString().toLowerCase();
                int spe = calculateSpeedFromJsonMon(mon);
                List<String> types = switch (species) {
                    case "arcanine" -> List.of("fire");
                    case "charizard" -> List.of("fire", "flying");
                    case "moltres" -> List.of("fire", "flying");
                    case "rillaboom" -> List.of("grass");
                    case "golisopod" -> List.of("bug", "water");
                    case "zeraora" -> List.of("electric");
                    default -> List.of("normal");
                };
                roster.add(new RosterMemberTyping(i, species, types, spe));
            }
            return roster;
        }
    }

    public static List<LeadAttempt> loadBlainePresetsFromDatapack() throws Exception {
        Path path = Path.of("../datapacks/hell-mode/data/rctmod/trainers/kanto_blaine.json");
        try (Reader reader = Files.newBufferedReader(path)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            return LeadSelectionConfig.parseAttemptsArray(root.getAsJsonArray("leadPresets"));
        }
    }

    @Test
    void test1_LiveRegression_MiraidonIronHandsSelectsDefaultSunIntimidate() throws Exception {
        List<LeadAttempt> presets = loadBlainePresetsFromDatapack();
        List<RosterMemberTyping> roster = loadBlaineRosterFromDatapack();

        // Live regression: Player leads Miraidon (electric/dragon) + Iron Hands (fighting/electric)
        // Neither matches water nor ground/rock. Neither is in favoredAgainstSpecies.
        // Speeds: Miraidon (base 135, lvl 70 ~205 < 210), Iron Hands (base 50, lvl 70 ~80 < 210)
        List<PlayerLeadTyping> liveLeads = List.of(
                new PlayerLeadTyping("miraidon", List.of("electric", "dragon"), 205),
                new PlayerLeadTyping("ironhands", List.of("fighting", "electric"), 80)
        );

        LeadSelectionResult result = engine.select(presets, liveLeads, roster);
        assertEquals("default_sun_intimidate", result.selectedAttempt().id(),
                "Miraidon + Iron Hands MUST select default_sun_intimidate (Mega Charizard Y + Arcanine), NOT anti_rain_core");
        assertArrayEquals(new int[]{1, 0}, result.selectedAttempt().leadSlots(),
                "Lead slots must be [1, 0] for Mega Charizard Y + Arcanine");
    }

    @Test
    void test2_TrueRainCoreSelectsZeraoraRillaboom() throws Exception {
        List<LeadAttempt> presets = loadBlainePresetsFromDatapack();
        List<RosterMemberTyping> roster = loadBlaineRosterFromDatapack();

        // Player leads Pelipper (water/flying) + Swampert (water/ground)
        // Both match water type (+2 each) and species (+2 each). Total matchup bonus = +8.
        // anti_rain_core total = -2 (base) + 8 (favored) + 1 (type chart) = 7 > default -3.
        List<PlayerLeadTyping> rainLeads = List.of(
                new PlayerLeadTyping("pelipper", List.of("water", "flying"), 65),
                new PlayerLeadTyping("swampert", List.of("water", "ground"), 60)
        );

        LeadSelectionResult result = engine.select(presets, rainLeads, roster);
        assertEquals("anti_rain_core", result.selectedAttempt().id(),
                "True Rain core (Pelipper + Swampert) MUST select anti_rain_core (Zeraora + Rillaboom)");
        assertArrayEquals(new int[]{5, 3}, result.selectedAttempt().leadSlots(),
                "Lead slots must be [5, 3] for Zeraora + Rillaboom");

        AttemptScore rainScore = result.evaluatedScores().stream()
                .filter(s -> s.attemptId().equals("anti_rain_core"))
                .findFirst().orElseThrow();
        assertEquals(7, rainScore.totalScore(), "Rain core score must be 7 (-2 base + 8 matchup bonus + 1 type chart)");
    }

    @Test
    void test3_SingleWaterThreatWithNonRainPartnerSelectsDefaultSunIntimidate() throws Exception {
        List<LeadAttempt> presets = loadBlainePresetsFromDatapack();
        List<RosterMemberTyping> roster = loadBlaineRosterFromDatapack();

        // Single water threat: Pelipper (water/flying) + Scizor (bug/steel)
        // Against Scizor, default Fire STAB scores heavily (+7), while anti_rain_core scores 0.
        List<PlayerLeadTyping> singleWaterLeads = List.of(
                new PlayerLeadTyping("pelipper", List.of("water", "flying"), 65),
                new PlayerLeadTyping("scizor", List.of("bug", "steel"), 65)
        );

        LeadSelectionResult result = engine.select(presets, singleWaterLeads, roster);
        assertEquals("default_sun_intimidate", result.selectedAttempt().id(),
                "Single water threat with non-rain partner MUST defer to default_sun_intimidate");
        assertArrayEquals(new int[]{1, 0}, result.selectedAttempt().leadSlots());
    }

    @Test
    void test4_TrueWaterGroundPressurePairSelectsMegaCharizardRillaboom() throws Exception {
        List<LeadAttempt> presets = loadBlainePresetsFromDatapack();
        List<RosterMemberTyping> roster = loadBlaineRosterFromDatapack();

        // Player leads Gastrodon (water/ground) + Landorus (ground/flying)
        // Both match ground type (+2 each) and species (+2 each). Total matchup bonus = +8.
        // anti_water_ground total = -2 (base) + 8 (favored) + 1 (type chart) = 7 > default -2.
        List<PlayerLeadTyping> waterGroundLeads = List.of(
                new PlayerLeadTyping("gastrodon", List.of("water", "ground"), 39),
                new PlayerLeadTyping("landorus", List.of("ground", "flying"), 101)
        );

        LeadSelectionResult result = engine.select(presets, waterGroundLeads, roster);
        assertEquals("anti_water_ground", result.selectedAttempt().id(),
                "True Water/Ground pressure pair MUST select anti_water_ground (Mega Charizard Y + Rillaboom)");
        assertArrayEquals(new int[]{1, 3}, result.selectedAttempt().leadSlots(),
                "Lead slots must be [1, 3] for Mega Charizard Y + Rillaboom");

        AttemptScore wgScore = result.evaluatedScores().stream()
                .filter(s -> s.attemptId().equals("anti_water_ground"))
                .findFirst().orElseThrow();
        assertEquals(7, wgScore.totalScore(), "Water/Ground score must be 7 (-2 base + 8 matchup bonus + 1 type chart)");
    }

    @Test
    void test5_SingleGroundThreatWithNonGroundPartnerSelectsDefaultSunIntimidate() throws Exception {
        List<LeadAttempt> presets = loadBlainePresetsFromDatapack();
        List<RosterMemberTyping> roster = loadBlaineRosterFromDatapack();

        // Single ground threat: Gastrodon (water/ground) + Scizor (bug/steel)
        // Scizor vulnerability to Fire allows default_sun_intimidate (score 7) to beat anti_water_ground (score 6).
        List<PlayerLeadTyping> singleGroundLeads = List.of(
                new PlayerLeadTyping("gastrodon", List.of("water", "ground"), 39),
                new PlayerLeadTyping("scizor", List.of("bug", "steel"), 65)
        );

        LeadSelectionResult result = engine.select(presets, singleGroundLeads, roster);
        assertEquals("default_sun_intimidate", result.selectedAttempt().id(),
                "Single ground threat with non-ground partner MUST defer to default_sun_intimidate");
        assertArrayEquals(new int[]{1, 0}, result.selectedAttempt().leadSlots());
    }

    @Test
    void test6_NeutralMatchupSelectsDefaultSunIntimidate() throws Exception {
        List<LeadAttempt> presets = loadBlainePresetsFromDatapack();
        List<RosterMemberTyping> roster = loadBlaineRosterFromDatapack();

        // Completely neutral matchup: Corviknight + Clefable (non-fast)
        List<PlayerLeadTyping> neutralLeads = List.of(
                new PlayerLeadTyping("corviknight", List.of("steel", "flying"), 67),
                new PlayerLeadTyping("clefable", List.of("fairy"), 60)
        );

        LeadSelectionResult result = engine.select(presets, neutralLeads, roster);
        assertEquals("default_sun_intimidate", result.selectedAttempt().id(),
                "Neutral matchup MUST select default_sun_intimidate");
        assertArrayEquals(new int[]{1, 0}, result.selectedAttempt().leadSlots());
    }

    @Test
    void test6b_TieBreakRule_DefaultWinsAgainstTiedSpecializedPreset() {
        // Direct proof of tie-break semantics:
        // Specialized preset has baseWeight = -2.
        // Single threat match gives typeFavored (+2) + speciesFavored (+2) = +4. Total = -2 + 4 = 2.
        // Default preset has baseWeight = 2, 0 bonuses. Total = 2.
        // Tie-breaker: default baseWeight (2) > specialized baseWeight (-2).
        LeadAttempt specialized = new LeadAttempt(
                "specialized_core",
                new int[]{1, 2},
                -2,
                List.of(),
                "Specialized preset",
                List.of("water"),
                List.of("pelipper"),
                0,
                0,
                false
        );

        LeadAttempt defaultLead = new LeadAttempt(
                "default_core",
                new int[]{0, 1},
                2,
                List.of(),
                "Default preset",
                List.of(),
                List.of(),
                0,
                0,
                true
        );

        // Neutral leads with no type interactions (e.g. normal vs normal)
        List<RosterMemberTyping> mockRoster = List.of(
                new RosterMemberTyping(0, "mon_a", List.of("normal"), 100),
                new RosterMemberTyping(1, "mon_b", List.of("normal"), 100),
                new RosterMemberTyping(2, "mon_c", List.of("normal"), 100)
        );

        List<PlayerLeadTyping> singleThreatLeads = List.of(
                new PlayerLeadTyping("pelipper", List.of("water"), 65),
                new PlayerLeadTyping("dummy", List.of("normal"), 50)
        );

        LeadSelectionResult result = engine.select(List.of(specialized, defaultLead), singleThreatLeads, mockRoster);
        assertEquals("default_core", result.selectedAttempt().id(),
                "When specialized preset ties default at score 2, default MUST win via baseWeight tie-breaker (2 > -2)");
    }


    @Test
    void test7_DoubleFastThreatsFasterThanThreatSpeedSelectsMegaCharizardMoltres() throws Exception {
        List<LeadAttempt> presets = loadBlainePresetsFromDatapack();
        List<RosterMemberTyping> roster = loadBlaineRosterFromDatapack();

        // Both opponents have actual speed strictly greater than threatSpeed (210)
        // anti_fast_threats has baseWeight = 0, fastBonus = 4 => total = 4 > default 2.
        List<PlayerLeadTyping> fastLeads = List.of(
                new PlayerLeadTyping("fluttermane", List.of("ghost", "fairy"), 293),
                new PlayerLeadTyping("dragapult", List.of("dragon", "ghost"), 304)
        );

        LeadSelectionResult result = engine.select(presets, fastLeads, roster);
        assertEquals("anti_fast_threats", result.selectedAttempt().id(),
                "Double fast threats exceeding threatSpeed MUST select anti_fast_threats (Mega Charizard Y + Moltres)");
        assertArrayEquals(new int[]{1, 2}, result.selectedAttempt().leadSlots(),
                "Lead slots must be [1, 2] for Mega Charizard Y + Moltres");

        AttemptScore fastScore = result.evaluatedScores().stream()
                .filter(s -> s.attemptId().equals("anti_fast_threats"))
                .findFirst().orElseThrow();
        assertEquals(4, fastScore.fastBonus(), "Fast bonus of 4 must be awarded when both leads exceed dynamic threatSpeed");
        assertEquals(4, fastScore.totalScore(), "Total score must be 4 (0 base + 4 fastBonus) beating default 2");
    }

    @Test
    void test8_SingleFastThreatDoesNotTriggerAntiFastThreats() throws Exception {
        List<LeadAttempt> presets = loadBlainePresetsFromDatapack();
        List<RosterMemberTyping> roster = loadBlaineRosterFromDatapack();

        // Only 1 opponent faster than threatSpeed (210): Dragapult (304), Clefable (100)
        // anti_fast_threats: base 0, fastBonus 0 => total = 0 < default 2.
        List<PlayerLeadTyping> mixedLeads = List.of(
                new PlayerLeadTyping("dragapult", List.of("dragon", "ghost"), 304),
                new PlayerLeadTyping("clefable", List.of("fairy"), 100)
        );

        LeadSelectionResult result = engine.select(presets, mixedLeads, roster);
        assertEquals("default_sun_intimidate", result.selectedAttempt().id(),
                "Only 1 fast threat must NOT trigger anti_fast_threats (requires minFastOpponents=2)");
        assertArrayEquals(new int[]{1, 0}, result.selectedAttempt().leadSlots());
    }

    @Test
    void test9_OpponentsBetweenSlowerAndFasterDefaultMembersAreNotFastThreats() throws Exception {
        List<LeadAttempt> presets = loadBlainePresetsFromDatapack();
        List<RosterMemberTyping> roster = loadBlaineRosterFromDatapack();

        int charizardSpeed = roster.get(1).speed(); // 210 (faster)
        int arcanineSpeed = roster.get(0).speed();  // 194 (slower)

        // Both opponents have speeds strictly between Arcanine (194) and Charizard (210): e.g. 200 and 205
        assertTrue(arcanineSpeed < 200 && 200 <= charizardSpeed);
        assertTrue(arcanineSpeed < 205 && 205 <= charizardSpeed);

        List<PlayerLeadTyping> midSpeedLeads = List.of(
                new PlayerLeadTyping("corviknight", List.of("steel", "flying"), 200),
                new PlayerLeadTyping("clefable", List.of("fairy"), 205)
        );

        LeadSelectionResult result = engine.select(presets, midSpeedLeads, roster);
        assertEquals("default_sun_intimidate", result.selectedAttempt().id(),
                "Opponents with speed <= max(default speeds) must NOT be counted as fast threats; default lead wins");
    }

    @Test
    void test10_DynamicThreatSpeedDerivationFromAuthoritativeDefault() throws Exception {
        List<LeadAttempt> presets = loadBlainePresetsFromDatapack();
        List<RosterMemberTyping> roster = loadBlaineRosterFromDatapack();

        LeadAttempt defaultAttempt = presets.stream()
                .filter(LeadAttempt::isDefault)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Blaine must have a preset marked default: true"));
        assertEquals("default_sun_intimidate", defaultAttempt.id());
        assertArrayEquals(new int[]{1, 0}, defaultAttempt.leadSlots());
        assertEquals(2, defaultAttempt.baseWeight(), "default_sun_intimidate baseWeight must be calibrated to 2");

        int charizardSpeed = roster.get(1).speed();
        int arcanineSpeed = roster.get(0).speed();
        int expectedThreatSpeed = Math.max(charizardSpeed, arcanineSpeed);

        // Verify speeds derived from JSON match expectations (Charizard Modest 252 EV = 210; Arcanine Jolly 100 EV = 194)
        assertEquals(210, charizardSpeed, "Charizard actual speed must be 210 at Level 70 Modest 252 EV");
        assertEquals(194, arcanineSpeed, "Arcanine actual speed must be 194 at Level 70 Jolly 100 EV");
        assertEquals(210, expectedThreatSpeed, "threatSpeed must equal max(210, 194) = 210");
    }

    @Test
    void test11_WeatherAbilityFalsePositiveRegression() throws Exception {
        List<LeadAttempt> presets = loadBlainePresetsFromDatapack();
        List<RosterMemberTyping> roster = loadBlaineRosterFromDatapack();

        // Swift Swim Swampert with raw actual speed 130 (< 210) paired with Landorus (140 < 210).
        // Since no field weather is active pre-battle, Swampert's speed must NOT be doubled to 260.
        List<PlayerLeadTyping> swiftSwimLeads = List.of(
                new PlayerLeadTyping("swampert", List.of("water", "ground"), 130),
                new PlayerLeadTyping("landorus", List.of("ground", "flying"), 140)
        );

        LeadSelectionResult result = engine.select(presets, swiftSwimLeads, roster);
        assertNotEquals("anti_fast_threats", result.selectedAttempt().id(),
                "Swift Swim must NOT receive unconditional x2 multiplier pre-battle to falsely trigger anti_fast_threats");
        assertEquals("anti_water_ground", result.selectedAttempt().id(),
                "Matchup should be handled by anti_water_ground based on typing and species");
    }

    @Test
    void test12_ChoiceScarfPlayerLeadExceedingThreatSpeed() throws Exception {
        List<LeadAttempt> presets = loadBlainePresetsFromDatapack();
        List<RosterMemberTyping> roster = loadBlaineRosterFromDatapack();

        // Gengar raw speed 150 (<= 210). With Choice Scarf (150 * 1.5 = 225), it exceeds threatSpeed (210).
        // Dragapult raw speed 304 exceeds threatSpeed (210).
        int gengarScarfSpeed = (int) Math.floor(150 * 1.5); // 225
        List<PlayerLeadTyping> scarfLeads = List.of(
                new PlayerLeadTyping("gengar", List.of("ghost", "poison"), gengarScarfSpeed),
                new PlayerLeadTyping("dragapult", List.of("dragon", "ghost"), 304)
        );

        LeadSelectionResult result = engine.select(presets, scarfLeads, roster);
        assertEquals("anti_fast_threats", result.selectedAttempt().id(),
                "Choice Scarf scaling that pushes speed above threatSpeed must count as a fast threat and trigger anti_fast_threats");
    }

    @Test
    void test13_ExplicitLegacyThresholdCompatibility() {
        // Preset with explicit fastSpeedThreshold: 100 takes precedence over dynamic derivation
        LeadAttempt legacyPreset = new LeadAttempt(
                "legacy_fast",
                new int[]{1, 2},
                0,
                List.of(),
                "Legacy test preset",
                List.of(),
                List.of(),
                2,
                100,
                false
        );

        LeadAttempt defaultPreset = new LeadAttempt(
                "default_lead",
                new int[]{0, 1},
                2,
                List.of(),
                "Default preset",
                List.of(),
                List.of(),
                0,
                0,
                true
        );

        List<RosterMemberTyping> roster = List.of(
                new RosterMemberTyping(0, "mon_a", List.of("fire"), 200),
                new RosterMemberTyping(1, "mon_b", List.of("fire"), 200),
                new RosterMemberTyping(2, "mon_c", List.of("flying"), 200)
        );

        // Player leads have speed 105 and 110 (which are >= 100 legacy threshold, but < 200 threatSpeed)
        List<PlayerLeadTyping> leads = List.of(
                new PlayerLeadTyping("opp_a", List.of("normal"), 105),
                new PlayerLeadTyping("opp_b", List.of("normal"), 110)
        );

        LeadSelectionResult result = engine.select(List.of(legacyPreset, defaultPreset), leads, roster);
        assertEquals("legacy_fast", result.selectedAttempt().id(),
                "Explicit fastSpeedThreshold > 0 must take precedence over dynamic derivation for backward compatibility");
    }

    @Test
    void test14_SingleDefaultLeadContractEnforced() {
        JsonObject att1 = new JsonObject();
        att1.addProperty("id", "def_1");
        JsonArray slots1 = new JsonArray();
        slots1.add(0);
        slots1.add(1);
        att1.add("leadSlots", slots1);
        att1.addProperty("default", true);

        JsonObject att2 = new JsonObject();
        att2.addProperty("id", "def_2");
        JsonArray slots2 = new JsonArray();
        slots2.add(2);
        slots2.add(3);
        att2.add("leadSlots", slots2);
        att2.addProperty("default", true);

        JsonArray arr = new JsonArray();
        arr.add(att1);
        arr.add(att2);

        List<LeadAttempt> parsed = LeadSelectionConfig.parseAttemptsArray(arr);
        assertEquals(1, parsed.size(), "Second default lead preset must be rejected by single-default authority enforcement");
        assertTrue(parsed.get(0).isDefault());
        assertEquals("def_1", parsed.get(0).id());
    }

    @Test
    void test15_ExpectedLeadMemberValidationMatchesActualBlaineRoster() throws Exception {
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
