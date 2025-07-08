package net.exylia.commons.item.config;

import lombok.Getter;
import net.exylia.commons.item.config.RegionFilterType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuración de un item interactivo almacenada en memoria
 * SIMPLIFICADO: Eliminados formatos de mensajes personalizados
 * Se usan placeholders directos: %current_uses%, %max_uses%, %cooldown_formatted%, %cooldown_seconds%
 * ACTUALIZADO: Cooldown en double para mayor precisión
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

    // Sistema de usos
    private final int maxUses;

    // Sistema de cooldown (ACTUALIZADO: double para decimales)
    private final double cooldownSeconds;

    // Efectos
    private final String soundOnUse;
    private final String particlesOnUse;
    private final String fireworkOnUse;
    private final boolean launchFireworkOnUse;

    // Configuración de acciones
    private final Map<String, Object> actionConfig;

    // Restricciones de movimiento
    private final boolean allowMovement;
    private final boolean allowShiftClick;
    private final boolean allowDrop;
    private final boolean allowSwapToOffhand;
    private final boolean allowNumberKeys;

    // Placeholders
    private final boolean usePlaceholders;

    // ===== CONFIGURACIÓN DE REGIONES =====

    /**
     * Tipo de filtro de regiones: WHITELIST o BLACKLIST
     */
    private final RegionFilterType regionType;

    /**
     * Lista de regiones para el filtro
     */
    private final List<String> regionList;

    /**
     * Cooldowns específicos por región (ACTUALIZADO: double)
     * Clave: nombre de región, Valor: cooldown en segundos (decimal)
     */
    private final Map<String, Double> regionCooldowns;

    ItemConfiguration(ItemConfigurationBuilder builder) {
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
     * Obtiene el cooldown específico para una región (ACTUALIZADO: double)
     * @param regionName Nombre de la región
     * @return Cooldown en segundos, o el cooldown por defecto si no está configurado
     */
    public double getCooldownForRegion(String regionName) {
        return regionCooldowns.getOrDefault(regionName, cooldownSeconds);
    }

    /**
     * Obtiene el cooldown más alto entre todas las regiones dadas (ACTUALIZADO: double)
     * @param regionNames Lista de nombres de regiones
     * @return Cooldown más alto en segundos
     */
    public double getHighestCooldownForRegions(List<String> regionNames) {
        if (regionNames.isEmpty()) {
            return cooldownSeconds;
        }

        return regionNames.stream()
                .mapToDouble(this::getCooldownForRegion)
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

    // ===== MÉTODOS DE CONVENIENCIA PARA COOLDOWN (ACTUALIZADO: double) =====

    public boolean hasCooldown() {
        return cooldownSeconds > 0.0;
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

    /**
     * Clase Builder separada en su propio archivo
     * @see ItemConfigurationBuilder
     */
    public static class Builder extends ItemConfigurationBuilder {
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