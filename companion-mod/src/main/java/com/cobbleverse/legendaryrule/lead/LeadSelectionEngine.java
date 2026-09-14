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

        LeadAttempt defaultAttempt = resolveDefaultAttempt(attempts);
        int dynamicThreatSpeed = 0;
        if (defaultAttempt != null) {
            int defaultSlotA = defaultAttempt.leadSlots()[0];
            int defaultSlotB = defaultAttempt.leadSlots()[1];
            RosterMemberTyping memA = rosterBySlot.get(defaultSlotA);
            RosterMemberTyping memB = rosterBySlot.get(defaultSlotB);
            int speedA = memA != null ? memA.speed() : 0;
            int speedB = memB != null ? memB.speed() : 0;
            dynamicThreatSpeed = Math.max(speedA, speedB);
        }

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
            int typeFavoredBonus = 0;
            int speciesFavoredBonus = 0;
            List<String> favoredTypes = attempt.favoredAgainst();
            List<String> favoredSpecies = attempt.favoredAgainstSpecies();

            for (PlayerLeadTyping player : playerLeads) {
                List<String> pTypes = player.types();
                offScore += scorer.scoreNpcVsPlayer(typesA, pTypes);
                offScore += scorer.scoreNpcVsPlayer(typesB, pTypes);

                defScore += scorer.scorePlayerVsNpc(pTypes, typesA);
                defScore += scorer.scorePlayerVsNpc(pTypes, typesB);

                if (favoredTypes != null && !favoredTypes.isEmpty()) {
                    for (String pt : pTypes) {
                        if (favoredTypes.contains(pt)) {
                            typeFavoredBonus += 2;
                            break;
                        }
                    }
                }

                if (favoredSpecies != null && !favoredSpecies.isEmpty()) {
                    String pSpec = player.species();
                    if (pSpec != null && favoredSpecies.contains(pSpec)) {
                        speciesFavoredBonus += 2;
                    }
                }
            }

            int fastBonus = 0;
            if (attempt.minFastOpponents() > 0) {
                boolean isDynamic = attempt.fastSpeedThreshold() <= 0 && dynamicThreatSpeed > 0;
                int threshold = isDynamic ? dynamicThreatSpeed : (attempt.fastSpeedThreshold() > 0 ? attempt.fastSpeedThreshold() : 100);
                int fastCount = 0;
                for (PlayerLeadTyping player : playerLeads) {
                    boolean isFast = isDynamic ? (player.speed() > threshold) : (player.speed() >= threshold);
                    if (isFast) {
                        fastCount++;
                    }
                }
                if (fastCount >= attempt.minFastOpponents()) {
                    fastBonus = 4;
                }
            }

            int opponentMatchBonus = 0;
            OpponentMatch match = attempt.opponentMatch();
            if (match != null) {
                Integer refSlot = match.fasterThanRosterSlot();
                RosterMemberTyping refMember = refSlot != null ? rosterBySlot.get(refSlot) : null;
                // Degeneracy guard: an absent referenced slot or a non-positive resolved speed makes the
                // property unsatisfiable rather than vacuously true.
                boolean resolvable = refSlot == null || (refMember != null && refMember.speed() > 0);
                if (resolvable) {
                    for (PlayerLeadTyping player : playerLeads) {
                        // Same-opponent AND: every present property is tested on this single lead.
                        if ((match.type() == null || player.types().contains(match.type()))
                                && (match.damagingMoveType() == null || player.damagingMoveTypes().contains(match.damagingMoveType()))
                                && (refMember == null || player.speed() > refMember.speed())) {
                            opponentMatchBonus = match.bonus();
                            break;
                        }
                    }
                }
            }

            int total = offScore + defScore + attempt.baseWeight() + typeFavoredBonus + speciesFavoredBonus + fastBonus + opponentMatchBonus;
            AttemptScore evidence = new AttemptScore(attempt.id(), offScore, defScore, attempt.baseWeight(), typeFavoredBonus, speciesFavoredBonus, fastBonus, opponentMatchBonus, total);
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

    private static LeadAttempt resolveDefaultAttempt(List<LeadAttempt> attempts) {
        for (LeadAttempt attempt : attempts) {
            if (attempt.isDefault()) {
                return attempt;
            }
        }
        for (LeadAttempt attempt : attempts) {
            if (attempt.minFastOpponents() == 0
                    && attempt.favoredAgainst().isEmpty()
                    && attempt.favoredAgainstSpecies().isEmpty()
                    && attempt.opponentMatch() == null) {
                return attempt;
            }
        }
        return attempts.get(0);
    }

    private record ScoredAttempt(LeadAttempt attempt, int totalScore, int baseWeight, int declarationIndex) {}
}
