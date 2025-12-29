package net.exylia.commons.v2.items.config;

import lombok.Getter;
import net.exylia.commons.v2.items.exception.ItemValidationException;
import net.exylia.commons.v2.items.utils.PlaceholderDetector;

import java.util.List;

@Getter
public class SlotConfig {

    private final Integer singleSlot;
    private final List<Integer> multiSlots;
    private final String rawSlot;
    private final List<String> rawSlots;

    private SlotConfig(Integer singleSlot, List<Integer> multiSlots, String rawSlot, List<String> rawSlots) {
        this.singleSlot = singleSlot;
        this.multiSlots = multiSlots;
        this.rawSlot = rawSlot;
        this.rawSlots = rawSlots;
    }

    public static SlotConfig single(int slot) {
        return new SlotConfig(slot, null, String.valueOf(slot), null);
    }

    public static SlotConfig singleRaw(String rawSlot) {
        return new SlotConfig(null, null, rawSlot, null);
    }

    public static SlotConfig multiple(List<Integer> slots) {
        return new SlotConfig(null, slots, null, null);
    }

    public static SlotConfig multipleRaw(List<String> rawSlots) {
        return new SlotConfig(null, null, null, rawSlots);
    }

    public boolean isSingle() {
        return singleSlot != null || rawSlot != null;
    }

    public boolean isMultiple() {
        return multiSlots != null || rawSlots != null;
    }

    public boolean hasPlaceholders() {
        if (rawSlot != null) {
            return PlaceholderDetector.contains(rawSlot);
        }
        if (rawSlots != null) {
            return rawSlots.stream().anyMatch(PlaceholderDetector::contains);
        }
        return false;
    }

    public Integer getSingleSlot() throws ItemValidationException {
        if (!isSingle()) {
            throw new ItemValidationException("SlotConfig is not configured as single slot");
        }
        return singleSlot;
    }

    public List<Integer> getMultiSlots() throws ItemValidationException {
        if (!isMultiple()) {
            throw new ItemValidationException("SlotConfig is not configured as multiple slots");
        }
        return multiSlots;
    }
}
