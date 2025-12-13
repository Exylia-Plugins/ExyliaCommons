package net.exylia.commons.v2.action.parser;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ArgumentToken {
    private final String value;
    private final ArgumentType type;
}
