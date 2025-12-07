package net.exylia.commons.v2.hologram.visibility;

import net.exylia.commons.v2.hologram.model.Hologram;
import org.bukkit.entity.Player;

@FunctionalInterface
public interface VisibilityCondition {
    boolean canSee(Player player, Hologram hologram);

    static VisibilityCondition permission(String permission) {
        return (player, hologram) -> player.hasPermission(permission);
    }

    static VisibilityCondition alwaysVisible() {
        return (player, hologram) -> true;
    }

    static VisibilityCondition neverVisible() {
        return (player, hologram) -> false;
    }

    default VisibilityCondition and(VisibilityCondition other) {
        return (player, hologram) -> this.canSee(player, hologram) && other.canSee(player, hologram);
    }

    default VisibilityCondition or(VisibilityCondition other) {
        return (player, hologram) -> this.canSee(player, hologram) || other.canSee(player, hologram);
    }

    default VisibilityCondition negate() {
        return (player, hologram) -> !this.canSee(player, hologram);
    }
}
