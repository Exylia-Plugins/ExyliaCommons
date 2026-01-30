package net.exylia.commons.v2.ui.animation.impl;

import net.exylia.commons.v2.ui.animation.AnimationType;
import net.exylia.commons.v2.ui.animation.MenuAnimation;

import java.util.ArrayList;
import java.util.List;

public class CheckerboardAnimation implements MenuAnimation {

    private static final int SLOTS_PER_FRAME = 4;

    @Override
    public AnimationType getType() {
        return AnimationType.CHECKERBOARD;
    }

    @Override
    public List<List<Integer>> calculateFrames(int rows, int cols) {
        List<Integer> evenSlots = new ArrayList<>();
        List<Integer> oddSlots = new ArrayList<>();

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int slot = row * cols + col;
                if ((row + col) % 2 == 0) {
                    evenSlots.add(slot);
                } else {
                    oddSlots.add(slot);
                }
            }
        }

        List<List<Integer>> frames = new ArrayList<>();

        for (int i = 0; i < evenSlots.size(); i += SLOTS_PER_FRAME) {
            int end = Math.min(i + SLOTS_PER_FRAME, evenSlots.size());
            frames.add(new ArrayList<>(evenSlots.subList(i, end)));
        }

        for (int i = 0; i < oddSlots.size(); i += SLOTS_PER_FRAME) {
            int end = Math.min(i + SLOTS_PER_FRAME, oddSlots.size());
            frames.add(new ArrayList<>(oddSlots.subList(i, end)));
        }

        return frames;
    }
}
