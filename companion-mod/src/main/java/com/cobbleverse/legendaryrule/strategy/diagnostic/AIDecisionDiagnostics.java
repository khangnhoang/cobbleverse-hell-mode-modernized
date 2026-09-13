package com.cobbleverse.legendaryrule.strategy.diagnostic;

import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobbleverse.legendaryrule.LegendaryRuleMod;
import com.gitlab.surilexa.rbrctai.api.ai.RunBunAI;

import java.util.*;

/**
 * Diagnostic logger for RunBunAI decision paths in Doubles battles.
 *
 * Provides non-mutating observability into:
 * 1. [AI-FINAL]: Post-adjustment scores of all candidate evaluations right before winner selection.
 * 2. [AI-TIE]: Tie detection and candidate enumeration when 2+ moves share the maximum final score.
 * 3. [AI-CHOSEN]: The final move selected by the battle AI.
 *
 * Hard Invariants:
 * - Read-only: Never mutates MoveEvaluation scores, damages, opponents, or ordering.
 * - Zero side effects: Does not alter tie-break semantics, damage calculation, or RNG.
 */
public final class AIDecisionDiagnostics {

    private AIDecisionDiagnostics() {
    }

    /**
     * Logs the post-adjustment candidate snapshot [AI-FINAL] right before native max selection.
     */
    public static void logFinalCandidates(List<RunBunAI.MoveEvaluation> evaluations, BattlePokemon attacker) {
        if (evaluations == null || evaluations.isEmpty()) {
            return;
        }
        LegendaryRuleMod.LOGGER.info("[AI-FINAL] attacker={}", getPokemonName(attacker));
        for (RunBunAI.MoveEvaluation eval : evaluations) {
            if (eval == null) continue;
            LegendaryRuleMod.LOGGER.info("[AI-FINAL] move={} target={} score={} damage={}",
                getMoveName(eval.getMove()),
                getTargetName(eval),
                eval.getScore(),
                eval.getDamage()
            );
        }
    }

    /**
     * Finds all candidates in the evaluation list that share the specified max score.
     */
    public static List<RunBunAI.MoveEvaluation> findTieCandidates(List<RunBunAI.MoveEvaluation> evaluations, int maxScore) {
        if (evaluations == null || evaluations.isEmpty()) {
            return Collections.emptyList();
        }
        List<RunBunAI.MoveEvaluation> tied = new ArrayList<>();
        for (RunBunAI.MoveEvaluation eval : evaluations) {
            if (eval != null && eval.getScore() == maxScore) {
                tied.add(eval);
            }
        }
        return tied;
    }

    /**
     * Logs [AI-TIE] if multiple candidates share the maximum score, followed by [AI-CHOSEN].
     */
    public static void logChosenAndTie(
        List<RunBunAI.MoveEvaluation> evaluations,
        RunBunAI.MoveEvaluation chosen,
        BattlePokemon attacker
    ) {
        if (chosen == null) {
            return;
        }
        String attackerName = getPokemonName(attacker);
        int maxScore = chosen.getScore();
        List<RunBunAI.MoveEvaluation> tieCandidates = findTieCandidates(evaluations, maxScore);

        if (tieCandidates.size() > 1) {
            LegendaryRuleMod.LOGGER.info("[AI-TIE] attacker={} maxScore={} candidates={}",
                attackerName, maxScore, tieCandidates.size());
            for (RunBunAI.MoveEvaluation tieEval : tieCandidates) {
                LegendaryRuleMod.LOGGER.info("[AI-TIE] move={} target={} score={}",
                    getMoveName(tieEval.getMove()),
                    getTargetName(tieEval),
                    tieEval.getScore()
                );
            }
            LegendaryRuleMod.LOGGER.info("[AI-TIE] chosen={} target={}",
                getMoveName(chosen.getMove()),
                getTargetName(chosen)
            );
        }

        LegendaryRuleMod.LOGGER.info("[AI-CHOSEN] attacker={} move={} target={} score={} damage={}",
            attackerName,
            getMoveName(chosen.getMove()),
            getTargetName(chosen),
            chosen.getScore(),
            chosen.getDamage()
        );
    }

    public static String getPokemonName(BattlePokemon bp) {
        if (bp == null) return "unknown";
        try {
            if (bp.getName() != null) {
                return bp.getName().getString();
            }
        } catch (Throwable ignored) {
        }
        try {
            if (bp.getEffectedPokemon() != null) {
                if (bp.getEffectedPokemon().getSpecies() != null) {
                    return bp.getEffectedPokemon().getSpecies().getName();
                }
                if (bp.getEffectedPokemon().getForm() != null && bp.getEffectedPokemon().getForm().getName() != null) {
                    return bp.getEffectedPokemon().getForm().getName();
                }
            }
        } catch (Throwable ignored) {
        }
        return "unknown";
    }

    public static String getMoveName(Move move) {
        if (move == null) return "unknown";
        try {
            return move.getName().toLowerCase(Locale.ROOT);
        } catch (Throwable ignored) {
            return "unknown";
        }
    }

    public static String getTargetName(RunBunAI.MoveEvaluation eval) {
        if (eval == null || eval.getOpponent() == null) return "unknown";
        return getPokemonName(eval.getOpponent().getBattlePokemon());
    }
}
