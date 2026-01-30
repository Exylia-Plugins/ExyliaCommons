package net.exylia.commons.v2.ui.animation.impl;

import net.exylia.commons.v2.ui.animation.AnimationType;
import net.exylia.commons.v2.ui.animation.MenuAnimation;

import java.util.ArrayList;
import java.util.List;

public class TypewriterAnimation implements MenuAnimation {

    @Override
    public AnimationType getType() {
        return AnimationType.TYPEWRITER;
    }

    @Override
    public List<List<Integer>> calculateFrames(int rows, int cols) {
        List<List<Integer>> frames = new ArrayList<>();

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                List<Integer> frame = new ArrayList<>();
                frame.add(row * cols + col);
                frames.add(frame);
            }
        }

        return frames;
    }
}
