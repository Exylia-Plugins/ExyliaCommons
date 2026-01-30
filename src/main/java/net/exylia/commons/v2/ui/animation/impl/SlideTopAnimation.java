package net.exylia.commons.v2.ui.animation.impl;

import net.exylia.commons.v2.ui.animation.AnimationType;
import net.exylia.commons.v2.ui.animation.MenuAnimation;

import java.util.ArrayList;
import java.util.List;

public class SlideTopAnimation implements MenuAnimation {

    @Override
    public AnimationType getType() {
        return AnimationType.SLIDE_TOP;
    }

    @Override
    public List<List<Integer>> calculateFrames(int rows, int cols) {
        List<List<Integer>> frames = new ArrayList<>();

        for (int row = 0; row < rows; row++) {
            List<Integer> frameSlots = new ArrayList<>();
            for (int col = 0; col < cols; col++) {
                frameSlots.add(row * cols + col);
            }
            frames.add(frameSlots);
        }

        return frames;
    }
}
