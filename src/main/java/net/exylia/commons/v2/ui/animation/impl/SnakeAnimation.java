package net.exylia.commons.v2.ui.animation.impl;

import net.exylia.commons.v2.ui.animation.AnimationType;
import net.exylia.commons.v2.ui.animation.MenuAnimation;

import java.util.ArrayList;
import java.util.List;

public class SnakeAnimation implements MenuAnimation {

    private static final int SLOTS_PER_FRAME = 2;

    @Override
    public AnimationType getType() {
        return AnimationType.SNAKE;
    }

    @Override
    public List<List<Integer>> calculateFrames(int rows, int cols) {
        List<Integer> snakeOrder = new ArrayList<>();

        for (int row = 0; row < rows; row++) {
            if (row % 2 == 0) {
                for (int col = 0; col < cols; col++) {
                    snakeOrder.add(row * cols + col);
                }
            } else {
                for (int col = cols - 1; col >= 0; col--) {
                    snakeOrder.add(row * cols + col);
                }
            }
        }

        List<List<Integer>> frames = new ArrayList<>();
        for (int i = 0; i < snakeOrder.size(); i += SLOTS_PER_FRAME) {
            int end = Math.min(i + SLOTS_PER_FRAME, snakeOrder.size());
            frames.add(new ArrayList<>(snakeOrder.subList(i, end)));
        }

        return frames;
    }
}
