package net.exylia.commons.v2.crafting.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class CustomRecipe {

    private String id;

    private NamespacedKey key;

    @Builder.Default
    private RecipeType type = RecipeType.SHAPED;

    @Builder.Default
    private List<String> shape = new ArrayList<>();

    @Builder.Default
    private Map<Character, RecipeIngredient> ingredientMap = new HashMap<>();

    @Builder.Default
    private List<RecipeIngredient> shapelessIngredients = new ArrayList<>();

    private String resultBase64;

    private transient ItemStack cachedResult;

    @Builder.Default
    private int resultAmount = 1;

    private String permission;

    @Builder.Default
    private boolean enabled = true;

    @Builder.Default
    private String group = "";

    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    public ItemStack getResult() {
        if (cachedResult != null) return cachedResult.clone();
        if (resultBase64 == null || resultBase64.isEmpty()) return null;

        cachedResult = RecipeIngredient.fromBase64(resultBase64);
        if (cachedResult != null) {
            cachedResult.setAmount(resultAmount);
        }
        return cachedResult != null ? cachedResult.clone() : null;
    }

    public void setResult(ItemStack item) {
        this.cachedResult = item;
        this.resultBase64 = RecipeIngredient.toBase64(item);
        if (item != null) {
            this.resultAmount = item.getAmount();
        }
    }

    public List<RecipeIngredient> getAllIngredients() {
        if (type == RecipeType.SHAPED) {
            return new ArrayList<>(ingredientMap.values());
        }
        return new ArrayList<>(shapelessIngredients);
    }

    public int getShapeWidth() {
        return shape.isEmpty() ? 0 : shape.get(0).length();
    }

    public int getShapeHeight() {
        return shape.size();
    }
}
