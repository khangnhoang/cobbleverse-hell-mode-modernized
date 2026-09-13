package com.cobbleverse.legendaryrule.strategy.spread;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.types.ElementalType;
import com.cobblemon.mod.common.battles.ActiveBattlePokemon;
import com.cobblemon.mod.common.battles.MoveTarget;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobbleverse.legendaryrule.LegendaryRuleMod;
import com.cobbleverse.legendaryrule.strategy.diagnostic.AIDecisionDiagnostics;
import com.cobbleverse.legendaryrule.strategy.switchai.AbilityImmunityTable;
import com.gitlab.surilexa.rbrctai.api.ai.RunBunAI;
import com.gitlab.surilexa.rbrctai.api.ai.utils.PokeMathMax;
import com.gitlab.surilexa.rbrctai.api.ai.utils.RBBattleSlots;
import com.gitlab.surilexa.rbrctai.api.ai.utils.RBStatStages;

import java.util.*;

/**
 * Evaluates friendly-fire impact of damaging allAdjacent spread moves (Earthquake, Surf,
 * Discharge, Sludge Wave, Boomburst) on an active alive Doubles partner.
 *
 * Invariants:
 * 1. If ally is genuinely immune (Flying, Levitate, Air Balloon, Water Absorb, etc.):
 *    friendlyFirePenalty = 0. Zero new bonus added.
 * 2. If ally takes damage:
 *    Continuous penalty derived from actual expected damage and normalized HP impact.
 * 3. If ally is KO'd (allyDamage >= currentAllyHP):
 *    KO_PENALTY (25) is applied, strongly overcoming native damage/move bonuses.
 * 4. Count friendly fire once:
 *    Multiple opponent evaluations for the same allAdjacent move action share the exact same
 *    penalty P. Since RunBunAI selects via max(eval.getScore()), the cost is deducted exactly once.
 */
public final class SpreadFriendlyFireValuationStrategy {

    public static final int KO_PENALTY = 25;
    public static final double CONTINUOUS_PENALTY_SCALE = 16.0;

    private SpreadFriendlyFireValuationStrategy() {
    }

    /**
     * Adjusts move evaluations for damaging allAdjacent spread moves if an alive Doubles ally is present.
     *
     * @param evaluations candidate move evaluations
     * @param attacker attacking BattlePokemon
     * @param activeBattlePokemon active battle slot
     * @param battle PokemonBattle instance (may be null)
     * @param statStages current stat stages (may be null)
     */
    public static void adjustFriendlyFireValuations(
        List<RunBunAI.MoveEvaluation> evaluations,
        BattlePokemon attacker,
        ActiveBattlePokemon activeBattlePokemon,
        PokemonBattle battle,
        RBStatStages statStages
    ) {
        if (evaluations == null || evaluations.isEmpty() || attacker == null || activeBattlePokemon == null) {
            return;
        }

        ActiveBattlePokemon partnerActive = resolveAlivePartner(activeBattlePokemon);
        if (partnerActive == null) {
            return;
        }
        BattlePokemon partnerBP = partnerActive.getBattlePokemon();
        if (partnerBP == null || partnerBP.getHealth() <= 0) {
            return;
        }

        // Group evaluations by distinct Move to ensure penalty is computed once per action
        Map<Move, List<RunBunAI.MoveEvaluation>> allAdjacentGroups = new HashMap<>();
        for (RunBunAI.MoveEvaluation eval : evaluations) {
            if (eval == null) continue;
            Move move = eval.getMove();
            if (move == null || move.getTemplate() == null) continue;

            if (move.getTemplate().getTarget() == MoveTarget.allAdjacent && isDamaging(move)) {
                allAdjacentGroups.computeIfAbsent(move, k -> new ArrayList<>()).add(eval);
            }
        }

        if (allAdjacentGroups.isEmpty()) {
            return;
        }

        String attackerName = AIDecisionDiagnostics.getPokemonName(attacker);
        String partnerName = AIDecisionDiagnostics.getPokemonName(partnerBP);

        for (Map.Entry<Move, List<RunBunAI.MoveEvaluation>> entry : allAdjacentGroups.entrySet()) {
            Move move = entry.getKey();
            List<RunBunAI.MoveEvaluation> evals = entry.getValue();
            String moveName = AIDecisionDiagnostics.getMoveName(move);

            FriendlyFireResult result = evaluateFriendlyFire(move, attacker, partnerActive, activeBattlePokemon, statStages);

            if (result.immune) {
                LegendaryRuleMod.LOGGER.info("[AI-FF] attacker={} move={} partner={} immune=true allyDamage=0 penalty=0",
                    attackerName, moveName, partnerName);
            } else {
                LegendaryRuleMod.LOGGER.info("[AI-FF] attacker={} move={} partner={} immune=false allyDamage={} allyHp={}/{} penalty={}",
                    attackerName, moveName, partnerName, result.allyDamage, result.currentHp, result.maxHp, result.penalty);
            }

            if (result.penalty > 0) {
                for (RunBunAI.MoveEvaluation eval : evals) {
                    int scoreBefore = eval.getScore();
                    int scoreAfter = scoreBefore - result.penalty;
                    eval.setScore(scoreAfter);
                    LegendaryRuleMod.LOGGER.info("[AI-FF] move={} scoreBefore={} scoreAfter={} target={}",
                        moveName, scoreBefore, scoreAfter, AIDecisionDiagnostics.getTargetName(eval));
                }
            }
        }
    }

    /**
     * Result of friendly-fire evaluation capturing intermediate calculation details for non-mutating observability.
     */
    public static final class FriendlyFireResult {
        public final boolean immune;
        public final int allyDamage;
        public final int currentHp;
        public final int maxHp;
        public final int penalty;

        public FriendlyFireResult(boolean immune, int allyDamage, int currentHp, int maxHp, int penalty) {
            this.immune = immune;
            this.allyDamage = allyDamage;
            this.currentHp = currentHp;
            this.maxHp = maxHp;
            this.penalty = penalty;
        }
    }

    /**
     * Evaluates the friendly-fire impact for a damaging allAdjacent move against an alive partner,
     * returning all intermediate calculation details for non-mutating observability.
     */
    public static FriendlyFireResult evaluateFriendlyFire(
        Move move,
        BattlePokemon attacker,
        ActiveBattlePokemon partnerActive,
        ActiveBattlePokemon activeBattlePokemon,
        RBStatStages statStages
    ) {
        if (move == null || attacker == null || partnerActive == null) {
            return new FriendlyFireResult(false, 0, 0, 0, 0);
        }
        BattlePokemon partnerBP = partnerActive.getBattlePokemon();
        if (partnerBP == null || partnerBP.getHealth() <= 0) {
            return new FriendlyFireResult(false, 0, 0, 0, 0);
        }

        int currentHp = partnerBP.getHealth();
        int maxHp = partnerBP.getMaxHealth();

        // 1. Existing battle semantics immunity check
        if (isPartnerImmune(move, attacker, partnerBP, activeBattlePokemon)) {
            return new FriendlyFireResult(true, 0, currentHp, maxHp, 0);
        }

        // 2. Expected damage calculation onto partner
        int allyDamage = PokeMathMax.damage(
            attacker,
            partnerBP,
            move,
            activeBattlePokemon,
            false,
            true,
            statStages
        );

        if (allyDamage <= 0) {
            return new FriendlyFireResult(false, 0, currentHp, maxHp, 0);
        }

        if (currentHp <= 0 || maxHp <= 0) {
            return new FriendlyFireResult(false, allyDamage, currentHp, maxHp, 0);
        }

        // 3. Ally KO
        if (allyDamage >= currentHp) {
            return new FriendlyFireResult(false, allyDamage, currentHp, maxHp, KO_PENALTY);
        }

        // 4. Continuous penalty based on normalized HP loss
        double maxHpLoss = (double) allyDamage / (double) maxHp;
        double currentHpLoss = (double) allyDamage / (double) currentHp;
        double normalizedHpLoss = Math.min(1.0, Math.max(maxHpLoss, currentHpLoss));

        int penalty = (int) Math.round(CONTINUOUS_PENALTY_SCALE * normalizedHpLoss);
        return new FriendlyFireResult(false, allyDamage, currentHp, maxHp, Math.max(1, penalty));
    }

    /**
     * Computes the friendly-fire penalty for a damaging allAdjacent move against an alive partner.
     */
    public static int computeFriendlyFirePenalty(
        Move move,
        BattlePokemon attacker,
        ActiveBattlePokemon partnerActive,
        ActiveBattlePokemon activeBattlePokemon,
        RBStatStages statStages
    ) {
        return evaluateFriendlyFire(move, attacker, partnerActive, activeBattlePokemon, statStages).penalty;
    }

    /**
     * Checks whether the partner is immune to the move via typing, ability, or raised state.
     */
    public static boolean isPartnerImmune(
        Move move,
        BattlePokemon attacker,
        BattlePokemon partnerBP,
        ActiveBattlePokemon activeBattlePokemon
    ) {
        if (move == null || attacker == null || partnerBP == null) {
            return false;
        }

        try {
            if (PokeMathMax.isImmuneCheck(
                move,
                attacker,
                partnerBP,
                activeBattlePokemon,
                PokeMathMax.teraToElementalType(partnerBP),
                false
            )) {
                return true;
            }
        } catch (Throwable ignored) {
        }

        String ability = getAbilityName(partnerBP);
        ElementalType moveType = move.getType();
        return AbilityImmunityTable.isImmune(ability, moveType);
    }

    /**
     * Resolves the alive Doubles partner for the given active battle slot.
     */
    public static ActiveBattlePokemon resolveAlivePartner(ActiveBattlePokemon source) {
        if (source == null || source.getBattle() == null) {
            return null;
        }
        try {
            List<ActiveBattlePokemon> allies = RBBattleSlots.getAllies(source);
            if (allies != null) {
                for (ActiveBattlePokemon ally : allies) {
                    if (ally != null && ally.isAlive() && ally.getBattlePokemon() != null && ally.getBattlePokemon().getHealth() > 0) {
                        return ally;
                    }
                }
            }
        } catch (Throwable ignored) {
        }

        try {
            Iterable<ActiveBattlePokemon> actives = source.getBattle().getActivePokemon();
            if (actives != null) {
                for (ActiveBattlePokemon abp : actives) {
                    if (abp != null && !abp.equals(source) && source.isAllied(abp)) {
                        BattlePokemon bp = abp.getBattlePokemon();
                        if (bp != null && bp.getHealth() > 0) {
                            return abp;
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }

        return null;
    }

    /**
     * Returns true if the move deals direct damage (i.e. not a status move).
     */
    public static boolean isDamaging(Move move) {
        if (move == null) return false;
        try {
            if (move.getDamageCategory() != null) {
                String cat = move.getDamageCategory().getName();
                if ("status".equalsIgnoreCase(cat)) {
                    return false;
                }
            }
            return move.getPower() > 0;
        } catch (Throwable ignored) {
            return move.getPower() > 0;
        }
    }

    private static String getAbilityName(BattlePokemon bp) {
        if (bp == null) return "";
        try {
            if (bp.getEffectedPokemon() != null && bp.getEffectedPokemon().getAbility() != null) {
                return bp.getEffectedPokemon().getAbility().getName();
            }
        } catch (Throwable ignored) {
        }
        return "";
    }
}
