package com.cobbleverse.legendaryrule.lead;

import com.cobblemon.mod.common.pokemon.Pokemon;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class LeadSelectionServiceTest {

    private TypeMatchupScorer scorer;

    @BeforeEach
    void setUp() {
        TypeChartData typeChart = TypeChartResourceLoader.load().orElseThrow(
                () -> new IllegalStateException("Failed to load canonical Gen 9 type chart for tests"));
        scorer = new TypeMatchupScorer(typeChart);
        LeadSelectionService.initialize(scorer);
        LeadSelectionConfig.setEnabled(true);
    }

    @AfterEach
    void tearDown() {
        LeadSelectionService.setUnavailable();
        LeadSelectionConfig.setEnabled(true);
    }

    @Test
    void testLifecycleAndAvailability() {
        LeadSelectionService.setUnavailable();
        assertFalse(LeadSelectionService.isAvailable());

        LeadSelectionService.initialize(scorer);
        assertTrue(LeadSelectionService.isAvailable());

        // When config is disabled, service reports unavailable
        LeadSelectionConfig.setEnabled(false);
        assertFalse(LeadSelectionService.isAvailable());

        LeadSelectionConfig.setEnabled(true);
        assertTrue(LeadSelectionService.isAvailable());

        LeadSelectionService.initialize(null);
        assertFalse(LeadSelectionService.isAvailable());
    }

    @Test
    void testSelectLeadReturnsEmptyWhenUnavailable() {
        LeadSelectionService.setUnavailable();
        Optional<LeadSelectionResult> result = LeadSelectionService.selectLead("kanto_sabrina", null, null);
        assertTrue(result.isEmpty(), "Must return empty when service is unavailable");
    }

    @Test
    void testSelectLeadReturnsEmptyOnNullOrEmptyParameters() {
        LeadSelectionService.initialize(scorer);

        assertTrue(LeadSelectionService.selectLead(null, null, null).isEmpty());
        assertTrue(LeadSelectionService.selectLead("kanto_sabrina", null, null).isEmpty());
        assertTrue(LeadSelectionService.selectLead("kanto_sabrina", new Pokemon[0], null).isEmpty());
    }

    @Test
    void testSelectLeadReturnsEmptyForUnconfiguredTrainer() {
        LeadSelectionService.initialize(scorer);

        // Brock has no configuration in LeadSelectionConfig
        Pokemon[] dummyTeam = new Pokemon[1];
        Object dummyPlayer = new Object();
        AtomicInteger resolverInvocations = new AtomicInteger(0);

        Optional<LeadSelectionResult> result = LeadSelectionService.selectLead(
                "kanto_brock",
                dummyTeam,
                dummyPlayer,
                p -> {
                    resolverInvocations.incrementAndGet();
                    return List.of(new PlayerLeadTyping("pikachu", List.of("electric")));
                },
                (slot, p) -> new PokemonIdentity("geodude", "", Collections.emptySet()),
                (slot, p) -> new RosterMemberTyping(slot, "geodude", List.of("rock", "ground"))
        );

        assertTrue(result.isEmpty(), "Unconfigured trainer must immediately return empty to preserve native ordering");
        assertEquals(0, resolverInvocations.get(), "Player lead resolution must NOT be invoked for unconfigured trainer");
    }

    @Test
    void testSuccessfulSelectionPathExactWinner() {
        // Setup config with two attempts:
        // Attempt 1: psychic_blitz (slots 0, 1), baseWeight = 1
        // Attempt 2: anti_dark (slots 2, 3), baseWeight = 0
        JsonObject root = new JsonObject();
        root.addProperty("enabled", true);
        JsonObject trainers = new JsonObject();
        JsonObject sabrina = new JsonObject();
        JsonArray attempts = new JsonArray();

        JsonObject att1 = new JsonObject();
        att1.addProperty("id", "psychic_blitz");
        JsonArray slots1 = new JsonArray();
        slots1.add(0);
        slots1.add(1);
        att1.add("leadSlots", slots1);
        att1.addProperty("baseWeight", 1);
        attempts.add(att1);

        JsonObject att2 = new JsonObject();
        att2.addProperty("id", "anti_dark");
        JsonArray slots2 = new JsonArray();
        slots2.add(2);
        slots2.add(3);
        att2.add("leadSlots", slots2);
        att2.addProperty("baseWeight", 0);
        attempts.add(att2);

        sabrina.add("attempts", attempts);
        trainers.add("kanto_sabrina", sabrina);
        root.add("trainers", trainers);
        LeadSelectionConfig.loadFromJson(root);

        Pokemon[] dummyTeam = new Pokemon[6];

        List<RosterMemberTyping> rosterTypings = List.of(
                new RosterMemberTyping(0, "alakazam", List.of("psychic")),
                new RosterMemberTyping(1, "espeon", List.of("psychic")),
                new RosterMemberTyping(2, "gardevoir", List.of("psychic", "fairy")),
                new RosterMemberTyping(3, "hatterene", List.of("psychic", "fairy")),
                new RosterMemberTyping(4, "metagross", List.of("steel", "psychic")),
                new RosterMemberTyping(5, "slowbro", List.of("water", "psychic"))
        );

        // Case A: Against Poison player leads, Psychic Blitz beats Anti-Dark
        // (Psychic hits Poison for 2.0 and takes 1.0; Fairy takes 2.0 super-effective from Poison)
        List<PlayerLeadTyping> poisonLeads = List.of(
                new PlayerLeadTyping("gengar", List.of("ghost", "poison")),
                new PlayerLeadTyping("muk", List.of("poison"))
        );

        Optional<LeadSelectionResult> resultA = LeadSelectionService.selectLead(
                "kanto_sabrina",
                dummyTeam,
                poisonLeads,
                (slot, p) -> new PokemonIdentity("dummy", "", Collections.emptySet()),
                (idx, p) -> rosterTypings.get(idx)
        );

        assertTrue(resultA.isPresent(), "Expected successful selection");
        assertEquals("psychic_blitz", resultA.get().selectedAttempt().id(), "Psychic blitz must win against poison leads");

        // Case B: Against Dark player leads, Anti-Dark (Fairy) should win
        List<PlayerLeadTyping> darkLeads = List.of(
                new PlayerLeadTyping("tyranitar", List.of("rock", "dark")),
                new PlayerLeadTyping("weavile", List.of("dark", "ice"))
        );

        Optional<LeadSelectionResult> resultB = LeadSelectionService.selectLead(
                "kanto_sabrina",
                dummyTeam,
                darkLeads,
                (slot, p) -> new PokemonIdentity("dummy", "", Collections.emptySet()),
                (idx, p) -> rosterTypings.get(idx)
        );

        assertTrue(resultB.isPresent(), "Expected successful selection");
        assertEquals("anti_dark", resultB.get().selectedAttempt().id(), "Anti-dark must win against dark leads");
    }

    @Test
    void testRuntimeInvalidAttemptFilteredWhileValidSiblingSelected() {
        // Attempt 1: leadSlots [0, 99] (out of bounds for team length 6)
        // Attempt 2: leadSlots [0, 1] (valid)
        JsonObject root = new JsonObject();
        root.addProperty("enabled", true);
        JsonObject trainers = new JsonObject();
        JsonObject sabrina = new JsonObject();
        JsonArray attempts = new JsonArray();

        JsonObject att1 = new JsonObject();
        att1.addProperty("id", "overflow_attempt");
        JsonArray slots1 = new JsonArray();
        slots1.add(0);
        slots1.add(99);
        att1.add("leadSlots", slots1);
        att1.addProperty("baseWeight", 0);
        attempts.add(att1);

        JsonObject att2 = new JsonObject();
        att2.addProperty("id", "valid_sibling");
        JsonArray slots2 = new JsonArray();
        slots2.add(0);
        slots2.add(1);
        att2.add("leadSlots", slots2);
        att2.addProperty("baseWeight", 0);
        attempts.add(att2);

        sabrina.add("attempts", attempts);
        trainers.add("kanto_sabrina", sabrina);
        root.add("trainers", trainers);
        LeadSelectionConfig.loadFromJson(root);

        Optional<TrainerLeadConfig> trainerCfg = LeadSelectionConfig.getTrainerConfig("kanto_sabrina");
        assertTrue(trainerCfg.isPresent());
        assertEquals(2, trainerCfg.get().attempts().size(), "Structural validation must accept both attempts");

        Pokemon[] dummyTeam = new Pokemon[6];
        List<PlayerLeadTyping> playerLeads = List.of(
                new PlayerLeadTyping("machamp", List.of("fighting"))
        );

        Optional<LeadSelectionResult> result = LeadSelectionService.selectLead(
                "kanto_sabrina",
                dummyTeam,
                playerLeads,
                (slot, p) -> new PokemonIdentity("alakazam", "", Collections.emptySet()),
                (idx, p) -> new RosterMemberTyping(idx, "alakazam", List.of("psychic"))
        );

        assertTrue(result.isPresent(), "Expected valid sibling to be selected despite overflow attempt");
        assertEquals("valid_sibling", result.get().selectedAttempt().id());
        assertEquals(1, result.get().evaluatedScores().size(), "Overflow attempt must have been filtered out");
    }

    @Test
    void testExpectedMemberDriftMismatchRejectsAttempt() {
        // Attempt 1 expects: [alakazam, slowbro] at slots [0, 1]
        // Attempt 2 expects: [alakazam, gengar] at slots [0, 2]
        JsonObject root = new JsonObject();
        root.addProperty("enabled", true);
        JsonObject trainers = new JsonObject();
        JsonObject sabrina = new JsonObject();
        JsonArray attempts = new JsonArray();

        JsonObject att1 = new JsonObject();
        att1.addProperty("id", "drifted_attempt");
        JsonArray slots1 = new JsonArray();
        slots1.add(0);
        slots1.add(1);
        att1.add("leadSlots", slots1);
        JsonArray exp1 = new JsonArray();
        JsonObject m1 = new JsonObject();
        m1.addProperty("species", "alakazam");
        JsonObject m2 = new JsonObject();
        m2.addProperty("species", "slowbro");
        exp1.add(m1);
        exp1.add(m2);
        att1.add("expectedLeadMembers", exp1);
        attempts.add(att1);

        JsonObject att2 = new JsonObject();
        att2.addProperty("id", "accurate_attempt");
        JsonArray slots2 = new JsonArray();
        slots2.add(0);
        slots2.add(2);
        att2.add("leadSlots", slots2);
        JsonArray exp2 = new JsonArray();
        JsonObject m3 = new JsonObject();
        m3.addProperty("species", "alakazam");
        JsonObject m4 = new JsonObject();
        m4.addProperty("species", "gengar");
        exp2.add(m3);
        exp2.add(m4);
        att2.add("expectedLeadMembers", exp2);
        attempts.add(att2);

        sabrina.add("attempts", attempts);
        trainers.add("kanto_sabrina", sabrina);
        root.add("trainers", trainers);
        LeadSelectionConfig.loadFromJson(root);

        Pokemon[] dummyTeam = new Pokemon[6];
        // Team has slot 0: alakazam, slot 1: machamp (NOT slowbro!), slot 2: gengar
        List<PokemonIdentity> teamIdentities = List.of(
                new PokemonIdentity("alakazam", "", Collections.emptySet()),
                new PokemonIdentity("machamp", "", Collections.emptySet()),
                new PokemonIdentity("gengar", "", Collections.emptySet()),
                new PokemonIdentity("dummy3", "", Collections.emptySet()),
                new PokemonIdentity("dummy4", "", Collections.emptySet()),
                new PokemonIdentity("dummy5", "", Collections.emptySet())
        );

        List<PlayerLeadTyping> playerLeads = List.of(
                new PlayerLeadTyping("normal_mon", List.of("normal"))
        );

        Optional<LeadSelectionResult> result = LeadSelectionService.selectLead(
                "kanto_sabrina",
                dummyTeam,
                playerLeads,
                (slot, p) -> teamIdentities.get(slot),
                (idx, p) -> new RosterMemberTyping(idx, "dummy", List.of("psychic"))
        );

        assertTrue(result.isPresent(), "Expected accurate attempt to be selected");
        assertEquals("accurate_attempt", result.get().selectedAttempt().id());
    }

    @Test
    void testZeroValidAttemptsReturnsEmptyFallback() {
        // All attempts invalid (e.g. slots out of bounds)
        JsonObject root = new JsonObject();
        root.addProperty("enabled", true);
        JsonObject trainers = new JsonObject();
        JsonObject sabrina = new JsonObject();
        JsonArray attempts = new JsonArray();

        JsonObject att1 = new JsonObject();
        att1.addProperty("id", "invalid_1");
        JsonArray slots1 = new JsonArray();
        slots1.add(10);
        slots1.add(11);
        att1.add("leadSlots", slots1);
        attempts.add(att1);

        sabrina.add("attempts", attempts);
        trainers.add("kanto_sabrina", sabrina);
        root.add("trainers", trainers);
        LeadSelectionConfig.loadFromJson(root);

        Pokemon[] dummyTeam = new Pokemon[6];
        List<PlayerLeadTyping> playerLeads = List.of(
                new PlayerLeadTyping("machamp", List.of("fighting"))
        );

        Optional<LeadSelectionResult> result = LeadSelectionService.selectLead(
                "kanto_sabrina",
                dummyTeam,
                playerLeads,
                (slot, p) -> new PokemonIdentity("alakazam", "", Collections.emptySet()),
                (idx, p) -> new RosterMemberTyping(idx, "alakazam", List.of("psychic"))
        );

        assertTrue(result.isEmpty(), "Must return empty when zero valid attempts remain");
    }

    @Test
    void testSelectionExceptionPropagatesToBoundary() {
        JsonObject root = new JsonObject();
        root.addProperty("enabled", true);
        JsonObject trainers = new JsonObject();
        JsonObject sabrina = new JsonObject();
        JsonArray attempts = new JsonArray();

        JsonObject att1 = new JsonObject();
        att1.addProperty("id", "valid_slots");
        JsonArray slots1 = new JsonArray();
        slots1.add(0);
        slots1.add(1);
        att1.add("leadSlots", slots1);
        attempts.add(att1);

        sabrina.add("attempts", attempts);
        trainers.add("kanto_sabrina", sabrina);
        root.add("trainers", trainers);
        LeadSelectionConfig.loadFromJson(root);

        Pokemon[] dummyTeam = new Pokemon[6];
        List<PlayerLeadTyping> playerLeads = List.of(
                new PlayerLeadTyping("machamp", List.of("fighting"))
        );

        // An unexpected exception inside typingFn or identityFn must NOT be swallowed by LeadSelectionService
        assertThrows(RuntimeException.class, () -> {
            LeadSelectionService.selectLead(
                    "kanto_sabrina",
                    dummyTeam,
                    playerLeads,
                    (slot, p) -> new PokemonIdentity("alakazam", "", Collections.emptySet()),
                    (idx, p) -> {
                        throw new RuntimeException("Unexpected runtime error in typing adapter");
                    }
            );
        }, "LeadSelectionService must propagate exceptions to let the Mixin boundary handle them");
    }
}
