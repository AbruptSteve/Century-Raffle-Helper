package dev.abruptsteve.centuryrafflehelper.update;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class UpdateInstaller {
    private UpdateInstaller() {
    }

    public static void main(String[] args) {
        if (args.length < 3) {
            return;
        }

        Path cleanupDir = args.length >= 4 ? Path.of(args[3]) : null;

        try {
            long parentPid;
            try {
                parentPid = Long.parseLong(args[0]);
            } catch (NumberFormatException ignored) {
                return;
            }

            Path stagedFile = Path.of(args[1]);
            Path targetJar = Path.of(args[2]);
            Path backupJar = targetJar.resolveSibling(targetJar.getFileName().toString() + ".old");

            waitForParentToExit(parentPid);
            for (int attempt = 0; attempt < 60; attempt++) {
                if (!Files.exists(stagedFile)) {
                    return;
                }
                try {
                    Files.deleteIfExists(backupJar);
                    if (Files.exists(targetJar)) {
                        Files.move(targetJar, backupJar, StandardCopyOption.REPLACE_EXISTING);
                    }
                    try {
                        Files.move(stagedFile, targetJar, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                    } catch (IOException ignored) {
                        Files.move(stagedFile, targetJar, StandardCopyOption.REPLACE_EXISTING);
                    }
                    Files.deleteIfExists(backupJar);
                    return;
                } catch (IOException e) {
                    try {
                        if (Files.exists(backupJar) && !Files.exists(targetJar)) {
                            Files.move(backupJar, targetJar, StandardCopyOption.REPLACE_EXISTING);
                        }
                    } catch (IOException ignored) {
                    }
                    Thread.sleep(1000L);
                }
            }
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        } finally {
            if (cleanupDir != null) {
                deleteRecursively(cleanupDir);
            }
        }
    }

    private static void deleteRecursively(Path root) {
        try {
            if (!Files.exists(root)) {
                return;
            }
            try (var paths = Files.walk(root)) {
                paths.sorted((a, b) -> b.compareTo(a)).forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException ignored) {
                    }
                });
            }
        } catch (IOException ignored) {
        }
    }

    private static void waitForParentToExit(long parentPid) throws InterruptedException {
        for (int attempt = 0; attempt < 300; attempt++) {
            var handle = ProcessHandle.of(parentPid);
            if (handle.isEmpty() || !handle.get().isAlive()) {
                Thread.sleep(1500L);
                return;
            }
            Thread.sleep(1000L);
        }
    }
}
