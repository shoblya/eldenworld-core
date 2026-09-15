package com.eldenworld.core.specialization;

import com.eldenworld.core.EldenWorldCore;
import com.eldenworld.core.abilities.AbilityUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = EldenWorldCore.MOD_ID)
public final class PetMasteryEvents {
    private PetMasteryEvents() {}

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        Entity attacker = event.getSource().getEntity();
        if (attacker instanceof OwnableEntity pet && pet.getOwner() instanceof ServerPlayer owner
                && AbilityUtil.has(owner, SpecializationIds.BEASTMASTER)) {
            event.setAmount(event.getAmount() * 1.20f);
        }
        if (event.getEntity() instanceof OwnableEntity pet && pet.getOwner() instanceof ServerPlayer owner
                && AbilityUtil.has(owner, SpecializationIds.BEASTMASTER)) {
            event.setAmount(event.getAmount() * 0.85f);
        }
        Entity vehicle = event.getEntity().getVehicle();
        if (vehicle != null && event.getEntity() instanceof ServerPlayer rider
                && AbilityUtil.has(rider, SpecializationIds.DRAGON_RIDER)) {
            event.setAmount(event.getAmount() * 0.90f);
        }
    }
}
