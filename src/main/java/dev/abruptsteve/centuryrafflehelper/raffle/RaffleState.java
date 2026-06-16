package dev.abruptsteve.centuryrafflehelper.raffle;

import com.google.gson.annotations.Expose;
import dev.abruptsteve.centuryrafflehelper.CenturyRaffleHelperMod;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.EnumMap;
import java.util.Map;

public class RaffleState {
    @Expose
    public boolean ticketsKnown = false;

    @Expose
    public int totalTickets = 0;

    @Expose
    public int cakeSlicesLeft = 50;

    @Expose
    public long eventEndEpochMillis = -1L;

    @Expose
    public String dailyKey = "";

    @Expose
    public Map<RaffleTask, Boolean> completedTasks = new EnumMap<>(RaffleTask.class);

    public void ensureDailyFresh() {
        String key = currentDailyKey();
        if (!key.equals(dailyKey)) {
            dailyKey = key;
            cakeSlicesLeft = 50;
            completedTasks.clear();
            CenturyRaffleHelperMod.CONFIG_MANAGER.saveAll();
        }
    }

    public void resetDaily(boolean save) {
        dailyKey = currentDailyKey();
        cakeSlicesLeft = 50;
        completedTasks.clear();
        if (save) {
            CenturyRaffleHelperMod.CONFIG_MANAGER.saveAll();
        }
    }

    public boolean isComplete(RaffleTask task) {
        return completedTasks.getOrDefault(task, false);
    }

    public void setComplete(RaffleTask task, boolean complete) {
        Boolean old = completedTasks.put(task, complete);
        if (old == null || old != complete) {
            CenturyRaffleHelperMod.CONFIG_MANAGER.saveAll();
        }
    }

    public void setTickets(int tickets) {
        if (!ticketsKnown || totalTickets != tickets) {
            ticketsKnown = true;
            totalTickets = Math.max(0, tickets);
            CenturyRaffleHelperMod.CONFIG_MANAGER.saveAll();
        }
    }

    public void addTickets(int tickets) {
        if (!ticketsKnown || tickets <= 0) return;
        totalTickets += tickets;
        CenturyRaffleHelperMod.CONFIG_MANAGER.saveAll();
    }

    public void consumeCakeSlice() {
        ensureDailyFresh();
        if (cakeSlicesLeft > 0) {
            cakeSlicesLeft--;
            CenturyRaffleHelperMod.CONFIG_MANAGER.saveAll();
        }
    }

    public void setEventEndFromDuration(long millisLeft) {
        if (millisLeft <= 0) return;
        long newEnd = System.currentTimeMillis() + millisLeft;
        if (Math.abs(eventEndEpochMillis - newEnd) > 1_000L) {
            eventEndEpochMillis = newEnd;
            CenturyRaffleHelperMod.CONFIG_MANAGER.saveAll();
        }
    }

    public String currentDailyKey() {
        int resetHour = CenturyRaffleHelperMod.CONFIG.general.resetHourUtc;
        ZonedDateTime now = Instant.now().atZone(ZoneOffset.UTC);
        LocalDate date = now.getHour() < resetHour ? now.toLocalDate().minusDays(1) : now.toLocalDate();
        return date.toString();
    }
}
