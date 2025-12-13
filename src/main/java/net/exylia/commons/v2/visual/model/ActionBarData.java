package net.exylia.commons.v2.visual.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ActionBarData {

    private String text;
    private boolean permanent;
    private long updateInterval;

    public static ActionBarData fromConfig(net.exylia.commons.v2.visual.config.ActionBarConfig config) {
        return new ActionBarData(
            config.getText(),
            config.isPermanent(),
            config.getUpdateInterval()
        );
    }
}
