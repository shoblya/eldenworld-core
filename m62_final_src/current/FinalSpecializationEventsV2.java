package com.eldenworld.core.specialization;

import com.eldenworld.core.EldenWorldCore;
import com.eldenworld.core.abilities.AbilityState;
import com.eldenworld.core.abilities.AbilityUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
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
import net.minecraftforge.event.entity.player.TradeWithVillagerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Mod.EventBusSubscriber(modid = EldenWorldCore.MOD_ID)
public final class FinalSpecializationEventsV2 {
    private FinalSpecializationEventsV2() {}

    private static final TagKey<Item> SS_LIGHT = TagKey.create(Registries.ITEM, id("simplyswords:light_weapons"));
    private static final TagKey<Item> SS_HEAVY = TagKey.create(Registries.ITEM, id("simplyswords:heavy_weapons"));

    private static final String NB_WINDOW="m62_nb_window", NB_REWARD="m62_nb_reward";
    private static final String PHANTOM_WINDOW="m62_phantom_window";
    private static final String DUEL_ROLL="m62_duel_roll", DUEL_EXPOSE="m62_duel_expose";
    private static final String GALE="m62_gale", SKIRMISH="m62_skirmish";
    private static final String SPECTER_SOUL="m62_specter_soul", SPECTER_SOUL_UNTIL="m62_specter_soul_until";
    private static final String OCCULT_SOUL="m62_occult_soul", OCCULT_SOUL_UNTIL="m62_occult_soul_until", OCCULT_BURST="m62_occult_burst";
    private static final String TEMPO="m62_tempo", TEMPO_UNTIL="m62_tempo_until";
    private static final String BLOOD="m62_blood", BLOOD_LAST="m62_blood_last", VAMP_UNTIL="m62_vamp_until";
    private static final String RESOLVE="m62_resolve", RESOLVE_UNTIL="m62_resolve_until";
    private static final String HUNTER_TARGET="m62_hunter_target", HUNTER_STACK="m62_hunter_stack", HUNTER_UNTIL="m62_hunter_until";
    private static final String PRED_TARGET="m62_pred_target", PRED_STACK="m62_pred_stack", PRED_UNTIL="m62_pred_until";
    private static final String COLOSSUS_TARGET="m62_col_target", COLOSSUS_STACK="m62_col_stack", COLOSSUS_UNTIL="m62_col_until";
    private static final String FLOOD="m62_flood", FLOOD_UNTIL="m62_flood_until";
    private static final String STONE_UNTIL="m62_stone_until", STONE_RETALIATE="m62_stone_retaliate";
    private static final String SPELLGUARD_SCHOOL="m62_spellguard_school", SPELLGUARD_UNTIL="m62_spellguard_until", SPELLGUARD_TARGET="m62_spellguard_target";
    private static final String MARKSMAN_STILL="m62_still";
    private static final String STORM="m62_storm";
    private static final String TRAIL_DIM_SINCE="m62_trail_dim_since", TRAIL_TYPE="m62_trail_type", TRAIL_UNTIL="m62_trail_until";
    private static final String EARTH="m62_earth", EARTH_UNTIL="m62_earth_until";
    private static final String LAST_STAND="m62_last_stand_until";
    private static final String WASTE_TYPE="m62_waste_type", WASTE_STACK="m62_waste_stack", WASTE_UNTIL="m62_waste_until";
    private static final String FEAST_UNTIL="m62_feast_until", FEAST_AURA="m62_feast_aura", FEAST_WEAK_DONE="m62_feast_weak_done";
    private static final String REV_SEEN="m62_revenant_seen", REV_UNTIL="m62_revenant_until", REV_PENALTY_AT="m62_revenant_penalty_at", REV_PENALTY_DONE="m62_revenant_penalty_done";
    private static final String AEGIS_SCHOOL="m62_aegis_school", AEGIS_UNTIL="m62_aegis_until", AEGIS_BURST_SCHOOL="m62_aegis_burst_school", AEGIS_BURST_UNTIL="m62_aegis_burst_until";
    private static final String SPELLBREAKER_TARGET="m62_spellbreaker_target", SPELLBREAKER_UNTIL="m62_spellbreaker_until";
    private static final String ARCH_ZONE="m62_arch_zone", FORT_BLOCK="m62_fort_block", FORT_UNTIL="m62_fort_until";
    private static final String SPEC_ROOT="EldenWorldM62Spec";
    private static final String COOK_OWNER="EldenWorldCookOwner";
    private static final String DEATH_MARK_OWNER="EldenWorldM62DeathMarkOwner", DEATH_MARK_UNTIL="EldenWorldM62DeathMarkUntil";

    private static final Map<UUID,double[]> LAST_POS=new HashMap<>();
    private static final Map<UUID,double[]> MIRAGE_REPOSITION=new HashMap<>();
    private static final Map<UUID,LinkedHashSet<String>> FEAST_ITEMS=new HashMap<>();
    private static final Map<UUID,Long> FEAST_START=new HashMap<>();
    private static final Map<UUID,Map<ResourceLocation,Integer>> BREWER_NEGATIVE_SEEN=new HashMap<>();

    static {
        registerCombatRollHook();
    }

    /**
     * Combat Roll is an optional integration at compile time. At runtime we bind to
     * ServerSideRollEvents.PLAYER_START_ROLLING from Combat Roll 1.3.3 using its
     * public Event.register(listener) API. This keeps EldenWorld boot-safe when the
     * optional mod is absent while still using the real server roll event when present.
     */
    private static void registerCombatRollHook() {
        try {
            Class<?> events = Class.forName("net.combat_roll.api.event.ServerSideRollEvents");
            Class<?> listener = Class.forName("net.combat_roll.api.event.ServerSideRollEvents$PlayerStartRolling");
            Object event = events.getField("PLAYER_START_ROLLING").get(null);
            Object proxy = java.lang.reflect.Proxy.newProxyInstance(
                    listener.getClassLoader(), new Class<?>[]{listener},
                    (obj, method, args) -> {
                        if ("onPlayerStartedRolling".equals(method.getName())
                                && args != null && args.length > 0 && args[0] instanceof ServerPlayer player) {
                            onCombatRoll(player);
                        }
                        return null;
                    });
            event.getClass().getMethod("register", listener).invoke(event, proxy);
        } catch (Throwable ignored) {
            // No Combat Roll or an incompatible optional version: skip only this hook.
        }
    }

    private static ResourceLocation id(String s){return new ResourceLocation(s);}
    private static boolean h(ServerPlayer p,String x){return AbilityUtil.has(p,id("eldenworld:"+x));}
    private static boolean m(ServerPlayer p,String base){return h(p,base+"/mastery");}
    private static long now(ServerPlayer p){return p.level().getGameTime();}
    private static boolean cd(ServerPlayer p,String key,long ticks){long n=now(p);if(!AbilityState.cooldownReady(p,key,n))return false;AbilityState.setCooldown(p,key,n,ticks);return true;}
    private static String path(ItemStack s){ResourceLocation k=ForgeRegistries.ITEMS.getKey(s.getItem());return k==null?"":k.getPath().toLowerCase(Locale.ROOT);}
    private static String ns(ItemStack s){ResourceLocation k=ForgeRegistries.ITEMS.getKey(s.getItem());return k==null?"":k.getNamespace().toLowerCase(Locale.ROOT);}
    private static boolean any(String s,String...xs){for(String x:xs)if(s.contains(x))return true;return false;}
    private static boolean projectile(LivingHurtEvent e){return e.getSource().getDirectEntity() instanceof Projectile;}
    private static boolean melee(LivingHurtEvent e){return e.getSource().getEntity() instanceof ServerPlayer && !projectile(e);}
    private static boolean shield(ServerPlayer p){return p.getMainHandItem().getItem() instanceof ShieldItem||p.getOffhandItem().getItem() instanceof ShieldItem;}
    private static boolean light(ItemStack s){return s.is(SS_LIGHT)||any(path(s),"sai","rapier","katana","cutlass","twinblade","warglaive","dagger");}
    private static boolean heavy(ItemStack s){return s.is(SS_HEAVY)||any(path(s),"claymore","greataxe","greathammer","great_hammer","halberd","greatsword");}
    private static boolean scythe(ItemStack s){return path(s).contains("scythe");}
    private static boolean oneHanded(ItemStack s){return light(s)||any(path(s),"sword","rapier","katana","cutlass");}
    private static boolean redstone(net.minecraft.world.level.block.state.BlockState s){return s.is(Blocks.REDSTONE_WIRE)||s.is(Blocks.REPEATER)||s.is(Blocks.COMPARATOR)||s.is(Blocks.PISTON)||s.is(Blocks.STICKY_PISTON)||s.is(Blocks.OBSERVER)||s.is(Blocks.DISPENSER)||s.is(Blocks.DROPPER)||s.is(Blocks.HOPPER);}
    private static void effect(LivingEntity e,String rid,int sec,int amp){MobEffect fx=ForgeRegistries.MOB_EFFECTS.getValue(id(rid));if(fx!=null)e.addEffect(new MobEffectInstance(fx,sec*20,amp,true,false,true));}
    private static void effectPath(LivingEntity e,String token,int sec,int amp){
        String t=token.toLowerCase(Locale.ROOT);
        for(ResourceLocation key:ForgeRegistries.MOB_EFFECTS.getKeys()){
            if(key.getPath().equals(t)||key.getPath().contains(t)){MobEffect fx=ForgeRegistries.MOB_EFFECTS.getValue(key);if(fx!=null){e.addEffect(new MobEffectInstance(fx,sec*20,amp,true,false,true));return;}}
        }
    }
    private static void vanilla(LivingEntity e,MobEffect fx,int sec,int amp){e.addEffect(new MobEffectInstance(fx,sec*20,amp,true,false,true));}
    private static boolean trueInvisible(ServerPlayer p){for(MobEffectInstance x:p.getActiveEffects()){ResourceLocation k=ForgeRegistries.MOB_EFFECTS.getKey(x.getEffect());if(k!=null&&k.getPath().contains("true_invisibility"))return true;}return false;}
    private static boolean boss(LivingEntity e){ResourceLocation k=ForgeRegistries.ENTITY_TYPES.getKey(e.getType());if(k==null)return false;String n=k.getNamespace(),p=k.getPath();return any(n,"cataclysm","mowzie","twilightforest","aquamirae","alexscaves","traveloptics","legendary")&&(e.getMaxHealth()>=80||any(p,"boss","ignis","leviathan","harbinger","lich","hydra","naga","ur_ghast","snow_queen","frostmaw","wroughtnaut","nightwarden"));}
    private static boolean elite(LivingEntity e){return boss(e)||(e instanceof Monster&&e.getMaxHealth()>=60);}
    private static String entityId(LivingEntity e){ResourceLocation k=ForgeRegistries.ENTITY_TYPES.getKey(e.getType());return k==null?"unknown":k.toString();}
    private static String damageType(net.minecraft.world.damagesource.DamageSource s){return s.typeHolder().unwrapKey().map(k->k.location().toString()).orElse("unknown");}
    private static boolean environmental(net.minecraft.world.damagesource.DamageSource s){return s.getEntity()==null;}
    private static void give(ServerPlayer p,ItemStack stack){if(stack.isEmpty())return;if(!p.getInventory().add(stack))p.drop(stack,false);}

    private static CompoundTag markTag(LivingEntity target){return target.getPersistentData();}
    private static boolean markedBy(LivingEntity target,ServerPlayer p){
        CompoundTag t=markTag(target);
        return t.hasUUID(DEATH_MARK_OWNER)&&p.getUUID().equals(t.getUUID(DEATH_MARK_OWNER))&&t.getLong(DEATH_MARK_UNTIL)>=now(p);
    }
    private static void mark(LivingEntity target,ServerPlayer p,int sec){
        CompoundTag t=markTag(target);t.putUUID(DEATH_MARK_OWNER,p.getUUID());t.putLong(DEATH_MARK_UNTIL,now(p)+sec*20L);
    }
    private static void clearMark(LivingEntity target){CompoundTag t=markTag(target);t.remove(DEATH_MARK_OWNER);t.remove(DEATH_MARK_UNTIL);}

    private static int inc(ServerPlayer p,String key,int max){int n=Math.min(max,AbilityState.getInt(p,key)+1);AbilityState.setInt(p,key,n);return n;}
    private static void addSpecterSoul(ServerPlayer p){inc(p,SPECTER_SOUL,3);AbilityState.setLong(p,SPECTER_SOUL_UNTIL,now(p)+160L);}
    private static void addOccultSoul(ServerPlayer p){inc(p,OCCULT_SOUL,3);AbilityState.setLong(p,OCCULT_SOUL_UNTIL,now(p)+160L);}
    private static void addBlood(ServerPlayer p){
        long n=now(p);if(n-AbilityState.getLong(p,BLOOD_LAST)<20L)return;
        AbilityState.setLong(p,BLOOD_LAST,n);inc(p,BLOOD,5);
    }
    private static void addResolve(ServerPlayer p){inc(p,RESOLVE,5);AbilityState.setLong(p,RESOLVE_UNTIL,now(p)+120L);}

    private static void onCombatRoll(ServerPlayer p){
        long n=now(p);
        if(h(p,"shadowstep/nightblade/specialization"))AbilityState.setLong(p,NB_WINDOW,n+60L);
        if(h(p,"ghost/phantom/specialization")&&cd(p,"m62_phantom_roll",160L)){
            effect(p,"irons_spellbooks:true_invisibility",m(p,"ghost/phantom")?3:2,0);
            AbilityState.setLong(p,PHANTOM_WINDOW,n+60L);
        }
        if(h(p,"opportunist/duelist/specialization"))AbilityState.setLong(p,DUEL_ROLL,n+60L);
        if(h(p,"windrunner/gale_dancer/specialization"))AbilityState.setLong(p,GALE,n+100L);
        if(h(p,"windrunner/skirmisher/specialization"))AbilityState.setLong(p,SKIRMISH,n+60L);
    }

    @SubscribeEvent
    public static void onAttack(LivingAttackEvent e){
        if(!(e.getEntity() instanceof ServerPlayer d))return;
        if(d.isBlocking()&&shield(d)&&h(d,"unyielding/bulwark/specialization")){
            int n=inc(d,FLOOD,3);AbilityState.setLong(d,FLOOD_UNTIL,now(d)+120L);
            if(n>=3&&m(d,"unyielding/bulwark")){
                AbilityState.setInt(d,FLOOD,0);
                for(LivingEntity le:d.level().getEntitiesOfClass(LivingEntity.class,d.getBoundingBox().inflate(4),x->x!=d&&x instanceof Monster)){
                    le.hurt(d.damageSources().magic(),8.0f);
                    le.knockback(1.5,d.getX()-le.getX(),d.getZ()-le.getZ());
                }
            }
        }
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent e){
        Entity src=e.getSource().getEntity();
        if(src instanceof ServerPlayer a){
            LivingEntity t=e.getEntity();ItemStack w=a.getMainHandItem();float x=e.getAmount();boolean pr=projectile(e);long n=now(a);
            AbilityState.setLong(a,"m62_pilgrim_combat_until",n+100L);
            AbilityState.setInt(a,"m62_pilgrim",0);

            // ROGUE
            if(h(a,"shadowstep/nightblade/specialization")&&!pr){
                boolean was=markedBy(t,a);
                if(was&&m(a,"shadowstep/nightblade")){
                    x+=4.0f;
                    AbilityState.setString(a,"m62_nb_detonated_target",t.getUUID().toString());
                    AbilityState.setLong(a,"m62_nb_detonated_until",n+20L);
                    clearMark(t);
                }else if(n<=AbilityState.getLong(a,NB_WINDOW)){
                    x*=1.15f;
                    AbilityState.setLong(a,NB_WINDOW,0L);
                    mark(t,a,5);
                }
            }
            if(h(a,"ghost/assassin/specialization")&&!pr&&light(w)){
                String active=AbilityState.getString(a,"m62_assassin_target");
                long until=AbilityState.getLong(a,"m62_assassin_until");
                if(!active.isEmpty()&&until>=n&&!active.equals(t.getUUID().toString()))x*=.75f;
                boolean was=markedBy(t,a);
                if(trueInvisible(a)){x*=1.25f;mark(t,a,6);AbilityState.setString(a,"m62_assassin_target",t.getUUID().toString());AbilityState.setLong(a,"m62_assassin_until",n+120L);}
                if(was&&t.getHealth()<=t.getMaxHealth()*.30f)x*=m(a,"ghost/assassin")?1.35f:1.15f;
            }
            if(h(a,"ghost/specter/specialization")&&!pr&&scythe(w)){
                if(m(a,"ghost/specter")&&AbilityState.getInt(a,SPECTER_SOUL)>=3){
                    clearSpecterSouls(a);
                    effectPath(t,"soul_lock",3,0);
                    effect(t,"irons_spellbooks:soul_burn",5,1);
                }else{
                    addSpecterSoul(a);
                }
            }
            if(h(a,"ghost/phantom/specialization")&&n<=AbilityState.getLong(a,PHANTOM_WINDOW)){
                x*=m(a,"ghost/phantom")?1.30f:1.15f;
                AbilityState.setLong(a,PHANTOM_WINDOW,0L);
                if(m(a,"ghost/phantom"))effect(a,"irons_spellbooks:evasion",3,0);
            }

            if(h(a,"opportunist/duelist/specialization")&&!pr&&a.getOffhandItem().isEmpty()&&oneHanded(w)){
                int tempo=AbilityState.getInt(a,TEMPO);
                if(m(a,"opportunist/duelist")&&tempo>=3){x*=1.35f;effect(a,"irons_spellbooks:evasion",2,0);AbilityState.setInt(a,TEMPO,0);}
                else if(n<=AbilityState.getLong(a,DUEL_ROLL)){inc(a,TEMPO,3);AbilityState.setLong(a,TEMPO_UNTIL,n+100L);AbilityState.setLong(a,DUEL_ROLL,0);}
            }
            if(h(a,"opportunist/predator/specialization")&&!pr){
                String uid=t.getUUID().toString(),old=AbilityState.getString(a,PRED_TARGET);
                if(!uid.equals(old)||n>AbilityState.getLong(a,PRED_UNTIL)){AbilityState.setString(a,PRED_TARGET,uid);AbilityState.setInt(a,PRED_STACK,0);}
                else inc(a,PRED_STACK,3);
                AbilityState.setLong(a,PRED_UNTIL,n+160L);
                int st=AbilityState.getInt(a,PRED_STACK);x*=1f+.07f*st;
                if(t.getHealth()>=t.getMaxHealth()*.70f)x*=.85f;
                if(m(a,"opportunist/predator")&&st>=3&&t.getHealth()<=t.getMaxHealth()*.20f){x*=1.35f;effectPath(a,"ravenous",4,0);}
            }
            if(h(a,"opportunist/trickster/specialization")){
                boolean snared=hasEffectPath(t,"snared");
                if(snared)effectPath(t,"insanity",4,m(a,"opportunist/trickster")?1:0);
                if(cd(a,"m62_trick",120L)){effect(t,"ars_nouveau:snared",m(a,"opportunist/trickster")?4:2,0);if(m(a,"opportunist/trickster"))effect(t,"ars_nouveau:hex",4,0);}
            }

            // WARRIOR
            if(h(a,"juggernaut/berserker/specialization")&&!pr){
                effect(t,"attributeslib:bleeding",m(a,"juggernaut/berserker")?5:4,m(a,"juggernaut/berserker")?1:0);
                if(a.getHealth()<=a.getMaxHealth()*.50f)x*=m(a,"juggernaut/berserker")?1.30f:1.15f;
            }
            if(h(a,"juggernaut/bloodguard/specialization")&&!pr){
                addBlood(a);int r=AbilityState.getInt(a,BLOOD);
                if(m(a,"juggernaut/bloodguard")&&r>=5){AbilityState.setInt(a,BLOOD,0);a.heal(a.getMaxHealth()*.10f);AbilityState.setLong(a,VAMP_UNTIL,n+100L);}
                else if(r>0)a.heal(Math.min(x*.02f*r,a.getMaxHealth()*.10f));
                if(n<=AbilityState.getLong(a,VAMP_UNTIL))a.heal(Math.min(x*.10f,a.getMaxHealth()*.10f));
            }
            if(h(a,"juggernaut/colossus/specialization")&&!pr&&heavy(w)){
                String uid=t.getUUID().toString();
                if(!uid.equals(AbilityState.getString(a,COLOSSUS_TARGET))||n>AbilityState.getLong(a,COLOSSUS_UNTIL)){AbilityState.setString(a,COLOSSUS_TARGET,uid);AbilityState.setInt(a,COLOSSUS_STACK,0);}
                int c=inc(a,COLOSSUS_STACK,3);AbilityState.setLong(a,COLOSSUS_UNTIL,n+120L);
                if(c==1)effectPath(t,"armor_crunch",4,0);
                if(c==2)effectPath(t,"broken_armor",5,0);
                if(c>=3){effect(t,"attributeslib:sundering",4,m(a,"juggernaut/colossus")?1:0);if(m(a,"juggernaut/colossus"))x*=1.30f;AbilityState.setInt(a,COLOSSUS_STACK,0);}
            }
            if(h(a,"unyielding/stoneguard/specialization")&&!pr&&n<=AbilityState.getLong(a,STONE_RETALIATE)){
                effectPath(t,"erode",4,m(a,"unyielding/stoneguard")?1:0);AbilityState.setLong(a,STONE_RETALIATE,0L);
            }
            if(h(a,"unyielding/spellguard/specialization")&&n<=AbilityState.getLong(a,SPELLGUARD_UNTIL)&&t.getUUID().toString().equals(AbilityState.getString(a,SPELLGUARD_TARGET))){
                if(m(a,"unyielding/spellguard"))x*=1.20f;AbilityState.setString(a,SPELLGUARD_TARGET,"");
            }
            if(h(a,"second_wind/ironheart/specialization")&&!pr&&weaponLevel(w)>0){
                int r=AbilityState.getInt(a,RESOLVE);
                if(m(a,"second_wind/ironheart")&&r>=5&&heavy(w)){x*=1.30f;AbilityState.setInt(a,RESOLVE,0);effectPath(a,"hardware_update",5,1);effect(a,"irons_spellbooks:fortify",5,1);}
                else{addResolve(a);x*=1f+.02f*AbilityState.getInt(a,RESOLVE);}
            }
            if(h(a,"second_wind/revenant/specialization")&&n<=AbilityState.getLong(a,REV_UNTIL)){
                x*=1.20f;
                if(m(a,"second_wind/revenant"))a.heal(Math.min(a.getMaxHealth()*.10f,x*.10f));
            }
            if(h(a,"second_wind/paladin/specialization")&&hasAlly(a))x*=.85f;

            if(h(a,"arcane_ward/spellbreaker/specialization")
                    && n<=AbilityState.getLong(a,SPELLBREAKER_UNTIL)
                    && t.getUUID().toString().equals(AbilityState.getString(a,SPELLBREAKER_TARGET))){
                effectPath(t,"blackout",m(a,"arcane_ward/spellbreaker")?5:4,m(a,"arcane_ward/spellbreaker")?1:0);
                if(m(a,"arcane_ward/spellbreaker"))effectPath(t,"erode",4,0);
                AbilityState.setLong(a,SPELLBREAKER_UNTIL,0L);
                AbilityState.setString(a,SPELLBREAKER_TARGET,"");
            }
            if(h(a,"arcane_ward/technomancer/specialization")&&!AbilityUtil.isMagicLike(e.getSource())){
                FinalSpecializationSpellEventsV2.hardwareHit(a);
            }

            // Occult Mastery finisher: first occult damage during the burst applies Soul Burn.
            if(h(a,"archmage/occultist/specialization")
                    && n<=AbilityState.getLong(a,OCCULT_BURST)
                    && isOccultDamage(e.getSource())){
                effect(t,"irons_spellbooks:soul_burn",5,1);
                AbilityState.setLong(a,OCCULT_BURST,0L);
            }

            // RANGER
            if(pr&&h(a,"keen_instinct/marksman/specialization")&&AbilityState.getInt(a,MARKSMAN_STILL)>=40&&cd(a,"m62_marksman",160L)){
                x*=m(a,"keen_instinct/marksman")?2.00f:1.25f;AbilityState.setInt(a,MARKSMAN_STILL,0);
            }else if(pr&&h(a,"keen_instinct/marksman/specialization")&&m(a,"keen_instinct/marksman")&&a.distanceTo(t)<6)x*=.75f;

            if(pr&&h(a,"keen_instinct/hunter/specialization")){
                String uid=t.getUUID().toString();
                if(!uid.equals(AbilityState.getString(a,HUNTER_TARGET))||n>AbilityState.getLong(a,HUNTER_UNTIL)){
                    if(m(a,"keen_instinct/hunter")&&!AbilityState.getString(a,HUNTER_TARGET).isEmpty())AbilityState.setLong(a,"m62_hunter_penalty",n+80L);
                    AbilityState.setString(a,HUNTER_TARGET,uid);AbilityState.setInt(a,HUNTER_STACK,1);
                }else inc(a,HUNTER_STACK,3);
                AbilityState.setLong(a,HUNTER_UNTIL,n+120L);
                int hs=AbilityState.getInt(a,HUNTER_STACK);x*=1f+.07f*hs;
                if(n<=AbilityState.getLong(a,"m62_hunter_penalty"))x*=.85f;
                if(m(a,"keen_instinct/hunter")&&hs>=3){x*=1.15f;effect(t,"irons_spellbooks:rend",4,0);}
            }
            if(pr&&h(a,"keen_instinct/sentinel/specialization")&&AbilityState.getInt(a,MARKSMAN_STILL)>=40){
                x*=m(a,"keen_instinct/sentinel")?1.35f:1.15f;effect(a,"irons_spellbooks:fortify",3,m(a,"keen_instinct/sentinel")?1:0);AbilityState.setInt(a,MARKSMAN_STILL,0);
            }
            if(pr&&h(a,"windrunner/gale_dancer/specialization")&&n<=AbilityState.getLong(a,GALE)){
                x*=m(a,"windrunner/gale_dancer")?1.35f:1.20f;effect(a,"irons_spellbooks:hastened",2,0);if(m(a,"windrunner/gale_dancer"))effect(t,"irons_spellbooks:airborne",3,0);AbilityState.setLong(a,GALE,0);
            }
            if(pr&&h(a,"windrunner/stormshot/specialization")){
                int c=AbilityState.getInt(a,STORM)+1;
                if(c>=3){c=0;x*=m(a,"windrunner/stormshot")?1.35f:1.20f;effect(t,"irons_spellbooks:charged",4,0);if(m(a,"windrunner/stormshot"))effect(t,"irons_spellbooks:volt_strike",3,0);}
                AbilityState.setInt(a,STORM,c);
            }
            if(pr&&h(a,"windrunner/skirmisher/specialization")&&n<=AbilityState.getLong(a,SKIRMISH)){
                x*=m(a,"windrunner/skirmisher")?1.35f:1.20f;effect(a,"irons_spellbooks:evasion",m(a,"windrunner/skirmisher")?2:1,0);AbilityState.setLong(a,SKIRMISH,0);
            }
            if(h(a,"pathfinder/dragon_rider/specialization")&&isIceFireMount(a.getVehicle()))x*=m(a,"pathfinder/dragon_rider")?1.28f:1.15f;
            else if(m(a,"pathfinder/dragon_rider"))x*=.88f;

            // BUILDER / MINER
            if(h(a,"master_builder/architect/specialization")&&n<=AbilityState.getLong(a,ARCH_ZONE))x*=.80f;
            if(h(a,"enduring_tools/geomancer/specialization")&&!pr&&heavy(w)&&m(a,"enduring_tools/geomancer")&&AbilityState.getInt(a,EARTH)>=5){
                x*=1.35f;AbilityState.setInt(a,EARTH,0);effectPath(t,"erode",5,0);vanilla(a,MobEffects.MOVEMENT_SLOWDOWN,5,0);
            }

            // ADVENTURER
            if(h(a,"treasure_hunter/relic_seeker/specialization")){int c=Math.min(m(a,"treasure_hunter/relic_seeker")?5:3,countRelics(a));x*=1f+(m(a,"treasure_hunter/relic_seeker")?.04f:.03f)*c;}
            if(h(a,"treasure_hunter/broker_envoy/specialization"))x*=.88f;
            if(h(a,"survivor/monster_slayer/specialization")){
                if(elite(t)){int trophies=trophyCount(a);float base=m(a,"survivor/monster_slayer")?.30f:.18f;float journal=Math.min(m(a,"survivor/monster_slayer")?.15f:.10f,trophies*.01f);x*=1f+base+journal;effect(t,"irons_spellbooks:rend",3,0);if(m(a,"survivor/monster_slayer"))effect(t,"attributeslib:sundering",3,0);}
                else if(m(a,"survivor/monster_slayer"))x*=.88f;
            }

            e.setAmount(x);
        }

        if(e.getEntity() instanceof ServerPlayer d){
            float x=e.getAmount();Entity attacker=e.getSource().getEntity();long n=now(d);
            AbilityState.setLong(d,"m62_pilgrim_combat_until",n+100L);
            AbilityState.setInt(d,"m62_pilgrim",0);

            // ROGUE defense
            if(h(d,"shadowstep/riftwalker/specialization")&&x>=d.getMaxHealth()*.15f&&cd(d,"m62_rift",m(d,"shadowstep/riftwalker")?120L:160L)){
                AbilityUtil.tryShadowstep(d,m(d,"shadowstep/riftwalker")?5.0:3.5);
                if(m(d,"shadowstep/riftwalker")){effectPath(d,"spectral_blink",3,0);AbilityState.setLong(d,"m62_rift_expose",n+80L);}
            }
            if(m(d,"shadowstep/mirage")
                    && n>=AbilityState.getLong(d,"m62_mirage_expose_start")
                    && n<=AbilityState.getLong(d,"m62_mirage_expose_until")){
                x*=1.20f;
            }
            if(h(d,"shadowstep/mirage/specialization")&&cd(d,"m62_mirage",200L)&&d.getRandom().nextFloat()<(m(d,"shadowstep/mirage")?.40f:.30f)){
                boolean mm=m(d,"shadowstep/mirage");
                x*=mm?.70f:.80f;
                spawnDecoys(d,mm?2:1,mm?100:80);
                setAttr(d,"minecraft:generic.movement_speed","mirage_move",.10);
                AbilityState.setLong(d,"m62_mirage_move",n+40L);
                if(mm){
                    if(attacker instanceof LivingEntity le){
                        effectPath(le,"blackout",2,1);
                        effectPath(le,"insanity",3,2);
                    }
                    var look=d.getLookAngle();
                    double sx=-look.z,sz=look.x;
                    double len=Math.sqrt(sx*sx+sz*sz);
                    if(len<1.0e-4){sx=1;sz=0;}else{sx/=len;sz/=len;}
                    double sign=d.getRandom().nextBoolean()?1.0:-1.0;
                    MIRAGE_REPOSITION.put(d.getUUID(),new double[]{n+10L,d.getX()+sx*2.0*sign,d.getY(),d.getZ()+sz*2.0*sign});
                    AbilityState.setLong(d,"m62_mirage_expose_start",n+100L);
                    AbilityState.setLong(d,"m62_mirage_expose_until",n+160L);
                }
            }
            if(h(d,"opportunist/duelist/specialization")&&attacker!=null){AbilityState.setInt(d,TEMPO,0);AbilityState.setLong(d,DUEL_EXPOSE,n+60L);}

            // WARRIOR defense
            if(h(d,"juggernaut/bloodguard/specialization")&&attacker instanceof LivingEntity)addBlood(d);
            if(h(d,"unyielding/bulwark/specialization")&&d.isBlocking()&&shield(d)){
                int st=AbilityState.getInt(d,FLOOD);x*=Math.max(.65f,1f-.05f*st);if(m(d,"unyielding/bulwark"))x*=.80f;
            }
            if(h(d,"unyielding/stoneguard/specialization")&&attacker instanceof LivingEntity&&cd(d,"m62_stone",120L)){
                effectPath(d,"aegis",4,0);AbilityState.setLong(d,STONE_UNTIL,n+80L);AbilityState.setLong(d,STONE_RETALIATE,n+80L);
            }
            if(h(d,"unyielding/spellguard/specialization")&&AbilityUtil.isMagicLike(e.getSource())){
                String school=damageType(e.getSource());
                String prev=AbilityState.getString(d,SPELLGUARD_SCHOOL);
                if(school.equals(prev)&&n<=AbilityState.getLong(d,SPELLGUARD_UNTIL))x*=m(d,"unyielding/spellguard")?.65f:.80f;
                AbilityState.setString(d,SPELLGUARD_SCHOOL,school);AbilityState.setLong(d,SPELLGUARD_UNTIL,n+(m(d,"unyielding/spellguard")?160L:120L));
                if(attacker!=null)AbilityState.setString(d,SPELLGUARD_TARGET,attacker.getUUID().toString());
            }
            if(h(d,"second_wind/ironheart/specialization")){
                int r=AbilityState.getInt(d,RESOLVE);if(r>0)x*=Math.max(.80f,1f-.02f*r);
            }

            if(h(d,"manaflow/channeler/specialization")&&x>=d.getMaxHealth()*.15f){
                AbilityState.setInt(d,"m62_flow",0);
                if(m(d,"manaflow/channeler"))AbilityState.setLong(d,"m62_flow_penalty",n+100L);
            }

            if(AbilityUtil.isMagicLike(e.getSource())){
                String exact=n<=AbilityState.getLong(d,"m62_exact_spell_school_until")
                        ?AbilityState.getString(d,"m62_exact_spell_school"):"";
                String school=!exact.isEmpty()?exact:magicElementFromDamage(e.getSource());
                if(h(d,"arcane_ward/aegis/specialization")&&!school.isEmpty()){
                    AbilityState.setString(d,AEGIS_SCHOOL,school);
                    AbilityState.setLong(d,AEGIS_UNTIL,n+(m(d,"arcane_ward/aegis")?160L:120L));
                }
                if(h(d,"arcane_ward/spellbreaker/specialization")&&attacker!=null
                        && cd(d,"m62_spellbreaker_open",200L)){
                    AbilityState.setString(d,SPELLBREAKER_TARGET,attacker.getUUID().toString());
                    AbilityState.setLong(d,SPELLBREAKER_UNTIL,n+(m(d,"arcane_ward/spellbreaker")?120L:80L));
                }
            }

            // RANGER defense
            if(projectile(e)&&h(d,"keen_instinct/sentinel/specialization")&&m(d,"keen_instinct/sentinel")&&cd(d,"m62_sentinel_guard",240L))x*=.50f;

            // PATHFINDER / SURVIVOR adaptation
            if(h(d,"pathfinder/trailblazer/specialization")&&environmental(e.getSource())&&n-AbilityState.getLong(d,TRAIL_DIM_SINCE)>=600L){
                String typ=damageType(e.getSource());if(typ.equals(AbilityState.getString(d,TRAIL_TYPE))&&n<=AbilityState.getLong(d,TRAIL_UNTIL))x*=m(d,"pathfinder/trailblazer")?.70f:.85f;
                AbilityState.setString(d,TRAIL_TYPE,typ);AbilityState.setLong(d,TRAIL_UNTIL,n+400L);
            }
            if(h(d,"survivor/wastelander/specialization")&&environmental(e.getSource())){
                String typ=damageType(e.getSource()),old=AbilityState.getString(d,WASTE_TYPE);int stacks=AbilityState.getInt(d,WASTE_STACK);
                if(!typ.equals(old)||n>AbilityState.getLong(d,WASTE_UNTIL)){if(!old.isEmpty()&&m(d,"survivor/wastelander"))x*=1.15f;stacks=1;AbilityState.setString(d,WASTE_TYPE,typ);}
                else stacks=Math.min(3,stacks+1);
                AbilityState.setInt(d,WASTE_STACK,stacks);AbilityState.setLong(d,WASTE_UNTIL,n+400L);
                x*=1f-(m(d,"survivor/wastelander")?.15f:.10f)*stacks;
            }

            // Last Stand window triggers when this hit would put the player at <=25% HP.
            if(h(d,"survivor/last_stand/specialization")&&d.getHealth()-x<=d.getMaxHealth()*.25f&&cd(d,"m62_laststand",900L)){
                int sec=m(d,"survivor/last_stand")?8:6;
                effect(d,"irons_spellbooks:fortify",sec,m(d,"survivor/last_stand")?1:0);
                effect(d,"irons_spellbooks:vigor",sec,m(d,"survivor/last_stand")?1:0);
                AbilityState.setLong(d,LAST_STAND,n+sec*20L);
                if(m(d,"survivor/last_stand")){
                    AbilityState.setLong(d,"m62_laststand_penalty_at",n+sec*20L);
                    AbilityState.setInt(d,"m62_laststand_penalty_done",0);
                }
            }

            // Paladin ally aura: one nearby Paladin is enough; do not stack.
            ServerPlayer pal=nearbyPaladin(d,false);
            if(pal!=null&&pal!=d)x*=.90f;

            e.setAmount(x);
        }

        // Beastmaster works through Minecraft's standard OwnableEntity contract, so
        // vanilla pets and compatible pet mods do not need a hard dependency.
        if(src instanceof OwnableEntity pet && pet.getOwner() instanceof ServerPlayer owner
                && h(owner,"pathfinder/beastmaster/specialization")){
            e.setAmount(e.getAmount()*(m(owner,"pathfinder/beastmaster")?1.45f:1.25f));
        }
        if(e.getEntity() instanceof OwnableEntity pet && pet.getOwner() instanceof ServerPlayer owner
                && h(owner,"pathfinder/beastmaster/specialization")){
            e.setAmount(e.getAmount()*(m(owner,"pathfinder/beastmaster")?.75f:.85f));
        }
    }

    @SubscribeEvent
    public static void onHeal(LivingHealEvent e){
        if(!(e.getEntity() instanceof ServerPlayer p))return;
        ServerPlayer pal=nearbyPaladin(p,false);
        if(pal!=null&&pal!=p)e.setAmount(e.getAmount()*1.10f);
    }

    @SubscribeEvent
    public static void lethal(LivingDamageEvent e){
        if(!(e.getEntity() instanceof ServerPlayer p)||e.getAmount()<p.getHealth())return;
        long n=now(p);

        // Last Stand Mastery can convert a lethal hit only while its short survival window is active.
        if(m(p,"survivor/last_stand")&&n<=AbilityState.getLong(p,LAST_STAND)&&cd(p,"m62_laststand_lethal",2400L)){
            e.setAmount(Math.max(0,p.getHealth()-1f));
            effectPath(p,"lingering_strain",8,0);
            return;
        }

        ServerPlayer pal=nearbyPaladin(p,true);
        if(pal!=null&&pal!=p&&cd(pal,"m62_paladin_save",2400L)){
            float leave=Math.max(1f,p.getMaxHealth()*.20f);e.setAmount(Math.max(0,p.getHealth()-leave));effect(p,"irons_spellbooks:fortify",5,1);
        }
    }

    @SubscribeEvent
    public static void death(LivingDeathEvent e){
        if(!(e.getSource().getEntity() instanceof ServerPlayer p))return;
        LivingEntity killed=e.getEntity();long n=now(p);

        boolean stillMarked=markedBy(killed,p);
        boolean nightDetonationKill=killed.getUUID().toString().equals(AbilityState.getString(p,"m62_nb_detonated_target"))
                && n<=AbilityState.getLong(p,"m62_nb_detonated_until");
        if(stillMarked&&m(p,"ghost/assassin")&&cd(p,"m62_assassin_reward",160L)){
            effect(p,"irons_spellbooks:true_invisibility",2,0);
        }
        if(m(p,"shadowstep/nightblade")&&(stillMarked||nightDetonationKill)&&cd(p,"m62_night_reward",160L)){
            AbilityState.setLong(p,NB_REWARD,n+60L);
        }
        if(nightDetonationKill){
            AbilityState.setString(p,"m62_nb_detonated_target","");
            AbilityState.setLong(p,"m62_nb_detonated_until",0L);
        }
        if(h(p,"treasure_hunter/fortune_hunter/specialization")&&elite(killed)&&p.getRandom().nextFloat()<(m(p,"treasure_hunter/fortune_hunter")?.35f:.20f))effect(p,"ars_nouveau:magic_find",m(p,"treasure_hunter/fortune_hunter")?10:8,m(p,"treasure_hunter/fortune_hunter")?1:0);
        if(h(p,"survivor/monster_slayer/specialization")&&boss(killed))recordTrophy(p,entityId(killed));
    }

    @SubscribeEvent
    public static void clone(PlayerEvent.Clone e){
        if(!(e.getEntity() instanceof ServerPlayer p))return;
        CompoundTag oldRoot=e.getOriginal().getPersistentData().getCompound(SPEC_ROOT);
        if(!oldRoot.isEmpty())p.getPersistentData().put(SPEC_ROOT,oldRoot.copy());
    }

    @SubscribeEvent
    public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e){
        if(!(e.getEntity() instanceof ServerPlayer p))return;long n=now(p);
        AbilityState.setLong(p,TRAIL_DIM_SINCE,n);AbilityState.setString(p,TRAIL_TYPE,"");AbilityState.setLong(p,TRAIL_UNTIL,0L);
        if(m(p,"pathfinder/trailblazer")){effect(p,"irons_spellbooks:hastened",15,0);effect(p,"irons_spellbooks:planar_sight",15,0);vanilla(p,MobEffects.WEAKNESS,5,0);}
        if(h(p,"wayfarer/dimension_walker/specialization")){
            int sec=m(p,"wayfarer/dimension_walker")?25:15;effect(p,"irons_spellbooks:planar_sight",sec,0);effect(p,"irons_spellbooks:hastened",sec,0);
            if(m(p,"wayfarer/dimension_walker")){effect(p,"irons_spellbooks:evasion",5,0);effect(p,"irons_spellbooks:slowed",3,0);}
        }
    }

    @SubscribeEvent
    public static void place(BlockEvent.EntityPlaceEvent e){
        if(!(e.getEntity() instanceof ServerPlayer p))return;long n=now(p);
        if(h(p,"master_builder/architect/specialization")){
            int rhythm=AbilityState.getInt(p,"builder_rhythm");
            if(rhythm>=6)AbilityState.setLong(p,ARCH_ZONE,n+(m(p,"master_builder/architect")?400L:200L));
        }
        if(h(p,"master_builder/fortifier/specialization")&&!e.getPlacedBlock().isAir()){
            effect(p,"irons_spellbooks:fortify",4,0);AbilityState.setLong(p,FORT_BLOCK,e.getPos().asLong());AbilityState.setLong(p,FORT_UNTIL,n+300L);
        }
        if(h(p,"master_builder/engineer/specialization")&&redstone(e.getPlacedBlock())){
            vanilla(p,MobEffects.DIG_SPEED,5,0);effect(p,"ars_nouveau:mana_regen",5,0);
            if(m(p,"master_builder/engineer")){effect(p,"ars_nouveau:shielding",5,0);effect(p,"irons_spellbooks:hastened",5,0);}
        }
    }

    @SubscribeEvent
    public static void breakBlock(BlockEvent.BreakEvent e){
        if(!(e.getPlayer() instanceof ServerPlayer p)||!e.getState().is(Tags.Blocks.ORES))return;long n=now(p);
        if(h(p,"enduring_tools/prospector/specialization")){
            if(p.getRandom().nextFloat()<(m(p,"enduring_tools/prospector")?.35f:.20f))effect(p,"ars_nouveau:magic_find",m(p,"enduring_tools/prospector")?10:8,m(p,"enduring_tools/prospector")?1:0);
        }
        if(h(p,"enduring_tools/geomancer/specialization")){inc(p,EARTH,5);AbilityState.setLong(p,EARTH_UNTIL,n+200L);}
    }

    @SubscribeEvent
    public static void finishItem(LivingEntityUseItemEvent.Finish e){
        if(!(e.getEntity() instanceof ServerPlayer p))return;ItemStack used=e.getItem();
        if(!ownedCook(p,used))return;long n=now(p);

        if(used.isEdible()){
            if(h(p,"prospector/chef/specialization")){
                p.getFoodData().eat(1,.15f);
                if(m(p,"prospector/chef")&&cd(p,"m62_chef",1200L)){effect(p,"irons_spellbooks:vigor",45,0);vanilla(p,MobEffects.REGENERATION,8,0);AbilityState.setLong(p,"m62_chef_slow",n+160L);}
            }
            if(h(p,"prospector/feastmaster/specialization"))registerFeastItem(p,used,n);
        }else if(h(p,"prospector/brewer/specialization")){
            if(m(p,"prospector/brewer")&&cd(p,"m62_brewer_cleanse",1200L))removeOneNegative(p);
        }
    }

    @SubscribeEvent
    public static void trade(TradeWithVillagerEvent e){
        if(!(e.getEntity() instanceof ServerPlayer p))return;
        if(!h(p,"treasure_hunter/broker_envoy/specialization"))return;
        float chance=m(p,"treasure_hunter/broker_envoy")?.20f:.10f;
        if(p.getRandom().nextFloat()<chance){
            ItemStack paid=e.getMerchantOffer().getCostA().copy();int refund=Math.max(1,(int)Math.floor(paid.getCount()*.20));paid.setCount(Math.min(paid.getCount(),refund));give(p,paid);
        }
        boostMcaRelationship(e.getAbstractVillager(),p,m(p,"treasure_hunter/broker_envoy")?.50:.20);
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent e){
        if(e.phase!=TickEvent.Phase.END||!(e.player instanceof ServerPlayer p)||!p.isAlive()||p.isRemoved()||p.tickCount%10!=0)return;
        long n=now(p);double[] old=LAST_POS.get(p.getUUID());double[] cur={p.getX(),p.getY(),p.getZ()};
        if(old!=null&&dist2(old,cur)<.01)AbilityState.setInt(p,MARKSMAN_STILL,Math.min(80,AbilityState.getInt(p,MARKSMAN_STILL)+10));else AbilityState.setInt(p,MARKSMAN_STILL,0);
        LAST_POS.put(p.getUUID(),cur);

        boolean evasionNow=hasEffectPath(p,"evasion");
        int evasionSeen=AbilityState.getInt(p,"m62_evasion_seen");
        if(evasionNow&&evasionSeen==0){
            if(h(p,"ghost/phantom/specialization")&&cd(p,"m62_phantom_roll",160L)){
                effect(p,"irons_spellbooks:true_invisibility",m(p,"ghost/phantom")?3:2,0);
                AbilityState.setLong(p,PHANTOM_WINDOW,n+60L);
            }
            if(h(p,"opportunist/duelist/specialization")){
                AbilityState.setLong(p,DUEL_ROLL,n+60L);
            }
        }
        AbilityState.setInt(p,"m62_evasion_seen",evasionNow?1:0);

        double[] miragePos=MIRAGE_REPOSITION.get(p.getUUID());
        if(miragePos!=null&&n>=(long)miragePos[0]){
            double dx=miragePos[1]-p.getX(),dy=miragePos[2]-p.getY(),dz=miragePos[3]-p.getZ();
            if(p.level().noCollision(p,p.getBoundingBox().move(dx,dy,dz))){
                p.teleportTo(miragePos[1],miragePos[2],miragePos[3]);
            }
            MIRAGE_REPOSITION.remove(p.getUUID());
        }

        if(n>AbilityState.getLong(p,SPECTER_SOUL_UNTIL))AbilityState.setInt(p,SPECTER_SOUL,0);
        if(n>AbilityState.getLong(p,OCCULT_SOUL_UNTIL))AbilityState.setInt(p,OCCULT_SOUL,0);
        if(n>AbilityState.getLong(p,TEMPO_UNTIL))AbilityState.setInt(p,TEMPO,0);
        if(n>AbilityState.getLong(p,RESOLVE_UNTIL))AbilityState.setInt(p,RESOLVE,0);
        if(n>AbilityState.getLong(p,EARTH_UNTIL))AbilityState.setInt(p,EARTH,0);
        if(n>AbilityState.getLong(p,FLOOD_UNTIL))AbilityState.setInt(p,FLOOD,0);

        // Revenant reacts to the root Second Wind serial instead of creating a second death-save.
        int secondWindSerial=AbilityState.getInt(p,"m62_second_wind_serial");
        int revenantSeen=AbilityState.getInt(p,REV_SEEN);
        if(!h(p,"second_wind/revenant/specialization")){
            AbilityState.setInt(p,REV_SEEN,secondWindSerial);
        }else if(secondWindSerial>revenantSeen){
            AbilityState.setInt(p,REV_SEEN,secondWindSerial);
            AbilityState.setLong(p,REV_UNTIL,n+160L);
            AbilityState.setLong(p,REV_PENALTY_AT,n+160L);
            AbilityState.setInt(p,REV_PENALTY_DONE,0);
            vanilla(p,MobEffects.REGENERATION,6,1);
            if(m(p,"second_wind/revenant"))effect(p,"irons_spellbooks:vigor",8,1);
        }
        if(h(p,"second_wind/revenant/specialization")
                && AbilityState.getLong(p,REV_PENALTY_AT)>0
                && n>AbilityState.getLong(p,REV_PENALTY_AT)
                && AbilityState.getInt(p,REV_PENALTY_DONE)==0){
            vanilla(p,MobEffects.WEAKNESS,12,1);
            effect(p,"attributeslib:grievous",20,0);
            AbilityState.setInt(p,REV_PENALTY_DONE,1);
        }
        if(m(p,"survivor/last_stand")
                && AbilityState.getLong(p,"m62_laststand_penalty_at")>0
                && n>AbilityState.getLong(p,"m62_laststand_penalty_at")
                && AbilityState.getInt(p,"m62_laststand_penalty_done")==0){
            effect(p,"attributeslib:grievous",20,1);
            AbilityState.setInt(p,"m62_laststand_penalty_done",1);
        }

        // Channeler Flow expires after 4 sec. without a cast; Mastery turns the break into a short mana-cost penalty.
        int flow=AbilityState.getInt(p,"m62_flow");
        if(flow>0&&n-AbilityState.getLong(p,"m62_flow_last")>80L){
            AbilityState.setInt(p,"m62_flow",0);
            if(m(p,"manaflow/channeler"))AbilityState.setLong(p,"m62_flow_penalty",n+100L);
        }

        // Timed attribute states.
        setAttr(p,"attributeslib:crit_chance","nb_reward",n<=AbilityState.getLong(p,NB_REWARD)?.20:0);
        setAttr(p,"minecraft:generic.movement_speed","nb_reward_move",n<=AbilityState.getLong(p,NB_REWARD)?.15:0);
        setAttr(p,"minecraft:generic.armor","duel_expose",n<=AbilityState.getLong(p,DUEL_EXPOSE)?-.15:0);
        setAttr(p,"minecraft:generic.armor","rift_expose",n<=AbilityState.getLong(p,"m62_rift_expose")?-.15:0);
        setAttr(p,"minecraft:generic.movement_speed","mirage_move",n<=AbilityState.getLong(p,"m62_mirage_move")?.10:0);
        setAttr(p,"minecraft:generic.attack_speed","berserker_mastery",h(p,"juggernaut/berserker/specialization")&&m(p,"juggernaut/berserker")&&p.getHealth()<=p.getMaxHealth()*.50?.10:0);
        setAttr(p,"minecraft:generic.movement_speed","blood_slow",AbilityState.getInt(p,BLOOD)>=3?-.12:0);
        setAttr(p,"minecraft:generic.attack_speed","resolve_slow",AbilityState.getInt(p,RESOLVE)>=5&&m(p,"second_wind/ironheart")?-.15:0);
        setElementSchoolPower(p,"earth","stone_geo",n<=AbilityState.getLong(p,STONE_UNTIL)&&m(p,"unyielding/stoneguard")?.20:0);
        setAttr(p,"minecraft:generic.armor","spellguard_physical",n<=AbilityState.getLong(p,SPELLGUARD_UNTIL)&&m(p,"unyielding/spellguard")?-.15:0);

        // Reservoir.
        if(h(p,"manaflow/reservoir/specialization")){
            double mana=manaRatio(p);
            double power=0,resist=0,move=0;
            if(mana>=0){
                if(m(p,"manaflow/reservoir")&&mana>=.80){power=.30;resist=.15;}
                else if(mana>=.70)power=.15;
                if(m(p,"manaflow/reservoir")&&mana<.25){power=-.25;move=-.20;}
            }
            setAttr(p,"irons_spellbooks:spell_power","reservoir_power",power);
            setAttr(p,"irons_spellbooks:spell_resist","reservoir_resist",resist);
            setAttr(p,"minecraft:generic.movement_speed","reservoir_move",move);
        }else{
            setAttr(p,"irons_spellbooks:spell_power","reservoir_power",0);
            setAttr(p,"irons_spellbooks:spell_resist","reservoir_resist",0);
            setAttr(p,"minecraft:generic.movement_speed","reservoir_move",0);
        }

        // Arcanist Weave and temporary Mastery burst.
        int weave=AbilityState.getInt(p,"m62_weave");
        boolean weaveAlive=weave>0&&n-AbilityState.getLong(p,"m62_weave_last")<=120L;
        setAttr(p,"irons_spellbooks:spell_power","weave_power",weaveAlive?weave*.04:0);
        setAttr(p,"irons_spellbooks:cooldown_reduction","weave_cdr",weaveAlive?weave*.03:0);
        boolean weaveBurst=n<=AbilityState.getLong(p,"m62_weave_burst_until");
        setAttr(p,"irons_spellbooks:spell_power","weave_burst_power",weaveBurst?.30:0);
        setAttr(p,"irons_spellbooks:cooldown_reduction","weave_burst_cdr",weaveBurst?.40:0);

        // Occult finisher and Geomancer burst cleanup.
        setOccultSchoolPower(p,"occult_burst",n<=AbilityState.getLong(p,OCCULT_BURST)?.30:0);
        setAttr(p,"irons_spellbooks:spell_power","geo_burst",n<=AbilityState.getLong(p,"m62_geo_burst_until")?.35:0);

        // Aegis specialization: use the real registered Iron's/addon school when available.
        String aegisSchool=AbilityState.getString(p,AEGIS_SCHOOL);
        boolean aegisAlive=!aegisSchool.isEmpty()&&n<=AbilityState.getLong(p,AEGIS_UNTIL);
        setDynamicAegisResists(p,aegisAlive?aegisSchool:"",m(p,"arcane_ward/aegis"));

        String burstSchool=AbilityState.getString(p,AEGIS_BURST_SCHOOL);
        boolean aegisBurst=!burstSchool.isEmpty()&&n<=AbilityState.getLong(p,AEGIS_BURST_UNTIL);
        setDynamicSchoolPowerById(p,aegisBurst?burstSchool:"","aegis_burst_exact",aegisBurst?.25:0);

        // Technomancer Mastery: Overcharged, then a short crash.
        boolean overcharged=n<=AbilityState.getLong(p,"m62_overcharged");
        boolean overchargeCrash=!overcharged&&n<=AbilityState.getLong(p,"m62_overcharge_crash");
        setAttr(p,"irons_spellbooks:spell_power","technomancer_overcharge",overcharged?.20:(overchargeCrash?-.20:0));
        setAttr(p,"minecraft:generic.armor","technomancer_overcharge_armor",overcharged?.15:0);
        if(overchargeCrash)vanilla(p,MobEffects.MOVEMENT_SLOWDOWN,2,0);

        int tempo=AbilityState.getInt(p,TEMPO);
        setAttr(p,"minecraft:generic.attack_speed","tempo",tempo*.04);
        setAttrAdd(p,"attributeslib:crit_chance","tempo_crit",tempo*.02);

        int specterSouls=AbilityState.getInt(p,SPECTER_SOUL);
        int occultSouls=AbilityState.getInt(p,OCCULT_SOUL);
        setOccultSchoolPower(p,"specter_soul",h(p,"ghost/specter/specialization")?specterSouls*.04:0);
        setOccultSchoolPower(p,"occult_soul",h(p,"archmage/occultist/specialization")?occultSouls*.05:0);
        setAttr(p,"attributeslib:healing_received","specter_soul_heal",h(p,"ghost/specter/specialization")&&specterSouls>0?-.25:0);
        setAttr(p,"attributeslib:healing_received","occult_soul_heal",h(p,"archmage/occultist/specialization")&&occultSouls>0?-.25:0);

        int flowStacks=AbilityState.getInt(p,"m62_flow");
        setAttr(p,"irons_spellbooks:spell_power","channeler_flow_power",
                h(p,"manaflow/channeler/specialization")&&m(p,"manaflow/channeler")&&flowStacks>=5?.10:0);

        if(h(p,"archmage/elementalist/specialization")){
            if(n>AbilityState.getLong(p,"m62_attune_until"))clearElementalPowers(p,"element_attune");
            if(n>AbilityState.getLong(p,"m62_avatar_until"))clearElementalPowers(p,"element_avatar");
        }

        int earth=AbilityState.getInt(p,EARTH);
        setElementSchoolPower(p,"earth","earth_charge",h(p,"enduring_tools/geomancer/specialization")?earth*.04:0);

        // Architect and Fortifier.
        int rhythm=AbilityState.getInt(p,"builder_rhythm");
        double reach=h(p,"master_builder/architect/specialization")&&rhythm>=4?(m(p,"master_builder/architect")?3.0:1.5):0;
        setAttrAdd(p,"forge:block_reach","architect_reach",reach);
        setAttr(p,"attributeslib:mining_speed","architect_zone",n<=AbilityState.getLong(p,ARCH_ZONE)?.20:0);
        if(h(p,"master_builder/fortifier/specialization")&&m(p,"master_builder/fortifier")&&n<=AbilityState.getLong(p,FORT_UNTIL)){
            BlockPos pos=BlockPos.of(AbilityState.getLong(p,FORT_BLOCK));boolean near=p.blockPosition().distSqr(pos)<=36;
            setAttrAdd(p,"minecraft:generic.armor","fortifier_armor",near?4:0);setAttrAdd(p,"minecraft:generic.knockback_resistance","fortifier_kb",near?.30:0);
        }else{setAttrAdd(p,"minecraft:generic.armor","fortifier_armor",0);setAttrAdd(p,"minecraft:generic.knockback_resistance","fortifier_kb",0);}

        // Deep Delver.
        if(h(p,"enduring_tools/deep_delver/specialization")){
            if(p.getY()<32){vanilla(p,MobEffects.DIG_SPEED,2,p.getY()<0&&m(p,"enduring_tools/deep_delver")?1:0);vanilla(p,MobEffects.DAMAGE_RESISTANCE,2,0);if(p.getY()<0&&m(p,"enduring_tools/deep_delver"))effect(p,"irons_spellbooks:fortify",2,0);}
            else if(p.getY()>80&&m(p,"enduring_tools/deep_delver"))vanilla(p,MobEffects.MOVEMENT_SLOWDOWN,2,0);
        }

        // Paladin self Vigor and personal downside while an ally is nearby.
        if(h(p,"second_wind/paladin/specialization")&&hasAlly(p))effect(p,"irons_spellbooks:vigor",2,0);

        // Dragon rider defensive side.
        if(h(p,"pathfinder/dragon_rider/specialization")&&isIceFireMount(p.getVehicle())){vanilla(p,MobEffects.FIRE_RESISTANCE,2,0);if(m(p,"pathfinder/dragon_rider"))effect(p,"irons_spellbooks:fortify",2,0);}

        // Skirmisher's extra roll belongs to Mastery, not Windrunner root.
        setAttrAdd(p,"combatroll:count","skirmisher_roll",m(p,"windrunner/skirmisher")?1.0:0);

        // Pilgrim.
        if(h(p,"wayfarer/pilgrim/specialization")){
            int mv=AbilityState.getInt(p,"m62_pilgrim");
            int stopped=AbilityState.getInt(p,"m62_pilgrim_stopped");
            boolean outOfCombat=n>AbilityState.getLong(p,"m62_pilgrim_combat_until");
            boolean moving=old!=null&&dist2(old,cur)>.04;
            if(!outOfCombat){
                mv=0;stopped=0;
            }else if(moving){
                mv=Math.min(600,mv+10);stopped=0;
            }else{
                stopped=Math.min(100,stopped+10);
                if(stopped>=100&&mv>0){
                    mv=0;
                    if(m(p,"wayfarer/pilgrim")&&cd(p,"m62_pilgrim_stop_price",100L))vanilla(p,MobEffects.WEAKNESS,5,0);
                }
            }
            AbilityState.setInt(p,"m62_pilgrim",mv);
            AbilityState.setInt(p,"m62_pilgrim_stopped",stopped);
            if(mv>=600){
                vanilla(p,MobEffects.MOVEMENT_SPEED,2,0);
                effect(p,"irons_spellbooks:vigor",2,0);
                if(m(p,"wayfarer/pilgrim")){
                    vanilla(p,MobEffects.REGENERATION,2,0);
                    for(ServerPlayer ally:nearbyPlayers(p,8))vanilla(ally,MobEffects.MOVEMENT_SPEED,2,0);
                }
            }
        }

        // Cartographer first-biome reward.
        ResourceLocation biome=p.serverLevel().registryAccess().registryOrThrow(Registries.BIOME).getKey(p.serverLevel().getBiome(p.blockPosition()).value());
        if(biome!=null&&h(p,"wayfarer/cartographer/specialization")){
            boolean fresh=AbilityState.markOnce(p,"m62_cartographer",biome.toString());
            if(fresh){effectPath(p,"astral_sense",m(p,"wayfarer/cartographer")?20:12,m(p,"wayfarer/cartographer")?1:0);vanilla(p,MobEffects.LUCK,m(p,"wayfarer/cartographer")?20:12,m(p,"wayfarer/cartographer")?1:0);}
            setAttrAdd(p,"minecraft:generic.luck","cartographer_old",!fresh&&m(p,"wayfarer/cartographer")?-.5:0);
        }

        // Relic resonance Luck.
        if(h(p,"treasure_hunter/relic_seeker/specialization")){int c=Math.min(m(p,"treasure_hunter/relic_seeker")?5:3,countRelics(p));setAttrAdd(p,"minecraft:generic.luck","relic_luck",.25*c);}

        // Feast.
        boolean feast=n<=AbilityState.getLong(p,FEAST_UNTIL);
        setAttr(p,"minecraft:generic.max_health","feast_hp",feast?.10:0);
        setAttr(p,"attributeslib:healing_received","feast_heal",feast?.10:0);
        if(!feast&&m(p,"prospector/feastmaster")&&AbilityState.getLong(p,FEAST_UNTIL)>0&&AbilityState.getInt(p,FEAST_WEAK_DONE)==0){vanilla(p,MobEffects.WEAKNESS,20,0);AbilityState.setInt(p,FEAST_WEAK_DONE,1);}
        boolean feastAlly=hasNearbyFeastAura(p,n);
        setAttr(p,"minecraft:generic.attack_damage","feast_ally_damage",feastAlly?.10:0);
        setAttr(p,"minecraft:generic.movement_speed","feast_ally_move",feastAlly?.10:0);

        // Timed mastery penalties/effects.
        setAttr(p,"minecraft:generic.movement_speed","chef_slow",n<=AbilityState.getLong(p,"m62_chef_slow")?-.15:0);
        tickBrewerNegativeDuration(p);
    }

    private static boolean hasNearbyFeastAura(ServerPlayer target,long n){
        for(ServerPlayer source:nearbyPlayers(target,8)){
            if(source!=target&&m(source,"prospector/feastmaster")&&n<=AbilityState.getLong(source,FEAST_AURA))return true;
        }
        return false;
    }

    private static String magicElementFromDamage(net.minecraft.world.damagesource.DamageSource source){
        String z=damageType(source).toLowerCase(Locale.ROOT);
        if(z.contains("fire")||z.contains("flame")||z.contains("burn"))return"fire";
        if(z.contains("ice")||z.contains("frost")||z.contains("freeze"))return"ice";
        if(z.contains("lightning")||z.contains("thunder")||z.contains("electric"))return"lightning";
        if(z.contains("wind")||z.contains("air")||z.contains("gust"))return"wind";
        if(z.contains("earth")||z.contains("geo"))return"earth";
        if(z.contains("water")||z.contains("aqua"))return"water";
        if(z.contains("nature")||z.contains("verdant")||z.contains("poison"))return"nature";
        if(z.contains("holy"))return"holy";
        if(z.contains("ender"))return"ender";
        if(z.contains("blood"))return"blood";
        if(z.contains("evocation")||z.contains("arcane"))return"evocation";
        if(z.contains("eldritch"))return"eldritch";
        if(z.contains("abyss"))return"abyssal";
        return"";
    }

    private static boolean isOccultDamage(net.minecraft.world.damagesource.DamageSource source){
        String z=damageType(source).toLowerCase(Locale.ROOT);
        return z.contains("blood")||z.contains("ender")||z.contains("eldritch")||z.contains("abyss");
    }

    private static double manaRatio(ServerPlayer p){
        try{
            Class<?> md=Class.forName("io.redspace.ironsspellbooks.api.magic.MagicData");
            Object data=md.getMethod("getPlayerMagicData",net.minecraft.world.entity.player.Player.class).invoke(null,p);
            double mana=((Number)data.getClass().getMethod("getMana").invoke(data)).doubleValue();
            var at=ForgeRegistries.ATTRIBUTES.getValue(id("irons_spellbooks:max_mana"));
            double max=at==null?0:p.getAttributeValue(at);
            return max<=0?-1:mana/max;
        }catch(Throwable ignored){return -1;}
    }

    private static ServerPlayer nearbyPaladin(ServerPlayer target,boolean masteryOnly){
        for(ServerPlayer p:nearbyPlayers(target,8))if(p!=target&&h(p,"second_wind/paladin/specialization")&&(!masteryOnly||m(p,"second_wind/paladin")))return p;return null;
    }
    private static boolean hasAlly(ServerPlayer p){for(ServerPlayer x:nearbyPlayers(p,8))if(x!=p)return true;return findFriendlyMca(p)!=null;}
    private static List<ServerPlayer> nearbyPlayers(ServerPlayer p,double r){return p.level().getEntitiesOfClass(ServerPlayer.class,p.getBoundingBox().inflate(r),x->x.isAlive()&&!x.isSpectator());}

    private static void registerFeastItem(ServerPlayer p,ItemStack stack,long n){
        long start=FEAST_START.getOrDefault(p.getUUID(),0L);LinkedHashSet<String> set=FEAST_ITEMS.computeIfAbsent(p.getUUID(),u->new LinkedHashSet<>());
        if(start==0||n-start>1200L){set.clear();FEAST_START.put(p.getUUID(),n);}
        ResourceLocation k=ForgeRegistries.ITEMS.getKey(stack.getItem());if(k!=null)set.add(k.toString());
        if(set.size()>=3){AbilityState.setLong(p,FEAST_UNTIL,n+2400L);AbilityState.setInt(p,FEAST_WEAK_DONE,0);}
        if(set.size()>=4&&m(p,"prospector/feastmaster"))AbilityState.setLong(p,FEAST_AURA,n+1800L);
    }

    private static boolean ownedCook(ServerPlayer p,ItemStack s){return !s.isEmpty()&&s.hasTag()&&s.getTag().hasUUID(COOK_OWNER)&&p.getUUID().equals(s.getTag().getUUID(COOK_OWNER));}
    private static void tickBrewerNegativeDuration(ServerPlayer p){
        if(!m(p,"prospector/brewer")){
            BREWER_NEGATIVE_SEEN.remove(p.getUUID());
            return;
        }
        Map<ResourceLocation,Integer> seen=BREWER_NEGATIVE_SEEN.computeIfAbsent(p.getUUID(),u->new HashMap<>());
        Set<ResourceLocation> active=new HashSet<>();
        List<MobEffectInstance> boosted=new ArrayList<>();
        for(MobEffectInstance inst:new ArrayList<>(p.getActiveEffects())){
            if(inst.getEffect().isBeneficial())continue;
            ResourceLocation key=ForgeRegistries.MOB_EFFECTS.getKey(inst.getEffect());
            if(key==null)continue;
            active.add(key);
            int prev=seen.getOrDefault(key,-1);
            int current=inst.getDuration();
            if(prev<0||current>prev+5){
                int duration=(int)Math.ceil(current*1.25);
                boosted.add(new MobEffectInstance(inst.getEffect(),duration,inst.getAmplifier(),inst.isAmbient(),inst.isVisible(),inst.showIcon()));
                seen.put(key,duration);
            }else{
                seen.put(key,current);
            }
        }
        seen.keySet().retainAll(active);
        for(MobEffectInstance inst:boosted)p.addEffect(inst);
    }
    private static void removeOneNegative(ServerPlayer p){for(MobEffectInstance inst:new ArrayList<>(p.getActiveEffects()))if(!inst.getEffect().isBeneficial()){p.removeEffect(inst.getEffect());return;}}

    private static void boostMcaRelationship(Entity villager,ServerPlayer p,double extra){
        try{
            Object brain=villager.getClass().getMethod("getVillagerBrain").invoke(villager);
            Object mem=brain.getClass().getMethod("getMemoriesForPlayer",net.minecraft.world.entity.player.Player.class).invoke(brain,p);
            Method get=mem.getClass().getMethod("getHearts");int hearts=((Number)get.invoke(mem)).intValue();int add=Math.max(1,(int)Math.round(Math.max(1,Math.abs(hearts)) * extra));
            for(String name:new String[]{"setHearts","setHeartValue"})try{mem.getClass().getMethod(name,int.class).invoke(mem,hearts+add);return;}catch(NoSuchMethodException ignored){}
        }catch(Throwable ignored){}
    }

    private static void recordTrophy(ServerPlayer p,String type){CompoundTag root=p.getPersistentData().getCompound(SPEC_ROOT);CompoundTag trophies=root.getCompound("Trophies");trophies.putBoolean(type.replace(':','_').replace('/','_'),true);root.put("Trophies",trophies);p.getPersistentData().put(SPEC_ROOT,root);}
    private static int trophyCount(ServerPlayer p){CompoundTag root=p.getPersistentData().getCompound(SPEC_ROOT);return root.getCompound("Trophies").getAllKeys().size();}

    private static boolean hasEffectPath(LivingEntity e,String token){for(MobEffectInstance i:e.getActiveEffects()){ResourceLocation k=ForgeRegistries.MOB_EFFECTS.getKey(i.getEffect());if(k!=null&&k.getPath().contains(token))return true;}return false;}
    private static double dist2(double[]a,double[]b){double x=a[0]-b[0],y=a[1]-b[1],z=a[2]-b[2];return x*x+y*y+z*z;}
    private static boolean isIceFireMount(Entity e){if(e==null)return false;ResourceLocation k=ForgeRegistries.ENTITY_TYPES.getKey(e.getType());return k!=null&&k.getNamespace().equals("iceandfire");}
    private static int weaponLevel(ItemStack s){try{Class<?> c=Class.forName("net.weaponleveling.api.LevelingAPI");return (Integer)c.getMethod("getLevel",ItemStack.class).invoke(null,s);}catch(Throwable ignored){return 0;}}
    private static int countRelics(ServerPlayer p){try{Class<?> api=Class.forName("top.theillusivec4.curios.api.CuriosApi");Object lazy=api.getMethod("getCuriosInventory",LivingEntity.class).invoke(null,p);Object opt=lazy.getClass().getMethod("resolve").invoke(lazy);Object handler=((Optional<?>)opt).orElse(null);if(handler==null)return 0;Object inv=handler.getClass().getMethod("getEquippedCurios").invoke(handler);Method slots=inv.getClass().getMethod("getSlots"),get=inv.getClass().getMethod("getStackInSlot",int.class);int n=((Number)slots.invoke(inv)).intValue(),c=0;for(int i=0;i<n;i++){ItemStack s=(ItemStack)get.invoke(inv,i);String ns=ns(s);if(ns.equals("relics")||ns.equals("artifacts"))c++;}return c;}catch(Throwable ignored){return 0;}}
    private static LivingEntity findFriendlyMca(ServerPlayer p){for(LivingEntity e:p.level().getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(10),x->x!=p&&x.getClass().getName().toLowerCase(Locale.ROOT).contains("mca"))){try{Object brain=e.getClass().getMethod("getVillagerBrain").invoke(e);Object mem=brain.getClass().getMethod("getMemoriesForPlayer",net.minecraft.world.entity.player.Player.class).invoke(brain,p);int hearts=((Number)mem.getClass().getMethod("getHearts").invoke(mem)).intValue();if(hearts>=20)return e;}catch(Throwable ignored){}}return null;}
    private static void spawnDecoys(ServerPlayer p,int count,int ticks){try{Class<?> c=Class.forName("com.hollingsworth.arsnouveau.common.entity.EntityDummy");var ctor=c.getConstructor(Level.class);for(int i=0;i<count;i++){Object o=ctor.newInstance(p.level());if(!(o instanceof Entity ent))continue;c.getMethod("setTicksLeft",int.class).invoke(o,ticks);c.getMethod("setOwnerID",UUID.class).invoke(o,p.getUUID());double side=i==0?1.5:-1.5;ent.setPos(p.getX()+side,p.getY(),p.getZ());p.level().addFreshEntity(ent);if(ent instanceof LivingEntity le)for(Mob mob:p.level().getEntitiesOfClass(Mob.class,le.getBoundingBox().inflate(20,10,20),x->x!=le))mob.setTarget(le);}}catch(Throwable ignored){effect(p,"irons_spellbooks:evasion",3,0);}}

    static void setAttr(ServerPlayer p,String attr,String key,double amount){UUID u=UUID.nameUUIDFromBytes(("eldenworld:m62:"+key).getBytes(StandardCharsets.UTF_8));AbilityUtil.setDynamicAttributeModifier(p,id(attr),u,"EldenWorld "+key,amount,AttributeModifier.Operation.MULTIPLY_TOTAL);}
    static void setAttrAdd(ServerPlayer p,String attr,String key,double amount){UUID u=UUID.nameUUIDFromBytes(("eldenworld:m62:add:"+key).getBytes(StandardCharsets.UTF_8));AbilityUtil.setDynamicAttributeModifier(p,id(attr),u,"EldenWorld "+key,amount,AttributeModifier.Operation.ADDITION);}
    private static int schoolAttributeScore(ResourceLocation attr,String rawSchool){
        if(rawSchool==null||rawSchool.isEmpty())return Integer.MIN_VALUE;
        ResourceLocation school;
        try{school=id(rawSchool.contains(":")?rawSchool:"minecraft:"+rawSchool);}
        catch(RuntimeException ex){return Integer.MIN_VALUE;}
        String q=attr.getPath().toLowerCase(Locale.ROOT);
        String sp=school.getPath().toLowerCase(Locale.ROOT);
        int score=0;
        if(attr.getNamespace().equals(school.getNamespace()))score+=100;
        if(q.contains(sp))score+=80;
        for(String token:sp.split("[_/\\.-]+")){
            if(token.length()>=3&&q.contains(token))score+=10;
        }
        if(sp.equals("geo")&&q.contains("earth"))score+=80;
        if(sp.equals("aqua")&&q.contains("water"))score+=80;
        return score;
    }

    private static void setDynamicAegisResists(ServerPlayer p,String rawSchool,boolean mastery){
        ResourceLocation best=null;int bestScore=Integer.MIN_VALUE;
        for(ResourceLocation rl:ForgeRegistries.ATTRIBUTES.getKeys()){
            String q=rl.getPath().toLowerCase(Locale.ROOT);
            if(!q.contains("magic_resist"))continue;
            int score=schoolAttributeScore(rl,rawSchool);
            if(score>bestScore){bestScore=score;best=rl;}
        }
        for(ResourceLocation rl:ForgeRegistries.ATTRIBUTES.getKeys()){
            String q=rl.getPath().toLowerCase(Locale.ROOT);
            if(!q.contains("magic_resist"))continue;
            UUID u=UUID.nameUUIDFromBytes(("eldenworld:m62:aegis_registry:"+rl).getBytes(StandardCharsets.UTF_8));
            double amount=0;
            if(best!=null&&!rawSchool.isEmpty()){
                if(rl.equals(best))amount=mastery?.35:.20;
                else if(mastery)amount=-.15;
            }
            AbilityUtil.setDynamicAttributeModifier(p,rl,u,"EldenWorld Aegis registry",amount,AttributeModifier.Operation.MULTIPLY_TOTAL);
        }
    }

    private static void setDynamicSchoolPowerById(ServerPlayer p,String rawSchool,String key,double amount){
        ResourceLocation best=null;int bestScore=Integer.MIN_VALUE;
        for(ResourceLocation rl:ForgeRegistries.ATTRIBUTES.getKeys()){
            String q=rl.getPath().toLowerCase(Locale.ROOT);
            if(!q.contains("spell_power"))continue;
            int score=schoolAttributeScore(rl,rawSchool);
            if(score>bestScore){bestScore=score;best=rl;}
        }
        for(ResourceLocation rl:ForgeRegistries.ATTRIBUTES.getKeys()){
            String q=rl.getPath().toLowerCase(Locale.ROOT);
            if(!q.contains("spell_power"))continue;
            UUID u=UUID.nameUUIDFromBytes(("eldenworld:m62:"+key+":"+rl).getBytes(StandardCharsets.UTF_8));
            double v=(best!=null&&rl.equals(best))?amount:0;
            AbilityUtil.setDynamicAttributeModifier(p,rl,u,"EldenWorld "+key,v,AttributeModifier.Operation.MULTIPLY_TOTAL);
        }
    }

    static void setElementSchoolPower(ServerPlayer p,String element,String key,double amount){
        for(ResourceLocation rl:ForgeRegistries.ATTRIBUTES.getKeys())if(isSchoolPowerAttribute(rl,element)){UUID u=UUID.nameUUIDFromBytes(("eldenworld:m62:"+key+":"+rl).getBytes(StandardCharsets.UTF_8));AbilityUtil.setDynamicAttributeModifier(p,rl,u,"EldenWorld "+key,amount,AttributeModifier.Operation.MULTIPLY_TOTAL);}
    }
    static void setElementSchoolResist(ServerPlayer p,String element,String key,double amount){
        for(ResourceLocation rl:ForgeRegistries.ATTRIBUTES.getKeys()){String q=rl.getPath().toLowerCase(Locale.ROOT);if((q.contains("resist"))&&schoolToken(q,element)){UUID u=UUID.nameUUIDFromBytes(("eldenworld:m62:"+key+":"+rl).getBytes(StandardCharsets.UTF_8));AbilityUtil.setDynamicAttributeModifier(p,rl,u,"EldenWorld "+key,amount,AttributeModifier.Operation.MULTIPLY_TOTAL);}}
    }
    private static boolean isSchoolPowerAttribute(ResourceLocation rl,String element){String q=rl.getPath().toLowerCase(Locale.ROOT);return q.contains("spell_power")&&schoolToken(q,element);}
    private static boolean schoolToken(String q,String element){return switch(element){case"earth"->q.contains("earth")||q.contains("geo");case"water"->q.contains("water")||q.contains("aqua");default->q.contains(element);};}
    static void setOccultSchoolPower(ServerPlayer p,String key,double amount){for(String s:new String[]{"blood","ender","eldritch","abyssal"})setElementSchoolPower(p,s,key+"_"+s,amount);}
    static void clearElementalPowers(ServerPlayer p,String key){for(String s:new String[]{"fire","ice","lightning","wind","earth","water","nature"})setElementSchoolPower(p,s,key+"_"+s,0);}
    static void applyEffect(ServerPlayer p,String rid,int sec,int amp){effect(p,rid,sec,amp);}
    static void applyEffectPath(ServerPlayer p,String token,int sec,int amp){effectPath(p,token,sec,amp);}
    static boolean has(ServerPlayer p,String s){return h(p,s);}
    static boolean mastery(ServerPlayer p,String s){return m(p,s);}
    static long time(ServerPlayer p){return now(p);}
    static int specterSouls(ServerPlayer p){return AbilityState.getInt(p,SPECTER_SOUL);}
    static void gainSpecterSoul(ServerPlayer p){addSpecterSoul(p);}
    static void clearSpecterSouls(ServerPlayer p){AbilityState.setInt(p,SPECTER_SOUL,0);AbilityState.setLong(p,SPECTER_SOUL_UNTIL,0);}
    static void applyEffectTo(LivingEntity e,String rid,int sec,int amp){effect(e,rid,sec,amp);}
    static void applyEffectPathTo(LivingEntity e,String token,int sec,int amp){effectPath(e,token,sec,amp);}
        static int souls(ServerPlayer p){return AbilityState.getInt(p,OCCULT_SOUL);}
    static void gainSoul(ServerPlayer p){addOccultSoul(p);}
    static void clearSouls(ServerPlayer p){AbilityState.setInt(p,OCCULT_SOUL,0);AbilityState.setLong(p,OCCULT_SOUL_UNTIL,0);}
    static void setOccultBurst(ServerPlayer p,int ticks){AbilityState.setLong(p,OCCULT_BURST,now(p)+ticks);}
    static long occultBurstUntil(ServerPlayer p){return AbilityState.getLong(p,OCCULT_BURST);}
    static void markGale(ServerPlayer p){AbilityState.setLong(p,GALE,now(p)+100L);}
    static void consumeEarthForSpell(ServerPlayer p){if(m(p,"enduring_tools/geomancer")&&AbilityState.getInt(p,EARTH)>=5){AbilityState.setInt(p,EARTH,0);setAttr(p,"irons_spellbooks:spell_power","geo_burst",.35);AbilityState.setLong(p,"m62_geo_burst_until",now(p)+40L);vanilla(p,MobEffects.MOVEMENT_SLOWDOWN,5,0);}}
}
