package net.exylia.commons.item.vanilla;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RegionLimitInfo {
    private final String itemId;
    private final String displayName;
    private final int current;
    private final int max;

    public String getFormattedUsage() {
        return current + "/" + max;
    }

    public boolean isAtLimit() {
        return current >= max;
    }

    public int getRemaining() {
        return Math.max(0, max - current);
    }

    public double getUsagePercent() {
        return max > 0 ? (double) current / max : 0.0;
    }
}