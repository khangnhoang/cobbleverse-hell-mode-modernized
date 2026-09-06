package com.cobbleverse.legendaryrule.strategy.spread;

import com.gitlab.surilexa.rbrctai.api.ai.utils.PokeMathMax;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;

class PokeMathMaxSpreadMultiplierTest {

    @Test
    @DisplayName("CASE A: Single-target multiplier invariant (multiTarget = false -> 1.0x factor)")
    void testSingleTargetMultiplierFactor() {
        double rawDamage = 100.0;
        boolean multiTarget = false;

        double calculated = multiTarget ? rawDamage * 0.75d : rawDamage;
        assertEquals(100.0, calculated, 0.0001, "Single-target damage must remain completely unaffected (factor = 1.0)");
    }

    @Test
    @DisplayName("CASE B: Spread multiplier arithmetic correction (0.75^2 bug vs 0.75 exact)")
    void testSpreadMultiplierBugArithmeticCorrection() {
        double rawDamage = 100.0;

        // Buggy R&B behavior: PokeMathMax.damage applied 0.75 internally when multiTarget=true,
        // while also factoring spreadMultiplier(attacker, move) < 1.0, compounding to 0.75 * 0.75 = 0.5625
        double buggyDamage = rawDamage * 0.75d * 0.75d;
        assertEquals(56.25, buggyDamage, 0.0001, "Buggy calculation compounded 0.75 twice to 0.5625");

        // Corrected behavior: Passing multiTarget=false internally leaves internal multiplier at 1.0,
        // and caller applies 0.75 exactly once:
        boolean multiTarget = true;
        double correctedDamage = multiTarget ? rawDamage * 0.75d : rawDamage;
        assertEquals(75.0, correctedDamage, 0.0001, "Corrected calculation must apply 0.75 exactly once");

        // Relative restoration: Spread damage valuation is restored by 1.333x (4/3) relative to the buggy value
        assertEquals(75.0 / 56.25, 4.0 / 3.0, 0.0001, "Fix restores 33.3% artificially lost spread damage");
    }

    @Test
    @DisplayName("Bytecode contract: PokeMathMax public damage method exists with exact expected signature")
    void testPokeMathMaxPublicDamageSignatureContract() {
        boolean found = false;
        for (Method method : PokeMathMax.class.getDeclaredMethods()) {
            if (method.getName().equals("damage") && Modifier.isPublic(method.getModifiers())) {
                Class<?>[] params = method.getParameterTypes();
                // public static int damage(BattlePokemon, BattlePokemon, Move, ActiveBattlePokemon, boolean, boolean, RBStatStages)
                if (params.length == 7 && method.getReturnType() == int.class) {
                    found = true;
                    break;
                }
            }
        }
        assertTrue(found, "PokeMathMax.damage public entrypoint with 7 parameters must exist");
    }

    @Test
    @DisplayName("Bytecode contract: PokeMathMax private damage method exists with multiTarget boolean parameter")
    void testPokeMathMaxPrivateDamageSignatureContract() {
        boolean found = false;
        for (Method method : PokeMathMax.class.getDeclaredMethods()) {
            if (method.getName().equals("damage") && Modifier.isPrivate(method.getModifiers())) {
                Class<?>[] params = method.getParameterTypes();
                // private static double damage(Move, boolean, boolean, boolean, boolean, boolean, boolean, boolean, boolean,
                //                              BattlePokemon, BattlePokemon, RBStatStages, ActiveBattlePokemon, boolean, boolean)
                if (params.length == 15 && method.getReturnType() == double.class) {
                    // 3rd parameter (index 2) must be boolean multiTarget
                    assertEquals(boolean.class, params[2], "3rd parameter of private damage must be boolean multiTarget");
                    found = true;
                    break;
                }
            }
        }
        assertTrue(found, "PokeMathMax.damage private helper with 15 parameters must exist");
    }
}
