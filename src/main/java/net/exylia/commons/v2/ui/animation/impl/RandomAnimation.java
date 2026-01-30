package net.exylia.commons.v2.ui.animation.impl;

import net.exylia.commons.v2.ui.animation.AnimationType;
import net.exylia.commons.v2.ui.animation.MenuAnimation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RandomAnimation implements MenuAnimation {

    private static final int SLOTS_PER_FRAME = 3;

    @Override
    public AnimationType getType() {
        return AnimationType.RANDOM;
    }

    @Override
    public List<List<Integer>> calculateFrames(int rows, int cols) {
        List<Integer> allSlots = new ArrayList<>();
        for (int i = 0; i < rows * cols; i++) {
            allSlots.add(i);
        }
        Collections.shuffle(allSlots);

        List<List<Integer>> frames = new ArrayList<>();
        for (int i = 0; i < allSlots.size(); i += SLOTS_PER_FRAME) {
            int end = Math.min(i + SLOTS_PER_FRAME, allSlots.size());
            frames.add(new ArrayList<>(allSlots.subList(i, end)));
        }

        return frames;
    }
}
