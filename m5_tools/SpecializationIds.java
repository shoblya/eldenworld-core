package com.eldenworld.core.specialization;

import net.minecraft.resources.ResourceLocation;

public final class SpecializationIds {
    private SpecializationIds() {}
    public static ResourceLocation id(String path) { return new ResourceLocation("eldenworld", path); }
    public static final ResourceLocation NIGHTBLADE = id("shadowstep/nightblade/mastery");
    public static final ResourceLocation DUELIST = id("opportunist/duelist/mastery");
    public static final ResourceLocation COLOSSUS = id("juggernaut/colossus/mastery");
    public static final ResourceLocation BULWARK = id("unyielding/bulwark/mastery");
    public static final ResourceLocation MARKSMAN = id("keen_instinct/marksman/mastery");
    public static final ResourceLocation BEASTMASTER = id("pathfinder/beastmaster/mastery");
    public static final ResourceLocation DRAGON_RIDER = id("pathfinder/dragon_rider/mastery");
    public static final ResourceLocation SPELLBREAKER = id("arcane_ward/spellbreaker/mastery");
}
