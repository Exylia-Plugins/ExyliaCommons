package net.exylia.commons.v2.ui.sound;

import lombok.Builder;
import lombok.Getter;
import net.exylia.commons.async.Schedulers;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;

@Getter
@Builder
public class SoundConfig {
    private final String soundKey;
    @Builder.Default
    private final float volume = 1.0f;
    @Builder.Default
    private final float pitch = 1.0f;
    @Builder.Default
    private final long delay = 0L;
    @Builder.Default
    private final boolean async = false;

    public CompletableFuture<Void> playAsync(Player player) {
        if (player == null || !player.isOnline()) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.runAsync(() -> {
            if (delay > 0) {
                Schedulers.syncLater(() -> play(player), delay);
            } else {
                Schedulers.sync(() -> play(player));
            }
        });
    }

    public void play(Player player) {
        if (player == null || !player.isOnline() || soundKey == null) {
            return;
        }

        try {
            Sound sound = Sound.valueOf(soundKey.toUpperCase());
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (IllegalArgumentException e) {
        }
    }

    public static SoundConfig fromString(String soundKey) {
        return builder()
                .soundKey(soundKey)
                .build();
    }
}
