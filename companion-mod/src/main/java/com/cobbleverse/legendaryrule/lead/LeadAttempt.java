package com.cobbleverse.legendaryrule.lead;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Pure domain model representing an authored lead attempt preset.
 */
public record LeadAttempt(
        String id,
        int[] leadSlots,
        int baseWeight,
        List<ExpectedLeadMember> expectedLeadMembers,
        String description,
        List<String> favoredAgainst,
        List<String> favoredAgainstSpecies
) {
    public LeadAttempt {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(leadSlots, "leadSlots must not be null");
        if (leadSlots.length != 2) {
            throw new IllegalArgumentException("leadSlots must contain exactly 2 indices, got: " + leadSlots.length);
        }
        if (leadSlots[0] < 0 || leadSlots[1] < 0) {
            throw new IllegalArgumentException("leadSlots indices must be non-negative, got: " + Arrays.toString(leadSlots));
        }
        if (leadSlots[0] == leadSlots[1]) {
            throw new IllegalArgumentException("leadSlots indices must be distinct, got: " + Arrays.toString(leadSlots));
        }
        if (baseWeight < -2 || baseWeight > 2) {
            throw new IllegalArgumentException("baseWeight must be between -2 and +2, got: " + baseWeight);
        }
        if (expectedLeadMembers != null && !expectedLeadMembers.isEmpty() && expectedLeadMembers.size() != 2) {
            throw new IllegalArgumentException("expectedLeadMembers if present must contain exactly 2 members, got: " + expectedLeadMembers.size());
        }
        expectedLeadMembers = expectedLeadMembers != null ? List.copyOf(expectedLeadMembers) : Collections.emptyList();
        description = description != null ? description : "";
        favoredAgainst = favoredAgainst != null ? normalizeStringList(favoredAgainst) : Collections.emptyList();
        favoredAgainstSpecies = favoredAgainstSpecies != null ? normalizeStringList(favoredAgainstSpecies) : Collections.emptyList();
        leadSlots = leadSlots.clone();
    }

    private static List<String> normalizeStringList(List<String> list) {
        return list.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(s -> s.trim().toLowerCase(java.util.Locale.ROOT))
                .distinct()
                .toList();
    }

    public LeadAttempt(String id, int[] leadSlots, int baseWeight, List<ExpectedLeadMember> expectedLeadMembers, String description, List<String> favoredAgainst) {
        this(id, leadSlots, baseWeight, expectedLeadMembers, description, favoredAgainst, Collections.emptyList());
    }

    public LeadAttempt(String id, int[] leadSlots, int baseWeight, List<ExpectedLeadMember> expectedLeadMembers, String description) {
        this(id, leadSlots, baseWeight, expectedLeadMembers, description, Collections.emptyList(), Collections.emptyList());
    }

    public LeadAttempt(String id, int[] leadSlots, int baseWeight, List<ExpectedLeadMember> expectedLeadMembers) {
        this(id, leadSlots, baseWeight, expectedLeadMembers, "", Collections.emptyList(), Collections.emptyList());
    }

    @Override
    public int[] leadSlots() {
        return leadSlots.clone();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LeadAttempt that)) return false;
        return baseWeight == that.baseWeight
                && Arrays.equals(leadSlots, that.leadSlots)
                && Objects.equals(id, that.id)
                && Objects.equals(expectedLeadMembers, that.expectedLeadMembers)
                && Objects.equals(description, that.description)
                && Objects.equals(favoredAgainst, that.favoredAgainst)
                && Objects.equals(favoredAgainstSpecies, that.favoredAgainstSpecies);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(id, baseWeight, expectedLeadMembers, description, favoredAgainst, favoredAgainstSpecies);
        result = 31 * result + Arrays.hashCode(leadSlots);
        return result;
    }
}
