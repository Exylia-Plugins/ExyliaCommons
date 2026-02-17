package net.exylia.commons.v2.debug.config;

import net.exylia.commons.v2.debug.core.DebugCategory;
import net.exylia.commons.v2.debug.core.DebugLevel;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class DebugConfig {

    private static Set<DebugCategory> cachedCategories = Collections.emptySet();

    public static Set<DebugCategory> getAllowedCategories() {
        if (cachedCategories.isEmpty() && !DebugDefaults.Debug.CATEGORIES.isEmpty()) {
            cachedCategories = DebugDefaults.Debug.CATEGORIES.stream()
                    .map(String::toUpperCase)
                    .map(DebugCategory::fromName)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(
                            () -> Collections.newSetFromMap(new ConcurrentHashMap<>())
                    ));
        }
        return cachedCategories;
    }

    public static boolean isEnabled(DebugLevel requiredLevel) {
        try {
            DebugLevel currentLevel = DebugLevel.fromLevel(DebugDefaults.Debug.LEVEL);
            return currentLevel == DebugLevel.ALL || currentLevel == requiredLevel;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isCategoryAllowed(DebugCategory category) {
        Set<DebugCategory> allowed = getAllowedCategories();
        return allowed.isEmpty() || allowed.contains(category);
    }

    public static void reload() {
        cachedCategories = Collections.emptySet();
    }
}
