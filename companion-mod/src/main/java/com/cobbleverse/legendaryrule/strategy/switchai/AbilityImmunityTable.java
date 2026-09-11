package com.cobbleverse.legendaryrule.strategy.switchai;

import com.cobblemon.mod.common.api.types.ElementalType;
import com.cobblemon.mod.common.api.types.ElementalTypes;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Pure data-only mapping of ability IDs to the ElementalType they confer immunity to.
 * NOTE: Does NOT perform suppression (Neutralizing Gas), mold-breaker bypass, or Ability Shield checks.
 * Those checks belong to the caller (SwitchCandidateScorer).
 */
public final class AbilityImmunityTable {

    private static final Map<String, Set<ElementalType>> ABILITY_IMMUNITIES;

    static {
        Map<String, Set<ElementalType>> map = new HashMap<>();

        // Water immunities
        Set<ElementalType> water = Set.of(ElementalTypes.WATER);
        map.put("stormdrain", water);
        map.put("waterabsorb", water);
        map.put("dryskin", water);

        // Electric immunities
        Set<ElementalType> electric = Set.of(ElementalTypes.ELECTRIC);
        map.put("voltabsorb", electric);
        map.put("lightningrod", electric);
        map.put("motordrive", electric);

        // Fire immunities
        Set<ElementalType> fire = Set.of(ElementalTypes.FIRE);
        map.put("flashfire", fire);
        map.put("wellbakedbody", fire);

        // Grass immunities
        Set<ElementalType> grass = Set.of(ElementalTypes.GRASS);
        map.put("sapsipper", grass);

        // Ground immunities
        Set<ElementalType> ground = Set.of(ElementalTypes.GROUND);
        map.put("levitate", ground);
        map.put("eartheater", ground);

        ABILITY_IMMUNITIES = Collections.unmodifiableMap(map);
    }

    private AbilityImmunityTable() {
    }

    /**
     * Returns true if the given ability ID confers immunity to the given move type.
     */
    public static boolean isImmune(String abilityId, ElementalType moveType) {
        if (abilityId == null || moveType == null) {
            return false;
        }
        Set<ElementalType> immunities = ABILITY_IMMUNITIES.get(abilityId.toLowerCase().trim());
        if (immunities == null) {
            return false;
        }
        return immunities.contains(moveType);
    }
}
