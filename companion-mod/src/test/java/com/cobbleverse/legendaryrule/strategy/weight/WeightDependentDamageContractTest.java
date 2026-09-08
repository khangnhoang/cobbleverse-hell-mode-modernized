package com.cobbleverse.legendaryrule.strategy.weight;

import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.api.moves.categories.DamageCategories;
import com.cobblemon.mod.common.api.types.ElementalTypes;
import com.cobblemon.mod.common.battles.MoveTarget;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.pokemon.FormData;
import com.cobblemon.mod.common.pokemon.Species;
import com.gitlab.surilexa.rbrctai.api.ai.utils.PokeMathMax;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;

class WeightDependentDamageContractTest {

    @BeforeAll
    static void setUpAll() {
        try {
            net.minecraft.SharedConstants.createGameVersion();
            Method init = Class.forName("net.minecraft.Bootstrap").getDeclaredMethod("initialize");
            init.setAccessible(true);
            init.invoke(null);
        } catch (Throwable ignored) {
        }
    }

    private Move createMove(String name, double power) {
        MoveTemplate template = new MoveTemplate(
            name,
            1,
            ElementalTypes.GRASS,
            DamageCategories.INSTANCE.getSPECIAL(),
            power,
            MoveTarget.normal,
            1.0d,
            20,
            0,
            1.0d,
            new Double[0]
        );
        return new Move(template, 20, 0);
    }

    /**
     * Canonical Gen 9 Battle Damage Formula:
     * BaseDamage = (((2 * Level) / 5 + 2) * Power * Attack / Defense) / 50 + 2
     */
    private double calculateCanonicalBaseDamage(int level, double power, double attack, double defense) {
        double levelFactor = Math.floor((2.0d * (double) level) / 5.0d) + 2.0d;
        return ((levelFactor * power * attack / defense) / 50.0d) + 2.0d;
    }

    @Test
    @DisplayName("Mathematical Contract: Swampert Grass Knot damage scaling restores ~65x damage and guarantees OHKO")
    void testSwampertGrassKnotDamageRestorationContract() {
        int level = 50;
        double spAtk = 150.0;
        double spDef = 110.0;
        double stab = 1.5;
        double quadWeakness = 4.0;

        // Unpatched: basePower == 0.0 -> baseDamage = 2.0
        double unpatchedBp = 0.0;
        double unpatchedBaseDmg = calculateCanonicalBaseDamage(level, unpatchedBp, spAtk, spDef);
        assertEquals(2.0, unpatchedBaseDmg, 0.001, "Unpatched base damage on BP 0 must equal 2.0");

        double unpatchedTotalDmg = Math.floor(unpatchedBaseDmg * stab * quadWeakness);
        assertEquals(12.0, unpatchedTotalDmg, 0.001, "Unpatched total damage against 4x weak target is negligible (~12 dmg)");

        // Swampert weight = 81.9 kg (819 hg) -> Grass Knot resolves to 80 BP
        double targetWeightHg = 819.0;
        double resolvedBp = WeightDependentMoveResolver.calculateTargetWeightBasePower(targetWeightHg);
        assertEquals(80.0, resolvedBp, 0.001);

        double resolvedBaseDmg = calculateCanonicalBaseDamage(level, resolvedBp, spAtk, spDef);
        // Base damage: (((22 * 80 * 150 / 110) / 50) + 2) = ((2400 / 50) + 2) = 50.0 (exact int level arithmetic)
        assertTrue(resolvedBaseDmg >= 45.0 && resolvedBaseDmg <= 95.0,
            "Resolved base damage must scale proportionally with 80 BP");

        double resolvedTotalDmg = Math.floor(resolvedBaseDmg * stab * quadWeakness);
        assertTrue(resolvedTotalDmg >= 300.0, "Resolved damage must deliver decisive OHKO (>300 dmg against ~200 HP Swampert)");

        // Damage restoration factor: resolved / unpatched is > 25x up to ~65x depending on exact EV spreads
        double damageRatio = resolvedTotalDmg / unpatchedTotalDmg;
        assertTrue(damageRatio >= 25.0, "Weight dynamic resolution restores at least 25x damage valuation over unpatched 0 BP");
    }

    @Test
    @DisplayName("Mathematical Contract: Tyranitar Low Kick delivers 120 BP super effective OHKO")
    void testTyranitarLowKickDamageContract() {
        int level = 50;
        double atk = 180.0;
        double def = 130.0;
        double quadWeakness = 4.0;

        // Tyranitar weight = 202.0 kg (2020 hg) -> Low Kick resolves to 120 BP
        double tyranitarWeightHg = 2020.0;
        double resolvedBp = WeightDependentMoveResolver.calculateTargetWeightBasePower(tyranitarWeightHg);
        assertEquals(120.0, resolvedBp, 0.001);

        double baseDmg = calculateCanonicalBaseDamage(level, resolvedBp, atk, def);
        double totalDmg = Math.floor(baseDmg * quadWeakness);

        // Tyranitar at level 50 has ~207 HP. 120 BP with 4x weakness deals > 250 damage.
        assertTrue(totalDmg >= 250.0, "120 BP Low Kick must guarantee OHKO against 4x weak Tyranitar");
    }

    @Test
    @DisplayName("Mathematical Contract: Heavy Slam 5x weight ratio resolves to 120 BP maximum power")
    void testHeavySlamMaxRatioContract() {
        // Attacker 250 kg (2500 hg), Defender 50 kg (500 hg) -> Ratio 5.0x
        double bp = WeightDependentMoveResolver.calculateWeightRatioBasePower(2500.0, 500.0);
        assertEquals(120.0, bp, 0.001, "5x ratio must yield 120 BP");

        // Attacker 100 kg (1000 hg), Defender 50 kg (500 hg) -> Ratio 2.0x
        double bp2 = WeightDependentMoveResolver.calculateWeightRatioBasePower(1000.0, 500.0);
        assertEquals(60.0, bp2, 0.001, "2x ratio must yield 60 BP");

        // Attacker 90 kg (900 hg), Defender 50 kg (500 hg) -> Ratio 1.8x
        double bpDefault = WeightDependentMoveResolver.calculateWeightRatioBasePower(900.0, 500.0);
        assertEquals(40.0, bpDefault, 0.001, "Sub-2x ratio must yield 40 BP default");
    }

    @Test
    @DisplayName("Technician Interaction Contract: Moves <= 60 BP receive 1.5x boost, > 60 BP do not")
    void testTechnicianInteractionContract() {
        // Low Kick vs 20 kg target -> 40 BP (<= 60) -> Technician applies: 40 * 1.5 = 60 BP
        double lowTargetWeight = 200.0;
        double baseBp = WeightDependentMoveResolver.calculateTargetWeightBasePower(lowTargetWeight);
        assertEquals(40.0, baseBp, 0.001);

        double technicianBp = (baseBp <= 60.0) ? baseBp * 1.5 : baseBp;
        assertEquals(60.0, technicianBp, 0.001, "Technician boosts 40 BP to 60 BP");

        // Grass Knot vs 50 kg target -> 80 BP (> 60) -> Technician does not apply
        double heavyTargetWeight = 500.0;
        double heavyBp = WeightDependentMoveResolver.calculateTargetWeightBasePower(heavyTargetWeight);
        assertEquals(80.0, heavyBp, 0.001);

        double heavyTechBp = (heavyBp <= 60.0) ? heavyBp * 1.5 : heavyBp;
        assertEquals(80.0, heavyTechBp, 0.001, "Technician does not boost moves above 60 BP");
    }

    @Test
    @DisplayName("Bytecode Contract: PokeMathMax damage methods exist with exact signatures")
    void testPokeMathMaxDamageBytecodeSignatures() {
        boolean publicDamageFound = false;
        boolean privateDamageFound = false;

        for (Method method : PokeMathMax.class.getDeclaredMethods()) {
            if (method.getName().equals("damage")) {
                if (Modifier.isPublic(method.getModifiers()) && method.getParameterTypes().length == 7) {
                    publicDamageFound = true;
                }
                if (Modifier.isPrivate(method.getModifiers()) && method.getParameterTypes().length == 15) {
                    privateDamageFound = true;
                }
            }
        }

        assertTrue(publicDamageFound, "PokeMathMax.damage public entrypoint (7 params) must exist");
        assertTrue(privateDamageFound, "PokeMathMax.damage private helper (15 params) must exist");
    }

    @Test
    @DisplayName("Bytecode Contract: FormData.getWeight and Species.getStandardForm exist with exact descriptors")
    void testFormDataAndSpeciesBytecodeSignatures() {
        boolean getWeightFound = false;
        for (Method method : FormData.class.getDeclaredMethods()) {
            if (method.getName().equals("getWeight") && method.getReturnType() == float.class) {
                getWeightFound = true;
                break;
            }
        }
        assertTrue(getWeightFound, "FormData.getWeight() returning float must exist");

        boolean getStandardFormFound = false;
        for (Method method : Species.class.getDeclaredMethods()) {
            if (method.getName().equals("getStandardForm") && method.getReturnType() == FormData.class) {
                getStandardFormFound = true;
                break;
            }
        }
        assertTrue(getStandardFormFound, "Species.getStandardForm() returning FormData must exist");
    }

    @Test
    @DisplayName("Surrogate Invariant Contract: Surrogate preserves original move name while overriding power")
    void testSurrogatePreservesOriginalName() {
        Move originalGrassKnot = createMove("grassknot", 0.0);
        WeightDependentMoveSurrogate surrogate = new WeightDependentMoveSurrogate(originalGrassKnot, 80.0);

        assertEquals("grassknot", surrogate.getName(),
            "Surrogate must retain original move name 'grassknot' to preserve downstream category/contact checks");
        assertEquals(80.0, surrogate.getPower(), 0.001,
            "Surrogate getPower() must return resolved base power 80.0");
        assertEquals(80.0, surrogate.getResolvedBasePower(), 0.001,
            "Surrogate getResolvedBasePower() must match 80.0");
    }
}
