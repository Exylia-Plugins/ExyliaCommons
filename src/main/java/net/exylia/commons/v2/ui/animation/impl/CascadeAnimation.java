package net.exylia.commons.v2.ui.animation.impl;

import net.exylia.commons.v2.ui.animation.AnimationType;
import net.exylia.commons.v2.ui.animation.MenuAnimation;

import java.util.ArrayList;
import java.util.List;

public class CascadeAnimation implements MenuAnimation {

    @Override
    public AnimationType getType() {
        return AnimationType.CASCADE;
    }

    @Override
    public List<List<Integer>> calculateFrames(int rows, int cols) {
        List<List<Integer>> frames = new ArrayList<>();
        int maxDiagonal = rows + cols - 1;

        for (int d = 0; d < maxDiagonal; d++) {
            List<Integer> frameSlots = new ArrayList<>();
            for (int row = 0; row < rows; row++) {
                int col = d - row;
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
