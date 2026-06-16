package dev.abruptsteve.centuryrafflehelper.hud;

import dev.abruptsteve.centuryrafflehelper.CenturyRaffleHelperMod;
import dev.abruptsteve.centuryrafflehelper.config.HudPosition;
import dev.abruptsteve.centuryrafflehelper.raffle.RaffleLogic;
import dev.abruptsteve.centuryrafflehelper.raffle.RaffleTask;
import dev.abruptsteve.centuryrafflehelper.raffle.TaskTier;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

public final class RaffleHudRenderer {
    public static final int EDITOR_BORDER = 3;
    private static final int MAX_TASKS_PER_TIER = 3;
    private static final int MAX_TASK_TEXT_LENGTH = 54;
    private static final int TITLE = 0xffd65a;
    private static final int TEXT = 0xffffff;
    private static final int MUTED = 0xaaaaaa;
    private static final int WARN = 0xffaa00;
    private static final int GOOD = 0x55ff55;

    private RaffleHudRenderer() {
    }

    public static void register() {
        HudElementRegistry.attachElementBefore(
            VanillaHudElements.SLEEP,
            Identifier.fromNamespaceAndPath(CenturyRaffleHelperMod.MOD_ID, "hud"),
            RaffleHudRenderer::render
        );
    }

    public static void render(GuiGraphics graphics, DeltaTracker tickCounter) {
        Minecraft client = Minecraft.getInstance();
        if (client.options.hideGui || !RaffleLogic.shouldRunOnCurrentServer()) return;
        for (HudBlock block : currentBlocks(false)) {
            renderBlock(graphics, block, false, false);
        }
    }

    public static List<HudBlock> currentBlocks(boolean preview) {
        List<HudBlock> blocks = new ArrayList<>();
        var config = CenturyRaffleHelperMod.CONFIG;

        if (config.hud.showTimerHud || preview) {
            blocks.add(new HudBlock("timer", "Timer HUD", config.hud.timerPosition, timerLines(preview)));
        }
        if (config.hud.showTicketHud || preview) {
            blocks.add(new HudBlock("ticket", "Ticket HUD", config.hud.ticketPosition, ticketLines(preview)));
        }
        if (config.hud.showCakeHud || preview) {
            blocks.add(new HudBlock("cake", "Cake HUD", config.hud.cakePosition, cakeLines()));
        }
        if (config.hud.showTaskHud || preview) {
            blocks.add(new HudBlock("tasks", "Task HUD", config.hud.taskPosition, taskLines(preview)));
        }
        return blocks;
    }

    public static void renderBlock(GuiGraphics graphics, HudBlock block, boolean editor, boolean hovered) {
        List<HudLine> lines = block.lines();
        if (lines.isEmpty()) {
            lines = List.of(new HudLine(block.label(), MUTED));
        }

        Font font = Minecraft.getInstance().font;
        HudPosition position = block.position();
        int x = position.x;
        int y = position.y;
        float scale = position.scale();
        int width = blockContentWidth(font, lines);
        int height = blockContentHeight(font, lines);

        int background = editor ? (hovered ? 0x992f6fff : 0x88202020) : 0x66000000;
        graphics.fill(x - EDITOR_BORDER, y - EDITOR_BORDER, x + scaledWidth(block) + 5, y + scaledHeight(block) + 4, background);
        if (editor) {
            graphics.renderOutline(x - EDITOR_BORDER, y - EDITOR_BORDER, scaledWidth(block) + 8, scaledHeight(block) + 7, hovered ? 0xffffffff : 0xffaaaaaa);
        }

        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale, scale);
        try {
            int lineY = 0;
            for (HudLine line : lines) {
                graphics.drawString(font, line.text(), 0, lineY, line.color(), true);
                lineY += font.lineHeight + 1;
            }
        } finally {
            graphics.pose().popMatrix();
        }
    }

    public static boolean contains(HudBlock block, double mouseX, double mouseY) {
        HudPosition position = block.position();
        return mouseX >= position.x - EDITOR_BORDER && mouseX <= position.x + scaledWidth(block) + 5
            && mouseY >= position.y - EDITOR_BORDER && mouseY <= position.y + scaledHeight(block) + 4;
    }

    public static int scaledWidth(HudBlock block) {
        return Math.max(1, Math.round(blockContentWidth(Minecraft.getInstance().font, block.lines()) * block.position().scale()));
    }

    public static int scaledHeight(HudBlock block) {
        return Math.max(1, Math.round(blockContentHeight(Minecraft.getInstance().font, block.lines()) * block.position().scale()));
    }

    private static int blockContentWidth(Font font, List<HudLine> lines) {
        int width = 1;
        for (HudLine line : lines) {
            width = Math.max(width, font.width(line.text()));
        }
        return width;
    }

    private static int blockContentHeight(Font font, List<HudLine> lines) {
        return Math.max(font.lineHeight, lines.size() * (font.lineHeight + 1) - 1);
    }

    private static List<HudLine> timerLines(boolean preview) {
        var config = CenturyRaffleHelperMod.CONFIG;
        List<HudLine> lines = new ArrayList<>();
        lines.add(new HudLine("Century Raffle", TITLE));
        if (config.timers.speedRaffle || preview) {
            lines.add(new HudLine("Speed: " + RaffleLogic.formatDuration(RaffleLogic.millisUntilNextSpeedRaffle()), TEXT));
        }
        if (config.timers.dailyRaffle || preview) {
            lines.add(new HudLine("Daily: " + RaffleLogic.formatDuration(RaffleLogic.millisUntilNextDailyRaffle()), TEXT));
        }
        if (config.timers.bigOne || preview) {
            long bigOne = preview && CenturyRaffleHelperMod.STATE.eventEndEpochMillis <= 0
                ? 5L * 24L * 60L * 60L * 1000L
                : RaffleLogic.millisUntilBigOne();
            if (bigOne < 0) {
                lines.add(new HudLine("Reminder: open raffle menu", WARN));
            } else {
                lines.add(new HudLine("The Big One: " + (bigOne == 0 ? "READY" : RaffleLogic.formatDuration(bigOne)), bigOne == 0 ? GOOD : TEXT));
            }
        }
        return lines;
    }

    private static List<HudLine> ticketLines(boolean preview) {
        if (CenturyRaffleHelperMod.STATE.ticketsKnown || preview) {
            int tickets = preview && !CenturyRaffleHelperMod.STATE.ticketsKnown ? 123 : CenturyRaffleHelperMod.STATE.totalTickets;
            return List.of(new HudLine("Raffle Tickets: " + tickets, TEXT));
        }
        return List.of(new HudLine("Raffle Tickets: open raffle menu", WARN));
    }

    private static List<HudLine> cakeLines() {
        return List.of(new HudLine("Cake Slices: " + CenturyRaffleHelperMod.STATE.cakeSlicesLeft + " left", TEXT));
    }

    private static List<HudLine> taskLines(boolean preview) {
        var config = CenturyRaffleHelperMod.CONFIG.tasks;
        List<HudLine> lines = new ArrayList<>();
        lines.add(new HudLine("Daily Raffle Tasks", TITLE));
        int shown = 0;
        int visible = 0;
        for (TaskTier tier : TaskTier.values()) {
            if (!showTier(tier) && !preview) continue;
            List<RaffleTask> remaining = RaffleTask.ALL.stream()
                .filter(task -> task.tier == tier)
                .filter(task -> preview || !CenturyRaffleHelperMod.STATE.isComplete(task))
                .toList();
            visible += remaining.size();
            if (remaining.isEmpty()) continue;
            lines.add(new HudLine(tier.label, tier.color));
            int displayCount = Math.min(MAX_TASKS_PER_TIER, remaining.size());
            for (int i = 0; i < displayCount; i++) {
                RaffleTask task = remaining.get(i);
                lines.add(new HudLine((i + 1) + ". " + displayTaskName(task), TEXT));
                shown++;
            }
            if (remaining.size() > displayCount) {
                lines.add(new HudLine("...", MUTED));
            }
        }
        if (shown == 0 && (config.showCompletedSummary || preview)) {
            lines.add(new HudLine(visible == 0 ? "All visible tasks complete" : "No tasks selected", GOOD));
        }
        return lines;
    }

    private static String displayTaskName(RaffleTask task) {
        var config = CenturyRaffleHelperMod.CONFIG.tasks;
        String text = config.showDescriptions ? task.description : task.title;
        if (text.length() <= MAX_TASK_TEXT_LENGTH) {
            return text;
        }
        return text.substring(0, MAX_TASK_TEXT_LENGTH - 3) + "...";
    }

    private static boolean showTier(TaskTier tier) {
        var config = CenturyRaffleHelperMod.CONFIG.tasks;
        return switch (tier) {
            case EASY -> config.showEasy;
            case MEDIUM -> config.showMedium;
            case HARD -> config.showHard;
        };
    }
}
