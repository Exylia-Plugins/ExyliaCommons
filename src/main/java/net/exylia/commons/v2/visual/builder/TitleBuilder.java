package net.exylia.commons.v2.visual.builder;

import net.exylia.commons.v2.visual.config.TitleConfig;
import net.exylia.commons.v2.visual.validation.ValidationResult;

import java.util.ArrayList;
import java.util.List;

public class TitleBuilder extends VisualBuilder<TitleConfig, TitleBuilder> {
    private String title = "";
    private String subtitle = "";
    private int fadeIn = 10;
    private int stay = 70;
    private int fadeOut = 20;

    private TitleBuilder() {
    }

    public static TitleBuilder create() {
        return new TitleBuilder();
    }

    public TitleBuilder title(String title) {
        this.title = title;
        return this;
    }

    public TitleBuilder subtitle(String subtitle) {
        this.subtitle = subtitle;
        return this;
    }

    public TitleBuilder fadeIn(int fadeIn) {
        this.fadeIn = fadeIn;
        return this;
    }

    public TitleBuilder stay(int stay) {
        this.stay = stay;
        return this;
    }

    public TitleBuilder fadeOut(int fadeOut) {
        this.fadeOut = fadeOut;
        return this;
    }

    public TitleBuilder times(int fadeIn, int stay, int fadeOut) {
        this.fadeIn = fadeIn;
        this.stay = stay;
        this.fadeOut = fadeOut;
        return this;
    }

    @Override
    protected ValidationResult validateInternal() {
        List<String> errors = new ArrayList<>();

        if ((title == null || title.isBlank()) && (subtitle == null || subtitle.isBlank())) {
            errors.add("Title or subtitle must be provided");
        }

        if (fadeIn < 0) {
            errors.add("Fade in must be >= 0");
        }

        if (stay < 0) {
            errors.add("Stay must be >= 0");
        }

        if (fadeOut < 0) {
            errors.add("Fade out must be >= 0");
        }

        return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
    }

    @Override
    protected TitleConfig buildInternal() {
        return TitleConfig.builder()
                .title(title)
                .subtitle(subtitle)
                .fadeIn(fadeIn)
                .stay(stay)
                .fadeOut(fadeOut)
                .updateInterval(updateInterval)
                .build();
    }
}
