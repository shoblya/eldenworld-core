package com.eldenworld.core.specialization;

import com.eldenworld.core.EldenWorldCore;
import com.eldenworld.core.abilities.AbilityUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = EldenWorldCore.MOD_ID)
public final class WeaponMasteryEvents {
    private WeaponMasteryEvents() {}

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer attacker) {
            ItemStack weapon = attacker.getMainHandItem();
            String path = itemPath(weapon);
            float mult = 1.0f;
            if (AbilityUtil.has(attacker, SpecializationIds.NIGHTBLADE) && hasAny(path, "dagger", "knife", "sai")) mult *= 1.15f;
            if (AbilityUtil.has(attacker, SpecializationIds.COLOSSUS) && hasAny(path, "greatsword", "claymore", "greataxe", "warhammer", "great_hammer")) mult *= 1.15f;
            if (AbilityUtil.has(attacker, SpecializationIds.DUELIST) && attacker.getOffhandItem().isEmpty() && !weapon.isEmpty()) mult *= 1.10f;
            if (AbilityUtil.has(attacker, SpecializationIds.MARKSMAN) && event.getSource().getDirectEntity() instanceof Projectile) mult *= 1.12f;
            event.setAmount(event.getAmount() * mult);
        }
        if (event.getEntity() instanceof ServerPlayer defender
                && AbilityUtil.has(defender, SpecializationIds.BULWARK)
                && (defender.getMainHandItem().getItem() instanceof ShieldItem || defender.getOffhandItem().getItem() instanceof ShieldItem)) {
            event.setAmount(event.getAmount() * 0.90f);
        }
    }

    private static String itemPath(ItemStack stack) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id == null ? "" : id.getPath();
    }
    private static boolean hasAny(String path, String... needles) {
        for (String needle : needles) if (path.contains(needle)) return true;
        return false;
    }
}
