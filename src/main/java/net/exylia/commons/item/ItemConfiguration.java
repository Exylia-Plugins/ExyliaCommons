package net.exylia.commons.item;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuración de un item interactivo almacenada en memoria
 * SIMPLIFICADO: Eliminados formatos de mensajes personalizados
 * Se usan placeholders directos: %current_uses%, %max_uses%, %cooldown_formatted%, %cooldown_seconds%
 */
@Getter
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

    // Sistema de usos (simplificado)
    private final int maxUses;

    // Sistema de cooldown (simplificado)
    private final int cooldownSeconds;

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

    // ===== CONFIGURACIÓN DE REGIONES (simplificada) =====

    /**
     * Tipo de filtro de regiones: WHITELIST o BLACKLIST
     */
    private final RegionFilterType regionType;

    /**
     * Lista de regiones para el filtro
     */
    private final List<String> regionList;

    /**
     * Cooldowns específicos por región
     * Clave: nombre de región, Valor: cooldown en segundos
     */
    private final Map<String, Integer> regionCooldowns;

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
        this.cooldownSeconds = builder.cooldownSeconds;

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

        // Propiedades de regiones
        this.regionType = builder.regionType;
        this.regionList = new ArrayList<>(builder.regionList);
        this.regionCooldowns = new HashMap<>(builder.regionCooldowns);
    }

    // ===== MÉTODOS DE CONVENIENCIA PARA REGIONES =====

    /**
     * Verifica si el item tiene configuración de regiones
     * @return true si tiene regiones configuradas
     */
    public boolean hasRegionConfiguration() {
        return regionType != RegionFilterType.NONE && !regionList.isEmpty();
    }

    /**
     * Verifica si el item puede ser usado en una región específica
     * @param regionName Nombre de la región
     * @return true si puede ser usado
     */
    public boolean canUseInRegion(String regionName) {
        if (!hasRegionConfiguration()) {
            return true; // Sin configuración = permitir en todas las regiones
        }

        boolean isInList = regionList.contains(regionName);

        return switch (regionType) {
            case WHITELIST -> isInList; // Solo permitir en regiones de la lista
            case BLACKLIST -> !isInList; // Permitir en todas excepto las de la lista
            case NONE -> true; // Sin filtro
        };
    }

    /**
     * Verifica si el item puede ser usado en cualquiera de las regiones dadas
     * @param regionNames Lista de nombres de regiones
     * @return true si puede ser usado en al menos una región
     */
    public boolean canUseInAnyRegion(List<String> regionNames) {
        if (!hasRegionConfiguration()) {
            return true;
        }

        return regionNames.stream().anyMatch(this::canUseInRegion);
    }

    /**
     * Obtiene el cooldown específico para una región
     * @param regionName Nombre de la región
     * @return Cooldown en segundos, o el cooldown por defecto si no está configurado
     */
    public int getCooldownForRegion(String regionName) {
        return regionCooldowns.getOrDefault(regionName, cooldownSeconds);
    }

    /**
     * Obtiene el cooldown más alto entre todas las regiones dadas
     * @param regionNames Lista de nombres de regiones
     * @return Cooldown más alto en segundos
     */
    public int getHighestCooldownForRegions(List<String> regionNames) {
        if (regionNames.isEmpty()) {
            return cooldownSeconds;
        }

        return regionNames.stream()
                .mapToInt(this::getCooldownForRegion)
                .max()
                .orElse(cooldownSeconds);
    }

    /**
     * Verifica si hay cooldowns específicos configurados para regiones
     * @return true si hay cooldowns por región
     */
    public boolean hasRegionCooldowns() {
        return !regionCooldowns.isEmpty();
    }

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

    public boolean hasCooldown() {
        return cooldownSeconds > 0;
    }

    // ===== MÉTODOS DE CONVENIENCIA PARA ACTION-CONFIG =====

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

    public String getActionConfigString(String key, String defaultValue) {
        Object value = actionConfig.get(key);
        return value != null ? value.toString() : defaultValue;
    }

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
        if (value instanceof String stringValue) {
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

    public List<String> getActionConfigListString(String key) {
        return getActionConfigListString(key, new ArrayList<>());
    }

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
        private int cooldownSeconds = 0;
        private boolean allowMovement = true;
        private boolean allowShiftClick = true;
        private boolean allowDrop = true;
        private boolean allowSwapToOffhand = true;
        private boolean allowNumberKeys = true;

        // Campos para efectos
        private String soundOnUse = null;
        private String particlesOnUse = null;
        private String fireworkOnUse = null;
        private boolean launchFireworkOnUse = false;

        private Map<String, Object> actionConfig = new HashMap<>();
        private boolean usePlaceholders = false;

        // ===== CAMPOS PARA REGIONES =====
        private RegionFilterType regionType = RegionFilterType.NONE;
        private List<String> regionList = new ArrayList<>();
        private Map<String, Integer> regionCooldowns = new HashMap<>();

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

        public Builder cooldownSeconds(int seconds) {
            this.cooldownSeconds = seconds;
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

        public Builder soundOnUse(String soundString) {
            this.soundOnUse = soundString;
            return this;
        }

        public Builder particlesOnUse(String particleString) {
            this.particlesOnUse = particleString;
            return this;
        }

        public Builder fireworkOnUse(String fireworkString) {
            this.fireworkOnUse = fireworkString;
            return this;
        }

        public Builder launchFireworkOnUse(boolean launch) {
            this.launchFireworkOnUse = launch;
            return this;
        }

        public Builder allowMovement(boolean allow) {
            this.allowMovement = allow;
            return this;
        }

        public Builder allowShiftClick(boolean allow) {
            this.allowShiftClick = allow;
            return this;
        }

        public Builder allowDrop(boolean allow) {
            this.allowDrop = allow;
            return this;
        }

        public Builder allowSwapToOffhand(boolean allow) {
            this.allowSwapToOffhand = allow;
            return this;
        }

        public Builder allowNumberKeys(boolean allow) {
            this.allowNumberKeys = allow;
            return this;
        }

        public Builder lobbyItem() {
            return allowMovement(false)
                    .allowShiftClick(false)
                    .allowDrop(false)
                    .allowSwapToOffhand(false)
                    .allowNumberKeys(false);
        }

        public Builder userItem() {
            return allowMovement(true)
                    .allowShiftClick(true)
                    .allowDrop(true)
                    .allowSwapToOffhand(true)
                    .allowNumberKeys(true);
        }

        // ===== BUILDERS PARA REGIONES =====

        public Builder regionType(RegionFilterType type) {
            this.regionType = type != null ? type : RegionFilterType.NONE;
            return this;
        }

        public Builder regionType(String typeString) {
            try {
                this.regionType = RegionFilterType.valueOf(typeString.toUpperCase());
            } catch (IllegalArgumentException e) {
                this.regionType = RegionFilterType.NONE;
            }
            return this;
        }

        public Builder regionList(List<String> regions) {
            this.regionList = new ArrayList<>(regions);
            return this;
        }

        public Builder regionList(String... regions) {
            this.regionList = List.of(regions);
            return this;
        }

        public Builder addRegion(String regionName) {
            if (!this.regionList.contains(regionName)) {
                this.regionList.add(regionName);
            }
            return this;
        }

        public Builder regionCooldowns(Map<String, Integer> cooldowns) {
            this.regionCooldowns = new HashMap<>(cooldowns);
            return this;
        }

        public Builder regionCooldown(String regionName, int cooldownSeconds) {
            this.regionCooldowns.put(regionName, cooldownSeconds);
            return this;
        }

        public Builder whitelistRegions(String... regions) {
            return regionType(RegionFilterType.WHITELIST)
                    .regionList(regions);
        }

        public Builder blacklistRegions(String... regions) {
            return regionType(RegionFilterType.BLACKLIST)
                    .regionList(regions);
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

            // Configuración de cooldown (simplificado)
            if (config.contains("cooldown")) {
                cooldownSeconds(config.getInt("cooldown"));
            }

            // Efectos
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

            // ===== CONFIGURACIONES PARA REGIONES =====

            // Tipo de región
            if (config.contains("region.type")) {
                regionType(config.getString("region.type"));
            }

            // Lista de regiones
            if (config.contains("region.list")) {
                if (config.isList("region.list")) {
                    regionList(config.getStringList("region.list"));
                } else {
                    // Si es un string, dividir por comas
                    String regionString = config.getString("region.list");
                    if (regionString != null && !regionString.trim().isEmpty()) {
                        String[] regions = regionString.split(",");
                        List<String> regionList = new ArrayList<>();
                        for (String region : regions) {
                            String trimmed = region.trim();
                            if (!trimmed.isEmpty()) {
                                regionList.add(trimmed);
                            }
                        }
                        regionList(regionList);
                    }
                }
            }

            // Cooldowns por región
            if (config.contains("region.cooldowns")) {
                ConfigurationSection cooldownSection = config.getConfigurationSection("region.cooldowns");
                if (cooldownSection != null) {
                    Map<String, Integer> cooldowns = new HashMap<>();
                    for (String regionName : cooldownSection.getKeys(false)) {
                        int cooldown = cooldownSection.getInt(regionName);
                        cooldowns.put(regionName, cooldown);
                    }
                    regionCooldowns(cooldowns);
                }
            }

            // Auto-detectar placeholders
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
                ", regionType=" + regionType +
                ", regionList=" + regionList.size() + " regions" +
                ", regionCooldowns=" + regionCooldowns.size() + " region cooldowns" +
                '}';
    }
}