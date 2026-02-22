package net.exylia.commons.v2.compat;

import org.bukkit.attribute.Attribute;

public final class AttributeCompat {

    private static volatile Attribute cachedMaxHealth;
    private static volatile boolean maxHealthResolved = false;

    private AttributeCompat() {}

    public static Attribute getMaxHealth() {
        if (maxHealthResolved) return cachedMaxHealth;
        synchronized (AttributeCompat.class) {
            if (maxHealthResolved) return cachedMaxHealth;
            cachedMaxHealth = resolve("GENERIC_MAX_HEALTH", "MAX_HEALTH");
            maxHealthResolved = true;
        }
        return cachedMaxHealth;
    }

    private static Attribute resolve(String... names) {
        for (String name : names) {
            try {
                return Attribute.valueOf(name);
            } catch (IllegalArgumentException ignored) {}
            try {
                return (Attribute) Attribute.class.getField(name).get(null);
            } catch (Exception ignored) {}
        }
        return null;
    }
}
