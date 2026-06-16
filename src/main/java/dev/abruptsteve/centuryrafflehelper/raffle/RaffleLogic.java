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
    private static final Pattern GAINED_TICKETS = Pattern.compile("(?i)\\+(\\d[\\d,]*)\\s+raffle tickets?");
    private static final Pattern OWNED_TICKETS_1 = Pattern.compile("(?i)(?:your|you have|total)[^\\d]{0,32}(\\d[\\d,]*)\\s+raffle tickets?");
    private static final Pattern OWNED_TICKETS_2 = Pattern.compile("(?i)raffle tickets?[^\\d]{0,32}(\\d[\\d,]*)");
    private static final Pattern COLON_TIME = Pattern.compile("(\\d{1,5}):(\\d{2})(?::(\\d{2}))?");
    private static final Pattern TOKEN_TIME = Pattern.compile("(?i)(\\d+)\\s*([dhms])");
    private static final Pattern COMPLETION_WORD = Pattern.compile("\\b(complete|completed|done|claimed)\\b", Pattern.CASE_INSENSITIVE);
    private static final List<RaffleTask> TASKS_BY_LONGEST_NAME = RaffleTask.ALL.stream()
        .sorted(Comparator.comparingInt((RaffleTask task) -> task.title.length()).reversed())
        .toList();

    private RaffleLogic() {
    }

    public static boolean shouldRunOnCurrentServer() {
        if (!CenturyRaffleHelperMod.CONFIG.general.onlyOnHypixel) {
            return true;
        }
        Minecraft client = Minecraft.getInstance();
        if (client.getCurrentServer() == null || client.getCurrentServer().ip == null) {
            return false;
        }
        String address = client.getCurrentServer().ip.toLowerCase(Locale.ROOT);
        return address.contains("hypixel.net") || address.contains("hypixel.io");
    }

    public static void handleChat(String rawMessage) {
        if (rawMessage == null || rawMessage.isBlank()) return;
        String plain = stripFormatting(rawMessage);
        String normalized = RaffleTask.normalize(plain);

        if (plain.contains("CAKE SLICE!")) {
            CenturyRaffleHelperMod.STATE.consumeCakeSlice();
        }

        Matcher gained = GAINED_TICKETS.matcher(plain);
        if (gained.find()) {
            CenturyRaffleHelperMod.STATE.addTickets(parseNumber(gained.group(1)));
        }

        if (looksLikeCompletion(normalized)) {
            for (RaffleTask task : TASKS_BY_LONGEST_NAME) {
                if (task.isMentionedIn(plain)) {
                    CenturyRaffleHelperMod.STATE.setComplete(task, true);
                    break;
                }
            }
        }

        parseEventEndFromLine(plain);
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

        for (Slot slot : containerScreen.getMenu().slots) {
            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) continue;
            List<String> tooltip = tooltipLines(client, stack);
            parseInventoryText(title, tooltip);
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
        parseEventEndFromLine(joined);

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
        if (hours > 0) return hours + "h " + minutes + "m";
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

    private static int parseNumber(String text) {
        return Integer.parseInt(text.replace(",", ""));
    }
}
