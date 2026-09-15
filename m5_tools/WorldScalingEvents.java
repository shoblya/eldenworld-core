package com.eldenworld.core.balance;

import com.eldenworld.core.EldenWorldCore;
import com.eldenworld.core.requirements.RequirementChecker;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = EldenWorldCore.MOD_ID)
public final class WorldScalingEvents {
    private static final UUID HP_ID = UUID.fromString("d39cbdd2-54f4-4f62-ae92-c99b57ea1f50");
    private static final UUID DAMAGE_ID = UUID.fromString("fb80b080-c08c-4a8d-b105-3319d2f5e02a");
    private static final UUID ARMOR_ID = UUID.fromString("a6d87c9e-3490-4370-a9a7-fef9180e0cb3");
    private WorldScalingEvents() {}

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof Monster mob) || mob.isNoAi()) return;
        List<ServerPlayer> players = level.getEntitiesOfClass(ServerPlayer.class, mob.getBoundingBox().inflate(64.0));
        if (players.isEmpty()) return;

        int sum = 0;
        int count = 0;
        for (ServerPlayer player : players) {
            try { sum += Math.min(100, RequirementChecker.pstLevel(player)); count++; } catch (RuntimeException ignored) {}
        }
        if (count == 0) return;
        double progress = (sum / (double) count) / 100.0;
        if (progress <= 0.05) return;

        double baseHp = mob.getMaxHealth();
        Tier tier = tier(mob, baseHp);
        addMultiplier(mob.getAttribute(Attributes.MAX_HEALTH), HP_ID, "EldenWorld M5 world HP", progress * tier.hp);
        addMultiplier(mob.getAttribute(Attributes.ATTACK_DAMAGE), DAMAGE_ID, "EldenWorld M5 world damage", progress * tier.damage);
        addAddition(mob.getAttribute(Attributes.ARMOR), ARMOR_ID, "EldenWorld M5 world armor", progress * tier.armor);
        mob.setHealth(mob.getMaxHealth());
    }

    private static Tier tier(LivingEntity mob, double hp) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(mob.getType());
        String ns = id == null ? "" : id.getNamespace();
        String path = id == null ? "" : id.getPath();
        boolean bossName = path.contains("boss") || path.contains("leviathan") || path.contains("ignis") || path.contains("harbinger") || path.contains("dragon");
        boolean bossMod = ns.contains("cataclysm") || ns.contains("mowzie") || ns.contains("aquamirae");
        if (hp >= 180 || bossName || (bossMod && hp >= 100)) return new Tier(0.12, 0.07, 2.0);
        if (hp >= 60) return new Tier(0.24, 0.12, 3.0);
        return new Tier(0.38, 0.18, 4.0);
    }

    private static void addMultiplier(AttributeInstance a, UUID id, String name, double amount) {
        if (a == null || a.getModifier(id) != null || amount <= 0) return;
        a.addPermanentModifier(new AttributeModifier(id, name, amount, AttributeModifier.Operation.MULTIPLY_BASE));
    }
    private static void addAddition(AttributeInstance a, UUID id, String name, double amount) {
        if (a == null || a.getModifier(id) != null || amount <= 0) return;
        a.addPermanentModifier(new AttributeModifier(id, name, amount, AttributeModifier.Operation.ADDITION));
    }
    private record Tier(double hp, double damage, double armor) {}
}
