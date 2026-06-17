package dev.abruptsteve.centuryrafflehelper.highlight;

import dev.abruptsteve.centuryrafflehelper.CenturyRaffleHelperMod;
import dev.abruptsteve.centuryrafflehelper.raffle.RaffleLogic;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.scores.PlayerTeam;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class CenturyCakeHighlighter {
    private static final int SERVER_CHECK_INTERVAL_TICKS = 20;

    private static CenturyCakeTeam activeTeam;
    private static boolean allowedOnServer;
    private static int ticksUntilServerCheck;

    private CenturyCakeHighlighter() {
    }

    public static void tick(Minecraft client) {
        if (CenturyRaffleHelperMod.CONFIG == null || !CenturyRaffleHelperMod.CONFIG.general.cakeTeamGlow) {
            activeTeam = null;
            return;
        }
        if (client.level == null || client.player == null) {
            activeTeam = null;
            allowedOnServer = false;
            ticksUntilServerCheck = 0;
            return;
        }

        if (ticksUntilServerCheck-- <= 0) {
            allowedOnServer = RaffleLogic.shouldRunOnCurrentServer();
            ticksUntilServerCheck = SERVER_CHECK_INTERVAL_TICKS;
        }

        activeTeam = allowedOnServer ? heldCakeTeam(client, client.player) : null;
    }

    public static int outlineColorFor(Entity entity, EntityRenderState renderState) {
        int rgb = outlineRgbFor(entity, renderState);
        return rgb == 0 ? 0 : ARGB.opaque(rgb);
    }

    public static int outlineRgbFor(Entity entity) {
        return outlineRgbFor(entity, null);
    }

    public static boolean shouldGlow(Entity entity) {
        return outlineRgbFor(entity) != 0;
    }

    private static int outlineRgbFor(Entity entity, EntityRenderState renderState) {
        boolean highlightAll = CenturyRaffleHelperMod.CONFIG != null
            && CenturyRaffleHelperMod.CONFIG.dev != null
            && CenturyRaffleHelperMod.CONFIG.dev.highlightAllCakeTeams;
        if ((!highlightAll && activeTeam == null) || !(entity instanceof Player player)) {
            return 0;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.player == null || player == client.player) {
            return 0;
        }

        DetectedTeam detectedTeam = renderState == null ? null : teamFromComponent(renderState.nameTag);
        if (detectedTeam == null) {
            detectedTeam = teamFromPlayer(player);
        }
        if (detectedTeam == null || (!highlightAll && detectedTeam.team() != activeTeam)) {
            return 0;
        }

        return detectedTeam.rgb();
    }

    public static List<String> debugReport(Minecraft client) {
        List<String> lines = new ArrayList<>();
        lines.add("Century Cake Glow Debug");
        lines.add("config: cakeTeamGlow=" + safeConfigCakeGlow()
            + ", highlightAllCakeTeams=" + safeConfigHighlightAll()
            + ", debug=" + safeConfigDebug());
        lines.add("server: allowedOnServer=" + allowedOnServer + ", activeTeam=" + teamLabel(activeTeam));

        if (client == null || client.player == null || client.level == null) {
            lines.add("client: missing player or level");
            return lines;
        }

        lines.add("self: " + client.player.getScoreboardName());
        addStackDebug(lines, client, client.player, "mainHand", client.player.getMainHandItem());
        addStackDebug(lines, client, client.player, "offHand", client.player.getOffhandItem());

        List<? extends Player> players = client.level.players().stream()
            .filter(player -> player != client.player)
            .sorted(Comparator.comparingDouble(player -> player.distanceToSqr(client.player)))
            .limit(12)
            .toList();
        lines.add("nearbyPlayers: " + players.size());
        for (Player player : players) {
            DetectedTeam detected = teamFromPlayer(player);
            int outlineRgb = outlineRgbFor(player, null);
            PlayerTeam scoreboardTeam = player.getTeam();
            lines.add("player: name=" + player.getScoreboardName()
                + ", dist=" + Math.round(Math.sqrt(player.distanceToSqr(client.player)))
                + ", detected=" + detectedLabel(detected)
                + ", shouldGlow=" + (outlineRgb != 0)
                + ", outlineRgb=" + hex(outlineRgb)
                + ", teamName=" + (scoreboardTeam == null ? "null" : scoreboardTeam.getName())
                + ", teamColor=" + (scoreboardTeam == null ? "null" : scoreboardTeam.getColor().getName()));
            lines.add("  display=" + componentDebug(player.getDisplayName()));
            if (scoreboardTeam != null) {
                lines.add("  prefix=" + componentDebug(scoreboardTeam.getPlayerPrefix()));
                lines.add("  suffix=" + componentDebug(scoreboardTeam.getPlayerSuffix()));
                lines.add("  formatted=" + componentDebug(PlayerTeam.formatNameForTeam(scoreboardTeam, Component.literal(player.getScoreboardName()))));
            }
        }
        return lines;
    }

    private static CenturyCakeTeam heldCakeTeam(Minecraft client, Player player) {
        CenturyCakeTeam mainHand = CenturyCakeTeam.fromItem(client, player, player.getMainHandItem());
        if (mainHand != null) {
            return mainHand;
        }
        return CenturyCakeTeam.fromItem(client, player, player.getOffhandItem());
    }

    private static DetectedTeam teamFromPlayer(Player player) {
        DetectedTeam scoreboardTeam = teamFromScoreboard(player);
        if (scoreboardTeam != null) {
            return scoreboardTeam;
        }
        return teamFromComponent(player.getDisplayName());
    }

    private static DetectedTeam teamFromScoreboard(Player player) {
        PlayerTeam team = player.getTeam();
        if (team == null) {
            return null;
        }

        DetectedTeam suffixTeam = teamFromComponent(team.getPlayerSuffix());
        if (suffixTeam != null) {
            return suffixTeam;
        }

        DetectedTeam formattedTeam = teamFromComponent(PlayerTeam.formatNameForTeam(team, Component.literal(player.getScoreboardName())));
        if (formattedTeam != null) {
            return formattedTeam;
        }

        return null;
    }

    private static DetectedTeam teamFromComponent(Component component) {
        if (component == null) {
            return null;
        }

        DetectedTeam[] iconTeam = new DetectedTeam[1];
        component.visit((style, text) -> {
            if (!text.isBlank()) {
                DetectedTeam legacyIconTeam = teamFromLegacyIconText(text);
                if (legacyIconTeam != null) {
                    iconTeam[0] = legacyIconTeam;
                } else if (text.indexOf(ChatFormatting.PREFIX_CODE) < 0 && looksLikeTeamIcon(text)) {
                    DetectedTeam detectedTeam = teamFromStyle(style);
                    if (detectedTeam != null) {
                        iconTeam[0] = detectedTeam;
                    }
                }
            }
            return Optional.empty();
        }, Style.EMPTY);
        return iconTeam[0];
    }

    private static DetectedTeam teamFromStyle(Style style) {
        TextColor color = style.getColor();
        if (color == null) {
            return null;
        }

        int rgb = color.getValue();
        CenturyCakeTeam team = CenturyCakeTeam.fromRgb(rgb);
        return team == null ? null : new DetectedTeam(team, rgb);
    }

    private static DetectedTeam teamFromLegacyIconText(String text) {
        ChatFormatting currentColor = null;
        DetectedTeam detectedTeam = null;

        for (int i = 0; i < text.length(); ) {
            int codePoint = text.codePointAt(i);
            if (codePoint == ChatFormatting.PREFIX_CODE && i + 1 < text.length()) {
                ChatFormatting formatting = ChatFormatting.getByCode(text.charAt(i + 1));
                if (formatting == ChatFormatting.RESET) {
                    currentColor = null;
                } else if (formatting != null && formatting.isColor()) {
                    currentColor = formatting;
                }
                i += 2;
                continue;
            }

            if (currentColor != null && isTeamIconCodePoint(codePoint)) {
                CenturyCakeTeam team = CenturyCakeTeam.fromFormatting(currentColor);
                if (team != null) {
                    detectedTeam = new DetectedTeam(team, team.rgb());
                }
            }
            i += Character.charCount(codePoint);
        }

        return detectedTeam;
    }

    private static boolean isTeamIconCodePoint(int codePoint) {
        return codePoint == 0x26c3;
    }

    private static boolean looksLikeTeamIcon(String text) {
        if (text.indexOf('❤') >= 0 || text.indexOf('♥') >= 0) {
            return false;
        }

        for (int i = 0; i < text.length(); ) {
            int codePoint = text.codePointAt(i);
            i += Character.charCount(codePoint);
            if (Character.isWhitespace(codePoint)
                || Character.isLetterOrDigit(codePoint)
                || codePoint == '['
                || codePoint == ']'
                || codePoint == '_'
                || codePoint == '-') {
                continue;
            }
            return true;
        }
        return false;
    }

    private static void addStackDebug(List<String> lines, Minecraft client, Player player, String label, ItemStack stack) {
        lines.add(label + ": empty=" + stack.isEmpty()
            + ", name=" + stack.getHoverName().getString()
            + ", detected=" + teamLabel(CenturyCakeTeam.fromItem(client, player, stack)));
        if (stack.isEmpty()) {
            return;
        }

        Item.TooltipContext context = client.level == null ? Item.TooltipContext.EMPTY : Item.TooltipContext.of(client.level);
        int index = 0;
        for (Component component : stack.getTooltipLines(context, player, TooltipFlag.NORMAL)) {
            lines.add("  tooltip[" + index + "]=" + componentDebug(component));
            index++;
        }
    }

    private static String componentDebug(Component component) {
        if (component == null) {
            return "null";
        }

        StringBuilder builder = new StringBuilder();
        builder.append('"').append(component.getString()).append('"');
        builder.append(" legacyIcon=").append(detectedLabel(teamFromLegacyIconText(component.getString())));
        builder.append(" parts=[");
        boolean[] first = {true};
        component.visit((style, text) -> {
            if (!text.isEmpty()) {
                if (!first[0]) {
                    builder.append(", ");
                }
                first[0] = false;
                builder.append('"').append(text.replace("\n", "\\n")).append('"')
                    .append("@").append(style.getColor() == null ? "no-color" : hex(style.getColor().getValue()));
            }
            return Optional.empty();
        }, Style.EMPTY);
        builder.append(']');
        return builder.toString();
    }

    private static String detectedLabel(DetectedTeam detectedTeam) {
        return detectedTeam == null ? "null" : detectedTeam.team().label() + "@" + hex(detectedTeam.rgb());
    }

    private static String teamLabel(CenturyCakeTeam team) {
        return team == null ? "null" : team.label();
    }

    private static boolean safeConfigCakeGlow() {
        return CenturyRaffleHelperMod.CONFIG != null && CenturyRaffleHelperMod.CONFIG.general.cakeTeamGlow;
    }

    private static boolean safeConfigHighlightAll() {
        return CenturyRaffleHelperMod.CONFIG != null
            && CenturyRaffleHelperMod.CONFIG.dev != null
            && CenturyRaffleHelperMod.CONFIG.dev.highlightAllCakeTeams;
    }

    private static boolean safeConfigDebug() {
        return CenturyRaffleHelperMod.CONFIG != null
            && CenturyRaffleHelperMod.CONFIG.dev != null
            && CenturyRaffleHelperMod.CONFIG.dev.cakeGlowDebug;
    }

    private static String hex(int rgb) {
        return String.format(Locale.ROOT, "#%06x", rgb & 0xffffff);
    }

    private record DetectedTeam(CenturyCakeTeam team, int rgb) {
    }

    private enum CenturyCakeTeam {
        BLUE("blue", ChatFormatting.BLUE, new int[] {0x5555ff, 0x0000aa}, "blueberry cake"),
        YELLOW("yellow", ChatFormatting.YELLOW, new int[] {0xffff55, 0xffaa00}, "lemon cheesecake"),
        GREEN("green", ChatFormatting.GREEN, new int[] {0x55ff55, 0x00aa00}, "matcha cake", "green velvet cake"),
        RED("red", ChatFormatting.RED, new int[] {0xff5555, 0xaa0000}, "red velvet cake"),
        PINK("pink", ChatFormatting.LIGHT_PURPLE, new int[] {0xff55ff, 0xaa00aa}, "strawberry shortcake");

        private final String teamName;
        private final ChatFormatting formatting;
        private final int[] acceptedRgb;
        private final String[] itemNames;
        private final int rgb;

        CenturyCakeTeam(String teamName, ChatFormatting formatting, int[] acceptedRgb, String... itemNames) {
            this.teamName = teamName;
            this.formatting = formatting;
            this.acceptedRgb = acceptedRgb;
            this.itemNames = itemNames;
            this.rgb = formatting.getColor() == null ? 0xffffff : formatting.getColor();
        }

        private int rgb() {
            return rgb;
        }

        private String label() {
            return teamName.toUpperCase(Locale.ROOT);
        }

        private static CenturyCakeTeam fromItem(Minecraft client, Player player, ItemStack stack) {
            if (stack.isEmpty()) {
                return null;
            }

            String name = RaffleLogic.stripFormatting(stack.getHoverName().getString()).toLowerCase(Locale.ROOT);
            CenturyCakeTeam team = fromItemText(name);
            if (team != null) {
                return team;
            }

            Item.TooltipContext context = client.level == null ? Item.TooltipContext.EMPTY : Item.TooltipContext.of(client.level);
            for (Component component : stack.getTooltipLines(context, player, TooltipFlag.NORMAL)) {
                team = fromItemText(RaffleLogic.stripFormatting(component.getString()).toLowerCase(Locale.ROOT));
                if (team != null) {
                    return team;
                }
            }
            return null;
        }

        private static CenturyCakeTeam fromItemText(String text) {
            for (CenturyCakeTeam team : values()) {
                for (String itemName : team.itemNames) {
                    if (text.contains(itemName)) {
                        return team;
                    }
                }
                if (text.contains(team.teamName + " team")) {
                    return team;
                }
            }
            return null;
        }

        private static CenturyCakeTeam fromFormatting(ChatFormatting formatting) {
            if (formatting == null || !formatting.isColor()) {
                return null;
            }

            for (CenturyCakeTeam team : values()) {
                if (team.formatting == formatting) {
                    return team;
                }
            }
            return null;
        }

        private static CenturyCakeTeam fromRgb(int rgb) {
            for (CenturyCakeTeam team : values()) {
                for (int accepted : team.acceptedRgb) {
                    if ((rgb & 0xffffff) == accepted) {
                        return team;
                    }
                }
            }
            return fromApproximateRgb(rgb);
        }

        private static CenturyCakeTeam fromApproximateRgb(int rgb) {
            int red = (rgb >> 16) & 0xff;
            int green = (rgb >> 8) & 0xff;
            int blue = rgb & 0xff;

            if (red >= 120 && green < 125 && blue < 125) {
                return RED;
            }
            if (red >= 165 && green >= 145 && blue < 115) {
                return YELLOW;
            }
            if (green >= 145 && red < 150 && blue < 150) {
                return GREEN;
            }
            if (blue >= 145 && red < 145 && green < 175) {
                return BLUE;
            }
            if (red >= 170 && blue >= 130 && green < 145) {
                return PINK;
            }
            return null;
        }
    }
}
