package com.cobbleverse.legendaryrule.strategy.spread;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.battles.ActiveBattlePokemon;
import com.cobblemon.mod.common.battles.BattleSide;
import com.cobblemon.mod.common.battles.MoveTarget;
import com.cobblemon.mod.common.battles.ShowdownActionResponse;
import com.cobblemon.mod.common.battles.ShowdownMoveset;
import com.gitlab.surilexa.rbrctai.api.ai.RunBunAI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;

class RunBunAICallSiteContractTest {

    @Test
    @DisplayName("Runtime contract: MoveTarget enum constants allAdjacentFoes and allAdjacent exist")
    void testMoveTargetEnumConstantsExist() {
        assertNotNull(MoveTarget.valueOf("allAdjacentFoes"), "MoveTarget.allAdjacentFoes must exist");
        assertNotNull(MoveTarget.valueOf("allAdjacent"), "MoveTarget.allAdjacent must exist");
        assertSame(MoveTarget.allAdjacentFoes, MoveTarget.valueOf("allAdjacentFoes"));
        assertSame(MoveTarget.allAdjacent, MoveTarget.valueOf("allAdjacent"));
    }

    @Test
    @DisplayName("Runtime contract: RunBunAI.MoveEvaluation class and required methods exist")
    void testMoveEvaluationStructureContract() throws NoSuchMethodException {
        Class<?> evalClass = RunBunAI.MoveEvaluation.class;
        assertTrue(Modifier.isPublic(evalClass.getModifiers()), "MoveEvaluation must be public");

        Method getDamage = evalClass.getDeclaredMethod("getDamage");
        assertEquals(int.class, getDamage.getReturnType());

        Method getScore = evalClass.getDeclaredMethod("getScore");
        assertEquals(int.class, getScore.getReturnType());

        Method setScore = evalClass.getDeclaredMethod("setScore", int.class);
        assertEquals(void.class, setScore.getReturnType());

        Method getMove = evalClass.getDeclaredMethod("getMove");
        assertEquals(Move.class, getMove.getReturnType());

        Method getOpponent = evalClass.getDeclaredMethod("getOpponent");
        assertEquals(ActiveBattlePokemon.class, getOpponent.getReturnType());
    }

    @Test
    @DisplayName("Runtime contract: RunBunAI.choose() method signature matches injection target")
    void testRunBunAIChooseSignatureContract() throws NoSuchMethodException {
        Method chooseMethod = RunBunAI.class.getDeclaredMethod(
            "choose",
            ActiveBattlePokemon.class,
            PokemonBattle.class,
            BattleSide.class,
            ShowdownMoveset.class,
            boolean.class
        );
        assertTrue(Modifier.isPublic(chooseMethod.getModifiers()), "RunBunAI.choose must be public");
        assertEquals(ShowdownActionResponse.class, chooseMethod.getReturnType());
    }
}
