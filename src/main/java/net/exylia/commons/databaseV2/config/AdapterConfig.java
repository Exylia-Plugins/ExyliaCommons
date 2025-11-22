package net.exylia.commons.databaseV2.config;

import lombok.Getter;
import net.exylia.commons.configSimple.Config;

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
        String prefix = "database-v2." + adapterType + ".";

        String h = config.string(prefix + "host");
        this.host = h != null ? h : "localhost";
        this.port = config.integer(prefix + "port", getDefaultPort(adapterType));
        String db = config.string(prefix + "database");
        this.database = db != null ? db : "minecraft";
        String u = config.string(prefix + "username");
        this.username = u != null ? u : "root";
        String p = config.string(prefix + "password");
        this.password = p != null ? p : "";
        String f = config.string(prefix + "file");
        this.file = f != null ? f : "./data/database";
        this.poolSize = config.integer(prefix + "pool-size", 10);
        this.minIdle = config.integer(prefix + "min-idle", 2);
        this.connectionTimeoutMs = config.longValue(prefix + "connection-timeout-ms", 30000);
        this.idleTimeoutMs = config.longValue(prefix + "idle-timeout-ms", 600000);
        this.maxLifetimeMs = config.longValue(prefix + "max-lifetime-ms", 1800000);
        this.ssl = config.bool(prefix + "ssl", false);
        String c = config.string(prefix + "charset");
        this.charset = c != null ? c : "utf8mb4";
        String col = config.string(prefix + "collation");
        this.collation = col != null ? col : "utf8mb4_unicode_ci";
        String ur = config.string(prefix + "uri");
        this.uri = ur != null ? ur : "";
        this.maxWaitQueueSize = config.integer(prefix + "max-wait-queue-size", 100);
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
        return "jdbc:mysql://" + host + ":" + port + "/" + database + "?characterEncoding=" + charset + "&useUnicode=true";
    }

    public String getH2Url() {
        return "jdbc:h2:" + file;
    }
}
