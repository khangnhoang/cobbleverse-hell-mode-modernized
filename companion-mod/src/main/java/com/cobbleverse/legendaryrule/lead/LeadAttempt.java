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
        String description
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
        leadSlots = leadSlots.clone();
    }

    public LeadAttempt(String id, int[] leadSlots, int baseWeight, List<ExpectedLeadMember> expectedLeadMembers) {
        this(id, leadSlots, baseWeight, expectedLeadMembers, "");
    }
}
