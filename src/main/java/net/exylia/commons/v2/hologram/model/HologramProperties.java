package net.exylia.commons.v2.hologram.model;

import lombok.Builder;
import lombok.Getter;
import lombok.With;
import org.bukkit.Color;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;

@Getter
@Builder
@With
public class HologramProperties {
    @Builder.Default
    private final Display.Billboard billboard = Display.Billboard.CENTER;

    @Builder.Default
    private final TextDisplay.TextAlignment alignment = TextDisplay.TextAlignment.CENTER;

    @Builder.Default
    private final float scaleX = 1.0f;

    @Builder.Default
    private final float scaleY = 1.0f;

    @Builder.Default
    private final float scaleZ = 1.0f;

    @Builder.Default
    private final boolean shadow = false;

    @Builder.Default
    private final boolean seeThrough = false;

    @Builder.Default
    private final int lineWidth = 200;

    private final Color backgroundColor;

    @Builder.Default
    private final double lineSpacing = 0.25;

    @Builder.Default
    private final int brightness = -1;

    public static HologramProperties defaultProperties() {
        return HologramProperties.builder().build();
    }

    public HologramProperties withScale(float x, float y, float z) {
        return HologramProperties.builder()
                .billboard(this.billboard)
                .alignment(this.alignment)
                .scaleX(x)
                .scaleY(y)
                .scaleZ(z)
                .shadow(this.shadow)
                .seeThrough(this.seeThrough)
                .lineWidth(this.lineWidth)
                .backgroundColor(this.backgroundColor)
                .lineSpacing(this.lineSpacing)
                .brightness(this.brightness)
                .build();
    }
}
