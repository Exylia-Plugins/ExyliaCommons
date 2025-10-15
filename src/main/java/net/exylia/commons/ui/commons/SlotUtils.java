package net.exylia.commons.ui.commons;

public class SlotUtils {

    public static int[] getBorderSlots(int rows) {
        if (rows <= 1) return new int[0];

        int size = rows * 9;
        int[] slots = new int[rows * 2 + (rows - 2) * 2];
        int index = 0;

        for (int i = 0; i < 9; i++) {
            slots[index++] = i;
            slots[index++] = size - 9 + i;
        }

        for (int row = 1; row < rows - 1; row++) {
            slots[index++] = row * 9;       
            slots[index++] = row * 9 + 8;   
        }

        return slots;
    }

    public static int[] getCenterSlots(int rows) {
        if (rows <= 2) return new int[]{4};  

        int[] slots = new int[(rows - 2) * 7];
        int index = 0;

        for (int row = 1; row < rows - 1; row++) {
            for (int col = 1; col < 8; col++) {
                slots[index++] = row * 9 + col;
            }
        }

        return slots;
    }

    public static int[] getPatternSlots(int rows, SlotPattern pattern) {
        return switch (pattern) {
            case BORDER -> getBorderSlots(rows);
            case CENTER -> getCenterSlots(rows);
            case CHECKERBOARD -> getCheckerboardSlots(rows);
            case CORNERS -> getCornerSlots(rows);
            case EDGES -> getEdgeSlots(rows);
        };
    }

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

    public static int[] getCornerSlots(int rows) {
        if (rows == 1) return new int[]{0, 8};

        return new int[]{
                0,                     
                8,                     
                (rows - 1) * 9,        
                (rows - 1) * 9 + 8     
        };
    }

    public static int[] getEdgeSlots(int rows) {
        int[] slots = new int[rows * 2];
        int index = 0;

        for (int row = 0; row < rows; row++) {
            slots[index++] = row * 9;       
            slots[index++] = row * 9 + 8;   
        }

        return slots;
    }

    public static int toSlot(int row, int col) {
        return row * 9 + col;
    }

    public static int toRow(int slot) {
        return slot / 9;
    }

    public static int toColumn(int slot) {
        return slot % 9;
    }

    public static boolean isBorderSlot(int slot, int rows) {
        int row = toRow(slot);
        int col = toColumn(slot);

        return row == 0 || row == rows - 1 || col == 0 || col == 8;
    }

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
                 
                String[] range = part.split("-");
                if (range.length == 2) {
                    try {
                        int start = Integer.parseInt(range[0].trim());
                        int end = Integer.parseInt(range[1].trim());

                        for (int i = Math.max(0, start); i <= Math.min(maxSlot, end); i++) {
                            slots.add(i);
                        }
                    } catch (NumberFormatException ignored) {
                         
                    }
                }
            } else {
                 
                try {
                    int slot = Integer.parseInt(part);
                    if (slot >= 0 && slot <= maxSlot) {
                        slots.add(slot);
                    }
                } catch (NumberFormatException ignored) {
                     
                }
            }
        }

        return slots.stream().mapToInt(Integer::intValue).toArray();
    }
}
