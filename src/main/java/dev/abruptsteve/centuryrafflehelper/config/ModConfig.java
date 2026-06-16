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
        @ConfigOption(name = "Ticket HUD", desc = "Show total raffle tickets.")
        @ConfigEditorBoolean
        public boolean showTicketHud = true;

        @Expose
        @ConfigOption(name = "Cake Eater HUD", desc = "Show how many cake slices can still be eaten today.")
        @ConfigEditorBoolean
        public boolean showCakeHud = true;

        @Expose
        @ConfigOption(name = "Task HUD", desc = "Show incomplete daily raffle tasks.")
        @ConfigEditorBoolean
        public boolean showTaskHud = true;

        @Expose
        public HudPosition timerPosition = new HudPosition(10, 50);

        @Expose
        public HudPosition ticketPosition = new HudPosition(10, 95);

        @Expose
        public HudPosition cakePosition = new HudPosition(10, 115);

        @Expose
        public HudPosition taskPosition = new HudPosition(10, 145);
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
        public boolean showDescriptions = false;

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
}
