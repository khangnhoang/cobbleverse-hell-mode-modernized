package com.cobbleverse.legendaryrule.lead;

/**
 * Pure domain evidence record containing scoring details for an evaluated lead attempt.
 */
public record AttemptScore(
        String attemptId,
        int offensiveScore,
        int defensiveScore,
        int baseWeight,
        int typeFavoredBonus,
        int speciesFavoredBonus,
        int totalScore
) {
    public AttemptScore(String attemptId, int offensiveScore, int defensiveScore, int baseWeight, int totalScore) {
        this(attemptId, offensiveScore, defensiveScore, baseWeight, 0, 0, totalScore);
    }
}
