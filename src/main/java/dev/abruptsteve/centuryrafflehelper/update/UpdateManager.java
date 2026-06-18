package dev.abruptsteve.centuryrafflehelper.update;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import dev.abruptsteve.centuryrafflehelper.CenturyRaffleHelperMod;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.JarFile;

public final class UpdateManager {
    private static final Gson GSON = new GsonBuilder().create();
    private static final HttpClient HTTP = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();
    private static final AtomicBoolean CHECKING = new AtomicBoolean();
    private static final long CHECK_INTERVAL_MS = 15L * 60L * 1000L;
    private static final String GITHUB_REPO = "AbruptSteve/Century-Raffle-Helper";
    private static final String PENDING_UPDATE_SUFFIX = ".crh-update";
    private static final String INSTALLER_MAIN_CLASS = "dev.abruptsteve.centuryrafflehelper.update.UpdateInstaller";
    private static final String INSTALLER_CLASS_RESOURCE = "/" + INSTALLER_MAIN_CLASS.replace('.', '/') + ".class";

    private static volatile long lastCheckStartedMs;
    private static volatile String statusLine = "Auto updater is disabled. Use /crh update on to enable it.";
    private static volatile UpdateState updateState = new UpdateState();

    private UpdateManager() {
    }

    public static void init() {
        updateState = loadState();
        if (isEnabled()) {
            statusLine = "Auto updater enabled. Checking GitHub releases...";
            checkForUpdatesAsync(false);
            return;
        }
        statusLine = "Auto updater is disabled. Use /crh update on to enable it.";
    }

    public static void onClientStopping() {
        if (!isEnabled()) {
            return;
        }
        trySchedulePendingInstall();
    }

    public static void onClientTick(Minecraft client) {
        if (!isEnabled() || CHECKING.get()) {
            return;
        }
        long now = Util.getMillis();
        if (lastCheckStartedMs == 0L || now - lastCheckStartedMs >= CHECK_INTERVAL_MS) {
            checkForUpdatesAsync(false);
        }
    }

    public static String getStatusLine() {
        return statusLine;
    }

    public static boolean isEnabled() {
        return updateState.autoUpdateEnabled;
    }

    public static void sendStatus(net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource source) {
        source.sendFeedback(chatLine(statusLine));
    }

    public static void enable(net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource source) {
        updateState.autoUpdateEnabled = true;
        saveState();
        statusLine = "Auto updater enabled. Checking GitHub releases...";
        source.sendFeedback(chatLine("Auto updater enabled. Checking for updates..."));
        checkForUpdatesAsync(true);
    }

    public static void (net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource source) {
        // updateState.autoUpdateEnabled = false;
        saveState();
        clearPendingUpdate();
        statusLine = "Auto updater is disabled. Use /crh update on to enable it.";
        source.sendFeedback(chatLine("Auto updater disabled."));
    }

    public static void checkForUpdatesAsync(boolean manual) {
        if (!isEnabled()) {
            statusLine = "Auto updater is disabled. Use /crh update on to enable it.";
            return;
        }
        if (!CHECKING.compareAndSet(false, true)) {
            statusLine = "Already checking GitHub for updates...";
            return;
        }
        lastCheckStartedMs = Util.getMillis();
        statusLine = "Checking GitHub releases...";

        Thread.startVirtualThread(() -> {
            try {
                performCheck(manual);
            } catch (Exception e) {
                statusLine = "Update check failed. See latest.log for details.";
                CenturyRaffleHelperMod.LOGGER.error("[CenturyRaffleHelper UpdateManager] Failed to check for updates", e);
            } finally {
                CHECKING.set(false);
            }
        });
    }

    private static void performCheck(boolean manual) throws IOException, InterruptedException {
        String minecraftVersion = currentMinecraftVersion();
        String currentVersion = currentModVersion();
        String currentAssetName = assetNameFor(minecraftVersion, currentVersion);

        HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.github.com/repos/" + GITHUB_REPO + "/releases?per_page=100"))
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .header("User-Agent", "CenturyRaffleHelper AutoUpdater")
            .timeout(Duration.ofSeconds(20))
            .GET()
            .build();

        HttpResponse<String> releaseResponse = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        if (releaseResponse.statusCode() != 200) {
            statusLine = "GitHub responded with HTTP " + releaseResponse.statusCode() + ".";
            return;
        }

        GithubRelease[] releases = GSON.fromJson(releaseResponse.body(), GithubRelease[].class);
        if (releases == null || releases.length == 0) {
            statusLine = "No releases were returned yet.";
            return;
        }

        GithubAsset chosenAsset = null;
        GithubRelease chosenRelease = null;
        for (GithubRelease release : releases) {
            if (release == null || release.assets == null || release.assets.length == 0) {
                continue;
            }
            Optional<GithubAsset> asset = Arrays.stream(release.assets)
                .filter(candidate -> candidate != null)
                .filter(candidate -> candidate.name != null && candidate.browserDownloadUrl != null)
                .filter(candidate -> candidate.name.toLowerCase(Locale.ROOT).endsWith(".jar"))
                .filter(candidate -> !candidate.name.contains("-sources"))
                .filter(candidate -> candidate.name.startsWith(ASSET_PREFIX()))
                .filter(candidate -> candidate.name.contains("-" + minecraftVersion + "-"))
                .findFirst();
            if (asset.isPresent()) {
                chosenAsset = asset.get();
                chosenRelease = release;
                break;
            }
        }

        if (chosenAsset == null || chosenRelease == null) {
            statusLine = "No jar asset was found for Minecraft " + minecraftVersion + ".";
            return;
        }

        if (chosenAsset.name.equals(currentAssetName)) {
            clearPendingUpdate();
            statusLine = "You're up to date on " + currentAssetName + ".";
            return;
        }

        Optional<Path> currentJar = resolveCurrentJarPath();
        String assetName = chosenAsset.name;
        Path destination = currentJar
            .map(UpdateManager::pendingPathFor)
            .orElseGet(() -> fallbackUpdateDir().resolve(assetName));
        Files.createDirectories(destination.getParent());

        if (Files.exists(destination) && chosenAsset.size > 0 && Files.size(destination) == chosenAsset.size) {
            statusLine = "Update " + chosenAsset.name + " is already downloaded.";
            return;
        }

        HttpRequest downloadRequest = HttpRequest.newBuilder(URI.create(chosenAsset.browserDownloadUrl))
            .header("User-Agent", "CenturyRaffleHelper AutoUpdater")
            .timeout(Duration.ofSeconds(60))
            .GET()
            .build();
        HttpResponse<InputStream> downloadResponse = HTTP.send(downloadRequest, HttpResponse.BodyHandlers.ofInputStream());
        if (downloadResponse.statusCode() != 200) {
            statusLine = "Download failed with HTTP " + downloadResponse.statusCode() + ".";
            return;
        }

        Path tempFile = destination.resolveSibling(destination.getFileName() + ".part");
        try (InputStream inputStream = downloadResponse.body()) {
            Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
        }
        if (!verifyDownloadedJar(tempFile, minecraftVersion)) {
            Files.deleteIfExists(tempFile);
            statusLine = "Downloaded file failed verification.";
            return;
        }
        try {
            Files.move(tempFile, destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException ignored) {
            Files.move(tempFile, destination, StandardCopyOption.REPLACE_EXISTING);
        }

        if (currentJar.isPresent()) {
            statusLine = "Downloaded " + chosenAsset.name + ". Close the game to install it.";
        } else {
            statusLine = "Downloaded " + chosenAsset.name + " to " + destination.toAbsolutePath() + ".";
        }
    }

    private static String currentMinecraftVersion() {
        return Minecraft.getInstance().getLaunchedVersion().trim();
    }

    private static String currentModVersion() {
        return FabricLoader.getInstance()
            .getModContainer(CenturyRaffleHelperMod.MOD_ID)
            .map(container -> container.getMetadata().getVersion().getFriendlyString().trim())
            .orElse("unknown");
    }

    private static String assetNameFor(String minecraftVersion, String modVersion) {
        return ASSET_PREFIX() + minecraftVersion + "-" + modVersion + ".jar";
    }

    private static String ASSET_PREFIX() {
        return CenturyRaffleHelperMod.MOD_ID + "-";
    }

    private static Optional<Path> resolveCurrentJarPath() {
        try {
            Path path = Path.of(CenturyRaffleHelperMod.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toAbsolutePath();
            if (Files.isRegularFile(path) && path.getFileName().toString().endsWith(".jar")) {
                return Optional.of(path);
            }
        } catch (Exception e) {
            CenturyRaffleHelperMod.LOGGER.debug("[CenturyRaffleHelper UpdateManager] Unable to resolve current jar path", e);
        }
        return Optional.empty();
    }

    private static Path pendingPathFor(Path currentJar) {
        return currentJar.resolveSibling(currentJar.getFileName().toString() + PENDING_UPDATE_SUFFIX);
    }

    private static Path fallbackUpdateDir() {
        return FabricLoader.getInstance().getConfigDir().resolve("centuryrafflehelper").resolve("updates");
    }

    private static Path statePath() {
        return FabricLoader.getInstance().getConfigDir().resolve("centuryrafflehelper").resolve("update-state.json");
    }

    private static UpdateState loadState() {
        Path path = statePath();
        if (!Files.exists(path)) {
            return new UpdateState();
        }
        try (var reader = Files.newBufferedReader(path)) {
            UpdateState loaded = GSON.fromJson(reader, UpdateState.class);
            return loaded == null ? new UpdateState() : loaded;
        } catch (Exception e) {
            CenturyRaffleHelperMod.LOGGER.debug("[CenturyRaffleHelper UpdateManager] Failed to load updater state", e);
            return new UpdateState();
        }
    }

    private static void saveState() {
        Path path = statePath();
        try {
            Files.createDirectories(path.getParent());
            try (var writer = Files.newBufferedWriter(path)) {
                GSON.toJson(updateState, writer);
            }
        } catch (IOException e) {
            CenturyRaffleHelperMod.LOGGER.debug("[CenturyRaffleHelper UpdateManager] Failed to save updater state", e);
        }
    }

    private static void trySchedulePendingInstall() {
        Optional<Path> currentJar = resolveCurrentJarPath();
        if (currentJar.isEmpty()) {
            return;
        }
        Path stagedFile = pendingPathFor(currentJar.get());
        if (!Files.exists(stagedFile)) {
            return;
        }
        try {
            launchInstaller(currentJar.get(), stagedFile);
        } catch (IOException e) {
            statusLine = "Update downloaded, but failed to schedule install.";
            CenturyRaffleHelperMod.LOGGER.error("[CenturyRaffleHelper UpdateManager] Failed to launch installer", e);
        }
    }

    private static void launchInstaller(Path currentJar, Path stagedFile) throws IOException {
        String javaExe = Path.of(
            System.getProperty("java.home"),
            "bin",
            System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win") ? "javaw.exe" : "java"
        ).toString();

        Path installerRoot = Files.createTempDirectory("centuryrafflehelper-updater-").toAbsolutePath();
        Path installerClass = installerRoot.resolve(INSTALLER_MAIN_CLASS.replace('.', '/') + ".class");
        Files.createDirectories(installerClass.getParent());
        try (InputStream in = UpdateManager.class.getResourceAsStream(INSTALLER_CLASS_RESOURCE)) {
            if (in == null) {
                throw new IOException("Unable to locate " + INSTALLER_CLASS_RESOURCE + " in mod jar");
            }
            Files.copy(in, installerClass, StandardCopyOption.REPLACE_EXISTING);
        }

        new ProcessBuilder(
            javaExe,
            "-cp",
            installerRoot.toString(),
            INSTALLER_MAIN_CLASS,
            Long.toString(ProcessHandle.current().pid()),
            stagedFile.toAbsolutePath().toString(),
            currentJar.toAbsolutePath().toString(),
            installerRoot.toString()
        ).start();
    }

    private static boolean verifyDownloadedJar(Path jarPath, String minecraftVersion) {
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            var entry = jarFile.getEntry("fabric.mod.json");
            if (entry == null) {
                return false;
            }

            try (InputStream in = jarFile.getInputStream(entry)) {
                String json = new String(in.readAllBytes());
                boolean idMatches = json.contains("\"id\": \"" + CenturyRaffleHelperMod.MOD_ID + "\"")
                    || json.contains("\"id\":\"" + CenturyRaffleHelperMod.MOD_ID + "\"");
                boolean minecraftMatches = json.contains("\"minecraft\": \"" + minecraftVersion + "\"")
                    || json.contains("\"minecraft\":\"" + minecraftVersion + "\"");
                return idMatches && minecraftMatches;
            }
        } catch (Exception e) {
            CenturyRaffleHelperMod.LOGGER.warn("[CenturyRaffleHelper UpdateManager] Downloaded jar failed verification", e);
            return false;
        }
    }

    private static void clearPendingUpdate() {
        resolveCurrentJarPath().ifPresent(currentJar -> {
            try {
                Files.deleteIfExists(pendingPathFor(currentJar));
            } catch (IOException e) {
                CenturyRaffleHelperMod.LOGGER.debug("[CenturyRaffleHelper UpdateManager] Failed to clear pending update", e);
            }
        });

        Path fallbackDir = fallbackUpdateDir();
        if (!Files.isDirectory(fallbackDir)) {
            return;
        }
        try (var paths = Files.list(fallbackDir)) {
            paths.forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    CenturyRaffleHelperMod.LOGGER.debug("[CenturyRaffleHelper UpdateManager] Failed to delete fallback update {}", path, e);
                }
            });
        } catch (IOException e) {
            CenturyRaffleHelperMod.LOGGER.debug("[CenturyRaffleHelper UpdateManager] Failed to clear fallback updates", e);
        }
    }

    private static Component chatLine(String message) {
        return Component.literal("[CRH] " + message);
    }

    private static final class GithubRelease {
        @SerializedName("tag_name")
        String tagName;
        GithubAsset[] assets;
    }

    private static final class GithubAsset {
        String name;
        long size;
        @SerializedName("browser_download_url")
        String browserDownloadUrl;
    }

    private static final class UpdateState {
        boolean autoUpdateEnabled = true;
    }
}
