package net.exylia.commons.v2.ui.animation.impl;

import net.exylia.commons.v2.ui.animation.AnimationType;
import net.exylia.commons.v2.ui.animation.MenuAnimation;

import java.util.ArrayList;
import java.util.List;

public class WaveHorizontalAnimation implements MenuAnimation {

    @Override
    public AnimationType getType() {
        return AnimationType.WAVE_HORIZONTAL;
    }

    @Override
    public List<List<Integer>> calculateFrames(int rows, int cols) {
        List<List<Integer>> frames = new ArrayList<>();

        for (int col = 0; col < cols; col++) {
            List<Integer> frameSlots = new ArrayList<>();

            for (int row = 0; row < rows; row++) {
                int actualCol = (row % 2 == 0) ? col : (cols - 1 - col);
                frameSlots.add(row * cols + actualCol);
            }

            frames.add(frameSlots);
        }

        return frames;
    }
}
