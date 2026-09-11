package com.cobbleverse.legendaryrule.lead.simulation.engine;

import com.cobbleverse.legendaryrule.lead.simulation.calculator.Turn1DamageCalculator;
import com.cobbleverse.legendaryrule.lead.simulation.calculator.Turn1DamageCalculator.DamageRange;
import com.cobbleverse.legendaryrule.lead.simulation.fixtures.KogaCompetitiveProfiles;
import com.cobbleverse.legendaryrule.lead.simulation.fixtures.SabrinaCompetitiveProfiles;
import com.cobbleverse.legendaryrule.lead.simulation.model.*;
import com.cobbleverse.legendaryrule.lead.simulation.resolver.Turn1ActionResolver;
import com.cobbleverse.legendaryrule.lead.simulation.resolver.Turn1EntryResolver;
import com.cobbleverse.legendaryrule.lead.simulation.resolver.Turn1SpeedResolver;

import java.util.*;

/**
 * Bounded adversarial Turn-1 simulator and objective outcome evaluator.
 */
public class Turn1LeadSimulator {

    public Turn1LeadSimulator() {}

    /**
     * Simulates Turn 1 across candidate actions, selecting the worst plausible player response.
     */
    public Turn1EvaluationResult simulate(
            List<CompetitivePokemonProfile> playerLeads,
            List<CompetitivePokemonProfile> kogaLeads,
            String playerPairName,
            String presetId
    ) {
        Objects.requireNonNull(playerLeads, "playerLeads must not be null");
        Objects.requireNonNull(kogaLeads, "kogaLeads must not be null");

        // 1. Initial State & Entry Resolution
        Turn1BattleState baseState = new Turn1BattleState(playerLeads, kogaLeads);
        Turn1EntryResolver.resolveEntry(baseState);

        // 2. Generate Candidate Actions for Koga
        List<String> aiDecisionLogs = new ArrayList<>();
        List<Turn1Action> kogaActions = determineKogaActions(baseState, aiDecisionLogs);

        // 3. Generate Candidate Action Lines for Player
        List<List<Turn1Action>> playerLines = generatePlayerLines(baseState);

        Turn1EvaluationResult worstResultForKoga = null;

        for (List<Turn1Action> pLine : playerLines) {
            Turn1EvaluationResult result = simulateSingleLine(
                    baseState,
                    pLine,
                    kogaActions,
                    playerPairName,
                    presetId,
                    aiDecisionLogs
            );

            if (worstResultForKoga == null || isWorseForKoga(result, worstResultForKoga)) {
                worstResultForKoga = result;
            }
        }

        return worstResultForKoga != null ? worstResultForKoga : createFallbackResult(baseState, playerPairName, presetId);
    }

    public Turn1EvaluationResult simulatePreset(
            List<CompetitivePokemonProfile> playerLeads,
            String presetId,
            String playerPairName
    ) {
        List<CompetitivePokemonProfile> kogaLeads = KogaCompetitiveProfiles.getPreset(presetId);
        return simulate(playerLeads, kogaLeads, playerPairName, presetId);
    }

    public Turn1EvaluationResult simulateSabrinaPreset(
            List<CompetitivePokemonProfile> playerLeads,
            String presetId,
            String playerPairName
    ) {
        List<CompetitivePokemonProfile> sabrinaLeads = SabrinaCompetitiveProfiles.getPreset(presetId);
        return simulate(playerLeads, sabrinaLeads, playerPairName, presetId);
    }

    public Turn1EvaluationResult simulateSpecificLine(
            List<CompetitivePokemonProfile> playerLeads,
            List<CompetitivePokemonProfile> kogaLeads,
            List<Turn1Action> playerActions,
            List<Turn1Action> kogaActions,
            String playerPairName,
            String presetId
    ) {
        Turn1BattleState baseState = new Turn1BattleState(playerLeads, kogaLeads);
        Turn1EntryResolver.resolveEntry(baseState);
        return simulateSingleLine(baseState, playerActions, kogaActions, playerPairName, presetId);
    }

    private record KogaActionSelection(Turn1Action action, String evaluationLog) {}

    private Turn1EvaluationResult simulateSingleLine(
            Turn1BattleState baseState,
            List<Turn1Action> playerActions,
            List<Turn1Action> kogaActions,
            String playerPairName,
            String presetId
    ) {
        return simulateSingleLine(baseState, playerActions, kogaActions, playerPairName, presetId, List.of());
    }

    private Turn1EvaluationResult simulateSingleLine(
            Turn1BattleState baseState,
            List<Turn1Action> playerActions,
            List<Turn1Action> kogaActions,
            String playerPairName,
            String presetId,
            List<String> aiDecisionLogs
    ) {
        // We simulate both min-roll and max-roll
        Turn1BattleState stateMin = baseState.copy();
        List<Turn1Action> allActionsMin = new ArrayList<>(playerActions);
        allActionsMin.addAll(kogaActions);
        List<String> logMin = Turn1ActionResolver.resolveActions(stateMin, allActionsMin, false);

        Turn1BattleState stateMax = baseState.copy();
        List<Turn1Action> allActionsMax = new ArrayList<>(playerActions);
        allActionsMax.addAll(kogaActions);
        List<String> logMax = Turn1ActionResolver.resolveActions(stateMax, allActionsMax, true);

        // Compute outcomes
        // Koga casualties: how many Koga Pokemon fainted
        int kogaCasualtiesMin = (stateMin.isFainted("koga_0") ? 1 : 0) + (stateMin.isFainted("koga_1") ? 1 : 0);
        int kogaCasualtiesMax = (stateMax.isFainted("koga_0") ? 1 : 0) + (stateMax.isFainted("koga_1") ? 1 : 0);
        int kogaCasualties = Math.max(kogaCasualtiesMin, kogaCasualtiesMax); // worst case for Koga: max casualties suffered
        int playerKnockoutsScored = kogaCasualties;

        // Player casualties: how many Player Pokemon fainted
        int playerCasualtiesMin = (stateMin.isFainted("player_0") ? 1 : 0) + (stateMin.isFainted("player_1") ? 1 : 0);
        int playerCasualtiesMax = (stateMax.isFainted("player_0") ? 1 : 0) + (stateMax.isFainted("player_1") ? 1 : 0);
        int playerCasualties = Math.min(playerCasualtiesMin, playerCasualtiesMax); // worst case for Koga: min player casualties scored
        int kogaKnockoutsScored = playerCasualties;

        double minKogaHpRemainingPercent = Math.min(stateMax.getHpPercent("koga_0"), stateMax.getHpPercent("koga_1")) * 100.0;
        double maxPlayerHpRemainingPercent = Math.max(stateMin.getHpPercent("player_0"), stateMin.getHpPercent("player_1")) * 100.0;

        Turn1Verdict verdict = evaluateVerdict(
                stateMin,
                stateMax,
                kogaKnockoutsScored,
                playerKnockoutsScored,
                minKogaHpRemainingPercent
        );

        String summary = String.format("[%s] Koga KOs scored: %d (Losses: %d), Player KOs scored: %d (Losses: %d), Min Koga HP: %.1f%%, Verdict: %s",
                presetId, kogaKnockoutsScored, kogaCasualties, playerKnockoutsScored, playerCasualties, minKogaHpRemainingPercent, verdict);

        List<String> combinedLog = new ArrayList<>(aiDecisionLogs);
        combinedLog.addAll(logMax);

        return new Turn1EvaluationResult(
                playerPairName,
                presetId,
                verdict,
                summary,
                kogaKnockoutsScored,
                playerKnockoutsScored,
                kogaCasualties,
                playerCasualties,
                minKogaHpRemainingPercent,
                maxPlayerHpRemainingPercent,
                combinedLog,
                stateMax
        );
    }

    /**
     * Unbiased objective outcome evaluator adhering strictly to plan criteria:
     * - GOOD: Net KO advantage OR dominant board control (neutralizes player control, both leads >= 70% min HP)
     * - QUESTIONABLE: Even trade (1-for-1), heavy damage taken without KO, roll/speed-tie dependent, or contested control
     * - CATASTROPHIC: Koga loses leads with 0 trades, OR player establishes unanswerable board control
     */
    public static Turn1Verdict evaluateVerdict(
            Turn1BattleState stateMin,
            Turn1BattleState stateMax,
            int kogaKnockoutsScored,
            int playerKnockoutsScored,
            double minKogaHpRemainingPercent
    ) {
        // Roll dependency check: if stateMin vs stateMax produces different casualty counts
        boolean kogaCasualtiesDiff = (stateMin.isFainted("koga_0") != stateMax.isFainted("koga_0"))
                || (stateMin.isFainted("koga_1") != stateMax.isFainted("koga_1"));
        boolean playerCasualtiesDiff = (stateMin.isFainted("player_0") != stateMax.isFainted("player_0"))
                || (stateMin.isFainted("player_1") != stateMax.isFainted("player_1"));

        if (kogaCasualtiesDiff || playerCasualtiesDiff) {
            return Turn1Verdict.QUESTIONABLE; // Roll-dependent outcome
        }

        // Check KO advantage
        if (kogaKnockoutsScored > playerKnockoutsScored && playerKnockoutsScored == 0) {
            return Turn1Verdict.GOOD;
        }

        if (playerKnockoutsScored > kogaKnockoutsScored) {
            return Turn1Verdict.CATASTROPHIC;
        }

        if (kogaKnockoutsScored > 0 && kogaKnockoutsScored == playerKnockoutsScored) {
            return Turn1Verdict.QUESTIONABLE; // 1-for-1 even trade
        }

        // 0 KOs on both sides: evaluate board control and HP bounds
        boolean playerUninterruptedTrickRoom = stateMax.isTrickRoom() && playerKnockoutsScored == 0 && kogaKnockoutsScored == 0;
        boolean playerTailwindActive = stateMax.isTailwindPlayer() && !stateMax.isTailwindKoga();

        if (playerUninterruptedTrickRoom || (playerTailwindActive && minKogaHpRemainingPercent < 40.0)) {
            return Turn1Verdict.CATASTROPHIC;
        }

        // Dominant board control without KOs: both Koga leads >= 70% HP, player control absent
        if (minKogaHpRemainingPercent >= 70.0 && !stateMax.isTrickRoom() && !stateMax.isTailwindPlayer()) {
            return Turn1Verdict.GOOD;
        }

        // Heavy damage taken (<50% min HP) or contested board
        if (minKogaHpRemainingPercent < 50.0) {
            return Turn1Verdict.QUESTIONABLE;
        }

        return Turn1Verdict.QUESTIONABLE;
    }

    private boolean isWorseForKoga(Turn1EvaluationResult a, Turn1EvaluationResult b) {
        // CATASTROPHIC is worse than QUESTIONABLE is worse than GOOD
        if (a.verdict() != b.verdict()) {
            return rankVerdict(a.verdict()) < rankVerdict(b.verdict());
        }
        // If same verdict, higher Koga casualties suffered is worse
        if (a.kogaCasualties() != b.kogaCasualties()) {
            return a.kogaCasualties() > b.kogaCasualties();
        }
        // If same, fewer player casualties (lower Koga KOs scored) is worse
        if (a.kogaKnockoutsScored() != b.kogaKnockoutsScored()) {
            return a.kogaKnockoutsScored() < b.kogaKnockoutsScored();
        }
        // If same, lower Koga remaining HP is worse
        return a.minKogaHpRemainingPercent() < b.minKogaHpRemainingPercent();
    }

    private int rankVerdict(Turn1Verdict v) {
        return switch (v) {
            case CATASTROPHIC -> 0;
            case QUESTIONABLE -> 1;
            case GOOD -> 2;
        };
    }

    public List<Turn1Action> determineKogaActions(Turn1BattleState state) {
        return determineKogaActions(state, null);
    }

    public List<Turn1Action> determineKogaActions(Turn1BattleState state, List<String> aiDecisionLogs) {
        List<Turn1Action> options0 = generateKogaOptions(state, "koga_0");
        List<Turn1Action> options1 = generateKogaOptions(state, "koga_1");

        Turn1Action bestA0 = options0.get(0);
        Turn1Action bestA1 = options1.get(0);
        int bestPairScore = Integer.MIN_VALUE;

        for (Turn1Action a0 : options0) {
            for (Turn1Action a1 : options1) {
                int score = evaluateKogaActionPair(state, a0, a1);
                if (score > bestPairScore) {
                    bestPairScore = score;
                    bestA0 = a0;
                    bestA1 = a1;
                }
            }
        }

        if (aiDecisionLogs != null) {
            CompetitivePokemonProfile mon0 = state.getProfileBySlot("koga_0");
            CompetitivePokemonProfile mon1 = state.getProfileBySlot("koga_1");
            logCandidateEvals(state, "koga_0", mon0, aiDecisionLogs);
            logCandidateEvals(state, "koga_1", mon1, aiDecisionLogs);
            aiDecisionLogs.add(String.format("[KOGA AI: coordinated pair selected: %s (%s on %s) + %s (%s on %s), pair score: %d]",
                    mon0.species(), bestA0.move().id(), bestA0.targetSlot(),
                    mon1.species(), bestA1.move().id(), bestA1.targetSlot(), bestPairScore));
        }

        return List.of(bestA0, bestA1);
    }

    private void logCandidateEvals(Turn1BattleState state, String slot, CompetitivePokemonProfile mon, List<String> logs) {
        List<String> evals = new ArrayList<>();
        for (MoveProfile move : mon.moves()) {
            if (move.isStatus()) continue;
            if (move.isSpread()) {
                DamageRange r0 = Turn1DamageCalculator.calculateDamage(mon, state.getProfileBySlot("player_0"), move, state, slot, "player_0");
                DamageRange r1 = Turn1DamageCalculator.calculateDamage(mon, state.getProfileBySlot("player_1"), move, state, slot, "player_1");
                evals.add(String.format("%s (spread): %d+%d=%d dmg", move.id(), r0.maxDamage(), r1.maxDamage(), r0.maxDamage() + r1.maxDamage()));
            } else {
                for (String tSlot : List.of("player_0", "player_1")) {
                    CompetitivePokemonProfile def = state.getProfileBySlot(tSlot);
                    DamageRange r = Turn1DamageCalculator.calculateDamage(mon, def, move, state, slot, tSlot);
                    evals.add(String.format("%s on %s: %d-%d dmg", move.id(), def.species(), r.minDamage(), r.maxDamage()));
                }
            }
        }
        logs.add(String.format("[KOGA AI: %s evaluated candidates: %s]", mon.species(), String.join(", ", evals)));
    }

    private List<Turn1Action> generateKogaOptions(Turn1BattleState state, String slot) {
        CompetitivePokemonProfile mon = state.getProfileBySlot(slot);
        List<Turn1Action> list = new ArrayList<>();
        int speed = Turn1SpeedResolver.calculateEffectiveSpeed(mon, state, slot);

        // 1. Redirection / Support
        MoveProfile followMe = mon.getMove("followme");
        if (followMe != null) {
            list.add(new Turn1Action(slot, followMe, slot, followMe.priority(), speed));
        }
        MoveProfile ragePowder = mon.getMove("ragepowder");
        if (ragePowder != null) {
            list.add(new Turn1Action(slot, ragePowder, slot, ragePowder.priority(), speed));
        }
        MoveProfile helpingHand = mon.getMove("helpinghand");
        if (helpingHand != null) {
            list.add(new Turn1Action(slot, helpingHand, "ally", helpingHand.priority(), speed));
        }

        // 2. Attacks
        for (MoveProfile move : mon.moves()) {
            if (move.isStatus()) continue;
            if (move.isSpread()) {
                list.add(new Turn1Action(slot, move, "all_opponents", move.priority(), speed));
            } else {
                list.add(new Turn1Action(slot, move, "player_0", move.priority(), speed));
                list.add(new Turn1Action(slot, move, "player_1", move.priority(), speed));
            }
        }

        if (list.isEmpty() && !mon.moves().isEmpty()) {
            list.add(new Turn1Action(slot, mon.moves().get(0), "player_0", mon.moves().get(0).priority(), speed));
        }

        return list;
    }

    private int evaluateKogaActionPair(Turn1BattleState state, Turn1Action a0, Turn1Action a1) {
        // Order the two actions by priority and speed
        Turn1Action first = a0;
        Turn1Action second = a1;

        int pCmp = Integer.compare(a1.priority(), a0.priority());
        if (pCmp > 0 || (pCmp == 0 && Turn1SpeedResolver.compareSpeed(a1.effectiveSpeed(), a0.effectiveSpeed(), state.isTrickRoom()) < 0)) {
            first = a1;
            second = a0;
        }

        int hp0 = state.getHp("player_0");
        int hp1 = state.getHp("player_1");

        // First action execution simulation
        int score = 0;
        boolean helpingHandBoosted = false;
        if (first.move().isStatus()) {
            score += 300; // Value for support moves like Rage Powder / Follow Me / Helping Hand
            if ("helpinghand".equals(first.move().id())) {
                helpingHandBoosted = true;
            }
        } else {
            score += evaluateActionAgainstHps(state, first, hp0, hp1, false);
            hp0 = applySimulatedDamage(state, first, "player_0", hp0);
            hp1 = applySimulatedDamage(state, first, "player_1", hp1);
        }

        // Second action execution simulation against remaining HPs
        if (second.move().isStatus()) {
            score += 300;
        } else {
            score += evaluateActionAgainstHps(state, second, hp0, hp1, helpingHandBoosted);
        }

        return score;
    }

    private int evaluateActionAgainstHps(Turn1BattleState state, Turn1Action action, int hp0, int hp1, boolean isHelpingHand) {
        CompetitivePokemonProfile attacker = state.getProfileBySlot(action.actorSlot());
        MoveProfile move = action.move();
        int score = 0;

        if (move.isSpread()) {
            for (String tSlot : List.of("player_0", "player_1")) {
                int currentHp = tSlot.equals("player_0") ? hp0 : hp1;
                if (currentHp <= 0) continue;
                CompetitivePokemonProfile def = state.getProfileBySlot(tSlot);
                DamageRange r = Turn1DamageCalculator.calculateDamage(attacker, def, move, state, action.actorSlot(), tSlot);
                int maxDmg = isHelpingHand ? (int)(r.maxDamage() * 1.5) : r.maxDamage();
                int minDmg = isHelpingHand ? (int)(r.minDamage() * 1.5) : r.minDamage();
                int dmg = Math.min(currentHp, maxDmg);
                score += dmg;
                score += (minDmg + maxDmg) / 4;
                if (maxDmg >= currentHp) {
                    score += 1000;
                    if (minDmg >= currentHp) score += 200;
                }
            }
        } else {
            String tSlot = action.targetSlot();
            int currentHp = tSlot.equals("player_0") ? hp0 : hp1;
            if (currentHp <= 0) {
                return -500; // Penalty for attacking an already-fainted target (overkill waste)
            }
            CompetitivePokemonProfile def = state.getProfileBySlot(tSlot);
            DamageRange r = Turn1DamageCalculator.calculateDamage(attacker, def, move, state, action.actorSlot(), tSlot);
            int maxDmg = isHelpingHand ? (int)(r.maxDamage() * 1.5) : r.maxDamage();
            int minDmg = isHelpingHand ? (int)(r.minDamage() * 1.5) : r.minDamage();
            int dmg = Math.min(currentHp, maxDmg);
            score += dmg;
            score += (minDmg + maxDmg) / 4;
            if (maxDmg >= currentHp) {
                score += 1000;
                if (minDmg >= currentHp) score += 200;
            }
        }

        return score;
    }

    private int applySimulatedDamage(Turn1BattleState state, Turn1Action action, String targetSlot, int currentHp) {
        if (currentHp <= 0) return 0;
        MoveProfile move = action.move();
        boolean applies = move.isSpread() || targetSlot.equals(action.targetSlot());
        if (!applies) return currentHp;

        CompetitivePokemonProfile attacker = state.getProfileBySlot(action.actorSlot());
        CompetitivePokemonProfile def = state.getProfileBySlot(targetSlot);
        DamageRange r = Turn1DamageCalculator.calculateDamage(attacker, def, move, state, action.actorSlot(), targetSlot);

        // Account for Disguise
        if (def.hasAbility("disguise") && currentHp == def.actualStats().hp() && r.maxDamage() > 0) {
            int disguiseDmg = Math.max(1, def.actualStats().hp() / 8);
            return Math.max(0, currentHp - disguiseDmg);
        }

        // Account for Focus Sash if defender is at full HP
        boolean hasFocusSash = def.hasItem("focus_sash") && currentHp == def.actualStats().hp();
        if (hasFocusSash && r.maxDamage() >= currentHp) {
            return 1;
        }

        return Math.max(0, currentHp - r.maxDamage());
    }

    public List<List<Turn1Action>> generatePlayerLines(Turn1BattleState state) {
        CompetitivePokemonProfile p0 = state.getProfileBySlot("player_0");
        CompetitivePokemonProfile p1 = state.getProfileBySlot("player_1");

        List<Turn1Action> options0 = generatePlayerOptions(state, "player_0", p0);
        List<Turn1Action> options1 = generatePlayerOptions(state, "player_1", p1);

        List<List<Turn1Action>> lines = new ArrayList<>();
        for (Turn1Action a0 : options0) {
            for (Turn1Action a1 : options1) {
                lines.add(List.of(a0, a1));
            }
        }
        return lines.isEmpty() ? List.of(List.of(
                new Turn1Action("player_0", p0.moves().get(0), "koga_0", 0, 100),
                new Turn1Action("player_1", p1.moves().get(0), "koga_1", 0, 100)
        )) : lines;
    }

    private List<Turn1Action> generatePlayerOptions(Turn1BattleState state, String slot, CompetitivePokemonProfile mon) {
        List<Turn1Action> list = new ArrayList<>();
        int speed = Turn1SpeedResolver.calculateEffectiveSpeed(mon, state, slot);

        // 1. Redirection (Follow Me / Rage Powder)
        MoveProfile followMe = mon.getMove("followme");
        if (followMe != null) {
            list.add(new Turn1Action(slot, followMe, slot, followMe.priority(), speed));
        }
        MoveProfile ragePowder = mon.getMove("ragepowder");
        if (ragePowder != null) {
            list.add(new Turn1Action(slot, ragePowder, slot, ragePowder.priority(), speed));
        }

        // 2. Helping Hand
        MoveProfile helpingHand = mon.getMove("helpinghand");
        if (helpingHand != null) {
            list.add(new Turn1Action(slot, helpingHand, "ally", helpingHand.priority(), speed));
        }

        // 3. Speed Control (Tailwind / Trick Room)
        MoveProfile tailwind = mon.getMove("tailwind");
        if (tailwind != null) {
            list.add(new Turn1Action(slot, tailwind, slot, tailwind.priority(), speed));
        }
        MoveProfile trickRoom = mon.getMove("trickroom");
        if (trickRoom != null) {
            list.add(new Turn1Action(slot, trickRoom, slot, trickRoom.priority(), speed));
        }

        // 4. Fake Out
        MoveProfile fakeOut = mon.getMove("fakeout");
        if (fakeOut != null) {
            for (String targetSlot : List.of("koga_0", "koga_1")) {
                list.add(new Turn1Action(slot, fakeOut, targetSlot, fakeOut.priority(), speed));
            }
        }

        // 5. Attacking options: highest single target against koga_0, against koga_1, and spread
        MoveProfile bestSpread = null;
        int maxSpreadDmg = -1;

        MoveProfile bestVsKoga0 = null;
        int maxDmg0 = -1;

        MoveProfile bestVsKoga1 = null;
        int maxDmg1 = -1;

        for (MoveProfile move : mon.moves()) {
            if (move.isStatus()) continue;

            boolean isExpandingForceSpread = "expandingforce".equals(move.id())
                    && state.getTerrain() == Turn1BattleState.Terrain.PSYCHIC
                    && Turn1DamageCalculator.isGrounded(mon);

            if (move.isSpread() || isExpandingForceSpread) {
                DamageRange r0 = Turn1DamageCalculator.calculateDamage(mon, state.getProfileBySlot("koga_0"), move, state, slot, "koga_0");
                DamageRange r1 = Turn1DamageCalculator.calculateDamage(mon, state.getProfileBySlot("koga_1"), move, state, slot, "koga_1");
                int tot = r0.maxDamage() + r1.maxDamage();
                if (tot > maxSpreadDmg) {
                    maxSpreadDmg = tot;
                    bestSpread = move;
                }
            } else {
                DamageRange r0 = Turn1DamageCalculator.calculateDamage(mon, state.getProfileBySlot("koga_0"), move, state, slot, "koga_0");
                if (r0.maxDamage() > maxDmg0) {
                    maxDmg0 = r0.maxDamage();
                    bestVsKoga0 = move;
                }

                DamageRange r1 = Turn1DamageCalculator.calculateDamage(mon, state.getProfileBySlot("koga_1"), move, state, slot, "koga_1");
                if (r1.maxDamage() > maxDmg1) {
                    maxDmg1 = r1.maxDamage();
                    bestVsKoga1 = move;
                }
            }
        }

        if (bestSpread != null) {
            list.add(new Turn1Action(slot, bestSpread, "all_opponents", bestSpread.priority(), speed));
        }
        if (bestVsKoga0 != null) {
            list.add(new Turn1Action(slot, bestVsKoga0, "koga_0", bestVsKoga0.priority(), speed));
        }
        if (bestVsKoga1 != null && bestVsKoga1 != bestVsKoga0) {
            list.add(new Turn1Action(slot, bestVsKoga1, "koga_1", bestVsKoga1.priority(), speed));
        }

        // If list is still empty, add first move
        if (list.isEmpty() && !mon.moves().isEmpty()) {
            list.add(new Turn1Action(slot, mon.moves().get(0), "koga_0", mon.moves().get(0).priority(), speed));
        }

        return list;
    }

    private Turn1EvaluationResult createFallbackResult(Turn1BattleState state, String pairName, String presetId) {
        return new Turn1EvaluationResult(
                pairName,
                presetId,
                Turn1Verdict.QUESTIONABLE,
                "Fallback evaluation",
                0,
                0,
                0,
                0,
                100.0,
                100.0,
                List.of(),
                state
        );
    }
}
