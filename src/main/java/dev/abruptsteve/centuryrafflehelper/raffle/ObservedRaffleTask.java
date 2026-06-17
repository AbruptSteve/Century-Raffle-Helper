package dev.abruptsteve.centuryrafflehelper.raffle;

import com.google.gson.annotations.Expose;

import java.util.Objects;

public class ObservedRaffleTask {
    @Expose
    public TaskTier tier = TaskTier.EASY;

    @Expose
    public String title = "";

    @Expose
    public String description = "";

    @Expose
    public boolean complete = false;

    public ObservedRaffleTask() {
    }

    public ObservedRaffleTask(TaskTier tier, String title, String description, boolean complete) {
        this.tier = tier == null ? TaskTier.EASY : tier;
        this.title = clean(title);
        this.description = clean(description);
        this.complete = complete;
    }

    public String key() {
        return tier.name() + ":" + RaffleTask.normalize(title);
    }

    public boolean isMentionedIn(String text) {
        String normalized = RaffleTask.normalize(text);
        String normalizedTitle = RaffleTask.normalize(title);
        String normalizedDescription = RaffleTask.normalize(description);
        return !normalizedTitle.isBlank() && normalized.contains(normalizedTitle)
            || !normalizedDescription.isBlank() && normalized.contains(normalizedDescription);
    }

    private static String clean(String text) {
        return text == null ? "" : text.trim();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ObservedRaffleTask task)) return false;
        return complete == task.complete
            && tier == task.tier
            && Objects.equals(title, task.title)
            && Objects.equals(description, task.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tier, title, description, complete);
    }
}
