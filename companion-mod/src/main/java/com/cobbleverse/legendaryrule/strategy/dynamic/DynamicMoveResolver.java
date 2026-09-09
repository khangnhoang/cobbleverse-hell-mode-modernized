package com.cobbleverse.legendaryrule.strategy.dynamic;

import com.cobblemon.mod.common.api.battles.interpreter.BattleContext;
import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.moves.categories.DamageCategories;
import com.cobblemon.mod.common.api.moves.categories.DamageCategory;
import com.cobblemon.mod.common.api.types.ElementalType;
import com.cobblemon.mod.common.api.types.ElementalTypes;
import com.cobblemon.mod.common.api.types.tera.TeraType;
import com.cobblemon.mod.common.battles.ActiveBattlePokemon;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobbleverse.legendaryrule.strategy.weather.MegaSolWeatherBallSurrogate;
import com.cobbleverse.legendaryrule.strategy.weather.MegaSolWeatherGuard;
import com.cobbleverse.legendaryrule.strategy.weather.WeatherContextNormalizer;
import com.gitlab.srcmc.rctapi.api.ai.utils.BattleEffects;
import com.gitlab.srcmc.rctapi.api.ai.utils.BattleStates;

import java.util.Collection;
import java.util.Locale;
import java.util.Set;

/**
 * Central resolver for moves with dynamic typing, power, or damage category
 * (Ivy Cudgel, Raging Bull, Weather Ball, Tera Blast, Revelation Dance)
 * in Run & Bun AI damage calculation.
 */
public final class DynamicMoveResolver {

    public static final String IVY_CUDGEL_NAME = "ivycudgel";
    public static final String RAGING_BULL_NAME = "ragingbull";
    public static final String WEATHER_BALL_NAME = "weatherball";
    public static final String TERA_BLAST_NAME = "terablast";
    public static final String REVELATION_DANCE_NAME = "revelationdance";

    public static final String WEATHER_BALL_FIRE = "weatherball_fire_resolved";
    public static final String WEATHER_BALL_WATER = "weatherball_water_resolved";
    public static final String WEATHER_BALL_ROCK = "weatherball_rock_resolved";
    public static final String WEATHER_BALL_ICE = "weatherball_ice_resolved";

    private static final String UTILITY_UMBRELLA = "utilityumbrella";

    private DynamicMoveResolver() {
    }

    /**
     * Backward-compatible 2-parameter overload delegating to the full resolution pipeline.
     */
    public static Move resolveEffectiveMove(Move move, BattlePokemon attacker) {
        return resolveEffectiveMove(move, attacker, null, false);
    }

    /**
     * Canonical entrypoint for dynamic move resolution.
     *
     * @param move the original Move
     * @param attacker the attacking BattlePokemon
     * @param abp the attacker's active battle slot context (may be null)
     * @param predictTera whether AI is predicting/evaluating terastallization
     * @return the resolved surrogate move or original move unchanged
     */
    public static Move resolveEffectiveMove(
        Move move,
        BattlePokemon attacker,
        ActiveBattlePokemon abp,
        boolean predictTera
    ) {
        if (move == null) {
            return null;
        }
        // Idempotence & recursion guard: never wrap an already-resolved surrogate
        if (move instanceof DynamicMoveSurrogate) {
            return move;
        }
        if (attacker == null) {
            return move;
        }

        String moveName = move.getName().toLowerCase(Locale.ROOT);
        return switch (moveName) {
            case IVY_CUDGEL_NAME -> resolveIvyCudgel(move, attacker);
            case RAGING_BULL_NAME -> resolveRagingBull(move, attacker);
            case WEATHER_BALL_NAME -> resolveWeatherBall(move, attacker, abp);
            case TERA_BLAST_NAME -> resolveTeraBlast(move, attacker, abp, predictTera);
            case REVELATION_DANCE_NAME -> resolveRevelationDance(move, attacker);
            default -> move;
        };
    }

    /**
     * Resolves Ivy Cudgel based on Ogerpon form, held mask item, aspects, or secondary type.
     * Invariant: retain contact move name "ivycudgel" in MoveTemplate.
     */
    private static Move resolveIvyCudgel(Move move, BattlePokemon attacker) {
        Pokemon pokemon = safeGetPokemon(attacker);
        if (pokemon == null) {
            return move;
        }

        String formName = safeGetFormName(pokemon);
        Set<String> aspects = safeGetAspects(pokemon);
        ElementalType secondary = safeGetSecondaryType(pokemon);
        String heldItem = getHeldItemId(attacker);

        ElementalType resolvedType = ElementalTypes.GRASS;
        if (formName.contains("wellspring") || heldItem.contains("wellspring")
            || hasAspectMatch(aspects, "wellspring") || ElementalTypes.WATER.equals(secondary)) {
            resolvedType = ElementalTypes.WATER;
        } else if (formName.contains("hearthflame") || heldItem.contains("hearthflame")
            || hasAspectMatch(aspects, "hearthflame") || ElementalTypes.FIRE.equals(secondary)) {
            resolvedType = ElementalTypes.FIRE;
        } else if (formName.contains("cornerstone") || heldItem.contains("cornerstone")
            || hasAspectMatch(aspects, "cornerstone") || ElementalTypes.ROCK.equals(secondary)) {
            resolvedType = ElementalTypes.ROCK;
        }

        DamageCategory category = move.getDamageCategory() != null
            ? move.getDamageCategory()
            : DamageCategories.INSTANCE.getPHYSICAL();

        return new DynamicMoveSurrogate(move, IVY_CUDGEL_NAME, resolvedType, 100.0d, category);
    }

    /**
     * Resolves Raging Bull based on Paldean Tauros form, aspects, or typing.
     * Invariant: retain contact move name "ragingbull" in MoveTemplate.
     */
    private static Move resolveRagingBull(Move move, BattlePokemon attacker) {
        Pokemon pokemon = safeGetPokemon(attacker);
        if (pokemon == null) {
            return move;
        }

        String formName = safeGetFormName(pokemon);
        Set<String> aspects = safeGetAspects(pokemon);
        ElementalType secondary = safeGetSecondaryType(pokemon);
        ElementalType primary = safeGetPrimaryType(pokemon);

        ElementalType resolvedType = ElementalTypes.NORMAL;
        if (formName.contains("blaze") || formName.contains("fire")
            || ElementalTypes.FIRE.equals(secondary) || hasAspectMatch(aspects, "blaze")) {
            resolvedType = ElementalTypes.FIRE;
        } else if (formName.contains("aqua") || formName.contains("water")
            || ElementalTypes.WATER.equals(secondary) || hasAspectMatch(aspects, "aqua")) {
            resolvedType = ElementalTypes.WATER;
        } else if (formName.contains("combat") || formName.contains("paldea")
            || ElementalTypes.FIGHTING.equals(primary) || hasAspectMatch(aspects, "combat")) {
            resolvedType = ElementalTypes.FIGHTING;
        }

        DamageCategory category = move.getDamageCategory() != null
            ? move.getDamageCategory()
            : DamageCategories.INSTANCE.getPHYSICAL();

        return new DynamicMoveSurrogate(move, RAGING_BULL_NAME, resolvedType, 90.0d, category);
    }

    /**
     * Resolves Weather Ball under personal weather (Mega Sol) or field weather.
     * Invariant: uses surrogate name "weatherball_*_resolved" to bypass upstream PokeMathMax overrides.
     */
    private static Move resolveWeatherBall(Move move, BattlePokemon attacker, ActiveBattlePokemon abp) {
        String heldItem = getHeldItemId(attacker);
        if (UTILITY_UMBRELLA.equalsIgnoreCase(heldItem)) {
            return move;
        }

        if (MegaSolWeatherGuard.hasMegaSol(attacker)) {
            return new MegaSolWeatherBallSurrogate(move);
        }

        Collection<BattleContext> weatherContexts = getWeatherContexts(attacker, abp);
        if (weatherContexts == null || weatherContexts.isEmpty()) {
            return move;
        }

        // Apply normalization defensively in case this call is made outside mixin interception
        Collection<BattleContext> normalized = WeatherContextNormalizer.normalize(weatherContexts);
        for (BattleContext ctx : normalized) {
            if (ctx == null || ctx.getId() == null) {
                continue;
            }
            String id = ctx.getId().toLowerCase(Locale.ROOT);
            if (isSunWeather(id)) {
                return new DynamicMoveSurrogate(move, WEATHER_BALL_FIRE, ElementalTypes.FIRE, 100.0d, move.getDamageCategory());
            }
            if (isRainWeather(id)) {
                return new DynamicMoveSurrogate(move, WEATHER_BALL_WATER, ElementalTypes.WATER, 100.0d, move.getDamageCategory());
            }
            if ("sandstorm".equals(id)) {
                return new DynamicMoveSurrogate(move, WEATHER_BALL_ROCK, ElementalTypes.ROCK, 100.0d, move.getDamageCategory());
            }
            if ("snow".equals(id) || "hail".equals(id)) {
                return new DynamicMoveSurrogate(move, WEATHER_BALL_ICE, ElementalTypes.ICE, 100.0d, move.getDamageCategory());
            }
        }

        return move;
    }

    /**
     * Resolves Tera Blast typing and damage category when terastallized or predicted.
     * Invariant: retains move name "terablast".
     */
    private static Move resolveTeraBlast(
        Move move,
        BattlePokemon attacker,
        ActiveBattlePokemon abp,
        boolean predictTera
    ) {
        Pokemon pokemon = safeGetPokemon(attacker);
        if (pokemon == null) {
            return move;
        }

        boolean isTerastallized = isTerastallized(attacker);
        if (!isTerastallized && !predictTera) {
            return move;
        }

        TeraType teraType = safeGetTeraType(pokemon);
        if (teraType == null) {
            return move;
        }

        ElementalType resolvedType = null;
        try {
            if (teraType.getName() != null) {
                resolvedType = ElementalTypes.get(teraType.getName());
            }
            if (resolvedType == null && teraType.showdownId() != null) {
                resolvedType = ElementalTypes.get(teraType.showdownId());
            }
        } catch (Throwable ignored) {
        }
        if (resolvedType == null) {
            return move;
        }

        DamageCategory category = DamageCategories.INSTANCE.getSPECIAL();
        try {
            if (pokemon.getAttack() > pokemon.getSpecialAttack()) {
                category = DamageCategories.INSTANCE.getPHYSICAL();
            }
        } catch (Throwable ignored) {
        }

        double power = "stellar".equalsIgnoreCase(safeGetShowdownId(teraType)) ? 100.0d : 80.0d;
        return new DynamicMoveSurrogate(move, TERA_BLAST_NAME, resolvedType, power, category);
    }

    /**
     * Resolves Revelation Dance to match attacker's primary typing.
     * Invariant: retains move name "revelationdance".
     */
    private static Move resolveRevelationDance(Move move, BattlePokemon attacker) {
        Pokemon pokemon = safeGetPokemon(attacker);
        if (pokemon == null) {
            return move;
        }
        ElementalType primary = safeGetPrimaryType(pokemon);
        if (primary == null || ElementalTypes.NORMAL.equals(primary)) {
            return move;
        }
        return new DynamicMoveSurrogate(
            move,
            REVELATION_DANCE_NAME,
            primary,
            90.0d,
            DamageCategories.INSTANCE.getSPECIAL()
        );
    }

    private static boolean isSunWeather(String id) {
        return "sunnyday".equals(id) || "sunny".equals(id) || "harshsunlight".equals(id)
            || "desolateland".equals(id) || "extremelyharshsunlight".equals(id);
    }

    private static boolean isRainWeather(String id) {
        return "raindance".equals(id) || "rain".equals(id) || "primordialsea".equals(id)
            || "heavyrain".equals(id) || "raining".equals(id);
    }

    private static boolean hasAspectMatch(Set<String> aspects, String token) {
        if (aspects == null || token == null) {
            return false;
        }
        String lowerToken = token.toLowerCase(Locale.ROOT);
        for (String aspect : aspects) {
            if (aspect != null && aspect.toLowerCase(Locale.ROOT).contains(lowerToken)) {
                return true;
            }
        }
        return false;
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

    private static Pokemon safeGetPokemon(BattlePokemon attacker) {
        try {
            return attacker.getEffectedPokemon();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String safeGetFormName(Pokemon pokemon) {
        try {
            return pokemon.getForm() != null ? pokemon.getForm().getName().toLowerCase(Locale.ROOT) : "";
        } catch (Throwable ignored) {
            return "";
        }
    }

    private static Set<String> safeGetAspects(Pokemon pokemon) {
        try {
            return pokemon.getAspects();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static ElementalType safeGetPrimaryType(Pokemon pokemon) {
        try {
            return pokemon.getPrimaryType();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static ElementalType safeGetSecondaryType(Pokemon pokemon) {
        try {
            return pokemon.getSecondaryType();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static TeraType safeGetTeraType(Pokemon pokemon) {
        try {
            return pokemon.getTeraType();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String safeGetShowdownId(TeraType teraType) {
        try {
            return teraType != null ? teraType.showdownId() : "";
        } catch (Throwable ignored) {
            return "";
        }
    }

    private static Collection<BattleContext> getWeatherContexts(BattlePokemon attacker, ActiveBattlePokemon abp) {
        try {
            if (abp != null && abp.getBattle() != null) {
                return abp.getBattle().getContextManager().get(BattleContext.Type.WEATHER);
            }
            if (attacker != null && attacker.getActor() != null && attacker.getActor().battle instanceof PokemonBattle pb) {
                return pb.getContextManager().get(BattleContext.Type.WEATHER);
            }
            if (attacker != null && attacker.getContextManager() != null) {
                return attacker.getContextManager().get(BattleContext.Type.WEATHER);
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static boolean isTerastallized(BattlePokemon attacker) {
        try {
            if (attacker != null && attacker.getActor() != null && attacker.getActor().battle instanceof PokemonBattle pb) {
                return BattleStates.get(pb)
                    .getPokemonState(attacker)
                    .has(BattleEffects.Custom.TERA);
            }
        } catch (Throwable ignored) {
        }
        return false;
    }
}
