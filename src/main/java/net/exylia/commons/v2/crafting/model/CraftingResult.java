package net.exylia.commons.v2.crafting.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bukkit.inventory.ItemStack;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CraftingResult {

    @Builder.Default
    private boolean success = false;

    private ItemStack resultItem;

    private CustomRecipe recipe;

    private String failureReason;

    public static CraftingResult success(ItemStack item, CustomRecipe recipe) {
        return CraftingResult.builder()
                .success(true)
                .resultItem(item)
                .recipe(recipe)
                .build();
    }

    public static CraftingResult failure(String reason) {
        return CraftingResult.builder()
                .success(false)
                .failureReason(reason)
                .build();
    }
}
