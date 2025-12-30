package net.exylia.commons.v2.items.validation;

import net.exylia.commons.v2.items.config.SlotConfig;
import net.exylia.commons.v2.items.exception.ItemValidationException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SlotValidator {

    private static final int MIN_SLOT = 0;
    private static final int MAX_SLOT = 53;

    public static void validateSlot(int slot) throws ItemValidationException {
        if (slot < MIN_SLOT || slot > MAX_SLOT) {
            throw new ItemValidationException("Slot must be between " + MIN_SLOT + " and " + MAX_SLOT + ", got: " + slot);
        }
    }

    public static void validateSlots(List<Integer> slots) throws ItemValidationException {
        if (slots == null || slots.isEmpty()) {
            throw new ItemValidationException("Slots list cannot be empty");
        }

        Set<Integer> seen = new HashSet<>();
        for (Integer slot : slots) {
            if (slot == null) {
                throw new ItemValidationException("Slot cannot be null");
            }
            validateSlot(slot);
            if (!seen.add(slot)) {
                throw new ItemValidationException("Duplicate slot found: " + slot);
            }
        }
    }

    public static void validate(SlotConfig slotConfig) throws ItemValidationException {
        if (slotConfig == null) {
            return;
        }

        if (slotConfig.hasPlaceholders()) {
            return;
        }

        if (slotConfig.isSingle()) {
            Integer singleSlot = slotConfig.getSingleSlot();
            if (singleSlot != null) {
                validateSlot(singleSlot);
            }
        } else if (slotConfig.isMultiple()) {
            List<Integer> multiSlots = slotConfig.getMultiSlots();
            if (multiSlots != null) {
                validateSlots(multiSlots);
            }
        }
    }
}
