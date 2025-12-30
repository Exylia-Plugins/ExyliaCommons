package net.exylia.commons.v2.items.validation;

import net.exylia.commons.v2.items.config.LeatherArmorConfig;
import net.exylia.commons.v2.items.exception.ItemValidationException;
import net.exylia.commons.v2.items.model.ItemData;
import net.exylia.commons.v2.items.utils.PlaceholderDetector;

public class ItemValidator {

    private static final int MIN_AMOUNT = 1;
    private static final int MAX_AMOUNT = 64;
    private static final int MIN_RGB = 0;
    private static final int MAX_RGB = 255;

    public static void validate(ItemData itemData) throws ItemValidationException {
        if (itemData == null) {
            throw new ItemValidationException("ItemData cannot be null");
        }

        MaterialValidator.validate(itemData.getRawMaterial());

        if (itemData.getRawAmount() != null) {
            validateAmount(itemData.getRawAmount());
        }

        if (itemData.getSlotConfig() != null) {
            SlotValidator.validate(itemData.getSlotConfig());
        }

        if (itemData.getLeatherArmorConfig() != null) {
            validateLeatherColor(itemData.getLeatherArmorConfig());
        }

        if (itemData.getMaxStackSize() > 0) {
            validateMaxStackSize(itemData.getMaxStackSize());
        }
    }

    private static void validateAmount(String rawAmount) throws ItemValidationException {
        if (PlaceholderDetector.contains(rawAmount)) {
            return;
        }

        try {
            int amount = Integer.parseInt(rawAmount);
            if (amount < MIN_AMOUNT || amount > MAX_AMOUNT) {
                throw new ItemValidationException("Amount must be between " + MIN_AMOUNT + " and " + MAX_AMOUNT + ", got: " + amount);
            }
        } catch (NumberFormatException ignored) {
        }
    }

    private static void validateLeatherColor(LeatherArmorConfig config) throws ItemValidationException {
        if (config.getR() < MIN_RGB || config.getR() > MAX_RGB) {
            throw new ItemValidationException("Red RGB value must be between " + MIN_RGB + " and " + MAX_RGB + ", got: " + config.getR());
        }
        if (config.getG() < MIN_RGB || config.getG() > MAX_RGB) {
            throw new ItemValidationException("Green RGB value must be between " + MIN_RGB + " and " + MAX_RGB + ", got: " + config.getG());
        }
        if (config.getB() < MIN_RGB || config.getB() > MAX_RGB) {
            throw new ItemValidationException("Blue RGB value must be between " + MIN_RGB + " and " + MAX_RGB + ", got: " + config.getB());
        }
    }

    private static void validateMaxStackSize(int maxStackSize) throws ItemValidationException {
        if (maxStackSize < MIN_AMOUNT || maxStackSize > 99) {
            throw new ItemValidationException("Max stack size must be between " + MIN_AMOUNT + " and 99, got: " + maxStackSize);
        }
    }
}
