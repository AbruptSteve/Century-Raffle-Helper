package dev.abruptsteve.centuryrafflehelper.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.abruptsteve.centuryrafflehelper.CenturyRaffleHelperMod;
import dev.abruptsteve.centuryrafflehelper.raffle.RaffleState;
import io.github.notenoughupdates.moulconfig.gui.GuiContext;
import io.github.notenoughupdates.moulconfig.gui.GuiElementComponent;
import io.github.notenoughupdates.moulconfig.gui.MoulConfigEditor;
import io.github.notenoughupdates.moulconfig.platform.MoulConfigScreenComponent;
import io.github.notenoughupdates.moulconfig.processor.BuiltinMoulConfigGuis;
import io.github.notenoughupdates.moulconfig.processor.ConfigProcessorDriver;
import io.github.notenoughupdates.moulconfig.processor.MoulConfigProcessor;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Map;

public class ConfigManager {
    private static final int CURRENT_CONFIG_VERSION = 3;
    private static final float DEFAULT_POSITION_SCALE = 1.0f;
    private static final Gson GSON = new GsonBuilder()
        .excludeFieldsWithoutExposeAnnotation()
        .enableComplexMapKeySerialization()
        .setPrettyPrinting()
        .create();

    private final Path configPath;
    private final Path statePath;

    private MoulConfigProcessor<ModConfig> processor;
    private MoulConfigEditor<ModConfig> editor;

    public ConfigManager() {
        Path dir = FabricLoader.getInstance().getConfigDir().resolve("centuryrafflehelper");
        this.configPath = dir.resolve("config.json");
        this.statePath = dir.resolve("state.json");
    }

    public ModConfig loadConfig() {
        ModConfig config = load(configPath, ModConfig.class, new ModConfig());
        repairConfig(config);
        migrateConfig(config);
        return config;
    }

    public RaffleState loadState() {
        RaffleState state = load(statePath, RaffleState.class, new RaffleState());
        if (state.completedTasks == null) {
            state.completedTasks = new java.util.EnumMap<>(dev.abruptsteve.centuryrafflehelper.raffle.RaffleTask.class);
        }
        if (state.observedTasks == null) {
            state.observedTasks = new ArrayList<>();
        }
        if (state.raffleDrawEpochMillis == null) {
            state.raffleDrawEpochMillis = new EnumMap<>(dev.abruptsteve.centuryrafflehelper.raffle.RaffleDraw.class);
        } else if (!(state.raffleDrawEpochMillis instanceof EnumMap)) {
            Map<dev.abruptsteve.centuryrafflehelper.raffle.RaffleDraw, Long> loaded = state.raffleDrawEpochMillis;
            state.raffleDrawEpochMillis = new EnumMap<>(dev.abruptsteve.centuryrafflehelper.raffle.RaffleDraw.class);
            state.raffleDrawEpochMillis.putAll(loaded);
        }
        if (state.raffleEnteredTickets == null) {
            state.raffleEnteredTickets = new EnumMap<>(dev.abruptsteve.centuryrafflehelper.raffle.RaffleDraw.class);
        } else if (!(state.raffleEnteredTickets instanceof EnumMap)) {
            Map<dev.abruptsteve.centuryrafflehelper.raffle.RaffleDraw, Integer> loaded = state.raffleEnteredTickets;
            state.raffleEnteredTickets = new EnumMap<>(dev.abruptsteve.centuryrafflehelper.raffle.RaffleDraw.class);
            state.raffleEnteredTickets.putAll(loaded);
        }
        if (state.taskProgress == null) {
            state.taskProgress = new EnumMap<>(dev.abruptsteve.centuryrafflehelper.raffle.TaskTier.class);
        } else if (!(state.taskProgress instanceof EnumMap)) {
            Map<dev.abruptsteve.centuryrafflehelper.raffle.TaskTier, dev.abruptsteve.centuryrafflehelper.raffle.TaskProgress> loaded = state.taskProgress;
            state.taskProgress = new EnumMap<>(dev.abruptsteve.centuryrafflehelper.raffle.TaskTier.class);
            state.taskProgress.putAll(loaded);
        }
        if (state.dailyKey == null || state.dailyKey.isBlank()) {
            state.dailyKey = state.currentDailyKey();
        }
        return state;
    }

    public void initProcessor(ModConfig config) {
        editor = null;
        processor = new MoulConfigProcessor<>(config);
        BuiltinMoulConfigGuis.addProcessors(processor);
        ConfigProcessorDriver driver = new ConfigProcessorDriver(processor);
        driver.warnForPrivateFields = false;
        driver.processConfig(config);
    }

    public MoulConfigEditor<ModConfig> getEditor() {
        if (editor == null) {
            editor = new MoulConfigEditor<>(processor);
        }
        return editor;
    }

    public void openConfigScreen() {
        openConfigScreen(null);
    }

    public void openConfigScreen(String search) {
        CenturyRaffleHelperMod.queueScreen(() -> createConfigScreen(Minecraft.getInstance().screen, search));
    }

    public Screen createConfigScreen(Screen parent, String search) {
        MoulConfigEditor<ModConfig> editor = getEditor();
        editor.search(search == null ? "" : search);
        return new MoulConfigScreenComponent(
            Component.empty(),
            new GuiContext(new GuiElementComponent(editor)),
            parent
        );
    }

    public void saveAll() {
        save(configPath, dev.abruptsteve.centuryrafflehelper.CenturyRaffleHelperMod.CONFIG);
        save(statePath, dev.abruptsteve.centuryrafflehelper.CenturyRaffleHelperMod.STATE);
    }

    private <T> T load(Path path, Class<T> type, T fallback) {
        if (!Files.exists(path)) {
            return fallback;
        }
        try (Reader reader = Files.newBufferedReader(path)) {
            T loaded = GSON.fromJson(reader, type);
            return loaded == null ? fallback : loaded;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private void repairConfig(ModConfig config) {
        if (config.general == null) {
            config.general = new ModConfig.General();
        }
        if (config.hud == null) {
            config.hud = new ModConfig.Hud();
        }
        if (config.timers == null) {
            config.timers = new ModConfig.Timers();
        }
        if (config.tasks == null) {
            config.tasks = new ModConfig.Tasks();
        }
        if (config.dev == null) {
            config.dev = new ModConfig.Dev();
        }
        if (config.hud.timerPosition == null) {
            config.hud.timerPosition = new HudPosition(799, 43, 0.8f);
        }
        if (config.hud.cakePosition == null) {
            config.hud.cakePosition = new HudPosition(862, 24, 0.8f);
        }
        if (config.hud.taskPosition == null) {
            config.hud.taskPosition = new HudPosition(9, 11, 0.9f);
        }
        if (config.hud.milestonePosition == null) {
            config.hud.milestonePosition = new HudPosition(25, 300, 1.0f);
        }
    }

    private void migrateConfig(ModConfig config) {
        if (config.configVersion >= CURRENT_CONFIG_VERSION) {
            return;
        }

        if (isPosition(config.hud.timerPosition, 10, 50, DEFAULT_POSITION_SCALE)) {
            config.hud.timerPosition = new HudPosition(799, 43, 0.8f);
        }
        if (isPosition(config.hud.cakePosition, 10, 115, DEFAULT_POSITION_SCALE)) {
            config.hud.cakePosition = new HudPosition(862, 24, 0.8f);
        }
        if (isPosition(config.hud.taskPosition, 10, 145, DEFAULT_POSITION_SCALE)) {
            config.hud.taskPosition = new HudPosition(9, 11, 0.9f);
        }
        if (isPosition(config.hud.milestonePosition, 10, 210, DEFAULT_POSITION_SCALE)) {
            config.hud.milestonePosition = new HudPosition(25, 300, 1.0f);
        }
        if (config.tasks.visibleTasksPerTier == 3) {
            config.tasks.visibleTasksPerTier = 7;
        }
        config.tasks.showDescriptions = true;
        config.configVersion = CURRENT_CONFIG_VERSION;
    }

    private boolean isPosition(HudPosition position, int x, int y, float scale) {
        return position != null
            && position.x == x
            && position.y == y
            && Math.abs(position.scale - scale) < 0.001f;
    }

    private void save(Path path, Object data) {
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(data, writer);
            }
        } catch (IOException ignored) {
        }
    }
}
