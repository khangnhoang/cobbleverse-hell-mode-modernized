package com.cobbleverse.legendaryrule.strategy.switchai;

import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.battles.ActiveBattlePokemon;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.gitlab.surilexa.rbrctai.api.ai.RunBunAI;
import com.gitlab.surilexa.rbrctai.api.ai.utils.RBStatStages;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Pure strategy detector for Phase 1 switch evaluation:
 * 1. isLowOffensivePressure: determines whether AI active Pokémon's damage output is strictly below threshold (< 20%).
 * 2. isUnderCriticalThreat: determines whether all eligible active opponents can OHKO the active Pokémon.
 */
public final class DeadMatchupDetector {

    public static final double LOW_PRESSURE_THRESHOLD = 0.20;
    public static final double STAY_JUSTIFICATION_THRESHOLD = 0.33;

    private DeadMatchupDetector() {
    }

    /**
     * Determines whether a move evaluation justifies staying on the field (vetoing switch).
     * Returns true if and only if:
     * 1. eval is non-null with score >= 6 and positive damage > 0.
     * 2. Target opponent is living and eligible (not gone, health > 0, maxHealth > 0).
     * 3. Move either delivers a lethal KO (damage >= target current health) OR
     *    delivers meaningful offensive pressure (damage >= 33% of target max health).
     */
    public static boolean isMeaningfulOffensiveMove(RunBunAI.MoveEvaluation eval) {
        if (eval == null || eval.getScore() < 6 || eval.getDamage() <= 0) {
            return false;
        }
        ActiveBattlePokemon opp = eval.getOpponent();
        if (opp == null || opp.isGone()) {
            return false;
        }
        BattlePokemon oppBP = opp.getBattlePokemon();
        if (oppBP == null || oppBP.getMaxHealth() <= 0 || oppBP.getHealth() <= 0) {
            return false;
        }
        // Lethal KO on a living target justifies staying
        if (eval.getDamage() >= oppBP.getHealth()) {
            return true;
        }
        // Meaningful offensive pressure (>= 33% max HP) justifies staying
        double ratio = (double) eval.getDamage() / (double) oppBP.getMaxHealth();
        return ratio >= STAY_JUSTIFICATION_THRESHOLD;
    }

    /**
     * True if the maximum damage ratio across all evaluations matching eligible active opponents
     * is strictly less than 20% of target max HP.
     *
     * Note: MoveEvaluation.getOpponent() returns ActiveBattlePokemon (not BattlePokemon).
     */
    public static boolean isLowOffensivePressure(
        List<RunBunAI.MoveEvaluation> evaluations,
        List<ActiveBattlePokemon> opponents
    ) {
        if (evaluations == null || evaluations.isEmpty() || opponents == null || opponents.isEmpty()) {
            return false;
        }

        double maxRatio = 0.0;
        boolean hasValidMeasurement = false;

        for (RunBunAI.MoveEvaluation eval : evaluations) {
            if (eval == null) {
                continue;
            }
            ActiveBattlePokemon evalOpponent = eval.getOpponent();
            if (evalOpponent == null) {
                continue;
            }

            for (ActiveBattlePokemon target : opponents) {
                if (target == null || target.isGone()) {
                    continue;
                }
                BattlePokemon targetBP = target.getBattlePokemon();
                if (targetBP == null || targetBP.getMaxHealth() <= 0) {
                    continue;
                }

                if (isTargetMatch(evalOpponent, target)) {
                    int damage = Math.max(0, eval.getDamage());
                    int maxHP = targetBP.getMaxHealth();
                    double ratio = (double) damage / (double) maxHP;
                    maxRatio = Math.max(maxRatio, ratio);
                    hasValidMeasurement = true;
                    break;
                }
            }
        }

        if (!hasValidMeasurement) {
            return false;
        }

        return maxRatio < LOW_PRESSURE_THRESHOLD;
    }

    /**
     * Resolves the effective random value for Gate 3:
     * If low offensive pressure is detected (< 20%), the 25% random veto is bypassed (returns 0.0d).
     * If low offensive pressure is false (including critical threat alone, native hasLowScore alone,
     * or gray zone 20-33%), preserves native random value untouched.
     */
    public static double resolveGate3RandomValue(double nativeVal, boolean lowPressure) {
        if (!(nativeVal < 0.75d) && lowPressure) {
            return 0.0d;
        }
        return nativeVal;
    }

    /**
     * True if there is at least one eligible opponent and EVERY eligible opponent can OHKO self.
     * Reuses native RunBunAI.isOHKO(oppMoves, oppBP, self, activeBattlePokemon, stages).
     */
    public static boolean isUnderCriticalThreat(
        BattlePokemon self,
        List<ActiveBattlePokemon> opponents,
        ActiveBattlePokemon activeBattlePokemon,
        RBStatStages stages
    ) {
        if (self == null || opponents == null || opponents.isEmpty()) {
            return false;
        }

        int eligibleCount = 0;

        for (ActiveBattlePokemon opp : opponents) {
            if (opp == null || opp.isGone()) {
                continue;
            }
            BattlePokemon oppBP = opp.getBattlePokemon();
            if (oppBP == null) {
                continue;
            }

            eligibleCount++;
            List<Move> oppMoves = oppBP.getMoveSet() != null ? oppBP.getMoveSet().getMoves() : Collections.emptyList();

            boolean canOHKO = RunBunAI.isOHKO(oppMoves, oppBP, self, activeBattlePokemon, stages);
            if (!canOHKO) {
                return false;
            }
        }

        return eligibleCount > 0;
    }

    private static boolean isTargetMatch(ActiveBattlePokemon evalOpponent, ActiveBattlePokemon target) {
        if (evalOpponent == target) {
            return true;
        }
        BattlePokemon evalBP = evalOpponent.getBattlePokemon();
        BattlePokemon targetBP = target.getBattlePokemon();
        if (evalBP != null && targetBP != null) {
            UUID evalUuid = evalBP.getUuid();
            UUID targetUuid = targetBP.getUuid();
            return evalUuid != null && evalUuid.equals(targetUuid);
        }
        return false;
    }
}
