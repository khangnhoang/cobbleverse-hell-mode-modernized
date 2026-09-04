package com.cobbleverse.legendaryrule.lead;

import java.util.List;
import java.util.Objects;

/**
 * Pure domain service evaluating type effectiveness and discrete matchup scores.
 * Receives TypeChartData via constructor. Zero I/O, zero logging, zero static state.
 */
public final class TypeMatchupScorer {
    private final TypeChartData typeChart;

    public TypeMatchupScorer(TypeChartData typeChart) {
        this.typeChart = Objects.requireNonNull(typeChart, "typeChart must not be null");
    }

    public double getEffectiveness(String attackType, List<String> defenderTypes) {
        if (attackType == null || defenderTypes == null || defenderTypes.isEmpty()) {
            return 1.0;
        }
        double mult = 1.0;
        for (String def : defenderTypes) {
            mult *= typeChart.getMultiplier(attackType, def);
        }
        return mult;
    }

    public int mapOffensiveScore(double multiplier) {
        if (multiplier >= 3.9) return 4;
        if (multiplier >= 1.9) return 2;
        if (multiplier >= 0.9) return 0;
        if (multiplier >= 0.49) return -1;
        if (multiplier > 0.01) return -2;
        return -4;
    }

    public int mapDefensiveScore(double incomingMultiplier) {
        if (incomingMultiplier >= 3.9) return -4;
        if (incomingMultiplier >= 1.9) return -2;
        if (incomingMultiplier >= 0.9) return 0;
        if (incomingMultiplier >= 0.49) return 1;
        if (incomingMultiplier > 0.01) return 2;
        return 4;
    }

    public int scoreNpcVsPlayer(List<String> npcStabTypes, List<String> playerDefenderTypes) {
        if (npcStabTypes == null || npcStabTypes.isEmpty()) {
            return 0;
        }
        double bestMult = 0.0;
        for (String stab : npcStabTypes) {
            double eff = getEffectiveness(stab, playerDefenderTypes);
            if (eff > bestMult) {
                bestMult = eff;
            }
        }
        return mapOffensiveScore(bestMult);
    }

    public int scorePlayerVsNpc(List<String> playerStabTypes, List<String> npcDefenderTypes) {
        if (playerStabTypes == null || playerStabTypes.isEmpty()) {
            return 0;
        }
        double worstIncoming = 0.0;
        for (String stab : playerStabTypes) {
            double eff = getEffectiveness(stab, npcDefenderTypes);
            if (eff > worstIncoming) {
                worstIncoming = eff;
            }
        }
        return mapDefensiveScore(worstIncoming);
    }
}
