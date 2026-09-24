package com.eldenworld.core.specialization;

import com.eldenworld.core.EldenWorldCore;
import com.eldenworld.core.abilities.AbilityState;
import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = EldenWorldCore.MOD_ID)
public final class FinalSpecializationSpellEventsV2 {
    private FinalSpecializationSpellEventsV2() {}
    @SubscribeEvent public static void cast(SpellOnCastEvent e){
        if(!(e.getEntity() instanceof ServerPlayer p))return;
        String school=e.getSchoolType()==null?"":e.getSchoolType().getId().getPath().toLowerCase();
        String spell=e.getSpellId()==null?"":e.getSpellId().toLowerCase();
        // Gale Dancer consumes a real Wind-school cast from Wind's Spellbooks/addons.
        if(FinalSpecializationEventsV2.has(p,"windrunner/gale_dancer/specialization")&&(school.contains("wind")||spell.contains("wind")||spell.contains("gust"))) FinalSpecializationEventsV2.markGale(p);
        // Elementalist: one active school at a time; other schools are reduced only at Mastery.
        if(FinalSpecializationEventsV2.has(p,"archmage/elementalist/specialization")){
            String s=element(school,spell); if(!s.isEmpty()){
                boolean mm=FinalSpecializationEventsV2.mastery(p,"archmage/elementalist");
                for(String x:new String[]{"fire","ice","lightning","nature"}) FinalSpecializationEventsV2.setAttr(p,"irons_spellbooks:"+x+"_spell_power","element_"+x,x.equals(s)?(mm?.25:.12):(mm?-.10:0));
                AbilityState.setLong(p,"m62_element_until",FinalSpecializationEventsV2.time(p)+120);
            }
        }
        // Occultist uses actual Iron's school IDs and spell-level mutation.
        if(FinalSpecializationEventsV2.has(p,"archmage/occultist/specialization")&&(school.contains("blood")||school.contains("ender")||school.contains("eldritch"))){
            boolean mm=FinalSpecializationEventsV2.mastery(p,"archmage/occultist"); e.setSpellLevel(e.getSpellLevel()+(mm?2:1));
            if(mm){if(school.contains("blood"))FinalSpecializationEventsV2.applyEffect(p,"irons_spellbooks:sacrificial_mark",4,0);else FinalSpecializationEventsV2.applyEffect(p,"irons_spellbooks:soul_burn",3,0);}
        }
        if(FinalSpecializationEventsV2.has(p,"archmage/arcanist/specialization")){FinalSpecializationEventsV2.applyEffect(p,"ars_nouveau:mana_regen",4,0);if(FinalSpecializationEventsV2.mastery(p,"archmage/arcanist"))FinalSpecializationEventsV2.applyEffect(p,"ars_nouveau:spell_damage",4,0);}
        if(FinalSpecializationEventsV2.has(p,"manaflow/channeler/specialization")){double f=FinalSpecializationEventsV2.mastery(p,"manaflow/channeler")?.82:.92;e.setManaCost(Math.max(0,(int)Math.ceil(e.getManaCost()*f)));FinalSpecializationEventsV2.applyEffect(p,"irons_spellbooks:hastened",FinalSpecializationEventsV2.mastery(p,"manaflow/channeler")?4:2,0);}
        if(FinalSpecializationEventsV2.has(p,"manaflow/overcaster/specialization")){boolean mm=FinalSpecializationEventsV2.mastery(p,"manaflow/overcaster");e.setSpellLevel(e.getSpellLevel()+(mm?2:1));e.setManaCost((int)Math.ceil(e.getManaCost()*(mm?1.45:1.25)));if(mm)FinalSpecializationEventsV2.applyEffect(p,"irons_spellbooks:soul_burn",3,0);}
        // Runesmith only activates while a real Simply Swords Runic weapon is held; the tag is checked in the core helper by namespace/path fallback.
        if(FinalSpecializationEventsV2.has(p,"enduring_tools/runesmith/specialization")&&p.getMainHandItem().getItem().builtInRegistryHolder().is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,new net.minecraft.resources.ResourceLocation("simplyswords:runic_weapons")))){FinalSpecializationEventsV2.applyEffect(p,"ars_nouveau:shielding",3,FinalSpecializationEventsV2.mastery(p,"enduring_tools/runesmith")?1:0);if(FinalSpecializationEventsV2.mastery(p,"enduring_tools/runesmith"))FinalSpecializationEventsV2.applyEffect(p,"ars_nouveau:mana_regen",4,0);}
    }
    private static String element(String school,String spell){String z=school+" "+spell;for(String s:new String[]{"fire","ice","lightning","nature"})if(z.contains(s))return s;return "";}
}
