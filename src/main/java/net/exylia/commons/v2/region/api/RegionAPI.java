package net.exylia.commons.v2.region.api;

import net.exylia.commons.v2.region.RegionManager;
import net.exylia.commons.v2.region.model.Region;
import net.exylia.commons.v2.region.schematic.SchematicManager;
import net.exylia.commons.v2.region.selection.SelectionManager;
import net.exylia.commons.v2.region.selection.Selection;
import net.exylia.commons.v2.region.visual.RegionSelector;
import net.exylia.commons.v2.region.visual.SelectionSession;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

public class RegionAPI {
    private final RegionManager manager;
    private final RegionSelector selector;
    private final SelectionManager selectionManager;
    private final SchematicManager schematicManager;

    public RegionAPI() {
        this.manager = RegionManager.getInstance();
        this.selector = RegionSelector.getInstance();
        this.selectionManager = SelectionManager.getInstance();
        this.schematicManager = SchematicManager.getInstance();
    }

    public static void initialize(JavaPlugin plugin) {
        RegionManager.initialize(plugin);
    }

    public static RegionAPI getInstance() {
        return new RegionAPI();
    }

    public RegionBuilder createRegion(String id) {
        return new RegionBuilder(id);
    }

    public boolean registerRegion(Region region) {
        return manager.registerRegion(region);
    }

    public boolean unregisterRegion(String regionId) {
        return manager.unregisterRegion(regionId);
    }

    public Optional<Region> getRegion(String regionId) {
        return manager.getRegion(regionId);
    }

    public Collection<Region> getAllRegions() {
        return manager.getAllRegions();
    }

    public List<Region> getRegionsAt(Location location) {
        return manager.getRegionsAt(location);
    }

    public Optional<Region> getHighestPriorityRegionAt(Location location) {
        return manager.getHighestPriorityRegionAt(location);
    }

    public Set<Region> getPlayerRegions(Player player) {
        return manager.getPlayerRegions(player);
    }

    public boolean isPlayerInRegion(Player player, Region region) {
        return manager.isPlayerInRegion(player, region);
    }

    public boolean isPlayerInAnyRegion(Player player) {
        return manager.isPlayerInAnyRegion(player);
    }

    public SelectionSession showSelector(Player player, Region region) {
        return selector.showSelector(player, region);
    }

    public SelectionSession showSelector(Player player, Region region, Color color) {
        return selector.showSelector(player, region, color);
    }

    public void stopSelector(Player player) {
        selector.stopSession(player);
    }

    public Optional<SelectionSession> getSelectionSession(Player player) {
        return selector.getSession(player);
    }

    public boolean hasActiveSelection(Player player) {
        return selector.hasActiveSession(player);
    }

    public ItemStack giveWand(Player player) {
        ItemStack wand = selectionManager.createWand();
        player.getInventory().addItem(wand);
        return wand;
    }

    public ItemStack createWand() {
        return selectionManager.createWand();
    }

    public ItemStack createWand(String selectionId) {
        return selectionManager.createWand(selectionId);
    }

    public boolean isWand(ItemStack item) {
        return selectionManager.isWand(item);
    }

    public Optional<Selection> getPlayerSelection(Player player) {
        return selectionManager.getSelection(player);
    }

    public void setSelectionPos1(Player player, Location location) {
        selectionManager.setPos1(player, location);
    }

    public void setSelectionPos2(Player player, Location location) {
        selectionManager.setPos2(player, location);
    }

    public void clearPlayerSelection(Player player) {
        selectionManager.clearSelection(player);
    }

    public void setSelectionCallback(Player player, Consumer<Selection> callback) {
        selectionManager.setCallback(player, callback);
    }

    public void clearSelectionCallback(Player player) {
        selectionManager.clearCallback(player);
    }

    public Region createRegionFromSelection(String regionId, Player player) {
        Optional<Selection> selectionOpt = selectionManager.getSelection(player);
        if (selectionOpt.isEmpty()) {
            return null;
        }

        Selection selection = selectionOpt.get();
        if (!selection.isComplete()) {
            return null;
        }

        Region region = new Region(regionId, selection.toSelection());
        if (registerRegion(region)) {
            return region;
        }

        return null;
    }

    public SchematicManager getSchematicManager() {
        return schematicManager;
    }
}
