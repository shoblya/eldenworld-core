package com.eldenworld.core.abilities;

import com.eldenworld.core.EldenWorldCore;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.brewing.PlayerBrewedPotionEvent;

import java.util.*;
import java.nio.charset.StandardCharsets;

@Mod.EventBusSubscriber(modid = EldenWorldCore.MOD_ID)
public final class KeystoneAbilityEvents {
    private static final ResourceLocation MANA_REGEN = id("irons_spellbooks:mana_regen");
    private static final ResourceLocation SPELL_POWER = id("irons_spellbooks:spell_power");
    private static final ResourceLocation SPELL_RESIST = id("irons_spellbooks:spell_resist");
    private static final ResourceLocation BLOCK_REACH = id("forge:block_reach");
    private static final ResourceLocation DODGE_CHANCE = id("attributeslib:dodge_chance");
    private static final ResourceLocation CRIT_CHANCE = id("attributeslib:crit_chance");
    private static final ResourceLocation MINING_SPEED = id("attributeslib:mining_speed");
    private static final ResourceLocation TRUE_INVISIBILITY = id("irons_spellbooks:true_invisibility");
    private static final ResourceLocation COMBATROLL_DISTANCE = id("combatroll:distance");
    private static final ResourceLocation COMBATROLL_RECHARGE = id("combatroll:recharge");

    private static final UUID MASTER_BUILDER_REACH_MODIFIER = uuid("6e9bfa12-2b8d-4a82-89a8-0e2f9be85002");
    private static final UUID MANAFLOW_MODIFIER = uuid("6e9bfa12-2b8d-4a82-89a8-0e2f9be85001");
    private static final UUID OPPORTUNIST_DODGE_MODIFIER = uuid("ba1b4fe4-1537-4c73-bd84-71569f1b9861");
    private static final UUID OPPORTUNIST_CRIT_MODIFIER = uuid("6ac6a6c2-f879-45fb-a2f8-2bb4a6f51df8");
    private static final UUID WINDRUNNER_MOVE_MODIFIER = uuid("20cb25cb-58bc-4831-b4e0-c01749aec75f");
    private static final UUID WINDRUNNER_SPELL_MODIFIER = uuid("6dd81e11-e366-48ad-93fc-1a25d581811f");
    private static final UUID PROSPECTOR_MINING_MODIFIER = uuid("ff4f9878-d06a-4140-9739-734f8c5cfb7e");
    private static final UUID SURVIVOR_DODGE_MODIFIER = uuid("de4fa9d8-86fc-4f1b-ac44-65d01762cc95");
    private static final UUID ARCANE_GENERIC_MODIFIER = uuid("c3c7d679-e96b-46cd-b5ac-5101092333f8");
    private static final UUID ARCANE_SCHOOL_MODIFIER = uuid("be615edf-7038-4260-8b4a-2baa758219f1");
    private static final UUID WINDRUNNER_ROLL_DISTANCE = uuid("5a2cfa18-4f66-4e66-a32e-629f63682001");
    private static final UUID WINDRUNNER_ROLL_RECHARGE = uuid("5a2cfa18-4f66-4e66-a32e-629f63682002");
    private static final UUID WAYFARER_JOURNAL_MOVE = uuid("5a2cfa18-4f66-4e66-a32e-629f63682003");
    private static final UUID WAYFARER_JOURNAL_RESIST = uuid("5a2cfa18-4f66-4e66-a32e-629f63682004");
    private static final UUID WAYFARER_AURA_MOVE = uuid("5a2cfa18-4f66-4e66-a32e-629f63682005");

    private static final Map<String, ResourceLocation> ARCANE_RESISTS = Map.of(
            "fire", id("irons_spellbooks:fire_magic_resist"),
            "ice", id("irons_spellbooks:ice_magic_resist"),
            "lightning", id("irons_spellbooks:lightning_magic_resist"),
            "holy", id("irons_spellbooks:holy_magic_resist"),
            "ender", id("irons_spellbooks:ender_magic_resist"),
            "blood", id("irons_spellbooks:blood_magic_resist"),
            "evocation", id("irons_spellbooks:evocation_magic_resist"),
            "nature", id("irons_spellbooks:nature_magic_resist")
    );

    private static final String LAST_COMBAT = "last_combat";
    private static final String SHADOWSTEP_CD = "shadowstep_cd";
    private static final String GHOST_CD = "ghost_cd";
    private static final String GHOST_ACTIVE = "ghost_active_until";
    private static final String SECOND_WIND_CD = "second_wind_cd";
    private static final String KEEN_CD = "keen_instinct_cd";
    private static final String MANAFLOW_STACKS = "manaflow_stacks";
    private static final String MANAFLOW_EXPIRES = "manaflow_expires";
    private static final String BUILDER_RHYTHM = "builder_rhythm";
    private static final String BUILDER_LAST_PLACE = "builder_last_place";
    private static final String OPPORTUNIST_STACKS = "opportunist_stacks";
    private static final String OPPORTUNIST_NEXT = "opportunist_next";
    private static final String WINDRUNNER_STACKS = "windrunner_stacks";
    private static final String WINDRUNNER_NEXT = "windrunner_next";
    private static final String PROSPECTOR_STACKS = "prospector_stacks";
    private static final String PROSPECTOR_LAST_ORE = "prospector_last_ore";
    private static final String ARCANE_WARD_SCHOOL = "arcane_ward_school";
    private static final String ARCANE_WARD_EXPIRES = "arcane_ward_expires";
    private static final String SURVIVOR_CD = "survivor_cd";
    private static final String SURVIVOR_ACTIVE = "survivor_active_until";
    private static final String TREASURE_BIOMES = "treasure_biomes";
    private static final String WAYFARER_BIOMES = "wayfarer_biomes";
    private static final String WAYFARER_DIMENSIONS = "wayfarer_dimensions";
    private static final String WAYFARER_BIOME_COUNT = "wayfarer_biome_count";
    private static final String WAYFARER_DIM_COUNT = "wayfarer_dim_count";
    private static final String WAYFARER_AURA_UNTIL = "wayfarer_aura_until";
    private static final String COOK_OWNER = "EldenWorldCookOwner";
    private static final Map<UUID, Map<ResourceLocation, Integer>> COOK_EFFECT_SNAPSHOT = new HashMap<>();
    private record TradeDiscount(MerchantOffer offer, int delta) {}
    private static final Map<UUID, List<TradeDiscount>> TREASURE_TRADE_DISCOUNTS = new HashMap<>();
    private static final Map<UUID, Integer> TREASURE_TRADE_MENU = new HashMap<>();

    private KeystoneAbilityEvents() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        // A PlayerTick can still fire for the old ServerPlayer instance immediately
        // after death, when Passive Skill Tree has already invalidated its capability.
        if (!player.isAlive() || player.isRemoved()) {
            return;
        }
        long now = player.level().getGameTime();
        tickWayfarerAuraReceiver(player, now);

        tickShadowstep(player, now);
        tickOpportunist(player, now);
        tickWindrunner(player, now);
        tickPathfinder(player);
        tickWayfarer(player, now);
        tickManaflow(player, now);
        tickArcaneWard(player, now);
        tickMasterBuilder(player, now);
        tickProspector(player, now);
        tickTreasureHunter(player);
        tickSurvivor(player, now);
    }

    private static void tickShadowstep(ServerPlayer player, long now) {
        if (!AbilityUtil.has(player, KeystoneIds.SHADOWSTEP)) {
            return;
        }
        float threshold = player.getMaxHealth() * 0.15f;
        if (player.getHealth() > threshold || !AbilityState.cooldownReady(player, SHADOWSTEP_CD, now)) {
            return;
        }

        AbilityState.setCooldown(player, SHADOWSTEP_CD, now, 20L * 60L);
        AbilityState.setLong(player, "m62_nb_window", now + 60L);
        AbilityUtil.tryShadowstep(player, 4.5);
        applyEffectById(player, TRUE_INVISIBILITY, 20 * 4, 0);
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 5, 1, true, false, true));
        player.sendSystemMessage(Component.literal("Shadowstep активирован").withStyle(ChatFormatting.DARK_PURPLE));
    }

    private static void tickOpportunist(ServerPlayer player, long now) {
        if (!AbilityUtil.has(player, KeystoneIds.OPPORTUNIST)) {
            AbilityState.setInt(player, OPPORTUNIST_STACKS, 0);
            AbilityUtil.removeDynamicAttributeModifier(player, DODGE_CHANCE, OPPORTUNIST_DODGE_MODIFIER);
            AbilityUtil.removeDynamicAttributeModifier(player, CRIT_CHANCE, OPPORTUNIST_CRIT_MODIFIER);
            return;
        }

        long sinceCombat = now - AbilityState.getLong(player, LAST_COMBAT);
        int stacks = AbilityState.getInt(player, OPPORTUNIST_STACKS);
        if (sinceCombat > 20L * 8L) {
            stacks = 0;
            AbilityState.setInt(player, OPPORTUNIST_STACKS, 0);
            AbilityState.setLong(player, OPPORTUNIST_NEXT, now + 20L * 3L);
        } else if (now >= AbilityState.getLong(player, OPPORTUNIST_NEXT)) {
            stacks = Math.min(5, stacks + 1);
            AbilityState.setInt(player, OPPORTUNIST_STACKS, stacks);
            AbilityState.setLong(player, OPPORTUNIST_NEXT, now + 20L * 3L);
        }

        double bonus = Math.min(5, stacks) * 0.02;
        AbilityUtil.setDynamicAttributeModifier(player, DODGE_CHANCE, OPPORTUNIST_DODGE_MODIFIER,
                "EldenWorld Opportunist Dodge", bonus, AttributeModifier.Operation.ADDITION);
        AbilityUtil.setDynamicAttributeModifier(player, CRIT_CHANCE, OPPORTUNIST_CRIT_MODIFIER,
                "EldenWorld Opportunist Crit", bonus, AttributeModifier.Operation.ADDITION);
    }

    private static void tickWindrunner(ServerPlayer player, long now) {
        if (!AbilityUtil.has(player, KeystoneIds.WINDRUNNER)) {
            clearWindrunner(player);
            AbilityUtil.removeDynamicAttributeModifier(player, COMBATROLL_DISTANCE, WINDRUNNER_ROLL_DISTANCE);
            AbilityUtil.removeDynamicAttributeModifier(player, COMBATROLL_RECHARGE, WINDRUNNER_ROLL_RECHARGE);
            return;
        }
        AbilityUtil.setDynamicAttributeModifier(player, COMBATROLL_DISTANCE, WINDRUNNER_ROLL_DISTANCE,
                "EldenWorld Windrunner Roll Distance", 0.5, AttributeModifier.Operation.ADDITION);
        AbilityUtil.setDynamicAttributeModifier(player, COMBATROLL_RECHARGE, WINDRUNNER_ROLL_RECHARGE,
                "EldenWorld Windrunner Roll Recharge", 5.0, AttributeModifier.Operation.ADDITION);

        boolean inCombat = now - AbilityState.getLong(player, LAST_COMBAT) <= 20L * 6L;
        if (!inCombat || !player.isSprinting()) {
            clearWindrunner(player);
            AbilityState.setLong(player, WINDRUNNER_NEXT, now + 40L);
            return;
        }

        int stacks = AbilityState.getInt(player, WINDRUNNER_STACKS);
        if (now >= AbilityState.getLong(player, WINDRUNNER_NEXT)) {
            stacks = Math.min(3, stacks + 1);
            AbilityState.setInt(player, WINDRUNNER_STACKS, stacks);
            AbilityState.setLong(player, WINDRUNNER_NEXT, now + 40L);
        }

        double bonus = stacks * 0.04;
        AbilityUtil.setDynamicAttributeModifier(player, new ResourceLocation("minecraft", "generic.movement_speed"),
                WINDRUNNER_MOVE_MODIFIER, "EldenWorld Windrunner Move", bonus, AttributeModifier.Operation.MULTIPLY_TOTAL);
        AbilityUtil.setDynamicAttributeModifier(player, SPELL_POWER,
                WINDRUNNER_SPELL_MODIFIER, "EldenWorld Windrunner Spell", bonus, AttributeModifier.Operation.MULTIPLY_TOTAL);
        if (stacks >= 3) AbilityUtil.refreshEffect(player, MobEffects.JUMP, 0);
    }

    private static void clearWindrunner(ServerPlayer player) {
        AbilityState.setInt(player, WINDRUNNER_STACKS, 0);
        AbilityUtil.removeDynamicAttributeModifier(player, new ResourceLocation("minecraft", "generic.movement_speed"), WINDRUNNER_MOVE_MODIFIER);
        AbilityUtil.removeDynamicAttributeModifier(player, SPELL_POWER, WINDRUNNER_SPELL_MODIFIER);
    }

    private static void tickPathfinder(ServerPlayer player) {
        if (AbilityUtil.has(player, KeystoneIds.PATHFINDER)) {
            AbilityUtil.refreshEffect(player, MobEffects.JUMP, 2); // Jump Boost III
        }
    }

    private static void tickWayfarer(ServerPlayer player, long now) {
        if (!AbilityUtil.has(player, KeystoneIds.WAYFARER)) {
            AbilityUtil.removeDynamicAttributeModifier(player, new ResourceLocation("minecraft", "generic.movement_speed"), WAYFARER_JOURNAL_MOVE);
            AbilityUtil.removeDynamicAttributeModifier(player, SPELL_RESIST, WAYFARER_JOURNAL_RESIST);
            return;
        }
        if (player.tickCount % 20 == 0) {
            player.level().getBiome(player.blockPosition()).unwrapKey().ifPresent(key -> {
                String safe = key.location().toString().replace(':','_').replace('/','_');
                if (AbilityState.markOnce(player, WAYFARER_BIOMES, safe)) {
                    AbilityState.setInt(player, WAYFARER_BIOME_COUNT, AbilityState.getInt(player, WAYFARER_BIOME_COUNT) + 1);
                }
            });
            String dim = player.level().dimension().location().toString().replace(':','_').replace('/','_');
            if (AbilityState.markOnce(player, WAYFARER_DIMENSIONS, dim)) {
                AbilityState.setInt(player, WAYFARER_DIM_COUNT, AbilityState.getInt(player, WAYFARER_DIM_COUNT) + 1);
            }
        }
        double move = Math.min(0.075, AbilityState.getInt(player, WAYFARER_BIOME_COUNT) * 0.0005);
        double resist = Math.min(0.05, AbilityState.getInt(player, WAYFARER_DIM_COUNT) * 0.005);
        AbilityUtil.setDynamicAttributeModifier(player, new ResourceLocation("minecraft", "generic.movement_speed"), WAYFARER_JOURNAL_MOVE,
                "EldenWorld Wayfarer Journal Move", move, AttributeModifier.Operation.MULTIPLY_TOTAL);
        AbilityUtil.setDynamicAttributeModifier(player, SPELL_RESIST, WAYFARER_JOURNAL_RESIST,
                "EldenWorld Wayfarer Journal Resist", resist, AttributeModifier.Operation.MULTIPLY_TOTAL);

        long lastCombat = AbilityState.getLong(player, LAST_COMBAT);
        if (now - lastCombat >= 20L * 10L) {
            AbilityUtil.refreshEffect(player, MobEffects.MOVEMENT_SPEED, 0);
            AbilityUtil.refreshEffect(player, MobEffects.JUMP, 0);
            for (ServerPlayer ally : ((ServerLevel)player.level()).getEntitiesOfClass(ServerPlayer.class, player.getBoundingBox().inflate(8.0),
                    p -> p != player && !p.isSpectator())) {
                AbilityState.setLong(ally, WAYFARER_AURA_UNTIL, now + 30L);
            }
        }
    }

    private static void tickWayfarerAuraReceiver(ServerPlayer player, long now) {
        if (now <= AbilityState.getLong(player, WAYFARER_AURA_UNTIL)) {
            AbilityUtil.setDynamicAttributeModifier(player, new ResourceLocation("minecraft", "generic.movement_speed"), WAYFARER_AURA_MOVE,
                    "EldenWorld Wayfarer Aura", 0.05, AttributeModifier.Operation.MULTIPLY_TOTAL);
        } else {
            AbilityUtil.removeDynamicAttributeModifier(player, new ResourceLocation("minecraft", "generic.movement_speed"), WAYFARER_AURA_MOVE);
        }
    }

    private static void tickManaflow(ServerPlayer player, long now) {
        if (!AbilityUtil.has(player, KeystoneIds.MANAFLOW)) {
            AbilityState.setInt(player, MANAFLOW_STACKS, 0);
            AbilityUtil.removeDynamicAttributeModifier(player, MANA_REGEN, MANAFLOW_MODIFIER);
            return;
        }

        int stacks = AbilityState.getInt(player, MANAFLOW_STACKS);
        if (stacks > 0 && now > AbilityState.getLong(player, MANAFLOW_EXPIRES)) {
            stacks--;
            AbilityState.setInt(player, MANAFLOW_STACKS, stacks);
            AbilityState.setLong(player, MANAFLOW_EXPIRES, now + 20L * 2L);
        }

        double bonus = Math.max(0, Math.min(5, stacks)) * 0.08;
        AbilityUtil.setDynamicAttributeModifier(player, MANA_REGEN, MANAFLOW_MODIFIER,
                "EldenWorld Manaflow", bonus, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    private static void tickArcaneWard(ServerPlayer player, long now) {
        if (!AbilityUtil.has(player, KeystoneIds.ARCANE_WARD)) {
            clearArcaneWard(player);
            clearArcaneBaseResists(player);
            return;
        }
        ensureArcaneBaseResists(player);
        if (now > AbilityState.getLong(player, ARCANE_WARD_EXPIRES)) {
            AbilityState.setString(player, ARCANE_WARD_SCHOOL, "");
            clearArcaneWardModifiersOnly(player);
            return;
        }
        String attrId = AbilityState.getString(player, ARCANE_WARD_SCHOOL);
        clearArcaneWardModifiersOnly(player);
        if (!attrId.isBlank()) {
            ResourceLocation resistance = ResourceLocation.tryParse(attrId);
            if (resistance != null && ForgeRegistries.ATTRIBUTES.containsKey(resistance)) {
                AbilityUtil.setDynamicAttributeModifier(player, resistance, ARCANE_SCHOOL_MODIFIER,
                        "EldenWorld Arcane Ward School", 0.25, AttributeModifier.Operation.MULTIPLY_TOTAL);
                return;
            }
        }
        AbilityUtil.setDynamicAttributeModifier(player, SPELL_RESIST, ARCANE_GENERIC_MODIFIER,
                "EldenWorld Arcane Ward Generic", 0.15, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    private static void ensureArcaneBaseResists(ServerPlayer player) {
        for (ResourceLocation key : ForgeRegistries.ATTRIBUTES.getKeys()) {
            String p = key.getPath();
            if (!p.contains("magic_resist") || p.equals("spell_resist")) continue;
            UUID mod = UUID.nameUUIDFromBytes(("eldenworld:m62:arcane_base:" + key).getBytes(StandardCharsets.UTF_8));
            AbilityUtil.setDynamicAttributeModifier(player, key, mod, "EldenWorld Arcane Ward Base " + key, 0.04,
                    AttributeModifier.Operation.MULTIPLY_TOTAL);
        }
    }

    private static void clearArcaneBaseResists(ServerPlayer player) {
        for (ResourceLocation key : ForgeRegistries.ATTRIBUTES.getKeys()) {
            if (!key.getPath().contains("magic_resist") || key.getPath().equals("spell_resist")) continue;
            UUID mod = UUID.nameUUIDFromBytes(("eldenworld:m62:arcane_base:" + key).getBytes(StandardCharsets.UTF_8));
            AbilityUtil.removeDynamicAttributeModifier(player, key, mod);
        }
    }

    private static void clearArcaneWard(ServerPlayer player) {
        AbilityState.setString(player, ARCANE_WARD_SCHOOL, "");
        clearArcaneWardModifiersOnly(player);
    }

    private static void clearArcaneWardModifiersOnly(ServerPlayer player) {
        AbilityUtil.removeDynamicAttributeModifier(player, SPELL_RESIST, ARCANE_GENERIC_MODIFIER);
        for (ResourceLocation key : ForgeRegistries.ATTRIBUTES.getKeys()) {
            if (key.getPath().contains("magic_resist")) {
                AbilityUtil.removeDynamicAttributeModifier(player, key, ARCANE_SCHOOL_MODIFIER);
            }
        }
    }

    private static void tickMasterBuilder(ServerPlayer player, long now) {
        if (!AbilityUtil.has(player, KeystoneIds.MASTER_BUILDER)) {
            AbilityState.setInt(player, BUILDER_RHYTHM, 0);
            AbilityUtil.removeDynamicAttributeModifier(player, BLOCK_REACH, MASTER_BUILDER_REACH_MODIFIER);
            return;
        }
        AbilityUtil.setDynamicAttributeModifier(player, BLOCK_REACH, MASTER_BUILDER_REACH_MODIFIER,
                "EldenWorld Master Builder Reach", 2.0, AttributeModifier.Operation.ADDITION);

        int rhythm = AbilityState.getInt(player, BUILDER_RHYTHM);
        long lastPlace = AbilityState.getLong(player, BUILDER_LAST_PLACE);
        if (rhythm > 0 && now - lastPlace > 60L) {
            rhythm = 0;
            AbilityState.setInt(player, BUILDER_RHYTHM, 0);
        }
        if (rhythm >= 1) {
            // 1-2 = Haste I, 3-4 = Haste II, 5-6 = Haste III.
            AbilityUtil.refreshEffect(player, MobEffects.DIG_SPEED, Math.min(2, (rhythm - 1) / 2));
        }
        if (rhythm >= 4) {
            AbilityUtil.refreshEffect(player, MobEffects.MOVEMENT_SPEED, 0);
        }
    }

    private static void tickProspector(ServerPlayer player, long now) {
        if (!AbilityUtil.has(player, KeystoneIds.ENDURING_TOOLS)) {
            AbilityState.setInt(player, PROSPECTOR_STACKS, 0);
            AbilityUtil.removeDynamicAttributeModifier(player, MINING_SPEED, PROSPECTOR_MINING_MODIFIER);
            return;
        }
        int stacks = AbilityState.getInt(player, PROSPECTOR_STACKS);
        if (stacks > 0 && now - AbilityState.getLong(player, PROSPECTOR_LAST_ORE) > 20L * 6L) {
            stacks = 0;
            AbilityState.setInt(player, PROSPECTOR_STACKS, 0);
        }
        AbilityUtil.setDynamicAttributeModifier(player, MINING_SPEED, PROSPECTOR_MINING_MODIFIER,
                "EldenWorld Miner Ore Streak", stacks * 0.05, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    private static void tickTreasureHunter(ServerPlayer player) {
        if (!AbilityUtil.has(player, KeystoneIds.TREASURE_HUNTER)) {
            clearTreasureTradeDiscount(player);
            return;
        }
        tickTreasureTradeDiscount(player);
        if (player.tickCount % 20 != 0) return;
        player.level().getBiome(player.blockPosition()).unwrapKey().ifPresent(key -> {
            String safeKey = key.location().toString().replace(':', '_').replace('/', '_');
            if (AbilityState.markOnce(player, TREASURE_BIOMES, safeKey)) {
                player.addEffect(new MobEffectInstance(MobEffects.LUCK, 20 * 90, 1, true, false, true));
                player.giveExperiencePoints(10);
                player.sendSystemMessage(Component.literal("Treasure Hunter: новый биом — усилена удача").withStyle(ChatFormatting.GOLD));
            }
        });
    }

    private static void tickSurvivor(ServerPlayer player, long now) {
        if (!AbilityUtil.has(player, KeystoneIds.SURVIVOR)) {
            AbilityUtil.removeDynamicAttributeModifier(player, DODGE_CHANCE, SURVIVOR_DODGE_MODIFIER);
            return;
        }

        if (player.getHealth() <= player.getMaxHealth() * 0.30f && AbilityState.cooldownReady(player, SURVIVOR_CD, now)) {
            AbilityState.setCooldown(player, SURVIVOR_CD, now, 20L * 90L);
            AbilityState.setLong(player, SURVIVOR_ACTIVE, now + 20L * 8L);
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20 * 8, 0, true, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 8, 1, true, false, true));
            player.sendSystemMessage(Component.literal("Survivor: инстинкт выживания активирован").withStyle(ChatFormatting.GREEN));
        }

        if (now <= AbilityState.getLong(player, SURVIVOR_ACTIVE)) {
            AbilityUtil.setDynamicAttributeModifier(player, DODGE_CHANCE, SURVIVOR_DODGE_MODIFIER,
                    "EldenWorld Survivor Dodge", 0.10, AttributeModifier.Operation.ADDITION);
        } else {
            AbilityUtil.removeDynamicAttributeModifier(player, DODGE_CHANCE, SURVIVOR_DODGE_MODIFIER);
        }
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getSource().getEntity() == null) {
            return;
        }
        long now = player.level().getGameTime();

        if (AbilityUtil.has(player, KeystoneIds.KEEN_INSTINCT)
                && AbilityState.cooldownReady(player, KEEN_CD, now)
                && now - AbilityState.getLong(player, LAST_COMBAT) >= 20L * 10L) {
            event.setCanceled(true);
            AbilityState.setCooldown(player, KEEN_CD, now, 20L * 20L);
            player.sendSystemMessage(Component.literal("Keen Instinct: первая атака избегнута").withStyle(ChatFormatting.AQUA));
            return;
        }
        markCombat(player, now);
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        long now = event.getEntity().level().getGameTime();

        if (event.getEntity() instanceof ServerPlayer victim) {
            if (event.getSource().getEntity() instanceof LivingEntity) {
                markCombat(victim, now);
            }

            if (AbilityUtil.has(victim, KeystoneIds.UNYIELDING)
                    && event.getSource().getEntity() instanceof LivingEntity attacker
                    && attacker != victim
                    && !event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                    && !event.getSource().is(net.minecraft.world.damagesource.DamageTypes.THORNS)) {
                float reflected = Math.min(6.0f, event.getAmount() * 0.15f);
                if (reflected > 0.0f) {
                    attacker.hurt(victim.damageSources().thorns(victim), reflected);
                }
            }

            if (AbilityUtil.has(victim, KeystoneIds.ARCANE_WARD) && AbilityUtil.isMagicLike(event.getSource())) {
                AbilityState.setString(victim, ARCANE_WARD_SCHOOL, detectMagicResistanceAttribute(event.getSource()));
                AbilityState.setLong(victim, ARCANE_WARD_EXPIRES, now + 20L * 8L);
            }
        }

        if (event.getSource().getEntity() instanceof ServerPlayer attacker) {
            markCombat(attacker, now);
            if (AbilityUtil.has(attacker, KeystoneIds.MANAFLOW) && AbilityUtil.isMagicLike(event.getSource())) {
                int stacks = Math.min(5, AbilityState.getInt(attacker, MANAFLOW_STACKS) + 1);
                AbilityState.setInt(attacker, MANAFLOW_STACKS, stacks);
                AbilityState.setLong(attacker, MANAFLOW_EXPIRES, now + 20L * 4L);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker) || event.getAmount() <= 0.0f) {
            return;
        }
        long now = attacker.level().getGameTime();

        if (AbilityUtil.has(attacker, KeystoneIds.JUGGERNAUT)) {
            float ratio = attacker.getHealth() / attacker.getMaxHealth();
            float fraction = ratio <= 0.25f ? 0.10f : ratio <= 0.50f ? 0.07f : ratio <= 0.75f ? 0.045f : 0.025f;
            attacker.heal(Math.min(4.0f, event.getAmount() * fraction));
        }

        if (AbilityUtil.has(attacker, KeystoneIds.GHOST)) {
            if (now <= AbilityState.getLong(attacker, GHOST_ACTIVE)) {
                removeEffectById(attacker, TRUE_INVISIBILITY);
                attacker.removeEffect(MobEffects.INVISIBILITY);
                AbilityState.setLong(attacker, GHOST_ACTIVE, 0L);
            } else if (AbilityState.cooldownReady(attacker, GHOST_CD, now) && attacker.getRandom().nextFloat() < 0.15f) {
                AbilityState.setCooldown(attacker, GHOST_CD, now, 20L * 6L);
                AbilityState.setLong(attacker, GHOST_ACTIVE, now + 20L * 4L);
                applyEffectById(attacker, TRUE_INVISIBILITY, 20 * 4, 0);
                attacker.sendSystemMessage(Component.literal("Ghost: вы растворились в тени").withStyle(ChatFormatting.DARK_GRAY));
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        long now = player.level().getGameTime();
        if (!AbilityUtil.has(player, KeystoneIds.SECOND_WIND)
                || !AbilityState.cooldownReady(player, SECOND_WIND_CD, now)) {
            return;
        }

        event.setCanceled(true);
        AbilityState.setCooldown(player, SECOND_WIND_CD, now, 20L * 60L * 5L);
        AbilityState.setLong(player, "m62_second_wind_trigger", now);
        AbilityState.setInt(player, "m62_second_wind_serial", AbilityState.getInt(player, "m62_second_wind_serial") + 1);
        player.setHealth(Math.max(1.0f, player.getMaxHealth() * 0.35f));
        player.clearFire();
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20 * 5, 1, true, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 5, 1, true, false, true));
        player.sendSystemMessage(Component.literal("Second Wind спас вас от смерти").withStyle(ChatFormatting.GOLD));
    }

    @SubscribeEvent
    public static void onFall(LivingHurtEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && AbilityUtil.has(player, KeystoneIds.PATHFINDER)
                && event.getSource().is(net.minecraft.world.damagesource.DamageTypes.FALL)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlaceBlock(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !AbilityUtil.has(player, KeystoneIds.MASTER_BUILDER)) {
            return;
        }
        long now = player.level().getGameTime();
        long last = AbilityState.getLong(player, BUILDER_LAST_PLACE);
        int rhythm = AbilityState.getInt(player, BUILDER_RHYTHM);
        rhythm = now - last <= 40L ? Math.min(6, rhythm + 1) : 1;
        AbilityState.setInt(player, BUILDER_RHYTHM, rhythm);
        AbilityState.setLong(player, BUILDER_LAST_PLACE, now);
    }

    @SubscribeEvent
    public static void onBreakBlock(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player) || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (!event.getState().is(Tags.Blocks.ORES)) {
            return;
        }

        if (AbilityUtil.has(player, KeystoneIds.ENDURING_TOOLS)) {
            long now = level.getGameTime();
            int stacks = now - AbilityState.getLong(player, PROSPECTOR_LAST_ORE) <= 20L * 6L
                    ? Math.min(5, AbilityState.getInt(player, PROSPECTOR_STACKS) + 1)
                    : 1;
            AbilityState.setInt(player, PROSPECTOR_STACKS, stacks);
            AbilityState.setLong(player, PROSPECTOR_LAST_ORE, now);
        }
    }

    private static String detectMagicResistanceAttribute(net.minecraft.world.damagesource.DamageSource source) {
        String path = source.typeHolder().unwrapKey().map(k -> k.location().getPath().toLowerCase()).orElse("");
        if (path.isBlank()) return "";
        List<String> tokens = new ArrayList<>();
        for (String t : path.split("[_/\\.-]+")) if (t.length() >= 3) tokens.add(t);
        if (path.contains("flame") || path.contains("burn")) tokens.add("fire");
        if (path.contains("frost") || path.contains("cold")) tokens.add("ice");
        if (path.contains("thunder") || path.contains("shock")) tokens.add("lightning");
        if (path.contains("divine")) tokens.add("holy");
        if (path.contains("water") || path.contains("tidal")) tokens.add("aqua");
        if (path.contains("earth") || path.contains("stone")) tokens.add("geo");
        if (path.contains("arcane") || path.contains("magic_missile")) tokens.add("evocation");
        for (ResourceLocation key : ForgeRegistries.ATTRIBUTES.getKeys()) {
            String ap = key.getPath().toLowerCase();
            if (!ap.contains("magic_resist")) continue;
            for (String token : tokens) {
                if (ap.contains(token)) return key.toString();
            }
        }
        return "";
    }

    @SubscribeEvent
    public static void onCraft(PlayerEvent.ItemCraftedEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && AbilityUtil.has(player, KeystoneIds.PROSPECTOR)) {
            tagCookItem(player, event.getCrafting());
        }
    }

    @SubscribeEvent
    public static void onSmelt(PlayerEvent.ItemSmeltedEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && AbilityUtil.has(player, KeystoneIds.PROSPECTOR)) {
            tagCookItem(player, event.getSmelting());
        }
    }

    @SubscribeEvent
    public static void onBrewedPotion(PlayerBrewedPotionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && AbilityUtil.has(player, KeystoneIds.PROSPECTOR)) {
            ItemStack stack = event.getStack();
            if (!stack.isEmpty() && stack.getItem() instanceof PotionItem) {
                stack.getOrCreateTag().putUUID(COOK_OWNER, player.getUUID());
            }
        }
    }

    @SubscribeEvent
    public static void onUseStart(LivingEntityUseItemEvent.Start event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack stack = event.getItem();
        if (!isOwnCookFood(player, stack) && !isOwnCookPotion(player, stack)) return;
        Map<ResourceLocation, Integer> snap = new HashMap<>();
        for (MobEffectInstance inst : player.getActiveEffects()) {
            ResourceLocation key = ForgeRegistries.MOB_EFFECTS.getKey(inst.getEffect());
            if (key != null) snap.put(key, inst.getDuration());
        }
        COOK_EFFECT_SNAPSHOT.put(player.getUUID(), snap);
    }

    @SubscribeEvent
    public static void onUseFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack stack = event.getItem();
        boolean ownFood = isOwnCookFood(player, stack);
        boolean ownPotion = isOwnCookPotion(player, stack);
        if (!ownFood && !ownPotion) return;
        if (ownFood) player.getFoodData().eat(1, 0.0f);
        Map<ResourceLocation, Integer> before = COOK_EFFECT_SNAPSHOT.remove(player.getUUID());
        if (before == null) before = Map.of();
        List<MobEffectInstance> boosted = new ArrayList<>();
        for (MobEffectInstance inst : player.getActiveEffects()) {
            if (!inst.getEffect().isBeneficial()) continue;
            ResourceLocation key = ForgeRegistries.MOB_EFFECTS.getKey(inst.getEffect());
            int old = key == null ? -1 : before.getOrDefault(key, -1);
            if (old < 0 || inst.getDuration() > old + 5) {
                boosted.add(new MobEffectInstance(inst.getEffect(), (int)Math.ceil(inst.getDuration() * 1.10), inst.getAmplifier(),
                        inst.isAmbient(), inst.isVisible(), inst.showIcon()));
            }
        }
        for (MobEffectInstance inst : boosted) player.addEffect(inst);
    }

    private static void tagCookItem(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty() || !stack.isEdible()) return;
        stack.getOrCreateTag().putUUID(COOK_OWNER, player.getUUID());
    }

    private static boolean isOwnCookFood(ServerPlayer player, ItemStack stack) {
        return !stack.isEmpty() && stack.isEdible() && stack.hasTag() && stack.getTag().hasUUID(COOK_OWNER)
                && player.getUUID().equals(stack.getTag().getUUID(COOK_OWNER));
    }


    private static boolean isOwnCookPotion(ServerPlayer player, ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof PotionItem && stack.hasTag()
                && stack.getTag().hasUUID(COOK_OWNER) && player.getUUID().equals(stack.getTag().getUUID(COOK_OWNER));
    }

    private static void tickTreasureTradeDiscount(ServerPlayer player) {
        if (!(player.containerMenu instanceof MerchantMenu menu)) {
            clearTreasureTradeDiscount(player);
            return;
        }
        int menuId = menu.containerId;
        Integer previous = TREASURE_TRADE_MENU.get(player.getUUID());
        if (previous != null && previous == menuId) return;

        clearTreasureTradeDiscount(player);
        List<TradeDiscount> records = new ArrayList<>();
        for (MerchantOffer offer : menu.getOffers()) {
            int current = offer.getCostA().getCount();
            if (current <= 1) continue;
            int target = Math.max(1, Math.round(current * 0.70f));
            int reduction = Math.max(0, current - target);
            if (reduction > 0) {
                offer.addToSpecialPriceDiff(-reduction);
                records.add(new TradeDiscount(offer, reduction));
            }
        }
        TREASURE_TRADE_MENU.put(player.getUUID(), menuId);
        TREASURE_TRADE_DISCOUNTS.put(player.getUUID(), records);
    }

    private static void clearTreasureTradeDiscount(ServerPlayer player) {
        List<TradeDiscount> records = TREASURE_TRADE_DISCOUNTS.remove(player.getUUID());
        if (records != null) {
            for (TradeDiscount record : records) {
                try { record.offer().addToSpecialPriceDiff(record.delta()); } catch (RuntimeException ignored) {}
            }
        }
        TREASURE_TRADE_MENU.remove(player.getUUID());
    }

    private static void applyEffectById(LivingEntity entity, ResourceLocation id, int duration, int amplifier) {
        MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(id);
        if (effect != null) entity.addEffect(new MobEffectInstance(effect, duration, amplifier, true, false, true));
    }

    private static void removeEffectById(LivingEntity entity, ResourceLocation id) {
        MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(id);
        if (effect != null) entity.removeEffect(effect);
    }

    private static void markCombat(ServerPlayer player, long now) {
        AbilityState.setLong(player, LAST_COMBAT, now);
    }

    private static ResourceLocation id(String value) {
        return new ResourceLocation(value);
    }

    private static UUID uuid(String value) {
        return UUID.fromString(value);
    }
}
