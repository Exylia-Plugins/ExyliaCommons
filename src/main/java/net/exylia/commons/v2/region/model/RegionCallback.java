package net.exylia.commons.v2.region.model;

import org.bukkit.entity.Player;

@FunctionalInterface
public interface RegionCallback {
    void execute(Player player, Region region);
}
