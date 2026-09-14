package com.cobbleverse.legendaryrule.mechanic;

import com.cobblemon.mod.common.api.battles.model.actor.AIBattleActor;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.battles.*;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.exception.IllegalActionChoiceException;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SideMechanicValidatorTest {

    private static Unsafe unsafe;
    private UUID battleId;

    public static class ConcretePlayerActor extends BattleActor {
        public ConcretePlayerActor(UUID uuid, List<BattlePokemon> pokemonList) {
            super(uuid, pokemonList);
        }

        @NotNull
        @Override
        public MutableText getName() {
            return Text.empty();
        }

        @NotNull
        @Override
        public MutableText nameOwned(@NotNull String s) {
            return Text.empty();
        }

        @NotNull
        @Override
        public com.cobblemon.mod.common.api.battles.model.actor.ActorType getType() {
            return com.cobblemon.mod.common.api.battles.model.actor.ActorType.PLAYER;
        }
    }

    public static class ConcreteAIActor extends AIBattleActor {
        public ConcreteAIActor(UUID uuid, List<BattlePokemon> pokemonList) {
            super(uuid, pokemonList, null);
        }

        @NotNull
        @Override
        public MutableText getName() {
            return Text.empty();
        }

        @NotNull
        @Override
        public MutableText nameOwned(@NotNull String s) {
            return Text.empty();
        }

        @NotNull
        @Override
        public com.cobblemon.mod.common.api.battles.model.actor.ActorType getType() {
            return com.cobblemon.mod.common.api.battles.model.actor.ActorType.NPC;
        }
    }

    @BeforeAll
    static void initUnsafe() throws Exception {
        Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        unsafe = (Unsafe) f.get(null);
    }

    @BeforeEach
    void setUp() {
        battleId = UUID.randomUUID();
        BattleSideMechanicTracker.clearBattle(battleId);
    }

    @AfterEach
    void tearDown() {
        BattleSideMechanicTracker.clearBattle(battleId);
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Class<?> clazz = target.getClass();
        Field field = null;
        while (clazz != null) {
            try {
                field = clazz.getDeclaredField(fieldName);
                break;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        if (field != null) {
            field.setAccessible(true);
            field.set(target, value);
        }
    }

    private InBattleMove createMove(String id, MoveTarget target) throws Exception {
        InBattleMove move = (InBattleMove) unsafe.allocateInstance(InBattleMove.class);
        setField(move, "id", id);
        setField(move, "target", target);
        setField(move, "pp", 10);
        setField(move, "maxpp", 10);
        setField(move, "disabled", false);
        return move;
    }

    private ShowdownMoveset createMoveset(List<InBattleMove> moves) throws Exception {
        ShowdownMoveset moveset = (ShowdownMoveset) unsafe.allocateInstance(ShowdownMoveset.class);
        setField(moveset, "moves", moves);
        return moveset;
    }

    private BattleActor createPlayerActor(ShowdownActionRequest request, List<ActiveBattlePokemon> activeMons) throws Exception {
        ConcretePlayerActor actor = (ConcretePlayerActor) unsafe.allocateInstance(ConcretePlayerActor.class);
        setField(actor, "request", request);
        setField(actor, "activePokemon", activeMons != null ? activeMons : new ArrayList<>());
        return actor;
    }

    private AIBattleActor createAIActor(ShowdownActionRequest request, List<ActiveBattlePokemon> activeMons) throws Exception {
        ConcreteAIActor actor = (ConcreteAIActor) unsafe.allocateInstance(ConcreteAIActor.class);
        setField(actor, "request", request);
        setField(actor, "activePokemon", activeMons != null ? activeMons : new ArrayList<>());
        return actor;
    }

    @Test
    @DisplayName("Player requesting conflicting gimmicks in same-turn Doubles batch throws IllegalActionChoiceException")
    void testPlayerConflictingGimmicksInBatch() throws Exception {
        ShowdownActionRequest request = (ShowdownActionRequest) unsafe.allocateInstance(ShowdownActionRequest.class);
        BattleActor player = createPlayerActor(request, null);

        MoveActionResponse slot1 = new MoveActionResponse("flamethrower", "p2a", "mega");
        MoveActionResponse slot2 = new MoveActionResponse("surf", null, "terastallize");
        List<ShowdownActionResponse> responses = List.of(slot1, slot2);

        assertThrows(IllegalActionChoiceException.class, () ->
                SideMechanicValidator.validateAndSanitizeResponses(player, battleId, 0, responses)
        );
    }

    @Test
    @DisplayName("Player requesting duplicate gimmicks (Mega + Mega) in same-turn Doubles batch throws IllegalActionChoiceException")
    void testPlayerDuplicateGimmickInBatchRegression() throws Exception {
        ShowdownActionRequest request = (ShowdownActionRequest) unsafe.allocateInstance(ShowdownActionRequest.class);
        BattleActor player = createPlayerActor(request, null);

        MoveActionResponse slot1 = new MoveActionResponse("flamethrower", "p2a", "mega");
        MoveActionResponse slot2 = new MoveActionResponse("earthquake", null, "mega");
        List<ShowdownActionResponse> responses = List.of(slot1, slot2);

        assertThrows(IllegalActionChoiceException.class, () ->
                SideMechanicValidator.validateAndSanitizeResponses(player, battleId, 0, responses)
        );
    }

    @Test
    @DisplayName("AI requesting conflicting gimmicks keeps first gimmick and demotes second to base move")
    void testAIConflictingGimmicksInBatch() throws Exception {
        InBattleMove thunderbolt = createMove("thunderbolt", MoveTarget.normal);
        ShowdownMoveset moveset1 = createMoveset(List.of(thunderbolt));

        InBattleMove flamethrower = createMove("flamethrower", MoveTarget.normal);
        ShowdownMoveset moveset2 = createMoveset(List.of(flamethrower));

        ShowdownActionRequest request = (ShowdownActionRequest) unsafe.allocateInstance(ShowdownActionRequest.class);
        setField(request, "active", List.of(moveset1, moveset2));

        AIBattleActor ai = createAIActor(request, null);

        MoveActionResponse slot1 = new MoveActionResponse("thunderbolt", "p2a", "mega");
        MoveActionResponse slot2 = new MoveActionResponse("flamethrower", "p2b", "terastallize");
        List<ShowdownActionResponse> responses = List.of(slot1, slot2);

        assertDoesNotThrow(() ->
                SideMechanicValidator.validateAndSanitizeResponses(ai, battleId, 0, responses)
        );

        // Slot 1 retains mega gimmick
        assertEquals("mega", slot1.getGimmickID());
        assertEquals("thunderbolt", slot1.getMoveName());

        // Slot 2 demoted: gimmick cleared, base move preserved
        assertNull(slot2.getGimmickID());
        assertEquals("flamethrower", slot2.getMoveName());
    }

    @Test
    @DisplayName("AI requesting duplicate gimmicks (Mega + Mega) keeps first and demotes second")
    void testAIDuplicateGimmickInBatchRegression() throws Exception {
        InBattleMove thunderbolt = createMove("thunderbolt", MoveTarget.normal);
        ShowdownMoveset moveset1 = createMoveset(List.of(thunderbolt));

        InBattleMove flamethrower = createMove("flamethrower", MoveTarget.normal);
        ShowdownMoveset moveset2 = createMoveset(List.of(flamethrower));

        ShowdownActionRequest request = (ShowdownActionRequest) unsafe.allocateInstance(ShowdownActionRequest.class);
        setField(request, "active", List.of(moveset1, moveset2));

        AIBattleActor ai = createAIActor(request, null);

        MoveActionResponse slot1 = new MoveActionResponse("thunderbolt", "p2a", "mega");
        MoveActionResponse slot2 = new MoveActionResponse("flamethrower", "p2b", "mega");
        List<ShowdownActionResponse> responses = List.of(slot1, slot2);

        assertDoesNotThrow(() ->
                SideMechanicValidator.validateAndSanitizeResponses(ai, battleId, 0, responses)
        );

        assertEquals("mega", slot1.getGimmickID());
        assertNull(slot2.getGimmickID());
        assertEquals("flamethrower", slot2.getMoveName());
    }

    @Test
    @DisplayName("Already-locked side rejects player gimmick and demotes AI gimmick")
    void testAlreadyLockedSide() throws Exception {
        BattleSideMechanicTracker.commitMechanic(battleId, 0, MajorBattleMechanic.MEGA);

        ShowdownActionRequest request = (ShowdownActionRequest) unsafe.allocateInstance(ShowdownActionRequest.class);
        InBattleMove move = createMove("surf", MoveTarget.allAdjacentFoes);
        ShowdownMoveset moveset = createMoveset(List.of(move));
        setField(request, "active", List.of(moveset));

        // Player: throws
        BattleActor player = createPlayerActor(request, null);
        MoveActionResponse playerMove = new MoveActionResponse("surf", null, "terastallize");
        assertThrows(IllegalActionChoiceException.class, () ->
                SideMechanicValidator.validateAndSanitizeResponses(player, battleId, 0, List.of(playerMove))
        );

        // AI: demotes
        AIBattleActor ai = createAIActor(request, null);
        MoveActionResponse aiMove = new MoveActionResponse("surf", "p2a", "dynamax");
        assertDoesNotThrow(() ->
                SideMechanicValidator.validateAndSanitizeResponses(ai, battleId, 0, List.of(aiMove))
        );
        assertNull(aiMove.getGimmickID());
        assertEquals("surf", aiMove.getMoveName());
        assertNull(aiMove.getTargetPnx()); // Spread move target cleared
    }

    @Test
    @DisplayName("Target normalization: Spread move demoted clears single-target pnx")
    void testDemoteSpreadMoveClearsTargetPnx() throws Exception {
        InBattleMove rockSlide = createMove("rockslide", MoveTarget.allAdjacentFoes);
        ShowdownMoveset moveset = createMoveset(List.of(rockSlide));

        MoveActionResponse moveResp = new MoveActionResponse("rockslide", "p2a", "dynamax");
        SideMechanicValidator.demoteToSafeBaseMove(moveResp, null, moveset);

        assertNull(moveResp.getGimmickID());
        assertEquals("rockslide", moveResp.getMoveName());
        assertNull(moveResp.getTargetPnx());
    }

    @Test
    @DisplayName("Target normalization: Targeted move preserves valid target pnx")
    void testDemoteTargetedMovePreservesValidTargetPnx() throws Exception {
        InBattleMove thunderbolt = createMove("thunderbolt", MoveTarget.normal);
        ShowdownMoveset moveset = createMoveset(List.of(thunderbolt));

        MoveActionResponse moveResp = new MoveActionResponse("thunderbolt", "p2a", "dynamax");
        SideMechanicValidator.demoteToSafeBaseMove(moveResp, null, moveset);

        assertNull(moveResp.getGimmickID());
        assertEquals("thunderbolt", moveResp.getMoveName());
        assertEquals("p2a", moveResp.getTargetPnx());
    }
}
