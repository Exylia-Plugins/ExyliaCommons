package net.exylia.commons.v2.ui.animation.impl;

import net.exylia.commons.v2.ui.animation.AnimationType;
import net.exylia.commons.v2.ui.animation.MenuAnimation;

import java.util.*;

public class ExplosionAnimation implements MenuAnimation {

    @Override
    public AnimationType getType() {
        return AnimationType.EXPLOSION;
    }

    @Override
    public List<List<Integer>> calculateFrames(int rows, int cols) {
        Random random = new Random();
        int centerRow = random.nextInt(rows);
        int centerCol = random.nextInt(cols);

        Map<Integer, List<Integer>> distanceGroups = new TreeMap<>();

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int slot = row * cols + col;
                int distance = Math.max(Math.abs(row - centerRow), Math.abs(col - centerCol));
                distanceGroups.computeIfAbsent(distance, k -> new ArrayList<>()).add(slot);
            }
        }

        return new ArrayList<>(distanceGroups.values());
    }
}
