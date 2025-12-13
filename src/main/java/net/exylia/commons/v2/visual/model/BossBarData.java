package net.exylia.commons.v2.visual.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.kyori.adventure.bossbar.BossBar;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BossBarData {

    private String text;
    private String color;
    private String style;
    private double progress;
    private boolean permanent;
    private long updateInterval;

    public static BossBarData fromConfig(net.exylia.commons.v2.visual.config.BossBarConfig config) {
        return new BossBarData(
            config.getText(),
            config.getColor().name(),
            config.getStyle().name(),
            config.getProgress(),
            config.isPermanent(),
            config.getUpdateInterval()
        );
    }

    public BossBar.Color getColorEnum() {
        try {
            return BossBar.Color.valueOf(color);
        } catch (Exception e) {
            return BossBar.Color.BLUE;
        }
    }

    public BossBar.Overlay getStyleEnum() {
        try {
            return BossBar.Overlay.valueOf(style);
        } catch (Exception e) {
            return BossBar.Overlay.PROGRESS;
        }
    }
}
