package dev.abruptsteve.centuryrafflehelper.hud;

import dev.abruptsteve.centuryrafflehelper.CenturyRaffleHelperMod;
import dev.abruptsteve.centuryrafflehelper.config.HudPosition;
import dev.abruptsteve.centuryrafflehelper.raffle.ObservedRaffleTask;
import dev.abruptsteve.centuryrafflehelper.raffle.RaffleDraw;
import dev.abruptsteve.centuryrafflehelper.raffle.RaffleLogic;
import dev.abruptsteve.centuryrafflehelper.raffle.TaskProgress;
import dev.abruptsteve.centuryrafflehelper.raffle.TaskTier;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

public final class RaffleHudRenderer {
    public static final int EDITOR_BORDER = 3;
    public static final int MIN_VISIBLE_TASKS_PER_TIER = 1;
    public static final int MAX_VISIBLE_TASKS_PER_TIER = 7;
    private static final int MAX_TASK_TEXT_LENGTH = 54;
    private static final long PREVIEW_SPEED_DRAW_MILLIS = 1L * 60L * 60L * 1000L + 40L * 60L * 1000L + 13L * 1000L;
    private static final long PREVIEW_DAILY_DRAW_MILLIS = 5L * 60L * 60L * 1000L + 40L * 60L * 1000L + 8L * 1000L;
    private static final long PREVIEW_BIG_ONE_MILLIS = 3L * 24L * 60L * 60L * 1000L + 23L * 60L * 60L * 1000L + 59L * 60L * 1000L;
    private static final long PREVIEW_TASK_RESET_MILLIS = 5L * 60L * 1000L + 47L * 1000L;
    private static final int PREVIEW_MILESTONE_TICKETS = 87;
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
            RaffleHudRenderer::extractRenderState
        );
    }

    public static void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker tickCounter) {
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
        if (config.hud.showCakeHud || preview) {
            blocks.add(new HudBlock("cake", "Cake HUD", config.hud.cakePosition, cakeLines()));
        }
        if (config.hud.showTaskHud || preview) {
            blocks.add(new HudBlock("tasks", "Task HUD", config.hud.taskPosition, taskLines(preview)));
        }
        if (config.hud.showMilestoneHud || preview) {
            blocks.add(new HudBlock("milestone", "Milestone Tracker HUD", config.hud.milestonePosition, milestoneLines(preview)));
        }
        return blocks;
    }

    public static void renderBlock(GuiGraphicsExtractor graphics, HudBlock block, boolean editor, boolean hovered) {
        List<HudLine> lines = block.lines();
        if (lines.isEmpty()) {
            lines = List.of(new HudLine(block.label(), MUTED));
        }

        Font font = Minecraft.getInstance().font;
        HudPosition position = block.position();
        int x = position.x;
        int y = position.y;
        float scale = position.scale();

        if (editor) {
            graphics.outline(x - EDITOR_BORDER, y - EDITOR_BORDER, scaledWidth(block) + 8, scaledHeight(block) + 7, hovered ? 0xffffffff : 0xffaaaaaa);
        }

        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale, scale);
        try {
            int lineY = 0;
            for (HudLine line : lines) {
                graphics.text(font, line.text(), 0, lineY, opaque(line.color()), true);
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
            lines.add(raffleTimerLine(RaffleDraw.SPEED, PREVIEW_SPEED_DRAW_MILLIS, 12, preview));
        }
        if (config.timers.dailyRaffle || preview) {
            lines.add(raffleTimerLine(RaffleDraw.DAILY, PREVIEW_DAILY_DRAW_MILLIS, 16, preview));
        }
        if (config.timers.bigOne || preview) {
            lines.add(raffleTimerLine(RaffleDraw.BIG_ONE, PREVIEW_BIG_ONE_MILLIS, 0, preview));
        }
        return lines;
    }

    private static List<HudLine> cakeLines() {
        long reset = CenturyRaffleHelperMod.STATE.cakeSliceResetEpochMillis() - System.currentTimeMillis();
        String timer = reset <= 0L ? "READY" : RaffleLogic.formatDuration(reset);
        return List.of(new HudLine("Cake Slices: " + CenturyRaffleHelperMod.STATE.cakeSlicesLeft + " left (" + timer + ")", TEXT));
    }

    private static List<HudLine> milestoneLines(boolean preview) {
        var state = CenturyRaffleHelperMod.STATE;
        int tickets = preview ? PREVIEW_MILESTONE_TICKETS : state.milestoneTickets;
        int target = preview ? 100 : state.milestoneTarget();
        String label = preview ? "V" : state.milestoneLabel();
        int shownTickets = Math.min(Math.max(0, tickets), target);
        String text = "Milestone " + label + ": " + shownTickets + "/" + target;
        if (!preview && state.allMilestonesReached()) {
            text += " complete";
        }
        return List.of(
            new HudLine("Raffle Milestone Tracker", TITLE),
            new HudLine(text, !preview && state.allMilestonesReached() ? GOOD : TEXT)
        );
    }

    private static List<HudLine> taskLines(boolean preview) {
        var config = CenturyRaffleHelperMod.CONFIG.tasks;
        List<HudLine> lines = new ArrayList<>();
        lines.add(new HudLine("Daily Raffle Tasks", TITLE));
        addTaskResetLine(lines, preview);

        List<ObservedRaffleTask> tasks = observedTasks(preview);
        if (tasks.isEmpty()) {
            addTaskProgressSummary(lines);
            lines.add(new HudLine("Open Raffle Tasks menu", WARN));
            return lines;
        }

        int shown = 0;
        int selectedTotal = 0;
        for (TaskTier tier : TaskTier.values()) {
            if (!showTier(tier) && !preview) continue;
            List<ObservedRaffleTask> tierTasks = tasks.stream()
                .filter(task -> task.tier == tier)
                .toList();
            selectedTotal += tierTasks.size();
            List<ObservedRaffleTask> remaining = tierTasks.stream()
                .filter(task -> preview || !task.complete)
                .toList();
            if (remaining.isEmpty()) continue;
            lines.add(new HudLine(tier.label, tier.color));
            int displayCount = Math.min(visibleTasksPerTier(), remaining.size());
            for (int i = 0; i < displayCount; i++) {
                ObservedRaffleTask task = remaining.get(i);
                lines.add(new HudLine((i + 1) + ". " + displayTaskName(task), TEXT));
                shown++;
            }
            if (remaining.size() > displayCount) {
                lines.add(new HudLine("...", MUTED));
            }
        }
        if (shown == 0 && (config.showCompletedSummary || preview)) {
            lines.add(new HudLine(selectedTotal == 0 ? "No tasks selected" : "All visible tasks complete", GOOD));
        }
        return lines;
    }

    private static void addTaskResetLine(List<HudLine> lines, boolean preview) {
        var config = CenturyRaffleHelperMod.CONFIG.tasks;
        if (!config.showTimeUntilReset && !preview) {
            return;
        }

        long reset = preview && CenturyRaffleHelperMod.STATE.taskResetEpochMillis <= 0L
            ? PREVIEW_TASK_RESET_MILLIS
            : RaffleLogic.millisUntilTaskReset();
        if (reset < 0L) {
            lines.add(new HudLine("Time until reset: open raffle box", WARN));
        } else {
            lines.add(new HudLine("Time until reset: " + (reset == 0L ? "READY" : RaffleLogic.formatDuration(reset)), reset == 0L ? GOOD : TEXT));
        }
    }

    private static List<ObservedRaffleTask> observedTasks(boolean preview) {
        List<ObservedRaffleTask> tasks = CenturyRaffleHelperMod.STATE.observedTasks;
        if ((tasks == null || tasks.isEmpty()) && preview) {
            return List.of(
                new ObservedRaffleTask(TaskTier.EASY, "Forest Collector", "Obtain some Forest Essence.", false),
                new ObservedRaffleTask(TaskTier.MEDIUM, "Arcane Slayer", "Kill an Arcane mob.", false),
                new ObservedRaffleTask(TaskTier.HARD, "Gold Fisher", "Fish up a GOLD or higher tier Trophy.", false)
            );
        }
        return tasks == null ? List.of() : tasks;
    }

    private static void addTaskProgressSummary(List<HudLine> lines) {
        for (TaskTier tier : TaskTier.values()) {
            if (!showTier(tier)) continue;
            TaskProgress progress = CenturyRaffleHelperMod.STATE.taskProgress.get(tier);
            if (progress != null && progress.isKnown()) {
                lines.add(new HudLine(tier.label + ": " + progress.completed + "/" + progress.total + " complete", tier.color));
            }
        }
    }

    private static String displayTaskName(ObservedRaffleTask task) {
        var config = CenturyRaffleHelperMod.CONFIG.tasks;
        String text = config.showDescriptions ? task.description : task.title;
        if (text == null || text.isBlank()) {
            text = task.title;
        }
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

    public static int opaque(int color) {
        return (color & 0xff000000) == 0 ? color | 0xff000000 : color;
    }

    public static int visibleTasksPerTier() {
        var config = CenturyRaffleHelperMod.CONFIG.tasks;
        int clamped = clamp(config.visibleTasksPerTier, MIN_VISIBLE_TASKS_PER_TIER, MAX_VISIBLE_TASKS_PER_TIER);
        if (config.visibleTasksPerTier != clamped) {
            config.visibleTasksPerTier = clamped;
        }
        return clamped;
    }

    public static void adjustVisibleTasksPerTier(int delta) {
        var config = CenturyRaffleHelperMod.CONFIG.tasks;
        config.visibleTasksPerTier = clamp(config.visibleTasksPerTier + delta, MIN_VISIBLE_TASKS_PER_TIER, MAX_VISIBLE_TASKS_PER_TIER);
    }

    private static HudLine raffleTimerLine(RaffleDraw raffle, long previewMillis, int previewTickets, boolean preview) {
        long millis = preview && CenturyRaffleHelperMod.STATE.raffleDrawEpochMillis(raffle) <= 0L
            ? previewMillis
            : RaffleLogic.millisUntilRaffleDraw(raffle);
        if (millis < 0L) {
            return new HudLine(raffle.hudLabel + ": open raffle box", WARN);
        }

        int tickets = preview && CenturyRaffleHelperMod.STATE.raffleEnteredTickets(raffle) < 0
            ? previewTickets
            : CenturyRaffleHelperMod.STATE.raffleEnteredTickets(raffle);
        String timer = millis == 0L ? "READY" : RaffleLogic.formatDuration(millis);
        return new HudLine(raffle.hudLabel + ": " + timer + " (" + ticketText(tickets) + ")", millis == 0L ? GOOD : TEXT);
    }

    private static String ticketText(int tickets) {
        if (tickets < 0) {
            return "? tickets";
        }
        return tickets + (tickets == 1 ? " ticket" : " tickets");
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
