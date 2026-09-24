package com.eldenworld.core.abilities;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import daripher.skilltree.capability.skill.PlayerSkillsProvider;
import com.eldenworld.core.requirements.RequirementChecker;
public final class KeystoneLimit {
    private KeystoneLimit() {}
    private static final int MAX_SPECIALIZATIONS = 3;
    private static final int MAX_MASTERIES = 3;
    private static final Set<ResourceLocation> IDS = Set.of(
        KeystoneIds.SHADOWSTEP, KeystoneIds.GHOST, KeystoneIds.OPPORTUNIST, KeystoneIds.JUGGERNAUT, KeystoneIds.UNYIELDING, KeystoneIds.SECOND_WIND, KeystoneIds.KEEN_INSTINCT, KeystoneIds.WINDRUNNER, KeystoneIds.PATHFINDER, KeystoneIds.ARCHMAGE, KeystoneIds.MANAFLOW, KeystoneIds.ARCANE_WARD, KeystoneIds.MASTER_BUILDER, KeystoneIds.ENDURING_TOOLS, KeystoneIds.PROSPECTOR, KeystoneIds.WAYFARER, KeystoneIds.TREASURE_HUNTER, KeystoneIds.SURVIVOR);
    public static boolean isKeystone(ResourceLocation id) { return IDS.contains(id); }
    public static boolean isSpecialization(ResourceLocation id) {
        return id != null && id.getNamespace().equals("eldenworld") && id.getPath().endsWith("/specialization");
    }
    public static boolean isMastery(ResourceLocation id) {
        return id != null && id.getNamespace().equals("eldenworld") && id.getPath().endsWith("/mastery");
    }
    public static int learnedSpecializationCount(ServerPlayer p) {
        return (int) PlayerSkillsProvider.get(p).getPlayerSkills().stream().filter(s -> isSpecialization(s.getId())).count();
    }
    public static int learnedMasteryCount(ServerPlayer p) {
        return (int) PlayerSkillsProvider.get(p).getPlayerSkills().stream().filter(s -> isMastery(s.getId())).count();
    }
    public static boolean canLearnSpecialization(ServerPlayer p) { return learnedSpecializationCount(p) < MAX_SPECIALIZATIONS; }
    public static boolean canLearnMastery(ServerPlayer p) { return learnedMasteryCount(p) < MAX_MASTERIES; }
    public static int slots(int level) { return level >= 80 ? 3 : level >= 60 ? 2 : level >= 40 ? 1 : 0; }
    public static int learnedCount(ServerPlayer p) {
        return (int) PlayerSkillsProvider.get(p).getPlayerSkills().stream().filter(s -> IDS.contains(s.getId())).count();
    }
    public static boolean canLearn(ServerPlayer p) { return learnedCount(p) < slots(RequirementChecker.pstLevel(p)); }
}
