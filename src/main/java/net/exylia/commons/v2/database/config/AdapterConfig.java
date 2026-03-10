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
    private final String authDatabase;
    private final String charset;
    private final String collation;
    private final String uri;
    private final boolean autoServer;

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
        this.username = u != null ? u : "";
        String p = config.string(prefix + "password");
        this.password = p != null ? p : "";
        String f = config.string(prefix + "file");
        this.file = resolveFilePath(f, plugin);

        this.poolSize = DatabaseDefaults.Database.Settings.POOL_SIZE;
        this.minIdle = DatabaseDefaults.Database.Settings.MINIMUM_IDLE;
        this.connectionTimeoutMs = DatabaseDefaults.Database.Settings.CONNECTION_TIMEOUT;
        this.idleTimeoutMs = DatabaseDefaults.Database.Settings.IDLE_TIMEOUT;
        this.maxLifetimeMs = DatabaseDefaults.Database.Settings.MAX_LIFETIME;

        this.ssl = config.bool(prefix + "ssl", false);
        String auth = config.string(prefix + "auth-database");
        this.authDatabase = auth != null ? auth : "admin";
        this.charset = "utf8mb4";
        this.collation = "utf8mb4_unicode_ci";
        String mongoUri = config.string(prefix + "connection-string");
        this.uri = mongoUri != null ? mongoUri : "";
        this.autoServer = config.bool(prefix + "auto-server", false);
    }

    private String resolveFilePath(String configPath, Plugin plugin) {
        if (configPath != null && !configPath.isEmpty()) {
            String resolvedPath = resolvePlaceholders(configPath, plugin);

            if (plugin != null) {
                File file = new File(resolvedPath);
                if (!file.isAbsolute()) {
                    file = new File(plugin.getDataFolder(), resolvedPath);
                }
                File parentDir = file.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    parentDir.mkdirs();
                }
                return file.getAbsolutePath();
            }

            if (!new File(resolvedPath).isAbsolute() && !resolvedPath.startsWith("./") && !resolvedPath.startsWith("~/")) {
                resolvedPath = "./" + resolvedPath;
            }
            return resolvedPath;
        }

        if (plugin != null) {
            return new File(plugin.getDataFolder(), "database").getAbsolutePath();
        }

        return "./database";
    }

    private String resolvePlaceholders(String path, Plugin plugin) {
        if (plugin == null) {
            return path;
        }

        return path.replace("{plugin_folder}", plugin.getDataFolder().getAbsolutePath())
                   .replace("{plugin_name}", plugin.getName().toLowerCase());
    }

    private static int getDefaultPort(String adapterType) {
        String type = adapterType.toLowerCase();
        if (type.contains("mysql") || type.contains("mariadb")) return 3306;
        if (type.contains("mongodb")) return 27017;
        return 0;
    }

    public String getJdbcUrl() {
        return "jdbc:mysql://" + host + ":" + port + "/" + database +
               "?useUnicode=true&characterEncoding=UTF-8" +
               (ssl ? "&useSSL=true&requireSSL=true" : "&useSSL=false") +
               "&serverTimezone=UTC&allowPublicKeyRetrieval=true" +
               "&rewriteBatchedStatements=true";
    }

    public String getH2Url() {
        String url = "jdbc:h2:" + file;
        if (autoServer) {
            url += ";AUTO_SERVER=TRUE";
        }
        return url;
    }
}
