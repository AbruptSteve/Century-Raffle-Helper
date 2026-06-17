package dev.abruptsteve.centuryrafflehelper.raffle;

public enum RaffleDraw {
    SPEED("Speed"),
    DAILY("Daily"),
    BIG_ONE("The Big One");

    public final String hudLabel;

    RaffleDraw(String hudLabel) {
        this.hudLabel = hudLabel;
    }
}
