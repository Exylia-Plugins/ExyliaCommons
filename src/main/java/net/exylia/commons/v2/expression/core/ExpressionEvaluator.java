package net.exylia.commons.v2.expression.core;

public final class ExpressionEvaluator {

    private ExpressionEvaluator() {
    }

    public static double evaluate(String expression) {
        Parser parser = new Parser(expression);
        double result = parser.parseExpression();
        parser.skipWhitespace();
        if (!parser.isAtEnd()) {
            throw new IllegalArgumentException("Unexpected character at position " + parser.position + " in '" + expression + "'");
        }
        return result;
    }

    private static final class Parser {
        private final String source;
        private int position;

        private Parser(String source) {
            this.source = source;
            this.position = 0;
        }

        private double parseExpression() {
            double value = parseTerm();
            while (true) {
                skipWhitespace();
                if (match('+')) {
                    value += parseTerm();
                } else if (match('-')) {
                    value -= parseTerm();
                } else {
                    break;
                }
            }
            return value;
        }

        private double parseTerm() {
            double value = parseFactor();
            while (true) {
                skipWhitespace();
                if (match('*')) {
                    value *= parseFactor();
                } else if (match('/')) {
                    double divisor = parseFactor();
                    if (divisor == 0.0) {
                        throw new ArithmeticException("Division by zero");
                    }
                    value /= divisor;
                } else {
                    break;
                }
            }
            return value;
        }

        private double parseFactor() {
            skipWhitespace();
            if (match('-')) {
                return -parseFactor();
            }
            if (match('+')) {
                return parseFactor();
            }
            if (match('(')) {
                double value = parseExpression();
                skipWhitespace();
                if (!match(')')) {
                    throw new IllegalArgumentException("Missing closing parenthesis at position " + position);
                }
                return value;
            }
            return parseNumber();
        }

        private double parseNumber() {
            skipWhitespace();
            int start = position;
            while (!isAtEnd() && (Character.isDigit(source.charAt(position)) || source.charAt(position) == '.')) {
                position++;
            }
            if (start == position) {
                throw new IllegalArgumentException("Expected number at position " + position + " in '" + source + "'");
            }
            return Double.parseDouble(source.substring(start, position));
        }

        private boolean match(char expected) {
            if (isAtEnd() || source.charAt(position) != expected) return false;
            position++;
            return true;
        }

        private void skipWhitespace() {
            while (!isAtEnd() && Character.isWhitespace(source.charAt(position))) {
                position++;
            }
        }

        private boolean isAtEnd() {
            return position >= source.length();
        }
    }
}
