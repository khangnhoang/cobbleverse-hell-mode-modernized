package com.cobbleverse.legendaryrule.lead;

/**
 * Pure domain evidence record containing scoring details for an evaluated lead attempt.
 * <p>
 * {@code eligible} is not a score component: it records whether the attempt is allowed to <em>win</em>.
 * An attempt declares eligibility by authored intent, not by arithmetic — an attempt carrying an
 * {@link OpponentMatch} is a conditional preset and stays eligible only while that matcher is satisfied.
 * {@code totalScore} remains the plain sum of the numeric components either way, so the evidence log
 * still accounts for every point an attempt did or did not earn.
 */
public record AttemptScore(
        String attemptId,
        int offensiveScore,
        int defensiveScore,
        int baseWeight,
        int typeFavoredBonus,
        int speciesFavoredBonus,
        int fastBonus,
        int opponentMatchBonus,
        boolean eligible,
        int totalScore
) {
    public AttemptScore(String attemptId, int offensiveScore, int defensiveScore, int baseWeight, int typeFavoredBonus, int speciesFavoredBonus, int fastBonus, int opponentMatchBonus, int totalScore) {
        this(attemptId, offensiveScore, defensiveScore, baseWeight, typeFavoredBonus, speciesFavoredBonus, fastBonus, opponentMatchBonus, true, totalScore);
    }

    public AttemptScore(String attemptId, int offensiveScore, int defensiveScore, int baseWeight, int typeFavoredBonus, int speciesFavoredBonus, int fastBonus, int totalScore) {
        this(attemptId, offensiveScore, defensiveScore, baseWeight, typeFavoredBonus, speciesFavoredBonus, fastBonus, 0, totalScore);
    }

    public AttemptScore(String attemptId, int offensiveScore, int defensiveScore, int baseWeight, int typeFavoredBonus, int speciesFavoredBonus, int totalScore) {
        this(attemptId, offensiveScore, defensiveScore, baseWeight, typeFavoredBonus, speciesFavoredBonus, 0, totalScore);
    }

    public AttemptScore(String attemptId, int offensiveScore, int defensiveScore, int baseWeight, int totalScore) {
        this(attemptId, offensiveScore, defensiveScore, baseWeight, 0, 0, 0, totalScore);
    }
}
