package net.exylia.commons.utils.hooks;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CMIUtils {
    private static final Pattern HEX_PATTERN = Pattern.compile("<#([0-9a-fA-F]{6})>");
    private static final Pattern BOLD_PATTERN = Pattern.compile("<bold>");
    private static final Pattern ITALIC_PATTERN = Pattern.compile("<italic>");
    private static final Pattern UNDERLINED_PATTERN = Pattern.compile("<underlined>");
    private static final Pattern STRIKETHROUGH_PATTERN = Pattern.compile("<strikethrough>");

    public static String convertMiniMessageToCMI(String message) {
        String result = message;
        result = convertGradientsAndText(result);
        result = convertHexColors(result);
        result = convertFormatting(result);
        return result;
    }

    private static String convertHexColors(String message) {
        Matcher matcher = HEX_PATTERN.matcher(message);
        return matcher.replaceAll("{#$1}");
    }

    private static String convertFormatting(String message) {
        String result = message;
        result = BOLD_PATTERN.matcher(result).replaceAll("&l");
        result = ITALIC_PATTERN.matcher(result).replaceAll("&o");
        result = UNDERLINED_PATTERN.matcher(result).replaceAll("&n");
        result = STRIKETHROUGH_PATTERN.matcher(result).replaceAll("&m");
        return result;
    }

    private static String convertGradientsAndText(String message) {
        if (!message.contains("<gradient:")) {
            return message;
        }

        Pattern fullPattern = Pattern.compile("<gradient:(#[0-9a-fA-F]{6}(?::#[0-9a-fA-F]{6})+)>(.*)");
        Matcher matcher = fullPattern.matcher(message);

        if (matcher.find()) {
            String colorsStr = matcher.group(1);
            String restOfMessage = matcher.group(2);
            String[] colors = colorsStr.split(":");

            String textToColor = extractPlainText(restOfMessage);
            String formattingTags = extractFormattingTags(restOfMessage);

            StringBuilder result = new StringBuilder();

            if (!textToColor.isEmpty()) {
                String formattingCodes = convertFormattingTagsToCodes(formattingTags);
                String cmiGradient = createCMIGradient(colors, textToColor, formattingCodes);
                result.append(cmiGradient);
            } else {
                result.append(restOfMessage);
            }

            return result.toString();
        }

        return message;
    }

    private static String extractPlainText(String message) {
        String result = message;
        result = result.replaceAll("<[^>]+>", "");
        return result;
    }

    private static String extractFormattingTags(String message) {
        StringBuilder tags = new StringBuilder();
        Pattern tagPattern = Pattern.compile("<([^/>]+)>");
        Matcher matcher = tagPattern.matcher(message);

        while (matcher.find()) {
            String tag = matcher.group(1);
            if (tag.equals("bold") || tag.equals("italic") || tag.equals("underlined") || tag.equals("strikethrough")) {
                tags.append("<").append(tag).append(">");
            }
        }

        return tags.toString();
    }

    private static String convertFormattingTagsToCodes(String tags) {
        String result = tags;
        result = result.replace("<bold>", "&l");
        result = result.replace("<italic>", "&o");
        result = result.replace("<underlined>", "&n");
        result = result.replace("<strikethrough>", "&m");
        return result;
    }

    private static String createCMIGradient(String[] colors, String text, String formattingCodes) {
        if (colors.length < 2 || text.isEmpty()) {
            return text;
        }

        StringBuilder result = new StringBuilder();
        int textLength = text.length();
        int colorCount = colors.length;

        if (textLength <= colorCount) {
            for (int i = 0; i < textLength; i++) {
                int colorIndex = i < colorCount ? i : colorCount - 1;

                if (i == 0) {
                    result.append("{").append(colors[colorIndex]).append(">}");
                } else if (i == textLength - 1) {
                    result.append("{").append(colors[colorIndex]).append("<}");
                } else {
                    result.append("{").append(colors[colorIndex]).append("<>}");
                }

                result.append(formattingCodes);
                result.append(text.charAt(i));
            }
        } else {
            int charsPerColor = textLength / colorCount;
            int remainingChars = textLength % colorCount;
            int currentPos = 0;

            for (int colorIndex = 0; colorIndex < colorCount; colorIndex++) {
                int charsForThisColor = charsPerColor + (colorIndex < remainingChars ? 1 : 0);

                if (colorIndex == 0) {
                    result.append("{").append(colors[colorIndex]).append(">}");
                } else if (colorIndex == colorCount - 1) {
                    result.append("{").append(colors[colorIndex]).append("<}");
                } else {
                    result.append("{").append(colors[colorIndex]).append("<>}");
                }

                result.append(formattingCodes);

                for (int j = 0; j < charsForThisColor && currentPos < textLength; j++) {
                    result.append(text.charAt(currentPos));
                    currentPos++;
                }
            }
        }

        return result.toString();
    }
}
