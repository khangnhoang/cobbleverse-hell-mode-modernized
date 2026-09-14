package com.cobbleverse.legendaryrule.mixin.battle;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.battles.BattleSide;
import com.cobblemon.mod.common.battles.ShowdownActionResponse;
import com.cobbleverse.legendaryrule.mechanic.SideMechanicValidator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.UUID;

@Mixin(value = BattleActor.class, remap = false)
public abstract class BattleActorActionResponsesMixin {
    @Shadow
    public abstract PokemonBattle getBattle();

    @Shadow
    public abstract BattleSide getSide();

    @Inject(method = "setActionResponses", at = @At("HEAD"))
    private void beforeSetActionResponses(List<? extends ShowdownActionResponse> responses, CallbackInfo ci) {
        BattleActor actor = (BattleActor) (Object) this;
        PokemonBattle battle = actor.getBattle();
        if (battle == null) {
            return;
        }
        BattleSide side = actor.getSide();
        int sideIndex = (side == battle.getSide1()) ? 0 : 1;
        UUID battleId = battle.getBattleId();
        SideMechanicValidator.validateAndSanitizeResponses(actor, battleId, sideIndex, responses);
    }
}
