package com.eldenworld.core.specialization;

import com.eldenworld.core.EldenWorldCore;
import com.eldenworld.core.abilities.AbilityState;
import com.eldenworld.core.abilities.AbilityUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Method;
import java.util.*;

@Mod.EventBusSubscriber(modid = EldenWorldCore.MOD_ID)
public final class FinalSpecializationEventsV2 {
    private FinalSpecializationEventsV2() {}
    private static final TagKey<Item> SS_LIGHT = TagKey.create(Registries.ITEM, id("simplyswords:light_weapons"));
    private static final TagKey<Item> SS_HEAVY = TagKey.create(Registries.ITEM, id("simplyswords:heavy_weapons"));
    private static final TagKey<Item> SS_RUNIC = TagKey.create(Registries.ITEM, id("simplyswords:runic_weapons"));
    private static final String LAST_TARGET="m62_target", STORM="m62_storm", HUNTER_TARGET="m62_hunter_target", GALE="m62_gale", LAST_MOVE="m62_last_move";
    private static final Map<UUID, double[]> LAST_POS = new HashMap<>();

    private static ResourceLocation id(String s){ return new ResourceLocation(s); }
    private static boolean h(ServerPlayer p,String x){ return AbilityUtil.has(p,id("eldenworld:"+x)); }
    private static boolean m(ServerPlayer p,String base){ return h(p,base+"/mastery"); }
    private static long now(ServerPlayer p){ return p.level().getGameTime(); }
    private static boolean cd(ServerPlayer p,String key,long ticks){ long n=now(p); if(!AbilityState.cooldownReady(p,key,n))return false; AbilityState.setCooldown(p,key,n,ticks); return true; }
    private static String path(ItemStack s){ ResourceLocation k=ForgeRegistries.ITEMS.getKey(s.getItem()); return k==null?"":k.getPath().toLowerCase(Locale.ROOT); }
    private static String ns(ItemStack s){ ResourceLocation k=ForgeRegistries.ITEMS.getKey(s.getItem()); return k==null?"":k.getNamespace().toLowerCase(Locale.ROOT); }
    private static boolean any(String s,String... xs){ for(String x:xs) if(s.contains(x)) return true; return false; }
    private static boolean projectile(LivingHurtEvent e){ return e.getSource().getDirectEntity() instanceof Projectile; }
    private static boolean shield(ServerPlayer p){ return p.getMainHandItem().getItem() instanceof ShieldItem || p.getOffhandItem().getItem() instanceof ShieldItem; }
    private static boolean light(ItemStack s){ return s.is(SS_LIGHT) || any(path(s),"sai","rapier","katana","cutlass","twinblade","warglaive"); }
    private static boolean heavy(ItemStack s){ return s.is(SS_HEAVY) || any(path(s),"claymore","greataxe","greathammer","great_hammer","halberd"); }
    private static boolean scythe(ItemStack s){ return path(s).contains("scythe"); }
    private static boolean redstone(net.minecraft.world.level.block.state.BlockState s){ return s.is(Blocks.REDSTONE_WIRE)||s.is(Blocks.REPEATER)||s.is(Blocks.COMPARATOR)||s.is(Blocks.PISTON)||s.is(Blocks.STICKY_PISTON)||s.is(Blocks.OBSERVER)||s.is(Blocks.DISPENSER)||s.is(Blocks.DROPPER)||s.is(Blocks.HOPPER); }
    private static void effect(LivingEntity e,String rid,int sec,int amp){ MobEffect fx=ForgeRegistries.MOB_EFFECTS.getValue(id(rid)); if(fx!=null)e.addEffect(new MobEffectInstance(fx,sec*20,amp,true,false,true)); }
    private static void vanilla(LivingEntity e,MobEffect fx,int sec,int amp){ e.addEffect(new MobEffectInstance(fx,sec*20,amp,true,false,true)); }
    private static boolean behind(ServerPlayer a,LivingEntity t){ var v=a.position().subtract(t.position()).normalize(); return t.getLookAngle().dot(v)<-0.35; }
    private static boolean boss(LivingEntity e){ ResourceLocation k=ForgeRegistries.ENTITY_TYPES.getKey(e.getType()); if(k==null)return false; String n=k.getNamespace(),p=k.getPath(); return any(n,"cataclysm","mowzie","twilightforest","aquamirae","alexscaves","legendary") && (e.getMaxHealth()>=80 || any(p,"boss","ignis","leviathan","harbinger","lich","hydra","naga","ur_ghast","snow_queen","frostmaw","wroughtnaut")); }

    @SubscribeEvent public static void onHurt(LivingHurtEvent e){
        Entity src=e.getSource().getEntity();
        if(src instanceof ServerPlayer a){
            LivingEntity t=e.getEntity(); ItemStack w=a.getMainHandItem(); float x=e.getAmount(); boolean pr=projectile(e);
            // ROGUE — Simply Swords implicits + Iron/Ars effects.
            if(h(a,"shadowstep/nightblade/specialization")&&!pr&&light(w)&&cd(a,"m62_night",80)){ effect(a,"irons_spellbooks:hastened",m(a,"shadowstep/nightblade")?4:2,0); if(m(a,"shadowstep/nightblade"))effect(t,"irons_spellbooks:rend",3,0); }
            if(h(a,"ghost/assassin/specialization")&&!pr&&light(w)&&behind(a,t)){ effect(t,"irons_spellbooks:heartstop",m(a,"ghost/assassin")?5:3,0); if(m(a,"ghost/assassin"))effect(t,"irons_spellbooks:rend",4,0); }
            if(h(a,"ghost/specter/specialization")&&!pr&&scythe(w)){ effect(t,"irons_spellbooks:soul_burn",m(a,"ghost/specter")?6:4,0); if(m(a,"ghost/specter"))effect(t,"irons_spellbooks:blight",3,0); }
            if(h(a,"opportunist/duelist/specialization")&&!pr&&a.getOffhandItem().isEmpty()&&any(path(w),"rapier","katana","cutlass")){ effect(a,"irons_spellbooks:hastened",2,0); if(m(a,"opportunist/duelist"))effect(a,"irons_spellbooks:evasion",2,0); }
            if(h(a,"opportunist/predator/specialization")){ if(t.getHealth()<=t.getMaxHealth()*.5f)effect(t,"irons_spellbooks:rend",3,t.getHealth()<=t.getMaxHealth()*.2f&&m(a,"opportunist/predator")?1:0); else if(m(a,"opportunist/predator")&&t.getHealth()>=t.getMaxHealth()*.7f)x*=.88f; if(m(a,"opportunist/predator")&&t.getHealth()<=t.getMaxHealth()*.2f)effect(t,"irons_spellbooks:heartstop",3,0); }
            if(h(a,"opportunist/trickster/specialization")&&cd(a,"m62_trick",120)){ effect(t,"ars_nouveau:snared",m(a,"opportunist/trickster")?4:2,0); if(m(a,"opportunist/trickster"))effect(t,"ars_nouveau:hex",3,0); }
            // WARRIOR — heavy weapon tags and Apothic effects.
            if(h(a,"juggernaut/berserker/specialization")&&!pr&&a.getHealth()<=a.getMaxHealth()*.4f){ x*=m(a,"juggernaut/berserker")?1.28f:1.15f; effect(a,"irons_spellbooks:vigor",2,m(a,"juggernaut/berserker")?1:0); }
            if(h(a,"juggernaut/colossus/specialization")&&!pr&&heavy(w)){ x*=m(a,"juggernaut/colossus")?1.30f:1.18f; effect(t,"attributeslib:sundering",m(a,"juggernaut/colossus")?4:3,m(a,"juggernaut/colossus")?1:0); }
            if(h(a,"unyielding/thorned_guard/specialization")&&!pr){ /* retaliation is handled on defender below */ }
            if(h(a,"second_wind/ironheart/specialization")&&!pr&&weaponLevel(w)>0){ effect(a,"irons_spellbooks:fortify",2,m(a,"second_wind/ironheart")?1:0); if(m(a,"second_wind/ironheart"))effect(t,"irons_spellbooks:rend",3,0); }
            // RANGER.
            if(pr&&h(a,"keen_instinct/marksman/specialization")){ double d=a.distanceTo(t); if(d>=12){x*=d>=18&&m(a,"keen_instinct/marksman")?1.25f:1.12f; effect(t,"irons_spellbooks:guided",3,0); if(d>=18&&m(a,"keen_instinct/marksman"))effect(t,"irons_spellbooks:rend",3,0);} else if(d<=5&&m(a,"keen_instinct/marksman"))x*=.80f; }
            if(pr&&h(a,"keen_instinct/hunter/specialization")&&t instanceof Monster){ String uid=t.getUUID().toString(); String old=AbilityState.getString(a,HUNTER_TARGET); if(!uid.equals(old)){AbilityState.setString(a,HUNTER_TARGET,uid);AbilityState.setCooldown(a,"m62_hunter_lock",now(a),80);} vanilla(t,MobEffects.GLOWING,5,0); if(uid.equals(old)&&AbilityState.cooldownReady(a,"m62_hunter_lock",now(a)))x*=m(a,"keen_instinct/hunter")?1.22f:1.10f; if(m(a,"keen_instinct/hunter"))effect(t,"irons_spellbooks:rend",3,0); }
            if(pr&&h(a,"keen_instinct/sentinel/specialization")&&AbilityState.getInt(a,"m62_still")>=40){ x*=m(a,"keen_instinct/sentinel")?1.28f:1.15f; effect(a,"irons_spellbooks:fortify",3,m(a,"keen_instinct/sentinel")?1:0); AbilityState.setInt(a,"m62_still",0); }
            if(pr&&h(a,"windrunner/gale_dancer/specialization")&&now(a)<=AbilityState.getLong(a,GALE)){ x*=m(a,"windrunner/gale_dancer")?1.30f:1.18f; effect(a,"irons_spellbooks:hastened",3,0); if(m(a,"windrunner/gale_dancer"))effect(t,"irons_spellbooks:airborne",2,0); AbilityState.setLong(a,GALE,0); }
            if(pr&&h(a,"windrunner/stormshot/specialization")){ int c=AbilityState.getInt(a,STORM)+1; if(c>=3){c=0;x*=m(a,"windrunner/stormshot")?1.35f:1.20f;effect(t,"irons_spellbooks:charged",4,0);if(m(a,"windrunner/stormshot"))effect(t,"irons_spellbooks:volt_strike",3,0);}AbilityState.setInt(a,STORM,c); }
            if(pr&&h(a,"windrunner/skirmisher/specialization")&&a.isSprinting()){ x*=m(a,"windrunner/skirmisher")?1.25f:1.15f; effect(a,"irons_spellbooks:hastened",2,0); if(m(a,"windrunner/skirmisher"))effect(a,"irons_spellbooks:evasion",2,0); }
            if(h(a,"pathfinder/dragon_rider/specialization")&&isIceFireMount(a.getVehicle()))x*=m(a,"pathfinder/dragon_rider")?1.28f:1.15f; else if(m(a,"pathfinder/dragon_rider"))x*=.88f;
            // ADVENTURER combat.
            if(h(a,"survivor/monster_slayer/specialization")){ if(boss(t)){x*=m(a,"survivor/monster_slayer")?1.32f:1.18f;effect(t,"irons_spellbooks:rend",3,0);if(m(a,"survivor/monster_slayer"))effect(t,"attributeslib:sundering",3,0);} else if(m(a,"survivor/monster_slayer"))x*=.88f; }
            if(h(a,"treasure_hunter/relic_seeker/specialization")){ int n=Math.min(m(a,"treasure_hunter/relic_seeker")?5:3,countRelics(a)); if(n>0)x*=1f+.03f*n; }
            if(m(a,"treasure_hunter/archaeologist")&&t instanceof Monster)x*=.92f;
            e.setAmount(x);
        }
        if(e.getEntity() instanceof ServerPlayer d){
            float x=e.getAmount(); Entity attacker=e.getSource().getEntity();
            if(h(d,"shadowstep/riftwalker/specialization")&&x>=4&&cd(d,"m62_rift",m(d,"shadowstep/riftwalker")?120:160)){ AbilityUtil.tryShadowstep(d,m(d,"shadowstep/riftwalker")?5:3); if(m(d,"shadowstep/riftwalker"))effect(d,"irons_spellbooks:true_invisibility",2,0); }
            if(h(d,"shadowstep/mirage/specialization")&&cd(d,"m62_mirage",m(d,"shadowstep/mirage")?180:240)){ spawnDecoys(d,m(d,"shadowstep/mirage")?2:1,120); }
            if(h(d,"ghost/phantom/specialization")&&cd(d,"m62_phantom",160)){ effect(d,"irons_spellbooks:evasion",m(d,"ghost/phantom")?5:3,0); if(m(d,"ghost/phantom"))effect(d,"irons_spellbooks:true_invisibility",2,0); }
            if(h(d,"juggernaut/bloodguard/specialization")&&attacker instanceof LivingEntity&&cd(d,"m62_bloodguard",160)){ effect(d,"irons_spellbooks:fortify",m(d,"juggernaut/bloodguard")?5:4,m(d,"juggernaut/bloodguard")?1:0); }
            if(h(d,"unyielding/bulwark/specialization")&&shield(d)){x*=m(d,"unyielding/bulwark")?.78f:.88f;effect(d,"irons_spellbooks:fortify",2,m(d,"unyielding/bulwark")?1:0);}
            if(h(d,"unyielding/thorned_guard/specialization")&&attacker instanceof LivingEntity le){effect(le,"attributeslib:bleeding",m(d,"unyielding/thorned_guard")?4:3,m(d,"unyielding/thorned_guard")?1:0);if(m(d,"unyielding/thorned_guard"))effect(le,"attributeslib:sundering",3,0);}
            if(h(d,"unyielding/spellguard/specialization")&&AbilityUtil.isMagicLike(e.getSource())){x*=m(d,"unyielding/spellguard")?.70f:.82f;effect(d,"irons_spellbooks:fortify",3,m(d,"unyielding/spellguard")?1:0);}
            if(h(d,"manaflow/channeler/specialization")&&m(d,"manaflow/channeler")&&x>=d.getMaxHealth()*.15f)effect(d,"irons_spellbooks:slowed",4,0);
            if(h(d,"arcane_ward/spellbreaker/specialization")&&AbilityUtil.isMagicLike(e.getSource())&&cd(d,"m62_spellbreaker",200)){effect(d,"irons_spellbooks:evasion",m(d,"arcane_ward/spellbreaker")?5:3,0);if(m(d,"arcane_ward/spellbreaker"))effect(d,"irons_spellbooks:fortify",5,0);}
            if(h(d,"arcane_ward/runewarden/specialization")&&cd(d,"m62_runeward",200)){effect(d,"ars_nouveau:shielding",5,m(d,"arcane_ward/runewarden")?1:0);if(m(d,"arcane_ward/runewarden"))effect(d,"ars_nouveau:mana_regen",5,0);}
            if(h(d,"survivor/last_stand/specialization")&&d.getHealth()<=d.getMaxHealth()*.25f){effect(d,"irons_spellbooks:fortify",3,m(d,"survivor/last_stand")?1:0);effect(d,"irons_spellbooks:vigor",3,m(d,"survivor/last_stand")?1:0);}
            if(h(d,"survivor/wastelander/specialization")&&attacker==null){x*=m(d,"survivor/wastelander")?.65f:.80f;effect(d,"irons_spellbooks:fortify",3,0);if(m(d,"survivor/wastelander"))vanilla(d,MobEffects.FIRE_RESISTANCE,4,0);}
            if(h(d,"arcane_ward/aegis/specialization")&&AbilityUtil.isMagicLike(e.getSource())) applyAegis(d,e.getSource().typeHolder().unwrapKey().map(k->k.location().getPath()).orElse(""));
            e.setAmount(x);
        }
        // Beastmaster uses the standard OwnableEntity contract, so Doggy Talents and vanilla pets work without hard dependency.
        if(src instanceof OwnableEntity pet && pet.getOwner() instanceof ServerPlayer o && h(o,"pathfinder/beastmaster/specialization")) e.setAmount(e.getAmount()*(m(o,"pathfinder/beastmaster")?1.45f:1.25f));
        if(e.getEntity() instanceof OwnableEntity pet && pet.getOwner() instanceof ServerPlayer o && h(o,"pathfinder/beastmaster/specialization")) e.setAmount(e.getAmount()*(m(o,"pathfinder/beastmaster")?.75f:.85f));
    }

    @SubscribeEvent public static void lethal(LivingDamageEvent e){
        if(!(e.getEntity() instanceof ServerPlayer p)||!h(p,"second_wind/revenant/specialization")||e.getAmount()<p.getHealth())return;
        long n=now(p); String k="m62_revenant"; if(!AbilityState.cooldownReady(p,k,n))return; boolean mm=m(p,"second_wind/revenant"); AbilityState.setCooldown(p,k,n,mm?1800:2400); e.setAmount(Math.max(0,p.getHealth()-1)); vanilla(p,MobEffects.REGENERATION,5,1); if(mm){effect(p,"irons_spellbooks:vigor",8,0);vanilla(p,MobEffects.WEAKNESS,12,1);effect(p,"attributeslib:grievous",20,0);} }

    @SubscribeEvent public static void death(LivingDeathEvent e){
        if(!(e.getSource().getEntity() instanceof ServerPlayer p))return;
        if(h(p,"wayfarer/pilgrim/specialization")){ vanilla(p,MobEffects.MOVEMENT_SPEED,5,m(p,"wayfarer/pilgrim")?1:0); }
        if(h(p,"treasure_hunter/fortune_hunter/specialization")&&p.getRandom().nextFloat()<(m(p,"treasure_hunter/fortune_hunter")?.35f:.20f))effect(p,"ars_nouveau:magic_find",8,m(p,"treasure_hunter/fortune_hunter")?1:0);
    }

    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e){
        if(!(e.getEntity() instanceof ServerPlayer p))return;
        if(h(p,"pathfinder/trailblazer/specialization")){effect(p,"irons_spellbooks:hastened",20,0);vanilla(p,MobEffects.DAMAGE_RESISTANCE,20,0);if(m(p,"pathfinder/trailblazer")){effect(p,"irons_spellbooks:planar_sight",20,0);vanilla(p,MobEffects.WEAKNESS,5,0);}}
        if(h(p,"wayfarer/dimension_walker/specialization")){effect(p,"irons_spellbooks:planar_sight",m(p,"wayfarer/dimension_walker")?25:15,0);effect(p,"irons_spellbooks:hastened",m(p,"wayfarer/dimension_walker")?25:15,0);if(m(p,"wayfarer/dimension_walker")){effect(p,"irons_spellbooks:evasion",5,0);vanilla(p,MobEffects.MOVEMENT_SLOWDOWN,3,0);}}
    }

    @SubscribeEvent public static void place(BlockEvent.EntityPlaceEvent e){
        if(!(e.getEntity() instanceof ServerPlayer p))return;
        if(h(p,"master_builder/architect/specialization")&&cd(p,"m62_architect",60)){effect(p,"ars_nouveau:scrying",4,0);vanilla(p,MobEffects.DIG_SPEED,4,0);if(m(p,"master_builder/architect"))effect(p,"ars_nouveau:shielding",4,0);}
        if(h(p,"master_builder/engineer/specialization")&&redstone(e.getPlacedBlock())){vanilla(p,MobEffects.DIG_SPEED,5,0);effect(p,"ars_nouveau:mana_regen",5,0);if(m(p,"master_builder/engineer"))effect(p,"ars_nouveau:shielding",5,0);}
        if(h(p,"master_builder/fortifier/specialization")){effect(p,"irons_spellbooks:fortify",m(p,"master_builder/fortifier")?5:4,m(p,"master_builder/fortifier")?1:0);}
    }

    @SubscribeEvent public static void breakBlock(BlockEvent.BreakEvent e){
        if(!(e.getPlayer() instanceof ServerPlayer p)||!e.getState().is(Tags.Blocks.ORES))return; ItemStack tool=p.getMainHandItem();
        if(h(p,"enduring_tools/smith/specialization")&&tool.isDamageableItem()){tool.setDamageValue(Math.max(0,tool.getDamageValue()-(m(p,"enduring_tools/smith")?5:2)));vanilla(p,MobEffects.DIG_SPEED,3,0);if(m(p,"enduring_tools/smith"))effect(p,"irons_spellbooks:fortify",3,0);}
        if(h(p,"prospector/gem_hunter/specialization"))effect(p,"ars_nouveau:magic_find",m(p,"prospector/gem_hunter")?10:8,m(p,"prospector/gem_hunter")?1:0);
        if(h(p,"prospector/excavator/specialization")){vanilla(p,MobEffects.DIG_SPEED,4,m(p,"prospector/excavator")?2:1); AbilityState.setLong(p,"m62_excavate",now(p)+80);}
    }

    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent e){
        if(e.phase!=TickEvent.Phase.END||!(e.player instanceof ServerPlayer p)||!p.isAlive()||p.isRemoved()||p.tickCount%10!=0)return; long n=now(p);
        // Sentinel stationary preparation.
        double[] old=LAST_POS.get(p.getUUID()); double[] cur={p.getX(),p.getY(),p.getZ()}; if(old!=null&&dist2(old,cur)<.01)AbilityState.setInt(p,"m62_still",Math.min(60,AbilityState.getInt(p,"m62_still")+10));else AbilityState.setInt(p,"m62_still",0); LAST_POS.put(p.getUUID(),cur);
        // Vanguard: actual MCA relationship hearts, reflection keeps MCA optional at compile time.
        if(h(p,"second_wind/vanguard/specialization")){ LivingEntity ally=findFriendlyMca(p); if(ally!=null){vanilla(p,MobEffects.DAMAGE_RESISTANCE,2,0);if(m(p,"second_wind/vanguard")){effect(p,"irons_spellbooks:vigor",2,0);effect(ally,"irons_spellbooks:fortify",2,0);}} }
        // Mounted Ice & Fire identity.
        if(h(p,"pathfinder/dragon_rider/specialization")&&isIceFireMount(p.getVehicle())){vanilla(p,MobEffects.FIRE_RESISTANCE,2,0);if(m(p,"pathfinder/dragon_rider"))effect(p,"irons_spellbooks:fortify",2,0);}
        // Builder/miner dynamic branches.
        if(h(p,"enduring_tools/temperer/specialization")){double ratio=durability(p.getMainHandItem());setAttr(p,"attributeslib:mining_speed","temper",ratio>=.8?(m(p,"enduring_tools/temperer")?.25:.12):(m(p,"enduring_tools/temperer")&&ratio>=0&&ratio<.25?-.15:0));}
        if(h(p,"prospector/deep_delver/specialization")){if(p.getY()<32){vanilla(p,MobEffects.DIG_SPEED,2,p.getY()<0&&m(p,"prospector/deep_delver")?1:0);vanilla(p,MobEffects.DAMAGE_RESISTANCE,2,0);if(p.getY()<0&&m(p,"prospector/deep_delver"))effect(p,"irons_spellbooks:fortify",2,0);}else if(p.getY()>80&&m(p,"prospector/deep_delver"))vanilla(p,MobEffects.MOVEMENT_SLOWDOWN,2,0);}
        setAttr(p,"irons_spellbooks:nature_spell_power","excavate",n<=AbilityState.getLong(p,"m62_excavate")?(m(p,"prospector/excavator")?.20:.10):0);
        // Pilgrim: actual continuous movement, reset by standing still.
        if(h(p,"wayfarer/pilgrim/specialization")){int mv=AbilityState.getInt(p,"m62_pilgrim"); if(old!=null&&dist2(old,cur)>.04)mv=Math.min(80,mv+10);else mv=Math.max(0,mv-20);AbilityState.setInt(p,"m62_pilgrim",mv);if(mv>=60){vanilla(p,MobEffects.MOVEMENT_SPEED,2,0);effect(p,"irons_spellbooks:vigor",2,0);if(m(p,"wayfarer/pilgrim"))vanilla(p,MobEffects.REGENERATION,2,0);}else if(m(p,"wayfarer/pilgrim")&&mv==0)vanilla(p,MobEffects.WEAKNESS,2,0);}
        // Cartographer / Archaeologist: first-seen biome is persistent per player.
        ResourceLocation biome=p.serverLevel().registryAccess().registryOrThrow(Registries.BIOME).getKey(p.serverLevel().getBiome(p.blockPosition()).value()); if(biome!=null){String b=biome.toString(); if(h(p,"wayfarer/cartographer/specialization")&&AbilityState.markOnce(p,"m62_cartographer",b)){effect(p,"ars_nouveau:scrying",8,m(p,"wayfarer/cartographer")?1:0);vanilla(p,MobEffects.LUCK,8,m(p,"wayfarer/cartographer")?1:0);} if(h(p,"treasure_hunter/archaeologist/specialization")&&AbilityState.markOnce(p,"m62_archaeologist",b)){effect(p,"ars_nouveau:scrying",10,0);vanilla(p,MobEffects.LUCK,10,1);if(m(p,"treasure_hunter/archaeologist"))vanilla(p,MobEffects.DAMAGE_RESISTANCE,8,0);} }
        // Relic Seeker reads actual Curios equipment through the installed Curios API.
        if(h(p,"treasure_hunter/relic_seeker/specialization")){int c=Math.min(m(p,"treasure_hunter/relic_seeker")?5:3,countRelics(p)); setAttr(p,"minecraft:generic.luck","relic_luck",.20*c);}
        // Timed school attunements must actually expire instead of becoming permanent hidden modifiers.
        if(h(p,"archmage/elementalist/specialization")&&n>AbilityState.getLong(p,"m62_element_until")){for(String x:new String[]{"fire","ice","lightning","nature"})setAttr(p,"irons_spellbooks:"+x+"_spell_power","element_"+x,0);}
        if(h(p,"arcane_ward/aegis/specialization")&&n>AbilityState.getLong(p,"m62_aegis_until")){for(String x:new String[]{"fire","ice","lightning","holy","ender","blood","evocation","nature","eldritch"})setAttr(p,"irons_spellbooks:"+x+"_magic_resist","aegis_"+x,0);}
        // Reservoir dynamic high/low mana uses Iron's magic data through reflection.
        if(h(p,"manaflow/reservoir/specialization")){double r=manaRatio(p);setAttr(p,"irons_spellbooks:spell_power","reservoir",r>=.70?(m(p,"manaflow/reservoir")?.22:.12):(m(p,"manaflow/reservoir")&&r>=0&&r<.25?-.20:0));if(r>=.70&&m(p,"manaflow/reservoir"))effect(p,"irons_spellbooks:fortify",2,0);if(r>=0&&r<.25&&m(p,"manaflow/reservoir"))effect(p,"irons_spellbooks:slowed",2,0);}
    }

    private static void applyAegis(ServerPlayer p,String damage){String school="";for(String s:new String[]{"fire","ice","lightning","holy","ender","blood","evocation","nature","eldritch"})if(damage.contains(s)){school=s;break;} if(school.isEmpty())return; for(String s:new String[]{"fire","ice","lightning","holy","ender","blood","evocation","nature","eldritch"})setAttr(p,"irons_spellbooks:"+s+"_magic_resist","aegis_"+s,s.equals(school)?(m(p,"arcane_ward/aegis")?.35:.20):(m(p,"arcane_ward/aegis")?-.10:0)); AbilityState.setLong(p,"m62_aegis_until",now(p)+120);}
    static void markGale(ServerPlayer p){ AbilityState.setLong(p,GALE,now(p)+100); }
    static void setAttr(ServerPlayer p,String attr,String key,double amount){ UUID u=UUID.nameUUIDFromBytes(("eldenworld:m62:"+key).getBytes(java.nio.charset.StandardCharsets.UTF_8)); AbilityUtil.setDynamicAttributeModifier(p,id(attr),u,"EldenWorld "+key,amount,net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.MULTIPLY_TOTAL); }
    static void applyEffect(ServerPlayer p,String rid,int sec,int amp){effect(p,rid,sec,amp);}
    static boolean has(ServerPlayer p,String s){return h(p,s);} static boolean mastery(ServerPlayer p,String s){return m(p,s);} static long time(ServerPlayer p){return now(p);}

    private static double dist2(double[] a,double[] b){double x=a[0]-b[0],y=a[1]-b[1],z=a[2]-b[2];return x*x+y*y+z*z;}
    private static double durability(ItemStack s){if(s.isEmpty()||!s.isDamageableItem())return -1;return 1.0-(double)s.getDamageValue()/Math.max(1,s.getMaxDamage());}
    private static boolean isIceFireMount(Entity e){if(e==null)return false;ResourceLocation k=ForgeRegistries.ENTITY_TYPES.getKey(e.getType());return k!=null&&k.getNamespace().equals("iceandfire");}
    private static int weaponLevel(ItemStack s){try{Class<?> c=Class.forName("net.weaponleveling.api.LevelingAPI");return (Integer)c.getMethod("getLevel",ItemStack.class).invoke(null,s);}catch(Throwable ignored){return 0;}}
    private static double manaRatio(ServerPlayer p){try{Class<?> md=Class.forName("io.redspace.ironsspellbooks.api.magic.MagicData");Object data=md.getMethod("getPlayerMagicData",net.minecraft.world.entity.player.Player.class).invoke(null,p);double mana=((Number)data.getClass().getMethod("getMana").invoke(data)).doubleValue();var at=ForgeRegistries.ATTRIBUTES.getValue(id("irons_spellbooks:max_mana"));double max=at==null?0:p.getAttributeValue(at);return max<=0?-1:mana/max;}catch(Throwable ignored){return -1;}}
    private static int countRelics(ServerPlayer p){try{Class<?> api=Class.forName("top.theillusivec4.curios.api.CuriosApi");Object lazy=api.getMethod("getCuriosInventory",LivingEntity.class).invoke(null,p);Object opt=lazy.getClass().getMethod("resolve").invoke(lazy);Object handler=((Optional<?>)opt).orElse(null);if(handler==null)return 0;Object inv=handler.getClass().getMethod("getEquippedCurios").invoke(handler);Method slots=inv.getClass().getMethod("getSlots"),get=inv.getClass().getMethod("getStackInSlot",int.class);int n=((Number)slots.invoke(inv)).intValue(),c=0;for(int i=0;i<n;i++){ItemStack s=(ItemStack)get.invoke(inv,i);String ns=ns(s);if(ns.equals("relics")||ns.equals("artifacts"))c++;}return c;}catch(Throwable ignored){return 0;}}
    private static LivingEntity findFriendlyMca(ServerPlayer p){for(LivingEntity e:p.level().getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(10),x->x!=p&&x.getClass().getName().toLowerCase(Locale.ROOT).contains("mca")&&x.getClass().getSimpleName().contains("VillagerEntityMCA"))){try{Object brain=e.getClass().getMethod("getVillagerBrain").invoke(e);Object mem=brain.getClass().getMethod("getMemoriesForPlayer",net.minecraft.world.entity.player.Player.class).invoke(brain,p);int hearts=((Number)mem.getClass().getMethod("getHearts").invoke(mem)).intValue();if(hearts>=20)return e;}catch(Throwable ignored){}}return null;}
    private static void spawnDecoys(ServerPlayer p,int count,int ticks){try{Class<?> c=Class.forName("com.hollingsworth.arsnouveau.common.entity.EntityDummy");var ctor=c.getConstructor(Level.class);for(int i=0;i<count;i++){Object o=ctor.newInstance(p.level());if(!(o instanceof Entity ent))continue;c.getMethod("setTicksLeft",int.class).invoke(o,ticks);c.getMethod("setOwnerID",UUID.class).invoke(o,p.getUUID());double side=i==0?1.5:-1.5;ent.setPos(p.getX()+side,p.getY(),p.getZ());p.level().addFreshEntity(ent);if(ent instanceof LivingEntity le)for(Mob mob:p.level().getEntitiesOfClass(Mob.class,le.getBoundingBox().inflate(20,10,20),x->x!=le))mob.setTarget(le);}}catch(Throwable ignored){ effect(p,"irons_spellbooks:evasion",3,0); }}
}
