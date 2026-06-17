package dev.abruptsteve.centuryrafflehelper.raffle;

import dev.abruptsteve.centuryrafflehelper.CenturyRaffleHelperMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RaffleLogic {
    private static final Pattern GAINED_TICKETS = Pattern.compile("(?i)\\+(\\d[\\d,]*)\\s+raffle tickets?\\b");
    private static final Pattern OWNED_TICKETS_1 = Pattern.compile("(?i)(?:your|you have|total)[^\\d]{0,32}(\\d[\\d,]*)\\s+raffle tickets?");
    private static final Pattern OWNED_TICKETS_2 = Pattern.compile("(?i)raffle tickets?[^\\d]{0,32}(\\d[\\d,]*)");
    private static final Pattern COLON_TIME = Pattern.compile("(\\d{1,5}):(\\d{2})(?::(\\d{2}))?");
    private static final Pattern TOKEN_TIME = Pattern.compile("(?i)(\\d+)\\s*([dhms])");
    private static final Pattern COMPLETION_WORD = Pattern.compile("\\b(complete|completed|done|claimed)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern ENTERED_RAFFLE_TICKETS = Pattern.compile("(?i)you\\s+entered\\s*:?\\s*(\\d[\\d,]*)\\s+tickets?");
    private static final Pattern TASK_TIER_LINE = Pattern.compile("(?i)^(easy|medium|hard)\\s+task$");
    private static final Pattern TASK_PROGRESS = Pattern.compile("(?i)\\[(\\d[\\d,]*)/(\\d[\\d,]*)\\]\\s*(easy|medium|hard)\\s+tasks?");
    private static final Pattern TOTAL_TASK_PROGRESS = Pattern.compile("(?i)total\\s+tasks\\s+completed\\s*:?\\s*\\[(\\d[\\d,]*)/(\\d[\\d,]*)\\]");
    private static final Pattern ITEM_ID_LINE = Pattern.compile("^[a-z0-9_.-]+:[a-z0-9_/.-]+$");
    private static final Pattern COMPONENTS_LINE = Pattern.compile("(?i)^\\d+\\s+component\\(s\\)$");
    private static final List<RaffleTask> TASKS_BY_LONGEST_NAME = RaffleTask.ALL.stream()
        .sorted(Comparator.comparingInt((RaffleTask task) -> task.title.length()).reversed())
        .toList();
    private static String lastTicketGainMessage = "";
    private static long lastTicketGainMillis = 0L;

    private RaffleLogic() {
    }

    public static boolean shouldRunOnCurrentServer() {
        if (!CenturyRaffleHelperMod.CONFIG.general.onlyOnHypixel) {
            return true;
        }
        Minecraft client = Minecraft.getInstance();
        if (client.getCurrentServer() != null && isHypixelAddress(client.getCurrentServer().ip)) {
            return true;
        }
        return hasHypixelScoreboard(client);
    }

    public static void handleChat(String rawMessage, boolean trustedServerMessage) {
        if (rawMessage == null || rawMessage.isBlank()) return;
        String plain = stripFormatting(rawMessage);
        String normalized = RaffleTask.normalize(plain);

        int gainedTickets = trustedServerMessage ? ticketGain(plain) : 0;
        if (gainedTickets > 0 && !duplicateTicketGain(plain)) {
            if (plain.startsWith("CAKE SLICE!")) {
                CenturyRaffleHelperMod.STATE.consumeCakeSlice();
            }
            CenturyRaffleHelperMod.STATE.addTickets(gainedTickets);
        }

        if (looksLikeCompletion(normalized)) {
            for (RaffleTask task : TASKS_BY_LONGEST_NAME) {
                if (task.isMentionedIn(plain)) {
                    CenturyRaffleHelperMod.STATE.setComplete(task, true);
                    break;
                }
            }
            CenturyRaffleHelperMod.STATE.setObservedTaskCompleteFromText(plain, true);
        }

        parseEventEndFromLine(plain);
        parseTaskResetFromLine(plain);
    }

    public static void tick(Minecraft client) {
        CenturyRaffleHelperMod.STATE.ensureDailyFresh();
        scanScoreboard(client);
        scanOpenInventory(client);
    }

    private static void scanScoreboard(Minecraft client) {
        if (client.level == null) return;
        Scoreboard scoreboard = client.level.getScoreboard();
        Objective objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
        if (objective == null) return;

        for (PlayerScoreEntry entry : scoreboard.listPlayerScores(objective)) {
            if (entry.isHidden()) continue;
            String line = scoreboardLine(scoreboard, entry);
            parseEventEndFromLine(stripFormatting(line));
        }
    }

    private static String scoreboardLine(Scoreboard scoreboard, PlayerScoreEntry entry) {
        Component owner = entry.ownerName() == null ? Component.literal(entry.owner()) : entry.ownerName();
        PlayerTeam team = scoreboard.getPlayersTeam(entry.owner());
        return PlayerTeam.formatNameForTeam(team, owner).getString();
    }

    private static void scanOpenInventory(Minecraft client) {
        Screen screen = client.screen;
        if (!(screen instanceof AbstractContainerScreen<?> containerScreen)) {
            return;
        }

        String title = stripFormatting(screen.getTitle().getString());
        boolean likelyRaffleMenu = containsAny(title, "Raffle", "Daily Quests", "Daily Tasks", "Incredible Raffle Box");
        if (!likelyRaffleMenu) return;

        boolean taskInventory = containsAny(title, "Raffle Tasks", "Daily Tasks", "Daily Quests");
        List<ObservedRaffleTask> observedTasks = new ArrayList<>();
        for (Slot slot : containerScreen.getMenu().slots) {
            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) continue;
            List<String> tooltip = tooltipLines(client, stack);
            parseInventoryText(title, tooltip);
            if (taskInventory) {
                ObservedRaffleTask task = parseTaskTooltip(tooltip);
                if (task != null) {
                    observedTasks.add(task);
                }
            }
        }

        if (taskInventory && !observedTasks.isEmpty()) {
            CenturyRaffleHelperMod.STATE.replaceObservedTasks(observedTasks);
        }
    }

    private static List<String> tooltipLines(Minecraft client, ItemStack stack) {
        List<String> lines = new ArrayList<>();
        lines.add(stack.getHoverName().getString());
        Item.TooltipContext context = client.level == null ? Item.TooltipContext.EMPTY : Item.TooltipContext.of(client.level);
        for (Component component : stack.getTooltipLines(context, client.player, TooltipFlag.NORMAL)) {
            lines.add(component.getString());
        }
        return lines;
    }

    private static void parseInventoryText(String inventoryTitle, List<String> tooltip) {
        String joined = stripFormatting(String.join("\n", tooltip));
        parseOwnedTickets(joined);
        parseRaffleDrawTooltip(joined);
        parseEventEndFromLine(joined);
        parseTaskResetFromLine(joined);
        parseTaskProgress(joined);

        boolean isTaskArea = containsAny(inventoryTitle, "Daily Quests", "Daily Tasks", "Raffle", "Incredible Raffle Box");
        if (!isTaskArea) return;

        String normalized = RaffleTask.normalize(joined);
        for (RaffleTask task : TASKS_BY_LONGEST_NAME) {
            if (!task.isMentionedIn(joined)) continue;
            boolean completed = COMPLETION_WORD.matcher(normalized).find() && !normalized.contains("incomplete");
            CenturyRaffleHelperMod.STATE.setComplete(task, completed);
            break;
        }
    }

    private static ObservedRaffleTask parseTaskTooltip(List<String> tooltip) {
        List<String> lines = cleanedTooltipLines(tooltip);
        if (lines.isEmpty()) return null;

        int tierIndex = -1;
        TaskTier tier = null;
        for (int i = 0; i < lines.size(); i++) {
            Matcher matcher = TASK_TIER_LINE.matcher(lines.get(i));
            if (matcher.matches()) {
                tier = parseTier(matcher.group(1));
                tierIndex = i;
                break;
            }
        }

        if (tier == null || tierIndex <= 0) {
            return null;
        }

        String title = "";
        for (int i = 0; i < tierIndex; i++) {
            String line = lines.get(i);
            if (!isTooltipMetadata(line)) {
                title = line;
                break;
            }
        }
        if (title.isBlank()) return null;

        List<String> descriptionLines = new ArrayList<>();
        boolean complete = false;
        for (int i = tierIndex + 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (isIncompleteLine(line)) {
                complete = false;
                break;
            }
            if (isCompleteLine(line)) {
                complete = true;
                break;
            }
            if (isTooltipMetadata(line)) {
                break;
            }
            descriptionLines.add(line);
        }

        return new ObservedRaffleTask(tier, title, String.join(" ", descriptionLines), complete);
    }

    private static List<String> cleanedTooltipLines(List<String> tooltip) {
        List<String> lines = new ArrayList<>();
        for (String rawLine : tooltip) {
            String line = stripFormatting(rawLine).trim();
            if (line.isBlank()) continue;
            if (!lines.isEmpty() && lines.get(lines.size() - 1).equals(line)) continue;
            lines.add(line);
        }
        return lines;
    }

    private static void parseTaskProgress(String text) {
        Matcher tierProgress = TASK_PROGRESS.matcher(text);
        while (tierProgress.find()) {
            CenturyRaffleHelperMod.STATE.setTaskProgress(
                parseTier(tierProgress.group(3)),
                parseNumber(tierProgress.group(1)),
                parseNumber(tierProgress.group(2))
            );
        }

        Matcher totalProgress = TOTAL_TASK_PROGRESS.matcher(text);
        if (totalProgress.find()) {
            CenturyRaffleHelperMod.STATE.setTotalTaskProgress(
                parseNumber(totalProgress.group(1)),
                parseNumber(totalProgress.group(2))
            );
        }
    }

    private static void parseOwnedTickets(String text) {
        Matcher first = OWNED_TICKETS_1.matcher(text);
        if (first.find()) {
            CenturyRaffleHelperMod.STATE.setTickets(parseNumber(first.group(1)));
            return;
        }
        Matcher second = OWNED_TICKETS_2.matcher(text);
        if (second.find()) {
            CenturyRaffleHelperMod.STATE.setTickets(parseNumber(second.group(1)));
        }
    }

    private static void parseRaffleDrawTooltip(String text) {
        RaffleDraw raffle = raffleDrawFromText(text);
        if (raffle == null) {
            return;
        }

        Matcher entered = ENTERED_RAFFLE_TICKETS.matcher(text);
        if (entered.find()) {
            CenturyRaffleHelperMod.STATE.setRaffleEnteredTickets(raffle, parseNumber(entered.group(1)));
        }

        String lower = text.toLowerCase(Locale.ROOT);
        int untilDrawIndex = lower.indexOf("until draw");
        if (untilDrawIndex >= 0) {
            long millis = parseDurationMillis(text.substring(untilDrawIndex));
            if (millis > 0L) {
                CenturyRaffleHelperMod.STATE.setRaffleDrawFromDuration(raffle, millis);
                if (raffle == RaffleDraw.BIG_ONE) {
                    CenturyRaffleHelperMod.STATE.setEventEndFromDuration(millis);
                }
            }
        }
    }

    private static void parseEventEndFromLine(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        if (!containsAny(lower, "century raffle", "big one", "raffle ends", "event ends", "concludes")) {
            return;
        }
        long millis = parseDurationMillis(text);
        if (millis > 0) {
            CenturyRaffleHelperMod.STATE.setEventEndFromDuration(millis);
        }
    }

    private static void parseTaskResetFromLine(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        int resetIndex = lower.indexOf("time until reset");
        if (resetIndex < 0) {
            return;
        }

        long millis = parseDurationMillis(text.substring(resetIndex));
        if (millis > 0) {
            CenturyRaffleHelperMod.STATE.setTaskResetFromDuration(millis);
        }
    }

    public static long parseDurationMillis(String text) {
        Matcher colon = COLON_TIME.matcher(text);
        if (colon.find()) {
            long first = Long.parseLong(colon.group(1));
            long second = Long.parseLong(colon.group(2));
            String thirdGroup = colon.group(3);
            if (thirdGroup == null) {
                return ((first * 60L) + second) * 60_000L;
            }
            long third = Long.parseLong(thirdGroup);
            return ((first * 3600L) + (second * 60L) + third) * 1000L;
        }

        Matcher token = TOKEN_TIME.matcher(text);
        long total = 0L;
        while (token.find()) {
            long value = Long.parseLong(token.group(1));
            switch (token.group(2).toLowerCase(Locale.ROOT)) {
                case "d" -> total += value * 86_400_000L;
                case "h" -> total += value * 3_600_000L;
                case "m" -> total += value * 60_000L;
                case "s" -> total += value * 1_000L;
                default -> {
                }
            }
        }
        return total;
    }

    public static long millisUntilNextSpeedRaffle() {
        long period = 2L * 60L * 60L * 1000L;
        long now = System.currentTimeMillis();
        long left = period - Math.floorMod(now, period);
        return left == period ? 0L : left;
    }

    public static long millisUntilNextDailyRaffle() {
        int resetHour = CenturyRaffleHelperMod.CONFIG.general.resetHourUtc;
        ZonedDateTime now = Instant.now().atZone(ZoneOffset.UTC);
        ZonedDateTime next = now.withHour(resetHour).withMinute(0).withSecond(0).withNano(0);
        if (!next.isAfter(now)) {
            next = next.plusDays(1);
        }
        return next.toInstant().toEpochMilli() - System.currentTimeMillis();
    }

    public static long millisUntilBigOne() {
        long end = CenturyRaffleHelperMod.STATE.eventEndEpochMillis;
        if (end <= 0) return -1L;
        return Math.max(0L, end - System.currentTimeMillis());
    }

    public static long millisUntilRaffleDraw(RaffleDraw raffle) {
        long end = CenturyRaffleHelperMod.STATE.raffleDrawEpochMillis(raffle);
        if (end <= 0L) return -1L;
        return Math.max(0L, end - System.currentTimeMillis());
    }

    public static long millisUntilTaskReset() {
        long end = CenturyRaffleHelperMod.STATE.taskResetEpochMillis;
        if (end <= 0) return -1L;
        return Math.max(0L, end - System.currentTimeMillis());
    }

    public static String formatDuration(long millis) {
        if (millis < 0) return "Unknown";
        long seconds = Math.max(0L, millis / 1000L);
        long days = seconds / 86_400L;
        seconds %= 86_400L;
        long hours = seconds / 3_600L;
        seconds %= 3_600L;
        long minutes = seconds / 60L;
        seconds %= 60L;

        if (days > 0) return days + "d " + hours + "h " + minutes + "m";
        if (hours > 0) return hours + "h " + minutes + "m " + seconds + "s";
        if (minutes > 0) return minutes + "m " + seconds + "s";
        return seconds + "s";
    }

    public static String stripFormatting(String text) {
        return text == null ? "" : text.replaceAll("\\u00a7.", "");
    }

    private static boolean looksLikeCompletion(String normalized) {
        return (normalized.contains("complete") || normalized.contains("completed") || normalized.contains("quest") || normalized.contains("raffle ticket"))
            && !normalized.contains("incomplete");
    }

    private static boolean containsAny(String text, String... needles) {
        for (String needle : needles) {
            if (text.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static int ticketGain(String plain) {
        Matcher gained = GAINED_TICKETS.matcher(plain);
        return gained.find() ? parseNumber(gained.group(1)) : 0;
    }

    private static boolean duplicateTicketGain(String plain) {
        long now = System.currentTimeMillis();
        if (plain.equals(lastTicketGainMessage) && now - lastTicketGainMillis < 1_000L) {
            return true;
        }

        lastTicketGainMessage = plain;
        lastTicketGainMillis = now;
        return false;
    }

    private static boolean hasHypixelScoreboard(Minecraft client) {
        if (client.level == null) return false;
        Scoreboard scoreboard = client.level.getScoreboard();
        Objective objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
        if (objective == null) return false;

        for (PlayerScoreEntry entry : scoreboard.listPlayerScores(objective)) {
            if (entry.isHidden()) continue;
            String line = stripFormatting(scoreboardLine(scoreboard, entry)).toLowerCase(Locale.ROOT);
            if (isHypixelAddress(line)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isHypixelAddress(String address) {
        if (address == null) return false;
        String normalized = address.toLowerCase(Locale.ROOT);
        return normalized.contains("hypixel.net")
            || normalized.contains("hypixel.io")
            || normalized.contains("alpha.hypixel");
    }

    private static RaffleDraw raffleDrawFromText(String text) {
        String normalized = text.toLowerCase(Locale.ROOT);
        if (normalized.contains("speed raffle")) {
            return RaffleDraw.SPEED;
        }
        if (normalized.contains("daily raffle") && !normalized.contains("daily raffle tasks")) {
            return RaffleDraw.DAILY;
        }
        if (normalized.contains("big one")) {
            return RaffleDraw.BIG_ONE;
        }
        return null;
    }

    private static boolean isIncompleteLine(String line) {
        return RaffleTask.normalize(line).equals("incomplete");
    }

    private static boolean isCompleteLine(String line) {
        String normalized = RaffleTask.normalize(line);
        return normalized.equals("complete")
            || normalized.equals("completed")
            || normalized.equals("status complete")
            || normalized.equals("status completed");
    }

    private static boolean isTooltipMetadata(String line) {
        return ITEM_ID_LINE.matcher(line).matches()
            || COMPONENTS_LINE.matcher(line).matches()
            || line.equalsIgnoreCase("minecraft:map")
            || line.equalsIgnoreCase("minecraft:filled_map")
            || line.equalsIgnoreCase("minecraft:paper");
    }

    private static TaskTier parseTier(String text) {
        String normalized = text.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "medium" -> TaskTier.MEDIUM;
            case "hard" -> TaskTier.HARD;
            default -> TaskTier.EASY;
        };
    }

    private static int parseNumber(String text) {
        return Integer.parseInt(text.replace(",", ""));
    }
}
