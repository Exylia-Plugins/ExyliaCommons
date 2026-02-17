package net.exylia.commons.v2.crafting.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class RecipeIngredient {

    private String id;

    private String base64;

    private transient ItemStack cachedItem;

    @Builder.Default
    private int amount = 1;

    public boolean isAir() {
        return base64 == null || base64.isEmpty();
    }

    public ItemStack getItemStack() {
        if (cachedItem != null) return cachedItem.clone();
        if (base64 == null || base64.isEmpty()) return null;

        cachedItem = fromBase64(base64);
        return cachedItem != null ? cachedItem.clone() : null;
    }

    public boolean matches(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) {
            return isAir();
        }

        if (isAir()) {
            return false;
        }

        ItemStack expected = getItemStack();
        if (expected == null) return false;

        if (stack.getType() != expected.getType()) {
            return false;
        }

        if (stack.getAmount() < amount) {
            return false;
        }

        return stack.isSimilar(expected);
    }

    public static String toBase64(ItemStack item) {
        if (item == null) return null;
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream)) {
            dataOutput.writeObject(item);
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception e) {
            return null;
        }
    }

    public static ItemStack fromBase64(String base64) {
        if (base64 == null || base64.isEmpty()) return null;
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(base64));
             BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream)) {
            return (ItemStack) dataInput.readObject();
        } catch (Exception e) {
            return null;
        }
    }

    public static RecipeIngredient fromItemStack(ItemStack item) {
        return RecipeIngredient.builder()
                .base64(toBase64(item))
                .amount(item != null ? item.getAmount() : 1)
                .cachedItem(item)
                .build();
    }
}
