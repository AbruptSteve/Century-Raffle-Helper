package dev.abruptsteve.centuryrafflehelper.raffle;

import com.google.gson.annotations.Expose;
import dev.abruptsteve.centuryrafflehelper.CenturyRaffleHelperMod;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class RaffleState {
    public static final int CAKE_SLICES_PER_SPEED_RAFFLE = 25;
    public static final int STARTING_MILESTONE_TICKETS = 3;
    private static final int[] MILESTONE_THRESHOLDS = {1, 10, 25, 50, 100, 200, 400};
    private static final String[] MILESTONE_LABELS = {"I", "II", "III", "IV", "V", "VI", "VII"};

    @Expose
    public boolean ticketsKnown = false;

    @Expose
    public int totalTickets = 0;

    @Expose
    public int milestoneTickets = STARTING_MILESTONE_TICKETS;

    @Expose
    public int cakeSlicesLeft = CAKE_SLICES_PER_SPEED_RAFFLE;

    @Expose
    public long cakeSliceResetEpochMillis = -1L;

    @Expose
    public long eventEndEpochMillis = -1L;

    @Expose
    public Map<RaffleDraw, Long> raffleDrawEpochMillis = new EnumMap<>(RaffleDraw.class);

    @Expose
    public Map<RaffleDraw, Integer> raffleEnteredTickets = new EnumMap<>(RaffleDraw.class);

    @Expose
    public String dailyKey = "";

    @Expose
    public Map<RaffleTask, Boolean> completedTasks = new EnumMap<>(RaffleTask.class);

    @Expose
    public List<ObservedRaffleTask> observedTasks = new ArrayList<>();

    @Expose
    public Map<TaskTier, TaskProgress> taskProgress = new EnumMap<>(TaskTier.class);

    @Expose
    public int totalTasksCompleted = -1;

    @Expose
    public int totalTasksAvailable = -1;

    @Expose
    public long taskResetEpochMillis = -1L;

    public void ensureDailyFresh() {
        String key = currentDailyKey();
        boolean dirty = false;
        dirty |= resetExpiredRaffles();
        dirty |= ensureMilestoneTickets();
        if (!key.equals(dailyKey)) {
            dailyKey = key;
            taskResetEpochMillis = -1L;
            clearTaskState();
            dirty = true;
        } else if (taskResetEpochMillis > 0L && System.currentTimeMillis() > taskResetEpochMillis + 1_000L) {
            taskResetEpochMillis = -1L;
            clearTaskState();
            dirty = true;
        }
        dirty |= ensureCakeSlicesFresh();

        if (dirty) {
            CenturyRaffleHelperMod.CONFIG_MANAGER.saveAll();
        }
    }

    public void resetDaily(boolean save) {
        dailyKey = currentDailyKey();
        cakeSlicesLeft = CAKE_SLICES_PER_SPEED_RAFFLE;
        cakeSliceResetEpochMillis = nextCakeSliceResetEpochMillis();
        taskResetEpochMillis = -1L;
        clearTaskState();
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
        tickets = Math.max(0, tickets);
        boolean dirty = false;
        if (!ticketsKnown || totalTickets != tickets) {
            ticketsKnown = true;
            totalTickets = tickets;
            dirty = true;
        }
        if (milestoneTickets < tickets) {
            milestoneTickets = tickets;
            dirty = true;
        }
        if (dirty) {
            CenturyRaffleHelperMod.CONFIG_MANAGER.saveAll();
        }
    }

    public void addTickets(int tickets) {
        if (tickets <= 0) return;
        boolean dirty = true;
        milestoneTickets = Math.max(STARTING_MILESTONE_TICKETS, milestoneTickets) + tickets;
        if (ticketsKnown) {
            totalTickets += tickets;
        }
        dirty |= addEnteredTickets(RaffleDraw.SPEED, tickets);
        dirty |= addEnteredTickets(RaffleDraw.DAILY, tickets);
        dirty |= addEnteredTickets(RaffleDraw.BIG_ONE, tickets);
        if (dirty) {
            CenturyRaffleHelperMod.CONFIG_MANAGER.saveAll();
        }
    }

    public void consumeCakeSlice() {
        ensureDailyFresh();
        if (cakeSlicesLeft > 0) {
            cakeSlicesLeft--;
            CenturyRaffleHelperMod.CONFIG_MANAGER.saveAll();
        }
    }

    public long cakeSliceResetEpochMillis() {
        if (cakeSliceResetEpochMillis <= 0L) {
            cakeSliceResetEpochMillis = nextCakeSliceResetEpochMillis();
        }
        return cakeSliceResetEpochMillis;
    }

    public void setEventEndFromDuration(long millisLeft) {
        if (millisLeft <= 0) return;
        long newEnd = System.currentTimeMillis() + millisLeft;
        if (Math.abs(eventEndEpochMillis - newEnd) > 1_000L) {
            eventEndEpochMillis = newEnd;
            CenturyRaffleHelperMod.CONFIG_MANAGER.saveAll();
        }
    }

    public void setRaffleDrawFromDuration(RaffleDraw raffle, long millisLeft) {
        if (raffle == null || millisLeft <= 0) return;
        long newEnd = System.currentTimeMillis() + millisLeft;
        long oldEnd = raffleDrawEpochMillis.getOrDefault(raffle, -1L);
        if (Math.abs(oldEnd - newEnd) > 1_000L) {
            raffleDrawEpochMillis.put(raffle, newEnd);
            CenturyRaffleHelperMod.CONFIG_MANAGER.saveAll();
        }
    }

    public void setRaffleEnteredTickets(RaffleDraw raffle, int tickets) {
        if (raffle == null) return;
        tickets = Math.max(0, tickets);
        int oldTickets = raffleEnteredTickets.getOrDefault(raffle, -1);
        if (oldTickets != tickets) {
            raffleEnteredTickets.put(raffle, tickets);
            CenturyRaffleHelperMod.CONFIG_MANAGER.saveAll();
        }
    }

    public long raffleDrawEpochMillis(RaffleDraw raffle) {
        if (raffle == null) return -1L;
        if (raffle == RaffleDraw.BIG_ONE) {
            long drawEnd = raffleDrawEpochMillis.getOrDefault(raffle, -1L);
            return drawEnd > 0L ? drawEnd : eventEndEpochMillis;
        }
        return raffleDrawEpochMillis.getOrDefault(raffle, -1L);
    }

    public int raffleEnteredTickets(RaffleDraw raffle) {
        if (raffle == null) return -1;
        long end = raffleDrawEpochMillis(raffle);
        if (end > 0L && System.currentTimeMillis() > end) {
            return 0;
        }
        return raffleEnteredTickets.getOrDefault(raffle, -1);
    }

    public String milestoneLabel() {
        return MILESTONE_LABELS[milestoneIndex()];
    }

    public int milestoneTarget() {
        return MILESTONE_THRESHOLDS[milestoneIndex()];
    }

    public boolean allMilestonesReached() {
        return milestoneTickets >= MILESTONE_THRESHOLDS[MILESTONE_THRESHOLDS.length - 1];
    }

    public void setTaskResetFromDuration(long millisLeft) {
        if (millisLeft <= 0) return;
        long newEnd = System.currentTimeMillis() + millisLeft;
        if (Math.abs(taskResetEpochMillis - newEnd) > 1_000L) {
            taskResetEpochMillis = newEnd;
            CenturyRaffleHelperMod.CONFIG_MANAGER.saveAll();
        }
    }

    public void setTaskProgress(TaskTier tier, int completed, int total) {
        if (tier == null) return;
        TaskProgress old = taskProgress.get(tier);
        TaskProgress progress = new TaskProgress(Math.max(0, completed), Math.max(0, total));
        if (!progress.equals(old)) {
            taskProgress.put(tier, progress);
            CenturyRaffleHelperMod.CONFIG_MANAGER.saveAll();
        }
    }

    public void setTotalTaskProgress(int completed, int total) {
        completed = Math.max(0, completed);
        total = Math.max(0, total);
        if (totalTasksCompleted != completed || totalTasksAvailable != total) {
            totalTasksCompleted = completed;
            totalTasksAvailable = total;
            CenturyRaffleHelperMod.CONFIG_MANAGER.saveAll();
        }
    }

    public void replaceObservedTasks(List<ObservedRaffleTask> tasks) {
        List<ObservedRaffleTask> cleaned = new ArrayList<>();
        for (ObservedRaffleTask task : tasks) {
            if (task == null || task.title == null || task.title.isBlank()) continue;
            ObservedRaffleTask copy = new ObservedRaffleTask(task.tier, task.title, task.description, task.complete);
            boolean duplicate = cleaned.stream().anyMatch(existing -> existing.key().equals(copy.key()));
            if (!duplicate) {
                cleaned.add(copy);
            }
        }

        if (!cleaned.equals(observedTasks)) {
            observedTasks = cleaned;
            CenturyRaffleHelperMod.CONFIG_MANAGER.saveAll();
        }
    }

    public void setObservedTaskCompleteFromText(String text, boolean complete) {
        boolean dirty = false;
        for (ObservedRaffleTask task : observedTasks) {
            if (task.isMentionedIn(text) && task.complete != complete) {
                task.complete = complete;
                dirty = true;
            }
        }
        if (dirty) {
            CenturyRaffleHelperMod.CONFIG_MANAGER.saveAll();
        }
    }

    public String currentDailyKey() {
        int resetHour = CenturyRaffleHelperMod.CONFIG.general.resetHourUtc;
        ZonedDateTime now = Instant.now().atZone(ZoneOffset.UTC);
        LocalDate date = now.getHour() < resetHour ? now.toLocalDate().minusDays(1) : now.toLocalDate();
        return date.toString();
    }

    private void clearTaskState() {
        completedTasks.clear();
        observedTasks.clear();
        taskProgress.clear();
        totalTasksCompleted = -1;
        totalTasksAvailable = -1;
    }

    private boolean addEnteredTickets(RaffleDraw raffle, int tickets) {
        int oldTickets = raffleEnteredTickets.getOrDefault(raffle, -1);
        if (oldTickets >= 0) {
            raffleEnteredTickets.put(raffle, oldTickets + tickets);
            return true;
        }
        return false;
    }

    private boolean resetExpiredRaffles() {
        boolean dirty = false;
        long now = System.currentTimeMillis();
        for (RaffleDraw raffle : RaffleDraw.values()) {
            long end = raffleDrawEpochMillis.getOrDefault(raffle, -1L);
            if (end > 0L && now > end + 1_000L) {
                long period = rafflePeriodMillis(raffle);
                if (period > 0L) {
                    while (now > end + 1_000L) {
                        end += period;
                    }
                    raffleDrawEpochMillis.put(raffle, end);
                } else {
                    raffleDrawEpochMillis.put(raffle, -1L);
                }
                raffleEnteredTickets.put(raffle, 0);
                dirty = true;
            }
        }
        return dirty;
    }

    private boolean ensureMilestoneTickets() {
        int syncedTickets = Math.max(STARTING_MILESTONE_TICKETS, milestoneTickets);
        if (ticketsKnown) {
            syncedTickets = Math.max(syncedTickets, totalTickets);
        }
        if (syncedTickets != milestoneTickets) {
            milestoneTickets = syncedTickets;
            return true;
        }
        return false;
    }

    private int milestoneIndex() {
        int tickets = Math.max(STARTING_MILESTONE_TICKETS, milestoneTickets);
        for (int i = 0; i < MILESTONE_THRESHOLDS.length; i++) {
            if (tickets < MILESTONE_THRESHOLDS[i]) {
                return i;
            }
        }
        return MILESTONE_THRESHOLDS.length - 1;
    }

    private boolean ensureCakeSlicesFresh() {
        boolean dirty = false;
        if (cakeSlicesLeft > CAKE_SLICES_PER_SPEED_RAFFLE) {
            cakeSlicesLeft = CAKE_SLICES_PER_SPEED_RAFFLE;
            dirty = true;
        }

        long now = System.currentTimeMillis();
        if (cakeSliceResetEpochMillis <= 0L) {
            cakeSliceResetEpochMillis = nextCakeSliceResetEpochMillis();
            return true;
        }

        long knownSpeedDraw = raffleDrawEpochMillis(RaffleDraw.SPEED);
        if (knownSpeedDraw > now && Math.abs(cakeSliceResetEpochMillis - knownSpeedDraw) > 1_000L) {
            cakeSliceResetEpochMillis = knownSpeedDraw;
            dirty = true;
        }

        long period = rafflePeriodMillis(RaffleDraw.SPEED);
        if (now > cakeSliceResetEpochMillis + 1_000L) {
            while (now > cakeSliceResetEpochMillis + 1_000L) {
                cakeSliceResetEpochMillis += period;
            }
            cakeSlicesLeft = CAKE_SLICES_PER_SPEED_RAFFLE;
            dirty = true;
        }
        return dirty;
    }

    private long nextCakeSliceResetEpochMillis() {
        long speedDraw = raffleDrawEpochMillis(RaffleDraw.SPEED);
        if (speedDraw > System.currentTimeMillis()) {
            return speedDraw;
        }
        return System.currentTimeMillis() + RaffleLogic.millisUntilNextSpeedRaffle();
    }

    private long rafflePeriodMillis(RaffleDraw raffle) {
        return switch (raffle) {
            case SPEED -> 2L * 60L * 60L * 1000L;
            case DAILY -> 24L * 60L * 60L * 1000L;
            case BIG_ONE -> 0L;
        };
    }
}
