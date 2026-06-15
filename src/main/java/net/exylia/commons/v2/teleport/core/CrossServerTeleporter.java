package net.exylia.commons.v2.teleport.core;

import net.exylia.commons.v2.database.redis.RedisConnectionPool;
import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.tasks.api.TaskAPI;
import net.exylia.commons.v2.teleport.model.ExyliaLocation;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

final class CrossServerTeleporter {

    private static final String BUNGEE_CHANNEL = "BungeeCord";
    private static final int PENDING_TTL_SECONDS = 300;

    private final Plugin plugin;
    private final RedisConnectionPool redisPool;
    private final String keyPrefix;

    CrossServerTeleporter(Plugin plugin, RedisConnectionPool redisPool, String keyPrefix) {
        this.plugin = plugin;
        this.redisPool = redisPool;
        this.keyPrefix = keyPrefix;
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, BUNGEE_CHANNEL);
    }

    void teleport(Player player, ExyliaLocation destination) {
        if (!player.isOnline()) return;

        String key = pendingKey(player.getUniqueId());
        String value = destination.toString();

        TaskAPI.async(() -> redisPool.execute(jedis -> jedis.setex(key, PENDING_TTL_SECONDS, value)));

        if (TaskAPI.isFolia()) {
            player.getScheduler().run(plugin, t -> sendConnect(player, destination.getServer()), null);
        } else {
            TaskAPI.sync(() -> sendConnect(player, destination.getServer()));
        }
    }

    void teleportAll(Collection<? extends Player> players, ExyliaLocation destination) {
        players.forEach(p -> teleport(p, destination));
    }

    Optional<ExyliaLocation> consumePending(UUID uuid) {
        String key = pendingKey(uuid);
        try {
            String value = redisPool.execute(jedis -> jedis.getDel(key));
            if (value == null) return Optional.empty();
            return Optional.of(ExyliaLocation.fromString(value));
        } catch (Exception e) {
            DebugAPI.logLibWarn("Failed to consume pending teleport for " + uuid + ": " + e.getMessage());
            return Optional.empty();
        }
    }

    private String pendingKey(UUID uuid) {
        return keyPrefix + ":teleport:pending:" + uuid;
    }

    private void sendConnect(Player player, String server) {
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(stream);
            out.writeUTF("Connect");
            out.writeUTF(server);
            player.sendPluginMessage(plugin, BUNGEE_CHANNEL, stream.toByteArray());
        } catch (IOException e) {
            DebugAPI.logLibError("Failed to send BungeeCord connect for " + player.getName() + ": " + e.getMessage());
        }
    }
}
