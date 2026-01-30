package net.exylia.commons.v2.ui.animation.impl;

import net.exylia.commons.v2.ui.animation.AnimationType;
import net.exylia.commons.v2.ui.animation.MenuAnimation;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class NoneAnimation implements MenuAnimation {

    @Override
    public AnimationType getType() {
        return AnimationType.NONE;
    }

    @Override
    public List<List<Integer>> calculateFrames(int rows, int cols) {
        List<List<Integer>> frames = new ArrayList<>();
        List<Integer> allSlots = new ArrayList<>();
        IntStream.range(0, rows * cols).forEach(allSlots::add);
        frames.add(allSlots);
        return frames;
    }
}
