package net.exylia.commons.v2.ui.animation.impl;

import net.exylia.commons.v2.ui.animation.AnimationType;
import net.exylia.commons.v2.ui.animation.MenuAnimation;

import java.util.ArrayList;
import java.util.List;

public class ColumnsAlternateAnimation implements MenuAnimation {

    @Override
    public AnimationType getType() {
        return AnimationType.COLUMNS_ALTERNATE;
    }

    @Override
    public List<List<Integer>> calculateFrames(int rows, int cols) {
        List<List<Integer>> frames = new ArrayList<>();

        for (int step = 0; step < rows; step++) {
            List<Integer> frameSlots = new ArrayList<>();

            for (int col = 0; col < cols; col++) {
                int row;
                if (col % 2 == 0) {
                    row = step;
                } else {
                    row = rows - 1 - step;
                }

                if (row >= 0 && row < rows) {
                    frameSlots.add(row * cols + col);
                }
            }

            if (!frameSlots.isEmpty()) {
                frames.add(frameSlots);
            }
        }

        return frames;
    }
}
