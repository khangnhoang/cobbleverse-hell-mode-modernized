package com.cobbleverse.legendaryrule.strategy.weather;

import com.cobblemon.mod.common.api.battles.interpreter.BattleContext;
import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.battles.ActiveBattlePokemon;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.gitlab.srcmc.rctapi.api.ai.utils.BattleEffects;
import com.gitlab.surilexa.rbrctai.api.ai.RunBunAI;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * Evaluates weather-modified effective accuracy, move valuation bonuses, and
 * move dominance comparisons for Run & Bun AI move selection.
 *
 * Enforces key domain invariants:
 * 1. Under active Rain, Thunder and Hurricane have 100% effective accuracy (1.0).
 * 2. Under active Snow/Hail, Blizzard has 100% effective accuracy (1.0).
 * 3. Under adverse Sun, Thunder, Hurricane, and Blizzard have 50% accuracy (0.5).
 * 4. Outside active weather, Thunder and Hurricane have 70% accuracy (0.7).
 * 5. Under Rain, Thunder strictly dominates lower-power alternatives (Thunderbolt)
 *    in both raw damage and effective accuracy, eliminating RNG inversion and tie-break loss.
 * 6. Outside Rain, Thunderbolt maintains higher reliability over Thunder (1.0 vs 0.7).
 */
public final class WeatherAccuracyValuationStrategy {

    public static final String THUNDER = "thunder";
    public static final String HURRICANE = "hurricane";
    public static final String BLIZZARD = "blizzard";
    public static final String THUNDERBOLT = "thunderbolt";

    private static final String UTILITY_UMBRELLA = "utilityumbrella";

    private WeatherAccuracyValuationStrategy() {
    }

    /**
     * Evaluates weather-modified effective accuracy for a move given active attacker context.
     *
     * @param move the candidate move
     * @param attacker the attacking BattlePokemon
     * @param abp the active battle slot (may be null)
     * @return effective accuracy in [0.0, 1.0]
     */
    public static double getEffectiveAccuracy(Move move, BattlePokemon attacker, ActiveBattlePokemon abp) {
        if (move == null) {
            return 0.0d;
        }
        String weatherId = resolveActiveWeatherId(attacker, abp);
        return getEffectiveAccuracy(move, weatherId);
    }

    /**
     * Evaluates weather-modified effective accuracy for a move given a weather condition token.
     *
     * @param move the candidate move
     * @param weatherId the weather ID string (may be null for neutral weather)
     * @return effective accuracy in [0.0, 1.0]
     */
    public static double getEffectiveAccuracy(Move move, String weatherId) {
        if (move == null) {
            return 0.0d;
        }
        String name = move.getName().toLowerCase(Locale.ROOT);
        boolean rain = isRainWeather(weatherId);
        boolean sun = isSunWeather(weatherId);
        boolean snow = isSnowWeather(weatherId);

        return switch (name) {
            case THUNDER, HURRICANE -> {
                if (rain) yield 1.0d;
                if (sun) yield 0.5d;
                yield 0.7d;
            }
            case BLIZZARD -> {
                if (snow) yield 1.0d;
                if (sun) yield 0.5d;
                yield 0.7d;
            }
            default -> {
                double baseAcc = move.getAccuracy();
                if (baseAcc <= 0.0d) {
                    yield 1.0d;
                }
                if (baseAcc > 1.0d) {
                    yield baseAcc / 100.0d;
                }
                yield baseAcc;
            }
        };
    }

    /**
     * Compares two moves to determine whether candidateA dominates candidateB under active field weather.
     *
     * @param candidateA first candidate move
     * @param candidateB second candidate move
     * @param attacker attacking BattlePokemon
     * @param abp active battle slot
     * @return positive integer if candidateA dominates candidateB,
     *         negative integer if candidateB dominates candidateA,
     *         0 if neither strictly dominates the other.
     */
    public static int compareWeatherMoveDominance(
        Move candidateA,
        Move candidateB,
        BattlePokemon attacker,
        ActiveBattlePokemon abp
    ) {
        String weatherId = resolveActiveWeatherId(attacker, abp);
        return compareWeatherMoveDominance(candidateA, candidateB, weatherId);
    }

    /**
     * Compares two moves to determine whether candidateA dominates candidateB under a specified weather condition.
     *
     * @param candidateA first candidate move
     * @param candidateB second candidate move
     * @param weatherId weather ID string (or null for neutral)
     * @return positive integer if candidateA dominates candidateB,
     *         negative integer if candidateB dominates candidateA,
     *         0 if neither strictly dominates the other.
     */
    public static int compareWeatherMoveDominance(Move candidateA, Move candidateB, String weatherId) {
        if (candidateA == null && candidateB == null) return 0;
        if (candidateA != null && candidateB == null) return 1;
        if (candidateA == null && candidateB != null) return -1;
        if (candidateA.getName().equalsIgnoreCase(candidateB.getName())) return 0;

        String nameA = candidateA.getName().toLowerCase(Locale.ROOT);
        String nameB = candidateB.getName().toLowerCase(Locale.ROOT);

        double accA = getEffectiveAccuracy(candidateA, weatherId);
        double accB = getEffectiveAccuracy(candidateB, weatherId);
        double powA = candidateA.getPower();
        double powB = candidateB.getPower();

        double evA = powA * accA;
        double evB = powB * accB;

        boolean isWeatherA = isWeatherAccuracyMove(nameA);
        boolean isWeatherB = isWeatherAccuracyMove(nameB);

        // Case 1: Weather accuracy move under 100% weather accuracy condition (e.g. Thunder in Rain)
        // Thunder (110 BP, 1.0 acc) strictly dominates Thunderbolt (90 BP, 1.0 acc)
        if (isWeatherA && accA >= 1.0d && powA > powB && accA >= accB) {
            return 1;
        }
        if (isWeatherB && accB >= 1.0d && powB > powA && accB >= accA) {
            return -1;
        }

        // Case 2: Weather accuracy move outside favorable weather vs reliable alternative
        // Thunderbolt (90 BP, 1.0 acc -> EV 90) dominates Thunder (110 BP, 0.7 acc -> EV 77)
        if (isWeatherA && accA < 1.0d && isReliableAlternative(nameB, nameA)) {
            if (evB >= evA && accB > accA) {
                return -1;
            }
        }
        if (isWeatherB && accB < 1.0d && isReliableAlternative(nameA, nameB)) {
            if (evA >= evB && accA > accB) {
                return 1;
            }
        }

        // Case 3: Same typing expected value and reliability comparison
        boolean sameType = (candidateA.getType() != null && candidateA.getType().equals(candidateB.getType()))
            || isRelatedMovePair(nameA, nameB);

        if (sameType) {
            if (evA > evB && accA >= accB) return 1;
            if (evB > evA && accB >= accA) return -1;
            if (Math.abs(evA - evB) < 0.001d) {
                if (accA > accB) return 1;
                if (accB > accA) return -1;
                if (powA > powB) return 1;
                if (powB > powA) return -1;
            }
            if (accA >= 1.0d && accB < 1.0d && evA >= 0.85d * evB) return 1;
            if (accB >= 1.0d && accA < 1.0d && evB >= 0.85d * evA) return -1;
        }

        return 0;
    }

    /**
     * Adjusts move valuation scores in RunBunAI.choose() right before candidate ranking.
     *
     * 1. Awards missing weather bonus (+7) to Thunder / Hurricane in Rain, matching Blizzard in Snow.
     * 2. Penalizes (-6) weather moves in Sun where accuracy drops to 50%.
     * 3. Resolves move dominance across candidate pairs: when candidateA dominates candidateB under
     *    active weather, score(candidateA) is guaranteed to be strictly greater than score(candidateB),
     *    eliminating independent RNG roll inversion and 50/50 tie-break coin flips.
     *
     * @param evaluations list of MoveEvaluation instances from RunBunAI.choose()
     * @param attacker attacking BattlePokemon
     * @param abp active battle slot
     * @param battle PokemonBattle instance (may be null)
     */
    public static void adjustMoveValuations(
        List<RunBunAI.MoveEvaluation> evaluations,
        BattlePokemon attacker,
        ActiveBattlePokemon abp,
        PokemonBattle battle
    ) {
        if (evaluations == null || evaluations.isEmpty()) {
            return;
        }

        String weatherId = resolveActiveWeatherId(attacker, abp, battle);
        boolean rain = isRainWeather(weatherId);
        boolean sun = isSunWeather(weatherId);

        // Step 1: Apply weather scoring adjustments for moves omitted from native switch
        for (RunBunAI.MoveEvaluation eval : evaluations) {
            if (eval == null || eval.getMove() == null) {
                continue;
            }
            String moveName = eval.getMove().getName().toLowerCase(Locale.ROOT);
            if (THUNDER.equals(moveName) || HURRICANE.equals(moveName)) {
                if (rain) {
                    eval.setScore(eval.getScore() + 7);
                } else if (sun) {
                    eval.setScore(eval.getScore() - 6);
                }
            } else if (BLIZZARD.equals(moveName)) {
                if (sun) {
                    eval.setScore(eval.getScore() - 6);
                }
            }
        }

        // Step 2: Enforce dominance invariants across candidate move pairs
        int size = evaluations.size();
        for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i < size; i++) {
                RunBunAI.MoveEvaluation evalA = evaluations.get(i);
                if (evalA == null || evalA.getMove() == null) continue;

                for (int j = i + 1; j < size; j++) {
                    RunBunAI.MoveEvaluation evalB = evaluations.get(j);
                    if (evalB == null || evalB.getMove() == null) continue;

                    if (!isSameOpponent(evalA, evalB)) {
                        continue;
                    }

                    int dom = compareWeatherMoveDominance(evalA.getMove(), evalB.getMove(), weatherId);
                    if (dom > 0) {
                        if (evalA.getScore() <= evalB.getScore()) {
                            evalA.setScore(evalB.getScore() + 1);
                        }
                    } else if (dom < 0) {
                        if (evalB.getScore() <= evalA.getScore()) {
                            evalB.setScore(evalA.getScore() + 1);
                        }
                    }
                }
            }
        }
    }

    public static boolean isWeatherAccuracyMove(String name) {
        if (name == null) return false;
        String lower = name.toLowerCase(Locale.ROOT);
        return THUNDER.equals(lower) || HURRICANE.equals(lower) || BLIZZARD.equals(lower);
    }

    public static boolean isReliableAlternative(String candidate, String weatherMove) {
        if (candidate == null || weatherMove == null) return false;
        String cand = candidate.toLowerCase(Locale.ROOT);
        String wm = weatherMove.toLowerCase(Locale.ROOT);
        return switch (wm) {
            case THUNDER -> THUNDERBOLT.equals(cand) || "discharge".equals(cand) || "voltswitch".equals(cand);
            case HURRICANE -> "airslash".equals(cand) || "aircutter".equals(cand);
            case BLIZZARD -> "icebeam".equals(cand) || "freezedry".equals(cand);
            default -> false;
        };
    }

    public static boolean isRelatedMovePair(String nameA, String nameB) {
        return isReliableAlternative(nameA, nameB) || isReliableAlternative(nameB, nameA);
    }

    public static boolean isRainWeather(String weatherId) {
        if (weatherId == null) return false;
        String id = weatherId.toLowerCase(Locale.ROOT).trim();
        return "rain".equals(id) || "heavyrain".equals(id)
            || "raindance".equals(id) || "primordialsea".equals(id)
            || "raining".equals(id);
    }

    public static boolean isSunWeather(String weatherId) {
        if (weatherId == null) return false;
        String id = weatherId.toLowerCase(Locale.ROOT).trim();
        return "sun".equals(id) || "sunny".equals(id) || "sunnyday".equals(id)
            || "harshsunlight".equals(id) || "desolateland".equals(id)
            || "extremelyharshsunlight".equals(id);
    }

    public static boolean isSnowWeather(String weatherId) {
        if (weatherId == null) return false;
        String id = weatherId.toLowerCase(Locale.ROOT).trim();
        return "snow".equals(id) || "hail".equals(id) || "snowdance".equals(id);
    }

    public static String resolveActiveWeatherId(BattlePokemon attacker, ActiveBattlePokemon abp) {
        return resolveActiveWeatherId(attacker, abp, null);
    }

    public static String resolveActiveWeatherId(BattlePokemon attacker, ActiveBattlePokemon abp, PokemonBattle battle) {
        String heldItem = getHeldItemId(attacker);
        if (UTILITY_UMBRELLA.equalsIgnoreCase(heldItem)) {
            return null;
        }

        Collection<BattleContext> contexts = getWeatherContexts(attacker, abp, battle);
        if (contexts != null && !contexts.isEmpty()) {
            Collection<BattleContext> normalized = WeatherContextNormalizer.normalize(contexts);
            for (BattleContext ctx : normalized) {
                if (ctx != null && ctx.getId() != null) {
                    String id = ctx.getId().toLowerCase(Locale.ROOT);
                    if (isRainWeather(id)) return "rain";
                    if (isSunWeather(id)) return "sunnyday";
                    if (isSnowWeather(id)) return "snow";
                    if ("sandstorm".equals(id)) return "sandstorm";
                }
            }
        }

        if (attacker != null) {
            try {
                if (BattleEffects.Field.Weather.rain(attacker) || BattleEffects.Field.Weather.heavyrain(attacker)) {
                    return "rain";
                }
                if (BattleEffects.Field.Weather.harshsunlight(attacker) || BattleEffects.Field.Weather.extremelyharshsunlight(attacker)) {
                    return "sunnyday";
                }
                if (BattleEffects.Field.Weather.snow(attacker) || BattleEffects.Field.Weather.hail(attacker)) {
                    return "snow";
                }
            } catch (Throwable ignored) {
            }
        }

        return null;
    }

    private static Collection<BattleContext> getWeatherContexts(BattlePokemon attacker, ActiveBattlePokemon abp, PokemonBattle battle) {
        if (battle != null) {
            try {
                if (battle.getContextManager() != null) {
                    return battle.getContextManager().get(BattleContext.Type.WEATHER);
                }
            } catch (Throwable ignored) {
            }
        }
        if (abp != null) {
            try {
                if (abp.getBattle() != null && abp.getBattle().getContextManager() != null) {
                    return abp.getBattle().getContextManager().get(BattleContext.Type.WEATHER);
                }
            } catch (Throwable ignored) {
            }
        }
        if (attacker != null) {
            try {
                if (attacker.getActor() != null && attacker.getActor().battle instanceof PokemonBattle pb) {
                    return pb.getContextManager().get(BattleContext.Type.WEATHER);
                }
            } catch (Throwable ignored) {
            }
            try {
                if (attacker.getContextManager() != null) {
                    return attacker.getContextManager().get(BattleContext.Type.WEATHER);
                }
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static String getHeldItemId(BattlePokemon bp) {
        try {
            if (bp != null && bp.getHeldItemManager() != null) {
                String id = bp.getHeldItemManager().showdownId(bp);
                return id != null ? id.toLowerCase(Locale.ROOT) : "";
            }
        } catch (Throwable ignored) {
        }
        return "";
    }

    private static boolean isSameOpponent(RunBunAI.MoveEvaluation a, RunBunAI.MoveEvaluation b) {
        if (a.getOpponent() == null && b.getOpponent() == null) return true;
        if (a.getOpponent() == null || b.getOpponent() == null) return false;
        if (a.getOpponent() == b.getOpponent()) return true;
        try {
            BattlePokemon bpA = a.getOpponent().getBattlePokemon();
            BattlePokemon bpB = b.getOpponent().getBattlePokemon();
            if (bpA != null && bpB != null && bpA.getUuid() != null && bpB.getUuid() != null) {
                return bpA.getUuid().equals(bpB.getUuid());
            }
        } catch (Throwable ignored) {
        }
        return false;
    }
}
