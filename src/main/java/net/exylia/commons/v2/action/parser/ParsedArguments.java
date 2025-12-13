package net.exylia.commons.v2.action.parser;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class ParsedArguments {
    private final String actionId;
    private final String raw;
    private final List<ArgumentToken> tokens;

    public int getInt(int index, int defaultValue) {
        if (index >= tokens.size()) {
            return defaultValue;
        }
        ArgumentToken token = tokens.get(index);
        try {
            return Integer.parseInt(token.getValue());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public String getString(int index, String defaultValue) {
        if (index >= tokens.size()) {
            return defaultValue;
        }
        return tokens.get(index).getValue();
    }

    public boolean getBoolean(int index, boolean defaultValue) {
        if (index >= tokens.size()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(tokens.get(index).getValue());
    }

    public double getDouble(int index, double defaultValue) {
        if (index >= tokens.size()) {
            return defaultValue;
        }
        ArgumentToken token = tokens.get(index);
        try {
            return Double.parseDouble(token.getValue());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public boolean hasFlag(String flag) {
        return tokens.stream()
                .filter(t -> t.getType() == ArgumentType.FLAG)
                .anyMatch(t -> t.getValue().equals(flag));
    }

    public int size() {
        return tokens.size();
    }

    public boolean isEmpty() {
        return tokens.isEmpty();
    }
}
