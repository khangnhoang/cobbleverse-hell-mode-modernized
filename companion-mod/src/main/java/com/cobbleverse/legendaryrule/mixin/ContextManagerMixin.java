package com.cobbleverse.legendaryrule.mixin;

import com.cobblemon.mod.common.api.battles.interpreter.BattleContext;
import com.cobblemon.mod.common.battles.interpreter.ContextManager;
import com.cobbleverse.legendaryrule.strategy.weather.WeatherContextNormalizer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;

/**
 * Intercepts ContextManager.get(BattleContext.Type) to normalize WEATHER contexts
 * by appending standard AI aliases (harshsunlight, rain, etc.) after canonical Showdown tokens.
 */
@Mixin(value = ContextManager.class, remap = false)
public abstract class ContextManagerMixin {

    @Inject(
        method = "get(Lcom/cobblemon/mod/common/api/battles/interpreter/BattleContext$Type;)Ljava/util/Collection;",
        at = @At("RETURN"),
        cancellable = true,
        remap = false
    )
    private void cobbleverse$normalizeWeatherContexts(
        BattleContext.Type bucketType,
        CallbackInfoReturnable<Collection<BattleContext>> cir
    ) {
        if (bucketType == BattleContext.Type.WEATHER) {
            Collection<BattleContext> raw = cir.getReturnValue();
            if (raw != null && !raw.isEmpty()) {
                cir.setReturnValue(WeatherContextNormalizer.normalize(raw));
            }
        }
    }
}
