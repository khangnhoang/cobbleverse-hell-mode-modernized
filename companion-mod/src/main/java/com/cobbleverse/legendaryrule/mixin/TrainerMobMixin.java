package com.cobbleverse.legendaryrule.mixin;

import com.cobbleverse.legendaryrule.LegendaryPartyRule;
import com.gitlab.srcmc.rctmod.world.entities.TrainerMob;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TrainerMob.class)
public abstract class TrainerMobMixin {

    @Inject(method = "canBattleAgainst", at = @At("HEAD"), cancellable = true)
    private void checkLegendaryPartyRule(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof ServerPlayerEntity player) {
            if (!LegendaryPartyRule.isAllowed(player)) {
                cir.setReturnValue(false);
            }
        }
    }

    @Inject(
        method = "replyTo",
        at = @At(
            value = "INVOKE",
            target = "Lcom/gitlab/srcmc/rctmod/api/utils/ChatUtils;reply(Lcom/gitlab/srcmc/rctmod/world/entities/TrainerMob;Lnet/minecraft/server/network/ServerPlayerEntity;[Ljava/lang/String;)V",
            ordinal = 10
        ),
        cancellable = true
    )
    private void onReplyUnknownReason(ServerPlayerEntity player, CallbackInfo ci) {
        if (!LegendaryPartyRule.isAllowed(player)) {
            LegendaryPartyRule.sendRejectionMessage(player);
            ci.cancel();
        }
    }
}
