package com.cobbleverse.legendaryrule.mixin;

import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobbleverse.legendaryrule.lead.DynamicLeadFallbackBoundary;
import com.cobbleverse.legendaryrule.lead.LeadSelectionResult;
import com.cobbleverse.legendaryrule.lead.LeadSelectionService;
import com.cobbleverse.legendaryrule.lead.RosterOrderer;
import com.gitlab.srcmc.rctapi.api.trainer.TrainerNPC;
import com.gitlab.srcmc.rctmod.api.RCTMod;
import com.gitlab.srcmc.rctmod.world.entities.TrainerMob;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.Optional;

@Mixin(RCTMod.class)
public abstract class RCTModMakeBattleMixin {

    @ModifyVariable(
        method = "makeBattle(Lcom/gitlab/srcmc/rctmod/world/entities/TrainerMob;Lnet/minecraft/entity/player/PlayerEntity;)Z",
        at = @At(value = "STORE", ordinal = 0),
        ordinal = 0
    )
    private TrainerNPC modifyTrainerNPC(TrainerNPC original, TrainerMob mob, PlayerEntity player) {
        if (original == null || mob == null || player == null) {
            return original;
        }

        String trainerId = mob.getTrainerId();
        Pokemon[] team = original.getTeam();
        if (trainerId == null || team == null || team.length == 0) {
            return original;
        }

        return DynamicLeadFallbackBoundary.execute(original, trainerId, () -> {
            Optional<LeadSelectionResult> optResult = LeadSelectionService.selectLead(trainerId, team, player);
            if (optResult.isEmpty()) {
                return original;
            }

            LeadSelectionResult result = optResult.get();
            // Clone TrainerNPC to ensure zero global mutation across battles and players
            TrainerNPC perBattleNPC = new TrainerNPC(original);
            Pokemon[] reordered = RosterOrderer.reorder(perBattleNPC.getTeam(), result.selectedAttempt().leadSlots(), Pokemon[]::new);
            System.arraycopy(reordered, 0, perBattleNPC.getTeam(), 0, reordered.length);

            return perBattleNPC;
        });
    }
}
