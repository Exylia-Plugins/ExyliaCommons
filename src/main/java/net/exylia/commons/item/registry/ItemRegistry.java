
package net.exylia.commons.item.registry;

import net.exylia.commons.item.config.ItemConfiguration;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Interfaz para el registro de configuraciones de items
 */
public interface ItemRegistry {

    /**
     * Registra una configuración de ítem
     * @param id ID único del ítem
     * @param config Configuración del ítem
     */
    void registerItemConfiguration(String id, ItemConfiguration config);

    /**
     * Registra múltiples configuraciones desde ConfigurationSection
     * @param configSection Sección con múltiples ítems
     */
    void registerItemConfigurations(ConfigurationSection configSection);

    /**
     * Obtiene una configuración registrada
     * @param id ID del ítem
     * @return Configuración o null si no existe
     */
    @Nullable
    ItemConfiguration getItemConfiguration(String id);

    /**
     * Verifica si existe una configuración
     * @param id ID del ítem
     * @return true si existe
     */
    boolean hasItemConfiguration(String id);

    /**
     * Remueve una configuración
     * @param id ID del ítem
     */
    void unregisterItemConfiguration(String id);

    /**
     * Recarga una configuración específica
     * @param id ID del ítem
     * @param config Nueva configuración
     */
    void reloadItemConfiguration(String id, ItemConfiguration config);

    /**
     * Recarga todas las configuraciones desde ConfigurationSection
     * @param configSection Sección con ítems
     */
    void reloadAllConfigurations(ConfigurationSection configSection);

    /**
     * Obtiene todas las configuraciones registradas
     * @return Map de configuraciones
     */
    Map<String, ItemConfiguration> getAllConfigurations();

    /**
     * Limpia todas las configuraciones
     */
    void clear();

    /**
     * Obtiene el número de configuraciones registradas
     * @return Número de configuraciones
     */
    int size();
}