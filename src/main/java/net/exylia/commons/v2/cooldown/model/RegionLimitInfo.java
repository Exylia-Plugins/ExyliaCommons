package net.exylia.commons.v2.cooldown.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class RegionLimitInfo {

    private final String displayName;
    private final int current;
    private final int max;

    public int getRemaining() {
        return Math.max(0, max - current);
    }

    public String getFormattedUsage() {
        return current + "/" + max;
    }
}
