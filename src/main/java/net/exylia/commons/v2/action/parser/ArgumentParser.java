package net.exylia.commons.v2.action.parser;

import net.exylia.commons.v2.action.exception.ActionException;

import java.util.ArrayList;
import java.util.List;

public class ArgumentParser {

    private static final QuotedStringParser quotedParser = new QuotedStringParser();

    public static ParsedArguments parse(String actionString) {
        if (actionString == null || actionString.trim().isEmpty()) {
            throw new ActionException.ActionParseException("Action string cannot be null or empty");
        }

        String trimmed = actionString.trim();
        List<ArgumentToken> tokens = tokenize(trimmed);

        if (tokens.isEmpty()) {
            throw new ActionException.ActionParseException("No action ID found");
        }

        String actionId = tokens.get(0).getValue();
        List<ArgumentToken> args = tokens.size() > 1 ? tokens.subList(1, tokens.size()) : new ArrayList<>();

        return new ParsedArguments(actionId, actionString, args);
    }

    private static List<ArgumentToken> tokenize(String input) {
        List<ArgumentToken> tokens = new ArrayList<>();
        int i = 0;

        while (i < input.length()) {
            char c = input.charAt(i);

            if (c == ' ') {
                i++;
                continue;
            }

            if (c == '"' || c == '\'') {
                String quoted = quotedParser.parseQuoted(input, i);
                tokens.add(new ArgumentToken(quoted, ArgumentType.QUOTED));
                int length = quotedParser.getQuotedStringLength(input, i);
                i += length;
            } else if (c == '-' && i + 1 < input.length() && input.charAt(i + 1) != ' ' && !Character.isDigit(input.charAt(i + 1))) {
                String flag = parseFlag(input, i);
                tokens.add(new ArgumentToken(flag, ArgumentType.FLAG));
                i += flag.length();
            } else {
                String token = parseToken(input, i);
                tokens.add(new ArgumentToken(token, inferType(token)));
                i += token.length();
            }
        }

        return tokens;
    }

    private static String parseToken(String input, int startIndex) {
        StringBuilder token = new StringBuilder();
        int i = startIndex;

        while (i < input.length() && input.charAt(i) != ' ') {
            token.append(input.charAt(i));
            i++;
        }

        return token.toString();
    }

    private static String parseFlag(String input, int startIndex) {
        StringBuilder flag = new StringBuilder();
        int i = startIndex;

        while (i < input.length() && input.charAt(i) != ' ') {
            flag.append(input.charAt(i));
            i++;
        }

        return flag.toString();
    }

    private static ArgumentType inferType(String token) {
        if (token.equalsIgnoreCase("true") || token.equalsIgnoreCase("false")) {
            return ArgumentType.BOOLEAN;
        }

        if (token.matches("-?\\d+")) {
            return ArgumentType.INT;
        }

        if (token.matches("-?\\d+\\.\\d+")) {
            return ArgumentType.DOUBLE;
        }

        return ArgumentType.STRING;
    }
}
