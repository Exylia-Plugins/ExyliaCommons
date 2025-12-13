package net.exylia.commons.v2.action.parser;

import net.exylia.commons.v2.action.exception.ActionException;

public class QuotedStringParser {

    public String parseQuoted(String input, int startIndex) {
        char quoteChar = input.charAt(startIndex);
        if (quoteChar != '"' && quoteChar != '\'') {
            throw new ActionException.ActionParseException("Expected quote character at index " + startIndex);
        }

        StringBuilder result = new StringBuilder();
        int i = startIndex + 1;
        boolean escaped = false;

        while (i < input.length()) {
            char c = input.charAt(i);

            if (escaped) {
                result.append(c);
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == quoteChar) {
                return result.toString();
            } else {
                result.append(c);
            }

            i++;
        }

        throw new ActionException.ActionParseException("Unclosed quoted string starting at index " + startIndex);
    }

    public int getQuotedStringLength(String input, int startIndex) {
        char quoteChar = input.charAt(startIndex);
        int i = startIndex + 1;
        boolean escaped = false;

        while (i < input.length()) {
            char c = input.charAt(i);

            if (escaped) {
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == quoteChar) {
                return i - startIndex + 1;
            }

            i++;
        }

        return -1;
    }
}
