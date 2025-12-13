package net.exylia.commons.v2.reload.detector;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Getter
public class SystemAvailability {
    private final Map<String, Boolean> availability;
    private final Map<String, String> reasons;

    private SystemAvailability(Builder builder) {
        this.availability = builder.availability;
        this.reasons = builder.reasons;
    }

    public boolean isAvailable(String systemName) {
        return availability.getOrDefault(systemName, false);
    }

    public String getReason(String systemName) {
        return reasons.getOrDefault(systemName, "Unknown");
    }

    public Set<String> getAvailableSystems() {
        return availability.entrySet().stream()
                .filter(Map.Entry::getValue)
                .map(Map.Entry::getKey)
                .collect(java.util.stream.Collectors.toSet());
    }

    public Set<String> getUnavailableSystems() {
        return availability.entrySet().stream()
                .filter(entry -> !entry.getValue())
                .map(Map.Entry::getKey)
                .collect(java.util.stream.Collectors.toSet());
    }

    public static class Builder {
        private final Map<String, Boolean> availability = new HashMap<>();
        private final Map<String, String> reasons = new HashMap<>();

        public Builder check(String systemName, boolean isAvailable, String reason) {
            availability.put(systemName, isAvailable);
            reasons.put(systemName, reason);
            return this;
        }

        public Builder available(String systemName) {
            availability.put(systemName, true);
            reasons.put(systemName, "Available");
            return this;
        }

        public Builder unavailable(String systemName, String reason) {
            availability.put(systemName, false);
            reasons.put(systemName, reason);
            return this;
        }

        public SystemAvailability build() {
            return new SystemAvailability(this);
        }
    }
}
