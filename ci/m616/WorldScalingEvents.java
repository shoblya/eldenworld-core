package com.eldenworld.core.balance;

import com.eldenworld.core.EldenWorldCore;
import com.eldenworld.core.requirements.RequirementChecker;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.Locale;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = EldenWorldCore.MOD_ID)
public final class WorldScalingEvents {
    private static final UUID HP_ID=UUID.fromString("d39cbdd2-54f4-4f62-ae92-c99b57ea1f50"),
        DAMAGE_ID=UUID.fromString("fb80b080-c08c-4a8d-b105-3319d2f5e02a"),
        ARMOR_ID=UUID.fromString("a6d87c9e-3490-4370-a9a7-fef9180e0cb3");
    private static final TagKey<EntityType<?>> HOSTILE = TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("eldenworld_core", "hostile_mobs"));
    private static final TagKey<EntityType<?>> EXCLUDED = TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("eldenworld_core", "scaling_excluded"));
    private static final String LEVEL="eldenworld_m616_level", SEEN="eldenworld_m616_hostile", DAMAGE="eldenworld_m616_damage";
    private static int ticks;
    private static double cachedLevel = -1;
    private WorldScalingEvents() {}

    private static boolean excluded(LivingEntity e) {
        return e instanceof Player || (e instanceof OwnableEntity owned && owned.getOwnerUUID()!=null)
            || e.getType().is(EXCLUDED) || (e instanceof Mob mob && mob.isNoAi());
    }
    private static boolean hostile(LivingEntity e) {
        return !excluded(e) && (e instanceof Enemy || e.getType().getCategory()==MobCategory.MONSTER
            || e.getType().is(HOSTILE) || e.getPersistentData().getBoolean(SEEN)
            || (e instanceof Mob mob && mob.getTarget() instanceof Player));
    }
    private static double level(ServerLevel l) {
        if(cachedLevel<0) cachedLevel=globalWorldLevel(l.getServer());
        return cachedLevel;
    }
    @SubscribeEvent public static void onJoin(EntityJoinLevelEvent e) {
        if(e.getLevel() instanceof ServerLevel l && e.getEntity() instanceof LivingEntity mob)
            apply(mob, level(l), true);
    }
    @SubscribeEvent public static void onTarget(LivingChangeTargetEvent e) {
        if(e.getNewTarget() instanceof Player && e.getEntity().level() instanceof ServerLevel l && !excluded(e.getEntity())) {
            e.getEntity().getPersistentData().putBoolean(SEEN,true);
            apply(e.getEntity(),level(l),false);
        }
    }
    // Scale the actual hit, including projectiles and magic with a living source.
    // Remove the old attack-attribute modifier so upgraded worlds never double-scale melee.
    @SubscribeEvent(priority=EventPriority.HIGH) public static void onHurt(LivingHurtEvent e) {
        if(!(e.getEntity().level() instanceof ServerLevel l)) return;
        if(e.getSource().getEntity() instanceof LivingEntity attacker && !excluded(attacker)) {
            if(e.getEntity() instanceof Player) attacker.getPersistentData().putBoolean(SEEN,true);
            apply(attacker,level(l),false);
            if(hostile(attacker)) e.setAmount(e.getAmount()*(1+(float)attacker.getPersistentData().getDouble(DAMAGE)));
        }
    }
    @SubscribeEvent public static void onTick(TickEvent.ServerTickEvent e) {
        if(e.phase!=TickEvent.Phase.END || ++ticks<200) return;
        ticks=0; cachedLevel=globalWorldLevel(e.getServer());
        // Also discovers changed hostility, ownership and entities loaded at the same world level.
        for(ServerLevel l:e.getServer().getAllLevels()) for(Entity x:l.getAllEntities())
            if(x instanceof LivingEntity mob) apply(mob,cachedLevel,false);
    }
    @SubscribeEvent public static void onStop(ServerStoppedEvent e) { ticks=0; cachedLevel=-1; }
    public static double globalWorldLevel(MinecraftServer server) {
        double sum=0; int count=0;
        for(ServerPlayer p:server.getPlayerList().getPlayers()) {
            if(p.isSpectator()) continue;
            try { sum+=Math.min(150,Math.max(1,RequirementChecker.pstLevel(p))); count++; }
            catch(RuntimeException ignored) {}
        }
        return count==0?0:sum/count;
    }
    private static void apply(LivingEntity e,double level,boolean force) {
        boolean active=hostile(e);
        var data=e.getPersistentData();
        double target=active?level:0;
        if(!force && data.contains(LEVEL) && Math.abs(data.getDouble(LEVEL)-target)<0.001) return;
        AttributeInstance hp=e.getAttribute(Attributes.MAX_HEALTH), armor=e.getAttribute(Attributes.ARMOR);
        boolean had=(hp!=null&&hp.getModifier(HP_ID)!=null)||(armor!=null&&armor.getModifier(ARMOR_ID)!=null);
        if(!active&&!had&&!data.contains(LEVEL)) return;
        float ratio=e.getMaxHealth()>0?e.getHealth()/e.getMaxHealth():1;
        remove(hp,HP_ID); remove(armor,ARMOR_ID); remove(e.getAttribute(Attributes.ATTACK_DAMAGE),DAMAGE_ID);
        Tier tier=tier(e,e.getMaxHealth()); double c=active?curve(level):0;
        add(hp,HP_ID,"EldenWorld world health",c*tier.hp,AttributeModifier.Operation.MULTIPLY_BASE);
        add(armor,ARMOR_ID,"EldenWorld world armor",c*tier.armor,AttributeModifier.Operation.ADDITION);
        data.putDouble(LEVEL,target); data.putDouble(DAMAGE,c*tier.damage);
        if(e.isAlive()) e.setHealth(Math.min(e.getMaxHealth(),Math.max(0,e.getMaxHealth()*ratio)));
    }
    static double curve(double l) {
        if(l<=10)return 0;if(l<=30)return .20*(l-10)/20;if(l<=60)return .20+.25*(l-30)/30;
        if(l<=90)return .45+.25*(l-60)/30;if(l<=120)return .70+.18*(l-90)/30;
        return Math.min(1,.88+.12*(l-120)/30);
    }
 static Tier tier(LivingEntity m,double hp){ResourceLocation id=ForgeRegistries.ENTITY_TYPES.getKey(m.getType());String ns=id==null?"":id.getNamespace().toLowerCase(Locale.ROOT),p=id==null?"":id.getPath().toLowerCase(Locale.ROOT);boolean boss=p.contains("boss")||p.contains("leviathan")||p.contains("ignis")||p.contains("harbinger")||p.contains("wither")||p.contains("warden")||p.contains("dragon")||p.contains("lich")||p.contains("hydra")||p.contains("naga")||p.contains("ur_ghast")||p.contains("snow_queen")||p.contains("ferrous_wroughtnaut")||p.contains("frostmaw");boolean bm=ns.contains("cataclysm")||ns.contains("mowzie")||ns.contains("aquamirae")||ns.contains("legendary")||ns.contains("block_factory")||ns.contains("twilightforest");boolean danger=ns.contains("born_in_chaos")||ns.contains("alexscaves")||ns.contains("iceandfire")||ns.contains("alexsmobs");if(hp>=180||boss||(bm&&hp>=100))return new Tier(.60,.35,5);if(hp>=80||bm)return new Tier(.85,.45,7);if(hp>=45||danger)return new Tier(1.10,.60,8);return new Tier(1.50,.75,10);}

    private static void remove(AttributeInstance a,UUID id) { if(a!=null&&a.getModifier(id)!=null)a.removeModifier(id); }
    private static void add(AttributeInstance a,UUID id,String name,double v,AttributeModifier.Operation op) {
        if(a!=null&&v>0)a.addPermanentModifier(new AttributeModifier(id,name,v,op));
    }
    record Tier(double hp,double damage,double armor) {}
}
