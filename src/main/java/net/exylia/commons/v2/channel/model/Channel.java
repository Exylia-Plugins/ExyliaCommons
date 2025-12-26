package net.exylia.commons.v2.channel.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@RequiredArgsConstructor
public class Channel {

    private final String id;
    private final String permission;
    private final String format;
    private final double cooldownSeconds;

    @Setter
    private String cooldownMessage;

    public boolean hasCooldown() {
        return cooldownSeconds > 0;
    }

    public String getBypassPermission() {
        return permission + ".bypass";
    }
}
