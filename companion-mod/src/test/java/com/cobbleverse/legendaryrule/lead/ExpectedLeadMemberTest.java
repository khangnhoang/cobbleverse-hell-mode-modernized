package com.cobbleverse.legendaryrule.lead;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ExpectedLeadMemberTest {

    @Test
    void testSpeciesMatchCaseInsensitive() {
        ExpectedLeadMember expected = new ExpectedLeadMember("Indeedee");
        PokemonIdentity actual = new PokemonIdentity("indeedee", null, Set.of());
        assertTrue(expected.matches(actual));

        PokemonIdentity upperActual = new PokemonIdentity("INDEEDEE", null, Set.of());
        assertTrue(expected.matches(upperActual));

        PokemonIdentity wrongSpecies = new PokemonIdentity("alakazam", null, Set.of());
        assertFalse(expected.matches(wrongSpecies));
    }

    @Test
    void testNullActual() {
        ExpectedLeadMember expected = new ExpectedLeadMember("indeedee");
        assertFalse(expected.matches(null));
    }

    @Test
    void testFormMatchingDirect() {
        ExpectedLeadMember expected = new ExpectedLeadMember("indeedee", "f");
        PokemonIdentity matchingActual = new PokemonIdentity("indeedee", "f", Set.of("female"));
        assertTrue(expected.matches(matchingActual));

        PokemonIdentity matchingCase = new PokemonIdentity("indeedee", "F", Set.of());
        assertTrue(expected.matches(matchingCase));

        PokemonIdentity wrongForm = new PokemonIdentity("indeedee", "m", Set.of("male"));
        assertFalse(expected.matches(wrongForm));

        PokemonIdentity noForm = new PokemonIdentity("indeedee", null, Set.of());
        assertFalse(expected.matches(noForm));
    }

    @Test
    void testFormAndAspectSeparationRegressions() {
        ExpectedLeadMember expected = new ExpectedLeadMember("indeedee", "f", List.of("female"));

        // 1. Correct form + required aspect -> pass
        PokemonIdentity correctBoth = new PokemonIdentity("indeedee", "f", Set.of("female", "psychic-surge"));
        assertTrue(expected.matches(correctBoth));

        // 2. Wrong form + matching aspect -> FAIL (must NOT allow form mismatch to pass via aspect)
        PokemonIdentity wrongFormMatchingAspect = new PokemonIdentity("indeedee", "m", Set.of("female"));
        assertFalse(expected.matches(wrongFormMatchingAspect), "Wrong form with matching aspect must FAIL");

        PokemonIdentity nullFormMatchingAspect = new PokemonIdentity("indeedee", null, Set.of("female", "f"));
        assertFalse(expected.matches(nullFormMatchingAspect), "Null form with token in aspects must FAIL");

        // 3. Correct form + missing required aspect -> FAIL
        PokemonIdentity correctFormMissingAspect = new PokemonIdentity("indeedee", "f", Set.of("psychic-surge"));
        assertFalse(expected.matches(correctFormMissingAspect), "Correct form with missing required aspect must FAIL");
    }

    @Test
    void testCaseNormalization() {
        ExpectedLeadMember expected = new ExpectedLeadMember("InDeeDee", "F", List.of("FeMaLe"));

        // Match against lower-cased identity
        PokemonIdentity lowerIdentity = new PokemonIdentity("indeedee", "f", Set.of("female"));
        assertTrue(expected.matches(lowerIdentity));

        // Match against upper-cased identity
        PokemonIdentity upperIdentity = new PokemonIdentity("INDEEDEE", "F", Set.of("FEMALE"));
        assertTrue(expected.matches(upperIdentity));
    }

    @Test
    void testRequiredAspectsMatching() {
        ExpectedLeadMember expected = new ExpectedLeadMember("indeedee", "f", List.of("female", "psychic-surge"));

        // Has all required aspects
        PokemonIdentity actual = new PokemonIdentity("indeedee", "f", Set.of("female", "psychic-surge", "shiny"));
        assertTrue(expected.matches(actual));

        // Missing one aspect
        PokemonIdentity missingAspect = new PokemonIdentity("indeedee", "f", Set.of("female"));
        assertFalse(expected.matches(missingAspect));

        // Empty aspects on actual
        PokemonIdentity emptyAspects = new PokemonIdentity("indeedee", "f", Set.of());
        assertFalse(expected.matches(emptyAspects));
    }

    @Test
    void testWildcardFormAndAspects() {
        ExpectedLeadMember expected = new ExpectedLeadMember("alakazam");
        PokemonIdentity actual = new PokemonIdentity("alakazam", "mega", Set.of("mega", "male"));
        assertTrue(expected.matches(actual), "Wildcard form/aspects should match any form/aspect of that species");
    }

    @Test
    void testConstructorValidation() {
        assertThrows(NullPointerException.class, () -> new ExpectedLeadMember(null));
    }

    @Test
    void testLeadAttemptExpectedMembersSizeInvariant() {
        ExpectedLeadMember m1 = new ExpectedLeadMember("indeedee");
        ExpectedLeadMember m2 = new ExpectedLeadMember("alakazam");
        ExpectedLeadMember m3 = new ExpectedLeadMember("metagross");

        // Exactly 2 -> accepted
        assertDoesNotThrow(() -> new LeadAttempt("att", new int[]{0, 1}, 0, List.of(m1, m2)));

        // Absent / empty -> accepted
        assertDoesNotThrow(() -> new LeadAttempt("att", new int[]{0, 1}, 0, List.of()));
        assertDoesNotThrow(() -> new LeadAttempt("att", new int[]{0, 1}, 0, null));

        // 1 -> rejected
        assertThrows(IllegalArgumentException.class, () -> new LeadAttempt("att", new int[]{0, 1}, 0, List.of(m1)));

        // 3 -> rejected
        assertThrows(IllegalArgumentException.class, () -> new LeadAttempt("att", new int[]{0, 1}, 0, List.of(m1, m2, m3)));
    }
}
