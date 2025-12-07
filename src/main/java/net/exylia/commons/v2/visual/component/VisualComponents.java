package net.exylia.commons.v2.visual.component;

public final class VisualComponents {
    private VisualComponents() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static ActionBarComponent actionBar() {
        return ActionBarComponent.getInstance();
    }

    public static BossBarComponent bossBar() {
        return BossBarComponent.getInstance();
    }

    public static TitleComponent title() {
        return TitleComponent.getInstance();
    }
}
