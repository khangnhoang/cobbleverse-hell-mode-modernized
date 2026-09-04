package com.cobbleverse.legendaryrule.lead;

import java.util.List;
import java.util.Objects;

/**
 * Pure domain result returned by LeadSelectionEngine.
 * Contains the chosen LeadAttempt and structured scoring evidence.
 */
public record LeadSelectionResult(
        LeadAttempt selectedAttempt,
        List<AttemptScore> evaluatedScores
) {
    public LeadSelectionResult {
        Objects.requireNonNull(selectedAttempt, "selectedAttempt must not be null");
        evaluatedScores = evaluatedScores != null ? List.copyOf(evaluatedScores) : List.of();
    }
}
