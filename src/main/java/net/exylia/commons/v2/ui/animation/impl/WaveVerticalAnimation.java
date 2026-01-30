package net.exylia.commons.v2.ui.animation.impl;

import net.exylia.commons.v2.ui.animation.AnimationType;
import net.exylia.commons.v2.ui.animation.MenuAnimation;

import java.util.ArrayList;
import java.util.List;

public class WaveVerticalAnimation implements MenuAnimation {

    @Override
    public AnimationType getType() {
        return AnimationType.WAVE_VERTICAL;
    }

    @Override
    public List<List<Integer>> calculateFrames(int rows, int cols) {
        List<List<Integer>> frames = new ArrayList<>();

        for (int row = 0; row < rows; row++) {
            List<Integer> frameSlots = new ArrayList<>();

            for (int col = 0; col < cols; col++) {
                int actualRow = (col % 2 == 0) ? row : (rows - 1 - row);
                frameSlots.add(actualRow * cols + col);
            }

            frames.add(frameSlots);
        }

        return frames;
    }
}
