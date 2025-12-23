package net.exylia.commons.v2.debug.core;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DebugType {
    DEBUG("DEBUG", "<#e7cfff>", "<#e7cfff>"),
    ERROR("ERROR", "<#a33b53>", "<#b36476>"),
    WARN("WARN", "<#ffc58f>", "<#ffd2a8>"),
    INFO("INFO", "<#59a4ff>", "<#7db7ff>"),
    SUCCESS("SUCCESS", "<#8fffc1>", "<#a1ffc3>");

    private final String label;
    private final String pluginColor;
    private final String libraryColor;
}
