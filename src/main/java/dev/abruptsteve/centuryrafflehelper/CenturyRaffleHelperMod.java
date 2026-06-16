package dev.abruptsteve.centuryrafflehelper;

import com.mojang.brigadier.CommandDispatcher;
import dev.abruptsteve.centuryrafflehelper.config.ConfigManager;
import dev.abruptsteve.centuryrafflehelper.config.ModConfig;
import dev.abruptsteve.centuryrafflehelper.hud.HudEditorScreen;
import dev.abruptsteve.centuryrafflehelper.hud.RaffleHudRenderer;
import dev.abruptsteve.centuryrafflehelper.raffle.RaffleLogic;
import dev.abruptsteve.centuryrafflehelper.raffle.RaffleState;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.network.chat.Component;

import java.util.function.Supplier;

public class CenturyRaffleHelperMod implements ClientModInitializer {
    public static final String MOD_ID = "centuryrafflehelper";
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
                client.player.displayClientMessage(chatMessage(
                    "To prevent false info and mismatched information please open the raffle menu at least once upon launching to ensure all displayed information is correct!"
                ), false);
            }
        });

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> RaffleLogic.handleChat(message.getString()));
        ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) ->
            RaffleLogic.handleChat(message.getString())
        );

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            tickCounter++;
            openQueuedScreen(client);
            if (tickCounter % 20 == 0) {
                RaffleLogic.tick(client);
            }
            if (tickCounter % 600 == 0) {
                CONFIG_MANAGER.saveAll();
            }
        });
    }

    private static void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register(CenturyRaffleHelperMod::registerCommandTree);
    }

    private static void registerCommandTree(
        CommandDispatcher<FabricClientCommandSource> dispatcher,
        CommandBuildContext registryAccess
    ) {
        dispatcher.register(ClientCommandManager.literal("crh")
            .executes(context -> {
                CONFIG_MANAGER.openConfigScreen();
                return 1;
            })
            .then(ClientCommandManager.literal("config")
                .executes(context -> {
                    CONFIG_MANAGER.openConfigScreen();
                    return 1;
                }))
            .then(ClientCommandManager.literal("hud")
                .executes(context -> {
                    openHudEditor();
                    return 1;
                }))
            .then(ClientCommandManager.literal("gui")
                .executes(context -> {
                    openHudEditor();
                    return 1;
                }))
            .then(ClientCommandManager.literal("edit")
                .executes(context -> {
                    openHudEditor();
                    return 1;
                }))
            .then(ClientCommandManager.literal("reset")
                .executes(context -> {
                    STATE.resetDaily(true);
                    context.getSource().sendFeedback(chatMessage("Century Raffle Helper daily state reset."));
                    return 1;
                }))
        );
    }

    private static Component chatMessage(String message) {
        return Component.literal(CHAT_PREFIX + message);
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
