package net.exylia.commons.v2.command.proxy;

import com.google.gson.Gson;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProxyCommandData {
    private static final Gson GSON = new Gson();

    private final String type;
    private final String command;
    private final String playerName;
    private final String playerId;

    @Builder.Default
    private final long timestamp = System.currentTimeMillis();

    public String toJson() {
        return GSON.toJson(this);
    }

    public static ProxyCommandData fromJson(String json) {
        return GSON.fromJson(json, ProxyCommandData.class);
    }
}
