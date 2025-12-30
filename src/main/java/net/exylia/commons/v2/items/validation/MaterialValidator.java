package net.exylia.commons.v2.items.validation;

import net.exylia.commons.v2.items.exception.ItemValidationException;
import net.exylia.commons.v2.items.utils.PlaceholderDetector;
import org.bukkit.Material;

public class MaterialValidator {

    public static void validate(String rawMaterial) throws ItemValidationException {
        if (rawMaterial == null || rawMaterial.trim().isEmpty()) {
            throw new ItemValidationException("Material cannot be null or empty");
        }

        if (PlaceholderDetector.contains(rawMaterial)) {
            return;
        }

        String materialUpper = rawMaterial.toUpperCase();

        if (materialUpper.startsWith("PLAYERHEAD:") || materialUpper.startsWith("PLAYERHEAD-") ||
            materialUpper.startsWith("BASEHEAD:") || materialUpper.startsWith("BASEHEAD-") ||
            materialUpper.startsWith("URLHEAD:") || materialUpper.startsWith("URLHEAD-") ||
            materialUpper.startsWith("HEADBASE:") || materialUpper.startsWith("HEADBASE-") ||
            materialUpper.startsWith("HEADBASE-") || materialUpper.startsWith("HEADURL-")) {
            return;
        }

        if (rawMaterial.contains(":")) {
            return;
        }

        try {
            Material.valueOf(materialUpper);
        } catch (IllegalArgumentException e) {
            throw new ItemValidationException("Invalid material: " + rawMaterial, e);
        }
    }
}
