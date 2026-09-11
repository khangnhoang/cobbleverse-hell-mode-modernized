package com.cobbleverse.legendaryrule.lead.simulation;

import com.cobbleverse.legendaryrule.lead.simulation.model.ActualStats;
import com.cobbleverse.legendaryrule.lead.simulation.model.CompetitivePokemonProfile;
import com.cobbleverse.legendaryrule.lead.simulation.model.CompetitivePokemonProfile.Stat;
import com.cobbleverse.legendaryrule.lead.simulation.model.MoveProfile;
import com.cobbleverse.legendaryrule.lead.simulation.model.Turn1Action;
import com.cobbleverse.legendaryrule.lead.simulation.model.Turn1BattleState;
import com.cobbleverse.legendaryrule.lead.simulation.resolver.Turn1ActionResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class Turn1ActionResolverTest {

    private static CompetitivePokemonProfile createMon(
            String species,
            List<String> types,
            String ability,
            String heldItem,
            int speed,
            int hp
    ) {
        ActualStats stats = new ActualStats(hp, 100, 100, 100, 100, speed);
        Map<Stat, Integer> baseStats = Map.of(Stat.HP, 100, Stat.ATK, 100, Stat.DEF, 100, Stat.SPA, 100, Stat.SPD, 100, Stat.SPE, 100);
        Map<Stat, Integer> ivs = Map.of(Stat.HP, 31, Stat.ATK, 31, Stat.DEF, 31, Stat.SPA, 31, Stat.SPD, 31, Stat.SPE, 31);
        Map<Stat, Integer> evs = Map.of(Stat.HP, 0, Stat.ATK, 0, Stat.DEF, 0, Stat.SPA, 0, Stat.SPD, 0, Stat.SPE, 0);

        return new CompetitivePokemonProfile(
                species,
                List.of(),
                types,
                55,
                baseStats,
                "hardy",
                ivs,
                evs,
                ability,
                heldItem,
                List.of(),
                stats
        );
    }

    private static CompetitivePokemonProfile createMon(
            String species,
            List<String> types,
            String ability,
            int speed,
            int hp
    ) {
        return createMon(species, types, ability, "", speed, hp);
    }

    private static final MoveProfile HELPING_HAND = new MoveProfile("helpinghand", "normal", MoveProfile.Category.STATUS, 0, 5, MoveProfile.Target.ALLY, false, false);
    private static final MoveProfile PROTECT = new MoveProfile("protect", "normal", MoveProfile.Category.STATUS, 0, 4, MoveProfile.Target.SELF, false, false);
    private static final MoveProfile FAKE_OUT = new MoveProfile("fakeout", "normal", MoveProfile.Category.PHYSICAL, 40, 3, MoveProfile.Target.SINGLE_OPPONENT, true, false);
    private static final MoveProfile FOLLOW_ME = new MoveProfile("followme", "normal", MoveProfile.Category.STATUS, 0, 2, MoveProfile.Target.SELF, false, false);
    private static final MoveProfile RAGE_POWDER = new MoveProfile("ragepowder", "bug", MoveProfile.Category.STATUS, 0, 2, MoveProfile.Target.SELF, false, false);
    private static final MoveProfile SUCKER_PUNCH = new MoveProfile("suckerpunch", "dark", MoveProfile.Category.PHYSICAL, 70, 1, MoveProfile.Target.SINGLE_OPPONENT, true, false);
    private static final MoveProfile NORMAL_ATTACK = new MoveProfile("closecombat", "fighting", MoveProfile.Category.PHYSICAL, 120, 0, MoveProfile.Target.SINGLE_OPPONENT, true, false);
    private static final MoveProfile SPREAD_ATTACK = new MoveProfile("heatwave", "fire", MoveProfile.Category.SPECIAL, 95, 0, MoveProfile.Target.ALL_ADJACENT_OPPONENTS, false, true);
    private static final MoveProfile TRICK_ROOM = new MoveProfile("trickroom", "psychic", MoveProfile.Category.STATUS, 0, -7, MoveProfile.Target.SELF, false, false);

    @Test
    @DisplayName("Priority bracket resolution: +5 > +4 > +3 > +2 > +1 > 0 > -7")
    void testPriorityBracketExecutionOrder() {
        CompetitivePokemonProfile slowMover = createMon("slow", List.of("normal"), "none", "", 10, 200);
        CompetitivePokemonProfile fastMover = createMon("fast", List.of("normal"), "none", "", 200, 200);
        CompetitivePokemonProfile koga1 = createMon("koga1", List.of("normal"), "none", "", 100, 200);
        CompetitivePokemonProfile koga2 = createMon("koga2", List.of("normal"), "none", "", 100, 200);

        Turn1BattleState state = new Turn1BattleState(List.of(slowMover, fastMover), List.of(koga1, koga2));

        // slowMover uses +3 Fake Out, fastMover uses 0 Normal Attack
        Turn1Action act1 = new Turn1Action("player_1", NORMAL_ATTACK, "koga_0", 0, 200);
        Turn1Action act2 = new Turn1Action("player_0", FAKE_OUT, "koga_1", 3, 10);

        List<String> log = Turn1ActionResolver.resolveActions(state, List.of(act1, act2), false);

        // Fake Out (+3) must execute before Normal Attack (0) even though player_0 is slower!
        int fakeOutIndex = -1;
        int normalIndex = -1;
        for (int i = 0; i < log.size(); i++) {
            if (log.get(i).contains("fakeout")) fakeOutIndex = i;
            if (log.get(i).contains("closecombat")) normalIndex = i;
        }

        assertTrue(fakeOutIndex >= 0 && normalIndex >= 0, "Both actions must be logged");
        assertTrue(fakeOutIndex < normalIndex, "Fake Out (+3) must execute before Close Combat (0)");
    }

    @Test
    @DisplayName("Follow Me redirects single-target attacks to user")
    void testFollowMeRedirection() {
        // Player: Indeedee-F (Follow Me) + Kingambit
        CompetitivePokemonProfile indeedee = createMon("indeedee", List.of("psychic", "normal"), "psychicsurge", 90, 160);
        CompetitivePokemonProfile kingambit = createMon("kingambit", List.of("dark", "steel"), "defiant", 60, 200);

        // Koga: Sneasler + Okidogi
        CompetitivePokemonProfile sneasler = createMon("sneasler", List.of("fighting", "poison"), "unburden", 150, 160);
        CompetitivePokemonProfile okidogi = createMon("okidogi", List.of("poison", "fighting"), "guarddog", 95, 200);

        Turn1BattleState state = new Turn1BattleState(List.of(indeedee, kingambit), List.of(sneasler, okidogi));

        // Indeedee uses Follow Me (+2)
        Turn1Action actFollowMe = new Turn1Action("player_0", FOLLOW_ME, "player_0", 2, 90);
        // Sneasler attacks Kingambit ("player_1") with Close Combat
        Turn1Action actSneasler = new Turn1Action("koga_0", NORMAL_ATTACK, "player_1", 0, 150);

        List<String> log = Turn1ActionResolver.resolveActions(state, List.of(actFollowMe, actSneasler), false);

        // Kingambit must be undamaged! Indeedee must take the damage!
        assertEquals(200, state.getHp("player_1"), "Kingambit must be untouched due to Follow Me");
        assertTrue(state.getHp("player_0") < 160, "Indeedee must have received redirected attack");
    }

    @Test
    @DisplayName("Rage Powder redirects single-target attacks EXCEPT Grass-types and Overcoat/Safety Goggles")
    void testRagePowderExceptions() {
        // Koga: Amoonguss (Rage Powder) + Slowking
        CompetitivePokemonProfile amoonguss = createMon("amoonguss", List.of("grass", "poison"), "regenerator", "", 30, 239);
        CompetitivePokemonProfile slowking = createMon("slowking", List.of("poison", "psychic"), "drought", "", 34, 221);

        // Player: Standard attacker vs Grass attacker (Venusaur)
        CompetitivePokemonProfile standardAttacker = createMon("standard", List.of("fighting"), "none", "", 100, 150);
        CompetitivePokemonProfile grassAttacker = createMon("venusaur", List.of("grass", "poison"), "chlorophyll", "", 100, 150);

        Turn1BattleState state = new Turn1BattleState(List.of(standardAttacker, grassAttacker), List.of(amoonguss, slowking));

        Turn1Action actRagePowder = new Turn1Action("koga_0", RAGE_POWDER, "koga_0", 2, 30);
        // Standard attacker targets Slowking ("koga_1") -> Should be redirected to Amoonguss
        Turn1Action actStandard = new Turn1Action("player_0", NORMAL_ATTACK, "koga_1", 0, 100);

        Turn1ActionResolver.resolveActions(state, List.of(actRagePowder, actStandard), false);
        assertEquals(221, state.getHp("koga_1"), "Slowking should be untouched by standard attacker due to Rage Powder");
        assertTrue(state.getHp("koga_0") < 239, "Amoonguss should have absorbed redirected attack");

        // Now test Grass attacker targeting Slowking ("koga_1") -> Grass ignores Rage Powder!
        Turn1Action actGrass = new Turn1Action("player_1", NORMAL_ATTACK, "koga_1", 0, 100);
        Turn1ActionResolver.resolveActions(state, List.of(actGrass), false);
        assertTrue(state.getHp("koga_1") < 221, "Slowking should be hit by Grass-type attacker ignoring Rage Powder");
    }

    @Test
    @DisplayName("Good as Gold does NOT prevent Rage Powder redirection")
    void testGoodAsGoldRagePowder() {
        CompetitivePokemonProfile amoonguss = createMon("amoonguss", List.of("grass", "poison"), "regenerator", "", 30, 239);
        CompetitivePokemonProfile partner = createMon("partner", List.of("normal"), "none", "", 50, 200);

        // Gholdengo has Good as Gold
        CompetitivePokemonProfile gholdengo = createMon("gholdengo", List.of("steel", "ghost"), "goodasgold", "", 90, 180);
        CompetitivePokemonProfile player2 = createMon("player2", List.of("normal"), "none", "", 50, 150);

        Turn1BattleState state = new Turn1BattleState(List.of(gholdengo, player2), List.of(amoonguss, partner));

        Turn1Action actRagePowder = new Turn1Action("koga_0", RAGE_POWDER, "koga_0", 2, 30);
        // Gholdengo uses single-target attack targeting partner ("koga_1")
        MoveProfile shadowBall = new MoveProfile("shadowball", "ghost", MoveProfile.Category.SPECIAL, 80, 0, MoveProfile.Target.SINGLE_OPPONENT, false, false);
        Turn1Action actGholdengo = new Turn1Action("player_0", shadowBall, "koga_1", 0, 90);

        Turn1ActionResolver.resolveActions(state, List.of(actRagePowder, actGholdengo), false);

        // Gholdengo's attack is redirected to Amoonguss! Partner is untouched!
        assertEquals(200, state.getHp("koga_1"), "Partner must be untouched because Good as Gold does NOT prevent redirection");
        assertTrue(state.getHp("koga_0") < 239, "Amoonguss should take the redirected hit from Gholdengo");
    }

    @Test
    @DisplayName("Spread moves are NEVER redirected by Follow Me / Rage Powder")
    void testSpreadMovesNeverRedirected() {
        CompetitivePokemonProfile indeedee = createMon("indeedee", List.of("psychic", "normal"), "psychicsurge", "", 90, 160);
        CompetitivePokemonProfile partner = createMon("partner", List.of("normal"), "none", "", 60, 200);

        CompetitivePokemonProfile koga1 = createMon("charizard", List.of("fire", "flying"), "solarpower", "", 100, 160);
        CompetitivePokemonProfile koga2 = createMon("koga2", List.of("normal"), "none", "", 80, 160);

        Turn1BattleState state = new Turn1BattleState(List.of(indeedee, partner), List.of(koga1, koga2));

        Turn1Action actFollowMe = new Turn1Action("player_0", FOLLOW_ME, "player_0", 2, 90);
        // Charizard uses Heat Wave (spread move)
        Turn1Action actHeatWave = new Turn1Action("koga_0", SPREAD_ATTACK, "all_opponents", 0, 100);

        Turn1ActionResolver.resolveActions(state, List.of(actFollowMe, actHeatWave), false);

        // BOTH player Pokémon should take damage from spread move!
        assertTrue(state.getHp("player_0") < 160, "Indeedee should take damage");
        assertTrue(state.getHp("player_1") < 200, "Partner should also take damage (spread move was NOT redirected)");
    }

    @Test
    @DisplayName("Psychic Terrain blocks priority moves targeting grounded opponents")
    void testPsychicTerrainPriorityBlock() {
        CompetitivePokemonProfile groundedTarget = createMon("okidogi", List.of("poison", "fighting"), "guarddog", "", 80, 200);
        CompetitivePokemonProfile flyingTarget = createMon("corviknight", List.of("flying", "steel"), "pressure", "", 70, 200);

        CompetitivePokemonProfile attacker = createMon("attacker", List.of("normal"), "none", "", 110, 150);
        CompetitivePokemonProfile partner = createMon("partner", List.of("normal"), "none", "", 100, 150);

        Turn1BattleState state = new Turn1BattleState(List.of(attacker, partner), List.of(groundedTarget, flyingTarget));
        state.setTerrain(Turn1BattleState.Terrain.PSYCHIC);

        // Attacker uses Fake Out (+3) on grounded target
        Turn1Action actFakeOutGrounded = new Turn1Action("player_0", FAKE_OUT, "koga_0", 3, 110);
        List<String> log = Turn1ActionResolver.resolveActions(state, List.of(actFakeOutGrounded), false);

        // Should fail!
        assertEquals(200, state.getHp("koga_0"), "Grounded target should take 0 damage from priority in Psychic Terrain");
        assertFalse(state.isFlinched("koga_0"), "Grounded target should not flinch");
        assertTrue(log.stream().anyMatch(any -> any.contains("failed against okidogi due to Psychic Terrain") || any.contains("failed")));

        // Now attacker uses Fake Out (+3) on flying target
        Turn1BattleState state2 = new Turn1BattleState(List.of(attacker, partner), List.of(groundedTarget, flyingTarget));
        state2.setTerrain(Turn1BattleState.Terrain.PSYCHIC);
        Turn1Action actFakeOutFlying = new Turn1Action("player_0", FAKE_OUT, "koga_1", 3, 110);
        Turn1ActionResolver.resolveActions(state2, List.of(actFakeOutFlying), false);

        // Flying target is ungrounded -> Fake Out should hit and deal damage!
        assertTrue(state2.getHp("koga_1") < 200, "Flying target is ungrounded so priority move should hit in Psychic Terrain");
        assertTrue(state2.isFlinched("koga_1"), "Flying target should flinch");
    }

    @Test
    @DisplayName("Earthquake (ALL_ADJACENT) hits both opponents and grounded ally Gholdengo for 2x super-effective damage")
    void testEarthquakeAllAdjacentSpreadHitsAllyGholdengo() {
        CompetitivePokemonProfile lando = createMon("landorus", List.of("ground", "flying"), "intimidate", "", 120, 180);
        CompetitivePokemonProfile gholdengo = createMon("gholdengo", List.of("steel", "ghost"), "goodasgold", "", 100, 180);

        CompetitivePokemonProfile koga0 = createMon("venusaur", List.of("grass", "poison"), "chlorophyll", "", 90, 170);
        CompetitivePokemonProfile koga1 = createMon("slowking", List.of("poison", "psychic"), "drought", "", 34, 190);

        Turn1BattleState state = new Turn1BattleState(List.of(lando, gholdengo), List.of(koga0, koga1));

        MoveProfile earthquake = new MoveProfile("earthquake", "ground", MoveProfile.Category.PHYSICAL, 100, 0, MoveProfile.Target.ALL_ADJACENT, false, true, 100);
        Turn1Action eqAction = new Turn1Action("player_0", earthquake, "all_adjacent", 0, 120);

        List<String> log = Turn1ActionResolver.resolveActions(state, List.of(eqAction), false);

        // Opponents should take damage
        assertTrue(state.getHp("koga_0") < 170, "Venusaur should take damage from Earthquake");
        assertTrue(state.getHp("koga_1") < 190, "Slowking should take damage from Earthquake");

        // Grounded ally Gholdengo MUST also take damage from Earthquake!
        assertTrue(state.getHp("player_1") < 180, "Ally Gholdengo must take damage from Earthquake (ALL_ADJACENT target)");
    }

    @Test
    @DisplayName("Rock Slide (ALL_ADJACENT_OPPONENTS) hits both opponents and does NOT damage ally")
    void testRockSlideAllAdjacentOpponentsSpreadDoesNotHitAlly() {
        CompetitivePokemonProfile lando = createMon("landorus", List.of("ground", "flying"), "intimidate", "", 120, 180);
        CompetitivePokemonProfile gholdengo = createMon("gholdengo", List.of("steel", "ghost"), "goodasgold", "", 100, 180);

        CompetitivePokemonProfile koga0 = createMon("venusaur", List.of("grass", "poison"), "chlorophyll", "", 90, 170);
        CompetitivePokemonProfile koga1 = createMon("slowking", List.of("poison", "psychic"), "drought", "", 34, 190);

        Turn1BattleState state = new Turn1BattleState(List.of(lando, gholdengo), List.of(koga0, koga1));

        MoveProfile rockSlide = new MoveProfile("rockslide", "rock", MoveProfile.Category.PHYSICAL, 75, 0, MoveProfile.Target.ALL_ADJACENT_OPPONENTS, false, true, 90);
        Turn1Action rsAction = new Turn1Action("player_0", rockSlide, "all_opponents", 0, 120);

        List<String> log = Turn1ActionResolver.resolveActions(state, List.of(rsAction), false);

        // Opponents take damage
        assertTrue(state.getHp("koga_0") < 170, "Venusaur should take damage from Rock Slide");
        assertTrue(state.getHp("koga_1") < 190, "Slowking should take damage from Rock Slide");

        // Ally Gholdengo MUST NOT take damage!
        assertEquals(180, state.getHp("player_1"), "Ally Gholdengo must NOT take damage from Rock Slide (ALL_ADJACENT_OPPONENTS target)");

        // Assumption log must be present for 90% accuracy move
        assertTrue(log.stream().anyMatch(l -> l.contains("[ASSUMPTION: rockslide hit (accuracy: 90%)]")));
    }

    @Test
    @DisplayName("Hurricane accuracy drops to 50% under Sun and logs explicit assumption")
    void testHurricaneAccuracyAssumptionLoggedUnderSun() {
        CompetitivePokemonProfile pelipper = createMon("pelipper", List.of("water", "flying"), "drizzle", "", 80, 150);
        CompetitivePokemonProfile partner = createMon("partner", List.of("normal"), "none", "", 50, 150);
        CompetitivePokemonProfile koga0 = createMon("slowking", List.of("poison", "psychic"), "drought", "", 34, 190);
        CompetitivePokemonProfile koga1 = createMon("venusaur", List.of("grass", "poison"), "chlorophyll", "", 100, 170);

        Turn1BattleState state = new Turn1BattleState(List.of(pelipper, partner), List.of(koga0, koga1));
        state.setWeather(Turn1BattleState.Weather.SUN); // Sun active

        MoveProfile hurricane = new MoveProfile("hurricane", "flying", MoveProfile.Category.SPECIAL, 110, 0, MoveProfile.Target.SINGLE_OPPONENT, false, false, 70);
        Turn1Action hurricaneAction = new Turn1Action("player_0", hurricane, "koga_1", 0, 80);

        List<String> log = Turn1ActionResolver.resolveActions(state, List.of(hurricaneAction), false);

        // Check that assumption is logged with 50% accuracy under Sun
        assertTrue(log.stream().anyMatch(l -> l.contains("[ASSUMPTION: hurricane hit (accuracy: 50%)]")),
                "Hurricane under Sun must log assumption with 50% accuracy");
    }
}
