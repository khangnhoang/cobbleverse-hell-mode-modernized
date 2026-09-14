package com.cobbleverse.legendaryrule.mixin.battle;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.battles.ShowdownActionRequest;
import com.cobblemon.mod.common.battles.ShowdownMoveset;
import com.cobbleverse.legendaryrule.mechanic.BattleSideMechanicTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = ShowdownActionRequest.class, remap = false)
public abstract class ShowdownActionRequestSanitizeMixin {
    @Shadow
    public abstract List<ShowdownMoveset> getActive();

    @Inject(method = "sanitize", at = @At("TAIL"))
    private void afterSanitize(PokemonBattle battle, BattleActor battleActor, CallbackInfo ci) {
        if (battle == null || battleActor == null || battleActor.getSide() == null) {
            return;
        }
        int sideIndex = (battleActor.getSide() == battle.getSide1()) ? 0 : 1;
        if (BattleSideMechanicTracker.isMechanicLocked(battle.getBattleId(), sideIndex)) {
            List<ShowdownMoveset> activeList = this.getActive();
            if (activeList != null) {
                for (ShowdownMoveset moveset : activeList) {
                    if (moveset != null) {
                        moveset.blockGimmick(ShowdownMoveset.Gimmick.MEGA_EVOLUTION);
                        moveset.blockGimmick(ShowdownMoveset.Gimmick.DYNAMAX);
                        moveset.blockGimmick(ShowdownMoveset.Gimmick.TERASTALLIZATION);
                        moveset.blockGimmick(ShowdownMoveset.Gimmick.Z_POWER);
                        moveset.blockGimmick(ShowdownMoveset.Gimmick.ULTRA_BURST);
                    }
                }
            }
        }
    }
}
