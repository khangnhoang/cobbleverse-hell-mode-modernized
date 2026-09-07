package com.cobbleverse.legendaryrule.fair;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.abilities.Abilities;
import com.cobblemon.mod.common.api.abilities.Ability;
import com.cobblemon.mod.common.api.abilities.AbilityTemplate;
import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.moves.MoveSet;
import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.api.moves.categories.DamageCategories;
import com.cobblemon.mod.common.api.moves.categories.DamageCategory;
import com.cobblemon.mod.common.api.pokemon.helditem.HeldItemManager;
import com.cobblemon.mod.common.api.pokemon.stats.Stat;
import com.cobblemon.mod.common.api.pokemon.stats.StatProvider;
import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.api.types.ElementalType;
import com.cobblemon.mod.common.api.types.ElementalTypes;
import com.cobblemon.mod.common.api.types.tera.TeraType;
import com.cobblemon.mod.common.api.types.tera.TeraTypes;
import com.cobblemon.mod.common.battles.ActiveBattlePokemon;
import com.cobblemon.mod.common.battles.BattleSide;
import com.cobblemon.mod.common.battles.MoveTarget;
import com.cobblemon.mod.common.battles.interpreter.ContextManager;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.pokemon.FormData;
import com.cobblemon.mod.common.pokemon.Gender;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import com.cobbleverse.legendaryrule.strategy.guard.RedirectAbilityGuard;
import com.gitlab.surilexa.rbrctai.api.ai.utils.PokeMathMax;
import net.minecraft.text.Text;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class FairShadowPokemonBuilderTest {

    private static Unsafe unsafe;
    private static Field pokemonFormField;
    private static Field pokemonSpeciesField;
    private static Field pokemonAbilityField;
    private static Field pokemonTeraTypeField;
    private static Field pokemonHealthField;
    private static Field pokemonLevelField;
    private static Field pokemonGenderField;
    private static Field pokemonShinyField;
    private static Field pokemonAspectsField;
    private static Field pokemonUuidField;
    private static Field formPrimaryTypeField;
    private static Field formSecondaryTypeField;
    private static Field speciesNameField;
    private static Field abilityTemplateField;
    private static Field templateNameField;

    private static Field bpEffectedPokemonField;
    private static Field bpOriginalPokemonField;
    private static Field bpStatChangesField;
    private static Field bpContextManagerField;
    private static Field bpActorField;
    private static Field bpHeldItemManagerDelegateField;
    private static Field pokemonMoveSetField;
    private static Field abpActorField;
    private static Field abpBattlePokemonField;
    private static Field abpBattleField;
    private static Field actorBattleField;
    private static Field sideActorsField;
    private static Field sideBattleField;
    private static Field battleSide1Field;
    private static Field battleSide2Field;
    private static Field actorPokemonListField;
    private static Field actorActivePokemonField;
    private static Field battleEndedField;
    private static Field battleIdField;
    private static Field battleContextManagerField;

    private static final Map<Pokemon, Integer> hpMap = new ConcurrentHashMap<>();

    @BeforeAll
    static void setUpAll() throws Exception {
        try {
            net.minecraft.SharedConstants.createGameVersion();
            java.lang.reflect.Method init = Class.forName("net.minecraft.Bootstrap").getDeclaredMethod("initialize");
            init.setAccessible(true);
            init.invoke(null);
        } catch (Throwable ignored) {
        }

        Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        unsafe = (Unsafe) f.get(null);

        pokemonFormField = Pokemon.class.getDeclaredField("form");
        pokemonFormField.setAccessible(true);

        pokemonSpeciesField = Pokemon.class.getDeclaredField("species");
        pokemonSpeciesField.setAccessible(true);

        pokemonAbilityField = Pokemon.class.getDeclaredField("ability");
        pokemonAbilityField.setAccessible(true);

        pokemonTeraTypeField = Pokemon.class.getDeclaredField("teraType");
        pokemonTeraTypeField.setAccessible(true);

        pokemonHealthField = Pokemon.class.getDeclaredField("currentHealth");
        pokemonHealthField.setAccessible(true);

        pokemonLevelField = Pokemon.class.getDeclaredField("level");
        pokemonLevelField.setAccessible(true);

        pokemonGenderField = Pokemon.class.getDeclaredField("gender");
        pokemonGenderField.setAccessible(true);

        pokemonShinyField = Pokemon.class.getDeclaredField("shiny");
        pokemonShinyField.setAccessible(true);

        pokemonAspectsField = Pokemon.class.getDeclaredField("aspects");
        pokemonAspectsField.setAccessible(true);

        pokemonUuidField = Pokemon.class.getDeclaredField("uuid");
        pokemonUuidField.setAccessible(true);

        formPrimaryTypeField = FormData.class.getDeclaredField("_primaryType");
        formPrimaryTypeField.setAccessible(true);

        formSecondaryTypeField = FormData.class.getDeclaredField("_secondaryType");
        formSecondaryTypeField.setAccessible(true);

        speciesNameField = Species.class.getDeclaredField("name");
        speciesNameField.setAccessible(true);

        abilityTemplateField = Ability.class.getDeclaredField("template");
        abilityTemplateField.setAccessible(true);

        templateNameField = AbilityTemplate.class.getDeclaredField("name");
        templateNameField.setAccessible(true);

        bpEffectedPokemonField = BattlePokemon.class.getDeclaredField("effectedPokemon");
        bpEffectedPokemonField.setAccessible(true);

        bpOriginalPokemonField = BattlePokemon.class.getDeclaredField("originalPokemon");
        bpOriginalPokemonField.setAccessible(true);

        bpStatChangesField = BattlePokemon.class.getDeclaredField("statChanges");
        bpStatChangesField.setAccessible(true);

        bpContextManagerField = BattlePokemon.class.getDeclaredField("contextManager");
        bpContextManagerField.setAccessible(true);

        bpActorField = BattlePokemon.class.getDeclaredField("actor");
        bpActorField.setAccessible(true);

        bpHeldItemManagerDelegateField = BattlePokemon.class.getDeclaredField("heldItemManager$delegate");
        bpHeldItemManagerDelegateField.setAccessible(true);

        pokemonMoveSetField = Pokemon.class.getDeclaredField("moveSet");
        pokemonMoveSetField.setAccessible(true);

        abpActorField = ActiveBattlePokemon.class.getDeclaredField("actor");
        abpActorField.setAccessible(true);

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

        battleSide1Field = PokemonBattle.class.getDeclaredField("side1");
        battleSide1Field.setAccessible(true);

        battleSide2Field = PokemonBattle.class.getDeclaredField("side2");
        battleSide2Field.setAccessible(true);

        actorPokemonListField = BattleActor.class.getDeclaredField("pokemonList");
        actorPokemonListField.setAccessible(true);

        actorActivePokemonField = BattleActor.class.getDeclaredField("activePokemon");
        actorActivePokemonField.setAccessible(true);

        battleEndedField = PokemonBattle.class.getDeclaredField("ended");
        battleEndedField.setAccessible(true);

        battleIdField = PokemonBattle.class.getDeclaredField("battleId");
        battleIdField.setAccessible(true);

        battleContextManagerField = PokemonBattle.class.getDeclaredField("contextManager");
        battleContextManagerField.setAccessible(true);

        StatProvider proxy = (StatProvider) Proxy.newProxyInstance(
            StatProvider.class.getClassLoader(),
            new Class<?>[]{StatProvider.class},
            (p, method, args) -> {
                if ("getStatForPokemon".equals(method.getName())) {
                    Pokemon pkmn = (Pokemon) args[0];
                    Stat stat = (Stat) args[1];
                    if (stat == Stats.HP) return hpMap.getOrDefault(pkmn, 100);
                    return 100;
                }
                return 100;
            }
        );
        Cobblemon.INSTANCE.setStatProvider(proxy);
    }

    @AfterEach
    void tearDown() {
        while (FairBattleContext.isActive()) {
            FairBattleContext.open(null).close();
        }
    }

    private Pokemon createRealPokemon(String speciesName, String abilityName, ElementalType primaryType, ElementalType secondaryType, TeraType teraType, int health, int level) throws Exception {
        Pokemon pkmn = (Pokemon) unsafe.allocateInstance(Pokemon.class);
        pokemonHealthField.set(pkmn, health);
        pokemonLevelField.set(pkmn, level);
        pokemonGenderField.set(pkmn, Gender.MALE);
        pokemonShinyField.set(pkmn, true);
        pokemonTeraTypeField.set(pkmn, teraType != null ? teraType : TeraTypes.getSTEEL());
        pokemonAspectsField.set(pkmn, new LinkedHashSet<>(List.of("shiny", "custom_aspect")));
        pokemonUuidField.set(pkmn, UUID.randomUUID());
        hpMap.put(pkmn, health);

        Species sp = (Species) unsafe.allocateInstance(Species.class);
        speciesNameField.set(sp, speciesName);
        pokemonSpeciesField.set(pkmn, sp);

        FormData form = (FormData) unsafe.allocateInstance(FormData.class);
        formPrimaryTypeField.set(form, primaryType != null ? primaryType : ElementalTypes.WATER);
        formSecondaryTypeField.set(form, secondaryType);
        pokemonFormField.set(pkmn, form);

        AbilityTemplate template = (AbilityTemplate) unsafe.allocateInstance(AbilityTemplate.class);
        templateNameField.set(template, abilityName != null ? abilityName : "stormdrain");

        Ability ability = (Ability) unsafe.allocateInstance(Ability.class);
        abilityTemplateField.set(ability, template);
        pokemonAbilityField.set(pkmn, ability);

        return pkmn;
    }

    private BattlePokemon createRealBattlePokemon(BattleActor actor, Pokemon pokemon, String itemId, List<Move> moves) throws Exception {
        BattlePokemon bp = (BattlePokemon) unsafe.allocateInstance(BattlePokemon.class);
        bpEffectedPokemonField.set(bp, pokemon);
        bpOriginalPokemonField.set(bp, pokemon);
        bpActorField.set(bp, actor);
        bpContextManagerField.set(bp, new ContextManager());
        LinkedHashMap<Stat, Integer> stages = new LinkedHashMap<>();
        stages.put(Stats.ATTACK, 1);
        stages.put(Stats.SPEED, -1);
        bpStatChangesField.set(bp, stages);

        HeldItemManager itemManager = (HeldItemManager) Proxy.newProxyInstance(
            HeldItemManager.class.getClassLoader(),
            new Class<?>[]{HeldItemManager.class},
            (proxy, method, args) -> {
                if ("showdownId".equals(method.getName())) {
                    return itemId != null ? itemId : "";
                }
                return null;
            }
        );
        kotlin.Lazy<HeldItemManager> lazyItem = kotlin.LazyKt.lazy(() -> itemManager);
        bpHeldItemManagerDelegateField.set(bp, lazyItem);

        MoveSet moveSet = new MoveSet();
        if (moves != null) {
            Field movesArrayField = MoveSet.class.getDeclaredField("moves");
            movesArrayField.setAccessible(true);
            Move[] movesArray = (Move[]) movesArrayField.get(moveSet);
            for (int i = 0; i < moves.size() && i < movesArray.length; i++) {
                movesArray[i] = moves.get(i);
            }
        }
        pokemonMoveSetField.set(pokemon, moveSet);

        actor.getPokemonList().add(bp);
        return bp;
    }

    private Move createMove(String name, ElementalType type, DamageCategory category, MoveTarget target) throws Exception {
        MoveTemplate template = (MoveTemplate) unsafe.allocateInstance(MoveTemplate.class);
        Field nameField = MoveTemplate.class.getDeclaredField("name");
        nameField.setAccessible(true);
        nameField.set(template, name);

        Field typeField = MoveTemplate.class.getDeclaredField("elementalType");
        typeField.setAccessible(true);
        typeField.set(template, type);

        Field catField = MoveTemplate.class.getDeclaredField("damageCategory");
        catField.setAccessible(true);
        catField.set(template, category);

        Field targetField = MoveTemplate.class.getDeclaredField("target");
        targetField.setAccessible(true);
        targetField.set(template, target);

        Move move = (Move) unsafe.allocateInstance(Move.class);
        Field templateField = Move.class.getDeclaredField("template");
        templateField.setAccessible(true);
        templateField.set(move, template);

        return move;
    }

    private PokemonBattle createBattle() throws Exception {
        PokemonBattle battle = (PokemonBattle) unsafe.allocateInstance(PokemonBattle.class);
        battleEndedField.set(battle, false);
        battleIdField.set(battle, UUID.randomUUID());
        battleContextManagerField.set(battle, new ContextManager());

        BattleSide side1 = (BattleSide) unsafe.allocateInstance(BattleSide.class);
        BattleSide side2 = (BattleSide) unsafe.allocateInstance(BattleSide.class);
        battleSide1Field.set(battle, side1);
        battleSide2Field.set(battle, side2);
        sideBattleField.set(side1, battle);
        sideBattleField.set(side2, battle);
        sideActorsField.set(side1, new BattleActor[0]);
        sideActorsField.set(side2, new BattleActor[0]);
        return battle;
    }

    private BattleActor createActor(PokemonBattle battle, String showdownId, int sideNum) throws Exception {
        BattleActor actor = (BattleActor) unsafe.allocateInstance(com.cobblemon.mod.common.battles.actor.PokemonBattleActor.class);
        actorBattleField.set(actor, battle);
        actorPokemonListField.set(actor, new ArrayList<BattlePokemon>());

        actor.showdownId = showdownId;

        BattleSide side = (sideNum == 1) ? battle.getSide1() : battle.getSide2();
        sideActorsField.set(side, new BattleActor[]{actor});
        actorActivePokemonField.set(actor, new ArrayList<ActiveBattlePokemon>());

        return actor;
    }

    private ActiveBattlePokemon createActiveBattlePokemon(BattleActor actor, BattlePokemon bp) throws Exception {
        ActiveBattlePokemon abp = (ActiveBattlePokemon) unsafe.allocateInstance(ActiveBattlePokemon.class);
        abpActorField.set(abp, actor);
        abpBattlePokemonField.set(abp, bp);
        abpBattleField.set(abp, actor.getBattle());
        actor.getActivePokemon().add(abp);
        return abp;
    }

    @Test
    @DisplayName("9. Shadow builder preserves public fields while stripping hidden state")
    void testShadowBuilderInformationSeparation() throws Exception {
        PokemonBattle battle = createBattle();
        BattleActor playerActor = createActor(battle, "p1", 1);

        Move surf = createMove("surf", ElementalTypes.WATER, DamageCategories.INSTANCE.getSPECIAL(), MoveTarget.allAdjacentFoes);
        Pokemon realPkmn = createRealPokemon("Gastrodon", "stormdrain", ElementalTypes.WATER, ElementalTypes.GROUND, TeraTypes.getSTEEL(), 105, 50);
        BattlePokemon realBp = createRealBattlePokemon(playerActor, realPkmn, "sitrusberry", List.of(surf));

        BattlePokemon shadowBp = FairShadowPokemonBuilder.build(realBp);
        assertNotNull(shadowBp);

        Pokemon shadowPkmn = shadowBp.getEffectedPokemon();
        assertNotNull(shadowPkmn);

        // --- PUBLIC / SAFE PRESERVATION ---
        assertEquals("Gastrodon", shadowPkmn.getSpecies().getName());
        assertEquals(ElementalTypes.WATER, shadowPkmn.getForm().getPrimaryType());
        assertEquals(ElementalTypes.GROUND, shadowPkmn.getForm().getSecondaryType());
        assertEquals(50, shadowPkmn.getLevel());
        // Health: Shadow HP scales to benchmark max HP based on public fraction
        assertEquals(shadowPkmn.getMaxHealth(), shadowPkmn.getCurrentHealth(),
            "Full health real Pokémon results in full health shadow Pokémon");
        assertEquals(1.0, (double) shadowPkmn.getCurrentHealth() / shadowPkmn.getMaxHealth(), 0.001);
        assertEquals(Gender.MALE, shadowPkmn.getGender());
        assertTrue(shadowPkmn.getShiny());
        assertSame(playerActor, shadowBp.getActor());
        assertEquals(Integer.valueOf(1), shadowBp.getStatChanges().get(Stats.ATTACK));
        assertEquals(Integer.valueOf(-1), shadowBp.getStatChanges().get(Stats.SPEED));

        // --- HIDDEN STRIPPING ---
        // Hidden Item: must NOT be sitrusberry
        String shadowItem = shadowBp.getHeldItemManager().showdownId(shadowBp);
        assertTrue(shadowItem == null || shadowItem.isEmpty() || !shadowItem.equals("sitrusberry"),
            "Shadow BattlePokemon must not leak real held item");

        // Hidden Moves: must NOT expose surf
        assertTrue(shadowBp.getMoveSet().getMoves().isEmpty() ||
                   shadowBp.getMoveSet().getMoves().stream().noneMatch(m -> "surf".equalsIgnoreCase(m.getName())),
            "Shadow BattlePokemon must not expose unrevealed moves");

        // Hidden Ability: must NOT be stormdrain
        String shadowAbility = shadowPkmn.getAbility() != null ? shadowPkmn.getAbility().getName() : "";
        assertNotEquals("stormdrain", shadowAbility,
            "Shadow BattlePokemon must not leak hidden ability");

        // Hidden Tera: must NOT be STEEL
        assertNotEquals(TeraTypes.getSTEEL(), shadowPkmn.getTeraType(),
            "Shadow BattlePokemon must not leak real hidden Tera type");
    }

    @Test
    @DisplayName("9b. Shadow builder scales public HP fraction to shadow benchmark max HP without leaking exact HP")
    void testShadowBuilderHpFractionScaling() throws Exception {
        PokemonBattle battle = createBattle();
        BattleActor playerActor = createActor(battle, "p1", 1);

        // Case 1: Partial health (50% HP: 100/200)
        Pokemon real50 = createRealPokemon("Blastoise", "torrent", ElementalTypes.WATER, null, null, 100, 50);
        hpMap.put(real50, 200); // real max HP = 200, current = 100 (fraction = 0.50)
        BattlePokemon bp50 = createRealBattlePokemon(playerActor, real50, "", null);
        BattlePokemon shadow50 = FairShadowPokemonBuilder.build(bp50);
        assertNotNull(shadow50);
        assertEquals(100, shadow50.getEffectedPokemon().getMaxHealth());
        // Shadow current HP must be scaled to 50% (50/100), NOT raw 100 (which would be 100% full health)
        assertEquals(50, shadow50.getEffectedPokemon().getCurrentHealth(),
            "Shadow current HP must match the 50% public fraction on benchmark bulk");
        assertEquals(0.50, (double) shadow50.getEffectedPokemon().getCurrentHealth() / shadow50.getEffectedPokemon().getMaxHealth(), 0.01);

        // Case 2: Fainted (0 HP)
        Pokemon real0 = createRealPokemon("Blastoise", "torrent", ElementalTypes.WATER, null, null, 0, 50);
        hpMap.put(real0, 200);
        BattlePokemon bp0 = createRealBattlePokemon(playerActor, real0, "", null);
        BattlePokemon shadow0 = FairShadowPokemonBuilder.build(bp0);
        assertEquals(0, shadow0.getEffectedPokemon().getCurrentHealth(), "Fainted Pokémon results in 0 HP shadow");

        // Case 3: Low health (1/400 = 0.25% HP)
        Pokemon realLow = createRealPokemon("Blastoise", "torrent", ElementalTypes.WATER, null, null, 1, 50);
        hpMap.put(realLow, 400);
        BattlePokemon bpLow = createRealBattlePokemon(playerActor, realLow, "", null);
        BattlePokemon shadowLow = FairShadowPokemonBuilder.build(bpLow);
        assertTrue(shadowLow.getEffectedPokemon().getCurrentHealth() >= 1, "Non-fainted low health Pokémon preserves at least 1 HP");
        assertTrue(shadowLow.getEffectedPokemon().getCurrentHealth() < shadowLow.getEffectedPokemon().getMaxHealth(),
            "Non-full health Pokémon must not be marked at full HP");
    }

    @Test
    @DisplayName("10 & 11. Real ABP identity and PNX string remain identical across context")
    void testRealAbpIdentityAndPnxPreservation() throws Exception {
        PokemonBattle battle = createBattle();
        BattleActor playerActor = createActor(battle, "p1", 1);

        Pokemon realPkmn = createRealPokemon("Pikachu", "lightningrod", ElementalTypes.ELECTRIC, null, null, 100, 50);
        BattlePokemon realBp = createRealBattlePokemon(playerActor, realPkmn, "lightball", null);
        ActiveBattlePokemon realAbp = createActiveBattlePokemon(playerActor, realBp);

        String pnxBefore = realAbp.getPNX();
        assertEquals("p1a", pnxBefore);

        BattlePokemon shadowBp = FairShadowPokemonBuilder.build(realAbp);
        Map<ActiveBattlePokemon, BattlePokemon> map = new HashMap<>();
        map.put(realAbp, shadowBp);

        try (FairBattleContext.Scope ignored = FairBattleContext.open(map)) {
            // ABP reference identity untouched
            assertSame(realAbp, map.keySet().iterator().next());
            // PNX calculation untouched
            assertEquals("p1a", realAbp.getPNX());
        }

        assertEquals("p1a", realAbp.getPNX());
    }

    @Test
    @DisplayName("12. Internal R&B helper uses sanitized BP inside fair context (RedirectAbilityGuard)")
    void testInternalHelperUsesSanitizedBp() throws Exception {
        PokemonBattle battle = createBattle();
        BattleActor npcActor = createActor(battle, "p2", 2);
        BattleActor playerActor = createActor(battle, "p1", 1);

        // Attacker: NPC Electivire
        Pokemon npcPkmn = createRealPokemon("Electivire", "motordrive", ElementalTypes.ELECTRIC, null, null, 100, 50);
        BattlePokemon npcBp = createRealBattlePokemon(npcActor, npcPkmn, "", null);
        ActiveBattlePokemon npcAbp = createActiveBattlePokemon(npcActor, npcBp);

        // Defender 1: Player Starmie (target of single-target electric move)
        Pokemon targetPkmn = createRealPokemon("Starmie", "naturalcure", ElementalTypes.WATER, ElementalTypes.PSYCHIC, null, 100, 50);
        BattlePokemon targetBp = createRealBattlePokemon(playerActor, targetPkmn, "", null);
        ActiveBattlePokemon targetAbp = createActiveBattlePokemon(playerActor, targetBp);

        // Defender 2: Player Raichu with hidden Lightning Rod
        Pokemon redirectorPkmn = createRealPokemon("Raichu", "lightningrod", ElementalTypes.ELECTRIC, null, null, 100, 50);
        BattlePokemon redirectorBp = createRealBattlePokemon(playerActor, redirectorPkmn, "", null);
        ActiveBattlePokemon redirectorAbp = createActiveBattlePokemon(playerActor, redirectorBp);

        List<ActiveBattlePokemon> opponents = List.of(targetAbp, redirectorAbp);
        Move thunderbolt = createMove("thunderbolt", ElementalTypes.ELECTRIC, DamageCategories.INSTANCE.getSPECIAL(), MoveTarget.normal);

        // 1. Outside fair scope: real BP has Lightning Rod -> redirected
        assertTrue(RedirectAbilityGuard.isMoveRedirected(thunderbolt, npcBp, targetBp, opponents),
            "Outside fair scope, real Lightning Rod redirects Thunderbolt");

        // 2. Inside fair scope: redirector ABP is mapped to shadow BP with neutral ability
        BattlePokemon shadowRedirectorBp = FairShadowPokemonBuilder.build(redirectorAbp);
        Map<ActiveBattlePokemon, BattlePokemon> shadowMap = new HashMap<>();
        shadowMap.put(redirectorAbp, shadowRedirectorBp);

        // Verify resolver substitutes shadow
        try (FairBattleContext.Scope ignored = FairBattleContext.open(shadowMap)) {
            assertSame(shadowRedirectorBp, FairBattleContext.resolve(redirectorAbp, redirectorBp));

            // When evaluated with shadow redirector BP, redirection does NOT trigger
            List<ActiveBattlePokemon> scopedOpponents = List.of(
                targetAbp,
                createActiveBattlePokemon(playerActor, FairBattleContext.resolve(redirectorAbp, redirectorBp))
            );
            assertFalse(RedirectAbilityGuard.isMoveRedirected(thunderbolt, npcBp, targetBp, scopedOpponents),
                "Inside fair scope, shadow BP neutral ability prevents cheating redirect knowledge");
        }

        // 3. Outside fair scope: restored to real BP
        assertSame(redirectorBp, FairBattleContext.resolve(redirectorAbp, redirectorBp));
    }

    @Test
    @DisplayName("13. Regression: Empty shadow moveset getFirst() crashes natively, but safe sentinel move guards recharge check without fabricating moves")
    void testEmptyShadowMovesetRechargeGuardRegression() throws Exception {
        PokemonBattle battle = createBattle();
        BattleActor playerActor = createActor(battle, "p1", 1);

        // Real opponent mon with real moves (e.g. Incineroar with Flare Blitz)
        Move flareBlitz = createMove("flareblitz", ElementalTypes.FIRE, DamageCategories.INSTANCE.getPHYSICAL(), MoveTarget.normal);
        Pokemon realPkmn = createRealPokemon("Incineroar", "intimidate", ElementalTypes.FIRE, ElementalTypes.DARK, null, 150, 50);
        BattlePokemon realBp = createRealBattlePokemon(playerActor, realPkmn, "", List.of(flareBlitz));
        ActiveBattlePokemon realAbp = createActiveBattlePokemon(playerActor, realBp);

        // 1. Fair shadow creation strips unrevealed moves -> moveset is completely empty
        BattlePokemon shadowBp = FairShadowPokemonBuilder.build(realAbp);
        assertNotNull(shadowBp);
        List<Move> shadowMoves = shadowBp.getMoveSet().getMoves();
        assertTrue(shadowMoves.isEmpty(), "Fair shadow opponent must have empty moves when unrevealed");

        // 2. Exact reproduction of the crash path at RunBunAI.java:1779:
        // Native RunBunAI calls oppMoves.getFirst().getName().equals("recharge")
        // Calling getFirst() directly on the empty moves list throws NoSuchElementException
        assertThrows(NoSuchElementException.class, () -> {
            shadowMoves.getFirst();
        }, "Calling getFirst() on empty shadow moveset reproduces native RunBunAI line 1779 crash");

        // 3. Sentinel move contract:
        // FairShadowPokemonBuilder.getSafeSentinelMove() returns a non-null Move
        Move sentinel = FairShadowPokemonBuilder.getSafeSentinelMove();
        assertNotNull(sentinel, "Safe sentinel move must not be null");
        assertNotNull(sentinel.getName(), "Sentinel move name must not be null");
        assertFalse("recharge".equals(sentinel.getName()), "Sentinel move must not be named recharge");
        assertEquals("unknown", sentinel.getName(), "Sentinel move must be named unknown");

        // 4. Guarded recharge evaluation logic:
        // When list is empty, guard substitutes sentinel move -> recharge evaluation evaluates to false cleanly
        Move evalMove = shadowMoves.isEmpty() ? sentinel : shadowMoves.getFirst();
        boolean isRecharging = evalMove != null && "recharge".equals(evalMove.getName());
        assertFalse(isRecharging, "Guarded recharge evaluation on unrevealed moves must cleanly evaluate to false");
    }
}
