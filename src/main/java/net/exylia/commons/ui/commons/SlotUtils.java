package net.exylia.commons.ui.commons;

/**
 * Utility class for working with inventory slots
 */
public class SlotUtils {

    /**
     * Gets all border slots for a menu
     * @param rows Number of rows
     * @return Array of border slots
     */
    public static int[] getBorderSlots(int rows) {
        if (rows <= 1) return new int[0];

        int size = rows * 9;
        int[] slots = new int[rows * 2 + (rows - 2) * 2];
        int index = 0;

        // Top and bottom rows
        for (int i = 0; i < 9; i++) {
            slots[index++] = i;
            slots[index++] = size - 9 + i;
        }

        // Side columns (excluding corners already added)
        for (int row = 1; row < rows - 1; row++) {
            slots[index++] = row * 9;      // Left side
            slots[index++] = row * 9 + 8;  // Right side
        }

        return slots;
    }

    /**
     * Gets all center slots for a menu (excluding borders)
     * @param rows Number of rows
     * @return Array of center slots
     */
    public static int[] getCenterSlots(int rows) {
        if (rows <= 2) return new int[]{4}; // Center slot for small menus

        int[] slots = new int[(rows - 2) * 7];
        int index = 0;

        for (int row = 1; row < rows - 1; row++) {
            for (int col = 1; col < 8; col++) {
                slots[index++] = row * 9 + col;
            }
        }

        return slots;
    }

    /**
     * Gets a specific pattern of slots
     * @param rows Number of rows
     * @param pattern The pattern type
     * @return Array of slots matching the pattern
     */
    public static int[] getPatternSlots(int rows, SlotPattern pattern) {
        return switch (pattern) {
            case BORDER -> getBorderSlots(rows);
            case CENTER -> getCenterSlots(rows);
            case CHECKERBOARD -> getCheckerboardSlots(rows);
            case CORNERS -> getCornerSlots(rows);
            case EDGES -> getEdgeSlots(rows);
        };
    }

    /**
     * Gets checkerboard pattern slots
     * @param rows Number of rows
     * @return Array of checkerboard slots
     */
    public static int[] getCheckerboardSlots(int rows) {
        int[] slots = new int[(rows * 9) / 2];
        int index = 0;

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < 9; col++) {
                if ((row + col) % 2 == 0) {
                    slots[index++] = row * 9 + col;
                }
            }
        }

        return java.util.Arrays.copyOf(slots, index);
    }

    /**
     * Gets corner slots
     * @param rows Number of rows
     * @return Array of corner slots
     */
    public static int[] getCornerSlots(int rows) {
        if (rows == 1) return new int[]{0, 8};

        return new int[]{
                0,                    // Top-left
                8,                    // Top-right
                (rows - 1) * 9,       // Bottom-left
                (rows - 1) * 9 + 8    // Bottom-right
        };
    }

    /**
     * Gets edge slots (first and last columns)
     * @param rows Number of rows
     * @return Array of edge slots
     */
    public static int[] getEdgeSlots(int rows) {
        int[] slots = new int[rows * 2];
        int index = 0;

        for (int row = 0; row < rows; row++) {
            slots[index++] = row * 9;      // Left edge
            slots[index++] = row * 9 + 8;  // Right edge
        }

        return slots;
    }

    /**
     * Converts row and column to slot index
     * @param row The row (0-based)
     * @param col The column (0-based)
     * @return The slot index
     */
    public static int toSlot(int row, int col) {
        return row * 9 + col;
    }

    /**
     * Converts slot index to row
     * @param slot The slot index
     * @return The row (0-based)
     */
    public static int toRow(int slot) {
        return slot / 9;
    }

    /**
     * Converts slot index to column
     * @param slot The slot index
     * @return The column (0-based)
     */
    public static int toColumn(int slot) {
        return slot % 9;
    }

    /**
     * Checks if a slot is in the border
     * @param slot The slot index
     * @param rows Number of rows
     * @return True if the slot is a border slot
     */
    public static boolean isBorderSlot(int slot, int rows) {
        int row = toRow(slot);
        int col = toColumn(slot);

        return row == 0 || row == rows - 1 || col == 0 || col == 8;
    }

    /**
     * Parses a slot string into an array of slot indices
     * @param slotsString The slot string (e.g., "10-16,19-25,28-34")
     * @param rows Number of rows for validation
     * @return Array of slot indices
     */
    public static int[] parseSlots(String slotsString, int rows) {
        if (slotsString == null || slotsString.isEmpty()) {
            return new int[0];
        }

        java.util.List<Integer> slots = new java.util.ArrayList<>();
        int maxSlot = rows * 9 - 1;

        String[] parts = slotsString.split(",");
        for (String part : parts) {
            part = part.trim();

            if (part.contains("-")) {
                // Range: "10-16"
                String[] range = part.split("-");
                if (range.length == 2) {
                    try {
                        int start = Integer.parseInt(range[0].trim());
                        int end = Integer.parseInt(range[1].trim());

                        for (int i = Math.max(0, start); i <= Math.min(maxSlot, end); i++) {
                            slots.add(i);
                        }
                    } catch (NumberFormatException ignored) {
                        // Skip invalid ranges
                    }
                }
            } else {
                // Single slot
                try {
                    int slot = Integer.parseInt(part);
                    if (slot >= 0 && slot <= maxSlot) {
                        slots.add(slot);
                    }
                } catch (NumberFormatException ignored) {
                    // Skip invalid slots
                }
            }
        }

        return slots.stream().mapToInt(Integer::intValue).toArray();
    }
}