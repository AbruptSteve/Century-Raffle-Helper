package dev.abruptsteve.centuryrafflehelper.raffle;

import com.google.gson.annotations.Expose;

import java.util.Objects;

public class TaskProgress {
    @Expose
    public int completed = -1;

    @Expose
    public int total = -1;

    public TaskProgress() {
    }

    public TaskProgress(int completed, int total) {
        this.completed = completed;
        this.total = total;
    }

    public boolean isKnown() {
        return completed >= 0 && total >= 0;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof TaskProgress progress)) return false;
        return completed == progress.completed && total == progress.total;
    }

    @Override
    public int hashCode() {
        return Objects.hash(completed, total);
    }
}
