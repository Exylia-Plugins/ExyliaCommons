package net.exylia.commons.v2.debug.config;

import lombok.Getter;
import net.exylia.commons.v2.config.Configs;
import net.exylia.commons.v2.debug.core.DebugCategory;
import net.exylia.commons.v2.debug.core.DebugLevel;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Getter
public class DebugConfig {
    private static volatile DebugConfig instance;

    private final DebugLevel level;
    private final Set<DebugCategory> allowedCategories;
    private final boolean showTimestamps;
    private final boolean showClassNames;
    private final boolean asyncLogging;

    private DebugConfig() {
        if (Configs.raw() != null) {
            ensureDefaults();
            this.level = DebugLevel.fromLevel(Configs.integer("debug.level", 0));
            this.allowedCategories = loadCategories();
            this.showTimestamps = Configs.bool("debug.show-timestamps", false);
            this.showClassNames = Configs.bool("debug.show-class-names", false);
            this.asyncLogging = Configs.bool("debug.async-logging", false);
        } else {
            this.level = DebugLevel.DISABLED;
            this.allowedCategories = Collections.emptySet();
            this.showTimestamps = false;
            this.showClassNames = false;
            this.asyncLogging = false;
        }
    }

    public static DebugConfig getInstance() {
        if (instance == null) {
            synchronized (DebugConfig.class) {
                if (instance == null) {
                    instance = new DebugConfig();
                }
            }
        }
        return instance;
    }

    public static void reload() {
        synchronized (DebugConfig.class) {
            instance = new DebugConfig();
        }
    }

    private Set<DebugCategory> loadCategories() {
        List<String> categories = Configs.stringList("debug.categories");
        if (categories.isEmpty()) {
            return Collections.emptySet();
        }

        return categories.stream()
                .map(String::toUpperCase)
                .map(DebugCategory::fromName)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(
                        () -> Collections.newSetFromMap(new ConcurrentHashMap<>())
                ));
    }

    public boolean isEnabled(DebugLevel requiredLevel) {
        return level == DebugLevel.ALL || level == requiredLevel;
    }

    public boolean isCategoryAllowed(DebugCategory category) {
        return allowedCategories.isEmpty() || allowedCategories.contains(category);
    }

    public static void ensureDefaults() {
        if (!Configs.exists("debug.level")) {
            Configs.set("debug.level", 0);
        }
        if (!Configs.exists("debug.categories")) {
            Configs.set("debug.categories", Collections.emptyList());
        }
        if (!Configs.exists("debug.show-timestamps")) {
            Configs.set("debug.show-timestamps", false);
        }
        if (!Configs.exists("debug.show-class-names")) {
            Configs.set("debug.show-class-names", false);
        }
        if (!Configs.exists("debug.async-logging")) {
            Configs.set("debug.async-logging", false);
        }
        Configs.save();
    }
}
