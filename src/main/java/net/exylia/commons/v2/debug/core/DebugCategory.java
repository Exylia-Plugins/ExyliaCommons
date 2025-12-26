package net.exylia.commons.v2.debug.core;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@AllArgsConstructor
public enum DebugCategory {
    GENERAL("GENERAL"),
    DATABASE("DATABASE"),
    REDIS("REDIS"),
    HOLOGRAM("HOLOGRAM"),
    UI("UI"),
    VISUAL("VISUAL"),
    REWARD("REWARD"),
    ITEMS("ITEMS"),
    SKULL("SKULL"),
    CONFIG("CONFIG"),
    PLACEHOLDER("PLACEHOLDER"),
    SCOREBOARD("SCOREBOARD"),
    REGION("REGION"),
    ACTION("ACTION"),
    COMMAND("COMMAND"),
    FORMATTER("FORMATTER"),
    LIFECYCLE("LIFECYCLE"),
    CLAN("CLAN"),
    LICENSE("LICENSE"),
    ASYNC("ASYNC"),
    CONVERSATION("CONVERSATION");

    private final String name;

    private static final Map<String, DebugCategory> LOOKUP = new ConcurrentHashMap<>();

    static {
        for (DebugCategory category : values()) {
            LOOKUP.put(category.name.toUpperCase(), category);
        }
    }

    public static DebugCategory fromName(String name) {
        if (name == null) return null;
        return LOOKUP.get(name.toUpperCase());
    }
}
