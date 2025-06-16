package net.exylia.commons.item;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuración de un item interactivo almacenada en memoria
 * Esta clase contiene toda la configuración que NO necesita persistir en NBT
 */
public class ItemConfiguration {

    // Propiedades visuales
    private final String material;
    private final String name;
    private final List<String> lore;
    private final int amount;
    private final boolean glowing;
    private final boolean hideAttributes;

    // Comportamiento
    private final List<String> commands;
    private final String action;
    private final boolean consumeOnUse;
    private final boolean cancelEvent;
    private final boolean stackable;

    // Sistema de usos
    private final int maxUses;
    private final String usesDisplayFormat;
    private final boolean showUsesInLore;
    private final boolean showUsesInName;

    // Sistema de cooldown
    private final int cooldownSeconds;
    private final String cooldownMessage;
    private final boolean showCooldownInLore;
    private final boolean showCooldownInName;

    private final String soundOnUse;
    private final String particlesOnUse;
    private final String fireworkOnUse;
    private final boolean launchFireworkOnUse;

    // Configuración de acciones
    private final Map<String, Object> actionConfig;


    private final boolean allowMovement;
    private final boolean allowShiftClick;
    private final boolean allowDrop;
    private final boolean allowSwapToOffhand;
    private final boolean allowNumberKeys;

    // Placeholders
    private final boolean usePlaceholders;

    private ItemConfiguration(Builder builder) {
        this.material = builder.material;
        this.name = builder.name;
        this.lore = new ArrayList<>(builder.lore);
        this.amount = builder.amount;
        this.glowing = builder.glowing;
        this.hideAttributes = builder.hideAttributes;
        this.commands = new ArrayList<>(builder.commands);
        this.action = builder.action;
        this.consumeOnUse = builder.consumeOnUse;
        this.cancelEvent = builder.cancelEvent;
        this.stackable = builder.stackable;
        this.maxUses = builder.maxUses;
        this.usesDisplayFormat = builder.usesDisplayFormat;
        this.showUsesInLore = builder.showUsesInLore;
        this.showUsesInName = builder.showUsesInName;
        this.cooldownSeconds = builder.cooldownSeconds;
        this.cooldownMessage = builder.cooldownMessage;
        this.showCooldownInLore = builder.showCooldownInLore;
        this.showCooldownInName = builder.showCooldownInName;

        this.soundOnUse = builder.soundOnUse;
        this.particlesOnUse = builder.particlesOnUse;
        this.fireworkOnUse = builder.fireworkOnUse;
        this.launchFireworkOnUse = builder.launchFireworkOnUse;

        this.actionConfig = new HashMap<>(builder.actionConfig);
        this.usePlaceholders = builder.usePlaceholders;

        this.allowMovement = builder.allowMovement;
        this.allowShiftClick = builder.allowShiftClick;
        this.allowDrop = builder.allowDrop;
        this.allowSwapToOffhand = builder.allowSwapToOffhand;
        this.allowNumberKeys = builder.allowNumberKeys;
    }

    // ===== GETTERS ORIGINALES =====

    public String getMaterial() { return material; }
    public String getName() { return name; }
    public List<String> getLore() { return new ArrayList<>(lore); }
    public int getAmount() { return amount; }
    public boolean isGlowing() { return glowing; }
    public boolean shouldHideAttributes() { return hideAttributes; }
    public List<String> getCommands() { return new ArrayList<>(commands); }
    public String getAction() { return action; }
    public boolean shouldConsumeOnUse() { return consumeOnUse; }
    public boolean shouldCancelEvent() { return cancelEvent; }
    public boolean isStackable() { return stackable; }
    public int getMaxUses() { return maxUses; }
    public String getUsesDisplayFormat() { return usesDisplayFormat; }
    public boolean shouldShowUsesInLore() { return showUsesInLore; }
    public boolean shouldShowUsesInName() { return showUsesInName; }
    public int getCooldownSeconds() { return cooldownSeconds; }
    public String getCooldownMessage() { return cooldownMessage; }
    public boolean shouldShowCooldownInLore() { return showCooldownInLore; }
    public boolean shouldShowCooldownInName() { return showCooldownInName; }
    public Map<String, Object> getActionConfig() { return new HashMap<>(actionConfig); }
    public boolean usesPlaceholders() { return usePlaceholders; }
    public boolean allowsMovement() { return allowMovement; }
    public boolean allowsShiftClick() { return allowShiftClick; }
    public boolean allowsDrop() { return allowDrop; }
    public boolean allowsSwapToOffhand() { return allowSwapToOffhand; }
    public boolean allowsNumberKeys() { return allowNumberKeys; }

    // ===== NUEVOS GETTERS PARA EFECTOS =====

    /**
     * Obtiene la configuración de sonido al usar el item
     * Formato: SOUND_NAME|VOLUME|PITCH
     * Ejemplo: BLOCK_NOTE_BLOCK_PLING|0.5|1.0
     */
    public String getSoundOnUse() { return soundOnUse; }

    /**
     * Obtiene la configuración de partículas al usar el item
     * Formato: PARTICLE_NAME|COUNT|OFFSET_X|OFFSET_Y|OFFSET_Z|EXTRA|DATA
     * Ejemplo: FLAME|10|0.5|0.5|0.5|0.1
     */
    public String getParticlesOnUse() { return particlesOnUse; }

    /**
     * Obtiene la configuración de fuegos artificiales al usar el item
     * Formato: TYPE|COLORS|FADE_COLORS|FLICKER|TRAIL|POWER
     * Ejemplo: BALL|255,0,0;0,255,0|255,255,255|true|true|1
     */
    public String getFireworkOnUse() { return fireworkOnUse; }

    /**
     * Verifica si debe lanzar un fuego artificial al usar el item
     */
    public boolean shouldLaunchFireworkOnUse() { return launchFireworkOnUse; }

    // ===== MÉTODOS DE CONVENIENCIA PARA EFECTOS =====

    /**
     * Verifica si el item tiene sonido configurado
     */
    public boolean hasSound() {
        return soundOnUse != null && !soundOnUse.trim().isEmpty();
    }

    /**
     * Verifica si el item tiene partículas configuradas
     */
    public boolean hasParticles() {
        return particlesOnUse != null && !particlesOnUse.trim().isEmpty();
    }

    /**
     * Verifica si el item tiene fuegos artificiales configurados
     */
    public boolean hasFirework() {
        return (fireworkOnUse != null && !fireworkOnUse.trim().isEmpty()) || launchFireworkOnUse;
    }

    /**
     * Verifica si el item tiene algún efecto visual/sonoro
     */
    public boolean hasEffects() {
        return hasSound() || hasParticles() || hasFirework();
    }

    // ===== MÉTODOS DE CONVENIENCIA PARA COOLDOWN =====

    /**
     * Verifica si el item tiene cooldown configurado
     * @return true si tiene cooldown
     */
    public boolean hasCooldown() {
        return cooldownSeconds > 0;
    }

    // ===== MÉTODOS DE CONVENIENCIA PARA ACTION-CONFIG =====

    /**
     * Obtiene un valor del action-config
     * @param key Clave del valor
     * @param defaultValue Valor por defecto si no existe
     * @return Valor encontrado o valor por defecto
     */
    @SuppressWarnings("unchecked")
    public <T> T getActionConfigValue(String key, T defaultValue) {
        Object value = actionConfig.get(key);
        if (value != null) {
            try {
                return (T) value;
            } catch (ClassCastException e) {
                // Si no se puede castear, intentar conversiones comunes
                return convertActionConfigValue(value, defaultValue);
            }
        }
        return defaultValue;
    }

    /**
     * Obtiene un valor entero del action-config
     * @param key Clave del valor
     * @param defaultValue Valor por defecto
     * @return Valor entero
     */
    public int getActionConfigInt(String key, int defaultValue) {
        Object value = actionConfig.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException ignored) {}
        }
        return defaultValue;
    }

    /**
     * Obtiene un valor double del action-config
     * @param key Clave del valor
     * @param defaultValue Valor por defecto
     * @return Valor double
     */
    public double getActionConfigDouble(String key, double defaultValue) {
        Object value = actionConfig.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException ignored) {}
        }
        return defaultValue;
    }

    /**
     * Obtiene un valor booleano del action-config
     * @param key Clave del valor
     * @param defaultValue Valor por defecto
     * @return Valor booleano
     */
    public boolean getActionConfigBoolean(String key, boolean defaultValue) {
        Object value = actionConfig.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            String str = ((String) value).toLowerCase();
            return str.equals("true") || str.equals("yes") || str.equals("1");
        }
        if (value instanceof Number) {
            return ((Number) value).intValue() != 0;
        }
        return defaultValue;
    }

    /**
     * Obtiene un valor string del action-config
     * @param key Clave del valor
     * @param defaultValue Valor por defecto
     * @return Valor string
     */
    public String getActionConfigString(String key, String defaultValue) {
        Object value = actionConfig.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    /**
     * Obtiene una lista de strings del action-config
     * @param key Clave del valor
     * @param defaultValue Lista por defecto si no existe
     * @return Lista de strings
     */
    public List<String> getActionConfigListString(String key, List<String> defaultValue) {
        Object value = actionConfig.get(key);

        if (value == null) {
            return defaultValue != null ? new ArrayList<>(defaultValue) : new ArrayList<>();
        }

        // Si ya es una lista
        if (value instanceof List<?>) {
            List<String> result = new ArrayList<>();
            for (Object item : (List<?>) value) {
                if (item != null) {
                    result.add(item.toString());
                }
            }
            return result;
        }

        // Si es un string, intentar dividirlo por comas
        if (value instanceof String) {
            String stringValue = (String) value;
            if (stringValue.trim().isEmpty()) {
                return defaultValue != null ? new ArrayList<>(defaultValue) : new ArrayList<>();
            }

            // Dividir por comas y limpiar espacios
            List<String> result = new ArrayList<>();
            String[] parts = stringValue.split(",");
            for (String part : parts) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    result.add(trimmed);
                }
            }
            return result;
        }

        // Si es cualquier otro tipo, convertir a string y devolver como lista de un elemento
        List<String> result = new ArrayList<>();
        result.add(value.toString());
        return result;
    }

    /**
     * Obtiene una lista de strings del action-config con valor por defecto vacío
     * @param key Clave del valor
     * @return Lista de strings (nunca null)
     */
    public List<String> getActionConfigListString(String key) {
        return getActionConfigListString(key, new ArrayList<>());
    }

    /**
     * Verifica si existe una clave en el action-config
     * @param key Clave a verificar
     * @return true si existe
     */
    public boolean hasActionConfig(String key) {
        return actionConfig.containsKey(key);
    }

    /**
     * Convierte valores de action-config a tipos compatibles
     */
    @SuppressWarnings("unchecked")
    private <T> T convertActionConfigValue(Object value, T defaultValue) {
        if (defaultValue instanceof Integer && value instanceof Number) {
            return (T) Integer.valueOf(((Number) value).intValue());
        }
        if (defaultValue instanceof Double && value instanceof Number) {
            return (T) Double.valueOf(((Number) value).doubleValue());
        }
        if (defaultValue instanceof Boolean) {
            if (value instanceof String) {
                String str = ((String) value).toLowerCase();
                return (T) Boolean.valueOf(str.equals("true") || str.equals("yes") || str.equals("1"));
            }
        }
        if (defaultValue instanceof String) {
            return (T) value.toString();
        }
        return defaultValue;
    }

    // ===== BUILDER =====

    public static Builder builder() {
        return new Builder();
    }

    public static Builder fromConfig(ConfigurationSection config) {
        return new Builder().loadFromConfig(config);
    }

    public static class Builder {
        private String material = "STONE";
        private String name = null;
        private List<String> lore = new ArrayList<>();
        private int amount = 1;
        private boolean glowing = false;
        private boolean hideAttributes = true;
        private List<String> commands = new ArrayList<>();
        private String action = null;
        private boolean consumeOnUse = false;
        private boolean cancelEvent = true;
        private boolean stackable = true;
        private int maxUses = -1;
        private String usesDisplayFormat = "§7Usos: §f%current%§7/§f%max%";
        private boolean showUsesInLore = true;
        private boolean showUsesInName = false;
        private int cooldownSeconds = 0;
        private String cooldownMessage = "§cDebes esperar %time% antes de usar este item nuevamente.";
        private boolean showCooldownInLore = false;
        private boolean showCooldownInName = false;
        private boolean allowMovement = true;
        private boolean allowShiftClick = true;
        private boolean allowDrop = true;
        private boolean allowSwapToOffhand = true;
        private boolean allowNumberKeys = true;

        // NUEVOS campos para efectos
        private String soundOnUse = null;
        private String particlesOnUse = null;
        private String fireworkOnUse = null;
        private boolean launchFireworkOnUse = false;

        private Map<String, Object> actionConfig = new HashMap<>();
        private boolean usePlaceholders = false;

        // ===== BUILDERS ORIGINALES =====

        public Builder material(String material) {
            this.material = material;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder lore(List<String> lore) {
            this.lore = new ArrayList<>(lore);
            return this;
        }

        public Builder lore(String... lore) {
            this.lore = List.of(lore);
            return this;
        }

        public Builder amount(int amount) {
            this.amount = amount;
            return this;
        }

        public Builder glowing(boolean glowing) {
            this.glowing = glowing;
            return this;
        }

        public Builder hideAttributes(boolean hideAttributes) {
            this.hideAttributes = hideAttributes;
            return this;
        }

        public Builder commands(List<String> commands) {
            this.commands = new ArrayList<>(commands);
            return this;
        }

        public Builder commands(String... commands) {
            this.commands = List.of(commands);
            return this;
        }

        public Builder action(String action) {
            this.action = action;
            return this;
        }

        public Builder consumeOnUse(boolean consumeOnUse) {
            this.consumeOnUse = consumeOnUse;
            return this;
        }

        public Builder cancelEvent(boolean cancelEvent) {
            this.cancelEvent = cancelEvent;
            return this;
        }

        public Builder stackable(boolean stackable) {
            this.stackable = stackable;
            return this;
        }

        public Builder maxUses(int maxUses) {
            this.maxUses = maxUses;
            return this;
        }

        public Builder usesDisplayFormat(String format) {
            this.usesDisplayFormat = format;
            return this;
        }

        public Builder showUsesInLore(boolean show) {
            this.showUsesInLore = show;
            return this;
        }

        public Builder showUsesInName(boolean show) {
            this.showUsesInName = show;
            return this;
        }

        public Builder cooldownSeconds(int seconds) {
            this.cooldownSeconds = seconds;
            return this;
        }

        public Builder cooldownMessage(String message) {
            this.cooldownMessage = message;
            return this;
        }

        public Builder showCooldownInLore(boolean show) {
            this.showCooldownInLore = show;
            return this;
        }

        public Builder showCooldownInName(boolean show) {
            this.showCooldownInName = show;
            return this;
        }

        public Builder usePlaceholders(boolean use) {
            this.usePlaceholders = use;
            return this;
        }

        public Builder actionConfig(Map<String, Object> config) {
            this.actionConfig = new HashMap<>(config);
            return this;
        }

        public Builder actionConfigValue(String key, Object value) {
            this.actionConfig.put(key, value);
            return this;
        }

        /**
         * Establece el sonido que se reproduce al usar el item
         * @param soundString Formato: SOUND_NAME|VOLUME|PITCH
         */
        public Builder soundOnUse(String soundString) {
            this.soundOnUse = soundString;
            return this;
        }

        /**
         * Establece las partículas que se muestran al usar el item
         * @param particleString Formato: PARTICLE_NAME|COUNT|OFFSET_X|OFFSET_Y|OFFSET_Z|EXTRA|DATA
         */
        public Builder particlesOnUse(String particleString) {
            this.particlesOnUse = particleString;
            return this;
        }

        /**
         * Establece los fuegos artificiales que se lanzan al usar el item
         * @param fireworkString Formato: TYPE|COLORS|FADE_COLORS|FLICKER|TRAIL|POWER
         */
        public Builder fireworkOnUse(String fireworkString) {
            this.fireworkOnUse = fireworkString;
            return this;
        }

        /**
         * Establece si debe lanzar un fuego artificial aleatorio al usar el item
         * @param launch true para lanzar fuego artificial aleatorio
         */
        public Builder launchFireworkOnUse(boolean launch) {
            this.launchFireworkOnUse = launch;
            return this;
        }

        /**
         * Establece si el item puede ser movido en inventarios
         * @param allow true para permitir movimiento
         * @return Este builder para encadenamiento
         */
        public Builder allowMovement(boolean allow) {
            this.allowMovement = allow;
            return this;
        }

        /**
         * Establece si se permite shift+click
         * @param allow true para permitir shift+click
         * @return Este builder para encadenamiento
         */
        public Builder allowShiftClick(boolean allow) {
            this.allowShiftClick = allow;
            return this;
        }

        /**
         * Establece si se permite soltar el item
         * @param allow true para permitir soltar
         * @return Este builder para encadenamiento
         */
        public Builder allowDrop(boolean allow) {
            this.allowDrop = allow;
            return this;
        }

        /**
         * Establece si se permite intercambiar con mano secundaria
         * @param allow true para permitir intercambio
         * @return Este builder para encadenamiento
         */
        public Builder allowSwapToOffhand(boolean allow) {
            this.allowSwapToOffhand = allow;
            return this;
        }

        /**
         * Establece si se permiten teclas numéricas
         * @param allow true para permitir teclas numéricas
         * @return Este builder para encadenamiento
         */
        public Builder allowNumberKeys(boolean allow) {
            this.allowNumberKeys = allow;
            return this;
        }

        /**
         * Configuración rápida para items de lobby (no movibles)
         * @return Este builder configurado para lobby
         */
        public Builder lobbyItem() {
            return allowMovement(false)
                    .allowShiftClick(false)
                    .allowDrop(false)
                    .allowSwapToOffhand(false)
                    .allowNumberKeys(false);
        }

        /**
         * Configuración rápida para items de usuario (completamente movibles)
         * @return Este builder configurado para usuario
         */
        public Builder userItem() {
            return allowMovement(true)
                    .allowShiftClick(true)
                    .allowDrop(true)
                    .allowSwapToOffhand(true)
                    .allowNumberKeys(true);
        }

        public Builder loadFromConfig(ConfigurationSection config) {
            // Cargar configuración original
            if (config.contains("material")) {
                material(config.getString("material"));
            }

            if (config.contains("name")) {
                name(config.getString("name"));
            }

            if (config.contains("lore")) {
                if (config.isList("lore")) {
                    lore(config.getStringList("lore"));
                } else {
                    lore(config.getString("lore"));
                }
            }

            if (config.contains("amount")) {
                amount(config.getInt("amount"));
            }

            if (config.contains("glow") || config.contains("glowing")) {
                glowing(config.getBoolean("glow", config.getBoolean("glowing")));
            }

            if (config.contains("hide-attributes")) {
                hideAttributes(config.getBoolean("hide-attributes"));
            }

            if (config.contains("commands")) {
                if (config.isList("commands")) {
                    commands(config.getStringList("commands"));
                } else {
                    commands(config.getString("commands"));
                }
            }

            if (config.contains("action")) {
                action(config.getString("action"));
            }

            if (config.contains("consume-on-use")) {
                consumeOnUse(config.getBoolean("consume-on-use"));
            }

            if (config.contains("cancel-event")) {
                cancelEvent(config.getBoolean("cancel-event"));
            }

            if (config.contains("stackable")) {
                stackable(config.getBoolean("stackable"));
            }

            if (config.contains("max-uses")) {
                maxUses(config.getInt("max-uses"));
            }

            if (config.contains("uses-format")) {
                usesDisplayFormat(config.getString("uses-format"));
            }

            if (config.contains("show-uses-in-lore")) {
                showUsesInLore(config.getBoolean("show-uses-in-lore"));
            }

            if (config.contains("show-uses-in-name")) {
                showUsesInName(config.getBoolean("show-uses-in-name"));
            }

            // Configuración de cooldown
            if (config.contains("cooldown")) {
                cooldownSeconds(config.getInt("cooldown"));
            }

            if (config.contains("cooldown-message")) {
                cooldownMessage(config.getString("cooldown-message"));
            }

            if (config.contains("show-cooldown-in-lore")) {
                showCooldownInLore(config.getBoolean("show-cooldown-in-lore"));
            }

            if (config.contains("show-cooldown-in-name")) {
                showCooldownInName(config.getBoolean("show-cooldown-in-name"));
            }

            // NUEVAS configuraciones para efectos
            if (config.contains("sound-on-use")) {
                soundOnUse(config.getString("sound-on-use"));
            }

            if (config.contains("particles-on-use")) {
                particlesOnUse(config.getString("particles-on-use"));
            }

            if (config.contains("firework-on-use")) {
                fireworkOnUse(config.getString("firework-on-use"));
            }

            if (config.contains("launch-firework")) {
                launchFireworkOnUse(config.getBoolean("launch-firework"));
            }
            if (config.contains("allow-movement")) {
                allowMovement(config.getBoolean("allow-movement"));
            }

            if (config.contains("allow-shift-click")) {
                allowShiftClick(config.getBoolean("allow-shift-click"));
            }

            if (config.contains("allow-drop")) {
                allowDrop(config.getBoolean("allow-drop"));
            }

            if (config.contains("allow-swap-offhand")) {
                allowSwapToOffhand(config.getBoolean("allow-swap-offhand"));
            }

            if (config.contains("allow-number-keys")) {
                allowNumberKeys(config.getBoolean("allow-number-keys"));
            }

            boolean autoDetectPlaceholders = false;
            String nameText = config.getString("name", "");
            List<String> loreList = config.getStringList("lore");

            if (containsPlaceholders(nameText) ||
                    loreList.stream().anyMatch(this::containsPlaceholders)) {
                autoDetectPlaceholders = true;
            }

            usePlaceholders(config.getBoolean("use-placeholders", autoDetectPlaceholders));

            if (config.contains("action-config")) {
                if (config.isConfigurationSection("action-config")) {
                    Map<String, Object> actionConfigMap = new HashMap<>();
                    var actionConfigSection = config.getConfigurationSection("action-config");
                    if (actionConfigSection != null) {
                        for (String key : actionConfigSection.getKeys(false)) {
                            actionConfigMap.put(key, actionConfigSection.get(key));
                        }
                    }
                    actionConfig(actionConfigMap);
                }
            }
            if (config.contains("item-type")) {
                String itemType = config.getString("item-type", "user");
                if (itemType.equalsIgnoreCase("lobby")) {
                    lobbyItem();
                } else if (itemType.equalsIgnoreCase("user")) {
                    userItem();
                }
            }

            return this;
        }

        private boolean containsPlaceholders(String text) {
            return text != null && (text.contains("%") || text.contains("{") || text.contains("<"));
        }

        public ItemConfiguration build() {
            return new ItemConfiguration(this);
        }
    }

    @Override
    public String toString() {
        return "ItemConfiguration{" +
                "material='" + material + '\'' +
                ", name='" + name + '\'' +
                ", commands=" + commands.size() +
                ", action='" + action + '\'' +
                ", maxUses=" + maxUses +
                ", cooldownSeconds=" + cooldownSeconds +
                ", actionConfig=" + actionConfig.size() + " keys" +
                ", stackable=" + stackable +
                ", hasSound=" + hasSound() +
                ", hasParticles=" + hasParticles() +
                ", hasFirework=" + hasFirework() +
                ", usesDisplayFormat='" + usesDisplayFormat + '\'' +
                ", showUsesInLore=" + showUsesInLore +
                ", showUsesInName=" + showUsesInName +
                ", cooldownMessage='" + cooldownMessage + '\'' +
                ", showCooldownInLore=" + showCooldownInLore +
                ", showCooldownInName=" + showCooldownInName +
                '}';
    }
}