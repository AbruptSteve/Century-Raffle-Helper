package dev.abruptsteve.centuryrafflehelper;

import com.mojang.brigadier.CommandDispatcher;
import dev.abruptsteve.centuryrafflehelper.config.ConfigManager;
import dev.abruptsteve.centuryrafflehelper.config.ModConfig;
import dev.abruptsteve.centuryrafflehelper.highlight.CenturyCakeHighlighter;
import dev.abruptsteve.centuryrafflehelper.hud.HudEditorScreen;
import dev.abruptsteve.centuryrafflehelper.hud.RaffleHudRenderer;
import dev.abruptsteve.centuryrafflehelper.raffle.RaffleLogic;
import dev.abruptsteve.centuryrafflehelper.raffle.RaffleState;
import dev.abruptsteve.centuryrafflehelper.update.UpdateManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

public class CenturyRaffleHelperMod implements ClientModInitializer {
    public static final String MOD_ID = "centuryrafflehelper";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final String CHAT_PREFIX = "[CRH] ";
    public static final ConfigManager CONFIG_MANAGER = new ConfigManager();
    public static ModConfig CONFIG;
    public static RaffleState STATE;

    private static boolean launchMessageShown = false;
    private static int tickCounter = 0;
    private static Supplier<Screen> queuedScreen;
    private static int queuedScreenTicks;

    @Override
    public void onInitializeClient() {
        CONFIG = CONFIG_MANAGER.loadConfig();
        STATE = CONFIG_MANAGER.loadState();
        STATE.ensureDailyFresh();
        CONFIG_MANAGER.initProcessor(CONFIG);
        CONFIG_MANAGER.saveAll();
        UpdateManager.init();

        RaffleHudRenderer.register();
        registerCommands();
        registerEvents();
    }

    public static void openHudEditor() {
        queueScreen(HudEditorScreen::new);
    }

    public static void queueScreen(Supplier<Screen> screenFactory) {
        queuedScreen = screenFactory;
        queuedScreenTicks = 1;
    }

    private static void registerEvents() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (!launchMessageShown && CONFIG.general.launchWarning && client.player != null) {
                launchMessageShown = true;
                client.player.sendSystemMessage(chatMessage(
                    "To prevent false info and mismatched information please open the raffle menu at least once upon launching to ensure all displayed information is correct!"
                ));
            }
        });

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> RaffleLogic.handleChat(message.getString(), true));
        ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) ->
            RaffleLogic.handleChat(message.getString(), false)
        );

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            tickCounter++;
            openQueuedScreen(client);
            CenturyCakeHighlighter.tick(client);
            UpdateManager.onClientTick(client);
            if (tickCounter % 20 == 0) {
                RaffleLogic.tick(client);
            }
            if (tickCounter % 600 == 0) {
                CONFIG_MANAGER.saveAll();
            }
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> UpdateManager.onClientStopping());
    }

    private static void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register(CenturyRaffleHelperMod::registerCommandTree);
    }

    private static void registerCommandTree(
        CommandDispatcher<FabricClientCommandSource> dispatcher,
        CommandBuildContext registryAccess
    ) {
        dispatcher.register(ClientCommands.literal("crh")
            .executes(context -> {
                CONFIG_MANAGER.openConfigScreen();
                return 1;
            })
            .then(ClientCommands.literal("config")
                .executes(context -> {
                    CONFIG_MANAGER.openConfigScreen();
                    return 1;
                }))
            .then(ClientCommands.literal("hud")
                .executes(context -> {
                    openHudEditor();
                    return 1;
                }))
            .then(ClientCommands.literal("gui")
                .executes(context -> {
                    openHudEditor();
                    return 1;
                }))
            .then(ClientCommands.literal("edit")
                .executes(context -> {
                    openHudEditor();
                    return 1;
                }))
            .then(ClientCommands.literal("reset")
                .executes(context -> {
                    STATE.resetDaily(true);
                    context.getSource().sendFeedback(chatMessage("Century Raffle Helper daily state reset."));
                    return 1;
                }))
            .then(ClientCommands.literal("update")
                .executes(context -> {
                    UpdateManager.sendStatus(context.getSource());
                    return 1;
                })
                .then(ClientCommands.literal("status")
                    .executes(context -> {
                        UpdateManager.sendStatus(context.getSource());
                        return 1;
                    }))
                .then(ClientCommands.literal("on")
                    .executes(context -> {
                        UpdateManager.enable(context.getSource());
                        return 1;
                    }))
                .then(ClientCommands.literal("off")
                    .executes(context -> {
                        context.getSource().sendFeedback(chatMessage("Updater turned off."));
                        return 1;
                    })))
            .then(ClientCommands.literal("cakedebug")
                .executes(context -> {
                    sendCakeDebug(context.getSource());
                    return 1;
                }))
        );
    }

    private static Component chatMessage(String message) {
        return Component.literal(CHAT_PREFIX + message);
    }

    private static void sendCakeDebug(FabricClientCommandSource source) {
        var lines = CenturyCakeHighlighter.debugReport(Minecraft.getInstance());
        LOGGER.info("{}", String.join(System.lineSeparator(), lines));
        source.sendFeedback(chatMessage("Cake glow debug dumped to latest.log. Showing the first lines here:"));
        int shown = Math.min(lines.size(), 12);
        for (int i = 0; i < shown; i++) {
            source.sendFeedback(Component.literal(lines.get(i)));
        }
        if (lines.size() > shown) {
            source.sendFeedback(chatMessage("More lines are in .minecraft/logs/latest.log; search for \"Century Cake Glow Debug\"."));
        }
    }

    private static void openQueuedScreen(Minecraft client) {
        if (queuedScreen == null) {
            return;
        }
        if (queuedScreenTicks > 0) {
            queuedScreenTicks--;
            return;
        }

        Screen screen = queuedScreen.get();
        queuedScreen = null;
        client.setScreen(screen);
    }
}
