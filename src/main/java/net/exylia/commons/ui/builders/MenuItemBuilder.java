package net.exylia.commons.ui.builders;

import net.exylia.commons.placeholders.ExyliaContext;
import net.exylia.commons.placeholders.PlaceholderSystemManager;
import net.exylia.commons.ui.events.MenuClickEvent;
import net.exylia.commons.ui.items.MenuItem;
import net.exylia.commons.ui.items.PotionConfig;
import net.exylia.commons.actions.ActionContext;
import net.exylia.commons.actions.ActionSource;
import net.exylia.commons.actions.GlobalActionManager;
import org.bukkit.Color;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.Map;

public class MenuItemBuilder {

    public static MenuItem fromConfig(ConfigurationSection config) {
        return fromConfig(config, null, null);
    }

    public static MenuItem fromConfig(ConfigurationSection config, Player player) {
        return fromConfig(config, player, null);
    }

    public static MenuItem fromConfig(ConfigurationSection config, Player player, ExyliaContext context) {
        if (config == null) {
            throw new IllegalArgumentException("Configuration section cannot be null");
        }

        String material = config.getString("material", "STONE");
        MenuItem item = new MenuItem(material);

        configureBasicProperties(item, config);
        configureVisualProperties(item, config);
        configureBehavior(item, config);
        configureEnchantments(item, config);
        configurePotionEffects(item, config);
        configureActions(item, config);
        configureCommands(item, config, player, context);

        if (context != null) {
            item.withContext(context);
        }

        item.process(player);

        return item;
    }

    private static void configureBasicProperties(MenuItem item, ConfigurationSection config) {
        if (config.contains("name")) {
            item.setName(config.getString("name"));
        }

        if (config.contains("lore")) {
            if (config.isList("lore")) {
                item.setLoreList(config.getStringList("lore"));
            } else {
                item.setLore(config.getString("lore"));
            }
        }

        if (config.contains("amount")) {
            Object amount = config.get("amount");
            if (amount instanceof String) {
                item.setAmount((String) amount);
            } else if (amount instanceof Integer) {
                item.setAmount((Integer) amount);
            }
        }
    }

    private static void configureVisualProperties(MenuItem item, ConfigurationSection config) {
        if (config.getBoolean("glow", false)) {
            item.setGlowing(true);
        }

        if (config.getBoolean("hide_attributes", false)) {
            item.hideAllAttributes();
        }
    }

    private static void configureBehavior(MenuItem item, ConfigurationSection config) {
        if (config.getBoolean("dynamic_update", false)) {
            item.setDynamicUpdate(true);

            if (config.contains("update_interval")) {
                item.setUpdateInterval(config.getLong("update_interval", 20L));
            }
        }
    }

    private static void configureEnchantments(MenuItem item, ConfigurationSection config) {
        ConfigurationSection enchantments = config.getConfigurationSection("enchantments");
        if (enchantments != null) {
            for (String enchantName : enchantments.getKeys(false)) {
                Object level = enchantments.get(enchantName);
                if (level instanceof Integer) {
                    item.addEnchantment(enchantName, (Integer) level);
                } else if (level instanceof String) {
                    item.addEnchantment(enchantName, (String) level);
                }
            }
        }

        if (config.isList("enchantments")) {
            List<?> enchantList = config.getList("enchantments");
            if (enchantList != null) {
                for (Object enchantObj : enchantList) {
                    if (enchantObj instanceof Map<?, ?> enchantMap) {
                        String enchantName = String.valueOf(enchantMap.get("type"));
                        Object level = enchantMap.get("level");
                        String levelStr = level != null ? String.valueOf(level) : "1";

                        item.addEnchantment(enchantName, levelStr);
                    }
                }
            }
        }
    }

    private static void configurePotionEffects(MenuItem item, ConfigurationSection config) {
        if (config.contains("potion")) {
            ConfigurationSection potionSection = config.getConfigurationSection("potion");
            if (potionSection != null) {
                PotionConfig potionConfig = PotionConfig.fromConfig(potionSection);
                item.setPotionConfig(potionConfig);
            }
        }

        else if (config.contains("potion_effects") || config.contains("base_potion_type") || config.contains("potion_color")) {
            PotionConfig potionConfig = new PotionConfig();

            if (config.contains("base_potion_type")) {
                potionConfig.setBasePotionType(config.getString("base_potion_type"));
            }

            if (config.contains("potion_color")) {
                potionConfig.setPotionColor(config.getString("potion_color"));
            }

            if (config.contains("potion_effects")) {
                List<?> effectsList = config.getList("potion_effects");
                if (effectsList != null) {
                    for (Object effectObj : effectsList) {
                        if (effectObj instanceof Map<?, ?> effectMap) {
                            String type = String.valueOf(effectMap.get("type"));
                            Object amplifier = effectMap.get("amplifier");
                            Object duration = effectMap.get("duration");

                            String amplifierStr = amplifier != null ? String.valueOf(amplifier) : "0";
                            String durationStr = duration != null ? String.valueOf(duration) : "600";

                            potionConfig.addCustomEffect(type, amplifierStr, durationStr);
                        }
                    }
                }
            }

            item.setPotionConfig(potionConfig);
        }
    }

    private static void configureActions(MenuItem item, ConfigurationSection config) {
        if (config.contains("action")) {
            String actionString = config.getString("action");
            item.setClickHandler(event -> {
                ActionContext actionContext = createActionContextFromMenuClick(event);
                GlobalActionManager.executeAction(actionString, actionContext);
            });
        }

        if (config.contains("click_type")) {
            String clickType = config.getString("click_type");
            switch (clickType.toLowerCase()) {
                case "close" -> item.setClickHandler(MenuClickEvent::closeMenu);
                case "back" -> item.setClickHandler(MenuClickEvent::openParentMenu);
            }
        }
    }

    private static ActionContext createActionContextFromMenuClick(MenuClickEvent event) {
        ActionContext context = new ActionContext(event.getPlayer(), ActionSource.MENU);

        context.withData("menu", event.getMenu());
        context.withData("item", event.getItem());
        context.withData("slot", event.getSlot());
        context.withData("clickType", event.getClickType());

        return context;
    }

    private static void configureCommands(MenuItem item, ConfigurationSection config, Player player, Object... context) {
        if (config.contains("commands")) {
            java.util.List<String> commands = config.getStringList("commands");
            if (!commands.isEmpty()) {
                item.setClickHandler(event -> {
                    executeCommands(commands, event.getPlayer(), context);
                });
            }
        }
    }

    private static void executeCommands(java.util.List<String> commands, Player player, Object... context) {
        for (String command : commands) {
            String processed = command;

            if (context != null) {
                processed = PlaceholderSystemManager.getInstance().process(processed, player, context);
            }

            if (processed.startsWith("player:")) {
                String cmd = processed.substring(7).trim();
                player.performCommand(cmd);
            } else if (processed.startsWith("console:")) {
                String cmd = processed.substring(8).trim();
                org.bukkit.Bukkit.getServer().dispatchCommand(
                        org.bukkit.Bukkit.getServer().getConsoleSender(), cmd);
            } else {
                player.performCommand(processed);
            }
        }
    }

    public static FluentMenuItemBuilder create(String material) {
        return new FluentMenuItemBuilder(new MenuItem(material));
    }

    public static FluentMenuItemBuilder from(MenuItem item) {
        return new FluentMenuItemBuilder(item);
    }

    public static class FluentMenuItemBuilder {
        private final MenuItem item;

        public FluentMenuItemBuilder(MenuItem item) {
            this.item = item;
        }

        public FluentMenuItemBuilder name(String name) {
            item.setName(name);
            return this;
        }

        public FluentMenuItemBuilder lore(String... lore) {
            item.setLore(lore);
            return this;
        }

        public FluentMenuItemBuilder lore(java.util.List<String> lore) {
            item.setLoreList(lore);
            return this;
        }

        public FluentMenuItemBuilder amount(int amount) {
            item.setAmount(amount);
            return this;
        }

        public FluentMenuItemBuilder amount(String amountString) {
            item.setAmount(amountString);
            return this;
        }

        public FluentMenuItemBuilder glow(boolean glowing) {
            item.setGlowing(glowing);
            return this;
        }

        public FluentMenuItemBuilder glow() {
            return glow(true);
        }

        public FluentMenuItemBuilder hideAttributes() {
            item.hideAllAttributes();
            return this;
        }

        public FluentMenuItemBuilder dynamicUpdate(boolean dynamic) {
            item.setDynamicUpdate(dynamic);
            return this;
        }

        public FluentMenuItemBuilder updateInterval(long interval) {
            item.setUpdateInterval(interval);
            return this;
        }

        public FluentMenuItemBuilder enchant(Enchantment enchantment, int level) {
            item.addEnchantment(enchantment, level);
            return this;
        }

        public FluentMenuItemBuilder enchant(String enchantmentName, int level) {
            item.addEnchantment(enchantmentName, level);
            return this;
        }

        public FluentMenuItemBuilder enchant(String enchantmentName, String level) {
            item.addEnchantment(enchantmentName, level);
            return this;
        }

        public FluentMenuItemBuilder potion(PotionConfig potionConfig) {
            item.setPotionConfig(potionConfig);
            return this;
        }

        public FluentMenuItemBuilder basePotionType(String potionType) {
            return basePotionType(potionType, false, false);
        }

        public FluentMenuItemBuilder basePotionType(String potionType, boolean upgraded, boolean extended) {
            PotionConfig config = item.getPotionConfig();
            if (config == null) {
                config = new PotionConfig();
                item.setPotionConfig(config);
            }
            config.setBasePotionType(potionType)
                    .setPotionUpgraded(upgraded)
                    .setPotionExtended(extended);
            return this;
        }

        public FluentMenuItemBuilder potionUpgraded(boolean upgraded) {
            PotionConfig config = item.getPotionConfig();
            if (config == null) {
                config = new PotionConfig();
                item.setPotionConfig(config);
            }
            config.setPotionUpgraded(upgraded);
            return this;
        }

        public FluentMenuItemBuilder potionExtended(boolean extended) {
            PotionConfig config = item.getPotionConfig();
            if (config == null) {
                config = new PotionConfig();
                item.setPotionConfig(config);
            }
            config.setPotionExtended(extended);
            return this;
        }

        public FluentMenuItemBuilder potionEffect(PotionEffectType effectType, int amplifier, int duration) {
            return potionEffect(effectType.getName(), amplifier, duration);
        }

        public FluentMenuItemBuilder potionEffect(String effectType, int amplifier, int duration) {
            return potionEffect(effectType, String.valueOf(amplifier), String.valueOf(duration));
        }

        public FluentMenuItemBuilder potionEffect(String effectType, String amplifier, String duration) {
            return potionEffect(effectType, amplifier, duration, false, true, true);
        }

        public FluentMenuItemBuilder potionEffect(String effectType, String amplifier, String duration,
                                                  boolean ambient, boolean particles, boolean icon) {
            PotionConfig config = item.getPotionConfig();
            if (config == null) {
                config = new PotionConfig();
                item.setPotionConfig(config);
            }
            config.addCustomEffect(effectType, amplifier, duration, ambient, particles, icon);
            return this;
        }

        public FluentMenuItemBuilder potionColor(Color color) {
            PotionConfig config = item.getPotionConfig();
            if (config == null) {
                config = new PotionConfig();
                item.setPotionConfig(config);
            }
            config.setPotionColor(color);
            return this;
        }

        public FluentMenuItemBuilder potionColor(String colorString) {
            PotionConfig config = item.getPotionConfig();
            if (config == null) {
                config = new PotionConfig();
                item.setPotionConfig(config);
            }
            config.setPotionColor(colorString);
            return this;
        }

        public FluentMenuItemBuilder potionColor(int r, int g, int b) {
            return potionColor(Color.fromRGB(r, g, b));
        }

        public FluentMenuItemBuilder click(java.util.function.Consumer<MenuClickEvent> handler) {
            item.setClickHandler(handler);
            return this;
        }

        public FluentMenuItemBuilder action(String actionString) {
            item.setClickHandler(event -> {
                ActionContext actionContext = createActionContextFromMenuClick(event);
                GlobalActionManager.executeAction(actionString, actionContext);
            });
            return this;
        }

        public FluentMenuItemBuilder closeOnClick() {
            item.setClickHandler(event -> event.closeMenu());
            return this;
        }

        public FluentMenuItemBuilder backOnClick() {
            item.setClickHandler(event -> event.openParentMenu());
            return this;
        }

        public MenuItem build() {
            return item;
        }
    }
}