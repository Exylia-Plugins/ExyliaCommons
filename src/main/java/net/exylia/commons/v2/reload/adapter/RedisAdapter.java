package net.exylia.commons.v2.reload.adapter;

import net.exylia.commons.ExyliaPlugin;
import net.exylia.commons.v2.redis.SimpleRedis;
import net.exylia.commons.v2.reload.core.ReloadPriority;
import org.bukkit.plugin.java.JavaPlugin;

public class RedisAdapter extends ReloadableSystemAdapter {

    private final JavaPlugin plugin;

    public RedisAdapter(JavaPlugin plugin) {
        super("Redis", ReloadPriority.HIGH);
        this.plugin = plugin;
    }

    @Override
    protected void performCacheClear() throws Exception {
    }

    @Override
    protected void performReload() throws Exception {
        boolean success = SimpleRedis.reload(plugin);
        if (!success) {
            throw new Exception("SimpleRedis reload failed");
        }
    }

    @Override
    public boolean isAvailable() {
        try {
            Class.forName("redis.clients.jedis.Jedis");
            SimpleRedis instance = SimpleRedis.getInstance();
            return instance != null && instance.isConnected();
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
