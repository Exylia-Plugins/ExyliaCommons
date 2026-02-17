package net.exylia.commons.v2.crafting.listener;

import net.exylia.commons.v2.crafting.core.CraftingManager;
import net.exylia.commons.v2.crafting.model.CraftingResult;
import net.exylia.commons.v2.crafting.model.CustomRecipe;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public class CraftingListener implements Listener {

    private final CraftingManager manager;

    public CraftingListener(CraftingManager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepareItemCraft(PrepareItemCraftEvent event) {
        CraftingInventory inventory = event.getInventory();
        HumanEntity viewer = event.getView().getPlayer();

        if (!(viewer instanceof Player)) return;
        Player player = (Player) viewer;

        Optional<CustomRecipe> matchingRecipe = manager.findMatchingRecipe(inventory);

        if (matchingRecipe.isPresent()) {
            CustomRecipe recipe = matchingRecipe.get();

            if (recipe.getPermission() != null && !player.hasPermission(recipe.getPermission())) {
                inventory.setResult(null);
                return;
            }

            ItemStack result = recipe.getResult();
            inventory.setResult(result);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCraftItem(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        CraftingInventory inventory = event.getInventory();

        CraftingResult result = manager.processCraft(inventory, player);

        if (result.isSuccess()) {
            event.getInventory().setResult(result.getResultItem());
        }
    }
}
