package com.eldenworld.core.balance;

import com.eldenworld.core.EldenWorldCore;
import com.eldenworld.core.requirements.RequirementChecker;
import com.google.gson.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.*;

@Mod.EventBusSubscriber(modid = EldenWorldCore.MOD_ID)
public final class WorldScalingEvents {
    private static final UUID HP_ID = UUID.fromString("d39cbdd2-54f4-4f62-ae92-c99b57ea1f50");
    private static final UUID OLD_DAMAGE_ID = UUID.fromString("fb80b080-c08c-4a8d-b105-3319d2f5e02a");
    private static final UUID ARMOR_ID = UUID.fromString("a6d87c9e-3490-4370-a9a7-fef9180e0cb3");
    private static final UUID ARMOR_SHRED_ID = UUID.fromString("6becc945-b235-44a0-9fc1-23bce45f6201");
    private static final UUID KB_ID = UUID.fromString("6becc945-b235-44a0-9fc1-23bce45f6202");

    private static final ResourceLocation ARMOR_SHRED = new ResourceLocation("attributeslib", "armor_shred");
    private static final TagKey<EntityType<?>> EXCLUDE = TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("eldenworld_scaling", "exclude"));
    private static final TagKey<EntityType<?>> ELITE = TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("eldenworld_scaling", "elites"));
    private static final TagKey<EntityType<?>> STANDARD_BOSS = TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("eldenworld_scaling", "standard_bosses"));
    private static final TagKey<EntityType<?>> BENCHMARK_BOSS = TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("eldenworld_scaling", "benchmark_bosses"));

    private static final String NBT_TIER = "EldenWorldM62Tier";
    private static final String NBT_PROFILE = "EldenWorldM62Profile";
    private static final String NBT_BASE_HP = "EldenWorldM62BaseHP";
    private static final String NBT_ANTIHEAL_UNTIL = "EldenWorldM62AntiHealUntil";
    private static final String NBT_ANTIHEAL_PCT = "EldenWorldM62AntiHealPct";
    private static final String CTRL_ROOT = "EldenWorldM62Control";

    private static final String WORLD_DATA_NAME = "eldenworld_m62_world_progress";
    private static int CURRENT_TIER = 0;
    private static int CURRENT_WORLD_LEVEL = 0;
    private static long SERVER_TICKS = 0;
    private static ScalingConfig CONFIG = ScalingConfig.defaults();

    private WorldScalingEvents() {}

    private enum Profile { COMMON, ELITE, BOSS, BENCHMARK }

    @SubscribeEvent
    public static void addReloadListener(AddReloadListenerEvent event) {
        event.addListener(new ScalingReloadListener());
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        // Only scan connected players once per second. The result can only raise the
        // persistent world record; logging out can never lower World Tier.
        if ((++SERVER_TICKS % 20L) != 0L) return;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        WorldProgressData data = progress(server);
        int observedMax = data.maxLevelReached;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.isSpectator() || player.isRemoved()) continue;
            try {
                observedMax = Math.max(observedMax, Math.min(150, RequirementChecker.pstLevel(player)));
            } catch (RuntimeException ignored) {}
        }

        if (observedMax > data.maxLevelReached) {
            data.maxLevelReached = observedMax;
            data.setDirty();
        }
        CURRENT_WORLD_LEVEL = data.maxLevelReached;
        int next = CONFIG.tierForLevel(CURRENT_WORLD_LEVEL);
        if (next != CURRENT_TIER) {
            CURRENT_TIER = next;
            rescaleLoaded(server);
        }
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel) || !(event.getEntity() instanceof LivingEntity living)) return;
        if (!isScalable(living)) return;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            WorldProgressData data = progress(server);
            CURRENT_WORLD_LEVEL = data.maxLevelReached;
            CURRENT_TIER = CONFIG.tierForLevel(CURRENT_WORLD_LEVEL);
        }
        applyScaling(living, CURRENT_TIER, true);
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.getPersistentData().contains(NBT_TIER)) {
            int tier = victim.getPersistentData().getInt(NBT_TIER);
            Profile profile = profileFromNbt(victim);
            if ((profile == Profile.BOSS || profile == Profile.BENCHMARK) && event.getSource().is(DamageTypeTags.IS_PROJECTILE)) {
                double factor = bossExtraFactor(profile);
                double resist = CONFIG.bossProjectileResist[tier] * factor;
                event.setAmount((float)(event.getAmount() * Math.max(0.0, 1.0 - resist)));
            }
        }

        LivingEntity attacker = event.getSource().getEntity() instanceof LivingEntity le ? le : null;
        if (attacker == null || attacker == victim || !attacker.getPersistentData().contains(NBT_TIER)) return;
        int tier = attacker.getPersistentData().getInt(NBT_TIER);
        Profile profile = profileFromNbt(attacker);
        double mult = damageMultiplier(profile, tier);
        if (mult > 0) event.setAmount((float)(event.getAmount() * mult));

        if (victim instanceof ServerPlayer player && (profile == Profile.BOSS || profile == Profile.BENCHMARK)) {
            double anti = CONFIG.bossAntiHeal[tier] * bossExtraFactor(profile);
            if (anti > 0) {
                long now = player.level().getGameTime();
                player.getPersistentData().putLong(NBT_ANTIHEAL_UNTIL, now + 100L);
                player.getPersistentData().putDouble(NBT_ANTIHEAL_PCT, anti);
            }
        }
    }

    @SubscribeEvent
    public static void onHeal(LivingHealEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        CompoundTag tag = player.getPersistentData();
        long now = player.level().getGameTime();
        if (now <= tag.getLong(NBT_ANTIHEAL_UNTIL)) {
            double anti = Math.max(0.0, Math.min(0.95, tag.getDouble(NBT_ANTIHEAL_PCT)));
            event.setAmount((float)(event.getAmount() * (1.0 - anti)));
        }
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity living = event.getEntity();
        if (!living.getPersistentData().contains(NBT_TIER)) return;
        Profile profile = profileFromNbt(living);
        if (profile != Profile.BOSS && profile != Profile.BENCHMARK) return;
        int tier = living.getPersistentData().getInt(NBT_TIER);
        double baseResist = CONFIG.bossControlResist[tier] * bossExtraFactor(profile);
        if (baseResist <= 0 || living.getActiveEffects().isEmpty()) return;

        long now = living.level().getGameTime();
        CompoundTag ctrl = living.getPersistentData().getCompound(CTRL_ROOT);
        List<net.minecraft.world.effect.MobEffect> toRemove = new ArrayList<>();
        for (MobEffectInstance inst : living.getActiveEffects()) {
            ResourceLocation id = ForgeRegistries.MOB_EFFECTS.getKey(inst.getEffect());
            if (id == null || !isControl(id)) continue;
            String key = id.toString().replace(':', '_').replace('/', '_');
            CompoundTag state = ctrl.getCompound(key);
            int lastDur = state.getInt("lastDur");
            if (!state.contains("start") || inst.getDuration() > lastDur + 3) {
                int stacks = state.getLong("tenacityUntil") >= now ? Math.min(3, state.getInt("stacks") + (state.contains("start") ? 1 : 0)) : 0;
                state.putLong("start", now);
                state.putInt("orig", inst.getDuration());
                state.putInt("stacks", stacks);
                state.putLong("tenacityUntil", now + 200L);
            }
            state.putInt("lastDur", inst.getDuration());
            double resist = Math.min(0.90, baseResist + state.getInt("stacks") * 0.10);
            int allowed = Math.max(1, (int)Math.ceil(state.getInt("orig") * (1.0 - resist)));
            if (now - state.getLong("start") >= allowed) toRemove.add(inst.getEffect());
            ctrl.put(key, state);
        }
        living.getPersistentData().put(CTRL_ROOT, ctrl);
        for (var effect : toRemove) living.removeEffect(effect);
    }

    private static boolean isControl(ResourceLocation id) {
        String p = id.getPath().toLowerCase(Locale.ROOT);
        return p.contains("stun") || p.contains("frozen") || p.contains("freeze") || p.contains("snare")
                || p.contains("paral") || p.contains("incapac") || p.contains("strangle") || p.contains("rooted")
                || p.contains("gloam_grasp") || p.contains("psychic_control");
    }

    private static WorldProgressData progress(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                WorldProgressData::load, WorldProgressData::new, WORLD_DATA_NAME);
    }

    /**
     * Persistent, irreversible EldenWorld progression.
     * Stores the highest character level ever observed in this save.
     */
    private static final class WorldProgressData extends SavedData {
        private int maxLevelReached;

        private WorldProgressData() {
            this.maxLevelReached = 0;
        }

        private static WorldProgressData load(CompoundTag tag) {
            WorldProgressData data = new WorldProgressData();
            data.maxLevelReached = Math.max(0, Math.min(150, tag.getInt("MaxLevelReached")));
            return data;
        }

        @Override
        public CompoundTag save(CompoundTag tag) {
            tag.putInt("MaxLevelReached", Math.max(0, Math.min(150, maxLevelReached)));
            return tag;
        }
    }

    public static int currentWorldLevel() {
        return CURRENT_WORLD_LEVEL;
    }

    public static int currentTier() {
        return CURRENT_TIER;
    }

    private static void rescaleLoaded(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity e : level.getAllEntities()) {
                if (e instanceof LivingEntity living && isScalable(living)) applyScaling(living, CURRENT_TIER, false);
            }
        }
    }

    private static boolean isScalable(LivingEntity living) {
        if (living instanceof Player || living.getType().is(EXCLUDE)) return false;
        if (living instanceof OwnableEntity owned && owned.getOwnerUUID() != null) return false;
        return living instanceof Monster || living.getType().getCategory() == MobCategory.MONSTER;
    }

    private static void applyScaling(LivingEntity living, int tier, boolean spawned) {
        CompoundTag nbt = living.getPersistentData();
        double baseHp = nbt.contains(NBT_BASE_HP) ? nbt.getDouble(NBT_BASE_HP) : living.getMaxHealth();
        if (!nbt.contains(NBT_BASE_HP)) nbt.putDouble(NBT_BASE_HP, baseHp);
        Profile profile = classify(living, baseHp);
        nbt.putInt(NBT_TIER, tier);
        nbt.putString(NBT_PROFILE, profile.name());

        AttributeInstance hp = living.getAttribute(Attributes.MAX_HEALTH);
        AttributeInstance attack = living.getAttribute(Attributes.ATTACK_DAMAGE);
        AttributeInstance armor = living.getAttribute(Attributes.ARMOR);
        AttributeInstance kb = living.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        remove(hp, HP_ID); remove(attack, OLD_DAMAGE_ID); remove(armor, ARMOR_ID); remove(kb, KB_ID);
        AttributeInstance shred = attribute(living, ARMOR_SHRED); remove(shred, ARMOR_SHRED_ID);

        float oldMax = living.getMaxHealth();
        float ratio = oldMax <= 0 ? 1.0f : living.getHealth() / oldMax;
        double hpMult = hpMultiplier(profile, tier);
        if (hp != null && hpMult > 1.0) hp.addPermanentModifier(new AttributeModifier(HP_ID, "EldenWorld M6.2 Tier HP", hpMult - 1.0, AttributeModifier.Operation.MULTIPLY_TOTAL));

        if (profile == Profile.BOSS || profile == Profile.BENCHMARK) {
            double factor = bossExtraFactor(profile);
            double armorAdd = CONFIG.bossArmor[tier] * factor;
            double shredPct = CONFIG.bossArmorShred[tier] * factor;
            double kbPct = CONFIG.bossKbResist[tier] * factor;
            if (armor != null && armorAdd > 0) armor.addPermanentModifier(new AttributeModifier(ARMOR_ID, "EldenWorld M6.2 Boss Armor", armorAdd, AttributeModifier.Operation.ADDITION));
            if (shred != null && shredPct > 0) shred.addPermanentModifier(new AttributeModifier(ARMOR_SHRED_ID, "EldenWorld M6.2 Boss Armor Shred", shredPct, AttributeModifier.Operation.ADDITION));
            if (kb != null && kbPct > 0) kb.addPermanentModifier(new AttributeModifier(KB_ID, "EldenWorld M6.2 Boss Knockback Resist", kbPct, AttributeModifier.Operation.ADDITION));
        }

        float newMax = living.getMaxHealth();
        living.setHealth(spawned ? newMax : Math.max(1.0f, Math.min(newMax, newMax * ratio)));
    }

    private static Profile classify(LivingEntity living, double baseHp) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(living.getType());
        String ns = id == null ? "" : id.getNamespace().toLowerCase(Locale.ROOT);
        String path = id == null ? "" : id.getPath().toLowerCase(Locale.ROOT);
        if (living.getType().is(BENCHMARK_BOSS) || (ns.equals("traveloptics") && (baseHp >= 80 || path.contains("nightwarden")))) return Profile.BENCHMARK;
        boolean bossName = path.contains("boss") || path.contains("lord_") || path.contains("warden") || path.contains("leviathan")
                || path.contains("ignis") || path.contains("harbinger") || path.contains("dragon") || path.contains("pumpkinhead");
        if (living.getType().is(STANDARD_BOSS) || bossName || baseHp >= 150) return Profile.BOSS;
        if (living.getType().is(ELITE) || baseHp >= 60) return Profile.ELITE;
        return Profile.COMMON;
    }

    private static double hpMultiplier(Profile p, int tier) {
        return switch (p) {
            case COMMON -> CONFIG.commonHp[tier];
            case ELITE -> CONFIG.eliteHp[tier];
            case BOSS -> CONFIG.bossHp[tier];
            case BENCHMARK -> CONFIG.benchmarkHp[tier];
        };
    }

    private static double damageMultiplier(Profile p, int tier) {
        return switch (p) {
            case COMMON -> CONFIG.commonDamage[tier];
            case ELITE -> CONFIG.eliteDamage[tier];
            case BOSS -> CONFIG.bossDamage[tier];
            case BENCHMARK -> CONFIG.benchmarkDamage[tier];
        };
    }

    private static double bossExtraFactor(Profile p) { return p == Profile.BENCHMARK ? CONFIG.benchmarkExtraFactor : 1.0; }
    private static Profile profileFromNbt(LivingEntity e) {
        try { return Profile.valueOf(e.getPersistentData().getString(NBT_PROFILE)); }
        catch (Exception ignored) { return Profile.COMMON; }
    }

    private static AttributeInstance attribute(LivingEntity e, ResourceLocation id) {
        Attribute a = ForgeRegistries.ATTRIBUTES.getValue(id);
        return a == null ? null : e.getAttribute(a);
    }
    private static void remove(AttributeInstance a, UUID id) { if (a != null && a.getModifier(id) != null) a.removeModifier(id); }

    private static final class ScalingReloadListener extends SimpleJsonResourceReloadListener {
        private ScalingReloadListener() { super(new Gson(), "eldenworld_scaling/profiles"); }
        @Override protected void apply(Map<ResourceLocation, JsonElement> map, ResourceManager manager, ProfilerFiller profiler) {
            JsonElement el = map.get(new ResourceLocation("eldenworld_scaling", "default"));
            if (el != null && el.isJsonObject()) {
                try { CONFIG = ScalingConfig.from(el.getAsJsonObject()); }
                catch (RuntimeException ex) { CONFIG = ScalingConfig.defaults(); }
            } else CONFIG = ScalingConfig.defaults();
        }
    }

    private static final class ScalingConfig {
        final int[] thresholds;
        final double[] commonHp, commonDamage, eliteHp, eliteDamage, bossHp, bossDamage, benchmarkHp, benchmarkDamage;
        final double[] bossArmor, bossArmorShred, bossKbResist, bossControlResist, bossAntiHeal, bossProjectileResist;
        final double benchmarkExtraFactor;

        private ScalingConfig(int[] thresholds, double[] commonHp, double[] commonDamage, double[] eliteHp, double[] eliteDamage,
                              double[] bossHp, double[] bossDamage, double[] benchmarkHp, double[] benchmarkDamage,
                              double[] bossArmor, double[] bossArmorShred, double[] bossKbResist, double[] bossControlResist,
                              double[] bossAntiHeal, double[] bossProjectileResist, double benchmarkExtraFactor) {
            this.thresholds=thresholds; this.commonHp=commonHp; this.commonDamage=commonDamage; this.eliteHp=eliteHp; this.eliteDamage=eliteDamage;
            this.bossHp=bossHp; this.bossDamage=bossDamage; this.benchmarkHp=benchmarkHp; this.benchmarkDamage=benchmarkDamage;
            this.bossArmor=bossArmor; this.bossArmorShred=bossArmorShred; this.bossKbResist=bossKbResist; this.bossControlResist=bossControlResist;
            this.bossAntiHeal=bossAntiHeal; this.bossProjectileResist=bossProjectileResist; this.benchmarkExtraFactor=benchmarkExtraFactor;
        }
        int tierForLevel(int level) { int t=0; for(int i=1;i<thresholds.length;i++) if(level>=thresholds[i]) t=i; return Math.min(t, thresholds.length-1); }
        static ScalingConfig defaults() {
            int[] th={0,30,40,50,60,70,80,90,100,110,120,130,140,150};
            double[] ch={1,1.2,1.4,1.6,1.9,2.2,2.5,2.8,3.1,3.4,3.7,4.0,4.3,4.5};
            double[] cd={1,1.3,1.6,1.9,2.3,2.7,3.1,3.5,3.9,4.3,4.7,5.1,5.5,6.0};
            double[] eh={1,1.4,1.7,2.0,2.3,2.6,3.0,3.3,3.6,3.9,4.2,4.5,4.8,5.0};
            double[] ed={1,1.5,1.9,2.3,2.7,3.1,3.6,4.1,4.6,5.0,5.4,5.8,6.2,6.5};
            double[] bh={1,1.5,1.8,2.1,2.4,2.7,3.0,3.3,3.5,3.7,3.9,4.1,4.3,4.5};
            double[] bd={1,1.6,2.0,2.4,2.8,3.2,3.7,4.2,4.7,5.1,5.5,5.9,6.3,7.0};
            double[] xh={1,1.04,1.08,1.12,1.16,1.20,1.24,1.28,1.32,1.36,1.40,1.43,1.47,1.50};
            double[] xd={1,1.04,1.08,1.12,1.16,1.20,1.24,1.28,1.32,1.36,1.40,1.43,1.47,1.50};
            double[] ar={0,1,2,3,4,5,6,7,8,9,10,11,12,14};
            double[] sh={0,0,0,.05,.05,.10,.10,.12,.15,.17,.20,.22,.25,.30};
            double[] kb={0,.05,.10,.15,.20,.25,.30,.35,.40,.45,.50,.55,.60,.70};
            double[] cr={0,0,0,.10,.15,.20,.30,.35,.40,.45,.50,.55,.60,.65};
            double[] ah={0,0,0,0,0,0,.05,.05,.10,.10,.15,.15,.20,.20};
            double[] pr={0,0,0,0,0,0,0,0,.10,.10,.15,.15,.15,.20};
            return new ScalingConfig(th,ch,cd,eh,ed,bh,bd,xh,xd,ar,sh,kb,cr,ah,pr,.50);
        }
        static ScalingConfig from(JsonObject o) {
            ScalingConfig d=defaults();
            int[] th=intArray(o,"thresholds",d.thresholds);
            int n=th.length;
            return new ScalingConfig(th,
                    arr(o,"common_hp",d.commonHp,n),arr(o,"common_damage",d.commonDamage,n),arr(o,"elite_hp",d.eliteHp,n),arr(o,"elite_damage",d.eliteDamage,n),
                    arr(o,"boss_hp",d.bossHp,n),arr(o,"boss_damage",d.bossDamage,n),arr(o,"benchmark_hp",d.benchmarkHp,n),arr(o,"benchmark_damage",d.benchmarkDamage,n),
                    arr(o,"boss_armor",d.bossArmor,n),arr(o,"boss_armor_shred",d.bossArmorShred,n),arr(o,"boss_knockback_resist",d.bossKbResist,n),
                    arr(o,"boss_control_resist",d.bossControlResist,n),arr(o,"boss_antiheal",d.bossAntiHeal,n),arr(o,"boss_projectile_resist",d.bossProjectileResist,n),
                    o.has("benchmark_extra_factor")?o.get("benchmark_extra_factor").getAsDouble():d.benchmarkExtraFactor);
        }
        static double[] arr(JsonObject o,String key,double[] fallback,int n){ if(!o.has(key))return fallback; JsonArray a=o.getAsJsonArray(key); if(a.size()!=n)throw new IllegalArgumentException(key); double[] r=new double[n]; for(int i=0;i<n;i++)r[i]=a.get(i).getAsDouble(); return r; }
        static int[] intArray(JsonObject o,String key,int[] fallback){ if(!o.has(key))return fallback; JsonArray a=o.getAsJsonArray(key); int[] r=new int[a.size()]; for(int i=0;i<r.length;i++)r[i]=a.get(i).getAsInt(); return r; }
    }
}
