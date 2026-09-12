package com.cobbleverse.legendaryrule.mixin;

import com.cobbleverse.legendaryrule.notify.NotifyWatchlistManager;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import us.timinc.mc.cobblemon.spawnnotification.Broadcaster;
import us.timinc.mc.cobblemon.spawnnotification.api.broadcast.BroadcastContext;

@Mixin(value = Broadcaster.class, remap = false)
public class BroadcasterMixin {

    @Inject(method = "broadcast", at = @At("HEAD"))
    private void onBroadcast(BroadcastContext broadcastContext, Identifier trigger, CallbackInfo ci) {
        NotifyWatchlistManager.handleBroadcast(broadcastContext, trigger);
    }
}
