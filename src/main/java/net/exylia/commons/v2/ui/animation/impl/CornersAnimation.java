package net.exylia.commons.v2.ui.animation.impl;

import net.exylia.commons.v2.ui.animation.AnimationType;
import net.exylia.commons.v2.ui.animation.MenuAnimation;

import java.util.*;

public class CornersAnimation implements MenuAnimation {

    @Override
    public AnimationType getType() {
        return AnimationType.CORNERS;
    }

    @Override
    public List<List<Integer>> calculateFrames(int rows, int cols) {
        int[][] corners = {
            {0, 0},
            {0, cols - 1},
            {rows - 1, 0},
            {rows - 1, cols - 1}
        };

        Map<Integer, List<Integer>> distanceGroups = new TreeMap<>();

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int slot = row * cols + col;

                int minDistance = Integer.MAX_VALUE;
                for (int[] corner : corners) {
                    int distance = Math.abs(row - corner[0]) + Math.abs(col - corner[1]);
                    minDistance = Math.min(minDistance, distance);
                }

                distanceGroups.computeIfAbsent(minDistance, k -> new ArrayList<>()).add(slot);
            }
        }

        return new ArrayList<>(distanceGroups.values());
    }
}
