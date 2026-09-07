package com.cobbleverse.legendaryrule.strategy.weather;

import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.api.moves.categories.DamageCategories;
import com.cobblemon.mod.common.api.types.ElementalTypes;
import com.cobblemon.mod.common.battles.MoveTarget;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MegaSolWeatherBallSurrogateTest {

    @Test
    @DisplayName("Surrogate correctly overrides name, type, and base power")
    void testSurrogateOverrides() {
        MoveTemplate template = new MoveTemplate(
            "weatherball",
            311,
            ElementalTypes.NORMAL,
            DamageCategories.INSTANCE.getSPECIAL(),
            50.0d,
            MoveTarget.normal,
            1.0d,
            10,
            0,
            1.0d,
            new Double[0]
        );
        Move original = new Move(template, 8, 1);

        MegaSolWeatherBallSurrogate surrogate = new MegaSolWeatherBallSurrogate(original);

        assertEquals(MegaSolWeatherBallSurrogate.RESOLVED_MOVE_NAME, surrogate.getName(),
            "Effective move name must be weatherball_fire_resolved");
        assertEquals(ElementalTypes.FIRE, surrogate.getType(),
            "Effective move type must be ElementalTypes.FIRE");
        assertEquals(MegaSolWeatherBallSurrogate.RESOLVED_BASE_POWER, surrogate.getPower(), 0.001,
            "Effective base power must be 100.0");
    }

    @Test
    @DisplayName("Surrogate preserves original template metadata, PP, and stages")
    void testSurrogatePreservedFields() {
        MoveTemplate template = new MoveTemplate(
            "weatherball",
            311,
            ElementalTypes.NORMAL,
            DamageCategories.INSTANCE.getSPECIAL(),
            50.0d,
            MoveTarget.normal,
            1.0d,
            10,
            2,
            1.5d,
            new Double[0]
        );
        Move original = new Move(template, 7, 3);

        MegaSolWeatherBallSurrogate surrogate = new MegaSolWeatherBallSurrogate(original);

        assertEquals(311, surrogate.getTemplate().getNum(), "Move number must be preserved");
        assertEquals(MoveTarget.normal, surrogate.getTemplate().getTarget(), "Target must remain MoveTarget.normal");
        assertEquals(DamageCategories.INSTANCE.getSPECIAL(), surrogate.getTemplate().getDamageCategory(),
            "Damage category must remain SPECIAL");
        assertEquals(1.0d, surrogate.getTemplate().getAccuracy(), 0.001, "Accuracy must be preserved");
        assertEquals(10, surrogate.getTemplate().getPp(), "Max base PP must be preserved");
        assertEquals(2, surrogate.getTemplate().getPriority(), "Priority must be preserved");
        assertEquals(1.5d, surrogate.getTemplate().getCritRatio(), 0.001, "Crit ratio must be preserved");

        assertEquals(7, surrogate.getCurrentPp(), "Current PP must be preserved from original");
        assertEquals(3, surrogate.getRaisedPpStages(), "Raised PP stages must be preserved from original");
    }
}
