package com.eldenworld.core.mixin;

import com.eldenworld.core.abilities.KeystoneLimit;
import com.eldenworld.core.requirements.NodeRequirement;
import com.eldenworld.core.requirements.RequirementChecker;
import com.eldenworld.core.requirements.RequirementRegistry;
import daripher.skilltree.network.message.LearnSkillMessage;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.function.Supplier;

@Mixin(value = LearnSkillMessage.class, remap = false)
public abstract class LearnSkillMessageMixin {
    @Inject(method = "receive", at = @At("HEAD"), cancellable = true, remap = false)
    private static void eldenworld$gateSpecialNode(
            LearnSkillMessage message,
            Supplier<NetworkEvent.Context> ctxSupplier,
            CallbackInfo ci
    ) {
        ResourceLocation skillId = ((LearnSkillMessageAccessor) (Object) message).eldenworld$getSkillId();
        NodeRequirement requirement = RequirementRegistry.INSTANCE.get(skillId);

        NetworkEvent.Context context = ctxSupplier.get();
        ServerPlayer player = context.getSender();
        if (player == null) {
            return;
        }

        if (KeystoneLimit.isKeystone(skillId) && !RequirementChecker.hasSkill(player, skillId)
                && !KeystoneLimit.canLearn(player)) {
            context.setPacketHandled(true);
            player.sendSystemMessage(Component.literal("Лимит подклассов: "
                    + KeystoneLimit.learnedCount(player) + "/" + KeystoneLimit.slots(RequirementChecker.pstLevel(player))
                    + ". Слоты открываются на уровнях 40, 60 и 80.").withStyle(ChatFormatting.RED));
            ci.cancel();
            return;
        }
        if (KeystoneLimit.isSpecialization(skillId) && !RequirementChecker.hasSkill(player, skillId)
                && !KeystoneLimit.canLearnSpecialization(player)) {
            context.setPacketHandled(true);
            player.sendSystemMessage(Component.literal("Лимит специализаций: "
                    + KeystoneLimit.learnedSpecializationCount(player) + "/3.").withStyle(ChatFormatting.RED));
            ci.cancel();
            return;
        }
        if (KeystoneLimit.isMastery(skillId) && !RequirementChecker.hasSkill(player, skillId)
                && !KeystoneLimit.canLearnMastery(player)) {
            context.setPacketHandled(true);
            player.sendSystemMessage(Component.literal("Лимит мастерств: "
                    + KeystoneLimit.learnedMasteryCount(player) + "/3.").withStyle(ChatFormatting.RED));
            ci.cancel();
            return;
        }
        if (requirement == null) return;

        List<String> unmet = RequirementChecker.unmet(player, requirement);
        if (unmet.isEmpty()) {
            return;
        }

        // PST learns the node only after this point. Cancelling here means no PST point is spent.
        context.setPacketHandled(true);
        player.sendSystemMessage(Component.literal("Особая нода пока заблокирована:").withStyle(ChatFormatting.RED));
        for (String line : unmet) {
            player.sendSystemMessage(Component.literal(" • " + line).withStyle(ChatFormatting.YELLOW));
        }
        ci.cancel();
    }
}
