package com.cobbleverse.legendaryrule.strategy.guard;

import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.types.ElementalType;
import com.cobblemon.mod.common.api.types.ElementalTypes;
import com.cobblemon.mod.common.battles.ActiveBattlePokemon;
import com.cobblemon.mod.common.battles.MoveTarget;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.gitlab.surilexa.rbrctai.api.ai.utils.PokeMathMax;
import com.gitlab.surilexa.rbrctai.api.ai.utils.RBBattleSlots;

import java.util.List;
import java.util.Set;

/**
 * Evaluates whether a candidate single-target move in Doubles is redirected by an active
 * opposing Pokémon with a redirection ability (Storm Drain for Water, Lightning Rod for Electric).
 *
 * Enforces:
 * 1. Positive whitelist for single-target MoveTarget (spread/self/side moves are never redirected).
 * 2. Suppression-aware ability lookup via PokeMathMax.hasAbility (accounting for Neutralizing Gas/Gastro Acid).
 * 3. Ability Shield awareness: Mold Breaker pierces redirection unless redirector holds Ability Shield;
 *    and Ability Shield protects redirector from Neutralizing Gas suppression.
 * 4. Bypass rules: Snipe Shot, Stalwart, and Propeller Tail ignore redirection.
 * 5. Direct targets on the redirector itself are not treated as redirected (handled natively).
 */
public final class RedirectAbilityGuard {

    private static final Set<MoveTarget> REDIRECTABLE_TARGETS = Set.of(
        MoveTarget.normal,
        MoveTarget.any,
        MoveTarget.adjacentFoe,
        MoveTarget.randomNormal
    );

    private static final List<String> REDIRECTION_IGNORING_ABILITIES = List.of(
        "stalwart",
        "propellertail"
    );

    private RedirectAbilityGuard() {
        // Utility class
    }

    /**
     * Checks if the given move from attacker targeting defender will be redirected by an active opponent.
     */
    public static boolean isMoveRedirected(
        Move move,
        BattlePokemon attacker,
        BattlePokemon defender,
        ActiveBattlePokemon attackerActive
    ) {
        if (attackerActive == null) {
            return false;
        }
        return isMoveRedirected(move, attacker, defender, RBBattleSlots.getOpponents(attackerActive));
    }

    /**
     * Core redirection check taking explicit opponents list (for testability and decoupling).
     */
    public static boolean isMoveRedirected(
        Move move,
        BattlePokemon attacker,
        BattlePokemon defender,
        List<ActiveBattlePokemon> opponents
    ) {
        if (move == null || attacker == null || defender == null || opponents == null || opponents.isEmpty()) {
            return false;
        }

        // 1. Positive whitelist: only single-target moves can be redirected
        if (move.getTemplate() == null || !REDIRECTABLE_TARGETS.contains(move.getTemplate().getTarget())) {
            return false;
        }

        // 2. Specific moves that ignore redirection (Snipe Shot)
        if ("snipeshot".equalsIgnoreCase(move.getName())) {
            return false;
        }

        // 3. Attacker abilities that ignore redirection (Stalwart, Propeller Tail)
        if (PokeMathMax.hasAbility(REDIRECTION_IGNORING_ABILITIES, attacker)) {
            return false;
        }

        // 4. Map move element to redirect ability
        ElementalType moveType = move.getType();
        String redirectAbility = null;
        if (ElementalTypes.WATER.equals(moveType)) {
            redirectAbility = "stormdrain";
        } else if (ElementalTypes.ELECTRIC.equals(moveType)) {
            redirectAbility = "lightningrod";
        }

        if (redirectAbility == null) {
            return false;
        }

        // 5. Check if attacker has unsuppressed ability-ignoring ability (Mold Breaker, Teravolt, Turboblaze)
        boolean attackerIgnoresAbilities = PokeMathMax.ignoreAbilities(attacker);

        // 6. Scan active opponents for an unsuppressed redirect ability
        for (ActiveBattlePokemon opp : opponents) {
            if (opp == null) continue;
            BattlePokemon bp = opp.getBattlePokemon();
            if (bp == null) continue;

            // Direct target is not redirected away; native damage/immunity handles it
            if (defender.getUuid() != null && defender.getUuid().equals(bp.getUuid())) {
                continue;
            }

            // PokeMathMax.hasAbility internally calls PokeMathMax.isSuppressed(bp)
            if (PokeMathMax.hasAbility(redirectAbility, bp)) {
                if (attackerIgnoresAbilities) {
                    String heldItem = bp.getHeldItemManager() != null ? bp.getHeldItemManager().showdownId(bp) : null;
                    if (!"abilityshield".equalsIgnoreCase(heldItem)) {
                        // Mold Breaker bypasses redirector without Ability Shield
                        continue;
                    }
                }
                return true;
            }
        }

        return false;
    }
}
