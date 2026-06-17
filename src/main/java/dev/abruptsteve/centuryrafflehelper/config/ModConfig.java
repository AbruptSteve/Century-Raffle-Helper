package dev.abruptsteve.centuryrafflehelper.config;

import com.google.gson.annotations.Expose;
import dev.abruptsteve.centuryrafflehelper.CenturyRaffleHelperMod;
import io.github.notenoughupdates.moulconfig.Config;
import io.github.notenoughupdates.moulconfig.annotations.Category;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;
import io.github.notenoughupdates.moulconfig.common.text.StructuredText;

public class ModConfig extends Config {
    @Expose
    public int configVersion = 0;

    @Expose
    @Category(name = "General", desc = "General Century Raffle Helper settings.")
    public General general = new General();

    @Expose
    @Category(name = "HUD", desc = "Enable and disable HUD elements.")
    public Hud hud = new Hud();

    @Expose
    @Category(name = "Timers", desc = "Choose which raffle timers appear.")
    public Timers timers = new Timers();

    @Expose
    @Category(name = "Tasks", desc = "Daily easy, medium, and hard task HUD settings.")
    public Tasks tasks = new Tasks();

    @Expose
    @Category(name = "Dev", desc = "Debug tools for testing Century Raffle Helper features.")
    public Dev dev = new Dev();

    @Override
    public StructuredText getTitle() {
        return StructuredText.of("Century Raffle Helper by abruptsteve");
    }

    @Override
    public void saveNow() {
        CenturyRaffleHelperMod.CONFIG_MANAGER.saveAll();
    }

    public static class General {
        @Expose
        @ConfigOption(name = "Only On Hypixel", desc = "Only render HUDs while connected to Hypixel.")
        @ConfigEditorBoolean
        public boolean onlyOnHypixel = true;

        @Expose
        @ConfigOption(name = "Launch Warning", desc = "Print the raffle menu recommendation once after joining a world each launch.")
        @ConfigEditorBoolean
        public boolean launchWarning = true;

        @Expose
        @ConfigOption(name = "Cake Team Glow", desc = "Glow-highlight players on the team matching your held Slice Of Century Cake.")
        @ConfigEditorBoolean
        public boolean cakeTeamGlow = true;

        @Expose
        @ConfigOption(name = "Reset Hour UTC", desc = "Hypixel daily reset hour in UTC. Default is 0.")
        @ConfigEditorSlider(minValue = 0.0f, maxValue = 23.0f, minStep = 1.0f)
        public int resetHourUtc = 0;

        @ConfigOption(name = "Open HUD Editor", desc = "Move the Century Raffle Helper HUD elements.")
        @ConfigEditorButton(buttonText = "Open")
        public transient Runnable openHudEditor = CenturyRaffleHelperMod::openHudEditor;

        @ConfigOption(name = "Reset Today", desc = "Reset today's cake and task counters.")
        @ConfigEditorButton(buttonText = "Reset")
        public transient Runnable resetToday = () -> CenturyRaffleHelperMod.STATE.resetDaily(true);
    }

    public static class Hud {
        @Expose
        @ConfigOption(name = "Timer HUD", desc = "Show raffle loop and event-conclusion timers.")
        @ConfigEditorBoolean
        public boolean showTimerHud = true;

        @Expose
        @ConfigOption(name = "Cake Eater HUD", desc = "Show how many cake slices can still be eaten today.")
        @ConfigEditorBoolean
        public boolean showCakeHud = true;

        @Expose
        @ConfigOption(name = "Task HUD", desc = "Show incomplete daily raffle tasks.")
        @ConfigEditorBoolean
        public boolean showTaskHud = true;

        @Expose
        @ConfigOption(name = "Milestone Tracker HUD", desc = "Show raffle milestone progress from gained tickets.")
        @ConfigEditorBoolean
        public boolean showMilestoneHud = true;

        @Expose
        public HudPosition timerPosition = new HudPosition(799, 43, 0.8f);

        @Expose
        public HudPosition cakePosition = new HudPosition(862, 24, 0.8f);

        @Expose
        public HudPosition taskPosition = new HudPosition(9, 11, 0.9f);

        @Expose
        public HudPosition milestonePosition = new HudPosition(25, 300, 1.0f);
    }

    public static class Timers {
        @Expose
        @ConfigOption(name = "Speed Raffle", desc = "Show the repeating 2 hour Speed Raffle timer.")
        @ConfigEditorBoolean
        public boolean speedRaffle = true;

        @Expose
        @ConfigOption(name = "Daily Raffle", desc = "Show the repeating daily raffle timer.")
        @ConfigEditorBoolean
        public boolean dailyRaffle = true;

        @Expose
        @ConfigOption(name = "The Big One", desc = "Show the raffle event conclusion timer when known.")
        @ConfigEditorBoolean
        public boolean bigOne = true;

        @Expose
        @ConfigOption(name = "Warn Unknown End", desc = "Show a reminder to open the raffle menu/sidebar if the conclusion timer is unknown.")
        @ConfigEditorBoolean
        public boolean warnUnknownConclusion = true;
    }

    public static class Tasks {
        @Expose
        @ConfigOption(name = "Show Descriptions", desc = "Show task descriptions instead of task names.")
        @ConfigEditorBoolean
        public boolean showDescriptions = true;

        @Expose
        @ConfigOption(name = "Time Until Reset", desc = "Show the task reset timer gathered from the raffle box menu.")
        @ConfigEditorBoolean
        public boolean showTimeUntilReset = true;

        @Expose
        @ConfigOption(name = "Visible Tasks Per Tier", desc = "Maximum incomplete tasks shown under each tier.")
        @ConfigEditorSlider(minValue = 1.0f, maxValue = 7.0f, minStep = 1.0f)
        public int visibleTasksPerTier = 7;

        @Expose
        @ConfigOption(name = "Easy Tasks", desc = "Show incomplete easy tasks.")
        @ConfigEditorBoolean
        public boolean showEasy = true;

        @Expose
        @ConfigOption(name = "Medium Tasks", desc = "Show incomplete medium tasks.")
        @ConfigEditorBoolean
        public boolean showMedium = true;

        @Expose
        @ConfigOption(name = "Hard Tasks", desc = "Show incomplete hard tasks.")
        @ConfigEditorBoolean
        public boolean showHard = true;

        @Expose
        @ConfigOption(name = "Completed Summary", desc = "Show a small completed count when all visible tasks are done.")
        @ConfigEditorBoolean
        public boolean showCompletedSummary = true;
    }

    public static class Dev {
        @Expose
        @ConfigOption(name = "Highlight All Cake Teams", desc = "Highlight every detected cake team member without holding a cake slice.")
        @ConfigEditorBoolean
        public boolean highlightAllCakeTeams = false;

        @Expose
        @ConfigOption(name = "Cake Glow Debug", desc = "Enable extra cake glow debug behavior and command output.")
        @ConfigEditorBoolean
        public boolean cakeGlowDebug = false;
    }
}
