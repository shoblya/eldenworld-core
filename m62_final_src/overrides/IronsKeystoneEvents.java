package com.eldenworld.core.abilities;

import com.eldenworld.core.EldenWorldCore;
import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = EldenWorldCore.MOD_ID)
public final class IronsKeystoneEvents {
    private IronsKeystoneEvents() {}

    @SubscribeEvent
    public static void onSpellCast(SpellOnCastEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (AbilityUtil.has(player, KeystoneIds.ARCHMAGE)) {
            int increasedCost = (int) Math.ceil(event.getManaCost() * 1.20d);
            event.setManaCost(Math.max(event.getManaCost(), increasedCost));
        }
    }
}
