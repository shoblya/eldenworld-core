package com.eldenworld.core.specialization;

import com.eldenworld.core.EldenWorldCore;
import com.eldenworld.core.abilities.AbilityState;
import com.hollingsworth.arsnouveau.api.event.SpellCastEvent;
import com.hollingsworth.arsnouveau.api.event.SpellCostCalcEvent;
import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Locale;

@Mod.EventBusSubscriber(modid = EldenWorldCore.MOD_ID)
public final class FinalSpecializationSpellEventsV2 {
    private FinalSpecializationSpellEventsV2() {}

    private static final String ELEMENT="m62_element", ELEMENT_COUNT="m62_element_count", ELEMENT_CHAIN="m62_element_chain";
    private static final String ATTUNE_UNTIL="m62_attune_until", AVATAR_UNTIL="m62_avatar_until";
    private static final String FLOW="m62_flow", FLOW_LAST="m62_flow_last", FLOW_PENALTY="m62_flow_penalty";
    private static final String WEAVE="m62_weave", WEAVE_LAST="m62_weave_last", WEAVE_SYSTEM="m62_weave_system", WEAVE_PENALTY="m62_weave_penalty";
    private static final String AEGIS_SCHOOL="m62_aegis_school", AEGIS_UNTIL="m62_aegis_until";
    private static final String SOFTWARE="m62_software", HARDWARE="m62_hardware", OVERCHARGED="m62_overcharged", OVERCHARGE_CRASH="m62_overcharge_crash";

    @SubscribeEvent
    public static void ironsCast(SpellOnCastEvent e){
        if(!(e.getEntity() instanceof ServerPlayer p))return;
        long n=FinalSpecializationEventsV2.time(p);
        String school=e.getSchoolType()==null?"":e.getSchoolType().getId().toString().toLowerCase(Locale.ROOT);
        String spell=e.getSpellId()==null?"":e.getSpellId().toLowerCase(Locale.ROOT);

        String elemental=element(school,spell);
        if(FinalSpecializationEventsV2.has(p,"archmage/elementalist/specialization")&&!elemental.isEmpty()){
            handleElementalist(p,elemental,n,e);
        }

        if(FinalSpecializationEventsV2.has(p,"archmage/occultist/specialization")&&isOccult(school,spell)){
            handleOccultist(p,n);
        }

        if(FinalSpecializationEventsV2.has(p,"archmage/arcanist/specialization")){
            boolean repeated=handleWeave(p,"iron",n);
            if(repeated&&FinalSpecializationEventsV2.mastery(p,"archmage/arcanist"))e.setManaCost((int)Math.ceil(e.getManaCost()*1.25));
        }

        if(FinalSpecializationEventsV2.has(p,"manaflow/channeler/specialization")){
            int flow;
            long last=AbilityState.getLong(p,FLOW_LAST);
            if(n-last<=80L)flow=Math.min(5,AbilityState.getInt(p,FLOW)+1);else flow=1;
            AbilityState.setInt(p,FLOW,flow);AbilityState.setLong(p,FLOW_LAST,n);
            double factor=1.0-.03*flow;
            if(FinalSpecializationEventsV2.mastery(p,"manaflow/channeler")&&flow>=5)factor=.75;
            if(n<=AbilityState.getLong(p,FLOW_PENALTY))factor*=1.30;
            e.setManaCost(Math.max(0,(int)Math.ceil(e.getManaCost()*factor)));
        }

        if(FinalSpecializationEventsV2.has(p,"manaflow/overcaster/specialization")){
            boolean mm=FinalSpecializationEventsV2.mastery(p,"manaflow/overcaster");
            e.setSpellLevel(e.getSpellLevel()+(mm?2:1));
            e.setManaCost((int)Math.ceil(e.getManaCost()*(mm?1.45:1.25)));
            if(mm)FinalSpecializationEventsV2.applyEffect(p,"irons_spellbooks:soul_burn",3,0);
        }

        if(FinalSpecializationEventsV2.has(p,"arcane_ward/aegis/specialization")&&FinalSpecializationEventsV2.mastery(p,"arcane_ward/aegis")){
            String ward=AbilityState.getString(p,AEGIS_SCHOOL);
            if(!ward.isEmpty()&&ward.equals(elemental)&&n<=AbilityState.getLong(p,AEGIS_UNTIL)){
                AbilityState.setLong(p,"m62_aegis_burst_until",n+40L);
                AbilityState.setLong(p,AEGIS_UNTIL,0L);
            }
        }

        if(FinalSpecializationEventsV2.has(p,"master_builder/engineer/specialization")){
            // Engineer keeps the utility identity; spell casting itself has no extra combat multiplier.
        }

        if(FinalSpecializationEventsV2.has(p,"enduring_tools/geomancer/specialization")&&"earth".equals(elemental)){
            FinalSpecializationEventsV2.consumeEarthForSpell(p);
        }

        if(FinalSpecializationEventsV2.has(p,"windrunner/gale_dancer/specialization")&&"wind".equals(elemental)){
            FinalSpecializationEventsV2.markGale(p);
        }

        if(FinalSpecializationEventsV2.has(p,"arcane_ward/technomancer/specialization")){
            AbilityState.setLong(p,SOFTWARE,n+100L);
            FinalSpecializationEventsV2.applyEffectPath(p,"software",5,0);
            combineTechnomancer(p,n);
        }
    }

    @SubscribeEvent
    public static void arsCast(SpellCastEvent e){
        if(!(e.getEntity() instanceof ServerPlayer p))return;
        long n=FinalSpecializationEventsV2.time(p);
        if(FinalSpecializationEventsV2.has(p,"archmage/arcanist/specialization"))handleWeave(p,"ars",n);
    }

    @SubscribeEvent
    public static void arsCost(SpellCostCalcEvent e){
        if(!(e.context.getUnwrappedCaster() instanceof ServerPlayer p))return;
        if(FinalSpecializationEventsV2.has(p,"archmage/arcanist/specialization")
                &&FinalSpecializationEventsV2.mastery(p,"archmage/arcanist")
                &&FinalSpecializationEventsV2.time(p)<=AbilityState.getLong(p,WEAVE_PENALTY)){
            e.currentCost=(int)Math.ceil(e.currentCost*1.25);
        }
    }

    private static void handleElementalist(ServerPlayer p,String el,long n,SpellOnCastEvent e){
        String old=AbilityState.getString(p,ELEMENT);
        int count=(el.equals(old)&&n<=AbilityState.getLong(p,ELEMENT_CHAIN))?AbilityState.getInt(p,ELEMENT_COUNT)+1:1;
        count=Math.min(5,count);
        AbilityState.setString(p,ELEMENT,el);AbilityState.setInt(p,ELEMENT_COUNT,count);AbilityState.setLong(p,ELEMENT_CHAIN,n+160L);

        if(count>=3)AbilityState.setLong(p,ATTUNE_UNTIL,n+200L);
        if(FinalSpecializationEventsV2.mastery(p,"archmage/elementalist")&&count>=5)AbilityState.setLong(p,AVATAR_UNTIL,n+160L);

        FinalSpecializationEventsV2.clearElementalPowers(p,"element_attune");
        FinalSpecializationEventsV2.clearElementalPowers(p,"element_avatar");

        if(n<=AbilityState.getLong(p,ATTUNE_UNTIL)){
            FinalSpecializationEventsV2.setElementSchoolPower(p,el,"element_attune_"+el,.15);
        }
        if(n<=AbilityState.getLong(p,AVATAR_UNTIL)){
            for(String x:new String[]{"fire","ice","lightning","wind","earth","water","nature"}){
                FinalSpecializationEventsV2.setElementSchoolPower(p,x,"element_avatar_"+x,x.equals(el)?.35:-.25);
            }
            e.setManaCost((int)Math.ceil(e.getManaCost()*.80));
        }else{
            String active=AbilityState.getString(p,ELEMENT);
            if(!active.isEmpty()&&!active.equals(el)&&FinalSpecializationEventsV2.time(p)<=AbilityState.getLong(p,AVATAR_UNTIL)){
                e.setManaCost((int)Math.ceil(e.getManaCost()*1.20));
            }
        }
    }

    private static void handleOccultist(ServerPlayer p,long n){
        if(FinalSpecializationEventsV2.mastery(p,"archmage/occultist")&&FinalSpecializationEventsV2.souls(p)>=3){
            FinalSpecializationEventsV2.clearSouls(p);
            FinalSpecializationEventsV2.setOccultBurst(p,40);
            p.setHealth(Math.max(1.0f,p.getHealth()-p.getMaxHealth()*.05f));
        }else FinalSpecializationEventsV2.gainSoul(p);
    }

    /**
     * @return true when the same spell system was repeated and therefore breaks Weave.
     */
    private static boolean handleWeave(ServerPlayer p,String system,long n){
        String old=AbilityState.getString(p,WEAVE_SYSTEM);
        long last=AbilityState.getLong(p,WEAVE_LAST);
        boolean repeated=system.equals(old)&&n-last<=120L;
        if(repeated){
            AbilityState.setInt(p,WEAVE,0);
            AbilityState.setLong(p,WEAVE_PENALTY,n+80L);
        }else{
            int w=(n-last<=120L)?Math.min(4,AbilityState.getInt(p,WEAVE)+1):1;
            AbilityState.setInt(p,WEAVE,w);
            if(w>=4&&FinalSpecializationEventsV2.mastery(p,"archmage/arcanist")){
                AbilityState.setLong(p,"m62_weave_burst_until",n+40L);
                AbilityState.setInt(p,WEAVE,0);
            }
        }
        AbilityState.setString(p,WEAVE_SYSTEM,system);AbilityState.setLong(p,WEAVE_LAST,n);
        return repeated;
    }

    private static void combineTechnomancer(ServerPlayer p,long n){
        if(n<=AbilityState.getLong(p,SOFTWARE)&&n<=AbilityState.getLong(p,HARDWARE)){
            AbilityState.setLong(p,"m62_rewired",n+100L);
            FinalSpecializationEventsV2.applyEffectPath(p,"rewired",5,0);
            if(FinalSpecializationEventsV2.mastery(p,"arcane_ward/technomancer")){
                AbilityState.setLong(p,OVERCHARGED,n+120L);
                AbilityState.setLong(p,OVERCHARGE_CRASH,n+220L);
                FinalSpecializationEventsV2.applyEffectPath(p,"overcharged",6,0);
            }
        }
    }

    static void hardwareHit(ServerPlayer p){
        if(!FinalSpecializationEventsV2.has(p,"arcane_ward/technomancer/specialization"))return;
        long n=FinalSpecializationEventsV2.time(p);
        AbilityState.setLong(p,HARDWARE,n+100L);
        FinalSpecializationEventsV2.applyEffectPath(p,"hardware",5,0);
        combineTechnomancer(p,n);
    }

    static String element(String school,String spell){
        String z=(school+" "+spell).toLowerCase(Locale.ROOT);
        if(z.contains("fire"))return"fire";
        if(z.contains("ice")||z.contains("frost"))return"ice";
        if(z.contains("lightning")||z.contains("thunder")||z.contains("electric"))return"lightning";
        if(z.contains("wind")||z.contains("gust")||z.contains("air"))return"wind";
        if(z.contains("earth")||z.contains("geo"))return"earth";
        if(z.contains("water")||z.contains("aqua"))return"water";
        if(z.contains("nature")||z.contains("verdant"))return"nature";
        return"";
    }

    static boolean isOccult(String school,String spell){
        String z=(school+" "+spell).toLowerCase(Locale.ROOT);
        return z.contains("blood")||z.contains("ender")||z.contains("eldritch")||z.contains("abyss");
    }
}
