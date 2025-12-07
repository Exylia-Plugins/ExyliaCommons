package net.exylia.commons.v2.redis;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString(exclude = "password")
public class SimpleRedisConfig {

    @Builder.Default
    private String host = "localhost";

    @Builder.Default
    private int port = 6379;

    private String password;

    @Builder.Default
    private int database = 0;

    @Builder.Default
    private int timeout = 2000;

    @Builder.Default
    private int poolSize = 8;

    @Builder.Default
    private String keyPrefix = "";

    public static SimpleRedisConfig defaults() {
        return builder().build();
    }

    public static SimpleRedisConfig of(String host, int port) {
        return builder().host(host).port(port).build();
    }

    public static SimpleRedisConfig of(String host, int port, String password) {
        return builder().host(host).port(port).password(password).build();
    }
}
