package net.exylia.commons.v2.database.serialization.builtin;

import org.bukkit.Bukkit;
import org.bukkit.World;

final class WorldResolver {

    private WorldResolver() {}

    static World find(String name) {
        if (name == null || name.isEmpty()) return null;

        World world = Bukkit.getWorld(name);
        if (world != null) return world;

        for (World w : Bukkit.getWorlds()) {
            if (w.getName().equalsIgnoreCase(name)) return w;
            if (w.key().asString().equals(name)) return w;
            if (w.key().asString().equalsIgnoreCase(name)) return w;
            if (w.key().value().equalsIgnoreCase(name)) return w;
        }

        return null;
    }
}
