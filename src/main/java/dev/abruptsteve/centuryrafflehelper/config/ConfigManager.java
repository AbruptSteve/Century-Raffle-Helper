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

public class ConfigManager {
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
        return load(configPath, ModConfig.class, new ModConfig());
    }

    public RaffleState loadState() {
        RaffleState state = load(statePath, RaffleState.class, new RaffleState());
        if (state.completedTasks == null) {
            state.completedTasks = new java.util.EnumMap<>(dev.abruptsteve.centuryrafflehelper.raffle.RaffleTask.class);
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
