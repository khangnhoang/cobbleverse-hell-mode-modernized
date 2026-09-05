package com.cobbleverse.legendaryrule.lead;

/**
 * Pure domain evidence record containing scoring details for an evaluated lead attempt.
 */
public record AttemptScore(
        String attemptId,
        int offensiveScore,
        int defensiveScore,
        int baseWeight,
        int totalScore
) {}
