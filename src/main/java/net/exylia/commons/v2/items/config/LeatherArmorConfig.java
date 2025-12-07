package net.exylia.commons.v2.items.config;

import lombok.Getter;
import org.bukkit.Color;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.meta.LeatherArmorMeta;

@Getter
public class LeatherArmorConfig {

    private Color color;

    public static LeatherArmorConfig fromConfig(ConfigurationSection config) {
        if (config == null) return null;

        LeatherArmorConfig armorConfig = new LeatherArmorConfig();

        if (config.contains("color")) {
            armorConfig.setColor(config.getString("color"));
        }

        return armorConfig;
    }

    public LeatherArmorConfig setColor(String colorString) {
        this.color = parseColor(colorString);
        return this;
    }

    public LeatherArmorConfig setColor(Color color) {
        this.color = color;
        return this;
    }

    public LeatherArmorConfig setColor(int r, int g, int b) {
        this.color = Color.fromRGB(r, g, b);
        return this;
    }

    public void applyColor(LeatherArmorMeta meta, org.bukkit.entity.Player player,
                           net.exylia.commons.placeholders.ExyliaContext context) {
        Color processedColor = getProcessedColor(player, context);
        if (processedColor != null) {
            meta.setColor(processedColor);
        }
    }

    public Color getProcessedColor(org.bukkit.entity.Player player,
                                   net.exylia.commons.placeholders.ExyliaContext context) {
        if (color == null) return null;

        if (player != null && context != null) {
            String colorString = context.processPlaceholders(color.toString(), player);
            Color processedColor = parseColor(colorString);
            return processedColor != null ? processedColor : color;
        }

        return color;
    }

    private Color parseColor(String colorString) {
        if (colorString == null) return null;

        try {
            if (colorString.startsWith("#")) {
                int rgb = Integer.parseInt(colorString.substring(1), 16);
                return Color.fromRGB(rgb);
            }

            String[] parts = colorString.split(",");
            if (parts.length == 3) {
                int r = Integer.parseInt(parts[0].trim());
                int g = Integer.parseInt(parts[1].trim());
                int b = Integer.parseInt(parts[2].trim());
                return Color.fromRGB(r, g, b);
            }

            return switch (colorString.toLowerCase()) {
                case "red" -> Color.RED;
                case "blue" -> Color.BLUE;
                case "green" -> Color.GREEN;
                case "yellow" -> Color.YELLOW;
                case "purple" -> Color.PURPLE;
                case "orange" -> Color.ORANGE;
                case "white" -> Color.WHITE;
                case "black" -> Color.BLACK;
                case "aqua", "cyan" -> Color.AQUA;
                case "fuchsia", "magenta" -> Color.FUCHSIA;
                case "gray", "grey" -> Color.GRAY;
                case "lime" -> Color.LIME;
                case "maroon" -> Color.MAROON;
                case "navy" -> Color.NAVY;
                case "olive" -> Color.OLIVE;
                case "silver" -> Color.SILVER;
                case "teal" -> Color.TEAL;
                default -> null;
            };
        } catch (Exception e) {
            return null;
        }
    }

    public boolean hasConfiguration() {
        return color != null;
    }
}
