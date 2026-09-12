package com.cobbleverse.legendaryrule.strategy.switchai;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.types.ElementalType;
import com.cobblemon.mod.common.battles.ActiveBattlePokemon;
import com.cobblemon.mod.common.battles.MoveTarget;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.gitlab.srcmc.rctapi.api.ai.utils.BattleEffects;
import com.gitlab.srcmc.rctapi.api.ai.utils.BattleStates;
import com.gitlab.surilexa.rbrctai.api.ai.RunBunAI;
import com.gitlab.surilexa.rbrctai.api.ai.utils.PokeMathMax;
import com.gitlab.surilexa.rbrctai.api.ai.utils.RBStatStages;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Pure strategy detector for Phase 1 switch evaluation:
 * 1. isLowOffensivePressure: determines whether AI active Pokémon's damage output is strictly below threshold (< 20%).
 * 2. hasMeaningfulSupportUtility: determines whether AI active Pokémon has meaningful Doubles support utility (Follow Me, Rage Powder, Helping Hand).
 * 3. isDeadPosition: true if low offensive pressure (< 20%) AND no meaningful support utility.
 * 4. isUnderCriticalThreat: determines whether all eligible active opponents can OHKO the active Pokémon.
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
     * If dead position is detected (low pressure with no meaningful support), the 25% random veto is bypassed (returns 0.0d).
     * If dead position is false (including critical threat alone, native hasLowScore alone,
     * support position, or gray zone 20-33%), preserves native random value untouched.
     */
    public static double resolveGate3RandomValue(double nativeVal, boolean deadPosition) {
        if (!(nativeVal < 0.75d) && deadPosition) {
            return 0.0d;
        }
        return nativeVal;
    }

    /**
     * True if AI active Pokemon has low offensive pressure (< 20% max HP on all opponents)
     * AND does NOT have meaningful strategic support utility for an active partner.
     */
    public static boolean isDeadPosition(
        List<RunBunAI.MoveEvaluation> evaluations,
        BattlePokemon self,
        List<ActiveBattlePokemon> opponents,
        ActiveBattlePokemon activeBattlePokemon,
        RBStatStages stages
    ) {
        if (!isLowOffensivePressure(evaluations, opponents)) {
            return false;
        }
        return !hasMeaningfulSupportUtility(evaluations, self, opponents, activeBattlePokemon, stages);
    }

    /**
     * Overload for testability allowing explicit active allies list.
     */
    public static boolean isDeadPosition(
        List<RunBunAI.MoveEvaluation> evaluations,
        BattlePokemon self,
        List<ActiveBattlePokemon> allies,
        List<ActiveBattlePokemon> opponents,
        ActiveBattlePokemon activeBattlePokemon,
        RBStatStages stages
    ) {
        if (!isLowOffensivePressure(evaluations, opponents)) {
            return false;
        }
        return !hasMeaningfulSupportUtility(evaluations, self, allies, opponents, activeBattlePokemon, stages);
    }

    /**
     * Resolves active allies from ActiveBattlePokemon and evaluates meaningful support utility.
     */
    public static boolean hasMeaningfulSupportUtility(
        List<RunBunAI.MoveEvaluation> evaluations,
        BattlePokemon self,
        List<ActiveBattlePokemon> opponents,
        ActiveBattlePokemon activeBattlePokemon,
        RBStatStages stages
    ) {
        List<ActiveBattlePokemon> allies = resolveActiveAllies(activeBattlePokemon);
        return hasMeaningfulSupportUtility(evaluations, self, allies, opponents, activeBattlePokemon, stages);
    }

    /**
     * Determines whether AI active Pokémon has meaningful strategic support utility for an active partner:
     * 1. Evaluates presence of support moves (Follow Me, Rage Powder, Helping Hand) on self.
     *    Protect/Detect strictly do NOT count as meaningful support utility.
     * 2. Verifies at least one living active ally exists on the field (Doubles requirement).
     * 3. Helping Hand: requires active ally to possess a meaningful offensive attack (lethal KO or >= 33% max HP).
     * 4. Follow Me: requires meaningful ally offense AND at least one redirectable active opposing threat.
     * 5. Rage Powder: requires meaningful ally offense AND at least one powder-redirectable active opposing threat
     *    (accounting for Grass-type, Overcoat, Safety Goggles, and Stalwart/Propeller Tail immunities).
     */
    public static boolean hasMeaningfulSupportUtility(
        List<RunBunAI.MoveEvaluation> evaluations,
        BattlePokemon self,
        List<ActiveBattlePokemon> allies,
        List<ActiveBattlePokemon> opponents,
        ActiveBattlePokemon activeBattlePokemon,
        RBStatStages stages
    ) {
        // 1. Identify support moves on self (Protect/Detect are strictly excluded)
        boolean hasFollowMe = hasSupportMove(evaluations, self, "followme");
        boolean hasRagePowder = hasSupportMove(evaluations, self, "ragepowder");
        boolean hasHelpingHand = hasSupportMove(evaluations, self, "helpinghand");

        if (!hasFollowMe && !hasRagePowder && !hasHelpingHand) {
            return false;
        }

        // 2. Active living ally check (Doubles requirement)
        if (allies == null || allies.isEmpty()) {
            return false;
        }
        BattlePokemon livingAllyBP = null;
        for (ActiveBattlePokemon ally : allies) {
            if (ally == null || ally.isGone()) continue;
            BattlePokemon abp = ally.getBattlePokemon();
            if (abp != null && abp.getHealth() > 0) {
                livingAllyBP = abp;
                break;
            }
        }
        if (livingAllyBP == null) {
            return false;
        }

        // 3. Active living opponents check
        if (opponents == null || opponents.isEmpty()) {
            return false;
        }
        List<ActiveBattlePokemon> livingOpponents = new ArrayList<>();
        for (ActiveBattlePokemon opp : opponents) {
            if (opp == null || opp.isGone()) continue;
            BattlePokemon oppBP = opp.getBattlePokemon();
            if (oppBP != null && oppBP.getHealth() > 0 && oppBP.getMaxHealth() > 0) {
                livingOpponents.add(opp);
            }
        }
        if (livingOpponents.isEmpty()) {
            return false;
        }

        // 4. Evaluate ally offensive threat
        boolean allyHasMeaningfulOffense = hasMeaningfulAllyOffense(livingAllyBP, livingOpponents, activeBattlePokemon, stages);

        // 5. Helping Hand: requires meaningful ally offensive action to amplify
        if (hasHelpingHand && allyHasMeaningfulOffense) {
            return true;
        }

        // 6. Follow Me: requires meaningful ally offense AND at least one redirectable opponent
        if (hasFollowMe && allyHasMeaningfulOffense) {
            for (ActiveBattlePokemon opp : livingOpponents) {
                if (canRedirectOpponent(opp.getBattlePokemon(), false)) {
                    return true;
                }
            }
        }

        // 7. Rage Powder: requires meaningful ally offense AND at least one powder-redirectable opponent
        if (hasRagePowder && allyHasMeaningfulOffense) {
            for (ActiveBattlePokemon opp : livingOpponents) {
                if (canRedirectOpponent(opp.getBattlePokemon(), true)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Resolves active allies on the field from ActiveBattlePokemon safely and statelessly.
     */
    public static List<ActiveBattlePokemon> resolveActiveAllies(ActiveBattlePokemon activeBattlePokemon) {
        if (activeBattlePokemon == null) {
            return Collections.emptyList();
        }
        try {
            if (activeBattlePokemon.getBattle() != null && activeBattlePokemon.getBattle().getActivePokemon() != null) {
                List<ActiveBattlePokemon> allies = new ArrayList<>();
                for (ActiveBattlePokemon abp : activeBattlePokemon.getBattle().getActivePokemon()) {
                    if (abp != null && !abp.equals(activeBattlePokemon) && activeBattlePokemon.isAllied(abp)) {
                        allies.add(abp);
                    }
                }
                return allies;
            }
        } catch (Throwable ignored) {
        }
        try {
            if (activeBattlePokemon.getAllActivePokemon() != null) {
                List<ActiveBattlePokemon> allies = new ArrayList<>();
                for (ActiveBattlePokemon abp : activeBattlePokemon.getAllActivePokemon()) {
                    if (abp != null && !abp.equals(activeBattlePokemon) && activeBattlePokemon.isAllied(abp)) {
                        allies.add(abp);
                    }
                }
                return allies;
            }
        } catch (Throwable ignored) {
        }
        return Collections.emptyList();
    }

    /**
     * Checks if the active Pokémon has a viable support move in its evaluated moves or moveset.
     */
    private static boolean hasSupportMove(List<RunBunAI.MoveEvaluation> evaluations, BattlePokemon self, String targetName) {
        if (evaluations != null && !evaluations.isEmpty()) {
            boolean evaluatedForTarget = false;
            for (RunBunAI.MoveEvaluation eval : evaluations) {
                if (eval != null && eval.getMove() != null && isMoveNamed(eval.getMove(), targetName)) {
                    evaluatedForTarget = true;
                    // Moves with viable scores (> -5) are usable/enabled
                    if (eval.getScore() > -5) {
                        return true;
                    }
                }
            }
            // If the move was explicitly evaluated by AI and none had score > -5,
            // the move is unavailable (e.g. Taunt, Disable, Torment, Choice lock).
            // Evaluated unusable moves MUST NOT fall back to raw moveset.
            if (evaluatedForTarget) {
                return false;
            }
        }
        if (self != null && self.getMoveSet() != null && self.getMoveSet().getMoves() != null) {
            for (Move m : self.getMoveSet().getMoves()) {
                if (m != null && isMoveNamed(m, targetName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isMoveNamed(Move move, String targetName) {
        if (move == null || move.getName() == null) {
            return false;
        }
        String name = move.getName().toLowerCase(Locale.ROOT).replace("_", "").replace(" ", "").replace("-", "");
        return name.equals(targetName);
    }

    /**
     * Evaluates whether the ally has a meaningful damaging action (lethal KO or >= 33% max HP)
     * into at least one active eligible opponent.
     */
    public static boolean hasMeaningfulAllyOffense(
        BattlePokemon ally,
        List<ActiveBattlePokemon> opponents,
        ActiveBattlePokemon activeBattlePokemon,
        RBStatStages stages
    ) {
        if (ally == null || opponents == null || opponents.isEmpty()) {
            return false;
        }
        List<Move> moves = ally.getMoveSet() != null ? ally.getMoveSet().getMoves() : Collections.emptyList();
        if (moves.isEmpty()) {
            return false;
        }
        for (ActiveBattlePokemon opp : opponents) {
            if (opp == null || opp.isGone()) continue;
            BattlePokemon oppBP = opp.getBattlePokemon();
            if (oppBP == null || oppBP.getHealth() <= 0 || oppBP.getMaxHealth() <= 0) continue;

            for (Move move : moves) {
                if (move == null) continue;
                int dmg = evaluateAllyMoveDamage(ally, oppBP, move, activeBattlePokemon, stages);
                if (dmg >= oppBP.getHealth()) {
                    return true; // Lethal KO
                }
                double ratio = (double) dmg / (double) oppBP.getMaxHealth();
                if (ratio >= STAY_JUSTIFICATION_THRESHOLD) {
                    return true; // >= 33% max HP meaningful offensive pressure
                }
            }
        }
        return false;
    }

    private static int evaluateAllyMoveDamage(
        BattlePokemon ally,
        BattlePokemon oppBP,
        Move move,
        ActiveBattlePokemon abp,
        RBStatStages stages
    ) {
        try {
            int dmg = SwitchCandidateScorer.evaluateDamage(ally, oppBP, move, abp, stages);
            if (dmg >= 0) {
                return dmg;
            }
        } catch (Throwable ignored) {
        }
        return fallbackDamageEstimate(ally, oppBP, move);
    }

    private static int fallbackDamageEstimate(BattlePokemon attacker, BattlePokemon defender, Move move) {
        if (move == null || move.getTemplate() == null) {
            return 0;
        }
        if (move.getDamageCategory() != null && "status".equalsIgnoreCase(move.getDamageCategory().getName())) {
            return 0;
        }
        double power = move.getTemplate().getPower();
        if (power <= 0) {
            return 0;
        }
        return (int) Math.round(power);
    }

    /**
     * Determines whether an opponent threat can have its attacks redirected:
     * - Stalwart and Propeller Tail ignore redirection.
     * - For Rage Powder: Grass-types, Overcoat, and Safety Goggles are immune.
     * - Spread moves (allAdjacentFoes, allAdjacent) and Snipe Shot cannot be redirected.
     */
    public static boolean canRedirectOpponent(BattlePokemon oppBP, boolean isRagePowder) {
        if (oppBP == null || oppBP.getHealth() <= 0) {
            return false;
        }

        // 1. Redirection-ignoring abilities (Stalwart, Propeller Tail)
        if (hasAnyAbility(oppBP, List.of("stalwart", "propellertail"))) {
            return false;
        }

        // 2. Rage Powder specific immunities
        if (isRagePowder) {
            // Grass-type Pokemon are immune to powder moves (Gen 6+)
            if (isGrassType(oppBP)) {
                return false;
            }
            // Overcoat ability grants powder immunity
            if (hasAbility(oppBP, "overcoat")) {
                return false;
            }
            // Safety Goggles held item grants powder immunity
            String heldItem = null;
            try {
                if (oppBP.getHeldItemManager() != null) {
                    heldItem = oppBP.getHeldItemManager().showdownId(oppBP);
                }
            } catch (Throwable ignored) {
            }
            if ("safetygoggles".equalsIgnoreCase(heldItem)) {
                return false;
            }
        }

        // 3. Opponent moveset check (if moveset is present and non-empty)
        if (oppBP.getMoveSet() != null && oppBP.getMoveSet().getMoves() != null && !oppBP.getMoveSet().getMoves().isEmpty()) {
            boolean hasRedirectableMove = false;
            for (Move move : oppBP.getMoveSet().getMoves()) {
                if (move == null) continue;
                if (isMoveNamed(move, "snipeshot")) {
                    continue;
                }
                if (move.getTemplate() != null) {
                    MoveTarget target = move.getTemplate().getTarget();
                    if (target == MoveTarget.allAdjacentFoes ||
                        target == MoveTarget.allAdjacent ||
                        target == MoveTarget.all ||
                        target == MoveTarget.self ||
                        target == MoveTarget.allySide ||
                        target == MoveTarget.allyTeam ||
                        target == MoveTarget.adjacentAlly ||
                        target == MoveTarget.adjacentAllyOrSelf) {
                        continue;
                    }
                }
                hasRedirectableMove = true;
                break;
            }
            if (!hasRedirectableMove) {
                return false;
            }
        }

        return true;
    }

    /**
     * Checks if a Pokémon is Grass-type (primary, secondary, or active Terastallization).
     */
    public static boolean isGrassType(BattlePokemon bp) {
        if (bp == null) {
            return false;
        }
        Pokemon pokemon = bp.getEffectedPokemon();
        if (pokemon == null) {
            return false;
        }
        try {
            if (bp.getActor() != null && bp.getActor().getBattle() != null) {
                PokemonBattle battle = (PokemonBattle) bp.getActor().getBattle();
                if (BattleStates.get(battle).getPokemonState(bp).has(BattleEffects.Custom.TERA)) {
                    ElementalType teraType = PokeMathMax.teraToElementalType(bp);
                    return isGrass(teraType);
                }
            }
        } catch (Throwable ignored) {
        }
        try {
            if (isGrass(pokemon.getPrimaryType()) || isGrass(pokemon.getSecondaryType())) {
                return true;
            }
        } catch (Throwable ignored) {
        }
        try {
            if (pokemon.getTypes() != null) {
                for (ElementalType t : pokemon.getTypes()) {
                    if (isGrass(t)) {
                        return true;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static boolean isGrass(ElementalType type) {
        if (type == null) {
            return false;
        }
        return "grass".equalsIgnoreCase(type.getName());
    }

    private static boolean hasAbility(BattlePokemon bp, String abilityName) {
        if (bp == null || abilityName == null) {
            return false;
        }
        Pokemon pokemon = bp.getEffectedPokemon();
        if (pokemon == null) {
            pokemon = bp.getOriginalPokemon();
        }
        if (pokemon == null) {
            return false;
        }
        try {
            return PokeMathMax.hasAbility(abilityName, bp);
        } catch (Throwable ignored) {
            try {
                if (pokemon.getAbility() != null && pokemon.getAbility().getName() != null) {
                    return abilityName.equalsIgnoreCase(pokemon.getAbility().getName());
                }
            } catch (Throwable ignored2) {
            }
            return false;
        }
    }

    private static boolean hasAnyAbility(BattlePokemon bp, List<String> abilityNames) {
        if (bp == null || abilityNames == null || abilityNames.isEmpty()) {
            return false;
        }
        for (String ab : abilityNames) {
            if (hasAbility(bp, ab)) {
                return true;
            }
        }
        return false;
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
