package com.eldenworld.core.abilities;

import com.eldenworld.core.EldenWorldCore;
import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.events.SpellDamageEvent;
import io.redspace.ironsspellbooks.api.events.SpellDamageEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = EldenWorldCore.MOD_ID)
public final class IronsKeystoneEvents {
    private IronsKeystoneEvents() {}

    @SubscribeEvent
    public static void onSpellDamage(SpellDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!AbilityUtil.has(player, KeystoneIds.ARCANE_WARD)) return;
        try {
            var school = event.getSpellDamageSource().spell().getSchoolType();
            if (school != null) {
                KeystoneAbilityEvents.markArcaneWardExactSchool(player, school.getId());
            }
        } catch (RuntimeException ignored) {
            // LivingHurt fallback will handle non-standard or incompatible magic sources.
        }
    }

    @SubscribeEvent
    public static void onSpellDamage(SpellDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var source = event.getSpellDamageSource();
        if (source == null || source.spell() == null || source.spell().getSchoolType() == null) return;
        var schoolId = source.spell().getSchoolType().getId();
        if (schoolId == null) return;

        long now = player.level().getGameTime();
        AbilityState.setString(player, "m62_exact_spell_school", schoolId.toString());
        AbilityState.setLong(player, "m62_exact_spell_school_until", now + 2L);
        KeystoneAbilityEvents.markArcaneWardExactSchool(player, schoolId);
    }

    @SubscribeEvent
    public static void onSpellCast(SpellOnCastEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (AbilityUtil.has(player, KeystoneIds.ARCHMAGE)) {
            int increasedCost = (int) Math.ceil(event.getManaCost() * 1.20d);
            event.setManaCost(Math.max(event.getManaCost(), increasedCost));
        }
    }
}
