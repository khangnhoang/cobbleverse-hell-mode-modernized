package com.cobbleverse.legendaryrule.lead.simulation.resolver;

import com.cobbleverse.legendaryrule.lead.simulation.model.CompetitivePokemonProfile;
import com.cobbleverse.legendaryrule.lead.simulation.model.Turn1BattleState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Resolves entry abilities (Weather, Terrain, Intimidate) on Turn 1 in canonical speed order.
 */
public final class Turn1EntryResolver {

    private Turn1EntryResolver() {}

    private record EntryMon(String slot, CompetitivePokemonProfile profile, int speed) {}

    /**
     * Resolves all on-entry abilities for the battle state.
     */
    public static void resolveEntry(Turn1BattleState state) {
        Objects.requireNonNull(state, "state must not be null");

        List<EntryMon> entries = new ArrayList<>();
        entries.add(new EntryMon("player_0", state.getPlayerLeads().get(0), state.getPlayerLeads().get(0).actualStats().spe()));
        entries.add(new EntryMon("player_1", state.getPlayerLeads().get(1), state.getPlayerLeads().get(1).actualStats().spe()));
        entries.add(new EntryMon("koga_0", state.getKogaLeads().get(0), state.getKogaLeads().get(0).actualStats().spe()));
        entries.add(new EntryMon("koga_1", state.getKogaLeads().get(1), state.getKogaLeads().get(1).actualStats().spe()));

        // Fast to slow activation order: slowest setter activates last and overrides previous weather/terrain
        entries.sort(Comparator.comparingInt(EntryMon::speed).reversed());

        // 1. Weather and Terrain setters
        for (EntryMon entry : entries) {
            String ability = entry.profile.ability();
            switch (ability) {
                case "drought" -> state.setWeather(Turn1BattleState.Weather.SUN);
                case "drizzle" -> state.setWeather(Turn1BattleState.Weather.RAIN);
                case "sandstream" -> state.setWeather(Turn1BattleState.Weather.SAND);
                case "snowwarning" -> state.setWeather(Turn1BattleState.Weather.SNOW);
                case "psychicsurge" -> state.setTerrain(Turn1BattleState.Terrain.PSYCHIC);
                case "grassysurge" -> state.setTerrain(Turn1BattleState.Terrain.GRASSY);
                case "electricsurge" -> state.setTerrain(Turn1BattleState.Terrain.ELECTRIC);
                case "mistysurge" -> state.setTerrain(Turn1BattleState.Terrain.MISTY);
            }
        }

        // 2. Intimidate activations
        for (EntryMon entry : entries) {
            if ("intimidate".equals(entry.profile.ability())) {
                resolveIntimidate(state, entry.slot);
            }
        }
    }

    private static void resolveIntimidate(Turn1BattleState state, String userSlot) {
        String opposingSide = state.getOpposingSide(userSlot);
        for (String targetSlot : state.getSlotsForSide(opposingSide)) {
            CompetitivePokemonProfile target = state.getProfileBySlot(targetSlot);
            String ability = target.ability();

            if ("guarddog".equals(ability)) {
                // Okidogi Guard Dog: Intimidate is blocked and boosts Attack by +1 stage
                state.modifyStatStage(targetSlot, "atk", 1);
            } else if ("defiant".equals(ability)) {
                // Kingambit Defiant: -1 from Intimidate triggers +2, net +1
                state.modifyStatStage(targetSlot, "atk", 1);
            } else if ("clearbody".equals(ability) || "whitesmoke".equals(ability) || "innerfocus".equals(ability)) {
                // Stat drop blocked completely
            } else {
                // Note: Good as Gold does NOT block Intimidate (it only blocks status moves).
                // Gholdengo and standard targets receive -1 Attack stage.
                state.modifyStatStage(targetSlot, "atk", -1);
            }
        }
    }
}
