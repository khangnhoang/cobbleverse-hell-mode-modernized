package com.cobbleverse.legendaryrule.strategy.weather;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.pokemon.helditem.HeldItemManager;
import com.cobblemon.mod.common.battles.ActiveBattlePokemon;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobbleverse.legendaryrule.strategy.tracker.BattleItemStateTracker;

import java.util.Locale;

/**
 * Evaluates execution timing, immediate scoring damage normalization, and adverse-weather
 * damage adjustment for two-turn solar moves (Solar Beam and Solar Blade) in Run & Bun AI.
 *
 * Invariants:
 * 1. Under active effective Sun, Solar Beam / Solar Blade execute immediately on turn 1
 *    with standard 120/125 BP; immediate scoring damage equals theoretical damage.
 * 2. With an active, unconsumed Power Herb, Solar Beam / Solar Blade execute immediately on turn 1;
 *    immediate scoring damage equals theoretical damage (adjusted for adverse weather if present).
 * 3. Outside active Sun and without usable Power Herb, Solar Beam / Solar Blade require a charge turn,
 *    dealing 0 damage on the current turn. Immediate scoring damage is normalized to 0 BEFORE
 *    RunBunAI candidate ranking (killingMoves / maxMove classification).
 * 4. Adverse-weather damage estimation: Under Rain, Sandstorm, Snow, or Hail, eventual power is halved (0.5x).
 *    Native PokeMathMax already halves damage under Snow/Hail but omits Rain/Sandstorm. This strategy
 *    applies 0.5x to Rain/Sandstorm without double-applying to Snow/Hail.
 * 5. Attacker holding Utility Umbrella ignores weather effects; power is not halved and Sun is not effective.
 */
public final class SolarMoveValuationStrategy {

    public static final String SOLAR_BEAM = "solarbeam";
    public static final String SOLAR_BLADE = "solarblade";
    public static final String POWER_HERB = "powerherb";

    private SolarMoveValuationStrategy() {
    }

    /**
     * Checks whether the move is a two-turn solar charge move (Solar Beam or Solar Blade).
     *
     * @param move candidate move
     * @return true if Solar Beam or Solar Blade
     */
    public static boolean isSolarMove(Move move) {
        return move != null && isSolarMove(move.getName());
    }

    /**
     * Checks whether the move name matches a two-turn solar charge move (Solar Beam or Solar Blade).
     *
     * @param moveName move name identifier
     * @return true if solarbeam or solarblade
     */
    public static boolean isSolarMove(String moveName) {
        if (moveName == null) return false;
        String name = moveName.toLowerCase(Locale.ROOT).trim();
        return SOLAR_BEAM.equals(name) || SOLAR_BLADE.equals(name);
    }

    /**
     * Resolves whether active effective Sun is present for the attacking Pokémon.
     * Takes into account attacker held item (Utility Umbrella negates effective weather)
     * and custom Sun-setting abilities (e.g. Mega Sol).
     *
     * @param attacker the attacking BattlePokemon
     * @param abp active battle slot (may be null)
     * @param battle the active PokemonBattle (may be null)
     * @return true if effective Sun is active
     */
    public static boolean isEffectiveSun(BattlePokemon attacker, ActiveBattlePokemon abp, PokemonBattle battle) {
        if (MegaSolWeatherGuard.hasMegaSol(attacker)) {
            return true;
        }
        String weatherId = WeatherAccuracyValuationStrategy.resolveActiveWeatherId(attacker, abp, battle);
        return WeatherAccuracyValuationStrategy.isSunWeather(weatherId);
    }

    /**
     * Resolves whether the attacking Pokémon holds a usable (unconsumed) Power Herb in this battle.
     *
     * @param attacker the attacking BattlePokemon
     * @return true if Power Herb is held and has not been consumed
     */
    public static boolean hasUsablePowerHerb(BattlePokemon attacker) {
        if (attacker == null) {
            return false;
        }
        try {
            HeldItemManager manager = attacker.getHeldItemManager();
            if (manager == null) {
                return false;
            }
            String heldItem = manager.showdownId(attacker);
            if (heldItem == null || !POWER_HERB.equalsIgnoreCase(heldItem.trim())) {
                return false;
            }
            if (attacker instanceof BattleItemStateTracker tracker && tracker.cobbleverse$isPowerHerbEnded()) {
                return false;
            }
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Resolves whether a solar move will attack immediately on the current turn
     * (either due to effective Sun or an unconsumed Power Herb).
     *
     * @param attacker the attacking BattlePokemon
     * @param abp active battle slot (may be null)
     * @param battle the active PokemonBattle (may be null)
     * @return true if attack executes immediately on turn 1
     */
    public static boolean isImmediateAttack(BattlePokemon attacker, ActiveBattlePokemon abp, PokemonBattle battle) {
        return isEffectiveSun(attacker, abp, battle) || hasUsablePowerHerb(attacker);
    }

    /**
     * Normalizes candidate scoring damage for RunBunAI.choose candidate ranking.
     * <p>
     * If the move is a solar move and cannot attack immediately this turn (no Sun and no usable Power Herb),
     * its immediate scoring damage is normalized to 0. This prevents RunBunAI from classifying it as a
     * fake killingMove or maxMove based on theoretical turn-2 damage, enforcing native damaging-zero-damage
     * scoring (-5) and allowing true immediate alternatives (e.g. Energy Ball) to dominate.
     *
     * @param theoreticalDamage theoretical damage calculated by PokeMathMax
     * @param move candidate move
     * @param attacker the attacking BattlePokemon
     * @param abp active battle slot (may be null)
     * @param battle the active PokemonBattle (may be null)
     * @return normalized immediate scoring damage (0 if charging, theoreticalDamage if immediate)
     */
    public static int resolveImmediateScoringDamage(int theoreticalDamage, Move move, BattlePokemon attacker, ActiveBattlePokemon abp, PokemonBattle battle) {
        if (!isSolarMove(move) || theoreticalDamage <= 0) {
            return theoreticalDamage;
        }
        if (isImmediateAttack(attacker, abp, battle)) {
            return theoreticalDamage;
        }
        return 0;
    }

    /**
     * Adjusts eventual damage in PokeMathMax for adverse weathers (Rain, Sandstorm)
     * that were omitted by native PokeMathMax.
     * <p>
     * In Gen 9 Showdown mechanics, Solar Beam and Solar Blade damage is halved (0.5x)
     * under Rain, Sandstorm, Hail, and Snow. Native PokeMathMax already applies 0.5x
     * under Snow and Hail (lines 1021-1023). This method applies 0.5x strictly under
     * Rain and Sandstorm, avoiding double-application for Snow/Hail.
     *
     * @param rawDamage raw damage calculated by PokeMathMax
     * @param move candidate move
     * @param attacker the attacking BattlePokemon
     * @param abp active battle slot (may be null)
     * @param battle the active PokemonBattle (may be null)
     * @return damage adjusted for adverse weather (0.5x if Rain or Sandstorm)
     */
    public static double adjustAdverseWeatherDamage(double rawDamage, Move move, BattlePokemon attacker, ActiveBattlePokemon abp, PokemonBattle battle) {
        if (!isSolarMove(move) || rawDamage <= 0.0d) {
            return rawDamage;
        }
        String weatherId = WeatherAccuracyValuationStrategy.resolveActiveWeatherId(attacker, abp, battle);
        if (weatherId == null) {
            return rawDamage;
        }
        if (WeatherAccuracyValuationStrategy.isRainWeather(weatherId) || "sandstorm".equalsIgnoreCase(weatherId.trim())) {
            return rawDamage * 0.5d;
        }
        return rawDamage;
    }
}
