package com.cobbleverse.legendaryrule.lead;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class LeadSelectionConfigTest {

    @BeforeEach
    void resetConfig() {
        LeadSelectionConfig.setEnabled(true);
    }

    @Test
    void testLoadValidConfigFromJson() {
        String json = """
        {
          "enabled": true,
          "trainers": {
            "kanto_sabrina": {
              "attempts": [
                {
                  "id": "psychic_terrain_blitz",
                  "leadSlots": [0, 5],
                  "expectedLeadMembers": [
                    { "species": "indeedee", "form": "f", "requiredAspects": ["female"] },
                    { "species": "alakazam" }
                  ],
                  "baseWeight": 1,
                  "description": "Psychic Surge blitz"
                },
                {
                  "id": "anti_dark",
                  "leadSlots": [1, 2],
                  "baseWeight": -1
                }
              ]
            }
          }
        }
        """;
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        LeadSelectionConfig.loadFromJson(root);

        assertTrue(LeadSelectionConfig.isEnabled());
        Optional<TrainerLeadConfig> opt = LeadSelectionConfig.getTrainerConfig("kanto_sabrina");
        assertTrue(opt.isPresent());

        // Case insensitivity check
        assertTrue(LeadSelectionConfig.getTrainerConfig("KANTO_SABRINA").isPresent());

        TrainerLeadConfig cfg = opt.get();
        assertEquals(2, cfg.attempts().size());

        LeadAttempt att0 = cfg.attempts().get(0);
        assertEquals("psychic_terrain_blitz", att0.id());
        assertArrayEquals(new int[]{0, 5}, att0.leadSlots());
        assertEquals(1, att0.baseWeight());
        assertEquals(2, att0.expectedLeadMembers().size());

        ExpectedLeadMember exp0 = att0.expectedLeadMembers().get(0);
        assertEquals("indeedee", exp0.species());
        assertEquals("f", exp0.form());
        assertEquals(List.of("female"), exp0.requiredAspects());

        ExpectedLeadMember exp1 = att0.expectedLeadMembers().get(1);
        assertEquals("alakazam", exp1.species());
        assertNull(exp1.form());
        assertTrue(exp1.requiredAspects().isEmpty());

        LeadAttempt att1 = cfg.attempts().get(1);
        assertEquals("anti_dark", att1.id());
        assertArrayEquals(new int[]{1, 2}, att1.leadSlots());
        assertEquals(-1, att1.baseWeight());
        assertTrue(att1.expectedLeadMembers().isEmpty());
    }

    @Test
    void testStructuralValidationFiltersInvalidAttemptsPreservesValid() {
        String json = """
        {
          "trainers": {
            "test_trainer": {
              "attempts": [
                {
                  "id": "bad_weight_high",
                  "leadSlots": [0, 1],
                  "baseWeight": 3
                },
                {
                  "id": "bad_weight_low",
                  "leadSlots": [0, 1],
                  "baseWeight": -3
                },
                {
                  "id": "negative_slot",
                  "leadSlots": [-1, 2]
                },
                {
                  "id": "duplicate_slots",
                  "leadSlots": [2, 2]
                },
                {
                  "id": "wrong_slot_count",
                  "leadSlots": [0, 1, 2]
                },
                {
                  "leadSlots": [0, 1]
                },
                {
                  "id": "valid_attempt",
                  "leadSlots": [0, 1],
                  "baseWeight": 2
                }
              ]
            }
          }
        }
        """;
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        LeadSelectionConfig.loadFromJson(root);

        Optional<TrainerLeadConfig> opt = LeadSelectionConfig.getTrainerConfig("test_trainer");
        assertTrue(opt.isPresent(), "Trainer with at least one valid attempt should be present");
        assertEquals(1, opt.get().attempts().size());
        assertEquals("valid_attempt", opt.get().attempts().get(0).id());
    }

    @Test
    void testTrainerWithOnlyInvalidAttemptsIsOmitted() {
        String json = """
        {
          "trainers": {
            "all_invalid": {
              "attempts": [
                {
                  "id": "bad1",
                  "leadSlots": [0, 0]
                }
              ]
            }
          }
        }
        """;
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        LeadSelectionConfig.loadFromJson(root);

        Optional<TrainerLeadConfig> opt = LeadSelectionConfig.getTrainerConfig("all_invalid");
        assertTrue(opt.isEmpty());
    }

    @Test
    void testDisabledConfigReturnsEmpty() {
        String json = """
        {
          "enabled": false,
          "trainers": {
            "kanto_sabrina": {
              "attempts": [
                { "id": "blitz", "leadSlots": [0, 1] }
              ]
            }
          }
        }
        """;
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        LeadSelectionConfig.loadFromJson(root);

        assertFalse(LeadSelectionConfig.isEnabled());
        assertTrue(LeadSelectionConfig.getTrainerConfig("kanto_sabrina").isEmpty());
    }

    @Test
    void testLoadFromNonExistentFileGracefullyDefaults(@TempDir Path tempDir) {
        Path missing = tempDir.resolve("non_existent.json");
        LeadSelectionConfig.load(missing);
        assertTrue(LeadSelectionConfig.isEnabled());
        assertTrue(LeadSelectionConfig.getTrainerConfig("any").isEmpty());
    }

    @Test
    void testLoadMalformedFileDisablesGracefully(@TempDir Path tempDir) throws IOException {
        Path badFile = tempDir.resolve("bad.json");
        Files.writeString(badFile, "{ not valid json ]");

        LeadSelectionConfig.load(badFile);
        assertFalse(LeadSelectionConfig.isEnabled(), "Malformed JSON should disable config to prevent errors");
        assertTrue(LeadSelectionConfig.getTrainerConfig("any").isEmpty());
    }

    @Test
    void testExpectedLeadMembersSizeInvariants() {
        String json = """
        {
          "trainers": {
            "test_trainer": {
              "attempts": [
                {
                  "id": "valid_size_2",
                  "leadSlots": [0, 1],
                  "expectedLeadMembers": [
                    { "species": "indeedee" },
                    { "species": "alakazam" }
                  ]
                },
                {
                  "id": "invalid_size_0",
                  "leadSlots": [0, 1],
                  "expectedLeadMembers": []
                },
                {
                  "id": "invalid_size_1",
                  "leadSlots": [0, 1],
                  "expectedLeadMembers": [
                    { "species": "indeedee" }
                  ]
                },
                {
                  "id": "invalid_size_3",
                  "leadSlots": [0, 1],
                  "expectedLeadMembers": [
                    { "species": "indeedee" },
                    { "species": "alakazam" },
                    { "species": "metagross" }
                  ]
                },
                {
                  "id": "valid_no_expected",
                  "leadSlots": [2, 3]
                }
              ]
            }
          }
        }
        """;
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        LeadSelectionConfig.loadFromJson(root);

        Optional<TrainerLeadConfig> opt = LeadSelectionConfig.getTrainerConfig("test_trainer");
        assertTrue(opt.isPresent(), "Trainer with valid attempts must be registered");
        List<LeadAttempt> attempts = opt.get().attempts();

        // Exactly 2 attempts should be valid: valid_size_2 and valid_no_expected
        // invalid_size_0, invalid_size_1, invalid_size_3 must be rejected without invalidating valid attempts
        assertEquals(2, attempts.size());
        assertEquals("valid_size_2", attempts.get(0).id());
        assertEquals(2, attempts.get(0).expectedLeadMembers().size());
        assertEquals("valid_no_expected", attempts.get(1).id());
        assertTrue(attempts.get(1).expectedLeadMembers().isEmpty());
    }
}
