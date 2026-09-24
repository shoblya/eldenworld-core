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
import java.util.List; import java.util.Locale; import java.util.UUID;

@Mod.EventBusSubscriber(modid = EldenWorldCore.MOD_ID)
public final class WorldScalingEvents {
 private static final UUID HP_ID=UUID.fromString("d39cbdd2-54f4-4f62-ae92-c99b57ea1f50"), DAMAGE_ID=UUID.fromString("fb80b080-c08c-4a8d-b105-3319d2f5e02a"), ARMOR_ID=UUID.fromString("a6d87c9e-3490-4370-a9a7-fef9180e0cb3");
 private static final String LEVEL_TAG="eldenworld_m61_world_level"; private static int tickCounter; private static double cachedLevel=-1;
 private WorldScalingEvents(){}
 @SubscribeEvent public static void onJoin(EntityJoinLevelEvent e){if(!(e.getLevel() instanceof ServerLevel l)||!(e.getEntity() instanceof Monster m)||m.isNoAi())return;double wl=cachedLevel>=0?cachedLevel:globalWorldLevel(l.getServer());apply(m,wl);}
 @SubscribeEvent public static void onServerTick(TickEvent.ServerTickEvent e){if(e.phase!=TickEvent.Phase.END||++tickCounter<200)return;tickCounter=0;MinecraftServer s=e.getServer();double wl=globalWorldLevel(s);if(cachedLevel>=0&&Math.abs(cachedLevel-wl)<1.0)return;cachedLevel=wl;for(ServerLevel l:s.getAllLevels())for(Entity x:l.getAllEntities())if(x instanceof Monster m&&!m.isNoAi())apply(m,wl);}
 public static double globalWorldLevel(MinecraftServer s){if(s==null)return 0;List<ServerPlayer> ps=s.getPlayerList().getPlayers();if(ps.isEmpty())return 0;double sum=0;int n=0;for(ServerPlayer p:ps)try{sum+=Math.min(150,Math.max(1,RequirementChecker.pstLevel(p)));n++;}catch(RuntimeException ignored){}return n==0?0:sum/n;}
 private static void apply(Monster m,double level){double prev=m.getPersistentData().getDouble(LEVEL_TAG);if(prev>0&&Math.abs(prev-level)<1)return;float old=m.getMaxHealth(),ratio=old<=0?1:Math.max(0,Math.min(1,m.getHealth()/old));remove(m.getAttribute(Attributes.MAX_HEALTH),HP_ID);remove(m.getAttribute(Attributes.ATTACK_DAMAGE),DAMAGE_ID);remove(m.getAttribute(Attributes.ARMOR),ARMOR_ID);if(level>=10){double c=curve(level);Tier t=tier(m,m.getMaxHealth());mul(m.getAttribute(Attributes.MAX_HEALTH),HP_ID,"EldenWorld M6.1 world HP",c*t.hp);mul(m.getAttribute(Attributes.ATTACK_DAMAGE),DAMAGE_ID,"EldenWorld M6.1 world damage",c*t.damage);add(m.getAttribute(Attributes.ARMOR),ARMOR_ID,"EldenWorld M6.1 world armor",c*t.armor);}m.getPersistentData().putDouble(LEVEL_TAG,level);m.setHealth(Math.max(1,m.getMaxHealth()*ratio));}
 static double curve(double l){if(l<=10)return 0;if(l<=30)return .20*(l-10)/20;if(l<=60)return .20+.25*(l-30)/30;if(l<=90)return .45+.25*(l-60)/30;if(l<=120)return .70+.18*(l-90)/30;return Math.min(1,.88+.12*(l-120)/30);}
 static Tier tier(LivingEntity m,double hp){ResourceLocation id=ForgeRegistries.ENTITY_TYPES.getKey(m.getType());String ns=id==null?"":id.getNamespace().toLowerCase(Locale.ROOT),p=id==null?"":id.getPath().toLowerCase(Locale.ROOT);boolean boss=p.contains("boss")||p.contains("leviathan")||p.contains("ignis")||p.contains("harbinger")||p.contains("wither")||p.contains("warden")||p.contains("dragon")||p.contains("lich")||p.contains("hydra")||p.contains("naga")||p.contains("ur_ghast")||p.contains("snow_queen")||p.contains("ferrous_wroughtnaut")||p.contains("frostmaw");boolean bm=ns.contains("cataclysm")||ns.contains("mowzie")||ns.contains("aquamirae")||ns.contains("legendary")||ns.contains("block_factory")||ns.contains("twilightforest");boolean danger=ns.contains("born_in_chaos")||ns.contains("alexscaves")||ns.contains("iceandfire")||ns.contains("alexsmobs");if(hp>=180||boss||(bm&&hp>=100))return new Tier(.18,.10,2);if(hp>=80||bm)return new Tier(.30,.17,3);if(hp>=45||danger)return new Tier(.45,.24,4);return new Tier(.65,.32,6);}
 private static void remove(AttributeInstance a,UUID id){if(a!=null&&a.getModifier(id)!=null)a.removeModifier(id);} private static void mul(AttributeInstance a,UUID id,String n,double v){if(a!=null&&v>0)a.addPermanentModifier(new AttributeModifier(id,n,v,AttributeModifier.Operation.MULTIPLY_BASE));} private static void add(AttributeInstance a,UUID id,String n,double v){if(a!=null&&v>0)a.addPermanentModifier(new AttributeModifier(id,n,v,AttributeModifier.Operation.ADDITION));}
 record Tier(double hp,double damage,double armor){}
}
