package com.cobbleverse.legendaryrule.lead.simulation.model;

/**
 * Objective verdict on the Turn-1 board state.
 */
public enum Turn1Verdict {
    /**
     * Net KO advantage or dominant board control preserved.
     */
    GOOD,

    /**
     * Even trade (1-for-1), heavy damage taken without secure KO, or roll/speed-tie dependent.
     */
    QUESTIONABLE,

    /**
     * Net KO disadvantage (0 trades) or unanswerable player board control.
     */
    CATASTROPHIC
}
