package net.exylia.commons.v2.ui.animation.impl;

import net.exylia.commons.v2.ui.animation.AnimationType;
import net.exylia.commons.v2.ui.animation.MenuAnimation;

import java.util.ArrayList;
import java.util.List;

public class RowsAlternateAnimation implements MenuAnimation {

    @Override
    public AnimationType getType() {
        return AnimationType.ROWS_ALTERNATE;
    }

    @Override
    public List<List<Integer>> calculateFrames(int rows, int cols) {
        List<List<Integer>> frames = new ArrayList<>();

        int maxSteps = Math.max(cols, cols);

        for (int step = 0; step < cols; step++) {
            List<Integer> frameSlots = new ArrayList<>();

            for (int row = 0; row < rows; row++) {
                int col;
                if (row % 2 == 0) {
                    col = step;
                } else {
                    col = cols - 1 - step;
                }

                if (col >= 0 && col < cols) {
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
