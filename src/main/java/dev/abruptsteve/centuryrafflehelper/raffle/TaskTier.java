package dev.abruptsteve.centuryrafflehelper.raffle;

public enum TaskTier {
    EASY("Easy", 0x55ff55),
    MEDIUM("Medium", 0xffaa00),
    HARD("Hard", 0xff5555);

    public final String label;
    public final int color;

    TaskTier(String label, int color) {
        this.label = label;
        this.color = color;
    }
}
