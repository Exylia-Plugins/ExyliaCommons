package net.exylia.commons.v2.config;

import java.util.LinkedHashMap;
import java.util.Map;

public class SectionDefaultsBuilder {

    private final Config config;
    private final String sectionPath;
    private final Map<String, Object> entries = new LinkedHashMap<>();

    SectionDefaultsBuilder(Config config, String sectionPath) {
        this.config = config;
        this.sectionPath = sectionPath;
        config.addPreservedPrefix(sectionPath);
    }

    public SectionDefaultsBuilder put(String relativeKey, Object value) {
        entries.put(relativeKey, value);
        return this;
    }

    public SectionDefaultsBuilder ifAbsent(String path) {
        applyIfAbsent();
        return new SectionDefaultsBuilder(config, path);
    }

    public Config done() {
        applyIfAbsent();
        return config;
    }

    public Config save() {
        applyIfAbsent();
        config.save();
        return config;
    }

    private void applyIfAbsent() {
        if (config.getKeys(sectionPath).isEmpty()) {
            entries.forEach((key, value) -> config.set(sectionPath + "." + key, value));
        }
    }
}
