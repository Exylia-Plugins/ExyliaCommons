package net.exylia.commons.utils.visuals;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OldColorUtils {
    private static final Pattern HEX_PATTERN = Pattern.compile("<#([0-9a-fA-F]{6})>");
    private static final Pattern BOLD_PATTERN = Pattern.compile("<bold>");
    private static final Pattern ITALIC_PATTERN = Pattern.compile("<italic>");
    private static final Pattern UNDERLINED_PATTERN = Pattern.compile("<underlined>");
    private static final Pattern STRIKETHROUGH_PATTERN = Pattern.compile("<strikethrough>");

    public static String convertMiniMessageToOLD(String message) {
        String result = message;
        result = convertGradientsAndTextToSectionHex(result);
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

    private static String convertGradientsAndTextToSectionHex(String message) {
        if (!message.contains("<gradient:")) return message;
        Pattern fullPattern = Pattern.compile("<gradient:(#[0-9a-fA-F]{6}(?::#[0-9a-fA-F]{6})+)>(.*)");
        Matcher matcher = fullPattern.matcher(message);
        if (!matcher.find()) return message;
        String colorsStr = matcher.group(1);
        String restOfMessage = matcher.group(2);
        String[] colors = colorsStr.split(":");
        String textToColor = extractPlainText(restOfMessage);
        String formattingTags = extractFormattingTags(restOfMessage);
        String formattingCodes = convertFormattingTagsToCodes(formattingTags);
        if (textToColor.isEmpty()) return restOfMessage;
        return createSectionHexGradient(colors, textToColor, formattingCodes);
    }

    private static String createSectionHexGradient(String[] colors, String text, String formattingCodes) {
        int[] cps = text.codePoints().toArray();
        if (cps.length == 0 || colors.length < 2) return text;
        int[][] stops = parseStops(colors);
        StringBuilder out = new StringBuilder(cps.length * 14);
        int n = cps.length;
        for (int i = 0; i < n; i++) {
            double t = n == 1 ? 0.0 : (double) i / (double) (n - 1);
            int[] rgb = interpolateBetweenStops(stops, t);
            out.append(toSectionHex(rgb[0], rgb[1], rgb[2]));
            out.append(formattingCodes);
            out.appendCodePoint(cps[i]);
        }
        return out.toString();
    }

    private static int[][] parseStops(String[] colors) {
        int[][] r = new int[colors.length][3];
        for (int i = 0; i < colors.length; i++) r[i] = hexToRgb(colors[i].startsWith("#") ? colors[i].substring(1) : colors[i]);
        return r;
    }

    private static int[] interpolateBetweenStops(int[][] stops, double t) {
        if (t <= 0) return stops[0];
        if (t >= 1) return stops[stops.length - 1];
        double pos = t * (stops.length - 1);
        int i = (int) Math.floor(pos);
        double lt = pos - i;
        int[] a = stops[i];
        int[] b = stops[i + 1];
        int r = (int) Math.round(a[0] * (1 - lt) + b[0] * lt);
        int g = (int) Math.round(a[1] * (1 - lt) + b[1] * lt);
        int bch = (int) Math.round(a[2] * (1 - lt) + b[2] * lt);
        return new int[]{r, g, bch};
    }

    private static String toSectionHex(int r, int g, int b) {
        char[] H = "0123456789ABCDEF".toCharArray();
        char[] out = new char[14];
        out[0]='§'; out[1]='x';
        out[2]='§'; out[3]=H[(r>>>4)&0xF];
        out[4]='§'; out[5]=H[r&0xF];
        out[6]='§'; out[7]=H[(g>>>4)&0xF];
        out[8]='§'; out[9]=H[g&0xF];
        out[10]='§'; out[11]=H[(b>>>4)&0xF];
        out[12]='§'; out[13]=H[b&0xF];
        return new String(out);
    }

    private static int[] hexToRgb(String hex) {
        String h = hex.startsWith("#") ? hex.substring(1) : hex;
        return new int[]{Integer.parseInt(h.substring(0,2),16), Integer.parseInt(h.substring(2,4),16), Integer.parseInt(h.substring(4,6),16)};
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

    private static String createOLDGradient(String[] colors, String text, String formattingCodes) {
        if (colors.length < 2 || text.isEmpty()) {
            return text;
        }

        StringBuilder result = new StringBuilder();
        int textLength = text.length();
        int colorCount = colors.length;

        if (textLength == 1) {
            result.append("{").append(colors[0]).append("}");
            result.append(formattingCodes);
            result.append(text.charAt(0));
        } else if (textLength <= colorCount) {
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
