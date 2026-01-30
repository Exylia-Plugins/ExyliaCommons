package net.exylia.commons.v2.ui.animation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class AnimationSettings {

    @Builder.Default
    private AnimationType openAnimation = AnimationType.NONE;

    @Builder.Default
    private AnimationType pageAnimation = AnimationType.NONE;

    @Builder.Default
    private int speed = 1;

    public boolean hasOpenAnimation() {
        return openAnimation != null && openAnimation != AnimationType.NONE;
    }

    public boolean hasPageAnimation() {
        return pageAnimation != null && pageAnimation != AnimationType.NONE;
    }

    public AnimationSettings copy() {
        return AnimationSettings.builder()
                .openAnimation(openAnimation)
                .pageAnimation(pageAnimation)
                .speed(speed)
                .build();
    }
}
