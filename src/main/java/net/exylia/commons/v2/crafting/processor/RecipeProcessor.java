package net.exylia.commons.v2.crafting.processor;

import net.exylia.commons.v2.config.Config;
import net.exylia.commons.v2.crafting.model.CustomRecipe;
import net.exylia.commons.v2.crafting.model.RecipeIngredient;
import net.exylia.commons.v2.crafting.model.RecipeType;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecipeProcessor {

    private final JavaPlugin plugin;

    public RecipeProcessor(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public List<CustomRecipe> processConfig(Config config, String recipesPath) {
        List<CustomRecipe> recipes = new ArrayList<>();
        ConfigurationSection section = config.section(recipesPath);

        if (section == null) return recipes;

        for (String recipeId : section.getKeys(false)) {
            try {
                CustomRecipe recipe = processRecipe(recipeId, config, recipesPath + "." + recipeId);
                if (recipe != null) {
                    recipes.add(recipe);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Error processing recipe '" + recipeId + "': " + e.getMessage());
            }
        }

        return recipes;
    }

    public CustomRecipe processRecipe(String id, Config config, String path) {
        boolean enabled = config.bool(path + ".enabled", true);
        String typeStr = config.string(path + ".type", "SHAPED").toUpperCase();
        RecipeType type = RecipeType.valueOf(typeStr);

        CustomRecipe.CustomRecipeBuilder builder = CustomRecipe.builder()
                .id(id)
                .key(new NamespacedKey(plugin, id.toLowerCase().replace(" ", "_")))
                .type(type)
                .enabled(enabled)
                .permission(config.string(path + ".permission", null))
                .group(config.string(path + ".group", ""));

        if (type == RecipeType.SHAPED) {
            List<String> shape = config.stringList(path + ".shape");
            builder.shape(shape);

            Map<Character, RecipeIngredient> ingredientMap = new HashMap<>();
            ConfigurationSection ingredientsSection = config.section(path + ".ingredients");

            if (ingredientsSection != null) {
                for (String key : ingredientsSection.getKeys(false)) {
                    char keyChar = key.charAt(0);
                    RecipeIngredient ingredient = processIngredient(config, path + ".ingredients." + key);
                    ingredient.setId(key);
                    ingredientMap.put(keyChar, ingredient);
                }
            }
            builder.ingredientMap(ingredientMap);
        } else {
            List<RecipeIngredient> shapelessIngredients = new ArrayList<>();
            ConfigurationSection ingredientsSection = config.section(path + ".ingredients");

            if (ingredientsSection != null) {
                for (String key : ingredientsSection.getKeys(false)) {
                    RecipeIngredient ingredient = processIngredient(config, path + ".ingredients." + key);
                    ingredient.setId(key);
                    shapelessIngredients.add(ingredient);
                }
            }
            builder.shapelessIngredients(shapelessIngredients);
        }

        builder.resultBase64(config.string(path + ".result", null));
        builder.resultAmount(config.integer(path + ".result-amount", 1));

        return builder.build();
    }

    private RecipeIngredient processIngredient(Config config, String path) {
        String base64 = config.string(path + ".base64", config.string(path, null));
        int amount = config.integer(path + ".amount", 1);

        return RecipeIngredient.builder()
                .base64(base64)
                .amount(amount)
                .build();
    }
}
