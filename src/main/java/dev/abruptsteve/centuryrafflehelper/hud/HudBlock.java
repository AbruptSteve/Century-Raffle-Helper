package dev.abruptsteve.centuryrafflehelper.hud;

import dev.abruptsteve.centuryrafflehelper.config.HudPosition;

import java.util.List;

public record HudBlock(String id, String label, HudPosition position, List<HudLine> lines) {
}
