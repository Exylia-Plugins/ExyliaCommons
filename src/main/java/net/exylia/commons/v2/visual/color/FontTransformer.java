package net.exylia.commons.v2.visual.color;

import java.util.HashMap;
import java.util.Map;

public class FontTransformer {
    private static final Map<String, Map<String, String>> FONT_MAPS = new HashMap<>();

    static {
        initializeFontMaps();
    }

    public enum FontType {
        SMALL,
        FRAKTUR,
        BOLD_FRAKTUR,
        SCRIPT,
        DOUBLE_STRUCK,
        SQUARED,
        BOLD,
        ITALIC,
        BOLD_ITALIC,
        MONOSPACE,
        NEGATIVE_SQUARED
    }

    private static void initializeFontMaps() {
        String[] normal = {"a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z"};
        String[] normalUpper = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z"};

        String[] small = {"ᴀ", "ʙ", "ᴄ", "ᴅ", "ᴇ", "ғ", "ɢ", "ʜ", "ɪ", "ᴊ", "ᴋ", "ʟ", "ᴍ", "ɴ", "ᴏ", "ᴘ", "ǫ", "ʀ", "s", "ᴛ", "ᴜ", "ᴠ", "ᴡ", "x", "ʏ", "ᴢ"};
        String[] fraktur = {"\uD835\uDD1E", "\uD835\uDD1F", "\uD835\uDD20", "\uD835\uDD21", "\uD835\uDD22", "\uD835\uDD23", "\uD835\uDD24", "\uD835\uDD25", "\uD835\uDD26", "\uD835\uDD27", "\uD835\uDD28", "\uD835\uDD29", "\uD835\uDD2A", "\uD835\uDD2B", "\uD835\uDD2C", "\uD835\uDD2D", "\uD835\uDD2E", "\uD835\uDD2F", "\uD835\uDD30", "\uD835\uDD31", "\uD835\uDD32", "\uD835\uDD33", "\uD835\uDD34", "\uD835\uDD35", "\uD835\uDD36", "\uD835\uDD37"};
        String[] boldFraktur = {"𝖆", "𝖇", "𝖈", "𝖉", "𝖊", "𝖋", "𝖌", "𝖍", "𝖎", "𝖏", "𝖐", "𝖑", "𝖒", "𝖓", "𝖔", "𝖕", "𝖖", "𝖗", "𝖘", "𝖙", "𝖚", "𝖛", "𝖜", "𝖝", "𝖞", "𝖟"};
        String[] script = {"𝓪", "𝓫", "𝓬", "𝓭", "𝓮", "𝓯", "𝓰", "𝓱", "𝓲", "𝓳", "𝓴", "𝓵", "𝓶", "𝓷", "𝓸", "𝓹", "𝓺", "𝓻", "𝓼", "𝓽", "𝓾", "𝓿", "𝔀", "𝔁", "𝔂", "𝔃"};
        String[] doubleStruck = {"𝕒", "𝕓", "𝕔", "𝕕", "𝕖", "𝕗", "𝕘", "𝕙", "𝕚", "𝕛", "𝕜", "𝕝", "𝕞", "𝕟", "𝕠", "𝕡", "𝕢", "𝕣", "𝕤", "𝕥", "𝕦", "𝕧", "𝕨", "𝕩", "𝕪", "𝕫"};
        String[] squared = {"🄰", "🄱", "🄲", "🄳", "🄴", "🄵", "🄶", "🄷", "🄸", "🄹", "🄺", "🄻", "🄼", "🄽", "🄾", "🄿", "🅀", "🅁", "🅂", "🅃", "🅄", "🅅", "🅆", "🅇", "🅈", "🅉"};
        String[] bold = {"𝐚", "𝐛", "𝐜", "𝐝", "𝐞", "𝐟", "𝐠", "𝐡", "𝐢", "𝐣", "𝐤", "𝐥", "𝐦", "𝐧", "𝐨", "𝐩", "𝐪", "𝐫", "𝐬", "𝐭", "𝐮", "𝐯", "𝐰", "𝐱", "𝐲", "𝐳"};
        String[] italic = {"𝘢", "𝘣", "𝘤", "𝘥", "𝘦", "𝘧", "𝘨", "𝘩", "𝘪", "𝘫", "𝘬", "𝘭", "𝘮", "𝘯", "𝘰", "𝘱", "𝘲", "𝘳", "𝘴", "𝘵", "𝘶", "𝘷", "𝘸", "𝘹", "𝘺", "𝘻"};
        String[] boldItalic = {"𝙖", "𝙗", "𝙘", "𝙙", "𝙚", "𝙛", "𝙜", "𝙝", "𝙞", "𝙟", "𝙠", "𝙡", "𝙢", "𝙣", "𝙤", "𝙥", "𝙦", "𝙧", "𝙨", "𝙩", "𝙪", "𝙫", "𝙬", "𝙭", "𝙮", "𝙯"};
        String[] monospace = {"𝚊", "𝚋", "𝚌", "𝚍", "𝚎", "𝚏", "𝚐", "𝚑", "𝚒", "𝚓", "𝚔", "𝚕", "𝚖", "𝚗", "𝚘", "𝚙", "𝚚", "𝚛", "𝚜", "𝚝", "𝚞", "𝚟", "𝚠", "𝚡", "𝚢", "𝚣"};
        String[] negativeSquared = {"🅰", "🅱", "🅲", "🅳", "🅴", "🅵", "🅶", "🅷", "🅸", "🅹", "🅺", "🅻", "🅼", "🅽", "🅾", "🅿", "🆀", "🆁", "🆂", "🆃", "🆄", "🆅", "🆆", "🆇", "🆈", "🆉"};

        Map<String, String> smallMap = new HashMap<>();
        Map<String, String> frakturMap = new HashMap<>();
        Map<String, String> boldFrakturMap = new HashMap<>();
        Map<String, String> scriptMap = new HashMap<>();
        Map<String, String> doubleStruckMap = new HashMap<>();
        Map<String, String> squaredMap = new HashMap<>();
        Map<String, String> boldMap = new HashMap<>();
        Map<String, String> italicMap = new HashMap<>();
        Map<String, String> boldItalicMap = new HashMap<>();
        Map<String, String> monospaceMap = new HashMap<>();
        Map<String, String> negativeSquaredMap = new HashMap<>();

        for (int i = 0; i < 26; i++) {
            String normalChar = normal[i];
            String upperChar = normalUpper[i];

            smallMap.put(normalChar, small[i]);
            smallMap.put(upperChar, small[i]);

            frakturMap.put(normalChar, fraktur[i]);
            frakturMap.put(upperChar, fraktur[i]);

            boldFrakturMap.put(normalChar, boldFraktur[i]);
            boldFrakturMap.put(upperChar, boldFraktur[i]);

            scriptMap.put(normalChar, script[i]);
            scriptMap.put(upperChar, script[i]);

            doubleStruckMap.put(normalChar, doubleStruck[i]);
            doubleStruckMap.put(upperChar, doubleStruck[i]);

            boldMap.put(normalChar, bold[i]);
            boldMap.put(upperChar, bold[i]);

            italicMap.put(normalChar, italic[i]);
            italicMap.put(upperChar, italic[i]);

            boldItalicMap.put(normalChar, boldItalic[i]);
            boldItalicMap.put(upperChar, boldItalic[i]);

            monospaceMap.put(normalChar, monospace[i]);
            monospaceMap.put(upperChar, monospace[i]);

            squaredMap.put(normalChar, squared[i]);
            squaredMap.put(upperChar, squared[i]);

            negativeSquaredMap.put(normalChar, negativeSquared[i]);
            negativeSquaredMap.put(upperChar, negativeSquared[i]);
        }

        FONT_MAPS.put("small", smallMap);
        FONT_MAPS.put("fraktur", frakturMap);
        FONT_MAPS.put("bold_fraktur", boldFrakturMap);
        FONT_MAPS.put("script", scriptMap);
        FONT_MAPS.put("double_struck", doubleStruckMap);
        FONT_MAPS.put("squared", squaredMap);
        FONT_MAPS.put("bold", boldMap);
        FONT_MAPS.put("italic", italicMap);
        FONT_MAPS.put("bold_italic", boldItalicMap);
        FONT_MAPS.put("monospace", monospaceMap);
        FONT_MAPS.put("negative_squared", negativeSquaredMap);
    }

    public static String transform(String message, FontType fontType, boolean forceUpperCase) {
        if (message == null || message.isEmpty() || fontType == null) {
            return message;
        }

        return transform(message, fontType.name().toLowerCase(), forceUpperCase);
    }

    public static String transform(String message, String fontType, boolean forceUpperCase) {
        if (message == null || message.isEmpty() || fontType == null) {
            return message;
        }

        Map<String, String> fontMap = FONT_MAPS.get(fontType.toLowerCase());
        if (fontMap == null) {
            return message;
        }

        StringBuilder result = new StringBuilder();
        boolean insideTag = false;
        boolean insideBrackets = false;
        int tagDepth = 0;

        for (int i = 0; i < message.length(); i++) {
            char c = message.charAt(i);

            if (c == '[' && !insideTag) {
                insideBrackets = true;
            } else if (c == ']' && !insideTag) {
                insideBrackets = false;
            }

            if (c == '<' && !insideTag && !insideBrackets) {
                int closingIndex = findTagEnd(message, i);
                if (closingIndex != -1) {
                    insideTag = true;
                    tagDepth = 1;
                }
            } else if (c == '<' && insideTag) {
                tagDepth++;
            } else if (c == '>' && insideTag) {
                tagDepth--;
                if (tagDepth <= 0) {
                    insideTag = false;
                    tagDepth = 0;
                }
            }

            if (c == '&' && i + 1 < message.length() && !insideTag && !insideBrackets) {
                char nextChar = message.charAt(i + 1);
                if (isColorCode(nextChar)) {
                    result.append(c);
                    i++;
                    result.append(nextChar);
                    continue;
                }
            }

            if (insideTag || insideBrackets) {
                result.append(c);
            } else {
                String targetChar = String.valueOf(forceUpperCase && isBasicLetter(c) ? Character.toUpperCase(c) : c);
                String transformed = fontMap.get(targetChar);
                result.append(transformed != null ? transformed : targetChar);
            }
        }

        return result.toString();
    }

    private static int findTagEnd(String message, int startIndex) {
        int depth = 0;
        for (int i = startIndex; i < message.length(); i++) {
            char c = message.charAt(i);
            if (c == '<') {
                depth++;
            } else if (c == '>') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static boolean isColorCode(char c) {
        return (c >= '0' && c <= '9') ||
               (c >= 'a' && c <= 'f') ||
               (c >= 'A' && c <= 'F') ||
               c == 'k' || c == 'l' || c == 'm' || c == 'n' ||
               c == 'o' || c == 'p' || c == 'r' ||
               c == 'K' || c == 'L' || c == 'M' || c == 'N' ||
               c == 'O' || c == 'P' || c == 'R' || c == '#';
    }

    private static boolean isBasicLetter(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }
}
