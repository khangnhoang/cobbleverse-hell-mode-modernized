package com.cobbleverse.legendaryrule.lead.simulation.calculator;

import com.cobbleverse.legendaryrule.lead.TypeChartData;
import com.cobbleverse.legendaryrule.lead.TypeChartResourceLoader;
import com.cobbleverse.legendaryrule.lead.simulation.model.CompetitivePokemonProfile;
import com.cobbleverse.legendaryrule.lead.simulation.model.MoveProfile;
import com.cobbleverse.legendaryrule.lead.simulation.model.Turn1BattleState;

import java.util.Locale;
import java.util.Objects;

/**
 * Deterministic damage calculator for Turn-1 simulation adhering to canonical Gen 9 mechanics.
 */
public final class Turn1DamageCalculator {

    private static final TypeChartData TYPE_CHART = TypeChartResourceLoader.load()
            .orElseThrow(() -> new IllegalStateException("Failed to load canonical Gen 9 type chart"));

    private Turn1DamageCalculator() {}

    public record DamageRange(
            int minDamage,
            int maxDamage,
            double minPercent,
            double maxPercent,
            boolean guaranteedOhko,
            boolean possibleOhko,
            boolean survives
    ) {}

    public static DamageRange calculateDamage(
            CompetitivePokemonProfile attacker,
            CompetitivePokemonProfile defender,
            MoveProfile move,
            Turn1BattleState state,
            String attackerSlot,
            String defenderSlot
    ) {
        Objects.requireNonNull(attacker, "attacker must not be null");
        Objects.requireNonNull(defender, "defender must not be null");
        Objects.requireNonNull(move, "move must not be null");
        Objects.requireNonNull(state, "state must not be null");

        if (move.isStatus()) {
            return new DamageRange(0, 0, 0.0, 0.0, false, false, true);
        }

        int level = attacker.level();
        int defenderHp = defender.actualStats().hp();

        // 1. Move dynamic attributes (Weather Ball, Expanding Force)
        String moveType = move.type();
        int basePower = move.basePower();
        boolean isSpread = move.isSpread();

        if ("weatherball".equals(move.id())) {
            switch (state.getWeather()) {
                case SUN -> {
                    moveType = "fire";
                    basePower = 100;
                }
                case RAIN -> {
                    moveType = "water";
                    basePower = 100;
                }
                case SAND -> {
                    moveType = "rock";
                    basePower = 100;
                }
                case SNOW -> {
                    moveType = "ice";
                    basePower = 100;
                }
                default -> {
                    moveType = "normal";
                    basePower = 50;
                }
            }
        } else if ("expandingforce".equals(move.id())) {
            if (state.getTerrain() == Turn1BattleState.Terrain.PSYCHIC && isGrounded(attacker)) {
                basePower = 120; // 80 * 1.5
                isSpread = true; // shifts to spread targeting
            }
        }

        // 2. Type effectiveness
        double typeMultiplier = calculateTypeEffectiveness(moveType, defender);
        if (typeMultiplier == 0.0) {
            return new DamageRange(0, 0, 0.0, 0.0, false, false, true);
        }

        // 3. Effective Attack & Defense
        boolean isPhysical = move.category() == MoveProfile.Category.PHYSICAL;
        int attackStat = isPhysical ? attacker.actualStats().atk() : attacker.actualStats().spa();
        int defenseStat = isPhysical ? defender.actualStats().def() : defender.actualStats().spd();

        // Stat stages
        String statKeyAtk = isPhysical ? "atk" : "spa";
        String statKeyDef = isPhysical ? "def" : "spd";
        int atkStage = attackerSlot != null ? state.getStatStage(attackerSlot, statKeyAtk) : 0;
        int defStage = defenderSlot != null ? state.getStatStage(defenderSlot, statKeyDef) : 0;

        double effectiveAtk = attackStat * getStatStageMultiplier(atkStage);
        double effectiveDef = defenseStat * getStatStageMultiplier(defStage);

        // Attacker Item modifiers
        if (attacker.hasItem("choice_band") && isPhysical) {
            effectiveAtk *= 1.5;
        } else if (attacker.hasItem("choice_specs") && !isPhysical) {
            effectiveAtk *= 1.5;
        }

        // Defender Item modifiers (Assault Vest 1.5x SpD)
        if (defender.hasItem("assault_vest") && !isPhysical) {
            effectiveDef = Math.floor(effectiveDef * 1.5);
        }

        // 4. Base Damage
        double levelFactor = Math.floor((2.0 * level) / 5.0) + 2.0;
        double baseDamage = Math.floor(Math.floor(levelFactor * basePower * effectiveAtk / effectiveDef) / 50.0) + 2.0;

        // 5. Multipliers
        // Spread modifier: 0.75x
        double spreadMod = isSpread ? 0.75 : 1.0;

        // Weather modifier:
        // Sun: Fire 1.5x, Water 0.5x
        // Rain: Water 1.5x, Fire 0.5x
        double weatherMod = 1.0;
        if (state.getWeather() == Turn1BattleState.Weather.SUN) {
            if ("fire".equals(moveType)) weatherMod = 1.5;
            else if ("water".equals(moveType)) weatherMod = 0.5;
        } else if (state.getWeather() == Turn1BattleState.Weather.RAIN) {
            if ("water".equals(moveType)) weatherMod = 1.5;
            else if ("fire".equals(moveType)) weatherMod = 0.5;
        }

        // Terrain modifier:
        // Psychic Terrain: 1.3x for grounded Psychic moves
        double terrainMod = 1.0;
        if (state.getTerrain() == Turn1BattleState.Terrain.PSYCHIC && "psychic".equals(moveType) && isGrounded(attacker)) {
            terrainMod = 1.3;
        }

        // STAB modifier:
        // 2.0x for Adaptability, 1.5x for standard STAB, 1.0x otherwise
        double stabMod = 1.0;
        if (attacker.hasType(moveType)) {
            stabMod = attacker.hasAbility("adaptability") ? 2.0 : 1.5;
        }

        // Helping Hand: 1.5x
        double helpingHandMod = (attackerSlot != null && state.isHelpingHandBoosted(attackerSlot)) ? 1.5 : 1.0;

        // Life Orb: 1.3x
        double lifeOrbMod = attacker.hasItem("life_orb") ? 1.3 : 1.0;

        double finalPreRoll = baseDamage * spreadMod * weatherMod * terrainMod * stabMod * typeMultiplier * helpingHandMod * lifeOrbMod;

        // 6. Min and Max bounds (0.85 to 1.00)
        int minDamage = Math.max(1, (int) Math.floor(finalPreRoll * 0.85));
        int maxDamage = Math.max(1, (int) Math.floor(finalPreRoll * 1.00));

        double minPercent = ((double) minDamage / defenderHp) * 100.0;
        double maxPercent = ((double) maxDamage / defenderHp) * 100.0;
        boolean guaranteedOhko = minDamage >= defenderHp;
        boolean possibleOhko = minDamage < defenderHp && maxDamage >= defenderHp;
        boolean survives = maxDamage < defenderHp;

        return new DamageRange(minDamage, maxDamage, minPercent, maxPercent, guaranteedOhko, possibleOhko, survives);
    }

    public static double calculateTypeEffectiveness(String moveType, CompetitivePokemonProfile defender) {
        String mt = moveType.toLowerCase(Locale.ROOT);

        // Special Ability Immunities
        if ("ground".equals(mt) && defender.hasAbility("levitate")) {
            return 0.0;
        }
        if ("fire".equals(mt) && (defender.hasAbility("flash_fire") || defender.hasAbility("flashfire"))) {
            return 0.0;
        }
        if ("ground".equals(mt) && defender.hasItem("air_balloon")) {
            return 0.0;
        }

        double mult = 1.0;
        for (String defType : defender.types()) {
            mult *= TYPE_CHART.getMultiplier(mt, defType.toLowerCase(Locale.ROOT));
        }
        return mult;
    }

    public static boolean isGrounded(CompetitivePokemonProfile mon) {
        if (mon.hasType("flying")) return false;
        if (mon.hasAbility("levitate")) return false;
        if (mon.hasItem("air_balloon")) return false;
        return true;
    }

    public static double getStatStageMultiplier(int stage) {
        if (stage > 0) {
            return (2.0 + stage) / 2.0;
        } else if (stage < 0) {
            return 2.0 / (2.0 - stage);
        }
        return 1.0;
    }
}
