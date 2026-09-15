package com.eldenworld.core.client;

import com.eldenworld.core.EldenWorldCore;
import daripher.skilltree.capability.skill.PlayerSkillsProvider;
import daripher.skilltree.client.screen.SkillTreeScreen;
import daripher.skilltree.client.screen.SkillTreeSelectionScreen;
import daripher.skilltree.client.widget.SkillTreeSelectionButton;
import daripher.skilltree.data.reloader.SkillTreesReloader;
import daripher.skilltree.skill.PassiveSkill;
import daripher.skilltree.skill.PassiveSkillTree;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** EldenWorld filtered Passive Skill Tree picker. */
public final class EldenWorldSkillTreeSelectionScreen extends Screen {
    private static final int BUTTON_SIZE = 19;
    private static final int BUTTON_SPACING = 5;
    private static final ResourceLocation MAIN_TREE = ResourceLocation.parse("skilltree:soldier");
    private static final Map<ResourceLocation, ResourceLocation> TREE_UNLOCKS = createUnlockMap();

    private EldenWorldSkillTreeSelectionScreen() { super(Component.empty()); }

    private static Map<ResourceLocation, ResourceLocation> createUnlockMap() {
        Map<ResourceLocation, ResourceLocation> map = new LinkedHashMap<>();
        unlock(map, "shadowstep", "skilltree:miner_mastery");
        unlock(map, "ghost", "skilltree:miner_subclass_2_mastery");
        unlock(map, "opportunist", "skilltree:miner_subclass_1_mastery");
        unlock(map, "juggernaut", "skilltree:blacksmith_mastery");
        unlock(map, "unyielding", "skilltree:blacksmith_subclass_1_mastery");
        unlock(map, "second_wind", "skilltree:blacksmith_subclass_2_mastery");
        unlock(map, "keen_instinct", "skilltree:hunter_mastery");
        unlock(map, "windrunner", "skilltree:hunter_subclass_1_mastery");
        unlock(map, "pathfinder", "skilltree:hunter_subclass_2_mastery");
        unlock(map, "archmage", "skilltree:alchemist_mastery");
        unlock(map, "manaflow", "skilltree:alchemist_subclass_2_mastery");
        unlock(map, "arcane_ward", "skilltree:alchemist_subclass_1_mastery");
        unlock(map, "master_builder", "skilltree:cook_mastery");
        unlock(map, "enduring_tools", "skilltree:cook_subclass_1_mastery");
        unlock(map, "prospector", "skilltree:cook_subclass_2_mastery");
        unlock(map, "wayfarer", "skilltree:enchanter_mastery");
        unlock(map, "treasure_hunter", "skilltree:enchanter_subclass_1_mastery");
        unlock(map, "survivor", "skilltree:enchanter_subclass_2_mastery");
        return map;
    }

    private static void unlock(Map<ResourceLocation, ResourceLocation> map, String treePath, String unlockSkill) {
        map.put(new ResourceLocation("eldenworld", treePath), ResourceLocation.parse(unlockSkill));
    }

    @Override
    protected void init() {
        clearWidgets();
        addTreeButtons();
    }

    private void addTreeButtons() {
        List<PassiveSkillTree> trees = visibleTrees();
        if (trees.isEmpty()) return;
        int columns = Math.min(7, trees.size());
        int rowWidth = columns * BUTTON_SIZE + (columns - 1) * BUTTON_SPACING;
        int rows = (trees.size() + columns - 1) / columns;
        int step = BUTTON_SIZE + BUTTON_SPACING;
        int startX = width / 2 - rowWidth / 2;
        int startY = height / 2 - (rows * BUTTON_SIZE + (rows - 1) * BUTTON_SPACING) / 2;
        for (int i = 0; i < trees.size(); i++) {
            int x = startX + (i % columns) * step;
            int y = startY + (i / columns) * step;
            PassiveSkillTree tree = trees.get(i);
            addRenderableWidget(new SkillTreeSelectionButton(x, y, BUTTON_SIZE, BUTTON_SIZE, tree.getId()));
        }
    }

    private List<PassiveSkillTree> visibleTrees() {
        Set<ResourceLocation> learned = learnedSkillIds();
        return SkillTreesReloader.getSkillTrees().values().stream()
                .filter(tree -> !tree.getSkillIds().isEmpty())
                .filter(tree -> isVisible(tree.getId(), learned))
                .sorted((a, b) -> sortKey(a.getId()).compareTo(sortKey(b.getId())))
                .toList();
    }

    private static String sortKey(ResourceLocation id) {
        if (MAIN_TREE.equals(id)) return "0";
        int index = 0;
        for (ResourceLocation treeId : TREE_UNLOCKS.keySet()) {
            if (treeId.equals(id)) return String.format("1-%02d", index);
            index++;
        }
        return "9-" + id;
    }

    private static boolean isVisible(ResourceLocation treeId, Set<ResourceLocation> learned) {
        if (MAIN_TREE.equals(treeId)) return true;
        ResourceLocation unlock = TREE_UNLOCKS.get(treeId);
        return unlock != null && learned.contains(unlock);
    }

    private static Set<ResourceLocation> learnedSkillIds() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !PlayerSkillsProvider.hasSkills(player)) return Set.of();
        return PlayerSkillsProvider.get(player).getPlayerSkills().stream()
                .map(PassiveSkill::getId)
                .collect(Collectors.toSet());
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        for (Renderable renderable : renderables) {
            if (renderable instanceof Button button && button.isMouseOver(mouseX, mouseY)) {
                graphics.renderTooltip(font, button.getMessage(), mouseX, mouseY);
            }
        }
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics graphics) {
        ResourceLocation texture = ResourceLocation.parse("skilltree:textures/screen/skill_tree_background.png");
        int size = SkillTreeScreen.BACKGROUND_SIZE;
        graphics.blit(texture, (width - size) / 2, (height - size) / 2, 0, 0F, 0F, size, size, size, size);
    }

    @Mod.EventBusSubscriber(modid = EldenWorldCore.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class Events {
        private Events() {}

        @SubscribeEvent
        public static void onScreenOpening(ScreenEvent.Opening event) {
            if (event.getNewScreen() instanceof SkillTreeSelectionScreen) {
                event.setNewScreen(new EldenWorldSkillTreeSelectionScreen());
            }
        }
    }
}
