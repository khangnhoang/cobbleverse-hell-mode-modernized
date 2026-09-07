package com.cobbleverse.legendaryrule.strategy.spread;

import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.battles.ActiveBattlePokemon;
import com.cobblemon.mod.common.battles.MoveTarget;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.gitlab.surilexa.rbrctai.api.ai.RunBunAI;

import java.util.*;

/**
 * Invocation-local context that computes derived board-value metrics for allAdjacentFoes spread moves
 * in Doubles without mutating MoveEvaluation.damage.
 */
public final class SpreadMoveValuationContext {
    private static final SpreadMoveValuationContext EMPTY = new SpreadMoveValuationContext(Collections.emptyMap(), Collections.emptyMap());

    private final Map<Move, Integer> rankingDamageMap;
    private final Map<Move, Double> normalizedPressureMap;

    public SpreadMoveValuationContext(Map<Move, Integer> rankingDamageMap, Map<Move, Double> normalizedPressureMap) {
        this.rankingDamageMap = rankingDamageMap != null ? rankingDamageMap : Collections.emptyMap();
        this.normalizedPressureMap = normalizedPressureMap != null ? normalizedPressureMap : Collections.emptyMap();
    }

    public static SpreadMoveValuationContext empty() {
        return EMPTY;
    }

    /**
     * Inspects the evaluations list and precomputes total board ranking damage and normalized pressure
     * exclusively for MoveTarget.allAdjacentFoes moves that hit multiple live opponents.
     */
    public static SpreadMoveValuationContext fromEvaluations(List<RunBunAI.MoveEvaluation> evaluations) {
        if (evaluations == null || evaluations.isEmpty()) {
            return EMPTY;
        }

        Map<Move, List<RunBunAI.MoveEvaluation>> spreadGroups = new HashMap<>();
        for (RunBunAI.MoveEvaluation eval : evaluations) {
            if (eval == null) continue;
            Move move = eval.getMove();
            if (move == null || move.getTemplate() == null) continue;

            // FROZEN SCOPE: Only allAdjacentFoes. Never aggregate allAdjacent (friendly fire not modeled).
            if (move.getTemplate().getTarget() == MoveTarget.allAdjacentFoes) {
                spreadGroups.computeIfAbsent(move, k -> new ArrayList<>()).add(eval);
            }
        }

        if (spreadGroups.isEmpty()) {
            return EMPTY;
        }

        Map<Move, Integer> ranking = new HashMap<>();
        Map<Move, Double> pressure = new HashMap<>();

        for (Map.Entry<Move, List<RunBunAI.MoveEvaluation>> entry : spreadGroups.entrySet()) {
            Move move = entry.getKey();
            List<RunBunAI.MoveEvaluation> evals = entry.getValue();

            // Only aggregate if there are multiple opposing target evaluations on the board
            if (evals.size() > 1) {
                int totalRankingDamage = 0;
                double totalPressure = 0.0;

                for (RunBunAI.MoveEvaluation eval : evals) {
                    int dmg = eval.getDamage();
                    totalRankingDamage += dmg;

                    ActiveBattlePokemon opponent = eval.getOpponent();
                    if (opponent != null && opponent.getBattlePokemon() != null) {
                        BattlePokemon bp = opponent.getBattlePokemon();
                        int maxHp = bp.getMaxHealth();
                        if (maxHp > 0) {
                            totalPressure += (double) dmg / (double) maxHp;
                        }
                    }
                }

                ranking.put(move, totalRankingDamage);
                pressure.put(move, totalPressure);
            }
        }

        return new SpreadMoveValuationContext(ranking, pressure);
    }

    /**
     * Returns candidate-ranking damage. For allAdjacentFoes moves hitting multiple targets,
     * returns SUM(damage). For single-target or allAdjacent moves, returns native eval.getDamage().
     */
    public int getRankingDamage(RunBunAI.MoveEvaluation eval) {
        if (eval == null) return 0;
        Move move = eval.getMove();
        if (move == null) return eval.getDamage();
        return rankingDamageMap.getOrDefault(move, eval.getDamage());
    }

    /**
     * Returns normalized board pressure: SUM(damage/maxHP).
     */
    public double getNormalizedPressure(Move move, double fallback) {
        if (move == null) return fallback;
        return normalizedPressureMap.getOrDefault(move, fallback);
    }

    public boolean isAggregatedSpreadMove(Move move) {
        return move != null && rankingDamageMap.containsKey(move);
    }

    /**
     * Adjusts the score for an allAdjacentFoes move evaluation if native single-target scoring
     * penalized it to -5 due to local target HP pressure < 0.30, but total board pressure is >= 0.30.
     */
    public void adjustSpreadPressureScore(RunBunAI.MoveEvaluation eval) {
        if (eval == null) return;
        Move move = eval.getMove();
        if (!isAggregatedSpreadMove(move)) return;

        // Target-local immunity/0-damage must stay penalized
        if (eval.getDamage() <= 0) return;

        double boardPressure = getNormalizedPressure(move, 0.0);
        // If native per-target percentChange penalized this move to -5, but board pressure >= 0.30,
        // promote it to +5 (the standard positive pressure score).
        if (boardPressure >= 0.30 && eval.getScore() == -5) {
            eval.setScore(5);
        }
    }
}
