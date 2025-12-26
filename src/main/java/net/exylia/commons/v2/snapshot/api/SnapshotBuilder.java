package net.exylia.commons.v2.snapshot.api;

import net.exylia.commons.v2.snapshot.model.SnapshotData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;

public class SnapshotBuilder {

    private final Player player;

    private boolean includeInventory = true;
    private boolean includeHealth = true;
    private boolean includeFood = true;
    private boolean includeExp = true;
    private boolean includePotionEffects = true;
    private boolean includeFlightState = true;

    public SnapshotBuilder(Player player) {
        this.player = player;
    }

    public SnapshotBuilder excludeInventory() {
        this.includeInventory = false;
        return this;
    }

    public SnapshotBuilder excludeHealth() {
        this.includeHealth = false;
        return this;
    }

    public SnapshotBuilder excludeFood() {
        this.includeFood = false;
        return this;
    }

    public SnapshotBuilder excludeExp() {
        this.includeExp = false;
        return this;
    }

    public SnapshotBuilder excludePotionEffects() {
        this.includePotionEffects = false;
        return this;
    }

    public SnapshotBuilder excludeFlightState() {
        this.includeFlightState = false;
        return this;
    }

    public CompletableFuture<SnapshotData> buildAsync() {
        return CompletableFuture.supplyAsync(this::build);
    }

    public SnapshotData build() {
        SnapshotData data = SnapshotData.fromPlayer(player);

        if (!includeInventory) {
            data.setArmor(new ItemStack[4]);
            data.setInventory(new ItemStack[36]);
            data.setOffHand(null);
        }
        if (!includeHealth) {
            data.setHealth(20);
            data.setMaxHealth(20);
        }
        if (!includeFood) {
            data.setFoodLevel(20);
            data.setSaturation(5);
        }
        if (!includeExp) {
            data.setLevel(0);
            data.setExp(0);
        }
        if (!includePotionEffects) {
            data.setPotionEffects(Collections.emptyList());
        }
        if (!includeFlightState) {
            data.setAllowFlight(false);
            data.setFlying(false);
            data.setFlySpeed(0.1f);
        }

        return data;
    }
}
