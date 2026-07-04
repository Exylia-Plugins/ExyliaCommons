package net.exylia.commons.v2.sequence.preview;

import net.exylia.commons.v2.sequence.SequenceAPI;
import net.exylia.commons.v2.sequence.SequenceContext;
import net.exylia.commons.v2.tasks.api.TaskAPI;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class EffectPreview {

    public static final Set<UUID> FROZEN_PLAYERS = ConcurrentHashMap.newKeySet();
    public static final Set<UUID> PREVIEW_DUMMIES = ConcurrentHashMap.newKeySet();

    private static final ChunkHandler CHUNK_HANDLER = loadChunkHandler();
    private static final int CHUNK_RADIUS = 4;
    private static final long DUMMY_DEATH_DELAY_MS = 750L;
    private static final long EXTRA_MS = 1500L;
    private static final long REOPEN_DELAY_MS = 400L;

    static Plugin hostPlugin;

    private EffectPreview() {}

    public static void init(Plugin plugin) {
        hostPlugin = plugin;
    }

    private static ChunkHandler loadChunkHandler() {
        try {
            Class<?> cls = Class.forName("com.github.retrooper.packetevents.PacketEvents");
            Object api = cls.getMethod("getAPI").invoke(null);
            if (api != null) {
                return (ChunkHandler) Class.forName("net.exylia.commons.v2.sequence.preview.PacketEventsChunkHandler")
                        .getDeclaredConstructor().newInstance();
            }
        } catch (Exception ignored) {}
        try {
            return new NMSChunkHandler();
        } catch (Exception ignored) {}
        return ChunkHandler.NOOP;
    }

    public static void play(Player player, List<String> effects, Runnable onComplete) {
        Consumer<Location> runner = loc -> {
            SequenceContext ctx = SequenceContext.builder()
                    .location(loc)
                    .sourcePlayer(player)
                    .particleFilter((observer, srcId) -> observer.getUniqueId().equals(player.getUniqueId()))
                    .build();
            SequenceAPI.execute(ctx, effects);
        };
        playInternal(player, runner, estimateDelayMs(effects), onComplete);
    }

    public static void play(Player player, Consumer<Location> effectRunner, Runnable onComplete) {
        playInternal(player, effectRunner, 0L, onComplete);
    }

    public static void play(Player player, Consumer<Location> effectRunner, long durationMs, Runnable onComplete) {
        playInternal(player, effectRunner, durationMs, onComplete);
    }

    private static void playInternal(Player player, Consumer<Location> effectRunner, long effectDurationMs, Runnable onComplete) {
        if (FROZEN_PLAYERS.contains(player.getUniqueId())) return;
        player.closeInventory();

        Location playerLoc = player.getLocation().clone();
        Vector dir = playerLoc.getDirection().setY(0).normalize();
        Location previewLoc = playerLoc.clone().add(dir.multiply(5));

        Location facingLoc = playerLoc.clone();
        facingLoc.setPitch(0f);
        player.teleport(facingLoc);

        FROZEN_PLAYERS.add(player.getUniqueId());
        CHUNK_HANDLER.clear(player, CHUNK_RADIUS);

        PreviewDummy dummy = createDummy(previewLoc, player);

        TaskAPI.atLater(previewLoc, () -> {
            dummy.die();
            effectRunner.accept(previewLoc);
        }, DUMMY_DEATH_DELAY_MS, TimeUnit.MILLISECONDS);

        long totalDelay = DUMMY_DEATH_DELAY_MS + effectDurationMs + EXTRA_MS;

        TaskAPI.atLater(playerLoc, () -> {
            FROZEN_PLAYERS.remove(player.getUniqueId());
            CHUNK_HANDLER.restore(player, CHUNK_RADIUS);
            dummy.remove();
            if (onComplete != null) {
                TaskAPI.atLater(playerLoc, onComplete::run, REOPEN_DELAY_MS, TimeUnit.MILLISECONDS);
            }
        }, totalDelay, TimeUnit.MILLISECONDS);
    }

    public static boolean hasChunkSupport() {
        return CHUNK_HANDLER != ChunkHandler.NOOP;
    }

    private static PreviewDummy createDummy(Location loc, Player viewer) {
        try {
            return new NMSPlayerDummy(loc, viewer);
        } catch (Exception e) {
            return new ZombieDummy(loc, viewer, hostPlugin);
        }
    }

    private static long estimateDelayMs(List<String> effects) {
        long total = 0;
        for (String effect : effects) {
            if (effect == null || effect.isBlank()) continue;
            int open = effect.indexOf('[');
            int close = effect.indexOf(']');
            if (open < 0 || close <= open) continue;
            String type = effect.substring(open + 1, close).trim().toUpperCase();
            if (!type.equals("DELAY")) continue;
            String arg = effect.substring(close + 1).trim();
            try {
                total += (long) (Double.parseDouble(arg) * 1000);
            } catch (NumberFormatException ignored) {}
        }
        return total;
    }
}
