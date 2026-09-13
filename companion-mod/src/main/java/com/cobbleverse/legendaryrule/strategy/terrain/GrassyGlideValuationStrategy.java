package com.cobbleverse.legendaryrule.strategy.terrain;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.battles.ActiveBattlePokemon;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.api.types.ElementalType;
import com.cobblemon.mod.common.api.types.ElementalTypes;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.gitlab.srcmc.rctapi.api.ai.utils.BattleEffects;
import com.gitlab.surilexa.rbrctai.api.ai.RunBunAI;
import com.gitlab.surilexa.rbrctai.api.ai.utils.PokeMathMax;
import com.gitlab.surilexa.rbrctai.api.ai.utils.RBStatStages;

import java.util.*;

/**
 * Strategy to dynamically value Grassy Glide priority under active Grassy Terrain.
 *
 * Invariants:
 * 1. Grassy Glide has +1 priority IF AND ONLY IF Grassy Terrain is active AND attacker is grounded (not raised).
 *    Outside Grassy Terrain or when raised, Grassy Glide is strictly normal-priority (zero score adjustments).
 * 2. Conceptual Hierarchy:
 *    guaranteed priority KO
 *    > guaranteed normal KO that can actually execute
 *    > priority damage when normal move likely never executes
 *    > large normal damage
 *    > weak priority chip
 *
 * Case A (Both moves OHKO):
 *    Grassy Glide is a guaranteed priority KO, strictly dominating normal KO moves against that target (Glide > Hammer).
 * Case B (Glide OHKOs, normal move does not):
 *    Glide wins naturally (ensured score >= 12).
 * Case C (Normal move OHKOs, Glide does not):
 *    - If attacker is faster, OR target cannot lethal-KO attacker: normal move executes safely and delivers KO (Hammer > Glide).
 *    - If target is faster AND can lethal-KO attacker: normal move will never execute (0 realized damage).
 *      Glide executes with priority before fainting (Glide > Hammer).
 * Case D (Neither move OHKOs):
 *    - If attacker is faster, OR target cannot lethal-KO attacker: normal move deals higher damage safely (Hammer > Glide).
 *      Preserves high-damage normal move choice (e.g. Arcanine E-Speed + Rillaboom Wood Hammer on Miraidon).
 *    - If target is faster AND can lethal-KO attacker: normal move never executes.
 *      Glide deals realized priority damage before fainting (Glide > Hammer).
 */
public final class GrassyGlideValuationStrategy {

    public static final String GRASSY_GLIDE = "grassyglide";

    private GrassyGlideValuationStrategy() {
    }

    public static boolean isGrassyGlide(Move move) {
        return move != null && isGrassyGlide(move.getName());
    }

    public static boolean isGrassyGlide(String moveName) {
        if (moveName == null) {
            return false;
        }
        return GRASSY_GLIDE.equalsIgnoreCase(moveName.trim());
    }

    public static boolean isGrassyTerrainActive(BattlePokemon attacker, ActiveBattlePokemon abp, PokemonBattle battle) {
        if (attacker != null) {
            try {
                if (BattleEffects.Field.Terrain.grassyterrain(attacker)) {
                    return true;
                }
            } catch (Throwable ignored) {
            }
        }
        if (battle != null) {
            try {
                com.cobblemon.mod.common.battles.interpreter.ContextManager cm = battle.getContextManager();
                if (cm != null) {
                    Collection<com.cobblemon.mod.common.api.battles.interpreter.BattleContext> contexts =
                        cm.get(com.cobblemon.mod.common.api.battles.interpreter.BattleContext.Type.TERRAIN);
                    if (contexts != null) {
                        for (com.cobblemon.mod.common.api.battles.interpreter.BattleContext ctx : contexts) {
                            if (ctx != null && "grassyterrain".equalsIgnoreCase(ctx.getId())) {
                                return true;
                            }
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        return false;
    }

    public static boolean isRaised(BattlePokemon bp) {
        if (bp == null) {
            return false;
        }
        try {
            return BattleEffects.Pokemon.State.raised(bp);
        } catch (Throwable t) {
            try {
                Pokemon pkmn = bp.getEffectedPokemon();
                if (pkmn != null) {
                    if (pkmn.getAbility() != null && "levitate".equalsIgnoreCase(pkmn.getAbility().getName())) {
                        return true;
                    }
                    Iterable<ElementalType> types = pkmn.getTypes();
                    if (types != null) {
                        for (ElementalType type : types) {
                            if (type != null && ElementalTypes.FLYING.equals(type)) {
                                return true;
                            }
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
            return false;
        }
    }

    public static boolean isGrassyGlidePriority(BattlePokemon attacker, ActiveBattlePokemon abp, PokemonBattle battle) {
        return isGrassyTerrainActive(attacker, abp, battle) && !isRaised(attacker);
    }

    public static boolean isAttackerFaster(BattlePokemon attacker, BattlePokemon target, RBStatStages stages) {
        if (attacker == null || target == null) {
            return false;
        }
        try {
            boolean trickRoom = BattleEffects.Field.Room.trickroom(attacker);
            Map<Stats, Integer> attackerStages = stages != null ? stages.getStatMap(attacker) : Collections.emptyMap();
            Map<Stats, Integer> targetStages = stages != null ? stages.getStatMap(target) : Collections.emptyMap();
            double attackerSpeed = PokeMathMax.getEffectiveSpeed(attacker, attackerStages);
            double targetSpeed = PokeMathMax.getEffectiveSpeed(target, targetStages);
            return trickRoom ? attackerSpeed <= targetSpeed : attackerSpeed >= targetSpeed;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean canTargetOHKOAttacker(
        BattlePokemon target,
        BattlePokemon attacker,
        ActiveBattlePokemon abp,
        RBStatStages stages
    ) {
        if (target == null || attacker == null) {
            return false;
        }
        try {
            if (target.getMoveSet() == null || target.getMoveSet().getMoves() == null) {
                return false;
            }
            List<Move> oppMoves = target.getMoveSet().getMoves();
            if (oppMoves.isEmpty()) {
                return false;
            }
            return RunBunAI.isOHKO(oppMoves, target, attacker, abp, stages);
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Adjusts MoveEvaluation scores in RunBunAI.choose() right before candidate selection,
     * giving Grassy Glide its rightful dynamic priority valuation under Grassy Terrain.
     */
    public static void adjustMoveValuations(
        List<RunBunAI.MoveEvaluation> evaluations,
        BattlePokemon attacker,
        ActiveBattlePokemon abp,
        PokemonBattle battle,
        RBStatStages stages
    ) {
        adjustMoveValuations(
            evaluations,
            attacker,
            abp,
            battle,
            stages,
            (atk, tgt) -> isAttackerFaster(atk, tgt, stages),
            (tgt, atk) -> canTargetOHKOAttacker(tgt, atk, abp, stages)
        );
    }

    public static void adjustMoveValuations(
        List<RunBunAI.MoveEvaluation> evaluations,
        BattlePokemon attacker,
        ActiveBattlePokemon abp,
        PokemonBattle battle,
        RBStatStages stages,
        java.util.function.BiPredicate<BattlePokemon, BattlePokemon> speedChecker,
        java.util.function.BiPredicate<BattlePokemon, BattlePokemon> targetOHKOChecker
    ) {
        if (evaluations == null || evaluations.isEmpty() || attacker == null) {
            return;
        }

        // Invariant 1: Outside active Grassy Terrain or when raised, Grassy Glide is normal priority (zero changes)
        if (!isGrassyGlidePriority(attacker, abp, battle)) {
            return;
        }

        // Group evaluations by target ActiveBattlePokemon
        Map<ActiveBattlePokemon, List<RunBunAI.MoveEvaluation>> evalsByTarget = new HashMap<>();
        for (RunBunAI.MoveEvaluation eval : evaluations) {
            if (eval == null || eval.getMove() == null || eval.getOpponent() == null) {
                continue;
            }
            evalsByTarget.computeIfAbsent(eval.getOpponent(), k -> new ArrayList<>()).add(eval);
        }

        for (Map.Entry<ActiveBattlePokemon, List<RunBunAI.MoveEvaluation>> entry : evalsByTarget.entrySet()) {
            ActiveBattlePokemon targetActive = entry.getKey();
            BattlePokemon targetBP = targetActive.getBattlePokemon();
            if (targetBP == null || targetBP.getHealth() <= 0) {
                continue;
            }

            List<RunBunAI.MoveEvaluation> targetEvals = entry.getValue();
            RunBunAI.MoveEvaluation glideEval = null;
            List<RunBunAI.MoveEvaluation> otherEvals = new ArrayList<>();

            for (RunBunAI.MoveEvaluation eval : targetEvals) {
                if (isGrassyGlide(eval.getMove())) {
                    glideEval = eval;
                } else {
                    otherEvals.add(eval);
                }
            }

            if (glideEval == null) {
                continue;
            }

            int targetHp = targetBP.getHealth();
            boolean glideOHKO = glideEval.getDamage() >= targetHp;
            boolean anyOtherOHKO = otherEvals.stream().anyMatch(e -> e.getDamage() >= targetHp);

            boolean isFaster = speedChecker != null ? speedChecker.test(attacker, targetBP) : isAttackerFaster(attacker, targetBP, stages);
            boolean targetCanOHKO = targetOHKOChecker != null ? targetOHKOChecker.test(targetBP, attacker) : canTargetOHKOAttacker(targetBP, attacker, abp, stages);

            if (glideOHKO) {
                if (anyOtherOHKO) {
                    // Case A: Both Glide and normal move OHKO target.
                    // Grassy Glide is a guaranteed priority KO, strictly dominating normal KO.
                    int maxOtherKillingScore = otherEvals.stream()
                        .filter(e -> e.getDamage() >= targetHp)
                        .mapToInt(RunBunAI.MoveEvaluation::getScore)
                        .max()
                        .orElse(0);
                    glideEval.setScore(Math.max(glideEval.getScore(), maxOtherKillingScore + 2));
                } else {
                    // Case B: Glide OHKOs, normal moves do not.
                    // Glide naturally wins; ensure full priority KO score baseline (>= 12).
                    glideEval.setScore(Math.max(glideEval.getScore(), 12));
                }
            } else {
                // Glide does NOT OHKO
                if (anyOtherOHKO) {
                    // Case C: Normal move OHKOs, Glide does NOT OHKO.
                    if (isFaster || !targetCanOHKO) {
                        // Case C1 / C2: Normal KO can actually execute before dying!
                        // Guaranteed normal KO that can execute > priority non-KO.
                        // Wood Hammer must win naturally; do NOT boost Glide!
                    } else {
                        // Case C3: Target is faster AND can lethal-KO attacker before normal move executes!
                        // Normal move will never execute (0 realized damage).
                        // Priority damage realizes action value before death (if damage > 0).
                        if (glideEval.getDamage() > 0) {
                            int maxOtherScore = otherEvals.stream()
                                .mapToInt(RunBunAI.MoveEvaluation::getScore)
                                .max()
                                .orElse(0);
                            glideEval.setScore(Math.max(glideEval.getScore(), maxOtherScore + 1));
                        }
                    }
                } else {
                    // Case D: Neither move OHKOs.
                    if (isFaster || !targetCanOHKO) {
                        // Case D1 / D2: Attacker can act safely.
                        // Large normal damage (Wood Hammer) > weak priority chip (Glide).
                        // Wood Hammer (maxMove) must win naturally; do NOT boost Glide!
                    } else {
                        // Case D3: Target is faster AND can lethal-KO attacker before normal move executes!
                        // Normal move will never execute. Priority chip deals realized damage (if damage > 0).
                        if (glideEval.getDamage() > 0) {
                            int maxOtherScore = otherEvals.stream()
                                .mapToInt(RunBunAI.MoveEvaluation::getScore)
                                .max()
                                .orElse(0);
                            glideEval.setScore(Math.max(glideEval.getScore(), maxOtherScore + 1));
                        }
                    }
                }
            }
        }
    }
}
