package com.eldenworld.core.balance;

import com.eldenworld.core.EldenWorldCore;
import com.eldenworld.core.requirements.RequirementChecker;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = EldenWorldCore.MOD_ID)
public final class WorldScalingEvents {
    private static final UUID HP_ID = UUID.fromString("d39cbdd2-54f4-4f62-ae92-c99b57ea1f50");
    private static final UUID DAMAGE_ID = UUID.fromString("fb80b080-c08c-4a8d-b105-3319d2f5e02a");
    private static final UUID ARMOR_ID = UUID.fromString("a6d87c9e-3490-4370-a9a7-fef9180e0cb3");
    private static final String LEVEL_TAG = "eldenworld_m6_world_level";
    private static int tickCounter;

    private WorldScalingEvents() {}

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof Monster monster) || monster.isNoAi()) return;
        apply(monster, globalWorldLevel(level.getServer()));
    }

    /* Re-evaluate loaded mobs periodically because the server-wide average can change
       when players level up, join or leave. Distance and dimension do not matter. */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (++tickCounter < 200) return; // every 10 seconds
        tickCounter = 0;
        MinecraftServer server = event.getServer();
        double worldLevel = globalWorldLevel(server);
        if (worldLevel <= 0) return;
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof Monster monster && !monster.isNoAi()) apply(monster, worldLevel);
            }
        }
    }

    public static double globalWorldLevel(MinecraftServer server) {
        if (server == null) return 0;
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        if (players.isEmpty()) return 0;
        double sum = 0;
        int count = 0;
        for (ServerPlayer player : players) {
            try {
                sum += Math.min(150, Math.max(1, RequirementChecker.pstLevel(player)));
                count++;
            } catch (RuntimeException ignored) {}
        }
        return count == 0 ? 0 : sum / count;
    }

    private static void apply(Monster monster, double level) {
        if (level <= 0) return;
        double previous = monster.getPersistentData().getDouble(LEVEL_TAG);
        if (previous > 0 && Math.abs(previous - level) < 1.0) return;

        float oldMax = monster.getMaxHealth();
        float healthRatio = oldMax <= 0 ? 1.0f : Math.max(0.0f, Math.min(1.0f, monster.getHealth() / oldMax));
        remove(monster.getAttribute(Attributes.MAX_HEALTH), HP_ID);
        remove(monster.getAttribute(Attributes.ATTACK_DAMAGE), DAMAGE_ID);
        remove(monster.getAttribute(Attributes.ARMOR), ARMOR_ID);

        if (level >= 10) {
            double progress = curve(level);
            Tier tier = tier(monster, monster.getMaxHealth());
            multiply(monster.getAttribute(Attributes.MAX_HEALTH), HP_ID, "EldenWorld M6 world HP", progress * tier.hp);
            multiply(monster.getAttribute(Attributes.ATTACK_DAMAGE), DAMAGE_ID, "EldenWorld M6 world damage", progress * tier.damage);
            add(monster.getAttribute(Attributes.ARMOR), ARMOR_ID, "EldenWorld M6 world armor", progress * tier.armor);
        }
        monster.getPersistentData().putDouble(LEVEL_TAG, level);
        monster.setHealth(Math.max(1.0f, monster.getMaxHealth() * healthRatio));
    }

    /* 10 -> 0%, 30 -> 20%, 60 -> 45%, 90 -> 70%, 120 -> 88%, 150 -> 100%. */
    static double curve(double level) {
        if (level <= 10) return 0;
        if (level <= 30) return .20 * (level - 10) / 20.0;
        if (level <= 60) return .20 + .25 * (level - 30) / 30.0;
        if (level <= 90) return .45 + .25 * (level - 60) / 30.0;
        if (level <= 120) return .70 + .18 * (level - 90) / 30.0;
        return Math.min(1.0, .88 + .12 * (level - 120) / 30.0);
    }

    static Tier tier(LivingEntity mob, double hp) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(mob.getType());
        String namespace = id == null ? "" : id.getNamespace().toLowerCase(Locale.ROOT);
        String path = id == null ? "" : id.getPath().toLowerCase(Locale.ROOT);
        boolean namedBoss = path.contains("boss") || path.contains("leviathan") || path.contains("ignis") || path.contains("harbinger") || path.contains("wither") || path.contains("warden") || path.contains("dragon") || path.contains("lich") || path.contains("hydra") || path.contains("naga") || path.contains("ur_ghast") || path.contains("snow_queen") || path.contains("ferrous_wroughtnaut") || path.contains("frostmaw");
        boolean bossMod = namespace.contains("cataclysm") || namespace.contains("mowzie") || namespace.contains("aquamirae") || namespace.contains("legendary") || namespace.contains("block_factory") || namespace.contains("twilightforest");
        boolean dangerous = namespace.contains("born_in_chaos") || namespace.contains("alexscaves") || namespace.contains("iceandfire") || namespace.contains("alexsmobs");
        if (hp >= 180 || namedBoss || (bossMod && hp >= 100)) return new Tier(.18, .10, 2.0);
        if (hp >= 80 || bossMod) return new Tier(.30, .17, 3.0);
        if (hp >= 45 || dangerous) return new Tier(.45, .24, 4.0);
        return new Tier(.65, .32, 6.0);
    }

    private static void remove(AttributeInstance attribute, UUID id) {
        if (attribute != null && attribute.getModifier(id) != null) attribute.removeModifier(id);
    }

    private static void multiply(AttributeInstance attribute, UUID id, String name, double value) {
        if (attribute != null && value > 0) attribute.addPermanentModifier(new AttributeModifier(id, name, value, AttributeModifier.Operation.MULTIPLY_BASE));
    }

    private static void add(AttributeInstance attribute, UUID id, String name, double value) {
        if (attribute != null && value > 0) attribute.addPermanentModifier(new AttributeModifier(id, name, value, AttributeModifier.Operation.ADDITION));
    }

    record Tier(double hp, double damage, double armor) {}
}
