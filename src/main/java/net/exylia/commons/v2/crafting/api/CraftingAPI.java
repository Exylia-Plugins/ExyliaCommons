package net.exylia.commons.v2.crafting.api;

import net.exylia.commons.v2.config.Config;
import net.exylia.commons.v2.crafting.core.CraftingManager;
import net.exylia.commons.v2.crafting.model.CraftingResult;
import net.exylia.commons.v2.crafting.model.CustomRecipe;
import net.exylia.commons.v2.crafting.model.RecipeIngredient;
import org.bukkit.entity.Player;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.Optional;

public final class CraftingAPI {

    private CraftingAPI() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static void initialize(JavaPlugin plugin) {
        CraftingManager.initialize(plugin);
    }

    public static boolean isInitialized() {
        return CraftingManager.isInitialized();
    }

    public static void shutdown() {
        CraftingManager.shutdown();
    }

    public static void loadRecipes(Config config, String path) {
        CraftingManager.getInstance().loadRecipes(config, path);
    }

    public static void registerRecipe(CustomRecipe recipe) {
        CraftingManager.getInstance().registerRecipe(recipe);
    }

    public static void unregisterRecipe(String id) {
        CraftingManager.getInstance().unregisterRecipe(id);
    }

    public static void unregisterAllRecipes() {
        CraftingManager.getInstance().unregisterAllRecipes();
    }

    public static void reload() {
        CraftingManager.getInstance().reload();
    }

    public static Optional<CustomRecipe> getRecipe(String id) {
        return CraftingManager.getInstance().getRecipe(id);
    }

    public static Collection<CustomRecipe> getAllRecipes() {
        return CraftingManager.getInstance().getAllRecipes();
    }

    public static CraftingResult processCraft(CraftingInventory inventory, Player player) {
        return CraftingManager.getInstance().processCraft(inventory, player);
    }

    public static Optional<CustomRecipe> findMatchingRecipe(CraftingInventory inventory) {
        return CraftingManager.getInstance().findMatchingRecipe(inventory);
    }

    public static CustomRecipe.CustomRecipeBuilder recipeBuilder() {
        return CustomRecipe.builder();
    }

    public static RecipeIngredient.RecipeIngredientBuilder ingredientBuilder() {
        return RecipeIngredient.builder();
    }

    public static String toBase64(ItemStack item) {
        return RecipeIngredient.toBase64(item);
    }

    public static ItemStack fromBase64(String base64) {
        return RecipeIngredient.fromBase64(base64);
    }

    public static RecipeIngredient ingredientFromItem(ItemStack item) {
        return RecipeIngredient.fromItemStack(item);
    }
}
