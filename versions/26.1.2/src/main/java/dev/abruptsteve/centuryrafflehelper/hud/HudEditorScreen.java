package dev.abruptsteve.centuryrafflehelper.hud;

import dev.abruptsteve.centuryrafflehelper.CenturyRaffleHelperMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class HudEditorScreen extends Screen {
    private HudBlock dragged;
    private int dragOffsetX;
    private int dragOffsetY;
    private int lastMouseX;
    private int lastMouseY;

    public HudEditorScreen() {
        super(Component.literal("Century Raffle Helper HUD Editor"));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        extractTransparentBackground(graphics);
        List<HudBlock> blocks = RaffleHudRenderer.currentBlocks(true);
        fitBlocksToScreen(blocks);
        HudBlock hoveredBlock = hoveredBlock(blocks, mouseX, mouseY);

        graphics.centeredText(
            Minecraft.getInstance().font,
            "Century Raffle Helper Position Editor",
            width / 2,
            12,
            RaffleHudRenderer.opaque(0xffd65a)
        );

        for (HudBlock block : blocks) {
            RaffleHudRenderer.renderBlock(graphics, block, true, block == hoveredBlock || block == dragged);
        }

        renderTooltip(graphics, hoveredBlock, mouseX, mouseY);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        HudBlock block = hoveredBlock(RaffleHudRenderer.currentBlocks(true), event.x(), event.y());
        if (block == null) {
            return super.mouseClicked(event, doubleClick);
        }

        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            dragged = block;
            dragOffsetX = (int) event.x() - block.position().x;
            dragOffsetY = (int) event.y() - block.position().y;
            return true;
        }
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            CenturyRaffleHelperMod.CONFIG_MANAGER.saveAll();
            CenturyRaffleHelperMod.CONFIG_MANAGER.openConfigScreen(configSearch(block));
            return true;
        }
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
            reset(block);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (dragged != null) {
            dragged.position().x = clamp((int) event.x() - dragOffsetX, 0, Math.max(0, width - RaffleHudRenderer.scaledWidth(dragged)));
            dragged.position().y = clamp((int) event.y() - dragOffsetY, 0, Math.max(0, height - RaffleHudRenderer.scaledHeight(dragged)));
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        HudBlock block = dragged == null ? hoveredBlock(RaffleHudRenderer.currentBlocks(true), mouseX, mouseY) : dragged;
        if (block == null || verticalAmount == 0.0) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }

        if (block.id().equals("tasks") && !controlDown()) {
            RaffleHudRenderer.adjustVisibleTasksPerTier(verticalAmount > 0.0 ? 1 : -1);
            HudBlock updatedBlock = currentBlock(block.id());
            if (updatedBlock != null) {
                keepOnScreen(updatedBlock);
            }
            CenturyRaffleHelperMod.CONFIG_MANAGER.saveAll();
            return true;
        }

        block.position().adjustScale(verticalAmount > 0.0 ? 0.1f : -0.1f);
        keepOnScreen(block);
        CenturyRaffleHelperMod.CONFIG_MANAGER.saveAll();
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        HudBlock block = dragged == null ? hoveredBlock(RaffleHudRenderer.currentBlocks(true), lastMouseX, lastMouseY) : dragged;
        if (block != null) {
            int amount = (event.modifiers() & GLFW.GLFW_MOD_SHIFT) != 0 ? 10 : 1;
            switch (event.key()) {
                case GLFW.GLFW_KEY_UP -> {
                    block.position().y -= amount;
                    keepOnScreen(block);
                    CenturyRaffleHelperMod.CONFIG_MANAGER.saveAll();
                    return true;
                }
                case GLFW.GLFW_KEY_DOWN -> {
                    block.position().y += amount;
                    keepOnScreen(block);
                    CenturyRaffleHelperMod.CONFIG_MANAGER.saveAll();
                    return true;
                }
                case GLFW.GLFW_KEY_LEFT -> {
                    block.position().x -= amount;
                    keepOnScreen(block);
                    CenturyRaffleHelperMod.CONFIG_MANAGER.saveAll();
                    return true;
                }
                case GLFW.GLFW_KEY_RIGHT -> {
                    block.position().x += amount;
                    keepOnScreen(block);
                    CenturyRaffleHelperMod.CONFIG_MANAGER.saveAll();
                    return true;
                }
                case GLFW.GLFW_KEY_MINUS, GLFW.GLFW_KEY_KP_SUBTRACT -> {
                    block.position().adjustScale(-0.1f);
                    keepOnScreen(block);
                    CenturyRaffleHelperMod.CONFIG_MANAGER.saveAll();
                    return true;
                }
                case GLFW.GLFW_KEY_EQUAL, GLFW.GLFW_KEY_KP_ADD -> {
                    block.position().adjustScale(0.1f);
                    keepOnScreen(block);
                    CenturyRaffleHelperMod.CONFIG_MANAGER.saveAll();
                    return true;
                }
                case GLFW.GLFW_KEY_R -> {
                    reset(block);
                    return true;
                }
                default -> {
                }
            }
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (dragged != null) {
            dragged = null;
            CenturyRaffleHelperMod.CONFIG_MANAGER.saveAll();
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public void onClose() {
        CenturyRaffleHelperMod.CONFIG_MANAGER.saveAll();
        super.onClose();
    }

    private void renderTooltip(GuiGraphicsExtractor graphics, HudBlock block, int mouseX, int mouseY) {
        if (block == null) {
            return;
        }
        List<Component> lines = block.id().equals("tasks")
            ? List.of(
                Component.literal(block.label()),
                Component.literal("x: " + block.position().x + ", y: " + block.position().y + ", scale: " + roundedScale(block.position().scale())),
                Component.literal("Visible tasks per tier: " + RaffleHudRenderer.visibleTasksPerTier()),
                Component.literal("Scroll to show more/fewer tasks. Ctrl+scroll or +/- to resize."),
                Component.literal("Arrow keys move. R or middle-click resets.")
            )
            : List.of(
                Component.literal(block.label()),
                Component.literal("x: " + block.position().x + ", y: " + block.position().y + ", scale: " + roundedScale(block.position().scale())),
                Component.literal("Right-click to open associated config options."),
                Component.literal("Scroll or +/- to resize. Arrow keys move. R or middle-click resets.")
            );
        graphics.setTooltipForNextFrame(Minecraft.getInstance().font, lines.stream().map(Component::getVisualOrderText).toList(), mouseX, mouseY);
    }

    private static HudBlock currentBlock(String id) {
        for (HudBlock block : RaffleHudRenderer.currentBlocks(true)) {
            if (block.id().equals(id)) {
                return block;
            }
        }
        return null;
    }

    private static HudBlock hoveredBlock(List<HudBlock> blocks, double mouseX, double mouseY) {
        for (int i = blocks.size() - 1; i >= 0; i--) {
            HudBlock block = blocks.get(i);
            if (RaffleHudRenderer.contains(block, mouseX, mouseY)) {
                return block;
            }
        }
        return null;
    }

    private void keepOnScreen(HudBlock block) {
        block.position().x = clamp(block.position().x, 0, Math.max(0, width - RaffleHudRenderer.scaledWidth(block)));
        block.position().y = clamp(block.position().y, 0, Math.max(0, height - RaffleHudRenderer.scaledHeight(block)));
    }

    private void fitBlocksToScreen(List<HudBlock> blocks) {
        for (HudBlock block : blocks) {
            while ((RaffleHudRenderer.scaledWidth(block) > width - 10 || RaffleHudRenderer.scaledHeight(block) > height - 10)
                && block.position().scale() > 0.5f) {
                block.position().adjustScale(-0.1f);
            }
            keepOnScreen(block);
        }
    }

    private static void reset(HudBlock block) {
        switch (block.id()) {
            case "timer" -> block.position().reset(799, 43, 0.8f);
            case "cake" -> block.position().reset(862, 24, 0.8f);
            case "tasks" -> block.position().reset(9, 11, 0.9f);
            case "milestone" -> block.position().reset(25, 300, 1.0f);
            default -> {
            }
        }
        CenturyRaffleHelperMod.CONFIG_MANAGER.saveAll();
    }

    private static String configSearch(HudBlock block) {
        return switch (block.id()) {
            case "timer" -> "Timer HUD";
            case "cake" -> "Cake Eater HUD";
            case "tasks" -> "Task HUD";
            case "milestone" -> "Milestone Tracker HUD";
            default -> block.label();
        };
    }

    private static String roundedScale(float scale) {
        return String.format(java.util.Locale.ROOT, "%.2f", scale);
    }

    private static boolean controlDown() {
        long handle = Minecraft.getInstance().getWindow().handle();
        return GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS
            || GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
