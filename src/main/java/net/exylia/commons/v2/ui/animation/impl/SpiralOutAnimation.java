package net.exylia.commons.v2.ui.animation.impl;

import net.exylia.commons.v2.ui.animation.AnimationType;
import net.exylia.commons.v2.ui.animation.MenuAnimation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SpiralOutAnimation implements MenuAnimation {

    @Override
    public AnimationType getType() {
        return AnimationType.SPIRAL_OUT;
    }

    @Override
    public List<List<Integer>> calculateFrames(int rows, int cols) {
        SpiralAnimation spiral = new SpiralAnimation();
        List<List<Integer>> frames = new ArrayList<>(spiral.calculateFrames(rows, cols));
        Collections.reverse(frames);
        return frames;
    }
}
