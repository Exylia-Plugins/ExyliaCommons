package net.exylia.commons.v2.reload.adapter;

import net.exylia.commons.ExyliaPlugin;
import net.exylia.commons.v2.redis.SimpleRedis;
import net.exylia.commons.v2.reload.core.ReloadPriority;

public class RedisAdapter extends ReloadableSystemAdapter {

    private final ExyliaPlugin plugin;

    public RedisAdapter(ExyliaPlugin plugin) {
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
