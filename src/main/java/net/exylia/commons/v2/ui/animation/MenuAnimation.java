package net.exylia.commons.v2.ui.animation;

import java.util.List;

public interface MenuAnimation {

    AnimationType getType();

    List<List<Integer>> calculateFrames(int rows, int cols);
}
