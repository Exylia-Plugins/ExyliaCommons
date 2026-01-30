package net.exylia.commons.v2.ui.animation.impl;

import net.exylia.commons.v2.ui.animation.AnimationType;
import net.exylia.commons.v2.ui.animation.MenuAnimation;

import java.util.ArrayList;
import java.util.List;

public class SpiralAnimation implements MenuAnimation {

    private static final int SLOTS_PER_FRAME = 3;

    @Override
    public AnimationType getType() {
        return AnimationType.SPIRAL;
    }

    @Override
    public List<List<Integer>> calculateFrames(int rows, int cols) {
        List<Integer> spiralOrder = new ArrayList<>();
        boolean[][] visited = new boolean[rows][cols];

        int top = 0, bottom = rows - 1, left = 0, right = cols - 1;

        while (top <= bottom && left <= right) {
            for (int col = left; col <= right; col++) {
                if (!visited[top][col]) {
                    spiralOrder.add(top * cols + col);
                    visited[top][col] = true;
                }
            }
            top++;

            for (int row = top; row <= bottom; row++) {
                if (!visited[row][right]) {
                    spiralOrder.add(row * cols + right);
                    visited[row][right] = true;
                }
            }
            right--;

            if (top <= bottom) {
                for (int col = right; col >= left; col--) {
                    if (!visited[bottom][col]) {
                        spiralOrder.add(bottom * cols + col);
                        visited[bottom][col] = true;
                    }
                }
                bottom--;
            }

            if (left <= right) {
                for (int row = bottom; row >= top; row--) {
                    if (!visited[row][left]) {
                        spiralOrder.add(row * cols + left);
                        visited[row][left] = true;
                    }
                }
                left++;
            }
        }

        List<List<Integer>> frames = new ArrayList<>();
        for (int i = 0; i < spiralOrder.size(); i += SLOTS_PER_FRAME) {
            int end = Math.min(i + SLOTS_PER_FRAME, spiralOrder.size());
            frames.add(new ArrayList<>(spiralOrder.subList(i, end)));
        }

        return frames;
    }
}
