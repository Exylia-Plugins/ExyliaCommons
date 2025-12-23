package net.exylia.commons.v2.database.config;

import lombok.Getter;
import net.exylia.commons.v2.config.Config;
import org.bukkit.plugin.Plugin;

import java.io.File;

@Getter
public class AdapterConfig {

    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;
    private final String file;
    private final int poolSize;
    private final int minIdle;
    private final long connectionTimeoutMs;
    private final long idleTimeoutMs;
    private final long maxLifetimeMs;
    private final boolean ssl;
    private final String charset;
    private final String collation;
    private final String uri;
    private final int maxWaitQueueSize;

    public AdapterConfig(Config config, String adapterType) {
        this(config, adapterType, null);
    }

    public AdapterConfig(Config config, String adapterType, Plugin plugin) {
        String prefix = adapterType + ".";

        String h = config.string(prefix + "host");
        this.host = h != null ? h : "localhost";
        this.port = config.integer(prefix + "port", getDefaultPort(adapterType));
        String db = config.string(prefix + "database");
        this.database = db != null ? db : "minecraft";
        String u = config.string(prefix + "username");
        this.username = u != null ? u : "root";
        String p = config.string(prefix + "password");
        this.password = p != null ? p : "";
        String f = config.string(prefix + "file-path");
        this.file = resolveFilePath(f, plugin);
        this.poolSize = config.integer(prefix + "pool.max-size", 10);
        this.minIdle = config.integer(prefix + "pool.min-idle", 2);
        this.connectionTimeoutMs = config.longValue(prefix + "pool.connection-timeout", 30000);
        this.idleTimeoutMs = config.longValue(prefix + "pool.idle-timeout", 600000);
        this.maxLifetimeMs = config.longValue(prefix + "pool.max-lifetime", 1800000);
        this.ssl = config.bool(prefix + "use-ssl", false);
        String c = config.string(prefix + "charset");
        this.charset = c != null ? c : "utf8mb4";
        String col = config.string(prefix + "collation");
        this.collation = col != null ? col : "utf8mb4_unicode_ci";
        String ur = config.string(prefix + "uri");
        this.uri = ur != null ? ur : "";
        this.maxWaitQueueSize = config.integer(prefix + "max-wait-queue-size", 100);
    }

    private String resolveFilePath(String configPath, Plugin plugin) {
        if (configPath != null && !configPath.isEmpty()) {
            return resolvePlaceholders(configPath, plugin);
        }

        if (plugin != null) {
            File dataFolder = new File(plugin.getDataFolder(), "data");
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }
            return new File(dataFolder, "database").getAbsolutePath();
        }

        return "./data/database";
    }

    private String resolvePlaceholders(String path, Plugin plugin) {
        if (plugin == null) {
            return path;
        }

        return path.replace("{plugin_folder}", plugin.getDataFolder().getAbsolutePath())
                   .replace("{plugin_name}", plugin.getName().toLowerCase());
    }

    private static int getDefaultPort(String adapterType) {
        return switch (adapterType.toLowerCase()) {
            case "mysql", "mariadb" -> 3306;
            case "mongodb" -> 27017;
            case "h2" -> 0;
            default -> 0;
        };
    }

    public String getJdbcUrl() {
        return "jdbc:mysql://" + host + ":" + port + "/" + database +
               "?useUnicode=true&characterEncoding=UTF-8" +
               (ssl ? "&useSSL=true&requireSSL=true" : "&useSSL=false") +
               "&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    }

    public String getH2Url() {
        return "jdbc:h2:" + file;
    }
}
