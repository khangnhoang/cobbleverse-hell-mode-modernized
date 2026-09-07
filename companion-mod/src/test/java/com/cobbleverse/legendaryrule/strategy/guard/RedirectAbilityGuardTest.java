package com.cobbleverse.legendaryrule.strategy.guard;

import com.cobblemon.mod.common.api.abilities.Ability;
import com.cobblemon.mod.common.api.abilities.AbilityTemplate;
import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.api.pokemon.helditem.HeldItemManager;
import com.cobblemon.mod.common.api.types.ElementalType;
import com.cobblemon.mod.common.api.types.ElementalTypes;
import com.cobblemon.mod.common.battles.ActiveBattlePokemon;
import com.cobblemon.mod.common.battles.BattleSide;
import com.cobblemon.mod.common.battles.MoveTarget;
import com.cobblemon.mod.common.battles.actor.PokemonBattleActor;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.pokemon.Pokemon;
import kotlin.LazyKt;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RedirectAbilityGuardTest {

    private static Unsafe unsafe;
    private static Field moveTemplateField;
    private static Field moveNameField;
    private static Field moveTargetField;
    private static Field moveElementalTypeField;
    private static Field abilityTemplateField;
    private static Field abilityTemplateNameField;
    private static Field pokemonAbilityField;
    private static Field bpEffectedPokemonField;
    private static Field bpActorField;
    private static Field bpHeldItemDelegateField;
    private static Field abpBattlePokemonField;
    private static Field abpBattleField;
    private static Field actorBattleField;
    private static Field sideActorsField;
    private static Field sideBattleField;
    private static Field actorActivePokemonField;
    private static Field battleSide1Field;
    private static Field battleSide2Field;

    @BeforeAll
    static void setUpAll() throws Exception {
        try {
            net.minecraft.SharedConstants.createGameVersion();
            java.lang.reflect.Method init = Class.forName("net.minecraft.Bootstrap").getDeclaredMethod("initialize");
            init.setAccessible(true);
            init.invoke(null);
        } catch (Throwable t) {
            // ignore if already initialized or running in environment where Bootstrap is unavailable
        }

        Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        unsafe = (Unsafe) f.get(null);

        moveTemplateField = Move.class.getDeclaredField("template");
        moveTemplateField.setAccessible(true);

        moveNameField = MoveTemplate.class.getDeclaredField("name");
        moveNameField.setAccessible(true);

        moveTargetField = MoveTemplate.class.getDeclaredField("target");
        moveTargetField.setAccessible(true);

        moveElementalTypeField = MoveTemplate.class.getDeclaredField("elementalType");
        moveElementalTypeField.setAccessible(true);

        abilityTemplateField = Ability.class.getDeclaredField("template");
        abilityTemplateField.setAccessible(true);

        abilityTemplateNameField = AbilityTemplate.class.getDeclaredField("name");
        abilityTemplateNameField.setAccessible(true);

        pokemonAbilityField = Pokemon.class.getDeclaredField("ability");
        pokemonAbilityField.setAccessible(true);

        bpEffectedPokemonField = BattlePokemon.class.getDeclaredField("effectedPokemon");
        bpEffectedPokemonField.setAccessible(true);

        bpActorField = BattlePokemon.class.getDeclaredField("actor");
        bpActorField.setAccessible(true);

        bpHeldItemDelegateField = BattlePokemon.class.getDeclaredField("heldItemManager$delegate");
        bpHeldItemDelegateField.setAccessible(true);

        abpBattlePokemonField = ActiveBattlePokemon.class.getDeclaredField("battlePokemon");
        abpBattlePokemonField.setAccessible(true);

        abpBattleField = ActiveBattlePokemon.class.getDeclaredField("battle");
        abpBattleField.setAccessible(true);

        actorBattleField = BattleActor.class.getDeclaredField("battle");
        actorBattleField.setAccessible(true);

        sideActorsField = BattleSide.class.getDeclaredField("actors");
        sideActorsField.setAccessible(true);

        sideBattleField = BattleSide.class.getDeclaredField("battle");
        sideBattleField.setAccessible(true);

        actorActivePokemonField = BattleActor.class.getDeclaredField("activePokemon");
        actorActivePokemonField.setAccessible(true);

        battleSide1Field = PokemonBattle.class.getDeclaredField("side1");
        battleSide1Field.setAccessible(true);

        battleSide2Field = PokemonBattle.class.getDeclaredField("side2");
        battleSide2Field.setAccessible(true);
    }

    private Move createMove(String name, MoveTarget target, ElementalType type) throws Exception {
        MoveTemplate template = (MoveTemplate) unsafe.allocateInstance(MoveTemplate.class);
        moveNameField.set(template, name);
        moveTargetField.set(template, target);
        moveElementalTypeField.set(template, type);

        Move move = (Move) unsafe.allocateInstance(Move.class);
        moveTemplateField.set(move, template);
        return move;
    }

    private ActiveBattlePokemon createActivePokemon(String abilityName, String heldItemId) throws Exception {
        return createActivePokemon(abilityName, heldItemId, null, null);
    }

    private ActiveBattlePokemon createActivePokemon(String abilityName, String heldItemId, BattleActor actor, PokemonBattle battle) throws Exception {
        AbilityTemplate abilityTemplate = (AbilityTemplate) unsafe.allocateInstance(AbilityTemplate.class);
        abilityTemplateNameField.set(abilityTemplate, abilityName);

        Ability ability = (Ability) unsafe.allocateInstance(Ability.class);
        abilityTemplateField.set(ability, abilityTemplate);

        Pokemon pokemon = (Pokemon) unsafe.allocateInstance(Pokemon.class);
        pokemonAbilityField.set(pokemon, ability);
        pokemon.setUuid(UUID.randomUUID());

        BattlePokemon bp = (BattlePokemon) unsafe.allocateInstance(BattlePokemon.class);
        bpEffectedPokemonField.set(bp, pokemon);

        if (actor == null) {
            actor = (BattleActor) unsafe.allocateInstance(PokemonBattleActor.class);
            if (battle != null) {
                actorBattleField.set(actor, battle);
            }
        }
        bpActorField.set(bp, actor);

        HeldItemManager manager = (HeldItemManager) Proxy.newProxyInstance(
            HeldItemManager.class.getClassLoader(),
            new Class<?>[]{HeldItemManager.class},
            (proxy, method, args) -> {
                if ("showdownId".equals(method.getName())) {
                    return heldItemId;
                }
                return null;
            }
        );
        bpHeldItemDelegateField.set(bp, LazyKt.lazyOf(manager));

        ActiveBattlePokemon active = (ActiveBattlePokemon) unsafe.allocateInstance(ActiveBattlePokemon.class);
        abpBattlePokemonField.set(active, bp);

        if (battle != null) {
            abpBattleField.set(active, battle);
        }

        return active;
    }

    @Test
    @DisplayName("CASE 1: Hydro Pump -> Claydol redirected by active Gastrodon with Storm Drain")
    void testHydroPumpRedirectedByStormDrainWhenTargetingClaydol() throws Exception {
        Move hydroPump = createMove("hydropump", MoveTarget.normal, ElementalTypes.WATER);
        ActiveBattlePokemon rotomW = createActivePokemon("levitate", null);
        ActiveBattlePokemon claydol = createActivePokemon("levitate", null);
        ActiveBattlePokemon gastrodon = createActivePokemon("stormdrain", null);

        List<ActiveBattlePokemon> opponents = List.of(claydol, gastrodon);

        assertTrue(
            RedirectAbilityGuard.isMoveRedirected(hydroPump, rotomW.getBattlePokemon(), claydol.getBattlePokemon(), opponents),
            "Hydro Pump aimed at Claydol must be redirected when Gastrodon with Storm Drain is active"
        );
    }

    @Test
    @DisplayName("CASE 2: Thunderbolt -> Gyarados redirected by active Raichu with Lightning Rod")
    void testThunderboltRedirectedByLightningRodWhenTargetingGyarados() throws Exception {
        Move thunderbolt = createMove("thunderbolt", MoveTarget.normal, ElementalTypes.ELECTRIC);
        ActiveBattlePokemon rotomW = createActivePokemon("levitate", null);
        ActiveBattlePokemon gyarados = createActivePokemon("intimidate", null);
        ActiveBattlePokemon raichu = createActivePokemon("lightningrod", null);

        List<ActiveBattlePokemon> opponents = List.of(gyarados, raichu);

        assertTrue(
            RedirectAbilityGuard.isMoveRedirected(thunderbolt, rotomW.getBattlePokemon(), gyarados.getBattlePokemon(), opponents),
            "Thunderbolt aimed at Gyarados must be redirected when Raichu with Lightning Rod is active"
        );
    }

    @Test
    @DisplayName("CASE 3: Direct target on redirector itself is NOT marked as redirected")
    void testDirectTargetOnRedirectorNotMarkedAsRedirected() throws Exception {
        Move hydroPump = createMove("hydropump", MoveTarget.normal, ElementalTypes.WATER);
        ActiveBattlePokemon rotomW = createActivePokemon("levitate", null);
        ActiveBattlePokemon claydol = createActivePokemon("levitate", null);
        ActiveBattlePokemon gastrodon = createActivePokemon("stormdrain", null);

        List<ActiveBattlePokemon> opponents = List.of(claydol, gastrodon);

        assertFalse(
            RedirectAbilityGuard.isMoveRedirected(hydroPump, rotomW.getBattlePokemon(), gastrodon.getBattlePokemon(), opponents),
            "Move directly targeting the redirector must not be treated as redirected (native absorption handles it)"
        );
    }

    @Test
    @DisplayName("CASE 4: Hydro Pump -> Claydol is NOT redirected when no Storm Drain redirector is active")
    void testNoRedirectWhenRedirectorNotActive() throws Exception {
        Move hydroPump = createMove("hydropump", MoveTarget.normal, ElementalTypes.WATER);
        ActiveBattlePokemon rotomW = createActivePokemon("levitate", null);
        ActiveBattlePokemon claydol = createActivePokemon("levitate", null);
        ActiveBattlePokemon tyranitar = createActivePokemon("sandstream", null);

        List<ActiveBattlePokemon> opponents = List.of(claydol, tyranitar);

        assertFalse(
            RedirectAbilityGuard.isMoveRedirected(hydroPump, rotomW.getBattlePokemon(), claydol.getBattlePokemon(), opponents),
            "Hydro Pump must not be redirected if no opponent has Storm Drain"
        );
    }

    @Test
    @DisplayName("CASE 5: Water spread moves (Muddy Water, Surf) are NOT redirected by Storm Drain")
    void testSpreadWaterMovesNotRedirected() throws Exception {
        Move muddyWater = createMove("muddywater", MoveTarget.allAdjacentFoes, ElementalTypes.WATER);
        Move surf = createMove("surf", MoveTarget.allAdjacent, ElementalTypes.WATER);
        ActiveBattlePokemon rotomW = createActivePokemon("levitate", null);
        ActiveBattlePokemon claydol = createActivePokemon("levitate", null);
        ActiveBattlePokemon gastrodon = createActivePokemon("stormdrain", null);

        List<ActiveBattlePokemon> opponents = List.of(claydol, gastrodon);

        assertFalse(
            RedirectAbilityGuard.isMoveRedirected(muddyWater, rotomW.getBattlePokemon(), claydol.getBattlePokemon(), opponents),
            "Muddy Water (allAdjacentFoes) must NOT be redirected by Storm Drain"
        );
        assertFalse(
            RedirectAbilityGuard.isMoveRedirected(surf, rotomW.getBattlePokemon(), claydol.getBattlePokemon(), opponents),
            "Surf (allAdjacent) must NOT be redirected by Storm Drain"
        );
    }

    @Test
    @DisplayName("CASE 6: Electric spread moves (Electroweb, Discharge) are NOT redirected by Lightning Rod")
    void testSpreadElectricMovesNotRedirected() throws Exception {
        Move electroweb = createMove("electroweb", MoveTarget.allAdjacentFoes, ElementalTypes.ELECTRIC);
        Move discharge = createMove("discharge", MoveTarget.allAdjacent, ElementalTypes.ELECTRIC);
        ActiveBattlePokemon rotomW = createActivePokemon("levitate", null);
        ActiveBattlePokemon gyarados = createActivePokemon("intimidate", null);
        ActiveBattlePokemon raichu = createActivePokemon("lightningrod", null);

        List<ActiveBattlePokemon> opponents = List.of(gyarados, raichu);

        assertFalse(
            RedirectAbilityGuard.isMoveRedirected(electroweb, rotomW.getBattlePokemon(), gyarados.getBattlePokemon(), opponents),
            "Electroweb (allAdjacentFoes) must NOT be redirected by Lightning Rod"
        );
        assertFalse(
            RedirectAbilityGuard.isMoveRedirected(discharge, rotomW.getBattlePokemon(), gyarados.getBattlePokemon(), opponents),
            "Discharge (allAdjacent) must NOT be redirected by Lightning Rod"
        );
    }

    @Test
    @DisplayName("CASE 7: Non-redirectable targets (self, allies, foeSide) are NOT redirected")
    void testNonRedirectableTargetsNotRedirected() throws Exception {
        Move aquaRing = createMove("aquaring", MoveTarget.self, ElementalTypes.WATER);
        Move lifeDew = createMove("lifedew", MoveTarget.allies, ElementalTypes.WATER);
        Move charge = createMove("charge", MoveTarget.self, ElementalTypes.ELECTRIC);

        ActiveBattlePokemon rotomW = createActivePokemon("levitate", null);
        ActiveBattlePokemon claydol = createActivePokemon("levitate", null);
        ActiveBattlePokemon gastrodon = createActivePokemon("stormdrain", null);
        ActiveBattlePokemon raichu = createActivePokemon("lightningrod", null);

        List<ActiveBattlePokemon> waterOpponents = List.of(claydol, gastrodon);
        List<ActiveBattlePokemon> electricOpponents = List.of(claydol, raichu);

        assertFalse(RedirectAbilityGuard.isMoveRedirected(aquaRing, rotomW.getBattlePokemon(), claydol.getBattlePokemon(), waterOpponents));
        assertFalse(RedirectAbilityGuard.isMoveRedirected(lifeDew, rotomW.getBattlePokemon(), claydol.getBattlePokemon(), waterOpponents));
        assertFalse(RedirectAbilityGuard.isMoveRedirected(charge, rotomW.getBattlePokemon(), claydol.getBattlePokemon(), electricOpponents));
    }

    @Test
    @DisplayName("CASE 8: Suppressed redirector (Neutralizing Gas) does NOT redirect moves")
    void testSuppressedRedirectorDoesNotRedirect() throws Exception {
        PokemonBattle battle = (PokemonBattle) unsafe.allocateInstance(PokemonBattle.class);

        BattleActor playerActor = (BattleActor) unsafe.allocateInstance(PokemonBattleActor.class);
        actorBattleField.set(playerActor, battle);

        BattleActor npcActor = (BattleActor) unsafe.allocateInstance(PokemonBattleActor.class);
        actorBattleField.set(npcActor, battle);

        BattleSide side1 = (BattleSide) unsafe.allocateInstance(BattleSide.class);
        sideActorsField.set(side1, new BattleActor[]{ playerActor });
        sideBattleField.set(side1, battle);

        BattleSide side2 = (BattleSide) unsafe.allocateInstance(BattleSide.class);
        sideActorsField.set(side2, new BattleActor[]{ npcActor });
        sideBattleField.set(side2, battle);

        battleSide1Field.set(battle, side1);
        battleSide2Field.set(battle, side2);

        ActiveBattlePokemon rotomW = createActivePokemon("levitate", null, playerActor, battle);
        ActiveBattlePokemon claydol = createActivePokemon("levitate", null, npcActor, battle);
        ActiveBattlePokemon gastrodon = createActivePokemon("stormdrain", null, npcActor, battle);
        ActiveBattlePokemon weezing = createActivePokemon("neutralizinggas", null, npcActor, battle);

        actorActivePokemonField.set(playerActor, new ArrayList<>(List.of(rotomW)));
        actorActivePokemonField.set(npcActor, new ArrayList<>(List.of(claydol, gastrodon, weezing)));

        Move hydroPump = createMove("hydropump", MoveTarget.normal, ElementalTypes.WATER);
        List<ActiveBattlePokemon> opponents = List.of(claydol, gastrodon, weezing);

        assertFalse(
            RedirectAbilityGuard.isMoveRedirected(hydroPump, rotomW.getBattlePokemon(), claydol.getBattlePokemon(), opponents),
            "Storm Drain must be suppressed by Neutralizing Gas and NOT redirect Hydro Pump"
        );
    }

    @Test
    @DisplayName("CASE 9: Ability Shield protects redirector from Neutralizing Gas suppression")
    void testAbilityShieldProtectsRedirectorFromSuppression() throws Exception {
        PokemonBattle battle = (PokemonBattle) unsafe.allocateInstance(PokemonBattle.class);

        BattleActor playerActor = (BattleActor) unsafe.allocateInstance(PokemonBattleActor.class);
        actorBattleField.set(playerActor, battle);

        BattleActor npcActor = (BattleActor) unsafe.allocateInstance(PokemonBattleActor.class);
        actorBattleField.set(npcActor, battle);

        BattleSide side1 = (BattleSide) unsafe.allocateInstance(BattleSide.class);
        sideActorsField.set(side1, new BattleActor[]{ playerActor });
        sideBattleField.set(side1, battle);

        BattleSide side2 = (BattleSide) unsafe.allocateInstance(BattleSide.class);
        sideActorsField.set(side2, new BattleActor[]{ npcActor });
        sideBattleField.set(side2, battle);

        battleSide1Field.set(battle, side1);
        battleSide2Field.set(battle, side2);

        ActiveBattlePokemon rotomW = createActivePokemon("levitate", null, playerActor, battle);
        ActiveBattlePokemon claydol = createActivePokemon("levitate", null, npcActor, battle);
        ActiveBattlePokemon gastrodon = createActivePokemon("stormdrain", "abilityshield", npcActor, battle);
        ActiveBattlePokemon weezing = createActivePokemon("neutralizinggas", null, npcActor, battle);

        actorActivePokemonField.set(playerActor, new ArrayList<>(List.of(rotomW)));
        actorActivePokemonField.set(npcActor, new ArrayList<>(List.of(claydol, gastrodon, weezing)));

        Move hydroPump = createMove("hydropump", MoveTarget.normal, ElementalTypes.WATER);
        List<ActiveBattlePokemon> opponents = List.of(claydol, gastrodon, weezing);

        assertTrue(
            RedirectAbilityGuard.isMoveRedirected(hydroPump, rotomW.getBattlePokemon(), claydol.getBattlePokemon(), opponents),
            "Ability Shield on Gastrodon prevents Neutralizing Gas suppression; Storm Drain still redirects"
        );
    }

    @Test
    @DisplayName("CASE 10: Mold Breaker attacker bypasses redirection unless redirector holds Ability Shield")
    void testMoldBreakerAttackerBypassesRedirectionUnlessAbilityShield() throws Exception {
        Move hydroPump = createMove("hydropump", MoveTarget.normal, ElementalTypes.WATER);
        ActiveBattlePokemon moldBreakerAttacker = createActivePokemon("moldbreaker", null);
        ActiveBattlePokemon claydol = createActivePokemon("levitate", null);
        ActiveBattlePokemon gastrodonNoShield = createActivePokemon("stormdrain", null);
        ActiveBattlePokemon gastrodonWithShield = createActivePokemon("stormdrain", "abilityshield");

        // 1. Without Ability Shield -> Mold Breaker pierces Storm Drain
        assertFalse(
            RedirectAbilityGuard.isMoveRedirected(hydroPump, moldBreakerAttacker.getBattlePokemon(), claydol.getBattlePokemon(), List.of(claydol, gastrodonNoShield)),
            "Mold Breaker attacker must pierce Storm Drain redirector without Ability Shield"
        );

        // 2. With Ability Shield -> Ability Shield protects Storm Drain against Mold Breaker
        assertTrue(
            RedirectAbilityGuard.isMoveRedirected(hydroPump, moldBreakerAttacker.getBattlePokemon(), claydol.getBattlePokemon(), List.of(claydol, gastrodonWithShield)),
            "Ability Shield on redirector must protect Storm Drain against Mold Breaker"
        );
    }

    @Test
    @DisplayName("CASE 11: Snipe Shot, Stalwart, and Propeller Tail bypass redirection")
    void testSnipeShotAndStalwartBypassesRedirection() throws Exception {
        Move snipeShot = createMove("snipeshot", MoveTarget.any, ElementalTypes.WATER);
        Move hydroPump = createMove("hydropump", MoveTarget.normal, ElementalTypes.WATER);

        ActiveBattlePokemon normalAttacker = createActivePokemon("levitate", null);
        ActiveBattlePokemon stalwartAttacker = createActivePokemon("stalwart", null);
        ActiveBattlePokemon propellerAttacker = createActivePokemon("propellertail", null);
        ActiveBattlePokemon claydol = createActivePokemon("levitate", null);
        ActiveBattlePokemon gastrodon = createActivePokemon("stormdrain", null);

        List<ActiveBattlePokemon> opponents = List.of(claydol, gastrodon);

        // Snipe Shot bypasses redirection
        assertFalse(
            RedirectAbilityGuard.isMoveRedirected(snipeShot, normalAttacker.getBattlePokemon(), claydol.getBattlePokemon(), opponents),
            "Snipe Shot must ignore redirection"
        );

        // Stalwart bypasses redirection
        assertFalse(
            RedirectAbilityGuard.isMoveRedirected(hydroPump, stalwartAttacker.getBattlePokemon(), claydol.getBattlePokemon(), opponents),
            "Stalwart attacker must ignore redirection"
        );

        // Propeller Tail bypasses redirection
        assertFalse(
            RedirectAbilityGuard.isMoveRedirected(hydroPump, propellerAttacker.getBattlePokemon(), claydol.getBattlePokemon(), opponents),
            "Propeller Tail attacker must ignore redirection"
        );
    }
}
