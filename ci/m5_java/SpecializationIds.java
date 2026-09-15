package com.eldenworld.core.specialization;
import net.minecraft.resources.ResourceLocation;
public final class SpecializationIds {
 private SpecializationIds() {}
 public static ResourceLocation id(String p){return new ResourceLocation("eldenworld",p);}
 public static final ResourceLocation NIGHTBLADE=id("shadowstep/nightblade/mastery"), DUELIST=id("opportunist/duelist/mastery"), COLOSSUS=id("juggernaut/colossus/mastery"), BULWARK=id("unyielding/bulwark/mastery"), MARKSMAN=id("keen_instinct/marksman/mastery"), BEASTMASTER=id("pathfinder/beastmaster/mastery"), DRAGON_RIDER=id("pathfinder/dragon_rider/mastery"), SPELLBREAKER=id("arcane_ward/spellbreaker/mastery");
}
