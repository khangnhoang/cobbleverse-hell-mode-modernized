package com.cobbleverse.legendaryrule.lead.simulation.resolver;

import com.cobbleverse.legendaryrule.lead.simulation.calculator.Turn1DamageCalculator;
import com.cobbleverse.legendaryrule.lead.simulation.calculator.Turn1DamageCalculator.DamageRange;
import com.cobbleverse.legendaryrule.lead.simulation.model.CompetitivePokemonProfile;
import com.cobbleverse.legendaryrule.lead.simulation.model.MoveProfile;
import com.cobbleverse.legendaryrule.lead.simulation.model.Turn1Action;
import com.cobbleverse.legendaryrule.lead.simulation.model.Turn1BattleState;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Resolves Turn 1 actions according to canonical Gen 9 priority, redirection, and terrain rules.
 */
public final class Turn1ActionResolver {

    private Turn1ActionResolver() {}

    /**
     * Resolves a turn given a list of declared actions for all alive Pokémon.
     * Actions are sorted by priority and effective speed, then resolved sequentially.
     *
     * @param state The current battle state (will be mutated as actions resolve)
     * @param actions Declared actions for this turn
     * @param useMaxDamageRoll If true, uses max damage roll (1.00); if false, uses min damage roll (0.85)
     * @return Log of executed actions and events
     */
    public static List<String> resolveActions(
            Turn1BattleState state,
            List<Turn1Action> actions,
            boolean useMaxDamageRoll
    ) {
        Objects.requireNonNull(state, "state must not be null");
        Objects.requireNonNull(actions, "actions must not be null");

        List<String> log = new ArrayList<>();

        // Sort actions by priority (descending) and effective speed (descending, or ascending if Trick Room)
        List<Turn1Action> sorted = new ArrayList<>(actions);
        sorted.sort((a, b) -> {
            int pCmp = Integer.compare(b.priority(), a.priority());
            if (pCmp != 0) return pCmp;
            return Turn1SpeedResolver.compareSpeed(a.effectiveSpeed(), b.effectiveSpeed(), state.isTrickRoom());
        });

        for (Turn1Action action : sorted) {
            String actorSlot = action.actorSlot();
            CompetitivePokemonProfile actor = state.getProfileBySlot(actorSlot);

            // 1. Check if actor is fainted or flinched
            if (state.isFainted(actorSlot)) {
                continue;
            }
            if (state.isFlinched(actorSlot)) {
                log.add(actor.species() + " flinched and could not move!");
                continue;
            }

            MoveProfile move = action.move();
            String targetSlot = action.targetSlot();

            // 2. Resolve High-Priority Non-Damage Actions
            if ("protect".equals(move.id())) {
                state.setProtected(actorSlot);
                log.add(actor.species() + " used Protect!");
                continue;
            }

            if ("helpinghand".equals(move.id())) {
                String allySide = state.getSide(actorSlot);
                for (String slot : state.getSlotsForSide(allySide)) {
                    if (!slot.equals(actorSlot) && !state.isFainted(slot)) {
                        state.setHelpingHandBoosted(slot);
                        log.add(actor.species() + " used Helping Hand on " + state.getProfileBySlot(slot).species() + "!");
                    }
                }
                continue;
            }

            if ("followme".equals(move.id())) {
                state.setRedirection(state.getSide(actorSlot), actorSlot, Turn1BattleState.RedirectionType.FOLLOW_ME);
                log.add(actor.species() + " became the center of attention (Follow Me)!");
                continue;
            }

            if ("ragepowder".equals(move.id())) {
                state.setRedirection(state.getSide(actorSlot), actorSlot, Turn1BattleState.RedirectionType.RAGE_POWDER);
                log.add(actor.species() + " scattered Rage Powder!");
                continue;
            }

            if ("tailwind".equals(move.id())) {
                if (state.getSide(actorSlot).equals("player")) {
                    state.setTailwindPlayer(true);
                } else {
                    state.setTailwindKoga(true);
                }
                log.add(actor.species() + " whipped up a Tailwind!");
                continue;
            }

            if ("trickroom".equals(move.id())) {
                state.setTrickRoom(!state.isTrickRoom());
                log.add(actor.species() + " twisted the dimensions (Trick Room)!");
                continue;
            }

            if ("spore".equals(move.id())) {
                // Spore status move
                resolveSpore(state, actor, actorSlot, targetSlot, log);
                continue;
            }

            if ("growth".equals(move.id())) {
                int boost = state.getWeather() == Turn1BattleState.Weather.SUN ? 2 : 1;
                state.modifyStatStage(actorSlot, "atk", boost);
                state.modifyStatStage(actorSlot, "spa", boost);
                log.add(actor.species() + " used Growth (+" + boost + " Atk/SpA)!");
                continue;
            }

            if ("swordsdance".equals(move.id())) {
                state.modifyStatStage(actorSlot, "atk", 2);
                log.add(actor.species() + " used Swords Dance (+2 Atk)!");
                continue;
            }

            // 3. Attacking Move Resolution
            resolveAttack(state, actor, actorSlot, move, targetSlot, action.priority(), useMaxDamageRoll, log);
        }

        return log;
    }

    private static void resolveSpore(
            Turn1BattleState state,
            CompetitivePokemonProfile actor,
            String actorSlot,
            String targetSlot,
            List<String> log
    ) {
        CompetitivePokemonProfile target = state.getProfileBySlot(targetSlot);
        if (state.isProtected(targetSlot)) {
            log.add(actor.species() + "'s Spore was blocked by Protect!");
            return;
        }
        if (target.hasType("grass") || target.hasAbility("overcoat") || target.hasItem("safety_goggles")) {
            log.add(target.species() + " is immune to Spore (Grass/Powder immunity)!");
            return;
        }
        if (target.hasAbility("goodasgold")) {
            log.add(target.species() + "'s Good as Gold blocked Spore!");
            return;
        }
        log.add(target.species() + " fell asleep from Spore!");
    }

    public static int calculateEffectiveAccuracy(MoveProfile move, Turn1BattleState state) {
        if ("hurricane".equals(move.id()) || "thunder".equals(move.id())) {
            if (state.getWeather() == Turn1BattleState.Weather.RAIN) return 100;
            if (state.getWeather() == Turn1BattleState.Weather.SUN) return 50;
            return 70;
        }
        if ("blizzard".equals(move.id())) {
            if (state.getWeather() == Turn1BattleState.Weather.SNOW) return 100;
            return 70;
        }
        return move.accuracy();
    }

    private static void resolveAttack(
            Turn1BattleState state,
            CompetitivePokemonProfile actor,
            String actorSlot,
            MoveProfile move,
            String declaredTargetSlot,
            int priority,
            boolean useMaxDamageRoll,
            List<String> log
    ) {
        // Track accuracy assumption
        int effectiveAccuracy = calculateEffectiveAccuracy(move, state);
        if (effectiveAccuracy < 100) {
            log.add(String.format("[ASSUMPTION: %s hit (accuracy: %d%%)]", move.id(), effectiveAccuracy));
        }

        boolean isExpandingForceInTerrain = "expandingforce".equals(move.id())
                && state.getTerrain() == Turn1BattleState.Terrain.PSYCHIC
                && Turn1DamageCalculator.isGrounded(actor);

        boolean isSpread = move.isSpread() || isExpandingForceInTerrain;

        if (isSpread) {
            // Spread moves hit all adjacent opponents (and ally if ALL_ADJACENT) and are NEVER redirected
            List<String> targetSlots = new ArrayList<>();
            String opposingSide = state.getOpposingSide(actorSlot);
            targetSlots.addAll(state.getSlotsForSide(opposingSide));

            // If ALL_ADJACENT (e.g. Earthquake), ally slot is also hit
            if (move.target() == MoveProfile.Target.ALL_ADJACENT) {
                String ownSide = state.getSide(actorSlot);
                for (String slot : state.getSlotsForSide(ownSide)) {
                    if (!slot.equals(actorSlot)) {
                        targetSlots.add(slot);
                    }
                }
            }

            for (String targetSlot : targetSlots) {
                if (state.isFainted(targetSlot)) continue;

                CompetitivePokemonProfile defender = state.getProfileBySlot(targetSlot);

                // Priority check (e.g. if priority > 0 spread, but spread moves are normally 0)
                if (priority > 0 && state.getTerrain() == Turn1BattleState.Terrain.PSYCHIC && Turn1DamageCalculator.isGrounded(defender)) {
                    log.add(move.id() + " failed against " + defender.species() + " due to Psychic Terrain!");
                    continue;
                }

                if (state.isProtected(targetSlot)) {
                    log.add(defender.species() + " protected itself against " + move.id() + "!");
                    continue;
                }

                // Immunities
                if ("ground".equals(move.type()) && !Turn1DamageCalculator.isGrounded(defender)) {
                    log.add(defender.species() + " is immune to Ground moves (ungrounded)!");
                    continue;
                }
                if ("fire".equals(move.type()) && (defender.hasAbility("flash_fire") || defender.hasAbility("flashfire"))) {
                    log.add(defender.species() + "'s Flash Fire absorbed " + move.id() + "!");
                    continue;
                }
                if (Turn1DamageCalculator.calculateTypeEffectiveness(move.type(), defender) == 0.0) {
                    log.add(defender.species() + " is immune to " + move.id() + "!");
                    continue;
                }

                executeDamage(state, actor, actorSlot, defender, targetSlot, move, useMaxDamageRoll, log);
            }
        } else {
            // Single-target move: check priority failure under Psychic Terrain
            String actualTargetSlot = declaredTargetSlot;

            // Check redirection
            String targetSide = state.getSide(actualTargetSlot);
            String redirectSlot = state.getRedirectionSlot(targetSide);
            Turn1BattleState.RedirectionType redirectType = state.getRedirectionType(targetSide);

            if (redirectSlot != null && !state.isFainted(redirectSlot)) {
                if (redirectType == Turn1BattleState.RedirectionType.FOLLOW_ME) {
                    actualTargetSlot = redirectSlot;
                } else if (redirectType == Turn1BattleState.RedirectionType.RAGE_POWDER) {
                    // Rage Powder redirects UNLESS attacker is Grass-type, has Overcoat, or holds Safety Goggles
                    // Good as Gold does NOT prevent Rage Powder redirection!
                    boolean immuneToRagePowder = actor.hasType("grass")
                            || actor.hasAbility("overcoat")
                            || actor.hasItem("safety_goggles");
                    if (!immuneToRagePowder) {
                        actualTargetSlot = redirectSlot;
                    }
                }
            }

            CompetitivePokemonProfile defender = state.getProfileBySlot(actualTargetSlot);

            // Priority check under Psychic Terrain
            if (priority > 0 && state.getTerrain() == Turn1BattleState.Terrain.PSYCHIC && Turn1DamageCalculator.isGrounded(defender)) {
                log.add(move.id() + " failed against " + defender.species() + " due to Psychic Terrain!");
                return;
            }

            // Protect check
            if (state.isProtected(actualTargetSlot)) {
                log.add(defender.species() + " protected itself against " + move.id() + "!");
                return;
            }

            // Immunities
            if ("ground".equals(move.type()) && !Turn1DamageCalculator.isGrounded(defender)) {
                log.add(defender.species() + " is immune to Ground moves (ungrounded)!");
                return;
            }
            if ("fire".equals(move.type()) && (defender.hasAbility("flash_fire") || defender.hasAbility("flashfire"))) {
                log.add(defender.species() + "'s Flash Fire absorbed " + move.id() + "!");
                return;
            }
            if (Turn1DamageCalculator.calculateTypeEffectiveness(move.type(), defender) == 0.0) {
                log.add(defender.species() + " is immune to " + move.id() + "!");
                return;
            }

            // Execute attack
            executeDamage(state, actor, actorSlot, defender, actualTargetSlot, move, useMaxDamageRoll, log);

            // Fake Out flinch
            if ("fakeout".equals(move.id()) && !defender.hasAbility("innerfocus") && !defender.hasType("ghost")) {
                state.setFlinched(actualTargetSlot);
                log.add(defender.species() + " flinched from Fake Out!");
            }

            // Sneasler White Herb on Close Combat
            if ("closecombat".equals(move.id()) && actor.hasItem("white_herb") && !state.isItemConsumed(actorSlot)) {
                state.consumeItem(actorSlot);
                log.add(actor.species() + "'s White Herb restored its defensive stats and activated Unburden!");
            }
        }
    }

    private static void executeDamage(
            Turn1BattleState state,
            CompetitivePokemonProfile attacker,
            String attackerSlot,
            CompetitivePokemonProfile defender,
            String defenderSlot,
            MoveProfile move,
            boolean useMaxDamageRoll,
            List<String> log
    ) {
        DamageRange range = Turn1DamageCalculator.calculateDamage(attacker, defender, move, state, attackerSlot, defenderSlot);
        int damage = useMaxDamageRoll ? range.maxDamage() : range.minDamage();
        int defenderMaxHp = defender.actualStats().hp();

        // Disguise ability: absorbs first hit, dealing 1/8 max HP
        if (defender.hasAbility("disguise") && !state.isItemConsumed(defenderSlot + "_disguise") && damage > 0) {
            state.consumeItem(defenderSlot + "_disguise");
            int disguiseDmg = Math.max(1, defenderMaxHp / 8);
            state.applyDamage(defenderSlot, disguiseDmg);
            log.add(String.format("%s's Disguise absorbed %s! Took %d damage (%d/%d HP remaining)",
                    defender.species(), move.id(), disguiseDmg, state.getHp(defenderSlot), defenderMaxHp));
            return;
        }

        state.applyDamage(defenderSlot, damage);
        String classification = range.guaranteedOhko() ? "guaranteed OHKO"
                : (range.possibleOhko() ? "possible OHKO" : "survives");

        int remainingHp = state.getHp(defenderSlot);

        log.add(String.format("%s used %s on %s dealing %d damage [range: %d-%d / %d HP, %.1f%%-%.1f%%] (%s, %d/%d HP remaining)",
                attacker.species(), move.id(), defender.species(), damage,
                range.minDamage(), range.maxDamage(), defenderMaxHp,
                range.minPercent(), range.maxPercent(),
                classification, remainingHp, defenderMaxHp));

        if (state.isFainted(defenderSlot)) {
            log.add(defender.species() + " fainted!");
        }
    }
}
