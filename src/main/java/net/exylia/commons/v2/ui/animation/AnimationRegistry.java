package net.exylia.commons.v2.ui.animation;

import net.exylia.commons.v2.ui.animation.impl.*;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class AnimationRegistry {

    private static final Map<AnimationType, MenuAnimation> ANIMATIONS = new ConcurrentHashMap<>();

    static {
        register(new NoneAnimation());
        register(new SlideLeftAnimation());
        register(new SlideTopAnimation());
        register(new CascadeAnimation());
        register(new CenterOutAnimation());
        register(new RandomAnimation());
        register(new SpiralAnimation());
        register(new SpiralOutAnimation());
        register(new CheckerboardAnimation());
        register(new WaveHorizontalAnimation());
        register(new WaveVerticalAnimation());
        register(new CornersAnimation());
        register(new SnakeAnimation());
        register(new RowsAlternateAnimation());
        register(new ColumnsAlternateAnimation());
        register(new ExplosionAnimation());
        register(new TypewriterAnimation());
    }

    private AnimationRegistry() {}

    public static void register(MenuAnimation animation) {
        ANIMATIONS.put(animation.getType(), animation);
    }

    public static Optional<MenuAnimation> get(AnimationType type) {
        return Optional.ofNullable(ANIMATIONS.get(type));
    }

    public static MenuAnimation getOrDefault(AnimationType type) {
        return ANIMATIONS.getOrDefault(type, ANIMATIONS.get(AnimationType.NONE));
    }
}
