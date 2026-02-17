package net.exylia.commons.v2.crafting.processor;

import net.exylia.commons.v2.crafting.model.CustomRecipe;
import net.exylia.commons.v2.crafting.model.RecipeIngredient;
import net.exylia.commons.v2.crafting.model.RecipeType;
import org.bukkit.Material;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class IngredientMatcher {

    public boolean matches(CustomRecipe recipe, CraftingInventory inventory) {
        ItemStack[] matrix = inventory.getMatrix();

        if (recipe.getType() == RecipeType.SHAPED) {
            return matchesShaped(recipe, matrix);
        } else {
            return matchesShapeless(recipe, matrix);
        }
    }

    private boolean matchesShaped(CustomRecipe recipe, ItemStack[] matrix) {
        List<String> shape = recipe.getShape();
        int width = recipe.getShapeWidth();
        int height = recipe.getShapeHeight();

        int matrixSize = (int) Math.sqrt(matrix.length);

        for (int offsetY = 0; offsetY <= matrixSize - height; offsetY++) {
            for (int offsetX = 0; offsetX <= matrixSize - width; offsetX++) {
                if (matchesAtOffset(recipe, matrix, matrixSize, offsetX, offsetY, false)) {
                    return true;
                }
                if (matchesAtOffset(recipe, matrix, matrixSize, offsetX, offsetY, true)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean matchesAtOffset(CustomRecipe recipe, ItemStack[] matrix, int matrixSize,
                                    int offsetX, int offsetY, boolean mirrored) {
        List<String> shape = recipe.getShape();
        int width = recipe.getShapeWidth();
        int height = recipe.getShapeHeight();

        for (int y = 0; y < matrixSize; y++) {
            for (int x = 0; x < matrixSize; x++) {
                int matrixIndex = y * matrixSize + x;
                ItemStack item = matrix[matrixIndex];

                int shapeY = y - offsetY;
                int shapeX = mirrored ? (width - 1 - (x - offsetX)) : (x - offsetX);

                RecipeIngredient expected = null;

                if (shapeY >= 0 && shapeY < height && shapeX >= 0 && shapeX < width) {
                    char key = shape.get(shapeY).charAt(shapeX);
                    expected = recipe.getIngredientMap().get(key);
                }

                if (expected == null || expected.isAir()) {
                    if (item != null && item.getType() != Material.AIR) {
                        return false;
                    }
                } else {
                    if (!expected.matches(item)) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    private boolean matchesShapeless(CustomRecipe recipe, ItemStack[] matrix) {
        List<RecipeIngredient> ingredients = new ArrayList<>(recipe.getShapelessIngredients());
        List<ItemStack> matrixItems = new ArrayList<>();

        for (ItemStack item : matrix) {
            if (item != null && item.getType() != Material.AIR) {
                matrixItems.add(item);
            }
        }

        if (matrixItems.size() != ingredients.size()) {
            return false;
        }

        boolean[] used = new boolean[ingredients.size()];

        for (ItemStack item : matrixItems) {
            boolean found = false;
            for (int i = 0; i < ingredients.size(); i++) {
                if (!used[i] && ingredients.get(i).matches(item)) {
                    used[i] = true;
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }

        return true;
    }
}
