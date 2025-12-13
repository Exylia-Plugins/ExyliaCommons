package net.exylia.commons.v2.scoreboard.cache;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.bukkit.entity.Player;

import java.util.UUID;

@Getter
@EqualsAndHashCode
@AllArgsConstructor
public class LineCacheKey {

    private final UUID playerId;
    private final String lineContent;
    private final long minute;

    public static LineCacheKey of(Player player, String content) {
        long minute = System.currentTimeMillis() / 60000;
        return new LineCacheKey(player.getUniqueId(), content, minute);
    }

    public static LineCacheKey of(UUID playerId, String content) {
        long minute = System.currentTimeMillis() / 60000;
        return new LineCacheKey(playerId, content, minute);
    }
}
