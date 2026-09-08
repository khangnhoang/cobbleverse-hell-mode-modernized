package com.cobbleverse.legendaryrule.strategy.weight;

import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.pokemon.helditem.HeldItemManager;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.pokemon.FormData;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.gitlab.surilexa.rbrctai.api.ai.utils.PokeMathMax;

import java.util.Set;

/**
 * Canonical resolver for weight-dependent moves in Run & Bun AI damage calculation.
 *
 * Resolves dynamic base power for:
 * 1. Target-Weight Moves (Grass Knot, Low Kick):
 *    Power scales based on defender effective weight in hectograms:
 *    - >= 2000.0 hg (>= 200.0 kg): 120 BP
 *    - >= 1000.0 hg (>= 100.0 kg): 100 BP
 *    - >= 500.0 hg  (>= 50.0 kg):   80 BP
 *    - >= 250.0 hg  (>= 25.0 kg):   60 BP
 *    - >= 100.0 hg  (>= 10.0 kg):   40 BP
 *    - < 100.0 hg   (< 10.0 kg):    20 BP
 *
 * 2. Weight-Ratio Moves (Heavy Slam, Heat Crash):
 *    Power scales based on attacker-to-defender effective weight ratio:
 *    - >= 5.0x: 120 BP
 *    - >= 4.0x: 100 BP
 *    - >= 3.0x:  80 BP
 *    - >= 2.0x:  60 BP
 *    - otherwise: 40 BP
 *
 * Evaluates effective weight considering:
 * - Species standard form fallback if Pokemon form is null.
 * - Heavy Metal (2.0x) and Light Metal (0.5x) if ability is not suppressed.
 * - Float Stone (0.5x) from held item.
 * - Clamping effective weight to >= 1.0 hg.
 */
public final class WeightDependentMoveResolver {

    public static final String GRASS_KNOT = "grassknot";
    public static final String LOW_KICK = "lowkick";
    public static final String HEAVY_SLAM = "heavyslam";
    public static final String HEAT_CRASH = "heatcrash";

    private static final Set<String> TARGET_WEIGHT_MOVES = Set.of(GRASS_KNOT, LOW_KICK);
    private static final Set<String> WEIGHT_RATIO_MOVES = Set.of(HEAVY_SLAM, HEAT_CRASH);

    private WeightDependentMoveResolver() {
    }

    /**
     * Normalizes a move name by trimming, lowercasing, and stripping whitespace, hyphens, and underscores.
     *
     * @param name raw move name
     * @return normalized move name or empty string if name is null
     */
    public static String normalizeMoveName(String name) {
        if (name == null) {
            return "";
        }
        return name.toLowerCase().replace("-", "").replace("_", "").replace(" ", "").trim();
    }

    /**
     * Checks if the given move is weight-dependent.
     *
     * @param move move to evaluate
     * @return true if Grass Knot, Low Kick, Heavy Slam, or Heat Crash; false otherwise
     */
    public static boolean isWeightDependent(Move move) {
        if (move == null) {
            return false;
        }
        return isWeightDependent(move.getName());
    }

    /**
     * Checks if the given move name corresponds to a weight-dependent move.
     *
     * @param moveName name of the move
     * @return true if Grass Knot, Low Kick, Heavy Slam, or Heat Crash; false otherwise
     */
    public static boolean isWeightDependent(String moveName) {
        String normalized = normalizeMoveName(moveName);
        return TARGET_WEIGHT_MOVES.contains(normalized) || WEIGHT_RATIO_MOVES.contains(normalized);
    }

    /**
     * Resolves the effective weight of a BattlePokemon in hectograms (1 hg = 0.1 kg).
     *
     * @param bp the BattlePokemon to evaluate
     * @return effective weight in hectograms, clamped to >= 1.0 hg
     */
    public static double getEffectiveWeight(BattlePokemon bp) {
        if (bp == null) {
            return 1.0d;
        }

        Pokemon pokemon = bp.getEffectedPokemon();
        if (pokemon == null) {
            pokemon = bp.getOriginalPokemon();
        }
        if (pokemon == null) {
            return 1.0d;
        }

        FormData form = pokemon.getForm();
        if (form == null && pokemon.getSpecies() != null) {
            form = pokemon.getSpecies().getStandardForm();
        }
        double weight = (form != null) ? (double) form.getWeight() : 1.0d;

        // Ability modifiers (suppression-aware)
        if (hasAbility(bp, "heavymetal")) {
            weight = weight * 2.0d;
        } else if (hasAbility(bp, "lightmetal")) {
            weight = Math.floor(weight / 2.0d);
        }

        // Held item modifier
        if (hasFloatStone(bp)) {
            weight = Math.floor(weight / 2.0d);
        }

        return Math.max(1.0d, weight);
    }

    /**
     * Calculates base power for target-weight moves (Grass Knot, Low Kick)
     * according to canonical Gen 9 mechanics.
     *
     * @param targetWeightHg target effective weight in hectograms
     * @return base power (20, 40, 60, 80, 100, or 120)
     */
    public static double calculateTargetWeightBasePower(double targetWeightHg) {
        if (targetWeightHg >= 2000.0d) {
            return 120.0d;
        } else if (targetWeightHg >= 1000.0d) {
            return 100.0d;
        } else if (targetWeightHg >= 500.0d) {
            return 80.0d;
        } else if (targetWeightHg >= 250.0d) {
            return 60.0d;
        } else if (targetWeightHg >= 100.0d) {
            return 40.0d;
        } else {
            return 20.0d;
        }
    }

    /**
     * Calculates base power for weight-ratio moves (Heavy Slam, Heat Crash)
     * according to canonical Gen 9 mechanics.
     *
     * @param attackerWeightHg attacker effective weight in hectograms
     * @param defenderWeightHg defender effective weight in hectograms
     * @return base power (40, 60, 80, 100, or 120)
     */
    public static double calculateWeightRatioBasePower(double attackerWeightHg, double defenderWeightHg) {
        if (defenderWeightHg <= 0.0d) {
            defenderWeightHg = 1.0d;
        }

        if (attackerWeightHg >= defenderWeightHg * 5.0d) {
            return 120.0d;
        } else if (attackerWeightHg >= defenderWeightHg * 4.0d) {
            return 100.0d;
        } else if (attackerWeightHg >= defenderWeightHg * 3.0d) {
            return 80.0d;
        } else if (attackerWeightHg >= defenderWeightHg * 2.0d) {
            return 60.0d;
        } else {
            return 40.0d;
        }
    }

    /**
     * Resolves the effective Move instance for Run & Bun AI damage calculation.
     * If the move is weight-dependent, wraps it in a WeightDependentMoveSurrogate
     * containing the dynamically calculated base power.
     * Otherwise, returns the original Move unchanged.
     *
     * @param move original move
     * @param attacker attacking BattlePokemon
     * @param defender defending BattlePokemon
     * @return effective Move instance
     */
    public static Move resolveEffectiveMove(Move move, BattlePokemon attacker, BattlePokemon defender) {
        if (move == null || attacker == null || defender == null) {
            return move;
        }
        if (move instanceof WeightDependentMoveSurrogate) {
            return move;
        }

        String normalized = normalizeMoveName(move.getName());
        if (TARGET_WEIGHT_MOVES.contains(normalized)) {
            double targetWeight = getEffectiveWeight(defender);
            double basePower = calculateTargetWeightBasePower(targetWeight);
            return new WeightDependentMoveSurrogate(move, basePower);
        } else if (WEIGHT_RATIO_MOVES.contains(normalized)) {
            double attackerWeight = getEffectiveWeight(attacker);
            double defenderWeight = getEffectiveWeight(defender);
            double basePower = calculateWeightRatioBasePower(attackerWeight, defenderWeight);
            return new WeightDependentMoveSurrogate(move, basePower);
        }

        return move;
    }

    private static boolean hasAbility(BattlePokemon bp, String abilityName) {
        if (bp == null || abilityName == null) {
            return false;
        }
        Pokemon pokemon = bp.getEffectedPokemon();
        if (pokemon == null) {
            pokemon = bp.getOriginalPokemon();
        }
        if (pokemon == null || pokemon.getAbility() == null) {
            return false;
        }
        try {
            return PokeMathMax.hasAbility(abilityName, bp);
        } catch (Throwable ignored) {
            return abilityName.equalsIgnoreCase(pokemon.getAbility().getName());
        }
    }

    private static boolean hasFloatStone(BattlePokemon bp) {
        if (bp == null) {
            return false;
        }
        try {
            HeldItemManager manager = bp.getHeldItemManager();
            if (manager != null) {
                String itemId = manager.showdownId(bp);
                if ("floatstone".equalsIgnoreCase(itemId)) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }
}
