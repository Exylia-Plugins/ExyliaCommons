package net.exylia.commons.v2.crafting.core;

import lombok.Getter;
import net.exylia.commons.v2.config.Config;
import net.exylia.commons.v2.crafting.listener.CraftingListener;
import net.exylia.commons.v2.crafting.model.CraftingResult;
import net.exylia.commons.v2.crafting.model.CustomRecipe;
import net.exylia.commons.v2.crafting.model.RecipeIngredient;
import net.exylia.commons.v2.crafting.processor.IngredientMatcher;
import net.exylia.commons.v2.crafting.processor.RecipeProcessor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.Optional;

public class CraftingManager {

    private static CraftingManager instance;
    private static final Object LOCK = new Object();

    @Getter
    private final JavaPlugin plugin;

    @Getter
    private final CraftingRegistry registry;

    @Getter
    private final RecipeProcessor processor;

    @Getter
    private final IngredientMatcher matcher;

    private CraftingListener listener;

    private CraftingManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.registry = new CraftingRegistry();
        this.processor = new RecipeProcessor(plugin);
        this.matcher = new IngredientMatcher();
    }

    public static void initialize(JavaPlugin plugin) {
        synchronized (LOCK) {
            if (instance == null) {
                instance = new CraftingManager(plugin);
                instance.registerListener();
            }
        }
    }

    public static CraftingManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("CraftingManager not initialized. Call CraftingAPI.initialize() first.");
        }
        return instance;
    }

    public static boolean isInitialized() {
        return instance != null;
    }

    public static void shutdown() {
        synchronized (LOCK) {
            if (instance != null) {
                instance.unregisterAllRecipes();
                instance = null;
            }
        }
    }

    private void registerListener() {
        listener = new CraftingListener(this);
        Bukkit.getPluginManager().registerEvents(listener, plugin);
    }

    public void loadRecipes(Config config, String path) {
        processor.processConfig(config, path).forEach(this::registerRecipe);
    }

    public void registerRecipe(CustomRecipe recipe) {
        registry.register(recipe);
        registerBukkitRecipe(recipe);
    }

    public void unregisterRecipe(String id) {
        registry.getById(id).ifPresent(recipe -> {
            if (recipe.getKey() != null) {
                Bukkit.removeRecipe(recipe.getKey());
            }
        });
        registry.unregister(id);
    }

    public void unregisterAllRecipes() {
        registry.getAll().forEach(recipe -> {
            if (recipe.getKey() != null) {
                Bukkit.removeRecipe(recipe.getKey());
            }
        });
        registry.clear();
    }

    public void reload() {
        unregisterAllRecipes();
    }

    private void registerBukkitRecipe(CustomRecipe recipe) {
        ItemStack resultItem = recipe.getResult();
        if (resultItem == null) return;

        Recipe bukkitRecipe = createBukkitRecipe(recipe, resultItem);
        if (bukkitRecipe != null) {
            try {
                Bukkit.addRecipe(bukkitRecipe);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to register recipe '" + recipe.getId() + "': " + e.getMessage());
            }
        }
    }

    private Recipe createBukkitRecipe(CustomRecipe recipe, ItemStack result) {
        switch (recipe.getType()) {
            case SHAPED:
                return createShapedRecipe(recipe, result);
            case SHAPELESS:
                return createShapelessRecipe(recipe, result);
            default:
                return null;
        }
    }

    private ShapedRecipe createShapedRecipe(CustomRecipe recipe, ItemStack result) {
        ShapedRecipe shaped = new ShapedRecipe(recipe.getKey(), result);
        shaped.shape(recipe.getShape().toArray(new String[0]));

        recipe.getIngredientMap().forEach((key, ingredient) -> {
            if (!ingredient.isAir()) {
                ItemStack item = ingredient.getItemStack();
                if (item != null) {
                    shaped.setIngredient(key, item.getType());
                }
            }
        });

        if (!recipe.getGroup().isEmpty()) {
            shaped.setGroup(recipe.getGroup());
        }

        return shaped;
    }

    private ShapelessRecipe createShapelessRecipe(CustomRecipe recipe, ItemStack result) {
        ShapelessRecipe shapeless = new ShapelessRecipe(recipe.getKey(), result);

        recipe.getShapelessIngredients().forEach(ingredient -> {
            if (!ingredient.isAir()) {
                ItemStack item = ingredient.getItemStack();
                if (item != null) {
                    shapeless.addIngredient(ingredient.getAmount(), item.getType());
                }
            }
        });

        if (!recipe.getGroup().isEmpty()) {
            shapeless.setGroup(recipe.getGroup());
        }

        return shapeless;
    }

    public CraftingResult processCraft(CraftingInventory inventory, Player player) {
        for (CustomRecipe recipe : registry.getEnabled()) {
            if (!recipe.isEnabled()) continue;

            if (recipe.getPermission() != null && !player.hasPermission(recipe.getPermission())) {
                continue;
            }

            if (matcher.matches(recipe, inventory)) {
                ItemStack result = recipe.getResult();
                if (result != null) {
                    return CraftingResult.success(result, recipe);
                }
            }
        }

        return CraftingResult.failure("No matching recipe found");
    }

    public Optional<CustomRecipe> findMatchingRecipe(CraftingInventory inventory) {
        return registry.getEnabled().stream()
                .filter(recipe -> matcher.matches(recipe, inventory))
                .findFirst();
    }

    public Collection<CustomRecipe> getAllRecipes() {
        return registry.getAll();
    }

    public Optional<CustomRecipe> getRecipe(String id) {
        return registry.getById(id);
    }

    public static String itemToBase64(ItemStack item) {
        return RecipeIngredient.toBase64(item);
    }

    public static ItemStack itemFromBase64(String base64) {
        return RecipeIngredient.fromBase64(base64);
    }
}
