package com.cobbleverse.legendaryrule.strategy.switchai;

import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.types.ElementalType;
import com.cobblemon.mod.common.battles.ActiveBattlePokemon;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobbleverse.legendaryrule.strategy.dynamic.DynamicMoveResolver;
import com.gitlab.srcmc.rctapi.api.ai.utils.BattleStates;
import com.gitlab.surilexa.rbrctai.api.ai.utils.PokeMathMax;
import com.gitlab.surilexa.rbrctai.api.ai.utils.RBStatStages;

import java.util.Collections;
import java.util.List;

/**
 * Phase 2 Switch Candidate Scorer:
 * Ranks bench candidates according to the reviewed contract hierarchy:
 *   Safety Tier (Tier 1 Safe > Tier 2 Unsafe)
 *   -> Offensive Coverage (0.0% to 200.0% across eligible opponents)
 *   -> Defensive Severity (Tier 2 tie-break: lower incoming damage ranks higher)
 *   -> Native Bonuses (-1 to 10 mapped to [0, 11] as final tie-breaker)
 *
 * Encoded deterministically into a 32-bit positive integer (max ~480,288,023 < Integer.MAX_VALUE).
 */
public final class SwitchCandidateScorer {

    public static final int TIER1 = 1;
    public static final int TIER2 = 0;

    public static final int TARGET_COVERAGE_MAX = 1000;  // Max 100.0% for 1 target (0.1% units)
    public static final int COVERAGE_TOTAL_MAX = 2000;   // Max 200.0% for 2 targets (Doubles)
    public static final int DEFENSE_RATIO_MAX = 10000;   // 1000.0% max incoming ratio (0.1% units)

    public static final int NATIVE_MIN = -1;
    public static final int NATIVE_MAX = 10;
    public static final int NATIVE_RADIX = 12;

    private SwitchCandidateScorer() {
    }

    /**
     * Primary entry point for scoring a candidate switch Pokémon.
     */
    public static int score(
        BattlePokemon candidate,
        List<ActiveBattlePokemon> opponents,
        ActiveBattlePokemon activeBattlePokemon,
        RBStatStages stages,
        BattlePokemon currentActive,
        int nativeSwitchScore
    ) {
        return scoreWithDetails(candidate, opponents, activeBattlePokemon, stages, currentActive, nativeSwitchScore).packedScore;
    }

    /**
     * Scores a candidate switch Pokémon and returns the full component breakdown
     * without requiring separate or redundant calculations.
     */
    public static CandidateScoreDetails scoreWithDetails(
        BattlePokemon candidate,
        List<ActiveBattlePokemon> opponents,
        ActiveBattlePokemon activeBattlePokemon,
        RBStatStages stages,
        BattlePokemon currentActive,
        int nativeSwitchScore
    ) {
        // Bounds check on native score (F-REV-04 fail-loud)
        if (nativeSwitchScore < NATIVE_MIN || nativeSwitchScore > NATIVE_MAX) {
            throw new IllegalStateException("Native switch score outside contract bounds [-1, 10]: " + nativeSwitchScore);
        }

        if (candidate == null) {
            int packed = packScore(TIER2, 0, 0, nativeSwitchScore);
            return new CandidateScoreDetails(TIER2, 0, 0, nativeSwitchScore, packed);
        }

        int currentHP = candidate.getHealth();
        if (currentHP <= 0) {
            int packed = packScore(TIER2, 0, 0, nativeSwitchScore);
            return new CandidateScoreDetails(TIER2, 0, 0, nativeSwitchScore, packed);
        }

        // 1. Calculate doubles combined survivability & tier
        SurvivabilityResult survivability = evaluateSurvivability(candidate, opponents, activeBattlePokemon, stages);
        int tier = survivability.tier;

        // 2. Calculate offensive coverage into active opponents
        long coverageTotal = calculateCoverage(candidate, opponents, activeBattlePokemon, stages);

        // 3. Calculate defense rank (Reconciled F-REV-02)
        long incomingRatio;
        if (survivability.isUnknown || currentHP <= 0) {
            incomingRatio = DEFENSE_RATIO_MAX;
        } else {
            incomingRatio = Math.min(DEFENSE_RATIO_MAX, (long) Math.ceil(1000.0 * survivability.combinedDamage / currentHP));
        }

        long defenseRank = (tier == TIER1) ? DEFENSE_RATIO_MAX : (DEFENSE_RATIO_MAX - incomingRatio);

        // 4. Pack into deterministic integer
        int packed = packScore(tier, coverageTotal, defenseRank, nativeSwitchScore);
        return new CandidateScoreDetails(tier, coverageTotal, defenseRank, nativeSwitchScore, packed);
    }

    /**
     * Evaluates incoming damage from eligible opponents and determines candidate tier.
     */
    public static SurvivabilityResult evaluateSurvivability(
        BattlePokemon candidate,
        List<ActiveBattlePokemon> opponents,
        ActiveBattlePokemon activeBattlePokemon,
        RBStatStages stages
    ) {
        if (candidate == null || candidate.getHealth() <= 0) {
            return new SurvivabilityResult(TIER2, 0, false);
        }

        if (opponents == null) {
            return new SurvivabilityResult(TIER2, 0, true);
        }
        if (opponents.isEmpty()) {
            return new SurvivabilityResult(TIER1, 0, false);
        }

        int currentHP = candidate.getHealth();
        long combinedDamage = 0;
        boolean hasSingleOHKO = false;
        boolean isUnknown = false;
        int eligibleOpponents = 0;

        for (ActiveBattlePokemon opp : opponents) {
            if (opp == null || opp.isGone()) {
                continue;
            }
            BattlePokemon oppBP = opp.getBattlePokemon();
            if (oppBP == null) {
                isUnknown = true;
                continue;
            }
            if (oppBP.getHealth() <= 0) {
                continue;
            }

            if (oppBP.getMoveSet() == null || oppBP.getMoveSet().getMoves() == null || oppBP.getMoveSet().getMoves().isEmpty()) {
                isUnknown = true;
                continue;
            }

            eligibleOpponents++;
            int highestDamageFromOpp = 0;
            List<Move> oppMoves = oppBP.getMoveSet().getMoves();

            for (Move move : oppMoves) {
                int dmg = evaluateDamage(oppBP, candidate, move, activeBattlePokemon, stages);
                if (dmg < 0) {
                    isUnknown = true;
                } else {
                    highestDamageFromOpp = Math.max(highestDamageFromOpp, dmg);
                }
            }

            if (highestDamageFromOpp >= currentHP) {
                hasSingleOHKO = true;
            }
            combinedDamage += highestDamageFromOpp;
        }

        if (eligibleOpponents == 0) {
            if (isUnknown) {
                return new SurvivabilityResult(TIER2, 0, true);
            }
            return new SurvivabilityResult(TIER1, 0, false);
        }

        int tier = determineTier(currentHP, isUnknown, hasSingleOHKO, combinedDamage);
        return new SurvivabilityResult(tier, combinedDamage, isUnknown);
    }

    /**
     * Determines whether candidate is Tier 1 (Safe) or Tier 2 (Unsafe).
     */
    public static int determineTier(int currentHP, boolean isUnknown, boolean hasSingleOHKO, long combinedDamage) {
        if (currentHP <= 0 || isUnknown || hasSingleOHKO || combinedDamage >= currentHP) {
            return TIER2;
        }
        return TIER1;
    }

    /**
     * Evaluates ability immunity taking into account suppression (Neutralizing Gas),
     * mold breaker bypass, and Ability Shield protection.
     */
    public static boolean isAbilityImmune(
        String defenderAbility,
        ElementalType moveType,
        boolean isDefenderSuppressed,
        boolean attackerIgnoresAbilities,
        boolean hasAbilityShield
    ) {
        if (defenderAbility == null || moveType == null) {
            return false;
        }
        boolean abilityActive = !isDefenderSuppressed || hasAbilityShield;
        if (abilityActive && attackerIgnoresAbilities && !hasAbilityShield) {
            abilityActive = false;
        }
        if (!abilityActive) {
            return false;
        }
        return AbilityImmunityTable.isImmune(defenderAbility, moveType);
    }

    /**
     * Calculates total offensive coverage across eligible opponents (0 to 2000 in Doubles).
     */
    public static long calculateCoverage(
        BattlePokemon candidate,
        List<ActiveBattlePokemon> opponents,
        ActiveBattlePokemon activeBattlePokemon,
        RBStatStages stages
    ) {
        if (candidate == null || opponents == null || opponents.isEmpty()) {
            return 0;
        }

        List<Move> candidateMoves = candidate.getMoveSet() != null ? candidate.getMoveSet().getMoves() : Collections.emptyList();
        if (candidateMoves.isEmpty()) {
            return 0;
        }

        long coverageTotal = 0;

        for (ActiveBattlePokemon opp : opponents) {
            if (opp == null || opp.isGone()) {
                continue;
            }
            BattlePokemon oppBP = opp.getBattlePokemon();
            if (oppBP == null || oppBP.getHealth() <= 0 || oppBP.getMaxHealth() <= 0) {
                continue;
            }

            int maxDamage = 0;
            for (Move move : candidateMoves) {
                int dmg = evaluateDamage(candidate, oppBP, move, activeBattlePokemon, stages);
                if (dmg > 0) {
                    maxDamage = Math.max(maxDamage, dmg);
                }
            }

            int targetMaxHp = oppBP.getMaxHealth();
            long targetCoverage = Math.min(TARGET_COVERAGE_MAX, (long) Math.ceil(1000.0 * maxDamage / targetMaxHp));
            coverageTotal += targetCoverage;
        }

        return Math.min((long) COVERAGE_TOTAL_MAX, coverageTotal);
    }

    /**
     * Evaluates effective damage from attacker to defender with ability immunity and dynamic move resolution.
     * Returns:
     *   -1: Context missing / unknown evidence
     *    0: Status move or immune (via ability/type)
     *   >0: Calculated damage
     */
    public static int evaluateDamage(
        BattlePokemon attacker,
        BattlePokemon defender,
        Move move,
        ActiveBattlePokemon abp,
        RBStatStages stages
    ) {
        if (attacker == null || defender == null || move == null) {
            return -1;
        }

        // Context gate
        try {
            if (attacker.getActor() == null || attacker.getActor().getBattle() == null ||
                defender.getActor() == null || defender.getActor().getBattle() == null) {
                return -1;
            }
        } catch (Throwable t) {
            return -1;
        }

        // Status move deals 0 damage
        if (move.getDamageCategory() != null &&
            "status".equalsIgnoreCase(move.getDamageCategory().getName())) {
            return 0;
        }

        // Resolve dynamic move type
        Move effectiveMove = DynamicMoveResolver.resolveEffectiveMove(move, attacker, abp, false);
        ElementalType moveType = effectiveMove != null ? effectiveMove.getType() : move.getType();

        // Effective abilities via BattleStates
        Pokemon defenderPkmn = BattleStates.getTransformationOrEffected(defender);
        String defenderAbility = (defenderPkmn != null && defenderPkmn.getAbility() != null)
            ? defenderPkmn.getAbility().getName()
            : null;

        // Ability Shield check on defender
        String heldItem = defender.getHeldItemManager() != null ? defender.getHeldItemManager().showdownId(defender) : null;
        boolean hasAbilityShield = "abilityshield".equalsIgnoreCase(heldItem);

        // Ability immunity with suppression & mold breaker guards
        if (isAbilityImmune(defenderAbility, moveType, PokeMathMax.isSuppressed(defender), PokeMathMax.ignoreAbilities(attacker), hasAbilityShield)) {
            return 0;
        }

        // Guard 4: Native damage calculation
        RBStatStages effectiveStages = (stages != null) ? stages : new RBStatStages();
        return PokeMathMax.damage(
            attacker,
            defender,
            effectiveMove != null ? effectiveMove : move,
            abp,
            false,
            false,
            effectiveStages
        );
    }

    /**
     * Packs the hierarchy fields into a single deterministic 32-bit positive integer:
     *   tierRank -> coverageTotal -> defenseRank -> nativeRank
     */
    public static int packScore(int tier, long coverageTotal, long defenseRank, int nativeSwitchScore) {
        if (nativeSwitchScore < NATIVE_MIN || nativeSwitchScore > NATIVE_MAX) {
            throw new IllegalStateException("Native switch score outside contract bounds [-1, 10]: " + nativeSwitchScore);
        }
        int nativeRank = nativeSwitchScore - NATIVE_MIN; // in [0, 11]

        int tierRank = (tier == TIER1) ? 1 : 0;
        long clampedCoverage = Math.max(0, Math.min(COVERAGE_TOTAL_MAX, coverageTotal));
        long clampedDefense = Math.max(0, Math.min(DEFENSE_RATIO_MAX, defenseRank));

        long packed = (((tierRank * (long) (COVERAGE_TOTAL_MAX + 1) + clampedCoverage)
                       * (long) (DEFENSE_RATIO_MAX + 1) + clampedDefense)
                      * (long) NATIVE_RADIX + (long) nativeRank);

        if (packed > Integer.MAX_VALUE || packed < 0) {
            throw new IllegalStateException("Packed switch score exceeded bounds: " + packed);
        }

        return (int) packed;
    }

    public static final class SurvivabilityResult {
        public final int tier;
        public final long combinedDamage;
        public final boolean isUnknown;

        public SurvivabilityResult(int tier, long combinedDamage, boolean isUnknown) {
            this.tier = tier;
            this.combinedDamage = combinedDamage;
            this.isUnknown = isUnknown;
        }
    }

    public static final class CandidateScoreDetails {
        public final int tier;
        public final long coverage;
        public final long defenseRank;
        public final int nativeScore;
        public final int packedScore;

        public CandidateScoreDetails(int tier, long coverage, long defenseRank, int nativeScore, int packedScore) {
            this.tier = tier;
            this.coverage = coverage;
            this.defenseRank = defenseRank;
            this.nativeScore = nativeScore;
            this.packedScore = packedScore;
        }
    }
}
