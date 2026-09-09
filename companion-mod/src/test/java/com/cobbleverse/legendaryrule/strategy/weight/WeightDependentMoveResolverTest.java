package com.cobbleverse.legendaryrule.strategy.weight;

import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.api.moves.categories.DamageCategories;
import com.cobblemon.mod.common.api.types.ElementalTypes;
import com.cobblemon.mod.common.battles.MoveTarget;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class WeightDependentMoveResolverTest {

    private static final Unsafe unsafe;

    static {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            unsafe = (Unsafe) field.get(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Move createMove(String name, double power) {
        MoveTemplate template = new MoveTemplate(
            name,
            100,
            ElementalTypes.GRASS,
            DamageCategories.INSTANCE.getSPECIAL(),
            power,
            MoveTarget.normal,
            1.0d,
            20,
            1,
            1.5d,
            new Double[0]
        );
        return new Move(template, 15, 2);
    }

    private BattlePokemon createDummyBattlePokemon() throws Exception {
        return (BattlePokemon) unsafe.allocateInstance(BattlePokemon.class);
    }

    @Test
    @DisplayName("Null move returns null, null attacker or defender returns original move")
    void testNullGuards() throws Exception {
        BattlePokemon bp = createDummyBattlePokemon();
        Move move = createMove("grassknot", 0.0d);

        assertNull(WeightDependentMoveResolver.resolveEffectiveMove(null, bp, bp));
        assertSame(move, WeightDependentMoveResolver.resolveEffectiveMove(move, null, bp));
        assertSame(move, WeightDependentMoveResolver.resolveEffectiveMove(move, bp, null));
        assertSame(move, WeightDependentMoveResolver.resolveEffectiveMove(move, null, null));
    }

    @Test
    @DisplayName("Unrelated moves are completely untouched by resolver")
    void testUnrelatedMoveUntouched() throws Exception {
        BattlePokemon attacker = createDummyBattlePokemon();
        BattlePokemon defender = createDummyBattlePokemon();

        Move energyBall = createMove("energyball", 90.0d);
        Move thunderbolt = createMove("thunderbolt", 90.0d);
        Move tackle = createMove("tackle", 40.0d);

        assertSame(energyBall, WeightDependentMoveResolver.resolveEffectiveMove(energyBall, attacker, defender));
        assertSame(thunderbolt, WeightDependentMoveResolver.resolveEffectiveMove(thunderbolt, attacker, defender));
        assertSame(tackle, WeightDependentMoveResolver.resolveEffectiveMove(tackle, attacker, defender));
    }

    @Test
    @DisplayName("Surrogate preserves original move metadata, PP, and stages while updating power")
    void testSurrogatePreservesMetadata() {
        Move original = createMove("grassknot", 0.0d);
        WeightDependentMoveSurrogate surrogate = new WeightDependentMoveSurrogate(original, 80.0d);

        assertEquals("grassknot", surrogate.getName(), "Name must match original to preserve upstream checks");
        assertEquals(100, surrogate.getTemplate().getNum(), "Move number must be preserved");
        assertEquals(ElementalTypes.GRASS, surrogate.getType(), "Elemental type must be preserved");
        assertEquals(DamageCategories.INSTANCE.getSPECIAL(), surrogate.getTemplate().getDamageCategory(), "Category must be preserved");
        assertEquals(MoveTarget.normal, surrogate.getTemplate().getTarget(), "Target must be preserved");
        assertEquals(1.0d, surrogate.getTemplate().getAccuracy(), 0.001, "Accuracy must be preserved");
        assertEquals(20, surrogate.getTemplate().getPp(), "Max PP must be preserved");
        assertEquals(1, surrogate.getTemplate().getPriority(), "Priority must be preserved");
        assertEquals(1.5d, surrogate.getTemplate().getCritRatio(), 0.001, "Crit ratio must be preserved");
        assertEquals(15, surrogate.getCurrentPp(), "Current PP must be preserved");
        assertEquals(2, surrogate.getRaisedPpStages(), "Raised PP stages must be preserved");

        assertEquals(80.0d, surrogate.getPower(), 0.001, "Effective power must return resolved base power");
        assertEquals(80.0d, surrogate.getResolvedBasePower(), 0.001, "getResolvedBasePower must match resolved power");
        assertSame(original, surrogate.getOriginal(), "getOriginal must return original Move instance");
    }

    @Test
    @DisplayName("Idempotent resolution: resolving already-resolved surrogate returns exact same instance")
    void testIdempotentResolution() throws Exception {
        BattlePokemon attacker = createDummyBattlePokemon();
        BattlePokemon defender = createDummyBattlePokemon();

        Move original = createMove("grassknot", 0.0d);
        Move surrogate = WeightDependentMoveResolver.resolveEffectiveMove(original, attacker, defender);
        assertInstanceOf(WeightDependentMoveSurrogate.class, surrogate);

        Move reResolved = WeightDependentMoveResolver.resolveEffectiveMove(surrogate, attacker, defender);
        assertSame(surrogate, reResolved, "Re-resolving a surrogate must return the same instance immediately");
    }

    @Test
    @DisplayName("normalizeMoveName handles casing, hyphens, underscores, and whitespace")
    void testNormalizeMoveName() {
        assertEquals("grassknot", WeightDependentMoveResolver.normalizeMoveName("Grass Knot"));
        assertEquals("grassknot", WeightDependentMoveResolver.normalizeMoveName("grass-knot"));
        assertEquals("grassknot", WeightDependentMoveResolver.normalizeMoveName("GRASS_KNOT"));
        assertEquals("lowkick", WeightDependentMoveResolver.normalizeMoveName("Low Kick"));
        assertEquals("lowkick", WeightDependentMoveResolver.normalizeMoveName("low-kick"));
        assertEquals("heavyslam", WeightDependentMoveResolver.normalizeMoveName("Heavy Slam"));
        assertEquals("heatcrash", WeightDependentMoveResolver.normalizeMoveName("Heat Crash"));
        assertEquals("", WeightDependentMoveResolver.normalizeMoveName(null));
    }

    @Test
    @DisplayName("isWeightDependent correctly classifies weight moves and rejects others")
    void testIsWeightDependent() {
        assertTrue(WeightDependentMoveResolver.isWeightDependent("grassknot"));
        assertTrue(WeightDependentMoveResolver.isWeightDependent("Grass Knot"));
        assertTrue(WeightDependentMoveResolver.isWeightDependent("lowkick"));
        assertTrue(WeightDependentMoveResolver.isWeightDependent("Low-Kick"));
        assertTrue(WeightDependentMoveResolver.isWeightDependent("heavyslam"));
        assertTrue(WeightDependentMoveResolver.isWeightDependent("Heavy Slam"));
        assertTrue(WeightDependentMoveResolver.isWeightDependent("heatcrash"));
        assertTrue(WeightDependentMoveResolver.isWeightDependent("Heat_Crash"));

        assertFalse(WeightDependentMoveResolver.isWeightDependent("energyball"));
        assertFalse(WeightDependentMoveResolver.isWeightDependent("closecombat"));
        assertFalse(WeightDependentMoveResolver.isWeightDependent("flashcannon"));
        assertFalse(WeightDependentMoveResolver.isWeightDependent("flareblitz"));
        assertFalse(WeightDependentMoveResolver.isWeightDependent((String) null));
    }
}
