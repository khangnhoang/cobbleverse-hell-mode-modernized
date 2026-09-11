package com.cobbleverse.legendaryrule.lead.simulation.model;

import java.util.*;

/**
 * Mutable/Cloneable battle state tracking Turn-1 field conditions and Pokémon statuses.
 */
public class Turn1BattleState {

    public enum Weather {
        NONE, SUN, RAIN, SAND, SNOW
    }

    public enum Terrain {
        NONE, PSYCHIC, GRASSY, ELECTRIC, MISTY
    }

    public enum RedirectionType {
        NONE, FOLLOW_ME, RAGE_POWDER
    }

    private final List<CompetitivePokemonProfile> playerLeads;
    private final List<CompetitivePokemonProfile> kogaLeads;

    private Weather weather = Weather.NONE;
    private Terrain terrain = Terrain.NONE;
    private boolean trickRoom = false;
    private boolean tailwindPlayer = false;
    private boolean tailwindKoga = false;

    // HP tracking: keyed by slot ("player_0", "player_1", "koga_0", "koga_1")
    private final Map<String, Integer> currentHp = new HashMap<>();

    // Stat stages: keyed by slot + "_" + stat (e.g., "koga_0_atk" -> -1)
    private final Map<String, Integer> statStages = new HashMap<>();

    // Status / Turn-1 mechanics tracking
    private final Set<String> consumedItems = new HashSet<>();
    private final Set<String> flinched = new HashSet<>();
    private final Set<String> protectedSlots = new HashSet<>();
    private final Set<String> helpingHandBoosted = new HashSet<>();

    // Redirection per side ("player" -> slot, "koga" -> slot)
    private final Map<String, String> redirectionSlot = new HashMap<>();
    private final Map<String, RedirectionType> redirectionType = new HashMap<>();

    public Turn1BattleState(List<CompetitivePokemonProfile> playerLeads, List<CompetitivePokemonProfile> kogaLeads) {
        if (playerLeads.size() != 2 || kogaLeads.size() != 2) {
            throw new IllegalArgumentException("Both sides must have exactly 2 lead Pokémon");
        }
        this.playerLeads = List.copyOf(playerLeads);
        this.kogaLeads = List.copyOf(kogaLeads);

        currentHp.put("player_0", playerLeads.get(0).actualStats().hp());
        currentHp.put("player_1", playerLeads.get(1).actualStats().hp());
        currentHp.put("koga_0", kogaLeads.get(0).actualStats().hp());
        currentHp.put("koga_1", kogaLeads.get(1).actualStats().hp());
    }

    public Turn1BattleState(Turn1BattleState other) {
        this.playerLeads = other.playerLeads;
        this.kogaLeads = other.kogaLeads;
        this.weather = other.weather;
        this.terrain = other.terrain;
        this.trickRoom = other.trickRoom;
        this.tailwindPlayer = other.tailwindPlayer;
        this.tailwindKoga = other.tailwindKoga;
        this.currentHp.putAll(other.currentHp);
        this.statStages.putAll(other.statStages);
        this.consumedItems.addAll(other.consumedItems);
        this.flinched.addAll(other.flinched);
        this.protectedSlots.addAll(other.protectedSlots);
        this.helpingHandBoosted.addAll(other.helpingHandBoosted);
        this.redirectionSlot.putAll(other.redirectionSlot);
        this.redirectionType.putAll(other.redirectionType);
    }

    public Turn1BattleState copy() {
        return new Turn1BattleState(this);
    }

    public List<CompetitivePokemonProfile> getPlayerLeads() {
        return playerLeads;
    }

    public List<CompetitivePokemonProfile> getKogaLeads() {
        return kogaLeads;
    }

    public Weather getWeather() {
        return weather;
    }

    public void setWeather(Weather weather) {
        this.weather = Objects.requireNonNull(weather);
    }

    public Terrain getTerrain() {
        return terrain;
    }

    public void setTerrain(Terrain terrain) {
        this.terrain = Objects.requireNonNull(terrain);
    }

    public boolean isTrickRoom() {
        return trickRoom;
    }

    public void setTrickRoom(boolean trickRoom) {
        this.trickRoom = trickRoom;
    }

    public boolean isTailwindPlayer() {
        return tailwindPlayer;
    }

    public void setTailwindPlayer(boolean tailwindPlayer) {
        this.tailwindPlayer = tailwindPlayer;
    }

    public boolean isTailwindKoga() {
        return tailwindKoga;
    }

    public void setTailwindKoga(boolean tailwindKoga) {
        this.tailwindKoga = tailwindKoga;
    }

    public int getHp(String slot) {
        return currentHp.getOrDefault(slot, 0);
    }

    public void setHp(String slot, int hp) {
        currentHp.put(slot, Math.max(0, hp));
    }

    public void applyDamage(String slot, int damage) {
        int hp = getHp(slot);
        setHp(slot, hp - damage);
    }

    public boolean isFainted(String slot) {
        return getHp(slot) <= 0;
    }

    public int getStatStage(String slot, String stat) {
        return statStages.getOrDefault(slot + "_" + stat.toLowerCase(Locale.ROOT), 0);
    }

    public void modifyStatStage(String slot, String stat, int delta) {
        String key = slot + "_" + stat.toLowerCase(Locale.ROOT);
        int current = statStages.getOrDefault(key, 0);
        int updated = Math.max(-6, Math.min(6, current + delta));
        statStages.put(key, updated);
    }

    public boolean isItemConsumed(String slot) {
        return consumedItems.contains(slot);
    }

    public void consumeItem(String slot) {
        consumedItems.add(slot);
    }

    public boolean isFlinched(String slot) {
        return flinched.contains(slot);
    }

    public void setFlinched(String slot) {
        flinched.add(slot);
    }

    public boolean isProtected(String slot) {
        return protectedSlots.contains(slot);
    }

    public void setProtected(String slot) {
        protectedSlots.add(slot);
    }

    public boolean isHelpingHandBoosted(String slot) {
        return helpingHandBoosted.contains(slot);
    }

    public void setHelpingHandBoosted(String slot) {
        helpingHandBoosted.add(slot);
    }

    public void setRedirection(String side, String slot, RedirectionType type) {
        redirectionSlot.put(side, slot);
        redirectionType.put(side, type);
    }

    public String getRedirectionSlot(String side) {
        return redirectionSlot.get(side);
    }

    public RedirectionType getRedirectionType(String side) {
        return redirectionType.getOrDefault(side, RedirectionType.NONE);
    }

    public CompetitivePokemonProfile getProfileBySlot(String slot) {
        return switch (slot) {
            case "player_0" -> playerLeads.get(0);
            case "player_1" -> playerLeads.get(1);
            case "koga_0" -> kogaLeads.get(0);
            case "koga_1" -> kogaLeads.get(1);
            default -> throw new IllegalArgumentException("Unknown slot: " + slot);
        };
    }

    public String getSide(String slot) {
        if (slot.startsWith("player")) return "player";
        if (slot.startsWith("koga")) return "koga";
        throw new IllegalArgumentException("Unknown slot: " + slot);
    }

    public String getOpposingSide(String slot) {
        return getSide(slot).equals("player") ? "koga" : "player";
    }

    public List<String> getSlotsForSide(String side) {
        if ("player".equals(side)) {
            return List.of("player_0", "player_1");
        } else if ("koga".equals(side)) {
            return List.of("koga_0", "koga_1");
        }
        return List.of();
    }

    public double getHpPercent(String slot) {
        CompetitivePokemonProfile profile = getProfileBySlot(slot);
        int max = profile.actualStats().hp();
        if (max == 0) return 0.0;
        return (double) getHp(slot) / (double) max;
    }
}
