package com.cobbleverse.legendaryrule.lead;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Pure domain service orchestrating candidate attempt evaluation and tie-breaking.
 * Zero external dependencies (no config, no Gson, no Minecraft, no logging).
 */
public final class LeadSelectionEngine {
    private final TypeMatchupScorer scorer;

    public LeadSelectionEngine(TypeMatchupScorer scorer) {
        this.scorer = Objects.requireNonNull(scorer, "scorer must not be null");
    }

    public LeadSelectionResult select(
            List<LeadAttempt> attempts,
            List<PlayerLeadTyping> playerLeads,
            List<RosterMemberTyping> npcRoster
    ) {
        Objects.requireNonNull(attempts, "attempts must not be null");
        Objects.requireNonNull(playerLeads, "playerLeads must not be null");
        Objects.requireNonNull(npcRoster, "npcRoster must not be null");

        if (attempts.isEmpty()) {
            throw new IllegalArgumentException("attempts list must not be empty");
        }

        Map<Integer, RosterMemberTyping> rosterBySlot = npcRoster.stream()
                .collect(Collectors.toMap(RosterMemberTyping::slot, r -> r, (a, b) -> a));

        List<ScoredAttempt> scoredList = new ArrayList<>();
        List<AttemptScore> evidenceList = new ArrayList<>();

        for (int i = 0; i < attempts.size(); i++) {
            LeadAttempt attempt = attempts.get(i);
            int slotA = attempt.leadSlots()[0];
            int slotB = attempt.leadSlots()[1];

            RosterMemberTyping memberA = rosterBySlot.get(slotA);
            RosterMemberTyping memberB = rosterBySlot.get(slotB);

            List<String> typesA = memberA != null ? memberA.types() : List.of();
            List<String> typesB = memberB != null ? memberB.types() : List.of();

            int offScore = 0;
            int defScore = 0;

            for (PlayerLeadTyping player : playerLeads) {
                List<String> pTypes = player.types();
                offScore += scorer.scoreNpcVsPlayer(typesA, pTypes);
                offScore += scorer.scoreNpcVsPlayer(typesB, pTypes);

                defScore += scorer.scorePlayerVsNpc(pTypes, typesA);
                defScore += scorer.scorePlayerVsNpc(pTypes, typesB);
            }

            int total = offScore + defScore + attempt.baseWeight();
            AttemptScore evidence = new AttemptScore(attempt.id(), offScore, defScore, attempt.baseWeight(), total);
            evidenceList.add(evidence);
            scoredList.add(new ScoredAttempt(attempt, total, attempt.baseWeight(), i));
        }

        // Tie-breaker: totalScore descending -> baseWeight descending -> declarationIndex ascending
        scoredList.sort(Comparator
                .comparingInt(ScoredAttempt::totalScore).reversed()
                .thenComparing(Comparator.comparingInt(ScoredAttempt::baseWeight).reversed())
                .thenComparingInt(ScoredAttempt::declarationIndex)
        );

        LeadAttempt winner = scoredList.get(0).attempt();
        return new LeadSelectionResult(winner, evidenceList);
    }

    private record ScoredAttempt(LeadAttempt attempt, int totalScore, int baseWeight, int declarationIndex) {}
}
